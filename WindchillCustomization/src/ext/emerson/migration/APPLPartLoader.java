package ext.emerson.migration;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import org.apache.logging.log4j.Logger;

import com.ptc.core.lwc.server.LoadAttValues;
import com.ptc.core.meta.common.IdentifierFactory;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.core.meta.common.impl.TypeIdentifierUtilityHelper;
import com.ptc.core.meta.common.impl.WCTypeIdentifier;
import com.ptc.core.meta.type.mgmt.server.impl.association.AssociationConstraintHelper;

import wt.access.AccessControlServerHelper;
import wt.access.SecurityLabeled;
import wt.admin.AdministrativeDomainHelper;
import wt.configuration.TraceCode;
import wt.doc.LoadDoc;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.eff.ClientEffGroup;
import wt.eff.EffGroupAssistant;
import wt.eff.EffHelper;
import wt.eff.EffTypeModifier;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTValuedHashMap;
import wt.filter.NavigationFilterHelper;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.folder.FolderNotFoundException;
import wt.generic.GenericType;
import wt.iba.value.AttributeContainer;
import wt.iba.value.DefaultAttributeContainer;
import wt.iba.value.IBAHolder;
import wt.iba.value.service.LoadValue;
import wt.identity.DisplayIdentity;
import wt.identity.IdentityFactory;
import wt.inf.container.LookupSpec;
import wt.inf.container.OrgContainer;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.inf.container.WTContainerTemplate;
import wt.inf.team.ContainerTeam;
import wt.inf.team.ContainerTeamHelper;
import wt.inf.team.ContainerTeamReference;
import wt.inf.template.ContainerTemplateHelper;
import wt.inf.template.WTContainerTemplateMaster;
import wt.introspection.ReflectionHelper;
import wt.ixb.publicforhandlers.IxbHndHelper;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.LifeCycleServerHelper;
import wt.lifecycle.State;
import wt.load.LoadServerHelper;
import wt.locks.LockException;
import wt.locks.LockHelper;
import wt.log4j.LogR;
import wt.method.MethodContext;
import wt.occurrence.OccurrenceHelper;
import wt.option.ChoiceMappable;
import wt.option.Expressionable;
import wt.org.DirectoryContextProvider;
import wt.org.OrganizationServicesHelper;
import wt.org.WTOrganization;
import wt.org.WTPrincipalReference;
import wt.org.WTUser;
import wt.part.LineNumber;
import wt.part.PartDocHelper;
import wt.part.PartPathOccurrence;
import wt.part.PartType;
import wt.part.PartUsesOccurrence;
import wt.part.Quantity;
import wt.part.QuantityUnit;
import wt.part.Source;
import wt.part.WTPart;
import wt.part.WTPartDescribeLink;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartReferenceLink;
import wt.part.WTPartUsageLink;
import wt.pdmlink.PDMLinkProduct;
import wt.pom.Transaction;
import wt.pom.UniquenessException;
import wt.prefs.PreferenceHelper;
import wt.prefs.WTPreferences;
import wt.project.Role;
import wt.query.ClassAttribute;
import wt.query.LogicalOperator;
import wt.query.OrderBy;
import wt.query.QueryException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.sandbox.SandboxHelper;
import wt.series.MultilevelSeries;
import wt.series.Series;
import wt.services.applicationcontext.UnableToCreateServiceException;
import wt.services.applicationcontext.implementation.DefaultServiceProvider;
import wt.session.SessionHelper;
import wt.session.SessionMgr;
import wt.session.SessionServerHelper;
import wt.team.TeamHelper;
import wt.team.TeamManaged;
import wt.type.TypeManaged;
import wt.type.Typed;
import wt.type.TypedUtility;
import wt.ufid.FederatableInfo;
import wt.ufid.Ufid;
import wt.util.LocalizableMessage;
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
import wt.vc.struct.StructHelper;
import wt.vc.struct.StructServerHelper;
import wt.vc.views.Variation1;
import wt.vc.views.Variation2;
import wt.vc.views.View;
import wt.vc.views.ViewException;
import wt.vc.views.ViewHelper;
import wt.vc.views.ViewReference;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.Workable;
import wt.viewmarkup.RepresentationForwardLoaderService;

public class APPLPartLoader {
	private static final String CURRENT_CONTENT_HOLDER = "Current ContentHolder";
	private static final String PART_CACHE_KEY = "PART_CACHE_KEY:";
	private static final String PART_MASTER_CACHE_KEY = "PART_MASTER_CACHE_KEY:";
	private static final String PARTUSAGELINK_CACHE_KEY = "PARTUSAGElINK_KEY:";
	private static final String PARTDOCLINK_CACHE_KEY = "PARTDOCLINK_CACHE_KEY:";
	public static final String PARTOCCURRENCE_CACHE_KEY = "PARTOCCURRENCE_KEY";
	private static final String IDA3_MASTER_REFERENCE = "masterReference.key.id";
	private static final String PART_PREVIOUS_USER = "PART_PREVIOUS_USER:";
	private static String CURRENT_PART = "Current Part";
	private static String RESOURCE = "wt.part.partResource";
	private static final int REFERENCE_LINK = 0;
	private static final int DESCRIBES_LINK = 1;
	private static final int UNKNOWN_LINK = 2;
	private static ResourceBundle rb;
	private static boolean dont_check_cache_for_lastIter;
	private static final String TYPEDEF = "typedef";
	private static final String DEFAULT_VIEW_PREF_NODE = "wt/part";
	private static final String DEFAULT_VIEW_PREF_KEY = "DefaultConfigSpecView";
	public static final boolean VERBOSE;
	private static final String WTHOME;
	private static final String DIRSEP;
	private static final String REPHELPER_CLASS = "com.ptc.wvs.server.ui.RepHelper";
	private static final String REPHELPER_METHOD = "loadRepresentation";
	private static final double DTOR = 0.017453292519943295;
	private static final TypeIdentifier PART_TI;
	private static final HashMap<String, String> codingSystemMap = new HashMap();
	private static final String DATE_EFFECTIVITY = "wt.effectivity.WTDatedEffectivity";
	private static final String SERIAL_EFFECTIVITY = "wt.part.ProductSerialNumberEffectivity";
	private static final String LOT_EFFECTIVITY = "wt.part.ProductLotNumberEffectivity";
	private static final String MSN_EFFECTIVITY = "wt.part.ProductMSNEffectivity";
	private static final String BLOCK_EFFECTIVITY = "wt.part.ProductBlockEffectivity";
	public static final String PROD_NUMBER = "prodNumber";
	public static final String TYPE = "type";
	public static final String TARGET_NUMBER = "targetNumber";
	public static final String TARGET_VER = "targetVer";
	public static final String TYPE_MODIFIER = "typeModifier";
	public static final String START_NUMBER = "startNumber";
	public static final String END_NUMBER = "endNumber";
	public static final String START_DATE = "startDate";
	public static final String END_DATE = "endDate";
	public static final String TARGET_VIEW = "targetView";
	public static final String START = "start";
	public static final String END = "end";
	private static final Object LAST_PART_KEY = new Object() {
		public String toString() {
			return "wt.part.LoadPart.LAST_PART_KEY";
		}
	};
	private static final Logger logger = LogR.getLoggerInternal(APPLPartLoader.class.getName());

	public static boolean createPart(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			checkTypedef(PART_TI, var0);
		} catch (WTException var4) {
			LoadServerHelper.printMessage("Typedef check failed");
			var4.printStackTrace();
			return false;
		}

		return createPartObject(var0, var1, var2) && updatePartObject(var0, var1, var2);
	}

	protected static void checkTypedef(TypeIdentifier var0, Hashtable var1) throws WTException {
		String var2 = (String) var1.get("typedef");
		if (var2 != null && !var2.trim().equals("")) {
			if (var2.startsWith("wt.part.WTPart")) {
				var2 = "WCTYPE|" + var2;
			} else if (var2.startsWith("WCTYPE|")) {
				TypeIdentifier var3;
				try {
					var3 = (TypeIdentifier) ReflectionHelper.dynamicInvoke(
							"com.ptc.core.foundation.type.server.impl.TypeHelper", "getTypeIdentifier",
							new Class[]{String.class}, new Object[]{var2});
				} catch (Exception var5) {
					throw new WTException("Could not find TypeIdentifier for typedef: " + var2);
				}

				if (!var3.isDescendedFrom(var0)) {
					throw new WTException(
							"Typedef does not descend from required parent. Child: " + var3 + " Parent: " + var0);
				}
			} else {
				String var10000 = var0.toExternalForm();
				var2 = var10000 + "|" + var2;
			}

			var1.put("typedef", var2);
		} else {
			var1.put("typedef", var0.toExternalForm());
		}

	}

	public static boolean beginCreateWTPart(Hashtable var0, Hashtable var1, Vector var2) {
		resetAttDirectiveFlags();

		try {
			checkTypedef(PART_TI, var0);
		} catch (WTException var4) {
			LoadServerHelper.printMessage("Typedef check failed");
			var4.printStackTrace();
			return false;
		}

		return createPartObject(var0, var1, var2);
	}

	public static boolean endCreateWTPart(Hashtable var0, Hashtable var1, Vector var2) {
		return updatePartObject(var0, var1, var2);
	}

	public static boolean beginCreateOrUpdateWTPart(Hashtable var0, Hashtable var1, Vector var2) {
		resetAttDirectiveFlags();

		try {
			checkTypedef(PART_TI, var0);
		} catch (WTException var4) {
			LoadServerHelper.printMessage("Typedef check failed");
			var4.printStackTrace();
			return false;
		}

		return createPartObject(var0, var1, var2);
	}

	public static boolean endCreateOrUpdateWTPart(Hashtable var0, Hashtable var1, Vector var2) {
		return updatePartObject(var0, var1, var2);
	}

	public static boolean beginWTPM(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			WTPartMaster var3 = WTPartMaster.newWTPartMaster();
			var3.setName(getValue("name", var0, var1, true));
			var3.setNumber(getValue("number", var0, var1, true));
			var3 = cacheMaster(var3);
			return true;
		} catch (WTException var4) {
			LoadServerHelper.printMessage("\nCreate Part Master Failed: " + var4.getLocalizedMessage());
			var4.printStackTrace();
		} catch (Exception var5) {
			LoadServerHelper.printMessage("\nCreate Part Master Failed: " + var5.getMessage());
			var5.printStackTrace();
		}

		return false;
	}

	public static boolean endWTPM(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			LoadValue.endIBAContainer();
			AttributeContainer var3 = LoadValue.extractDefaultIBAContainer();
			WTPartMaster var4 = getMaster();
			if (var3 != null) {
				var4.setAttributeContainer(var3);
			}

			var4 = (WTPartMaster) PersistenceHelper.manager.store(var4);
			var4 = cacheMaster(var4);
			var2.addElement(var4);
			return true;
		} catch (WTException var5) {
			System.out.println("EndCreateWTPM: Exception: " + var5);
			var5.printStackTrace();
			return false;
		}
	}

	public static boolean beginOccurrencedAssemblyAdd(Hashtable var0, Hashtable var1, Vector var2) {
		boolean var3 = addPartToOccurrencedAssembly(var0, var1, var2);
		if (!var3) {
			return var3;
		} else {
			try {
				resetAttDirectiveFlags();
				WTPartUsageLink var4 = getCachedPartUsage();
				LoadValue.establishCurrentIBAHolder(var4);
				LoadValue.beginIBAContainer();
				WTPart var5 = var4.getUsedBy();
				WTPart var6 = var4.getUsedBy();
				if (!WorkInProgressHelper.isCheckedOut(var6)) {
					try {
						var5 = (WTPart) WorkInProgressHelper.service
								.checkout(var6, var6.getContainer().getDefaultCabinet(), "").getWorkingCopy();
					} catch (WTPropertyVetoException var8) {
						var8.printStackTrace();
					}

					String var7 = var4.getComponentId();
					var4 = getUsageLink(var5, var7);
					if (var4 == null) {
						String var10002 = var5.getNumber();
						throw new WTException(
								"For some reason cannot find usagelink for " + var10002 + "With componentId " + var7);
					}
				}

				LoadAttValues.establishCurrentTypeManaged(var4);
				cachePartUsageObject(var4);
				cacheChoiceMappableObject(var4);
				cacheExpressionableObject(var4);
				cachePartUsesOccurance(var4);
				return var3;
			} catch (WTException var9) {
				LoadServerHelper.printMessage("beginAddPartToOccurrencedAssembly failed");
				var9.printStackTrace();
				return false;
			}
		}
	}

	private static void cachePartUsesOccurance(WTPartUsageLink var0) throws WTException {
		QuerySpec var1 = new QuerySpec();
		int var2 = var1.addClassList(PartUsesOccurrence.class, true);
		var1.appendWhere(new SearchCondition(PartUsesOccurrence.class, "linkReference.key.id", "=",
				PersistenceHelper.getObjectIdentifier(var0).getId()), new int[]{var2});
		QueryResult var3 = PersistenceHelper.manager.find(var1);
		HashMap var4 = new HashMap();

		while (var3.hasMoreElements()) {
			Persistable[] var5 = (Persistable[]) var3.nextElement();
			PartUsesOccurrence var6 = (PartUsesOccurrence) var5[0];
			var4.put(var6.getName(), var6);
		}

		LoadServerHelper.setCacheValue("PARTOCCURRENCE_KEY", var4);
	}

	protected static WTPartUsageLink getUsageLink(Persistable var0, String var1) throws WTException {
		QuerySpec var2 = new QuerySpec(WTPartUsageLink.class);
		var2.appendWhere(new SearchCondition(WTPartUsageLink.class, "roleAObjectRef.key.id", "=",
				PersistenceHelper.getObjectIdentifier(var0).getId()), new int[]{0});
		var2.appendAnd();
		var2.appendWhere(new SearchCondition(WTPartUsageLink.class, "componentId", "=", var1), new int[]{0});
		QueryResult var3 = PersistenceHelper.manager.find(var2);
		WTPartUsageLink var4 = null;
		if (var3.hasMoreElements()) {
			var4 = (WTPartUsageLink) var3.nextElement();
		}

		return var4;
	}

	public static boolean endOccurrencedAssemblyAdd(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			boolean var3 = Boolean.TRUE.equals(MethodContext.getContext().get("csvLoadValue key"));
			WTPartUsageLink var4 = getCachedPartUsage();
			if (var4 == null) {
				throw new WTException("For some reason cannot find cached usagelink ");
			}

			if (var3) {
				var4 = (WTPartUsageLink) LoadAttValues.getCurrentTypeManaged();
				var4 = (WTPartUsageLink) PersistenceHelper.manager.modify(var4);
			}

			boolean var5 = Boolean.TRUE.equals(MethodContext.getContext().get("csvIBAValue load directive was used"));
			if (var5) {
				var4 = (WTPartUsageLink) LoadValue.applySoftAttributes(var4);
				var4 = (WTPartUsageLink) PersistenceHelper.manager.modify(var4);
			}

			WTPart var6 = var4.getUsedBy();
			var6 = (WTPart) WorkInProgressHelper.service.checkin(var6, "");
			cachePart(var6);
			cachePartUsageObject(var4);
			LoadServerHelper.removeCacheValue("PARTOCCURRENCE_KEY");
			boolean var7 = true;
			return var7;
		} catch (WTException var12) {
			System.out.println("endAddPartToOccurrencedAssembly: Exception: " + var12);
			var12.printStackTrace();
		} catch (WTPropertyVetoException var13) {
			System.out.println("endAddPartToOccurrencedAssembly: Exception: " + var13);
			var13.printStackTrace();
		} finally {
			LoadValue.endIBAContainer();
			resetAttDirectiveFlags();
		}

		return false;
	}

	private static boolean isAttributeContainerPopulated(AttributeContainer var0) throws WTException {
		return var0 != null && var0 instanceof DefaultAttributeContainer
				&& ((DefaultAttributeContainer) var0).getAttributeValues() != null
				&& ((DefaultAttributeContainer) var0).getAttributeValues().length > 0;
	}

	public static boolean setAsPrimaryEndItem(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			String var3 = getValue("partNumber", var0, var1, false);
			WTContainerRef var4 = LoadServerHelper.getTargetContainer(var0, var1);
			WTContainer var5 = var4.getReferencedContainer();
			if (!(var5 instanceof PDMLinkProduct)) {
				LoadServerHelper
						.printMessage("SetAsPrimaryEndItem failed because the target container is not a Product");
				return false;
			} else {
				PDMLinkProduct var6 = (PDMLinkProduct) var5;
				WTPartMaster var7 = getMaster(var3, var4);
				if (var7 == null) {
					LoadServerHelper.printMessage(
							"SetAsPrimaryEndItem failed because the target Product does not contain a Part with NUMBER="
									+ var3);
					return false;
				} else if (var7.isEndItem()) {
					var6.setProduct(var7);
					var6 = (PDMLinkProduct) PersistenceHelper.manager.save(var6);
					return true;
				} else {
					LoadServerHelper
							.printMessage("SetAsPrimaryEndItem failed because Part '" + var3 + "' is not an End Item");
					return false;
				}
			}
		} catch (WTException var8) {
			var8.printStackTrace();
			return false;
		} catch (WTPropertyVetoException var9) {
			var9.printStackTrace();
			return false;
		}
	}

	public static boolean addPartToAssembly(Hashtable var0, Hashtable var1, Vector var2) {
		return addPartToAssembly(var0, var1, var2, false);
	}

	public static boolean addPartToAssemblyLoad(Hashtable var0, Hashtable var1, Vector var2) {
		return addPartToAssembly(var0, var1, var2, false);
	}

	public static boolean createPartDocReference(Hashtable var0, Hashtable var1, Vector var2) {
		return createPartDocLink(var0, var1, var2, 0, false);
	}

	public static boolean createPartDocDescribes(Hashtable var0, Hashtable var1, Vector var2) {
		return createPartDocLink(var0, var1, var2, 1, false);
	}

	public static boolean createPartDocLink(Hashtable var0, Hashtable var1, Vector var2) {
		return createPartDocLink(var0, var1, var2, 2, false);
	}

	public static boolean beginWTPartDescribeLink(Hashtable var0, Hashtable var1, Vector var2) {
		boolean var3 = createPartDocDescribes(var0, var1, var2);
		if (!var3) {
			return var3;
		} else {
			try {
				WTPartDescribeLink var4 = (WTPartDescribeLink) getCachedPartDocLinks();
				LoadValue.establishCurrentIBAHolder(var4);
				LoadValue.beginIBAContainer();
				return var3;
			} catch (WTException var5) {
				LoadServerHelper.printMessage("beginWTPartDescribeLink failed");
				var5.printStackTrace();
				return false;
			}
		}
	}

	public static boolean endWTPartDescribeLink(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			WTPartDescribeLink var3 = (WTPartDescribeLink) getCachedPartDocLinks();
			var3 = (WTPartDescribeLink) LoadValue.applySoftAttributes(var3);
			boolean var4 = true;
			return var4;
		} catch (WTException var8) {
			System.out.println("endWTPartDescribeLink: Exception: " + var8);
			var8.printStackTrace();
		} finally {
			LoadValue.endIBAContainer();
		}

		return false;
	}

	public static boolean beginWTPartReferenceLink(Hashtable var0, Hashtable var1, Vector var2) {
		boolean var3 = createPartDocReference(var0, var1, var2);
		if (!var3) {
			return var3;
		} else {
			try {
				WTPartReferenceLink var4 = (WTPartReferenceLink) getCachedPartDocLinks();
				LoadValue.establishCurrentIBAHolder(var4);
				LoadValue.beginIBAContainer();
				return var3;
			} catch (WTException var5) {
				LoadServerHelper.printMessage("beginWTPartReferenceLink failed");
				var5.printStackTrace();
				return false;
			}
		}
	}

	public static boolean endWTPartReferenceLink(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			WTPartReferenceLink var3 = (WTPartReferenceLink) getCachedPartDocLinks();
			var3 = (WTPartReferenceLink) LoadValue.applySoftAttributes(var3);
			boolean var4 = true;
			return var4;
		} catch (WTException var8) {
			System.out.println("endWTPartReferenceLink: Exception: " + var8);
			var8.printStackTrace();
		} finally {
			LoadValue.endIBAContainer();
		}

		return false;
	}

	public static boolean createPartRepresentationLoad(Hashtable var0, Hashtable var1, Vector var2) {
		return createPartRepresentation(var0, var1, var2, 2, false);
	}

	public static boolean createPartRepresentation(Hashtable var0, Hashtable var1, Vector var2) {
		return createPartRepresentation(var0, var1, var2, 2, true);
	}

	public static boolean removePartFromAssembly(Hashtable var0, Hashtable var1, Vector var2) {
		return removePartFromAssembly(var0, var1, var2, true);
	}

	public static boolean removePartFromAssemblyLoad(Hashtable var0, Hashtable var1, Vector var2) {
		return removePartFromAssembly(var0, var1, var2, false);
	}

	public static boolean addPartToOccurrencedAssembly(Hashtable var0, Hashtable var1, Vector var2) {
		return addPartToAssembly(var0, var1, var2, true);
	}

	public static boolean addPartToOccurrencedAssemblyLoad(Hashtable var0, Hashtable var1, Vector var2) {
		return addPartToAssembly(var0, var1, var2, true);
	}

	public static boolean createPartDocReferenceOld(Hashtable var0, Hashtable var1, Vector var2) {
		return createPartDocReference(var0, var1, var2);
	}

	public static boolean createPartDocReferenceLoadOld(Hashtable var0, Hashtable var1, Vector var2) {
		return createPartDocLink(var0, var1, var2, 0, true);
	}

	public static boolean assignUserToProduct(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			String[] var3 = new String[1];
			String var4 = null;
			String var5 = getValue("user", var0, var1, true);
			String var6 = getValue("name", var0, var1, true);
			String var7 = getValue("role", var0, var1, true);
			WTContainerRef var8 = LoadServerHelper.getTargetContainer(var0, var1);
			if (VERBOSE) {
				System.out.println("In assignUserToProduct");
				System.out.println("<<user>> = <<" + var5 + ">>");
				System.out.println("<<name>> = <<" + var6 + ">>");
				System.out.println("<<role>> = <<" + var7 + ">>");
				System.out.println("<<containerRef>> = <<" + var8 + ">>");
			}

			WTUser var9 = null;
			Enumeration var10 = OrganizationServicesHelper.manager.findUser("name", var5);

			while (var10.hasMoreElements()) {
				var9 = (WTUser) var10.nextElement();
				if (VERBOSE) {
					System.out.println("Located user " + var5);
				}
			}

			if (var9 == null) {
				var3[0] = var5;
				var4 = WTMessage.getLocalizedMessage(RESOURCE, "201", var3);
				LoadServerHelper.printMessage("\n" + var4);
				return false;
			} else {
				PDMLinkProduct var11 = lookupProduct(var6, var8);
				if (var11 == null) {
					var3[0] = var6;
					var4 = WTMessage.getLocalizedMessage(RESOURCE, "202", var3);
					LoadServerHelper.printMessage("\n" + var4);
					return false;
				} else {
					if (VERBOSE) {
						System.out.println("Found  Product " + var11);
					}

					ContainerTeam var12 = ContainerTeamHelper.service.getContainerTeam(var11);
					if (VERBOSE) {
						System.out.println("Found  ContainerTeam " + var12);
					}

					Role var13 = Role.toRole(var7);
					if (var13 == null) {
						var3[0] = var7;
						var4 = WTMessage.getLocalizedMessage(RESOURCE, "203", var3);
						LoadServerHelper.printMessage("\n" + var4);
						return false;
					} else {
						var12.addPrincipal(var13, var9);
						if (VERBOSE) {
							System.out.println("Successfully added " + var5 + " to " + var6 + " with the role " + var7);
						}

						return true;
					}
				}
			}
		} catch (WTException var14) {
			LoadServerHelper.printMessage("\nAssign User to Product Failed: " + var14.getLocalizedMessage());
			var14.printStackTrace();
			return false;
		} catch (Exception var15) {
			LoadServerHelper.printMessage("\nAssign User to Product Failed: " + var15.getMessage());
			var15.printStackTrace();
			return false;
		}
	}

	public static boolean setUserDefaultView(Hashtable var0, Hashtable var1, Vector var2) {
		String var3 = null;

		boolean var5;
		try {
			String var4 = LoadServerHelper.getValue("view", var0, var1, 0);
			String var20 = LoadServerHelper.getValue("user", var0, var1, 0);
			var3 = SessionHelper.getPrincipal().getName();
			LoadServerHelper.changePrincipal(var20);
			View var6 = ViewHelper.service.getView(var4);
			boolean var7;
			if (var6 != null) {
				WTPartHelper.service.setDefaultConfigSpecViewPref(var6);
				var7 = true;
				return var7;
			}

			var7 = false;
			return var7;
		} catch (WTException var18) {
			LoadServerHelper.printMessage("\nSetUserdefaultView Failed: " + var18.getLocalizedMessage());
			var18.printStackTrace();
			var5 = false;
		} finally {
			try {
				LoadServerHelper.changePrincipal(var3);
			} catch (WTException var17) {
				var17.printStackTrace();
			}

		}

		return var5;
	}

	public static boolean setSiteDefaultView(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			String var3 = LoadServerHelper.getValue("view", var0, var1, 0);
			View var4 = ViewHelper.service.getView(var3);
			if (var4 == null) {
				if (VERBOSE) {
					System.out.println("View " + var3 + " could not be found");
				}

				return false;
			} else {
				WTPreferences var5 = (WTPreferences) WTPreferences.root().node("wt/part");
				var5.setEditContext(PreferenceHelper.createEditMask(WTContainerHelper.getExchangeRef(),
						(WTUser) SessionHelper.getPrincipal(), false));
				if (VERBOSE) {
					System.out.println("Setting Site Default View Preference to " + var4.getName());
				}

				var5.put("DefaultConfigSpecView", var4.getName());
				return true;
			}
		} catch (WTException var6) {
			LoadServerHelper.printMessage("\nSetSiteDefaultView Failed: " + var6.getLocalizedMessage());
			var6.printStackTrace();
			return false;
		}
	}

	public static String getValue(Hashtable var0, String var1, boolean var2) throws WTException {
		return getValue(var1, var0, new Hashtable(), var2);
	}

	public static boolean isCheckoutAllowed(Workable var0) throws LockException, WTException {
		return !WorkInProgressHelper.isWorkingCopy(var0) && !WorkInProgressHelper.isCheckedOut(var0)
				&& !LockHelper.isLocked(var0);
	}

	public static boolean isUndoCheckoutAllowed(Workable var0) {
		try {
			return WorkInProgressHelper.isWorkingCopy(var0) && WorkInProgressHelper.isCheckedOut(var0);
		} catch (WTException var2) {
			return false;
		}
	}

	public static Workable getCheckOutObject(Workable var0) throws LockException, WTException {
		Workable var1 = null;

		try {
			if (isCheckoutAllowed(var0)) {
				WorkInProgressHelper.service.checkout(var0, WorkInProgressHelper.service.getCheckoutFolder(),
						"Updating attributes during load.");
				var1 = WorkInProgressHelper.service.workingCopyOf(var0);
			}
		} catch (Exception var3) {
			var3.printStackTrace();
			throw new WTException(var3.getMessage());
		}

		if (var1 == null) {
			throw new WTException("Checkout Failed!");
		} else {
			return var1;
		}
	}

	protected static boolean createPartObject(Hashtable var0, Hashtable var1, Vector var2) {
		String var10000;
		try {
			setUser(var0, var1);
			WTPart var3 = constructPart(var0, var1);
			var3 = cachePart(var3);
			return true;
		} catch (WTException var4) {
			var10000 = getDisplayInfo(var0, var1);
			LoadServerHelper.printMessage("\nCreate Part Failed (" + var10000 + "): " + var4.getLocalizedMessage());
			var4.printStackTrace();
		} catch (Exception var5) {
			var10000 = getDisplayInfo(var0, var1);
			LoadServerHelper.printMessage("\nCreate Part Failed (" + var10000 + "): " + var5.getMessage());
			var5.printStackTrace();
		}

		return false;
	}

	protected static String getDisplayInfo(Hashtable var0, Hashtable var1) {
		String var2 = null;
		String var3 = null;
		String var4 = null;
		String var5 = null;

		try {
			var2 = getValue("number", var0, var1, false);
			if (var2 == null) {
				var2 = getValue("partNumber", var0, var1, false);
			}

			var3 = getValue("version", var0, var1, false);
			var4 = getValue("iteration", var0, var1, false);
			var5 = getValue("view", var0, var1, false);
		} catch (WTException var7) {
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

		if (var5 == null) {
			var5 = "<view>";
		}

		return var2 + " " + var3 + "." + var4 + "," + var5;
	}

	protected static boolean updatePartObject(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			boolean var4 = Boolean.TRUE.equals(MethodContext.getContext().get("csvLoadValue key"));
			WTPart var3;
			if (var4) {
				var3 = (WTPart) LoadAttValues.getCurrentTypeManaged();
				var3 = (WTPart) PersistenceHelper.manager.modify(var3);
				if (logger.isDebugEnabled()) {
					logger.error("new soft att directive was used for part: " + var3.getName());
				}
			} else {
				var3 = getPart();
				if (logger.isDebugEnabled()) {
					logger.error("new soft att value directive was *not* used for part: " + var3.getName());
				}
			}

			boolean var5 = Boolean.TRUE.equals(MethodContext.getContext().get("csvIBAValue load directive was used"));
			if (var5) {
				var3 = (WTPart) LoadValue.applySoftAttributes(var3);
				if (logger.isDebugEnabled()) {
					logger.debug("old iba att value directive was used for part: " + var3.getName());
				}
			} else if (logger.isDebugEnabled()) {
				logger.debug("old iba att value directive was *not* used for part: " + var3.getName());
			}

			if (var4 || var5) {
				var3 = cachePart(var3);
				PersistenceHelper.manager.save(var3.getMaster());
			}

			var2.addElement(var3);
			boolean var6 = true;
			return var6;
		} catch (WTException var18) {
			LoadServerHelper.printMessage("\nUpdate Part Failed: " + var18.getLocalizedMessage());
			var18.printStackTrace();
		} catch (Exception var19) {
			LoadServerHelper.printMessage("\nUpdate Part Failed: " + var19.getMessage());
			var19.printStackTrace();
		} finally {
			try {
				resetUser();
			} catch (WTException var17) {
				LoadServerHelper.printMessage("\nUpdate Part Failed: " + var17.getMessage());
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

	protected static Object getFirstIterationOf(Mastered var0) throws WTException {
		Class var1 = var0.getClassInfo().getOtherSideRole("iteration").getValidClassInfo().getBusinessClass();
		QuerySpec var2 = new QuerySpec(var1);
		var2.appendWhere(VersionControlHelper.getSearchCondition(var1, var0), new int[]{0});
		var2.appendAnd();
		var2.appendWhere(new SearchCondition(var1, "iterationInfo.predecessor.key.id", "=", 0L));
		QueryResult var3 = PersistenceHelper.manager.find(var2);
		return var3.size() == 0 ? null : var3.nextElement();
	}

	protected static boolean addPartToAssembly(Hashtable var0, Hashtable var1, Vector var2, boolean var3) {
		TypeIdentifier var4 = null;

		try {
			String[] var5 = new String[2];
			String var7 = getValue("assemblyPartVersion", var0, var1, false);
			String var8 = getValue("assemblyPartIteration", var0, var1, false);
			String var9 = getValue("assemblyPartView", var0, var1, false);
			String var10 = getValue("assemblyPartVariation1", var0, var1, false);
			String var11 = getValue("assemblyPartVariation2", var0, var1, false);
			String var12 = getValue("organizationName", var0, var1, false);
			String var13 = getValue("organizationID", var0, var1, false);
			String var14 = getValue("componentId", var0, var1, false);
			String var15 = getValue("inclusionOption", var0, var1, false);
			String var16 = getValue("quantityOption", var0, var1, false);
			String var17 = getValue("reference", var0, var1, false);
			if (var7 != null && var7.equals("")) {
				var7 = null;
			}

			if (var8 != null && var8.equals("")) {
				var8 = null;
			}

			if (var9 != null && var9.equals("")) {
				var9 = null;
			}

			if (var10 != null && var10.equals("")) {
				var10 = null;
			}

			if (var11 != null && var11.equals("")) {
				var11 = null;
			}

			if (var12 != null && var12.equals("")) {
				var12 = null;
			}

			if (var13 != null && var13.equals("")) {
				var13 = null;
			}

			String var18 = getValue("assemblyPartNumber", var0, var1, true);
			WTPart var19 = getPart(var18, var7, var8, var9, var10, var11, var13, var12);
			String var6;
			if (var19 == null) {
				var5[0] = getValue("assemblyPartNumber", var0, var1, true);
				var6 = WTMessage.getLocalizedMessage(RESOURCE, "4", var5);
				LoadServerHelper.printMessage(var6);
			} else {
				WTPartMaster var20 = getMaster(getValue("constituentPartNumber", var0, var1, true));
				if (var20 != null) {
					if (isChildSameAsParent(var19, var20)) {
						var5[0] = var20.getIdentity();
						var6 = WTMessage.getLocalizedMessage(RESOURCE, "218", var5);
						LoadServerHelper.printMessage(var6);
						return false;
					}

					TypeIdentifier var21 = TypeIdentifierHelper.getType(var19);
					var4 = WTPartHelper.service.getValidUsageLinkType(var21);
					if (!AssociationConstraintHelper.service.isValidAssociation(var19, var4, var20)) {
						Object var51 = getFirstIterationOf(var20);
						Object[] var52 = new Object[]{var19.getIdentity(), var20.getIdentity(), var21.getTypename(),
								TypeIdentifierHelper.getType(var51).getTypename()};
						var6 = WTMessage.getLocalizedMessage(RESOURCE, "237", var52);
						LoadServerHelper.printMessage(var6);
						return false;
					}

					WTPartUsageLink var22 = WTPartUsageLink.newWTPartUsageLink(var19, var20);
					var22.setTypeDefinitionReference(TypedUtility.getTypeDefinitionReference(var4.getTypename()));
					WTHashSet var23 = new WTHashSet(WTHashSet.getInitialCapacity(1));
					var23.add(var22);
					StructServerHelper.validateRoleObjectContainers(var23);
					double var24 = Double.valueOf(getValue("constituentPartQty", var0, var1, true));
					QuantityUnit var26 = QuantityUnit.toQuantityUnit(getValue("constituentPartUnit", var0, var1, true));
					if (QuantityUnit.EA.equals(var26) && var24 == 0.0) {
						var5[0] = var20.getIdentity();
						var5[1] = QuantityUnit.EA.getDisplay();
						var6 = WTMessage.getLocalizedMessage(RESOURCE, "270", var5);
						LoadServerHelper.printMessage(var6);
						return false;
					}

					var22.setQuantity(Quantity.newQuantity(var24, var26));
					if (var14 != null && !var14.equals("")) {
						boolean var27 = false;
						Field[] var28 = var22.getClass().getSuperclass().getDeclaredFields();
						Field[] var29 = var28;
						int var30 = var28.length;

						int var31;
						Field var32;
						for (var31 = 0; var31 < var30; ++var31) {
							var32 = var29[var31];
							if (var32.getName().equals("componentId")) {
								var32.setAccessible(true);
								var32.set(var22, var14);
								var27 = true;
								break;
							}
						}

						if (!var27) {
							var28 = var22.getClass().getSuperclass().getDeclaredFields();
							var29 = var28;
							var30 = var28.length;

							for (var31 = 0; var31 < var30; ++var31) {
								var32 = var29[var31];
								if (var32.getName().equals("componentId")) {
									var32.setAccessible(true);
									var32.set(var22, var14);
									var27 = true;
									break;
								}
							}
						}
					}

					if (var15 != null && !var15.equals("")) {
						var22.setInclusionOption(var15);
					}

					if (var16 != null && !var16.equals("")) {
						var22.setQuantityOption(var16);
					}

					if (var17 != null && !var17.equals("")) {
						var22.setReference(var17);
					}

					String var53 = getValue("lineNumber", var0, var1, false);
					if (var53 != null && !var53.equals("")) {
						LineNumber var54 = LineNumber.newLineNumber(Long.valueOf(var53));
						var22.setLineNumber(var54);
					}

					String var55 = getValue("findNumber", var0, var1, false);
					if (var55 != null && !var55.equals("")) {
						var22.setFindNumber(var55);
					}

					PersistenceServerHelper.manager.insert(var22);
					cachePartUsageObject(var22);
					cacheChoiceMappableObject(var22);
					cacheExpressionableObject(var22);
					if (var3 && QuantityUnit.EA.equals(var26)) {
						int var56 = Double.valueOf(var24).intValue();
						String[] var57 = getValues("occurrenceLocation", var0, var1, false);
						String[] var58 = getValues("referenceDesignator", var0, var1, false);
						if (var57 != null || var58 != null) {
							String var59 = "";
							String var33 = "";

							for (int var34 = 0; var34 < var56; ++var34) {
								if (var57 != null && var34 < var57.length) {
									var59 = var57[var34];
								} else {
									var59 = "";
								}

								if (var58 != null && var34 < var58.length) {
									var33 = var58[var34];
								} else {
									var33 = "";
								}

								if (var59 == null) {
									var59 = "";
								}

								PartUsesOccurrence var35 = PartUsesOccurrence.newPartUsesOccurrence(var22);
								var35.setName(var33);
								var35.setTransform(getLocationMatrix(var59));
								Object var36 = null;

								try {
									OccurrenceHelper.service.setSkipValidation(true);
								} catch (Exception var47) {
									var47.printStackTrace();
								}

								try {
									if (var33 != null && !var33.equals("")) {
										OccurrenceHelper.service.saveUsesOccurrenceAndData(var35, (Vector) var36);
									}
								} finally {
									try {
										OccurrenceHelper.service.setSkipValidation(false);
									} catch (Exception var46) {
										var46.printStackTrace();
									}

								}
							}
						}
					}

					var5[0] = var20.getIdentity();
					var5[1] = var19.getIdentity();
					var6 = WTMessage.getLocalizedMessage(RESOURCE, "5", var5);
					var2.addElement(var6);
					return true;
				}

				var5[0] = getValue("constituentPartNumber", var0, var1, true);
				var6 = WTMessage.getLocalizedMessage(RESOURCE, "4", var5);
				LoadServerHelper.printMessage(var6);
			}
		} catch (WTException var49) {
			LoadServerHelper.printMessage("\naddPartToAssembly: " + var49.getLocalizedMessage());
			var49.printStackTrace();
		} catch (Exception var50) {
			LoadServerHelper.printMessage("\naddPartToAssembly: " + var50.getMessage());
			var50.printStackTrace();
		}

		return false;
	}

	protected static boolean createPartDocLink(Hashtable var0, Hashtable var1, Vector var2, int var3, boolean var4) {
		try {
			String[] var5 = new String[2];
			String var7 = getValue("partNumber", var0, var1, false);
			String var8 = getValue("partVersion", var0, var1, false);
			String var9 = getValue("partIteration", var0, var1, false);
			String var10 = getValue("partView", var0, var1, false);
			String var11 = getValue("partVariation1", var0, var1, false);
			String var12 = getValue("partVariation2", var0, var1, false);
			String var13 = getValue("organizationName", var0, var1, false);
			String var14 = getValue("organizationID", var0, var1, false);
			if (var10 != null && var10.equals("")) {
				var10 = null;
			}

			if (var11 != null && var11.equals("")) {
				var11 = null;
			}

			if (var12 != null && var12.equals("")) {
				var12 = null;
			}

			if (var13 != null && var13.equals("")) {
				var13 = null;
			}

			if (var14 != null && var14.equals("")) {
				var14 = null;
			}

			WTPart var15 = getPart(var7, var8, var9, var10, var11, var12, var14, var13);
			String var6;
			if (var15 == null) {
				var6 = WTMessage.getLocalizedMessage(RESOURCE, "8", var5);
				LoadServerHelper.printMessage(var6);
			} else {
				String var16 = getValue("docNumber", var0, var1, false);
				String var17 = getValue("docVersion", var0, var1, false);
				String var18 = getValue("docIteration", var0, var1, false);
				WTDocument var19 = LoadDoc.getDocument(var16, var17, var18);
				if (var19 == null) {
					LoadServerHelper.printMessage("Document not found");
				} else if (isValidPartDocAssociation(var3, var15, var19)) {
					boolean var20 = false;
					Object var21 = null;
					WTDocumentMaster var22 = (WTDocumentMaster) var19.getMaster();
					boolean var23 = false;
					boolean var24 = false;
					if (var3 == 0) {
						var24 = isDuplicatePartDocRecord(var15, var22);
					} else if (var3 == 1) {
						var23 = isDuplicatePartDocRecord(var15, var19);
					}

					Logger var10000;
					String var10001;
					LocalizableMessage var32;
					switch (var3) {
						case 0 :
							if (var24) {
								var10000 = logger;
								var32 = IdentityFactory.getDisplayIdentifier(var15);
								var10000.error("A reference link already exists between part " + var32
										+ " and document " + IdentityFactory.getDisplayIdentifier(var22) + ".");
								return true;
							}

							if (!PartDocHelper.isWcPDMMethod() && !PartDocHelper.isReferenceDocument(var19)) {
								logger.error("Cannot load a part reference link:");
								logger.error("  part[" + var15 + "], document[" + var19 + "]");
								logger.error("since the specified document is not a reference document");
								String var10002 = var19.getNumber();
								throw new WTException("Not a reference document: number[" + var10002 + ", name["
										+ var19.getName() + "]");
							}

							var21 = WTPartReferenceLink.newWTPartReferenceLink(var15, var22);
							break;
						case 1 :
							String var25 = getValue("docRemoveOldRev", var0, var1, false);
							if (var25 != null && var25.length() > 0) {
								var20 = Boolean.valueOf(var25);
							}

							if (logger.isDebugEnabled()) {
								logger.debug("'docRemoveOldRev' flag : " + var20);
							}

							if (var23) {
								var10000 = logger;
								var10001 = var15.getNumber();
								var10000.error("Part-Doc describe link from [" + var10001 + " - " + var15.getName()
										+ "] to [" + var19.getNumber() + " - " + var19.getName() + "] already exists.");
								return true;
							}

							if (var20) {
								WTPartDescribeLink var26 = getLatestPartDocLink(var15,
										(WTDocumentMaster) var19.getMasterReference().getObject());
								if (var26 != null) {
									if (logger.isDebugEnabled()) {
										logger.debug("Described Link to be deleted : " + var26);
									}

									String var27 = var19.getVersionIdentifier().getValue();
									WTDocument var28 = (WTDocument) var26.getRoleBObject();
									String var29 = var28.getVersionIdentifier().getValue();
									if (!var27.equals(var29)) {
										PersistenceServerHelper.manager.remove(var26);
										if (logger.isDebugEnabled()) {
											logger.debug("Described Link '" + var26 + " with RoleB document '" + var28
													+ "' is successfully deleted.");
										}
									}
								}
							}

							var21 = WTPartDescribeLink.newWTPartDescribeLink(var15, var19);
							break;
						default :
							String var34 = TypedUtility
									.getPersistedType("WCTYPE|wt.doc.WTDocument|com.ptc.ReferenceDocument");
							if (var34 == null) {
								System.out.println(
										" WARNING - WCTYPE|wt.doc.WTDocument|Reference Document is not defined.\n Run loaddata.bat PLMLinkPartDocs.csv");
							}

							if (TypedUtility.isInstanceOf(TypedUtility.getPersistedType(var19), var34)) {
								if (isDuplicatePartDocRecord(var15, var22)) {
									var10000 = logger;
									var32 = IdentityFactory.getDisplayIdentifier(var15);
									var10000.error("A reference link already exists between part " + var32
											+ " and document " + IdentityFactory.getDisplayIdentifier(var22) + ".");
									return true;
								}

								var21 = WTPartReferenceLink.newWTPartReferenceLink(var15, var22);
							} else {
								if (isDuplicatePartDocRecord(var15, var19)) {
									var10000 = logger;
									var10001 = var15.getNumber();
									var10000.error("Part-Doc describe link from [" + var10001 + " - " + var15.getName()
											+ "] to [" + var19.getNumber() + " - " + var19.getName()
											+ "] already exists.");
									return true;
								}

								var21 = WTPartDescribeLink.newWTPartDescribeLink(var15, var19);
							}
					}

					PersistenceServerHelper.manager.insert((Persistable) var21);
					Persistable var33 = cachePartDocLinkObject((Persistable) var21);
					var5[0] = var19.getIdentity();
					var5[1] = var15.getIdentity();
					var6 = WTMessage.getLocalizedMessage(RESOURCE, "6", var5);
					var2.addElement(var6);
					return true;
				}
			}
		} catch (WTException var30) {
			LoadServerHelper.printMessage("\ncreatePartDocLink: " + var30.getLocalizedMessage());
			var30.printStackTrace();
		} catch (Exception var31) {
			LoadServerHelper.printMessage("\ncreatePartDocLink: " + var31.getMessage());
			var31.printStackTrace();
		}

		return false;
	}

	private static WTPartDescribeLink getLatestPartDocLink(WTPart var0, WTDocumentMaster var1) throws WTException {
		WTPartDescribeLink var2 = null;
		QuerySpec var3 = new QuerySpec();
		int var4 = var3.addClassList(WTPartDescribeLink.class, true);
		int var5 = var3.addClassList(WTDocument.class, false);
		var3.setAdvancedQueryEnabled(true);
		var3.appendWhere(new SearchCondition(WTPartDescribeLink.class, "roleAObjectRef.key.id", "=",
				var0.getPersistInfo().getObjectIdentifier().getId()), new int[]{var4});
		var3.appendAnd();
		var3.appendWhere(new SearchCondition(WTDocument.class, "masterReference.key.id", "=",
				var1.getPersistInfo().getObjectIdentifier().getId()), new int[]{var5});
		var3.appendAnd();
		var3.appendWhere(
				new SearchCondition(new ClassAttribute(WTPartDescribeLink.class, "roleBObjectRef.key.id"), "=",
						new ClassAttribute(WTDocument.class, "thePersistInfo.theObjectIdentifier.id")),
				new int[]{var4, var5});
		var3.appendAnd();
		var3.appendWhere(new SearchCondition(WTDocument.class, "iterationInfo.latest", "TRUE"), new int[]{var5});
		var3.appendOrderBy(
				new OrderBy(new ClassAttribute(WTDocument.class, "versionInfo.identifier.versionSortId"), true),
				new int[]{var5});
		if (logger.isDebugEnabled()) {
			logger.debug("Query : " + var3);
		}

		QueryResult var6 = PersistenceHelper.manager.find(var3);
		if (logger.isDebugEnabled()) {
			logger.debug("Query Result size : " + var6.size());
		}

		if (var6.hasMoreElements()) {
			Persistable[] var7 = (Persistable[]) var6.nextElement();
			var2 = (WTPartDescribeLink) var7[0];
		}

		return var2;
	}

	protected static boolean removePartFromAssembly(Hashtable var0, Hashtable var1, Vector var2, boolean var3) {
		try {
			String[] var4 = new String[2];
			String var6 = getValue("assemblyPartNumber", var0, var1, true);
			String var7 = getValue("assemblyPartVersion", var0, var1, false);
			String var8 = getValue("assemblyPartIteration", var0, var1, false);
			String var9 = getValue("assemblyPartView", var0, var1, false);
			String var10 = getValue("assemblyPartVariation1", var0, var1, false);
			String var11 = getValue("assemblyPartVariation2", var0, var1, false);
			String var12 = getValue("organizationName", var0, var1, false);
			String var13 = getValue("organizationID", var0, var1, false);
			if (var7 != null && var7.equals("")) {
				var7 = null;
			}

			if (var8 != null && var8.equals("")) {
				var8 = null;
			}

			if (var9 != null && var9.equals("")) {
				var9 = null;
			}

			if (var10 != null && var10.equals("")) {
				var10 = null;
			}

			if (var11 != null && var11.equals("")) {
				var11 = null;
			}

			if (var12 != null && var12.equals("")) {
				var12 = null;
			}

			if (var13 != null && var13.equals("")) {
				var13 = null;
			}

			WTPart var14 = getPart(var6, var7, var8, var9, var10, var11, var13, var12);
			String var5;
			if (var14 == null) {
				var4[0] = getValue("assemblyPartNumber", var0, var1, true);
				var5 = WTMessage.getLocalizedMessage(RESOURCE, "185", var4);
				LoadServerHelper.printMessage(var5);
			} else {
				WTPartMaster var15 = getMaster(getValue("constituentPartNumber", var0, var1, true));
				if (var15 == null) {
					var4[0] = getValue("constituentPartNumber", var0, var1, true);
					var5 = WTMessage.getLocalizedMessage(RESOURCE, "185", var4);
					LoadServerHelper.printMessage(var5);
				} else {
					try {
						QueryResult var16 = null;
						var16 = StructHelper.service.navigateUses(var14, false);
						Vector var17 = OccurrenceHelper.service
								.getPopulatedOccurrenceableLinks(var16.getObjectVectorIfc().getVector());
						Iterator var18 = var17.iterator();

						while (true) {
							WTPartUsageLink var19;
							WTPartMaster var20;
							do {
								if (!var18.hasNext()) {
									DisplayIdentity var10002 = IdentityFactory.getDisplayIdentity(var14);
									throw new RuntimeException(
											"Cannot remove usage link because no usage link exists from " + var10002
													+ " to " + IdentityFactory.getDisplayIdentity(var15));
								}

								var19 = (WTPartUsageLink) var18.next();
								var20 = (WTPartMaster) var19.getRoleBObject();
							} while (!var20.getNumber().equals(var15.getNumber()));

							try {
								Vector var21 = var19.getUsesOccurrenceVector();
								if (var21 == null) {
									var21 = new Vector();
								}

								WTHashSet var22 = new WTHashSet(var21);
								WTHashSet var23 = new WTHashSet();
								Iterator var24 = var21.iterator();

								while (var24.hasNext()) {
									PartUsesOccurrence var25 = (PartUsesOccurrence) var24.next();
									PartPathOccurrence var26 = (PartPathOccurrence) var25.getPathOccurrence();
									if (var26 != null) {
										var23.add(var26);
									}
								}

								PersistenceServerHelper.manager.remove(var23);
								PersistenceServerHelper.manager.remove(var22);
								var19 = (WTPartUsageLink) PersistenceServerHelper.manager.restore(var19);
								PersistenceServerHelper.manager.remove(var19);
								var14 = cachePart(var14);
								return true;
							} catch (WTException var27) {
								var27.printStackTrace();
							}
						}
					} catch (Exception var28) {
						var28.printStackTrace();
					}
				}
			}
		} catch (Exception var29) {
			var29.printStackTrace();
		}

		return false;
	}

	public static boolean createNewViewVersion(Hashtable var0, Hashtable var1, Vector var2) throws WTException {
		String var3 = getValue("partNumber", var0, var1, true);
		String var4 = getValue("partVersion", var0, var1, false);
		String var5 = getValue("partIteration", var0, var1, false);
		String var6 = getValue("partView", var0, var1, false);
		String var7 = getValue("partVariation1", var0, var1, false);
		String var8 = getValue("partVariation2", var0, var1, false);
		String var9 = getValue("view", var0, var1, true);
		String var10 = getValue("variation1", var0, var1, false);
		String var11 = getValue("variation2", var0, var1, false);
		String var12 = getValue("organizationName", var0, var1, false);
		String var13 = getValue("organizationID", var0, var1, false);
		if (var4 != null && var4.equals("")) {
			var4 = null;
		}

		if (var5 != null && var5.equals("")) {
			var5 = null;
		}

		if (var6 != null && var6.equals("")) {
			var6 = null;
		}

		if (var7 != null && var7.equals("")) {
			var7 = null;
		}

		if (var8 != null && var8.equals("")) {
			var8 = null;
		}

		if (var10 != null && var10.equals("")) {
			var10 = null;
		}

		if (var11 != null && var11.equals("")) {
			var11 = null;
		}

		if (var12 != null && var12.equals("")) {
			var12 = null;
		}

		if (var13 != null && var13.equals("")) {
			var13 = null;
		}

		WTPart var14 = null;
		Object var15 = null;
		View var16 = null;
		Variation1 var17 = Variation1.toVariation1(var10);
		Variation2 var18 = Variation2.toVariation2(var11);

		String[] var20;
		String var21;
		try {
			var16 = ViewHelper.service.getView(var9);
			if (var6 != null) {
				var14 = getPart(var3, var4, var5, var6, var7, var8, var13, var12);
			} else {
				for (View var19 = ViewHelper.service.getParent(var16); var14 == null
						&& var19 != null; var19 = ViewHelper.service.getParent(var19)) {
					var14 = getPart(var3, var4, var5, var19.getName(), var7, var8, var13, var12);
				}
			}
		} catch (ViewException var24) {
			var20 = new String[]{var9};
			var21 = WTMessage.getLocalizedMessage(RESOURCE, "191", var20);
			LoadServerHelper.printMessage(var21);
			var24.printStackTrace();
			return false;
		} catch (WTException var25) {
			var20 = new String[]{var9};
			var21 = WTMessage.getLocalizedMessage(RESOURCE, "191", var20);
			LoadServerHelper.printMessage(var21);
			var25.printStackTrace();
			return false;
		}

		if (var14 == null) {
			String[] var27 = new String[]{var3};
			String var28 = WTMessage.getLocalizedMessage(RESOURCE, "190", var27);
			LoadServerHelper.printMessage(var28);
			return false;
		} else {
			try {
				WTPart var26 = (WTPart) ViewHelper.service.newBranchForViewAndVariations(var14, var16, var17, var18);
				var26 = applyHardAttributes(var26, var0, var1);
				var26 = (WTPart) PersistenceHelper.manager.store(var26);
				var26 = cachePart(var26);
				return true;
			} catch (WTPropertyVetoException var22) {
				var20 = new String[]{var3};
				var21 = WTMessage.getLocalizedMessage(RESOURCE, "192", var20);
				LoadServerHelper.printMessage(var21);
				var22.printStackTrace();
				return false;
			} catch (WTException var23) {
				var20 = new String[]{var3};
				var21 = WTMessage.getLocalizedMessage(RESOURCE, "192", var20);
				LoadServerHelper.printMessage(var21);
				var23.printStackTrace();
				return false;
			}
		}
	}

	protected static boolean createPartRepresentation(Hashtable var0, Hashtable var1, Vector var2, int var3,
			boolean var4) {
		try {
			String[] var5 = new String[2];
			String var7 = getValue("partNumber", var0, var1, false);
			String var8 = getValue("partVersion", var0, var1, false);
			String var9 = getValue("partIteration", var0, var1, false);
			String var10 = getValue("partView", var0, var1, false);
			String var11 = getValue("partVariation1", var0, var1, false);
			String var12 = getValue("partVariation2", var0, var1, false);
			String var13 = getValue("organizationName", var0, var1, false);
			String var14 = getValue("organizationID", var0, var1, false);
			if (var10 != null && var10.equals("")) {
				var10 = null;
			}

			if (var11 != null && var11.equals("")) {
				var11 = null;
			}

			if (var12 != null && var12.equals("")) {
				var12 = null;
			}

			if (var13 != null && var13.equals("")) {
				var13 = null;
			}

			if (var14 != null && var14.equals("")) {
				var14 = null;
			}

			WTPart var15 = getPart(var7, var8, var9, var10, var11, var12, var14, var13);
			String var6;
			if (var15 != null) {
				String var16 = getValue("repDirectory", var0, var1, true);
				var16 = WTHOME + DIRSEP + var16;
				String var17 = getRefFromObject(var15);
				boolean var18 = false;
				String var19 = getValue("repName", var0, var1, false);
				String var20 = getValue("repDescription", var0, var1, false);
				boolean var21 = getBooleanValue("repDefault", var0, var1, false, false);
				boolean var22 = getBooleanValue("repCreateThumbnail", var0, var1, false, true);
				boolean var23 = getBooleanValue("repStoreEdz", var0, var1, false, false);
				Vector var24 = new Vector();
				var24.addElement("ignoreonmerge=true");
				Boolean var25 = loadRepresentation(var16, var17, var18, var19, var20, var21, var22, var23, var24);
				var5[0] = var19;
				var5[1] = var15.getIdentity();
				if (var25) {
					var6 = WTMessage.getLocalizedMessage(RESOURCE, "181", var5);
				} else {
					var6 = WTMessage.getLocalizedMessage(RESOURCE, "182", var5);
				}

				var2.addElement(var6);
				return var25;
			}

			var5[0] = getDisplayInfo(var0, var1);
			var6 = WTMessage.getLocalizedMessage(RESOURCE, "180", var5);
			LoadServerHelper.printMessage(var6);
		} catch (WTException var26) {
			LoadServerHelper.printMessage("\ncreatePartRepresentation: " + var26.getLocalizedMessage());
			var26.printStackTrace();
		} catch (Exception var27) {
			LoadServerHelper.printMessage("\ncreatePartRepresentation: " + var27.getMessage());
			var27.printStackTrace();
		}

		return false;
	}

	public static boolean cachePart(Hashtable var0, Hashtable var1, Vector var2) {
		try {
			String var3 = getValue("partNumber", var0, var1, false);
			String var4 = getValue("partVersion", var0, var1, false);
			String var5 = getValue("partIteration", var0, var1, false);
			String var6 = getValue("partView", var0, var1, false);
			String var7 = getValue("partVariation1", var0, var1, false);
			String var8 = getValue("partVariation2", var0, var1, false);
			String var9 = getValue("organizationName", var0, var1, false);
			String var10 = getValue("organizationID", var0, var1, false);
			if (var7 != null && var7.equals("")) {
				var7 = null;
			}

			if (var8 != null && var8.equals("")) {
				var8 = null;
			}

			if (var9 != null && var9.equals("")) {
				var9 = null;
			}

			if (var10 != null && var10.equals("")) {
				var10 = null;
			}

			WTPart var11 = getPart(var3, var4, var5, var6, var7, var8, var10, var9);
			cachePart(var11);
		} catch (WTException var12) {
			LoadServerHelper.printMessage("\nCache Part Failed: " + var12.getLocalizedMessage());
			var12.printStackTrace();
		}

		return true;
	}

	protected static Boolean loadRepresentation(String var0, String var1, boolean var2, String var3, String var4,
			boolean var5, boolean var6, boolean var7, Vector var8) {
		Class[] var9 = new Class[]{String.class, String.class, Boolean.TYPE, String.class, String.class, Boolean.TYPE,
				Boolean.TYPE, Boolean.TYPE, Vector.class};
		Object[] var10 = new Object[]{var0, var1, var2, var3, var4, var5, var6, var7, var8};

		try {
			Class var11 = Class.forName("com.ptc.wvs.server.ui.RepHelper");
			Method var12 = var11.getMethod("loadRepresentation", var9);
			return (Boolean) var12.invoke((Object) null, var10);
		} catch (Exception var13) {
			var13.printStackTrace();
			return Boolean.FALSE;
		}
	}

	protected static String getRefFromObject(Persistable var0) {
		try {
			ReferenceFactory var1 = new ReferenceFactory();
			return var1.getReferenceString(
					ObjectReference.newObjectReference(var0.getPersistInfo().getObjectIdentifier()));
		} catch (Exception var2) {
			return null;
		}
	}

	protected static Matrix4d getLocationMatrix(String var0) {
		Matrix4d var1;
		if (var0 != null && !var0.equals("")) {
			StringTokenizer var2 = new StringTokenizer(var0);
			if (var2.countTokens() > 5) {
				double var3 = Double.parseDouble(var2.nextToken());
				double var5 = Double.parseDouble(var2.nextToken());
				double var7 = Double.parseDouble(var2.nextToken());
				double var9 = Double.parseDouble(var2.nextToken());
				double var11 = Double.parseDouble(var2.nextToken());
				double var13 = Double.parseDouble(var2.nextToken());
				double var15 = 1.0;
				if (var2.hasMoreTokens()) {
					var15 = Double.parseDouble(var2.nextToken());
				}

				var1 = getMatrix4dFromLocation(var3, var5, var7, var9, var11, var13, var15);
			} else {
				var1 = new Matrix4d();
				var1.setIdentity();
			}
		} else {
			var1 = new Matrix4d();
			var1.setIdentity();
		}

		return var1;
	}

	protected static Matrix4d getMatrix4dFromLocation(double var0, double var2, double var4, double var6, double var8,
			double var10, double var12) {
		Matrix4d var14 = new Matrix4d();
		Matrix4d var15 = new Matrix4d();
		var14.rotZ(var4 * 0.017453292519943295);
		var15.rotY(var2 * 0.017453292519943295);
		var14.mul(var15);
		var15.rotX(var0 * 0.017453292519943295);
		var14.mul(var15);
		var14.setTranslation(new Vector3d(var6, var8, var10));
		var14.setScale(var12);
		return var14;
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

	protected static WTPart constructPart(Hashtable var0, Hashtable var1) throws WTException {
		Map var2 = null;
		boolean var3 = false;

		WTPart var39;
		try {
			NavigationFilterHelper.service.getOptionExclusionReference();
			Timestamp var4 = null;
			Timestamp var5 = null;
			boolean var6 = false;
			String var7 = getValue("partNumber", var0, var1, false);
			String var8 = getValue("version", var0, var1, false);
			String var9 = getValue("iteration", var0, var1, false);
			String var10 = getValue("view", var0, var1, false);
			String var11 = getValue("variation1", var0, var1, false);
			String var12 = getValue("variation2", var0, var1, false);
			String var13 = getValue("organizationName", var0, var1, false);
			String var14 = getValue("organizationID", var0, var1, false);
			getValue("phantom", var0, var1, false);
			if (var11 != null && var11.equals("")) {
				var11 = null;
			}

			if (var12 != null && var12.equals("")) {
				var12 = null;
			}

			if (var13 != null && var13.equals("")) {
				var13 = null;
			}

			if (var14 != null && var14.equals("")) {
				var14 = null;
			}

			String var16 = getValue("createTimestamp", var0, var1, false);
			String var17 = getValue("modifyTimestamp", var0, var1, false);
			var4 = parseTimestamp(var16);
			var5 = parseTimestamp(var17);
			if (var4 == null) {
				if (var5 != null) {
					var4 = var5;
				}
			} else if (var5 == null) {
				var5 = var4;
			}

			if (var4 == null && var17 == null) {
				var6 = true;
			}

			WTPart var18 = null;
			WTPart var19 = null;
			if (var7 != null) {
				if (var8 == null) {
					var18 = getPart(var7, (String) null, (String) null, var10, var11, var12, var14, var13);
				} else if (var8 != null && var9 == null) {
					var18 = getPart(var7, var8, (String) null, var10, var11, var12, var14, var13);
					if (var18 == null) {
						var18 = getPart(var7, (String) null, (String) null, var10, var11, var12, var14, var13);
					}
				} else if (var8 != null && var9 != null) {
					var18 = getPart(var7, var8, var9, var10, var11, var12, var14, var13);
					if (var18 == null) {
						var18 = getPart(var7, var8, (String) null, var10, var11, var12, var14, var13);
					}

					if (var18 == null) {
						var18 = getPart(var7, (String) null, (String) null, var10, var11, var12, var14, var13);
					}
				}
			}

			String var10000;
			String var20;
			String var33;
			if (var18 == null) {
				var2 = getTransactionMap();
				if (var2 != null) {
					var2.put("SkipLifecycleHistoryCreation", Boolean.TRUE);
				}

				MethodContext.getContext().put("SKIP_WHEN_USING_LOADERS", "SKIP_WHEN_USING_LOADERS");
				MethodContext.getContext().put("BYPASS_CREATE_MEMBERSHIPS_QUERY_KEY",
						"BYPASS_CREATE_MEMBERSHIPS_QUERY_KEY");
				var20 = getValue("typedef", var0, var1, false);
				if (var20 == null || var20.trim().equals("")) {
					var20 = "WCTYPE|wt.part.WTPart";
				}

				TypeIdentifier var21 = (TypeIdentifier) ReflectionHelper.dynamicInvoke(
						"com.ptc.core.foundation.type.server.impl.TypeHelper", "getTypeIdentifier",
						new Class[]{String.class}, new Object[]{var20});
				Object var22 = ReflectionHelper.dynamicInvoke("com.ptc.core.foundation.type.server.impl.TypeHelper",
						"newInstance", new Class[]{TypeIdentifier.class}, new Object[]{var21});
				if (!(var22 instanceof WTPart)) {
					throw new WTException("Expected instance of wtpart, but was: " + var22.getClass().getName());
				}

				var18 = (WTPart) var22;

				try {
					if (var18.getIterationInfo() == null) {
						var18.setIterationInfo(IterationInfo.newIterationInfo());
					}
				} catch (WTPropertyVetoException var31) {
					throw new WTException(var31);
				}

				var18 = applyConstructionTimeAttributes(var18, var0, var1);
			} else {
				if (var6 && !VersionControlHelper.isLatestIteration(var18)) {
					var18 = (WTPart) VersionControlHelper.getLatestIteration(var18);
				}

				if (VERBOSE) {
					var10000 = var18.getVersionDisplayIdentifier()
							.getLocalizedMessage(WTContext.getContext().getLocale());
					var20 = var10000 + "." + var18.getIterationIdentifier().getValue();
					var33 = WorkInProgressHelper.getState(var18).getDisplay();
					System.out.println(
							"Iterating on an existing part = " + var7 + " " + var20 + " with state of " + var33);
				}

				try {
					WTContainerRef var34 = var18.getContainerReference();
					WTContainerRef var35 = LoadServerHelper.getTargetContainer(var0, var1);
					if (!var34.equals(var35)) {
						throw new WTException("Can not create '" + var7 + " " + var18.getName() + " - " + var8
								+ "' in Container: '" + var35.getName() + "', because it already exists in Container: '"
								+ var34.getName() + "'");
					}

					if (var6) {
						String var38 = null;
						var3 = isNewVersion(var18, var8);
						if (var9 == null) {
							if (!var3) {
								int var23 = Integer.parseInt(var18.getIterationIdentifier().getValue());
								var38 = Integer.toString(var23 + 1);
							} else {
								var38 = Integer.toString(1);
							}
						}

						var18 = (WTPart) VersionControlHelper.service.newIteration(var18);
						if (var38 != null) {
							setIteration(var18, var38);
						}
					} else if (var8 != null
							&& !var8.equals(VersionControlHelper.getVersionIdentifier(var18).getValue())) {
						var18 = (WTPart) VersionControlHelper.service.newVersion(var18);
					} else {
						WTPart var40 = (WTPart) VersionControlHelper.service.newIteration(var18);
						var18 = (WTPart) PersistenceHelper.manager.refresh(var18);
						var18 = (WTPart) VersionControlHelper.service.supersede(var18, var40);
					}
				} catch (WTPropertyVetoException var30) {
					throw new WTException(var30);
				}
			}

			var18 = applyHardAttributes(var18, var0, var1);
			logger.debug("SetCreatedByAndModifiedBy");
			var18 = (WTPart) SetCreatedByAndModifiedBy(var18, var0, var1, false);
			if (var6) {
				try {
					var18.setFederatableInfo(new FederatableInfo());
				} catch (WTPropertyVetoException var29) {
					throw new WTException(var29,
							"Error creating the FederatableInfo required for inserting versions/iterations");
				}

				var18 = (WTPart) VersionControlHelper.service.insertNode(var18, (Ufid) null, (Ufid) null, (Ufid[]) null,
						(Ufid[]) null);
				if (!var3 && var9 != null && !var9.isEmpty() && !var9.equalsIgnoreCase("")) {
					int var36 = Integer.valueOf(var9) - 1;
					var33 = Integer.toString(var36);
					dont_check_cache_for_lastIter = true;
					var19 = getPart(var7, var8, var33, var10, var11, var12, var14, var13);
					dont_check_cache_for_lastIter = false;
					if (var19 != null) {
						copyForwardRepresentation(var19, var18);
					}
				}

				WTPartMaster var37 = var18.getMaster();
				PersistenceServerHelper.manager.update(var37, false);
			} else {
				var18 = (WTPart) PersistenceServerHelper.manager.store(var18, var4, var5);
			}

			if (VERBOSE) {
				var10000 = var18.getVersionDisplayIdentifier().getLocalizedMessage(WTContext.getContext().getLocale());
				var20 = var10000 + "." + var18.getIterationIdentifier().getValue();
				var33 = WorkInProgressHelper.getState(var18).getDisplay();
				System.out.println("New part = " + var7 + " " + var20 + " with state of " + var33);
			}

			setLogicBase(var18, var0, var1);
			var39 = var18;
		} finally {
			if (var2 != null) {
				var2.remove("SkipLifecycleHistoryCreation");
			}

			MethodContext.getContext().remove("SKIP_WHEN_USING_LOADERS");
			MethodContext.getContext().remove("BYPASS_CREATE_MEMBERSHIPS_QUERY_KEY");
		}

		return var39;
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
		if(createdby==null) {
			createdby = OrganizationServicesHelper.manager.getAuthenticatedUser("migrationUser");
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
		if(modifiedby==null) {
			modifiedby = OrganizationServicesHelper.manager.getAuthenticatedUser("migrationUser");
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

	private static void copyForwardRepresentation(WTPart var0, WTPart var1) throws WTException {
		RepresentationForwardLoaderService var2 = null;

		try {
			var2 = (RepresentationForwardLoaderService) DefaultServiceProvider
					.getService(RepresentationForwardLoaderService.class, "default");
		} catch (UnableToCreateServiceException var9) {
			if (logger.isTraceEnabled()) {
				logger.trace("RepresentationForwardLoaderService is not found for table-");
			}
		}

		if (var2 != null) {
			boolean var3 = SessionServerHelper.manager.setAccessEnforced(false);

			try {
				WTValuedHashMap var4 = new WTValuedHashMap();
				var4.put(var0, var1);
				var2.processRepresentationCopyForward(var4, false, false, false, false, true);
			} finally {
				SessionServerHelper.manager.setAccessEnforced(var3);
			}
		}

	}

	protected static boolean isNewVersion(WTPart var0, String var1) throws WTException {
		if (var1 == null) {
			return false;
		} else {
			return !var1.equals(VersionControlHelper.getVersionIdentifier(var0).getValue());
		}
	}

	public static WTPart getPart() throws WTException {
		return getCachedPart((String) null, (String) null, (String) null, (String) null);
	}

	public static WTPart getPart(String var0, String var1, String var2, String var3) throws WTException {
		return getPart(var0, var1, var2, var3, (String) null, (String) null);
	}

	public static WTPart getPart(String var0, String var1, String var2, String var3, String var4, String var5)
			throws WTException {
		return getPart(var0, var1, var2, var3, (String) null, (String) null, var4, var5);
	}

	public static WTPart getPart(String var0, String var1, String var2, String var3, String var4, String var5,
			String var6, String var7) throws WTException {
		if (!dont_check_cache_for_lastIter) {
			WTPart var8 = getCachedPart(var0, var1, var2, var3, var4, var5, var6, var7);
			if (var8 != null) {
				return var8;
			}
		}

		if (var0 == null) {
			return null;
		} else {
			LatestConfigSpec var15 = null;
			QuerySpec var9 = new QuerySpec(WTPart.class);
			var9.appendWhere(new SearchCondition(WTPart.class, "master>number", "=", var0.toUpperCase(), false));
			WTOrganization var10 = getOrganization(var6, var7);
			if (var10 != null) {
				var9.appendAnd();
				var9.appendWhere(new SearchCondition(WTPart.class, "master>organizationReference.key.id", "=",
						PersistenceHelper.getObjectIdentifier(var10).getId()));
			}

			if (var3 != null) {
				View var11 = ViewHelper.service.getView(var3);
				var9.appendAnd();
				var9.appendWhere(new SearchCondition(WTPart.class, "view.key", "=",
						PersistenceHelper.getObjectIdentifier(var11)));
			}

			if (var4 != null) {
				Variation1 var16 = Variation1.toVariation1(var4);
				var9 = ViewHelper.appendWhereVariation(var9, WTPart.class, new int[]{0}, Variation1.class, var16,
						false);
			}

			if (var5 != null) {
				Variation2 var17 = Variation2.toVariation2(var5);
				var9 = ViewHelper.appendWhereVariation(var9, WTPart.class, new int[]{0}, Variation2.class, var17,
						false);
			}

			if (var1 != null) {
				var9.appendAnd();
				var9.appendWhere(
						new SearchCondition(WTPart.class, "versionInfo.identifier.versionId", "=", var1, false));
				if (var2 != null) {
					var9.appendAnd();
					var9.appendWhere(new SearchCondition(WTPart.class, "iterationInfo.identifier.iterationId", "=",
							var2, false));
				} else {
					var9.appendAnd();
					var9.appendWhere(new SearchCondition(WTPart.class, "iterationInfo.latest", "TRUE"));
				}
			} else {
				var15 = new LatestConfigSpec();
				var15.appendSearchCriteria(var9);
			}

			SandboxHelper.addHiddenObjectFilter(var9, WTPart.class, 0, LogicalOperator.AND);
			if (VERBOSE) {
				System.out.println(
						"getPart(" + var0 + "," + var1 + "," + var2 + "," + var3 + "," + var6 + "," + var7 + ")");
				System.out.println("getPart() SQL: " + var9.toString());
			}

			QueryResult var18 = PersistenceHelper.manager.find(var9);
			if (VERBOSE) {
				System.out.println("Query found " + var18.size() + " matching parts.");
			}

			if (var15 != null) {
				var18 = var15.process(var18);
			}

			if (VERBOSE && var15 != null) {
				System.out.println("Query filtered by ConfigSpec found " + var18.size() + " matching parts.");
			}

			if (var18.size() == 1) {
				WTPart var19 = (WTPart) var18.nextElement();
				if (WorkInProgressHelper.isCheckedOut(var19)) {
					String[] var13 = new String[0];
					String var14 = WTMessage.getLocalizedMessage("wt.vc.wip.wipResource", "1", var13);
					LoadServerHelper.printMessage(var14);
					throw new WTException(var14);
				} else {
					var19 = cachePart(var19);
					return var19;
				}
			} else if (var18.size() > 1) {
				String var12 = "Found " + var18.size() + " parts that match number=" + var0 + " version=" + var1
						+ " iteration=" + var2 + " view=" + var3 + ". Expecting only one part.";
				LoadServerHelper.printMessage(var12);
				throw new WTException(var12);
			} else {
				return null;
			}
		}
	}

	public static WTPartMaster getMaster() throws WTException {
		return getMaster((String) null);
	}

	public static WTPartMaster getMaster(String var0) throws WTException {
		WTPartMaster var1 = getCachedMaster(var0);
		if (var1 == null && var0 != null) {
			QuerySpec var2 = new QuerySpec(WTPartMaster.class);
			var2.appendWhere(new SearchCondition(WTPartMaster.class, "number", "=", var0.toUpperCase()));
			QueryResult var3 = PersistenceHelper.manager.find(var2);
			if (var3.size() == 1) {
				var1 = cacheMaster((WTPartMaster) var3.nextElement());
			}
		}

		return var1;
	}

	public static WTPartMaster getMaster(String var0, WTContainerRef var1) throws WTException {
		if (var0 != null) {
			QuerySpec var2 = new QuerySpec(WTPartMaster.class);
			var2.appendWhere(new SearchCondition(WTPartMaster.class, "number", "=", var0.toUpperCase()));
			QueryResult var3 = PersistenceHelper.manager.find(var2);

			while (var3.hasMoreElements()) {
				WTPartMaster var4 = (WTPartMaster) var3.nextElement();
				if (var4.getContainerReference().equals(var1)) {
					return var4;
				}
			}
		}

		return null;
	}

	public static WTPartMaster getMaster(String var0, String var1) throws WTException {
		if (var0 != null) {
			QuerySpec var2 = new QuerySpec(WTPartMaster.class);
			var2.appendWhere(new SearchCondition(WTPartMaster.class, "number", "=", var0.toUpperCase()), new int[]{0});
			WTOrganization var3 = getOrganization((String) null, var1);
			if (var3 != null) {
				var2.appendAnd();
				var2.appendWhere(new SearchCondition(WTPartMaster.class, "organizationReference.key.id", "=",
						PersistenceHelper.getObjectIdentifier(var3).getId()), new int[]{0});
			}

			QueryResult var4 = PersistenceHelper.manager.find(var2);
			if (var4.size() == 1) {
				return (WTPartMaster) var4.nextElement();
			}
		}

		return null;
	}

	protected static WTPart getCachedPart() throws WTException {
		return getCachedPart((String) null, (String) null, (String) null, (String) null);
	}

	protected static WTPart getCachedPart(String var0) throws WTException {
		return getCachedPart(var0, (String) null, (String) null, (String) null);
	}

	protected static WTPart getCachedPart(String var0, String var1) throws WTException {
		return getCachedPart(var0, var1, (String) null, (String) null);
	}

	protected static WTPart getCachedPart(String var0, String var1, String var2) throws WTException {
		return getCachedPart(var0, var1, var2, (String) null);
	}

	protected static WTPart getCachedPart(String var0, String var1, String var2, String var3) throws WTException {
		return getCachedPart(var0, var1, var2, var3, (String) null, (String) null, (String) null, (String) null);
	}

	protected static WTPart getCachedPart(String var0, String var1, String var2, String var3, String var4, String var5,
			String var6, String var7) throws WTException {
		Object var8 = getPartCacheKey(var0, var1, var2, var3, var4, var5, var6, var7);
		WTPart var9 = (WTPart) LoadServerHelper.getFromCache(var8);
		if (VERBOSE) {
			if (var9 == null) {
				System.out.println("Getting part from cache using key: " + var8 + ". Part not found.");
			} else {
				System.out.println("Getting part from cache using key: " + var8 + ". Part found.");
			}
		}

		return var9;
	}

	protected static Object getPartCacheKey(final String var0, final String var1, final String var2, final String var3,
			final String var4, final String var5, final String var6, final String var7) throws WTException {
		class PartKey {
			final String partNumber = var0 == null ? null : var0.toUpperCase();
			final String partVersion = var1 == null ? null : var1.toUpperCase();
			final String partIteration = var2 == null ? null : var2.toUpperCase();
			final String partView = var3 == null ? null : var3.toUpperCase();
			final String partVariation1 = var4 == null ? null : var4.toUpperCase();
			final String partVariation2 = var5 == null ? null : var5.toUpperCase();
			final String partOrgID = var6 == null ? null : var6.toUpperCase();
			final String partOrgName = var7 == null ? null : var7.toUpperCase();
			volatile int hashCode;

			public boolean equals(Object var1x) {
				if (var1x == this) {
					return true;
				} else if (!(var1x instanceof PartKey)) {
					return false;
				} else {
					PartKey var2x = (PartKey) var1x;
					return this.equals(this.partNumber, var2x.partNumber)
							&& this.equals(this.partVersion, var2x.partVersion)
							&& this.equals(this.partIteration, var2x.partIteration)
							&& this.equals(this.partView, var2x.partView)
							&& this.equals(this.partVariation1, var2x.partVariation1)
							&& this.equals(this.partVariation2, var2x.partVariation2)
							&& this.equals(this.partOrgID, var2x.partOrgID)
							&& this.equals(this.partOrgName, var2x.partOrgName);
				}
			}

			boolean equals(String var1x, String var2x) {
				if (var1x == null) {
					return var2x == null;
				} else {
					return var2x == null ? false : var1x.equals(var2x);
				}
			}

			public int hashCode() {
				if (this.hashCode == 0) {
					int var1x = 13;
					var1x = 37 * var1x + this.partNumber.hashCode();
					if (this.partVersion != null) {
						var1x = 37 * var1x + this.partVersion.hashCode();
					}

					if (this.partIteration != null) {
						var1x = 37 * var1x + this.partIteration.hashCode();
					}

					if (this.partView != null) {
						var1x = 37 * var1x + this.partView.hashCode();
					}

					if (this.partVariation1 != null) {
						var1x = 37 * var1x + this.partVariation1.hashCode();
					}

					if (this.partVariation2 != null) {
						var1x = 37 * var1x + this.partVariation2.hashCode();
					}

					if (this.partOrgID != null) {
						var1x = 37 * var1x + this.partOrgID.hashCode();
					}

					if (this.partOrgName != null) {
						var1x = 37 * var1x + this.partOrgName.hashCode();
					}

					this.hashCode = var1x;
				}

				return this.hashCode;
			}

			public String toString() {
				return "wt.load.LoadPart.PartKey [number=\"" + this.partNumber + "\" version=\"" + this.partVersion
						+ "\" iteration=\"" + this.partIteration + "\" view=\"" + this.partView + "\" variation1=\""
						+ this.partVariation1 + "\" variation2=\"" + this.partVariation2 + "\" orgID=\""
						+ this.partOrgID + "\" orgName=\"" + this.partOrgName + "\"]";
			}
		}

		return var0 == null ? LAST_PART_KEY : new PartKey();
	}

	protected static WTPart cachePart(WTPart var0) throws WTException {
		if (var0 == null) {
			LoadServerHelper.removeFromCache(getPartCacheKey((String) null, (String) null, (String) null, (String) null,
					(String) null, (String) null, (String) null, (String) null));
			LoadServerHelper.removeFromCache("Current ContentHolder");
			LoadValue.establishCurrentIBAHolder((IBAHolder) null);
			LoadAttValues.establishCurrentTypeManaged((TypeManaged) null);
			cacheChoiceMappableObject((ChoiceMappable) null);
			cacheExpressionableObject((Expressionable) null);
			LoadServerHelper.removeFromCache(CURRENT_PART);
		} else {
			String var1 = var0.getNumber();
			String var2 = VersionControlHelper.getVersionIdentifier(var0).getValue();
			String var3 = VersionControlHelper.getIterationIdentifier(var0).getValue();
			String var4 = ViewHelper.getViewName(var0);
			String var5 = var0.getVariation1() == null ? null : var0.getVariation1().toString();
			String var6 = var0.getVariation2() == null ? null : var0.getVariation2().toString();
			String var7 = null;
			String var8 = null;
			WTOrganization var9 = var0.getOrganization();
			if (var9 != null) {
				var7 = var9.getName();
				var8 = getOrgID(var9);
			}

			if (VERBOSE) {
				System.out.println("Caching part with all combinations of key [ number=\"" + var1 + "\" version=\""
						+ var2 + "\" iteration=\"" + var3 + "\" view=\"" + var4 + "\" variation1=\"" + var5
						+ "\" variation2=\"" + var6 + "\" orgID=\"" + var8 + "\" orgName=\"" + var7 + "\" ]");
			}

			LoadServerHelper.putCacheValue(getPartCacheKey((String) null, (String) null, (String) null, (String) null,
					(String) null, (String) null, (String) null, (String) null), var0);
			LoadServerHelper.putCacheValue(getPartCacheKey(var1, (String) null, (String) null, (String) null,
					(String) null, (String) null, (String) null, (String) null), var0);
			LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, (String) null, (String) null, (String) null,
					(String) null, (String) null, (String) null), var0);
			LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, var3, (String) null, (String) null,
					(String) null, (String) null, (String) null), var0);
			if (var4 != null) {
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, (String) null, (String) null, var4, var5, var6,
						(String) null, (String) null), var0);
				LoadServerHelper.putCacheValue(
						getPartCacheKey(var1, var2, (String) null, var4, var5, var6, (String) null, (String) null),
						var0);
				LoadServerHelper.putCacheValue(
						getPartCacheKey(var1, var2, var3, var4, var5, var6, (String) null, (String) null), var0);
			}

			if (var9 != null) {
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, (String) null, (String) null, (String) null,
						(String) null, (String) null, var8, var7), var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, (String) null, (String) null, (String) null,
						(String) null, var8, var7), var0);
				LoadServerHelper.putCacheValue(
						getPartCacheKey(var1, var2, var3, (String) null, (String) null, (String) null, var8, var7),
						var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, (String) null, (String) null, (String) null,
						(String) null, (String) null, var8, (String) null), var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, (String) null, (String) null, (String) null,
						(String) null, var8, (String) null), var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, var3, (String) null, (String) null,
						(String) null, var8, (String) null), var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, (String) null, (String) null, (String) null,
						(String) null, (String) null, (String) null, var7), var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, (String) null, (String) null, (String) null,
						(String) null, (String) null, var7), var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, var3, (String) null, (String) null,
						(String) null, (String) null, var7), var0);
			}

			if (var4 != null && var9 != null) {
				LoadServerHelper.putCacheValue(
						getPartCacheKey(var1, (String) null, (String) null, var4, var5, var6, var8, var7), var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, (String) null, var4, var5, var6, var8, var7),
						var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, var3, var4, var5, var6, var8, var7), var0);
				LoadServerHelper.putCacheValue(
						getPartCacheKey(var1, (String) null, (String) null, var4, var5, var6, var8, (String) null),
						var0);
				LoadServerHelper.putCacheValue(
						getPartCacheKey(var1, var2, (String) null, var4, var5, var6, var8, (String) null), var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, var3, var4, var5, var6, var8, (String) null),
						var0);
				LoadServerHelper.putCacheValue(
						getPartCacheKey(var1, (String) null, (String) null, var4, var5, var6, (String) null, var7),
						var0);
				LoadServerHelper.putCacheValue(
						getPartCacheKey(var1, var2, (String) null, var4, var5, var6, (String) null, var7), var0);
				LoadServerHelper.putCacheValue(getPartCacheKey(var1, var2, var3, var4, var5, var6, (String) null, var7),
						var0);
			}

			LoadServerHelper.putCacheValue("Current ContentHolder", var0);
			cacheChoiceMappableObject(var0);
			cacheExpressionableObject(var0);
			LoadServerHelper.putCacheValue(CURRENT_PART, var0);
			cacheMaster(var0.getMaster());
		}

		LoadValue.establishCurrentIBAHolder(var0);
		LoadAttValues.establishCurrentTypeManaged(var0);
		LoadValue.beginIBAContainer();
		return var0;
	}

	protected static WTPartMaster getCachedMaster() throws WTException {
		return getCachedMaster((String) null);
	}

	protected static WTPartMaster getCachedMaster(String var0) throws WTException {
		return (WTPartMaster) LoadServerHelper.getCacheValue(getMasterCacheKey(var0));
	}

	protected static String getMasterCacheKey(String var0) throws WTException {
		StringBuffer var1 = new StringBuffer("PART_MASTER_CACHE_KEY:");
		if (var0 != null) {
			var1.append(var0.toUpperCase());
		}

		return var1.toString();
	}

	protected static WTPartMaster cacheMaster(WTPartMaster var0) throws WTException {
		if (var0 == null) {
			LoadServerHelper.removeCacheValue(getMasterCacheKey((String) null));
		} else {
			String var1 = var0.getNumber();
			LoadServerHelper.setCacheValue(getMasterCacheKey((String) null), var0);
			LoadServerHelper.setCacheValue(getMasterCacheKey(var1), var0);
		}

		LoadValue.establishCurrentIBAHolder(var0);
		LoadValue.beginIBAContainer();
		return var0;
	}

	protected static WTPartUsageLink getCachedPartUsage() throws WTException {
		return (WTPartUsageLink) LoadServerHelper.getCacheValue("PARTUSAGElINK_KEY:");
	}

	protected static WTPartUsageLink cachePartUsageObject(WTPartUsageLink var0) throws WTException {
		if (var0 == null) {
			LoadServerHelper.removeCacheValue("PARTUSAGElINK_KEY:");
		} else {
			LoadServerHelper.setCacheValue("PARTUSAGElINK_KEY:", var0);
		}

		return var0;
	}

	protected static Persistable getCachedPartDocLinks() throws WTException {
		return (Persistable) LoadServerHelper.getCacheValue("PARTDOCLINK_CACHE_KEY:");
	}

	protected static Persistable cachePartDocLinkObject(Persistable var0) throws WTException {
		if (var0 == null) {
			LoadServerHelper.removeCacheValue("PARTDOCLINK_CACHE_KEY:");
		} else {
			LoadServerHelper.setCacheValue("PARTDOCLINK_CACHE_KEY:", var0);
		}

		return var0;
	}

	public static ChoiceMappable getCachedChoiceMappable() throws WTException {
		return (ChoiceMappable) LoadServerHelper.getCacheValue("CHOICE_MAPPABLE_OBJECT");
	}

	public static ChoiceMappable cacheChoiceMappableObject(ChoiceMappable var0) throws WTException {
		if (var0 == null) {
			LoadServerHelper.removeCacheValue("CHOICE_MAPPABLE_OBJECT");
		} else {
			LoadServerHelper.setCacheValue("CHOICE_MAPPABLE_OBJECT", var0);
		}

		return var0;
	}

	public static Expressionable getCachedExpressionable() throws WTException {
		return (Expressionable) LoadServerHelper.getCacheValue("EXPRESSIONABLE_OBJECT");
	}

	public static Expressionable cacheExpressionableObject(Expressionable var0) throws WTException {
		if (var0 == null) {
			LoadServerHelper.removeCacheValue("EXPRESSIONABLE_OBJECT");
		} else {
			LoadServerHelper.setCacheValue("EXPRESSIONABLE_OBJECT", var0);
		}

		return var0;
	}

	protected static WTPart applyHardAttributes(WTPart var0, Hashtable var1, Hashtable var2) throws WTException {
		WTContainerRef var3 = LoadServerHelper.getTargetContainer(var1, var2);
		setContainer(var0, var3);
		setIteration(var0, getValue("iteration", var1, var2, false));
		setSecurityLabels(var0, getValue("securityLabels", var1, var2, false));
		setPartType(var0, getValue("type", var1, var2, true));
		setSource(var0, getValue("source", var1, var2, true));
		setFolder(var3, var0, getValue("folder", var1, var2, true));
		setLifeCycle(var3, var0, getValue("lifecycle", var1, var2, false));
		String[] var4 = parseTeamTemplate(var1, var2);
		setTeamTemplate(var3, var0, var4[0], var4[1]);
		setState(var0, getValue("lifecyclestate", var1, var2, false));
		setVersion(var0, getValue("version", var1, var2, false));
		setEndItemFlag(var0, getValue("enditem", var1, var2, false));
		setTraceCode(var0, getValue("traceCode", var1, var2, false));
		setMinimumRequired(var0, getValue("minRequired", var1, var2, false));
		setMaximumAllowed(var0, getValue("maxAllowed", var1, var2, false));
		setDefaultUnit(var0, getValue("defaultUnit", var1, var2, false));
		setServiceable(var0, getValue("serviceable", var1, var2, false));
		setServicekit(var0, getValue("servicekit", var1, var2, false));
		setAuthoringLanguage(var0, getValue("authoringLanguage", var1, var2, false));
		return var0;
	}

	protected static void setAuthoringLanguage(WTPart var0, String var1) throws WTException {
		if (var1 != null) {
			if (isPersisted(var0)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Setting Authoring Language for Existing Part");
				}

				if (var1.equals(var0.getAuthoringLanguage())) {
					return;
				}

				throw new WTException("AuthoringLanguage cannot be changed for persisted parts");
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Setting Authoring Language for NEW Part");
			}

			try {
				var0.setAuthoringLanguage(var1);
			} catch (WTPropertyVetoException var3) {
				throw new WTException(var3);
			}
		}

	}

	private static boolean isPersisted(WTPart var0) {
		return var0.getMaster().getPersistInfo().isPersisted();
	}

	protected static WTPart applyConstructionTimeAttributes(WTPart var0, Hashtable var1, Hashtable var2)
			throws WTException {
		setName(var0, getValue("partName", var1, var2, true));
		setNumber(var0, getValue("partNumber", var1, var2, false));
		setView(var0, getValue("view", var1, var2, false));
		setVariation1(var0, getValue("variation1", var1, var2, false));
		setVariation2(var0, getValue("variation2", var1, var2, false));
		setPhantom(var0, getValue("phantom", var1, var2, false));
		setEndItemFlag(var0, getValue("enditem", var1, var2, false));
		setTraceCode(var0, getValue("traceCode", var1, var2, false));
		String var3 = getValue("organizationID", var1, var2, false);
		String var4 = getValue("organizationName", var1, var2, false);
		setOrganization(var0, var3, var4);
		setGenericType(var0, getValue("genericType", var1, var2, false));
		setCollapsible(var0, getValue("collapsible", var1, var2, false));
		return var0;
	}

	protected static void setName(WTPart var0, String var1) throws WTException {
		try {
			var0.setName(var1);
		} catch (WTPropertyVetoException var3) {
			LoadServerHelper.printMessage("\nsetName: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	protected static void setNumber(WTPart var0, String var1) throws WTException {
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

	protected static void setView(WTPart var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				ViewHelper.assignToView(var0, ViewHelper.service.getView(var1));
			}

		} catch (Exception var3) {
			LoadServerHelper.printMessage("\nsetView: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	protected static void setCollapsible(WTPart var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				boolean var2 = Boolean.parseBoolean(var1);
				var0.setCollapsible(var2);
			} else {
				var0.setCollapsible(false);
			}

		} catch (Exception var3) {
			LoadServerHelper.printMessage("\nsetPhantom: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	protected static void setPhantom(WTPart var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				boolean var2 = Boolean.parseBoolean(var1);
				var0.setPhantom(var2);
			} else {
				var0.setPhantom(false);
			}

		} catch (Exception var3) {
			LoadServerHelper.printMessage("\nsetPhantom: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	protected static void setVariation1(WTPart var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				var0.setVariation1(Variation1.toVariation1(var1));
			} else {
				var0.setVariation1((Variation1) null);
			}

		} catch (Exception var3) {
			LoadServerHelper.printMessage("\nsetVariation1: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	protected static void setVariation2(WTPart var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				var0.setVariation2(Variation2.toVariation2(var1));
			} else {
				var0.setVariation2((Variation2) null);
			}

		} catch (Exception var3) {
			LoadServerHelper.printMessage("\nsetVariation2: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	private static void setEndItemFlag(WTPart var0, String var1) throws WTException {
		try {
			if (var1 != null && var1.equalsIgnoreCase("yes")) {
				var0.setEndItem(true);
			} else {
				var0.setEndItem(false);
			}

		} catch (Exception var3) {
			LoadServerHelper.printMessage("\nsetEndItemFlag: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	private static void setTraceCode(WTPart var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				var0.setDefaultTraceCode(TraceCode.toTraceCode(var1));
			}

		} catch (WTPropertyVetoException var3) {
			LoadServerHelper.printMessage("\nsetTraceCode: " + var3.getMessage());
			var3.printStackTrace();
			throw new WTException(var3);
		}
	}

	private static void setMinimumRequired(WTPart var0, String var1) throws WTException {
		if (var1 != null && var0.getGenericType().equals(GenericType.DYNAMIC)) {
			try {
				Integer var2 = Integer.valueOf(var1.trim());
				var0.setMinimumRequired(var2);
			} catch (WTPropertyVetoException var3) {
				LoadServerHelper.printMessage("\nsetMinimumRequired: " + var3.getMessage());
				var3.printStackTrace();
				throw new WTException(var3);
			}
		}

	}

	private static void setMaximumAllowed(WTPart var0, String var1) throws WTException {
		if (var1 != null && var0.getGenericType().equals(GenericType.DYNAMIC)) {
			try {
				Integer var2 = Integer.valueOf(var1.trim());
				var0.setMaximumAllowed(var2);
			} catch (WTPropertyVetoException var3) {
				LoadServerHelper.printMessage("\nsetMaximumAllowed: " + var3.getMessage());
				var3.printStackTrace();
				throw new WTException(var3);
			}
		}

	}

	private static void setDefaultUnit(WTPart var0, String var1) {
		if (var1 != null && !var1.trim().equals("")) {
			WTPartMaster var2 = var0.getMaster();

			try {
				var2.setDefaultUnit(QuantityUnit.toQuantityUnit(var1));
			} catch (WTInvalidParameterException var4) {
				var4.printStackTrace();
			} catch (WTPropertyVetoException var5) {
				var5.printStackTrace();
			}
		}

	}

	private static void setServiceable(WTPart var0, String var1) throws WTException {
		if (var1 != null) {
			try {
				WTPartMaster var2 = var0.getMaster();
				Boolean var3 = Boolean.valueOf(var1.trim());
				var2.setServiceable(var3);
			} catch (WTPropertyVetoException var4) {
				LoadServerHelper.printMessage("\nserviceable: " + var4.getMessage());
				var4.printStackTrace();
				throw new WTException(var4);
			}
		}

	}

	private static void setServicekit(WTPart var0, String var1) throws WTException {
		if (var1 != null) {
			try {
				WTPartMaster var2 = var0.getMaster();
				Boolean var3 = Boolean.valueOf(var1.trim());
				var2.setServicekit(var3);
			} catch (WTPropertyVetoException var4) {
				LoadServerHelper.printMessage("\nservicekit: " + var4.getMessage());
				var4.printStackTrace();
				throw new WTException(var4);
			}
		}

	}

	protected static void setOrganization(WTPart var0, String var1, String var2) throws WTException {
		try {
			WTOrganization var3 = getOrganization(var1, var2);
			if (var3 != null) {
				var0.setOrganization(var3);
			}

		} catch (Exception var4) {
			LoadServerHelper.printMessage("\nsetOrganization: " + var4.getMessage());
			var4.printStackTrace();
			throw new WTException(var4);
		}
	}

	protected static void setPartType(WTPart var0, String var1) throws WTException {
		var0.setPartType(PartType.toPartType(var1));
	}

	protected static void setSource(WTPart var0, String var1) throws WTException {
		var0.setSource(Source.toSource(var1));
	}

	protected static String[] parseTeamTemplate(Hashtable var0, Hashtable var1) throws WTException {
		String[] var2 = new String[2];
		String var3 = getValue("teamTemplate", var0, var1, false);

		try {
			if (var3 != null) {
				StringTokenizer var4 = new StringTokenizer(var3, ".");
				var2[1] = var4.nextToken();
				var2[0] = var4.nextToken();
			}

			return var2;
		} catch (NoSuchElementException var6) {
			String[] var5 = new String[]{var3};
			LoadServerHelper.printMessage(WTMessage.getLocalizedMessage(RESOURCE, "148", var5));
			var6.printStackTrace();
			throw new WTException(var6);
		}
	}

	protected static String getValue(String var0, Hashtable var1, Hashtable var2, boolean var3) throws WTException {
		String var4 = LoadServerHelper.getValue(var0, var1, var2, var3 ? 0 : 1);
		if (var4 != null) {
			var4 = var4.trim();
			if (var4.equals("")) {
				var4 = null;
			}
		}

		return var4;
	}

	protected static String[] getValues(String var0, Hashtable var1, Hashtable var2, boolean var3) throws WTException {
		String var4 = LoadServerHelper.getValue(var0, var1, var2, var3 ? 0 : 1);
		if (var3 && var4 == null) {
			throw new WTException("Required value for " + var0 + " not provided in input file.");
		} else if (var4 == null) {
			return null;
		} else {
			ArrayList var5 = new ArrayList();
			char[] var9 = new char[var4.length()];
			int var10 = 0;
			byte var11 = 0;

			for (int var12 = 0; var12 < var4.length(); ++var12) {
				char var13 = var4.charAt(var12);
				switch (var11) {
					case 0 :
						if (var13 == '/') {
							var11 = 1;
						} else {
							var9[var10++] = var13;
						}
						break;
					case 1 :
						if (var13 == '/') {
							var11 = 0;
							var9[var10++] = '/';
						} else {
							var5.add(new String(var9, 0, var10));
							byte var14 = 0;
							var10 = var14 + 1;
							var9[var14] = var13;
							var11 = 0;
						}
				}
			}

			var5.add(new String(var9, 0, var10));
			if (var5.size() == 0) {
				return null;
			} else {
				return (String[]) var5.toArray(new String[var5.size()]);
			}
		}
	}

	protected static boolean getBooleanValue(String var0, Hashtable var1, Hashtable var2, boolean var3, boolean var4)
			throws WTException {
		boolean var5 = var4;
		String var6 = LoadServerHelper.getValue(var0, var1, var2, var3 ? 0 : 1);
		if (var3 && var6 == null) {
			throw new WTException("Required value for " + var0 + " not provided in input file.");
		} else {
			if (var6 != null) {
				if (var6.trim().equalsIgnoreCase("true")) {
					var5 = true;
				} else if (var6.trim().equalsIgnoreCase("false")) {
					var5 = false;
				}
			}

			return var5;
		}
	}

	protected static void setContainer(WTContained var0, WTContainerRef var1) throws WTException {
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

	protected static void setType(Typed var0, String var1) throws WTException {
		LoadValue.setType(var0, var1);
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

	protected static void setIteration(Iterated var0, String var1) throws WTException {
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

	protected static void setFolder(WTContainerRef var0, FolderEntry var1, String var2) throws WTException {
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

	protected static void setLifeCycle(WTContainerRef var0, LifeCycleManaged var1, String var2) throws WTException {
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

	protected static void setState(LifeCycleManaged var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				String var2 = SessionMgr.getPrincipal().getName();
				String var3 = AdministrativeDomainHelper.ADMINISTRATOR_NAME;
				boolean var4 = var3.equals(var2);
				if (!var4) {
					LoadServerHelper.changePrincipal(var3);
					LoadServerHelper.printMessage("\nSwitching user to Administrator (Setting lifecycle state...)");
				}

				try {
					LifeCycleServerHelper.setState(var0, State.toState(var1));
				} finally {
					if (!var4) {
						LoadServerHelper.changePrincipal(var2);
						LoadServerHelper.printMessage("\nSwitching user from Administrator (...lifecycle state set)");
					}

				}
			}

		} catch (WTPropertyVetoException var9) {
			LoadServerHelper.printMessage("\nsetState: " + var9.getMessage());
			var9.printStackTrace();
			throw new WTException(var9);
		}
	}

	protected static void setTeamTemplate(WTContainerRef var0, TeamManaged var1, String var2, String var3)
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

	protected static PDMLinkProduct lookupProduct(String var0, WTContainerRef var1) throws WTException {
		QuerySpec var2 = new QuerySpec(PDMLinkProduct.class);
		var2.appendWhere(new SearchCondition(PDMLinkProduct.class, "containerInfo.name", "=", var0), 0);
		QueryResult var3 = PersistenceHelper.manager.find(var2);
		PDMLinkProduct var4 = null;

		WTContainerRef var5;
		do {
			if (!var3.hasMoreElements()) {
				return null;
			}

			var4 = (PDMLinkProduct) var3.nextElement();
			var5 = var4.getContainerReference();
		} while (!var5.equals(var1));

		return var4;
	}

	public static boolean createProductContainer(Hashtable var0, Hashtable var1, Vector var2) {
		boolean var4;
		try {
			setUser(var0, var1);
			String var3 = getValue("name", var0, var1, true);
			String var51 = getValue("number", var0, var1, false);
			String var5 = getValue("sharedTeamName", var0, var1, false);
			String var6 = getValue("containerExtendable", var0, var1, false);
			String var7 = getValue("description", var0, var1, false);
			String var8 = getValue("view", var0, var1, false);
			String var9 = getValue("variation1", var0, var1, false);
			String var10 = getValue("variation2", var0, var1, false);
			String var11 = getValue("source", var0, var1, false);
			String var12 = getValue("defaultUnit", var0, var1, false);
			String var13 = getValue("type", var0, var1, false);
			String var14 = getValue("containerTemplate", var0, var1, true);
			String var15 = getValue("organizationName", var0, var1, false);
			String var16 = getValue("organizationID", var0, var1, false);
			boolean var17 = Boolean.valueOf(getValue("createPrimaryEndItem", var0, var1, false));
			WTContainerRef var18 = LoadServerHelper.getTargetContainer(var0, var1);
			OrgContainer var19 = null;
			if (var18 != null) {
				WTContainer var20 = var18.getReferencedContainer();
				if (var20 instanceof OrgContainer) {
					var19 = (OrgContainer) var20;
				}
			}

			if (var3 != null && var3.length() == 0) {
				var3 = null;
			}

			if (var51 != null && var51.length() == 0) {
				var51 = null;
			}

			if (var5 != null && var5.length() == 0) {
				var5 = null;
			}

			if (var6 != null && var6.length() == 0) {
				var6 = null;
			}

			if (var7 != null && var7.length() == 0) {
				var7 = null;
			}

			if (var8 != null && var8.length() == 0) {
				var8 = null;
			}

			if (var9 != null && var9.length() == 0) {
				var9 = null;
			}

			if (var10 != null && var10.length() == 0) {
				var10 = null;
			}

			if (var11 != null && var11.length() == 0) {
				var11 = null;
			}

			if (var12 != null && var12.length() == 0) {
				var12 = null;
			}

			if (var13 != null && var13.length() == 0) {
				var13 = null;
			}

			if (var14 != null && var14.length() == 0) {
				var14 = null;
			}

			if (var15 != null && var15.length() == 0) {
				var15 = null;
			}

			if (var16 != null && var16.length() == 0) {
				var16 = null;
			}

			if (var6 != null && var6.equalsIgnoreCase("true")) {
				MethodContext.getContext().put(WTContainerHelper.EXTENDABLE_CONTAINER, true);
			}

			WTContainerRef var52 = LoadServerHelper.getTargetContainer(var0, var1);
			WTOrganization var22 = getOrganization(var16, var15);
			PDMLinkProduct var21;
			if (var17) {
				WTPart var23 = WTPart.newWTPart();
				var23.setEndItem(true);
				var23.setDefaultTraceCode(TraceCode.SERIAL_NUMBER);
				if (var51 != null) {
					var23.setNumber(var51);
				}

				if (var3 != null) {
					var23.setName(var3);
				}

				if (var22 != null) {
					var23.setOrganization(var22);
				}

				if (var11 != null) {
					Source var24 = Source.toSource(var11);
					var23.setSource(var24);
				}

				if (var8 != null) {
					View var56 = ViewHelper.service.getView(var8);
					ViewReference var25 = ViewReference.newViewReference(var56);
					var23.setView(var25);
					if (var9 != null) {
						Variation1 var26 = Variation1.toVariation1(var9);
						var23.setVariation1(var26);
					}

					if (var10 != null) {
						Variation2 var63 = Variation2.toVariation2(var10);
						var23.setVariation2(var63);
					}
				}

				if (var12 != null) {
					QuantityUnit var57 = QuantityUnit.toQuantityUnit(var12);
					var23.setDefaultUnit(var57);
				}

				if (var13 != null) {
					PartType var58 = PartType.toPartType(var13);
					var23.setPartType(var58);
				}

				var21 = PDMLinkProduct.newPDMLinkProduct(var23);
			} else {
				var21 = PDMLinkProduct.newPDMLinkProduct();
			}

			if (var5 != null && var19 != null) {
				ContainerTeam var53 = ContainerTeamHelper.service.getSharedTeamByName(var19, var5);
				if (var53 != null) {
					ContainerTeamReference var59 = ContainerTeamReference.newContainerTeamReference(var53);
					var21 = (PDMLinkProduct) ContainerTeamHelper.assignSharedTeamToContainer(var21, var59, false);
				}
			}

			var21.setName(var3);
			var21.setDescription(var7);
			var21.setContainerReference(var52);
			if (var22 != null) {
				var21.setOrganization(var22);
			}

			if (var14 != null && var14.length() != 0) {
				QuerySpec var54 = new QuerySpec(WTContainerTemplateMaster.class);
				SearchCondition var61 = new SearchCondition(WTContainerTemplateMaster.class, "name", "=", var14);
				var54.appendWhere(var61, new int[]{0, 1});
				LookupSpec var60 = new LookupSpec(var54, var52);
				var60.setFirstMatchOnly(true);
				QueryResult var64 = WTContainerHelper.service.lookup(var60);
				if (!var64.hasMoreElements()) {
					String[] var65 = new String[]{var14};
					throw new WTException(RESOURCE, "205", var65);
				}

				WTContainerTemplateMaster var27 = (WTContainerTemplateMaster) var64.nextElement();
				WTContainerTemplate var28 = ContainerTemplateHelper.service.getContainerTemplateRef(var27)
						.getTemplate();
				var21.setContainerTemplate(var28);
			}

			WTContainerHelper.service.create(var21);
			WTContainerRef var55 = WTContainerRef.newWTContainerRef(var21);

			try {
				FolderHelper.service.createSubFolder("/System/Reports", var55);
			} catch (UniquenessException var46) {
			}

			try {
				FolderHelper.service.createSubFolder("/System/Reports/ChangeMonitor", var55);
			} catch (UniquenessException var45) {
			}

			try {
				FolderHelper.service.createSubFolder("/System/Reports/ChangeMonitor/Custom", var55);
			} catch (UniquenessException var44) {
			}

			var2.add(var21);
			boolean var62 = true;
			return var62;
		} catch (WTException var47) {
			LoadServerHelper.printMessage("createProductContainer: " + var47.getLocalizedMessage());
			var47.printStackTrace();
			var4 = false;
			return var4;
		} catch (WTInvalidParameterException var48) {
			LoadServerHelper.printMessage("createProductContainer: " + var48.getLocalizedMessage());
			var48.printStackTrace();
			var4 = false;
			return var4;
		} catch (WTPropertyVetoException var49) {
			LoadServerHelper.printMessage("createProductContainer: " + var49.getLocalizedMessage());
			var49.printStackTrace();
			var4 = false;
		} finally {
			try {
				resetUser();
				MethodContext.getContext().remove(WTContainerHelper.EXTENDABLE_CONTAINER);
			} catch (WTException var43) {
				LoadServerHelper.printMessage("\ncreateProductContainer: " + var43.getMessage());
			}

		}

		return var4;
	}

	public static boolean createProductSerialNumberEffectivity(Hashtable var0, Hashtable var1, Vector var2) {
		return createProductEffectivity(var0, var1, var2, "wt.part.ProductSerialNumberEffectivity");
	}

	public static boolean createProductLotNumberEffectivity(Hashtable var0, Hashtable var1, Vector var2) {
		return createProductEffectivity(var0, var1, var2, "wt.part.ProductLotNumberEffectivity");
	}

	public static boolean createProductMSNEffectivity(Hashtable var0, Hashtable var1, Vector var2) {
		return createProductEffectivity(var0, var1, var2, "wt.part.ProductMSNEffectivity");
	}

	public static boolean createProductBlockEffectivity(Hashtable var0, Hashtable var1, Vector var2) {
		return createProductEffectivity(var0, var1, var2, "wt.part.ProductBlockEffectivity");
	}

	public static boolean createProductDateEffectivity(Hashtable var0, Hashtable var1, Vector var2) {
		return createProductEffectivity(var0, var1, var2, "wt.effectivity.WTDatedEffectivity");
	}

	protected static boolean createProductEffectivity(Hashtable var0, Hashtable var1, Vector var2, String var3) {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Start of createProductEffectivity method");
			}

			boolean var4 = false;
			new HashMap();
			if (null == var3) {
				if (logger.isDebugEnabled()) {
					logger.debug("effTypeClass is null");
				}

				return false;
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("effTypeClass : " + var3);
				}

				if (var3.equals("wt.effectivity.WTDatedEffectivity")) {
					var4 = true;
				}

				Map var5 = getInputValueMap(var0, var1, var4);
				String var6 = (String) var5.get("prodNumber");
				String var7 = (String) var5.get("start");
				String var8 = (String) var5.get("end");
				String var9 = (String) var5.get("type");
				String var10 = (String) var5.get("targetNumber");
				if (logger.isDebugEnabled()) {
					logger.debug("prodNumber : " + var6 + " start : " + var7 + " end : " + var8 + " type : " + var9
							+ " targetNum : " + var10);
				}

				if (var6 == null && !var3.equals("wt.effectivity.WTDatedEffectivity")) {
					if (logger.isDebugEnabled()) {
						logger.debug("prodNumber not provided in input file.");
					}

					throw new WTException("Required value for prodNumber not provided in input file.");
				} else if (var10 != null && var7 != null) {
					WTPartMaster var11 = null;
					if (var6 != null) {
						var11 = getPartMaster(var6, var9);
						if (var11 == null) {
							if (logger.isDebugEnabled()) {
								logger.debug("effContext not found");
							}

							return false;
						}

						Boolean var12 = validEffType(var11, var3, var6);
						if (!var12) {
							if (logger.isDebugEnabled()) {
								logger.debug("Not a valid effType");
							}

							return false;
						}

						if (logger.isDebugEnabled()) {
							logger.debug("effTypeClass : " + var3 + " for effContext : " + var11);
						}
					}

					WTPart var19 = getTargetPart(var10, (String) var5.get("targetVer"), (String) var5.get("targetView"),
							var9);
					if (var19 == null) {
						if (logger.isDebugEnabled()) {
							logger.debug("Target part not found");
						}

						return false;
					} else {
						if (logger.isDebugEnabled()) {
							Logger var10000 = logger;
							String var10001 = var19.getNumber();
							var10000.debug("targetPart Number : " + var10001 + " targetPart Name : " + var19.getName());
						}

						String var13 = var7 + EffGroupAssistant.getDash();
						if (var8 != null) {
							var13 = var13 + var8;
						}

						if (logger.isDebugEnabled()) {
							logger.debug("effValue : " + var13);
						}

						String var14 = (String) var5.get("typeModifier");
						EffTypeModifier var15 = null;
						if (var14 != null) {
							var15 = EffTypeModifier.toEffTypeModifier(var14);
							if (logger.isDebugEnabled()) {
								logger.debug("modifier : " + var15);
							}
						}

						ArrayList var16 = new ArrayList();
						ClientEffGroup var17 = createClientEffGroup(var11, var19, var13, var15, var3);
						var16.add(var17);
						EffHelper.service.saveClientEffGroups(var16, (List) null, (List) null);
						if (logger.isDebugEnabled()) {
							logger.debug("End of createProductEffectivity method without any exception");
						}

						return true;
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("targetNumber or startRange is null");
					}

					throw new WTException("Required value for targetNumber or startRange not provided in input file.");
				}
			}
		} catch (Exception var18) {
			if (logger.isDebugEnabled()) {
				logger.debug("End of createProductEffectivity method with exception");
			}

			LoadServerHelper.printMessage("createProductEffectivity: " + var18.getLocalizedMessage());
			var18.printStackTrace();
			return false;
		}
	}

	protected static ClientEffGroup createClientEffGroup(WTPartMaster var0, WTPart var1, String var2,
			EffTypeModifier var3, String var4) throws WTPropertyVetoException, ClassNotFoundException {
		ClientEffGroup var5 = new ClientEffGroup();
		var5.setEffContextReference(EffHelper.getWTReference(var0));
		var5.setRange(var2);
		var5.setChangeableReference(EffHelper.getWTReference(var1));
		var5.setEffTypeModifier(var3);
		Class var6 = Class.forName(var4);
		var5.setEffFormClass(EffHelper.getEffFormClass(var6));
		return var5;
	}

	protected static WTPartMaster getPartMaster(String var0, String var1) {
		String[] var2 = new String[2];
		String var3 = null;

		try {
			QuerySpec var4 = new QuerySpec(WTPartMaster.class);
			var4.appendWhere(new SearchCondition(WTPartMaster.class, "number", "=", var0.toUpperCase()), new int[]{0});
			QueryResult var5 = PersistenceHelper.manager.find(var4);
			Object var6 = null;
			if (var5.size() == 0) {
				var2[0] = var0;
				var3 = WTMessage.getLocalizedMessage(RESOURCE, "202", var2);
				LoadServerHelper.printMessage("\n" + var3);
				return null;
			} else if (var5.size() > 1) {
				var2[0] = var1;
				var2[1] = var0;
				var3 = WTMessage.getLocalizedMessage(RESOURCE, "209", var2);
				LoadServerHelper.printMessage("\n" + var3);
				return null;
			} else {
				return (WTPartMaster) var5.nextElement();
			}
		} catch (QueryException var7) {
			LoadServerHelper.printMessage("getPartMaster : " + var7.getLocalizedMessage());
			var7.printStackTrace();
			return null;
		} catch (WTException var8) {
			LoadServerHelper.printMessage("getPartMaster : " + var8.getLocalizedMessage());
			var8.printStackTrace();
			return null;
		}
	}

	protected static boolean validEffType(WTPartMaster var0, String var1, String var2) {
		Class[] var3 = EffHelper.getValidEffectivityForms(var0);
		boolean var5 = false;

		try {
			Class var4 = Class.forName(var1);
			Class var6 = EffHelper.getEffFormClass(var4);

			for (int var7 = 0; var7 < var3.length; ++var7) {
				if (var3[var7].getName().equals(var6.getName())) {
					var5 = true;
					break;
				}
			}

			if (!var5) {
				String[] var10 = new String[2];
				String var8 = null;
				var10[0] = EffHelper.getFormDisplayName(var6);
				var10[1] = var2;
				var8 = WTMessage.getLocalizedMessage(RESOURCE, "244", var10);
				LoadServerHelper.printMessage("\n" + var8);
				return false;
			}
		} catch (ClassNotFoundException var9) {
			LoadServerHelper.printMessage("validEffType : " + var9.getLocalizedMessage());
			var9.printStackTrace();
		}

		return var5;
	}

	protected static Map<String, String> getInputValueMap(Hashtable var0, Hashtable var1, boolean var2)
			throws WTException {
		HashMap var3 = new HashMap();
		var3.put("type", getValue("type", var0, var1, false));
		var3.put("targetNumber", getValue("targetNumber", var0, var1, true));
		var3.put("targetVer", getValue("targetVer", var0, var1, false));
		var3.put("typeModifier", getValue("typeModifier", var0, var1, false));
		var3.put("targetView", getValue("targetView", var0, var1, false));
		if (var2) {
			var3.put("prodNumber", getValue("prodNumber", var0, var1, false));
			var3.put("start", getValue("startDate", var0, var1, true));
			var3.put("end", getValue("endDate", var0, var1, false));
		} else {
			var3.put("prodNumber", getValue("prodNumber", var0, var1, true));
			var3.put("start", getValue("startNumber", var0, var1, true));
			var3.put("end", getValue("endNumber", var0, var1, false));
		}

		if (var3.get("type") == null || ((String) var3.get("type")).length() == 0) {
			var3.put("type", "wt.part.WTPart");
		}

		Iterator var4 = var3.entrySet().iterator();

		while (var4.hasNext()) {
			Map.Entry var5 = (Map.Entry) var4.next();
			if (var3.get(var5.getValue()) != null && ((String) var3.get(var5.getValue())).length() == 0) {
				var3.put((String) var5.getKey(), (Object) null);
			}
		}

		return var3;
	}

	protected static WTPart getTargetPart(String var0, String var1, String var2, String var3) throws WTException {
		String[] var4 = new String[2];
		String var5 = null;
		WTPart var6 = null;
		if (var3.equals("wt.part.WTPart")) {
			var6 = getPart(var0, var1, (String) null, var2);
		}

		if (var6 == null) {
			var4[0] = var3;
			var4[1] = var0;
			var5 = WTMessage.getLocalizedMessage(RESOURCE, "208", var4);
			LoadServerHelper.printMessage("\n" + var5);
			return null;
		} else {
			return var6;
		}
	}

	protected static void setUser(Hashtable var0, Hashtable var1) throws WTException {
		LoadServerHelper.setCacheValue("PART_PREVIOUS_USER:", SessionMgr.getPrincipal().getName());
		String var2 = getValue("user", var0, var1, false);
		if (var2 != null) {
			LoadServerHelper.changePrincipal(var2);
		}

	}

	protected static void resetUser() throws WTException {
		String var0 = (String) LoadServerHelper.getCacheValue("PART_PREVIOUS_USER:");
		if (var0 != null) {
			LoadServerHelper.changePrincipal(var0);
		}

	}

	public static WTOrganization getOrganization(String var0, String var1) throws WTException {
		if (var0 == null && var1 == null) {
			return null;
		} else {
			String var2 = null;
			String var3 = null;
			String var4 = null;
			String var5 = null;

			class OrgKey {
				volatile int hashCode;
				final String key;

				OrgKey(String var1) {
					this.key = var1;
				}

				public boolean equals(Object var1) {
					if (var1 == this) {
						return true;
					} else if (!(var1 instanceof OrgKey)) {
						return false;
					} else {
						OrgKey var2 = (OrgKey) var1;
						return this.key.equals(var2.key);
					}
				}

				public int hashCode() {
					if (this.hashCode == 0) {
						int var1 = 17;
						var1 = 37 * var1 + this.key.hashCode();
						this.hashCode = var1;
					}

					return this.hashCode;
				}

				public String toString() {
					return "wt.load.LoadPart.OrgKey [key=\"" + this.key + "\"]";
				}
			}

			OrgKey var6 = null;
			if (var0 != null) {
				int var7 = var0.indexOf("$");
				if (var7 < 0) {
					throw new WTException("Invalid orgainzation id. The format is <coding system>$<value>.");
				}

				var3 = var0.substring(0, var7);
				var4 = var0.substring(var7 + 1);
				var5 = getCodingSystem(var3);
				var2 = var5 + "$" + var4;
				var6 = new OrgKey(var2);
			}

			OrgKey var10 = var1 == null ? null : new OrgKey(var1);
			WTOrganization var8 = null;
			if (var6 != null) {
				var8 = (WTOrganization) LoadServerHelper.getFromCache(var6);
			}

			if (var8 == null && var10 != null) {
				var8 = (WTOrganization) LoadServerHelper.getFromCache(var10);
			}

			if (var8 == null) {
				if (var0 != null) {
					var8 = IxbHndHelper.getOrganizationByGlobalOrgId(var2);
					if (VERBOSE) {
						System.out.println(
								"LoadPart.getOrganization() found organization. ID Key: " + var6 + " Org: " + var8);
					}

					if (var1 != null) {
						vertifyOrgName(var1, var8);
					}
				}

				if (var8 == null && var1 != null) {
					DirectoryContextProvider var9 = WTContainerHelper.service.getExchangeContainer()
							.getContextProvider();
					var8 = OrganizationServicesHelper.manager.getOrganization(var1, var9);
					if (VERBOSE) {
						System.out.println(
								"LoadPart.getOrganization() found organization. Name Key: " + var10 + " Org: " + var8);
					}

					if (var0 != null) {
						vertifyOrgId(var3, var4, var5, var8);
					}
				}

				if (var8 != null) {
					if (var6 == null) {
						String var11 = getOrgID(var8);
						if (var11 != null) {
							var6 = new OrgKey(var11);
						}
					}

					LoadServerHelper.putCacheValue(var6, var8);
					if (var10 == null) {
						var10 = new OrgKey(var8.getName());
					}

					LoadServerHelper.putCacheValue(var10, var8);
				}
			}

			return var8;
		}
	}

	private static String getCodingSystem(String var0) throws WTException {
		String var1 = null;
		if (codingSystemMap.values().contains(var0)) {
			var1 = var0;
		} else {
			var1 = (String) codingSystemMap.get(var0.toUpperCase());
			if (var1 == null) {
				throw new WTException("Invalid coding system.");
			}
		}

		return var1;
	}

	private static void vertifyOrgId(String var0, String var1, String var2, WTOrganization var3) throws WTException {
		if (var3 != null) {
			String var4 = null;
			Set var5 = codingSystemMap.keySet();
			Iterator var6 = var5.iterator();

			while (true) {
				if (!var6.hasNext()) {
					if (!var3.getUniqueIdentifier().equals(var1) || !var3.getCodingSystem().equals(var2)) {
						throw new WTException(
								"Org with matching name does not have matching ID.\n Actual coding system: " + var4
										+ " (" + var3.getCodingSystem() + ")\n Expected org name: " + var0
										+ "\n Actual unique identifier: " + var3.getUniqueIdentifier()
										+ "\n Expected unique identifier: " + var1 + "\n Org: " + var3);
					}
					break;
				}

				Object var7 = var6.next();
				if (((String) codingSystemMap.get(var7)).equals(var3.getCodingSystem())) {
					var4 = (String) var7;
				}
			}
		}

	}

	private static void vertifyOrgName(String var0, WTOrganization var1) throws WTException {
		if (var0 != null && var1 != null && !var1.getName().equals(var0)) {
			throw new WTException("Org with matching ID does not have matching name.\nActual org name: "
					+ var1.getName() + "\nExpected org name: " + var0 + "\nOrg: " + var1);
		}
	}

	private static String getOrgID(WTOrganization var0) {
		if (var0.getCodingSystem() == null) {
			return null;
		} else {
			String var10000 = var0.getCodingSystem();
			return var10000 + "$" + var0.getUniqueIdentifier();
		}
	}

	protected static void setGenericType(WTPart var0, String var1) throws WTException {
		try {
			if (var1 != null) {
				var0.setGenericType(GenericType.toGenericType(var1));
			}

		} catch (WTPropertyVetoException var3) {
			throw new WTException(var3);
		}
	}

	protected static void setSecurityLabels(SecurityLabeled var0, String var1) throws WTException {
		if (var1 != null && var1.length() > 0) {
			try {
				AccessControlServerHelper.manager.setSecurityLabels(var0, var1, false);
			} catch (WTPropertyVetoException var3) {
				throw new WTException(var3);
			}
		}

	}

	protected static void setLogicBase(WTPart var0, Hashtable var1, Hashtable var2) throws WTException {
		try {
			String var3 = LoadServerHelper.getValue("logicbasePath", var1, var2, 1);
			if (var3 != null) {
				Class[] var4 = new Class[]{WTPart.class, String.class, Hashtable.class, Hashtable.class};
				Class var5 = Class.forName("com.ptc.wpcfg.load.LoadHelper");
				Method var6 = var5.getMethod("setLogicBase", var4);
				var6.invoke((Object) null, var0, var3, var1, var2);
			}
		} catch (SecurityException var7) {
			var7.printStackTrace();
		} catch (IllegalArgumentException var8) {
			var8.printStackTrace();
		} catch (InvocationTargetException var9) {
			var9.printStackTrace();
		} catch (ClassNotFoundException var10) {
			var10.printStackTrace();
		} catch (NoSuchMethodException var11) {
			var11.printStackTrace();
		} catch (IllegalAccessException var12) {
			var12.printStackTrace();
		}

	}

	private static boolean isChildSameAsParent(WTPart var0, WTPartMaster var1) {
		return var1.equals(var0.getMaster());
	}

	private static boolean isDuplicatePartDocRecord(WTPart var0, WTDocumentMaster var1) throws WTException {
		if (logger.isDebugEnabled()) {
			logger.debug("Enter => isDuplicatePartDocRecord(WTPart, WTDocumentMaster).");
			logger.debug("part: " + IdentityFactory.getDisplayIdentifier(var0));
			logger.debug("documentMaster: " + IdentityFactory.getDisplayIdentifier(var1));
		}

		boolean var2 = false;
		int[] var3 = new int[]{0};
		QuerySpec var4 = new QuerySpec();
		var4.appendClassList(WTPartReferenceLink.class, true);
		var4.appendWhere(new SearchCondition(WTPartReferenceLink.class, "roleAObjectRef.key.id", "=",
				var0.getPersistInfo().getObjectIdentifier().getId()), var3);
		var4.appendAnd();
		var4.appendWhere(new SearchCondition(WTPartReferenceLink.class, "roleBObjectRef.key.id", "=",
				var1.getPersistInfo().getObjectIdentifier().getId()), var3);
		if (logger.isDebugEnabled()) {
			logger.debug("querySpec: " + var4);
		}

		QueryResult var5 = PersistenceHelper.manager.find(var4);
		if (var5 != null && var5.hasMoreElements()) {
			var2 = true;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Returning: " + var2);
		}

		return var2;
	}

	private static boolean isDuplicatePartDocRecord(WTPart var0, WTDocument var1) throws WTException {
		boolean var2 = false;
		QuerySpec var3 = new QuerySpec();
		var3.appendClassList(WTPartDescribeLink.class, true);
		var3.appendWhere(new SearchCondition(WTPartDescribeLink.class, "roleAObjectRef.key.id", "=",
				var0.getPersistInfo().getObjectIdentifier().getId()));
		var3.appendAnd();
		var3.appendWhere(new SearchCondition(WTPartDescribeLink.class, "roleBObjectRef.key.id", "=",
				var1.getPersistInfo().getObjectIdentifier().getId()));
		QueryResult var4 = PersistenceHelper.manager.find(var3);
		if (var4.hasMoreElements()) {
			var2 = true;
			return var2;
		} else {
			return var2;
		}
	}

	private static boolean isValidPartDocAssociation(int var0, WTPart var1, WTDocument var2) throws WTException {
		String var3 = null;
		boolean var4 = false;
		List var5 = null;
		ArrayList var6 = new ArrayList();
		TypeIdentifier var7 = TypedUtility.getTypeIdentifier("WCTYPE|wt.doc.WTDocument|com.ptc.ReferenceDocument");
		if (var7 == null) {
			System.out.println(
					" WARNING - WCTYPE|wt.doc.WTDocument|Reference Document is not defined.\n Run loaddata.bat PLMLinkPartDocs.csv");
		}

		if (var0 == 0) {
			var3 = "wt.part.WTPartReferenceLink";
		} else if (var0 == 1) {
			var3 = "wt.part.WTPartDescribeLink";
		}

		TypeIdentifier var8 = null;
		TypeIdentifier var9 = null;
		WCTypeIdentifier var10 = null;
		boolean var11 = PartDocHelper.isWcPDMMethod();

		try {
			var8 = TypeIdentifierUtilityHelper.service.getTypeIdentifier(var1);
			IdentifierFactory var12 = (IdentifierFactory) DefaultServiceProvider.getService(IdentifierFactory.class,
					"logical");
			if (var3 == null) {
				logger.debug("Link type is null");
				return true;
			}

			var10 = (WCTypeIdentifier) var12.get(var3);
			WTContainer var13 = var1.getContainer();
			var9 = TypeIdentifierUtilityHelper.service.getTypeIdentifier(var2);
			var5 = AssociationConstraintHelper.service.getValidRoleBTypes(var8, var10, var13, false);
			Iterator var14;
			TypeIdentifier var15;
			if (!var11 && var0 == 0) {
				var14 = var5.iterator();

				label93 : while (true) {
					do {
						if (!var14.hasNext()) {
							break label93;
						}

						var15 = (TypeIdentifier) var14.next();
					} while (!var15.equals(var7) && !var15.toString().contains(var7.toString()));

					var6.add(var15);
				}
			}

			if (!var11 && var0 == 1) {
				var14 = var5.iterator();

				while (var14.hasNext()) {
					var15 = (TypeIdentifier) var14.next();
					if (!var15.equals(var7) && !var15.toString().contains(var7.toString())) {
						var6.add(var15);
					}
				}
			}
		} catch (RemoteException var16) {
			var16.printStackTrace();
		} catch (WTException var17) {
			var17.printStackTrace();
		}

		if (var11 && var5.contains(var9)) {
			var4 = true;
		} else {
			if (!var6.contains(var9)) {
				String[] var18 = new String[3];
				String var19 = "null";
				var18[0] = var8 != null ? var8.toString() : var19;
				var18[1] = var10 != null ? var10.toString() : var19;
				var18[2] = var9 != null ? var9.toString() : var19;
				logger.error("No valid relationship constraint found for parent type " + var18[0] + ", link type"
						+ var18[1] + ", and child type " + var18[2] + ".");
				throw new WTException(
						WTMessage.getLocalizedMessage(RESOURCE, "NO_VALID_ASSOCIATION_CONSTRAINT", var18));
			}

			var4 = true;
		}

		return var4;
	}

	private static Map getTransactionMap() throws WTException {
		return Transaction.getCurrentTransaction().isShared() ? Transaction.getSharedMap() : Transaction.getGlobalMap();
	}

	static {
		try {
			rb = ResourceBundle.getBundle(RESOURCE, WTContext.getContext().getLocale());
			WTProperties var0 = WTProperties.getLocalProperties();
			VERBOSE = var0.getProperty("wt.part.load.verbose", false);
			WTHOME = var0.getProperty("wt.home", "");
			DIRSEP = var0.getProperty("dir.sep", "");
			PART_TI = (TypeIdentifier) ReflectionHelper.dynamicInvoke(
					"com.ptc.core.foundation.type.server.impl.TypeHelper", "getTypeIdentifier",
					new Class[]{String.class}, new Object[]{WTPart.class.getName()});
			codingSystemMap.put("CAGE", "0141");
			codingSystemMap.put("DUNS", "0060");
			codingSystemMap.put("ISO65233", "0026");
		} catch (Throwable var1) {
			System.err.println("Error initializing " + APPLPartLoader.class.getName());
			var1.printStackTrace(System.err);
			throw new ExceptionInInitializerError(var1);
		}
	}
}