package ext.emerson.util;

import java.util.Iterator;

import org.apache.logging.log4j.Logger;

import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.collections.WTArrayList;
import wt.log4j.LogR;
import wt.maturity.PromotionNotice;
import wt.org.WTPrincipalReference;
import wt.workflow.engine.WfEngineHelper;
import wt.workflow.engine.WfVotingEventAudit;
import wt.workflow.work.WorkItem;
import wt.workflow.work.WorkflowHelper;

public class APPLWorkflowHelper {
	protected static final Logger LOGGER = LogR.getLogger(APPLWorkflowHelper.class.getName());

	public static void getPromotionNoticePBO(PromotionNotice pn) throws Exception {
		
		WorkItem task = null;
		QueryResult qs = WorkflowHelper.service.getWorkItems(pn);
		while (qs.hasMoreElements()) {
			task = (WorkItem) qs.nextElement();
		}
		
	
		WTPrincipalReference taskOwner = task.getOwnership().getOwner();
		WTArrayList auditCol = (wt.fc.collections.WTArrayList) WfEngineHelper.service.getVotingEvents(
				(wt.workflow.engine.WfProcess) ((wt.workflow.work.WfAssignedActivity) task.getSource().getObject())
						.getParentProcess(),
				null, null, null);
		@SuppressWarnings("rawtypes")
		Iterator auditEvents = auditCol.persistableIterator();
		WfVotingEventAudit wfVotingEventAudit = null;
		while (auditEvents.hasNext()) {
			wfVotingEventAudit = (WfVotingEventAudit) auditEvents.next();
			 System.out.println("Workitem :"+wfVotingEventAudit.getWorkItem().getOwnership().getOwner());
			String reviewerName = wfVotingEventAudit.getAssigneeRef().getFullName();
			 System.out.println("Reviewer Name :"+reviewerName);
		}
	}
}
