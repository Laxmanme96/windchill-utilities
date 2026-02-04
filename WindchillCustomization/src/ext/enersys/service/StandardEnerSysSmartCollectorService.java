package ext.enersys.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.beans.CreateAndEditWizBean;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.enterprise.lifecycle.LifeCycleClientHelper;
import com.ptc.windchill.enterprise.maturity.PromotionRequestHelper;
import com.ptc.windchill.enterprise.maturity.beans.PromotionObjectBean;
import com.ptc.windchill.enterprise.maturity.beans.PromotionRequestDataUtilityBean;
import com.ptc.windchill.enterprise.part.commands.AssociationLinkObject;
import com.ptc.windchill.enterprise.part.commands.PartDocServiceCommand;
import com.ptc.windchill.suma.axl.AXLContext;
import com.ptc.windchill.suma.axl.AXLEntry;
import com.ptc.windchill.suma.axl.AXLHelper;
import com.ptc.windchill.suma.axl.AXLPreference;
import com.ptc.windchill.suma.part.ManufacturerPart;

import ext.enersys.cm3.CM3Helper;
import ext.enersys.smartCollector.bean.SmartCollectorUIInput;
import ext.enersys.utilities.Debuggable;
import ext.enersys.utilities.EnerSysLogUtils;
import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeRequest2;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.epm.EPMDocument;
import wt.epm.build.EPMBuildHistory;
import wt.epm.build.EPMBuildRule;
import wt.epm.structure.EPMDescribeLink;
import wt.epm.structure.EPMReferenceLink;
import wt.fc.BinaryLink;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.fc.collections.WTCollection;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTSet;
import wt.inf.container.OrgContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.lifecycle.State;
import wt.lifecycle.Transition;
import wt.log4j.LogR;
import wt.maturity.PromotionNotice;
import wt.org.WTOrganization;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartStandardConfigSpec;
import wt.preference.PreferenceClient;
import wt.preference.PreferenceHelper;
import wt.services.StandardManager;
import wt.type.TypedUtility;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.views.View;
import wt.vc.views.ViewHelper;
import wt.lifecycle.LifeCycleManaged;

public class StandardEnerSysSmartCollectorService extends StandardManager
		implements Serializable, EnerSysSmartCollectorService, Debuggable {

	private static final long serialVersionUID = 4855774077047893555L;
	private static final String CLASSNAME = StandardEnerSysSmartCollectorService.class.getName();
	private static final Logger LOGGER = LogR.getLoggerInternal(StandardEnerSysSmartCollectorService.class.getName());
	
	private static final String CREATE_TYPE = "createType";
	private static final String CREATE_TYPE_HANDLE = "!~objectHandle~task~!createType";
	private static final String OID = "oid";
	private static final String RELEASEDFLAGSTATE = "RELEASE";
	private static final String TABLESEPARATOR = "tableID=";
	public static final String ALLOWED_RELEASEDSTATES_ENERSYS = "/ext/enersys/ENERSYS_SMARTCOLLECTOR_PREFERENCES/VAIDRELEASEDSTATESFORENERSYS";
	public static final String ALLOWED_RELEASEDSTATES_FIRMWARE = "/ext/enersys/ENERSYS_SMARTCOLLECTOR_PREFERENCES/VAIDRELEASEDSTATESFORFRIMWARE";
	private static final String PREFERENCE_SEPARATOR = ";";
	public static StandardEnerSysSmartCollectorService newStandardEnerSysSmartCollectorService() throws WTException {
		StandardEnerSysSmartCollectorService service = new StandardEnerSysSmartCollectorService();
		service.initialize();
		return service;
	}
	
	@Override
	public String getSourceTableIdForSmartCollector(NmCommandBean commandBean) {
		String sourceTableIdForSmartCollector = "";
		try {
			HashMap<String, Object> allParameterSC = commandBean.getParameterMap();
			String[] contextValuesArr = (String[]) allParameterSC.get("context");
			String contextValue = contextValuesArr[0];
			if (contextValue.contains(PROMOTION_REQ_TABLE_STEP)) {
				sourceTableIdForSmartCollector = PROMOTION_REQ_TABLE_ID;
			}else if (contextValue.contains(AFFECTED_DATA_TABLE_STEP)) {
				sourceTableIdForSmartCollector = AFFECTED_DATA_TABLE_ID;
			}else if(contextValue.contains(ACTIVITY_DATA_TABLE_STEP)) {
				sourceTableIdForSmartCollector = ACTIVITY_DATA_TABLE_ID;				
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return sourceTableIdForSmartCollector;
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
	public HashSet<WTPart> getParentParts(HashSet<WTPart> parentPartSet,HashSet<TypeIdentifier> allowedPartTypesSet,WTPart childPart,String targetState,NmCommandBean commandBean ){
		try {
			WTPartStandardConfigSpec config = WTPartStandardConfigSpec.newWTPartStandardConfigSpec();
			View view = ViewHelper.service.getView(childPart.getViewName());
			if (view != null) {
				try {
					config.setView(view);
				} catch (WTPropertyVetoException e) {
					e.printStackTrace();
				}
				QueryResult usedByResultSet = WTPartHelper.service.getUsedByWTParts((WTPartMaster) childPart.getMaster());
				while (usedByResultSet.hasMoreElements()) {
					Persistable object = (Persistable) usedByResultSet.nextElement();
					if (object instanceof WTPart) {
						WTPart parentPart = (WTPart) object;
						if (allowedPartTypesSet.contains(TypeIdentifierHelper.getType(parentPart))) {
							if (isValidforTableofSmartCollector(parentPart,targetState,commandBean)) {
								parentPartSet.add(parentPart);
							}
						}
					}
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return parentPartSet;
	}

	/**
	 * This method is used to get all the childParts.
	 * 
	 * @param parentPart  : Part
	 * @param targetState : TargetState
	 * @return : level : false - First level, true - All levels
	 */
	@Override
	public HashMap<WTPart,HashSet<WTPart>> getChildParts(HashMap<WTPart,HashSet<WTPart>> childPartsMap,HashSet<TypeIdentifier> allowedPartTypesSet,WTPart parentPart, Boolean level, String targetState,NmCommandBean commandBean) {
		try {
			WTPartStandardConfigSpec config = WTPartStandardConfigSpec.newWTPartStandardConfigSpec();
			HashSet<WTPart> childSet = new HashSet<WTPart>();
			View view = ViewHelper.service.getView(parentPart.getViewName());
			if (view != null) {
				try {
					config.setView(view);
				} catch (WTPropertyVetoException e) {
					e.printStackTrace();
				}
			}
			QueryResult usesByResultSet = WTPartHelper.service.getUsesWTParts(parentPart, config);
			while (usesByResultSet.hasMoreElements()) {
				Persistable[] child = (Persistable[]) usesByResultSet.nextElement();
				Persistable object = child[1];
				if (object instanceof WTPart) {
					WTPart childPart = (WTPart) object;
					if (allowedPartTypesSet.contains(TypeIdentifierHelper.getType(childPart))) {
						if (isValidforTableofSmartCollector(childPart,targetState,commandBean)) {
							childSet.add(childPart);
						}
						if (Boolean.TRUE.equals(level)) {
							getChildParts(childPartsMap,allowedPartTypesSet,childPart, level, targetState,commandBean);
						}
					}
				}
			}
			childPartsMap.put(parentPart, childSet);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return childPartsMap;
	}


	/**
	 * This method is used to get the DescribeBy or ReferenceBy Documents based on
	 * the flag attribute.
	 * 
	 * @param part         : Part
	 * @param refByorDesBySet : Flag attribute i.e., ReferenceBy/DescribeBy/Both
	 * @return List : list of WTDocuments
	 * @throws WTException : WTException
	 */
	public HashMap<String,HashSet<WTDocument>> getDesByorRefByDocuments(WTPart part, Set refByorDesBySet, HashMap<String,HashSet<WTDocument>> allObjectsMap, String targetState, NmCommandBean commandBean)
	 {ReferenceFactory rf = new ReferenceFactory();
		try {
		// DescribeBy and ReferenceBy objects collection
		if (refByorDesBySet.contains(DESCRIBE_BY) || refByorDesBySet.contains(ALL)) {
			HashSet<WTDocument> describeByDocsSet = new HashSet<WTDocument>();
			QueryResult qr2 = PersistenceHelper.manager.navigate(part, ROLE_B_OBJECT, wt.part.WTPartDescribeLink.class,
					true);
			while (qr2.hasMoreElements()) {
				WTDocument wtDoc = (WTDocument) qr2.nextElement();
				QueryResult qr3 = wt.vc.VersionControlHelper.service.allVersionsOf(wtDoc);
				if (qr3.hasMoreElements()) {
					WTDocument docObject = (WTDocument) qr3.nextElement();
					if (isValidforTableofSmartCollector(docObject,targetState,commandBean)) {
						LOGGER.debug("Object details :: " + docObject.getDisplayIdentifier());
						describeByDocsSet.add(docObject);
					}
				}
			}
			allObjectsMap.put(DESCRIBE_BY, describeByDocsSet);
		}
		if (refByorDesBySet.contains(REFERENCE_BY) || refByorDesBySet.contains(ALL)) {
			HashSet<WTDocument> referenceByDocsSet = new HashSet<WTDocument>();
			QueryResult qr2 = PersistenceHelper.manager.navigate(part, ROLE_B_OBJECT, wt.part.WTPartReferenceLink.class,
					true);
			while (qr2.hasMoreElements()) {
				WTDocumentMaster wtDocumentMaster = (WTDocumentMaster) qr2.nextElement();
				QueryResult qr3 = wt.vc.VersionControlHelper.service.allVersionsOf(wtDocumentMaster);
				if (qr3.hasMoreElements()) {
					WTDocument docObject = (WTDocument) qr3.nextElement();
					if (isValidforTableofSmartCollector(docObject,targetState,commandBean)) {
						referenceByDocsSet.add(docObject);
					}
				}
			}
			allObjectsMap.put(REFERENCE_BY, referenceByDocsSet);
		}
		}catch(WTException e) {
			e.printStackTrace();
		}
		return allObjectsMap;
	}

	/**
	 * This method is used to collect all EPM objects.
	 * 
	 * @param part            : Parent Part
	 * @param associationTypeSet : Type of association
	 * @return List : list of EPM Documents
	 * @throws WTException : WTException
	 */
	public HashMap<String,HashSet<EPMDocument>> getEPMDocuments(WTPart part, Set associationTypeSet, HashMap<String,HashSet<EPMDocument>> allObjectsMap, String targetState,NmCommandBean commandBean){
		try {
		ReferenceFactory rf = new ReferenceFactory();
		Collection<?> associatedCadParts = PartDocServiceCommand.getAssociatedCADDocumentsAndLinks(part);
		Iterator<?> itr = associatedCadParts.iterator();
		while (itr.hasNext()) {
			String association = "";
			AssociationLinkObject associationLinkObj = (AssociationLinkObject) itr.next();
			EPMDocument epmDoc = associationLinkObj.getCadObject();
			BinaryLink link = associationLinkObj.getLink();

			boolean validTargetState = isValidforTableofSmartCollector(epmDoc,targetState,commandBean);

			if (link instanceof EPMBuildHistory) {
				EPMBuildHistory ownerLink = (EPMBuildHistory) link;
				if ((ownerLink.getBuildType() == 7)) {
					association = OWNER;
				} else if ((ownerLink.getBuildType() == 2)) {
					association = CONTRIBUTING_CONTENT;
				} else if ((ownerLink.getBuildType() == 4)) {
					association = IMAGE;
				} else if ((ownerLink.getBuildType() == 6)) {
					association = CONTRIBUTING_IMAGE;
				}
			} else if (link instanceof EPMBuildRule) {
				EPMBuildRule ownerLink = (EPMBuildRule) link;
				// Owner contributing content link
				if ((ownerLink.getBuildType() == 7)) {
					association = OWNER;
				} else if ((ownerLink.getBuildType() == 2)) {
					association = CONTRIBUTING_CONTENT;
				} else if ((ownerLink.getBuildType() == 4)) {
					association = IMAGE;
				} else if ((ownerLink.getBuildType() == 6)) {
					association = CONTRIBUTING_IMAGE;
				}

			} else if (link instanceof EPMDescribeLink) {
				// epmDescribeLink.
				association = CONTENT;
			} else if (link instanceof EPMReferenceLink) {
				// EPMReferenceLink
				association = CALCULATED;
			} else {
				LOGGER.info("Link That is not processed" + EnerSysLogUtils.format(link));
			}
			if (associationTypeSet.contains(association) && validTargetState) {
				addToMap(allObjectsMap,association,epmDoc);
			}
		}
		}catch(WTException e) {
			e.printStackTrace();
		}
		return allObjectsMap;
	}

	/**
	 * Extracts AML Information from given COTS Part.<br>
	 * Conforms to basic rules.<br>
	 * 
	 * @param per
	 * @param sumaPartSet
	 * @return
	 */

	public HashSet<WTPart> getAMLPartInformation(Persistable per, HashSet<WTPart> sumaPartSet, String targetState,NmCommandBean commandBean) {
		ReferenceFactory rf = new ReferenceFactory();
		if (per instanceof WTPart) {
			WTPart prt = (WTPart) per;
			try {
				WTCollection sourcingContextColl = getEnerSysSourcingContexts();
				if (sourcingContextColl != null) {
					Iterator<?> srcContItr = sourcingContextColl.persistableIterator();
					while (srcContItr.hasNext()) {
						AXLContext srcContObj = (AXLContext) srcContItr.next();
						// Process Manufacturer Parts for given Sourcing Context
						WTCollection amlAXLEntries = AXLHelper.service.getAML(prt, srcContObj);
						LOGGER.debug("amlAXLEntries size " + amlAXLEntries.size());
						if (amlAXLEntries != null && !amlAXLEntries.isEmpty()) {
							Iterator<?> amlAXLEntriesItr = amlAXLEntries.persistableIterator();
							while (amlAXLEntriesItr.hasNext()) {
								AXLEntry axlEntryObj = (AXLEntry) amlAXLEntriesItr.next();
								ManufacturerPart manuPrt = axlEntryObj.getLatestManufacturerPart();
								if (!axlEntryObj.getAmlPreference().equals(AXLPreference.DO_NOT_USE)) {
									if (isValidforTableofSmartCollector(manuPrt,targetState,commandBean)) {
									sumaPartSet.add(manuPrt);
									}
								}
							}
						}
					}
				}
			} catch (WTException e) {
				LOGGER.error(e);
			}
		}
		return sumaPartSet;
	}

	/**
	 * Retrieves Sourcing Contexts defined for EnerSys Organization.<br>
	 * 
	 * @since Build v2.5
	 * @return
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
	
	private void addToMap(HashMap<String, HashSet<EPMDocument>> map, String key, EPMDocument document) {
	    map.computeIfAbsent(key, k -> new HashSet<>()).add(document);
	}
	@Override
	public boolean isValidforTableofSmartCollector (Persistable currentObject,String targetState,NmCommandBean commandBean) {
		boolean returnOutput = false;
		try {
		String sourceSCTabledID = getSourceTableIdForSmartCollector(commandBean);		
		if (sourceSCTabledID.equalsIgnoreCase(PROMOTION_REQ_TABLE_ID)) {
        WTSet promotableObjects = new WTHashSet();
        promotableObjects.add(currentObject);
		Map<Object, List<State>> currentTransitionMap;
			currentTransitionMap = LifeCycleClientHelper.getObjectTransitionStates(promotableObjects, Transition.PROMOTE);
		List<State> currentPromoteStates = currentTransitionMap.get(currentObject);
		for(State currentState : currentPromoteStates) {
			if (currentState.getValue().equalsIgnoreCase(targetState)) {
				returnOutput = true;
				return returnOutput;
			}
		}
		}else {
			TypeIdentifier objTI = TypedUtility.getTypeIdentifier(currentObject);
			
			WTOrganization org = EnerSysHelper.service.getEnerSysOrgContainer();
			OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(org);
			HashSet<String> validStateList = new HashSet<String>();
			if(currentObject instanceof LifeCycleManaged) {
			LifeCycleManaged currentLifeCycleManagedObj = (LifeCycleManaged) currentObject;
			String currentStateofObject = currentLifeCycleManagedObj.getState().getState().toString().toUpperCase();
			
			if(objTI.equals(SmartCollectorUIInput.FIRMWAREPART_TI)) {
				validStateList.clear();
				String subTypeFirmwarePreferenceValue = (String) PreferenceHelper.service.getValue(ALLOWED_RELEASEDSTATES_FIRMWARE, PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);	
				for (String str : subTypeFirmwarePreferenceValue.split(PREFERENCE_SEPARATOR)) {
					validStateList.add(str);
				}
				if (validStateList.contains(currentStateofObject)) {
					returnOutput = true;
					return returnOutput;
				}
			}else if (objTI.equals(SmartCollectorUIInput.ENERSYSPART_TI)) {
				validStateList.clear();
				String subTypeEnersysPreferenceValue = (String) PreferenceHelper.service.getValue(ALLOWED_RELEASEDSTATES_ENERSYS, PreferenceClient.WINDCHILL_CLIENT_NAME, orgContainer);
				for (String str : subTypeEnersysPreferenceValue.split(PREFERENCE_SEPARATOR)) {
					validStateList.add(str);
				}
				if (validStateList.contains(currentStateofObject)) {
					returnOutput = true;
					return returnOutput;
				}
			}
			else if(currentStateofObject.contains(RELEASEDFLAGSTATE)) {
					returnOutput = true;
					return returnOutput;
				}
			}
			
		}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return returnOutput;
	}
}
