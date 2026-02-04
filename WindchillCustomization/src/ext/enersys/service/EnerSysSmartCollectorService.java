package ext.enersys.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.doc.WTDocument;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.collections.WTSet;
import wt.method.RemoteInterface;
import wt.part.WTPart;
import wt.util.WTException;

@RemoteInterface
public interface EnerSysSmartCollectorService {

	/*
	 * Constants
	 */
	public static final String STATE_INWORK = "INWORK";
	public static final String STATE_CANCELLED = "CANCELLED";
	public static final String STATE_OBSOLETE = "OBSOLETE";
	public static final String STATE_UNDERREVIEW = "UNDERREVIEW";
	public static final String STATE_END_OF_LIFE = "END_OF_LIFE";
	public static final String STATE_DRAFT = "DRAFT";
	public static final String STATE_RELEASED = "RELEASED";
	public static final String STATE_PRODUCTION_RELEASED = "PRODUCTIONRELEASED";
	public static final String STATE_PROTOTYPE_A = "PROTOTYPE_A";
	public static final String STATE_PROTOTYPE_B = "PROTOTYPE_B";
	public static final String STATE_PROTOTYPE_C = "PROTOTYPE_C";
	public static final String STATE_A_RELEASE_CONCEPT = "A_RELEASE_CONCEPT";
	public static final String STATE_B_RELEASE_CONCEPT = "B_RELEASE_CONCEPT";
	public static final String STATE_C_RELEASE_CONCEPT = "C_RELEASE_CONCEPT";
	public static final String STATE_B_INWORK = "B_INWORK";
	public static final String STATE_C_INWORK = "C_INWORK";
	public static final String STATE_PRODUCTION_INWORK = "PRODUCTION_INWORK";

	public static final String DESCRIBE_BY = "DescribeBy";
	public static final String REFERENCE_BY = "ReferenceBy";
	public static final String ROLE_B_OBJECT = "roleBObject";
	public static final String ALL = "All";
	
	public static final String OWNER = "Owner";
	public static final String IMAGE = "Image";
	public static final String CONTENT = "Content";
	public static final String CALCULATED = "Calculated";
	public static final String CONTRIBUTING_CONTENT = "Contributing Content";
	public static final String CONTRIBUTING_IMAGE = "Contributing Image";

	public static final String AFFECTED_DATA_TABLE_ID = "changeRequest_affectedData_table";
	public static final String PROMOTION_REQ_TABLE_ID = "promotionRequest.promotionObjects";
	public static final String ACTIVITY_DATA_TABLE_ID = "changeTask_affectedItems_table";
    public static final String AFFECTED_DATA_TABLE_STEP = "change$affectedDataStep";
    public static final String PROMOTION_REQ_TABLE_STEP = "promotionRequest$promotionObjectsTableStep";
    public static final String ACTIVITY_DATA_TABLE_STEP = "changeTask$affectedAndResultingItemsStep";
    public HashMap<WTPart,HashSet<WTPart>> getChildParts(HashMap<WTPart,HashSet<WTPart>> childPartsMap,HashSet<TypeIdentifier> allowedPartTypesSet,WTPart parentPart, Boolean level, String targetState,NmCommandBean commandBean);
	public HashMap<String,HashSet<WTDocument>> getDesByorRefByDocuments(WTPart part, Set refByorDesBySet, HashMap<String,HashSet<WTDocument>> allObjectsMap, String targetState,NmCommandBean commandBean);
	public HashMap<String,HashSet<EPMDocument>> getEPMDocuments(WTPart part, Set associationTypeSet, HashMap<String,HashSet<EPMDocument>> allObjectsMap, String targetState,NmCommandBean commandBean);
	public HashSet<WTPart> getAMLPartInformation(Persistable per, HashSet<WTPart> sumaPartSet, String targetState,NmCommandBean commandBean);
	public HashSet<WTPart> getParentParts(HashSet<WTPart> childSet,HashSet<TypeIdentifier> allowedPartTypesSet,WTPart childPart,String targetState,NmCommandBean commandBean);
	public boolean isValidforTableofSmartCollector (Persistable currentObject,String targetState,NmCommandBean commandBean);
	public String getSourceTableIdForSmartCollector(NmCommandBean commandBean);
	}
