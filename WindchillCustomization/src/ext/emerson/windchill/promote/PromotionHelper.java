package ext.emerson.windchill.promote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.ptc.netmarkets.util.beans.NmCommandBean;

import ext.emerson.properties.CustomProperties;
import ext.emerson.query.QueryHelper;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTCollection;
import wt.maturity.Promotable;
import wt.util.WTException;

public class PromotionHelper {

private static final Logger logger = CustomProperties.getlogger(PromotionHelper.class.getName());

public static WTCollection getPromotables(NmCommandBean paramNmCommandBean, boolean paramBoolean) {
	String str1 = paramNmCommandBean.getTextParameter("table_data_manager_store");

	WTCollection localArrayList = new WTArrayList();
	try {
		JSONObject localJSONObject1 = str1 == null ? new JSONObject() : new JSONObject(str1);
		// Iterator iter = localJSONObject1.keys();
		JSONObject localJSONObject2 = localJSONObject1.getJSONObject("promotionRequest.promotionObjects");
		Iterator localIterator = localJSONObject2.keys();
		while (localIterator.hasNext()) {
			String str2 = (String) localIterator.next();

			JSONObject localJSONObject3 = (JSONObject) localJSONObject2.get(str2);
			Object localObject1;
			if (paramBoolean) {

				localObject1 = QueryHelper.getObject(str2);

				localArrayList.add((Promotable) localObject1);

			} else {
				localObject1 = localJSONObject3.keys();
				while (((Iterator) localObject1).hasNext()) {
					String str3 = (String) ((Iterator) localObject1).next();
					if (str3.equals("_promotionStatus")) {
						Object localObject2 = localJSONObject3.get(str3);
						if (localObject2 instanceof String) {
							String str4 = (String) localObject2;
							if (str4.equals("true")) {

								Object localPromotable = QueryHelper.getObject(str2);
								localArrayList.add((Promotable) localPromotable);
							}
						}
					}
				}
			}
		}
	} catch (Exception localException) {
	}
	return localArrayList;
}

public static List<String> getMaturityState(NmCommandBean paramNmCommandBean, boolean paramBoolean) throws WTException {

	String[] maturityStateParam = (String[]) paramNmCommandBean.getParameterMap().get("maturityState");
	List<String> maturityStateList;
	if (maturityStateParam != null) {
		maturityStateList = Arrays.asList(maturityStateParam);

	} else {
		maturityStateList = (ArrayList) paramNmCommandBean.getChangedComboBox().get("maturityState");
	}

	if (maturityStateList == null) {

		String[] temp = (String[]) paramNmCommandBean.getParameterMap()
				.get("promotionRequest$promotionStateSelection$$___maturityState___combobox");

		if (temp != null) {
			maturityStateList = Arrays.asList(temp);
		}
	}
	// logger.debug("maturityState :" + maturityStateList);

	return maturityStateList;
}

}
