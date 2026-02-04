
package ext.emerson.windchill.promote.businessRules.engine;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.ptc.core.businessRules.validation.RuleValidationCriteria;
import com.ptc.core.businessRules.validation.RuleValidationResultSet;

import ext.emerson.properties.CustomProperties;
import ext.emerson.query.QueryHelper;
import wt.businessRules.BusinessRulesHelper;
import wt.fc.collections.WTCollection;
import wt.maturity.Promotable;
import wt.maturity.PromotionNotice;
import wt.util.WTException;

/**
 * @author Pooja Sah
 *
 */
public class BusinessRulesEngine {

private CustomProperties	props	= new CustomProperties(CustomProperties.PROMOTE);
private Logger			logger	= props.getlogger(BusinessRulesEngine.class.getName());

public RuleValidationResultSet executeRuleSet(Object primaryBusinessObject, String ruleSetname) throws WTException {

	// ruleSetname = PROMOTION_WORKFLOW_RULESET
	List<Promotable> promotables1 = QueryHelper.getPromotables((PromotionNotice) primaryBusinessObject);

	RuleValidationCriteria ruleValidnCriteria = new RuleValidationCriteria((WTCollection) promotables1,
			((PromotionNotice) primaryBusinessObject).getContainerReference());

	ruleValidnCriteria.setPrimaryBusinessObject(primaryBusinessObject);

	logger.debug("Executing " + ruleSetname + " for promotion notice : "
			+ ((PromotionNotice) primaryBusinessObject).getNumber());

	RuleValidationResultSet resultSet = BusinessRulesHelper.engine.execute(ruleSetname, ruleValidnCriteria);

	return resultSet;
}
}
