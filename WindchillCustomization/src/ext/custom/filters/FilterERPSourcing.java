package ext.custom.filters;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.ui.validation.DefaultSimpleValidationFilter;
import com.ptc.core.ui.validation.UIValidationCriteria;
import com.ptc.core.ui.validation.UIValidationKey;
import com.ptc.core.ui.validation.UIValidationStatus;

import wt.inf.container.WTContained;
import wt.part.WTPart;
import wt.session.SessionHelper;
import wt.type.ClientTypedUtility;
import wt.util.WTException;

public class FilterERPSourcing extends DefaultSimpleValidationFilter {

	@Override
	public UIValidationStatus preValidateAction(UIValidationKey valKey, UIValidationCriteria valCriteria) {
		UIValidationStatus valStatus = UIValidationStatus.DISABLED;

		System.out.println("---- FilterERPSourcing filter Started-------------");
		try {
			WTContained contextObj = (WTContained) valCriteria.getContextObject().getObject();
			String valideType = "WCTYPE|wt.part.WTPart|com.pluraltech.windchill.MECHANICAL_PART";
			if (contextObj instanceof WTPart) {
				WTPart part = (WTPart) contextObj;

				// API to get Container & organization
				wt.inf.container.WTContainer c = part.getContainer();
				// System.out.println("### Container = "+c.getName()+" ###");
				wt.inf.container.OrgContainer org = wt.inf.container.WTContainerHelper.service.getOrgContainer(part);
				// System.out.println("### GetOrganization = "+org.getName()+" ###");

				if (org.getName().equals("ptpl")) {
					String tiObj = ClientTypedUtility.getTypeIdentifier(part).toString();
					System.out.println("---- pType -------------" + tiObj.toString());
					if (tiObj.equals(valideType)) {
						System.out.println(
								"---- FilterERPSourcing Processing Valide Type -------------" + tiObj.toString());
						String erpIntegrated = "ERPINTEGRATED";
						Boolean ibaValue = null;
						PersistableAdapter obj = null;
						obj = new PersistableAdapter(part, null, SessionHelper.getLocale(), null);
						obj.load(erpIntegrated);
						ibaValue = (Boolean) obj.get(erpIntegrated);
						if (ibaValue != null) {
							if (ibaValue) {
								System.out.println(
										"---- FilterERPSourcing filter erpIntegratedValue is true -------------");
								valStatus = UIValidationStatus.ENABLED;
							} else {
								valStatus = UIValidationStatus.HIDDEN;
								System.out.println(
										"---- FilterERPSourcing filter erpIntegratedValue is false -------------");
							}
						}
					} else {
						valStatus = UIValidationStatus.HIDDEN;
						System.out.println(
								"---- FilterERPSourcing Processing inValide Type , Action will be hidden -------------"
										+ tiObj.toString());
					}
				} else {
					valStatus = UIValidationStatus.HIDDEN;
					System.out.println(
							"---- FilterERPSourcing Processing inValide Org  , Action will be hidden -------------"
									+ org.getName());
				}
			}
		} catch (WTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return valStatus;
	}
}
