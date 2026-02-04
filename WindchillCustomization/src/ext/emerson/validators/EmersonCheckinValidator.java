package ext.emerson.validators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.apache.logging.log4j.Logger;
import com.ptc.core.lwc.common.view.AttributeDefinitionReadView;
import com.ptc.core.lwc.common.view.ConstraintDefinitionReadView;
import com.ptc.core.lwc.common.view.TypeDefinitionReadView;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.lwc.server.TypeDefinitionServiceHelper;
import com.ptc.core.meta.common.AttributeTypeIdentifier;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.impl.InstanceBasedAttributeTypeIdentifier;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationFeedbackMsg;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationResult;
import com.ptc.core.ui.validation.UIValidationStatus;
import com.ptc.windchill.enterprise.wip.CheckinValidator;
import wt.doc.WTDocument;
import wt.fc.Persistable;
import wt.fc.WTObject;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.type.ClientTypedUtility;
import wt.type.TypedUtility;
import wt.util.WTException;

public class EmersonCheckinValidator extends CheckinValidator {
	protected static final Logger logger = LogR.getLogger(EmersonCheckinValidator.class.getName());
	private static final String VALUE_REQUIRED_CONSTRAINT_CLASSNAME = "com.ptc.core.meta.container.common.impl.ValueRequiredConstraint";

	@SuppressWarnings("deprecation")
	@Override
	public UIValidationResult validateSelectedAction(UIValidationKey var1, UIValidationCriteria vCriteria,
			Locale var3) {
		// TODO Auto-generated method stub
		logger.debug("ENTERING EmersonCheckinValidator.validateSelectedAction");
		UIValidationResult result = super.validateSelectedAction(var1, vCriteria, var3);

		Persistable obj = vCriteria.getContextObject().getObject();
		try {
			if (obj instanceof WTDocument || obj instanceof WTPart) {

				WTObject wtObject = (WTObject) obj;
				ArrayList<String> listOfRequiredAttributes = getAllRequiredIBA(wtObject);
				ArrayList<String> emptyRequiredAttributes = getEmptyRequiredIBA(wtObject, listOfRequiredAttributes);
				if (!emptyRequiredAttributes.isEmpty()) {
					for (String reqIBA : emptyRequiredAttributes) {
						result.addFeedbackMsg(new UIValidationFeedbackMsg("Attribute " + reqIBA + " can not be empty",
								FeedbackType.ERROR));
					}
					result.setStatus(UIValidationStatus.DENIED);
				}
			}
		} catch (Exception e) {
			logger.error("Error during validation", e);
		}
		logger.debug("EXITING EmersonCheckinValidator.validateSelectedAction");
		return result;
	}

	@SuppressWarnings("null")
	public static ArrayList<String> getAllRequiredIBA(WTObject obj) throws Exception {
		logger.debug("allRequiredAttributes STARTED ---> ");
		ArrayList<String> listOfRequiredAttributes = new ArrayList<>();
		TypeIdentifier tiObj = ClientTypedUtility.getTypeIdentifier(obj);
		TypeDefinitionReadView typeDefinitionReadView = TypeDefinitionServiceHelper.service.getTypeDefView(tiObj);
		Collection<AttributeDefinitionReadView> allAttributes = typeDefinitionReadView.getAllAttributes();
		for (AttributeDefinitionReadView attribute : allAttributes) {
			AttributeTypeIdentifier ati = attribute.getAttributeTypeIdentifier();
			if (ati instanceof InstanceBasedAttributeTypeIdentifier) {
				String attributenName = attribute.getName();
				Collection<ConstraintDefinitionReadView> AllConstraints = attribute.getAllConstraints();
				for (ConstraintDefinitionReadView constraintType : AllConstraints) {
					if (constraintType.getRule().getRuleClassname().equals(VALUE_REQUIRED_CONSTRAINT_CLASSNAME)) {
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
		logger.debug("allEmptyRequiredIBA Started ");
		for (String reqIBA : listOfRequiredAttributes) {
			PersistableAdapter paObj = new PersistableAdapter(obj, null, null, new DisplayOperationIdentifier());
			paObj.persist();
			paObj.load(reqIBA);
			Object ibaValue = paObj.get(reqIBA);
			TypeIdentifier typeIdentifier = TypedUtility.getTypeIdentifier(obj);
	        TypeDefinitionReadView typeDefView = TypeDefinitionServiceHelper.service.getTypeDefView(typeIdentifier);
	        AttributeDefinitionReadView attrSummary = typeDefView.getAttributeByName(reqIBA);
	        String reqAttributeDisplayName =  attrSummary.getDisplayName();
			if (ibaValue == null) {
				logger.debug("Attributen Name : " + reqIBA + " " + " Attribute Value :" + ibaValue);
				emptyRequiredAttributes.add(reqAttributeDisplayName);
			}
		}
		return emptyRequiredAttributes;
	}
}
