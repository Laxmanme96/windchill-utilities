package ext.company.association;

import java.util.Locale;

import wt.epm.structure.EPMStructureEvent;
import wt.epm.structure.EPMStructureServiceEventListener;
import wt.fc.PersistenceHelper;
import wt.fc.Persistable;
import wt.iba.value.service.LoadValue;
import wt.iba.value.service.StandardIBAValueService;
import wt.part.WTPart;
import wt.session.SessionHelper;
import wt.util.WTException;
import com.ptc.core.lwc.server.PersistableAdapter;

public class UpdateAttributeOnAssociate implements EPMStructureServiceEventListener {

    /**
     * This method gets triggered after auto-association of CAD to WTPart.
     */
    @Override
    public void postAssociate(EPMStructureEvent event) throws WTException {
        try {
            Persistable obj = event.getNewPart();   // The newly associated WTPart
            if (obj instanceof WTPart) {
                WTPart wtPart = (WTPart) obj;

                // Get WTPart name
                String partName = wtPart.getName();

                // Update the custom attribute
                updateCustomAttribute(wtPart, "CustomAttributeName", partName);

                System.out.println("Updated WTPart [" + wtPart.getNumber() + "] CustomAttributeName = " + partName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new WTException("Error updating custom attribute during auto-associate: " + e.getMessage());
        }
    }

    /**
     * Utility method to update IBA/custom attribute on WTPart
     */
    private void updateCustomAttribute(WTPart wtPart, String attributeName, String attributeValue) throws Exception {
        Locale locale = SessionHelper.manager.getLocale();
        PersistableAdapter obj = new PersistableAdapter(wtPart, null, locale, new LoadValue());
        obj.load(attributeName);
        obj.set(attributeName, attributeValue);
        obj.apply();
        PersistenceHelper.manager.modify(wtPart);
    }
}
