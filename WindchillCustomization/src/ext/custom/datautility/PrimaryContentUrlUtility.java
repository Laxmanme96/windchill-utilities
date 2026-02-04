package ext.custom.datautility;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.LogicSeparatedDataUtility;
import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.AbstractDataUtility;
import com.ptc.netmarkets.util.misc.NmAction;
import com.ptc.netmarkets.util.misc.NmActionServiceHelper;
import com.ptc.windchill.enterprise.attachments.validators.AttachmentsValidationHelper;

import wt.access.AccessPermission;
import wt.content.ApplicationData;
import wt.content.ContentHolder;
import wt.content.ContentItem;
import wt.content.FormatContentHolder;
import wt.content.URLData;
import wt.log4j.LogR;
import wt.util.WTException;
import wt.util.WTProperties;

public class PrimaryContentUrlUtility extends AbstractDataUtility implements LogicSeparatedDataUtility {
	private static final Logger logger = LogR.getLoggerInternal(PrimaryContentUrlUtility.class.getName());

	public String getPrimaryContentUrl(ContentHolder contentHolder) throws WTException {
		if (!(contentHolder instanceof FormatContentHolder)) {
			return null;
		}

		FormatContentHolder formatContentHolder = (FormatContentHolder) contentHolder;
		ContentItem primaryContentItem = AttachmentsValidationHelper.getPrimaryContentItem(formatContentHolder);
		if (primaryContentItem == null) {
			return null;
		}

		WTProperties serverProperties;
		try {
			serverProperties = WTProperties.getServerProperties();
		} catch (IOException e) {
			throw new WTException(e);
		}

		String serverCodebase = serverProperties.getProperty("wt.server.codebase");
		NmAction viewAction = NmActionServiceHelper.service.getAction("object", "view");
		String viewUrl = viewAction.getUrl();
		String primaryContentUrl = "";

		if (primaryContentItem instanceof ApplicationData) {
			logger.debug("Primary content is ApplicationData");
			if (AttachmentsValidationHelper.hasPermission(formatContentHolder, AccessPermission.DOWNLOAD)) {
				primaryContentUrl = serverCodebase + "/servlet/AttachmentsDownloadDirectionServlet?oid=OR:";
				primaryContentUrl += formatContentHolder.getPersistInfo().getObjectIdentifier().getStringValue();
				primaryContentUrl += "&oid=OR:";
				primaryContentUrl += primaryContentItem.getPersistInfo().getObjectIdentifier().getStringValue();
				primaryContentUrl += "&role=";
				primaryContentUrl += primaryContentItem.getRole();
			}
		} else if (primaryContentItem instanceof URLData) {
			logger.debug("Primary content is URLData");
			URLData urlData = (URLData) primaryContentItem;
			primaryContentUrl = urlData.getUrlLocation();
		}

		return primaryContentUrl;
	}

	@Override
	public Object getDataValue(String dataKey, Object contentHolder, ModelContext modelContext) throws WTException {
		return getPrimaryContentUrl((ContentHolder) contentHolder);
	}

	@Override
	public Object getPlainDataValue(String dataKey, Object contentHolder, ModelContext modelContext)
			throws WTException {
		return getPrimaryContentUrl((ContentHolder) contentHolder);
	}
}
