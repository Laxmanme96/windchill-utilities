package ext.enersys.service;

import java.io.Serializable;

import org.apache.logging.log4j.Logger;

import ext.enersys.listeners.EnerSysCOTSListenerHandler;
import ext.enersys.listeners.EnerSysCertificationDocumentListenerHandler;
import ext.enersys.listeners.EnerSysDocumentListenerHandler;
import ext.enersys.listeners.EnerSysDownstreamPartListenerHandler;
import ext.enersys.listeners.EnerSysESGListenerHandler;
import ext.enersys.listeners.EnerSysPartListenerHandler;
import ext.enersys.listeners.EnerSysPartNameLengthListenerHandler;
import ext.enersys.listeners.EnerSysPromotionRequestListenerHandler;
import ext.enersys.listeners.EnerSysQualityDocumentListenerHandler;
import ext.enersys.listeners.EnersysAssociatedDocListener;
import ext.enersys.listeners.EnersysDocLCReassignOnReviseListener;
import ext.enersys.listeners.EnersysEPMDocumentRenameListenerHandler;
import ext.enersys.listeners.EnersysSaveAsCriticalPartListenerHandler;
import ext.enersys.listeners.EnersysTimelineUpdateListener;
import ext.enersys.listeners.RequiredFieldsValidationListenerHandler;
import ext.enersys.listeners.RestrictedContextBOMControlListener;
import ext.enersys.listeners.VariantNumberControlListener;
import wt.enterprise.EnterpriseServiceEvent;
import wt.epm.workspaces.EPMWorkspaceManagerEvent;
import wt.events.KeyedEventListener;
import wt.fc.IdentityServiceEvent;
import wt.fc.PersistenceManagerEvent;
import wt.log4j.LogR;
import wt.part.PartDocServiceEvent;
import wt.services.ManagerException;
import wt.services.StandardManager;
import wt.util.WTException;
import wt.vc.VersionControlServiceEvent;
import wt.vc.wip.WorkInProgressServiceEvent;

public class EnerSysEventListenersManager extends StandardManager implements ListenersManager, Serializable {

	private static final long serialVersionUID = 1L;
	private static final String CLASSNAME = EnerSysEventListenersManager.class.getName();
	private final static Logger LOGGER = LogR.getLoggerInternal(EnerSysEventListenersManager.class.getName());
	private KeyedEventListener listener1;
	private KeyedEventListener listener2;
	private KeyedEventListener listener3;
	private KeyedEventListener listener4;
	private KeyedEventListener downstreamPartListener;
	private KeyedEventListener ESGListener;
	private KeyedEventListener COTSAssociationListener;
	private KeyedEventListener NameLengthConstraintListener;
	private KeyedEventListener restrcitedContextBOMControlListener;
	private KeyedEventListener requiredFieldsValidationListenerHandler;
	private KeyedEventListener timelineAttributeListener;
	private KeyedEventListener variantNumberControlListener;
	private KeyedEventListener EPMDocumentRenameListener; // For Jira 7673
	private KeyedEventListener reassignLCAssociatedDoc;
	private KeyedEventListener saveAsCriticalPartListener; // For Jira 7599 Build V3.7

	private KeyedEventListener reassignLifeCycleForDocOnRevise; // #DEVOPS:11100 - Buildv3.16
	private KeyedEventListener enersysDocumentListener; // ES - Migration - Plural

	public String getConceptualClassname() {
		return CLASSNAME;

	}

	public static EnerSysEventListenersManager newEnerSysEventListenersManager() throws WTException {
		EnerSysEventListenersManager instance = new EnerSysEventListenersManager();
		instance.initialize();
		return instance;
	}

	/**
	 * Add events that need to be listened to
	 */
	protected void performStartupProcess() throws ManagerException {
		LOGGER.debug("EnerSysEventListenersManager#performStartupProcess -- start");
		listener1 = new EnerSysQualityDocumentListenerHandler(this.getConceptualClassname());
		// listener for Part Revise Action
		listener2 = new EnerSysPartListenerHandler(this.getConceptualClassname());
		// Listener for Promotion Request JIRA-143
		listener3 = new EnerSysPromotionRequestListenerHandler(this.getConceptualClassname());
		// Listener for Certification Document AutoNaming JIRA-163
		listener4 = new EnerSysCertificationDocumentListenerHandler(this.getConceptualClassname());
		// Listener to ESG related events
		downstreamPartListener = new EnerSysDownstreamPartListenerHandler(this.getConceptualClassname());
		;
		ESGListener = new EnerSysESGListenerHandler(this.getConceptualClassname());
		// JIRA - 544 - Listener for COTS services
		COTSAssociationListener = new EnerSysCOTSListenerHandler(this.getConceptualClassname());
		// JIRA - 612 - Listener for Naming services services
		NameLengthConstraintListener = new EnerSysPartNameLengthListenerHandler(this.getConceptualClassname());

		// Listener to Restricted Context BOM Behavior control
		restrcitedContextBOMControlListener = new RestrictedContextBOMControlListener(this.getConceptualClassname());

		timelineAttributeListener = new EnersysTimelineUpdateListener(this.getConceptualClassname());

		requiredFieldsValidationListenerHandler = new RequiredFieldsValidationListenerHandler(
				this.getConceptualClassname());

		variantNumberControlListener = new VariantNumberControlListener(this.getConceptualClassname());

		// JIRA - 7673 - Listener for EPMDoument services services
		EPMDocumentRenameListener = new EnersysEPMDocumentRenameListenerHandler(this.getConceptualClassname());

		//// For Jira 7599 Build V3.7 - Updating the criticalPart AttributeValue while
		//// Save as Part is exceuted
		saveAsCriticalPartListener = new EnersysSaveAsCriticalPartListenerHandler(this.getConceptualClassname());
		// Add events that need to be listened to, you can add more than one, here is
		// the delete event POST_DELETE and modify the event POST_MODIFY
		reassignLCAssociatedDoc = new EnersysAssociatedDocListener(this.getConceptualClassname());

		// #DEVOPS:11100 - Buildv3.16
		reassignLifeCycleForDocOnRevise = new EnersysDocLCReassignOnReviseListener(this.getConceptualClassname());
		// All EnerSys Document Listener
		enersysDocumentListener = new EnerSysDocumentListenerHandler(this.getConceptualClassname());

		getManagerService().addEventListener(listener1,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));
		getManagerService().addEventListener(listener1,
				IdentityServiceEvent.generateEventKey(IdentityServiceEvent.POST_CHANGE_IDENTITY));
		getManagerService().addEventListener(listener1,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_DELETE));
		getManagerService().addEventListener(listener1,
				WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.POST_CHECKIN));
		// Enersys Part Event handler added for revise event.
		getManagerService().addEventListener(listener2,
				VersionControlServiceEvent.generateEventKey(VersionControlServiceEvent.NEW_VERSION));
		// Enersys Part Event handler added for POST_STORE event
		getManagerService().addEventListener(listener2,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));

		getManagerService().addEventListener(listener3,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));

		getManagerService().addEventListener(listener4,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));
		getManagerService().addEventListener(listener4,
				IdentityServiceEvent.generateEventKey(IdentityServiceEvent.POST_CHANGE_IDENTITY));
		getManagerService().addEventListener(listener4,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_DELETE));
		getManagerService().addEventListener(listener4,
				WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.POST_CHECKIN));

		getManagerService().addEventListener(downstreamPartListener,
				VersionControlServiceEvent.generateEventKey(VersionControlServiceEvent.NEW_VERSION));
		getManagerService().addEventListener(downstreamPartListener,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.PRE_STORE));
		getManagerService().addEventListener(downstreamPartListener,
				EPMWorkspaceManagerEvent.generateEventKey(EPMWorkspaceManagerEvent.POST_WORKSPACE_CHECKIN));

		// Adding event listener for POST_CHECKIN event
		// getManagerService().addEventListener(ESGListener,
		// PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));
		getManagerService().addEventListener(ESGListener,
				WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.POST_CHECKIN));
		getManagerService().addEventListener(ESGListener,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));
		// getManagerService().addEventListener(ESGListener,
		// PersistenceManagerEvent.generateEventKey(VersionControlServiceEvent.NEW_ITERATION));

		// Adding event listener for COTS services
		getManagerService().addEventListener(COTSAssociationListener,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));

		// Adding event listener for Name length Control for Parts
		getManagerService().addEventListener(NameLengthConstraintListener,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));
		getManagerService().addEventListener(NameLengthConstraintListener,
				IdentityServiceEvent.generateEventKey(IdentityServiceEvent.POST_CHANGE_IDENTITY));
		getManagerService().addEventListener(NameLengthConstraintListener,
				WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.POST_CHECKIN));

		// Listener to Restricted Context BOM Behavior control
		getManagerService().addEventListener(restrcitedContextBOMControlListener,
				WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.PRE_CHECKIN));
		getManagerService().addEventListener(restrcitedContextBOMControlListener,
				WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.POST_CHECKIN));
		getManagerService().addEventListener(restrcitedContextBOMControlListener,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));

		// Listener to handle empty required fields validation during workspace check in
		getManagerService().addEventListener(requiredFieldsValidationListenerHandler,
				EPMWorkspaceManagerEvent.generateEventKey(EPMWorkspaceManagerEvent.PRE_WORKSPACE_CHECKIN));
		getManagerService().addEventListener(timelineAttributeListener,
				WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.POST_CHECKIN));
		getManagerService().addEventListener(timelineAttributeListener,
				VersionControlServiceEvent.generateEventKey(VersionControlServiceEvent.NEW_VERSION));

		getManagerService().addEventListener(variantNumberControlListener,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));

		// Renumber & Rename CAD as per associated Owner link.
		getManagerService().addEventListener(EPMDocumentRenameListener,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));
		// For Jira 9195 Build - sSandbox/ProtoType process : Renumbering Part & EPM
		// document
		getManagerService().addEventListener(EPMDocumentRenameListener,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_MODIFY));

		// For Jira 7599 Build V3.7 - Updating the criticalPart AttributeValue while
		// Save as Part is exceuted
		getManagerService().addEventListener(saveAsCriticalPartListener,
				PersistenceManagerEvent.generateEventKey(PersistenceManagerEvent.POST_STORE));

		getManagerService().addEventListener(reassignLCAssociatedDoc,
				PartDocServiceEvent.generateEventKey(PartDocServiceEvent.ADD_PART_DESCRIBE_LINK));
		getManagerService().addEventListener(reassignLCAssociatedDoc,
				PartDocServiceEvent.generateEventKey(PartDocServiceEvent.ADD_PART_REFERENCE_LINK));
		getManagerService().addEventListener(reassignLCAssociatedDoc,
				PartDocServiceEvent.generateEventKey(PartDocServiceEvent.PART_RELATED_NEW_DOCUMENT));

		// #DEVOPS:11100 - Buildv3.16
		getManagerService().addEventListener(reassignLifeCycleForDocOnRevise,
				VersionControlServiceEvent.generateEventKey(VersionControlServiceEvent.NEW_VERSION));
		// Listener for Document revision
		getManagerService().addEventListener(enersysDocumentListener,
				VersionControlServiceEvent.generateEventKey(VersionControlServiceEvent.NEW_VERSION));
		getManagerService().addEventListener(enersysDocumentListener,
				WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.POST_CHECKIN));
		LOGGER.debug("EnerSysEventListenersManager#performStartupProcess -- end");
	}

}