package ext.emerson.listener;

import java.beans.PropertyVetoException;
import java.io.Serializable;

import org.apache.logging.log4j.Logger;

import com.ptc.core.meta.common.TypeIdentifierHelper;

import wt.enterprise.EnterpriseServiceEvent;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentHelper;
import wt.epm.EPMDocumentMaster;
import wt.epm.EPMDocumentMasterIdentity;
import wt.fc.Identified;
import wt.fc.PersistenceManagerEvent;
import wt.fc.collections.WTKeyedHashMap;
import wt.fc.collections.WTKeyedMap;
import wt.log4j.LogR;
import wt.services.ManagerException;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

public class APPLEPMEventServiceImpl extends StandardManager implements APPLEPMEventService, Serializable {

	private static final long serialVersionUID = 5815089430340821313L;
	protected static final Logger LOGGER = LogR.getLogger(APPLEPMEventServiceImpl.class.getName());


	public static APPLEPMEventServiceImpl newAPPLEPMEventServiceImpl() throws WTException {
		APPLEPMEventServiceImpl applPartEventInstance = new APPLEPMEventServiceImpl();
		applPartEventInstance.initialize();
		return applPartEventInstance;
	}

	@Override
	protected synchronized void performStartupProcess() throws ManagerException {

		getManagerService()
				.addEventListener(new ServiceEventListenerAdapter(APPLEPMEventServiceImpl.class.getName()) {

			public void notifyVetoableEvent(Object postStoreEvent)
					throws WTException, PropertyVetoException, WTPropertyVetoException {
				LOGGER.debug("Inside APPLPartEventServiceImpl class POST_STORE event : START");

				PersistenceManagerEvent persistencePostCheckOutEvent = (PersistenceManagerEvent) postStoreEvent;
				LOGGER.debug("POST_STORE : persistencePostCheckOutEvent : " + persistencePostCheckOutEvent);
				Object target = persistencePostCheckOutEvent.getEventTarget();
				// get the EPM Document object
				if (target instanceof EPMDocument) {
					LOGGER.debug("target is instance of EPMDocument");

					Object object = persistencePostCheckOutEvent.getEventTarget();
					LOGGER.debug("POST_STORE : object : " + object);
					EPMDocument empDoc = ((EPMDocument) object);
					LOGGER.debug("POST_STORE : CAD Document : " + empDoc.getIdentity());
					String empType = TypeIdentifierHelper.getType(empDoc).toString();
					LOGGER.debug("POST_STORE : empType : " + empType);

					// String cadName = empDoc.getCADName();
					String cadNumber = empDoc.getNumber();
					LOGGER.debug("POST_STORE : CAD Number :" + cadNumber);

					if (cadNumber.contains(".drw")) {

						EPMDocumentMasterIdentity cadDocID = (EPMDocumentMasterIdentity) (((Identified) empDoc
								.getMaster()).getIdentificationObject());
						cadDocID.setName("CAD" + cadNumber);
						cadDocID.setNumber("CAD" + cadNumber);

						EPMDocumentMaster epmdocumentmaster = (EPMDocumentMaster) empDoc.getMaster();
						WTKeyedMap map = new WTKeyedHashMap(1);
						map.put(epmdocumentmaster, "CAD" + cadNumber + ".drw");
						EPMDocumentHelper.service.changeCADName(map);

					} else if (cadNumber.contains(".prt")) {

						EPMDocumentMasterIdentity cadDocID = (EPMDocumentMasterIdentity) (((Identified) empDoc
								.getMaster()).getIdentificationObject());
						cadDocID.setName("NCC" + cadNumber);
						cadDocID.setNumber("NCC" + cadNumber);
						EPMDocumentMaster epmdocumentmaster = (EPMDocumentMaster) empDoc.getMaster();
						WTKeyedMap map = new WTKeyedHashMap(1);
						map.put(epmdocumentmaster, "NCC" + cadNumber + ".prt");
						EPMDocumentHelper.service.changeCADName(map);
					} else if (cadNumber.contains(".asm")) {

						EPMDocumentMasterIdentity cadDocID = (EPMDocumentMasterIdentity) (((Identified) empDoc
								.getMaster()).getIdentificationObject());
						cadDocID.setName("NCC" + cadNumber);
						cadDocID.setNumber("NCC" + cadNumber);
						EPMDocumentMaster epmdocumentmaster = (EPMDocumentMaster) empDoc.getMaster();
						WTKeyedMap map = new WTKeyedHashMap(1);
						map.put(epmdocumentmaster, "NCC" + cadNumber + ".asm");
						EPMDocumentHelper.service.changeCADName(map);
					}
				}
			}
				}, EnterpriseServiceEvent.generateEventKey(EnterpriseServiceEvent.POST_COPY));
	}
}
