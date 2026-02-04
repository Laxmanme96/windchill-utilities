package ext.enersys.cm2.xml.service;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import ext.enersys.cm2.CM2Helper;
import ext.enersys.cm2.xml.EnerSysApprovalMatrixUtility;
import ext.enersys.cm3.CM3Helper;
import ext.enersys.cm3.service.StandardCM3Service;
import ext.enersys.cm4.CM4ServiceUtility;
import ext.enersys.cm4.service.CM4Service;
import ext.enersys.service.ESBusinessHelper;
import ext.enersys.service.ESPropertyHelper;
import ext.enersys.service.EnerSysHelper;
import ext.enersys.service.EnerSysService;
import ext.enersys.utilities.Debuggable;
import ext.enersys.utilities.EnerSysLogUtils;
import wt.change2.ChangeActivityIfc;
import wt.change2.ChangeHelper2;
import wt.change2.ChangeOrder2;
import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeReview;
import wt.change2.WTVariance;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentItem;
import wt.content.ContentRoleType;
import wt.content.ContentServerHelper;
import wt.content.FormatContentHolder;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.enterprise.RevisionControlled;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTHashSet;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.folder.FolderNotFoundException;
import wt.inf.container.OrgContainer;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.inf.library.WTLibrary;
import wt.lifecycle.State;
import wt.lifecycle.Transition;
import wt.log4j.LogR;
import wt.maturity.PromotionNotice;
import wt.org.OrganizationServicesHelper;
import wt.org.WTOrganization;
import wt.org.WTPrincipal;
import wt.org.WTUser;
import wt.part.WTPart;
import wt.pdmlink.PDMLinkProduct;
import wt.pds.StatementSpec;
import wt.pom.Transaction;
import wt.preference.PreferenceClient;
import wt.preference.PreferenceHelper;
import wt.project.Role;
import wt.projmgmt.admin.Project2;
import wt.query.ClassAttribute;
import wt.query.OrderBy;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionContext;
import wt.session.SessionContext.TempSessionContext;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTAttributeNameIfc;
import wt.util.WTException;
import wt.util.WTProperties;
import wt.util.WTPropertyVetoException;
import wt.vc.VersionControlHelper;
import wt.vc.config.LatestConfigSpec;

public class StandardEnerSysApprovalMatrixDefinition
		implements Serializable, EnerSysApprovalMatrixDefinition, Debuggable {

	/**
	 * 
	 */
	private static final String preferenceValue = "/ext/enersys/MULTI_USER_PARTICIPANT_SELECTION";

	private static final long serialVersionUID = 5107827415346490666L;
	private static final String CLASSNAME = StandardEnerSysApprovalMatrixDefinition.class.getName();
	private final static Logger LOGGER = LogR
			.getLoggerInternal(StandardEnerSysApprovalMatrixDefinition.class.getName());

	private Document approvalMatrix;
	private final XPathFactory xpathFactory;
	private final XPath xpath;

	// CHANGE TO CONTROL THE FORCE LOADING OF THE MATRIX EVERY TIME
	private static final boolean FORCE_LOAD_MATRIX = true;// Build 3.1 updated to load everytime
	private static final int DEFAULT_SEQUENCE_NUMBER = 1;

	static final String CHANGE_TRACK_ON_CN_TRACKER_FOR_CT = "CHANGE_TRACK_ON_CN_TRACKER_FOR_CT";

	// v1.13 -- Type Identifiers defined for Firmware Change Types
	static final String FIRMWARE_CR = "WCTYPE|wt.change2.WTChangeRequest2|ext.enersys.firmwareChangeRequest";
	static final String FIRMWARE_CN = "WCTYPE|wt.change2.WTChangeOrder2|ext.enersys.firmwareChangeNotice";
	static final String FIRMWARE_CT = "WCTYPE|wt.change2.WTChangeActivity2|ext.enersys.firmwareChangeActivity";
	static final TypeIdentifier FIRMWARE_CR_TI = TypeIdentifierHelper.getTypeIdentifier(FIRMWARE_CR);
	static final TypeIdentifier FIRMWARE_CN_TI = TypeIdentifierHelper.getTypeIdentifier(FIRMWARE_CN);
	static final TypeIdentifier FIRMWARE_CT_TI = TypeIdentifierHelper.getTypeIdentifier(FIRMWARE_CT);

	// v2.11 - ChangeTrack attribute Internal Name & ENUM Internal Names
	private final static String CHANGE_TRACK_ON_CN_INTR_NAME = "ext.enersys.complexity";
	private static final String FAST_TRACK_INTER_NAME = "PROTOTYPE";
	private static final String FULL_TRACK_INTER_NAME = "PRODUCTION";

	// FAST-TRACK CT CACHE MAINTAINED in the approval matrix - Build v2.11
	private HashSet<Element> changeActivityFastTrackElements;

	// DEVIATION CACHE MAINTAINED in the approval matrix - Build v1.13
	private HashSet<Element> deviationFullTrackElements;
	private HashSet<Element> deviationFastTrackElements;
	private HashSet<Element> deviationNotificationTeamElements;

	// FIRMWARE CRB CACHE MAINTAINED in the approval matrix - Build v1.13
	private HashSet<Element> firmwareCRBElements;
	private HashSet<Element> firmwareNotificationTeamElements;

	// INTERNAL CACHE MAINTAINED FOR RULES defined in the approval matrix
	private HashMap<TypeIdentifier, Element> tiElementApplyToDescendentsMap;
	private HashMap<TypeIdentifier, Element> tiElementSpecificMap;
	private Element elementStructureForDefaultType;

	// INTERNAL CACHE MAINTAINED FOR RULES defined in the approval matrix
	private Map<TypeIdentifier, Set<String>> tiBlackListApplyToDescendentsMap;
	private Map<TypeIdentifier, Set<String>> tiBlackListSpecificMap;
	private Set<String> blackListForDefaultType;

	// INTERNAL CACHE MAINTAINED FOR DOCUMENT TYPE FOR DOCUMENT APPROVAL - ES
	// Migration - Plural
	private HashMap<String, Element> documentTypeElementSpecificMap;
	private Element documentTypeElementStructureForDefault;

	// Jira: 518
	private String SYSTEM_TEST_MANDATORY_ROLE_CRITICAL_PART_PREFERENCE = "/ext/enersys/CRITICAL_PART_MANDATORY_ROLE/SYSTEM_TEST_ENGINEER_VERIFICATION_AND_VALIDATION";
	private String SYSTEM_TEST_ENGINEER_ROLE = "SYSTEM TEST ENGINEER VERIFICATION AND VALIDATION";

	// Jira: 712
	private String QUALITY_MANAGER_ROLE = "QUALITY MANAGER";
	private String LOCAL_PLANT_QUALITY_ROLE = "LOCAL PLANT QUALITY";
	// Tray Requirement -Workflow
	public static final String TRAY_PREF_INTR_NAME = "/ext/enersys/TRAYSPECIFIC_PREFERENCE/INTERNALNAME";
	private static final String PREFERENCE_SEPARATOR = ";";

	// Build 3.7 Version
	private LinkedHashMap<String, LinkedHashMap<String, WTContainer>> newRoleMap = new LinkedHashMap<>();
	// Build 3.8 Version
	private HashSet<String> releasedTargetStateRoles = new HashSet<String>();
	private String CRITICAL_PART_ROLE = "CRITICAL_PART_SELECTOR";

	/* Added Build_V3.8 - Sprint 9 - 8206 */
	private HashMap<String, Object> comboBoxCTHashMapValues = new HashMap<String, Object>();
	/* Added Build_V3.8 - Sprint 9 - 8206 */
	static final String COTS_PART_INT_NAME = "WCTYPE|wt.part.WTPart|com.ptcmscloud.COTS_PART";
	// DEVOPS: Bug 11100 -- Build_v3.16
	static final String MANUFACTURE_PART_INT_NAME = "com.ptc.windchill.suma.part.ManufacturerPart";

	// DEVOPS: Bug 14839 -- Build_v3.16
	static final String ENERSYS_PART_INT_NAME = "WCTYPE|wt.part.WTPart|com.ptcmscloud.ENERSYS_PART";
	static final String FIRMWARE_PART_INT_NAME = "WCTYPE|wt.part.WTPart|com.ptcmscloud.FIRMWARE_PART";

	/*
	 * public static StandardEnerSysApprovalMatrixService
	 * newStandardEnerSysApprovalMatrixService() throws WTException {
	 * LOGGER.debug("--StandardEnerSysApprovalMatrixService--Starting");
	 * StandardEnerSysApprovalMatrixService service = new
	 * StandardEnerSysApprovalMatrixService(); service.initialize();
	 * LOGGER.debug("--StandardEnerSysApprovalMatrixService--INITIALIZED"); return
	 * service; }
	 */

	public StandardEnerSysApprovalMatrixDefinition() {
		// Create XPathFactory object
		this.xpathFactory = XPathFactory.newInstance();
		// Create XPath object
		this.xpath = xpathFactory.newXPath();
		loadApprovalMatrixDOMDocument();
	}

	/* FOR DYNAMIC LOADING REQUIREMENT - START */
	private boolean loadWTDocument() {
		checkAndWriteDebug(Debuggable.START, "#loadWTDocument");
		boolean ret = true;
		WTDocument doc = searchForApprovalMatrixWTDocumentAndReturnDoc();
		if (doc == null) {
			doc = createApprovalMatrixWTDocument();
		}
		if (doc != null) {
			// long newId = doc.getPersistInfo().getObjectIdentifier().getId();
			// if (APPROVAL_MATRIX_WTDOCUMENT_IDA2A2 == -1L ||
			// APPROVAL_MATRIX_WTDOCUMENT_IDA2A2 != newId) {
			// Overwrite the older APPROVAL MATRIX IDA2A2
			// APPROVAL_MATRIX_WTDOCUMENT_IDA2A2 = newId;
			// Found document; retrieve the primary content & begin the processing...
			// Extract content & populate APPROVAL_MATRIX document type;
			try {
				WTArrayList contentItems = new WTArrayList(
						ContentHelper.service.getContentsByRole(doc, ContentRoleType.PRIMARY));
				if (!contentItems.isEmpty()) {
					approvalMatrix = null;
					for (Iterator<ContentItem> i = contentItems.persistableIterator(); i.hasNext();) {
						ContentItem contentItem = i.next();
						if (contentItem instanceof ApplicationData) {
							ApplicationData appData = (ApplicationData) contentItem;
							if (appData.getFileName().endsWith("xml") || appData.getFileName().endsWith("XML")) {
								approvalMatrix = DocumentBuilderFactory.newInstance().newDocumentBuilder()
										.parse(ContentServerHelper.service.findContentStream(appData));
								checkAndWriteDebug(Debuggable.LINE, "#loadWTDocument -->", " approvalMatrix: ",
										approvalMatrix);
							} else {
								checkAndWriteDebug(Debuggable.LINE,
										"#loadWTDocument --> Something else was found as primary content with filename: ",
										appData.getFileName());
							}
						} else {
							checkAndWriteDebug(Debuggable.LINE,
									"#loadWTDocument --> Something else was found as primary content!");
						}
					}
				} else {
					ret = false;
				}
			} catch (WTException | SAXException | IOException | ParserConfigurationException e) {
				e.printStackTrace();
			}
			// }
		} else {
			ret = false;
		}
		checkAndWriteDebug(Debuggable.END, "#loadWTDocument -->", " ret: ", ret);
		return ret;
	}

	/**
	 * Used in check if DS is null.
	 * 
	 * @return
	 * @deprecated - Removed in Build 3.1
	 */
	private boolean isMoreRecentApprovalMatrixWTDocumentExisting() {
		// TODO: add Loggers
		boolean retBool = false;
		if (APPROVAL_MATRIX_WTDOCUMENT_IDA2A2 == -1L) {
			return true;
		}
		if (APPROVAL_MATRIX_WTDOCUMENT_IDA2A2 != -1) {
			QueryResult qr = null;
			try {
				QuerySpec qs = new QuerySpec();
				int indx = qs.addClassList(WTDocument.class, false);
				int[] idxDoc = { indx };
				qs.appendWhere(new SearchCondition(WTDocument.class, WTDocument.NUMBER, SearchCondition.EQUAL,
						EnerSysApprovalMatrixDefinition.WTDOCUMENT_APPROVAL_MATRIX_DOCU_NUMBER), idxDoc);
				qs.appendAnd();
				qs.appendWhere(new SearchCondition(WTDocument.class, WTDocument.NAME, SearchCondition.EQUAL,
						EnerSysApprovalMatrixDefinition.WTDOCUMENT_APPROVAL_MATRIX_DOCU_NAME), idxDoc);
				qs.appendAnd();
				qs.appendWhere(new SearchCondition(WTDocument.class, WTAttributeNameIfc.ID_NAME,
						SearchCondition.GREATER_THAN, APPROVAL_MATRIX_WTDOCUMENT_IDA2A2), idxDoc);
				qs = new LatestConfigSpec().appendSearchCriteria(qs);
				qs.appendSelectAttribute(WTAttributeNameIfc.ID_NAME, indx, true);
				qs.setQueryLimit(1);
				OrderBy ob2 = new OrderBy(new ClassAttribute(WTDocument.class, WTAttributeNameIfc.ID_NAME), true);
				qs.appendOrderBy(ob2, idxDoc);

				qr = PersistenceHelper.manager.find((StatementSpec) qs);
				if (qr.hasMoreElements()) {
					retBool = true;
				}

			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		return retBool;
	}

	private volatile long APPROVAL_MATRIX_WTDOCUMENT_IDA2A2 = -1L;

	private WTDocument createApprovalMatrixWTDocument() {
		checkAndWriteDebug(Debuggable.START, "#createApprovalMatrixWTDocument");
		Transaction transactionObj = null;
		WTDocument docObj = null;

		transactionObj = new Transaction();
		try {
			transactionObj.start();

			WTProperties wtprops = WTProperties.getServerProperties();
			final String MATRIX_LOCATION = wtprops.getProperty("wt.codebase.location") + APPROVAL_MATRIX_FILE_PATH
					+ APPROVAL_MATRIX_FILENAME;

			File approvalFile = new File(MATRIX_LOCATION);

			WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
			WTContainerRef containerRef = WTContainerHelper.service.getOrgContainerRef(org);
			if (containerRef != null) {

				WTContainer containerObj = containerRef.getReferencedContainer();

				String folderNameInContext = "/Default";
				Folder folder = null;
				try {
					folder = FolderHelper.service.getFolder(folderNameInContext, containerRef);
				} catch (FolderNotFoundException ex) {
					checkAndWriteDebug(Debuggable.LINE, "#createApprovalMatrixWTDocument -->",
							" Folder was not found, Creating : " + folderNameInContext + ", in context : "
									+ containerObj.getName());
					folder = FolderHelper.service.createSubFolder(folderNameInContext,
							containerObj.getDefaultDomainReference(), containerRef);
				}

				docObj = WTDocument.newWTDocument();
				docObj.setName(EnerSysApprovalMatrixDefinition.WTDOCUMENT_APPROVAL_MATRIX_DOCU_NAME);
				docObj.setNumber(EnerSysApprovalMatrixDefinition.WTDOCUMENT_APPROVAL_MATRIX_DOCU_NUMBER);

				docObj.setTypeDefinitionReference(
						TypedUtilityServiceHelper.service.getTypeDefinitionReference("wt.doc.WTDocument"));
				docObj.setContainer(containerObj);

				FolderHelper.assignLocation((FolderEntry) docObj, folder);

				docObj = (WTDocument) PersistenceHelper.manager.save(docObj);

				// UPLOADING PRIMARY CONTENT
				// Create ApplicationData for the NEw WTDocument
				ApplicationData theContent = ApplicationData.newApplicationData(docObj);
				theContent.setFileName(approvalFile.getName());
				theContent.setUploadedFromPath(approvalFile.getPath());
				theContent.setRole(ContentRoleType.toContentRoleType("PRIMARY"));
				theContent.setFileSize(approvalFile.length());
				theContent.setDescription("Uploading Approval Matrix");
				theContent = ContentServerHelper.service.updatePrimary((FormatContentHolder) docObj,
						(ApplicationData) theContent, (InputStream) new FileInputStream(approvalFile));
				ContentServerHelper.service.updateHolderFormat(docObj);
				docObj = (WTDocument) PersistenceHelper.manager.refresh((Persistable) docObj, true, true);
			}
			if (docObj != null) {
				checkAndWriteDebug(Debuggable.LINE, "#createApprovalMatrixWTDocument -->",
						" New Document Name : " + docObj.getName() + "\t" + "Number : " + docObj.getNumber());
			}
			transactionObj.commit();
			transactionObj = null;
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		} catch (RemoteException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (transactionObj != null) {
				transactionObj.rollback();
			}
		}
		checkAndWriteDebug(Debuggable.END, "#createApprovalMatrixWTDocument -->", " docObj: ", docObj);
		return docObj;
	}

	// Build 3.1 changed to WTDocumentMaster search
	private WTDocument searchForApprovalMatrixWTDocumentAndReturnDoc() {
		checkAndWriteDebug(Debuggable.START, "#searchForApprovalMatrixWTDocumentAndReturnDoc");
		WTDocument doc = null;
		try {
			QuerySpec qs = new QuerySpec(WTDocumentMaster.class);

			int[] idxDoc = { 0 };
			qs.appendWhere(new SearchCondition(WTDocumentMaster.class, WTDocumentMaster.NUMBER, SearchCondition.EQUAL,
					EnerSysApprovalMatrixDefinition.WTDOCUMENT_APPROVAL_MATRIX_DOCU_NUMBER), idxDoc);
			qs.appendAnd();
			qs.appendWhere(new SearchCondition(WTDocumentMaster.class, WTDocumentMaster.NAME, SearchCondition.EQUAL,
					EnerSysApprovalMatrixDefinition.WTDOCUMENT_APPROVAL_MATRIX_DOCU_NAME), idxDoc);
			// qs = new LatestConfigSpec().appendSearchCriteria(qs);

			// OrderBy ob2 = new OrderBy(new ClassAttribute(WTDocument.class,
			// WTAttributeNameIfc.ID_NAME), true);

			// qs.appendOrderBy(ob2, idxDoc);

			QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
			if (qr.hasMoreElements()) {
				WTDocumentMaster docMas = (WTDocumentMaster) qr.nextElement();
				doc = (WTDocument) VersionControlHelper.service.allIterationsOf(docMas).nextElement();
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#searchForApprovalMatrixWTDocumentAndReturnDoc -->", " doc: ", doc);
		return doc;
	}

	/* DYNAMIC LOADING REQUIREMENT - END */
	@Override
	public Set<String> getBlackListedStatesForObject(WTObject affectedItem) {
		checkAndWriteDebug(Debuggable.START, "#getBlackListedStatesForObject -->", " affectedItem: ", affectedItem);
		Set<String> retHS = null;
		if (checkIfAnyDSIsNull()) {
			loadApprovalMatrixDOMDocument();
		}
		TypeIdentifier ti = TypeIdentifierHelper.getType(affectedItem);
		if (ti != null) {
			// 1st priority is Type Specific
			retHS = tiBlackListSpecificMap.get(ti);
			if (retHS == null) {
				// 2nd priority is Descendent specific
				retHS = tiBlackListApplyToDescendentsMap.get(ti);
			}
			if (retHS == null) {
				// 3rd will be states assigned for DEFAULT type
				retHS = blackListForDefaultType;
			}
		}
		checkAndWriteDebug(Debuggable.END, "#getBlackListedStatesForObject -->", " retHS: ", retHS);
		return retHS;
	}

	@Override
	public LinkedHashMap<String, WTContainer> getRoleContainerInfo(
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap, String role) {
		checkAndWriteDebug(Debuggable.START, "#getRoleContainerInfo --> ", " fullHashMap: ", fullHashMap, " role: ",
				role);
		LinkedHashMap<String, WTContainer> ret = null;
		if (fullHashMap != null) {
			for (Entry<String, LinkedHashMap<String, WTContainer>> entry : fullHashMap.entrySet()) {
				if (entry.getKey().startsWith(role + COLON_ROLE_MAP_DELIM)) {
					ret = entry.getValue();
					break;
				}
			}
		}
		checkAndWriteDebug(Debuggable.END, "#getRoleContainerInfo --> ", " ret: ", ret);
		return ret;
	}

	/**
	 * Extracts EMAIL IDS from list of given users-IDs.
	 */
	@Override
	public String getNotificationEmailIds(final WTObject pbo, final String notificationRoleUserMap, final String DELIM)
			throws WTException {
		HashSet<String> set = new HashSet<>();
		// notificationRoleUserMap looks like: "FIRMWARE DESIGN
		// AUTHORITY=tpm1,FUNCTIONAL SAFETY=log1,PROGRAM MANAGER=tpm1,SOFTWARE QUALTY
		// AUTHORITY=pack1,SYSTEM TEST ENGINEER
		// VERIFICATION & VALIDATION=tpm1,";
		List<String> list = new ArrayList<>(Arrays.asList(notificationRoleUserMap.split(COMMA_DELIM)));
		OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer((WTContained) pbo);

		for (String roleUser : list) {
			String[] usrs = roleUser.split("=");
			// added below check to handle case : Manufacturing=,Notification Only=abc:xyz
			if (usrs.length > 1) {
				usrs = usrs[1].split(COLON_ROLE_MAP_DELIM);
				for (String usr : usrs) {
					try {
						WTUser usrObj = OrganizationServicesHelper.manager.getUser(usr,
								orgContainer.getContextProvider());
						if (usrObj != null) {
							set.add(usrObj.getEMail());
						} else if (usrObj == null) {
							break;
						}
					}

					catch (NullPointerException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return StringUtils.join(set, DELIM);
	}

	/**
	 *
	 * Returns an Iterator which can be used to generate the SEQUENCE defined in the
	 * approval-matrix.<br>
	 */
	@Override
	public Iterator<Integer> getSequencesInOrder(
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap) {
		checkAndWriteDebug(Debuggable.START, "#getSequencesInOrder --> ", " fullHashMap: ", fullHashMap);
		TreeSet<Integer> sequencesInOrder = new TreeSet<>();
		if (fullHashMap != null) {
			for (String key : fullHashMap.keySet()) {
				String[] keys = key.split(COLON_ROLE_MAP_DELIM);
				if (keys.length >= 3) {// v1.13 -- changed from == to >=
					String order = keys[SEQUENCE_ORDER_INDEX];
					int orderSeq = Integer.MIN_VALUE;
					try {
						orderSeq = Integer.parseInt(order);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} finally {
						if (orderSeq < 1) {
							// DONT ADD ROLES FOR INVALID SEQUENCES!!!
							checkAndWriteDebug(Debuggable.LINE,
									"#getSequencesInOrder --> sequence has an invalid non-positive/or zero sequence (Not a Natural number), so, assigning default order : ",
									DEFAULT_SEQUENCE_NUMBER);
							orderSeq = DEFAULT_SEQUENCE_NUMBER;
						} else {
							sequencesInOrder.add(orderSeq);
						}
					}
				}
			}
		}
		checkAndWriteDebug(Debuggable.END, "#getSequencesInOrder --> ", " sequencesInOrder: ", sequencesInOrder);
		return sequencesInOrder.iterator();
	}

	/**
	 * @deprecated from build V3.8 CM 4.0
	 */
	@Override
	public String getSequencesInOrder_String(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap) {
		Iterator<Integer> itr = getSequencesInOrder(fullHashMap);
		return StringUtils.join(itr, COMMA_DELIM);
	}

	/**
	 * @deprecated from build V3.8 CM 4.0
	 */
	@Override
	public String getSubSetRoles_Lite(int givenSequenceNumber, String requiredRoleUserMap, String keyStr) {
		checkAndWriteDebug(Debuggable.START, "#getSubSetRoles_Lite --> ", " givenSequenceNumber: ", givenSequenceNumber,
				" requiredRoleUserMap: ", requiredRoleUserMap, " keyStr: ", keyStr);
		LinkedHashSet<String> sb = new LinkedHashSet<>();

		if (keyStr != null && !keyStr.isEmpty()) {
			keyStr = keyStr.replace("[", "");
			keyStr = keyStr.replace("]", "");
			LinkedHashSet<String> lhs = new LinkedHashSet<String>(Arrays.asList(keyStr.split(COMMA_DELIM)));

			LinkedHashSet<String> t = getParticipantRolesForSequenceNumber_Lite(givenSequenceNumber, lhs);
			if (t != null && !t.isEmpty()) {
				List<String> list = new ArrayList<>(Arrays.asList(requiredRoleUserMap.split(COMMA_DELIM)));
				checkAndWriteDebug(Debuggable.LINE, "#getSubSetRoles_Lite --> ", " list: ", list);
				// SPLITS THE ROLES
				for (String tt : t) {
					// ITERATE OVER THE ROLE USER MAP TO FIND THE CORRESPONDING ROLE+USER MAPPING
					for (String lt : list) {
						if (lt.trim().startsWith(tt.split(COLON_ROLE_MAP_DELIM)[0].trim())) {
							sb.add(lt);
						}
					}
				}
			}
		}
		checkAndWriteDebug(Debuggable.END, "#getSubSetRoles_Lite --> ", " StringUtils.join(sb, COMMA_DELIM): ",
				StringUtils.join(sb, COMMA_DELIM));
		return StringUtils.join(sb, COMMA_DELIM);
	}

	/**
	 * @deprecated from build V3.8 CM 4.0
	 */
	@Override
	public LinkedHashSet<String> getParticipantRolesForSequenceNumber_Lite(int givenSequenceNumber,
			Set<String> mapKeys) {
		checkAndWriteDebug(Debuggable.START, "#getParticipantRolesForSequenceNumber_Lite --> ",
				" givenSequenceNumber: ", givenSequenceNumber, " fullHashMap: ", mapKeys);
		LinkedHashSet<String> rolesInOrder = new LinkedHashSet<>();
		for (String key : mapKeys) {
			String[] keys = key.split(COLON_ROLE_MAP_DELIM);
			if (keys.length >= 3) {// v1.13 -- changed from == to >=
				String order = keys[SEQUENCE_ORDER_INDEX];
				int orderSeq = Integer.MIN_VALUE;
				try {
					orderSeq = Integer.parseInt(order);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} finally {
					if (orderSeq < 1) {
						checkAndWriteDebug(Debuggable.LINE,
								"#getParticipantRolesForSequenceNumber_Lite --> sequence has an invalid non-positive/or zero sequence (Not a Natural number), so, assigning default order : ",
								DEFAULT_SEQUENCE_NUMBER);
						orderSeq = DEFAULT_SEQUENCE_NUMBER;
					}
				}
				if (orderSeq == givenSequenceNumber) {
					String roleName = keys[PARTICIPANT_ROLE_INDEX].trim();
					String required = keys[IS_REQUIRED_INDEX].trim();
					if (required.equalsIgnoreCase(APPR_REQUIRED_VALUE)) {
						roleName = roleName + COLON_ROLE_MAP_DELIM + APPR_REQUIRED_VALUE;
					} else {
						roleName = roleName + COLON_ROLE_MAP_DELIM + APPR_OPTIONAL_VALUE;
					}
					rolesInOrder.add(roleName);
				}
			}
		}
		checkAndWriteDebug(Debuggable.END, "#getParticipantRolesForSequenceNumber_Lite --> ", " rolesInOrder: ",
				rolesInOrder);
		return rolesInOrder;
	}

	/**
	 * Get the relevant roles and its voting right as per given sequence order, from
	 * the approval matrix map.
	 *
	 * @deprecated from build V3.8 CM 4.0
	 * 
	 * @param givenSequenceNumber
	 * @param fullHashMap
	 * @return
	 * @since Build v2.1
	 */
	@Override
	public String getSubSetOfOnlyRoles(int givenSequenceNumber,
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap) {
		checkAndWriteDebug(Debuggable.START, "#getSubSetOfOnlyRoles --> ", " givenSequenceNumber: ",
				givenSequenceNumber, " fullHashMap: ", fullHashMap);
		LinkedHashSet<String> rolesInfoForSeqNumber = getParticipantRolesForSequenceNumber(givenSequenceNumber,
				fullHashMap);
		checkAndWriteDebug(Debuggable.END, "#getSubSetOfOnlyRoles --> ",
				" StringUtils.join(rolesInfoForSeqNumber, COMMA_DELIM): ",
				StringUtils.join(rolesInfoForSeqNumber, COMMA_DELIM));
		return StringUtils.join(rolesInfoForSeqNumber, COMMA_DELIM);
	}

	/**
	 * @deprecated from build V3.8 CM 4.0
	 *
	 *             Return specific roles and its associated participants using the
	 *             Full Role map (from approval matrix) & requiredRoleUserMap (from
	 *             process).
	 */
	@Override
	public String getSubSetRoles(int givenSequenceNumber, String requiredRoleUserMap,
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap) {
		checkAndWriteDebug(Debuggable.START, "#getSubSetRoles --> ", " givenSequenceNumber: ", givenSequenceNumber,
				" requiredRoleUserMap: ", requiredRoleUserMap, " fullHashMap: ", fullHashMap);
		LinkedHashSet<String> sb = new LinkedHashSet<>();
		LinkedHashSet<String> t = getParticipantRolesForSequenceNumber(givenSequenceNumber, fullHashMap);
		if (t != null && !t.isEmpty()) {
			List<String> list = new ArrayList<String>(Arrays.asList(requiredRoleUserMap.split(COMMA_DELIM)));
			checkAndWriteDebug(Debuggable.LINE, "#getSubSetRoles --> ", " list: ", list);
			// SPLITS THE ROLES
			for (String tt : t) {
				// ITERATE OVER THE ROLE USER MAP TO FIND THE CORRESPONDING ROLE+USER MAPPING
				for (String lt : list) {
					if (lt.startsWith(tt.split(COLON_ROLE_MAP_DELIM)[0])) {
						sb.add(lt);
					}
				}
			}
		}

		checkAndWriteDebug(Debuggable.END, "#getSubSetRoles --> ", " StringUtils.join(sb, COMMA_DELIM): ",
				StringUtils.join(sb, COMMA_DELIM));
		return StringUtils.join(sb, COMMA_DELIM);
	}

	/**
	 * Given a SEQUENCE NUMBER & the Full Role Map, it returns the list of
	 * Participant(s) (ROLES) who are supposed to APPROVE the particular task.
	 * 
	 * returns a list : [ROLE1:(REQ/OPT), ROLE2:(REQ/OPT), ROLE3:(REQ/OPT)]
	 */
	@Override
	public LinkedHashSet<String> getParticipantRolesForSequenceNumber(int givenSequenceNumber,
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap) {
		return getParticipantRolesForSequenceNumber_Lite(givenSequenceNumber, fullHashMap.keySet());
	}

	/**
	 * Returns the Notification Team for a given Primary Business Object.
	 * 
	 * @throws WTException
	 * 
	 * @since Build v2.1
	 */
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getNotificationRoleMapWizard(
			Set<NmOid> affectedObj, TypeIdentifier pboTI, NmCommandBean commandBean) throws WTException {
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> ret = null;
		/*
		 * New Matrix For Firmware Part/Document - Added in Build 3.8 Change Management
		 * of Firmware changed - Separate approval matrix for CR,CN,CT in Build 3.8 as
		 * per Enersys Change managment
		 */
		/*
		 * if (isFirmwareCRCNCTpbo(pboTI)) { ret =
		 * calculateParticipantsForNewDeviationAndNotification(pboTI,
		 * getContainerFromCommandBean(commandBean), firmwareNotificationTeamElements,
		 * null); }
		 */
		if (CM2Helper.service.getChangeTypeString(pboTI).equalsIgnoreCase(EnerSysService.ACTIVITY_DEVIATION)) {
			ret = calculateParticipantsForNewDeviationAndNotification(pboTI, getContainerFromCommandBean(commandBean),
					deviationNotificationTeamElements, null);
		} else {
			// TODO: add logic for generic notification teams based on affected items
			ret = getOverallNotificationTeamRoleMapWizard(pboTI, affectedObj);
		}
		return ret;
	}

	private WTContainer getContainerFromCommandBean(NmCommandBean commandBean) throws WTException {
		return commandBean.getContainer();
	}

	/**
	 * Extracts & returns HashMap with Participant tags from the approval matrix for
	 * defined notification team.
	 * 
	 * @return
	 * @throws WTException
	 * @since Build v2.1
	 */
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getOverallNotificationTeamRoleMapWizard(
			TypeIdentifier pboTI, Set<NmOid> affectedObj) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#getOverallNotificationTeamRoleMapWizard -->", " pbo: ", pboTI);
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap = new LinkedHashMap<>();

		Iterator targetRefIterator = affectedObj.iterator();
		while (targetRefIterator.hasNext()) {
			Persistable per = (Persistable) (((NmOid) targetRefIterator.next()).getRefObject());
			String containerName = null;
			WTContainer cont = null;
			if (per instanceof WTContained) {
				containerName = ((WTContained) per).getContainerName();
				cont = ((WTContained) per).getContainer();
				NodeList participantNodes = getNotificationTeamNodeListForObject((WTObject) per, pboTI);
				processParticipantNodes(participantNodes, roleMap, containerName, cont, false, pboTI, "");
			}
		}
		checkAndWriteDebug(Debuggable.END, "#getOverallNotificationTeamRoleMapWizard");
		if (roleMap.isEmpty()) {
			roleMap = null;
		}
		return roleMap;
	}

	/**
	 * Build v2.9 - JIRA: 712 Utility method to convert set of NmOids to set of
	 * Persistable.
	 * 
	 * @param nmOidSet
	 * @return
	 */
	private Set<Persistable> convertNmOidSetToPerSet(Set<NmOid> nmOidSet) {
		Set<Persistable> persistableSet = new HashSet<>();
		for (NmOid nmOid : nmOidSet) {
			persistableSet.add(nmOid.getWtRef().getObject());
		}
		return persistableSet;
	}

	/**
	 * Added in Build v2.1, to support operations from Create-Wizards.<br>
	 * Returns Role-Container maps in order.
	 * 
	 * @since Build v2.1
	 */
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMapWizard(
			Set<NmOid> affectedObj, TypeIdentifier pboTI, NmCommandBean commandBean) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#getChangeApprovalRoleMapWizard -->", " pboTI: ", pboTI);
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap = new LinkedHashMap<>();
		newRoleMap.clear();
		releasedTargetStateRoles.clear();

		// ADO: 14768
		LOGGER.debug("getChangeApprovalRoleMapWizard");
		String changeTrackValue = getChangeTrackInternalValue(pboTI, commandBean);
		LOGGER.debug("changeTrackValue:" + changeTrackValue);
		String RECORDED_CHANGE_TRACK = null;
		if ((changeTrackValue.replaceAll("\\]|\\[", "")).equalsIgnoreCase("MFG")) {
			RECORDED_CHANGE_TRACK = "MFG" + getChangeTypeInternalValue(pboTI, commandBean);
		} else {
			RECORDED_CHANGE_TRACK = changeTrackValue; // Build v3.1
		}
		LOGGER.debug("RECORDED_CHANGE_TRACK:" + RECORDED_CHANGE_TRACK);
		// JIRA: 518, To read preference - Make "System Test Engineer (Verification and
		// Validation)" role as required if affected objects table contains a Critical
		// Part
		boolean prefValue = (boolean) PreferenceHelper.service.getValue(commandBean.getContainerRef(),
				SYSTEM_TEST_MANDATORY_ROLE_CRITICAL_PART_PREFERENCE, PreferenceClient.WINDCHILL_CLIENT_NAME,
				(WTUser) SessionHelper.manager.getPrincipal());
		// Build V3.7 , To read preference - Need to create preference which will be
		// enabling and disabling of the change in order of the task assigned based on
		// approval matrix
		// if critical part selector role is added
		if (isFastTrackCNCTpboBeanCheck(pboTI, commandBean)) {
			// Added on Build v2.11 -- to get FAST-TRACK specific roles
			roleMap = getFastTrackCNCTParticipants((WTObject) getContainerFromCommandBean(commandBean), pboTI,
					prefValue ? affectedObj : null);
			processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObj), roleMap); // Build v2.9 - 712
			return roleMap;
		} /*
			 * New Matrix For Firmware Part/Document - Added in Build 3.8 Change Management
			 * of Firmware changed - Separate approval matrix for CR,CN,CT in Build 3.8 else
			 * if (isFirmwareCRCNCTpbo(pboTI)) { // Added on Build v1.13 -- to get FIRMWARE
			 * CR/CN/CT specific roles roleMap = getFirmwareCRBParticipants((WTObject)
			 * getContainerFromCommandBean(commandBean), pboTI, prefValue ? affectedObj :
			 * null);
			 * processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObj),
			 * roleMap);// Build v2.9 - 712 return roleMap;
			 * 
			 * }
			 */ else if (CM2Helper.service.getChangeTypeString(pboTI)
				.equalsIgnoreCase(EnerSysService.ACTIVITY_DEVIATION)) {
			// Added on Build v1.13 -- to get Deviation specific roles
			if (isFullTrackDeviation(commandBean)) {
				roleMap = getDeviationFullTrackParticipants((WTObject) getContainerFromCommandBean(commandBean), pboTI,
						prefValue ? affectedObj : null);
				processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObj), roleMap); // Build v2.9 - 712
				roleMap = makingDeviationRequiredApproversBasedonState(roleMap, affectedObj);// Added in build V3.10
				return roleMap;
			} else {
				roleMap = getDeviationFastTrackParticipants((WTObject) getContainerFromCommandBean(commandBean), pboTI,
						prefValue ? affectedObj : null);
				processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObj), roleMap); // Build v2.9 - 712
				roleMap = makingDeviationRequiredApproversBasedonState(roleMap, affectedObj);// Added in build V3.10
				return roleMap;
			}
		} else {
			String pnMaturityState = null;
			if (CM2Helper.service.getChangeTypeString(pboTI)
					.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)) {
				if (commandBean.getTextParameter("maturityState") != null) {
					pnMaturityState = commandBean.getTextParameter("maturityState");
				} else if (commandBean.getComboBox().get("maturityState") != null) {
					pnMaturityState = (String) ((List<?>) commandBean.getComboBox().get("maturityState")).get(0);
				}
			} else if (CM2Helper.service.getChangeTypeString(pboTI)
					.equalsIgnoreCase(EnerSysService.ACTIVITY_DOCUMENTAPPROVAL)) {
				HashMap comboBox = (HashMap) commandBean.getComboBox();
				LOGGER.debug("Value of comboBox  " + comboBox);
				Set<String> comboBoxKey = comboBox.keySet();
				for (String key : comboBoxKey) {
					LOGGER.debug("Value of key  " + key);
					if (key != null && !key.isEmpty() && key.contains("ext.enersys.dapTrack")) {
						if (comboBox.get(key) != null) {
							pnMaturityState = (String) ((List<?>) commandBean.getComboBox().get(key)).get(0);
							LOGGER.debug("Value of DAP Track  " + pnMaturityState);
							break;
						}
					}
				}
			}
			/* Added Build_V3.8 - Sprint 9 - 8206 */
			Set<Persistable> affectedObjectsPer = convertNmOidSetToPerSet(affectedObj);
			checkAndWriteDebug(Debuggable.LINE, "#getChangeApprovalRoleMapWizard -->", " affectedObjectsPer: ",
					affectedObjectsPer);
			Set<Persistable> resultingObjects = new HashSet<>();
			if (CM2Helper.service.getChangeTypeString(pboTI).equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {
				resultingObjects = StandardCM3Service.processAndFetchChangeNoticeResultingObjects(commandBean);
				affectedObjectsPer = resultingObjects;
			} else if (CM2Helper.service.getChangeTypeString(pboTI)
					.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEACTIVITY)) {
				resultingObjects = convertNmOidSetToPerSet(
						CM3Helper.service.getChangeTaskResultingObjects(commandBean));
				affectedObjectsPer = resultingObjects;
				comboBoxCTHashMapValues.clear();
				comboBoxCTHashMapValues = commandBean.getComboBox();
			}
			/*
			 * START--Added Build_V4.0 - Bug 9096--Check if COTS PART is present in
			 * Promotion request -If present Excludes wtdocuments approvers
			 */

			// DEVOPS: Bug 11100 -- Check if Manufacturer PART is present in Promotion
			// request
			// If present excludes WTDocuments approvers

			// Added for Build_v3.19
			// #ADO: US 14839 -- Check if Enersys or Firmware is present in
			// Promotion request/ Change request/ Change Notice
			// If present excludes WTDocuments approvers
			boolean isCOTSorManufactureorEnersysorFirmwarePresent = false;
			Set<Persistable> persistableSetWTDocument = new HashSet<>();

			// Added for Build_v3.19
			// #ADO: US 14839
			// Added second and third check for Change Request, Notice and Activity
			if (CM2Helper.service.getChangeTypeString(pboTI).equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)
					|| CM2Helper.service.getChangeTypeString(pboTI)
							.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEREQUEST)
					|| CM2Helper.service.getChangeTypeString(pboTI)
							.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)
					|| CM2Helper.service.getChangeTypeString(pboTI)
							.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEACTIVITY)) {
				for (Persistable persObject : affectedObjectsPer) {

					// Added for Build_v3.19
					// #ADO: US 14839
					// Added third and fourth check for Enersys and Firmware Part
					if (TypeIdentifierHelper.getType(persObject)
							.equals(TypeIdentifierHelper.getTypeIdentifier(COTS_PART_INT_NAME))
							|| TypeIdentifierHelper.getType(persObject)
									.equals(TypeIdentifierHelper.getTypeIdentifier(MANUFACTURE_PART_INT_NAME))
							|| TypeIdentifierHelper.getType(persObject)
									.equals(TypeIdentifierHelper.getTypeIdentifier(ENERSYS_PART_INT_NAME))
							|| TypeIdentifierHelper.getType(persObject)
									.equals(TypeIdentifierHelper.getTypeIdentifier(FIRMWARE_PART_INT_NAME))) {
						LOGGER.debug("######COTS or MANUFACTURE or Enersys or Firmware part found");
						isCOTSorManufactureorEnersysorFirmwarePresent = true;
					}
					if (persObject instanceof WTDocument) {
						persistableSetWTDocument.add(persObject);
					}
				}
				if (isCOTSorManufactureorEnersysorFirmwarePresent) {
					affectedObjectsPer.removeAll(persistableSetWTDocument);
				}
			}

			/* Added Build_V3.8 - Sprint 9 - 8206 */
			Iterator<Persistable> targetRefIterator = affectedObjectsPer.iterator();
			HashMap<String, WTContained> trayObjectContainerMap = new HashMap<>();
			int trayObjectsCount = 0;
			while (targetRefIterator.hasNext()) {
				Persistable per = targetRefIterator.next();
				String containerName = null;
				WTContainer cont = null;
				if (per instanceof WTContained) {
					containerName = ((WTContained) per).getContainerName();
					cont = ((WTContained) per).getContainer();
					NodeList participantNodes = getParticipantNodeListForObjectWizard((WTObject) per, pboTI,
							pnMaturityState, comboBoxCTHashMapValues);
					if (per instanceof WTPart && isTrayclassifiedPart((WTPart) per)) {
						trayObjectContainerMap.put(containerName, cont);
						++trayObjectsCount;
					}
					processParticipantNodes(participantNodes, roleMap, containerName, cont,
							(prefValue && EnerSysHelper.service.isCriticalPart(per)), pboTI, RECORDED_CHANGE_TRACK);
				}
			}
			traySpecificChanges((trayObjectsCount >= 1), trayObjectContainerMap, pboTI, roleMap);
			// Build v2.9 - 661
			if (prefValue && CM2Helper.service.getChangeTypeString(pboTI)
					.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE))
				processResultingObjectsForCriticalPart(
						StandardCM3Service.processAndFetchChangeNoticeResultingObjects(commandBean), roleMap);

			// Build v2.9 - 712
			if (CM2Helper.service.getChangeTypeString(pboTI).equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)
					|| CM2Helper.service.getChangeTypeString(pboTI)
							.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEREQUEST)
					|| CM2Helper.service.getChangeTypeString(pboTI)
							.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {
				processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObj), roleMap); // for aff obj
				if (CM2Helper.service.getChangeTypeString(pboTI)
						.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {
					Set<Persistable> resObjs = StandardCM3Service
							.processAndFetchChangeNoticeResultingObjects(commandBean);
					processAffObjForQualityCriticalPart(resObjs, roleMap); // for res obj
				}
			}
		}
		checkAndWriteDebug(Debuggable.END, "#getChangeApprovalRoleMapWizard");
		return roleMap;
	}

	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMapCN(Set<NmOid> affectedObj,
			TypeIdentifier pboTI, WTObject obj) throws WTException {
		// here affectedObj are resulting Object
		checkAndWriteDebug(Debuggable.START, "#getChangeApprovalRoleMapWizard -->", " pboTI: ", pboTI);
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap = new LinkedHashMap<>();
		newRoleMap.clear();
		releasedTargetStateRoles.clear();
		String RECORDED_CHANGE_TRACK = getChangeTrackInternalValue(pboTI, obj);
		// JIRA: 518, To read preference - Make "System Test Engineer (Verification and
		// Validation)" role as required if affected objects table contains a Critical
		// Part
		boolean prefValue = false;
		try {
			prefValue = (boolean) PreferenceHelper.service.getValue(((WTContained) obj).getContainerReference(),
					SYSTEM_TEST_MANDATORY_ROLE_CRITICAL_PART_PREFERENCE, PreferenceClient.WINDCHILL_CLIENT_NAME,
					(WTUser) SessionHelper.manager.getPrincipal());
		} catch (WTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Build V3.7 , To read preference - Need to create preference which will be
		// enabling and disabling of the change in order of the task assigned based on
		// approval matrix
		// if critical part selector role is added
		if (checkIfObjectIsFastTrack((Persistable) obj)) {
			// Added on Build v2.11 -- to get FAST-TRACK specific roles
			// roleMap = getFastTrackCNCTParticipants((WTObject)
			// getContainerFromCommandBean(commandBean), pboTI, prefValue ? affectedObj :
			// null);
			roleMap = getFastTrackCNCTParticipants(obj, pboTI, prefValue ? affectedObj : null);// check for CN Are we
																								// pasing resulting or
																								// Affected???to be
																								// check
			processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObj), roleMap); // Build v2.9 - 712
			return roleMap;
		} /*
			 * New Matrix For Firmware Part/Document - Added in Build 3.8 Change Management
			 * of Firmware changed - Separate approval matrix for CR,CN,CT in Build 3.8 else
			 * if (isFirmwareCRCNCTpbo(pboTI)) { // Added on Build v1.13 -- to get FIRMWARE
			 * CR/CN/CT specific roles roleMap = getFirmwareCRBParticipants((WTObject)
			 * getContainerFromCommandBean(commandBean), pboTI, prefValue ? affectedObj :
			 * null);
			 * processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObj),
			 * roleMap);// Build v2.9 - 712 return roleMap;
			 * 
			 * }
			 */ else {
			String pnMaturityState = null;
			/* Added Build_V3.8 - Sprint 9 - 8206 */
			Set<Persistable> affectedObjectsPer = convertNmOidSetToPerSet(affectedObj);
			
			// Added for Build_v3.19
			// #ADO: 14839
			boolean isCOTSorManufactureorEnersysorFirmwarePresent = false;
			Set<Persistable> persistableSetWTDocument = new HashSet<>();
			String pboTypeString = CM2Helper.service.getChangeTypeString(pboTI);
			if (pboTypeString.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)
					|| pboTypeString.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEREQUEST)
					|| pboTypeString.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)
					|| pboTypeString.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEACTIVITY)) {
				for (Persistable persObject : affectedObjectsPer) {

					// Added for Build_v3.19
					// #ADO: US 14839
					// Added third and fourth check for Enersys and Firmware Part
					TypeIdentifier affecetedObjectTypeId = TypeIdentifierHelper.getType(persObject);
					if (affecetedObjectTypeId.equals(TypeIdentifierHelper.getTypeIdentifier(COTS_PART_INT_NAME))
							|| affecetedObjectTypeId
									.equals(TypeIdentifierHelper.getTypeIdentifier(MANUFACTURE_PART_INT_NAME))
							|| affecetedObjectTypeId
									.equals(TypeIdentifierHelper.getTypeIdentifier(ENERSYS_PART_INT_NAME))
							|| affecetedObjectTypeId
									.equals(TypeIdentifierHelper.getTypeIdentifier(FIRMWARE_PART_INT_NAME))) {
						LOGGER.debug("######COTS or MANUFACTURE or Enersys or Firmware part found");
						isCOTSorManufactureorEnersysorFirmwarePresent = true;
					}
					if (persObject instanceof WTDocument) {
						persistableSetWTDocument.add(persObject);
					}
				}
				if (isCOTSorManufactureorEnersysorFirmwarePresent) {
					affectedObjectsPer.removeAll(persistableSetWTDocument);
				}
			}
			
			/* Added Build_V3.8 - Sprint 9 - 8206 */
			Iterator<Persistable> targetRefIterator = affectedObjectsPer.iterator();
			HashMap<String, WTContained> trayObjectContainerMap = new HashMap<>();
			int trayObjectsCount = 0;
			while (targetRefIterator.hasNext()) {
				Persistable per = targetRefIterator.next();
				String containerName = null;
				WTContainer cont = null;
				if (per instanceof WTContained) {
					containerName = ((WTContained) per).getContainerName();
					cont = ((WTContained) per).getContainer();
					NodeList participantNodes = getParticipantNodeListForObject((WTObject) per, obj);
					if (per instanceof WTPart && isTrayclassifiedPart((WTPart) per)) {
						trayObjectContainerMap.put(containerName, cont);
						++trayObjectsCount;
					}
					processParticipantNodes(participantNodes, roleMap, containerName, cont,
							(prefValue && EnerSysHelper.service.isCriticalPart(per)), pboTI, RECORDED_CHANGE_TRACK);
				}
			}
			traySpecificChanges((trayObjectsCount >= 1), trayObjectContainerMap, pboTI, roleMap);
			// Build v2.9 - 661
			if (prefValue && CM2Helper.service.getChangeTypeString(pboTI)
					.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE))
				processResultingObjectsForCriticalPart(affectedObjectsPer, roleMap);

			// Build v2.9 - 712
			if (CM2Helper.service.getChangeTypeString(pboTI).equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)
					|| CM2Helper.service.getChangeTypeString(pboTI)
							.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEREQUEST)
					|| CM2Helper.service.getChangeTypeString(pboTI)
							.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {
				processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObj), roleMap); // for aff obj
				if (CM2Helper.service.getChangeTypeString(pboTI)
						.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {
					processAffObjForQualityCriticalPart(affectedObjectsPer, roleMap); // for res obj
				}
			}
		}
		checkAndWriteDebug(Debuggable.END, "#getChangeApprovalRoleMapWizard");
		return roleMap;
	}

	private LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getFastTrackCNCTParticipants(Object obj,
			TypeIdentifier pboTI, Set<NmOid> affectedObj) {
		// TODO Auto-generated method stub
		checkAndWriteDebug(Debuggable.START, "#getFastTrackCTParticipants");
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> object = calculateParticipantsForNewFastTrackCT(pboTI,
				(WTContained) obj, changeActivityFastTrackElements, affectedObj);
		checkAndWriteDebug(Debuggable.END, "#getFastTrackCTParticipants");
		return object;
	}

	/**
	 * 
	 * Added in Build v2.9 - 712 Processes role map to check if "QUALITY MANAGER"
	 * exists. If found returns key otherwise returns an empty string
	 * 
	 * @param roleMap
	 * @return String
	 */
	private String processRoleMapForQualityManagerRole(
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap) {
		for (Map.Entry<String, LinkedHashMap<String, WTContainer>> entry : roleMap.entrySet()) {
			String oldKey = entry.getKey();
			if (oldKey.startsWith(QUALITY_MANAGER_ROLE)) {
				return oldKey;
			}
		}
		return "";

	}

	/**
	 * 
	 * Added in Build v2.9 - 712 Makes "QUALITY MANAGER" mandatory if aff objs list
	 * includes a quality critical part
	 * 
	 * @param roleMap
	 */
	private void processAffObjForQualityCriticalPart(Set<Persistable> affObjects,
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap) {
		checkAndWriteDebug(Debuggable.START, "#processAffObjForQualityCriticalPart -->", " roleMap: ", roleMap);
		Iterator affObjItr = affObjects.iterator();
		while (affObjItr.hasNext()) {
			Persistable per = (Persistable) affObjItr.next();
			if (EnerSysHelper.service.isQualityCriticalPart(per)) {
				checkAndWriteDebug(Debuggable.LINE, "#processAffObjForQualityCriticalPart -->",
						" quality critical object present", per);
				String oldKey = processRoleMapForQualityManagerRole(roleMap);
				if (oldKey.isEmpty() && (per instanceof WTContained)) {
					String containerName = ((WTContained) per).getContainerName();
					WTContainer cont = ((WTContained) per).getContainer();
					final String QUALITY_CRITICAL_PART_KEY = QUALITY_MANAGER_ROLE + COLON_ROLE_MAP_DELIM + "REQUIRED"
							+ COLON_ROLE_MAP_DELIM + "1" + COLON_ROLE_MAP_DELIM + false;
					LinkedHashMap<String, WTContainer> keyValueContainerMap = new LinkedHashMap<>();
					if (roleMap.get(QUALITY_CRITICAL_PART_KEY) == null) {
						roleMap.put(QUALITY_CRITICAL_PART_KEY, keyValueContainerMap);
						final String ROLE_TYPE = QUALITY_MANAGER_ROLE + COLON_ROLE_MAP_DELIM + "ROLE"
								+ COLON_ROLE_MAP_DELIM + false + COLON_ROLE_MAP_DELIM + containerName;
						keyValueContainerMap.computeIfAbsent(ROLE_TYPE, k -> cont);
					}
				} else if (roleMap.containsKey(oldKey)) {
					String[] oldKeyArr = oldKey.split(":");
					if (oldKeyArr[1] != null && oldKeyArr[1].equals("OPTIONAL")) {
						oldKeyArr[1] = "REQUIRED";
						String newKey = String.join(":", oldKeyArr);
						roleMap.put(newKey, roleMap.remove(oldKey));
					}
				}
				break;
			}
		}
		checkAndWriteDebug(Debuggable.END, "#processAffObjForQualityCriticalPart -->", " roleMap", roleMap);
	}

	/**
	 * 
	 * Added in Build v2.9 - 661 Processes role map to check if
	 * "SYSTEM_TEST_ENGINEER_ROLE" exists. If found returns key otherwise returns an
	 * empty string
	 * 
	 * @param roleMap
	 * @return String
	 */
	private String processRoleMapForSystemTestRole(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap) {
		for (Map.Entry<String, LinkedHashMap<String, WTContainer>> entry : roleMap.entrySet()) {
			String oldKey = entry.getKey();
			if (oldKey.startsWith(SYSTEM_TEST_ENGINEER_ROLE)) {
				return oldKey;
			}
		}
		return "";

	}

	/**
	 * 
	 * Added in Build v2.9 - 661 Makes "SYSTEM_TEST_ENGINEER_ROLE" mandatory in the
	 * CN's setup participant step if any critical part found in the resulting
	 * objects
	 * 
	 * @param roleMap
	 */
	private void processResultingObjectsForCriticalPart(Set<Persistable> resultingObjects,
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap) {
		checkAndWriteDebug(Debuggable.START, "#processResultingObjectsForCriticalPart -->", " roleMap: ", roleMap);
		Iterator resObjItr = resultingObjects.iterator();
		while (resObjItr.hasNext()) {
			Persistable per = (Persistable) resObjItr.next();
			if (EnerSysHelper.service.isCriticalPart(per)) {
				checkAndWriteDebug(Debuggable.LINE, "#processResultingObjectsForCriticalPart -->",
						" critical object present in resulting objects table", per);
				String oldKey = processRoleMapForSystemTestRole(roleMap);
				if (oldKey.isEmpty() && (per instanceof WTContained)) {
					String containerName = ((WTContained) per).getContainerName();
					WTContainer cont = ((WTContained) per).getContainer();
					final String CRITICAL_PART_KEY = SYSTEM_TEST_ENGINEER_ROLE + COLON_ROLE_MAP_DELIM + "REQUIRED"
							+ COLON_ROLE_MAP_DELIM + "1" + COLON_ROLE_MAP_DELIM + false;
					LinkedHashMap<String, WTContainer> keyValueContainerMap = new LinkedHashMap<>();
					if (roleMap.get(CRITICAL_PART_KEY) == null) {
						roleMap.put(CRITICAL_PART_KEY, keyValueContainerMap);
						final String ROLE_TYPE = SYSTEM_TEST_ENGINEER_ROLE + COLON_ROLE_MAP_DELIM + "ROLE"
								+ COLON_ROLE_MAP_DELIM + false + COLON_ROLE_MAP_DELIM + containerName;
						keyValueContainerMap.computeIfAbsent(ROLE_TYPE, k -> cont);
					}
				} else if (roleMap.containsKey(oldKey)) {
					String[] oldKeyArr = oldKey.split(":");
					if (oldKeyArr[1] != null && oldKeyArr[1].equals("OPTIONAL")) {
						oldKeyArr[1] = "REQUIRED";
						String newKey = String.join(":", oldKeyArr);
						roleMap.put(newKey, roleMap.remove(oldKey));
					}
				}
				break;
			}
		}
		checkAndWriteDebug(Debuggable.END, "#processResultingObjectsForCriticalPart -->", " roleMap", roleMap);
	}

	/**
	 * @deprecated No longer needed from Build 3.3
	 *
	 *             Method adding Tray specific roles on role map.
	 * 
	 * @since Build v2.8
	 */
	private void traySpecificChanges(boolean foundATray, Map<String, WTContained> trayObjectContainerSet,
			TypeIdentifier pboTI, LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap) {
		// added for tray
		if (foundATray) {
			try {
				// Added Program Manager and Purchasing Manager as Optional role
				// If PBO_TYPE is PROMOTIONREQUEST or CHANGEREQUEST or CHANGENOTICE
				if (CM2Helper.service.getChangeTypeString(pboTI)
						.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)
						|| CM2Helper.service.getChangeTypeString(pboTI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEREQUEST)
						|| CM2Helper.service.getChangeTypeString(pboTI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {

					String regExprToCheck_PM = "PROGRAM MANAGER" + COLON_ROLE_MAP_DELIM + "(?i)(REQUIRED)"
							+ COLON_ROLE_MAP_DELIM + "(\\d+)" + COLON_ROLE_MAP_DELIM + "(?i)(true|false)";
					String regExprToCheck_PURM = "PURCHASING MANAGER" + COLON_ROLE_MAP_DELIM + "(?i)(REQUIRED)"
							+ COLON_ROLE_MAP_DELIM + "(\\d+)" + COLON_ROLE_MAP_DELIM + "(?i)(true|false)";
					Pattern ptrn_PM = Pattern.compile(regExprToCheck_PM);
					Pattern ptrn_PURM = Pattern.compile(regExprToCheck_PURM);

					LinkedHashMap<String, Object[]> keysToReplace = new LinkedHashMap<String, Object[]>();

					// 1. Check if PROGRAM MANAGER/PURCHASING MANAGER is included in RoleMap
					for (Entry<String, LinkedHashMap<String, WTContainer>> o : roleMap.entrySet()) {
						Matcher m_PM = ptrn_PM.matcher(o.getKey());
						Matcher m_PURM = ptrn_PURM.matcher(o.getKey());
						if (m_PM.matches()) {
							String newKey = "PROGRAM MANAGER" + COLON_ROLE_MAP_DELIM + "OPTIONAL" + COLON_ROLE_MAP_DELIM
									+ Integer.parseInt(m_PM.group(2)) + COLON_ROLE_MAP_DELIM
									+ Boolean.parseBoolean(m_PM.group(3));
							keysToReplace.put(o.getKey(), new Object[] { newKey, o.getValue() });
						} else if (m_PURM.matches()) {
							String newKey = "PURCHASING MANAGER" + COLON_ROLE_MAP_DELIM + "OPTIONAL"
									+ COLON_ROLE_MAP_DELIM + Integer.parseInt(m_PURM.group(2)) + COLON_ROLE_MAP_DELIM
									+ Boolean.parseBoolean(m_PURM.group(3));
							keysToReplace.put(o.getKey(), new Object[] { newKey, o.getValue() });
						}
					}

					// 2. Remove existing keys from RoleMap
					for (String keysToDelete : keysToReplace.keySet()) {
						roleMap.remove(keysToDelete);
					}

					// 3. Re-add Values with New Keys
					for (Entry<String, Object[]> e : keysToReplace.entrySet()) {
						roleMap.put((String) e.getValue()[0], (LinkedHashMap<String, WTContainer>) e.getValue()[1]);
					}
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
			// Addded Tray auditor as required role(sequence no->10)
			// If PBO_TYPE is PROMOTIONREQUEST or CHANGENOTICE
			try {
				if (CM2Helper.service.getChangeTypeString(pboTI)
						.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)
						|| CM2Helper.service.getChangeTypeString(pboTI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {

					for (Entry<String, WTContained> e : trayObjectContainerSet.entrySet()) {
						final WTContainer cont = (WTContainer) e.getValue();
						String containerName = e.getKey();

						String KEY_TRAYAUDITOR = "TRAY_AUDITOR" + COLON_ROLE_MAP_DELIM + "REQUIRED"
								+ COLON_ROLE_MAP_DELIM + "10" + COLON_ROLE_MAP_DELIM + false;
						LinkedHashMap<String, WTContainer> keyValueContainerMap2 = null;

						if (roleMap.get(KEY_TRAYAUDITOR) != null) {
							keyValueContainerMap2 = roleMap.get(KEY_TRAYAUDITOR);
						} else {
							keyValueContainerMap2 = new LinkedHashMap<>();
							roleMap.put(KEY_TRAYAUDITOR, keyValueContainerMap2);
						}

						final String ROLE_TYPE2 = "TRAY_AUDITOR" + COLON_ROLE_MAP_DELIM + "ROLE" + COLON_ROLE_MAP_DELIM
								+ false + COLON_ROLE_MAP_DELIM + containerName;
						keyValueContainerMap2.computeIfAbsent(ROLE_TYPE2, k -> cont);
					}
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @deprecated No longer needed from Build 3.3
	 * 
	 *             This method reads the preference and checks for the classified
	 *             part
	 * 
	 * @param part
	 * @return boolean
	 * @throws WTException
	 */
	public static boolean isTrayclassifiedPart(WTPart part) throws WTException {
		boolean isouterTray = false;
		Set<String> trayenabledContexts = null;
		String bind_attr_value = null;
		if (trayenabledContexts == null) {
			trayenabledContexts = new HashSet<>();
		}
		trayenabledContexts.clear();
		WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
		OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);
		String TrayPrefValue = (String) PreferenceHelper.service.getValue(TRAY_PREF_INTR_NAME,
				PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);

		if (TrayPrefValue != null && !TrayPrefValue.isEmpty()) {
			for (String str : TrayPrefValue.split(PREFERENCE_SEPARATOR)) {
				trayenabledContexts.add(str.trim());
			}
		}
		final String main_bind_attr_name = "ENERSYS_CLASSIFICATION_BINDING_ATTR";
		if (main_bind_attr_name != null) {
			PersistableAdapter obj = new PersistableAdapter(part, null, Locale.US, null);
			obj.load(main_bind_attr_name);
			bind_attr_value = (String) obj.getAsString(main_bind_attr_name);
			if (bind_attr_value != null) {
				LOGGER.debug("The classification node internal name value is " + bind_attr_value);
				if (trayenabledContexts.contains(bind_attr_value)) {
					isouterTray = true;
				}
			}
		}
		return isouterTray;
	}

	/**
	 * 
	 * Method which processes ParticipantNodes as per given Container & options.
	 * 
	 * @param participantElements
	 * @param roleMap
	 * @param containerName
	 * @param cont
	 * @since Build v2.1
	 */
	private void processParticipantNodes(HashSet<Element> participantElements,
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap, String containerName, WTContainer cont,
			boolean isCriticalPart, TypeIdentifier pboTI, String RECORDED_CHANGE_TRACK) {
		if (participantElements != null) {
			int i = 0;

			for (Element participantNode : participantElements) {
				Element participantElement = participantNode;

				// EXTRACT ALL INFORMATION FROM THE PARTICIPANT XML TAG
				final String PARTICIPANTS_LINE = participantElement
						.getAttribute(EnerSysApprovalMatrixDefinition.PARTICIPANT_PARTICIPANTS_ATTR);
				final String DESTINATION_ROLE = participantElement
						.getAttribute(EnerSysApprovalMatrixDefinition.PARTICIPANT_DEST_ROLE_ATTR);
				final String PARTICIPANT_TYPE = participantElement
						.getAttribute(EnerSysApprovalMatrixDefinition.PARTICIPANT_TYPE_ATTR);
				String IS_REQUIRED = participantElement
						.getAttribute(EnerSysApprovalMatrixDefinition.PARTICIPANT_APPROVAL_ATTR);
				String TYPE_SELECTION = participantElement.getAttribute(EnerSysApprovalMatrixDefinition.TYPE_SELECTION);

				if (TYPE_SELECTION == null || TYPE_SELECTION == "") {
					TYPE_SELECTION = "single";
				}

				// ES Customization, enable multi select for ES
				try {
					WTContainerRef containerReference = WTContainerRef.newWTContainerRef(cont);
					boolean featureFlag = ESBusinessHelper.getFrameWorkPreferenceValue(preferenceValue,
							containerReference);
					if (featureFlag)
						TYPE_SELECTION = "multiple";
				} catch (WTException e) {
					e.printStackTrace();
				}

				// Add "is Context bypass" - True or false value to the role map
				boolean IS_FETCH_ALL_PARTICIPANTS = false;
				if (participantElement.hasAttribute(EnerSysApprovalMatrixDefinition.FETCH_ALL_PARTICIPANTS_ATTR)) {
					IS_FETCH_ALL_PARTICIPANTS = Boolean.valueOf(participantElement
							.getAttribute(EnerSysApprovalMatrixDefinition.FETCH_ALL_PARTICIPANTS_ATTR));
				}

				// 2 attributes added for Build v2.2
				// Build v2.2 -- strict-auto-populate-from-context-team-for
				String STRICT_AUTO_POP = participantElement
						.getAttribute(EnerSysApprovalMatrixDefinition.STRICT_AUTO_POPULATE_FROM_CONTEXT_TEAM_ATTR);
				STRICT_AUTO_POP = (STRICT_AUTO_POP == null || STRICT_AUTO_POP.isEmpty()) ? "" : STRICT_AUTO_POP;
				final boolean BOOL_STRICT_AUTO_POP = checkIfContainerNameFound(STRICT_AUTO_POP, cont.getName());

				// Build v3.1 -- make-mandatory-for-change-track
				String MAKE_MANDATORY_FOR_CHANGE_TRACK = participantElement
						.getAttribute(EnerSysApprovalMatrixDefinition.MAKE_MANDATORY_FOR_CHANGE_TRACK);
				MAKE_MANDATORY_FOR_CHANGE_TRACK = (MAKE_MANDATORY_FOR_CHANGE_TRACK == null
						|| MAKE_MANDATORY_FOR_CHANGE_TRACK.isEmpty()) ? "" : MAKE_MANDATORY_FOR_CHANGE_TRACK;
				final boolean BOOL_MAKE_MANDATORY_FOR_CHANGE_TRACK = checkIfMakeMandatoryForClass(
						MAKE_MANDATORY_FOR_CHANGE_TRACK, RECORDED_CHANGE_TRACK);
				if (BOOL_MAKE_MANDATORY_FOR_CHANGE_TRACK) {
					IS_REQUIRED = "REQUIRED";
				}

				// Build v2.2 -- make-mandatory-for
				String MAKE_MANDATORY_FOR = participantElement
						.getAttribute(EnerSysApprovalMatrixDefinition.MAKE_MANDATORY_FOR_ATTR);
				MAKE_MANDATORY_FOR = (MAKE_MANDATORY_FOR == null || MAKE_MANDATORY_FOR.isEmpty()) ? ""
						: MAKE_MANDATORY_FOR;
				final boolean BOOL_MAKE_MANDATORY_FOR = checkIfContainerNameFound(MAKE_MANDATORY_FOR, cont.getName());
				if (BOOL_MAKE_MANDATORY_FOR) {
					IS_REQUIRED = "REQUIRED";
				}

				// 1 Attribute added for Build v2.3 -- make-optional-for
				String MAKE_OPTIONAL_FOR = participantElement
						.getAttribute(EnerSysApprovalMatrixDefinition.MAKE_OPTIONAL_FOR_ATTR);
				MAKE_OPTIONAL_FOR = (MAKE_OPTIONAL_FOR == null || MAKE_OPTIONAL_FOR.isEmpty()) ? "" : MAKE_OPTIONAL_FOR;
				final boolean BOOL_MAKE_OPTIONAL_FOR = checkIfContainerNameFound(MAKE_OPTIONAL_FOR, cont.getName());
				if (BOOL_MAKE_OPTIONAL_FOR) {
					IS_REQUIRED = "OPTIONAL";
				}

				// EXTRACT NEW Order ATTRIBUTE ON THE PARTICIPANT NODE
				String SEQUENCE_ORDER = participantElement
						.getAttribute(EnerSysApprovalMatrixDefinition.PARTICIPANT_ORDER_ATTR);

				// BY DEFAULT, ASSIGN ORDER 1 to Roles which are missing Order
				SEQUENCE_ORDER = ((SEQUENCE_ORDER == null) || SEQUENCE_ORDER.trim().isEmpty())
						? Integer.toString(DEFAULT_SEQUENCE_NUMBER)
						: SEQUENCE_ORDER;

				String KEY_TO_CHECK = DESTINATION_ROLE + COLON_ROLE_MAP_DELIM + IS_REQUIRED + COLON_ROLE_MAP_DELIM
						+ SEQUENCE_ORDER + COLON_ROLE_MAP_DELIM + IS_FETCH_ALL_PARTICIPANTS;

				// v2.2 -- Consolidation logic below
				// CHECK IF DESTINATION ROLE is present in the ROLE-MAP
				if (isRoleConsolidationRequired(roleMap, DESTINATION_ROLE, KEY_TO_CHECK)) {
					// PERFORM CONSOLIDATION by extracting information using RegExp from existing
					// keys
					// Rules for Consolidation:
					// 1. Prefer the highest Sequence Order
					// 2. Prefer REQUIRED over OPTIONAL
					// 3. Prefer TRUE over FALSE

					String regExprToCheck = DESTINATION_ROLE + COLON_ROLE_MAP_DELIM + "(?i)(REQUIRED|OPTIONAL)"
							+ COLON_ROLE_MAP_DELIM + "(\\d+)" + COLON_ROLE_MAP_DELIM + "(?i)(true|false)";
					Pattern ptrn = Pattern.compile(regExprToCheck);

					List<Integer> seqList = new ArrayList<>();
					Set<String> resultingIsRequiredSet = new HashSet<>();
					Set<String> keysToReplace = new HashSet<>();
					LinkedHashMap<String, WTContainer> resultingRoleTypeValues = new LinkedHashMap<>();

					boolean isResultingFetchAllParticipant = false;
					// Extract values for current object -> KEY_TO_CHECK
					seqList.add(Integer.parseInt(SEQUENCE_ORDER));
					isResultingFetchAllParticipant |= IS_FETCH_ALL_PARTICIPANTS;
					resultingIsRequiredSet.add(IS_REQUIRED);

					// Apply Pattern on All-Keys
					for (Entry<String, LinkedHashMap<String, WTContainer>> o : roleMap.entrySet()) {
						Matcher m = ptrn.matcher(o.getKey());
						if (m.matches()) {
							keysToReplace.add(o.getKey());// record which key to replace
							// record merging scenario will not happen since WTContainer names are stored in
							// KEY; so clashes will be minimal, if not, non-existant
							resultingRoleTypeValues.putAll(o.getValue());// record which key to save

							resultingIsRequiredSet.add(m.group(1));// Required or Optional
							seqList.add(Integer.parseInt(m.group(2)));// Sequence Order
							isResultingFetchAllParticipant |= Boolean.parseBoolean(m.group(3));// Is Fetch All
																								// Participants
						}
					}
					// Sort List and take first element
					Collections.sort(seqList, Collections.reverseOrder());
					final int resultingSeqOrder = seqList.get(0);
					final String resultingIsRequired = (resultingIsRequiredSet.contains("REQUIRED")) ? "REQUIRED"
							: "OPTIONAL";

					// Remove existing keys from RoleMap
					for (String keysToDelete : keysToReplace) {
						roleMap.remove(keysToDelete);
					}
					final String replacementKEY = DESTINATION_ROLE + COLON_ROLE_MAP_DELIM + resultingIsRequired
							+ COLON_ROLE_MAP_DELIM + resultingSeqOrder + COLON_ROLE_MAP_DELIM
							+ isResultingFetchAllParticipant;

					roleMap.put(replacementKEY, resultingRoleTypeValues);
					// Change the Existing KEY
					KEY_TO_CHECK = replacementKEY;
				}

				LinkedHashMap<String, WTContainer> keyContainerMap;
				// SPLIT PARTICIPANTS BY ";" if THERE ARE >1 PARTICIPANTS
				String[] participants = PARTICIPANTS_LINE.split(SEMICOLON_DELIM);

				if (roleMap.get(KEY_TO_CHECK) != null) {
					keyContainerMap = roleMap.get(KEY_TO_CHECK);
				} else {
					keyContainerMap = new LinkedHashMap<>();
					roleMap.put(KEY_TO_CHECK, keyContainerMap);
				}
				for (int jj = 0; jj < participants.length; ++jj) {
					String participant = participants[jj].trim();
					if (!participant.isEmpty()) {
						String ROLE_TYPE = participant + COLON_ROLE_MAP_DELIM + PARTICIPANT_TYPE + COLON_ROLE_MAP_DELIM
								+ BOOL_STRICT_AUTO_POP + COLON_ROLE_MAP_DELIM + containerName + COLON_ROLE_MAP_DELIM
								+ TYPE_SELECTION;
						keyContainerMap.computeIfAbsent(ROLE_TYPE, k -> cont);
						/*
						 * if (keyContainerMap. get(roleType) == null) { keyContainerMap.put(roleType,
						 * cont); }
						 */
					}
				}
				checkAndWriteDebug(Debuggable.LINE, "#processParticipantNodes -->", " keyContainerMap: ",
						keyContainerMap);
			}

			// JIRA: 518
			if (isCriticalPart) {
				try {
					if (CM2Helper.service.getChangeTypeString(pboTI)
							.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)
							|| CM2Helper.service.getChangeTypeString(pboTI)
									.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEREQUEST)
							|| CM2Helper.service.getChangeTypeString(pboTI)
									.equalsIgnoreCase(EnerSysService.ACTIVITY_DEVIATION)
							|| CM2Helper.service.getChangeTypeString(pboTI)
									.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {
						final String CRITICAL_PART_KEY = SYSTEM_TEST_ENGINEER_ROLE + COLON_ROLE_MAP_DELIM + "REQUIRED"
								+ COLON_ROLE_MAP_DELIM + "1" + COLON_ROLE_MAP_DELIM + false;

						LinkedHashMap<String, WTContainer> keyValueContainerMap = new LinkedHashMap<>();
						String oldKey = processRoleMapForSystemTestRole(roleMap);
						if (oldKey.isEmpty() && roleMap.get(CRITICAL_PART_KEY) == null) {
							roleMap.put(CRITICAL_PART_KEY, keyValueContainerMap);
							final String ROLE_TYPE = SYSTEM_TEST_ENGINEER_ROLE + COLON_ROLE_MAP_DELIM + "ROLE"
									+ COLON_ROLE_MAP_DELIM + false + COLON_ROLE_MAP_DELIM + containerName;
							keyValueContainerMap.computeIfAbsent(ROLE_TYPE, k -> cont);
						} else if (roleMap.containsKey(oldKey)) {
							String[] oldKeyArr = oldKey.split(":");
							if (oldKeyArr[1] != null && oldKeyArr[1].equals("OPTIONAL")) {
								oldKeyArr[1] = "REQUIRED";
								String newKey = String.join(":", oldKeyArr);
								roleMap.put(newKey, roleMap.remove(oldKey));
							}
						}
					}
				} catch (WTException e) {
					e.printStackTrace();
				}
			}
			checkAndWriteDebug(Debuggable.LINE, "#processParticipantNodes -->", " roleMap: ", roleMap);
		}
	}

	/**
	 * Spits the Tag information based on delimiter (;) and checks if the
	 * RECORDED_CHANGE_TRACK string is present in it.
	 * 
	 * @param classInfoFromTag
	 * @param RECORDED_CHANGE_TRACK
	 * @return True - if RECORDED_CHANGE_TRACK is present in the tag information.
	 * @since Build v3.1
	 */
	private boolean checkIfMakeMandatoryForClass(String classInfoFromTag, String RECORDED_CHANGE_TRACK) {
		if (classInfoFromTag == null || classInfoFromTag.isEmpty() || RECORDED_CHANGE_TRACK == null
				|| RECORDED_CHANGE_TRACK.isEmpty()) {
			return false;
		}
		RECORDED_CHANGE_TRACK = RECORDED_CHANGE_TRACK.replaceAll("\\]|\\[", "");
		if (Arrays.asList(classInfoFromTag.split(EnerSysApprovalMatrixDefinition.SEMICOLON_DELIM))
				.contains(RECORDED_CHANGE_TRACK)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if the given FINAL key is present in the roleMap, if so then returns
	 * FALSE (NO CONSOLIDATION REQUIRED).<br>
	 * Then checks if any key in the RoleMap starts with the DESTINATION ROLE, if so
	 * then returns TRUE.<br>
	 * Returns FALSE otherwise.
	 * 
	 * @param roleMap
	 * @param destRole
	 * @param keyToChk
	 * @return
	 */
	private boolean isRoleConsolidationRequired(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap,
			String destRole, String keyToChk) {
		// if (roleMap.containsKey(keyToChk)) {
		// return false;
		// }
		// process all keys and check the ROLE is available
		for (String key : roleMap.keySet()) {
			if (key.startsWith(destRole + COLON_ROLE_MAP_DELIM)) {
				return true;
			}
		}
		if (roleMap.containsKey(keyToChk)) {
			return false;
		}
		return false;
	}

	/**
	 * 
	 * @param strToSplitAndCheck
	 * @param contName
	 * @return
	 * @since Build v2.2
	 */
	private boolean checkIfContainerNameFound(String strToSplitAndCheck, String contName) {
		if (strToSplitAndCheck != null && !strToSplitAndCheck.isEmpty()) {
			for (String containerNameToCheck : strToSplitAndCheck.split(SEMICOLON_DELIM)) {
				if (contName.equalsIgnoreCase(containerNameToCheck)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Overridden method which processes ParticipantNodes as per given Container &
	 * options.
	 * 
	 * @param participantNodes
	 * @param roleMap
	 * @param containerName
	 * @param cont
	 * @since Build v2.1
	 */
	private void processParticipantNodes(NodeList participantNodes,
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap, String containerName, WTContainer cont,
			boolean isCriticalPart, TypeIdentifier pboTI, String RECORDED_CHANGE_TRACK) {
		if (participantNodes != null) {
			HashSet<Element> participantElements = new HashSet<>();
			for (int i = 0; i < participantNodes.getLength(); ++i) {
				participantElements.add((Element) participantNodes.item(i));
			}
			processParticipantNodes(participantElements, roleMap, containerName, cont, isCriticalPart, pboTI,
					RECORDED_CHANGE_TRACK);
		}
	}

	/**
	 * @deprecated from build V3.8 CM 4.0
	 */
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getChangeApprovalRoleMap(WTObject pbo)
			throws WTException {
		checkAndWriteDebug(Debuggable.START, "#getChangeApprovalRoleMap -->", " pbo: ", pbo);
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap = new LinkedHashMap<>();
		// Build v3.2 - PLM - 489 - Start
		try (TempSessionContext tempSessionContext = new TempSessionContext()) {
			SessionServerHelper.manager.setAccessEnforced(false);
			WTPrincipal adminPrincipal = SessionHelper.manager.getAdministrator();
			SessionContext.setEffectivePrincipal(adminPrincipal);
			SessionHelper.manager.setAdministrator();
			// Build v3.2 - PLM - 489 - End
			TypeIdentifier pbo_TI = TypeIdentifierHelper.getType(pbo);
			// JIRA: 518
			QueryResult affectedObjectsQR = CM2Helper.service.getAffectedObjects(pbo);
			Set<NmOid> affectedObjects = new HashSet<>();
			while (affectedObjectsQR.hasMoreElements()) {
				affectedObjects.add(new NmOid((Persistable) affectedObjectsQR.nextElement()));
			}
			boolean prefValue = (boolean) PreferenceHelper.service.getValue(((WTContained) pbo).getContainerReference(),
					SYSTEM_TEST_MANDATORY_ROLE_CRITICAL_PART_PREFERENCE, PreferenceClient.WINDCHILL_CLIENT_NAME,
					(WTUser) SessionHelper.manager.getPrincipal());

			// ADO: 14768
			String changeTrackValue = getChangeTrackInternalValue(pbo_TI, pbo);
			String RECORDED_CHANGE_TRACK = null;
			if ((changeTrackValue.replaceAll("\\]|\\[", "")).equalsIgnoreCase("MFG")) {
				RECORDED_CHANGE_TRACK = "MFG" + getChangeTypeInternalValue(pbo_TI, pbo);
			} else {
				RECORDED_CHANGE_TRACK = changeTrackValue; // Build v3.1
			}

			if (pbo != null && isFastTrackCNCTobjectCheck(pbo)) {
				// Added on Build v2.11 -- to get FAST-TRACK specific roles
				roleMap = getFastTrackCNCTParticipants(pbo, pbo_TI, prefValue ? affectedObjects : null);
				processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObjects), roleMap); // Build v2.9 -
																										// 712
				return roleMap;
			} else if (pbo != null && isFirmwareCRCNCTpbo(pbo)) {
				// Added on Build v1.13 -- to get FIRMWARE CR/CN/CT specific roles
				roleMap = getFirmwareCRBParticipants(pbo, pbo_TI, prefValue ? affectedObjects : null);
				processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObjects), roleMap); // Build v2.9 -
																										// 712
				return roleMap;
			} else if (pbo instanceof WTVariance) {
				// Added on Build v1.13 -- to get Deviation specific roles
				if (isFullTrackDeviation(pbo)) {
					roleMap = getDeviationFullTrackParticipants(pbo, pbo_TI, prefValue ? affectedObjects : null);
					processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObjects), roleMap); // Build
																											// v2.9 -
																											// 712
					return roleMap;
				} else {
					roleMap = getDeviationFastTrackParticipants(pbo, pbo_TI, prefValue ? affectedObjects : null);
					processAffObjForQualityCriticalPart(convertNmOidSetToPerSet(affectedObjects), roleMap); // Build
																											// v2.9 -
																											// 712
					return roleMap;
				}
			} else {
				QueryResult resultingObjects = CM2Helper.service.getAffectedObjects(pbo);
				int trayObjectsCount = 0;
				while (resultingObjects.hasMoreElements()) {
					WTObject obj = (WTObject) resultingObjects.nextElement();
					WTContained contained = (WTContained) obj;

					WTContainer cont = contained.getContainer();
					String containerName = contained.getContainerName();

					NodeList participantNodes = getParticipantNodeListForObject(obj, pbo);
					if (obj instanceof WTPart && isTrayclassifiedPart((WTPart) obj)) {
						++trayObjectsCount;
					}

					processParticipantNodes(participantNodes, roleMap, containerName, cont,
							(prefValue && EnerSysHelper.service.isCriticalPart(obj)), pbo_TI, RECORDED_CHANGE_TRACK);
				}

				// Build v2.9 - 661
				if (prefValue && CM2Helper.service.getChangeTypeString(pbo_TI)
						.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {
					Set<Persistable> finalResultingObjects = new HashSet<>();
					WTHashSet resObjects = CM2Helper.service.getAllResultingObjects(pbo);
					Iterator resObjItr = resObjects.iterator();
					while (resObjItr.hasNext()) {
						Persistable per = ((WTReference) resObjItr.next()).getObject();
						finalResultingObjects.add(per);
					}
					processResultingObjectsForCriticalPart(finalResultingObjects, roleMap);
				}

				// Build v2.9 - 712
				if (CM2Helper.service.getChangeTypeString(pbo_TI)
						.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)
						|| CM2Helper.service.getChangeTypeString(pbo_TI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEREQUEST)
						|| CM2Helper.service.getChangeTypeString(pbo_TI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {

					Set<Persistable> finalAffAndResultingObjects = new HashSet<>();
					WTHashSet affAndResultingObjects = CM2Helper.service.getAllAffectedObjects(pbo);
					Iterator affAndResObjItr = affAndResultingObjects.iterator();
					while (affAndResObjItr.hasNext()) {
						Persistable per = ((WTReference) affAndResObjItr.next()).getObject();
						finalAffAndResultingObjects.add(per);
					}
					processAffObjForQualityCriticalPart(finalAffAndResultingObjects, roleMap);
				}
			}
			checkAndWriteDebug(Debuggable.END, "#getChangeApprovalRoleMap");

			boolean orderChngCriticalPartPrefValue = (boolean) PreferenceHelper.service.getValue(
					((WTContained) pbo).getContainerReference(),
					CM3Helper.service.CRITICAL_PART_SELECTOR_ROLE_PREFERENCE, PreferenceClient.WINDCHILL_CLIENT_NAME,
					(WTUser) SessionHelper.manager.getPrincipal());
			if (orderChngCriticalPartPrefValue) {
				roleMap = EnerSysApprovalMatrixUtility.getInstance().additionalRolesAdded(roleMap, affectedObjects);
			}
		} catch (Exception e) {
			LOGGER.error("Error In the  display ApprovalFlow Display page ");
			LOGGER.error("Error Info : " + e.getMessage());
		}
		return roleMap;
	}

	/**
	 * Checks if <i>"fetch-all-participants="true"</i> is specified for a particular
	 * Role in the approval map.
	 */
	@Override
	public boolean isBypassContextForUserSelection(
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMapToCheck, Role roleToCheck) {
		boolean ret = false;
		for (String key : roleMapToCheck.keySet()) {
			String[] keys = key.split(COLON_ROLE_MAP_DELIM);
			if (keys.length >= 3 && (keys[PARTICIPANT_ROLE_INDEX]).equalsIgnoreCase(roleToCheck.toString())) {// v1.13
																												// --
																												// changed
																												// from
																												// == to
																												// >=
				if (keys[IS_FETCH_ALL_PARTICIPANTS_INDEX].equalsIgnoreCase("true")) {
					ret = true;
					break;
				} else {
					// Stop processing since Role match + check is already performed.
					break;
				}
			}
		}
		return ret;
	}

	/**
	 * Checks if Strict-Auto-Population value is true for the particular Role in the
	 * approval map (for any context).
	 * 
	 * @since Build v2.2
	 */
	@Override
	public boolean isStrictAutoPopulation(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMapToCheck,
			Role roleToCheck) {
		for (Entry<String, LinkedHashMap<String, WTContainer>> entryObj : roleMapToCheck.entrySet()) {
			String[] keys = entryObj.getKey().split(COLON_ROLE_MAP_DELIM);
			if (keys.length >= 3 && (keys[PARTICIPANT_ROLE_INDEX]).equalsIgnoreCase(roleToCheck.toString())) {
				for (String roleTypeInfo : entryObj.getValue().keySet()) {
					if (roleTypeInfo.split(COLON_ROLE_MAP_DELIM)[STRICT_AUTO_POP_INDEX__ROLETYPE]
							.equalsIgnoreCase("true")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Checks if the Deviation Object has the Type set to Full-Track.
	 * 
	 * @param pbo
	 * @return
	 * @throws WTException
	 */
	private boolean isFullTrackDeviation(Object pbo) throws WTException {
		final String DEVIATION_TYPE_INTERNAL_NAME = "ext.enersys.DEVIATION_TYPE";
		if (pbo instanceof WTVariance) {
			PersistableAdapter pers = new PersistableAdapter((Persistable) pbo, null, Locale.US,
					new DisplayOperationIdentifier());
			pers.load(DEVIATION_TYPE_INTERNAL_NAME);
			String deviationTypeValue = (String) pers.getAsString(DEVIATION_TYPE_INTERNAL_NAME);
			if (deviationTypeValue != null && deviationTypeValue.equalsIgnoreCase("Full Track")) {
				return true;
			}
		} else if (pbo instanceof NmCommandBean) {
			// Build v2.1
			NmCommandBean commandBean = (NmCommandBean) pbo;
			Iterator itr = commandBean.getComboBox().keySet().iterator();
			while (itr.hasNext()) {
				String keyVal = (String) itr.next();
				if (keyVal.contains(DEVIATION_TYPE_INTERNAL_NAME)) {
					ArrayList aList = (ArrayList) commandBean.getComboBox().get(keyVal);
					if (aList != null && !aList.isEmpty()) {
						String selectedValue = (String) aList.get(0);
						if (selectedValue.equalsIgnoreCase("Full Track")) {
							return true;
						}
						break;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Return true if the Primary Business Object is of Firmware CR/CN or CT task.
	 * 
	 * @param pbo
	 * @return
	 */
	@Override
	public boolean isFirmwareCRCNCTpbo(WTObject pbo) {
		return (pbo != null && (TypeIdentifierHelper.getType(pbo).equals(FIRMWARE_CR_TI)
				|| TypeIdentifierHelper.getType(pbo).equals(FIRMWARE_CN_TI)
				|| TypeIdentifierHelper.getType(pbo).equals(FIRMWARE_CT_TI)));
	}

	/**
	 * Return CHANGE TRACK/PROMOTION TRACK Value, if the Command Bean contain that
	 * information for CR/CN/CT/PROMOTION NOTICE objects. <br>
	 * Also supports direct PBOs as 2nd parameter.<br>
	 * 
	 * @param objTI Type Instance of the object
	 * @param obj   Object - Either a Command Bean or a PBO persistable object
	 * @since Build v3.1
	 */
	@Override
	public String getChangeTrackInternalValue(TypeIdentifier objTI, Object obj) {
		if (obj instanceof NmCommandBean) {
			NmCommandBean commandBean = (NmCommandBean) obj;

			if (objTI.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.change2.WTChangeOrder2"))
					|| objTI.isDescendedFrom(
							TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.change2.WTChangeRequest2"))
					|| objTI.isDescendedFrom(
							TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.maturity.PromotionNotice"))) { // for CN
																												// or CR
																												// or
																												// PROMOTION
																												// NOTICE
				return ((HashMap<String, List>) commandBean.getComboBox()).entrySet().stream()
						.filter(e -> (e.getKey().contains(CHANGE_TRACK_ON_CN_INTR_NAME))).map(Entry::getValue)
						.findFirst().map(List::toString).orElse("");
			} else if (objTI
					.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.change2.WTChangeActivity2"))
					&& commandBean.getTextParameter(CHANGE_TRACK_ON_CN_TRACKER_FOR_CT) != null) { // for CT
				return commandBean.getTextParameter(CHANGE_TRACK_ON_CN_TRACKER_FOR_CT);
			}
		} else if (obj instanceof Persistable) {
			Persistable per = (Persistable) obj;
			QueryResult changeNoticeQR;
			try {
				if (per instanceof WTChangeActivity2) {
					changeNoticeQR = ChangeHelper2.service.getChangeOrder((ChangeActivityIfc) per, true);
					while (changeNoticeQR.hasMoreElements()) {
						ChangeOrder2 linkedCN = (ChangeOrder2) changeNoticeQR.nextElement();
						per = linkedCN;
						break;
					}
				}
				PersistableAdapter perObj = new PersistableAdapter(per, null, Locale.getDefault(),
						new DisplayOperationIdentifier());
				perObj.load(CHANGE_TRACK_ON_CN_INTR_NAME);
				return (String) perObj.getAsString(CHANGE_TRACK_ON_CN_INTR_NAME);
			} catch (Exception e) {
				LOGGER.error("Error in method getChangeTrackInternalValue() : " + e);
				if (LOGGER.isDebugEnabled()) {
					e.printStackTrace();
				}
			}
		}
		return "";
	}

	/**
	 * Return CHANGE TRACK/PROMOTION TRACK Value, if the Command Bean contain that
	 * information for CR/CN/CT/PROMOTION NOTICE objects. <br>
	 * Also supports direct PBOs as 2nd parameter.<br>
	 * 
	 * @param objTI Type Instance of the object
	 * @param obj   Object - Either a Command Bean or a PBO persistable object
	 * @since Build v3.1
	 */
	public String getChangeTypeInternalValue(TypeIdentifier objTI, Object obj) {
		String internalValueFlag = "";
		if (obj instanceof NmCommandBean) {
			NmCommandBean commandBean = (NmCommandBean) obj;
			if (objTI.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.maturity.PromotionNotice"))) {
				String changeTypeInternalValue = ((HashMap<String, List>) commandBean.getComboBox()).entrySet().stream()
						.filter(e -> (e.getKey().contains("ext.enersys.PROMOTION_TYPE"))).map(Entry::getValue)
						.findFirst().map(List::toString).orElse("");
				LOGGER.debug("changeTypeInternalValue : " + changeTypeInternalValue);
				if ((changeTypeInternalValue.replaceAll("\\]|\\[", "")).equalsIgnoreCase("eBom_plantBom")) {
					internalValueFlag = "EBPB";
				} else {
					internalValueFlag = "PSU";
				}
				// for CN or CR or PROMOTION NOTICE
				LOGGER.debug("internalValueFlag : " + internalValueFlag);
				return internalValueFlag;
			}
		} else if (obj instanceof PromotionNotice) {
			Persistable per = (Persistable) obj;
			QueryResult changeNoticeQR;
			try {
				PersistableAdapter perObj = new PersistableAdapter(per, null, Locale.getDefault(),
						new DisplayOperationIdentifier());
				perObj.load("ext.enersys.PROMOTION_TYPE");
				String changeTypeInternalValue = (String) perObj.getAsString("ext.enersys.PROMOTION_TYPE");
				LOGGER.debug("changeTypeInternalValue : " + changeTypeInternalValue);
				if (changeTypeInternalValue.equalsIgnoreCase("eBom_plantBom")) {
					internalValueFlag = "EBPB";
				} else {
					internalValueFlag = "PSU";
				}
				LOGGER.debug("internalValueFlag : " + internalValueFlag);
				return internalValueFlag;
			} catch (Exception e) {
				LOGGER.error("Error in method getChangeTypeInternalValue() : " + e);
				if (LOGGER.isDebugEnabled()) {
					e.printStackTrace();
				}
			}
		}
		return "";
	}

	/**
	 * Return true if the Command Bean contain Change Track information for CN/CT
	 * objects.
	 * 
	 * @param objTI
	 * @param commandBean
	 * @since Build v2.11
	 * @return
	 */
	@Override
	public boolean isFastTrackCNCTpboBeanCheck(TypeIdentifier objTI, NmCommandBean commandBean) {
		if (objTI.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.change2.WTChangeOrder2"))) { // for
																													// CN
			String valRetrieved = ((HashMap<String, List>) commandBean.getComboBox()).entrySet().stream()
					.filter(e -> (e.getKey().contains(CHANGE_TRACK_ON_CN_INTR_NAME))).map(Entry::getValue).findFirst()
					.map(List::toString).orElse("");

			if (valRetrieved.contains(FAST_TRACK_INTER_NAME)) {
				return true;
			}
		} else if (objTI.isDescendedFrom(TypeIdentifierHelper.getTypeIdentifier("WCTYPE|wt.change2.WTChangeActivity2"))
				&& commandBean.getTextParameter(CHANGE_TRACK_ON_CN_TRACKER_FOR_CT) != null && commandBean
						.getTextParameter(CHANGE_TRACK_ON_CN_TRACKER_FOR_CT).equalsIgnoreCase(FAST_TRACK_INTER_NAME)) { // for
																														// CT
			return true;
		}
		return false;
	}

	/**
	 * Given a CN object, opens a persistable adapter, loads the Change Track and
	 * returns its value.
	 * 
	 * @param per
	 * @return
	 * @since Build v2.11
	 */
	public boolean checkIfObjectIsFastTrack(Persistable o) {
		try {
			PersistableAdapter obj = new PersistableAdapter(o, null, Locale.getDefault(),
					new DisplayOperationIdentifier());
			obj.load(CHANGE_TRACK_ON_CN_INTR_NAME);
			String changeTrack = (String) obj.getAsString(CHANGE_TRACK_ON_CN_INTR_NAME);
			if (changeTrack != null && changeTrack.equalsIgnoreCase(FAST_TRACK_INTER_NAME)) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Error in method checkIfObjectIsFastTrack() : " + e);
			if (LOGGER.isDebugEnabled()) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * For a CN Object --> checks the Change Track attribute.<br>
	 * For a CT object --> Gets the linked CN and check the Change Track attribute
	 * on it.<br>
	 * 
	 * @since Build v2.11
	 */
	@Override
	public boolean isFastTrackCNCTobjectCheck(WTObject obj) {
		if (obj instanceof WTChangeOrder2) {
			return checkIfObjectIsFastTrack(obj);
		} else if (obj instanceof WTChangeActivity2) {
			// Get CN Objects, check if CN is Fast Track
			QueryResult changeNoticeQR;
			try {
				changeNoticeQR = ChangeHelper2.service.getChangeOrder((ChangeActivityIfc) obj, true);

				while (changeNoticeQR.hasMoreElements()) {
					ChangeOrder2 linkedCN = (ChangeOrder2) changeNoticeQR.nextElement();
					return checkIfObjectIsFastTrack(linkedCN);
				}
			} catch (WTException e1) {
				LOGGER.error("Error in method isFastTrackCNCTobjectCheck() : " + e1);
				if (LOGGER.isDebugEnabled()) {
					e1.printStackTrace();
				}
			}
		}
		return false;
	}

	/**
	 * Return true if the TypeIdentifier passed is of Firmware CR/CN or CT task.
	 * 
	 * @param objTI
	 * @since Build v2.1
	 * @return
	 */
	@Override
	public boolean isFirmwareCRCNCTpbo(TypeIdentifier objTI) {
		return (objTI != null
				&& (objTI.equals(FIRMWARE_CR_TI) || objTI.equals(FIRMWARE_CN_TI) || objTI.equals(FIRMWARE_CT_TI)));
	}

	/**
	 * 
	 * @since Build v2.1
	 */
	public NodeList getParticipantNodeListForObjectWizard(WTObject affectedItem, TypeIdentifier pboTI,
			String pnMaturityState, HashMap<String, Object> comboxHashMapValues) {
		checkAndWriteDebug(Debuggable.START, "#getParticipantNodeListForObject -->", " affectedItem: ", affectedItem,
				" pboTI: ", pboTI);
		NodeList participantsList = null;
		ESPropertyHelper props = new ESPropertyHelper(ESPropertyHelper.ES_DOCUMENTAPPROVAL_HELPER);
		try {
			if (affectedItem != null && pboTI != null) {
				// Get the TypeIdentifier of the Object
				String subTypeNameOrDefault = TypeIdentifierHelper.getType(affectedItem).getTypeInternalName();

				String lcState = null;
				final String PBO_TYPE = CM2Helper.service.getChangeTypeString(pboTI);

				String PBO_TYPE_TO_SEARCH = getCorrespondingPBOTag(PBO_TYPE);

				// Get PBO Type; If Type = PROMOTION REQUEST then lcState = Maturity State of
				// the PR
				if (PBO_TYPE.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)) {
					// In v2.1 maturity state is fetched using commandBean in caller method
					lcState = pnMaturityState;
				} else {
					// Get LC State from the affectedItem
					lcState = EnerSysHelper.service.getObjectState(affectedItem);
				}

				if (PBO_TYPE.equalsIgnoreCase(EnerSysService.ACTIVITY_DOCUMENTAPPROVAL)) {

					LOGGER.debug("DAP Track: " + pnMaturityState);
					if ((pnMaturityState.equals("C_PRODUCTION_APPROVED")
							|| pnMaturityState.equals("C/Production Approved"))) {
						String docType = "";
						if (affectedItem instanceof WTDocument) {
							if (subTypeNameOrDefault.equalsIgnoreCase("com.ptcmscloud.FIRMWARE_DOCUMENT")) {
								docType = EnerSysHelper.service.getAttributeValues(affectedItem,
										"ext.enersys.FIRMWARE_DOCUMENT_DOC_TYPE");
							} else {
								docType = EnerSysHelper.service.getAttributeValues(affectedItem, "DOCUMENT_TYPE");
							}
						} else if (affectedItem instanceof EPMDocument) {
							docType = "CAD";
						}

						HashMap<String, String> docTypeMap = props.streamConvert();
						for (Entry<String, String> s : docTypeMap.entrySet()) {
							String[] documentTypeValues = s.getValue().split(",");
							for (String a : documentTypeValues) {
								if (a.equalsIgnoreCase(docType)) {
									lcState = s.getKey();
									break;
								}
							}
						}
					} else
						lcState = EnerSysApprovalMatrixDefinition.DEFAULT_TAG;
				}

				// Check if the there are any entries defined for the given type, otherwise, set
				// subTypeNameOrDefault to DEFAULT
				if (!isTypeNodePresent(subTypeNameOrDefault)) {
					subTypeNameOrDefault = EnerSysApprovalMatrixDefinition.DEFAULT_TAG;
				}

				// Check if the Type has the specified CHANGE OBJECT defined in the
				// matrix;Otherwise Use DEFAULT!!!
				if (!isChangeNodeDefinedForType(PBO_TYPE_TO_SEARCH, subTypeNameOrDefault)) {
					PBO_TYPE_TO_SEARCH = EnerSysApprovalMatrixDefinition.DEFAULT_TAG;
				}
				/* Added Build_V3.8 - Sprint 9 - 8206 */
				if ((CM2Helper.service.getChangeTypeString(pboTI).equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)
						|| CM2Helper.service.getChangeTypeString(pboTI)
								.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEACTIVITY))
						&& affectedItem instanceof RevisionControlled) {
					RevisionControlled currentObject = (RevisionControlled) affectedItem;
					String resultingItemObjectID = String.valueOf(currentObject.getBranchIdentifier());
					for (Map.Entry<String, Object> entry : comboxHashMapValues.entrySet()) {
						if (entry.getKey().contains("changeTargetTransition")
								&& entry.getKey().contains(resultingItemObjectID)) {
							String releasedTargetState = (String) ((List) entry.getValue()).get(0);// DisplayValue need
																									// to intername of
																									// Lifecycle
							if (releasedTargetState != null && !releasedTargetState.isEmpty()) {
								if (releasedTargetState.equals("CHANGE")) {
									CM4Service cmdService = CM4ServiceUtility.getInstance();
									lcState = cmdService.getChangeTransitionState(currentObject);
								} else {
									releasedTargetState = getDisplayNameForTransition(releasedTargetState);
									releasedTargetState = getInternalNameForState(releasedTargetState);
									lcState = releasedTargetState;
								}
							}
							break;
						}
					}
				}
				/* Added Build_V3.8 - Sprint 9 - 8206 */
				// Check if the CHANGE OBJECT specified under the Type has the STATE defined in
				// the matrix;Otherwise Use DEFAULT!!!
				if (!isStateNodeDefinedForTypeAndChange(lcState, PBO_TYPE_TO_SEARCH, subTypeNameOrDefault)) {
					lcState = EnerSysApprovalMatrixDefinition.DEFAULT_TAG;
				}
				// extract the nwiodes for the given State & PBO's type based on the Type filter
				participantsList = getParticipantsNodeForGivenInternalStateAndPBOTagInfoForType(lcState,
						PBO_TYPE_TO_SEARCH, subTypeNameOrDefault);

			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#getParticipantNodeListForObject -->", " participantsList: ",
				participantsList);
		return participantsList;
	}

	@Override
	public NodeList getParticipantNodeListForObject(WTObject affectedItem, WTObject pbo) {
		checkAndWriteDebug(Debuggable.START, "#getParticipantNodeListForObject -->", " affectedItem: ", affectedItem,
				" pbo: ", pbo);
		NodeList participantsList = null;
		ESPropertyHelper props = new ESPropertyHelper(ESPropertyHelper.ES_DOCUMENTAPPROVAL_HELPER);
		try {
			RevisionControlled currentObject = (RevisionControlled) affectedItem;
			if (affectedItem != null && pbo != null) {
				// Get the TypeIdentifier of the Object
				String subTypeNameOrDefault = TypeIdentifierHelper.getType(affectedItem).getTypeInternalName();

				String lcState = null;
				final String PBO_TYPE = CM2Helper.service.getChangeTypeString(pbo);

				String PBO_TYPE_TO_SEARCH = getCorrespondingPBOTag(PBO_TYPE);

				// Get PBO Type; If Type = PROMOTION REQUEST then lcState = Maturity State of
				// the PR
				if (PBO_TYPE.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)) {
					PromotionNotice promotionNotice = (PromotionNotice) pbo;
					lcState = promotionNotice.getMaturityState().toString();
				} else if (PBO_TYPE.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {
					// Get LC State from the affectedItem
					QueryResult qr = ChangeHelper2.service.getChangeablesAfter((WTChangeOrder2) pbo, false);
					while (qr.hasMoreElements()) {
						wt.change2.ChangeRecord2 crecord = (wt.change2.ChangeRecord2) qr.nextElement();
						// wt.fc.Persistable persistable = (wt.fc.Persistable) qr.nextElement();
						wt.fc.Persistable persistable = crecord.getChangeable2();
						if (persistable.getPersistInfo().getObjectIdentifier()
								.equals(affectedItem.getPersistInfo().getObjectIdentifier())) {
							String targetTransitionDisplayvalue = crecord.getTargetTransition().getDisplay();
							String targetTransitionvalue = crecord.getTargetTransition().getValue();
							LOGGER.debug("targetTransitionDisplayvalue ::" + targetTransitionDisplayvalue
									+ "targetTransitionvalue:: " + targetTransitionvalue);
							if (targetTransitionvalue.equals("CHANGE")) {
								CM4Service cmdService = CM4ServiceUtility.getInstance();
								lcState = cmdService.getChangeTransitionState(currentObject);
								LOGGER.debug("Inside Change lcState::" + lcState);
								if (lcState == null || lcState.equalsIgnoreCase("")) {
									lcState = EnerSysHelper.service.getObjectState(affectedItem);
								}

							} else {
								String releasedTargetState = getDisplayNameForTransition(targetTransitionvalue);
								releasedTargetState = getInternalNameForState(releasedTargetState);
								lcState = releasedTargetState;
								LOGGER.debug("Inside else lcState::" + lcState);

							}
						}
					}

					// lcState = EnerSysHelper.service.getObjectState(affectedItem);
				} else {
					// Get LC State from the affectedItem
					lcState = EnerSysHelper.service.getObjectState(affectedItem);
				}

				if (PBO_TYPE.equalsIgnoreCase(EnerSysService.ACTIVITY_DOCUMENTAPPROVAL)) {
					WTChangeReview rev = (WTChangeReview) pbo;
					String dapTrack = (String) ESBusinessHelper.getIBAValue(rev, "ext.enersys.dapTrack");
					LOGGER.debug("DAP Track: " + dapTrack);
					if ((dapTrack.equals("C_PRODUCTION_APPROVED"))) {
						String docType = "";
						if (affectedItem instanceof WTDocument) {
							if (subTypeNameOrDefault.equalsIgnoreCase("com.ptcmscloud.FIRMWARE_DOCUMENT")) {
								docType = EnerSysHelper.service.getAttributeValues(affectedItem,
										"ext.enersys.FIRMWARE_DOCUMENT_DOC_TYPE");
							} else {
								docType = EnerSysHelper.service.getAttributeValues(affectedItem, "DOCUMENT_TYPE");
							}
						} else if (affectedItem instanceof EPMDocument) {
							docType = "CAD";
						}

						HashMap<String, String> docTypeMap = props.streamConvert();
						for (Entry<String, String> s : docTypeMap.entrySet()) {
							String[] documentTypeValues = s.getValue().split(",");
							for (String a : documentTypeValues) {
								if (a.equalsIgnoreCase(docType)) {
									lcState = s.getKey();
									break;
								}
							}
						}
					} else
						lcState = EnerSysApprovalMatrixDefinition.DEFAULT_TAG;
				}

				// Check if the there are any entries defined for the given type, otherwise, set
				// subTypeNameOrDefault to DEFAULT
				if (!isTypeNodePresent(subTypeNameOrDefault)) {
					subTypeNameOrDefault = EnerSysApprovalMatrixDefinition.DEFAULT_TAG;
				}

				// Check if the Type has the specified CHANGE OBJECT defined in the
				// matrix;Otherwise Use DEFAULT!!!
				if (!isChangeNodeDefinedForType(PBO_TYPE_TO_SEARCH, subTypeNameOrDefault)) {
					PBO_TYPE_TO_SEARCH = EnerSysApprovalMatrixDefinition.DEFAULT_TAG;
				}

				// Check if the CHANGE OBJECT specified under the Type has the STATE defined in
				// the matrix;Otherwise Use DEFAULT!!!
				if (!isStateNodeDefinedForTypeAndChange(lcState, PBO_TYPE_TO_SEARCH, subTypeNameOrDefault)) {
					lcState = EnerSysApprovalMatrixDefinition.DEFAULT_TAG;
				}

				// extract the nodes for the given State & PBO's type based on the Type filter
				participantsList = getParticipantsNodeForGivenInternalStateAndPBOTagInfoForType(lcState,
						PBO_TYPE_TO_SEARCH, subTypeNameOrDefault);
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#getParticipantNodeListForObject -->", " participantsList: ",
				participantsList);
		return participantsList;
	}

	@Override
	public boolean isStateNodeDefinedForTypeAndChange(String lcState, String PBO_TYPE_TO_SEARCH, String subTypeInfo) {
		checkAndWriteDebug(Debuggable.START, "#isStateNodeDefinedForTypeAndChange -->", " lcState: ", lcState,
				" PBO_TYPE_TO_SEARCH: ", PBO_TYPE_TO_SEARCH, " subTypeInfo: ", subTypeInfo);

		boolean isPresent = false;
		if (subTypeInfo != null) {
			subTypeInfo = subTypeInfo.trim();
			NodeList appMatElems = getParticipantsNodeForGivenInternalStateAndPBOTagInfoForType(lcState,
					PBO_TYPE_TO_SEARCH, subTypeInfo);
			if (appMatElems != null && appMatElems.getLength() >= 1) {
				isPresent = true;
			}
		}
		checkAndWriteDebug(Debuggable.END, "#isStateNodeDefinedForTypeAndChange -->", " isPresent: ", isPresent);
		return isPresent;
	}

	@Override
	public boolean isChangeNodeDefinedForType(String PBO_TYPE_TO_SEARCH, String subTypeInfo) {
		checkAndWriteDebug(Debuggable.START, "#isChangeNodeDefinedForType -->", " PBO_TYPE_TO_SEARCH: ",
				PBO_TYPE_TO_SEARCH, " subTypeInfo: ", subTypeInfo);
		boolean isPresent = false;
		if (checkIfAnyDSIsNull()) {
			loadApprovalMatrixDOMDocument();
		}
		if (subTypeInfo != null) {
			subTypeInfo = subTypeInfo.trim();
			Element elementNode = getNodeForGivenType(subTypeInfo, true);

			String xpathExpressionStr = "./" + PBO_TYPE_TO_SEARCH;
			try {
				XPathExpression xpathExpression = xpath.compile(xpathExpressionStr);
				NodeList appMatElems = (NodeList) xpathExpression.evaluate(elementNode, XPathConstants.NODESET);
				if (appMatElems != null && appMatElems.getLength() >= 1) {
					isPresent = true;
				}
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}
		checkAndWriteDebug(Debuggable.END, "#isChangeNodeDefinedForType -->", " isPresent: ", isPresent);
		return isPresent;
	}

	@Override
	public Element getApprovalMatrixElementForObject(WTObject affectedItem) {
		checkAndWriteDebug(Debuggable.START, "#getApprovalMatrixElementForObject -->", " affectedItem: ", affectedItem);
		Element elem_type_node = null;
		if (checkIfAnyDSIsNull()) {
			loadApprovalMatrixDOMDocument();
		}
		if (affectedItem != null) {
			// Get the TypeIdentifier of the Object
			String subTypeNameOrDefault = TypeIdentifierHelper.getType(affectedItem).getTypeInternalName();

			elem_type_node = getNodeForGivenType(subTypeNameOrDefault, true);
		}
		checkAndWriteDebug(Debuggable.END, "#getApprovalMatrixElementForObject -->", " elem_type_node: ",
				elem_type_node);
		return elem_type_node;
	}

	@Override
	public String getCorrespondingPBOTag(String pboType) {
		checkAndWriteDebug(Debuggable.START, "#getCorrespondingPBOTag -->", " pboType: ", pboType);
		String retStr = pboType;
		if (pboType.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGENOTICE)) {
			retStr = EnerSysApprovalMatrixDefinition.CHANGE_NOTICE_TAG;
		} else if (pboType.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEREQUEST)) {
			retStr = EnerSysApprovalMatrixDefinition.CHANGE_REQUEST_TAG;
		} else if (pboType.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)) {
			retStr = EnerSysApprovalMatrixDefinition.PROMOTION_REQUEST_TAG;
		} else if (pboType.equalsIgnoreCase(EnerSysService.ACTIVITY_CHANGEACTIVITY)) {
			retStr = EnerSysApprovalMatrixDefinition.CHANGE_ACTIVITY_TAG;
		} else if (pboType.equalsIgnoreCase(EnerSysService.ACTIVITY_DEVIATION)) {
			retStr = EnerSysApprovalMatrixDefinition.DEVIATION_TAG; // Added for Build v1.12
		} else if (pboType.equalsIgnoreCase(EnerSysService.ACTIVITY_DOCUMENTAPPROVAL)) {
			retStr = EnerSysApprovalMatrixDefinition.DOCUMENT_APPROVAL_TAG;
		}
		checkAndWriteDebug(Debuggable.END, "#getCorrespondingPBOTag -->", " retStr: ", retStr);
		return retStr;
	}

	/**
	 * Used internally
	 * 
	 * @param state
	 * @param pboType
	 * @param intrName
	 * @return
	 */
	private NodeList getParticipantsNodeForGivenInternalStateAndPBOTagInfoForType(String state, String pboType,
			String intrName) {
		checkAndWriteDebug(Debuggable.START, "#getParticipantsNodeForGivenInternalStateAndPBOTagInfoForType -->",
				" state:", state, " pboType:", pboType, " intrName:", intrName);
		NodeList participantlist = null;
		if (checkIfAnyDSIsNull()) {
			loadApprovalMatrixDOMDocument();
		}
		Element elementNode = getNodeForGivenType(intrName, true);
		String xpathExpressionStr = "./" + pboType + "/" + state + "/"
				+ EnerSysApprovalMatrixDefinition.PARTICIPANT_TAG;
		try {
			XPathExpression xpathExpression = xpath.compile(xpathExpressionStr);
			participantlist = (NodeList) xpathExpression.evaluate(elementNode, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#getParticipantsNodeForGivenInternalStateAndPBOTagInfoForType -->",
				" participantlist:", participantlist);
		return participantlist;
	}

	@Deprecated
	@Override
	public String generateRoleWithSequencesToDisplayInUI(
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> fullHashMap) {
		checkAndWriteDebug(Debuggable.START, "#generateRoleWithSequencesToDisplayInUI -->", " fullHashMap:",
				fullHashMap);
		String retStr = "";
		Iterator<Integer> itr = getSequencesInOrder(fullHashMap);
		LinkedHashSet<String> tempMainHS = new LinkedHashSet<String>();
		TreeSet<String> tempHS = new TreeSet<>(); // Fixed -- Build v1.13--> changed from LinkedHashSet to TreeSet to
													// get sorted role-names within sequence
		tempMainHS.clear();
		while (itr.hasNext()) {
			int nxt = itr.next();
			LinkedHashSet<String> set = getParticipantRolesForSequenceNumber(nxt, fullHashMap);
			if (set != null && !set.isEmpty()) {
				tempHS.clear();
				for (String str : set) {

					String h[] = str.split(COLON_ROLE_MAP_DELIM);
					String roleName = h[0];
					// String tempH = (WordUtils.capitalize(StringUtils.lowerCase(h[0])));
					String tempH = Role.toRole(roleName).getDisplay(Locale.US);
					if (h[1].equalsIgnoreCase(EnerSysApprovalMatrixDefinition.APPR_OPTIONAL_VALUE)) {
						tempH += " (Opt.)";
					}
					tempHS.add(tempH);
				}
				if (tempHS.size() > 0) {
					tempMainHS.add("\"" + StringUtils.join(tempHS, COLON_ROLE_MAP_DELIM) + "\"");
				}
			}
		}
		retStr = StringUtils.join(tempMainHS, COMMA_DELIM);
		checkAndWriteDebug(Debuggable.END, "#generateRoleWithSequencesToDisplayInUI -->", " retStr:", retStr);
		return retStr;
	}

	/**
	 * Resets & reloads all Internal Type & Element specific hashmaps & element node
	 * variable for DEFAULT scenario.<br>
	 */
	@Override
	public void reloadTypeListMappingFromMatrix() {
		// RESET ALL DATA STRUCTURES - storing a combination of TYPEIDENTIFIER - ELEMENT
		// INFORMATION
		checkAndWriteDebug(Debuggable.START, "#reloadTypeListMappingFromMatrix");
		checkAndWriteDebug(Debuggable.LINE, "#reloadTypeListMappingFromMatrix --> ",
				" tiElementApplyToDescendentsMap: ", tiElementApplyToDescendentsMap);
		checkAndWriteDebug(Debuggable.LINE, "#reloadTypeListMappingFromMatrix --> ", " tiElementSpecificMap: ",
				tiElementSpecificMap);
		checkAndWriteDebug(Debuggable.LINE, "#reloadTypeListMappingFromMatrix --> ",
				" elementStructureForDefaultType: ", elementStructureForDefaultType);

		if (tiElementApplyToDescendentsMap != null) {
			tiElementApplyToDescendentsMap.clear();
		} else {
			tiElementApplyToDescendentsMap = new HashMap<TypeIdentifier, Element>() {
				private static final long serialVersionUID = 1L;

				@Override
				public boolean containsKey(Object key) {
					TypeIdentifier ti = null;
					if (key instanceof TypeIdentifier) {
						ti = (TypeIdentifier) key;
					} else {
						ti = TypeIdentifierHelper.getType(key);
					}
					for (TypeIdentifier f : keySet()) {
						if (ti.isDescendedFrom(f)) {
							return true;
						}
					}
					return false;
				};

				@Override
				public Element get(Object key) {
					Element ret = super.get(key);
					if (ret == null && containsKey(key)) {
						TypeIdentifier ti = null;
						if (key instanceof TypeIdentifier) {
							ti = (TypeIdentifier) key;
						} else {
							ti = TypeIdentifierHelper.getType(key);
						}
						for (TypeIdentifier f : keySet()) {
							if (ti.isDescendedFrom(f)) {
								ret = super.get(f);
								break;
							}
						}
					}
					return ret;
				};
			};
		}
		if (tiElementSpecificMap != null) {
			tiElementSpecificMap.clear();
		} else {
			tiElementSpecificMap = new HashMap<>();
		}
		// Setting Default Structure Approval Matrix Info to null;
		elementStructureForDefaultType = null;

		String xpathExpressionStr = "/" + EnerSysApprovalMatrixDefinition.HEAD_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.APPR_MAT_TAG;
		try {
			XPathExpression xpathExpression = xpath.compile(xpathExpressionStr);
			NodeList appMatElems = (NodeList) xpathExpression.evaluate(approvalMatrix, XPathConstants.NODESET);
			if (appMatElems.getLength() >= 1) {
				for (int i = 0; i < appMatElems.getLength(); ++i) {
					Node approvalMatNode = appMatElems.item(i);
					Element approvalMatElem = (Element) approvalMatNode;
					String strIntrNames = approvalMatElem
							.getAttribute(EnerSysApprovalMatrixDefinition.APPR_MAT_TYPE_ATTR);

					// SKIP NULL, EMPTY & DEFAULT TYPES
					if (strIntrNames != null && !strIntrNames.isEmpty()
							&& !strIntrNames.equalsIgnoreCase(EnerSysApprovalMatrixDefinition.DEFAULT_TAG)) {
						String[] strTypeNameSplit = strIntrNames.split(SEMICOLON_DELIM);
						for (String strTypeName : strTypeNameSplit) {
							String strApplyToDescendents = approvalMatElem
									.getAttribute(EnerSysApprovalMatrixDefinition.APPR_MAT_DESCENT_ATTR);
							boolean isAppliedToDescendents = ((strApplyToDescendents == null)
									|| (strApplyToDescendents.isEmpty())
									|| strApplyToDescendents.equalsIgnoreCase("false")) ? false : true;
							TypeIdentifier ti = TypeIdentifierHelper.getTypeIdentifier(strTypeName);
							if (isAppliedToDescendents) {
								tiElementApplyToDescendentsMap.put(ti, approvalMatElem);
							} else {
								tiElementSpecificMap.put(ti, approvalMatElem);
							}
						}
					} else if (strIntrNames != null
							&& strIntrNames.equalsIgnoreCase(EnerSysApprovalMatrixDefinition.DEFAULT_TAG)) {
						elementStructureForDefaultType = approvalMatElem;
					}
				}
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.LINE, "#reloadTypeListMappingFromMatrix --> AFT",
					" tiElementApplyToDescendentsMap: ", tiElementApplyToDescendentsMap);
			checkAndWriteDebug(Debuggable.LINE, "#reloadTypeListMappingFromMatrix --> AFT", " tiElementSpecificMap: ",
					tiElementSpecificMap);
			checkAndWriteDebug(Debuggable.LINE, "#reloadTypeListMappingFromMatrix --> AFT",
					" elementStructureForDefaultType: ", elementStructureForDefaultType);
			checkAndWriteDebug(Debuggable.END, "#reloadTypeListMappingFromMatrix");
		}
	}

	public void refreshBlackListedStatesDS() {
		checkAndWriteDebug(Debuggable.START, "#refreshBlackListedStatesDS");
		if (elementStructureForDefaultType != null && blackListForDefaultType != null) {
			blackListForDefaultType.clear();
		} else {
			blackListForDefaultType = new HashSet<>();
		}
		if (tiBlackListSpecificMap != null) {
			tiBlackListSpecificMap.clear();
		} else {
			tiBlackListSpecificMap = new HashMap<TypeIdentifier, Set<String>>();
		}
		if (tiBlackListApplyToDescendentsMap != null) {
			tiBlackListApplyToDescendentsMap.clear();
		} else {
			tiBlackListApplyToDescendentsMap = new HashMap<TypeIdentifier, Set<String>>() {
				private static final long serialVersionUID = 1L;

				@Override
				public boolean containsKey(Object key) {
					TypeIdentifier ti = null;
					if (key instanceof TypeIdentifier) {
						ti = (TypeIdentifier) key;
					} else {
						ti = TypeIdentifierHelper.getType(key);
					}
					for (TypeIdentifier f : keySet()) {
						if (ti.isDescendedFrom(f)) {
							return true;
						}
					}
					return false;
				};

				@Override
				public Set<String> get(Object key) {
					Set<String> ret = super.get(key);
					if (ret == null && containsKey(key)) {
						TypeIdentifier ti = null;
						if (key instanceof TypeIdentifier) {
							ti = (TypeIdentifier) key;
						} else {
							ti = TypeIdentifierHelper.getType(key);
						}
						for (TypeIdentifier f : keySet()) {
							if (ti.isDescendedFrom(f)) {
								ret = super.get(f);
								break;
							}
						}
					}
					return ret;
				};
			};
		}

		for (Entry<TypeIdentifier, Element> g : tiElementApplyToDescendentsMap.entrySet()) {
			Element elem = g.getValue();
			HashSet<String> hs = new HashSet<>();
			if (elem != null) {
				String blacklistStates = elem
						.getAttribute(EnerSysApprovalMatrixDefinition.APPR_MAT_AO_BLACKLISTED_STATE_ATTR);
				if (blacklistStates != null && !blacklistStates.isEmpty()) {
					Collections.addAll(hs, StringUtils.split(blacklistStates, SEMICOLON_DELIM));
				}
			}
			tiBlackListApplyToDescendentsMap.put(g.getKey(), hs);
		}
		for (Entry<TypeIdentifier, Element> g : tiElementSpecificMap.entrySet()) {
			Element elem = g.getValue();
			HashSet<String> hs = new HashSet<>();
			if (elem != null) {
				String blacklistStates = elem
						.getAttribute(EnerSysApprovalMatrixDefinition.APPR_MAT_AO_BLACKLISTED_STATE_ATTR);
				if (blacklistStates != null && !blacklistStates.isEmpty()) {
					Collections.addAll(hs, StringUtils.split(blacklistStates, SEMICOLON_DELIM));
				}
			}
			tiBlackListSpecificMap.put(g.getKey(), hs);
		}

		if (elementStructureForDefaultType != null) {
			HashSet<String> hs = new HashSet<>();
			String blacklistStates = elementStructureForDefaultType
					.getAttribute(EnerSysApprovalMatrixDefinition.APPR_MAT_AO_BLACKLISTED_STATE_ATTR);
			if (blacklistStates != null && !blacklistStates.isEmpty()) {
				Collections.addAll(hs, StringUtils.split(blacklistStates, SEMICOLON_DELIM));
			}
			blackListForDefaultType.addAll(hs);
		}
		checkAndWriteDebug(Debuggable.END, "#refreshBlackListedStatesDS");
	}

	/**
	 * Checks if, for a particular Type, there are any <approval-matrix>
	 * defined.<br>
	 */
	@Override
	public boolean isTypeNodePresent(String subTypeInfo) {
		checkAndWriteDebug(Debuggable.START, "#isTypeNodePresent -->", " subTypeInfo: ", subTypeInfo);
		boolean isPresent = false;
		if (checkIfAnyDSIsNull()) {
			loadApprovalMatrixDOMDocument();
		}
		if (subTypeInfo != null) {
			subTypeInfo = subTypeInfo.trim();
			isPresent = ((getNodeForGivenType(subTypeInfo, false) == null) ? false : true);
		}
		checkAndWriteDebug(Debuggable.END, "#isTypeNodePresent -->", " isPresent: ", isPresent);
		return isPresent;
	}

	/**
	 * Returns null if the subtype passed is empty or null.<br>
	 * Otherwise, calculate the TypeIdentifier Information & get the Specific
	 * Element map only if it is defined exactly in the Approval Matrix.<br>
	 * Otherwise, calculates the TypeIdentifier Information first & get the Element
	 * map only if its a descendant.<br>
	 * Else returns DEFAULT element type if assignDefault is true; Also returns
	 * DEFAULT element type if the SUBTYPE info is passed as DEFAULT.<br>
	 * If the passed subtype info is non-empty but incorrect & if assignDefault is
	 * false --> return NULL<br>
	 * If the passed subtype info is non-empty but incorrect & if assignDefault is
	 * true --> return DEFAULT Element<br>
	 * --> 1st priority : is for specific types <br>
	 * --> Then --> 2nd priority : is for types with descendants enabled <br>
	 * --> Then --> 3rd priority : assign the DEFAULT in case of no applicable types
	 */
	@Override
	public Element getNodeForGivenType(String subTypeInfoOrDefault, boolean assignDefault) {
		checkAndWriteDebug(Debuggable.START, "#getNodeForGivenType -->", " subTypeInfoOrDefault: ",
				subTypeInfoOrDefault, " assignDefault: ", assignDefault);
		Element elem = null;
		if (checkIfAnyDSIsNull()) {
			loadApprovalMatrixDOMDocument();
		}
		if (subTypeInfoOrDefault != null) {
			subTypeInfoOrDefault = subTypeInfoOrDefault.trim();
			TypeIdentifier ti = TypeIdentifierHelper.getTypeIdentifier(subTypeInfoOrDefault);

			if (subTypeInfoOrDefault.equalsIgnoreCase(EnerSysApprovalMatrixDefinition.DEFAULT_TAG)) {
				// IF IT WAS EXPLICITLY ASKED TO GET DEFAULT TAG, GET THE Element for the
				// DEFAULT TAG
				elem = elementStructureForDefaultType;
			} else if (tiElementSpecificMap.containsKey(ti)) {
				// 1st priority is for specific types
				elem = tiElementSpecificMap.get(ti);
			} else if (tiElementApplyToDescendentsMap.containsKey(ti)) {
				// 2nd priority is for types with descendants enabled
				elem = tiElementApplyToDescendentsMap.get(ti);
			} else if (assignDefault) {
				// 3rd : assign the DEFAULT in case of no applicable types
				elem = elementStructureForDefaultType;
			}
		}
		checkAndWriteDebug(Debuggable.START, "#getNodeForGivenType -->", " elem: ", elem);
		return elem;
	}

	@Override
	public void loadApprovalMatrixDOMDocument() {
		checkAndWriteDebug(Debuggable.START, "#loadApprovalMatrixDOMDocument");
		try {
			boolean isOK = loadWTDocument();
			if (!isOK) {
				// Manual Loading of Approval matrix from FS
				approvalMatrix = null;
				WTProperties wtprops = WTProperties.getServerProperties();
				String MATRIX_LOCATION = wtprops.getProperty("wt.codebase.location") + APPROVAL_MATRIX_FILE_PATH
						+ APPROVAL_MATRIX_FILENAME;
				checkAndWriteDebug(Debuggable.LINE, "#loadApprovalMatrixDOMDocument -->", " MATRIX_LOCATION: ",
						MATRIX_LOCATION);
				approvalMatrix = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(MATRIX_LOCATION);
				checkAndWriteDebug(Debuggable.LINE, "#loadApprovalMatrixDOMDocument -->", " approvalMatrix: ",
						approvalMatrix);
			}
			reloadTypeListMappingFromMatrix();
			reloadDeviationMappingFromMatrix();// Added in Build v1.13
			reloadFirmwareMappingFromMatrix();// Added in Build v1.13
			reloadFastTrackCNCTMappingFromMatrix(); // Added in Build v2.11
			refreshBlackListedStatesDS();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#loadApprovalMatrixDOMDocument");
		}
	}

	private boolean checkIfAnyDSIsNull() {
		// TODO: Add more Data Structures
		checkAndWriteDebug(Debuggable.START, "#checkIfAnyDSIsNull");
		boolean retBool = false;
		if (FORCE_LOAD_MATRIX || approvalMatrix == null || isMoreRecentApprovalMatrixWTDocumentExisting()
				|| tiElementApplyToDescendentsMap == null || tiElementSpecificMap == null
				|| elementStructureForDefaultType == null || blackListForDefaultType == null
				|| tiBlackListSpecificMap == null || tiBlackListApplyToDescendentsMap == null
				|| deviationFastTrackElements == null || deviationFullTrackElements == null
				|| firmwareCRBElements == null || deviationNotificationTeamElements == null
				|| firmwareNotificationTeamElements == null || changeActivityFastTrackElements == null) {
			retBool = true;
		}
		checkAndWriteDebug(Debuggable.END, "#checkIfAnyDSIsNull -->", " retBool: ", retBool);
		return retBool;
	}

	@Override
	public void checkAndWriteDebug(String prefix, String middle, Object... args) {
		if (LOGGER.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append(prefix);
			sb.append(CLASSNAME);
			sb.append(middle);
			if (args != null) {
				for (Object o : args) {
					if (o instanceof String) {
						sb.append(o);
					} else if (o instanceof Persistable) {
						sb.append(EnerSysLogUtils.format((Persistable) o));
					} else {
						sb.append(o);
					}
				}
			}
			LOGGER.debug(sb);
		}
	}

	/**
	 * Resets & reloads all Fast-Track Change Activity specific hashsets containing
	 * participants.<br>
	 */
	@Override
	public void reloadFastTrackCNCTMappingFromMatrix() {
		// RESET ALL DATA STRUCTURES - storing ELEMENT INFORMATION
		checkAndWriteDebug(Debuggable.START, "#reloadFastTrackChangeActivityMappingFromMatrix");
		checkAndWriteDebug(Debuggable.LINE, "#reloadFastTrackChangeActivityMappingFromMatrix --> ",
				" changeActivityFastTrackElements: ", changeActivityFastTrackElements);

		if (changeActivityFastTrackElements != null) {
			changeActivityFastTrackElements.clear();
		} else {
			changeActivityFastTrackElements = new HashSet<>();
		}
		final String xpathFastTrackExpressionStr = "/" + EnerSysApprovalMatrixDefinition.HEAD_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.FAST_TRACK_CN_CT_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.PARTICIPANT_TAG;
		try {
			// Fetch and Save Fast-Track Participants
			XPathExpression xpathExpression = xpath.compile(xpathFastTrackExpressionStr);
			NodeList appMatDeviationFastTrackElems = (NodeList) xpathExpression.evaluate(approvalMatrix,
					XPathConstants.NODESET);
			if (appMatDeviationFastTrackElems.getLength() >= 1) {
				for (int i = 0; i < appMatDeviationFastTrackElems.getLength(); ++i) {
					Node deviationParticipantNode = appMatDeviationFastTrackElems.item(i);
					Element deviationParticipantElement = (Element) deviationParticipantNode;
					changeActivityFastTrackElements.add(deviationParticipantElement);
				}
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.LINE, "#reloadFastTrackChangeActivityMappingFromMatrix --> AFT",
					" changeActivityFastTrackElements: ", changeActivityFastTrackElements);
			checkAndWriteDebug(Debuggable.END, "#reloadFastTrackChangeActivityMappingFromMatrix");
		}
	}

	/**
	 * Resets & reloads all Deviation specific hashsets containing participants.<br>
	 */
	@Override
	public void reloadDeviationMappingFromMatrix() {
		// TODO Auto-generated method stub
		// RESET ALL DATA STRUCTURES - storing ELEMENT INFORMATION
		checkAndWriteDebug(Debuggable.START, "#reloadDeviationMappingFromMatrix");
		checkAndWriteDebug(Debuggable.LINE, "#reloadDeviationMappingFromMatrix --> ", " deviationFastTrackElements: ",
				deviationFastTrackElements);
		checkAndWriteDebug(Debuggable.LINE, "#reloadDeviationMappingFromMatrix --> ", " deviationFullTrackElements: ",
				deviationFullTrackElements);

		if (deviationFullTrackElements != null) {
			deviationFullTrackElements.clear();
		} else {
			deviationFullTrackElements = new HashSet<>();
		}
		if (deviationFastTrackElements != null) {
			deviationFastTrackElements.clear();
		} else {
			deviationFastTrackElements = new HashSet<>();
		}
		if (deviationNotificationTeamElements != null) {
			deviationNotificationTeamElements.clear();
		} else {
			deviationNotificationTeamElements = new HashSet<>();
		}

		final String xpathFastTrackExpressionStr = "/" + EnerSysApprovalMatrixDefinition.HEAD_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.DEVIATION_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.DEVIATION_FAST_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.PARTICIPANT_TAG;
		final String xpathFullTrackExpressionStr = "/" + EnerSysApprovalMatrixDefinition.HEAD_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.DEVIATION_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.DEVIATION_FULL_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.PARTICIPANT_TAG;
		final String xpathNotificationExpressionStr = "/" + EnerSysApprovalMatrixDefinition.HEAD_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.DEVIATION_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.NOTIFICATION_TEAM_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.PARTICIPANT_TAG;

		try {
			// Fetch and Save Fast-Track Participants
			XPathExpression xpathExpression = xpath.compile(xpathFastTrackExpressionStr);
			NodeList appMatDeviationFastTrackElems = (NodeList) xpathExpression.evaluate(approvalMatrix,
					XPathConstants.NODESET);
			if (appMatDeviationFastTrackElems.getLength() >= 1) {
				for (int i = 0; i < appMatDeviationFastTrackElems.getLength(); ++i) {
					Node deviationParticipantNode = appMatDeviationFastTrackElems.item(i);
					Element deviationParticipantElement = (Element) deviationParticipantNode;
					deviationFastTrackElements.add(deviationParticipantElement);
				}
			}
			// Fetch and Save Full-Track Participants
			xpathExpression = xpath.compile(xpathFullTrackExpressionStr);
			NodeList appMatDeviationFullTrackElems = (NodeList) xpathExpression.evaluate(approvalMatrix,
					XPathConstants.NODESET);
			if (appMatDeviationFullTrackElems.getLength() >= 1) {
				for (int i = 0; i < appMatDeviationFullTrackElems.getLength(); ++i) {
					Node deviationParticipantNode = appMatDeviationFullTrackElems.item(i);
					Element deviationParticipantElement = (Element) deviationParticipantNode;
					deviationFullTrackElements.add(deviationParticipantElement);
				}
			}

			// Fetch and Save Notification Participants
			xpathExpression = xpath.compile(xpathNotificationExpressionStr);
			NodeList appMatNotificationElems = (NodeList) xpathExpression.evaluate(approvalMatrix,
					XPathConstants.NODESET);
			if (appMatNotificationElems.getLength() >= 1) {
				for (int i = 0; i < appMatNotificationElems.getLength(); ++i) {
					Node deviationParticipantNode = appMatNotificationElems.item(i);
					Element deviationParticipantElement = (Element) deviationParticipantNode;
					deviationNotificationTeamElements.add(deviationParticipantElement);
				}
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.LINE, "#reloadDeviationMappingFromMatrix --> AFT",
					" deviationFastTrackElements: ", deviationFastTrackElements);
			checkAndWriteDebug(Debuggable.LINE, "#reloadDeviationMappingFromMatrix --> AFT",
					" deviationFullTrackElements: ", deviationFullTrackElements);
			checkAndWriteDebug(Debuggable.END, "#reloadDeviationMappingFromMatrix");
		}
	}

	@Override
	public void reloadFirmwareMappingFromMatrix() {
		// TODO Auto-generated method stub

		// RESET ALL DATA STRUCTURES - storing ELEMENT INFORMATION
		checkAndWriteDebug(Debuggable.START, "#reloadFirmwareMappingFromMatrix");
		checkAndWriteDebug(Debuggable.LINE, "#reloadFirmwareMappingFromMatrix --> ", " firmwareCRBElements: ",
				firmwareCRBElements);
		checkAndWriteDebug(Debuggable.LINE, "#reloadFirmwareMappingFromMatrix --> ",
				" firmwareNotificationTeamElements: ", firmwareNotificationTeamElements);

		if (firmwareCRBElements != null) {
			firmwareCRBElements.clear();
		} else {
			firmwareCRBElements = new HashSet<Element>();
		}
		if (firmwareNotificationTeamElements != null) {
			firmwareNotificationTeamElements.clear();
		} else {
			firmwareNotificationTeamElements = new HashSet<Element>();
		}

		final String xpathCRBExpressionStr = "/" + EnerSysApprovalMatrixDefinition.HEAD_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.FIRMWARE_TAG + "/" + EnerSysApprovalMatrixDefinition.FIRMWARE_CRB_TAG
				+ "/" + EnerSysApprovalMatrixDefinition.PARTICIPANT_TAG;

		final String xpathNotificationTeamExpressionStr = "/" + EnerSysApprovalMatrixDefinition.HEAD_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.FIRMWARE_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.NOTIFICATION_TEAM_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.PARTICIPANT_TAG;
		try {
			// Fetch and Save Fast-Track Participants
			XPathExpression xpathExpression = xpath.compile(xpathCRBExpressionStr);
			NodeList appMatFirmwareElems = (NodeList) xpathExpression.evaluate(approvalMatrix, XPathConstants.NODESET);
			if (appMatFirmwareElems.getLength() >= 1) {
				for (int i = 0; i < appMatFirmwareElems.getLength(); ++i) {
					Node notificationParticipantNode = appMatFirmwareElems.item(i);
					Element deviationParticipantElement = (Element) notificationParticipantNode;
					firmwareCRBElements.add(deviationParticipantElement);
				}
			}
			// Fetch and Save Full-Track Participants
			xpathExpression = xpath.compile(xpathNotificationTeamExpressionStr);
			appMatFirmwareElems = (NodeList) xpathExpression.evaluate(approvalMatrix, XPathConstants.NODESET);
			if (appMatFirmwareElems.getLength() >= 1) {
				for (int i = 0; i < appMatFirmwareElems.getLength(); ++i) {
					Node notificationParticipantNode = appMatFirmwareElems.item(i);
					Element deviationParticipantElement = (Element) notificationParticipantNode;
					firmwareNotificationTeamElements.add(deviationParticipantElement);
				}
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.LINE, "#reloadFirmwareMappingFromMatrix --> AFT", " firmwareCRBElements: ",
					firmwareCRBElements);
			checkAndWriteDebug(Debuggable.LINE, "#reloadFirmwareMappingFromMatrix --> AFT",
					" firmwareNotificationTeamElements: ", firmwareNotificationTeamElements);
			checkAndWriteDebug(Debuggable.END, "#reloadFirmwareMappingFromMatrix");
		}
	}

	/**
	 * Generates Participant Information based on "participant" elements mentioned
	 * under Fast-Track Tag for Deviations.
	 */
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getDeviationFastTrackParticipants(WTObject varObj,
			TypeIdentifier pboTI, Set<NmOid> affectedObj) {
		// TODO Auto-generated method stub
		checkAndWriteDebug(Debuggable.START, "#getDeviationFastTrackParticipants");
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> obj = calculateParticipantsForNewDeviationAndNotification(
				pboTI, (WTContained) varObj, deviationFastTrackElements, affectedObj);
		checkAndWriteDebug(Debuggable.END, "#getDeviationFastTrackParticipants");
		return obj;
	}

	/**
	 * Generates Participant Information based on "participant" elements mentioned
	 * under Fast-Track Tag for Deviations.
	 */
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getFastTrackCNCTParticipants(WTObject varObj,
			TypeIdentifier pboTI, Set<NmOid> affectedObj) {
		// TODO Auto-generated method stub
		checkAndWriteDebug(Debuggable.START, "#getFastTrackCTParticipants");
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> obj = calculateParticipantsForNewFastTrackCT(pboTI,
				(WTContained) varObj, changeActivityFastTrackElements, affectedObj);
		checkAndWriteDebug(Debuggable.END, "#getFastTrackCTParticipants");
		return obj;
	}

	private LinkedHashMap<String, LinkedHashMap<String, WTContainer>> calculateParticipantsForNewFastTrackCT(
			TypeIdentifier pboTI, WTContained contained, HashSet<Element> participantElements, Set<NmOid> affectedObj) {
		checkAndWriteDebug(Debuggable.START, "#calculateParticipantsForNewFastTrackCT");

		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap = new LinkedHashMap<>();
		// Get Container name of the Variance, Participants would be from this container
		WTContainer cont = null;
		if (contained instanceof PDMLinkProduct || contained instanceof WTLibrary || contained instanceof Project2) {
			cont = (WTContainer) contained;
		} else {
			cont = contained.getContainer();
		}

		String containerName = contained.getContainerName();
		// JIRA: 518
		Persistable criticalObj = null;
		boolean isCriticalObj = false;
		try {
			if (affectedObj != null) {
				Iterator targetRefIterator = affectedObj.iterator();
				while (targetRefIterator.hasNext()) {
					Persistable per = (Persistable) (((NmOid) targetRefIterator.next()).getRefObject());
					if (EnerSysHelper.service.isCriticalPart(per)) {
						isCriticalObj = true;
						break;
					}
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		processParticipantNodes(participantElements, roleMap, containerName, cont, isCriticalObj, pboTI, "PROTOTYPE");
		checkAndWriteDebug(Debuggable.END, "#calculateParticipantsForNewFastTrackCT");
		return roleMap;
	}

	/**
	 * Calculates Firmware CRB team and returns the CRB Role map.
	 */
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getFirmwareCRBParticipants(WTObject pbo,
			TypeIdentifier pboTI, Set<NmOid> affectedObj) {
		checkAndWriteDebug(Debuggable.START, "#getFirmwareCRBParticipants");
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> obj = calculateParticipantsForNewDeviationAndNotification(
				pboTI, (WTContained) pbo, firmwareCRBElements, affectedObj);
		checkAndWriteDebug(Debuggable.END, "#getFirmwareCRBParticipants");
		return obj;
	}

	/**
	 * Generates Participant Information based on "participant" elements mentioned
	 * under Full-Track Tag for Deviations.
	 */
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getDeviationFullTrackParticipants(WTObject varObj,
			TypeIdentifier pboTI, Set<NmOid> affectedObj) {
		// TODO Auto-generated method stub
		// WorkflowProcessHelper.setVarianceAuthorRole(arg0, arg1);
		// StandardTeamService
		// WorkflowProcessHelper.isRelatedChildrenInStates(paramObject,
		// paramArrayOfString)
		// StandardWorkflowService
		checkAndWriteDebug(Debuggable.START, "#getDeviationFullTrackParticipants");
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> obj = calculateParticipantsForNewDeviationAndNotification(
				pboTI, (WTContained) varObj, deviationFullTrackElements, affectedObj);
		checkAndWriteDebug(Debuggable.END, "#getDeviationFullTrackParticipants");
		return obj;
	}

	/**
	 * 
	 * Generates Participant Information based on "participant" elements passed as
	 * parameter to this method.
	 * 
	 * @param varObj
	 * @param participantElements
	 * @return
	 */
	private LinkedHashMap<String, LinkedHashMap<String, WTContainer>> calculateParticipantsForNewDeviationAndNotification(
			TypeIdentifier pboTI, WTContained contained, HashSet<Element> participantElements, Set<NmOid> affectedObj) {
		checkAndWriteDebug(Debuggable.START, "#calculateParticipantsForNewDeviationAndNotification");

		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap = new LinkedHashMap<>();

		// Get Container name of the Variance, Participants would be from this container
		WTContainer cont = null;
		if (contained instanceof PDMLinkProduct || contained instanceof WTLibrary || contained instanceof Project2) {
			cont = (WTContainer) contained;
		} else {
			cont = contained.getContainer();
		}

		String containerName = contained.getContainerName();
		// JIRA: 518
		Persistable criticalObj = null;
		boolean isCriticalObj = false;
		try {
			if (affectedObj != null) {
				Iterator targetRefIterator = affectedObj.iterator();
				while (targetRefIterator.hasNext()) {
					Persistable per = (Persistable) (((NmOid) targetRefIterator.next()).getRefObject());
					if (EnerSysHelper.service.isCriticalPart(per)) {
						isCriticalObj = true;
						break;
					}
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		processParticipantNodes(participantElements, roleMap, containerName, cont, isCriticalObj, pboTI, "");
		checkAndWriteDebug(Debuggable.END, "#calculateParticipantsForNewDeviationAndNotification");
		return roleMap;
	}

	/**
	 * Returns the Notification Team for a given Primary Business Object.
	 */
	@Override
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getNotificationRoleMap(WTObject pbo) {
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> ret = null;
		if (isFirmwareCRCNCTpbo(pbo)) {
			ret = calculateParticipantsForNewDeviationAndNotification(TypeIdentifierHelper.getType(pbo),
					(WTContained) pbo, firmwareNotificationTeamElements, null);
		} else if (pbo instanceof WTVariance) {
			ret = calculateParticipantsForNewDeviationAndNotification(TypeIdentifierHelper.getType(pbo),
					(WTContained) pbo, deviationNotificationTeamElements, null);
		} else {
			// TODO: add logic for generic notification teams based on affected items
			try {
				ret = getOverallNotificationTeamRoleMap(pbo);
			} catch (WTException e) {
				LOGGER.error("Error in method getNotificationRoleMap  : " + e);
				if (LOGGER.isDebugEnabled()) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	/**
	 * Extracts & returns HashMap with Participant tags from the approval matrix for
	 * defined notification team.
	 * 
	 * @param pbo
	 * @return
	 * @throws WTException
	 */
	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> getOverallNotificationTeamRoleMap(WTObject pbo)
			throws WTException {
		checkAndWriteDebug(Debuggable.START, "#getOverallNotificationTeamRoleMap -->", " pbo: ", pbo);
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap = new LinkedHashMap<String, LinkedHashMap<String, WTContainer>>();
		QueryResult resultingObjects = CM2Helper.service.getAffectedObjects(pbo);
		while (resultingObjects.hasMoreElements()) {
			WTObject obj = (WTObject) resultingObjects.nextElement();
			WTContained contained = (WTContained) obj;

			WTContainer cont = contained.getContainer();
			String containerName = contained.getContainerName();

			NodeList participantNodes = getNotificationTeamNodeListForObject(obj, pbo);
			// Added in v2.1 to reduce code duplication
			processParticipantNodes(participantNodes, roleMap, containerName, cont, false, null, "");
		}
		checkAndWriteDebug(Debuggable.END, "#getOverallNotificationTeamRoleMap");
		if (roleMap.isEmpty()) {
			roleMap = null;
		}
		return roleMap;
	}

	/**
	 * Searches for Notification Team based on PBO type & the Affected Item's Type.
	 * <br>
	 * v1.14
	 * 
	 * @param affectedItem
	 * @param pbo
	 * @return
	 */
	public NodeList getNotificationTeamNodeListForObject(WTObject affectedItem, Object pbo) {
		checkAndWriteDebug(Debuggable.START, "#getNotificationTeamNodeListForObject -->", " affectedItem: ",
				affectedItem, " pbo: ", pbo);
		NodeList participantsList = null;
		try {
			if (affectedItem != null && pbo != null) {
				// Get the TypeIdentifier of the Object
				String subTypeNameOrDefault = TypeIdentifierHelper.getType(affectedItem).getTypeInternalName();

				// String lcState;
				String PBO_TYPE = CM2Helper.service.getChangeTypeString(pbo);

				String PBO_TYPE_TO_SEARCH = getCorrespondingPBOTag(PBO_TYPE);

				// Get PBO Type; If Type = PROMOTION REQUEST then lcState = Maturity State of
				// the PR
				/*
				 * if (PBO_TYPE.equalsIgnoreCase(EnerSysService.ACTIVITY_PROMOTIONREQUEST)) {
				 * PromotionNotice promotionNotice = (PromotionNotice) pbo; lcState =
				 * promotionNotice.getMaturityState().toString(); } else { // Get LC State from
				 * the affectedItem lcState =
				 * EnerSysHelper.service.getObjectState(affectedItem); }
				 */

				// Check if the there are any entries defined for the given type, otherwise, set
				// subTypeNameOrDefault to DEFAULT
				if (!isTypeNodePresent(subTypeNameOrDefault)) {
					subTypeNameOrDefault = EnerSysApprovalMatrixDefinition.DEFAULT_TAG;
				}

				// Check if the Type has the specified CHANGE OBJECT defined in the
				// matrix;Otherwise Use DEFAULT!!!
				if (!isChangeNodeDefinedForType(PBO_TYPE_TO_SEARCH, subTypeNameOrDefault)) {
					PBO_TYPE_TO_SEARCH = EnerSysApprovalMatrixDefinition.DEFAULT_TAG;
				}

				// Check if the CHANGE OBJECT specified under the Type has the STATE defined in
				// the matrix;Otherwise Use DEFAULT!!!
				/*
				 * if (!isStateNodeDefinedForTypeAndChange(lcState, PBO_TYPE_TO_SEARCH,
				 * subTypeNameOrDefault)) { lcState =
				 * EnerSysApprovalMatrixDefinition.DEFAULT_TAG; }
				 */

				// extract the nodes for the given PBO's type based on the Type filter
				participantsList = getNotificationTeamNodeForGivenPBOTagInfoForType(PBO_TYPE_TO_SEARCH,
						subTypeNameOrDefault);
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#getNotificationTeamNodeListForObject -->", " participantsList: ",
				participantsList);
		return participantsList;
	}

	/**
	 * Used internally
	 * 
	 * v1.14
	 * 
	 * @param state
	 * @param pboType
	 * @param intrName
	 * @return
	 */
	private NodeList getNotificationTeamNodeForGivenPBOTagInfoForType(String pboType, String intrName) {
		checkAndWriteDebug(Debuggable.START, "#getNotificationTeamNodeForGivenPBOTagInfoForType -->", " pboType:",
				pboType, " intrName:", intrName);
		NodeList participantlist = null;
		if (checkIfAnyDSIsNull()) {
			loadApprovalMatrixDOMDocument();
		}
		Element elementNode = getNodeForGivenType(intrName, true);
		String xpathExpressionStr = "./" + pboType + "/" + EnerSysApprovalMatrixDefinition.NOTIFICATION_TEAM_TAG + "/"
				+ EnerSysApprovalMatrixDefinition.PARTICIPANT_TAG;
		try {
			XPathExpression xpathExpression = xpath.compile(xpathExpressionStr);
			participantlist = (NodeList) xpathExpression.evaluate(elementNode, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#getNotificationTeamNodeForGivenPBOTagInfoForType -->", " participantlist:",
				participantlist);
		return participantlist;
	}

	/**
	 * @param LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap
	 * @param Set<NmOid>            affectedItemSet
	 * @return LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap
	 *         This method adds the Critical Part Selector role to the existing
	 *         approval matrix
	 */

	public LinkedHashMap<String, LinkedHashMap<String, WTContainer>> additionalRolesAdded(
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap, Set<NmOid> affectedItemSet) {
		LOGGER.debug("additionalRolesAdded Started");
		try {
			Set<Persistable> affectedObjects = convertNmOidSetToPerSet(affectedItemSet);
			if (affectedObjectsConatinsCriticalPart(affectedObjects)) {
				LOGGER.debug("Before Changing Role Map Value : " + roleMap.size());
				newRoleMap.clear();
				int currentOrder = 1;
				if (gettingHashMapEnrtyForRole(CRITICAL_PART_ROLE, roleMap, currentOrder)) {// Added in build V3.10
					LOGGER.debug("New Critical Part Role Moved");
					LOGGER.debug("Current Order Length " + currentOrder);
					rearrangingOrderIntheRoleMap(roleMap, currentOrder);
					LOGGER.debug("additionalRolesAdded Ended");
					return newRoleMap;
				}
			}
		} catch (Exception e) {
			LOGGER.error(
					"The error in Critical Part Role Addition and and changing order : " + e.getLocalizedMessage());
			if (LOGGER.isDebugEnabled()) {
				e.printStackTrace();
			}
		}
		return roleMap;
	}

	/**
	 * 
	 * Added in Build v3.7 - 7599
	 * 
	 * 
	 * This method created in Build v3.7 - 7599 , this method will insert data into
	 * role map of Approval Matrix
	 */

	/**
	 * @param roleValue
	 * @param roleinnerValue
	 * @param container
	 * @param order
	 */
	private void changingEntries(String[] roleValue, String[] roleinnerValue, WTContainer container, int order) {
		String CRITICAL_PART_KEY = roleValue[0] + COLON_ROLE_MAP_DELIM + roleValue[1] + COLON_ROLE_MAP_DELIM + order
				+ COLON_ROLE_MAP_DELIM + roleValue[3];
		LinkedHashMap<String, WTContainer> keyValueContainerMap = new LinkedHashMap<>();
		if (newRoleMap.get(CRITICAL_PART_KEY) == null) {
			String typeInfo = (roleinnerValue.length > 4) ? roleinnerValue[4] : "single";
			String ROLE_TYPE = roleinnerValue[0] + COLON_ROLE_MAP_DELIM + roleinnerValue[1] + COLON_ROLE_MAP_DELIM
					+ roleinnerValue[2] + COLON_ROLE_MAP_DELIM + roleinnerValue[3] + COLON_ROLE_MAP_DELIM + typeInfo;
			newRoleMap.put(CRITICAL_PART_KEY, keyValueContainerMap);
			keyValueContainerMap.computeIfAbsent(ROLE_TYPE, k -> container);
		}
	}

	/**
	 * 
	 * Added in Build v3.7 - 7599
	 * 
	 * @param roleMap
	 * @param roleName
	 * @return boolean
	 * 
	 *         This method created in Build v3.7 - 7599 , this method will insert
	 *         the role in specific order into list of roleMap fetched from approval
	 *         matrix , if it is a critical part , it becomes "REQUIRED".
	 */
	private boolean gettingHashMapEnrtyForRole(String roleName,
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap, int order) {
		LOGGER.debug("The Size of Outer Linked Hash Map " + roleMap.size());
		boolean returnValue = false;
		Iterator<Map.Entry<String, LinkedHashMap<String, WTContainer>>> iterator = roleMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, LinkedHashMap<String, WTContainer>> outerEntry = iterator.next();
			String keyValueWithOrder = outerEntry.getKey();
			String[] roleValue = keyValueWithOrder.split(EnerSysApprovalMatrixDefinition.COLON_ROLE_MAP_DELIM);
			LinkedHashMap<String, WTContainer> innerValueWithContainer = outerEntry.getValue();
			LOGGER.debug("The Size of Inner Linked Hash Map " + innerValueWithContainer.size());
			Iterator<Map.Entry<String, WTContainer>> innerIterator = innerValueWithContainer.entrySet().iterator();
			while (innerIterator.hasNext()) {
				Map.Entry<String, WTContainer> innerEntry = innerIterator.next();
				String innerKey = innerEntry.getKey();
				String[] roleInnerValue = innerKey.split(EnerSysApprovalMatrixDefinition.COLON_ROLE_MAP_DELIM);
				WTContainer container = innerEntry.getValue();
				if (roleName.equals(roleValue[0])) {
					roleValue[1] = "REQUIRED";
					LOGGER.debug("Outer Key: " + keyValueWithOrder + ", Inner Key: " + innerKey + ", WTContainer: "
							+ container + " - Changed Order :" + order);
					changingEntries(roleValue, roleInnerValue, container, order);
					innerIterator.remove();
					returnValue = true;
				}
			}
			if (innerValueWithContainer.isEmpty()) {
				iterator.remove();
			}
		}
		return returnValue;
	}

	/**
	 * 
	 * Added in Build v3.7 - 7599 - This method rearranging the roleMap Values if
	 * any new role is added as Critical Part Selector or move the existing roles
	 * order by the number of roles newly added
	 * 
	 * @param roleMap
	 * @param currentOrder
	 */
	private void rearrangingOrderIntheRoleMap(LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap,
			int currentOrder) {
		LOGGER.debug("Started rearrangingOrderIntheRoleMap Method");
		LOGGER.debug("The Size of Outer Linked Hash Map " + roleMap.size());
		if (roleMap != null && roleMap.size() > 0) {
			for (Map.Entry<String, LinkedHashMap<String, WTContainer>> outerEntry : roleMap.entrySet()) {
				String keyValuewithorder = outerEntry.getKey();
				String[] roleValue = keyValuewithorder.split(EnerSysApprovalMatrixDefinition.COLON_ROLE_MAP_DELIM);
				LinkedHashMap<String, WTContainer> innerValuewithContainer = outerEntry.getValue();
				LOGGER.debug("The Size of Inner Linked Hash Map " + innerValuewithContainer.size());
				// Inner loop: iterate over the entries of the inner LinkedHashMap
				for (Map.Entry<String, WTContainer> innerValue : innerValuewithContainer.entrySet()) {
					String innerKey = innerValue.getKey();
					String[] roleinnerValue = innerKey.split(EnerSysApprovalMatrixDefinition.COLON_ROLE_MAP_DELIM);
					WTContainer container = innerValue.getValue();
					// Do something with outerKey, innerKey, and container
					int orderValue = Integer.parseInt(roleValue[2]) + currentOrder;
					LOGGER.debug("Outer Key: " + keyValuewithorder + ", Inner Key: " + innerKey + ", WTContainer: "
							+ container + " - Changed Order :" + orderValue);
					changingEntries(roleValue, roleinnerValue, container, orderValue);
				}
			}
		}
		LOGGER.debug("Ended rearrangingOrderIntheRoleMap Method");
	}

	/**
	 * 
	 * Added in Build v3.7 - Checking Affected Objects contains the critical part
	 * 
	 * @param roleMap
	 * @return boolean
	 */
	private boolean affectedObjectsConatinsCriticalPart(Set<Persistable> affectedObjects) {
		checkAndWriteDebug(Debuggable.START, "#addingCriticalRoleForCriticalPart -->", " roleMap: ", newRoleMap);
		boolean returnValue = false;
		Iterator resObjItr = affectedObjects.iterator();
		while (resObjItr.hasNext()) {
			Persistable per = (Persistable) resObjItr.next();
			if (EnerSysHelper.service.isCriticalPart(per)) {
				returnValue = true;
				break;
			}
		}
		checkAndWriteDebug(Debuggable.END, "#addingCriticalRoleForCriticalPart -->", " roleMap", newRoleMap);
		return returnValue;
	}

	/**
	 * 
	 * Added in Build v3.8 - Sprint 9 - 8206 - Get The display name from the
	 * Transition RB
	 * 
	 * @param roleMap
	 * @return boolean
	 */
	public static String getDisplayNameForTransition(String internalNameOfTransition) {
		String displayNameOfTransition = null;
		Transition[] TransitionArray = Transition.getTransitionSet();
		for (Transition currentTransition : TransitionArray) {
			String internalValueTransition = currentTransition.toString();
			if (internalNameOfTransition.equals(internalValueTransition)) {
				displayNameOfTransition = currentTransition.getFullDisplay();
				break;
			}
		}
		if (displayNameOfTransition == null) {
			LOGGER.error(
					"The Error in fetching the Display name for the Transition with the internal name (From TransitionRB) : "
							+ internalNameOfTransition);
			throw new NullPointerException(
					"The Error in fetching the Display name for the Transition with the internal name (From TransitionRB)  : "
							+ internalNameOfTransition);
		}
		return displayNameOfTransition;
	}

	/**
	 * 
	 * Added in Build v3.8 - Sprint 9 - 8206 - Get The internal name from the State
	 * RB
	 * 
	 * @param roleMap
	 * @return boolean
	 */
	public static String getInternalNameForState(String displayNameOfState) {
		String stateInternalName = null;
		State[] StateArray = State.getStateSet();
		for (State currentState : StateArray) {
			String displayValueState = currentState.getFullDisplay();
			if (displayNameOfState.equals(displayValueState)) {
				stateInternalName = currentState.toString();
			}
		}
		if (stateInternalName == null) {
			LOGGER.error("The Error in fetching the Internal name for the State with the display name (From StateRB) : "
					+ displayNameOfState);
			throw new NullPointerException(
					"The Error in fetching the Internal name for the State with the display name (From StateRB) : "
							+ displayNameOfState);
		}
		return stateInternalName;
	}

	// Added in build V3.10
	private LinkedHashMap<String, LinkedHashMap<String, WTContainer>> makingDeviationRequiredApproversBasedonState(
			LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMap, Set<NmOid> affectedObj) {
		LinkedHashMap<String, LinkedHashMap<String, WTContainer>> roleMapCopy = new LinkedHashMap<String, LinkedHashMap<String, WTContainer>>();
		roleMapCopy.putAll(roleMap);
		Set<Persistable> affectedObjectsPer = convertNmOidSetToPerSet(affectedObj);
		Iterator<Persistable> targetRefIterator = affectedObjectsPer.iterator();
		HashSet<String> statesAllAffected = new HashSet<String>();
		boolean qualityManagerRoleFlag = false;
		boolean localPlantQualityRoleFlag = false;
		while (targetRefIterator.hasNext()) {
			Persistable per = targetRefIterator.next();
			if (per instanceof WTObject) {
				WTObject affectedItem = (WTObject) per;
				String lcState = EnerSysHelper.service.getObjectState(affectedItem);
				if (lcState != null && !lcState.isEmpty()) {
					statesAllAffected.add(lcState);
					if (lcState.equals("PRODUCTIONRELEASED")) {
						localPlantQualityRoleFlag = true;
					} else if (lcState.contains("RELEASE")) {
						qualityManagerRoleFlag = true;
					}
				}
			}
		}
		for (Entry<String, LinkedHashMap<String, WTContainer>> o : roleMapCopy.entrySet()) {
			String currentKeyValue = o.getKey();
			String replacementKeyValue = currentKeyValue;
			LinkedHashMap<String, WTContainer> replacementValues = o.getValue();
			String[] allRoleValues = currentKeyValue.split(COLON_ROLE_MAP_DELIM);
			boolean updated = false;
			if (allRoleValues.length > 2) {
				String currentRole = allRoleValues[0];
				if (currentRole.equals(QUALITY_MANAGER_ROLE) && qualityManagerRoleFlag) {
					allRoleValues[1] = "REQUIRED";
					updated = true;
				}
				if (currentRole.equals(LOCAL_PLANT_QUALITY_ROLE) && localPlantQualityRoleFlag) {
					allRoleValues[1] = "REQUIRED";
					updated = true;
				}
				if (updated) {
					replacementKeyValue = String.join(COLON_ROLE_MAP_DELIM, allRoleValues);
					roleMap.remove(currentKeyValue);
					roleMap.put(replacementKeyValue, replacementValues);
				}
			}
		}
		return roleMap;
	}

}
