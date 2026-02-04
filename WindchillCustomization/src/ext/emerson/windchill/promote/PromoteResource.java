package ext.emerson.windchill.promote;

import wt.util.resource.RBArgComment0;
import wt.util.resource.RBComment;
import wt.util.resource.RBEntry;
import wt.util.resource.RBUUID;
import wt.util.resource.WTListResourceBundle;

/**
 * @author Pooja Sah Resource files for all Promote actions
 */

@RBUUID("ext.emerson.windchill.promote.PromoteResource")
public class PromoteResource extends WTListResourceBundle {

@RBEntry("This is the localized text with a single substitution:  \"{0}\".")
@RBComment("A demonstration entry1 .")
@RBArgComment0("Any dfdvdfsg string...")
public static final String	DEMO_STRING						= "0";

@RBEntry("This is the localized text with a single substitution:  \"{0}\".")
@RBComment("A demonstration entry@ .")
@RBArgComment0("#####Comment0 string attempt 2")
public static final String	DEMO_STRING2					= "1";

@RBEntry("Rule Conflicts")
@RBComment("Rule Conflicts")
public static final String	RULE_CONFLICTS					= "customBusinessRules.customPromotionBusinessRulesResultStep.title";

// Business rules resource bundle entry START

@RBEntry("Promotion Validation for Designer role")
@RBComment("Name of the Custom rule set that will be used to display to the user when the validation fails the check before starting promotion notice.")
public static final String	PROMOTION_RULESET_DESIGNER_NAME			= "PROMOTION_RULESET_DESIGNER_NAME";
@RBEntry("Custom Business Rule Set used before objects are promoted as part of the promotion process")
@RBComment("Description for the PROMOTION RULESET.")
public static final String	PROMOTION_RULESET_DESIGNER_DESC			= "PROMOTION_RULESET_DESIGNER_DESC";

@RBEntry("Promotion Validation")
@RBComment("Name of the Custom rule set that will be used to display to the user when the validation fails the check before starting promotion notice.")
public static final String	PROMOTION_RULESET_NAME			= "PROMOTION_RULESET_NAME";
@RBEntry("Custom Business Rule Set used before objects are promoted as part of the promotion process")
@RBComment("Description for the PROMOTION RULESET.")
public static final String	PROMOTION_RULESET_DESC			= "PROMOTION_RULESET_DESC";


@RBEntry("Promotables Validation")
@RBComment("Name of the Custom rule set that will be used to display to the user when the validation fails the check before starting promotion notice.")
public static final String	PROMOTABLE_RULESET_NAME			= "PROMOTABLE_RULESET_NAME";
@RBEntry("Custom Business Rule Set used before objects are promoted as part of the promotion process")
@RBComment("Description for the PROMOTION RULESET.")
public static final String	PROMOTABLE_RULESET_DESC			= "PROMOTABLE_RULESET_DESC";



@RBEntry("Type not valid for business rule")
@RBComment("Type not valid for business rule.")
public static final String	NOT_VALIDATABLE_TYPE			= "NOT_VALIDATABLE_TYPE";

@RBEntry("Warning: Participant(s) in Designer and Engineering Reviewer roles are the same. Please update Promotion Request with appropriate Engineering Reviewer, if needed")
@RBComment("Participant(s) in Designer and Engineering Reviewer roles are the same")
public static final String	PARTICIPANT_IS_DESIGNER_REVIEWER	= "PARTICIPANT_IS_DESIGNER_REVIEWER";



@RBEntry("Empty content check Promotion Notice Rule")
@RBComment("Rule Validator to check empty content of Promotable.")
public static final String	EMPTYCONTENT_RULE_NAME			= "EMPTYCONTENT_RULE_NAME";

@RBEntry("Rule Validator to check empty content of Promotable")
@RBComment("Description for EMPTYCONTENT_RULE_DESC")
public static final String	EMPTYCONTENT_RULE_DESC			= "EMPTYCONTENT_RULE_DESC";

@RBEntry("Primary content is empty for Promotables - \"{0}\".")
@RBComment("Description for EMPTYCONTENT_RULE error")
public static final String	EMPTYCONTENT_RULE_MESSAGE						= "EMPTYCONTENT_RULE_MESSAGE";


@RBEntry("Format check Promotion Notice Rule")
@RBComment("Rule Validator to check number format of Promotable.")
public static final String	FORMAT_RULE_NAME			= "FORMAT_RULE_NAME";

@RBEntry("Rule Validator to check number format of Promotable")
@RBComment("Description for FORMAT_RULE_DESC")
public static final String	FORMAT_RULE_DESC			= "FORMAT_RULE_DESC";

@RBEntry("Number Format incorrect for Promotables - \"{0}\".")
@RBComment("Description for FORMAT_RULE error")
public static final String	FORMAT_RULE_MESSAGE						= "FORMAT_RULE_MESSAGE";



@RBEntry("Same User Role Rule")
@RBComment("Rule Validator to check if duplicate users are entered on the Promotion Notice.")
public static final String	SAMEUSER_ROLE_RULE_NAME		= "SAMEUSER_ROLE_RULE_NAME";


@RBEntry("Rule Validator to check if duplicate users are entered on the Promotion Notice.")
@RBComment("Description for EMPTY_ROLE_RULE_DESC")
public static final String	SAMEUSER_ROLE_RULE_DESC		= "SAMEUSER_ROLE_RULE_DESC";

@RBEntry("Same user in Designer and Reviewer roles on this Promotion request -  \"{0}\".")
@RBComment("Description for EMPTY_ROLE_RULE error")
public static final String	SAMEUSER_ROLE_RULE				= "SAMEUSER_ROLE_RULE";



@RBEntry("Empty Role Rule for Designer")
@RBComment("Rule Validator to check if all required roles are entered on the Promotion Notice.")
public static final String	EMPTY_ROLE_RULE_FOR_DESIGNER_NAME		= "EMPTY_ROLE_RULE_FOR_DESIGNER_NAME";


@RBEntry("Rule Validator to check if all required roles are entered on the Promotion Notice.")
@RBComment("Description for EMPTY_ROLE_RULE_FOR_DESIGNER_DESC")
public static final String	EMPTY_ROLE_RULE_FOR_DESIGNER_DESC		= "EMPTY_ROLE_RULE_FOR_DESIGNER_DESC";

@RBEntry("Following roles are required to be filled on this Promotion request -  \"{0}\".")
@RBComment("Description for EMPTY_ROLE_FOR_DESIGNER  error")
public static final String	EMPTY_ROLE_FOR_DESIGNER				= "EMPTY_ROLE_FOR_DESIGNER";




@RBEntry("Empty Role Rule")
@RBComment("Rule Validator to check if all required roles are entered on the Promotion Notice.")
public static final String	EMPTY_ROLE_RULE_NAME		= "EMPTY_ROLE_RULE_NAME";

@RBEntry("Rule Validator to check if all required roles are entered on the Promotion Notice.")
@RBComment("Description for EMPTY_ROLE_RULE_DESC")
public static final String	EMPTY_ROLE_RULE_DESC		= "EMPTY_ROLE_RULE_DESC";

@RBEntry("Following roles are required to be filled on this Promotion request -  \"{0}\".")
@RBComment("Description for EMPTY_ROLE_RULE error")
public static final String	EMPTY_ROLE				= "EMPTY_ROLE";


// Business rules resource bundle entry END

}
