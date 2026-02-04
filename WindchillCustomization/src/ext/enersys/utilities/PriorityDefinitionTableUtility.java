package ext.enersys.utilities;

import java.beans.PropertyVetoException;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.AttributeInputCompositeComponent;
import com.ptc.core.components.rendering.guicomponents.GUIComponentArray;
import com.ptc.core.components.rendering.guicomponents.UrlDisplayComponent;

import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentHolder;
import wt.content.FormatContentHolder;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.log4j.LogR;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;
import wt.vc.config.ConfigHelper;
import wt.vc.config.LatestConfigSpec;

public class PriorityDefinitionTableUtility extends DefaultDataUtility {

	private static final Logger LOGGER = LogR.getLoggerInternal(PriorityDefinitionTableUtility.class.getName());

	@Override
	public Object getDataValue(String component_id, Object datum, ModelContext mc) throws WTException {
		LOGGER.debug("##### Start getDataValue - PriorityDefinitionTableUtility");

		// Get attribute value

		// If the attribute data type is "String"
		AttributeInputCompositeComponent priorityComponent = (AttributeInputCompositeComponent) super.getDataValue(
				component_id, datum, mc);
		Object object = priorityComponent;
		String name = "GPLM Problem Report:Review Priority values";
		String url = null;
		QuerySpec qs = new QuerySpec(WTDocumentMaster.class);
		qs.appendWhere(new SearchCondition(WTDocumentMaster.class, WTDocumentMaster.NAME, SearchCondition.EQUAL, name));

		QueryResult qr = PersistenceHelper.manager.find(qs);
		LOGGER.debug("QueryResult Size: " + qr.size());
		if (qr.hasMoreElements()) {
			WTDocumentMaster docm = (WTDocumentMaster) qr.nextElement();
			QueryResult qrLatest = ConfigHelper.service.filteredIterationsOf(docm, new LatestConfigSpec());
			if (qrLatest.hasMoreElements()) {
				WTDocument doc = (WTDocument) qrLatest.nextElement();
				LOGGER.debug("Document Name: " + doc.getName());
				ContentHolder ch;
				try {
					ch = ContentHelper.service.getContents(doc);

					ApplicationData ad = (ApplicationData) ContentHelper.getPrimary((FormatContentHolder) ch);
					url = ContentHelper.getDownloadURL(ch, ad, false).toString();
				} catch (WTException e) {
					LOGGER.debug(e.getMessage());
					e.printStackTrace();
				} catch (PropertyVetoException e) {
					LOGGER.debug(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		UrlDisplayComponent urlComponent = new UrlDisplayComponent();
		urlComponent.setLabelForTheLink("Priority Definition Table");
		urlComponent.setLink(url);
		urlComponent.setTarget("_blank");

		GUIComponentArray array = new GUIComponentArray();
		array.addGUIComponent(priorityComponent);
		array.addGUIComponent(urlComponent);

		LOGGER.debug("##### End getDataValue - PriorityDefinitionTableUtility");
		return array;

	}
}
