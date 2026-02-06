package wt.workflow.worklist;

import java.io.CharArrayWriter;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
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
import wt.fc.CachedObjectReference;
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
import wt.org.DirectoryContextProvider;
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
	private static final Logger logger = LogR.getLoggerInternal(CLASSNAME);
	static final long serialVersionUID = -2054561049350297424L;
	private Boolean adHocActivitiesDisplayed = null;
	private Boolean adHocActivitiesInProgress = null;
	private ObjectReference workItemRef = null;
	private String messageText = "";
	private String pAction = "";
	private WfActivity myActivity = null;
	private WfProcess myProcess = null;
	private WorkItem workItem = null;
	private static String ADHOC_ACTIVITY_GIF = "wt/clients/images/adhocactivity.gif";
	private static String FORUM_GIF = "netmarkets/images/forum.gif";
	private static String JIT_PROJECTS_GIF = "wt/clients/images/definejitproject.gif";
	private static String JIT_TEAMS_GIF = "wt/clients/images/definejitproject.gif";
	private static String NOTEBOOK_GIF = "netmarkets/images/notebook.gif";
	private static String ROLES_UPDATE_GIF = "wt/clients/images/augmentroles.gif";
	private static String UPDATE_CONTENT_GIF = "wt/clients/images/contentholder.gif";
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
	public void writeExternal(ObjectOutput objectOutput) throws IOException {
		objectOutput.writeLong(-2054561049350297424L);
		ReferenceFactory referenceFactory = new ReferenceFactory();
		try {
			objectOutput.writeUTF(referenceFactory.getReferenceString(this.workItemRef));
		} catch (WTException wTException) {
			throw new WTRuntimeException(wTException);
		}
		objectOutput.writeObject(this.adHocActivitiesDisplayed);
		objectOutput.writeObject(this.adHocActivitiesInProgress);
		objectOutput.writeUTF(this.pAction);
		objectOutput.writeUTF(this.messageText);
	}

	@Override
	public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
		long l = objectInput.readLong();
		this.readVersion(this, objectInput, l, false, false);
	}

	protected boolean readVersion(WfTaskProcessor wfTaskProcessor, ObjectInput objectInput, long l, boolean bl,
			boolean bl2) throws IOException, ClassNotFoundException {
		if (l != -2054561049350297424L) {
			return this.readOldVersion(objectInput, l, bl, bl2);
		}
		ReferenceFactory referenceFactory = new ReferenceFactory();
		String string = objectInput.readUTF();
		try {
			this.workItemRef = (ObjectReference) referenceFactory.getReference(string);
		} catch (WTException wTException) {
			throw new WTIOException(wTException.getLocalizedMessage());
		}
		this.adHocActivitiesDisplayed = (Boolean) objectInput.readObject();
		this.adHocActivitiesInProgress = (Boolean) objectInput.readObject();
		this.pAction = objectInput.readUTF();
		this.messageText = objectInput.readUTF();
		return true;
	}

	private boolean readOldVersion(ObjectInput objectInput, long l, boolean bl, boolean bl2)
			throws IOException, ClassNotFoundException {
		boolean bl3 = true;
		throw new InvalidClassException(CLASSNAME,
				"Local class not compatible: stream classdesc externalizationVersionUID = " + l
						+ " local class externalizationVersionUID = -2054561049350297424");
	}

	public WorkItem getWorkItem() {
		try {
			if (this.workItem == null && this.workItemRef != null) {
				this.workItem = (WorkItem) this.workItemRef.getObject();
			}
			this.workItem = (WorkItem) PersistenceHelper.manager.refresh(this.workItem);
			return this.workItem;
		} catch (WTException wTException) {
			throw new WTRuntimeException(wTException);
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
				} catch (WTRuntimeException wTRuntimeException) {
					return false;
				}
			}
			if (this.workItem != null) {
				this.workItem = (WorkItem) PersistenceHelper.manager.refresh(this.workItem);
				return true;
			}
			return false;
		} catch (WTException wTException) {
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
		} catch (WTException wTException) {
			throw new WTRuntimeException(wTException);
		}
	}

	public WfTaskProcessor() {
	}

	public WfTaskProcessor(Object object) {
		this.setContextObj(object);
	}

	public void setWorkItem(WorkItem workItem) {
		this.workItem = workItem;
		boolean bl = false;
		try {
			this.workItemRef = ObjectReference.newObjectReference(workItem);
		} catch (WTException wTException) {
			throw new WTRuntimeException(wTException);
		}
		try {
			this.myActivity = (WfActivity) workItem.getSource().getObject();
		} catch (WTRuntimeException wTRuntimeException) {
			Throwable throwable = wTRuntimeException.getNestedThrowable();
			if (throwable instanceof NotAuthorizedException) {
				logger.debug("WfTaskProcessor: User is an external participant, bypassing access control check");
				this.isExternalParticipant = true;
				MethodContext.getContext().put("externalParticipant", (Object) this.isExternalParticipant);
				try {
					bl = SessionServerHelper.manager.setAccessEnforced(false);
					this.myActivity = (WfActivity) workItem.getSource().getObject();
				} finally {
					SessionServerHelper.manager.setAccessEnforced(bl);
				}
			}
			throw wTRuntimeException;
		}
		try {
			this.myProcess = this.myActivity.getParentProcess();
		} catch (WTException wTException) {
			throw new WTRuntimeException(wTException);
		}
		this.setContextObj(this.myActivity);
	}

	@Override
	public void readContext(HTTPRequest hTTPRequest) throws WTException {
		block9: {
			this.getState().getResponseObj().setHeader("cache-control", "no-cache");
			try {
				this.setWorkItem((WorkItem) this.getContextObj());
				if (this.getWorkItem().isComplete()) {
					this.addToResponseHeaders(new WTMessage(RESOURCE, "17", null));
					this.setContextAction("WfMessage");
					break block9;
				}
				if (this.isWorkItemOwner()) {
					boolean bl = false;
					try {
						bl = SessionServerHelper.manager.setAccessEnforced(false);
						this.myActivity = (WfActivity) this.getWorkItem().getSource().getObject();
						this.setContextObj(this.myActivity);
						if (this.myActivity.getState().equals(WfState.OPEN_NOT_RUNNING_SUSPENDED_INTERMITTED)) {
							this.addToResponseHeaders(new WTMessage(RESOURCE, "81", null));
							this.setContextAction("WfMessage");
						} else {
							this.myProcess = this.myActivity.getParentProcess();
						}
						this.setContext();
						break block9;
					} finally {
						bl = SessionServerHelper.manager.setAccessEnforced(bl);
					}
				}
				this.addToResponseHeaders(new WTMessage(RESOURCE, "79", null));
				this.setContextAction("WfMessage");
			} catch (Exception exception) {
				logger.error("", (Throwable) exception);
				this.addToResponseHeaders(new WTMessage(RESOURCE, "17", null));
				this.setContextAction("WfMessage");
			}
		}
	}

	public void activityName(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.print(this.getActivity().getName());
		printWriter.flush();
	}

	public void activityAttributes(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException, WTPropertyVetoException {
		if (this.workItemExists()) {
			if (!this.contextSet) {
				this.setContext();
			}
			if (this.pjlContext) {
				this.projectLinkActivityAttributes(properties, locale, outputStream);
				return;
			}
			if (this.pdmlContext || QMS_INSTALLED) {
				this.plmLinkActivityAttributes(properties, locale, outputStream);
				return;
			}
			PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
			printWriter.println("<TABLE BORDER=0 WIDTH=\"100%\" ALIGN=CENTER CELLPADDING=3 >");
			printWriter.println("<TR>" + this.tableCell(
					"<B>" + this.getActivityInstructionsLabel(locale) + "</B>" + this.getActivityInstructionsAsHtml(),
					5));
			printWriter.println(this.tableCellBold(this.getProcessNameLabel(locale), 1));
			printWriter.println(this.tableCell(this.translateToHtml(this.getProcessName())));
			printWriter.println("<TR>" + this.tableCellBold(this.getProcessInitiatorLabel(locale), 1));
			printWriter.println(this.tableCell(this.translateToHtml(this.getProcessInitiator())));
			printWriter.println("<TR>" + this.tableCellBold(this.getDueDateLabel(locale), 1));
			printWriter.println(this.tableCell(this.translateToHtml(this.getDueDate(locale))));
			printWriter.println("<TR>" + this.tableCellBold(this.getWorkItemRoleLabel(locale), 1));
			printWriter.println(this.tableCell(this.translateToHtml(this.getWorkItemRole(locale))));
			printWriter.println("<TR>" + this.tableCellBold(this.getAssigneeLabel(locale), 1));
			printWriter
					.println(this.tableCell(this.translateToHtml(this.getAssignee() + this.getOriginalOwnerLocal())));
			printWriter.println("</TABLE>");
			if (!this.contextSet) {
				printWriter.println("<font color='red'><B>Warning: Context not known.</B></font>");
			}
			printWriter.flush();
		}
	}

	public void projectLinkActivityAttributes(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException, WTPropertyVetoException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println("<table border=\"0\" cellpadding=\"3\" cellspacing=\"0\">");
		printWriter.println("<tr><td colspan=\"2\"><hr size=\"1\" color=\"#40637A\"></td></tr>");
		String string = WorkflowHtmlUtil.getExternalTaskURL(this.getWorkItem());
		if (string != null) {
			String string2 = this.formatLabel(new WTMessage(RESOURCE, "40", null).getLocalizedMessage(locale));
			String string3 = "(" + new WTMessage(RESOURCE, "NAVIGATE_LABEL", null).getLocalizedMessage(locale) + ")";
			String string4 = "(" + new WTMessage(RESOURCE, "WINDCHILL_LABEL", null).getLocalizedMessage(locale) + ")";
			printWriter.println(
					"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
			printWriter.println(string2);
			printWriter.println("</b></font></td>");
			printWriter.println(
					"<td align=\"left\" colspan=\"4\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
			String string5 = "<A id=\"WfExternalTaskTProcessor02\" HREF=\""
					+ HTMLEncoder.encodeForHTMLAttribute((String) string) + "\">"
					+ HTMLEncoder.encodeForHTMLContent((String) this.myActivity.getName()) + "</A>";
			printWriter.println(this.translateToHtml(string5 + string3));
			printWriter.println("</font></td></tr>");
			String string6 = "<A id=\"WfExternalTaskTProcessor03\" HREF=\"" + this.getTaskUrl(this.getContextRef())
					+ "\">" + HTMLEncoder.encodeForHTMLContent((String) this.myActivity.getName()) + "</A>";
			printWriter.println(
					"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
			printWriter.println("");
			printWriter.println("</b></font></td>");
			printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
			printWriter.println(this.translateToHtml(string6 + string4));
			printWriter.println("</font></td></tr>");
		}
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(this.getActivityInstructionsLabel(locale));
		printWriter.println("</b></font></td>");
		printWriter.println(
				"<td align=\"left\" colspan=\"4\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		printWriter.println(this.getActivityInstructionsAsHtml());
		printWriter.println("</font></td></tr>");
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(this.getProcessInitiatorLabel(locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		printWriter.println(this.translateToHtml(this.getProcessInitiator()));
		printWriter.println("</font></td></tr>");
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(this.getDueDateLabel(locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		printWriter.println(this.getProjectLinkDueDate(locale));
		printWriter.println("</font></td></tr>");
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(this.getWorkItemRoleLabel(locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		printWriter.println(this.getWorkItemRole(locale));
		printWriter.println("</font></td></tr>");
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(this.getAssigneeLabel(locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		printWriter.println(this.translateToHtml(this.getAssignee() + this.getOriginalOwnerLocal()));
		printWriter.println("</font></td></tr>");
		printWriter.println("<tr><td colspan=\"2\"><hr size=\"1\" color=\"#40637A\"></td></tr>");
		this.projectLinkAttributes(properties, locale, outputStream);
		printWriter.println("</table>");
		printWriter.flush();
	}

	public String getProjectLinkDueDate(Locale locale) throws WTException {
		try {
			WfActivity wfActivity = this.getActivity();
			Timestamp timestamp = wfActivity.getDeadline();
			if (timestamp == null) {
				return "&nbsp;";
			}
			WTPrincipal wTPrincipal = OwnershipHelper.getOwner(this.getWorkItem());
			UserFacade userFacade = UserHelper.getFacade();
			TimeZone timeZone = userFacade.getLocalTimeZoneForUser(wTPrincipal);
			ResourceBundle resourceBundle = ResourceBundle.getBundle("wt.util.utilResource", locale);
			String string = resourceBundle.getString("22");
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(string, locale);
			simpleDateFormat.setTimeZone(timeZone);
			return simpleDateFormat.format(timestamp);
		} catch (Exception exception) {
			logger.error("", (Throwable) exception);
			if (logger.isDebugEnabled()) {
				exception.printStackTrace();
			}
			return "&nbsp;";
		}
	}

	public void plmLinkActivityAttributes(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException, WTPropertyVetoException {
		String string = null;
		if (properties != null) {
			string = properties.getProperty("notification");
		}
		boolean bl = false;
		if (string != null) {
			bl = Boolean.parseBoolean(string);
		}
		if (bl) {
			this.notificationActivityAttributes(properties, locale, outputStream);
		} else {
			PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
			printWriter.println("<table border=\"0\" cellpadding=\"3\" cellspacing=\"0\">");
			printWriter.println("<tr><td align=\"right\" valign=\"top\" nowrap >");
			printWriter.println("&nbsp;");
			printWriter.println("</td><tr>");
			printWriter.println("<tr><td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			printWriter.println(this.getActivityInstructionsLabel(locale));
			printWriter.println("</td>");
			printWriter.println("<td colspan=\"4\" align=\"left\" valign=\"top\" class=\"propValue\">");
			printWriter.println(this.getActivityInstructionsAsHtml());
			printWriter.println("</td><tr>");
			printWriter.println("<tr><td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			printWriter.println(this.getProcessInitiatorLabel(locale));
			printWriter.println("</td>");
			printWriter.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			printWriter.println(this.translateToHtml(this.getProcessInitiator()));
			printWriter.println("</td>");
			printWriter.println("<td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			printWriter.println("&nbsp;&nbsp;&nbsp;" + this.getPriorityLabel(locale));
			printWriter.println("</td>");
			printWriter.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			printWriter.println(this.translateToHtml(this.getPriority(locale)));
			printWriter.println("</td></tr>");
			printWriter.println("<tr><td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			printWriter.println(this.getAssigneeLabel(locale));
			printWriter.println("</td>");
			printWriter.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			printWriter.println(this.translateToHtml(this.getAssignee() + this.getOriginalOwnerLocal()));
			printWriter.println("</td>");
			printWriter.println("<td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			printWriter.println("&nbsp;&nbsp;&nbsp;" + this.getDueDateLabel(locale));
			printWriter.println("</td>");
			printWriter.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			String string2 = this.getDueDate(locale);
			if (string2 == null) {
				string2 = " ";
			}
			printWriter.println(string2);
			printWriter.println("</td></tr>");
			printWriter.println("<tr><td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			printWriter.println(this.getWorkItemRoleLabel(locale));
			printWriter.println("</td>");
			printWriter.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			printWriter.println(this.getWorkItemRole(locale));
			printWriter.println("</td>");
			printWriter.println("<td align=\"right\" valign=\"top\" nowrap class=\"propTitle\">");
			printWriter.println("&nbsp;&nbsp;&nbsp;" + this.getProcessNameLabel(locale));
			printWriter.println("</td>");
			printWriter.println("<td align=\"left\" valign=\"top\" class=\"propValue\">");
			printWriter.println(this.translateToHtml(this.getProcessName()));
			printWriter.println("</td></tr>");
			printWriter.println("</table>");
			printWriter.flush();
		}
	}

	public void activityAttributesPlain(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException, WTPropertyVetoException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getActivity().getName());
		String string = this.getActivityInstructions();
		if (string == null) {
			string = " ";
		}
		printWriter.println(this.trimHtml(this.getActivityInstructionsLabel(locale)) + string);
		printWriter.println(this.trimHtml(this.getProcessNameLabel(locale)) + this.getProcess().getName() + " ("
				+ this.stripHtmlFormat(this.getProcessName()) + ")");
		printWriter.println(this.trimHtml(this.getProcessInitiatorLabel(locale))
				+ this.stripHtmlFormat(this.getProcessInitiator()));
		String string2 = this.getDueDate(locale);
		if (string2 == null) {
			string2 = " ";
		}
		printWriter.println(this.trimHtml(this.getDueDateLabel(locale)) + string2);
		printWriter.println(this.trimHtml(this.getWorkItemRoleLabel(locale)) + this.getWorkItemRole(locale));
		printWriter.println(this.trimHtml(this.getAssigneeLabel(locale)) + this.stripHtmlFormat(this.getAssignee()));
		printWriter.flush();
	}

	public void activityAttributesWithGroup(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException, WTPropertyVetoException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		if (this.pdmlContext) {
			printWriter.println("<TABLE BORDER=0 WIDTH=\"100%\" ALIGN=CENTER CELLPADDING=3  class=\"tableHdr\">");
		} else {
			printWriter.println("<TABLE BORDER=0 WIDTH=\"100%\" ALIGN=CENTER CELLPADDING=3 bgcolor=\""
					+ WfTaskProcessor.getWCColor("t1-bg-col-head") + "\">");
		}
		printWriter.println("<TR>" + this.tableCell(
				"<B>" + this.getActivityInstructionsLabel(locale) + "</B>" + this.getActivityInstructionsAsHtml(), 6));
		printWriter.println(this.tableCellBold(this.getProcessNameLabel(locale), 1));
		printWriter.println(this.tableCell(this.translateToHtml(this.getProcessName())));
		printWriter.println("<TR>" + this.tableCellBold(this.getProcessInitiatorLabel(locale), 1));
		printWriter.println(this.tableCell(this.translateToHtml(this.getProcessInitiator())));
		printWriter.println("<TR>" + this.tableCellBold(this.getDueDateLabel(locale), 1));
		printWriter.println(this.tableCell(this.translateToHtml(this.getDueDate(locale))));
		printWriter.println("<TR>" + this.tableCellBold(this.getWorkItemRoleLabel(locale), 1));
		printWriter.println(this.tableCell(this.translateToHtml(this.getWorkItemRole(locale))));
		printWriter.println("<TR>" + this.tableCellBold(this.getAssigneeLabel(locale), 1));
		printWriter.println(this.tableCell(this.translateToHtml(this.getAssignee())));
		printWriter.println("<TR>" + this.tableCellBold(this.getGroupLabel(locale), 1));
		printWriter.println(this.tableCell(this.translateToHtml(this.getGroup())));
		printWriter.println("</TABLE>");
		printWriter.flush();
	}

	private String stripHtmlFormat(String string) {
		int n;
		if (logger.isDebugEnabled()) {
			logger.debug("=> WfTaskProcessor.stripHtmlFormat - IN: " + string);
		}
		if ((n = string.indexOf(34)) == -1) {
			return string;
		}
		int n2 = string.indexOf(34, n + 1);
		if (n2 == -1) {
			return string;
		}
		String string2 = string.substring(n + 1, n2);
		if (logger.isDebugEnabled()) {
			logger.debug("   stripHtmlFormat - OUT: " + string2);
		}
		return string2;
	}

	private String trimHtml(String string) {
		int n = string.indexOf(38);
		if (n == -1) {
			return string;
		}
		return INDENT + string.substring(0, n) + " ";
	}

	public String getActivityInstructionsLabel(Locale locale) throws WTException {
		return this.formatLabel(new WTMessage(RESOURCE, "2", null).getLocalizedMessage(locale));
	}

	public void activityInstructionsLabel(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getActivityInstructionsLabel(locale));
		printWriter.flush();
	}

	public String getActivityInstructions() throws WTException, WTPropertyVetoException {
		WfAssignedActivity wfAssignedActivity = (WfAssignedActivity) this.getActivity();
		String string = wfAssignedActivity.getInstructions();
		if ((string == null || string.equals(""))
				&& (string = (String) wfAssignedActivity.getContext().getValue("instructions")) != null) {
			Transaction transaction = new Transaction();
			try {
				transaction.start();
				wfAssignedActivity.setInstructions(string);
				PersistenceHelper.manager.save(wfAssignedActivity);
				transaction.commit();
				transaction = null;
			} finally {
				if (transaction != null) {
					transaction.rollback();
					logger.debug("Error Lazy migration: set instructions for WfAssignedActivity.");
				}
			}
		}
		return string;
	}

	public String getActivityInstructionsAsHtml() throws WTException, WTPropertyVetoException {
		String string = WorkflowHtmlUtil.getTranslatedInstruction(this.getActivityInstructions());
		return string;
	}

	public void activityInstructions(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException, WTPropertyVetoException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getActivityInstructionsAsHtml());
		printWriter.flush();
	}

	public String getProcessNameLabel(Locale locale) throws WTException {
		return this.formatLabel(new WTMessage(RESOURCE, "10", null).getLocalizedMessage(locale));
	}

	public void processNameLabel(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getProcess().getName());
		printWriter.flush();
	}

	public String getProcessName() throws WTException {
		if (MethodContext.getContext().get("externalParticipant") != null
				|| !this.isUserMemberOfGroup("Administrators") && !this.isUserOrgAdmin(this.getProcess())) {
			return WTMessage.getLocalizedMessage(ACCESS_RESOURCE, "18", null, SessionHelper.getLocale());
		}
		NetmarketsHref netmarketsHref = new NetmarketsHref(NetmarketsType.workflow,
				NetmarketsCommand.openProcessManager, this.getProcess());
		String string = netmarketsHref.getHrefNew();
		String string2 = HtmlUtil.createLink(string, null, this.getProcess().getName());
		return string2;
	}

	public boolean isUserMemberOfGroup(String string) throws WTException {
		boolean bl = false;
		WTPrincipal wTPrincipal = SessionHelper.manager.getPrincipal();
		WTGroup wTGroup = OrganizationServicesHelper.manager.getGroup(string);
		if (wTGroup != null) {
			bl = wTGroup.isMember(wTPrincipal);
		}
		return bl;
	}

	public boolean isUserOrgAdmin(WfProcess wfProcess) throws WTException {
		boolean bl = false;
		Object var3_3 = null;
		OrgContainer orgContainer = null;
		orgContainer = WTContainerHelper.service.getOrgContainer(wfProcess);
		if (orgContainer == null) {
			return false;
		}
		bl = WTContainerServerHelper.getAdministratorsReadOnly(orgContainer)
				.isMember(SessionHelper.manager.getPrincipal());
		return bl;
	}

	public boolean isExternalParticipant() {
		return MethodContext.getContext().get("externalParticipant") != null;
	}

	public static boolean isExternalParticipant(WorkItem workItem) {
		ObjectReference objectReference = null;
		try {
			objectReference = ObjectReference.newObjectReference(workItem);
		} catch (WTException wTException) {
			throw new WTRuntimeException(wTException);
		}
		try {
			workItem.getSource().getObject();
		} catch (WTRuntimeException wTRuntimeException) {
			Throwable throwable = wTRuntimeException.getNestedThrowable();
			if (throwable instanceof NotAuthorizedException) {
				logger.debug("WfTaskProcessor: User is an external participant, bypassing access control check");
				return true;
			}
			throw wTRuntimeException;
		}
		return false;
	}

	public WTObject getPBO() {
		WTObject wTObject = null;
		boolean bl = false;
		try {
			bl = SessionServerHelper.manager.setAccessEnforced(false);
			wTObject = (WTObject) this.workItem.getPrimaryBusinessObject().getObject();
		} catch (NullPointerException nullPointerException) {
			wTObject = null;
		} finally {
			SessionServerHelper.manager.setAccessEnforced(bl);
		}
		return wTObject;
	}

	public void processName(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.translateToHtml(this.getProcessName()));
		printWriter.flush();
	}

	public String getProcessInitiatorLabel(Locale locale) throws WTException {
		return this.formatLabel(new WTMessage(RESOURCE, "12", null).getLocalizedMessage(locale));
	}

	public void processInitiatorLabel(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getProcessInitiatorLabel(locale));
		printWriter.flush();
	}

	public String getProcessInitiator() throws WTException {
		String string = "";
		WfProcess wfProcess = this.getProcess();
		string = wfProcess.getCreator().getDisplayName();
		return string;
	}

	public void processInitiator(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.translateToHtml(this.getProcessInitiator()));
		printWriter.flush();
	}

	public String getDueDateLabel(Locale locale) throws WTException {
		return this.formatLabel(new WTMessage(RESOURCE, "11", null).getLocalizedMessage(locale));
	}

	public void dueDateLabel(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getDueDateLabel(locale));
		printWriter.flush();
	}

	public String getDueDate(Locale locale) throws WTException {
		try {
			Timestamp timestamp = this.getActivity().getDeadline();
			if (timestamp == null) {
				return "&nbsp;";
			}
			ResourceBundle resourceBundle = ResourceBundle.getBundle("wt.util.utilResource", locale);
			String string = resourceBundle.getString("22");
			return WTStandardDateFormat.format((Date) this.getActivity().getDeadline(), string);
		} catch (Exception exception) {
			logger.error("", (Throwable) exception);
			return "&nbsp;";
		}
	}

	public void dueDate(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.translateToHtml(this.getDueDate(locale)));
		printWriter.flush();
	}

	public String getWorkItemRoleLabel(Locale locale) throws WTException {
		return this.formatLabel(new WTMessage(RESOURCE, "38", null).getLocalizedMessage(locale));
	}

	public void workItemRoleLabel(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getWorkItemRoleLabel(locale));
		printWriter.flush();
	}

	public String getWorkItemRole(Locale locale) throws WTException {
		return this.getWorkItem().getRole().getDisplay(locale);
	}

	public void role(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.translateToHtml(this.getWorkItemRole(locale)));
		printWriter.flush();
	}

	public String getAssigneeLabel(Locale locale) throws WTException {
		return this.formatLabel(new WTMessage(RESOURCE, "13", null).getLocalizedMessage(locale));
	}

	public void assigneeLabel(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getAssigneeLabel(locale));
		printWriter.flush();
	}

	public String getAssignee() throws WTException {
		return this.getPrincipalFName(OwnershipHelper.getOwner(this.getWorkItem()));
	}

	public String getOriginalOwner() throws WTException {
		String string = null;
		if (this.getWorkItem().getOrigOwner() != null && this.getWorkItem().getOrigOwner().getObject() != null) {
			string = ((WTUser) this.getWorkItem().getOrigOwner().getObject()).getName();
		}
		return string;
	}

	public String getOriginalOwnerLocal() throws WTException {
		String string = this.getOriginalOwner();
		String string2 = "";
		if (string != null && string.length() > 0) {
			string2 = WTMessage.getLocalizedMessage(RESOURCE, "167", new Object[] { string });
		}
		return string2;
	}

	public void assignee(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.translateToHtml(this.getAssignee()));
		printWriter.flush();
	}

	public String getActivityDescriptionLabel(Locale locale) throws WTException {
		return this.formatLabel(new WTMessage(RESOURCE, "51", null).getLocalizedMessage(locale));
	}

	public void activityDescriptionLabel(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getActivityDescriptionLabel(locale));
		printWriter.flush();
	}

	public String getActivityDescription() throws WTException {
		return this.getActivity().getDescription();
	}

	public void activityDescription(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.translateToHtml(this.getActivityDescription()));
		printWriter.flush();
	}

	public String getProcessDescriptionLabel(Locale locale) throws WTException {
		return this.formatLabel(new WTMessage(RESOURCE, "22", null).getLocalizedMessage(locale));
	}

	public void processDescriptionLabel(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getProcessDescriptionLabel(locale));
		printWriter.flush();
	}

	public String getProcessDescription() throws WTException {
		return this.getProcess().getDescription();
	}

	public void ProcessDescription(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.translateToHtml(this.getProcessDescription()));
		printWriter.flush();
	}

	public void processVariable(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = properties.getProperty(VARIABLE_KEY);
		printWriter.println(this.getProcessVariableDisplay(string, locale));
		printWriter.flush();
	}

	public void processVariables(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println("<TR>" + this.columnLabel(new WTMessage(RESOURCE, "4", null).getLocalizedMessage(locale)));
		printWriter.println(this.columnLabel(new WTMessage(RESOURCE, "5", null).getLocalizedMessage(locale)));
		ProcessData processData = this.getProcess().getContext();
		String[] stringArray = processData.getNames();
		String[] stringArray2 = processData.getDisplayNames(locale);
		for (int i = 0; i < stringArray.length; ++i) {
			Object object = processData.getValue(stringArray[i]);
			String string = stringArray2[i];
			if (stringArray[i].equals("instructions")) {
				string = new WTMessage(RESOURCE, "21", null).getLocalizedMessage(locale);
			}
			if (stringArray[i].equals(PBO_CONTEXT)) {
				string = new WTMessage(RESOURCE, "14", null).getLocalizedMessage(locale);
			}
			if (stringArray[i].equals("self"))
				continue;
			printWriter.println("<TR>" + this.tableCell(string)
					+ this.tableCell(this.getProcessVariableDisplay(stringArray[i], locale)));
		}
		printWriter.flush();
	}

	private String getProcessVariableDisplay(String string, Locale locale) {
		Object object;
		Object object2 = this.getProcess().getContext().getValue(string);
		if (object2 == null) {
			return "&nbsp;";
		}
		try {
			object = this.getProcess().getContext().getVariableClass(string);
			if (logger.isDebugEnabled()) {
				logger.debug(string + " class: " + ((Class) object).getName());
			}
			if (WTObject.class.isAssignableFrom((Class<?>) object)) {
				return WfHtmlFormat.createObjectLink((WTObject) object2, null, locale);
			}
			if (EnumeratedType.class.isAssignableFrom((Class<?>) object)) {
				return this.translateToHtml(((EnumeratedType) object2).getDisplay(locale));
			}
		} catch (Exception exception) {
			// empty catch block
		}
		if (object2 instanceof ObjectReference) {
			return object2.toString();
		}
		if (object2 instanceof Date) {
			object = ResourceBundle.getBundle("wt.util.utilResource", locale);
			String string2 = ((ResourceBundle) object).getString("22");
			return this.translateToHtml(WTStandardDateFormat.format((Date) object2, string2));
		}
		if (object2 instanceof WfDueDate) {
			object = ResourceBundle.getBundle("wt.util.utilResource", locale);
			String string3 = ((ResourceBundle) object).getString("22");
			return this
					.translateToHtml(WTStandardDateFormat.format((Date) ((WfDueDate) object2).getDeadline(), string3));
		}
		if (object2 instanceof URL) {
			return this.translateToHtml(object2 == null ? null
					: HtmlUtil.createLink(((URL) object2).toExternalForm(), null, ((URL) object2).toExternalForm()));
		}
		object = object2.toString();
		object = this.escapeHtml((String) object);
		return this.translateToHtml((String) object);
	}

	public void setContextObject(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		String string = properties.getProperty(CONTEXT_KEY);
		if (string.equals(PROCESS_CONTEXT)) {
			this.setContextObj(this.getProcess());
		} else if (string.equals(ACTIVITY_CONTEXT)) {
			this.setContextObj(this.getActivity());
		} else if (string.equals(PBO_CONTEXT)) {
			boolean bl = true;
			try {
				bl = SessionServerHelper.manager.setAccessEnforced(false);
				this.setContextObj((WTObject) this.getProcess().getContext().getValue(PBO_CONTEXT));
			} finally {
				SessionServerHelper.manager.setAccessEnforced(bl);
			}
		} else {
			PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
			printWriter.print(string + " is not a recognized parameter for the setContextObject script ");
			printWriter.print("process, activity,  and primaryBusinessObject are valid parameters.");
			printWriter.flush();
		}
	}

	private boolean isUpdateContentDisplayed() {
		Properties properties = this.getFormData();
		String string = properties.getProperty(CONTENT_UPDATE);
		return string != null && string.length() > 0 && string.equals(CONTENT_UPDATE_VALUE);
	}

	private boolean isUpdateRolesDisplayed() {
		Properties properties = this.getFormData();
		String string = properties.getProperty(ROLES_UPDATE);
		return string != null && string.length() > 0 && string.equals(ROLES_UPDATE_VALUE);
	}

	private boolean isUpdateJITProjectsDisplayed() {
		Properties properties = this.getFormData();
		String string = properties.getProperty(JIT_PROJECTS_UPDATE);
		return string != null && string.length() > 0 && string.equals(JIT_PROJECTS_UPDATE_VALUE);
	}

	public void processContent(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = properties.getProperty(STYLE_KEY, LINK);
		Properties properties2 = new Properties();
		if (this.isUpdateContentDisplayed() || string.equals(EXPAND)) {
			printWriter.println("");
		} else {
			printWriter.println(this.hiddenContextString(CONTENT_UPDATE, CONTENT_UPDATE));
			WfProcess wfProcess = this.getProcess();
			WTPrincipal wTPrincipal = SessionHelper.getPrincipal();
			if (AccessControlHelper.manager.hasAccess(wTPrincipal, wfProcess, AccessPermission.MODIFY)) {
				Object[] objectArray = new Object[] { wfProcess.getName() };
				String string2 = "";
				string2 = this.pdmlContext
						? WfTaskProcessor.getContextActionLink(
								this.getURLProcessorLink("URLTemplateAction", (Properties) null, true), CONTENT_UPDATE,
								CONTENT_UPDATE_VALUE, new WTMessage(RESOURCE, "164", null).getLocalizedMessage(locale))
						: WfTaskProcessor.getContextActionLink(this.getURLProcessorLink("URLTemplateAction", null),
								CONTENT_UPDATE, CONTENT_UPDATE_VALUE,
								new WTMessage(RESOURCE, "118", objectArray).getLocalizedMessage(locale));
				if (this.pdmlContext) {
					printWriter.println(string2);
				} else {
					printWriter.println(
							"<IMG SRC=\"" + CODEBASE + "/" + UPDATE_CONTENT_GIF + "\" border=\"0\">&nbsp" + string2);
				}
			}
		}
		printWriter.flush();
	}

	public void updateProjects(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = properties.getProperty(STYLE_KEY, LINK);
		if (this.isUpdateJITProjectsDisplayed() || string.equals(EXPAND)) {
			Properties properties2 = new Properties();
			properties2.setProperty(JIT_PROJECTS_UPDATE, JIT_PROJECTS_UPDATE_VALUE);
			printWriter.flush();
			boolean bl = false;
			WfVariableInfo[] wfVariableInfoArray = ((WfAssignedActivityTemplate) this.myActivity.getTemplateReference()
					.getObject()).getContextSignature().getVariableList();
			for (int i = 0; i < wfVariableInfoArray.length; ++i) {
				if (!Project.class.isAssignableFrom(wfVariableInfoArray[i].getVariableClass()))
					continue;
				bl = true;
				break;
			}
			if (bl) {
				printWriter.println("");
			} else {
				printWriter.println(new WTMessage(RESOURCE, "114", null).getLocalizedMessage(locale));
			}
		} else {
			printWriter.println(this.hiddenContextString(JIT_PROJECTS_UPDATE, JIT_PROJECTS_UPDATE));
			String string2 = WfTaskProcessor.getContextActionLink(this.getURLProcessorLink("URLTemplateAction", null),
					JIT_PROJECTS_UPDATE, JIT_PROJECTS_UPDATE_VALUE,
					new WTMessage(RESOURCE, "113", null).getLocalizedMessage(locale));
			printWriter.println("<IMG SRC=\"" + CODEBASE + "/" + JIT_PROJECTS_GIF + "\" border=\"0\">&nbsp" + string2);
		}
		printWriter.flush();
	}

	public void updateTeams(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = properties.getProperty(STYLE_KEY, LINK);
		if (this.isUpdateJITProjectsDisplayed() || string.equals(EXPAND)) {
			Properties properties2 = new Properties();
			properties2.setProperty(JIT_PROJECTS_UPDATE, JIT_PROJECTS_UPDATE_VALUE);
			printWriter.flush();
			boolean bl = false;
			WfVariableInfo[] wfVariableInfoArray = ((WfAssignedActivityTemplate) this.myActivity.getTemplateReference()
					.getObject()).getContextSignature().getVariableList();
			for (int i = 0; i < wfVariableInfoArray.length; ++i) {
				if (!TeamTemplate.class.isAssignableFrom(wfVariableInfoArray[i].getVariableClass()))
					continue;
				bl = true;
				break;
			}
			if (bl) {
				printWriter.println("");
			} else {
				printWriter.println(new WTMessage(RESOURCE, "158", null).getLocalizedMessage(locale));
			}
		} else {
			printWriter.println(this.hiddenContextString(JIT_TEAMS_UPDATE, JIT_TEAMS_UPDATE));
			String string2 = WfTaskProcessor.getContextActionLink(this.getURLProcessorLink("URLTemplateAction", null),
					JIT_TEAMS_UPDATE, JIT_TEAMS_UPDATE_VALUE,
					new WTMessage(RESOURCE, "157", null).getLocalizedMessage(locale));
			printWriter.println("<IMG SRC=\"" + CODEBASE + "/" + JIT_TEAMS_GIF + "\" border=\"0\">&nbsp" + string2);
		}
		printWriter.flush();
	}

	public void primaryBusinessObjectLink(Properties properties, Locale locale, OutputStream outputStream) {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		try {
			WTObject wTObject = (WTObject) this.getProcess().getContext().getValue(PBO_CONTEXT);
			if (wTObject != null) {
				printWriter.println(WfHtmlFormat.createObjectLink(wTObject, null, locale));
				printWriter.flush();
			}
		} catch (WTRuntimeException wTRuntimeException) {
			printWriter.println(new WTMessage(RESOURCE, "24", null).getLocalizedMessage(locale));
			printWriter.flush();
			logger.debug("The following exception occurred fetching the target object: ",
					(Throwable) wTRuntimeException);
		} catch (WTException wTException) {
			printWriter.println(new WTMessage(RESOURCE, "79", null).getLocalizedMessage(locale));
			printWriter.flush();
			logger.debug("The following exception occurred fetching the target object: ", (Throwable) wTException);
		}
	}

	public void augmentRoles(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = properties.getProperty(STYLE_KEY, LINK);
		if (this.isUpdateRolesDisplayed() || string.equals(EXPAND)) {
			printWriter.println("");
		} else {
			printWriter.println(this.hiddenContextString(ROLES_UPDATE, ROLES_UPDATE));
			String string2 = WfTaskProcessor.getContextActionLink(this.getURLProcessorLink("URLTemplateAction", null),
					ROLES_UPDATE, ROLES_UPDATE_VALUE, new WTMessage(RESOURCE, "112", null).getLocalizedMessage(locale));
			printWriter.println("<IMG SRC=\"" + CODEBASE + "/" + ROLES_UPDATE_GIF + "\" border=\"0\">&nbsp" + string2);
		}
		printWriter.flush();
	}

	protected static String getContextActionLink(String string, String string2, String string3, String string4) {
		String string5 = "javascript:refreshForm ('" + string + "', '" + string2 + "', '" + string3 + "')";
		String string6 = "class=\"detailsLink\"";
		return HtmlUtil.createLink(string5, string6, string4);
	}

	private boolean isAdHocActivitiesDisplayed() {
		if (this.adHocActivitiesDisplayed == null) {
			Properties properties = this.getFormData();
			String string = properties.getProperty(AD_HOC_CREATE);
			this.adHocActivitiesDisplayed = string != null && string.length() > 0 && string.equals(AD_HOC_CREATE_VALUE)
					? Boolean.TRUE
					: Boolean.FALSE;
		}
		return this.adHocActivitiesDisplayed;
	}

	private boolean isAdHocActivitiesInProgress() throws WTException {
		WfAdHocActivity wfAdHocActivity = null;
		try {
			wfAdHocActivity = (WfAdHocActivity) this.getActivity();
		} catch (ClassCastException classCastException) {
			this.adHocActivitiesInProgress = Boolean.FALSE;
		}
		if (this.adHocActivitiesInProgress == null) {
			WfContainer wfContainer = wfAdHocActivity.getPerformer();
			this.adHocActivitiesInProgress = wfContainer != null && WfState.OPEN.includes(wfContainer.getState())
					? Boolean.TRUE
					: Boolean.FALSE;
		}
		return this.adHocActivitiesInProgress;
	}

	public void adHocActivities(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = properties.getProperty(STYLE_KEY, LINK);
		String string2 = properties.getProperty("numActivities", "5");
		Properties properties2 = this.getFormData();
		String string3 = properties2.getProperty("numActivities");
		if (string3 == null || string3.length() <= 0) {
			properties2.put("numActivities", string2);
		}
		WfAdHocActivity wfAdHocActivity = null;
		try {
			wfAdHocActivity = (WfAdHocActivity) this.getActivity();
		} catch (ClassCastException classCastException) {
			return;
		}
		if (this.isAdHocActivitiesDisplayed() || string.equals(EXPAND)) {
			printWriter.println(this.hiddenContextString(AD_HOC_CREATE, AD_HOC_CREATE_VALUE));
		} else {
			printWriter.println(this.hiddenContextString(AD_HOC_CREATE, AD_HOC_CREATE));
		}
		printWriter.flush();
		WfContainer wfContainer = wfAdHocActivity.getPerformer();
		if (wfContainer != null) {
			return;
		}
		if (this.isAdHocActivitiesDisplayed() || string.equals(EXPAND)) {
			String string4 = this.getContextAction();
			try {
				this.setContextAction(AD_HOC_ACTION);
				Properties properties3 = new Properties();
				String string5 = this.getPageContext().getID();
				properties3.put("PageContext", string5);
				SubTemplateService subTemplateService = new SubTemplateService(properties3, locale, outputStream);
				subTemplateService.processTemplate(this.getState());
			} catch (Exception exception) {
				throw new WTException(exception);
			} finally {
				this.setContextAction(string4);
			}
		} else {
			String string6 = WfTaskProcessor.getContextActionLink(this.getURLProcessorLink("URLTemplateAction", null),
					AD_HOC_CREATE, AD_HOC_CREATE_VALUE,
					new WTMessage(RESOURCE, "86", null).getLocalizedMessage(locale));
			printWriter
					.println("<IMG SRC=\"" + CODEBASE + "/" + ADHOC_ACTIVITY_GIF + "\" border=\"0\">&nbsp" + string6);
			printWriter.flush();
		}
	}

	public void taskCompleteButton(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		Properties properties2 = new Properties();
		properties2.put("action", "WfTaskComplete");
		properties2.put("oid", new ReferenceFactory().getReferenceString(this.getWorkItem()));
		URL uRL = GatewayURL.getAuthenticatedGateway(null).getURL("wt.enterprise.URLProcessor", "processForm", "",
				properties2);
		printWriter.print(this.generateTaskCompleteButton(uRL,
				new WTMessage(RESOURCE, "9", null).getLocalizedMessage(locale), properties2, locale));
		printWriter.flush();
	}

	protected String generateTaskCompleteButton(URL uRL, String string, Properties properties, Locale locale)
			throws WTException {
		CharArrayWriter charArrayWriter = new CharArrayWriter(300);
		try {
			charArrayWriter.write("<FORM method = \"POST\" action = \"" + uRL.toExternalForm() + "\">\n");
			charArrayWriter.write("<INPUT TYPE=\"hidden\" NAME=\"refreshAction\"  VALUE=\"" + this.getContextAction()
					+ "\"></INPUT>");
			charArrayWriter.write("<INPUT TYPE=\"hidden\" NAME=\"refreshOid\"  VALUE=\""
					+ new ReferenceFactory().getReferenceString(this.getWorkItem()) + "\"></INPUT>");
			this.writeActivityVariables(charArrayWriter, locale);
			this.writeRoutingChoices(charArrayWriter);
			this.writeTaskCompleteButton(charArrayWriter, string);
			this.writeCloseForm(charArrayWriter);
		} catch (IOException iOException) {
			throw new WTException(iOException);
		}
		return charArrayWriter.toString();
	}

	public void activityVariables(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		CharArrayWriter charArrayWriter = new CharArrayWriter(300);
		try {
			this.writeActivityVariables(charArrayWriter, locale);
		} catch (IOException iOException) {
			throw new WTException(iOException);
		}
		printWriter.print(charArrayWriter.toString());
		printWriter.flush();
	}

	private void writeActivityVariables(CharArrayWriter charArrayWriter, Locale locale) throws IOException {
		charArrayWriter.write("<DIV ALIGN=left>\n");
		charArrayWriter.write("<TABLE CELLPADDING=3 >\n");
		ProcessData processData = this.getActivity().getContext();
		WfVariableInfo[] wfVariableInfoArray = ((WfAssignedActivityTemplate) this.getActivity().getTemplateReference()
				.getObject()).getContextSignature().getVariableList();
		int n = wfVariableInfoArray.length;
		String string = "";
		for (int i = 0; i < wfVariableInfoArray.length; ++i) {
			if (!this.showVariable(wfVariableInfoArray[i].getName(), wfVariableInfoArray[i].isVisible()))
				continue;
			string = wfVariableInfoArray[i].isRequired() ? "*" : "";
			charArrayWriter.write("<TR><TD VALIGN=top align=right class=\"propTitle\"><B><font class=tableWfHeader>"
					+ string + wfVariableInfoArray[i].getDisplayName(locale)
					+ new WTMessage(RESOURCE, "3", null).getLocalizedMessage(locale) + "</B></TD>");
			charArrayWriter.write("<TD VALIGN=top align=left class=\"propValue\"><font class=tableWfHeader>"
					+ this.displayActivityVariable(wfVariableInfoArray[i], processData, DEFAULT_ROW_NUMBER,
							DEFAULT_COLUMN_NUMBER, locale)
					+ "</TD></TR>\n");
		}
		charArrayWriter.write("</TABLE>");
	}

	public void routingChoices(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		CharArrayWriter charArrayWriter = new CharArrayWriter(300);
		if (this.isAdHocActivitiesInProgress() || this.isAdHocActivitiesDisplayed()) {
			return;
		}
		try {
			this.writeRoutingChoices(charArrayWriter);
		} catch (IOException iOException) {
			throw new WTException(iOException);
		}
		printWriter.print(charArrayWriter.toString());
		printWriter.flush();
	}

	void writeRoutingChoices(CharArrayWriter charArrayWriter) throws IOException, WTException {
		if (this.isAdHocActivitiesInProgress() || this.isAdHocActivitiesDisplayed()) {
			return;
		}
		charArrayWriter.write("<TABLE CELLPADDING=3 WIDTH=\"90%\" >\n");
		if (this.getActivity().getRouterType().equals(WfRouterType.MANUAL)
				|| this.getActivity().getRouterType().equals(WfRouterType.MANUAL_EXCLUSIVE)) {
			Vector vector = this.getActivity().getUserEventList().toVector();
			String string = "checkbox";
			String string2 = "";
			if (this.getActivity().getRouterType().equals(WfRouterType.MANUAL_EXCLUSIVE)) {
				string = "radio";
				string2 = " checked";
			}
			int n = 0;
			Enumeration enumeration = vector.elements();
			while (enumeration.hasMoreElements()) {
				String string3 = (String) enumeration.nextElement();
				String string4 = (String) this.getState().getFormData().get(ROUTER_EVENT + n);
				if (string4 != null && string4.equals(string3)) {
					string2 = " checked";
				}
				charArrayWriter.write("<TR><TD align=left><font size=\"2\">&nbsp;<INPUT type=" + string + string2
						+ " name=\"WfUserEvent" + n + "\" value=\"" + string3 + "\" > " + string3 + "</TD></TR>\n");
				if (string.equals("checkbox")) {
					++n;
				}
				string2 = "";
			}
		}
		charArrayWriter.write("</TABLE><BR>");
	}

	public void soloTaskCompleteButton(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		if (this.isAdHocActivitiesInProgress()) {
			printWriter.print(new WTMessage(RESOURCE, "106", null).getLocalizedMessage(locale));
		} else {
			SignatureEngine signatureEngine;
			String string = null;
			if (((WfAssignedActivityTemplate) this.myActivity.getTemplateReference().getObject()).isSigningRequired()
					&& (signatureEngine = SignatureEngineFactory.getInstance()) != null) {
				String string2 = null;
				string2 = this.isAdHocActivitiesDisplayed()
						? new WTMessage(ENGINES_RESOURCE, "4", null).getLocalizedMessage(locale)
						: new WTMessage(ENGINES_RESOURCE, "3", null).getLocalizedMessage(locale);
				printWriter.print("<font face=\"arial, helvetica\" size=\"2\">" + string2 + "<p>");
				SigVariableInfo[] sigVariableInfoArray = signatureEngine.getVariableInfo();
				for (int i = 0; i < sigVariableInfoArray.length; ++i) {
					printWriter.print("<font face=\"arial, helvetica\" size=\"2\"><B>"
							+ this.formatLabel((sigVariableInfoArray[i].isRequired() ? "*" : "&nbsp;")
									+ sigVariableInfoArray[i].getLabel().getLocalizedMessage(locale))
							+ "</B>");
					printWriter.print("<input name=\"" + sigVariableInfoArray[i].getFormField() + "\"");
					if (sigVariableInfoArray[i].isPasswordField()) {
						printWriter.print(" type=\"password\"");
					} else {
						printWriter.print(" type=\"text\"");
					}
					printWriter.print(" size=\"" + (sigVariableInfoArray[i].getPreferredSize() < 40
							? sigVariableInfoArray[i].getPreferredSize()
							: 40) + "\" AUTOCOMPLETE=\"OFF\">");
					printWriter.println("<BR>");
				}
				printWriter.println("<P>");
			}
			if (this.isAdHocActivitiesDisplayed()) {
				string = new WTMessage(RESOURCE, "87", null).getLocalizedMessage(locale);
				printWriter
						.print("<INPUT type = \"BUTTON\" value = \"" + string + "\" onClick=validatePredecessors()>");
			} else if (this.getContextAction() != null && this.getContextAction().equals(SUBMIT_TASK_ACTION)) {
				string = new WTMessage(LIFECYCLE_RESOURCE, "90", null).getLocalizedMessage(locale);
				printWriter.print("<INPUT type = \"SUBMIT\" value = \"" + string + "\">");
			} else {
				string = new WTMessage(RESOURCE, "9", null).getLocalizedMessage(locale);
				printWriter.print("<INPUT type = \"SUBMIT\" value = \"" + string + "\">");
			}
		}
		printWriter.flush();
	}

	void writeTaskCompleteButton(CharArrayWriter charArrayWriter, String string) throws IOException, WTException {
		charArrayWriter.write("<DIV ALIGN=left><INPUT type = \"SUBMIT\" value = \"" + string + "\">");
	}

	public void beginForm(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		CharArrayWriter charArrayWriter = new CharArrayWriter(300);
		try {
			Properties properties2 = new Properties();
			properties2.put("action", "WfTaskComplete");
			properties2.put("oid", new ReferenceFactory().getReferenceString(this.getWorkItem()));
			URL uRL = GatewayURL.getAuthenticatedGateway(null).getURL("wt.workflow.work.WorkItemURLProcessor",
					"processForm", "", properties2);
			this.writeBeginForm(charArrayWriter, uRL);
		} catch (IOException iOException) {
			throw new WTException(iOException);
		}
		printWriter.print(charArrayWriter.toString());
		printWriter.flush();
	}

	protected void writeBeginForm(CharArrayWriter charArrayWriter, URL uRL) throws IOException, WTException {
		charArrayWriter.write("<FORM method = \"POST\" action = \"" + uRL.toExternalForm() + "\">\n");
		charArrayWriter.write(
				"<INPUT TYPE=\"hidden\" NAME=\"refreshAction\"  VALUE=\"" + this.getContextAction() + "\"></INPUT>");
		charArrayWriter.write("<INPUT TYPE=\"hidden\" NAME=\"refreshOid\"  VALUE=\""
				+ new ReferenceFactory().getReferenceString(this.getWorkItem()) + "\"></INPUT>");
	}

	public void closeForm(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		CharArrayWriter charArrayWriter = new CharArrayWriter(300);
		try {
			this.writeCloseForm(charArrayWriter);
		} catch (IOException iOException) {
			throw new WTException(iOException);
		}
		printWriter.print(charArrayWriter.toString());
		printWriter.flush();
	}

	void writeCloseForm(CharArrayWriter charArrayWriter) throws IOException {
		charArrayWriter.write("</FORM><BR>\n");
	}

	String displayActivityVariableDisplayName(WfVariableInfo wfVariableInfo, ProcessData processData, String string,
			Locale locale) {
		String string2 = wfVariableInfo.getName();
		WfVariable wfVariable = processData.getVariable(string2);
		Class clazz = wfVariable.getVariableClass();
		String string3 = wfVariable.getTypeName();
		Object object = wfVariable.getValue();
		if (this.showVariable(string2, wfVariableInfo.isVisible())) {
			if (logger.isDebugEnabled()) {
				logger.debug("TypeName:      " + wfVariable.getTypeName());
				logger.debug("Display Name:  " + wfVariable.getName());
				logger.debug("Value       :  " + wfVariable.getValue());
				logger.debug("Default Value  " + wfVariableInfo.getDefaultValue());
				logger.debug("Mutable        " + wfVariableInfo.isMutable());
			}
			if (!string.equals("true")) {
				return wfVariableInfo.getDisplayName(locale);
			}
			String string4 = "";
			string4 = wfVariableInfo.isRequired() ? "*" : "";
			return "<B><font class=tableWfHeader>" + string4 + wfVariableInfo.getDisplayName(locale)
					+ new WTMessage(RESOURCE, "3", null).getLocalizedMessage(locale) + "</B>";
		}
		return "&nbsp;";
	}

	String displayActivityVariable(WfVariableInfo wfVariableInfo, ProcessData processData, String string,
			String string2, Locale locale) {
		String string3 = wfVariableInfo.getName();
		WfVariable wfVariable = processData.getVariable(string3);
		Class clazz = wfVariable.getVariableClass();
		String string4 = wfVariable.getTypeName();
		Object object = wfVariable.getValue();
		WTContainerRef wTContainerRef = null;
		Object object2 = this.getContextObj();
		if (object2 instanceof WTContained) {
			wTContainerRef = ((WTContained) object2).getContainerReference();
		}
		if (this.showVariable(string3, wfVariableInfo.isVisible())) {
			String string5 = "";
			if (logger.isDebugEnabled()) {
				logger.debug("TypeName:      " + wfVariable.getTypeName());
				logger.debug("Display Name:  " + wfVariable.getName());
				logger.debug("Value       :  " + wfVariable.getValue());
				logger.debug("Default Value  " + wfVariableInfo.getDefaultValue());
				logger.debug("Mutable        " + wfVariableInfo.isMutable());
			}
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Processing variable of class: " + clazz.getName());
				}
				if (WTObject.class.isAssignableFrom(clazz)) {
					if (WTPrincipal.class.isAssignableFrom(clazz)) {
						if (wfVariableInfo.isReadOnly()) {
							return processData.getValue(string3) == null ? "&nbsp;"
									: (WTUser.class.isAssignableFrom(clazz)
											? ((WTUser) processData.getValue(string3)).getFullName()
											: ((WTPrincipal) processData.getValue(string3)).getName());
						}
						return this.principalSelector(wfVariableInfo.getTypeName(),
								(WTPrincipal) processData.getValue(string3), wfVariableInfo.getName());
					}
					if (string4.equals("wt.team.Team")) {
						Externalizable externalizable;
						Serializable serializable;
						if (wfVariableInfo.isReadOnly()) {
							return this.translateToHtml(processData.getValue(string3) == null ? null
									: ((Team) processData.getValue(string3)).getName());
						}
						String string6 = null;
						if (processData.getValue(string3) != null) {
							serializable = new ReferenceFactory();
							externalizable = TeamReference.newTeamReference((Team) processData.getValue(string3));
							string6 = ((TeamReference) externalizable).getIdentity();
						}
						serializable = TeamHelper.service.findTeams();
						externalizable = new ReferenceFactory();
						Vector<String> vector = new Vector<String>(((Vector) serializable).size());
						Vector<String> vector2 = new Vector<String>(((Vector) serializable).size());
						int n = 0;
						boolean bl = !wfVariableInfo.isRequired();
						for (int i = 0; i < ((Vector) serializable).size(); ++i) {
							TeamReference teamReference = (TeamReference) ((Vector) serializable).elementAt(i);
							String string7 = ((ReferenceFactory) externalizable).getReferenceString(teamReference);
							vector.addElement(string7);
							String string8 = teamReference.getIdentity();
							vector2.addElement(string8);
							if (string6 == null || !string6.equals(string8))
								continue;
							n = i;
							if (!bl)
								continue;
							++n;
						}
						return HtmlUtil.createSelectHTML(string3, null, vector, vector2, bl, n);
					}
					if (string4.equals("wt.team.TeamTemplate")) {
						Externalizable externalizable;
						Serializable serializable;
						if (wfVariableInfo.isReadOnly()) {
							return this.translateToHtml(processData.getValue(string3) == null ? null
									: ((TeamTemplate) processData.getValue(string3)).getName());
						}
						String string9 = null;
						if (processData.getValue(string3) != null) {
							serializable = new ReferenceFactory();
							externalizable = TeamTemplateReference
									.newTeamTemplateReference((TeamTemplate) processData.getValue(string3));
							string9 = ((TeamTemplateReference) externalizable).getIdentity();
						}
						serializable = TeamHelper.service.findTeamTemplates(wTContainerRef);
						externalizable = new ReferenceFactory();
						Vector<String> vector = new Vector<String>(((Vector) serializable).size());
						Vector<String> vector3 = new Vector<String>(((Vector) serializable).size());
						int n = 0;
						boolean bl = !wfVariableInfo.isRequired();
						for (int i = 0; i < ((Vector) serializable).size(); ++i) {
							TeamTemplateReference teamTemplateReference = (TeamTemplateReference) ((Vector) serializable)
									.elementAt(i);
							String string10 = ((ReferenceFactory) externalizable)
									.getReferenceString(teamTemplateReference);
							vector.addElement(string10);
							String string11 = teamTemplateReference.getIdentity();
							vector3.addElement(string11);
							if (string9 == null || !string9.equals(string11))
								continue;
							n = i;
							if (!bl)
								continue;
							++n;
						}
						return HtmlUtil.createSelectHTML(string3, null, vector, vector3, bl, n);
					}
					return WfHtmlFormat.createObjectLink((WTObject) processData.getValue(string3), null, locale);
				}
				if (EnumeratedType.class.isAssignableFrom(clazz)) {
					if (wfVariableInfo.isReadOnly()) {
						return processData.getValue(string3) == null ? "&nbsp;"
								: ((EnumeratedType) processData.getValue(string3)).getDisplay(locale);
					}
					EnumeratedType enumeratedType = null;
					if (processData.getValue(string3) != null) {
						enumeratedType = (EnumeratedType) processData.getValue(string3);
					}
					if (this.getFormData() != null && this.getFormData().getProperty(string3) != null
							&& this.getFormData().getProperty(string3).length() > 0) {
						enumeratedType = EnumeratedTypeUtil.toEnumeratedType(wfVariableInfo.getTypeName(),
								this.getFormData().getProperty(string3));
					}
					return this.enumeratedTypeSelector(clazz, enumeratedType, string3, locale);
				}
			} catch (WTException wTException) {
				logger.error("", (Throwable) wTException);
			}
			Object object3 = this.getFormData().getProperty(string3) == null
					? (processData.getValue(string3) == null ? null : processData.getValue(string3).toString())
					: (string5 = this.getFormData().getProperty(string3));
			if (wfVariableInfo.getTypeName().equals("java.lang.String")) {
				if (wfVariableInfo.isReadOnly()) {
					string5 = this.escapeHtml(string5);
					return this.translateToHtml(string5);
				}
				return "<TEXTAREA name=\"" + string3 + "\"  rows=\"" + string + "\" cols=\"" + string2 + "\" WRAP>"
						+ (string5 == null ? "" : string5) + "</TEXTAREA>";
			}
			if (wfVariableInfo.getTypeName().equals("java.lang.Boolean")
					|| wfVariableInfo.getTypeName().equals("boolean")) {
				if (wfVariableInfo.isReadOnly()) {
					return (Boolean) processData.getValue(string3) != false
							? new WTMessage(RESOURCE, "134", null).getLocalizedMessage(locale)
							: new WTMessage(RESOURCE, "135", null).getLocalizedMessage(locale);
				}
				String string12 = "";
				if (this.getFormData().getProperty(string3) != null) {
					string12 = this.getFormData().getProperty(string3).equals("true") ? "checked" : "";
				} else if (processData.getValue(string3) != null) {
					string12 = (Boolean) processData.getValue(string3) != false ? "checked" : "";
				}
				return "<INPUT type=\"CHECKBOX\" name=\"" + string3 + "\" size=\"" + string2 + "\" value=\"true\""
						+ string12 + ">";
			}
			if (wfVariableInfo.getTypeName().equals("java.net.URL")) {
				if (wfVariableInfo.isReadOnly()) {
					return HtmlUtil.createLink(string5, null, string5);
				}
				String string13 = "<INPUT type=\"TEXT\" name=\"" + string3 + "\" size=\"" + string2 + "\" value=\""
						+ (string5 == null ? "" : string5) + "\">";
				if (string5 != null) {
					string13 = string13 + "&nbsp;" + HtmlUtil.createLink(string5, null, string5);
				}
				return string13;
			}
			if (wfVariableInfo.getTypeName().equals("java.util.Date")) {
				String string14 = "";
				if (this.getFormData().getProperty(string3) != null) {
					string14 = string5;
				} else if (processData.getValue(string3) != null) {
					ResourceBundle resourceBundle = ResourceBundle.getBundle("wt.util.utilResource", locale);
					String string15 = resourceBundle.getString("22");
					string14 = WTStandardDateFormat.format((Date) processData.getValue(string3), string15);
				}
				if (wfVariableInfo.isReadOnly()) {
					return this.translateToHtml(string14);
				}
				return "<INPUT type=\"TEXT\" name=\"" + string3 + "\" size=\"" + string2 + "\" value=\"" + string14
						+ "\">";
			}
			if (wfVariableInfo.getTypeName().equals("wt.workflow.engine.WfDueDate")) {
				String string16 = "";
				if (this.getFormData().getProperty(string3) != null) {
					string16 = string5;
				} else if (processData.getValue(string3) != null) {
					ResourceBundle resourceBundle = ResourceBundle.getBundle("wt.util.utilResource", locale);
					String string17 = resourceBundle.getString("22");
					string16 = WTStandardDateFormat
							.format((Date) ((WfDueDate) processData.getValue(string3)).getDeadline(), string17);
				}
				if (wfVariableInfo.isReadOnly()) {
					return this.translateToHtml(string16);
				}
				return "<INPUT type=\"TEXT\" name=\"" + string3 + "\" size=\"" + string2 + "\" value=\"" + string16
						+ "\">";
			}
			if (wfVariableInfo.isReadOnly()) {
				string5 = this.escapeHtml(string5);
				return this.translateToHtml(string5);
			}
			return "<INPUT type=\"TEXT\" name=\"" + string3 + "\" size=\"" + string2 + "\" value=\""
					+ (string5 == null ? "" : string5) + "\">";
		}
		return "&nbsp;";
	}

	public void activityVariableDisplayName(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = properties.getProperty(VARIABLE_KEY);
		String string2 = properties.getProperty(FORMATTED, DEFAULT_FORMATTED_VALUE);
		WfVariableInfo wfVariableInfo = ((WfAssignedActivityTemplate) this.getActivity().getTemplateReference()
				.getObject()).getContextSignature().getVariableInfo(string);
		if (wfVariableInfo == null) {
			Object[] objectArray = new Object[] { string };
			printWriter.println(new WTMessage(RESOURCE, "133", objectArray).getLocalizedMessage(locale));
		} else {
			ProcessData processData = this.getActivity().getContext();
			printWriter.println(this.displayActivityVariableDisplayName(wfVariableInfo, processData, string2, locale));
		}
		printWriter.flush();
	}

	public void activityVariable(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = properties.getProperty(VARIABLE_KEY);
		String string2 = properties.getProperty(ROWS, DEFAULT_ROW_NUMBER);
		String string3 = properties.getProperty(COLUMNS, DEFAULT_COLUMN_NUMBER);
		WfVariableInfo wfVariableInfo = ((WfAssignedActivityTemplate) this.getActivity().getTemplateReference()
				.getObject()).getContextSignature().getVariableInfo(string);
		if (wfVariableInfo == null) {
			Object[] objectArray = new Object[] { string };
			printWriter.println(new WTMessage(RESOURCE, "133", objectArray).getLocalizedMessage(locale));
		} else {
			ProcessData processData = this.getActivity().getContext();
			printWriter.println(this.displayActivityVariable(wfVariableInfo, processData, string2, string3, locale));
		}
		printWriter.flush();
	}

	public void taskURL(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		this.taskURL(properties, locale, outputStream, this.getContextRef());
	}

	public void taskURL(Properties properties, Locale locale, OutputStream outputStream, WTContainerRef wTContainerRef)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = this.getTaskUrl(this.getContextRef());
		WorkItem workItem = this.getWorkItem();
		String string2 = WorkflowHtmlUtil.getExternalTaskURL(workItem);
		if (string2 != null) {
			String string3 = "<A id=\"WfExternalTaskTProcessor02\" HREF=\""
					+ HTMLEncoder.encodeForHTMLContent((String) string2) + "\">"
					+ HTMLEncoder.encodeForHTMLContent((String) this.myActivity.getName()) + "</A>";
			String string4 = this
					.formatLabel(new WTMessage(RESOURCE, "NAVIGATE_LABEL", null).getLocalizedMessage(locale));
			String string5 = this
					.formatLabel(new WTMessage(RESOURCE, "WINDCHILL_LABEL", null).getLocalizedMessage(locale));
			printWriter.println(string3 + "(" + string4 + ")");
			printWriter.println("<br>");
			printWriter.println(string + "(" + string5 + ")");
		} else {
			printWriter.println(string);
		}
		printWriter.flush();
	}

	public void pageTitle(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println("<TITLE>" + this.getActivity().getName() + "</TITLE>");
		printWriter.flush();
	}

	public void listProcessContent(Properties properties, Locale locale, OutputStream outputStream) throws Exception {
		try {
			this.setContextObj(this.getProcess());
			this.listContent(properties, locale, outputStream);
		} catch (WTException wTException) {
			if (wTException instanceof NotAuthorizedException) {
				logger.debug("", (Throwable) wTException);
			}
			throw wTException;
		}
	}

	protected boolean showVariable(String string, boolean bl) {
		if (!bl) {
			return false;
		}
		if (string.equals("instructions")) {
			return false;
		}
		return !string.equals(PBO_CONTEXT);
	}

	public void createWfNavigationBar(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		this.createGlobalNavigationBar(properties, locale, outputStream);
	}

	public void getHelpURL(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = properties.getProperty("helpKey");
		String string2 = properties.getProperty("resource");
		if (string == null) {
			throw new WTException(LIFECYCLE_RESOURCE, "1", new Object[] { "helpKey" });
		}
		if (string2 == null) {
			throw new WTException(LIFECYCLE_RESOURCE, "1", new Object[] { "resource" });
		}
		try {
			printWriter.println(CODEBASE + "/" + WTMessage.getLocalizedMessage(string2, string, null, locale));
		} catch (MissingResourceException missingResourceException) {
			logger.error("*ERROR* " + missingResourceException.getMessage() + ": {"
					+ missingResourceException.getClassName() + " - " + missingResourceException.getKey() + "}");
			throw missingResourceException;
		}
		printWriter.flush();
	}

	public String principalSelector(String string, WTPrincipal wTPrincipal, String string2) throws WTException {
		WTPrincipal wTPrincipal2;
		Enumeration enumeration;
		Vector vector;
		Vector vector2;
		Vector vector3 = new Vector();
		Vector vector4 = new Vector();
		DirectoryContextProvider directoryContextProvider = OrganizationServicesHelper.manager
				.newDirectoryContextProvider((String[]) null, (String[]) null);
		if (string.equals("wt.org.WTPrincipal") || string.equals("wt.org.WTUser")) {
			vector2 = (Vector) MethodContext.getContext().get(WF_USERS);
			vector = (Vector) MethodContext.getContext().get(WF_DISPLAY_USERS);
			if (vector2 == null || vector == null) {
				enumeration = OrganizationServicesHelper.manager.queryPrincipals(WTUser.class, "name='*'",
						directoryContextProvider);
				SortedEnumByPrincipal sortedEnumByPrincipal = new SortedEnumByPrincipal(enumeration, false, 1);
				while (sortedEnumByPrincipal.hasMoreElements()) {
					wTPrincipal2 = (WTUser) sortedEnumByPrincipal.nextElement();
					vector3.addElement(wTPrincipal2.getName());
					vector4.addElement(SortedEnumByPrincipal.getLastNameFirstName((WTUser) wTPrincipal2));
				}
				MethodContext.getContext().put(WF_USERS, vector4);
				MethodContext.getContext().put(WF_DISPLAY_USERS, vector3);
			} else {
				vector4 = vector2;
				vector3 = vector;
			}
		}
		if (string.equals("wt.org.WTPrincipal") || string.equals("wt.org.WTGroup")) {
			vector2 = (Vector) MethodContext.getContext().get(WF_GROUPS);
			vector = (Vector) MethodContext.getContext().get(WF_DISPLAY_GROUPS);
			if (vector2 == null || vector == null) {
				enumeration = new SortedEnumeration(OrganizationServicesMgr.allGroups(),
						new CollationKeyFactory(WTContext.getContext().getLocale()));
				while (enumeration.hasMoreElements()) {
					wTPrincipal2 = (WTGroup) enumeration.nextElement();
					vector3.addElement(wTPrincipal2.getName());
					vector4.addElement(wTPrincipal2.getName());
				}
				MethodContext.getContext().put(WF_GROUPS, vector4);
				MethodContext.getContext().put(WF_DISPLAY_GROUPS, vector3);
			} else {
				vector4 = vector2;
				vector3 = vector;
			}
		}
		int n = wTPrincipal == null ? 0 : vector3.indexOf(wTPrincipal.getName()) + 1;
		return HtmlUtil.createSelectHTML(string2, null, vector3, vector4, true, n);
	}

	public String enumeratedTypeSelector(Class clazz, EnumeratedType enumeratedType, String string, Locale locale)
			throws WTException {
		Vector<String> vector = new Vector<String>();
		Vector<String> vector2 = new Vector<String>();
		int n = 0;
		Object object = "<select name=\"" + string + "\" size=\"1\">";
		object = ((String) object).concat("<option value = > </option>\n");
		try {
			String string2 = this.getSimpleName(clazz);
			Method method = null;
			method = clazz.getMethod("get" + string2 + "Set", null);
			Object object2 = method.invoke(null, null);
			EnumeratedType[] enumeratedTypeArray = (EnumeratedType[]) object2;
			int n2 = 0;
			for (int i = 0; i < enumeratedTypeArray.length; ++i) {
				if (!enumeratedTypeArray[i].isSelectable())
					continue;
				++n2;
				vector.addElement(enumeratedTypeArray[i].toString());
				vector2.addElement(enumeratedTypeArray[i].getDisplay(locale));
				if (!enumeratedTypeArray[i].equals(enumeratedType))
					continue;
				n = n2;
			}
		} catch (InvocationTargetException invocationTargetException) {
			logger.error("", (Throwable) invocationTargetException);
		} catch (NoSuchMethodException noSuchMethodException) {
			logger.error("", (Throwable) noSuchMethodException);
		} catch (IllegalAccessException illegalAccessException) {
			logger.error("", (Throwable) illegalAccessException);
		}
		return HtmlUtil.createSelectHTML(string, null, vector, vector2, true, n);
	}

	protected String getSimpleName(Class clazz) {
		char[] cArray = clazz.getName().toCharArray();
		String string = null;
		for (int i = cArray.length - 1; i > 0; --i) {
			if (cArray[i] != '.')
				continue;
			string = clazz.getName().substring(i + 1);
			break;
		}
		return string;
	}

	public void messageText(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getMessage());
		printWriter.flush();
	}

	public void projectLinkAttributes(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException, WTPropertyVetoException {
		WTContainer wTContainer;
		CachedObjectReference cachedObjectReference = null;
		Object object = this.getContextObj();
		if (object instanceof WTContained) {
			cachedObjectReference = ((WTContained) object).getContainerReference();
		}
		if (!((wTContainer = (WTContainer) cachedObjectReference.getObject()) instanceof Project2)) {
			throw new WTRuntimeException("Wrong context: expected ProjectLink got " + wTContainer.getClass().getName());
		}
		this.projectLinkAttributes(properties, locale, outputStream, (Project2) wTContainer);
	}

	public void projectLinkAttributes(Properties properties, Locale locale, OutputStream outputStream,
			Project2 project2) throws WTException, WTPropertyVetoException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		NetmarketsHref netmarketsHref = new NetmarketsHref(
				new ReferenceFactory().getReference(project2.getPersistInfo().getObjectIdentifier().getStringValue()));
		netmarketsHref.setFullyQualified(true);
		String string = netmarketsHref.getHref();
		if (logger.isDebugEnabled()) {
			logger.debug("   .....Project: " + HtmlUtil.createInlineLink(string, "", project2.getName()));
		}
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(WTMessage.getLocalizedMessage(RESOURCE, "200", null, locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		printWriter.println(project2.getName());
		printWriter.println("</font></td></tr>\n");
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(WTMessage.getLocalizedMessage(RESOURCE, "201a", null, locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		if (project2.getOwnerReference() != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("   .....Project Manager: " + project2.getOwnerReference().getFullName());
			}
			printWriter.println(project2.getOwnerReference().getFullName() + "</a>");
		} else {
			printWriter.println(WTMessage.getLocalizedMessage(RESOURCE, "204", null, locale));
		}
		printWriter.println("</font></td></tr>");
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(WTMessage.getLocalizedMessage(RESOURCE, "202", null, locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(project2);
		printWriter.println(orgContainer != null ? orgContainer.getName() : "&nbsp;");
		printWriter.println("</font></td></tr>\n");
		if (logger.isDebugEnabled()) {
			logger.debug("   .....Project Description: " + project2.getDescription());
		}
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(WTMessage.getLocalizedMessage(RESOURCE, "203", null, locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		String string2 = project2.getDescription();
		if (string2 != null && string2.trim().length() > 0) {
			printWriter.println(this.translateToHtml(string2));
		} else {
			printWriter.println(WTMessage.getLocalizedMessage(RESOURCE, "204", null, locale));
		}
		printWriter.println("</font></td></tr>\n");
		printWriter.println("<tr><td colspan=\"2\"><hr size=\"1\" color=\"#40637A\"></td></tr>");
		printWriter.flush();
	}

	protected String formatLabel(String string) {
		return string + new WTMessage(RESOURCE, "3", null).getLocalizedMessage() + "&nbsp;";
	}

	public void enterComments(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		WfActivity wfActivity = null;
		wfActivity = (WfActivity) this.workItem.getSource().getObject();
		if (((WfAssignedActivityTemplate) wfActivity.getTemplateReference().getObject()).isSigningRequired()) {
			String string = this.getContextAction();
			PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
			printWriter.println(new WTMessage(LIFECYCLE_RESOURCE, "99", null).getLocalizedMessage(locale)
					+ new WTMessage(LIFECYCLE_RESOURCE, "150", null).getLocalizedMessage(locale)
					+ "<BR><textarea name=\"comments\" rows=\"10\" cols=\"60\"></textarea>");
			printWriter.flush();
		}
	}

	public void setMessage(String string) {
		this.messageText = string;
	}

	public String getMessage() {
		return this.messageText;
	}

	public String getGroupLabel(Locale locale) throws WTException {
		return this
				.formatLabel(new WTMessage("wt.workflow.definer.DefinerRB", "216", null).getLocalizedMessage(locale));
	}

	public void groupLabel(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getGroupLabel(locale));
		printWriter.flush();
	}

	public String getGroup() throws WTException {
		WfAssignee wfAssignee;
		String string = "&nbsp;";
		ReferenceFactory referenceFactory = new ReferenceFactory();
		WTPrincipalReference wTPrincipalReference = (WTPrincipalReference) referenceFactory
				.getReference(OwnershipHelper.getOwner(this.getWorkItem()));
		WTPrincipal wTPrincipal = (WTPrincipal) wTPrincipalReference.getObject();
		if (wTPrincipal instanceof WTUser
				&& (wfAssignee = ((WfAssignment) this.getWorkItem().getParentWA().getObject())
						.getAssignee()) instanceof WfPrincipalAssignee
				&& ((WfPrincipalAssignee) wfAssignee).getPrincipal().getObject() instanceof WTGroup) {
			string = ((WTGroup) ((WfPrincipalAssignee) wfAssignee).getPrincipal().getObject()).getName();
		}
		return string;
	}

	public void group(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.translateToHtml(this.getGroup()));
		printWriter.flush();
	}

	public boolean isWorkItemOwner() {
		boolean bl = false;
		try {
			WTGroup wTGroup;
			WTPrincipal wTPrincipal;
			WTPrincipal wTPrincipal2 = SessionHelper.manager.getPrincipal();
			WorkItem workItem = this.getWorkItem();
			bl = OwnershipHelper.isOwnedBy(workItem, wTPrincipal2);
			if (!bl && (wTPrincipal = OwnershipHelper.getOwner(workItem)) instanceof WTGroup
					&& (wTGroup = (WTGroup) wTPrincipal).isMember(wTPrincipal2)) {
				bl = true;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("=> WfTaskProcessor.isWorkItemOwnwer" + wTPrincipal2.getName() + " is "
						+ (bl ? "" : " not") + "the owner of " + workItem);
			}
		} catch (WTException wTException) {
			// empty catch block
		}
		return bl;
	}

	public void processNotebook(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		Object object;
		WfProcess wfProcess = this.getProcess();
		Object object2 = "";
		ObjectIdentifier objectIdentifier = PersistenceHelper.getObjectIdentifier(wfProcess);
		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("processOid", objectIdentifier.toString());
		try {
			object = (WTObject) this.getProcess().getContext().getValue(PBO_CONTEXT);
			if (object == null || !(object instanceof SubjectOfNotebook)) {
				object = wfProcess;
			}
			NetmarketsHref netmarketsHref = new NetmarketsHref(NetmarketsType.notebookfolder, NetmarketsCommand.list,
					new ReferenceFactory()
							.getReference(PersistenceHelper.getObjectIdentifier(wfProcess).getStringValue()));
			String string = netmarketsHref.getHref();
			String string2 = new WTMessage(NOTEBOOK_RESOURCE, "0", null).getLocalizedMessage(locale);
			String string3 = "><IMG SRC=\"" + CODEBASE + "/" + NOTEBOOK_GIF + "\" border=\"0\" alt=\"" + string2 + "\"";
			object2 = this.pdmlContext ? " <A HREF=\"javascript:var nbw = wfWindowOpen('" + string
					+ "','nmnotebooklist',config='resizable=yes,scrollbars=yes,menubar=yes,toolbar=yes,location=yes,status=yes')\" class=\"detailsLink\" >"
					+ string2 + "</A>"
					: " <A HREF=\"javascript:var nbw = window.open ('" + string
							+ "','nmnotebooklist',config='resizable=yes,scrollbars=yes,menubar=yes,toolbar=yes,location=yes,status=yes')\""
							+ string3 + ">" + string2 + "</A>";
			if (logger.isDebugEnabled()) {
				logger.debug("\n WfTaskProcessor.processNotebook (): link  == " + string);
				logger.debug("\n WfTaskProcessor.processNotebook (): finalLink == " + (String) object2);
			}
		} catch (Exception exception) {
			logger.debug("", (Throwable) exception);
		}
		object = this.getPrintWriter(outputStream, locale);
		((PrintWriter) object).println((String) object2);
		((PrintWriter) object).flush();
	}

	public void processForum(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		WfProcess wfProcess = this.getProcess();
		NetmarketsHref netmarketsHref = new NetmarketsHref(NetmarketsType.forum, NetmarketsCommand.discuss,
				new ReferenceFactory().getReference(wfProcess.getPersistInfo().getObjectIdentifier().getStringValue()));
		netmarketsHref.setFullyQualified(true);
		String string = netmarketsHref.getHref();
		String string2 = "";
		Object object = "";
		Object object2 = "";
		if (this.pdmlContext) {
			string2 = new WTMessage(RESOURCE, "165", null).getLocalizedMessage(locale);
			object2 = "<A HREF=\"javascript:var forumWindow=wfWindowOpen('" + string
					+ "','nmforumview',config='resizable=yes,scrollbars=yes,menubar=yes,toolbar=yes,location=yes,status=yes')\" class=\"detailsLink\">"
					+ string2 + "</A>";
		} else {
			string2 = new WTMessage(FORUM_RESOURCE, "27", null).getLocalizedMessage(locale);
			object = "><IMG SRC=\"" + CODEBASE + "/" + FORUM_GIF + "\" border=\"0\" alt=\"" + string2 + "\"";
			object2 = "<A HREF=\"javascript:var forumWindow=window.open ('" + string
					+ "','nmforumview',config='resizable=yes,scrollbars=yes,menubar=yes,toolbar=yes,location=yes,status=yes')\""
					+ (String) object + ">" + string2 + "</A>";
		}
		printWriter.println((String) object2);
		printWriter.flush();
	}

	public void PJLProjectLabel(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.formatLabel(new WTMessage(RESOURCE, "200", null).getLocalizedMessage(locale)));
		printWriter.flush();
	}

	public void PJLProject(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		WTContainer wTContainer;
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		CachedObjectReference cachedObjectReference = null;
		Object object = this.getContextObj();
		if (object instanceof WTContained) {
			cachedObjectReference = ((WTContained) object).getContainerReference();
		}
		if (!((wTContainer = (WTContainer) cachedObjectReference.getObject()) instanceof Project2)) {
			throw new WTRuntimeException("Wrong context: expected ProjectLink got " + wTContainer.getClass().getName());
		}
		Project2 project2 = (Project2) wTContainer;
		NetmarketsHref netmarketsHref = new NetmarketsHref(
				new ReferenceFactory().getReference(project2.getPersistInfo().getObjectIdentifier().getStringValue()));
		netmarketsHref.setFullyQualified(true);
		String string = netmarketsHref.getHref();
		printWriter.println(string);
		printWriter.flush();
	}

	public void PJLProjectSponsorLabel(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.formatLabel(new WTMessage(RESOURCE, "201", null).getLocalizedMessage(locale)));
		printWriter.flush();
	}

	public void PJLProjectSponsor(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		WTContainer wTContainer;
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		CachedObjectReference cachedObjectReference = null;
		Object object = this.getContextObj();
		if (object instanceof WTContained) {
			cachedObjectReference = ((WTContained) object).getContainerReference();
		}
		if (!((wTContainer = (WTContainer) cachedObjectReference.getObject()) instanceof Project2)) {
			throw new WTRuntimeException("Wrong context: expected ProjectLink got " + wTContainer.getClass().getName());
		}
		Project2 project2 = (Project2) wTContainer;
		if (project2.getSponsor() != null) {
			printWriter.println("<a href=\"mailto:" + project2.getSponsor().getEMail() + "\">");
			printWriter.println(project2.getSponsor().getFullName() + "</a>");
		} else {
			printWriter.println(WTMessage.getLocalizedMessage(RESOURCE, "204", null, locale));
		}
		printWriter.flush();
	}

	public void PJLOrganizationHostLabel(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.formatLabel(new WTMessage(RESOURCE, "202", null).getLocalizedMessage(locale)));
		printWriter.flush();
	}

	public void PJLOrganizationHost(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		WTContainer wTContainer;
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		CachedObjectReference cachedObjectReference = null;
		Object object = this.getContextObj();
		if (object instanceof WTContained) {
			cachedObjectReference = ((WTContained) object).getContainerReference();
		}
		if (!((wTContainer = (WTContainer) cachedObjectReference.getObject()) instanceof Project2)) {
			throw new WTRuntimeException("Wrong context: expected ProjectLink got " + wTContainer.getClass().getName());
		}
		Project2 project2 = (Project2) wTContainer;
		OrgContainer orgContainer = WTContainerHelper.service.getOrgContainer(project2);
		printWriter.println(orgContainer != null ? orgContainer.getName() : "&nbsp;");
		printWriter.flush();
	}

	public void PJLProjectDescriptionLabel(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.formatLabel(new WTMessage(RESOURCE, "203", null).getLocalizedMessage(locale)));
		printWriter.flush();
	}

	public void PJLProjectDescription(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		WTContainer wTContainer;
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		CachedObjectReference cachedObjectReference = null;
		Object object = this.getContextObj();
		if (object instanceof WTContained) {
			cachedObjectReference = ((WTContained) object).getContainerReference();
		}
		if (!((wTContainer = (WTContainer) cachedObjectReference.getObject()) instanceof Project2)) {
			throw new WTRuntimeException("Wrong context: expected ProjectLink got " + wTContainer.getClass().getName());
		}
		Project2 project2 = (Project2) wTContainer;
		String string = project2.getDescription();
		if (string != null && string.trim().length() > 0) {
			printWriter.println(this.translateToHtml(string));
		} else {
			printWriter.println(WTMessage.getLocalizedMessage(RESOURCE, "204", null, locale));
		}
		printWriter.flush();
	}

	public String getRequestedPromotionStateLabel(Locale locale) throws WTException {
		return this.formatLabel(new WTMessage(RESOURCE, "223", null).getLocalizedMessage(locale));
	}

	private boolean check_for_PromotionRequestTask() {
		boolean bl = false;
		String string = ((WfAssignedActivityTemplate) this.getActivity().getTemplateReference().getObject())
				.getTaskName();
		if (string.equals("WfPromotionRequestTask") || string.equals("WfTask")) {
			bl = true;
		}
		return bl;
	}

	public String getPriorityLabel(Locale locale) throws WTException {
		return this.formatLabel(new WTMessage(RESOURCE, "42", null).getLocalizedMessage(locale));
	}

	public void priorityLabel(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.getPriorityLabel(locale));
		printWriter.flush();
	}

	public String getPriority(Locale locale) throws WTException {
		String string = this.getWorkItem().getPriority();
		switch (Integer.parseInt(string)) {
		case 1: {
			return new WTMessage(RESOURCE, "59", null).getLocalizedMessage(locale);
		}
		case 2: {
			return new WTMessage(RESOURCE, "60", null).getLocalizedMessage(locale);
		}
		case 3: {
			return new WTMessage(RESOURCE, "61", null).getLocalizedMessage(locale);
		}
		case 4: {
			return new WTMessage(RESOURCE, "62", null).getLocalizedMessage(locale);
		}
		case 5: {
			return new WTMessage(RESOURCE, "63", null).getLocalizedMessage(locale);
		}
		}
		return "";
	}

	public void priority(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println(this.translateToHtml(this.getPriority(locale)));
		printWriter.flush();
	}

	public void activityNameFull(Properties properties, Locale locale, OutputStream outputStream) throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		String string = "";
		try {
			WTObject wTObject = (WTObject) this.getProcess().getContext().getValue(PBO_CONTEXT);
			if (wTObject != null) {
				string = wTObject.getDisplayIdentity().toString();
			}
		} catch (WTRuntimeException wTRuntimeException) {
			// empty catch block
		}
		printWriter.print(WTMessage.getLocalizedMessage(RESOURCE, "163",
				new Object[] { this.getActivity().getName(), string }, locale));
		printWriter.flush();
	}

	private void setPDMLinkContext(WTContainer wTContainer) throws WTException {
		if (PDM_INSTALLED && (wTContainer instanceof WTLibrary || wTContainer instanceof PDMLinkProduct
				|| wTContainer instanceof OrgContainer || wTContainer instanceof ExchangeContainer)) {
			this.pdmlContext = true;
		}
	}

	private void setProjectLinkContext(WTContainer wTContainer) throws WTException {
		if (wTContainer instanceof Project2) {
			this.pjlContext = true;
		}
	}

	protected void setContext() throws WTException {
		WTContainerRef wTContainerRef = null;
		Object object = this.getContextObj();
		if (object instanceof WTContained) {
			wTContainerRef = ((WTContained) object).getContainerReference();
		}
		try {
			if (wTContainerRef == null) {
				System.out.println("In WfTaskProcessor.setContext for getExchangeRef");
				wTContainerRef = WTContainerHelper.service.getExchangeRef();
			}
			WTContainer wTContainer = (WTContainer) wTContainerRef.getObject();
			this.setProjectLinkContext(wTContainer);
			this.setPDMLinkContext(wTContainer);
			this.contextSet = true;
		} catch (WTException wTException) {
			logger.debug("Exception occurred while setting context: ", (Throwable) wTException);
			this.contextSet = false;
		}
	}

	public void notificationActivityAttributes(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException, WTPropertyVetoException {
		Object object;
		Object object2;
		Object object3;
		WTReference wTReference = this.getProcess().getBusinessObjectReference(new ReferenceFactory());
		PromotionNotice promotionNotice = null;
		if (wTReference != null && PromotionNotice.class.isAssignableFrom(wTReference.getReferencedClass())) {
			promotionNotice = (PromotionNotice) wTReference.getObject();
		}
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		printWriter.println("<table border=\"0\" cellpadding=\"3\" cellspacing=\"0\">");
		printWriter.println("<tr><td colspan=\"2\"><hr size=\"1\" color=\"#40637A\"></td></tr>");
		String string = WorkflowHtmlUtil.getExternalTaskURL(this.getWorkItem());
		if (string != null) {
			object3 = this.formatLabel(new WTMessage(RESOURCE, "40", null).getLocalizedMessage(locale));
			object2 = "(" + new WTMessage(RESOURCE, "NAVIGATE_LABEL", null).getLocalizedMessage(locale) + ")";
			object = "(" + new WTMessage(RESOURCE, "WINDCHILL_LABEL", null).getLocalizedMessage(locale) + ")";
			printWriter.println(
					"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
			printWriter.println((String) object3);
			printWriter.println("</b></font></td>");
			printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
			String string2 = "<A id=\"WfExternalTaskTProcessor02\" HREF=\""
					+ HTMLEncoder.encodeForHTMLAttribute((String) string) + "\">"
					+ HTMLEncoder.encodeForHTMLContent((String) this.myActivity.getName()) + "</A>";
			printWriter.println(this.translateToHtml(string2 + (String) object2));
			printWriter.println("</font></td></tr>");
			String string3 = "<A id=\"WfExternalTaskTProcessor03\" HREF=\"" + this.getTaskUrl(this.getContextRef())
					+ "\">" + HTMLEncoder.encodeForHTMLContent((String) this.myActivity.getName()) + "</A>";
			printWriter.println(
					"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
			printWriter.println("");
			printWriter.println("</b></font></td>");
			printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
			printWriter.println(this.translateToHtml(string3 + (String) object));
			printWriter.println("</font></td></tr>");
		}
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(this.getActivityInstructionsLabel(locale));
		printWriter.println("</b></font></td>");
		printWriter.println(
				"<td align=\"left\" colspan=\"4\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		object3 = SessionContext.getEffectivePrincipal();
		try {
			SessionContext.setEffectivePrincipal(this.getWorkItem().getOwnership().getOwner().getPrincipal());
			printWriter.println(this.getActivityInstructionsAsHtml());
		} catch (Exception exception) {
			throw new WTException(exception);
		} finally {
			SessionContext.setEffectivePrincipal((WTPrincipal) object3);
		}
		printWriter.println("</font></td></tr>");
		if (promotionNotice != null) {
			printWriter.println(
					"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
			printWriter.println(this.getRequestedPromotionStateLabel(locale));
			printWriter.println("</b></font></td>");
			printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
			printWriter.println(this.translateToHtml(
					promotionNotice.getMaturityState() != null ? promotionNotice.getMaturityState().getDisplay(locale)
							: ""));
			printWriter.println("</font></td></tr>");
		}
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(this.getProcessInitiatorLabel(locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		printWriter.println(
				this.translateToHtml(HTMLEncoder.encodeAndFormatForHTMLContent((String) this.getProcessInitiator())));
		printWriter.println("</font></td></tr>");
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(this.getDueDateLabel(locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		printWriter.println(this.getDueDate(locale));
		printWriter.println("</font></td></tr>");
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(this.getWorkItemRoleLabel(locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		printWriter.println(this.getWorkItemRole(locale));
		printWriter.println("</font></td></tr>");
		printWriter.println(
				"<tr><td align=\"right\" valign=\"top\" nowrap><font face=\"Arial, Helvetica, sans-serif\"><b>");
		printWriter.println(this.getAssigneeLabel(locale));
		printWriter.println("</b></font></td>");
		printWriter.println("<td align=\"left\" valign=\"top\"><font face=\"Arial, Helvetica, sans-serif\">");
		printWriter
				.println(this.translateToHtml(HTMLEncoder.encodeAndFormatForHTMLContent((String) this.getAssignee())));
		printWriter.println("</font></td></tr>");
		printWriter.println("<tr><td colspan=\"2\"><hr size=\"1\" color=\"#40637A\"></td></tr>");
		object2 = this.getContextObj();
		this.setContextObj(this.getActivity());
		object = new Properties();
		((Properties) object).put("service", "wt.workflow.work.WorkProcessorService");
		((Properties) object).put("method", "projectLinkAttributes");
		this.useProcessorService((Properties) object, locale, outputStream);
		this.setContextObj(object2);
		printWriter.println("</table>");
		printWriter.flush();
	}

	public void activityNotificationUrl(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException, WTPropertyVetoException {

		String string = null;
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);

		if (this.workItemExists()) {
			String string2;
			WTPrincipal object;
			String object2 = "";

			WorkItem workItem = this.getWorkItem();
			ObjectReference objectReference = workItem.getOrigOwner();

			if (objectReference != null && objectReference.getObject() != null) {
				object = (WTUser) this.getWorkItem().getOrigOwner().getObject();
				Object[] args = new Object[] { this.getPrincipalEmailLink((WTPrincipal) object, null) };
				string2 = new WTMessage(RESOURCE, "206", args).getLocalizedMessage(locale);
				object2 = this.translateToHtml(string2) + "<BR><BR>";
			}

			String externalTaskUrl = WorkflowHtmlUtil.getExternalTaskURL(workItem);

			if (externalTaskUrl != null) {
				string = new WTMessage(RESOURCE, "NOTIFICATION_EXTERNAL_TASK_LABEL", null).getLocalizedMessage(locale);
				printWriter.println(object2 + string);
				printWriter.println("<br>");
			} else {
				Object[] objectArray = new Object[] { this.getTaskUrl(this.getContextRef()),
						HTMLEncoder.encodeAndFormatForHTMLContent(this.getActivity().getName()) };
				string = new WTMessage(RESOURCE, "205", objectArray).getLocalizedMessage(locale);
				printWriter.println(object2 + this.translateToHtml(string));
			}
		} else {
			string = new WTMessage(RESOURCE, "230", new Object[0]).getLocalizedMessage(locale);
			printWriter.println(this.translateToHtml(string));
		}

		printWriter.flush();
	}

	public void processReassignmentHistory(Properties properties, Locale locale, OutputStream outputStream)
			throws WTException {
		PrintWriter printWriter = this.getPrintWriter(outputStream, locale);
		WfProcess wfProcess = this.getProcess();
		NetmarketsHref netmarketsHref = new NetmarketsHref(NetmarketsType.workflow,
				NetmarketsCommand.reassignmentHistory,
				new ReferenceFactory().getReference(wfProcess.getPersistInfo().getObjectIdentifier().getStringValue()));
		String string = netmarketsHref.getHref();
		String string2 = "";
		Object object = "";
		string2 = new WTMessage(RESOURCE, "166", null).getLocalizedMessage(locale);
		object = "<A HREF=\"javascript:var historyWindow=wfWindowOpen('" + string
				+ "','nmReassignmentHistoryview',config='resizable=yes,scrollbars=yes,menubar=yes,toolbar=yes,location=yes,status=yes')\" class=\"detailsLink\">"
				+ string2 + "</A>";
		printWriter.println((String) object);
		printWriter.flush();
	}

	public String escapeHtml(String string) {
		return HTMLEncoder.encodeAndFormatForHTMLContent((String) string);
	}

	protected WTContainerRef getContextRef() throws WTException {
		WfActivity wfActivity = this.getActivity();
		WTContainerRef wTContainerRef = null;
		if (wfActivity instanceof WTContained) {
			wTContainerRef = ((WTContained) wfActivity).getContainerReference();
		}
		if (wTContainerRef == null) {
			System.out.println("In WfTaskProcessor.getContext for getExchangeRef");
			wTContainerRef = WTContainerHelper.service.getExchangeRef();
		}
		return wTContainerRef;
	}

	private String getPrincipalFName(WTPrincipal wTPrincipal) {
		String string = "";
		if (wTPrincipal != null) {
			try {
				ReferenceFactory referenceFactory = new ReferenceFactory();
				WTReference wTReference = referenceFactory.getReference(wTPrincipal);
				string = ((WTPrincipalReference) wTReference).getDisplayName();
			} catch (WTRuntimeException wTRuntimeException) {
				Throwable throwable = wTRuntimeException.getNestedThrowable();
				if (throwable instanceof NotAuthorizedException) {
					string = "";
				}
				throw wTRuntimeException;
			} catch (WTException wTException) {
				string = "";
			}
		}
		return string;
	}

	protected String getTaskUrl(WTContainerRef wTContainerRef) throws WTException {
		NetmarketsHref netmarketsHref = new NetmarketsHref(new ReferenceFactory()
				.getReference(this.getWorkItem().getPersistInfo().getObjectIdentifier().getStringValue()));
		netmarketsHref.setFullyQualified(true);
		return netmarketsHref.getHref();
	}

	static {
		FORMATTED = "formatted";
		DEFAULT_FORMATTED_VALUE = "true";
		COLUMNS = "columns";
		DEFAULT_COLUMN_NUMBER = "60";
		DEFAULT_ROW_NUMBER = "7";
		ROWS = "rows";
		PDM_INSTALLED = false;
		QMS_INSTALLED = false;
		try {
			WTProperties wTProperties = WTProperties.getLocalProperties();
			VERBOSE = wTProperties.getProperty("wt.workflow.verbose", false);
			PLMLINK = wTProperties.getProperty("com.ptc.netmarkets.showPLMLink", false);
			CODEBASE = wTProperties.getProperty("wt.server.codebase", "");
			ESCAPE_HTML = wTProperties.getProperty("wt.workflow.worklist.escapeHtml", "scriptonly");
			PDM_INSTALLED = InstalledProperties.isInstalled("pdmSystem");
			QMS_INSTALLED = InstalledProperties.isInstalled("Windchill.QualityManagement.QMS");
			rb = ResourceBundle.getBundle(RESOURCE, WTContext.getContext().getLocale());
			if (VERBOSE && !logger.isDebugEnabled()) {
				Configurator.setLevel((String) logger.getName(), (Level) Level.DEBUG);
			}
		} catch (Throwable throwable) {
			throw new ExceptionInInitializerError(throwable);
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
						var6.println("<td align='center'>" + getIBAValue(part, "PART_NAME") + "</td>");
						var6.println("<td align='center'>" + getIBAValue(part, "DESCRIPTION_1") + "</td>");
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
						var6.println("<td align='center'>" + getIBAValue(doc, "PART_NAME") + "</td>");
						var6.println("<td align='center'>" + getIBAValue(doc, "DESCRIPTION_1") + "</td>");
						var6.println("</tr>");
					} else if (obj instanceof EPMDocument) {
						EPMDocument epm = (EPMDocument) obj;
						subType = epm.getDisplayType().getLocalizedMessage(null);
						href = getURLOfObject(obj);
						href = "<a href='" + href + "'>" + epm.getNumber() + "</a>";
						var6.println("<tr>");
						var6.println("<td align='center'>" + subType + "</td>");
						var6.println("<td align='center'>" + href + "</td>");
						var6.println("<td align='center'>" + epm.getName() + "</td>");
						var6.println("<td align='center'>" + state + "</td>");
						var6.println("<td align='center'>" + epm.getIterationDisplayIdentifier() + "</td>");
						var6.println("<td align='center'>" + getIBAValue(epm, "PART_NAME") + "</td>");
						var6.println("<td align='center'>" + getIBAValue(epm, "DESCRIPTION_1") + "</td>");
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

	public static Map<WTObject, String> getPromotedFromStates(PromotionNotice pn)
			throws MaturityException, WTException {
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