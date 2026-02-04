package ext.enersys.workflow.es;

import ext.enersys.service.ESBusinessHelper;
import wt.change2.ChangeOrder2;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeReview;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.inf.library.WTLibrary;
import wt.maturity.PromotionNotice;
import wt.pdmlink.PDMLinkProduct;
import wt.util.WTPropertyVetoException;

public class EPMAttributeHelper {
	private static final String preferenceValue = "/ext/enersys/APPROVALS_ON_CAD";

	public static void setEPMAttributes(Object pbo) throws WTPropertyVetoException, Exception {
		WTContainerRef containerReference = null;

		// check library also
		if (pbo instanceof PromotionNotice) {
			PromotionNotice pn = (PromotionNotice) pbo;
			containerReference = pn.getContainerReference();
		} else if (pbo instanceof ChangeOrder2) {
			WTChangeOrder2 cn = (WTChangeOrder2) pbo;
			containerReference = cn.getContainerReference();
		} else if (pbo instanceof WTChangeReview) {
			WTChangeReview cr = (WTChangeReview) pbo;
			containerReference = cr.getContainerReference();
		}

		boolean featureFlag = ESBusinessHelper.getFrameWorkPreferenceValue(preferenceValue, containerReference);
		if (featureFlag) {
			GetApproversAndTimestamp.setApproversAndTimestamp(pbo);
		}
	}
}
