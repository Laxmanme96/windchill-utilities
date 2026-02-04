package ext.custom.listener;

import wt.doc.WTDocument;
import wt.events.KeyedEvent;
import wt.fc.QueryResult;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.services.ManagerException;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.wip.WorkInProgressServiceEvent;

public class PartCheckinCancelledDocListenerService extends StandardManager implements CustomListenerInterface{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// get instance of class
	public static PartCheckinCancelledDocListenerService newPartCheckinCancelledDocListenerService() throws WTException {
		PartCheckinCancelledDocListenerService instance = new PartCheckinCancelledDocListenerService();
		instance.initialize();
		return instance;
	}
	private static final String CLASSNAME =PartCheckinCancelledDocListenerService.class.getName();
	public String getConceptualClassname() {
		return CLASSNAME;
	}
	@Override
	protected synchronized void performStartupProcess() throws ManagerException {
		getManagerService().addEventListener(new ServiceEventListenerAdapter(PartCheckinCancelledDocListenerService.class.getName()) {
			public void notifyVetoableEvent(Object object) throws WTException, WTPropertyVetoException {
				KeyedEvent event = (KeyedEvent) object;
				Object target = event.getEventTarget();

				if (target instanceof WTPart) {											
					WTPart part = ((WTPart) ((WorkInProgressServiceEvent) event).getWorkingCopy());
					QueryResult documentQR =  WTPartHelper.service.getDescribedByDocuments(part);
					while(documentQR.hasMoreElements())
					{
						WTDocument doc= (WTDocument) documentQR.nextElement();

							String state=doc.getState().toString();
							if(state.equalsIgnoreCase("Cancelled")) {
								throw new WTException("Check-in not allowed!!!\nOne or more described by document is in Canceled state.");
							}	
					}

				}
			}
		}, WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.PRE_CHECKIN));
	}
}