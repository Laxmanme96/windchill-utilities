package ext.ptpl.datautility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.rendering.GuiComponent;
import com.ptc.core.components.rendering.guicomponents.AttributeInputCompositeComponent;
import com.ptc.core.components.rendering.guicomponents.IconComponent;
import com.ptc.core.components.util.OidHelper;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.enterprise.wip.datautilities.GeneralStatusHelper;
import com.ptc.windchill.enterprise.wip.datautilities.StatusHelper;

import wt.fc.Persistable;
import wt.fc.WTReference;
import wt.fc.collections.WTCollection;
import wt.inf.container.WTContainer;
import wt.log4j.LogR;
import wt.part.WTPart;

public class DisableName {
	protected static final Logger LOGGER = LogR.getLogger(DisableName.class.getName());

	public Object getDataValue(String paramString, Object localObject, ModelContext paramModelContext)
			throws Exception {
//		Object localObject = super.getDataValue(paramString, paramObject, paramModelContext);

		List<WTPart> list = new ArrayList<>();
		HashMap<WTReference, GuiComponent> cachedGlyphMap = new HashMap<>();

		if (localObject instanceof AttributeInputCompositeComponent) {
			NmCommandBean nmCommandBean = paramModelContext.getNmCommandBean();
			Persistable object = paramModelContext.getNmCommandBean().getPrimaryOid().getWtRef().getObject();
			nmCommandBean.getPrimaryOid().getWtRef().getObject();
			
			if (object instanceof WTPart) {
				list.add((WTPart) object);

				WTCollection localWTCollection = OidHelper.getWTCollection(list);
				WTContainer localWTContainer = StatusHelper.getContainer(paramModelContext);

				cachedGlyphMap = new GeneralStatusHelper().getGeneralStatus(localWTCollection, localWTContainer,paramModelContext, false );

				LOGGER.debug("cachedGlyphMap :" + cachedGlyphMap);

				for (Entry<WTReference, GuiComponent> entry : cachedGlyphMap.entrySet()) {
					if (entry.getValue() instanceof IconComponent) {
						IconComponent displayComp = (IconComponent) entry.getValue();
						String toolTip = displayComp.getTooltip();
						LOGGER.debug("FILTER INTERNAL VALUE :" + displayComp.getFilterInternalValue());
						LOGGER.debug("INTERNAL VALUE:" + displayComp.getInternalValueString());
						LOGGER.debug("TOOL TIP :" + displayComp.getTooltip());
						LOGGER.debug("PRINTABLE VALUE :" + displayComp.getPrintableValue());

						if ((toolTip != null) && (!toolTip.isEmpty()) && (toolTip.equals("New"))) {
							LOGGER.debug("***ITS A NEW OBJECT****");
							AttributeInputCompositeComponent numberField = (AttributeInputCompositeComponent) localObject;
							return numberField;
						}
					}
				}
			}
		}

		return localObject;
	}
}