package ext.ptpl.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.apache.logging.log4j.Logger;

import com.ptc.core.lwc.common.view.AttributeDefinitionReadView;
import com.ptc.core.lwc.common.view.ConstraintDefinitionReadView;
import com.ptc.core.lwc.common.view.TypeDefinitionReadView;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.ui.validation.DefaultUIComponentValidator;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationResultSet;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.doc.WTDocument;
import wt.fc.WTObject;
import wt.fc.WTReference;
import wt.log4j.LogR;
import wt.session.SessionHelper;
import wt.type.ClientTypedUtility;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTException;

public class ValidateAction extends DefaultUIComponentValidator {

	protected static final Logger logger = LogR.getLogger(DisableReviseSetstate.class.getName());

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public UIValidationResultSet performFullPreValidation(UIValidationKey vKey, UIValidationCriteria vCriteria,
			Locale locale) throws WTException {
		System.out.println("ValidateRequiredAttributes STARTED : ");
		UIValidationResult result = UIValidationResult.newInstance(vKey, UIValidationStatus.ENABLED);
		UIValidationResultSet resultset = UIValidationResultSet.newInstance();
		WTReference reference = vCriteria.getContextObject();
		Object obj = reference.getObject();

		if (obj instanceof WTDocument) {
			WTDocument doc = (WTDocument) obj;
			System.out.println("Document processinge : " + doc.getNumber().toString());
			try {

				ArrayList allRAList = getAllRequiredIBA(doc);
				// System.out.println("----------allRequiredAttributes---------" + allRAList);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		System.out.println("ValidateRequiredAttributes ENDED");
		return null;

	}

	@SuppressWarnings("null")
	public static ArrayList getAllRequiredIBA(WTObject obj) throws Exception {
		System.out.println("getRequuiredContraints STARTED ---> ");

		ArrayList<String> listOfRequiredAttributes = new ArrayList<>();

		TypeIdentifier tiObj = ClientTypedUtility.getTypeIdentifier(obj);
		TypeIdentifier typeIdentifier = TypedUtilityServiceHelper.service.getTypeIdentifier(tiObj);
		TypeDefinitionReadView typeDefinitionReadView = TypeDefinitionServiceHelper.service
				.getTypeDefView(typeIdentifier);
		Collection<AttributeDefinitionReadView> allAttributes = typeDefinitionReadView.getAllAttributes();
		for (AttributeDefinitionReadView attribute : allAttributes) {
			String attributenName = attribute.getName();
			System.out.println("Proceesing  attributenName  : " + attributenName);
			Collection<ConstraintDefinitionReadView> AllConstraints = attribute.getAllConstraints();
			for (ConstraintDefinitionReadView constraintType : AllConstraints) {

//				System.out.println("Attribute Name ---> " + attributenName + " Constraint Type ---> "
//						+ constraintType.getRule().getRuleClassname());
				// Read all Required constraints.
				if (constraintType != null) {
					if (constraintType.getRule().getRuleClassname()
							.equals("com.ptc.core.meta.container.common.impl.ValueRequiredConstraint")) {
//						System.out.println(
//								"Required Attribute Data Value ---> " + constraintType.getRule().getRuleClassname());
						listOfRequiredAttributes.add(attributenName);

					}
				}
			}

		}
		return listOfRequiredAttributes;
	}

	public static String getIBAValue(WTObject wtObj, String iba) throws Exception {
		PersistableAdapter paObj = new PersistableAdapter(wtObj, null, SessionHelper.getLocale(), null);
		paObj.load(iba);
		String ibaValue = (String) paObj.get(iba);
		if (ibaValue != null)
			System.out.println("Attribute Value  :" + ibaValue);
		return ibaValue;
	}

}
