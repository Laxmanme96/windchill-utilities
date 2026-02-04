package ext.custom.rmi;

import java.util.ArrayList;
import java.util.Collection;

import com.ptc.core.lwc.common.view.AttributeDefinitionReadView;
import com.ptc.core.lwc.common.view.ConstraintDefinitionReadView;
import com.ptc.core.lwc.common.view.TypeDefinitionReadView;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;
import com.ptc.core.meta.common.AttributeTypeIdentifier;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.impl.InstanceBasedAttributeTypeIdentifier;
import com.ptc.core.meta.container.common.AttributeTypeSummary;

import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.type.ClientTypedUtility;
import wt.type.TypedUtility;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

public class GetAttributeContraints implements RemoteAccess {

	public static void main(String ar[]) throws Exception {
		RemoteMethodServer remotemethodserver = RemoteMethodServer.getDefault();

		remotemethodserver.setUserName("wcadmin");
		remotemethodserver.setPassword("wcadmin");

		QuerySpec qs = new QuerySpec(WTDocument.class);
		WTDocument doc = null;
		int count = 0;
		System.out.println("Into CheckIn method");
		try {
			qs.appendWhere(
					new SearchCondition(WTDocument.class, WTDocument.NUMBER, SearchCondition.EQUAL, "CAD000049A000"),
					null);
			QueryResult qr = PersistenceHelper.manager.find(qs);
			System.out.println("Query size : " + qr.size());

			while (qr.hasMoreElements()) {
				if (count >= 1) {
					break; // Stop after processing the first document
				}
				doc = (WTDocument) qr.nextElement();
				QueryResult qr2 = VersionControlHelper.service.allVersionsOf(doc);
				System.out.println("qr-------" + qr2.size());
				if (qr2.hasMoreElements()) {
					WTDocument LatestDoc = (WTDocument) qr2.nextElement();
					ArrayList<String> listOfRequiredAttributes = getAllRequiredIBA(LatestDoc);
					ArrayList<String> emptyRequiredAttributes = getEmptyRequiredIBA(LatestDoc,
							listOfRequiredAttributes);

					if (emptyRequiredAttributes != null) {

						System.out.println("Empty Required Attributes are : ---> " + emptyRequiredAttributes);
					}

					count++;
				}
			}

		} catch (

		WTException e) {
			e.printStackTrace();
		}

	}

	@SuppressWarnings("null")
	public static ArrayList<String> getAllRequiredIBA(WTObject obj) throws Exception {
		System.out.println("getRequuiredContraints STARTED ---> ");

		ArrayList<String> listOfRequiredAttributes = new ArrayList<>();

		TypeIdentifier tiObj = ClientTypedUtility.getTypeIdentifier(obj);
		TypeDefinitionReadView typeDefinitionReadView = TypeDefinitionServiceHelper.service.getTypeDefView(tiObj);
		Collection<AttributeDefinitionReadView> allAttributes = typeDefinitionReadView.getAllAttributes();
		for (AttributeDefinitionReadView attribute : allAttributes) {
			AttributeTypeIdentifier ati = attribute.getAttributeTypeIdentifier();

			if (ati instanceof InstanceBasedAttributeTypeIdentifier) {
				String attributenName = attribute.getName();
				// System.out.println(attributenName + " is instanceof
				// InstanceBasedAttributeTypeIdentifier ");
				Collection<ConstraintDefinitionReadView> AllConstraints = attribute.getAllConstraints();
				for (ConstraintDefinitionReadView constraintType : AllConstraints) {
					if (constraintType.getRule().getRuleClassname()
							.equals("com.ptc.core.meta.container.common.impl.ValueRequiredConstraint")) {
						System.out.println("Found required attribute: "+attributenName);
						listOfRequiredAttributes.add(attributenName);
					}
				}
			}
		}
		return listOfRequiredAttributes;

	}

	@SuppressWarnings("null")
	public static ArrayList<String> getEmptyRequiredIBA(WTObject obj, ArrayList<String> listOfRequiredAttributes)
			throws WTException {

		ArrayList<String> emptyRequiredAttributes = new ArrayList<>();
		System.out.println("allEmptyRequiredAttributes Started ");
		for (String reqAttribute : listOfRequiredAttributes) {
			PersistableAdapter paObj = new PersistableAdapter(obj, null, null, new DisplayOperationIdentifier());
			paObj.persist();
			paObj.load(reqAttribute);

			 TypeIdentifier typeIdentifier = TypedUtility.getTypeIdentifier(obj);
		        TypeDefinitionReadView typeDefView = TypeDefinitionServiceHelper.service.getTypeDefView(typeIdentifier);

		        AttributeDefinitionReadView attrSummary = typeDefView.getAttributeByName(reqAttribute);
		      String reqAttributeName =  attrSummary.getDisplayName();
			Object ibaValue = paObj.get(reqAttribute);
			if (ibaValue == null) {
				System.out.println("Attributen Name : " + reqAttributeName + " " + " Attribute Value :" + ibaValue);
				emptyRequiredAttributes.add(reqAttributeName);
			}
		}
		return emptyRequiredAttributes;
	}
}