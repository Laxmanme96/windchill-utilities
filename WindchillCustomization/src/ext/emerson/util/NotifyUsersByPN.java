package ext.emerson.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.misc.NmAction;
import com.ptc.netmarkets.util.misc.NmActionServiceHelper;

import ext.emerson.properties.CustomProperties;
import wt.doc.WTDocument;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.fc.collections.WTArrayList;
import wt.mail.EMailMessage;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.part.WTPart;
import wt.workflow.engine.WfEngineHelper;
import wt.workflow.engine.WfProcess;
import wt.workflow.engine.WfVotingEventAudit;
import wt.workflow.work.WfAssignedActivity;
import wt.workflow.work.WorkItem;
import wt.workflow.work.WorkflowHelper;

public class NotifyUsersByPN {
	private static final Logger logger = CustomProperties.getlogger("ext.emerson.migration");

	/**
	 * @param pn
	 * @throws Exception
	 *
	 *                   wt.maturity.PromotionNotice pn =
	 *                   (wt.maturity.PromotionNotice)primaryBusinessObject;
	 *                   ext.emerson.migration.NotifyUsersByPN.getPromotionNoticePBO(pn);
	 */
	public static void getPromotionNoticePBO(PromotionNotice pn) throws Exception {
		PersistableAdapter pnObj = new PersistableAdapter(pn, null, null, null);
		WorkItem task = null;
		QueryResult qs = WorkflowHelper.service.getWorkItems(pn);
		while (qs.hasMoreElements()) {
			task = (WorkItem) qs.nextElement();
		}
		WTArrayList auditCol = (wt.fc.collections.WTArrayList) WfEngineHelper.service.getVotingEvents(
				(wt.workflow.engine.WfProcess) ((wt.workflow.work.WfAssignedActivity) task.getSource().getObject())
						.getParentProcess(),
				null, null, null);
		@SuppressWarnings("rawtypes")
		Iterator auditEvents = auditCol.persistableIterator();
		WfVotingEventAudit wfVotingEventAudit = null;
		while (auditEvents.hasNext()) {
			wfVotingEventAudit = (WfVotingEventAudit) auditEvents.next();
			break;
		}
		Set<String> mailSet = getNotifyUserMailSet(pnObj);
		if (!mailSet.isEmpty() && wfVotingEventAudit != null) {
			StringBuilder finalString = writeEmail(pn, task, wfVotingEventAudit);
			sendEmail(mailSet, finalString, pn);
		}
	}

	/**
	 * @param pnObj
	 * @throws Exception
	 */
	public static Set<String> getNotifyUserMailSet(PersistableAdapter pnObj) throws Exception {
		pnObj.load("notifyUsers");
		Set<String> mailSet = new HashSet<String>();
		if (pnObj.get("notifyUsers") != null) {
			String pnEmails = ((String) pnObj.get("notifyUsers")).toLowerCase();
			logger.debug("Emails: " + pnEmails);

			String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
					+ "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
			if (pnEmails.contains(",")) {
				String[] mailArray = pnEmails.split("\\s*,\\s*");
				List<String> mailList = Arrays.asList(mailArray);
				for (String mails : mailList) {
					mails.replaceAll("\\s+", "");
					if (mails.matches(regexPattern)) {
						mailSet.add(mails);
					} /*
						 * else { continue; }
						 */
				}
			} else if (!pnEmails.contains(",")) {
				pnEmails.replaceAll("\\s+", "");
				if (pnEmails.matches(regexPattern)) {
					mailSet.add(pnEmails);
				}
			}
		}
		return mailSet;
	}

	/**
	 * @param pn
	 * @param task
	 * @param wfVotingEventAudit
	 * @throws Exception
	 */
	public static StringBuilder writeEmail(PromotionNotice pn, WorkItem task, WfVotingEventAudit wfVotingEventAudit)
			throws Exception {
		Object obj = null;
		WTPart part = null;
		WTDocument doc = null;
		String href = null;
		href = getURLOfObject(pn);
		href = "<a href='" + href + "'>" + pn.getNumber() + "</a>";
		StringBuilder str = new StringBuilder();
		str.append("<!DOCTYPE html>" + "<html>" + "<head>"
				+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
				+ "<style data-merge-styles=\"true\"></style>" + "<meta charset=\"utf-8\">" + "</head>" + "<body>");
		str.append(
				"<table border=\"0\" cellspacing=\"0\" cellpadding=\"3\" min-scale=\"0.9761904761904762\" style=\"transform: scale(0.97619, 0.97619); transform-origin: left top;\">");
		str.append("<tbody>");
		str.append("<tr bgcolor=\"#124280\">");
		str.append(
				"<td colspan=\"2\"><font color=\"white\" face=\"Arial,Helvetica,sans-serif\"><b>Promotion Request Process Notification </b></font></td></tr>");
		str.append("<tr>\r\n"
				+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font face=\"Arial,Helvetica,sans-serif\"><b>Process Name : </b></font></td>\r\n"
				+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">"
				+ wfVotingEventAudit.getProcessName() + "</font></td></tr>");
		str.append("<tr>\r\n"
				+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font face=\"Arial,Helvetica,sans-serif\"><b>Number : </b></font></td>\r\n"
				+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">" + href
				+ "</font></td></tr>");
		str.append("<tr>\r\n"
				+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font face=\"Arial,Helvetica,sans-serif\"><b>Creator : </b></font></td>\r\n"
				+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">"
				+ pn.getCreatorFullName() + "</font></td></tr>");
		str.append("<tr>\r\n"
				+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font face=\"Arial,Helvetica,sans-serif\"><b>Process Template : </b></font></td>\r\n"
				+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">"
				+ wfVotingEventAudit.getProcessTemplateName() + "</font></td></tr>");
		str.append("<tr>\r\n"
				+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font face=\"Arial,Helvetica,sans-serif\"><b>Container Name : </b></font></td>\r\n"
				+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">" + pn.getContainerName()
				+ "</font></td></tr>");
		str.append("<tr>\r\n"
				+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font face=\"Arial,Helvetica,sans-serif\"><b>Activity Name : </b></font></td>\r\n"
				+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">"
				+ wfVotingEventAudit.getActivityName() + "</font></td></tr>");
		str.append("<tr>\r\n"
				+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font face=\"Arial,Helvetica,sans-serif\"><b>Assignee Name : </b></font></td>\r\n"
				+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">"
				+ wfVotingEventAudit.getAssigneeRef().getFullName() + "</font></td></tr>");
		if(wfVotingEventAudit.getEventList()!=null&& !wfVotingEventAudit.getEventList().isEmpty()) {
			logger.debug("Event List: "+wfVotingEventAudit.getEventList());
		str.append("<tr>\r\n"
				+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font  color=\"Orange\" face=\"Arial,Helvetica,sans-serif\"><b>Activity Vote : </b></font></td>\r\n"
				+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">"
				+ "<b>" + wfVotingEventAudit.getEventList().get(0).toString() + "</b></font></td></tr>");
		} else {
			str.append("<tr>\r\n"
					+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font  color=\"Orange\" face=\"Arial,Helvetica,sans-serif\"><b>Activity Vote : </b></font></td>\r\n"
					+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">"
					+ "<b>" + "</b></font></td></tr>");
		}

		str.append("<tr>\r\n"
				+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font face=\"Arial,Helvetica,sans-serif\"><b>Completed By : </b></font></td>"
				+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">" + task.getCompletedBy()
				+ "</font></td></tr>");

		str.append("<tr>\r\n"
				+ "<td align=\"right\" valign=\"top\" nowrap=\"\"><font face=\"Arial,Helvetica,sans-serif\"><b>User Comments : </b></font></td>"
				+ "<td align=\"left\" valign=\"top\"><font face=\"Arial,Helvetica,sans-serif\">"
				+ wfVotingEventAudit.getUserComment() + "</font></td></tr>");
		str.append("<tr><td colspan=\"2\"><hr color=\"#40637A\" size=\"1\"></td></tr>");
		str.append("</tbody>");
		str.append("</table>");
		str.append(
				"<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\" min-scale=\"0.9761904761904762\" style=\"transform: scale(0.97619, 0.97619); transform-origin: center;\">");
		str.append("<tbody>");
		str.append("<tr bgcolor=\"#124280\">");
		str.append(
				"<td align=\"center\" colspan=\"5\"><font color=\"white\" face=\"Arial,Helvetica,sans-serif\"><b>Promotion Notice Target Object Details </b></font></td></tr>");
		str.append("<tr bgcolor=\"#124280\">");
		str.append(
				"<td align=\"center\"><font color=\"white\" face=\"Arial,Helvetica,sans-serif\"><b>Number</b></font></td>"
						+ "<td align=\"center\"><font color=\"white\" face=\"Arial,Helvetica,sans-serif\"><b>Name</b></font></td>"
						+ "<td align=\"center\"><font color=\"white\" face=\"Arial,Helvetica,sans-serif\"><b>Current State</b></font></td>"
						+ "<td align=\"center\"><font color=\"white\" face=\"Arial,Helvetica,sans-serif\"><b>Version</b></font></td>");
		str.append("</tr>");

		QueryResult qs = MaturityHelper.service.getPromotionTargets(pn);
		if (qs.size() > 0) {
			while (qs.hasMoreElements()) {
				obj = qs.nextElement();
				if (obj instanceof WTPart) {
					part = (WTPart) obj;
					logger.debug("Target Part Details : " + part.getIdentity());
					href = getURLOfObject(obj);
					href = "<a href='" + href + "'>" + part.getNumber() + "</a>";
					str.append("<tr>");
					str.append(
							"<td valign='top' nowrap='' align='center' style=\"color:Black;margin-top:3px;margin-bottom:3px\">"
									+ href + "</td>");
					str.append(
							"<td valign='top' nowrap='' align='center' style=\"color:Black;margin-top:3px;margin-bottom:3px\">"
									+ part.getName() + "</td>");
					str.append(
							"<td valign='top' nowrap='' align='center' style=\"color:Black;margin-top:3px;margin-bottom:3px\">"
									+ part.getState() + "</td>");
					str.append(
							"<td valign='top' nowrap='' align='center' style=\"color:Black;margin-top:3px;margin-bottom:3px\">"
									+ part.getIterationDisplayIdentifier() + "</td>");
					str.append("</tr>");
				} else if (obj instanceof WTDocument) {
					doc = (WTDocument) obj;
					href = getURLOfObject(obj);
					href = "<a href='" + href + "'>" + doc.getNumber() + "</a>";
					str.append("<tr>");
					str.append(
							"<td valign='top' nowrap='' align='center' style=\"color:Black;margin-top:3px;margin-bottom:3px\">"
									+ href + "</td>");
					str.append(
							"<td valign='top' nowrap='' align='center' style=\"color:Black;margin-top:3px;margin-bottom:3px\">"
									+ doc.getName() + "</td>");
					str.append(
							"<td valign='top' nowrap='' align='center' style=\"color:Black;margin-top:3px;margin-bottom:3px\">"
									+ doc.getState() + "</td>");
					str.append(
							"<td valign='top' nowrap='' align='center' style=\"color:Black;margin-top:3px;margin-bottom:3px\">"
									+ doc.getIterationDisplayIdentifier() + "</td>");
					str.append("</tr>");
				}
			}
		} else {
			throw new NullPointerException("Unable to find Target objects for Promotion Notice : " + pn.getNumber());
		}
		str.append("</tbody>");
		str.append("</table>");
		str.append("</font>");
		str.append("<tr><td colspan=\"2\"><hr color=\"#40637A\" size=\"1\"></td></tr>");
		str.append(
				"<table border=\"0\"><tbody><tr><td>" + "<font color=\"Black\" face=\"Arial,Helvetica,sans-serif\"><h3>"
						+ wfVotingEventAudit.getActivityName() + " has been completed" + "</h3></font>"
						+ "</td></tr><tr></tr><tr></tr>");
		str.append("</body></html>");
		return str;

	}

	/**
	 * @param obj
	 * @return href
	 * @throws Exception
	 */
	public static String getURLOfObject(Object obj) throws Exception {
		NmOid tgtOid = new NmOid(wt.fc.PersistenceHelper.getObjectIdentifier((Persistable) obj));
		NmAction infoPageAction = NmActionServiceHelper.service
				.getAction(com.ptc.netmarkets.util.misc.NmAction.Type.OBJECT, "view");
		infoPageAction.setContextObject(tgtOid);
		infoPageAction.setIcon(null);
		String href = infoPageAction.getActionUrlExternal();
		return href;
	}

	/**
	 * @param mailSet
	 * @param str
	 * @param pn
	 * @throws Exception
	 */
	public static void sendEmail(Set<String> mailSet, StringBuilder str, PromotionNotice pn) throws Exception {
		EMailMessage mail = EMailMessage.newEMailMessage();
		String[] stringArray = Arrays.copyOf(mailSet.toArray(), mailSet.toArray().length, String[].class);
		mail.addEmailAddress(stringArray);
		mail.setSubject("Promotion notice : " + pn.getNumber());
		mail.addPart(str.toString(), "text/html; charset=utf-8");
		mail.send(true);
		logger.debug("********************** Mail sent to users ********************************");
	}

}
