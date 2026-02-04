package wt.workflow.worklist;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.misc.NmAction;
import com.ptc.netmarkets.util.misc.NmActionServiceHelper;

import wt.access.AccessControlHelper;
import wt.access.AccessPermission;
import wt.access.NotAuthorizedException;
import wt.doc.WTDocument;
import wt.epm.EPMDocument;
import wt.facade.netmarkets.NetmarketsCommand;
import wt.facade.netmarkets.NetmarketsHref;
import wt.facade.netmarkets.NetmarketsType;
import wt.facade.netmarkets.UserFacade;
import wt.facade.netmarkets.UserHelper;
import wt.fc.EnumeratedType;
import wt.fc.EnumeratedTypeUtil;
import wt.fc.ObjectIdentifier;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.htmlutil.HtmlUtil;
import wt.httpgw.GatewayURL;
import wt.httpgw.HTTPRequest;
import wt.inf.container.ExchangeContainer;
import wt.inf.container.OrgContainer;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.inf.container.WTContainerServerHelper;
import wt.inf.library.WTLibrary;
import wt.log4j.LogR;
import wt.maturity.MaturityException;
import wt.maturity.MaturityHelper;
import wt.maturity.PromotionNotice;
import wt.maturity.PromotionTarget;
import wt.method.MethodContext;
import wt.org.OrganizationServicesHelper;
import wt.org.OrganizationServicesMgr;
import wt.org.WTGroup;
import wt.org.WTPrincipal;
import wt.org.WTPrincipalReference;
import wt.org.WTUser;
import wt.org.electronicIdentity.SigVariableInfo;
import wt.org.electronicIdentity.SignatureEngine;
import wt.org.electronicIdentity.SignatureEngineFactory;
import wt.ownership.OwnershipHelper;
import wt.part.WTPart;
import wt.pdmlink.PDMLinkProduct;
import wt.pom.Transaction;
import wt.project.Project;
import wt.projmgmt.admin.Project2;
import wt.session.SessionContext;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.team.Team;
import wt.team.TeamHelper;
import wt.team.TeamReference;
import wt.team.TeamTemplate;
import wt.team.TeamTemplateReference;
import wt.templateutil.processor.SubTemplateService;
import wt.util.CollationKeyFactory;
import wt.util.Evolvable;
import wt.util.HTMLEncoder;
import wt.util.InstalledProperties;
import wt.util.SortedEnumeration;
import wt.util.WTContext;
import wt.util.WTException;
import wt.util.WTIOException;
import wt.util.WTMessage;
import wt.util.WTProperties;
import wt.util.WTPropertyVetoException;
import wt.util.WTRuntimeException;
import wt.util.WTStandardDateFormat;
import wt.workflow.SortedEnumByPrincipal;
import wt.workflow.WorkflowProcessor;
import wt.workflow.definer.WfAssignedActivityTemplate;
import wt.workflow.definer.WfVariableInfo;
import wt.workflow.engine.ProcessData;
import wt.workflow.engine.WfActivity;
import wt.workflow.engine.WfAdHocActivity;
import wt.workflow.engine.WfContainer;
import wt.workflow.engine.WfDueDate;
import wt.workflow.engine.WfProcess;
import wt.workflow.engine.WfRouterType;
import wt.workflow.engine.WfState;
import wt.workflow.engine.WfVariable;
import wt.workflow.notebook.SubjectOfNotebook;
import wt.workflow.work.WfAssignedActivity;
import wt.workflow.work.WfAssignee;
import wt.workflow.work.WfAssignment;
import wt.workflow.work.WfHtmlFormat;
import wt.workflow.work.WfPrincipalAssignee;
import wt.workflow.work.WorkItem;
import wt.workflow.work.WorkflowHtmlUtil;

public class WfTaskProcessor extends WorkflowProcessor implements Evolvable {
	public static final long EXTERNALIZATION_VERSION_UID = -2054561049350297424L;
	private static final String CLASSNAME = WfTaskProcessor.class.getName();
	private static final Logger logger;
	static final long serialVersionUID = -2054561049350297424L;
	private Boolean adHocActivitiesDisplayed = null;
	private Boolean adHocActivitiesInProgress = null;
	private ObjectReference workItemRef = null;
	private String messageText = "";
	private String pAction = "";
	private WfActivity myActivity = null;
	private WfProcess myProcess = null;
	private WorkItem workItem = null;
	private static String ADHOC_ACTIVITY_GIF;
	private static String FORUM_GIF;
	private static String JIT_PROJECTS_GIF;
	private static String JIT_TEAMS_GIF;
	private static String NOTEBOOK_GIF;
	private static String ROLES_UPDATE_GIF;
	private static String UPDATE_CONTENT_GIF;
	private static boolean PLMLINK;
	private static String ESCAPE_HTML;
	private static boolean VERBOSE;
	public static final String SUBMIT_TASK_ACTION = "submit";
	private static final String ACTIVITY_CONTEXT = "activity";
	private static final String AD_HOC_ACTION = "CreateAdHocActivities";
	private static final String CONTEXT_KEY = "context";
	private static final String EXPAND = "expand";
	private static final String INDENT = "         ";
	private static final String LINK = "link";
	private static final String PBO_CONTEXT = "primaryBusinessObject";
	private static final String PROCESS_CONTEXT = "process";
	private static final String REFRESH_ACTION = "refreshAction";
	private static final String REFRESH_OID = "refreshOid";
	private static final String STYLE_KEY = "style";
	private static final String VARIABLE_KEY = "variable";
	protected static String FORMATTED;
	protected static String DEFAULT_FORMATTED_VALUE;
	protected static String COLUMNS;
	protected static String DEFAULT_COLUMN_NUMBER;
	protected static String DEFAULT_ROW_NUMBER;
	protected static String ROWS;
	protected static String CODEBASE;
	protected static final String ROUTER_EVENT = "WfUserEvent";
	protected static final String TABLE_ATTRIBUTES = " BORDER=0 WIDTH=\"100%\" ALIGN=CENTER CELLPADDING=3 ";
	public static final String AD_HOC_CREATE = "create";
	public static final String AD_HOC_CREATE_VALUE = "adhocactivities";
	public static final String COMMENT_AREA = "comments";
	public static final String CONTENT_UPDATE = "contentUpdate";
	public static final String CONTENT_UPDATE_VALUE = "updatingContent";
	public static final String JIT_PROJECTS_UPDATE = "jitProjectsUpdate";
	public static final String JIT_PROJECTS_UPDATE_VALUE = "updatingJITProjects";
	public static final String JIT_TEAMS_UPDATE = "jitTeamsUpdate";
	public static final String JIT_TEAMS_UPDATE_VALUE = "updatingJITTeams";
	public static final String ROLES_UPDATE = "rolesUpdate";
	public static final String ROLES_UPDATE_VALUE = "updatingRoles";
	public static boolean PDM_INSTALLED;
	public static boolean QMS_INSTALLED;
	public boolean pjlContext = false;
	public boolean pdmlContext = false;
	private boolean contextSet = false;
	private static final ResourceBundle rb;
	private static final String ENGINES_RESOURCE = "wt.org.electronicIdentity.engines.EnginesRB";
	private static final String FORUM_RESOURCE = "wt.workflow.forum.forumResource";
	private static final String ACCESS_RESOURCE = "wt.access.accessResource";
	private static final String NOTEBOOK_RESOURCE = "wt.workflow.notebook.notebookResource";
	private static final String RESOURCE = "wt.workflow.worklist.worklistResource";
	protected static final String LIFECYCLE_RESOURCE = "wt.lifecycle.lifecycleResource";
	private static final String WF_DISPLAY_USERS = "WF_DISPLAY_USERS";
	private static final String WF_USERS = "WF_USERS";
	private static final String WF_DISPLAY_GROUPS = "WF_DISPLAY_GROUPS";
	private static final String WF_GROUPS = "WF_GROUPS";
	private boolean isExternalParticipant = false;

	@Override
	public void writeExternal(ObjectOutput var1) throws IOException {
		var1.writeLong(-2054561049350297424L);
		ReferenceFactory var2 = new ReferenceFactory();

		try {
			var1.writeUTF(var2.getReferenceString(this.workItemRef));
		} catch (WTException var4) {
			throw new WTRuntimeException(var4);
		}

		var1.writeObject(this.adHocActivitiesDisplayed);
		var1.writeObject(this.adHocActivitiesInProgress);
		var1.writeUTF(this.pAction);
		var1.writeUTF(this.messageText);
	}

	@Override
	public void readExternal(ObjectInput var1) throws IOException, ClassNotFoundException {
		long var2 = var1.readLong();
		this.readVersion(this, var1, var2, false, false);
	}

	protected boolean readVersion(WfTaskProcessor var1, ObjectInput var2, long var3, boolean var5, boolean var6)
			throws IOException, ClassNotFoundException {
		if (var3 != -2054561049350297424L) {
			return this.readOldVersion(var2, var3, var5, var6);
		} else {
			ReferenceFactory var7 = new ReferenceFactory();
			String var8 = var2.readUTF();

			try {
				this.workItemRef = (ObjectReference) var7.getReference(var8);
			} catch (WTException var10) {
				throw new WTIOException(var10.getLocalizedMessage());
			}

			this.adHocActivitiesDisplayed = (Boolean) var2.readObject();
			this.adHocActivitiesInProgress = (Boolean) var2.readObject();
			this.pAction = var2.readUTF();
			this.messageText = var2.readUTF();
			return true;
		}
	}

	private boolean readOldVersion(ObjectInput var1, long var2, boolean var4, boolean var5)
			throws IOException, ClassNotFoundException {
		boolean var6 = true;
		throw new InvalidClassException(CLASSNAME,
				"Local class not compatible: stream classdesc externalizationVersionUID = " + var2
						+ " local class externalizationVersionUID = -2054561049350297424");
	}

	public WorkItem getWorkItem() {
		try {
			if (this.workItem == null && this.workItemRef != null) {
				this.workItem = (WorkItem) this.workItemRef.getObject();
			}

			this.workItem = (WorkItem) PersistenceHelper.manager.refresh(this.workItem);
			return this.workItem;
		} catch (WTException var2) {
			throw new WTRuntimeException(var2);
		}
	}

	public WorkItem getExistingWorkItem() throws WTRuntimeException {
		if (logger.isTraceEnabled()) {
			logger.trace("IN: WfTaskProcessor.getExistingWorkItem");
		}

		if (this.workItem == null && this.workItemRef != null) {
			this.workItem = (WorkItem) this.workItemRef.getObject();
		}

		if (logger.isTraceEnabled()) {
			logger.trace("IN: WfTaskProcessor.getExistingWorkItem");
		}

		return this.workItem;
	}

	public boolean workItemExists() {
		try {
			if (this.workItem == null && this.workItemRef != null) {
				try {
					this.workItem = (WorkItem) this.workItemRef.getObject();
				} catch (WTRuntimeException var2) {
					return false;
				}
			}

			if (this.workItem != null) {
				this.workItem = (WorkItem) PersistenceHelper.manager.refresh(this.workItem);
				return true;
			} else {
				return false;
			}
		} catch (WTException var3) {
			return false;
		}
	}

	public WfActivity getActivity() {
		if (this.myActivity == null) {
			this.myActivity = (WfActivity) this.getWorkItem().getSource().getObject();
			this.setContextObj(this.myActivity);
		}

		return this.myActivity;
	}

	public WfActivity getActivityForExistingWorkitem() {
		if (logger.isTraceEnabled()) {
			logger.trace("IN: WfTaskProcessor.getActivityForExistingWorkitem");
		}

		if (this.myActivity == null) {
			this.myActivity = (WfActivity) this.getExistingWorkItem().getSource().getObject();
			this.setContextObj(this.myActivity);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("OUT: WfTaskProcessor.getActivityForExistingWorkitem");
		}

		return this.myActivity;
	}

	public WfProcess getProcess() {
		try {
			if (this.myProcess == null) {
				this.myProcess = this.getActivity().getParentProcess();
			}

			return this.myProcess;
		} catch (WTException var2) {
			throw new WTRuntimeException(var2);
		}
	}

	public WfTaskProcessor() {
	}

	public WfTaskProcessor(Object var1) {
		this.setContextObj(var1);
	}

	public void setWorkItem(WorkItem var1) {
		this.workItem = var1;
		boolean var2 = false;

		try {
			this.workItemRef = ObjectReference.newObjectReference(var1);
		} catch (WTException var12) {
			throw new WTRuntimeException(var12);
		}

		try {
			this.myActivity = (WfActivity) var1.getSource().getObject();
		} catch (WTRuntimeException var13) {
			Throwable var4 = var13.getNestedThrowable();
			if (!(var4 instanceof NotAuthorizedException)) {
				throw var13;
			}

			logger.debug("WfTaskProcessor: User is an external participant, bypassing access control check");
			this.isExternalParticipant = true;
			MethodContext.getContext().put("externalParticipant", this.isExternalParticipant);

			try {
				var2 = SessionServerHelper.manager.setAccessEnforced(false);
				this.myActivity = (WfActivity) var1.getSource().getObject();
			} finally {
				SessionServerHelper.manager.setAccessEnforced(var2);
			}
		}

		try {
			this.myProcess = this.myActivity.getParentProcess();
		} catch (WTException var10) {
			throw new WTRuntimeException(var10);
		}

		this.setContextObj(this.myActivity);
	}

	@Override
	public void readContext(HTTPRequest var1) throws WTException {
		this.getState().getResponseObj().setHeader("cache-control", "no-cache");

		try {
			this.setWorkItem((WorkItem) this.getContextObj());
			if (this.getWorkItem().isComplete()) {
				this.addToResponseHeaders(
						new WTMessage("wt.workflow.worklist.worklistResource", "17", (Object[]) null));
				this.setContextAction("WfMessage");
			} else if (this.isWorkItemOwner()) {
				boolean var2 = false;

				try {
					var2 = SessionServerHelper.manager.setAccessEnforced(false);
					this.myActivity = (WfActivity) this.getWorkItem().getSource().getObject();
					this.setContextObj(this.myActivity);
					if (this.myActivity.getState().equals(WfState.OPEN_NOT_RUNNING_SUSPENDED_INTERMITTED)) {
						this.addToResponseHeaders(
								new WTMessage("wt.workflow.worklist.worklistResource", "81", (Object[]) null));
						this.setContextAction("WfMessage");
					} else {
						this.myProcess = this.myActivity.getParentProcess();
					}

					this.setContext();
				} finally {
					var2 = SessionServerHelper.manager.setAccessEnforced(var2);
				}
			} else {
				this.addToResponseHeaders(
						new WTMessage("wt.workflow.worklist.worklistResource", "79", (Object[]) null));
				this.setContextAction("WfMessage");
			}
		} catch (Exception var7) {
			logger.error("", var7);
			this.addToResponseHeaders(new WTMessage("wt.workflow.worklist.worklistResource", "17", (Object[]) null));
			this.setContextAction("WfMessage");
		}

	}

	public void activityName(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.print(this.getActivity().getName());
		var4.flush();
	}

	public void activityAttributes(Properties var1, Locale var2, OutputStream var3) throws Exception {

		if (this.workItemExists()) {
			if (!this.contextSet) {

				this.setContext();
			}

			if (this.pjlContext) {

				this.projectLinkActivityAttributes(var1, var2, var3);
				return;
			}

			if (this.pdmlContext || QMS_INSTALLED) {
				;
				this.plmLinkActivityAttributes(var1, var2, var3);
				return;
			}

			PrintWriter var4 = this.getPrintWriter(var3, var2);
			var4.println("<TABLE BORDER=0 WIDTH=\"100%\" ALIGN=CENTER CELLPADDING=3 >");
			var4.println("<TR>" + this.tableCell(
					"<B>" + this.getActivityInstructionsLabel(var2) + "</B>" + this.getActivityInstructionsAsHtml(),
					5));
			var4.println(this.tableCellBold(this.getProcessNameLabel(var2), 1));
			var4.println(this.tableCell(this.translateToHtml(this.getProcessName())));
			String var5 = this.tableCellBold(this.getProcessInitiatorLabel(var2), 1);
			var4.println("<TR>" + var5);
			var4.println(this.tableCell(this.translateToHtml(this.getProcessInitiator())));
			var5 = this.tableCellBold(this.getDueDateLabel(var2), 1);
			var4.println("<TR>" + var5);
			var4.println(this.tableCell(this.translateToHtml(this.getDueDate(var2))));
			var5 = this.tableCellBold(this.getWorkItemRoleLabel(var2), 1);
			var4.println("<TR>" + var5);
			var4.println(this.tableCell(this.translateToHtml(this.getWorkItemRole(var2))));
			var5 = this.tableCellBold(this.getAssigneeLabel(var2), 1);
			var4.println("<TR>" + var5);
			String var6 = this.getAssignee();
			var4.println(this.tableCell(this.translateToHtml(var6 + this.getOriginalOwnerLocal())));
			var4.println("</TABLE>");

			if (!this.contextSet) {

				var4.println("<font color='red'><B>Warning: Context not known.</B></font>");
			}

			var4.flush();
		}

	}

	public void projectLinkActivityAttributes(Properties var1, Locale var2, OutputStream var3)
			throws WTException, WTPropertyVetoException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println("<table border=\"0\" cellpadding=\"3\" cellspacing=\"0\">");
		var4.println("<tr><td colspan=\"2\"><hr size=\"1\" color=\"#40637A\"></td></tr>");
		String var5 = WorkflowHtmlUtil.getExternalTaskURL(this.getWorkItem());
		String var6;
		if (var5 != null) {
			var6 = this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "40", (Object[]) null))
					.getLocalizedMessage(var2));
			String var7 = "("
					+ (new WTMessage("wt.workflow.worklist.worklistResource", "NAVIGATE_LABEL", (Object[]) null))
							.getLocalizedMessage(var2)
					+ ")";
			String var8 = "("
					+ (new WTMessage("wt.workflow.worklist.worklistResource", "WINDCHILL_LABEL", (Object[]) null))
							.getLocalizedMessage(var2)
					+ ")";
			var4.println(
					"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
			var4.println(var6);
			var4.println("</b></font></td>");
			var4.println(
					"<td align=\"left\" colspan=\"4\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
			String var9 = HTMLEncoder.encodeForHTMLAttribute(var5);
			String var10 = "<A id=\"WfExternalTaskTProcessor02\" HREF=\"" + var9 + "\">"
					+ HTMLEncoder.encodeForHTMLContent(this.myActivity.getName()) + "</A>";
			var4.println(this.translateToHtml(var10 + var7));
			var4.println("</font></td></tr>");
			var9 = this.getTaskUrl(this.getContextRef());
			String var11 = "<A id=\"WfExternalTaskTProcessor03\" HREF=\"" + var9 + "\">"
					+ HTMLEncoder.encodeForHTMLContent(this.myActivity.getName()) + "</A>";
			var4.println(
					"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
			var4.println("");
			var4.println("</b></font></td>");
			var4.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
			var4.println(this.translateToHtml(var11 + var8));
			var4.println("</font></td></tr>");
		}

		var4.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var4.println(this.getActivityInstructionsLabel(var2));
		var4.println("</b></font></td>");
		var4.println("<td align=\"left\" colspan=\"4\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var4.println(this.getActivityInstructionsAsHtml());
		var4.println("</font></td></tr>");
		var4.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var4.println(this.getProcessInitiatorLabel(var2));
		var4.println("</b></font></td>");
		var4.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var4.println(this.translateToHtml(this.getProcessInitiator()));
		var4.println("</font></td></tr>");
		var4.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var4.println(this.getDueDateLabel(var2));
		var4.println("</b></font></td>");
		var4.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var4.println(this.getProjectLinkDueDate(var2));
		var4.println("</font></td></tr>");
		var4.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var4.println(this.getWorkItemRoleLabel(var2));
		var4.println("</b></font></td>");
		var4.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var4.println(this.getWorkItemRole(var2));
		var4.println("</font></td></tr>");
		var4.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var4.println(this.getAssigneeLabel(var2));
		var4.println("</b></font></td>");
		var4.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var6 = this.getAssignee();
		var4.println(this.translateToHtml(var6 + this.getOriginalOwnerLocal()));
		var4.println("</font></td></tr>");
		var4.println("<tr><td colspan=\"2\"><hr size=\"1\" color=\"#40637A\"></td></tr>");
		this.projectLinkAttributes(var1, var2, var3);
		var4.println("</table>");
		var4.flush();
	}

	public String getProjectLinkDueDate(Locale var1) throws WTException {
		try {
			WfActivity var2 = this.getActivity();
			Timestamp var3 = var2.getDeadline();
			if (var3 == null) {
				return "&nbsp;";
			} else {
				WTPrincipal var4 = OwnershipHelper.getOwner(this.getWorkItem());
				UserFacade var5 = UserHelper.getFacade();
				TimeZone var6 = var5.getLocalTimeZoneForUser(var4);
				ResourceBundle var7 = ResourceBundle.getBundle("wt.util.utilResource", var1);
				String var8 = var7.getString("22");
				SimpleDateFormat var9 = new SimpleDateFormat(var8, var1);
				var9.setTimeZone(var6);
				return var9.format(var3);
			}
		} catch (Exception var10) {
			logger.error("", var10);
			if (logger.isDebugEnabled()) {
				var10.printStackTrace();
			}

			return "&nbsp;";
		}
	}

	public void plmLinkActivityAttributes(Properties var1, Locale var2, OutputStream var3) throws Exception {
		String var4 = null;
		if (var1 != null) {
			var4 = var1.getProperty("notification");
		}

		boolean var5 = false;
		if (var4 != null) {
			var5 = Boolean.parseBoolean(var4);
		}

		if (var5) {
			
			this.notificationActivityAttributes(var1, var2, var3);
		} else {
			PrintWriter var6 = this.getPrintWriter(var3, var2);
			var6.println("<table border=\"0\" cellpadding=\"3\" cellspacing=\"0\">");
			var6.println("<tr><td align=\"right\" valign=\"top\" nowrap >");
			var6.println("&nbsp;");
			var6.println("</td><tr>");
			var6.println("<tr><td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			var6.println(this.getActivityInstructionsLabel(var2));
			var6.println("</td>");
			var6.println("<td colspan=\"4\" align=\"left\" valign=\"top\" class=\"propValue\">");
			var6.println(this.getActivityInstructionsAsHtml());
			var6.println("</td><tr>");
			var6.println("<tr><td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			var6.println(this.getProcessInitiatorLabel(var2));
			var6.println("</td>");
			var6.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			var6.println(this.translateToHtml(this.getProcessInitiator()));
			var6.println("</td>");
			var6.println("<td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			String var7 = this.getPriorityLabel(var2);
			var6.println("&nbsp;&nbsp;&nbsp;" + var7);
			var6.println("</td>");
			var6.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			var6.println(this.translateToHtml(this.getPriority(var2)));
			var6.println("</td></tr>");
			var6.println("<tr><td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			var6.println(this.getAssigneeLabel(var2));
			var6.println("</td>");
			var6.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			String var8 = this.getAssignee();
			var6.println(this.translateToHtml(var8 + this.getOriginalOwnerLocal()));
			var6.println("</td>");
			var6.println("<td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			var7 = this.getDueDateLabel(var2);
			var6.println("&nbsp;&nbsp;&nbsp;" + var7);
			var6.println("</td>");
			var6.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			String var9 = this.getDueDate(var2);
			if (var9 == null) {
				var9 = " ";
			}

			var6.println(var9);
			var6.println("</td></tr>");
			var6.println("<tr><td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			var6.println(this.getWorkItemRoleLabel(var2));
			var6.println("</td>");
			var6.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			var6.println(this.getWorkItemRole(var2));
			var6.println("</td>");
			var6.println("<td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			var7 = this.getProcessNameLabel(var2);
			var6.println("&nbsp;&nbsp;&nbsp;" + var7);
			var6.println("</td>");
			var6.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			var6.println(this.translateToHtml(this.getProcessName()));
			var6.println("</td></tr>");
			var6.println("</table>");
			var6.flush();
		}

	}

	public void activityAttributesPlain(Properties var1, Locale var2, OutputStream var3)
			throws WTException, WTPropertyVetoException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getActivity().getName());
		String var5 = this.getActivityInstructions();
		if (var5 == null) {
			var5 = " ";
		}

		String var6 = this.trimHtml(this.getActivityInstructionsLabel(var2));
		var4.println(var6 + var5);
		var6 = this.trimHtml(this.getProcessNameLabel(var2));
		var4.println(var6 + this.getProcess().getName() + " (" + this.stripHtmlFormat(this.getProcessName()) + ")");
		var6 = this.trimHtml(this.getProcessInitiatorLabel(var2));
		var4.println(var6 + this.stripHtmlFormat(this.getProcessInitiator()));
		String var7 = this.getDueDate(var2);
		if (var7 == null) {
			var7 = " ";
		}

		var6 = this.trimHtml(this.getDueDateLabel(var2));
		var4.println(var6 + var7);
		var6 = this.trimHtml(this.getWorkItemRoleLabel(var2));
		var4.println(var6 + this.getWorkItemRole(var2));
		var6 = this.trimHtml(this.getAssigneeLabel(var2));
		var4.println(var6 + this.stripHtmlFormat(this.getAssignee()));
		var4.flush();
	}

	public void activityAttributesWithGroup(Properties var1, Locale var2, OutputStream var3)
			throws WTException, WTPropertyVetoException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		if (this.pdmlContext) {
			var4.println("<TABLE BORDER=0 WIDTH=\"100%\" ALIGN=CENTER CELLPADDING=3  class=\"tableHdr\">");
		} else {
			var4.println("<TABLE BORDER=0 WIDTH=\"100%\" ALIGN=CENTER CELLPADDING=3 bgcolor=\""
					+ getWCColor("t1-bg-col-head") + "\">");
		}

		var4.println("<TR>" + this.tableCell(
				"<B>" + this.getActivityInstructionsLabel(var2) + "</B>" + this.getActivityInstructionsAsHtml(), 6));
		var4.println(this.tableCellBold(this.getProcessNameLabel(var2), 1));
		var4.println(this.tableCell(this.translateToHtml(this.getProcessName())));
		String var5 = this.tableCellBold(this.getProcessInitiatorLabel(var2), 1);
		var4.println("<TR>" + var5);
		var4.println(this.tableCell(this.translateToHtml(this.getProcessInitiator())));
		var5 = this.tableCellBold(this.getDueDateLabel(var2), 1);
		var4.println("<TR>" + var5);
		var4.println(this.tableCell(this.translateToHtml(this.getDueDate(var2))));
		var5 = this.tableCellBold(this.getWorkItemRoleLabel(var2), 1);
		var4.println("<TR>" + var5);
		var4.println(this.tableCell(this.translateToHtml(this.getWorkItemRole(var2))));
		var5 = this.tableCellBold(this.getAssigneeLabel(var2), 1);
		var4.println("<TR>" + var5);
		var4.println(this.tableCell(this.translateToHtml(this.getAssignee())));
		var5 = this.tableCellBold(this.getGroupLabel(var2), 1);
		var4.println("<TR>" + var5);
		var4.println(this.tableCell(this.translateToHtml(this.getGroup())));
		var4.println("</TABLE>");
		var4.flush();
	}

	private String stripHtmlFormat(String var1) {
		if (logger.isDebugEnabled()) {
			logger.debug("=> WfTaskProcessor.stripHtmlFormat - IN: " + var1);
		}

		int var2 = var1.indexOf(34);
		if (var2 == -1) {
			return var1;
		} else {
			int var3 = var1.indexOf(34, var2 + 1);
			if (var3 == -1) {
				return var1;
			} else {
				String var4 = var1.substring(var2 + 1, var3);
				if (logger.isDebugEnabled()) {
					logger.debug("   stripHtmlFormat - OUT: " + var4);
				}

				return var4;
			}
		}
	}

	private String trimHtml(String var1) {
		int var2 = var1.indexOf(38);
		if (var2 == -1) {
			return var1;
		} else {
			String var3 = var1.substring(0, var2);
			return "         " + var3 + " ";
		}
	}

	public String getActivityInstructionsLabel(Locale var1) throws WTException {
		return this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "2", (Object[]) null))
				.getLocalizedMessage(var1));
	}

	public void activityInstructionsLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getActivityInstructionsLabel(var2));
		var4.flush();
	}

	public String getActivityInstructions() throws WTException, WTPropertyVetoException {
		WfAssignedActivity var1 = (WfAssignedActivity) this.getActivity();
		String var2 = var1.getInstructions();
		if (var2 == null || var2.equals("")) {
			var2 = (String) var1.getContext().getValue("instructions");
			if (var2 != null) {
				Transaction var3 = new Transaction();

				try {
					var3.start();
					var1.setInstructions(var2);
					PersistenceHelper.manager.save(var1);
					var3.commit();
					var3 = null;
				} finally {
					if (var3 != null) {
						var3.rollback();
						logger.debug("Error Lazy migration: set instructions for WfAssignedActivity.");
					}

				}
			}
		}

		return var2;
	}

	public String getActivityInstructionsAsHtml() throws WTException, WTPropertyVetoException {
		String var1 = WorkflowHtmlUtil.getTranslatedInstruction(this.getActivityInstructions());
		return var1;
	}

	public void activityInstructions(Properties var1, Locale var2, OutputStream var3)
			throws WTException, WTPropertyVetoException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getActivityInstructionsAsHtml());
		var4.flush();
	}

	public String getProcessNameLabel(Locale var1) throws WTException {
		return this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "10", (Object[]) null))
				.getLocalizedMessage(var1));
	}

	public void processNameLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getProcess().getName());
		var4.flush();
	}

	public String getProcessName() throws WTException {
		if (MethodContext.getContext().get("externalParticipant") != null
				|| !this.isUserMemberOfGroup("Administrators") && !this.isUserOrgAdmin(this.getProcess())) {
			return WTMessage.getLocalizedMessage("wt.access.accessResource", "18", (Object[]) null,
					SessionHelper.getLocale());
		} else {
			NetmarketsHref var1 = new NetmarketsHref(NetmarketsType.workflow, NetmarketsCommand.openProcessManager,
					this.getProcess());
			String var2 = var1.getHrefNew();
			String var3 = HtmlUtil.createLink(var2, (String) null, this.getProcess().getName());
			return var3;
		}
	}

	public boolean isUserMemberOfGroup(String var1) throws WTException {
		boolean var2 = false;
		WTPrincipal var3 = SessionHelper.manager.getPrincipal();
		WTGroup var4 = OrganizationServicesHelper.manager.getGroup(var1);
		if (var4 != null) {
			var2 = var4.isMember(var3);
		}

		return var2;
	}

	public boolean isUserOrgAdmin(WfProcess var1) throws WTException {
		boolean var2 = false;
		Object var3 = null;
		OrgContainer var4 = null;
		var4 = WTContainerHelper.service.getOrgContainer(var1);
		if (var4 == null) {
			return false;
		} else {
			var2 = WTContainerServerHelper.getAdministratorsReadOnly(var4)
					.isMember(SessionHelper.manager.getPrincipal());
			return var2;
		}
	}

	public boolean isExternalParticipant() {
		return MethodContext.getContext().get("externalParticipant") != null;
	}

	public static boolean isExternalParticipant(WorkItem var0) {
		ObjectReference var1 = null;

		try {
			var1 = ObjectReference.newObjectReference(var0);
		} catch (WTException var4) {
			throw new WTRuntimeException(var4);
		}

		try {
			var0.getSource().getObject();
			return false;
		} catch (WTRuntimeException var5) {
			Throwable var3 = var5.getNestedThrowable();
			if (var3 instanceof NotAuthorizedException) {
				logger.debug("WfTaskProcessor: User is an external participant, bypassing access control check");
				return true;
			} else {
				throw var5;
			}
		}
	}

	public WTObject getPBO() {
		WTObject var1 = null;
		boolean var2 = false;

		try {
			var2 = SessionServerHelper.manager.setAccessEnforced(false);
			var1 = (WTObject) this.workItem.getPrimaryBusinessObject().getObject();
		} catch (NullPointerException var7) {
			var1 = null;
		} finally {
			SessionServerHelper.manager.setAccessEnforced(var2);
		}

		return var1;
	}

	public void processName(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.translateToHtml(this.getProcessName()));
		var4.flush();
	}

	public String getProcessInitiatorLabel(Locale var1) throws WTException {
		return this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "12", (Object[]) null))
				.getLocalizedMessage(var1));
	}

	public void processInitiatorLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getProcessInitiatorLabel(var2));
		var4.flush();
	}

	public String getProcessInitiator() throws WTException {
		String var1 = "";
		WfProcess var2 = this.getProcess();
		var1 = var2.getCreator().getDisplayName();
		return var1;
	}

	public void processInitiator(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.translateToHtml(this.getProcessInitiator()));
		var4.flush();
	}

	public String getDueDateLabel(Locale var1) throws WTException {
		return this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "11", (Object[]) null))
				.getLocalizedMessage(var1));
	}

	public void dueDateLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getDueDateLabel(var2));
		var4.flush();
	}

	public String getDueDate(Locale var1) throws WTException {
		try {
			Timestamp var2 = this.getActivity().getDeadline();
			if (var2 == null) {
				return "&nbsp;";
			} else {
				ResourceBundle var3 = ResourceBundle.getBundle("wt.util.utilResource", var1);
				String var4 = var3.getString("22");
				return WTStandardDateFormat.format(this.getActivity().getDeadline(), var4);
			}
		} catch (Exception var5) {
			logger.error("", var5);
			return "&nbsp;";
		}
	}

	public void dueDate(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.translateToHtml(this.getDueDate(var2)));
		var4.flush();
	}

	public String getWorkItemRoleLabel(Locale var1) throws WTException {
		return this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "38", (Object[]) null))
				.getLocalizedMessage(var1));
	}

	public void workItemRoleLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getWorkItemRoleLabel(var2));
		var4.flush();
	}

	public String getWorkItemRole(Locale var1) throws WTException {
		return this.getWorkItem().getRole().getDisplay(var1);
	}

	public void role(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.translateToHtml(this.getWorkItemRole(var2)));
		var4.flush();
	}

	public String getAssigneeLabel(Locale var1) throws WTException {
		return this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "13", (Object[]) null))
				.getLocalizedMessage(var1));
	}

	public void assigneeLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getAssigneeLabel(var2));
		var4.flush();
	}

	public String getAssignee() throws WTException {
		return this.getPrincipalFName(OwnershipHelper.getOwner(this.getWorkItem()));
	}

	public String getOriginalOwner() throws WTException {
		String var1 = null;
		if (this.getWorkItem().getOrigOwner() != null && this.getWorkItem().getOrigOwner().getObject() != null) {
			var1 = ((WTUser) this.getWorkItem().getOrigOwner().getObject()).getName();
		}

		return var1;
	}

	public String getOriginalOwnerLocal() throws WTException {
		String var1 = this.getOriginalOwner();
		String var2 = "";
		if (var1 != null && var1.length() > 0) {
			var2 = WTMessage.getLocalizedMessage("wt.workflow.worklist.worklistResource", "167", new Object[] { var1 });
		}

		return var2;
	}

	public void assignee(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.translateToHtml(this.getAssignee()));
		var4.flush();
	}

	public String getActivityDescriptionLabel(Locale var1) throws WTException {
		return this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "51", (Object[]) null))
				.getLocalizedMessage(var1));
	}

	public void activityDescriptionLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getActivityDescriptionLabel(var2));
		var4.flush();
	}

	public String getActivityDescription() throws WTException {
		return this.getActivity().getDescription();
	}

	public void activityDescription(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.translateToHtml(this.getActivityDescription()));
		var4.flush();
	}

	public String getProcessDescriptionLabel(Locale var1) throws WTException {
		return this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "22", (Object[]) null))
				.getLocalizedMessage(var1));
	}

	public void processDescriptionLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getProcessDescriptionLabel(var2));
		var4.flush();
	}

	public String getProcessDescription() throws WTException {
		return this.getProcess().getDescription();
	}

	public void ProcessDescription(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.translateToHtml(this.getProcessDescription()));
		var4.flush();
	}

	public void processVariable(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = var1.getProperty("variable");
		var4.println(this.getProcessVariableDisplay(var5, var2));
		var4.flush();
	}

	public void processVariables(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = this.columnLabel((new WTMessage("wt.workflow.worklist.worklistResource", "4", (Object[]) null))
				.getLocalizedMessage(var2));
		var4.println("<TR>" + var5);
		var4.println(this.columnLabel((new WTMessage("wt.workflow.worklist.worklistResource", "5", (Object[]) null))
				.getLocalizedMessage(var2)));
		ProcessData var6 = this.getProcess().getContext();
		String[] var7 = var6.getNames();
		String[] var8 = var6.getDisplayNames(var2);

		for (int var9 = 0; var9 < var7.length; ++var9) {
			var6.getValue(var7[var9]);
			String var10 = var8[var9];
			if (var7[var9].equals("instructions")) {
				var10 = (new WTMessage("wt.workflow.worklist.worklistResource", "21", (Object[]) null))
						.getLocalizedMessage(var2);
			}

			if (var7[var9].equals("primaryBusinessObject")) {
				var10 = (new WTMessage("wt.workflow.worklist.worklistResource", "14", (Object[]) null))
						.getLocalizedMessage(var2);
			}

			if (!var7[var9].equals("self")) {
				var5 = this.tableCell(var10);
				var4.println("<TR>" + var5 + this.tableCell(this.getProcessVariableDisplay(var7[var9], var2)));
			}
		}

		var4.flush();
	}

	private String getProcessVariableDisplay(String var1, Locale var2) {
		Object var3 = this.getProcess().getContext().getValue(var1);
		if (var3 == null) {
			return "&nbsp;";
		} else {
			try {
				Class var4 = this.getProcess().getContext().getVariableClass(var1);
				if (logger.isDebugEnabled()) {
					logger.debug(var1 + " class: " + var4.getName());
				}

				if (WTObject.class.isAssignableFrom(var4)) {
					return WfHtmlFormat.createObjectLink((WTObject) var3, (String) null, var2);
				}

				if (EnumeratedType.class.isAssignableFrom(var4)) {
					return this.translateToHtml(((EnumeratedType) var3).getDisplay(var2));
				}
			} catch (Exception var7) {
			}

			if (var3 instanceof ObjectReference) {
				return var3.toString();
			} else {
				ResourceBundle var5;
				String var8;
				if (var3 instanceof Date) {
					var5 = ResourceBundle.getBundle("wt.util.utilResource", var2);
					var8 = var5.getString("22");
					return this.translateToHtml(WTStandardDateFormat.format((Date) var3, var8));
				} else if (var3 instanceof WfDueDate) {
					var5 = ResourceBundle.getBundle("wt.util.utilResource", var2);
					var8 = var5.getString("22");
					return this.translateToHtml(WTStandardDateFormat.format(((WfDueDate) var3).getDeadline(), var8));
				} else if (var3 instanceof URL) {
					return this.translateToHtml(var3 == null ? null
							: HtmlUtil.createLink(((URL) var3).toExternalForm(), (String) null,
									((URL) var3).toExternalForm()));
				} else {
					String var6 = var3.toString();
					var6 = this.escapeHtml(var6);
					return this.translateToHtml(var6);
				}
			}
		}
	}

	public void setContextObject(Properties var1, Locale var2, OutputStream var3) throws WTException {
		String var4 = var1.getProperty("context");
		if (var4.equals("process")) {
			this.setContextObj(this.getProcess());
		} else if (var4.equals("activity")) {
			this.setContextObj(this.getActivity());
		} else if (var4.equals("primaryBusinessObject")) {
			boolean var5 = true;

			try {
				var5 = SessionServerHelper.manager.setAccessEnforced(false);
				this.setContextObj(this.getProcess().getContext().getValue("primaryBusinessObject"));
			} finally {
				SessionServerHelper.manager.setAccessEnforced(var5);
			}
		} else {
			PrintWriter var9 = this.getPrintWriter(var3, var2);
			var9.print(var4 + " is not a recognized parameter for the setContextObject script ");
			var9.print("process, activity,  and primaryBusinessObject are valid parameters.");
			var9.flush();
		}

	}

	private boolean isUpdateContentDisplayed() {
		Properties var1 = this.getFormData();
		String var2 = var1.getProperty("contentUpdate");
		return var2 != null && var2.length() > 0 && var2.equals("updatingContent");
	}

	private boolean isUpdateRolesDisplayed() {
		Properties var1 = this.getFormData();
		String var2 = var1.getProperty("rolesUpdate");
		return var2 != null && var2.length() > 0 && var2.equals("updatingRoles");
	}

	private boolean isUpdateJITProjectsDisplayed() {
		Properties var1 = this.getFormData();
		String var2 = var1.getProperty("jitProjectsUpdate");
		return var2 != null && var2.length() > 0 && var2.equals("updatingJITProjects");
	}

	public void processContent(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = var1.getProperty("style", "link");
		new Properties();
		if (!this.isUpdateContentDisplayed() && !var5.equals("expand")) {
			var4.println(this.hiddenContextString("contentUpdate", "contentUpdate"));
			WfProcess var6 = this.getProcess();
			WTPrincipal var7 = SessionHelper.getPrincipal();
			if (AccessControlHelper.manager.hasAccess(var7, var6, AccessPermission.MODIFY)) {
				Object[] var8 = new Object[] { var6.getName() };
				String var9 = "";
				if (this.pdmlContext) {
					var9 = getContextActionLink(this.getURLProcessorLink("URLTemplateAction", (Properties) null, true),
							"contentUpdate", "updatingContent",
							(new WTMessage("wt.workflow.worklist.worklistResource", "164", (Object[]) null))
									.getLocalizedMessage(var2));
				} else {
					var9 = getContextActionLink(this.getURLProcessorLink("URLTemplateAction", (Properties) null),
							"contentUpdate", "updatingContent",
							(new WTMessage("wt.workflow.worklist.worklistResource", "118", var8))
									.getLocalizedMessage(var2));
				}

				if (this.pdmlContext) {
					var4.println(var9);
				} else {
					var4.println("<IMG SRC=\"" + CODEBASE + "/" + UPDATE_CONTENT_GIF + "\" border=\"0\">&nbsp" + var9);
				}
			}
		} else {
			var4.println("");
		}

		var4.flush();
	}

	public void updateProjects(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = var1.getProperty("style", "link");
		if (!this.isUpdateJITProjectsDisplayed() && !var5.equals("expand")) {
			var4.println(this.hiddenContextString("jitProjectsUpdate", "jitProjectsUpdate"));
			String var10 = getContextActionLink(this.getURLProcessorLink("URLTemplateAction", (Properties) null),
					"jitProjectsUpdate", "updatingJITProjects",
					(new WTMessage("wt.workflow.worklist.worklistResource", "113", (Object[]) null))
							.getLocalizedMessage(var2));
			var4.println("<IMG SRC=\"" + CODEBASE + "/" + JIT_PROJECTS_GIF + "\" border=\"0\">&nbsp" + var10);
		} else {
			Properties var6 = new Properties();
			var6.setProperty("jitProjectsUpdate", "updatingJITProjects");
			var4.flush();
			boolean var7 = false;
			WfVariableInfo[] var8 = ((WfAssignedActivityTemplate) this.myActivity.getTemplateReference().getObject())
					.getContextSignature().getVariableList();

			for (int var9 = 0; var9 < var8.length; ++var9) {
				if (Project.class.isAssignableFrom(var8[var9].getVariableClass())) {
					var7 = true;
					break;
				}
			}

			if (var7) {
				var4.println("");
			} else {
				var4.println((new WTMessage("wt.workflow.worklist.worklistResource", "114", (Object[]) null))
						.getLocalizedMessage(var2));
			}
		}

		var4.flush();
	}

	public void updateTeams(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = var1.getProperty("style", "link");
		if (!this.isUpdateJITProjectsDisplayed() && !var5.equals("expand")) {
			var4.println(this.hiddenContextString("jitTeamsUpdate", "jitTeamsUpdate"));
			String var10 = getContextActionLink(this.getURLProcessorLink("URLTemplateAction", (Properties) null),
					"jitTeamsUpdate", "updatingJITTeams",
					(new WTMessage("wt.workflow.worklist.worklistResource", "157", (Object[]) null))
							.getLocalizedMessage(var2));
			var4.println("<IMG SRC=\"" + CODEBASE + "/" + JIT_TEAMS_GIF + "\" border=\"0\">&nbsp" + var10);
		} else {
			Properties var6 = new Properties();
			var6.setProperty("jitProjectsUpdate", "updatingJITProjects");
			var4.flush();
			boolean var7 = false;
			WfVariableInfo[] var8 = ((WfAssignedActivityTemplate) this.myActivity.getTemplateReference().getObject())
					.getContextSignature().getVariableList();

			for (int var9 = 0; var9 < var8.length; ++var9) {
				if (TeamTemplate.class.isAssignableFrom(var8[var9].getVariableClass())) {
					var7 = true;
					break;
				}
			}

			if (var7) {
				var4.println("");
			} else {
				var4.println((new WTMessage("wt.workflow.worklist.worklistResource", "158", (Object[]) null))
						.getLocalizedMessage(var2));
			}
		}

		var4.flush();
	}

	public void primaryBusinessObjectLink(Properties var1, Locale var2, OutputStream var3) {
		PrintWriter var4 = this.getPrintWriter(var3, var2);

		try {
			WTObject var5 = (WTObject) this.getProcess().getContext().getValue("primaryBusinessObject");
			if (var5 != null) {
				var4.println(WfHtmlFormat.createObjectLink(var5, (String) null, var2));
				var4.flush();
			}
		} catch (WTRuntimeException var6) {
			var4.println((new WTMessage("wt.workflow.worklist.worklistResource", "24", (Object[]) null))
					.getLocalizedMessage(var2));
			var4.flush();
			logger.debug("The following exception occurred fetching the target object: ", var6);
		} catch (WTException var7) {
			var4.println((new WTMessage("wt.workflow.worklist.worklistResource", "79", (Object[]) null))
					.getLocalizedMessage(var2));
			var4.flush();
			logger.debug("The following exception occurred fetching the target object: ", var7);
		}

	}

	public void augmentRoles(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = var1.getProperty("style", "link");
		if (!this.isUpdateRolesDisplayed() && !var5.equals("expand")) {
			var4.println(this.hiddenContextString("rolesUpdate", "rolesUpdate"));
			String var6 = getContextActionLink(this.getURLProcessorLink("URLTemplateAction", (Properties) null),
					"rolesUpdate", "updatingRoles",
					(new WTMessage("wt.workflow.worklist.worklistResource", "112", (Object[]) null))
							.getLocalizedMessage(var2));
			var4.println("<IMG SRC=\"" + CODEBASE + "/" + ROLES_UPDATE_GIF + "\" border=\"0\">&nbsp" + var6);
		} else {
			var4.println("");
		}

		var4.flush();
	}

	protected static String getContextActionLink(String var0, String var1, String var2, String var3) {
		String var4 = "javascript:refreshForm ('" + var0 + "', '" + var1 + "', '" + var2 + "')";
		String var5 = "class=\"detailsLink\"";
		return HtmlUtil.createLink(var4, var5, var3);
	}

	private boolean isAdHocActivitiesDisplayed() {
		if (this.adHocActivitiesDisplayed == null) {
			Properties var1 = this.getFormData();
			String var2 = var1.getProperty("create");
			if (var2 != null && var2.length() > 0 && var2.equals("adhocactivities")) {
				this.adHocActivitiesDisplayed = Boolean.TRUE;
			} else {
				this.adHocActivitiesDisplayed = Boolean.FALSE;
			}
		}

		return this.adHocActivitiesDisplayed;
	}

	private boolean isAdHocActivitiesInProgress() throws WTException {
		WfAdHocActivity var1 = null;

		try {
			var1 = (WfAdHocActivity) this.getActivity();
		} catch (ClassCastException var3) {
			this.adHocActivitiesInProgress = Boolean.FALSE;
		}

		if (this.adHocActivitiesInProgress == null) {
			WfContainer var2 = var1.getPerformer();
			if (var2 != null && WfState.OPEN.includes(var2.getState())) {
				this.adHocActivitiesInProgress = Boolean.TRUE;
			} else {
				this.adHocActivitiesInProgress = Boolean.FALSE;
			}
		}

		return this.adHocActivitiesInProgress;
	}

	public void adHocActivities(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = var1.getProperty("style", "link");
		String var6 = var1.getProperty("numActivities", "5");
		Properties var7 = this.getFormData();
		String var8 = var7.getProperty("numActivities");
		if (var8 == null || var8.length() <= 0) {
			var7.put("numActivities", var6);
		}

		WfAdHocActivity var9 = null;

		try {
			var9 = (WfAdHocActivity) this.getActivity();
		} catch (ClassCastException var21) {
			return;
		}

		if (!this.isAdHocActivitiesDisplayed() && !var5.equals("expand")) {
			var4.println(this.hiddenContextString("create", "create"));
		} else {
			var4.println(this.hiddenContextString("create", "adhocactivities"));
		}

		var4.flush();
		WfContainer var10 = var9.getPerformer();
		if (var10 == null) {
			String var11;
			if (!this.isAdHocActivitiesDisplayed() && !var5.equals("expand")) {
				var11 = getContextActionLink(this.getURLProcessorLink("URLTemplateAction", (Properties) null), "create",
						"adhocactivities",
						(new WTMessage("wt.workflow.worklist.worklistResource", "86", (Object[]) null))
								.getLocalizedMessage(var2));
				var4.println("<IMG SRC=\"" + CODEBASE + "/" + ADHOC_ACTIVITY_GIF + "\" border=\"0\">&nbsp" + var11);
				var4.flush();
			} else {
				var11 = this.getContextAction();

				try {
					this.setContextAction("CreateAdHocActivities");
					Properties var12 = new Properties();
					String var13 = this.getPageContext().getID();
					var12.put("PageContext", var13);
					SubTemplateService var14 = new SubTemplateService(var12, var2, var3);
					var14.processTemplate(this.getState());
				} catch (Exception var19) {
					throw new WTException(var19);
				} finally {
					this.setContextAction(var11);
				}
			}
		}

	}

	/** @deprecated */
	@Deprecated
	public void taskCompleteButton(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		Properties var5 = new Properties();
		var5.put("action", "WfTaskComplete");
		var5.put("oid", (new ReferenceFactory()).getReferenceString(this.getWorkItem()));
		URL var6 = GatewayURL.getAuthenticatedGateway((URL) null).getURL("wt.enterprise.URLProcessor", "processForm",
				"", var5);
		var4.print(this.generateTaskCompleteButton(var6,
				(new WTMessage("wt.workflow.worklist.worklistResource", "9", (Object[]) null))
						.getLocalizedMessage(var2),
				var5, var2));
		var4.flush();
	}

	protected String generateTaskCompleteButton(URL var1, String var2, Properties var3, Locale var4)
			throws WTException {
		CharArrayWriter var5 = new CharArrayWriter(300);

		try {
			var5.write("<FORM method = \"POST\" action = \"" + var1.toExternalForm() + "\">\n");
			var5.write("<INPUT TYPE=\"hidden\" NAME=\"refreshAction\"  VALUE=\"" + this.getContextAction()
					+ "\"></INPUT>");
			var5.write("<INPUT TYPE=\"hidden\" NAME=\"refreshOid\"  VALUE=\""
					+ (new ReferenceFactory()).getReferenceString(this.getWorkItem()) + "\"></INPUT>");
			this.writeActivityVariables(var5, var4);
			this.writeRoutingChoices(var5);
			this.writeTaskCompleteButton(var5, var2);
			this.writeCloseForm(var5);
		} catch (IOException var7) {
			throw new WTException(var7);
		}

		return var5.toString();
	}

	public void activityVariables(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		CharArrayWriter var5 = new CharArrayWriter(300);

		try {
			this.writeActivityVariables(var5, var2);
		} catch (IOException var7) {
			throw new WTException(var7);
		}

		var4.print(var5.toString());
		var4.flush();
	}

	private void writeActivityVariables(CharArrayWriter var1, Locale var2) throws IOException {
		var1.write("<DIV ALIGN=left>\n");
		var1.write("<TABLE CELLPADDING=3 >\n");
		ProcessData var3 = this.getActivity().getContext();
		WfVariableInfo[] var4 = ((WfAssignedActivityTemplate) this.getActivity().getTemplateReference().getObject())
				.getContextSignature().getVariableList();
		int var5 = var4.length;
		String var6 = "";

		for (int var7 = 0; var7 < var4.length; ++var7) {
			if (this.showVariable(var4[var7].getName(), var4[var7].isVisible())) {
				if (var4[var7].isRequired()) {
					var6 = "*";
				} else {
					var6 = "";
				}

				var1.write("<TR><TD VALIGN=top align=right class=\"propTitle\"><B><font class=tableWfHeader>" + var6
						+ var4[var7].getDisplayName(var2)
						+ (new WTMessage("wt.workflow.worklist.worklistResource", "3", (Object[]) null))
								.getLocalizedMessage(var2)
						+ "</B></TD>");
				String var8 = this.displayActivityVariable(var4[var7], var3, DEFAULT_ROW_NUMBER, DEFAULT_COLUMN_NUMBER,
						var2);
				var1.write("<TD VALIGN=top align=left class=\"propValue\"><font class=tableWfHeader>" + var8
						+ "</TD></TR>\n");
			}
		}

		var1.write("</TABLE>");
	}

	public void routingChoices(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		CharArrayWriter var5 = new CharArrayWriter(300);
		if (!this.isAdHocActivitiesInProgress() && !this.isAdHocActivitiesDisplayed()) {
			try {
				this.writeRoutingChoices(var5);
			} catch (IOException var7) {
				throw new WTException(var7);
			}

			var4.print(var5.toString());
			var4.flush();
		}

	}

	void writeRoutingChoices(CharArrayWriter var1) throws IOException, WTException {
		if (!this.isAdHocActivitiesInProgress() && !this.isAdHocActivitiesDisplayed()) {
			var1.write("<TABLE CELLPADDING=3 WIDTH=\"90%\" >\n");
			if (this.getActivity().getRouterType().equals(WfRouterType.MANUAL)
					|| this.getActivity().getRouterType().equals(WfRouterType.MANUAL_EXCLUSIVE)) {
				Vector var2 = this.getActivity().getUserEventList().toVector();
				String var3 = "checkbox";
				String var4 = "";
				if (this.getActivity().getRouterType().equals(WfRouterType.MANUAL_EXCLUSIVE)) {
					var3 = "radio";
					var4 = " checked";
				}

				int var5 = 0;

				for (Enumeration var6 = var2.elements(); var6.hasMoreElements(); var4 = "") {
					String var7 = (String) var6.nextElement();
					String var8 = (String) this.getState().getFormData().get("WfUserEvent" + var5);
					if (var8 != null && var8.equals(var7)) {
						var4 = " checked";
					}

					var1.write("<TR><TD align=left><font size=\"2\">&nbsp;<INPUT type=" + var3 + var4
							+ " name=\"WfUserEvent" + var5 + "\" value=\"" + var7 + "\" > " + var7 + "</TD></TR>\n");
					if (var3.equals("checkbox")) {
						++var5;
					}
				}
			}

			var1.write("</TABLE><BR>");
		}

	}

	public void soloTaskCompleteButton(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		if (this.isAdHocActivitiesInProgress()) {
			var4.print((new WTMessage("wt.workflow.worklist.worklistResource", "106", (Object[]) null))
					.getLocalizedMessage(var2));
		} else {
			String var5 = null;
			if (((WfAssignedActivityTemplate) this.myActivity.getTemplateReference().getObject()).isSigningRequired()) {
				SignatureEngine var6 = SignatureEngineFactory.getInstance();
				if (var6 != null) {
					String var7 = null;
					if (this.isAdHocActivitiesDisplayed()) {
						var7 = (new WTMessage("wt.org.electronicIdentity.engines.EnginesRB", "4", (Object[]) null))
								.getLocalizedMessage(var2);
					} else {
						var7 = (new WTMessage("wt.org.electronicIdentity.engines.EnginesRB", "3", (Object[]) null))
								.getLocalizedMessage(var2);
					}

					var4.print("<font face=\"arial, helvetica\" size=\"2\">" + var7 + "<p>");
					SigVariableInfo[] var8 = var6.getVariableInfo();

					for (int var9 = 0; var9 < var8.length; ++var9) {
						String var10 = var8[var9].isRequired() ? "*" : "&nbsp;";
						String var11 = this.formatLabel(var10 + var8[var9].getLabel().getLocalizedMessage(var2));
						var4.print("<font face=\"arial, helvetica\" size=\"2\"><B>" + var11 + "</B>");
						var4.print("<input name=\"" + var8[var9].getFormField() + "\"");
						if (var8[var9].isPasswordField()) {
							var4.print(" type=\"password\"");
						} else {
							var4.print(" type=\"text\"");
						}

						int var12 = var8[var9].getPreferredSize() < 40 ? var8[var9].getPreferredSize() : 40;
						var4.print(" size=\"" + var12 + "\" AUTOCOMPLETE=\"OFF\">");
						var4.println("<BR>");
					}

					var4.println("<P>");
				}
			}

			if (this.isAdHocActivitiesDisplayed()) {
				var5 = (new WTMessage("wt.workflow.worklist.worklistResource", "87", (Object[]) null))
						.getLocalizedMessage(var2);
				var4.print("<INPUT type = \"BUTTON\" value = \"" + var5 + "\" onClick=validatePredecessors()>");
			} else if (this.getContextAction() != null && this.getContextAction().equals("submit")) {
				var5 = (new WTMessage("wt.lifecycle.lifecycleResource", "90", (Object[]) null))
						.getLocalizedMessage(var2);
				var4.print("<INPUT type = \"SUBMIT\" value = \"" + var5 + "\">");
			} else {
				var5 = (new WTMessage("wt.workflow.worklist.worklistResource", "9", (Object[]) null))
						.getLocalizedMessage(var2);
				var4.print("<INPUT type = \"SUBMIT\" value = \"" + var5 + "\">");
			}
		}

		var4.flush();
	}

	void writeTaskCompleteButton(CharArrayWriter var1, String var2) throws IOException, WTException {
		var1.write("<DIV ALIGN=left><INPUT type = \"SUBMIT\" value = \"" + var2 + "\">");
	}

	public void beginForm(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		CharArrayWriter var5 = new CharArrayWriter(300);

		try {
			Properties var6 = new Properties();
			var6.put("action", "WfTaskComplete");
			var6.put("oid", (new ReferenceFactory()).getReferenceString(this.getWorkItem()));
			URL var7 = GatewayURL.getAuthenticatedGateway((URL) null).getURL("wt.workflow.work.WorkItemURLProcessor",
					"processForm", "", var6);
			this.writeBeginForm(var5, var7);
		} catch (IOException var8) {
			throw new WTException(var8);
		}

		var4.print(var5.toString());
		var4.flush();
	}

	protected void writeBeginForm(CharArrayWriter var1, URL var2) throws IOException, WTException {
		var1.write("<FORM method = \"POST\" action = \"" + var2.toExternalForm() + "\">\n");
		var1.write("<INPUT TYPE=\"hidden\" NAME=\"refreshAction\"  VALUE=\"" + this.getContextAction() + "\"></INPUT>");
		var1.write("<INPUT TYPE=\"hidden\" NAME=\"refreshOid\"  VALUE=\""
				+ (new ReferenceFactory()).getReferenceString(this.getWorkItem()) + "\"></INPUT>");
	}

	public void closeForm(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		CharArrayWriter var5 = new CharArrayWriter(300);

		try {
			this.writeCloseForm(var5);
		} catch (IOException var7) {
			throw new WTException(var7);
		}

		var4.print(var5.toString());
		var4.flush();
	}

	void writeCloseForm(CharArrayWriter var1) throws IOException {
		var1.write("</FORM><BR>\n");
	}

	String displayActivityVariableDisplayName(WfVariableInfo var1, ProcessData var2, String var3, Locale var4) {
		String var5 = var1.getName();
		WfVariable var6 = var2.getVariable(var5);
		Class var7 = var6.getVariableClass();
		String var8 = var6.getTypeName();
		Object var9 = var6.getValue();
		if (this.showVariable(var5, var1.isVisible())) {
			if (logger.isDebugEnabled()) {
				logger.debug("TypeName:      " + var6.getTypeName());
				logger.debug("Display Name:  " + var6.getName());
				logger.debug("Value       :  " + var6.getValue());
				logger.debug("Default Value  " + var1.getDefaultValue());
				logger.debug("Mutable        " + var1.isMutable());
			}

			if (!var3.equals("true")) {
				return var1.getDisplayName(var4);
			} else {
				String var10 = "";
				if (var1.isRequired()) {
					var10 = "*";
				} else {
					var10 = "";
				}

				return "<B><font class=tableWfHeader>" + var10 + var1.getDisplayName(var4)
						+ (new WTMessage("wt.workflow.worklist.worklistResource", "3", (Object[]) null))
								.getLocalizedMessage(var4)
						+ "</B>";
			}
		} else {
			return "&nbsp;";
		}
	}

	String displayActivityVariable(WfVariableInfo var1, ProcessData var2, String var3, String var4, Locale var5) {
		String var6 = var1.getName();
		WfVariable var7 = var2.getVariable(var6);
		Class var8 = var7.getVariableClass();
		String var9 = var7.getTypeName();
		Object var10 = var7.getValue();
		WTContainerRef var11 = null;
		Object var12 = this.getContextObj();
		if (var12 instanceof WTContained) {
			var11 = ((WTContained) var12).getContainerReference();
		}

		if (this.showVariable(var6, var1.isVisible())) {
			if (logger.isDebugEnabled()) {
				logger.debug("TypeName:      " + var7.getTypeName());
				logger.debug("Display Name:  " + var7.getName());
				logger.debug("Value       :  " + var7.getValue());
				logger.debug("Default Value  " + var1.getDefaultValue());
				logger.debug("Mutable        " + var1.isMutable());
			}

			String var13;
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Processing variable of class: " + var8.getName());
				}

				if (WTObject.class.isAssignableFrom(var8)) {
					if (WTPrincipal.class.isAssignableFrom(var8)) {
						if (var1.isReadOnly()) {
							return var2.getValue(var6) == null ? "&nbsp;"
									: (WTUser.class.isAssignableFrom(var8)
											? ((WTUser) var2.getValue(var6)).getFullName()
											: ((WTPrincipal) var2.getValue(var6)).getName());
						}

						return this.principalSelector(var1.getTypeName(), (WTPrincipal) var2.getValue(var6),
								var1.getName());
					}

					String var17;
					String var18;
					boolean var19;
					int var20;
					ReferenceFactory var21;
					Vector var22;
					Vector var25;
					int var26;
					Vector var28;
					if (var9.equals("wt.team.Team")) {
						if (var1.isReadOnly()) {
							return this.translateToHtml(
									var2.getValue(var6) == null ? null : ((Team) var2.getValue(var6)).getName());
						}

						var13 = null;
						TeamReference var29;
						if (var2.getValue(var6) != null) {
							new ReferenceFactory();
							var29 = TeamReference.newTeamReference((Team) var2.getValue(var6));
							var13 = var29.getIdentity();
						}

						var22 = TeamHelper.service.findTeams();
						var21 = new ReferenceFactory();
						var28 = new Vector(var22.size());
						var25 = new Vector(var22.size());
						var26 = 0;
						var19 = !var1.isRequired();

						for (var20 = 0; var20 < var22.size(); ++var20) {
							var29 = (TeamReference) var22.elementAt(var20);
							var18 = var21.getReferenceString(var29);
							var28.addElement(var18);
							var17 = var29.getIdentity();
							var25.addElement(var17);
							if (var13 != null && var13.equals(var17)) {
								var26 = var20;
								if (var19) {
									var26 = var20 + 1;
								}
							}
						}

						return HtmlUtil.createSelectHTML(var6, (String) null, var28, var25, var19, var26);
					}

					if (var9.equals("wt.team.TeamTemplate")) {
						if (var1.isReadOnly()) {
							return this.translateToHtml(var2.getValue(var6) == null ? null
									: ((TeamTemplate) var2.getValue(var6)).getName());
						}

						var13 = null;
						TeamTemplateReference var23;
						if (var2.getValue(var6) != null) {
							new ReferenceFactory();
							var23 = TeamTemplateReference.newTeamTemplateReference((TeamTemplate) var2.getValue(var6));
							var13 = var23.getIdentity();
						}

						var22 = TeamHelper.service.findTeamTemplates(var11);
						var21 = new ReferenceFactory();
						var28 = new Vector(var22.size());
						var25 = new Vector(var22.size());
						var26 = 0;
						var19 = !var1.isRequired();

						for (var20 = 0; var20 < var22.size(); ++var20) {
							var23 = (TeamTemplateReference) var22.elementAt(var20);
							var18 = var21.getReferenceString(var23);
							var28.addElement(var18);
							var17 = var23.getIdentity();
							var25.addElement(var17);
							if (var13 != null && var13.equals(var17)) {
								var26 = var20;
								if (var19) {
									var26 = var20 + 1;
								}
							}
						}

						return HtmlUtil.createSelectHTML(var6, (String) null, var28, var25, var19, var26);
					}

					return WfHtmlFormat.createObjectLink((WTObject) var2.getValue(var6), (String) null, var5);
				}

				if (EnumeratedType.class.isAssignableFrom(var8)) {
					if (var1.isReadOnly()) {
						return var2.getValue(var6) == null ? "&nbsp;"
								: ((EnumeratedType) var2.getValue(var6)).getDisplay(var5);
					}

					EnumeratedType var27 = null;
					if (var2.getValue(var6) != null) {
						var27 = (EnumeratedType) var2.getValue(var6);
					}

					if (this.getFormData() != null && this.getFormData().getProperty(var6) != null
							&& this.getFormData().getProperty(var6).length() > 0) {
						var27 = EnumeratedTypeUtil.toEnumeratedType(var1.getTypeName(),
								this.getFormData().getProperty(var6));
					}

					return this.enumeratedTypeSelector(var8, var27, var6, var5);
				}
			} catch (WTException var24) {
				logger.error("", var24);
			}

			var13 = this.getFormData().getProperty(var6) == null
					? (var2.getValue(var6) == null ? null : var2.getValue(var6).toString())
					: this.getFormData().getProperty(var6);
			if (var1.getTypeName().equals("java.lang.String")) {
				if (var1.isReadOnly()) {
					var13 = this.escapeHtml(var13);
					return this.translateToHtml(var13);
				} else {
					return "<TEXTAREA name=\"" + var6 + "\"  rows=\"" + var3 + "\" cols=\"" + var4 + "\" WRAP>"
							+ (var13 == null ? "" : var13) + "</TEXTAREA>";
				}
			} else {
				String var14;
				if (!var1.getTypeName().equals("java.lang.Boolean") && !var1.getTypeName().equals("boolean")) {
					if (var1.getTypeName().equals("java.net.URL")) {
						if (var1.isReadOnly()) {
							return HtmlUtil.createLink(var13, (String) null, var13);
						} else {
							var14 = "<INPUT type=\"TEXT\" name=\"" + var6 + "\" size=\"" + var4 + "\" value=\""
									+ (var13 == null ? "" : var13) + "\">";
							if (var13 != null) {
								var14 = var14 + "&nbsp;" + HtmlUtil.createLink(var13, (String) null, var13);
							}

							return var14;
						}
					} else {
						ResourceBundle var15;
						String var16;
						if (var1.getTypeName().equals("java.util.Date")) {
							var14 = "";
							if (this.getFormData().getProperty(var6) != null) {
								var14 = var13;
							} else if (var2.getValue(var6) != null) {
								var15 = ResourceBundle.getBundle("wt.util.utilResource", var5);
								var16 = var15.getString("22");
								var14 = WTStandardDateFormat.format((Date) var2.getValue(var6), var16);
							}

							return var1.isReadOnly() ? this.translateToHtml(var14)
									: "<INPUT type=\"TEXT\" name=\"" + var6 + "\" size=\"" + var4 + "\" value=\""
											+ var14 + "\">";
						} else if (var1.getTypeName().equals("wt.workflow.engine.WfDueDate")) {
							var14 = "";
							if (this.getFormData().getProperty(var6) != null) {
								var14 = var13;
							} else if (var2.getValue(var6) != null) {
								var15 = ResourceBundle.getBundle("wt.util.utilResource", var5);
								var16 = var15.getString("22");
								var14 = WTStandardDateFormat.format(((WfDueDate) var2.getValue(var6)).getDeadline(),
										var16);
							}

							return var1.isReadOnly() ? this.translateToHtml(var14)
									: "<INPUT type=\"TEXT\" name=\"" + var6 + "\" size=\"" + var4 + "\" value=\""
											+ var14 + "\">";
						} else if (var1.isReadOnly()) {
							var13 = this.escapeHtml(var13);
							return this.translateToHtml(var13);
						} else {
							return "<INPUT type=\"TEXT\" name=\"" + var6 + "\" size=\"" + var4 + "\" value=\""
									+ (var13 == null ? "" : var13) + "\">";
						}
					}
				} else if (var1.isReadOnly()) {
					return (Boolean) var2.getValue(var6)
							? (new WTMessage("wt.workflow.worklist.worklistResource", "134", (Object[]) null))
									.getLocalizedMessage(var5)
							: (new WTMessage("wt.workflow.worklist.worklistResource", "135", (Object[]) null))
									.getLocalizedMessage(var5);
				} else {
					var14 = "";
					if (this.getFormData().getProperty(var6) != null) {
						var14 = this.getFormData().getProperty(var6).equals("true") ? "checked" : "";
					} else if (var2.getValue(var6) != null) {
						var14 = (Boolean) var2.getValue(var6) ? "checked" : "";
					}

					return "<INPUT type=\"CHECKBOX\" name=\"" + var6 + "\" size=\"" + var4 + "\" value=\"true\"" + var14
							+ ">";
				}
			}
		} else {
			return "&nbsp;";
		}
	}

	public void activityVariableDisplayName(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = var1.getProperty("variable");
		String var6 = var1.getProperty(FORMATTED, DEFAULT_FORMATTED_VALUE);
		WfVariableInfo var7 = ((WfAssignedActivityTemplate) this.getActivity().getTemplateReference().getObject())
				.getContextSignature().getVariableInfo(var5);
		if (var7 == null) {
			Object[] var8 = new Object[] { var5 };
			var4.println(
					(new WTMessage("wt.workflow.worklist.worklistResource", "133", var8)).getLocalizedMessage(var2));
		} else {
			ProcessData var9 = this.getActivity().getContext();
			var4.println(this.displayActivityVariableDisplayName(var7, var9, var6, var2));
		}

		var4.flush();
	}

	public void activityVariable(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = var1.getProperty("variable");
		String var6 = var1.getProperty(ROWS, DEFAULT_ROW_NUMBER);
		String var7 = var1.getProperty(COLUMNS, DEFAULT_COLUMN_NUMBER);
		WfVariableInfo var8 = ((WfAssignedActivityTemplate) this.getActivity().getTemplateReference().getObject())
				.getContextSignature().getVariableInfo(var5);
		if (var8 == null) {
			Object[] var9 = new Object[] { var5 };
			var4.println(
					(new WTMessage("wt.workflow.worklist.worklistResource", "133", var9)).getLocalizedMessage(var2));
		} else {
			ProcessData var10 = this.getActivity().getContext();
			var4.println(this.displayActivityVariable(var8, var10, var6, var7, var2));
		}

		var4.flush();
	}

	public void taskURL(Properties var1, Locale var2, OutputStream var3) throws WTException {
		this.taskURL(var1, var2, var3, this.getContextRef());
	}

	public void taskURL(Properties var1, Locale var2, OutputStream var3, WTContainerRef var4) throws WTException {
		PrintWriter var5 = this.getPrintWriter(var3, var2);
		String var6 = this.getTaskUrl(this.getContextRef());
		WorkItem var7 = this.getWorkItem();
		String var8 = WorkflowHtmlUtil.getExternalTaskURL(var7);
		if (var8 != null) {
			String var9 = HTMLEncoder.encodeForHTMLContent(var8);
			String var10 = "<A id=\"WfExternalTaskTProcessor02\" HREF=\"" + var9 + "\">"
					+ HTMLEncoder.encodeForHTMLContent(this.myActivity.getName()) + "</A>";
			String var11 = this.formatLabel(
					(new WTMessage("wt.workflow.worklist.worklistResource", "NAVIGATE_LABEL", (Object[]) null))
							.getLocalizedMessage(var2));
			String var12 = this.formatLabel(
					(new WTMessage("wt.workflow.worklist.worklistResource", "WINDCHILL_LABEL", (Object[]) null))
							.getLocalizedMessage(var2));
			var5.println(var10 + "(" + var11 + ")");
			var5.println("<br>");
			var5.println(var6 + "(" + var12 + ")");
		} else {
			var5.println(var6);
		}

		var5.flush();
	}

	public void pageTitle(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println("<TITLE>" + this.getActivity().getName() + "</TITLE>");
		var4.flush();
	}

	public void listProcessContent(Properties var1, Locale var2, OutputStream var3) throws Exception {
		try {
			this.setContextObj(this.getProcess());
			this.listContent(var1, var2, var3);
		} catch (WTException var5) {
			if (!(var5 instanceof NotAuthorizedException)) {
				throw var5;
			}

			logger.debug("", var5);
		}

	}

	protected boolean showVariable(String var1, boolean var2) {
		if (!var2) {
			return false;
		} else if (var1.equals("instructions")) {
			return false;
		} else {
			return !var1.equals("primaryBusinessObject");
		}
	}

	public void createWfNavigationBar(Properties var1, Locale var2, OutputStream var3) throws WTException {
		this.createGlobalNavigationBar(var1, var2, var3);
	}

	public void getHelpURL(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = var1.getProperty("helpKey");
		String var6 = var1.getProperty("resource");
		if (var5 == null) {
			throw new WTException("wt.lifecycle.lifecycleResource", "1", new Object[] { "helpKey" });
		} else if (var6 == null) {
			throw new WTException("wt.lifecycle.lifecycleResource", "1", new Object[] { "resource" });
		} else {
			String var7;
			try {
				var7 = CODEBASE;
				var4.println(var7 + "/" + WTMessage.getLocalizedMessage(var6, var5, (Object[]) null, var2));
			} catch (MissingResourceException var10) {
				Logger var9 = logger;
				var7 = var10.getMessage();
				var9.error("*ERROR* " + var7 + ": {" + var10.getClassName() + " - " + var10.getKey() + "}");
				throw var10;
			}

			var4.flush();
		}
	}

	public String principalSelector(String var1, WTPrincipal var2, String var3) throws WTException {
		Vector var4 = new Vector();
		Vector var5 = new Vector();
		Vector var6;
		Vector var7;
		if (var1.equals("wt.org.WTPrincipal") || var1.equals("wt.org.WTUser")) {
			var6 = (Vector) MethodContext.getContext().get("WF_USERS");
			var7 = (Vector) MethodContext.getContext().get("WF_DISPLAY_USERS");
			if (var6 != null && var7 != null) {
				var5 = var6;
				var4 = var7;
			} else {
				HashMap var8 = new HashMap<String, List<String>>();
				var8.put("name", new ArrayList(Arrays.asList("*")));
				Enumeration var9 = OrganizationServicesHelper.manager.queryPrincipals(WTUser.class, var8);
				SortedEnumByPrincipal var10 = new SortedEnumByPrincipal(var9, false, 1);

				while (var10.hasMoreElements()) {
					WTUser var11 = (WTUser) var10.nextElement();
					var4.addElement(var11.getName());
					var5.addElement(SortedEnumByPrincipal.getLastNameFirstName(var11));
				}

				MethodContext.getContext().put("WF_USERS", var5);
				MethodContext.getContext().put("WF_DISPLAY_USERS", var4);
			}
		}

		if (var1.equals("wt.org.WTPrincipal") || var1.equals("wt.org.WTGroup")) {
			var6 = (Vector) MethodContext.getContext().get("WF_GROUPS");
			var7 = (Vector) MethodContext.getContext().get("WF_DISPLAY_GROUPS");
			if (var6 != null && var7 != null) {
				var5 = var6;
				var4 = var7;
			} else {
				SortedEnumeration var12 = new SortedEnumeration(OrganizationServicesMgr.allGroups(),
						new CollationKeyFactory(WTContext.getContext().getLocale()));

				while (var12.hasMoreElements()) {
					WTGroup var14 = (WTGroup) var12.nextElement();
					var4.addElement(var14.getName());
					var5.addElement(var14.getName());
				}

				MethodContext.getContext().put("WF_GROUPS", var5);
				MethodContext.getContext().put("WF_DISPLAY_GROUPS", var4);
			}
		}

		int var13 = var2 == null ? 0 : var4.indexOf(var2.getName()) + 1;
		return HtmlUtil.createSelectHTML(var3, (String) null, var4, var5, true, var13);
	}

	public String enumeratedTypeSelector(Class var1, EnumeratedType var2, String var3, Locale var4) throws WTException {
		Vector var5 = new Vector();
		Vector var6 = new Vector();
		int var7 = 0;
		String var8 = "<select name=\"" + var3 + "\" size=\"1\">";
		var8 = var8.concat("<option value = > </option>\n");

		try {
			String var9 = this.getSimpleName(var1);
			Method var10 = null;
			var10 = var1.getMethod("get" + var9 + "Set", (Class[]) null);
			Object var11 = var10.invoke((Object) null, (Object[]) null);
			EnumeratedType[] var12 = (EnumeratedType[]) var11;
			int var13 = 0;

			for (int var14 = 0; var14 < var12.length; ++var14) {
				if (var12[var14].isSelectable()) {
					++var13;
					var5.addElement(var12[var14].toString());
					var6.addElement(var12[var14].getDisplay(var4));
					if (var12[var14].equals(var2)) {
						var7 = var13;
					}
				}
			}
		} catch (InvocationTargetException var15) {
			logger.error("", var15);
		} catch (NoSuchMethodException var16) {
			logger.error("", var16);
		} catch (IllegalAccessException var17) {
			logger.error("", var17);
		}

		return HtmlUtil.createSelectHTML(var3, (String) null, var5, var6, true, var7);
	}

	protected String getSimpleName(Class var1) {
		char[] var2 = var1.getName().toCharArray();
		String var3 = null;

		for (int var4 = var2.length - 1; var4 > 0; --var4) {
			if (var2[var4] == '.') {
				var3 = var1.getName().substring(var4 + 1);
				break;
			}
		}

		return var3;
	}

	public void messageText(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getMessage());
		var4.flush();
	}

	public void projectLinkAttributes(Properties var1, Locale var2, OutputStream var3)
			throws WTException, WTPropertyVetoException {
		WTContainerRef var4 = null;
		Object var5 = this.getContextObj();
		if (var5 instanceof WTContained) {
			var4 = ((WTContained) var5).getContainerReference();
		}

		WTContainer var6 = (WTContainer) var4.getObject();
		if (!(var6 instanceof Project2)) {
			throw new WTRuntimeException("Wrong context: expected ProjectLink got " + var6.getClass().getName());
		} else {
			this.projectLinkAttributes(var1, var2, var3, (Project2) var6);
		}
	}

	public void projectLinkAttributes(Properties var1, Locale var2, OutputStream var3, Project2 var4)
			throws WTException, WTPropertyVetoException {
		PrintWriter var5 = this.getPrintWriter(var3, var2);
		NetmarketsHref var6 = new NetmarketsHref(
				(new ReferenceFactory()).getReference(var4.getPersistInfo().getObjectIdentifier().getStringValue()));
		var6.setFullyQualified(true);
		String var7 = var6.getHref();
		String var9;
		if (logger.isDebugEnabled()) {
			Logger var8 = logger;
			var9 = HtmlUtil.createInlineLink(var7, "", var4.getName());
			var8.debug("   .....Project: " + var9);
		}

		var5.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var5.println(
				WTMessage.getLocalizedMessage("wt.workflow.worklist.worklistResource", "200", (Object[]) null, var2));
		var5.println("</b></font></td>");
		var5.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var5.println(var4.getName());
		var5.println("</font></td></tr>\n");
		var5.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var5.println(
				WTMessage.getLocalizedMessage("wt.workflow.worklist.worklistResource", "201a", (Object[]) null, var2));
		var5.println("</b></font></td>");
		var5.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		if (var4.getOwnerReference() != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("   .....Project Manager: " + var4.getOwnerReference().getFullName());
			}

			var5.println(var4.getOwnerReference().getFullName() + "</a>");
		} else {
			var5.println(WTMessage.getLocalizedMessage("wt.workflow.worklist.worklistResource", "204", (Object[]) null,
					var2));
		}

		var5.println("</font></td></tr>");
		var5.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var5.println(
				WTMessage.getLocalizedMessage("wt.workflow.worklist.worklistResource", "202", (Object[]) null, var2));
		var5.println("</b></font></td>");
		var5.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		OrgContainer var10 = WTContainerHelper.service.getOrgContainer(var4);
		var5.println(var10 != null ? var10.getName() : "&nbsp;");
		var5.println("</font></td></tr>\n");
		if (logger.isDebugEnabled()) {
			logger.debug("   .....Project Description: " + var4.getDescription());
		}

		var5.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var5.println(
				WTMessage.getLocalizedMessage("wt.workflow.worklist.worklistResource", "203", (Object[]) null, var2));
		var5.println("</b></font></td>");
		var5.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var9 = var4.getDescription();
		if (var9 != null && var9.trim().length() > 0) {
			var5.println(this.translateToHtml(var9));
		} else {
			var5.println(WTMessage.getLocalizedMessage("wt.workflow.worklist.worklistResource", "204", (Object[]) null,
					var2));
		}

		var5.println("</font></td></tr>\n");
		var5.println("<tr><td colspan=\"2\"><hr size=\"1\" color=\"#40637A\"></td></tr>");
		var5.flush();
	}

	protected String formatLabel(String var1) {
		return var1
				+ (new WTMessage("wt.workflow.worklist.worklistResource", "3", (Object[]) null)).getLocalizedMessage()
				+ "&nbsp;";
	}

	public void enterComments(Properties var1, Locale var2, OutputStream var3) throws WTException {
		WfActivity var4 = null;
		var4 = (WfActivity) this.workItem.getSource().getObject();
		if (((WfAssignedActivityTemplate) var4.getTemplateReference().getObject()).isSigningRequired()) {
			String var5 = this.getContextAction();
			PrintWriter var6 = this.getPrintWriter(var3, var2);
			String var7 = (new WTMessage("wt.lifecycle.lifecycleResource", "99", (Object[]) null))
					.getLocalizedMessage(var2);
			var6.println(var7 + (new WTMessage("wt.lifecycle.lifecycleResource", "150", (Object[]) null))
					.getLocalizedMessage(var2) + "<BR><textarea name=\"comments\" rows=\"10\" cols=\"60\"></textarea>");
			var6.flush();
		}

	}

	public void setMessage(String var1) {
		this.messageText = var1;
	}

	public String getMessage() {
		return this.messageText;
	}

	public String getGroupLabel(Locale var1) throws WTException {
		return this.formatLabel(
				(new WTMessage("wt.workflow.definer.DefinerRB", "216", (Object[]) null)).getLocalizedMessage(var1));
	}

	public void groupLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getGroupLabel(var2));
		var4.flush();
	}

	public String getGroup() throws WTException {
		String var1 = "&nbsp;";
		ReferenceFactory var2 = new ReferenceFactory();
		WTPrincipalReference var3 = (WTPrincipalReference) var2
				.getReference(OwnershipHelper.getOwner(this.getWorkItem()));
		WTPrincipal var4 = (WTPrincipal) var3.getObject();
		if (var4 instanceof WTUser) {
			WfAssignee var5 = ((WfAssignment) this.getWorkItem().getParentWA().getObject()).getAssignee();
			if (var5 instanceof WfPrincipalAssignee
					&& ((WfPrincipalAssignee) var5).getPrincipal().getObject() instanceof WTGroup) {
				var1 = ((WTGroup) ((WfPrincipalAssignee) var5).getPrincipal().getObject()).getName();
			}
		}

		return var1;
	}

	public void group(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.translateToHtml(this.getGroup()));
		var4.flush();
	}

	public boolean isWorkItemOwner() {
		boolean var1 = false;

		try {
			WTPrincipal var2 = SessionHelper.manager.getPrincipal();
			WorkItem var3 = this.getWorkItem();
			var1 = OwnershipHelper.isOwnedBy(var3, var2);
			if (!var1) {
				WTPrincipal var4 = OwnershipHelper.getOwner(var3);
				if (var4 instanceof WTGroup) {
					WTGroup var5 = (WTGroup) var4;
					if (var5.isMember(var2)) {
						var1 = true;
					}
				}
			}

			if (logger.isDebugEnabled()) {
				Logger var7 = logger;
				String var8 = var2.getName();
				var7.debug("=> WfTaskProcessor.isWorkItemOwnwer" + var8 + " is " + (var1 ? "" : " not")
						+ "the owner of " + var3);
			}
		} catch (WTException var6) {
		}

		return var1;
	}

	public void processNotebook(Properties var1, Locale var2, OutputStream var3) throws WTException {
		WfProcess var4 = this.getProcess();
		String var5 = "";
		ObjectIdentifier var6 = PersistenceHelper.getObjectIdentifier(var4);
		HashMap var7 = new HashMap();
		var7.put("processOid", var6.toString());

		try {
			WTObject var8 = (WTObject) this.getProcess().getContext().getValue("primaryBusinessObject");
			if (var8 != null && !(var8 instanceof SubjectOfNotebook)) {
			}

			NetmarketsHref var9 = new NetmarketsHref(NetmarketsType.notebookfolder, NetmarketsCommand.list,
					(new ReferenceFactory())
							.getReference(PersistenceHelper.getObjectIdentifier(var4).getStringValue()));
			String var10 = var9.getHref();
			String var11 = (new WTMessage("wt.workflow.notebook.notebookResource", "0", (Object[]) null))
					.getLocalizedMessage(var2);
			String var12 = "><IMG SRC=\"" + CODEBASE + "/" + NOTEBOOK_GIF + "\" border=\"0\" alt=\"" + var11 + "\"";
			if (this.pdmlContext) {
				var5 = " <A HREF=\"javascript:var nbw = wfWindowOpen('" + var10
						+ "','nmnotebooklist',config='resizable=yes,scrollbars=yes,menubar=yes,toolbar=yes,location=yes,status=yes')\" class=\"detailsLink\" >"
						+ var11 + "</A>";
			} else {
				var5 = " <A HREF=\"javascript:var nbw = window.open ('" + var10
						+ "','nmnotebooklist',config='resizable=yes,scrollbars=yes,menubar=yes,toolbar=yes,location=yes,status=yes')\""
						+ var12 + ">" + var11 + "</A>";
			}

			if (logger.isDebugEnabled()) {
				logger.debug("\n WfTaskProcessor.processNotebook (): link  == " + var10);
				logger.debug("\n WfTaskProcessor.processNotebook (): finalLink == " + var5);
			}
		} catch (Exception var13) {
			logger.debug("", var13);
		}

		PrintWriter var14 = this.getPrintWriter(var3, var2);
		var14.println(var5);
		var14.flush();
	}

	public void processForum(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		WfProcess var5 = this.getProcess();
		NetmarketsHref var6 = new NetmarketsHref(NetmarketsType.forum, NetmarketsCommand.discuss,
				(new ReferenceFactory()).getReference(var5.getPersistInfo().getObjectIdentifier().getStringValue()));
		var6.setFullyQualified(true);
		String var7 = var6.getHref();
		String var8 = "";
		String var9 = "";
		String var10 = "";
		if (this.pdmlContext) {
			var8 = (new WTMessage("wt.workflow.worklist.worklistResource", "165", (Object[]) null))
					.getLocalizedMessage(var2);
			var10 = "<A HREF=\"javascript:var forumWindow=wfWindowOpen('" + var7
					+ "','nmforumview',config='resizable=yes,scrollbars=yes,menubar=yes,toolbar=yes,location=yes,status=yes')\" class=\"detailsLink\">"
					+ var8 + "</A>";
		} else {
			var8 = (new WTMessage("wt.workflow.forum.forumResource", "27", (Object[]) null)).getLocalizedMessage(var2);
			var9 = "><IMG SRC=\"" + CODEBASE + "/" + FORUM_GIF + "\" border=\"0\" alt=\"" + var8 + "\"";
			var10 = "<A HREF=\"javascript:var forumWindow=window.open ('" + var7
					+ "','nmforumview',config='resizable=yes,scrollbars=yes,menubar=yes,toolbar=yes,location=yes,status=yes')\""
					+ var9 + ">" + var8 + "</A>";
		}

		var4.println(var10);
		var4.flush();
	}

	public void PJLProjectLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "200", (Object[]) null))
				.getLocalizedMessage(var2)));
		var4.flush();
	}

	public void PJLProject(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		WTContainerRef var5 = null;
		Object var6 = this.getContextObj();
		if (var6 instanceof WTContained) {
			var5 = ((WTContained) var6).getContainerReference();
		}

		WTContainer var7 = (WTContainer) var5.getObject();
		if (!(var7 instanceof Project2)) {
			throw new WTRuntimeException("Wrong context: expected ProjectLink got " + var7.getClass().getName());
		} else {
			Project2 var8 = (Project2) var7;
			NetmarketsHref var9 = new NetmarketsHref((new ReferenceFactory())
					.getReference(var8.getPersistInfo().getObjectIdentifier().getStringValue()));
			var9.setFullyQualified(true);
			String var10 = var9.getHref();
			var4.println(var10);
			var4.flush();
		}
	}

	public void PJLProjectSponsorLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "201", (Object[]) null))
				.getLocalizedMessage(var2)));
		var4.flush();
	}

	public void PJLProjectSponsor(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		WTContainerRef var5 = null;
		Object var6 = this.getContextObj();
		if (var6 instanceof WTContained) {
			var5 = ((WTContained) var6).getContainerReference();
		}

		WTContainer var7 = (WTContainer) var5.getObject();
		if (!(var7 instanceof Project2)) {
			throw new WTRuntimeException("Wrong context: expected ProjectLink got " + var7.getClass().getName());
		} else {
			Project2 var8 = (Project2) var7;
			if (var8.getSponsor() != null) {
				var4.println("<a href=\"mailto:" + var8.getSponsor().getEMail() + "\">");
				var4.println(var8.getSponsor().getFullName() + "</a>");
			} else {
				var4.println(WTMessage.getLocalizedMessage("wt.workflow.worklist.worklistResource", "204",
						(Object[]) null, var2));
			}

			var4.flush();
		}
	}

	public void PJLOrganizationHostLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "202", (Object[]) null))
				.getLocalizedMessage(var2)));
		var4.flush();
	}

	public void PJLOrganizationHost(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		WTContainerRef var5 = null;
		Object var6 = this.getContextObj();
		if (var6 instanceof WTContained) {
			var5 = ((WTContained) var6).getContainerReference();
		}

		WTContainer var7 = (WTContainer) var5.getObject();
		if (!(var7 instanceof Project2)) {
			throw new WTRuntimeException("Wrong context: expected ProjectLink got " + var7.getClass().getName());
		} else {
			Project2 var8 = (Project2) var7;
			OrgContainer var9 = WTContainerHelper.service.getOrgContainer(var8);
			var4.println(var9 != null ? var9.getName() : "&nbsp;");
			var4.flush();
		}
	}

	public void PJLProjectDescriptionLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "203", (Object[]) null))
				.getLocalizedMessage(var2)));
		var4.flush();
	}

	public void PJLProjectDescription(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		WTContainerRef var5 = null;
		Object var6 = this.getContextObj();
		if (var6 instanceof WTContained) {
			var5 = ((WTContained) var6).getContainerReference();
		}

		WTContainer var7 = (WTContainer) var5.getObject();
		if (!(var7 instanceof Project2)) {
			throw new WTRuntimeException("Wrong context: expected ProjectLink got " + var7.getClass().getName());
		} else {
			Project2 var8 = (Project2) var7;
			String var9 = var8.getDescription();
			if (var9 != null && var9.trim().length() > 0) {
				var4.println(this.translateToHtml(var9));
			} else {
				var4.println(WTMessage.getLocalizedMessage("wt.workflow.worklist.worklistResource", "204",
						(Object[]) null, var2));
			}

			var4.flush();
		}
	}

	public String getRequestedPromotionStateLabel(Locale var1) throws WTException {
		return this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "223", (Object[]) null))
				.getLocalizedMessage(var1));
	}

	private boolean check_for_PromotionRequestTask() {
		boolean var1 = false;
		String var2 = ((WfAssignedActivityTemplate) this.getActivity().getTemplateReference().getObject())
				.getTaskName();
		if (var2.equals("WfPromotionRequestTask") || var2.equals("WfTask")) {
			var1 = true;
		}

		return var1;
	}

	public String getPriorityLabel(Locale var1) throws WTException {
		return this.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "42", (Object[]) null))
				.getLocalizedMessage(var1));
	}

	public void priorityLabel(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.getPriorityLabel(var2));
		var4.flush();
	}

	public String getPriority(Locale var1) throws WTException {
		return var1.toString();
	}

	public void priority(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		var4.println(this.translateToHtml(this.getPriority(var2)));
		var4.flush();
	}

	public void activityNameFull(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		String var5 = "";

		try {
			WTObject var6 = (WTObject) this.getProcess().getContext().getValue("primaryBusinessObject");
			if (var6 != null) {
				var5 = var6.getDisplayIdentity().toString();
			}
		} catch (WTRuntimeException var7) {
		}

		var4.print(WTMessage.getLocalizedMessage("wt.workflow.worklist.worklistResource", "163",
				new Object[] { this.getActivity().getName(), var5 }, var2));
		var4.flush();
	}

	private void setPDMLinkContext(WTContainer var1) throws WTException {
		if (PDM_INSTALLED && (var1 instanceof WTLibrary || var1 instanceof PDMLinkProduct
				|| var1 instanceof OrgContainer || var1 instanceof ExchangeContainer)) {
			this.pdmlContext = true;
		}

	}

	private void setProjectLinkContext(WTContainer var1) throws WTException {
		if (var1 instanceof Project2) {
			this.pjlContext = true;
		}

	}

	protected void setContext() throws WTException {
		WTContainerRef var1 = null;
		Object var2 = this.getContextObj();
		if (var2 instanceof WTContained) {
			var1 = ((WTContained) var2).getContainerReference();
		}

		try {
			if (var1 == null) {
				logger.debug("In WfTaskProcessor.setContext for getExchangeRef");
				var1 = WTContainerHelper.service.getExchangeRef();
			}

			WTContainer var3 = (WTContainer) var1.getObject();
			this.setProjectLinkContext(var3);
			this.setPDMLinkContext(var3);
			this.contextSet = true;
		} catch (WTException var4) {
			logger.debug("Exception occurred while setting context: ", var4);
			this.contextSet = false;
		}

	}

	public void notificationActivityAttributes(Properties var1, Locale var2, OutputStream var3) throws Exception {
		logger.debug(" ********************** notificationActivityAttributes Started");
		WTReference var4 = this.getProcess().getBusinessObjectReference(new ReferenceFactory());
		PromotionNotice var5 = null;
		if (var4 != null && PromotionNotice.class.isAssignableFrom(var4.getReferencedClass())) {
			var5 = (PromotionNotice) var4.getObject();
		}

		PrintWriter var6 = this.getPrintWriter(var3, var2);
		var6.println("<table border=\"0\" cellpadding=\"3\" cellspacing=\"0\">");
		var6.println("<tr><td colspan=\"2\"><hr size=\"1\" color=\"#40637A\"></td></tr>");
		String var7 = WorkflowHtmlUtil.getExternalTaskURL(this.getWorkItem());
		if (var7 != null) {
			String var8 = this
					.formatLabel((new WTMessage("wt.workflow.worklist.worklistResource", "40", (Object[]) null))
							.getLocalizedMessage(var2));
			String var9 = "("
					+ (new WTMessage("wt.workflow.worklist.worklistResource", "NAVIGATE_LABEL", (Object[]) null))
							.getLocalizedMessage(var2)
					+ ")";
			String var10 = "("
					+ (new WTMessage("wt.workflow.worklist.worklistResource", "WINDCHILL_LABEL", (Object[]) null))
							.getLocalizedMessage(var2)
					+ ")";
			var6.println(
					"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
			var6.println(var8);
			var6.println("</b></font></td>");
			var6.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
			String var11 = HTMLEncoder.encodeForHTMLAttribute(var7);
			String var12 = "<A id=\"WfExternalTaskTProcessor02\" HREF=\"" + var11 + "\">"
					+ HTMLEncoder.encodeForHTMLContent(this.myActivity.getName()) + "</A>";
			var6.println(this.translateToHtml(var12 + var9));
			var6.println("</font></td></tr>");
			var11 = this.getTaskUrl(this.getContextRef());
			String var13 = "<A id=\"WfExternalTaskTProcessor03\" HREF=\"" + var11 + "\">"
					+ HTMLEncoder.encodeForHTMLContent(this.myActivity.getName()) + "</A>";
			var6.println(
					"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
			var6.println("");
			var6.println("</b></font></td>");
			var6.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
			var6.println(this.translateToHtml(var13 + var10));
			var6.println("</font></td></tr>");
		}

		var6.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var6.println(this.getActivityInstructionsLabel(var2));
		var6.println("</b></font></td>");
		var6.println("<td align=\"left\" colspan=\"4\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		WTPrincipal var20 = SessionContext.getEffectivePrincipal();

		try {
			SessionContext.setEffectivePrincipal(this.getWorkItem().getOwnership().getOwner().getPrincipal());
			var6.println(this.getActivityInstructionsAsHtml());
		} catch (Exception var17) {
			throw new WTException(var17);
		} finally {
			SessionContext.setEffectivePrincipal(var20);
		}

		var6.println("</font></td></tr>");
		if (var5 != null) {
			var6.println(
					"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
			var6.println(this.getRequestedPromotionStateLabel(var2));
			var6.println("</b></font></td>");
			var6.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
			var6.println(this
					.translateToHtml(var5.getMaturityState() != null ? var5.getMaturityState().getDisplay(var2) : ""));
			var6.println("</font></td></tr>");
		}

		var6.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var6.println(this.getProcessInitiatorLabel(var2));
		var6.println("</b></font></td>");
		var6.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var6.println(this.translateToHtml(HTMLEncoder.encodeAndFormatForHTMLContent(this.getProcessInitiator())));
		var6.println("</font></td></tr>");
		var6.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var6.println(this.getDueDateLabel(var2));
		var6.println("</b></font></td>");
		var6.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var6.println(this.getDueDate(var2));
		var6.println("</font></td></tr>");
		var6.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var6.println(this.getWorkItemRoleLabel(var2));
		var6.println("</b></font></td>");
		var6.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var6.println(this.getWorkItemRole(var2));
		var6.println("</font></td></tr>");
		var6.println("<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		var6.println(this.getAssigneeLabel(var2));
		var6.println("</b></font></td>");
		var6.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		var6.println(this.translateToHtml(HTMLEncoder.encodeAndFormatForHTMLContent(this.getAssignee())));
		var6.println("</font></td></tr>");
		var6.println("<tr><td colspan=\"2\"><hr size=\"1\" color=\"#40637A\"></td></tr>");
		Object var21 = this.getContextObj();
		this.setContextObj(this.getActivity());
		Properties var19 = new Properties();
		var19.put("service", "wt.workflow.work.WorkProcessorService");
		var19.put("method", "projectLinkAttributes");
		this.useProcessorService(var19, var2, var3);
		this.setContextObj(var21);
		var6.println("</table>");

		var6.flush();
	}



	public void activityNotificationUrl(Properties var1, Locale var2, OutputStream var3)
			throws WTException, WTPropertyVetoException {
		logger.debug(" ********************** activityNotificationUrl Started");
		String var4 = null;
		PrintWriter var5 = this.getPrintWriter(var3, var2);
		if (this.workItemExists()) {
			String var6 = "";
			WorkItem var7 = this.getWorkItem();
			ObjectReference var8 = var7.getOrigOwner();
			Object[] var10;
			if (var8 != null && var8.getObject() != null) {
				WTUser var9 = (WTUser) this.getWorkItem().getOrigOwner().getObject();
				var10 = new Object[] { this.getPrincipalEmailLink(var9, (String) null) };
				var6 = (new WTMessage("wt.workflow.worklist.worklistResource", "206", var10)).getLocalizedMessage(var2);
				String var11 = this.translateToHtml(var6);
				var6 = var11 + "<BR><BR>";
			}

			String var12 = WorkflowHtmlUtil.getExternalTaskURL(var7);
			var6 = var6 + this.translateToHtml(var4);
			if (var12 != null) {
				var4 = (new WTMessage("wt.workflow.worklist.worklistResource", "NOTIFICATION_EXTERNAL_TASK_LABEL",
						(Object[]) null)).getLocalizedMessage(var2);
				var5.println(var6 + var4);
				var5.println("<br>");
			} else {
				var10 = new Object[] { this.getTaskUrl(this.getContextRef()),
						HTMLEncoder.encodeAndFormatForHTMLContent(this.getActivity().getName()) };
				var4 = (new WTMessage("wt.workflow.worklist.worklistResource", "205", var10)).getLocalizedMessage(var2);
				var5.println(var6 + this.translateToHtml(var4));
			}
		} else {
			var4 = (new WTMessage("wt.workflow.worklist.worklistResource", "230", new Object[0]))
					.getLocalizedMessage(var2);
			var5.println(this.translateToHtml(var4));
		}

		var5.flush();
	}

	public void processReassignmentHistory(Properties var1, Locale var2, OutputStream var3) throws WTException {
		PrintWriter var4 = this.getPrintWriter(var3, var2);
		WfProcess var5 = this.getProcess();
		NetmarketsHref var6 = new NetmarketsHref(NetmarketsType.workflow, NetmarketsCommand.reassignmentHistory,
				(new ReferenceFactory()).getReference(var5.getPersistInfo().getObjectIdentifier().getStringValue()));
		String var7 = var6.getHref();
		String var8 = "";
		String var9 = "";
		var8 = (new WTMessage("wt.workflow.worklist.worklistResource", "166", (Object[]) null))
				.getLocalizedMessage(var2);
		var9 = "<A HREF=\"javascript:var historyWindow=wfWindowOpen('" + var7
				+ "','nmReassignmentHistoryview',config='resizable=yes,scrollbars=yes,menubar=yes,toolbar=yes,location=yes,status=yes')\" class=\"detailsLink\">"
				+ var8 + "</A>";
		var4.println(var9);
		var4.flush();
	}

	public String escapeHtml(String var1) {
		return HTMLEncoder.encodeAndFormatForHTMLContent(var1);
	}

	protected WTContainerRef getContextRef() throws WTException {
		WfActivity var1 = this.getActivity();
		WTContainerRef var2 = null;
		if (var1 instanceof WTContained) {
			var2 = var1.getContainerReference();
		}

		if (var2 == null) {
			logger.debug("In WfTaskProcessor.getContext for getExchangeRef");
			var2 = WTContainerHelper.service.getExchangeRef();
		}

		return var2;
	}

	private String getPrincipalFName(WTPrincipal var1) {
		String var2 = "";
		if (var1 != null) {
			try {
				ReferenceFactory var3 = new ReferenceFactory();
				WTReference var7 = var3.getReference(var1);
				var2 = ((WTPrincipalReference) var7).getDisplayName();
			} catch (WTRuntimeException var5) {
				Throwable var4 = var5.getNestedThrowable();
				if (!(var4 instanceof NotAuthorizedException)) {
					throw var5;
				}

				var2 = "";
			} catch (WTException var6) {
				var2 = "";
			}
		}

		return var2;
	}

	protected String getTaskUrl(WTContainerRef var1) throws WTException {
		NetmarketsHref var2 = new NetmarketsHref((new ReferenceFactory())
				.getReference(this.getWorkItem().getPersistInfo().getObjectIdentifier().getStringValue()));
		var2.setFullyQualified(true);
		return var2.getHref();
	}

	static {
		logger = LogR.getLoggerInternal(CLASSNAME);
		ADHOC_ACTIVITY_GIF = "wt/clients/images/adhocactivity.gif";
		FORUM_GIF = "netmarkets/images/forum.gif";
		JIT_PROJECTS_GIF = "wt/clients/images/definejitproject.gif";
		JIT_TEAMS_GIF = "wt/clients/images/definejitproject.gif";
		NOTEBOOK_GIF = "netmarkets/images/notebook.gif";
		ROLES_UPDATE_GIF = "wt/clients/images/augmentroles.gif";
		UPDATE_CONTENT_GIF = "wt/clients/images/contentholder.gif";
		FORMATTED = "formatted";
		DEFAULT_FORMATTED_VALUE = "true";
		COLUMNS = "columns";
		DEFAULT_COLUMN_NUMBER = "60";
		DEFAULT_ROW_NUMBER = "7";
		ROWS = "rows";
		PDM_INSTALLED = false;
		QMS_INSTALLED = false;

		try {
			WTProperties var0 = WTProperties.getLocalProperties();
			VERBOSE = var0.getProperty("wt.workflow.verbose", false);
			PLMLINK = var0.getProperty("com.ptc.netmarkets.showPLMLink", false);
			CODEBASE = var0.getProperty("wt.server.codebase", "");
			ESCAPE_HTML = var0.getProperty("wt.workflow.worklist.escapeHtml", "scriptonly");
			PDM_INSTALLED = InstalledProperties.isInstalled("pdmSystem");
			QMS_INSTALLED = InstalledProperties.isInstalled("Windchill.QualityManagement.QMS");
			rb = ResourceBundle.getBundle("wt.workflow.worklist.worklistResource", WTContext.getContext().getLocale());
			if (VERBOSE && !logger.isDebugEnabled()) {
				Configurator.setLevel(logger.getName(), Level.DEBUG);
			}

		} catch (Throwable var1) {
			throw new ExceptionInInitializerError(var1);
		}
	}
	
// Emerson Activity Email Notification Customization Starts Here
	public void emersonAffectedObjects(Properties var1, Locale var2, OutputStream var3) throws Exception {
		logger.debug(" ********************** emersonAffectedObjects Started");
		WTReference var4 = this.getProcess().getBusinessObjectReference(new ReferenceFactory());
		PromotionNotice var5 = null;
		Boolean flag = false; 
		if (var4 != null && PromotionNotice.class.isAssignableFrom(var4.getReferencedClass())) {
			var5 = (PromotionNotice) var4.getObject();
			flag = true; 
		}
		PrintWriter var6 = this.getPrintWriter(var3, var2);
		if (flag) {
			
			// Add the CSS styles
			var6.println("<style>");
			var6.println(".table-container {");
			var6.println("    height: 200px;");
			var6.println("    overflow-y: auto;");
			var6.println("    overflow-x: auto;");
			var6.println("    width: 100%;");
			var6.println("}");
			var6.println(".table123 {");
			var6.println("    width: 100%;");
			var6.println("    border-collapse: collapse;");
			var6.println("    table-layout: fixed;");
			var6.println("}");
			var6.println(".table123 th, .table123 td {");
			var6.println("    border: 2px solid #000;");
			var6.println("    padding: 4px;");
			var6.println("    text-align: center;");
			var6.println("    font-size: 12px;");
			var6.println("}");
			var6.println(".table123 thead {");
			var6.println("    background-color: #f2f2f2;");
			var6.println("    position: sticky;");
			var6.println("    top: 0;");
			var6.println("    z-index: 2;");
			var6.println("}");
			var6.println(".headline {");
			var6.println("    background-color: solid #000;");
			var6.println("    font-weight: bold;");
			var6.println("    text-align: center;");
			var6.println("    padding: 10px;");
			var6.println("    border: 2px solid black;");
			var6.println("}");
			var6.println("</style>");

			var6.println("</table>");


			// Add the new table here
			var6.println("<div class=\"headline\">Affected Objects Table</div>");
			var6.println("<div class=\"table-container\">");
			var6.println("<table class=\"table123\">");
			var6.println("<thead>");
			var6.println("<tr>");
			var6.println("<th style='width: 10%;'>Type</th>");										 
			var6.println("<th style='width: 20%;'>Number</th>");
			var6.println("<th style='width: 20%;'>Name</th>");
			var6.println("<th style='width: 10%;'>Promoted From State</th>");
			var6.println("<th style='width: 10%;'>Version</th>");
			var6.println("<th style='width: 20%;'>Part Name</th>");
			var6.println("<th style='width: 20%;'>Description1</th>");
			var6.println("</tr>");
			var6.println("</thead>");
			var6.println("<tbody>");

			QueryResult qs = MaturityHelper.service.getPromotionTargets(var5);
			Map<WTObject, String> promotedFromStates = getPromotedFromStates(var5);
			if (qs.size() > 0) {
				while (qs.hasMoreElements()) {
					Object obj = qs.nextElement();
					String state = promotedFromStates.get(obj);
					String href;
					String subType;
					if (obj instanceof WTPart) {
						WTPart part = (WTPart) obj;
						subType = part.getDisplayType().getLocalizedMessage(null);																		
						href = getURLOfObject(obj);
						href = "<a href='" + href + "'>" + part.getNumber() + "</a>";
						var6.println("<tr>");
						var6.println("<td align='center'>" + subType + "</td>");									  
						var6.println("<td align='center'>" + href + "</td>");
						var6.println("<td align='center'>" + part.getName() + "</td>");
						var6.println("<td align='center'>" + state + "</td>");
						var6.println("<td align='center'>" + part.getIterationDisplayIdentifier() + "</td>");
						var6.println("<td align='center'>" + getIBAValue(part,"PART_NAME") + "</td>");
						var6.println("<td align='center'>" + getIBAValue(part,"DESCRIPTION_1") + "</td>");
						var6.println("</tr>");
					} else if (obj instanceof WTDocument) {
						WTDocument doc = (WTDocument) obj;
						subType = doc.getDisplayType().getLocalizedMessage(null);																	  
						href = getURLOfObject(obj);
						href = "<a href='" + href + "'>" + doc.getNumber() + "</a>";
						var6.println("<tr>");
						var6.println("<td align='center'>" + subType + "</td>");									  
						var6.println("<td align='center'>" + href + "</td>");
						var6.println("<td align='center'>" + doc.getName() + "</td>");
						var6.println("<td align='center'>" + state + "</td>");
						var6.println("<td align='center'>" + doc.getIterationDisplayIdentifier() + "</td>");
						var6.println("<td align='center'>" + getIBAValue(doc,"PART_NAME") + "</td>");
						var6.println("<td align='center'>" + getIBAValue(doc,"DESCRIPTION_1") + "</td>");
						var6.println("</tr>");
					} else if (obj instanceof EPMDocument) {
						EPMDocument epm = (EPMDocument) obj;
						subType = epm.getDisplayType().getLocalizedMessage(null);																	  
						href = getURLOfObject(obj);
						href = "<a href='" + href + "'>" + epm.getNumber() + "</a>";
						var6.println("<tr>");
						var6.println("<td align='center'>" + subType+ "</td>");														 
						var6.println("<td align='center'>" + href + "</td>");
						var6.println("<td align='center'>" + epm.getName() + "</td>");
						var6.println("<td align='center'>" + state + "</td>");
						var6.println("<td align='center'>" + epm.getIterationDisplayIdentifier() + "</td>");
						var6.println("<td align='center'>" + getIBAValue(epm,"PART_NAME") + "</td>");
						var6.println("<td align='center'>" + getIBAValue(epm,"DESCRIPTION_1") + "</td>");
						var6.println("</tr>");
					}
				}
			} else {
				throw new NullPointerException(
						"Unable to find Target objects for Promotion Notice : " + var5.getNumber());
			}
			var6.println("</tbody>");
			var6.println("</table>");
			var6.println("</div>"); // Close the scrollable container
			var6.println("<div style='height: 20px;'></div>");
			var6.println("<hr style='border: 1px solid #000;'>");
			// Add blank space below the scrollable container
			var6.println("<div style='height: 50px;'></div>");
			
		}
		
		// Emerson Activity Email Notification Customization ENDS Here

		var6.flush();
	}

	
	public static String getURLOfObject(Object obj) throws Exception {
		NmOid tgtOid = new NmOid(wt.fc.PersistenceHelper.getObjectIdentifier((Persistable) obj));
		NmAction infoPageAction = NmActionServiceHelper.service
				.getAction(com.ptc.netmarkets.util.misc.NmAction.Type.OBJECT, "view");
		infoPageAction.setContextObject(tgtOid);
		infoPageAction.setIcon(null);
		String href = infoPageAction.getActionUrlExternal();
		return href;
	}
	
	public static String getIBAValue(WTObject obj, String iba) throws Exception {
		String emptyValue = "-";				  
		PersistableAdapter pa = new PersistableAdapter(obj, null, SessionHelper.getLocale(), null);
		pa.load(iba);
		String ibaValue = (String) pa.get(iba);
		if (ibaValue != null) {
			logger.debug("Attribute Value  :" + ibaValue);
		return ibaValue;
		}
		return emptyValue;	
	}
	
	public static Map<WTObject, String> getPromotedFromStates(PromotionNotice pn) throws MaturityException, WTException {
	    Map<WTObject, String> promotedFromStates = new HashMap<>();

	    QueryResult promotionObjectLinks = MaturityHelper.service.getPromotionTargets(pn, false);
	    while (promotionObjectLinks.hasMoreElements()) {
	        PromotionTarget pt = (PromotionTarget) promotionObjectLinks.nextElement();
	        WTObject targetObject = (WTObject) pt.getRoleBObject();
	        String state = pt.getCreateState().getDisplay().toString();
	        promotedFromStates.put(targetObject, state);
	        logger.debug("Object: " + targetObject.getPersistInfo() + ", Promoted from state: " + state);
	    }
	    return promotedFromStates;
	}

}