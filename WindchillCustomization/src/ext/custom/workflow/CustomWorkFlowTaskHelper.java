package ext.custom.workflow;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.logging.log4j.Logger;

import wt.fc.ObjectReference;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.log4j.LogR;
import wt.maturity.PromotionNotice;
import wt.org.WTPrincipal;
import wt.org.WTPrincipalReference;
import wt.org.WTUser;
import wt.pds.StatementSpec;
import wt.project.Role;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionHelper;
import wt.team.TeamHelper;
import wt.team.TeamManaged;
import wt.team.WTRoleHolder2;
import wt.util.WTException;
import wt.workflow.WfException;
import wt.workflow.engine.ProcessData;
import wt.workflow.engine.WfActivity;
import wt.workflow.engine.WfEventHelper;
import wt.workflow.engine.WfProcess;
import wt.workflow.engine.WfState;
import wt.workflow.work.WfAssignedActivity;
import wt.workflow.work.WorkItem;
import wt.workflow.work.WorkflowHelper;

/**
 * This is a helper class which is called from work flow on Approve Promotion
 * Request activity to complete the work item if approver and creator are same.
 *
 */
public class CustomWorkFlowTaskHelper {
	private static final Logger LOGGER = LogR.getLoggerInternal(CustomWorkFlowTaskHelper.class.getName());
	private static final String APPROVER = "APPROVER";
	private static final String Completed_By_System = "Completed by System";
	private static final String APPROVE = "Approve";

	/**
	 * This method is used to check if approver and creator of Promotion Request are same.
	 * 
	 * @param criteriaObject
	 * @return approversAndApproverCount
	 * @throws WTException 
	 * 
	 */
	public static String[] isApproverAndCreatorSame(WTObject criteriaObject) throws WTException {
		LOGGER.info("Method isApproverAndCreatorSame : START");
		boolean isValid = false ;
		int approverCount = 0;
		String[] approversAndApproverCount = new String[2];
		WTPrincipal creator=null;
		if (criteriaObject instanceof PromotionNotice) {
			PromotionNotice pn = (PromotionNotice) criteriaObject;
			try {
				creator = pn.getCreator().getPrincipal();
				LOGGER.debug("Creator Name :" + creator.getName().toString());
				WTRoleHolder2 team = TeamHelper.service.getTeam((TeamManaged) pn);
				if (team != null) {
					@SuppressWarnings("unchecked")
					Vector<Role> roles = team.getRoles();
					Enumeration<Role> enumRoles = roles.elements();
					while (enumRoles.hasMoreElements()) {
						Role role = (Role) enumRoles.nextElement();
						LOGGER.debug("role : "+role);
						if (role.toString().equalsIgnoreCase(APPROVER)) {
							Enumeration members = team.getPrincipalTarget(role);
							while (members.hasMoreElements()) {
								WTPrincipalReference roleUserRef = (WTPrincipalReference) members.nextElement();
								WTPrincipal principal = ((WTPrincipalReference) roleUserRef).getPrincipal();
								if ((principal!= null && creator!= null) && principal instanceof WTUser) {
									approverCount++;
									LOGGER.debug("Creator Name :" + creator.getName().toString());
									LOGGER.debug("Principal Name :" + principal.getName().toString());
									if (creator.getName().toString().equals(principal.getName().toString())) {
										LOGGER.debug("Creator Name matched with Approver Name");
										isValid = true;
									}
								}
							}
						}
					}
				}
			} catch (WTException e) {
				throw new WTException("Exception inside isApproverAndCreatorSame method. " + e.getLocalizedMessage());
			}
		}
		if(isValid && approverCount==1) {
			approversAndApproverCount[0] = "true";
			approversAndApproverCount[1] = String.valueOf(approverCount);
		}else {
			approversAndApproverCount[0] = "false";
			approversAndApproverCount[1] = String.valueOf(approverCount);
		}
		LOGGER.info("Method isApproverAndCreatorSame : END");
		return approversAndApproverCount ;
	}

	/**
	 * This method is used to auto-complete the workitem if approver and creator of Promotion Request are same.
	 * 
	 * @param queryResult
	 * @param creator
	 * @return 
	 * @throws WTException 
	 * @throws WfException 
	 * 
	 */
	public static void workItemCompletion(QueryResult queryResult, String creator) throws WfException, WTException {
		LOGGER.info("Method workItemCompletion : START");
		try {
			while(queryResult.hasMoreElements()) 
			{
				WorkItem workItem = (WorkItem) queryResult.nextElement();
				WfActivity activity = (WfActivity) workItem.getSource().getObject();
				WTPrincipalReference roleUserRef = workItem.getOwnership().getOwner();
				WTPrincipal principal = ((WTPrincipalReference) roleUserRef).getPrincipal();
				String userName = principal.getName().toString();
				LOGGER.debug("creator : "+creator);
				LOGGER.debug("userName : "+userName);

				if(creator.equals(userName) && !workItem.isComplete()) {
					ProcessData processData = activity.getContext();
					LOGGER.debug("processData : "+processData);
					if(processData != null) {
						processData.setTaskComments(Completed_By_System);
						workItem = (WorkItem) PersistenceHelper.manager.save(workItem);
					}
					ArrayList<String> events = new ArrayList<>();
					events.add(APPROVE);
					WTPrincipalReference principalRef = WTPrincipalReference.newWTPrincipalReference(SessionHelper.manager.getAdministrator());
					WorkflowHelper.service.workComplete(workItem, principalRef, new Vector<String>(events));
					WfEventHelper.createVotingEvent(null, activity, workItem, principalRef, Completed_By_System, new Vector<String>(events), false, false);
				}
				LOGGER.debug("Task : "+workItem);
			}
		}catch (WTException e) {
			throw new WTException("Exception inside workItemCompletion method. " + e.getLocalizedMessage());
		}	
		LOGGER.info("Method workItemCompletion : END");
	}

	/**
	 * This method is used to get the workitem if it exists from the assigned activity.
	 * 
	 * @param assignedActivity
	 * @return QueryResult
	 * @throws WTException 
	 * 
	 */
	public static QueryResult getWorkItem(WfAssignedActivity assignedActivity) throws WTException {
		LOGGER.info("Method getWorkItem : START");
		QuerySpec querySpec = new QuerySpec(WorkItem.class);
		querySpec.setAdvancedQueryEnabled(true);
		querySpec.appendWhere(new SearchCondition(WorkItem.class, "source.key", SearchCondition.EQUAL,
				PersistenceHelper.getObjectIdentifier(assignedActivity)), new int[] { 0 });
		LOGGER.debug("querySpec : " + querySpec);
		LOGGER.info("Method getWorkItem : END");
		return PersistenceServerHelper.manager.query(querySpec);
	}

	/**
	 * This method is used to get the assigned activity from the process object reference.
	 * 
	 * @param self
	 * @return QueryResult
	 * @throws WTException 
	 * 
	 */
	public static QueryResult getAssignedActivity(ObjectReference self) throws WTException {
		LOGGER.info("Method getAssignedActivity : START");
		WfAssignedActivity activity = null;
		if(self.getObject() instanceof WfProcess) {
			WfProcess process = (WfProcess) self.getObject();
			LOGGER.debug("process : " + process);
			long processOid = process.getPersistInfo().getObjectIdentifier().getId();
			LOGGER.debug("processOid : " + processOid);
			QuerySpec assignedActivityQuerySpec = new QuerySpec(WfAssignedActivity.class);
			assignedActivityQuerySpec.appendWhere(new SearchCondition(WfAssignedActivity.class, "parentProcessRef.key.id", SearchCondition.EQUAL, processOid),new int[] { 0 });
			assignedActivityQuerySpec.appendAnd();
			assignedActivityQuerySpec.appendWhere(new SearchCondition(WfAssignedActivity.class, "state", SearchCondition.EQUAL, WfState.OPEN_RUNNING),new int[] { 0 });
			LOGGER.debug("assignedActivityQuerySpec : " + assignedActivityQuerySpec);
			QueryResult queryResult = PersistenceHelper.manager.find((StatementSpec)assignedActivityQuerySpec);
			while(queryResult.hasMoreElements()) {
				activity = (WfAssignedActivity) queryResult.nextElement();
			}
		}
		LOGGER.info("Method getAssignedActivity : END");
		//get workitem from activity
		return getWorkItem(activity);
	}
	
	/**
	 * This method is used to check the number of approvers selected for PR approval.
	 * 
	 * @param criteriaObject
	 * @return approverCount
	 * @throws WTException 
	 * 
	 */
	public static int approverCount(WTObject criteriaObject) throws WTException {
		LOGGER.info("Method approverCount : START");
		int approverCount = 0;
		WTPrincipal creator=null;
		if (criteriaObject instanceof PromotionNotice) {
			PromotionNotice pn = (PromotionNotice) criteriaObject;
			try {
				creator = pn.getCreator().getPrincipal();
				LOGGER.debug("Creator Name :" + creator.getName().toString());
				WTRoleHolder2 team = TeamHelper.service.getTeam((TeamManaged) pn);
				if (team != null) {
					@SuppressWarnings("unchecked")
					Vector<Role> roles = team.getRoles();
					Enumeration<Role> enumRoles = roles.elements();
					while (enumRoles.hasMoreElements()) {
						Role role = (Role) enumRoles.nextElement();
						LOGGER.debug("role : "+role);
						if (role.toString().equalsIgnoreCase(APPROVER)) {
							Enumeration members = team.getPrincipalTarget(role);
							while (members.hasMoreElements()) {
								WTPrincipalReference roleUserRef = (WTPrincipalReference) members.nextElement();
								WTPrincipal principal = ((WTPrincipalReference) roleUserRef).getPrincipal();
								if (principal instanceof WTUser) {
									approverCount++;
								}
							}
						}
					}
				}
			} catch (WTException e) {
				throw new WTException("Exception inside approverCount method. " + e.getLocalizedMessage());
			}
		}
		LOGGER.info("Method approverCount : END");
		return approverCount ;
	}
}