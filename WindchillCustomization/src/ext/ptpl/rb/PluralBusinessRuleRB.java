package ext.ptpl.rb;
import wt.util.resource.RBEntry;
import wt.util.resource.RBUUID;
import wt.util.resource.WTListResourceBundle;

	@RBUUID("ext.ptpl.businessRule. ")
	public class PluralBusinessRuleRB extends WTListResourceBundle	{
	 
	@RBEntry("PTPL Business Rule Set")
	public static final String PTPL_BUSINESS_RULES = "PTPL_BUSINESS_RULES";
	@RBEntry("Folowing are the PTPL Custom Business Rule Set.")
	public static final String PTPL_BUSINESS_RULES_DESC = "PTPL_BUSINESS_RULES_DESC";
	@RBEntry("Validate Customer Credit status")
	public static final String VALIDATE_CUSTOMER_CREDIT_STATUS_RULE_NAME = "VALIDATE_CUSTOMER_CREDIT_STATUS_RULE_NAME";
	@RBEntry("This Rule Validates Customer Credit status.")
	public static final String VALIDATE_CUSTOMER_CREDIT_STATUS_RULE_DESC = "VALIDATE_CUSTOMER_CREDIT_STATUS_RULE_DESC";
	@RBEntry("The Billing Customer- '{0}' has invalid Credit Status'' and can not proceed with Change Notice- '{2}'")
	public static final String ABS_BILLING_CUSTOMER_RULE_MESSAGE = "ABS_BILLING_CUSTOMER_RULE_MESSAGE";
	@RBEntry("The affected part- '{0}' is missing in resulting objects'' and can not proceed with Change Notice- '{2}'")
	public static final String CHECK_AFFECTED_RESULTING_OBJECTS_MESSAGE = "CHECK_AFFECTED_RESULTING_OBJECTS_MESSAGE";
	@RBEntry("The BOM in resulting object- '{0}' has canceled state child'' and can not proceed with Change Notice- '{2}'")
	public static final String VALIDATE_BOM_CHILD_CANCELED_STATE = "VALIDATE_BOM_CHILD_CANCELED_STATE";
	
	@RBEntry("The Designer and Engineering Reviewer is same user")
	public static final String PARTICIPANT_IS_DESIGNER_REVIEWER = "PARTICIPANT_IS_DESIGNER_REVIEWER";
	
	@RBEntry("You must select at least one Approver")
	public static final String APPROVER_IS_MISSING = "APPROVER_IS_MISSING";

	
	
}
	


