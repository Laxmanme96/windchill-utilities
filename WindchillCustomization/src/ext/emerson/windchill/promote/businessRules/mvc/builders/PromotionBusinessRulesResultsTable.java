
package ext.emerson.windchill.promote.businessRules.mvc.builders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import com.ptc.core.businessRules.engine.BusinessRuleSetBean;
import com.ptc.core.businessRules.validation.RuleValidationCriteria;
import com.ptc.core.businessRules.validation.RuleValidationResultSet;
import com.ptc.core.businessRules.validation.RuleValidationStatus;
import com.ptc.mvc.components.ComponentBuilder;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.enterprise.businessRules.mvc.builders.AbstractBusinessRulesResultsTable;

import ext.emerson.properties.CustomProperties;
import ext.emerson.windchill.promote.PromotionHelper;
import wt.businessRules.BusinessRulesHelper;
import wt.fc.collections.WTCollection;
import wt.maturity.PromotionNotice;
import wt.maturity.PromotionTarget;
import wt.org.OrganizationServicesHelper;
import wt.org.WTPrincipal;
import wt.session.SessionHelper;
import wt.util.WTException;

/**
 * @formoff
 * @author Pooja Sah
 *
 *         Builder class for Business Rules.
 *
 *         1. In case of displaying table in custom JSP, use
 *         <jsp:include page="${mvc:getComponentURL('PromotionBusinessRulesResultsTable')}" />
 *
 *
 *         2. In case of displaying table in Promotion workflow, refer to instructions and method body,
 *         public void promotionWorkflowBusinessRulesImpl(Object primaryBusinessObject)
 *
 *
wt.maturity.PromotionNotice pn = (wt.maturity.PromotionNotice)primaryBusinessObject;

try
{
    result="Valid";
    businessRuleValidationMsg =  com.ptc.windchill.enterprise.maturity.PromotionNoticeWorkflowHelper.refresh(pn);

    if (!businessRuleValidationMsg.isEmpty() ) {
      result="InvalidIterations";
      businessRuleValidationMsg = com.ptc.windchill.enterprise.maturity.PromotionNoticeWorkflowHelper.truncateInstructions(businessRuleValidationMsg);
    }

    com.ptc.core.businessRules.engine.BusinessRuleSetBean defaultBean = com.ptc.core.businessRules.engine.BusinessRuleSetBean.newBusinessRuleSetBean("PROMOTION_RULESET", com.ptc.core.businessRules.engine.BusinessRuleSetBean.PRIMARY_BUSINESS_OBJECT);
    com.ptc.core.businessRules.engine.BusinessRuleSetBean[] beans = new com.ptc.core.businessRules.engine.BusinessRuleSetBean[] { defaultBean };
    businessRulesResultSetGlobal = "";
    businessRuleConflictsMsg = "";
    com.ptc.core.businessRules.validation.RuleValidationResultSet resultSet = wt.businessRules.BusinessRulesHelper.engine.execute(primaryBusinessObject, beans);

     if (resultSet.hasResultsByStatus(com.ptc.core.businessRules.validation.RuleValidationStatus.FAILURE))
     {
        result="Invalid";
        businessRulesResultSetGlobal = wt.businessRules.BusinessRulesHelper.serialize(resultSet);
        businessRuleConflictsMsg = new wt.util.WTMessage("com.ptc.windchill.enterprise.maturity.maturityClientResource",
		com.ptc.windchill.enterprise.maturity.maturityClientResource.BUSINESS_RULES_VALIDATION_MSG, null).getLocalizedMessage();
        if (!businessRuleValidationMsg.isEmpty() ) {
           businessRuleValidationMsg = businessRuleValidationMsg + "\n";
        }
        businessRuleValidationMsg = businessRuleValidationMsg +  businessRuleConflictsMsg;
        businessRuleConflictsMsg = businessRuleConflictsMsg + "\n" + resultSet.getFailedRulesMessage(java.util.Locale.getDefault());
    }
    special_instructions = businessRuleValidationMsg;
 }
catch( Exception wte )
{
     wte.printStackTrace();
}
 *
 *         3. In case of displaying table in custom workflow, refer to instructions and method body,
 *         public void workflowBusinessRulesImpl(ContentHolder primaryBusinessObject)
 * @formon
 */
@ComponentBuilder("PromotionBusinessRulesResultsTable")
public class PromotionBusinessRulesResultsTable extends AbstractBusinessRulesResultsTable {

private CustomProperties	props	= new CustomProperties(CustomProperties.PROMOTE);
private final Logger	logger	= CustomProperties.getlogger(PromotionBusinessRulesResultsTable.class.getName());

/**
 * {@inheritDoc}
 */
@Override
public RuleValidationResultSet getRuleValidationResultSet(NmCommandBean commandBean) throws WTException {
	RuleValidationResultSet resultSet = new RuleValidationResultSet();

	NmOid contextOid = commandBean.getActionOid();

	WTCollection promotables = PromotionHelper.getPromotables(commandBean, false);

	if (promotables.size() > 0) {

		RuleValidationCriteria ruleValidnCriteria = new RuleValidationCriteria(promotables,
				commandBean.getContainerRef());
		ArrayList<Object> list = new ArrayList<>();

		if (contextOid.isA(PromotionNotice.class)) {
			logger.debug("Context object for rules validation is a promotion notice.");
			ruleValidnCriteria.setPrimaryBusinessObject(contextOid.getRefObject());
		}
		logger.debug("Executing the PROMOTION_RULESET for the promotion notice resulting objects.");
		resultSet = BusinessRulesHelper.engine.execute("PROMOTION_RULESET", ruleValidnCriteria);
	}
	String validresult = "SUCCESS";
	HashMap localHashMap = commandBean.getText();

	if (resultSet.hasResultsByStatus(RuleValidationStatus.FAILURE)) {

		WTPrincipal currentPrincipal = SessionHelper.manager.getPrincipal();

		if (!isInAdminGrp(currentPrincipal)) {
			validresult = "FAILURE";
		} else {
			logger.info("Original status = Failure but since admin validresult = " + validresult);
		}
	}
	logger.info("After validation, result in PromotionBusinessRulesResultsTable.java is " + validresult);
	commandBean.addToMap(localHashMap, "validresult", validresult, true);
	commandBean.addRequestDataParam("validresult", validresult, true);
	commandBean.getRequest().setAttribute("validresult", validresult);

	return resultSet;
}

private boolean isInAdminGrp(WTPrincipal currentPrincipal) throws WTException {

	String adminGrpName = props.getProperty("ext.emerson.windchill.promote.adminGroup");

	if (adminGrpName != null && !adminGrpName.equals("")) {
		String[] services = OrganizationServicesHelper.manager.getDirectoryServiceNames();

		// for site groups:
		wt.org.DirectoryContextProvider dc_provider = OrganizationServicesHelper.manager
				.newDirectoryContextProvider(services, null);
		wt.org.WTGroup poolGroup = wt.org.OrganizationServicesHelper.manager.getGroup(adminGrpName, dc_provider);


		if (OrganizationServicesHelper.manager.isMember(poolGroup, currentPrincipal)) {
logger.info(
					"User : " + currentPrincipal.getName() + " is in Admin Group to bypass validations : " + poolGroup);

			return true;
		}
	}

	return false;

}


// These are OOTB workflow variables. You do not need to define them in Promotion workflow.
//These have beeen defined here just so that there are no errors in code below.
String result, businessRuleValidationMsg, businessRulesResultSetGlobal, businessRuleConflictsMsg, special_instructions;

/**
 * @param primaryBusinessObject
 *            This is to include business rule validators in Promotion workflow. This method has not been tested on a normal workflow. Use it as is but change the name of Ruleset as mentioned here
 *            com.ptc.core.businessRules.engine.BusinessRuleSetBean defaultBean = com.ptc.core.businessRules.engine.BusinessRuleSetBean .newBusinessRuleSetBean("DOWNLOAD_RULESET",
 *            "wt.maturity.PromotionTarget");
 * @throws IOException
 *
 */
public void promotablesWorkflowBusinessRulesImpl(Object primaryBusinessObject) throws IOException {

	wt.maturity.PromotionNotice pn = (wt.maturity.PromotionNotice) primaryBusinessObject;
	result = "Valid";

		com.ptc.core.businessRules.engine.BusinessRuleSetBean defaultBean = com.ptc.core.businessRules.engine.BusinessRuleSetBean
				.newBusinessRuleSetBean("PROMOTABLE_RULESET", "wt.maturity.PromotionTarget");
		com.ptc.core.businessRules.engine.BusinessRuleSetBean[] beans = new com.ptc.core.businessRules.engine.BusinessRuleSetBean[] {
				defaultBean };
		businessRulesResultSetGlobal = "";
		businessRuleConflictsMsg = "";
		businessRuleValidationMsg="";

		com.ptc.core.businessRules.validation.RuleValidationResultSet resultSet = wt.businessRules.BusinessRulesHelper.engine
				.execute(primaryBusinessObject, beans);

		if (resultSet.hasResultsByStatus(com.ptc.core.businessRules.validation.RuleValidationStatus.FAILURE)) {
			result = "Invalid";
			businessRulesResultSetGlobal = wt.businessRules.BusinessRulesHelper.serialize(resultSet);
			businessRuleConflictsMsg = new wt.util.WTMessage(
					"com.ptc.windchill.enterprise.maturity.maturityClientResource",
					com.ptc.windchill.enterprise.maturity.maturityClientResource.BUSINESS_RULES_VALIDATION_MSG, null)
							.getLocalizedMessage();
			if (!businessRuleValidationMsg.isEmpty()) {
				businessRuleValidationMsg = businessRuleValidationMsg + "\n";
			}
			businessRuleValidationMsg = businessRuleValidationMsg + businessRuleConflictsMsg;
			businessRuleConflictsMsg = businessRuleConflictsMsg + "\n"
					+ resultSet.getFailedRulesMessage(java.util.Locale.getDefault());
		}
		special_instructions = businessRuleValidationMsg;

}
public void promotionWorkflowBusinessRulesImpl(Object primaryBusinessObject) {

	wt.maturity.PromotionNotice pn = (wt.maturity.PromotionNotice) primaryBusinessObject;
	try {
		result = "Valid";

		com.ptc.core.businessRules.engine.BusinessRuleSetBean defaultBean = com.ptc.core.businessRules.engine.BusinessRuleSetBean
				.newBusinessRuleSetBean("PROMOTION_RULESET", com.ptc.core.businessRules.engine.BusinessRuleSetBean.PRIMARY_BUSINESS_OBJECT);
		com.ptc.core.businessRules.engine.BusinessRuleSetBean[] beans = new com.ptc.core.businessRules.engine.BusinessRuleSetBean[] {
				defaultBean };
		businessRulesResultSetGlobal = "";
		businessRuleConflictsMsg = "";
		com.ptc.core.businessRules.validation.RuleValidationResultSet resultSet = wt.businessRules.BusinessRulesHelper.engine
				.execute(primaryBusinessObject, beans);

		if (resultSet.hasResultsByStatus(com.ptc.core.businessRules.validation.RuleValidationStatus.FAILURE)) {
			result = "Invalid";
			businessRulesResultSetGlobal = wt.businessRules.BusinessRulesHelper.serialize(resultSet);
			businessRuleConflictsMsg = new wt.util.WTMessage(
					"com.ptc.windchill.enterprise.maturity.maturityClientResource",
					com.ptc.windchill.enterprise.maturity.maturityClientResource.BUSINESS_RULES_VALIDATION_MSG, null)
							.getLocalizedMessage();
			if (!businessRuleValidationMsg.isEmpty()) {
				businessRuleValidationMsg = businessRuleValidationMsg + "\n";
			}
			businessRuleValidationMsg = businessRuleValidationMsg + businessRuleConflictsMsg;
			businessRuleConflictsMsg = businessRuleConflictsMsg + "\n"
					+ resultSet.getFailedRulesMessage(java.util.Locale.getDefault());
		}
		special_instructions = businessRuleValidationMsg;
	} catch (Exception wte) {
		wte.printStackTrace();
	}
}

}