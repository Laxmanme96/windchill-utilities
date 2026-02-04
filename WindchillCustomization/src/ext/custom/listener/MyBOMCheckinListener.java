package ext.custom.listener;

import java.math.BigDecimal;

import wt.events.KeyedEvent;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartUsageLink;
import wt.services.ManagerException;
import wt.services.ServiceEventListenerAdapter;
import wt.services.StandardManager;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.VersionControlHelper;
import wt.vc.wip.WorkInProgressServiceEvent;
import wt.vc.wip.Workable;


public class MyBOMCheckinListener extends StandardManager implements MyBOMInterface {

	private static final long serialVersionUID = 1L;

	private static final String CLASSNAME =MyBOMCheckinListener.class.getName();

	public String getConceptualClassname() {
		return CLASSNAME;
	}

	//get instance of class
	public static MyBOMCheckinListener newMyBOMCheckinListener()
			throws WTException {
		MyBOMCheckinListener instance = new MyBOMCheckinListener();
		instance.initialize();
		return instance;
	}


	protected synchronized void performStartupProcess() throws ManagerException {
		//System.out.println("--------I am inside performStartupProcess() Method of Listener---------");
		getManagerService().addEventListener (new ServiceEventListenerAdapter(this.getConceptualClassname()) {
			public void notifyVetoableEvent( Object event ) throws WTException, WTPropertyVetoException {
				final Object target = ((KeyedEvent) event).getEventTarget();
				if ( target instanceof WTPart) {	
					WTPart part = ((WTPart) ((WorkInProgressServiceEvent) event).getWorkingCopy());
					System.out.println("Before Calling method : "+part.getNumber());
					checkInQuantityAttribute(part);
				}   
			}
		},
				WorkInProgressServiceEvent.generateEventKey(WorkInProgressServiceEvent.POST_CHECKIN));

	}

	protected void checkInQuantityAttribute(WTPart part) throws WTException {

		System.out.println("Object is instance of WTPart and currently checking Bom structure to get quantity attribute: ");

		//try {
		QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(part);
		while (qr.hasMoreElements()) {
		    WTPartUsageLink usageLink = (WTPartUsageLink) qr.nextElement();
		    double qty = usageLink.getQuantity().getAmount();
		    System.out.println("Quantity is------" + qty);

		    // Convert to BigDecimal for precision
		    BigDecimal bigDecimalQty = new BigDecimal(Double.toString(qty)).stripTrailingZeros();

		    // Extract digits before the decimal point
		    BigDecimal integerPart = bigDecimalQty.setScale(0, BigDecimal.ROUND_DOWN);
		    int numberOfDigits = integerPart.toPlainString().length();

		    System.out.println("Digits before decimal: " + numberOfDigits);

		    if (numberOfDigits > 3) {
		        throw new WTException("Quantity cannot exceed 3 digits before the decimal point (e.g., max 999.999).");
		    }
		}
	}

	/* Recursive method that will check the multibom and check the usage attribute Quantity */
	public void getChild(WTPart part, StringBuilder msg) {

		try {
			QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(part);
			while (qr.hasMoreElements()) {
				WTObject obj = (WTObject) qr.nextElement();
				if (obj instanceof WTPartUsageLink) {
					final WTPartUsageLink usageLink = (WTPartUsageLink) obj;
					WTPartMaster partMaster = usageLink.getUses();
					double qty = usageLink.getQuantity().getAmount();
					Double localDouble = qty;
					String stringVal = localDouble.toString();
					String val = stringVal.substring(stringVal.indexOf('.') + 1);
					if (val.length() > 3) {
						System.out.println("QuantityValidation :" + "quantity is :" + stringVal + ", " + val + " for "
								+ partMaster.getNumber() + "of parent " + part.getDisplayIdentifier().toString());
						if (msg.length() == 0) {
							msg.append(partMaster.getNumber());
						} else {
							msg.append("," + partMaster.getNumber());
						}
						System.out.println("QuantityValidation :msg :" + msg);
					}
					final QueryResult partMasterQS = VersionControlHelper.service.allIterationsOf(partMaster);
					final WTPart wtpart = (WTPart) (partMasterQS.hasMoreElements() ? partMasterQS.nextElement() : null);
					if (wtpart != null) {

						getChild(wtpart, msg);
					}
				}
			}
		} catch (WTException ex) {
			System.out.println("QuantityValidation Exception" + ex.getMessage());
		}

	}



}

/*
  <Property name="wt.services.service.99995" overridable="true"
  targetFile="codebase/wt.properties"
 value="ext.listener.MyBOMInterface/ext.listener.MyBOMCheckinListener"/>
 */