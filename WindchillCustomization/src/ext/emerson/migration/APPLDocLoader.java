package ext.emerson.migration;

import java.beans.PropertyVetoException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.logging.log4j.Logger;

import com.ptc.core.lwc.server.LoadAttValues;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.core.meta.type.mgmt.server.impl.association.AssociationConstraintHelper;

import wt.access.AccessControlServerHelper;
import wt.access.SecurityLabeled;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentItem;
import wt.content.ContentServerHelper;
import wt.doc.DepartmentList;
import wt.doc.DocumentType;
import wt.doc.WTDocument;
import wt.doc.WTDocumentHelper;
import wt.doc.WTDocumentMaster;
import wt.doc.WTDocumentUsageLink;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.folder.FolderNotFoundException;
import wt.iba.value.IBAHolder;
import wt.iba.value.service.LoadValue;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainerRef;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.LifeCycleServerHelper;
import wt.lifecycle.State;
import wt.load.LoadContent;
import wt.load.LoadServerHelper;
import wt.log4j.LogR;
import wt.method.MethodContext;
import wt.org.OrganizationServicesHelper;
import wt.org.WTPrincipalReference;
import wt.org.WTUser;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.series.MultilevelSeries;
import wt.series.Series;
import wt.session.SessionMgr;
import wt.team.TeamHelper;
import wt.team.TeamManaged;
import wt.type.TypeManaged;
import wt.type.Typed;
import wt.ufid.FederatableInfo;
import wt.util.WTContext;
import wt.util.WTException;
import wt.util.WTInvalidParameterException;
import wt.util.WTMessage;
import wt.util.WTProperties;
import wt.util.WTPropertyVetoException;
import wt.vc.Iterated;
import wt.vc.IterationIdentifier;
import wt.vc.IterationInfo;
import wt.vc.Mastered;
import wt.vc.VersionControlHelper;
import wt.vc.VersionControlServerHelper;
import wt.vc.VersionIdentifier;
import wt.vc.Versioned;
import wt.vc.config.LatestConfigSpec;
import wt.vc.wip.CheckoutLink;
import wt.vc.wip.WorkInProgressHelper;

public class APPLDocLoader {
	private static final Logger LOGGER = LogR.getLoggerInternal("ext.emerson.migration");
	private static String CURRENT_CONTENT_HOLDER = "Current ContentHolder";
	private static final String DOCUMENT_CACHE_KEY = "DOCUMENT_CACHE_KEY:";
	private static final String DOCUMENT_PREVIOUS_USER = "DOCUMENT_PREVIOUS_USER:";
	private static final String ASSOCIATION_RESOURCE = "com.ptc.core.meta.type.mgmt.server.impl.association.associationResource";
	private static final String DOCUMENT_NEW_VERSION = "DOCUMENT_NEW_VERSION";
	private static String CURRENT_DOCUMENT = "Current Document";
	private static String RESOURCE = "wt.doc.docResource";
	private static ResourceBundle rb;
	public static boolean VERBOSE;
	private static String DOCRESOURCE = "ext.emerson.migration.docResource";


	public static boolean createGeneral(Hashtable var0, Hashtable var1, Vector var2) {
		return createDocumentObject(var0, var1, var2) && updateDocumentObject(var0, var1, var2);
	}

	public static boolean createDocumentRetainContent(Hashtable var0, Hashtable var1, Vector var2) {
		return createDocumentObject(var0, var1, var2, false) && updateDocumentObject(var0, var1, var2);
	}

	public static boolean beginCreateWTDocument(Hashtable var0, Hashtable var1, Vector var2) {
		resetAttDirectiveFlags();
		return createDocumentObject(var0, var1, var2);
	}

	public static boolean endCreateWTDocument(Hashtable var0, Hashtable var1, Vector var2) {
		return updateDocumentObject(var0, var1, var2);
	}

	public static boolean beginCreateWTDocumentRetainContent(Hashtable var0, Hashtable var1, Vector var2) {
		resetAttDirectiveFlags();
		return createDocumentObject(var0, var1, var2, false);
	}

	public static boolean endCreateWTDocumentRetainContent(Hashtable var0, Hashtable var1, Vector var2) {
		return endCreateWTDocument(var0, var1, var2);
	}

	public static boolean addToDocumentStructure(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			WTDocument var3 = getDocument(getValue("parent", var0, var1, true));
			WTDocument var4 = getDocument(getValue("child", var0, var1, true));
			if (var3 != null && var4 != null) {
				if (getUsageLink(var3, (WTDocumentMaster) var4.getMaster()) == null) {
					WTDocumentUsageLink var5 = WTDocumentUsageLink.newWTDocumentUsageLink(var3,
							(WTDocumentMaster) var4.getMaster());
					TypeIdentifier var6 = TypeIdentifierHelper.getType(var5);
					LOGGER.debug("Association Link Type :: " + var6);
					if (!AssociationConstraintHelper.service.isValidAssociation(var3, var6, var4)) {
						Object[] var7 = new Object[] { var3.getIdentity(), var4.getIdentity(), var6.getTypename(),
								TypeIdentifierHelper.getType(var3).getTypename(),
								TypeIdentifierHelper.getType(var4).getTypename() };
						String var8 = WTMessage.getLocalizedMessage(
								"com.ptc.core.meta.type.mgmt.server.impl.association.associationResource", "0", var7);
						LOGGER.debug(var8);
						throw new WTException(var8);
					}

					PrintStream var11 = System.out;
					String var10001 = var3.getIdentity();
					var11.println("Valid relationship for " + var10001 + ", and " + var4.getIdentity()
							+ ". Relationship is " + var6.getTypename() + " between "
							+ TypeIdentifierHelper.getType(var3).getTypename() + " and "
							+ TypeIdentifierHelper.getType(var4).getTypename());
					var5.setStructureOrder(0);
					PersistenceServerHelper.manager.insert(var5);
					return true;
				}

				String var10000 = var3.getName();
				LoadServerHelper.printMessage(
						"\nWTDocumentUsageLink already exists between " + var10000 + " and " + var4.getName());
			}
		} catch (WTException var9) {
			LoadServerHelper.printMessage("\nAdd To Document Structure Failed: " + var9.getLocalizedMessage());
			var9.printStackTrace();
		} catch (Exception var10) {
			LoadServerHelper.printMessage("\nAdd To Document Structure Failed: " + var10.getMessage());
			var10.printStackTrace();
		}

		return false;
	}

	public static boolean addDocumentDependency(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			WTDocument var3 = getDocument(getValue("doc", var0, var1, true));
			if (var3 != null) {
				WTDocument var4 = getDocument(getValue("referenceddoc", var0, var1, true));
				if (var4 != null) {
					String var5 = getValue("comment", var0, var1, false);
					Folder var6 = WorkInProgressHelper.service.getCheckoutFolder();
					CheckoutLink var7 = WorkInProgressHelper.service.checkout(var3, var6,
							"Checking out to create DocumentDependency using LoadFromFile utility");
					WTDocument var8 = (WTDocument) var7.getWorkingCopy();
					WTDocumentHelper.service.createDependencyLink(var8, var4, var5);
					var3 = (WTDocument) WorkInProgressHelper.service.checkin(var8,
							"Created DocumentDependency using LoadFromFile utility");
					var3 = cacheDocument(var3);
					return true;
				}
			}
		} catch (WTException var9) {
			LoadServerHelper.printMessage("\nAdd Document Dependency Failed: " + var9.getLocalizedMessage());
			var9.printStackTrace();
		} catch (Exception var10) {
			LoadServerHelper.printMessage("\nAdd Document Dependency Failed: " + var10.getMessage());
			var10.printStackTrace();
		}

		return false;
	}

	public static WTDocument getDocument() throws WTException {
		return getDocument((String) null, (String) null, (String) null);
	}

	public static WTDocument getDocument(String var0) throws WTException {
		return getDocument(var0, (String) null, (String) null);
	}

	public static WTDocument getDocument(String var0, String var1) throws WTException {
		return getDocument(var0, var1, (String) null);
	}

	public static WTDocument getDocument(String var0, String var1, String var2) throws WTException {
		WTDocument var3 = getCachedDocument(var0, var1, var2);
		LatestConfigSpec var4 = null;
		if (var3 == null && var0 != null) {
			QuerySpec var5 = new QuerySpec(WTDocument.class);
			var5.appendWhere(new SearchCondition(WTDocument.class, "master>number", "=", var0.toUpperCase(), false));
			if (var1 == null) {
				var5.appendAnd();
				var5.appendWhere(new SearchCondition(WTDocument.class, "iterationInfo.latest", "TRUE"));
				var4 = new LatestConfigSpec();
			} else if (var1 != null) {
				var5.appendAnd();
				var5.appendWhere(
						new SearchCondition(WTDocument.class, "versionInfo.identifier.versionId", "=", var1, false));
				if (var2 == null) {
					var5.appendAnd();
					var5.appendWhere(new SearchCondition(WTDocument.class, "iterationInfo.latest", "TRUE"));
				} else if (var2 != null) {
					var5.appendAnd();
					var5.appendWhere(new SearchCondition(WTDocument.class, "iterationInfo.identifier.iterationId", "=",
							var2, false));
				}
			}

			if (var4 != null) {
				var5 = var4.appendSearchCriteria(var5);
			}

			QueryResult var6 = PersistenceHelper.manager.find(var5);
			if (var4 != null) {
				var6 = var4.process(var6);
			}

			int var7 = var6.size();
			String var8;
			String var9;
			String var10;
			String var10000;
			if (var7 == 1) {
				var3 = (WTDocument) var6.nextElement();
				if (WorkInProgressHelper.isCheckedOut(var3)) {
					var10000 = var3.getVersionDisplayIdentifier()
							.getLocalizedMessage(WTContext.getContext().getLocale());
					var8 = var10000 + "." + var3.getIterationIdentifier().getValue();
					var9 = WorkInProgressHelper.getState(var3).getDisplay();
					var10 = "Operation failed because the document is checked out, " + var0 + " " + var8
							+ " with state of " + var9;
					throw new WTException(var10);
				}

				var3 = cacheDocument(var3);
			} else if (var7 > 1) {
				var3 = (WTDocument) var6.nextElement();
				if (WorkInProgressHelper.isCheckedOut(var3)) {
					var10000 = var3.getVersionDisplayIdentifier()
							.getLocalizedMessage(WTContext.getContext().getLocale());
					var8 = var10000 + "." + var3.getIterationIdentifier().getValue();
					var9 = WorkInProgressHelper.getState(var3).getDisplay();
					var10 = "Operation failed because the document is checked out, " + var0 + " " + var8
							+ " with state of " + var9;
					throw new WTException(var10);
				}

				StringBuffer var11 = new StringBuffer(var0);
				if (var1 != null) {
					var11.append(", " + var1);
					if (var2 != null) {
						var11.append("." + var2);
					}
				}

				var9 = "Searching for document returned " + var7 + " documents, document criteria = " + var11
						+ " Only one document expected.";
				throw new WTException(var9);
			}
		}

		return var3;
	}

	private static boolean createDocumentObject(Hashtable var0, Hashtable var1, Vector var2) {
		return createDocumentObject(var0, var1, var2, true);
	}

	private static boolean createDocumentObject(Hashtable var0, Hashtable var1, Vector var2, boolean var3) {
		String var10000;
		try {
			setUser(var0, var1);
			WTDocument var4 = constructDocument(var0, var1);
			if (var3 && (Boolean) LoadServerHelper.getCacheValue("DOCUMENT_NEW_VERSION")) {
				var4 = clearContent(var4);
			}

			var4 = cacheDocument(var4);
			return true;
		} catch (WTException var5) {
			var10000 = getDisplayInfo(var0, var1);
			LoadServerHelper.printMessage("\nCreate Document Failed (" + var10000 + "): " + var5.getLocalizedMessage());
			var5.printStackTrace();
		} catch (Exception var6) {
			var10000 = getDisplayInfo(var0, var1);
			LoadServerHelper.printMessage("\nCreate Document Failed (" + var10000 + "): " + var6.getMessage());
			var6.printStackTrace();
		}

		return false;
	}

	private static String getDisplayInfo(Hashtable var0, Hashtable var1) {
		String var2 = null;
		String var3 = null;
		String var4 = null;

		try {
			var2 = getValue("number", var0, var1, false);
			var3 = getValue("version", var0, var1, false);
			var4 = getValue("iteration", var0, var1, false);
		} catch (WTException var6) {
		}

		if (var2 == null) {
			var2 = "<no number>";
		}

		if (var3 == null) {
			var3 = "<version>";
		}

		if (var4 == null) {
			var4 = "<iteration>";
		}

		return var2 + " " + var3 + "." + var4;
	}

	private static boolean updateDocumentObject(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			boolean var4 = Boolean.TRUE.equals(MethodContext.getContext().get("csvLoadValue key"));
			WTDocument var3;
			if (var4) {
				var3 = (WTDocument) LoadAttValues.getCurrentTypeManaged();
				var3 = (WTDocument) PersistenceHelper.manager.modify(var3);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.error("new soft att directive was used for document: " + var3.getName());
				}
			} else {
				var3 = getDocument();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.error("new soft att value directive was *not* used for document: " + var3.getName());
				}
			}

			boolean var5 = Boolean.TRUE.equals(MethodContext.getContext().get("csvIBAValue load directive was used"));
			if (var5) {
				var3 = (WTDocument) LoadValue.applySoftAttributes(var3);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("old iba att value directive was used for document: " + var3.getName());
				}
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("old iba att value directive was *not* used for document: " + var3.getName());
			}

			var3 = setPrimaryContent(var3, var0, var1, var2);
			var3 = cacheDocument(var3);
			var2.addElement(var3);
			boolean var6 = true;
			return var6;
		} catch (WTException var18) {
			LoadServerHelper.printMessage("\nUpdate Document Failed: " + var18.getLocalizedMessage());
			var18.printStackTrace();
		} catch (Exception var19) {
			LoadServerHelper.printMessage("\nUpdate Document Failed: " + var19.getMessage());
			var19.printStackTrace();
		} finally {
			try {
				resetUser();
			} catch (WTException var17) {
			}

			resetAttDirectiveFlags();
		}

		return false;
	}

	private static void resetAttDirectiveFlags() {
		MethodContext var0 = MethodContext.getContext();
		var0.remove("csvLoadValue key");
		var0.remove("csvIBAValue load directive was used");
	}

	private static WTDocument constructDocument(Hashtable var0, Hashtable var1) throws WTException {
		String var2 = getValue("number", var0, var1, false);
		String var3 = getValue("version", var0, var1, false);
		String var4 = getValue("iteration", var0, var1, false);
		LOGGER.debug("The current number in work is: " + var2);
		// boolean var5 = true;
		boolean insert_on_latest_iteration = false;
		WTDocument var6 = null;

		Timestamp createTimestampObj = null;
		Timestamp modifyTimestampObj = null;

		String createTimestamp = getValue("createTimestamp", var0, var1, false);
		String modifyTimestamp = getValue("modifyTimestamp", var0, var1, false);

		createTimestampObj = parseTimestamp(createTimestamp);
		modifyTimestampObj = parseTimestamp(modifyTimestamp);

		if (createTimestampObj == null) {
			if (modifyTimestampObj != null) {
				createTimestampObj = modifyTimestampObj;
			}
		} else if (modifyTimestampObj == null) {
			modifyTimestampObj = createTimestampObj;
		}

		if (createTimestampObj == null && modifyTimestamp == null) {
			insert_on_latest_iteration = true;
		}

		if (var2 != null) {
			var6 = getDocument(var2, var3, var4);
			if (var6 == null && var3 != null && var4 != null) {
				var6 = getDocument(var2, var3, (String) null);
			}

			if (var6 == null && var3 != null) {
				var6 = getDocument(var2, (String) null, (String) null);
			}
		}

		String var7;
		String var8;
		String var10000;
		if (var6 == null) {
			var6 = WTDocument.newWTDocument();

			try {
				if (var6.getIterationInfo() == null) {
					var6.setIterationInfo(IterationInfo.newIterationInfo());
				}
			} catch (WTPropertyVetoException var13) {
				throw new WTException(var13);
			}

			var6 = applyConstructionTimeAttributes(var6, var0, var1);

			if (VERBOSE) {
				LOGGER.debug("Creating a new document = " + var2);
			}

			LoadServerHelper.setCacheValue("DOCUMENT_NEW_VERSION", Boolean.FALSE);
		} else {
			if (insert_on_latest_iteration && !VersionControlHelper.isLatestIteration(var6)) {
				var6 = (WTDocument) VersionControlHelper.getLatestIteration(var6);
			}

			if (VERBOSE) {
				var10000 = var6.getVersionDisplayIdentifier().getLocalizedMessage(WTContext.getContext().getLocale());
				var7 = var10000 + "." + var6.getIterationIdentifier().getValue();
				var8 = WorkInProgressHelper.getState(var6).getDisplay();
				System.out
						.println("Iterating on an existing document = " + var2 + " " + var7 + " with state of " + var8);
			}

			try {
				WTContainerRef var15 = var6.getContainerReference();
				WTContainerRef var16 = LoadServerHelper.getTargetContainer(var0, var1);
				if (!var15.equals(var16)) {
					throw new WTException("Can not create '" + var2 + " " + var6.getName() + " - " + var3
							+ "' in Container: '" + var16.getName() + "', because it already exists in Container: '"
							+ var15.getName() + "'");
				}

				String var9;
				if (WorkInProgressHelper.isCheckedOut(var6)) {
					var10000 = var6.getVersionDisplayIdentifier()
							.getLocalizedMessage(WTContext.getContext().getLocale());
					var9 = var10000 + "." + var6.getIterationIdentifier().getValue();
					String var17 = WorkInProgressHelper.getState(var6).getDisplay();
					throw new WTException(
							"Creating the new version/iteration failed because the previous document is checked out, "
									+ var2 + " " + var9 + " with state of " + var17);
				}

				if (insert_on_latest_iteration) {
					var9 = null;
					boolean var10 = isNewVersion(var6, var3);
					if (var4 == null) {
						if (!var10) {
							int var11 = Integer.parseInt(var6.getIterationIdentifier().getValue());
							var9 = Integer.toString(var11 + 1);
						} else {
							var9 = Integer.toString(1);
						}
					}

					var6 = (WTDocument) VersionControlHelper.service.newIteration(var6);
					LoadServerHelper.setCacheValue("DOCUMENT_NEW_VERSION", Boolean.TRUE);
					if (var9 != null) {
						setIteration(var6, var9);
					}
				} else if (var3 != null && !var3.equals(VersionControlHelper.getVersionIdentifier(var6).getValue())) {
					var6 = (WTDocument) VersionControlHelper.service.newVersion(var6);
					LoadServerHelper.setCacheValue("DOCUMENT_NEW_VERSION", Boolean.TRUE);
				} else {
					WTDocument var18 = (WTDocument) VersionControlHelper.service.newIteration(var6);
					var6 = (WTDocument) PersistenceHelper.manager.refresh(var6);
					var6 = (WTDocument) VersionControlHelper.service.supersede(var6, var18);
					LoadServerHelper.setCacheValue("DOCUMENT_NEW_VERSION", Boolean.TRUE);
				}
			} catch (WTPropertyVetoException var14) {
				throw new WTException(var14);
			}

		}

		var6 = applyHardAttributes(var6, var0, var1);
		// Emerson modification
		var6 = (WTDocument) SetCreatedByAndModifiedBy(var6, var0, var1, false);
		if (insert_on_latest_iteration) {
			try {
				var6.setFederatableInfo(new FederatableInfo());
			} catch (WTPropertyVetoException wtpve) {
				throw new WTException(wtpve,
						"Error creating the FederatableInfo required for inserting versions/iterations");
			}
			// Use null for the ufids of this object and the branch, because we
			// want the default
			// insert behavior of using the latest iteration as the branch point
			// on all new versions.
			//
			// The following call to insertNode MUST NOT BE REMOVED.
			// To turn off the "insert out of order" behavior, you should
			// instead set the
			// insert_on_latest_iteration flag to false
			LOGGER.info("PersistenceServerHelper.manager.insertNode");
			var6 = (WTDocument) VersionControlHelper.service.insertNode(var6, null, null, null, null);
		} else {
			// This else is not reachable with the insert flag set to true, this
			// store is now
			// done in the insertNode for both the new object, new iteration,
			// and new version cases.
			var6 = (WTDocument) PersistenceServerHelper.manager.store(var6, createTimestampObj, modifyTimestampObj);
		}
		if (VERBOSE) {
			var10000 = var6.getVersionDisplayIdentifier().getLocalizedMessage(WTContext.getContext().getLocale());
			var7 = var10000 + "." + var6.getIterationIdentifier().getValue();
			var8 = WorkInProgressHelper.getState(var6).getDisplay();
			LOGGER.debug("New document = " + var2 + " " + var7 + " with state of " + var8);
		}
//		FormatContentHolder var5 = (FormatContentHolder) LoadServerHelper.getCacheValue(CURRENT_CONTENT_HOLDER);
//		LOGGER.debug("*********Printing content from *******************" + ((WTDocument) var5).getName());
//
//		LOGGER.debug("*********Printing content from *******************"
//				+ ((WTDocument) var5).getVersionIdentifier().getValue());

		PersistenceServerHelper.update(var6);
		return var6;
	}

	private static boolean isNewVersion(WTDocument var0, String var1) throws WTException {
		if (var1 == null) {
			return false;
		} else {
			return !var1.equals(VersionControlHelper.getVersionIdentifier(var0).getValue());
		}
	}

	private static WTDocument getCachedDocument() throws WTException {
		return getCachedDocument((String) null, (String) null, (String) null);
	}

	private static WTDocument getCachedDocument(String var0) throws WTException {
		return getCachedDocument(var0, (String) null, (String) null);
	}

	private static WTDocument getCachedDocument(String var0, String var1) throws WTException {
		return getCachedDocument(var0, var1, (String) null);
	}

	private static WTDocument getCachedDocument(String var0, String var1, String var2) throws WTException {
		return (WTDocument) LoadServerHelper.getCacheValue(getDocumentCacheKey(var0, var1, var2));
	}

	private static String getDocumentCacheKey(String var0, String var1, String var2) throws WTException {
		StringBuffer var3 = new StringBuffer("DOCUMENT_CACHE_KEY:");
		if (var0 != null) {
			var3.append(var0.toUpperCase());
			if (var1 != null) {
				var3.append("|").append(var1);
				if (var2 != null) {
					var3.append("|").append(var2);
				}
			}
		}

		return var3.toString();
	}

	private static WTDocument cacheDocument(WTDocument var0) throws WTException {
		if (var0 == null) {
			LoadServerHelper.removeCacheValue(getDocumentCacheKey((String) null, (String) null, (String) null));
			LoadServerHelper.removeCacheValue(CURRENT_CONTENT_HOLDER);
			LoadValue.establishCurrentIBAHolder((IBAHolder) null);
			LoadAttValues.establishCurrentTypeManaged((TypeManaged) null);
			LoadServerHelper.removeCacheValue(CURRENT_DOCUMENT);
		} else {
			String var1 = var0.getNumber();
			String var2 = VersionControlHelper.getVersionIdentifier(var0).getValue();
			String var3 = VersionControlHelper.getIterationIdentifier(var0).getValue();
			LoadServerHelper.setCacheValue(getDocumentCacheKey((String) null, (String) null, (String) null), var0);
			LoadServerHelper.setCacheValue(getDocumentCacheKey(var1, (String) null, (String) null), var0);
			LoadServerHelper.setCacheValue(getDocumentCacheKey(var1, var2, (String) null), var0);
			LoadServerHelper.setCacheValue(getDocumentCacheKey(var1, var2, var3), var0);
			LoadServerHelper.setCacheValue(CURRENT_CONTENT_HOLDER, var0);
			LoadServerHelper.setCacheValue(CURRENT_DOCUMENT, var0);
		}

		LoadValue.establishCurrentIBAHolder(var0);
		LoadAttValues.establishCurrentTypeManaged(var0);
		LoadValue.beginIBAContainer();
		return var0;
	}

	private static WTDocument applyHardAttributes(WTDocument var0, Hashtable var1, Hashtable var2) throws WTException {
		WTContainerRef var3 = LoadServerHelper.getTargetContainer(var1, var2);
		setContainer(var0, var3);
		setType(var0, getValue("typedef", var1, var2, false));
		setIteration(var0, getValue("iteration", var1, var2, false));
		setSecurityLabels(var0, getValue("securityLabels", var1, var2, false));
		setTitle(var0, getValue("title", var1, var2, false));
		setDescription(var0, getValue("description", var1, var2, false));
		setDepartment(var0, getValue("department", var1, var2, true));
		setFolder(var3, var0, getValue("saveIn", var1, var2, true));
		setLifeCycle(var3, var0, getValue("lifecycletemplate", var1, var2, false));
		setTeamTemplate(var3, var0, getValue("teamTemplate", var1, var2, false), getValue("domain", var1, var2, false));
		setState(var0, getValue("lifecyclestate", var1, var2, false));
		setVersion(var0, getValue("version", var1, var2, false));
		return var0;
	}

	private static WTDocument applyConstructionTimeAttributes(WTDocument var0, Hashtable var1, Hashtable var2)
			throws WTException {
		setName(var0, getValue("name", var1, var2, true));
		setNumber(var0, getValue("number", var1, var2, false));
		setDocType(var0, getValue("type", var1, var2, true));

		return var0;
	}

	private static void setName(WTDocument var0, String var1) throws WTException {
		try {
			var0.setName(var1);
		} catch (WTPropertyVetoException var3) {
			LoadServerHelper.printMessage("\nsetName: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	private static void setNumber(WTDocument var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				var0.setNumber(var1);
			}

		} catch (WTPropertyVetoException var3) {
			LoadServerHelper.printMessage("\nsetNumber: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	private static void setDocType(WTDocument var0, String var1) throws WTException {
		try {
			try {
				var0.setDocType(DocumentType.toDocumentType(var1));
			} catch (WTInvalidParameterException var5) {
				try {
					var0.setDocType(DocumentType.toDocumentType("$$" + var1));
				} catch (WTInvalidParameterException var4) {
					throw new WTException("Unknown document type <" + var1 + ">");
				}
			}

		} catch (WTPropertyVetoException var6) {
			LoadServerHelper.printMessage("\nsetDocType: " + var6.getMessage());
			var6.printStackTrace();
			throw new WTException(var6);
		}
	}

	private static void setTitle(WTDocument var0, String var1) throws WTException {
		try {
			var0.setTitle(var1);
		} catch (WTPropertyVetoException var3) {
			LoadServerHelper.printMessage("\nsetTitle: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	private static void setDescription(WTDocument var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				var0.setDescription(var1);
			}

		} catch (WTPropertyVetoException var3) {
			LoadServerHelper.printMessage("\nsetDescription: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	private static void setDepartment(WTDocument var0, String var1) throws WTException {
		try {
			var0.setDepartment(DepartmentList.toDepartmentList(var1));
		} catch (WTPropertyVetoException var3) {
			LoadServerHelper.printMessage("\nsetDepartment: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	private static WTDocument setPrimaryContent(WTDocument var0, Hashtable var1, Hashtable var2, Vector var3)
			throws WTException {
		LoadServerHelper.setCacheValue(CURRENT_CONTENT_HOLDER, var0);
		String var4 = getValue("path", var1, var2, false);
		if (var4 != null && !LoadContent.createPrimary(var1, var2, var3)) {
			throw new WTException("LoadDoc - Failed to save content for file_path = " + var4);
		} else {
			return (WTDocument) LoadServerHelper.getCacheValue(CURRENT_CONTENT_HOLDER);
		}
	}

	private static void setUser(Hashtable var0, Hashtable var1) throws WTException {
		LoadServerHelper.setCacheValue("DOCUMENT_PREVIOUS_USER:", SessionMgr.getPrincipal().getName());
		String var2 = getValue("user", var0, var1, false);
		if (var2 != null) {
			LoadServerHelper.changePrincipal(var2);
		}

	}

	private static void resetUser() throws WTException {
		String var0 = (String) LoadServerHelper.getCacheValue("DOCUMENT_PREVIOUS_USER:");
		if (var0 != null) {
			LoadServerHelper.changePrincipal(var0);
		}

	}

	private static String getValue(String var0, Hashtable var1, Hashtable var2, boolean var3) throws WTException {
		String var4 = LoadServerHelper.getValue(var0, var1, var2, var3 ? 0 : 1);
		if (var3 && var4 == null) {
			throw new WTException("\nRequired value for " + var0 + " not provided in input file.");
		} else {
			return var4;
		}
	}

	private static void setContainer(WTContained var0, WTContainerRef var1) throws WTException {
		try {
			if (var1 != null) {
				var0.setContainerReference(var1);
			}

		} catch (WTPropertyVetoException var3) {
			LoadServerHelper.printMessage("\nsetContainer: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	private static void setType(Typed var0, String var1) throws WTException {
		LoadValue.setType(var0, var1);
	}

	private static void setSecurityLabels(SecurityLabeled var0, String var1) throws WTException {
		if (var1 != null && var1.length() > 0) {
			try {
				AccessControlServerHelper.manager.setSecurityLabels(var0, var1, false);
			} catch (WTPropertyVetoException var3) {
				throw new WTException(var3);
			}
		}

	}

	private static void setVersion(Versioned var0, String var1) throws WTException {
		try {
			if (var1 == null || var1.trim().length() == 0) {
				var1 = null;
				if (var0.getVersionInfo() != null) {
					return;
				}
			}

			MultilevelSeries var2 = null;
			Mastered var3 = var0.getMaster();
			if (var3 != null) {
				String var4 = var3.getSeries();
				if (var4 == null) {
					if (var0 instanceof WTContained && ((WTContained) var0).getContainer() != null) {
						var2 = VersionControlHelper.getVersionIdentifierSeries(var0);
						VersionControlServerHelper.changeSeries(var3, var2.getUniqueSeriesName());
					}
				} else {
					var2 = MultilevelSeries.newMultilevelSeries(var4);
				}
			}

			if (var2 == null) {
				var2 = MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier", var1);
			}

			if (var1 != null) {
				var2.setValueWithoutValidating(var1.trim());
			}

			VersionIdentifier var7 = VersionIdentifier.newVersionIdentifier(var2);
			VersionControlServerHelper.setVersionIdentifier(var0, var7, false);
		} catch (WTPropertyVetoException var5) {
			LoadServerHelper.printMessage("\nsetVersion: " + var5.getMessage());
			var5.printStackTrace();
			throw new WTException(var5);
		} catch (Exception var6) {
			throw new WTException(var6);
		}
	}

	private static void setIteration(Iterated var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				Series var2 = Series.newSeries("wt.vc.IterationIdentifier", var1);
				IterationIdentifier var3 = IterationIdentifier.newIterationIdentifier(var2);
				VersionControlHelper.setIterationIdentifier(var0, var3);
			}

		} catch (WTPropertyVetoException var4) {
			LoadServerHelper.printMessage("\nsetIteration: " + var4.getMessage());
			var4.printStackTrace();
			throw new WTException(var4);
		}
	}

	private static void setFolder(WTContainerRef var0, FolderEntry var1, String var2) throws WTException {
		if (var2 != null) {
			Object var3;
			try {
				var3 = FolderHelper.service.getFolder(var2, var0);
			} catch (FolderNotFoundException var5) {
				var3 = null;
			}

			if (var3 == null) {
				var3 = FolderHelper.service.createSubFolder(var2, var0);
			}

			FolderHelper.assignLocation(var1, (Folder) var3);
		}

	}

	private static void setLifeCycle(WTContainerRef var0, LifeCycleManaged var1, String var2) throws WTException {
		try {
			if (var2 != null) {
				LifeCycleHelper.setLifeCycle(var1, LifeCycleHelper.service.getLifeCycleTemplate(var2, var0));
			}

		} catch (WTPropertyVetoException var4) {
			LoadServerHelper.printMessage("\nsetLifeCycle: " + var4.getMessage());
			var4.printStackTrace();
			throw new WTException(var4);
		}
	}

	private static void setState(LifeCycleManaged var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				LifeCycleServerHelper.setState(var0, State.toState(var1));
			}

		} catch (WTPropertyVetoException var3) {
			LoadServerHelper.printMessage("\nsetState: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	private static void setTeamTemplate(WTContainerRef var0, TeamManaged var1, String var2, String var3)
			throws WTException {
		try {
			if (var2 != null && var3 != null) {
				TeamHelper.service.setTeamTemplate(var0, var1, var2, var3);
			}

		} catch (WTPropertyVetoException var5) {
			LoadServerHelper.printMessage("\nsetTeamTemplate: " + var5.getMessage());
			var5.printStackTrace();
			throw new WTException(var5);
		}
	}

	private static WTDocument clearContent(WTDocument var0) throws WTException {
		try {
			var0 = (WTDocument) ContentHelper.service.getContents(var0);
			String var1 = var0.getNumber();
			Vector var2 = ContentHelper.getContentListAll(var0);
			int var3 = var2.size();
			if (VERBOSE) {
				LOGGER.debug("Removing " + var3 + " content items from " + var1);
			}

			for (int var4 = 0; var4 < var3; ++var4) {
				ContentItem var5 = (ContentItem) var2.elementAt(var4);
				if (VERBOSE) {
					if (var5 instanceof ApplicationData) {
						LOGGER.debug("Removing file " + ((ApplicationData) var5).getFileName());
					} else {
						LOGGER.debug("Removing content item, but not a file " + var5.getDescription());
					}
				}

				ContentServerHelper.service.deleteContent(var0, var5, false);
			}

			return var0;
		} catch (WTException var6) {
			throw new WTException(var6);
		} catch (WTPropertyVetoException var7) {
			throw new WTException(var7);
		} catch (PropertyVetoException var8) {
			throw new WTException(var8);
		}
	}

	private static WTDocumentUsageLink getUsageLink(WTDocument var0, WTDocumentMaster var1) throws WTException {
		if (VERBOSE) {
			LOGGER.debug("getUsageLink - parent:" + var0 + " child_master:" + var1);
			LOGGER.debug("This is the getUsageLink query:");
		}

		QueryResult var2 = PersistenceHelper.manager.find(WTDocumentUsageLink.class, var0, "usedBy", var1);
		if (var2 != null && var2.size() != 0) {
			WTDocumentUsageLink var3 = (WTDocumentUsageLink) var2.nextElement();
			return var3;
		} else {
			return null;
		}
	}

	static {
		try {
			rb = ResourceBundle.getBundle(RESOURCE, WTContext.getContext().getLocale());
			WTProperties var0 = WTProperties.getLocalProperties();
			//VERBOSE = var0.getProperty("wt.doc.load.verbose", false);
				VERBOSE = true;

		} catch (Throwable var1) {
			System.err.println("Error initializing " + APPLDocLoader.class.getName());
			var1.printStackTrace(System.err);
			throw new ExceptionInInitializerError(var1);
		}
	}

	private static Iterated SetCreatedByAndModifiedBy(Iterated iterated, Hashtable nv, Hashtable cmd_line,
			boolean required) throws WTException {

		WTUser createdby = null;
		WTUser modifiedby = null;

		Iterated object = iterated;

		String customcreatedby = getValue("customcreatedby", nv, cmd_line, false);

		if (customcreatedby != null && customcreatedby.toString().length() > 0) {
			createdby = OrganizationServicesHelper.manager.getAuthenticatedUser(customcreatedby);
		}

		if (createdby != null) {
			try {
				object = VersionControlHelper.assignIterationCreator(object,
						WTPrincipalReference.newWTPrincipalReference(createdby));
			} catch (WTPropertyVetoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		String custommodifiedby = getValue("custommodifiedby", nv, cmd_line, false);

		if (custommodifiedby != null && custommodifiedby.toString().length() > 0) {
			modifiedby = OrganizationServicesHelper.manager.getAuthenticatedUser(custommodifiedby);
		}

		if (modifiedby != null) {
			try {
				VersionControlHelper.setIterationModifier(object,
						WTPrincipalReference.newWTPrincipalReference(modifiedby));
			} catch (WTPropertyVetoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return object;
	}

	private static Timestamp parseTimestamp(String time) throws WTException {
		SimpleDateFormat sdf = new SimpleDateFormat();
		Date date = null;
		ParsePosition pos = new ParsePosition(0);
		String localizedPattern = null;

		if (time == null || time.trim().equals(""))
			return null;

		localizedPattern = sdf.toLocalizedPattern();
		if (date == null) {
			sdf.applyPattern(localizedPattern);
			date = sdf.parse(time, pos);
			pos.setIndex(0);
		}

		if (date == null) {
			sdf.applyPattern("MM/dd/yyyy HH:mm:ss z");
			date = sdf.parse(time, pos);
			pos.setIndex(0);
		}
		if (date == null) {
			sdf.applyPattern("yyyy-mm-dd HH:mm:ss.SSS");
			date = sdf.parse(time, pos);
			pos.setIndex(0);
		}

		if (date == null) {
			sdf.applyPattern("MM/dd/yyyy HH:mm z");
			date = sdf.parse(time, pos);
			pos.setIndex(0);
		}

		if (date == null) {
			// GMT assumption
			sdf.applyPattern("MM/dd/yyyy HH:mm");
			date = sdf.parse(time, pos);
			pos.setIndex(0);
		}

		if (date == null) {
			// GMT assumption
			sdf.applyPattern("MM/dd/yyyy");
			date = sdf.parse(time, pos);
			pos.setIndex(0);
		}

		if (date == null) {
			StringBuffer errmsg = new StringBuffer();
			errmsg.append("\n Timestamp date format: '" + time + "' is incorrect! \n");
			errmsg.append("Timestamp must be in this format (choose one): \n");
			errmsg.append("1. " + localizedPattern + "\n");
			errmsg.append("2. MM/dd/yyyy HH:mm:ss z  ( 12/31/2005 15:30:01 CST ) \n");
			errmsg.append("22. MM/dd/yyyy HH:mm:ss  ( 12/31/2005 15:30:01 ) \n");
			errmsg.append("3. MM/dd/yyyy HH:mm z     ( 12/31/2005 15:30 CST ) \n");
			errmsg.append("4. MM/dd/yyyy HH:mm       ( 12/31/2005 15:30 ) - GMT implied\n");
			errmsg.append("5. MM/dd/yyyy             ( 12/31/2005 ) - GMT implied \n");
			throw new WTException(errmsg.toString());
		}

		return new Timestamp(date.getTime());
	}



}