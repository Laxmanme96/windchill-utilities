package ext.ptpl.businessRule;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.businessRules.feedback.RuleFeedbackMessage;
import com.ptc.core.businessRules.feedback.RuleFeedbackType;
import com.ptc.core.businessRules.validation.RuleValidation;
import com.ptc.core.businessRules.validation.RuleValidationCriteria;
import com.ptc.core.businessRules.validation.RuleValidationKey;
import com.ptc.core.businessRules.validation.RuleValidationObject;
import com.ptc.core.businessRules.validation.RuleValidationResult;
import com.ptc.core.businessRules.validation.RuleValidationStatus;

import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.fc.WTReference;
import wt.lifecycle.State;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartUsageLink;
import wt.util.WTException;
import wt.util.WTMessage;
import wt.vc.VersionControlHelper;

/* ValidateBOMChildCanceleddStateBR is class to validate the Child of BOM againts the Canceled state
 * 
 * @author Laxman Baviskar
 */
public class ValidateBOMChildCanceleddStateBR implements RuleValidation {

	private static final Logger LOGGER = LogR.getLoggerInternal(ValidateBOMChildCanceleddStateBR.class.getName());
	private static final String RESOURCE = "ext.ptpl.businessRule.PluralBusinessRuleRB";
	static Set<WTPart> set = new HashSet<>();
	
	@Override
	public Class[] getSupportedClasses(RuleValidationKey arg0) {
		// TODO Auto-generated method stub
		return new Class[] { WTPart.class };
	}

	@Override
	public boolean isConfigurationValid(RuleValidationKey arg0) throws WTException {
		// TODO Auto-generated method stub
		return true;
	}

	/* this overrriden method "performValidation" is used to validate if BOM child is in canceled state
	 * 
	 * @params vKey 
	 * @params vObject
	 * @param vCriteria
	 */
	@Override
	public RuleValidationResult performValidation(RuleValidationKey vKey, RuleValidationObject vObject,
			RuleValidationCriteria vCriteria) throws WTException {
		// TODO Auto-generated method stub
		System.out.println("******* ValidateBOMChildCanceleddStateBR Started***********");
		RuleValidationStatus rulevalidationstatus = RuleValidationStatus.SUCCESS;
		RuleValidationResult ruleValidationResult = new RuleValidationResult(rulevalidationstatus);
		Persistable targetObject = (WTPart) vObject.getTargetObject().getObject();
		if (targetObject instanceof WTPart) {
			WTPart part = (WTPart) targetObject;
			LOGGER.debug("WTPart" + part.getNumber());
			System.out.println("WTPart" + part.getNumber());
			ObjectReference objRef = ObjectReference.newObjectReference(part);
			try {
				if (isChildStateCancelled(part)) {
					System.out.println("\"******* Child State is Canceled");
					String[] errorMessage = { part.getNumber() };
					ruleValidationResult = getValidationResult(objRef, vKey, errorMessage,
							"VALIDATE_BOM_CHILD_CANCELED_STATE");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LOGGER.error(
						"Error occured in perfromValdidation of ValidateBOMChildCanceleddStateBR" + e.getMessage());
			}
		}
		System.out.println("\"******* ValidateBOMChildCanceleddStateBR ENDED");
		return ruleValidationResult;
	}

	public boolean isChildStateCancelled(WTPart part) throws WTException, Exception {
		System.out.println("******* isChildStateCanceled Started ***********");
		getBOM(part);
		for (Object obj : set) {
			if (obj instanceof WTPart) {
				WTPart childPart = (WTPart) obj;
				System.out.println("Checking Child Part: " + childPart.getNumber());
				State state = childPart.getLifeCycleState();
				if (state.equals(State.toState("CANCELLED"))) {
					return true;
				}
			} else {
				System.out.println("Skipped non-WTPart object: " + obj.getClass());
			}
		}
		return false;
	}

	public WTPart getBOM(WTPart part) throws WTException {
		System.out.println("******* getBOM Started ***********");
	    set.clear();
	    part = (WTPart) VersionControlHelper.service.getLatestIteration(part, false);
	    QueryResult qr = WTPartHelper.service.getUsesWTPartMasters(part);
	    while (qr.hasMoreElements()) {
	        Object obj = qr.nextElement();
	        if (obj instanceof WTPartUsageLink) {
	            WTPartUsageLink ul = (WTPartUsageLink) obj;
	            WTPartMaster childPartMaster = (WTPartMaster) ul.getUses();
	            QueryResult childPartIterations = VersionControlHelper.service.allIterationsOf(childPartMaster);
	            WTPart childPart = (WTPart) childPartIterations.nextElement();
	            System.out.println("******* Child of Parent " + childPart.getNumber());
	            if (childPart instanceof WTPart) {
	                set.add((WTPart) childPart);
	               getChildrens((WTPart) childPart);
	                System.out.println("******* Printing all WTPart values in the set *******");
	                for (Object obj2 : set) {
	                    if (obj2 instanceof WTPart) {
	                        WTPart part2 = (WTPart) obj2;
	                        System.out.println("WTPart Number: " + part2.getNumber());
	                        System.out.println("WTPart Name: " + part2.getName());
	                    } else {
	                        System.out.println("Non-WTPart object in set: " + obj.getClass());
	                    }
	                }
	                
	            } else {
	                System.out.println("Skipping non-WTPart object in child iterations: " + childPart.getClass());
	            }

				getBOM(childPart);
	        } else {
	            System.out.println("Skipping non-WTPartUsageLink object: " + obj.getClass());
	        }
	    }

	    return part;
	}


	private static void getChildrens(WTPart childPart) throws WTException {
		System.out.println("******* getChildrens***********");
	    QueryResult qr2 = WTPartHelper.service.getUsesWTPartMasters(childPart);
	    while (qr2.hasMoreElements()) {
	    	System.out.println("******* Check4***********");
	        Object obj = qr2.nextElement();
	        if (obj instanceof WTPartUsageLink) {
	        	System.out.println("******* Check5***********");
	            WTPartUsageLink ul = (WTPartUsageLink) obj;
	            WTPartMaster subChildPartMaster = (WTPartMaster) ul.getUses();
	            QueryResult childPartIterations = VersionControlHelper.service.allIterationsOf(subChildPartMaster);
	            WTPart subChildPart = (WTPart) childPartIterations.nextElement();
	            System.out.println("******* Child of Parent " + subChildPart.getNumber());
	            if (subChildPart instanceof WTPart) {
	            	System.out.println("******* Check6***********");
	                set.add((WTPart) subChildPart);
	                getChildrens((WTPart) subChildPart);
	            } else {
	                System.out.println("Skipping non-WTPart object in subchild iterations: " + subChildPart.getClass());
	            }
	        } else {
	            System.out.println("Skipping non-WTPartUsageLink object: " + obj.getClass());
	        }
	    }
	}

	public RuleValidationResult getValidationResult(WTReference localWTReference,
			RuleValidationKey paramRuleValidationkey, String[] errorMessage, String validationMseeage) {
		LOGGER.debug("Inside getValidationResult()");
		RuleValidationStatus rulevalidationstatus = RuleValidationStatus.FAILURE;
		RuleValidationResult ruleValidationResult = new RuleValidationResult(rulevalidationstatus);
		ruleValidationResult.setTargetObject(localWTReference);
		ruleValidationResult.setValidationKey(paramRuleValidationkey);

		RuleFeedbackMessage feedbackMessage = new RuleFeedbackMessage(
				new WTMessage(RESOURCE, validationMseeage, errorMessage), RuleFeedbackType.WARNING);
		ruleValidationResult.addFeedbackMessage(feedbackMessage);
		return ruleValidationResult;
	}

	@Override
	public void prepareForValidation(RuleValidationKey arg0, RuleValidationCriteria arg1) throws WTException {
		// TODO Auto-generated method stub

	}

}
