package ext.ptpl.validator;

import java.util.Iterator;
import java.util.Locale;

import org.apache.logging.log4j.Logger;

import com.ptc.core.ui.validation.DefaultUIComponentValidator;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationResultSet;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.fc.WTReference;
import wt.lifecycle.LifeCycleState;
import wt.lifecycle.State;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.util.WTException;

public class DisableReviseSetstate extends DefaultUIComponentValidator {
	protected static final Logger logger = LogR.getLogger(DisableReviseSetstate.class.getName());

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public UIValidationResultSet performFullPreValidation(UIValidationKey vKey, UIValidationCriteria vCriteria,
			Locale locale) throws WTException {
		// TODO Auto-generated method stub
		System.out.println("---------- Disable Revise and Set State Validation Started-----------");
		UIValidationResult result = UIValidationResult.newInstance(vKey, UIValidationStatus.ENABLED);
		UIValidationResultSet resultset = UIValidationResultSet.newInstance();
		WTPart part = null;
		WTReference reference = vCriteria.getContextObject();
		Object obj = reference.getObject();

		if (obj instanceof WTPart) {
			part = (WTPart) obj;
			wt.fc.collections.WTCollection promotables = new wt.fc.collections.WTArrayList();
			promotables.add(part);

			wt.fc.collections.WTCollection promotionNotices = wt.maturity.MaturityHelper.service
					.getPromotionNotices(promotables);
			for (Iterator iterator = promotionNotices.iterator(); iterator.hasNext();) {
				wt.fc.ObjectReference or = (wt.fc.ObjectReference) iterator.next();
				wt.maturity.PromotionNotice pr = (wt.maturity.PromotionNotice) or.getObject();
				System.out.println(
						"Promotion Notice:" + pr.getName() + " State:" + pr.getState().getState().getDisplay());
				LifeCycleState state = pr.getState();
				System.out.println("States==>" + state);
				if (!state.equals(State.toState("APPROVED"))) {
					result = UIValidationResult.newInstance(vKey, UIValidationStatus.DISABLED);
					System.out.println("---------- Revise and Set State action is DISABLED-----------");
				}
			}
		}
		resultset.addResult(result);
		return resultset;
	}

}
