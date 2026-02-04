package ext.custom.listener;

import java.util.Iterator;

import wt.epm.EPMDocument;
import wt.epm.workspaces.EPMWorkspaceManagerEvent;
import wt.fc.collections.WTKeyedMap;
import wt.fc.collections.WTSet;
import wt.services.ManagerException;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.util.WTException;

public class CadCheckinToDefaultLocation extends StandardManager implements ListenerInterface{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8024776010521495069L;
	/** Static filed to store class name. **/
	private static final String CLASSNAME = CadCheckinToDefaultLocation.class.getName();
	/**
	 * Method to create Object of
	 * {@link CadCheckinToDefaultLocation}.
	 * new ListenerService must be implemented to instantiate the listener 
	 * @return the object of {@link CadCheckinToDefaultLocation}
	 * 
	 * @exception WTException
	 *                throws {@link WTException}
	 */
	public static CadCheckinToDefaultLocation newCadCheckinToDefaultLocation()throws WTException {
		CadCheckinToDefaultLocation instance = new CadCheckinToDefaultLocation();
		instance.initialize();
		return instance;
	}

	/**
	 * 
	 * Method performStartupProcess must be implemented to register the Listener Adapter
	 * against the EPMWorkspaceManagerEvent.POST_WORKSPACE_CHECKIN event
	 * @exception ManagerException
	 *                throws {@link ManagerException}
	 */
	protected synchronized void performStartupProcess() throws ManagerException {
		getManagerService().addEventListener(new ServiceEventListenerAdapter(CLASSNAME) {
			public void notifyVetoableEvent(Object event)
					throws WTException {
				System.out.println("Listener for CAD Document checkin");

				WTSet eventObj =((WTKeyedMap) ((EPMWorkspaceManagerEvent) event).getWIPMap()).wtKeySet();

				checkingWorkspace(eventObj);


			}

			private void checkingWorkspace(WTSet epmSet) throws WTException {
				System.out.println("inside checkingWorkspace method");
				Iterator<Object> it = epmSet.persistableIterator(EPMDocument.class, false);
				while (it.hasNext()) {
					Object currentObject = it.next();
					if(currentObject instanceof EPMDocument) {
						EPMDocument epm1 = (EPMDocument) currentObject;
						System.out.println("Container ::"+epm1.getContainerName());
						System.out.println("Container ::"+epm1.getLocation());
						if(epm1.getLocation().equalsIgnoreCase("/Default")) {
							throw new WTException("Not allowed to check in to Default folder . Please change folder Location and checkIn");

						}
					}
				}
			} 
		},
				EPMWorkspaceManagerEvent.generateEventKey(EPMWorkspaceManagerEvent.POST_WORKSPACE_CHECKIN));
	}  
	public String getConceptualClassName() {
		return CLASSNAME;
	}
}
/*
	<Property name="wt.services.service.99996" overridable="true"
	targetFile="codebase/wt.properties"
	value="ext.custom.listener.ListenerInterface/ext.custom.listener.CadCheckinToDefaultLocation"/> 
*/