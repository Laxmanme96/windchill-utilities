	package ext.ptpl.businessRule;
	
	import com.ptc.core.businessRules.feedback.RuleFeedbackMessage;
	import com.ptc.core.businessRules.feedback.RuleFeedbackType;
	import com.ptc.core.businessRules.validation.RuleValidation;
	import com.ptc.core.businessRules.validation.RuleValidationCriteria;
	import com.ptc.core.businessRules.validation.RuleValidationKey;
	import com.ptc.core.businessRules.validation.RuleValidationObject;
	import com.ptc.core.businessRules.validation.RuleValidationResult;
	import com.ptc.core.businessRules.validation.RuleValidationStatus;
	import com.ptc.core.lwc.server.PersistableAdapter;
	import com.ptc.core.meta.common.DisplayOperationIdentifier;
	
	import wt.fc.ObjectReference;
	import wt.fc.Persistable;
	import wt.fc.WTReference;
	import wt.log4j.LogR;
	import wt.part.WTPart;
	import wt.session.SessionHelper;
	import wt.util.WTException;
	import wt.util.WTMessage;
	
	import org.apache.logging.log4j.*;
	
	public class PluralTestBusinessRule implements RuleValidation {
		private static final Logger LOGGER = LogR.getLoggerInternal(PluralTestBusinessRule.class.getName());
		private static final String RESOURCE = "ext.ptpl.businessRule.PluralBusinessRuleRB";
		private String billingCustomerName;
		private String customerCreditStatus;
	
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
	
		@Override
		public RuleValidationResult performValidation(RuleValidationKey arg0, RuleValidationObject arg1,
				RuleValidationCriteria arg2) throws WTException {
	
			LOGGER.debug("Inside performValidation()");
			// Object pbo paramRuleValidationCriteria.getPrimaryBusinessObject();
			RuleValidationStatus rulevalidationstatus = RuleValidationStatus.SUCCESS;
			RuleValidationResult ruleValidationResult = new RuleValidationResult(rulevalidationstatus);
			Persistable targetObject = (WTPart) arg1.getTargetObject().getObject();
	
			if (targetObject instanceof WTPart) {
				WTPart wtPart = (WTPart) targetObject;
				LOGGER.debug("WTPart" + wtPart.getNumber());
				ObjectReference objRef = ObjectReference.newObjectReference(wtPart);
				if ((!isValidBillingCustomer(wtPart))) {
					String[] errorMessage = { billingCustomerName, customerCreditStatus, wtPart.getNumber() };
					ruleValidationResult = getValidationResult(objRef, arg0, errorMessage,
							"ABS_BILLING_CUSTOMER_RULE_MESSAGE");
				}
			}
			return ruleValidationResult;
		}
	
		public boolean isValidBillingCustomer(WTPart wtPart) {
			LOGGER.debug("Inside isValidBillingCustomer()");
			boolean isValidBillingCustomer = false;
			try {
				PersistableAdapter getPart = new PersistableAdapter(wtPart, null, SessionHelper.getLocale(),
						new DisplayOperationIdentifier());
				getPart.load("CREDITSTATUS");
				customerCreditStatus = (String) getPart.get("CREDITSTATUS");
				getPart.load("CUSTOMERNAME");
				billingCustomerName = (String) getPart.get("CUSTOMERNAME");
				if ("CREDITED".equalsIgnoreCase(customerCreditStatus)) {
	
					isValidBillingCustomer = true;
				}
	
			} catch (WTException e) {
	
				e.printStackTrace();
			}
			return isValidBillingCustomer;
		}
	
		public RuleValidationResult getValidationResult(WTReference localWTReference,
				RuleValidationKey paramRuleValidationkey, String[] errorMessage, String validationMseeage) {
			LOGGER.debug("Inside getValidationResult()");
			RuleValidationStatus rulevalidationstatus = RuleValidationStatus.FAILURE ;
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
