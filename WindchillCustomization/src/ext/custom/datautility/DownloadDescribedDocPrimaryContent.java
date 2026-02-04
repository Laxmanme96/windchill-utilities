package ext.custom.datautility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.AbstractGuiComponent;
import com.ptc.core.components.rendering.guicomponents.GUIComponentArray;
import com.ptc.core.components.rendering.guicomponents.IconComponent;
import com.ptc.core.components.rendering.guicomponents.Label;
import com.ptc.core.components.rendering.guicomponents.TextDisplayComponent;
import com.ptc.core.components.rendering.guicomponents.UrlDisplayComponent;
import com.ptc.core.meta.common.DataTypesUtility;
import com.ptc.core.ui.resources.ComponentMode;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.netmarkets.util.misc.NmAction;
import com.ptc.netmarkets.util.misc.NmActionServiceHelper;
import com.ptc.windchill.enterprise.attachments.attachmentsResource;
import com.ptc.windchill.enterprise.attachments.dataUtilities.AttachmentsDataUtilityHelper;
import com.ptc.windchill.enterprise.attachments.validators.AttachmentsValidationHelper;

import wt.access.AccessPermission;
import wt.content.ApplicationData;
import wt.content.ContentHolder;
import wt.content.ContentItem;
import wt.content.DataFormat;
import wt.content.DataFormatReference;
import wt.content.FormatContentHolder;
import wt.content.URLData;
import wt.doc.WTDocument;
import wt.fc.QueryResult;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.util.WTMessage;
import wt.util.WTProperties;

public class DownloadDescribedDocPrimaryContent extends DefaultDataUtility {

	@Override
	public Object getDataValue(String str, Object paramObject, ModelContext modelContext) throws WTException {
		System.out.println("DownloadDescribeDocPrimaryContent STARTED");
		ComponentMode mode = modelContext.getDescriptorMode();
		GUIComponentArray guiComponents = new GUIComponentArray();
		Object object = super.getDataValue(str, paramObject, modelContext);

		NmCommandBean commandBean = null;
		commandBean = modelContext.getNmCommandBean();
		String userAgent = "";
		if (commandBean != null) {
			userAgent = commandBean.getTextParameter("ua");
		}

		WTProperties serverProperties = null;

		try {
			serverProperties = WTProperties.getServerProperties();
		} catch (IOException e) {
			throw new WTException(e);
		}

		String serverCodebase = serverProperties.getProperty("wt.server.codebase");
		Locale userLocale = SessionHelper.getLocale();

		NmAction viewAction = NmActionServiceHelper.service.getAction("object", "view");
		String viewUrl = viewAction.getUrl();
		String detailsIconPath = "/netmarkets/images/details.gif";

		if (mode.equals(ComponentMode.VIEW)) {

			if (paramObject instanceof WTPart) {
				WTPart part = (WTPart) paramObject;
				ArrayList<WTDocument> describedDocs = getDescribedByDocs(part);
				for (WTDocument doc : describedDocs) {
					Object contentHolder = doc;

					if (contentHolder instanceof FormatContentHolder) {
						FormatContentHolder formatContentHolder = (FormatContentHolder) contentHolder;
						ContentItem primaryContentItem = this.getPrimaryContentItem(formatContentHolder);
						if (primaryContentItem != null) {
							DataFormat dataFormat = null;
							DataFormatReference dataFormatReference = primaryContentItem.getFormat();
							if (dataFormatReference != null) {
								dataFormat = dataFormatReference.getDataFormat();
							}

							String formatName = "";
							if (dataFormat != null) {
								formatName = dataFormat.getFormatName();
								formatName = DataFormat.getLocalizedFormatName(formatName, userLocale);
							}

							String downloadUrl = "";
							String detailsUrl;
							String tooltip;
							String displayName;
							IconComponent iconComponent;
							AbstractGuiComponent contentFormatIcon;
							if (primaryContentItem instanceof ApplicationData) {
								System.out.println("Primary content is ApplicationData");
								if (AttachmentsValidationHelper.hasPermission(formatContentHolder,
										AccessPermission.DOWNLOAD)) {
									System.out.println("Content holder has download access");
									downloadUrl = serverCodebase
											+ "/servlet/AttachmentsDownloadDirectionServlet?oid=OR:";
									downloadUrl = downloadUrl + formatContentHolder.getPersistInfo()
											.getObjectIdentifier().getStringValue();
									downloadUrl = downloadUrl + "&oid=OR:";
									downloadUrl = downloadUrl + this.getOidString(primaryContentItem);
									downloadUrl = downloadUrl + "&role=";
									downloadUrl = downloadUrl + primaryContentItem.getRole();
								}

								detailsUrl = serverCodebase + viewUrl;
								detailsUrl = detailsUrl + "?oid=OR:";
								detailsUrl = detailsUrl + this.getOidString(primaryContentItem);
								detailsUrl = detailsUrl + "&chOid=";
								detailsUrl = detailsUrl + formatContentHolder.toString();
								tooltip = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(),
										"TOOL_TIP_DETAILS", (Object[]) null, userLocale);
								displayName = doc.getNumber().toString();
								if (!"".equals(downloadUrl) && !"OFFICE365".equals(userAgent)) {
									System.out.println("Download URL found");
									UrlDisplayComponent urlDisplayComponent = new UrlDisplayComponent();
									urlDisplayComponent.setCheckXSS(true);
									urlDisplayComponent.setLabelForTheLink(displayName);
									urlDisplayComponent.setLink(downloadUrl);
									urlDisplayComponent.setTarget("ContentFormatIconPopup");
									String fileSize = DataTypesUtility.toString(
											((ApplicationData) primaryContentItem).getFileSizeKB(), userLocale);
									String[] tooltipParams = new String[] { formatName, fileSize };
									String downloadTooltip = this.getTooltip(primaryContentItem);
									urlDisplayComponent.setToolTip(downloadTooltip);
									guiComponents.addGUIComponent(urlDisplayComponent);
								} else {
									System.out.println("Download URL not available or user agent is OFFICE365");
									guiComponents.addGUIComponent(new Label(displayName));
								}

								if (!"DTI".equals(userAgent) && !"OFFICE365".equals(userAgent)) {
									iconComponent = new IconComponent();
									iconComponent.setSrc(serverCodebase + detailsIconPath);
									iconComponent.setUrl(detailsUrl);
									iconComponent.setTooltip(tooltip);
									guiComponents.addGUIComponent(iconComponent);
									contentFormatIcon = AttachmentsDataUtilityHelper
											.getContentFormatIcon(primaryContentItem, userLocale);
									guiComponents.addGUIComponent(contentFormatIcon);
								}
							} else {
								detailsUrl = serverCodebase + viewUrl;
								detailsUrl = detailsUrl + "?oid=OR:";
								detailsUrl = detailsUrl + this.getOidString(primaryContentItem);
								tooltip = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(),
										"TOOL_TIP_DETAILS", (Object[]) null, userLocale);
								displayName = AttachmentsDataUtilityHelper.getDisplayName(primaryContentItem);
								if (primaryContentItem instanceof URLData) {
									System.out.println("Primary content is URLData");
									URLData urlData = (URLData) primaryContentItem;
									UrlDisplayComponent urlDisplayComponent = new UrlDisplayComponent();
									urlDisplayComponent.setCheckXSS(true);
									urlDisplayComponent.setLabelForTheLink(displayName);
									urlDisplayComponent.setLink(urlData.getUrlLocation());
									String openUrlTooltip = WTMessage.getLocalizedMessage(
											attachmentsResource.class.getName(), "TOOL_TIP_OPEN_URL", (Object[]) null,
											userLocale);
									urlDisplayComponent.setToolTip(openUrlTooltip);
									guiComponents.addGUIComponent(urlDisplayComponent);
								} else {
									System.out.println("Render the displayName");
									guiComponents.addGUIComponent(new Label(displayName));
								}

								iconComponent = new IconComponent();
								iconComponent.setSrc(serverCodebase + detailsIconPath);
								iconComponent.setUrl(detailsUrl);
								iconComponent.setTooltip(tooltip);
								guiComponents.addGUIComponent(iconComponent);
								contentFormatIcon = AttachmentsDataUtilityHelper
										.getContentFormatIcon(primaryContentItem, userLocale);
								guiComponents.addGUIComponent(contentFormatIcon);
							}
							Label lineBreak = new Label("\n");
							guiComponents.addGUIComponent(lineBreak);

						} else {
							System.out.println("Render NO_CONTENT_TEXT ");
							String displayName = doc.getNumber().toString();
							String noContentText = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(),
									"NO_CONTENT_TEXT", (Object[]) null, userLocale);
							TextDisplayComponent textDisplayComponent = new TextDisplayComponent((String) null, true);
							textDisplayComponent.setValue(displayName + " : " + noContentText);
							guiComponents.addGUIComponent(textDisplayComponent);
							Label lineBreak = new Label("\n");
							guiComponents.addGUIComponent(lineBreak);
						}
					}
				}
			}
		}
		return guiComponents;
	}

	public ArrayList<WTDocument> getDescribedByDocs(WTPart part) throws WTException {
		System.out.println("-------> getdescribedByDoc Started :");

		ArrayList<WTDocument> describedByDocs = new ArrayList<>();
		System.out.println("allEmptyRequiredAttributes Started ");

		QueryResult qr = WTPartHelper.service.getDescribedByWTDocuments(part);
		// QueryResult qr =
		// PersistenceHelper.manager.navigate(lastestPart,WTPartDescribeLink.DESCRIBED_BY_ROLE,
		// wt.part.WTPartDescribeLink.class,false);

		while (qr.hasMoreElements()) {
			// WTPartDescribeLink link = (WTPartDescribeLink) qr.nextElement();
			Object obj = qr.nextElement();
			System.out.println("-------> Obj Class : " + obj.getClass());
			WTDocument describedByDoc = (WTDocument) obj;
			System.out.println("-------> Described Document : " + describedByDoc.getNumber());
			describedByDocs.add(describedByDoc);
		}
		return describedByDocs;
	}

	protected ContentItem getPrimaryContentItem(ContentHolder contentHolder) throws WTException {
		return AttachmentsValidationHelper.getPrimaryContentItem(contentHolder);
	}

	protected String getOidString(ContentItem contentItem) {
		return contentItem.getPersistInfo().getObjectIdentifier().getStringValue();
	}

	public String getTooltip(ContentItem contentItem) throws WTException {
		Locale userLocale = SessionHelper.getLocale();
		DataFormat dataFormat = null;
		DataFormatReference dataFormatReference = contentItem.getFormat();
		if (dataFormatReference != null) {
			dataFormat = dataFormatReference.getDataFormat();
		}

		String formatName = "";
		if (dataFormat != null) {
			formatName = dataFormat.getFormatName();
			formatName = DataFormat.getLocalizedFormatName(formatName, userLocale);
		}

		String fileSize = DataTypesUtility.toString(((ApplicationData) contentItem).getFileSizeKB(), userLocale);
		String[] tooltipParams = new String[] { formatName, fileSize };
		String tooltip = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(), "TOOL_TIP_DOWNLOAD",
				tooltipParams, userLocale);
		return tooltip;
	}

}
