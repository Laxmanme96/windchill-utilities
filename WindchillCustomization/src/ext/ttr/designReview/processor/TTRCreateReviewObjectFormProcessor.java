package ext.ttr.designReview.processor;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.FormResult;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.enterprise.change2.forms.processors.CreateChangeReviewFormProcessor;

//import ext.ttr.designReview.helper.TTRDesignReviewHelper;
import wt.change2.WTChangeReview;
import wt.log4j.LogR;
import wt.util.WTException;

public class TTRCreateReviewObjectFormProcessor extends CreateChangeReviewFormProcessor {

	private static final String CLASSNAME = TTRCreateReviewObjectFormProcessor.class.getName();
	private static final Logger logger = LogR.getLogger(CLASSNAME);

	@Override
	public FormResult doOperation(NmCommandBean bean, List<ObjectBean> paramList) throws WTException {
		System.out.println("***********Inside preProcess**********");
		final FormResult formResult = super.doOperation(bean, paramList);
		System.out.println("paramList size --->  " + paramList.size());
		try {
			boolean hasValid = false;

			WTChangeReview mainReview = null;
			for (final ObjectBean beanObj : paramList) {

				final Object obj = beanObj.getObject();
				if (obj instanceof WTChangeReview) {

					mainReview = (WTChangeReview) obj;
					logger.debug("Main Review Object : " + mainReview);
					System.out.println("Main Review Object ---->: " + mainReview.getName());

					// Resulting objects
					List<NmOid> resultingData = bean.getAddedItemsByName("changeReview_affectedData_table");
					logger.debug("resultingData Size ---->" + resultingData.size());
					System.out.println("resultingData Size ---->" + resultingData.size());

//					if (!(resultingData == null) || !(resultingData.isEmpty())) {
//						for (NmOid resultingObjectOid : resultingData) {
//							//Object persistableObj = (Persistable) TTRDesignReviewHelper
//									.getObjectByOid(resultingObjectOid);
//
//							if (persistableObj instanceof EPMDocument) {
//								EPMDocument epm = (EPMDocument) persistableObj;
//								System.out.println("EPM Document name ----> " + epm.getName());
//								String docType = epm.getDocType() != null ? epm.getDocType().toString().toLowerCase()
//										: "";
//								if (docType.equals("asm") || docType.equals("part")) {
//									hasValid = true;
//									System.out.println("EPMDocumet hasValid ---> " + hasValid);
//									break;
//								}
//							}
//
//							if (persistableObj instanceof WTPart) {
//								WTPart part = (WTPart) persistableObj;
//								System.out.println("WTPart name ----> " + part.getName());
//								hasValid = true;
//								System.out.println("WTPart hasValid ---> " + hasValid);
//								break;
//							}
//						}
//					}

					// Attachments
										
					List<NmOid> attachmentOids = bean.getAddedItemsByName("attachments.list.editable");
					logger.debug("attchamentsOid Size ---->" + attachmentOids.size());
					System.out.println("attchamentsOid Size ---->" + attachmentOids.size());
					
					if (!(attachmentOids == null) || !attachmentOids.isEmpty()) {
						for (NmOid attachmentOid : attachmentOids) {
							System.out.println("attachmentOid : " + attachmentOid);	
							
						}
					}

				}
			}

			if (!hasValid) {

				System.out.println("Feedback MEssage ************");
//					FeedbackMessage fb = new FeedbackMessage(FeedbackType.FAILURE, Locale.getDefault(), null, null,
//							"Please attach at least one '.ppt' file OR add a resulting object of type WTPart or EPMDocument with docType '.asm'");
//					formResult.setStatus(FormProcessingStatus.FAILURE);
//					formResult.addFeedbackMessage(fb);

			}

		} catch (Exception e) {
			e.printStackTrace();
//	        formResult.addException(e);
//	        formResult.setStatus(FormProcessingStatus.FAILURE);
		}
		return formResult;
	}

}