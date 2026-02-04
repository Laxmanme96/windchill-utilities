package ext.custom.datautility;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.AttributeGuiComponent;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.UpdateOperationIdentifier;
import com.ptc.core.ui.resources.ComponentMode;

import wt.doc.WTDocument;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.iba.definition.StringDefinition;
import wt.iba.value.StringValue;
import wt.part.WTPart;
import wt.pds.StatementSpec;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

//This class will store Original number of Objects in a global IBA called Original Number
public class RetainObjectNumberDataUtility extends DefaultDataUtility {

	/**
	 * @param str          - attr name
	 * @param paramObject  - wtobject
	 * @param modelContext - input
	 * @return component - ui component
	 * @throws WTException exception
	 */

	/**
	 * Method to copy Object Number to a new attribute.
	 */
	@Override
	public Object getDataValue(String str, Object paramObject, ModelContext modelContext) throws WTException {
		System.out.println(" RetainObjectNumberDataUtility STARTED   ");
		StringValue stringvalue = null;
		ComponentMode mode = modelContext.getDescriptorMode();
		Object object = super.getDataValue(str, paramObject, modelContext);
		AttributeGuiComponent guiComp = (AttributeGuiComponent) object;
		String number = null;
		if (mode.equals(ComponentMode.VIEW)) {
			if (paramObject instanceof EPMDocument) {
				EPMDocument epm = (EPMDocument) paramObject;
				try {
					number = epm.getNumber();
					System.out.println("Number is----" + number);
					final QueryResult qr = VersionControlHelper.service.allVersionsOf(epm);
					if (qr.size() == 1) {
						PersistableAdapter pers = new PersistableAdapter(epm, null, null,
								new UpdateOperationIdentifier());
						pers.load("OriginalNumber");
						String origNum = (String) pers.get("OriginalNumber");
						if (origNum == null) {
							StringDefinition stringdefinition = getStringDef("OriginalNumber");
							stringvalue = getStringValue(epm, stringdefinition);
							if (stringvalue == null) {
								stringvalue = StringValue.newStringValue(stringdefinition, epm, number);
							}
						} else {
							System.out.println("No need to update epm");
							return object;
						}
					} else {
						System.out.println("No need to update epm");
						return object;
					}
				} catch (WTException wtExp) {
					return "";
				}
			} else if (paramObject instanceof WTPart) {

				WTPart part = (WTPart) paramObject;
				number = part.getNumber();
				final QueryResult qr = VersionControlHelper.service.allVersionsOf(part);
				if (qr.size() == 1) {
					PersistableAdapter pers = new PersistableAdapter(part, null, null, new UpdateOperationIdentifier());
					pers.load("OriginalNumber");
					String origNum = (String) pers.get("OriginalNumber");
					if (origNum == null) {
						StringDefinition stringdefinition = getStringDef("OriginalNumber");
						stringvalue = getStringValue(part, stringdefinition);
						if (stringvalue == null) {
							stringvalue = StringValue.newStringValue(stringdefinition, part, number);
						}
					} else {
						System.out.println("No need to update part");
						return object;

					}
				} else {
					return object;
				}
			} else if (paramObject instanceof WTDocument) {
				System.out.println(" RetainObjectNumberDataUtility STARTED for WTDocument   ");
				WTDocument doc = (WTDocument) paramObject;
				number = doc.getNumber();
				final QueryResult qr = VersionControlHelper.service.allVersionsOf(doc);
				if (qr.size() == 1) {
					PersistableAdapter pers = new PersistableAdapter(doc, null, null, new UpdateOperationIdentifier());
					pers.load("OriginalNumber");
					String origNum = (String) pers.get("OriginalNumber");
					if (origNum == null) {
						StringDefinition stringdefinition = getStringDef("OriginalNumber");
						stringvalue = getStringValue(doc, stringdefinition);
						if (stringvalue == null) {
							stringvalue = StringValue.newStringValue(stringdefinition, doc, number);
						}
					} else {
						return object;

					}
				} else {
					return object;
				}
			}
			PersistenceHelper.manager.save(stringvalue);

			return number;
		} else if (mode.equals(ComponentMode.EDIT)) {
			guiComp.setComponentHidden(true);
			return object;
		}

		return object;
	}

	private static StringDefinition getStringDef(String stringDefName) throws WTException {

		QuerySpec queryspec = new QuerySpec(StringDefinition.class);
		SearchCondition searchcondition = new SearchCondition(StringDefinition.class, "name", "=", stringDefName);
		queryspec.appendWhere(searchcondition, new int[] { 0 });
		QueryResult queryresult = PersistenceHelper.manager.find((StatementSpec) queryspec);
		StringDefinition stringdefinition = null;
		if (queryresult.hasMoreElements()) {
			return (StringDefinition) queryresult.nextElement();
		}

		return stringdefinition;
	}

	private static StringValue getStringValue(Persistable persistable, StringDefinition stringdefinition)
			throws WTException {

		if (stringdefinition == null) {
			return null;
		}
		QuerySpec queryspec = new QuerySpec(StringValue.class);
		queryspec.appendWhere(new SearchCondition(StringValue.class, "theIBAHolderReference.key", "=",
				persistable.getPersistInfo().getObjectIdentifier()), new int[] { 0 });
		queryspec.appendAnd();
		queryspec.appendWhere(new SearchCondition(StringValue.class, "definitionReference.key", "=",
				stringdefinition.getPersistInfo().getObjectIdentifier()), new int[] { 0 });
		QueryResult queryresult = PersistenceHelper.manager.find((StatementSpec) queryspec);
		if (queryresult.hasMoreElements()) {

			return (StringValue) queryresult.nextElement();
		} else {
			return null;

		}
	}

}
/*
 * <Service context="default"
 * name="com.ptc.core.components.descriptor.DataUtility"
 * targetFile="codebase/service.properties"> <Option cardinality="duplicate"
 * order="0" overridable="true" requestor="java.lang.Object"
 * selector="OriginalNumber"
 * serviceClass="ext.training.dataUtility.RetainObjectNumberDataUtility"/>
 * </Service>
 */
