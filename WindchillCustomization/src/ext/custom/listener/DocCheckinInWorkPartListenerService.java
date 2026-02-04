package ext.custom.listener;

import wt.doc.WTDocument;
import wt.events.KeyedEvent;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.part.WTPart;
import wt.part.WTPartDescribeLink;
import wt.part.WTPartHelper;
import wt.services.ManagerException;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.wip.WorkInProgressServiceEvent;

public class DocCheckinInWorkPartListenerService extends StandardManager
		implements DocCheckinInWorkPartListenerInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// get instance of class
	public static DocCheckinInWorkPartListenerService newDocCheckinInWorkPartListenerService() throws WTException {
		DocCheckinInWorkPartListenerService instance = new DocCheckinInWorkPartListenerService();
		instance.initialize();
		return instance;
	}

	private static final String CLASSNAME = DocCheckinInWorkPartListenerService.class.getName();

	public String getConceptualClassname() {
		return CLASSNAME;
	}

	@Override
	protected synchronized void performStartupProcess() throws ManagerException {
		getManagerService()
				.addEventListener(new ServiceEventListenerAdapter(DocCheckinInWorkPartListenerService.class.getName()) {
					public void notifyVetoableEvent(Object object) throws WTException, WTPropertyVetoException {
						KeyedEvent event = (KeyedEvent) object;
						Object target = event.getEventTarget();

						if (target instanceof WTDocument) {
						    WTDocument doc = ((WTDocument) ((WorkInProgressServiceEvent) event).getWorkingCopy());
						    //System.out.println("Verifying described part of document before check-in: " + doc.getNumber());
						    QueryResult partQR = WTPartHelper.service.getDescribesWTParts(doc);
						    while (partQR.hasMoreElements()) {
						        WTPart part = (WTPart) partQR.nextElement();
						        String state = part.getState().toString();
						        //System.out.println("Describes Part is in: " + state+ " state");
						        if (state.equalsIgnoreCase("INWORK")) {
						            throw new WTException("Check-in not allowed!!!\nOne or more described Part is in 'IN Work' state.");
						        }
						    }
						}
					}
				}, WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.PRE_CHECKIN));
	}
}