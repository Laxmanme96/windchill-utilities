package ext.ptpl.workflow;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.windchill.enterprise.maturity.forms.delegates.PromotionParticipantsFormDelegate;

import wt.lifecycle.LifeCycleManaged;
import wt.log4j.LogR;
import wt.org.WTPrincipal;
import wt.project.Role;
import wt.util.WTException;

public class CompletePRApproveActivity extends PromotionParticipantsFormDelegate {
	private static final Logger logger = LogR.getLoggerInternal(CompletePRApproveActivity.class.getName());

	// wt.maturity.PromotionNotice pbo = null;

	public void completePRApproveActivity(ObjectBean pbo) throws WTException {
		
		ObjectBean prObject = pbo;
		if (prObject instanceof LifeCycleManaged) {

			FormResult result;
			FeedbackMessage errors = new FeedbackMessage();
			Map<Role, Set<WTPrincipal>> mapselectedParticipants = getParticipantsToProcess(prObject);
			Set<WTPrincipal> approvers = mapselectedParticipants.get(Role.toRole("APPROVER"));

			System.out.println("mapselectedParticipants 2nd attempt :" + mapselectedParticipants);
			System.out.println("Approver selected: " + approvers);
		
	}

}
}
