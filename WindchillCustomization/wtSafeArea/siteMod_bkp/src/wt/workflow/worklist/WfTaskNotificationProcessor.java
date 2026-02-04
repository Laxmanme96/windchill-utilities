package wt.workflow.worklist;

import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import wt.fc.ObjectIdentifier;
import wt.fc.ObjectNoLongerExistsException;
import wt.fc.PersistenceHelper;
import wt.htmlutil.StringOutputStream;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.util.WTRuntimeException;
import wt.util.WTStandardDateFormat;
import wt.util.utilResource;
import wt.workflow.WorkflowNotificationTemplateProcessor;
import wt.workflow.engine.WfActivity;
import wt.workflow.engine.WfExecutionObject;
import wt.workflow.engine.WfProcess;
import wt.workflow.work.WorkItem;
import wt.workflow.work.workResource;

public class WfTaskNotificationProcessor extends WorkflowNotificationTemplateProcessor {
	private WfTaskProcessor taskProcessor;

	public WfTaskNotificationProcessor() {
		this.taskProcessor = new WfTaskProcessor();
	}

	public WfTaskNotificationProcessor(Object var1) {
		this();
		this.setContextObj(var1);
		this.setWorkItem((WorkItem) var1);
	}

	public void setWorkItem(WorkItem var1) {
		this.workItem = var1;
		this.taskProcessor.setWorkItem(var1);
		if (this.activity == null) {
			this.getActivity();
		}

		if (this.process == null) {
			this.getProcess();
		}

	}

	public WorkItem getWorkItem() {
		Object var1 = super.getContextObj();
		if (this.workItem == null && var1 != null && var1 instanceof WorkItem) {
			this.setWorkItem((WorkItem) var1);
		}

		return this.workItem;
	}

	public void setActivity(WfActivity var1) {
		this.activity = var1;
	}

	public WfActivity getActivity() {
		if (this.activity == null && this.getWorkItem() != null) {
			this.activity = (WfActivity) this.workItem.getSource().getObject();
		}

		this.setActivity(this.activity);
		return this.activity;
	}

	public WfProcess getProcess() {
		try {
			if (this.process == null && this.getActivity() != null) {
				this.process = this.activity.getParentProcess();
			}

			return this.process;
		} catch (WTException var2) {
			throw new WTRuntimeException(var2);
		}
	}

	public String getTitleKey() {
		return "139";
	}

	public String getResourceClassName() {
		return workResource.class.getName();
	}

	public void getStyleSheetProps(Properties var1, Locale var2, OutputStream var3) throws WTException {
		this.getWorkItem();
		this.taskProcessor.getStyleSheetProps(var1, var2, var3);
	}

	public void activityAttributes(Properties var1, Locale var2, OutputStream var3)
			throws WTException, Exception {
		if (!(var3 instanceof StringOutputStream)) {
			this.taskProcessor.activityAttributes(var1, var2, var3);
		} else if (logger.isDebugEnabled()) {
			logger.debug("WfTaskNotificationProcessor.activityAttributes is not supported in subject");
		}

	}
	
	
	public void emersonAffectedObjects(Properties var1, Locale var2, OutputStream var3)
			throws WTException, Exception {
		if (!(var3 instanceof StringOutputStream)) {
			logger.debug(" ********************** WfTaskNotificationProcessor.emersonAffectedObjects Started");
			this.taskProcessor.emersonAffectedObjects(var1, var2, var3);
		} else if (logger.isDebugEnabled()) {
			logger.debug("WfTaskNotificationProcessor.emersonAffectedObjects is not supported in subject");
		}

	}

	public void activityNotificationUrl(Properties var1, Locale var2, OutputStream var3)
			throws WTException, WTPropertyVetoException {
		this.getWorkItem();
		if (!(var3 instanceof StringOutputStream)) {
			this.taskProcessor.activityNotificationUrl(var1, var2, var3);
		} else if (logger.isDebugEnabled()) {
			logger.debug("WfTaskNotificationProcessor.activityNotificationUrl is not supported in subject");
		}

	}

	public void getDeadline(Properties var1, Locale var2, OutputStream var3) {
		ResourceBundle var4 = ResourceBundle.getBundle(utilResource.class.getName(), var2);
		String var5 = var4.getString("3");
		WfExecutionObject var6 = null;

		try {
			ObjectIdentifier var7 = PersistenceHelper.getObjectIdentifier(this.getActivity());
			var6 = (WfExecutionObject) PersistenceHelper.manager.refresh(var7);
		} catch (ObjectNoLongerExistsException var9) {
			if (VERBOSE) {
				var9.printStackTrace(System.err);
			}
		} catch (WTException var10) {
			if (VERBOSE) {
				var10.printStackTrace(System.err);
			}
		}

		Timestamp var11 = var6.getDeadline();
		if (var11 != null) {
			String var8 = WTStandardDateFormat.format(var11, var5);
			this.processValues(var2, var3, var8);
		}

	}
}