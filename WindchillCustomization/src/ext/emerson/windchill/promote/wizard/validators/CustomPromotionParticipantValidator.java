package ext.emerson.windchill.promote.wizard.validators;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.enterprise.maturity.forms.delegates.PromotionParticipantsFormDelegate;

import ext.emerson.windchill.promote.PromoteResource;
import ext.emerson.properties.CustomProperties;
import wt.lifecycle.LifeCycleManaged;
import wt.org.OrganizationServicesHelper;
import wt.org.WTGroup;
import wt.org.WTPrincipal;
import wt.org.WTUser;
import wt.project.Role;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.util.WTMessage;

public class CustomPromotionParticipantValidator extends PromotionParticipantsFormDelegate {

	private CustomProperties props = new CustomProperties(CustomProperties.PROMOTE);

	boolean isCustomisationEnabled = Boolean
			.valueOf(props.getProperty("ext.emerson.windchill.promote.wizard.participantValidationEnabled", "true"))
			.booleanValue();

	private final Logger logger = CustomProperties.getlogger(CustomPromotionParticipantValidator.class.getName());
	private static final String RESOURCE = "ext.emerson.windchill.promote.PromoteResource";
	int count = 0;

	@Override
	public FormResult preProcess(NmCommandBean commandBean, List<ObjectBean> objectBeans)

			throws WTException {
		count++;
		logger.debug("count :" + count);

		// getParticipantsToProcess(arg0)
		WTUser sessionuser = (WTUser) SessionHelper.manager.getPrincipal();
		for (ObjectBean objectBean : objectBeans) {

			Object object = objectBean.getObject();

			if (object instanceof LifeCycleManaged) {

				FormResult result;
				FeedbackMessage errors = new FeedbackMessage();
				Map<Role, Set<WTPrincipal>> mapselectedParticipants = getParticipantsToProcess(objectBean);

				Set<WTPrincipal> wtprincipals = mapselectedParticipants.get(Role.toRole("ENG_REVIEWER"));
				logger.debug("mapselectedParticipants 2nd attempt :" + mapselectedParticipants);
				if (wtprincipals != null) {
					for (WTPrincipal principal : wtprincipals) {

						if (principal instanceof WTUser) {
							logger.debug(
									"User selected : " + ((WTUser) principal).getName() + " " + principal.getClass());

							if (isPromotionCreator(principal, sessionuser)) {

								errors.addMessage(WTMessage.getLocalizedMessage(RESOURCE,
										PromoteResource.PARTICIPANT_IS_DESIGNER_REVIEWER));

								result = new FormResult(FormProcessingStatus.NON_FATAL_ERROR);

								result.addFeedbackMessage(errors);

								return result;

							}
						} else if (principal instanceof WTGroup) {
							logger.debug(
									"Group selected : " + ((WTGroup) principal).getName() + " " + principal.getClass());
							Enumeration<WTUser> groupUsers = OrganizationServicesHelper.manager
									.members((WTGroup) principal, true);
							while (groupUsers.hasMoreElements()) {
								WTUser selectedUser = groupUsers.nextElement();
								logger.debug("Found user in group : " + selectedUser.getName());
								if (isPromotionCreator(selectedUser, sessionuser)) {
									errors.addMessage(WTMessage.getLocalizedMessage(RESOURCE,
											PromoteResource.PARTICIPANT_IS_DESIGNER_REVIEWER));

									result = new FormResult(FormProcessingStatus.NON_FATAL_ERROR);

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
