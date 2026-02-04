package ext.enersys.service;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.logging.log4j.Logger;

import com.ptc.core.businessRules.engine.BusinessRuleSetBean;
import com.ptc.core.businessRules.validation.RuleValidationResultSet;
import com.ptc.core.businessRules.validation.RuleValidationStatus;
import com.ptc.core.businessfield.common.BusinessField;
import com.ptc.core.businessfield.common.BusinessFieldIdFactoryHelper;
import com.ptc.core.businessfield.common.BusinessFieldServiceHelper;
import com.ptc.core.businessfield.server.BusinessFieldIdentifier;
import com.ptc.core.lwc.common.BaseDefinitionService;
import com.ptc.core.lwc.common.EnumerationDefinitionSystemState;
import com.ptc.core.lwc.common.view.AttributeDefaultValueReadView;
import com.ptc.core.lwc.common.view.AttributeDefinitionReadView;
import com.ptc.core.lwc.common.view.EnumerationDefinitionReadView;
import com.ptc.core.lwc.common.view.TypeDefinitionReadView;
import com.ptc.core.lwc.server.LWCEnumerationEntryValuesFactory;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;
import com.ptc.core.meta.common.CreateOperationIdentifier;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.EnumeratedSet;
import com.ptc.core.meta.common.EnumerationEntryIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.core.meta.common.UpdateOperationIdentifier;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.validation.ValidationResult;
import com.ptc.core.validation.ValidationStatus;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.suma.axl.AXLContext;
import com.ptc.windchill.suma.axl.AXLEntry;
import com.ptc.windchill.suma.axl.AXLHelper;
import com.ptc.windchill.suma.axl.AXLPreference;
import com.ptc.windchill.suma.part.ManufacturerPart;
import com.ptc.windchill.uwgm.common.container.OrganizationHelper;
import com.ptc.windchill.uwgm.common.pdm.retriever.RevisionIterationInfoHelper;

import ext.enersys.cm4.CM4ServiceUtility;
import ext.enersys.cm4.service.CM4Service;
import ext.enersys.poc.utility.properties.ExtractorPropertyHelper;
import ext.enersys.utilities.Debuggable;
import ext.enersys.utilities.EnerSysLogUtils;
import ext.enersys.utilities.EnerSysSoftTypeHelper;
import wt.businessRules.BusinessRulesHelper;
import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeIssue;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeRequest2;
import wt.change2.WTVariance;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentItem;
import wt.content.ContentRoleType;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.enterprise.RevisionControlled;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentMaster;
import wt.epm.EPMDocumentType;
import wt.fc.ObjectIdentifier;
import wt.fc.PersistInfo;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.filter.NavigationCriteria;
import wt.iba.value.DefaultAttributeContainer;
import wt.iba.value.IBAHolder;
import wt.iba.value.service.IBAValueDBService;
import wt.inf.container.OrgContainer;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.inf.team.ContainerTeam;
import wt.inf.team.ContainerTeamHelper;
import wt.inf.team.ContainerTeamManaged;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.State;
import wt.lifecycle.Transition;
import wt.log4j.LogR;
import wt.maturity.PromotionNotice;
import wt.meta.LocalizedValues;
import wt.org.OrganizationServicesHelper;
import wt.org.WTGroup;
import wt.org.WTOrganization;
import wt.org.WTPrincipal;
import wt.org.WTUser;
import wt.part.WTPart;
import wt.part.WTPartConfigSpec;
import wt.part.WTPartMaster;
import wt.part.WTPartStandardConfigSpec;
import wt.pds.StatementSpec;
import wt.pom.Transaction;
import wt.preference.PreferenceClient;
import wt.preference.PreferenceHelper;
import wt.query.ClassAttribute;
import wt.query.QueryException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.services.ServiceFactory;
import wt.services.StandardManager;
import wt.session.SessionContext;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.type.TypedUtility;
import wt.util.WTException;
import wt.util.WTProperties;
import wt.util.WTPropertyVetoException;
import wt.vc.Iterated;
import wt.vc.Mastered;
import wt.vc.VersionControlHelper;
import wt.vc.baseline.ManagedBaseline;
import wt.vc.config.ConfigHelper;
import wt.vc.config.ConfigSpec;
import wt.vc.config.LatestConfigSpec;
import wt.vc.views.View;
import wt.vc.views.ViewHelper;

public class StandardEnerSysService extends StandardManager implements Serializable, EnerSysService, Debuggable {

	private static final long serialVersionUID = 4855774077047893555L;
	private static final String CLASSNAME = StandardEnerSysService.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(StandardEnerSysService.class.getName());
	private static final String RESTIRTED_ATRIRBUTE_INTERNAL_NAME = "ext.enersys.IS_RESTRICTED_CONTAINER";
	private static final String RESTRICTED_CONTEXT_AUTHORIZED_CONTAINERS_INTRNAL_NAME = "ext.enersys.AUTHORIZED_CONTAINERS";
	private static final String PERSIST_KEY_ID = Persistable.PERSIST_INFO + "." + PersistInfo.OBJECT_IDENTIFIER + "."
			+ ObjectIdentifier.ID;

	private final Set<String> releasedStateSet = new HashSet<>();
	private final Set<String> releasedStateSetMinusA = new HashSet<>();
	private final Set<String> releasedOrObsoleteStateSetMinusA = new HashSet<>();

	// Build v2.3 addition
	private final HashSet<String> cadPartDocTypesIntrNames = new HashSet<>();

	// Build v2.6 addition
	private final HashSet<String> cadAsmDocTypesIntrNames = new HashSet<>();

	public Set<String> getSetOfReleasedState() {
		return Collections.unmodifiableSet(releasedStateSet);
	}

	private StandardEnerSysService() {
		// Add the States
		releasedStateSet.add(STATE_PRODUCTION_RELEASED);
		releasedStateSet.add(STATE_C_RELEASE_CONCEPT);
		releasedStateSet.add(STATE_B_RELEASE_CONCEPT);
		releasedStateSet.add(STATE_A_RELEASE_CONCEPT);
		releasedStateSet.add(STATE_PROTOTYPE_C);
		releasedStateSet.add(STATE_PROTOTYPE_B);
		releasedStateSet.add(STATE_PROTOTYPE_A);
		releasedStateSet.add(STATE_RELEASED);

		// Added ERP Specific States to Check
		releasedStateSetMinusA.add(STATE_PRODUCTION_RELEASED);
		releasedStateSetMinusA.add(STATE_C_RELEASE_CONCEPT);
		releasedStateSetMinusA.add(STATE_B_RELEASE_CONCEPT);
		releasedStateSetMinusA.add(STATE_PROTOTYPE_C);
		releasedStateSetMinusA.add(STATE_PROTOTYPE_B);
		releasedStateSetMinusA.add(STATE_RELEASED);

		// Added Specific States to Check for Flat-File Report
		releasedOrObsoleteStateSetMinusA.add(STATE_PRODUCTION_RELEASED);
		releasedOrObsoleteStateSetMinusA.add(STATE_C_RELEASE_CONCEPT);
		releasedOrObsoleteStateSetMinusA.add(STATE_B_RELEASE_CONCEPT);
		releasedOrObsoleteStateSetMinusA.add(STATE_PROTOTYPE_C);
		releasedOrObsoleteStateSetMinusA.add(STATE_PROTOTYPE_B);
		releasedOrObsoleteStateSetMinusA.add(STATE_RELEASED);
		releasedOrObsoleteStateSetMinusA.add(STATE_OBSOLETE);

		// Build v2.3 additions - Add CAD Types
		cadPartDocTypesIntrNames.add("CADCOMPONENT");
		cadPartDocTypesIntrNames.add("CADASSEMBLY");
		// Build v2.4 addition - Add ASM Type
		cadAsmDocTypesIntrNames.add("CADASSEMBLY");
	}

	public static StandardEnerSysService newStandardEnerSysService() throws WTException {
		StandardEnerSysService service = new StandardEnerSysService();
		service.initialize();
		return service;
	}

	@Override
	public boolean isStringNumerical(String str) {
		checkAndWriteDebug(Debuggable.START, "#isStringNumerical -->", " str: ", str);
		boolean retBool = false;
		try {
			Double.parseDouble(str);
			retBool = true;
			return retBool;
		} catch (NumberFormatException e) {
			return retBool;
		} finally {
			checkAndWriteDebug(Debuggable.END, "#isStringNumerical -->", " retBool: ", retBool);
		}
	}

	/**
	 * 
	 * @param currentUser
	 * @return
	 * @throws WTException
	 */
	@Override
	public boolean isSiteAdmin(WTPrincipal currentUser) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#isSiteAdmin -->", " currentUser: ", currentUser);
		boolean retBool = WTContainerHelper.service.isAdministrator(WTContainerHelper.service.getExchangeRef(),
				currentUser);
		checkAndWriteDebug(Debuggable.END, "#isSiteAdmin -->", " retBool: ", retBool);
		return retBool;
	}

	@Override
	public boolean isOrgAdmin(WTPrincipal currentUser) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#isOrgAdmin -->", " currentUser: ", currentUser);
		WTOrganization org = OrganizationServicesHelper.manager.getOrganization(currentUser);
		WTContainerRef orgContainerRef = WTContainerHelper.service.getOrgContainerRef(org);
		boolean isOrgAdmin = false;
		if (orgContainerRef != null) {
			isOrgAdmin = WTContainerHelper.service.isAdministrator(orgContainerRef, currentUser);
		}
		checkAndWriteDebug(Debuggable.END, "#isOrgAdmin -->", " isOrgAdmin: ", isOrgAdmin);
		return isOrgAdmin;
	}

	@Override
	public void fetchUsersRecursivelyFromGroup(WTGroup group, WTHashSet usrSet) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#fetchUsersRecursivelyFromGroup -->", " group: ", group, " usrSet: ",
				usrSet);
		if (group != null) {
			Enumeration<?> en = group.members();
			while (en.hasMoreElements()) {
				Object nxtElm = en.nextElement();
				if (nxtElm instanceof WTUser) {
					usrSet.add(nxtElm);
				} else if (nxtElm instanceof WTGroup) {
					fetchUsersRecursivelyFromGroup((WTGroup) nxtElm, usrSet);
				}
			}
		}
		checkAndWriteDebug(Debuggable.END, "#fetchUsersRecursivelyFromGroup");
	}

	/**
	 * 
	 * Find the set of users belonging to a particular Team Role from a particular
	 * context.
	 * 
	 */
	@Override
	public WTHashSet fetchUsersInTeamRole(Object role, WTObject someContextObject) {
		checkAndWriteDebug(Debuggable.START, "#fetchUsersInTeamRole -->", " role: ", role, " someContextObject: ",
				someContextObject);
		WTHashSet usrSet = new WTHashSet();

		try {
			if (someContextObject instanceof WTContained) {
				WTContained contained = (WTContained) someContextObject;
				WTContainer cont = contained.getContainer();
				Enumeration<?> containerUsers = null;
				if (cont instanceof ContainerTeamManaged) {
					ContainerTeam containerTeam = ContainerTeamHelper.service
							.getContainerTeam((ContainerTeamManaged) cont);
					WTGroup roleGroup = ContainerTeamHelper.service.findContainerTeamGroup(containerTeam, "roleGroups",
							role.toString());

					// WRITE LOGGER DEBUG LINES SEPARATELY
					checkAndWriteDebug(Debuggable.LINE, "#fetchUsersInTeamRole -->", " Contained Info: ", contained);
					checkAndWriteDebug(Debuggable.LINE, "#fetchUsersInTeamRole -->", " Container Info: ", cont);
					checkAndWriteDebug(Debuggable.LINE, "#fetchUsersInTeamRole -->", " ContainerTeam Info: ",
							containerTeam);
					checkAndWriteDebug(Debuggable.LINE, "#fetchUsersInTeamRole -->", " roleGroup Info : ", roleGroup);

					if (roleGroup != null) {
						containerUsers = OrganizationServicesHelper.manager.members(roleGroup, false);
						Object nxtElm = null;
						while (containerUsers.hasMoreElements()) {
							nxtElm = containerUsers.nextElement();
							if (nxtElm instanceof WTUser) {
								WTUser tmp = (WTUser) nxtElm;
								if (!usrSet.contains(tmp)) {
									usrSet.add(tmp);
								}
							} else if (nxtElm instanceof WTGroup) {
								WTGroup group = (WTGroup) nxtElm;
								fetchUsersRecursivelyFromGroup(group, usrSet);
							}
						}
					}
				} else {
					// Object is not in PRODUCT/LIBRARY/PROJECT; its in SITE or ORG Level
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#fetchUsersInTeamRole -->", " usrSet: ", usrSet);
		}
		return usrSet;
	}

	/**
	 * Find if a user belongs to a Role defined in current context.
	 * 
	 * @param role
	 * @param wtObject
	 * @param userStr
	 * @return
	 * 
	 */
	@Override
	public boolean isUserInCurrentContextRole(Object role, WTObject someContextObj, String userStr) {
		checkAndWriteDebug(Debuggable.START, "#isUserInCurrentContextRole -->", " role: ", role, " someContextObj: ",
				someContextObj, " userStr: ", userStr);
		boolean retBool = false;
		try {
			WTHashSet usrSet = fetchUsersInTeamRole(role, someContextObj);
			if (usrSet != null && usrSet.size() > 0) {
				Iterator<?> itr = usrSet.persistableIterator();
				while (itr.hasNext()) {
					WTUser usr = (WTUser) itr.next();
					if (usr.getName().equals(userStr)) {
						retBool = true;
						return retBool;
					}
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#isUserInCurrentContextRole -->", " retBool: ", retBool);
		}
		return false;
	}

	/**
	 * Logic to generate a Navigation criteria based on default config specs &
	 * settings.<br>
	 * "DESIGN" view is selected as default<br>
	 * setWorkingIncluded --> False <br>
	 * "LatestConfigSpec" is also used<br>
	 * 
	 * @return NavigationCriteria
	 */
	public NavigationCriteria generateDefaultNavCriteria() {
		checkAndWriteDebug(Debuggable.START, "#generateDefaultNavCriteria");
		NavigationCriteria retNC = null;
		retNC = new NavigationCriteria();
		try {
			View view = ViewHelper.service.getView("Design");
			retNC.setApplyToTopLevelObject(true);
			retNC.setApplicableType("wt.part.WTPart");

			LatestConfigSpec latestConfigSpec = new LatestConfigSpec();

			List<ConfigSpec> configSpecList = new ArrayList<ConfigSpec>();
			WTPartStandardConfigSpec wtPartViewConfigSpec = WTPartStandardConfigSpec.newWTPartStandardConfigSpec();
			wtPartViewConfigSpec.setWorkingIncluded(false);
			wtPartViewConfigSpec.setView(view);
			configSpecList.add(wtPartViewConfigSpec);
			configSpecList.add(latestConfigSpec);
			retNC.setConfigSpecs(configSpecList);
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#generateDefaultNavCriteria -->", " retNC: " + retNC);
		}

		return retNC;
	}

	// BUILD V1.3 for Navigation Criteria selection by User

	@Override
	public NavigationCriteria getNavigationCriteriaByID(String id) {
		checkAndWriteDebug(Debuggable.START, "#getNavigationCriteriaByID -->", " id: ", id);
		NavigationCriteria retNC = null;
		try {
			if (id != null && !id.isEmpty()) {
				QuerySpec ncQS = new QuerySpec(NavigationCriteria.class);
				SearchCondition ncNameSC = new SearchCondition(NavigationCriteria.class, PERSIST_KEY_ID,
						SearchCondition.EQUAL, Long.parseLong(id));
				ncQS.appendWhere(ncNameSC, new int[] { 0 });

				QueryResult ncQR = PersistenceHelper.manager.find((StatementSpec) ncQS);
				if (ncQR.hasMoreElements()) {
					retNC = (NavigationCriteria) ncQR.nextElement();
				}
			}
		} catch (QueryException qe) {
			qe.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#getNavigationCriteriaByID -->", " retNC: ", retNC);
		}
		return retNC;
	}

	/**
	 * Used by JSP
	 */
	@Override
	public NavigationCriteria getNavigationCriteriaByName(String ncName) {
		checkAndWriteDebug(Debuggable.START, "#getAssociatedNavigationCriteria -->", " ncName: ", ncName);
		NavigationCriteria retNC = null;
		try {
			if (ncName != null && !ncName.isEmpty()) {
				QuerySpec ncQS = new QuerySpec(NavigationCriteria.class);
				SearchCondition ncNameSC = new SearchCondition(NavigationCriteria.class, NavigationCriteria.NAME,
						SearchCondition.EQUAL, ncName);
				ncQS.appendWhere(ncNameSC, new int[] { 0 });

				QueryResult ncQR = PersistenceHelper.manager.find((StatementSpec) ncQS);
				if (ncQR.hasMoreElements()) {
					retNC = (NavigationCriteria) ncQR.nextElement();
				}
			}
		} catch (QueryException qe) {
			qe.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#getAssociatedNavigationCriteria -->", " retNC: ", retNC);
		}
		return retNC;
	}

	/**
	 * Used by JSP
	 */
	@Override
	public Map<String, String> getAssociatedNavigationCriteria(WTPrincipal usr) {
		checkAndWriteDebug(Debuggable.START, "#getAssociatedNavigationCriteria -->", " usr: ", usr);
		HashMap<String, String> retMap = new HashMap<>();

		try {
			QuerySpec ncQS = new QuerySpec();
			int classIndex = ncQS.appendClassList(NavigationCriteria.class, false);

			// Query Settings
			ncQS.setAdvancedQueryEnabled(true);
			ncQS.setDistinct(true);

			// Select Conditions
			ncQS.appendSelect(new ClassAttribute(NavigationCriteria.class, NavigationCriteria.NAME),
					new int[] { classIndex }, false);
			ncQS.appendSelect(new ClassAttribute(NavigationCriteria.class, PERSIST_KEY_ID), new int[] { classIndex },
					false);

			// Search Conditions
			SearchCondition ncOwnerSC = new SearchCondition(NavigationCriteria.class, "ownerReference.key.id",
					SearchCondition.EQUAL, usr.getPersistInfo().getObjectIdentifier().getId());
			SearchCondition ncSharedSC = new SearchCondition(NavigationCriteria.class, NavigationCriteria.SHARED_TO_ALL,
					SearchCondition.IS_TRUE);
			SearchCondition ncApplicableTypeSC = new SearchCondition(NavigationCriteria.class,
					NavigationCriteria.APPLICABLE_TYPE, SearchCondition.EQUAL, "wt.part.WTPart");

			// Where Conditions
			ncQS.appendOpenParen();
			ncQS.appendWhere(ncOwnerSC, new int[] { classIndex });
			ncQS.appendOr();
			ncQS.appendWhere(ncSharedSC, new int[] { classIndex });
			ncQS.appendCloseParen();
			ncQS.appendAnd();
			ncQS.appendWhere(ncApplicableTypeSC, new int[] { classIndex });

			QueryResult ncQR = PersistenceHelper.manager.find((StatementSpec) ncQS);
			while (ncQR.hasMoreElements()) {
				Object[] row = (Object[]) ncQR.nextElement();
				String name = (String) row[0];
				BigDecimal id = (BigDecimal) row[1];
				retMap.put(id.toString(), name);
			}
		} catch (QueryException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#getAssociatedNavigationCriteria -->", " retMap: ", retMap);
		return retMap;
	}

	/**
	 * Retrieves the Life-Cycle State of the Object.
	 */
	@Override
	public String getObjectState(WTObject obj) {
		checkAndWriteDebug(Debuggable.START, "#getObjectState -->", " obj: ", obj);
		String retStr = "";
		if (obj instanceof LifeCycleManaged) {
			retStr = ((LifeCycleManaged) obj).getLifeCycleState().toString();
		}
		checkAndWriteDebug(Debuggable.END, "#getObjectState -->", " retStr: ", retStr);
		return retStr;
	}

	@Override
	public String getObjectDisplayState(WTObject obj) {
		checkAndWriteDebug(Debuggable.START, "#getObjectDisplayState -->", " obj: ", obj);
		String retStr = "";
		if (obj instanceof LifeCycleManaged) {
			retStr = ((LifeCycleManaged) obj).getLifeCycleState().getDisplay();
		}
		checkAndWriteDebug(Debuggable.END, "#getObjectDisplayState -->", " retStr: ", retStr);
		return retStr;
	}

	/**
	 * Returns the Number Information otherwise returns null.
	 */
	@Override
	public String getNumberInformation(Persistable perObj) {
		String num = null;
		if (perObj != null) {
			if (perObj instanceof EPMDocument) {
				EPMDocument obj = (EPMDocument) perObj;
				num = obj.getNumber();
			} else if (perObj instanceof WTPart) {
				WTPart obj = (WTPart) perObj;
				num = obj.getNumber();
			} else if (perObj instanceof WTDocument) {
				WTDocument obj = (WTDocument) perObj;
				num = obj.getNumber();
			} else if (perObj instanceof WTPartMaster) {
				WTPartMaster obj = (WTPartMaster) perObj;
				num = obj.getNumber();
			} else if (perObj instanceof EPMDocumentMaster) {
				EPMDocumentMaster obj = (EPMDocumentMaster) perObj;
				num = obj.getNumber();
			} else if (perObj instanceof WTDocumentMaster) {
				WTDocumentMaster obj = (WTDocumentMaster) perObj;
				num = obj.getNumber();
			} else if (perObj instanceof WTChangeIssue) {
				WTChangeIssue obj = (WTChangeIssue) perObj;
				num = obj.getNumber();
			} else if (perObj instanceof WTChangeRequest2) {
				WTChangeRequest2 obj = (WTChangeRequest2) perObj;
				num = obj.getNumber();
			} else if (perObj instanceof WTChangeActivity2) {
				WTChangeActivity2 obj = (WTChangeActivity2) perObj;
				num = obj.getNumber();
			} else if (perObj instanceof ManagedBaseline) {
				ManagedBaseline obj = (ManagedBaseline) perObj;
				num = obj.getNumber();
			}
			// Added in Build V 3.8
			else if (perObj instanceof WTChangeOrder2) {
				WTChangeOrder2 obj = (WTChangeOrder2) perObj;
				num = obj.getNumber();
			} else if (perObj instanceof PromotionNotice) {
				PromotionNotice obj = (PromotionNotice) perObj;
				num = obj.getNumber();
			}
		}
		return num;
	}

	@Override
	public WTOrganization getEnerSysOrgContainer() {
		WTOrganization org = null;
		try {
			org = OrganizationHelper.getOrganizationByName("enersys");
			if (org == null) {
				QuerySpec qs = new QuerySpec(WTOrganization.class);
				qs.appendWhere(new SearchCondition(WTOrganization.class, WTOrganization.NAME, SearchCondition.EQUAL,
						"enersys"), new int[] { 0 });
				QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
				if (qr.hasMoreElements()) {
					org = (WTOrganization) qr.nextElement();
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return org;
	}

	/**
	 * Returns the Version Information otherwise returns null.
	 */
	@Override
	public String getVersionInformation(Persistable perObj) {
		String ver = null;
		if (perObj != null && perObj instanceof Iterated) {
			ver = RevisionIterationInfoHelper.displayInfo((Iterated) perObj);
		}
		return ver;
	}

	/**
	 * Returns the Iteration Information otherwise returns null.
	 */
	@Override
	public String getIterationInformation(Persistable perObj) {
		String itr = null;
		if (perObj != null && perObj instanceof Iterated) {
			itr = ((Iterated) perObj).getIterationIdentifier().getValue();

		}
		return itr;
	}

	@Override
	public void setSessionPrincipal(String principalName) {
		checkAndWriteDebug(Debuggable.START, "#setSessionPrincipal -->", " principalName: ", principalName);
		try {
			if (SessionHelper.manager.getAdministrator().getName().equalsIgnoreCase(principalName))
				SessionHelper.manager.setAdministrator();
			else
				SessionHelper.manager.setPrincipal(principalName);
		} catch (WTException wte) {
			wte.printStackTrace();
		} finally {
			checkAndWriteDebug(Debuggable.END, "#setSessionPrincipal ");
		}
	}

	@Override
	public void setState(Object obj, String toState) throws WTException {
		checkAndWriteDebug(Debuggable.START, "#setState -->", " Object Info: ", obj, " toState: ", toState);
		try {
			if (obj instanceof LifeCycleManaged) {
				String principalName = SessionHelper.getPrincipal().getName();
				setSessionPrincipal(SessionHelper.manager.getAdministrator().getName());
				LifeCycleHelper.service.setLifeCycleState((LifeCycleManaged) obj, State.toState(toState));
				setSessionPrincipal(principalName);
			}
		} catch (WTException exception) {
			throw new WTException(exception);
		} finally {
			checkAndWriteDebug(Debuggable.END, "#setState");
		}
	}

	@Override
	public String getName(WTObject o) throws WTException {
		String retStr = "";
		if (o instanceof RevisionControlled) {
			retStr = ((RevisionControlled) o).getName();
		} else {
			retStr = "UNKNOWN OBJECT (NAME NOT FOUND) : " + o + "\t" + TypeIdentifierHelper.getType(o);
		}
		return retStr;
	}

	@Override
	public String getNumber(WTObject o) throws WTException {
		String retStr = "";
		if (o instanceof WTPart) {
			retStr = ((WTPart) o).getNumber();
		} else if (o instanceof EPMDocument) {
			retStr = ((EPMDocument) o).getNumber();
		} else if (o instanceof WTDocument) {
			retStr = ((WTDocument) o).getNumber();
		} else if (o instanceof WTChangeIssue) {
			retStr = ((WTChangeIssue) o).getNumber();
		} else if (o instanceof WTChangeRequest2) {
			retStr = ((WTChangeRequest2) o).getNumber();
		} else if (o instanceof WTChangeOrder2) {
			retStr = ((WTChangeOrder2) o).getNumber();
		} else if (o instanceof WTChangeActivity2) {
			retStr = ((WTChangeActivity2) o).getNumber();
		} else if (o instanceof ManagedBaseline) {
			retStr = ((ManagedBaseline) o).getNumber();
		} else if (o instanceof WTPartMaster) {
			retStr = ((WTPartMaster) o).getNumber();
		} else if (o instanceof EPMDocumentMaster) {
			retStr = ((EPMDocumentMaster) o).getNumber();
		} else if (o instanceof WTDocumentMaster) {
			retStr = ((WTDocumentMaster) o).getNumber();
		} else {
			retStr = "UNKNOWN OBJECT (NUMBER NOT FOUND) : " + o + "\t" + TypeIdentifierHelper.getType(o);
		}
		return retStr;
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

	@Override
	public WTDocument getLatestDocumentIfExistingWithNumber(String WTDOCUMENT_NUMBER) {
		try {
			QuerySpec querySpec = new QuerySpec(WTDocumentMaster.class);
			querySpec.appendWhere(new SearchCondition(WTDocumentMaster.class, WTDocumentMaster.NUMBER,
					SearchCondition.EQUAL, WTDOCUMENT_NUMBER), new int[] { 0 });
			QueryResult qr = PersistenceHelper.manager.find(querySpec);

			if (qr.hasMoreElements()) {
				WTDocumentMaster object = (WTDocumentMaster) qr.nextElement();
				QueryResult qr2 = ConfigHelper.service.filteredIterationsOf(object, new LatestConfigSpec());
				return qr2.hasMoreElements() ? (WTDocument) qr2.nextElement() : null;
			}

		} catch (QueryException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the latest WTPart with given NUMBER & VIEW
	 * 
	 * @param WTPART_NUMBER
	 * @param viewName
	 * @return
	 */
	@Override
	public WTPart getLatestPartIfExistingWithNumberAndView(String WTPART_NUMBER, String viewName) {
		try {
			QuerySpec querySpec = new QuerySpec(WTPartMaster.class);
			querySpec.appendWhere(
					new SearchCondition(WTPartMaster.class, WTPartMaster.NUMBER, SearchCondition.EQUAL, WTPART_NUMBER),
					new int[] { 0 });

			WTPartStandardConfigSpec stdSpec = WTPartStandardConfigSpec.newWTPartStandardConfigSpec();
			View view = ViewHelper.service.getView(viewName);
			stdSpec.setView(view);
			WTPartConfigSpec configSpec = WTPartConfigSpec.newWTPartConfigSpec(stdSpec);

			QueryResult qr = PersistenceHelper.manager.find(querySpec);

			if (qr.hasMoreElements()) {
				WTPartMaster object = (WTPartMaster) qr.nextElement();
				QueryResult qr2 = ConfigHelper.service.filteredIterationsOf(object, configSpec);
				return qr2.hasMoreElements() ? (WTPart) qr2.nextElement() : null;
			}

		} catch (QueryException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Do not use it instead, inverse the result of {@link #isReleasedState(String)}
	 * 
	 */
	@Deprecated
	@Override
	public boolean isNonReleasedState(String stateToCheck) {
		if (stateToCheck != null && !stateToCheck.isEmpty()
				&& (stateToCheck.equalsIgnoreCase(STATE_INWORK) || stateToCheck.equalsIgnoreCase(STATE_DRAFT)
						|| stateToCheck.equalsIgnoreCase(STATE_B_INWORK)
						|| stateToCheck.equalsIgnoreCase(STATE_C_INWORK)
						|| stateToCheck.equalsIgnoreCase(STATE_PRODUCTION_INWORK))) {
			return true;
		}
		return false;
	}

	/**
	 * Retrieves the Navigation criteria based on the filter-name given<br>
	 * in the ORG level preference
	 * {@link EnerSysService#SMD_UTILITY_RELEASED_FILTER_ORG_PREF_KEY}.
	 */
	@Override
	public NavigationCriteria getSMDUtilityReleasedFilter() {
		checkAndWriteDebug(Debuggable.START, "#getSMDUtilityReleasedFilter");
		NavigationCriteria ret = getOrgFilterPreferenceAndReturnNC(
				EnerSysService.SMD_UTILITY_RELEASED_FILTER_ORG_PREF_KEY);
		checkAndWriteDebug(Debuggable.END, "#getSMDUtilityReleasedFilter");
		return ret;
	}

	/**
	 * Retrieves the Navigation criteria based on the filter-name given<br>
	 * in the ORG level preference
	 * {@link EnerSysService#BOM_UTILITY_RELEASED_FILTER_ORG_PREF_KEY}.
	 */
	@Override
	public NavigationCriteria getBOMUtilityReleasedFilter() {
		checkAndWriteDebug(Debuggable.START, "#getSMDUtilityReleasedFilter");
		NavigationCriteria ret = getOrgFilterPreferenceAndReturnNC(
				EnerSysService.BOM_UTILITY_RELEASED_FILTER_ORG_PREF_KEY);
		checkAndWriteDebug(Debuggable.END, "#getSMDUtilityReleasedFilter");
		return ret;
	}

	/**
	 * Retrieve Navigation Criteria objects<br>
	 * based on a filter-name specified in a ORG level preference.<br>
	 * <br>
	 * It is a Private method, and is used inside the service.<br>
	 * Primarily, used by the below public visible methods: <br>
	 * {@link #getSMDUtilityReleasedFilter()} &
	 * {@link #getBOMUtilityReleasedFilter()}
	 * 
	 * @param prefKey
	 * @return
	 */
	private NavigationCriteria getOrgFilterPreferenceAndReturnNC(String prefKey) {
		NavigationCriteria ret = null;
		try {
			WTOrganization org = getEnerSysOrgContainer();
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);
			// 1. Get value from ORG level preference.
			String preferenceValue = (String) PreferenceHelper.service.getValue(prefKey,
					PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);
			// 2. Check & Get if the Filter by name otherwise return null.
			if (preferenceValue != null && !preferenceValue.trim().isEmpty()) {
				ret = getNavigationCriteriaByName(preferenceValue);
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * Check if the state's internal name matches any of the identified valid
	 * EnerSys Released states. (shown below)<br>
	 * <ul>
	 * <li>{@link EnerSysService#STATE_PRODUCTION_RELEASED}</li>
	 * <li>{@link EnerSysService#STATE_C_RELEASE_CONCEPT}</li>
	 * <li>{@link EnerSysService#STATE_B_RELEASE_CONCEPT}</li>
	 * <li>{@link EnerSysService#STATE_A_RELEASE_CONCEPT}</li>
	 * <li>{@link EnerSysService#STATE_PROTOTYPE_C}</li>
	 * <li>{@link EnerSysService#STATE_PROTOTYPE_B}</li>
	 * <li>{@link EnerSysService#STATE_PROTOTYPE_A}</li>
	 * <li>{@link EnerSysService#STATE_RELEASED}</li>
	 * </ul>
	 */
	@Override
	public boolean isReleasedState(String stateToCheck) {
		// stateToCheck.equalsIgnoreCase(STATE_PRODUCTION_RELEASED) ||
		// stateToCheck.equalsIgnoreCase(STATE_C_RELEASE_CONCEPT) ||
		// stateToCheck.equalsIgnoreCase(STATE_B_RELEASE_CONCEPT) ||
		// stateToCheck.equalsIgnoreCase(STATE_A_RELEASE_CONCEPT) ||
		// stateToCheck.equalsIgnoreCase(STATE_PROTOTYPE_C) ||
		// stateToCheck.equalsIgnoreCase(STATE_PROTOTYPE_B) ||
		// stateToCheck.equalsIgnoreCase(STATE_PROTOTYPE_A) ||
		// stateToCheck.equalsIgnoreCase(STATE_RELEASED)
		if (stateToCheck != null && !stateToCheck.isEmpty() && releasedStateSet.contains(stateToCheck)) {
			return true;
		}
		return false;
	}

	/**
	 * Added to check if object is purely B/C/Production or Released.<br>
	 * No check for A-Released or Prototype-A.
	 * <ul>
	 * <li>{@link EnerSysService#STATE_PRODUCTION_RELEASED}</li>
	 * <li>{@link EnerSysService#STATE_C_RELEASE_CONCEPT}</li>
	 * <li>{@link EnerSysService#STATE_B_RELEASE_CONCEPT}</li>
	 * <li>{@link EnerSysService#STATE_PROTOTYPE_C}</li>
	 * <li>{@link EnerSysService#STATE_PROTOTYPE_B}</li>
	 * <li>{@link EnerSysService#STATE_RELEASED}</li>
	 * </ul>
	 * 
	 * @since Build v2.12
	 * @param stateToCheck
	 * @return
	 */
	@Override
	public boolean isReleasedStateMinusAReleased(String stateToCheck) {
		if (stateToCheck != null && !stateToCheck.isEmpty() && releasedStateSetMinusA.contains(stateToCheck)) {
			return true;
		}
		return false;
	}

	/**
	 * Added to check if object is purely B/C/Production/Released or Obsolete.<br>
	 * No check for A-Released or Prototype-A.
	 * <ul>
	 * <li>{@link EnerSysService#STATE_PRODUCTION_RELEASED}</li>
	 * <li>{@link EnerSysService#STATE_C_RELEASE_CONCEPT}</li>
	 * <li>{@link EnerSysService#STATE_B_RELEASE_CONCEPT}</li>
	 * <li>{@link EnerSysService#STATE_PROTOTYPE_C}</li>
	 * <li>{@link EnerSysService#STATE_PROTOTYPE_B}</li>
	 * <li>{@link EnerSysService#STATE_RELEASED}</li>
	 * <li>{@link EnerSysService#STATE_OBSOLETE}</li>
	 * </ul>
	 * 
	 * @since Build v2.12
	 * @param stateToCheck
	 * @return
	 */
	@Override
	public boolean isReleasedOrObsoleteStateMinusAReleased(String stateToCheck) {
		if (stateToCheck != null && !stateToCheck.isEmpty()
				&& releasedOrObsoleteStateSetMinusA.contains(stateToCheck)) {
			return true;
		}
		return false;
	}

	/**
	 * Check for the given mastered item, the latest object is A-Released or
	 * Obsolete or Cancelled or Inwork.
	 * 
	 * <li>{@link EnerSysService#STATE_A_RELEASE_CONCEPT}</li>
	 * <li>{@link EnerSysService#STATE_PROTOTYPE_A}</li>
	 * <li>{@link EnerSysService#STATE_OBSOLETE}</li>
	 * <li>{@link EnerSysService#STATE_CANCELLED}</li>
	 * <li>{@link EnerSysService#STATE_INWORK}</li>
	 * 
	 * @since Build v2.12
	 * @param masteredObj
	 * @return
	 */
	@Override
	public boolean isLatestVersionObjectInAReleasedOrObsoleteOrCancelledOrInwork(Mastered masteredObj) {
		boolean ret = false;
		if (masteredObj != null) {
			try {
				WTObject o = (WTObject) VersionControlHelper.service.allVersionsOf(masteredObj).nextElement();
				String st = EnerSysHelper.service.getObjectState(o);
				if (st != null && !st.isEmpty()
						&& (EnerSysService.STATE_INWORK.equals(st) || EnerSysService.STATE_A_RELEASE_CONCEPT.equals(st)
								|| EnerSysService.STATE_CANCELLED.equals(st)
								|| EnerSysService.STATE_PROTOTYPE_A.equals(st)
								|| EnerSysService.STATE_OBSOLETE.equals(st))) {
					return true;
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	/**
	 * Check for the given mastered item, the latest object is A-Released or InWork
	 * or Cancelled.
	 * 
	 * <li>{@link EnerSysService#STATE_A_RELEASE_CONCEPT}</li>
	 * <li>{@link EnerSysService#STATE_PROTOTYPE_A}</li>
	 * <li>{@link EnerSysService#STATE_CANCELLED}</li>
	 * <li>{@link EnerSysService#STATE_INWORK}</li>
	 * 
	 * @since Build v2.12
	 * @param masteredObj
	 * @return
	 */
	@Override
	public boolean isLatestVersionObjectInAReleasedOrCancelledOrInwork(Mastered masteredObj) {
		boolean ret = false;
		if (masteredObj != null) {
			try {
				WTObject o = (WTObject) VersionControlHelper.service.allVersionsOf(masteredObj).nextElement();
				String st = EnerSysHelper.service.getObjectState(o);
				if (st != null && !st.isEmpty()
						&& (EnerSysService.STATE_INWORK.equals(st) || EnerSysService.STATE_A_RELEASE_CONCEPT.equals(st)
								|| EnerSysService.STATE_CANCELLED.equals(st)
								|| EnerSysService.STATE_PROTOTYPE_A.equals(st))) {
					return true;
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	/**
	 * ExtractorPropertyHelper values are read from the property file :
	 * BOM_Extractor_Utility.properties<br>
	 * Only applies to WTDocument Objects.<br>
	 * 
	 */
	@Override
	public boolean isDocumentHavingClearance(WTDocument doc) {
		LOGGER.debug(">>>> Start of StandardEnerSysService#isDocumentHavingClearance");
		boolean ret = false;
		try {
			boolean isIgnoreDataClassificationIfEmpty = ExtractorPropertyHelper.isIgnoreDataClassificationIfEmpty();
			// Fetch the attribute from the doc
			PersistableAdapter obj = new PersistableAdapter(doc, null, SessionHelper.getLocale(),
					new DisplayOperationIdentifier());
			obj.load(EnerSysService.IBA_DATA_CLASSIFICATION);
			String classificationVal = (String) obj.get(EnerSysService.IBA_DATA_CLASSIFICATION);
			if ((classificationVal == null || classificationVal.trim().isEmpty())
					&& !isIgnoreDataClassificationIfEmpty) {
				LOGGER.info(">>>Document Classification is not defined: " + EnerSysLogUtils.format(doc));
			} else if ((classificationVal == null || classificationVal.trim().isEmpty())
					&& isIgnoreDataClassificationIfEmpty) {
				ret = true;
			} else if (classificationVal != null) {
				Set<String> docClearanceLevel = ExtractorPropertyHelper.getDocumentClassificationLevels();
				ret = docClearanceLevel.contains(classificationVal) ? true : false;
			}
		} catch (WTException e) {
			// Can skip the error
		}
		LOGGER.debug(">>>> End of StandardEnerSysService#isDocumentHavingClearance");
		return ret;
	}

	/**
	 * getDownloadUrl Helper class to get url of document to be downloaded during
	 * Attachment validation of EnerSys CR
	 * 
	 * @since Build v2.2
	 */
	@Override
	public String getDownloadUrl() throws WTException {
		LOGGER.debug(">>>> Start of StandardEnerSysService#getDownloadUrl");
		String downloadURL = "";
		boolean access = SessionServerHelper.manager.setAccessEnforced(false);
		WTPrincipal principal = setPermissions();
		try {
			WTOrganization org = getEnerSysOrgContainer();
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);
			String templateValue = (String) PreferenceHelper.service.getValue(
					"/ext/enersys/CR_ATTACHMENT_VALIDATION/DOC_TEMPLATE_NUMBER", PreferenceClient.WINDCHILL_CLIENT_NAME,
					orgContainer);
			LOGGER.debug(">>>> Template preference from java *****" + templateValue);
			WTProperties serverProps = WTProperties.getServerProperties();

			String baseURL = serverProps.getProperty("wt.server.codebase");
			LOGGER.debug(">>>> baseURL  ***baseURL***" + baseURL);

			QuerySpec qs = new QuerySpec(WTDocument.class);
			WTDocument doc = null;
			qs.appendWhere(
					new SearchCondition(WTDocument.class, WTDocument.NUMBER, SearchCondition.EQUAL, templateValue),
					new int[] { 0 });
			QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
			if (qr.hasMoreElements()) {
				doc = (WTDocument) qr.nextElement();
				doc = (WTDocument) VersionControlHelper.service.allIterationsOf(doc.getMaster()).nextElement();
				String wtDocClassName = doc.getClassInfo().getClassname();
				wtDocClassName = wtDocClassName.substring(wtDocClassName.lastIndexOf(".") + 1);
				NmOid wtDocOID = new NmOid(wtDocClassName, doc.getPersistInfo().getObjectIdentifier());
				String docOID = wtDocOID.toNmOidStr();
				LOGGER.debug("Template URL ******" + wtDocOID + " --- " + docOID);

				QueryResult primaryContentQR = ContentHelper.service.getContentsByRole(doc, ContentRoleType.PRIMARY);
				if (primaryContentQR.hasMoreElements()) {
					ContentItem contentItem = (ContentItem) primaryContentQR.nextElement();
					if (contentItem instanceof ApplicationData) {
						ApplicationData appData = (ApplicationData) contentItem;
						String classNameAppData = appData.getClassInfo().getClassname();
						classNameAppData = classNameAppData.substring(classNameAppData.lastIndexOf(".") + 1);
						NmOid oidAppData = new NmOid(classNameAppData, appData.getPersistInfo().getObjectIdentifier());
						String oidAppDataString = oidAppData.toNmOidStr();
						oidAppDataString = oidAppDataString.replace("OR:", "");
						downloadURL = baseURL + "/servlet/AttachmentsDownloadDirectionServlet?oid=" + docOID
								+ "&cioids=" + oidAppDataString + "&role=PRIMARY";
						return downloadURL;
					}
				}
			}
		} catch (IOException | WTException e1) {
			e1.printStackTrace();
		} finally {
			SessionServerHelper.manager.setAccessEnforced(access);
			removePermissions(principal);
		}
		return downloadURL;
	}

	/**
	 * Method to setPermissions of Administrator
	 * 
	 * @since Build v2.2
	 */
	private static WTPrincipal setPermissions() throws WTException {
		WTPrincipal principal = SessionHelper.getPrincipal();
		WTPrincipal adminPrincipal = SessionHelper.manager.getAdministrator();
		SessionContext.setEffectivePrincipal(adminPrincipal);
		SessionHelper.manager.setAdministrator();
		return principal;
	}

	/**
	 * Method to remove permissions of the user
	 * 
	 * @since Build v2.2
	 */
	private static void removePermissions(WTPrincipal principal) throws WTException {
		SessionContext.setEffectivePrincipal(principal);
		SessionHelper.manager.setPrincipal(principal.getDn());
	}

	/**
	 * Check if the state of the object is Obsolete.
	 * 
	 * @since v2.3
	 * @param obj
	 * @return
	 */
	public boolean isObsoleted(LifeCycleManaged obj) {
		if (obj != null) {
			if (obj.getLifeCycleState().toString().equals(EnerSysService.STATE_OBSOLETE)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Logic to check if EPMDocument is a CAD Part or CAD Assembly.<br>
	 * (CADCOMPONENT or CADASSEMBLY)<br>
	 * 
	 * @since v2.3
	 * @param epm
	 * @return
	 */
	public boolean isCADPart(EPMDocument epm) {
		if (epm != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("epm getDocType 1-->" + epm.getDocType());
				LOGGER.debug("EPMDocumentType getEPMDocumentTypeSet 2-->"
						+ Arrays.asList(EPMDocumentType.getEPMDocumentTypeSet()));
			}
			if (cadPartDocTypesIntrNames.contains(epm.getDocType().toString())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Logic to check if EPMDocument is a CAD Assembly.<br>
	 * (CADASSEMBLY)<br>
	 * 
	 * @since v2.6
	 * @param epm
	 * @return
	 */
	public boolean isCADAsm(EPMDocument epm) {
		if (epm != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("#isCADAsm epm getDocType 1-->" + epm.getDocType());
				LOGGER.debug("isCADAsm EPMDocumentType getEPMDocumentTypeSet 2-->"
						+ Arrays.asList(EPMDocumentType.getEPMDocumentTypeSet()));
			}
			if (cadAsmDocTypesIntrNames.contains(epm.getDocType().toString())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * Method which checks whether the persistable is critical part or not
	 * 
	 * @param per
	 * @return Since Build 2.8 - JIRA: 518
	 *
	 */
	@Override
	public boolean isCriticalPart(Persistable per) {
		checkAndWriteDebug(Debuggable.START, "#isCriticalPart -->", "object: ", per);
		boolean isCriticalPart = false;
		if (per != null && per instanceof WTPart) {
			try {
				PersistableAdapter pa = new PersistableAdapter(per, null, Locale.US, new CreateOperationIdentifier());
				pa.load("ext.enersys.IS_PART_CRITICAL");
				Object isCriticalPartObj = pa.get("ext.enersys.IS_PART_CRITICAL");
				checkAndWriteDebug(Debuggable.LINE, "#isCriticalPart -->", " critical part attribute value: ",
						isCriticalPartObj);
				if (isCriticalPartObj != null && isCriticalPartObj instanceof Boolean
						&& ((Boolean) isCriticalPartObj) == true) {
					isCriticalPart = true;
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		checkAndWriteDebug(Debuggable.END, "#isCriticalPart");
		return isCriticalPart;
	}

	/**
	 * 
	 * Method which checks whether the Persistable is quality critical part or not
	 * 
	 * @param per
	 * @return Since Build 2.9 - JIRA: 712
	 *
	 */
	@Override
	public boolean isQualityCriticalPart(Persistable per) {
		checkAndWriteDebug(Debuggable.START, "#isQualityCriticalPart -->", "object: ", per);
		boolean isQualityCriticalPart = false;
		if (per != null && per instanceof WTPart) {
			try {
				PersistableAdapter pa = new PersistableAdapter(per, null, Locale.US, new CreateOperationIdentifier());
				pa.load("ext.enersys.PART_IS_QUALITYCRITICAL");
				Object isQualityCriticalPartObj = pa.get("ext.enersys.PART_IS_QUALITYCRITICAL");
				checkAndWriteDebug(Debuggable.LINE, "#isQualityCriticalPart -->",
						" quality critical part attribute value: ", isQualityCriticalPartObj);
				if (isQualityCriticalPartObj != null && isQualityCriticalPartObj instanceof Boolean
						&& ((Boolean) isQualityCriticalPartObj) == true) {
					isQualityCriticalPart = true;
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		checkAndWriteDebug(Debuggable.END, "#isQualityCriticalPart");
		return isQualityCriticalPart;
	}

	/**
	 * Checks if the Persistable passed has an Attribute <br>
	 * with the name = Internal name (passed as 2nd argument)
	 * 
	 * @since Build v2.9
	 */
	public boolean isAttributeApplicableOnObject(Persistable per, String internalName) {
		checkAndWriteDebug(Debuggable.START, "#isAttributeApplicableOnObject -->", "object: ", per);
		if (per != null && internalName != null && !internalName.isEmpty()) {
			try {
				TypeIdentifier selectedObjType = TypeIdentifierHelper.getType(per);
				TypeDefinitionReadView tdrv = TypeDefinitionServiceHelper.service.getTypeDefView(selectedObjType);
				for (AttributeDefinitionReadView f : tdrv.getAllAttributes()) {
					if (f.getName().equalsIgnoreCase(internalName)) {
						checkAndWriteDebug(Debuggable.END, "#isAttributeApplicableOnObject --> returning true");
						return true;
					}
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		checkAndWriteDebug(Debuggable.END, "#isAttributeApplicableOnObject --> returning false");
		return false;
	}

	/**
	 * Gets display name of the given internal name from the given Enumeration
	 * 
	 * @param internalValue
	 * @return displayValue Since Build 2.10 - JIRA: 628
	 */
	@Override
	public String getEnumDisplayValue(String internalValue, String globalEnumeration) {
		checkAndWriteDebug(Debuggable.START, "#getEnumDisplayValue -->", "Global Enum Internal Name: ",
				globalEnumeration, " & Internal Name of Enum Value: ", internalValue);
		String displayValue = "";
		try {
			BaseDefinitionService baseDefService = (BaseDefinitionService) ServiceFactory
					.getService(com.ptc.core.lwc.common.BaseDefinitionService.class);
			EnumerationDefinitionReadView enumDefView = baseDefService.getEnumDefView(globalEnumeration,
					EnumerationDefinitionSystemState.NON_SYSTEM_ONLY);
			EnumeratedSet es = (EnumeratedSet) enumDefView.getEnumeratedSet(SessionHelper.getLocale());
			EnumerationEntryIdentifier eei = es.getElementByKey(internalValue);
			LWCEnumerationEntryValuesFactory eevf = new LWCEnumerationEntryValuesFactory();
			LocalizedValues value = eevf.get(eei, SessionHelper.getLocale());
			displayValue = value.getDisplay();
		} catch (Exception e) {
			e.printStackTrace();
		}
		checkAndWriteDebug(Debuggable.END, "#getEnumDisplayValue -->", "Returning Display Value: ", displayValue);
		return displayValue;
	}

	/**
	 * Method added in 2.10 build for setting IsChangeRequestRequired <br>
	 * value in Deviation and Problem Report.
	 * 
	 * @since Build 2.10
	 */
	@Override
	public boolean setIsCRRequired(WTObject perObj, boolean valuetoSet) {
		boolean iscrRequired = false;
		PersistableAdapter pa;
		if (perObj instanceof WTVariance) {
			try {
				WTVariance var = (WTVariance) perObj;
				pa = new PersistableAdapter(var, null, Locale.US, new UpdateOperationIdentifier());
				pa.load("ext.enersys.DEVIATION_CHANGE_REQUEST_REQUIRED");
				if (valuetoSet) {
					pa.set("ext.enersys.DEVIATION_CHANGE_REQUEST_REQUIRED", "Yes");
					var = (WTVariance) pa.apply();
					IBAValueDBService ibaserv = new IBAValueDBService();
					ibaserv.updateAttributeContainer(var,
							((DefaultAttributeContainer) var.getAttributeContainer()).getConstraintParameter(), null,
							null);
					PersistenceServerHelper.manager.update(var);
				} else {
					pa.set("ext.enersys.DEVIATION_CHANGE_REQUEST_REQUIRED", "No");
					var = (WTVariance) pa.apply();
					IBAValueDBService ibaserv = new IBAValueDBService();
					ibaserv.updateAttributeContainer(var,
							((DefaultAttributeContainer) var.getAttributeContainer()).getConstraintParameter(), null,
							null);
					PersistenceServerHelper.manager.update(var);
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
		} else if (perObj instanceof WTChangeIssue) {
			try {
				WTChangeIssue prbreport = (WTChangeIssue) perObj;
				pa = new PersistableAdapter(prbreport, null, Locale.US, new UpdateOperationIdentifier());
				pa.load("ext.enersys.PROBLEMREPORT_CHANGE_REQUEST_REQUIRED");
				if (valuetoSet) {
					pa.set("ext.enersys.PROBLEMREPORT_CHANGE_REQUEST_REQUIRED", "Yes");
					prbreport = (WTChangeIssue) pa.apply();
					IBAValueDBService ibaserv = new IBAValueDBService();
					ibaserv.updateAttributeContainer(prbreport,
							((DefaultAttributeContainer) prbreport.getAttributeContainer()).getConstraintParameter(),
							null, null);
					PersistenceServerHelper.manager.update(prbreport);
				} else {
					pa.set("ext.enersys.PROBLEMREPORT_CHANGE_REQUEST_REQUIRED", "No");
					prbreport = (WTChangeIssue) pa.apply();
					IBAValueDBService ibaserv = new IBAValueDBService();
					ibaserv.updateAttributeContainer(prbreport,
							((DefaultAttributeContainer) prbreport.getAttributeContainer()).getConstraintParameter(),
							null, null);
					PersistenceServerHelper.manager.update(prbreport);
				}
			} catch (WTException e) {
				e.printStackTrace();
			}
		}
		return iscrRequired;
	}

	/**
	 * Quickly checks if a given Change/Promotion Notice object is SOP.<br>
	 * It does this by looking at the Change Track/Promotion Track field on the
	 * object.
	 * 
	 * @param obj - WTObject - represents the Change Object/Promotion Notice object
	 * @return True - If the Object is SOP; False otherwise.
	 * @since Build v3.1
	 */
	@Override
	public boolean isPBOTrackSOP(WTObject obj) {
		// FOR : EnerSys CR/CN/Promotion Request
		final String CHANGE_TRACK_INTERNAL_NAME = "ext.enersys.complexity";
		if (obj instanceof Persistable) {

			try {
				PersistableAdapter pa = new PersistableAdapter(obj, null, Locale.US, new DisplayOperationIdentifier());
				pa.load(CHANGE_TRACK_INTERNAL_NAME);
				String changeTrackValue = (String) pa.getAsString(CHANGE_TRACK_INTERNAL_NAME);

				if (changeTrackValue != null && !changeTrackValue.isEmpty()
						&& (obj instanceof WTChangeRequest2
								&& EnerSysSoftTypeHelper.isExactlyType(obj, "ext.enersys.enersysChangeRequest"))
						|| (obj instanceof WTChangeOrder2
								&& EnerSysSoftTypeHelper.isExactlyType(obj, "ext.enersys.enersysChangeNotice")
								|| obj instanceof PromotionNotice)) {

					if ("VARIANT".equalsIgnoreCase(changeTrackValue) || "PROTOTYPE".equalsIgnoreCase(changeTrackValue)
							|| "SOP_CLASS_1".equalsIgnoreCase(changeTrackValue)
							|| "SOP_CLASS_2".equalsIgnoreCase(changeTrackValue)
							|| "SOP_CLASS_3".equalsIgnoreCase(changeTrackValue)) {
						return true;
					}
				}

			} catch (Exception e) {
				//
			}
		}
		return false;
	}

	// ADO:14768
	public boolean isPromotionTrackMFG(WTObject obj) {
		// FOR : EnerSys CR/CN/Promotion Request
		final String CHANGE_TRACK_INTERNAL_NAME = "ext.enersys.complexity";
		if (obj instanceof Persistable) {

			try {
				PersistableAdapter pa = new PersistableAdapter(obj, null, Locale.US, new DisplayOperationIdentifier());
				pa.load(CHANGE_TRACK_INTERNAL_NAME);
				String changeTrackValue = (String) pa.getAsString(CHANGE_TRACK_INTERNAL_NAME);

				if (changeTrackValue != null && !changeTrackValue.isEmpty()
						&& (obj instanceof WTChangeRequest2
								&& EnerSysSoftTypeHelper.isExactlyType(obj, "ext.enersys.enersysChangeRequest"))
						|| (obj instanceof WTChangeOrder2
								&& EnerSysSoftTypeHelper.isExactlyType(obj, "ext.enersys.enersysChangeNotice")
								|| obj instanceof PromotionNotice)) {

					if ("MFG".equalsIgnoreCase(changeTrackValue)) {
						return true;
					}
				}

			} catch (Exception e) {
				//
			}
		}
		return false;
	}

	/**
	 * Gets Long Description value from a WTPart object.
	 * 
	 * @since Build v3.2
	 */
	@Override
	public String getLongDescription(WTObject obj) {
		final String LD_INTR_NAME = "ext.enersys.PART_LONG_DESCRIPTION";
		if (obj instanceof WTPart) {
			WTPart prt = (WTPart) obj;
			try {
				PersistableAdapter pa = new PersistableAdapter(prt, null, SessionHelper.getLocale(), null);
				pa.load(LD_INTR_NAME);
				String val = "";
				try {
					val = (String) pa.getAsString(LD_INTR_NAME);
				} catch (Exception e) {
				}
				return val;
			} catch (Exception e) {
			}
		}
		return "";
	}

	/**
	 * Gathers AML Information for a given WTPart and returns a HashMap with <br>
	 * Key = Manufacturers, and Value = AML Numbers.<br>
	 * Skips DO NOT USE Supplier Parts.<br>
	 * In-work AMLs are also shown.<br>
	 * 
	 * @since Build v3.2
	 * @return HashMap<String, List<String>> - Key = Manufacturers, and Value = AML
	 *         Numbers
	 */
	@Override
	public HashMap<String, List<String>> getAMLInformation(WTObject obj) {
		HashMap<String, List<String>> ret = new HashMap<>();
		if (obj instanceof WTPart) {
			WTPart prt = (WTPart) obj;
			try {
				WTCollection sourcingContextColl = getEnerSysSourcingContexts();
				if (sourcingContextColl != null) {
					Iterator<?> srcContItr = sourcingContextColl.persistableIterator();
					while (srcContItr.hasNext()) {
						AXLContext srcContObj = (AXLContext) srcContItr.next();
						// Process Manufacturer Parts for given Sourcing Context
						WTCollection amlAXLEntries = AXLHelper.service.getAML(prt, srcContObj);
						if (amlAXLEntries != null && !amlAXLEntries.isEmpty()) {
							Iterator<?> amlAXLEntriesItr = amlAXLEntries.persistableIterator();
							while (amlAXLEntriesItr.hasNext()) {
								AXLEntry axlEntryObj = (AXLEntry) amlAXLEntriesItr.next();
								ManufacturerPart manuPrt = axlEntryObj.getLatestManufacturerPart();
								if (!axlEntryObj.getAmlPreference().equals(AXLPreference.DO_NOT_USE)
								// &&
								// EnerSysHelper.service.isReleasedState(manuPrt.getLifeCycleState().toString())
								) {
									String orgName = manuPrt.getOrganization().getName();
									if (axlEntryObj.getAmlPreference().equals(AXLPreference.PREFERRED)) {
										orgName = orgName + " (" + axlEntryObj.getAmlPreference().getDisplay() + ")";
									}

									if (ret.containsKey(orgName)) {
										ret.get(orgName).add(manuPrt.getNumber());
									} else {
										List<String> o = new ArrayList<>();
										o.add(manuPrt.getNumber());
										ret.put(orgName, o);
									}
								}
							}
						}
					}
				}
			} catch (WTException e) {
				// SILENT FAIL
			}
		}
		return ret;
	}

	/**
	 * Retrieves Sourcing Contexts defined for EnerSys Organization.<br>
	 * 
	 * @since Build v3.2
	 * @return - WTCollection of Sourcing Contexts
	 */
	public WTCollection getEnerSysSourcingContexts() {
		WTCollection col = null;
		try {
			WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
			WTContainerRef containerRef = WTContainerHelper.service.getOrgContainerRef(org);
			col = AXLHelper.service.getContexts(containerRef);
		} catch (WTException e) {
			e.printStackTrace();
		}
		return col;
	}

	/**
	 * Gets Default value applied on the given attr of given type in Type & Attr
	 * Manager
	 * 
	 * @since Build 3.3
	 * 
	 * @return Default value of attribute
	 */
	@Override
	public String getAttrDefaultValue(TypeIdentifier ti, String attrName) {
		String attrDefaultValue = "";
		try {
			BusinessField businessField = getTypeBusinessField(attrName, ti);
			TypeDefinitionReadView typeDefReadView = TypeDefinitionServiceHelper.service.getTypeDefView(ti);
			AttributeDefinitionReadView attrDefReadView = typeDefReadView.getAttributeByName(businessField.getName());
			Collection<AttributeDefaultValueReadView> attrBefaultValueReadViewCollection = attrDefReadView
					.getAllDefaultValues();
			for (AttributeDefaultValueReadView attrDefaultValueReadView : attrBefaultValueReadViewCollection) {
				attrDefaultValue = (String) attrDefaultValueReadView.getValue();
			}
		} catch (Exception e) {
			LOGGER.error("Exception has occured");
			e.printStackTrace();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(attrName + " attribute default value:" + attrDefaultValue);
		}

		return attrDefaultValue;
	}

	/**
	 * Helper method to get Business Field reference for the given attribute of the
	 * given type
	 * 
	 * @since Build 3.3
	 */
	private static BusinessField getTypeBusinessField(String attrName, TypeIdentifier ti) throws WTException {
		BusinessFieldIdentifier bfid = BusinessFieldIdFactoryHelper.FACTORY.getTypeBusinessFieldIdentifier(attrName,
				ti);
		return BusinessFieldServiceHelper.SERVICE.getBusinessField(bfid);
	}

	/**
	 * Utility method to update incoming attribute value to its default value from
	 * Type & Attribute Manager on the incoming part
	 * 
	 * @since Build 3.3
	 * 
	 * @return updated part
	 */
	@Override
	public WTPart updatePartAttrWithDefaultValue(WTPart part, String attrInternalName) {
		LOGGER.debug("updatePartAttrWithDefaultValue:START");
		TypeIdentifier partTI = TypeIdentifierHelper.getType(part);
		String attrDefualtValue = getAttrDefaultValue(partTI, attrInternalName);
		if (attrDefualtValue != null && !attrDefualtValue.isEmpty()) {
			try {
				PersistableAdapter pa = new PersistableAdapter(part, null, Locale.US, new UpdateOperationIdentifier());
				pa.load(attrInternalName);
				LOGGER.debug(attrInternalName + " value before update:" + pa.get(attrInternalName));
				pa.set(attrInternalName, attrDefualtValue);
				part = (WTPart) pa.apply();
				IBAValueDBService ibaserv = new IBAValueDBService();
				ibaserv.updateAttributeContainer(part,
						((DefaultAttributeContainer) part.getAttributeContainer()).getConstraintParameter(), null,
						null);
				PersistenceServerHelper.manager.update(part);
				part = (WTPart) PersistenceHelper.manager.refresh(part);
				LOGGER.debug(attrInternalName + " value after update:" + pa.get(attrInternalName));
				LOGGER.debug("updatePartAttrWithDefaultValue:END");
				return part;

			} catch (Exception e) {
				LOGGER.error("Exception has occured while updating " + attrInternalName + " on " + partTI);
				e.printStackTrace();
				return null;

			}

		}
		return null;
	}

	/**
	 * Helper method to check if the user belong the group and returns boolean
	 * 
	 * @since Build 3.3
	 */
	@Override
	public boolean IsUserPartOfGroup(WTPrincipal currentUser, String groupName) {
		try {
			WTOrganization org = OrganizationServicesHelper.manager.getOrganization(currentUser);
			WTGroup supportPLMGroup = OrganizationServicesHelper.manager.getGroup(groupName, org);
			return OrganizationServicesHelper.manager.isMember(supportPLMGroup, currentUser);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public Set<String> getAttributeValues(Set<String> attributeValues, Persistable updatedPart,
			String attrInternalName) {

		try {
			PersistableAdapter pa = new PersistableAdapter(updatedPart, null, Locale.US,
					new DisplayOperationIdentifier());
			pa.load(attrInternalName);
			Object attrObj = pa.get(attrInternalName);

			if (attrObj instanceof Object[]) {
				Object[] array_Str = (Object[]) attrObj;
				for (Object value : array_Str) {
					attributeValues.add(value.toString());
				}
			} else if (attrObj instanceof String) {
				attributeValues.add(attrObj.toString());
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

		return attributeValues;
	}

	/**
	 * Updating attributes without iterating & modifying the Time stamp, Running as
	 * an Admin
	 *
	 */
	@Override
	public void updateAttribute(Persistable updatedPart, String[] attrValue, String attrName) {
		Transaction transaction = new Transaction();
		try {

			boolean bool = SessionServerHelper.manager.isAccessEnforced();
			WTPrincipal principal = null;
			SessionContext previousContext = SessionContext.newContext();
			transaction.start();
			try {
				SessionServerHelper.manager.setAccessEnforced(false);
				principal = SessionHelper.getPrincipal();
				WTPrincipal adminPrincipal = SessionHelper.manager.getAdministrator();
				previousContext.setEffectivePrincipal(adminPrincipal);
				SessionHelper.manager.setAdministrator();

				PersistableAdapter adapter = new PersistableAdapter(updatedPart, null, SessionHelper.getLocale(),
						new UpdateOperationIdentifier());
				adapter.load(attrName);
				adapter.set(attrName, attrValue);
				updatedPart = (WTObject) adapter.apply();
				IBAValueDBService ibaserv = new IBAValueDBService();
				ibaserv.updateAttributeContainer((IBAHolder) updatedPart,
						((DefaultAttributeContainer) ((IBAHolder) updatedPart).getAttributeContainer())
								.getConstraintParameter(),
						null, null);
				PersistenceServerHelper.manager.update(updatedPart);
				transaction.commit();
				transaction = null;

			} catch (Exception e) {
				System.out.println("Failed");
				e.printStackTrace();

			} finally {
				SessionServerHelper.manager.setAccessEnforced(bool);
				if (previousContext != null) {
					SessionContext.setContext(previousContext);
				}
				if (principal != null) {
					SessionHelper.manager.setPrincipal(principal.getDn());
					SessionContext.setEffectivePrincipal(principal);
				}
			}

			// part = (WTPart) PersistenceHelper.manager.refresh(part);

		} catch (java.lang.Exception e) {
			System.out.println("Failed");
			e.printStackTrace();

		} finally {

			if (transaction != null) {
				transaction.rollback();
			}
		}

	}

	/**
	 *
	 */
	@Override
	public void checkAndCopyAttribute(Persistable updatedPart, String sourceAttribute, String targetAttribute,
			boolean override) {

		Set<String> sourceAttributeSet = getAttributeValues(new HashSet<String>(), updatedPart, sourceAttribute);
		Set<String> targetAttributeSet = getAttributeValues(new HashSet<String>(), updatedPart, targetAttribute);

		boolean updateNeeded = false;
		// Checking if attr is available in target
		for (String attrValue : sourceAttributeSet) {

			if (!targetAttributeSet.contains(attrValue)) {
				// Update Attribute in part
				updateNeeded = true;
				break;

			}

		}
		if (updateNeeded) {
			// String[] strArray=(String[]) targetAttributeSet.toArray();
			if (override) {
				// If true Simple copy values from Source to target attribute, overriding
				// existing target attr values
				// targetAttributeSet.addAll(sourceAttributeSet);
				String[] arrayOfString = sourceAttributeSet.toArray(new String[0]);

				updateAttribute(updatedPart, arrayOfString, targetAttribute);
			} else {
				targetAttributeSet.addAll(sourceAttributeSet);
				String[] arrayOfString = targetAttributeSet.toArray(new String[0]);

				updateAttribute(updatedPart, arrayOfString, targetAttribute);
			}
		}
		// String value = (String) pa.get(InternalAttributeValue);
	}

	/**
	 * This method returns boolean user level preference Changed in Build V 3.8
	 */
	@Override
	public boolean isDataVerificationEnabled(WTPrincipal currentUser, WTContained obj) {
		boolean isDataVerificationEnabled = false;
		try {
			if (obj instanceof WTPart) {
				isDataVerificationEnabled = (boolean) PreferenceHelper.service.getValue((WTUser) currentUser,
						EnerSysService.WTPART_RULE_SET_ACTIVATE, PreferenceClient.WINDCHILL_CLIENT_NAME);
			} else if (obj instanceof WTDocument) {
				isDataVerificationEnabled = (boolean) PreferenceHelper.service.getValue((WTUser) currentUser,
						EnerSysService.WTDOC_RULE_SET_ACTIVATE, PreferenceClient.WINDCHILL_CLIENT_NAME);
			} else if (obj instanceof EPMDocument) {
				isDataVerificationEnabled = (boolean) PreferenceHelper.service.getValue((WTUser) currentUser,
						EnerSysService.EPM_RULE_SET_ACTIVATE, PreferenceClient.WINDCHILL_CLIENT_NAME);
			} else if (obj instanceof PromotionNotice) {
				isDataVerificationEnabled = (boolean) PreferenceHelper.service.getValue((WTUser) currentUser,
						EnerSysService.PROMO_RULE_SET_ACTIVATE, PreferenceClient.WINDCHILL_CLIENT_NAME);
			} else if (obj instanceof WTChangeRequest2) {
				isDataVerificationEnabled = (boolean) PreferenceHelper.service.getValue((WTUser) currentUser,
						EnerSysService.CR_RULE_SET_ACTIVATE, PreferenceClient.WINDCHILL_CLIENT_NAME);
			} else if (obj instanceof WTChangeOrder2) {
				isDataVerificationEnabled = (boolean) PreferenceHelper.service.getValue((WTUser) currentUser,
						EnerSysService.CN_RULE_SET_ACTIVATE, PreferenceClient.WINDCHILL_CLIENT_NAME);
			} else if (obj instanceof WTChangeActivity2) {
				isDataVerificationEnabled = (boolean) PreferenceHelper.service.getValue((WTUser) currentUser,
						EnerSysService.CT_RULE_SET_ACTIVATE, PreferenceClient.WINDCHILL_CLIENT_NAME);
			} else if (obj instanceof WTVariance) {
				isDataVerificationEnabled = (boolean) PreferenceHelper.service.getValue((WTUser) currentUser,
						EnerSysService.DEVIATION_RULE_SET_ACTIVATE, PreferenceClient.WINDCHILL_CLIENT_NAME);
			} else if (obj instanceof WTChangeIssue) {
				isDataVerificationEnabled = (boolean) PreferenceHelper.service.getValue((WTUser) currentUser,
						EnerSysService.WH_PROBLEM_RULE_SET_ACTIVATE, PreferenceClient.WINDCHILL_CLIENT_NAME);
			}

		} catch (WTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOGGER.debug("Value of isDataVerificationEnabled ===>>>" + isDataVerificationEnabled);
		return isDataVerificationEnabled;
	}

	@Override
	public RuleValidationResultSet getValidationResults(Persistable persistable) throws WTException {
		// Invoke Business rule engine to get results
		RuleValidationResultSet resultSet = new RuleValidationResultSet();
		try {
			WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);
			String preferenceValue = (String) PreferenceHelper.service.getValue(
					"/ext/enersys/DATA_VERIFICATION/BUSINESSRULE_SET", PreferenceClient.WINDCHILL_CLIENT_NAME,
					orgContainer);
			LOGGER.debug("Business Rule Set =" + preferenceValue);
			String[] splitValues = preferenceValue.split(";");
			List<String> buseinessRuleSet = Arrays.asList(splitValues);
			for (String ruleSet : buseinessRuleSet) {
				if (ruleSet != null && !ruleSet.isEmpty()) {

					BusinessRuleSetBean defaultBean = BusinessRuleSetBean.newBusinessRuleSetBean(ruleSet,
							"EnerSysDataVerificationRuleDelegate");

					BusinessRuleSetBean[] beans = new BusinessRuleSetBean[] { defaultBean };

					resultSet.appendResults(BusinessRulesHelper.engine.execute(persistable, beans));
				}
			}

			LOGGER.debug("resultSet==>" + resultSet);
			if (!resultSet.hasResultsByStatus(RuleValidationStatus.FAILURE)) {
				LOGGER.debug("Validation Passed");
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}

		return resultSet;
	}

	@Override
	public RuleValidationResultSet getValidationResults(Persistable persistable, String delegate) throws WTException {
		// Invoke Business rule engine to get results
		RuleValidationResultSet resultSet = new RuleValidationResultSet();
		try {
			WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);
			String preferenceValue = "";
			if (persistable instanceof WTPart) {

				preferenceValue = (String) PreferenceHelper.service.getValue(
						((WTContained) persistable).getContainerReference(), EnerSysService.WTPART_RULE_SET,
						PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());

			} else if (persistable instanceof PromotionNotice) {
				preferenceValue = (String) PreferenceHelper.service.getValue(
						((WTContained) persistable).getContainerReference(), EnerSysService.PROMO_RULE_SET,
						PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());
			} else if (persistable instanceof WTChangeRequest2) {
				preferenceValue = (String) PreferenceHelper.service.getValue(
						((WTContained) persistable).getContainerReference(), EnerSysService.CR_RULE_SET,
						PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());
			} else if (persistable instanceof WTChangeOrder2) {
				preferenceValue = (String) PreferenceHelper.service.getValue(
						((WTContained) persistable).getContainerReference(), EnerSysService.CN_RULE_SET,
						PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());
			} else if (persistable instanceof WTChangeActivity2) {
				preferenceValue = (String) PreferenceHelper.service.getValue(
						((WTContained) persistable).getContainerReference(), EnerSysService.CT_RULE_SET,
						PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());
			} else if (persistable instanceof EPMDocument) {
				preferenceValue = (String) PreferenceHelper.service.getValue(
						((WTContained) persistable).getContainerReference(), EnerSysService.EPM_RULE_SET,
						PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());
			} else if (persistable instanceof WTDocument) {
				preferenceValue = (String) PreferenceHelper.service.getValue(
						((WTContained) persistable).getContainerReference(), EnerSysService.WTDOC_RULE_SET,
						PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());
			} else if (persistable instanceof WTVariance) {
				preferenceValue = (String) PreferenceHelper.service.getValue(
						((WTContained) persistable).getContainerReference(), EnerSysService.DEVIATION_RULE_SET,
						PreferenceClient.WINDCHILL_CLIENT_NAME, (WTUser) SessionHelper.manager.getPrincipal());
			}

			LOGGER.debug("Business Rule Set =" + preferenceValue);

			// WTPart pref-->BUSINESSRULE_SET;BUSINESSRULE_SET2
			// PromotionNotice pref-->BUSINESSRULE_SET4;BUSINESSRULE_SET3

			String[] splitValues = preferenceValue.split(";");
			List<String> buseinessRuleSet = Arrays.asList(splitValues);

			for (String ruleSet : buseinessRuleSet) {
				if (ruleSet != null && !ruleSet.isEmpty()) {

					BusinessRuleSetBean defaultBean = BusinessRuleSetBean.newBusinessRuleSetBean(ruleSet, delegate);

					BusinessRuleSetBean[] beans = new BusinessRuleSetBean[] { defaultBean };

					resultSet.appendResults(BusinessRulesHelper.engine.execute(persistable, beans));
				}
			}

			LOGGER.debug("resultSet==>" + resultSet);
			if (!resultSet.hasResultsByStatus(RuleValidationStatus.FAILURE)) {
				LOGGER.debug("Validation Passed");
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}

		return resultSet;
	}

	@Override
	public void updateAttribute(Persistable updatedPart, Object attrValue, String attrName) {
		Transaction transaction = new Transaction();
		try {

			boolean bool = SessionServerHelper.manager.isAccessEnforced();
			WTPrincipal principal = null;
			SessionContext previousContext = SessionContext.newContext();
			transaction.start();
			try {
				SessionServerHelper.manager.setAccessEnforced(false);
				principal = SessionHelper.getPrincipal();
				WTPrincipal adminPrincipal = SessionHelper.manager.getAdministrator();
				previousContext.setEffectivePrincipal(adminPrincipal);
				SessionHelper.manager.setAdministrator();

				PersistableAdapter adapter = new PersistableAdapter(updatedPart, null, SessionHelper.getLocale(),
						new UpdateOperationIdentifier());
				adapter.load(attrName);
				Object existingvalue = adapter.get(attrName);
				if (!attrValue.equals(existingvalue)) {// Update only of different
					adapter.set(attrName, attrValue);
					updatedPart = (WTObject) adapter.apply();
					IBAValueDBService ibaserv = new IBAValueDBService();
					ibaserv.updateAttributeContainer((IBAHolder) updatedPart,
							((DefaultAttributeContainer) ((IBAHolder) updatedPart).getAttributeContainer())
									.getConstraintParameter(),
							null, null);
					PersistenceServerHelper.manager.update(updatedPart, false);
				}
				transaction.commit();
				transaction = null;

			} catch (Exception e) {
				System.out.println("Failed");
				e.printStackTrace();

			} finally {
				SessionServerHelper.manager.setAccessEnforced(bool);
				if (previousContext != null) {
					SessionContext.setContext(previousContext);
				}
				if (principal != null) {
					SessionHelper.manager.setPrincipal(principal.getDn());
					SessionContext.setEffectivePrincipal(principal);
				}
			}

			// part = (WTPart) PersistenceHelper.manager.refresh(part);

		} catch (java.lang.Exception e) {
			System.out.println("Failed");
			e.printStackTrace();

		} finally {

			if (transaction != null) {
				transaction.rollback();
			}
		}

	}

	@Override
	public int validateObject(Persistable persistable) throws WTException {
		int i = 3;

		RuleValidationResultSet resultSet = EnerSysHelper.service.getValidationResults(persistable,
				"EnerSysDataVerificationRuleDelegate");

		List<ValidationResult> validationResultList = resultSet.getResultList();
		for (ValidationResult validationResult : validationResultList) {
			// EnerSysAdminTypeChangeBean rowBean = new EnerSysAdminTypeChangeBean();
			ValidationStatus status = validationResult.getStatus();
			// FAILURE SUCCESS
			String status_Str = status != null ? status.toString() : "";

			if ("FAILURE".equalsIgnoreCase(status_Str)) {
				i = 0;
				break;
			} else if ("SUCCESS".equalsIgnoreCase(status_Str)) {
				i = 2;
			}

		}
		return i;
	}

	@Override
	public void sendEmailWithAttachment(String recipientAddress, String subject, String messageText, File aFile) {
		try {
			String host = WTProperties.getLocalProperties().getProperty("wt.mail.mailhost");
			String mailFrom = WTProperties.getLocalProperties().getProperty("wt.mail.from");
			Properties mailProp = new Properties();
			mailProp.put("mail.smtp.host", host);
			Session session = Session.getDefaultInstance(mailProp);

			MimeBodyPart fileBodyPart = new MimeBodyPart();
			Multipart multipart = new MimeMultipart();
			DataSource source = new FileDataSource(aFile);
			fileBodyPart.setDataHandler(new DataHandler(source));
			fileBodyPart.setFileName(aFile.getName());
			multipart.addBodyPart(fileBodyPart);

			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(messageText, "text/html");
			multipart.addBodyPart(messageBodyPart);

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mailFrom));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientAddress));
			message.setSubject(subject);
			message.setHeader("Content-Type", "text/html");
			message.setContent(multipart);
			Transport.send(message);
			LOGGER.info("Sent message successfully");
		} catch (MessagingException | IOException exp) {
			LOGGER.error("Problem during sending mail to " + recipientAddress);
		}
	}

	// Aim is to check for warning & execute only once
	@Override
	public RuleValidationResultSet allowWarningOnce(Object[] rulesToSKip, RuleValidationResultSet resultSet,
			boolean setToFailure) {
		// RuleValidationResultSet resultSetFinal = new RuleValidationResultSet();
		List<ValidationResult> validationResultList = resultSet.getResultList();
		// resultSet.re
		for (ValidationResult validationResult : validationResultList) {
			for (Object obj : rulesToSKip) {
				if (validationResult.getValidationKey().toString().contains(obj.toString())) {
					if (setToFailure) {// true

						validationResult.setStatus(RuleValidationStatus.FAILURE);
					} else {

						validationResult.setStatus(RuleValidationStatus.SUCCESS);
					}
					if (!validationResult.getFeedbackMessages().isEmpty()) {
						resultSet.replaceResult(validationResult);
					}
				}

			}

		}
		return resultSet;
	}

	@Override
	public RuleValidationResultSet allowWarningOnce(Object[] rulesToSKip, RuleValidationResultSet resultSet) {
		// RuleValidationResultSet resultSetFinal = new RuleValidationResultSet();
		List<ValidationResult> validationResultList = resultSet.getResultList();
		// resultSet.re
		for (ValidationResult validationResult : validationResultList) {
			// EnerSysAdminTypeChangeBean rowBean = new EnerSysAdminTypeChangeBean();
			for (Object obj : rulesToSKip) {
				if (validationResult.getValidationKey().toString().contains(obj.toString())) {
					// if (setToFailure) {
					validationResult.setStatus(RuleValidationStatus.FAILURE);
					// } else {
					// validationResult.setStatus(RuleValidationStatus.SUCCESS);
					// }

					resultSet.replaceResult(validationResult);
				}

			}

		}
		return resultSet;
	}

	@Override
	public boolean hasResultsByStatus(RuleValidationStatus ruleValidationStatus, RuleValidationResultSet resultSet) {
		List<ValidationResult> validationResultList = resultSet.getResultList();
		// resultSet.re
		// resultSet.hasResultsByStatus(com.ptc.core.businessRules.validation.RuleValidationStatus.FAILURE);
		for (ValidationResult validationResult : validationResultList) {
			if (ruleValidationStatus.equals(validationResult.getStatus())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @since Build 3.4
	 * 
	 *        Checks whether all incoming parts are from same view
	 */
	@Override
	public boolean areAllPartsOfSameView(List<WTPart> parts) {
		LOGGER.debug("areAllPartsOfSameView: START");
		try {
			Iterator<WTPart> partsItr = parts.iterator();
			while (partsItr.hasNext()) {
				WTPart part = partsItr.next();
				String partViewName = part.getViewName();
				for (WTPart partCompare : parts) {
					String partCompareViewName = partCompare.getViewName();
					if (!partViewName.equals(partCompareViewName)) {
						return false;
					}
				}
			}
			LOGGER.debug("areAllPartsOfSameView: END");
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Exception has occured while checking parts view");
			return true;
		}
	}

	/**
	 * @param WTContainer container
	 * @return Boolean
	 * 
	 *         The method isRestrictedAttribute() returns the true if the
	 *         WTContainer is Restricted else it is false.
	 * 
	 * 
	 * @throws WTException
	 */
	public boolean isRestrictedContainer(WTContainer container) {
		LOGGER.debug(CLASSNAME + "isRestrictedAttribute() method started");
		Boolean currentContainerIsRestricted = false;
		try {
			PersistableAdapter adapterRestrtictedContainerAttribute = new PersistableAdapter(container, null,
					SessionHelper.getLocale(), null);
			adapterRestrtictedContainerAttribute.load(RESTIRTED_ATRIRBUTE_INTERNAL_NAME);
			currentContainerIsRestricted = (Boolean) adapterRestrtictedContainerAttribute
					.get(RESTIRTED_ATRIRBUTE_INTERNAL_NAME);
			if (currentContainerIsRestricted == null) {
				LOGGER.debug(
						CLASSNAME + "Running Null Logic for Restricted Attribute : " + currentContainerIsRestricted);
				SessionServerHelper.manager.setAccessEnforced(false);
				adapterRestrtictedContainerAttribute.set(RESTIRTED_ATRIRBUTE_INTERNAL_NAME, Boolean.FALSE);
				adapterRestrtictedContainerAttribute.apply();
				PersistenceHelper.manager.modify(container);
				SessionServerHelper.manager.setAccessEnforced(true);
				currentContainerIsRestricted = false;
				return currentContainerIsRestricted;
			}
		} catch (WTException e) {
			e.printStackTrace();
			LOGGER.error("Exception has occured while checking the isRestrictedAttribute for the container "
					+ container.getName());
		}
		LOGGER.debug(CLASSNAME + "isRestrictedAttribute() method Ended");
		return currentContainerIsRestricted;
	}

	public boolean isAllowedContainerforRestricted(WTContainer restrictedContainer, WTContained currentObj) {
		boolean isAllowedContainer = false;
		Set<String> allowedContainerList = getAttributeValues(new HashSet<String>(), restrictedContainer,
				RESTRICTED_CONTEXT_AUTHORIZED_CONTAINERS_INTRNAL_NAME);
		allowedContainerList.add(restrictedContainer.getName());

		LOGGER.debug(CLASSNAME + "isAllowedContainerforRestricted() method started");
		if (allowedContainerList.contains(currentObj.getContainerName())) {
			isAllowedContainer = true;
			return isAllowedContainer;
		}
		LOGGER.debug(CLASSNAME + "isAllowedContainerforRestricted() method ended");
		return isAllowedContainer;
	}

	/**
	 * @since Build 3.4
	 * 
	 *        Checks if the incoming unrestricted container is authorized to use
	 *        incoming restricted container
	 */

	@Override
	public boolean isAuthorizedRestrictedContainer(WTContainer restrictedContainer, WTContainer unRestrictedContainer) {
		LOGGER.debug(CLASSNAME + ": isAuthorizedRestrictedContainer(): START");
		LOGGER.debug("Passed Restricted Container Name:" + restrictedContainer.getName());
		LOGGER.debug("Passed UnRestricted Container Name:" + unRestrictedContainer.getName());
		Set<String> authorizedContainersList = getAttributeValues(new HashSet<String>(), restrictedContainer,
				RESTRICTED_CONTEXT_AUTHORIZED_CONTAINERS_INTRNAL_NAME);
		LOGGER.debug("Authorized UnRestricted Containers List for Restricted Container " + restrictedContainer.getName()
				+ ": " + authorizedContainersList);
		boolean isAuthorizedRestrictedContainer = authorizedContainersList.contains(unRestrictedContainer.getName());
		if (isAuthorizedRestrictedContainer) {
			LOGGER.debug("The unrestricted container " + unRestrictedContainer.getName()
					+ " is authorized to use restricted context " + restrictedContainer.getName() + " data");
		} else {
			LOGGER.debug("The unrestricted container " + unRestrictedContainer.getName()
					+ " is not authorized to use restricted context " + restrictedContainer.getName() + " data");
		}
		return isAuthorizedRestrictedContainer;

	}

	/**
	 * @since Build 3.4
	 * 
	 *        Checks if the incoming downstream part state is at higher level than
	 *        its equivalent design part
	 * @return boolean
	 */
	@Override
	public boolean isMFGViewPartAtHigherMaturityThanDesignPart(WTPart mfgEquivalentDesignPart, State mfgViewPartState) {
		LOGGER.debug(CLASSNAME + ": isMFGViewPartAtHigherMaturityThanDesignPart(): START");
		State mfgEquivalentDesignPartState = mfgEquivalentDesignPart.getLifeCycleState();

		if (State.toState("A_RELEASE_CONCEPT").equals(mfgViewPartState)
				&& mfgEquivalentDesignPartState.equals(State.toState("INWORK"))) {
			LOGGER.debug("Downstream Part state " + mfgViewPartState
					+ " is higher than its equivalent Design Part state " + mfgEquivalentDesignPartState);
			return true;
		} else if (State.toState("B_RELEASE_CONCEPT").equals(mfgViewPartState)
				&& (mfgEquivalentDesignPartState.equals(State.toState("INWORK"))
						|| mfgEquivalentDesignPartState.equals(State.toState("A_RELEASE_CONCEPT"))
						|| mfgEquivalentDesignPartState.equals(State.toState("B_INWORK")))) {
			LOGGER.debug("Downstream Part state " + mfgViewPartState
					+ " is higher than its equivalent Design Part state " + mfgEquivalentDesignPartState);
			return true;

		} else if (State.toState("C_RELEASE_CONCEPT").equals(mfgViewPartState)
				&& (mfgEquivalentDesignPartState.equals(State.toState("INWORK"))
						|| mfgEquivalentDesignPartState.equals(State.toState("A_RELEASE_CONCEPT"))
						|| mfgEquivalentDesignPartState.equals(State.toState("B_INWORK"))
						|| mfgEquivalentDesignPartState.equals(State.toState("B_RELEASE_CONCEPT"))
						|| mfgEquivalentDesignPartState.equals(State.toState("C_INWORK")))) {
			LOGGER.debug("Downstream Part state " + mfgViewPartState
					+ " is higher than its equivalent Design Part state " + mfgEquivalentDesignPartState);
			return true;

		} else if (State.toState("PRODUCTIONRELEASED").equals(mfgViewPartState)
				&& (mfgEquivalentDesignPartState.equals(State.toState("INWORK"))
						|| mfgEquivalentDesignPartState.equals(State.toState("A_RELEASE_CONCEPT"))
						|| mfgEquivalentDesignPartState.equals(State.toState("B_INWORK"))
						|| mfgEquivalentDesignPartState.equals(State.toState("B_RELEASE_CONCEPT"))
						|| mfgEquivalentDesignPartState.equals(State.toState("C_INWORK"))
						|| mfgEquivalentDesignPartState.equals(State.toState("C_RELEASE_CONCEPT"))
						|| mfgEquivalentDesignPartState.equals(State.toState("PRODUCTION_INWORK")))) {
			LOGGER.debug("Downstream Part state " + mfgViewPartState
					+ " is higher than its equivalent Design Part state " + mfgEquivalentDesignPartState);
			return true;
		}

		return false;

	}

	/**
	 * Utility method for ADO-7441
	 */
	@Override
	public Map<String, ArrayList<WTPart>> sortPart(Map<String, ArrayList<WTPart>> numberViewSortMap,
			WTPart equivalentPart) {

		String key = equivalentPart.getNumber() + "-" + equivalentPart.getViewName();

		if (numberViewSortMap.containsKey(key)) {

			ArrayList<WTPart> partList = (ArrayList<WTPart>) numberViewSortMap.get(key);
			partList.add(equivalentPart);

		} else {

			ArrayList<WTPart> partList = new ArrayList<WTPart>();
			partList.add(equivalentPart);
			numberViewSortMap.put(key, partList);
		}
//		}

		return numberViewSortMap;
	}

	/**
	 * Utility method for ADO-7441
	 */
	@Override
	public ArrayList<WTPart> getSortedPart(Map<String, ArrayList<WTPart>> numberViewSortMap) {
		ArrayList<WTPart> finalList = new ArrayList<WTPart>();
		Iterator<?> number_View_Sort_Itr = numberViewSortMap.entrySet().iterator();
		while (number_View_Sort_Itr.hasNext()) {
			Entry<String, ArrayList<WTPart>> pair = (Entry<String, ArrayList<WTPart>>) number_View_Sort_Itr.next();
			ArrayList<WTPart> toBeSorted = pair.getValue();
			Map<BigDecimal, WTPart> sortPartMap = new HashMap<BigDecimal, WTPart>();
			// Build 3.8
			Set<BigDecimal> decimalSortSet = new TreeSet<>();
			for (WTPart part : toBeSorted) {
				sortPartMap.put(new BigDecimal(part.getPersistInfo().getObjectIdentifier().getId()), part);
				decimalSortSet.add(new BigDecimal(part.getPersistInfo().getObjectIdentifier().getId()));
			}

			Set<BigDecimal> set = sortPartMap.keySet();

			TreeSet<BigDecimal> treeSet = new TreeSet<>(set);
			// String first = treeSet.first();
			BigDecimal last = treeSet.last();
			// String.valueOf(last);
			// finalList.add(sortPartMap.get(first));
			finalList.add(sortPartMap.get(last));
			LOGGER.debug("Final Upstream Part considered  " + EnerSysLogUtils.format(sortPartMap.get(last)));

		}
		return finalList;

	}

	public NavigationCriteria changingViewOfNavCriteria(View targetPartView,
			NavigationCriteria tartgetPartNavCriteria) {
		LOGGER.debug(CLASSNAME + "The method changingViewOfNavCriteria() Started.");
		// v3.4 --- Added for ticket 7496 -- Enable SMD export for Plant BOM
		List<ConfigSpec> currentConfigSpecList = tartgetPartNavCriteria.getConfigSpecs();
		WTPartStandardConfigSpec currentPartConfigSpec = new WTPartStandardConfigSpec();
		for (ConfigSpec currentConfigSpec : currentConfigSpecList) {
			if (currentConfigSpec instanceof WTPartConfigSpec) {
				currentPartConfigSpec = ((WTPartConfigSpec) currentConfigSpec).getStandard();
			} else if (currentConfigSpec instanceof WTPartStandardConfigSpec) {
				currentPartConfigSpec = (WTPartStandardConfigSpec) currentConfigSpec;
			}
			try {
				currentPartConfigSpec.setView(targetPartView);
			} catch (WTPropertyVetoException e) {
				LOGGER.debug(
						"Error in getFilteredPartAsPerSpec while changing the view for filter and navigation criteria");
				e.printStackTrace();
			}
			String ViewName = currentPartConfigSpec.getView().getIdentity();
			LOGGER.debug("Viewname part : " + ViewName);
		}
		tartgetPartNavCriteria.setConfigSpecs(currentConfigSpecList);
		// v3.4 --- Added for ticket 7496 -- Enable SMD export for Plant BOM
		LOGGER.debug(CLASSNAME + "The method changingViewOfNavCriteria() Ended.");
		return tartgetPartNavCriteria;
	}

	/**
	 * Added in Build V3.12 - 8085 This method is used to get the attribute value
	 * for the given internal name. Persistable obj : object. String internalName :
	 * internal name of the attribute. return String : returns the attribute value.
	 */
	@Override
	public String getAttributeValues(Persistable obj, String interName) throws WTException {
		PersistableAdapter pa = new PersistableAdapter(obj, null, SessionHelper.getLocale(), null);
		pa.load(interName);
		return (String) pa.get(interName);
	}

	/**
	 * Added in Build V3.12 - 8085 This method is used to Set the attribute value.
	 * Persistable obj : object. String internalName : internal name of the
	 * attribute. String attributeValue : Attribute value to be set.
	 */
	@Override
	public void setAttributeValues(Persistable obj, String internalName, String attributeValue) throws WTException {
		PersistableAdapter pa = new PersistableAdapter(obj, null, SessionHelper.getLocale(), null);
		pa.load(internalName);
		pa.set(internalName, attributeValue);
		obj = pa.apply();
		IBAValueDBService ibaserv = new IBAValueDBService();
		ibaserv.updateAttributeContainer((IBAHolder) obj,
				((DefaultAttributeContainer) ((IBAHolder) obj).getAttributeContainer()).getConstraintParameter(), null,
				null);
		PersistenceServerHelper.manager.update(obj);
	}

	/**
	 * Build 3.10: ADO-9108 - This utility method takes input as validation criteria
	 * and to be compared type internal name and checks whether both are of same
	 * type
	 * 
	 */
	@Override
	public boolean isValidationCriteriaHasExactType(UIValidationCriteria criteria,
			String toBeComparedTypeInternalName) {
		LOGGER.debug("START: isValidationCriteriaHasExactType()");
		try {
			// Create Page
			TypeIdentifier objectType = criteria.getObjectTypeBeingCreated();

			// For Edit page
			NmCommandBean cBean = NmCommandBean.getNmCommandBean(criteria.getFormData());
			if (objectType == null) {
				objectType = TypeIdentifierHelper.getType(criteria.getContextObject().getObject());
			}
			LOGGER.debug("Object type identified from validation criteria:" + objectType);
			LOGGER.debug("Passed 'to be compared type internal name':" + toBeComparedTypeInternalName);

			if (objectType != null) {
				TypeIdentifier toBeComparedTI = TypeIdentifierHelper.getTypeIdentifier(toBeComparedTypeInternalName);
				if (objectType.equals(toBeComparedTI)) {
					LOGGER.debug(
							"Object type identified from validation criteria and type passed to compare, are matched...returning true");
					return true;
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		LOGGER.debug(
				"Object type identified from validation criteria and type passed to compare, are not matched...returning false");
		return false;
	}

	@Override
	public Set<String> getSelectedReleaseTargetStateSet(Set<NmOid> resultedItemSet, NmCommandBean commandBean) {
		Set<String> selectedReleaseTargetStateList = new HashSet<>();
		HashMap<String, Object> comboBoxCTHashMapValues = commandBean.getComboBox();
		for (NmOid resultingItemOid : resultedItemSet) {
			String selectedLCStateConvertedFromReleaseTarget = "";
			Persistable resPer = resultingItemOid.getWtRef().getObject();
			if (resPer instanceof RevisionControlled) {
				RevisionControlled rcResObj = (RevisionControlled) resPer;
				String resultingItemObjectID = String.valueOf(rcResObj.getBranchIdentifier());
				if (resultedItemSet.size() != 0 && comboBoxCTHashMapValues.size() > 0) {
					for (Map.Entry<String, Object> entry : comboBoxCTHashMapValues.entrySet()) {
						if (entry.getKey().contains("changeTargetTransition")
								&& entry.getKey().contains(resultingItemObjectID)) {
							String releasedTargetState = (String) ((List) entry.getValue()).get(0);
							if (releasedTargetState != null && !releasedTargetState.isEmpty()) {
								if (releasedTargetState.equals("CHANGE")) {
									CM4Service cmdService = CM4ServiceUtility.getInstance();
									selectedLCStateConvertedFromReleaseTarget = cmdService
											.getChangeTransitionState(rcResObj);
								} else {
									releasedTargetState = getDisplayNameForTransition(releasedTargetState);
									releasedTargetState = getInternalNameForState(releasedTargetState);
									selectedLCStateConvertedFromReleaseTarget = releasedTargetState;
								}
							}
							selectedReleaseTargetStateList.add(selectedLCStateConvertedFromReleaseTarget);
							break;
						}
					} // end of hash map combo box iteration
				}
			}
		} // end of resulting items iteration

		return selectedReleaseTargetStateList;
	}

	/**
	 * 
	 * Added in Build v3.8 - Sprint 9 - 8206 - Get The display name from the
	 * Transition RB
	 * 
	 * @param roleMap
	 * @return boolean
	 */
	@Override
	public String getDisplayNameForTransition(String internalNameOfTransition) {
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
	@Override
	public String getInternalNameForState(String displayNameOfState) {
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
		}
		return stateInternalName;
	}

	@Override
	public String getDisplayNameForState(String internalNameOfState) {
		String stateDisplayName = null;
		State[] StateArray = State.getStateSet();
		for (State currentState : StateArray) {
			if (currentState.getValue().equals(internalNameOfState)) {
				stateDisplayName = currentState.getFullDisplay();
				System.out.println(stateDisplayName);
			}
		}
		if (stateDisplayName == null) {
			LOGGER.error("The Error in fetching the Display name for the State with the internal name (From StateRB) : "
					+ internalNameOfState);
		}
		return stateDisplayName;
	}

}
