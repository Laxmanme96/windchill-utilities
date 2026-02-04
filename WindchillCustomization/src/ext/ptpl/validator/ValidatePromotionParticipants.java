package ext.ptpl.validator;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.enterprise.maturity.forms.delegates.PromotionParticipantsFormDelegate;

import ext.ptpl.rb.PluralBusinessRuleRB;
import wt.lifecycle.LifeCycleManaged;
import wt.log4j.LogR;
import wt.org.OrganizationServicesHelper;
import wt.org.WTGroup;
import wt.org.WTPrincipal;
import wt.org.WTUser;
import wt.project.Role;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.util.WTMessage;

public class ValidatePromotionParticipants extends PromotionParticipantsFormDelegate {

	private static final Logger logger = LogR.getLoggerInternal(ValidatePromotionParticipants.class.getName());
	private static final String RESOURCE = "ext.ptpl.rb.PluralBusinessRuleRB";
	int count = 0;

	public FormResult preProcess(NmCommandBean commandBean, List<ObjectBean> objectBeans) throws WTException {
		count++;
		logger.debug("count :" + count);

		System.out.println("******* ValidatePromotionParticipants Started***********");
		FeedbackMessage feedbackMessage = null;
		StringBuilder message = new StringBuilder();

		WTUser sessionuser = (WTUser) SessionHelper.manager.getPrincipal();
		System.out.println("****************** Session user is " + sessionuser.getName());

		for (ObjectBean objectBean : objectBeans) {
			Object object = objectBean.getObject();
			if (object instanceof LifeCycleManaged) {

				FormResult result;
				FeedbackMessage errors = new FeedbackMessage();
				Map<Role, Set<WTPrincipal>> mapselectedParticipants = getParticipantsToProcess(objectBean);
				Set<WTPrincipal> approvers = mapselectedParticipants.get(Role.toRole("APPROVER"));

				System.out.println("mapselectedParticipants 2nd attempt :" + mapselectedParticipants);
				System.out.println("Approver selected: " + approvers);

//				if (approvers == null || approvers.isEmpty()) {
//					result = new FormResult(FormProcessingStatus.FAILURE);
//					errors.addMessage(
//							WTMessage.getLocalizedMessage(RESOURCE, PluralBusinessRuleRB.APPROVER_IS_MISSING));
//					result.addFeedbackMessage(errors);
//					return result;
//
//				}
//
//				else
				if (approvers != null) {
					for (WTPrincipal principal : approvers) {

						if (principal instanceof WTUser) {
							System.out.println(
									"User selected : " + ((WTUser) principal).getName() + " " + principal.getClass());
							System.out.println("****************** Checking princiapl user  " + principal.getName());

							if (isPromotionCreator(principal, sessionuser)) {

								errors.addMessage(WTMessage.getLocalizedMessage(RESOURCE,
										PluralBusinessRuleRB.PARTICIPANT_IS_DESIGNER_REVIEWER));

								result = new FormResult(FormProcessingStatus.FAILURE);
								feedbackMessage = new FeedbackMessage(FeedbackType.FAILURE, Locale.getDefault(),
										"Title attribute value cannot be null or empty", null, message.toString());
								
								result.addFeedbackMessage(feedbackMessage);
								return result;
							}
						} else if (principal instanceof WTGroup) {
							System.out.println(
									"Group selected : " + ((WTGroup) principal).getName() + " " + principal.getClass());
							@SuppressWarnings("unchecked")
							Enumeration<WTUser> groupUsers = OrganizationServicesHelper.manager
									.members((WTGroup) principal, true);
							while (groupUsers.hasMoreElements()) {
								WTUser selectedUser = groupUsers.nextElement();
								// System.out.println("Found user in group : " + selectedUser.getName());
								if (isPromotionCreator(selectedUser, sessionuser)) {
									errors.addMessage(WTMessage.getLocalizedMessage(RESOURCE,
											PluralBusinessRuleRB.PARTICIPANT_IS_DESIGNER_REVIEWER));
									result = new FormResult(FormProcessingStatus.FAILURE);
									result.addFeedbackMessage(errors);
									return result;
								}
							}
						}

					}
				}

			}
		}
		return super.preProcess(commandBean, objectBeans);

	}

	/*
	 * Check if princpal is Promotion Creator
	 */
	private boolean isPromotionCreator(WTPrincipal principal, WTPrincipal sessionUser) throws WTException {
		if (principal instanceof WTUser) {

			if (sessionUser.getName().equals(principal.getName())) {
				return true;
			}
		}
		return false;
	}

}
