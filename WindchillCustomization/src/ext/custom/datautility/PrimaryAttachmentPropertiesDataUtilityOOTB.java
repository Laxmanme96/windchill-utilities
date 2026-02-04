
package ext.custom.datautility;

import java.io.IOException;
import java.util.Locale;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ptc.core.components.descriptor.LogicSeparatedDataUtility;
import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.AbstractDataUtility;
import com.ptc.core.components.rendering.AbstractGuiComponent;
import com.ptc.core.components.rendering.guicomponents.GUIComponentArray;
import com.ptc.core.components.rendering.guicomponents.IconComponent;
import com.ptc.core.components.rendering.guicomponents.Label;
import com.ptc.core.components.rendering.guicomponents.TextDisplayComponent;
import com.ptc.core.components.rendering.guicomponents.UrlDisplayComponent;
import com.ptc.core.meta.common.DataTypesUtility;
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
import wt.content.ExternalStoredData;
import wt.content.FormatContentHolder;
import wt.content.URLData;
import wt.log4j.LogR;
import wt.session.SessionHelper;
import wt.util.WTException;
import wt.util.WTMessage;
import wt.util.WTProperties;

public class PrimaryAttachmentPropertiesDataUtilityOOTB extends AbstractDataUtility
		implements LogicSeparatedDataUtility {
	private static final Logger logger = LogR
			.getLoggerInternal(PrimaryAttachmentPropertiesDataUtilityOOTB.class.getName());

	@Override
	public Object getDataValue(String dataKey, Object contentHolder, ModelContext modelContext) throws WTException {
		logger.trace("In PrimaryAttachmentPropertiesDataUtility");
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
		NmAction viewAction = NmActionServiceHelper.service.getAction("object", "view");
		String viewUrl = viewAction.getUrl();
		String detailsIconPath = "/netmarkets/images/details.gif";
		Locale userLocale = SessionHelper.getLocale();
		GUIComponentArray guiComponents = new GUIComponentArray();
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
					logger.debug("Primary content is ApplicationData");
					if (AttachmentsValidationHelper.hasPermission(formatContentHolder, AccessPermission.DOWNLOAD)) {
						logger.debug("Content holder has download access");
						downloadUrl = serverCodebase + "/servlet/AttachmentsDownloadDirectionServlet?oid=OR:";
						downloadUrl = downloadUrl
								+ formatContentHolder.getPersistInfo().getObjectIdentifier().getStringValue();
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
					tooltip = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(), "TOOL_TIP_DETAILS",
							(Object[]) null, userLocale);
					displayName = AttachmentsDataUtilityHelper.getDisplayName(primaryContentItem);
					if (!"".equals(downloadUrl) && !"OFFICE365".equals(userAgent)) {
						logger.debug("Download URL found");
						UrlDisplayComponent urlDisplayComponent = new UrlDisplayComponent();
						urlDisplayComponent.setCheckXSS(true);
						urlDisplayComponent.setLabelForTheLink(displayName);
						urlDisplayComponent.setLink(downloadUrl);
						urlDisplayComponent.setTarget("ContentFormatIconPopup");
						String fileSize = DataTypesUtility
								.toString(((ApplicationData) primaryContentItem).getFileSizeKB(), userLocale);
						String[] tooltipParams = new String[] { formatName, fileSize };
						String downloadTooltip = this.getTooltip(primaryContentItem);
						urlDisplayComponent.setToolTip(downloadTooltip);
						guiComponents.addGUIComponent(urlDisplayComponent);
					} else {
						logger.debug("Download URL not available or user agent is OFFICE365");
						guiComponents.addGUIComponent(new Label(displayName));
					}

					if (!"DTI".equals(userAgent) && !"OFFICE365".equals(userAgent)) {
						iconComponent = new IconComponent();
						iconComponent.setSrc(serverCodebase + detailsIconPath);
						iconComponent.setUrl(detailsUrl);
						iconComponent.setTooltip(tooltip);
						guiComponents.addGUIComponent(iconComponent);
						contentFormatIcon = AttachmentsDataUtilityHelper.getContentFormatIcon(primaryContentItem,
								userLocale);
						guiComponents.addGUIComponent(contentFormatIcon);
					}
				} else {
					detailsUrl = serverCodebase + viewUrl;
					detailsUrl = detailsUrl + "?oid=OR:";
					detailsUrl = detailsUrl + this.getOidString(primaryContentItem);
					tooltip = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(), "TOOL_TIP_DETAILS",
							(Object[]) null, userLocale);
					displayName = AttachmentsDataUtilityHelper.getDisplayName(primaryContentItem);
					if (primaryContentItem instanceof URLData) {
						logger.debug("Primary content is URLData");
						URLData urlData = (URLData) primaryContentItem;
						UrlDisplayComponent urlDisplayComponent = new UrlDisplayComponent();
						urlDisplayComponent.setCheckXSS(true);
						urlDisplayComponent.setLabelForTheLink(displayName);
						urlDisplayComponent.setLink(urlData.getUrlLocation());
						String openUrlTooltip = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(),
								"TOOL_TIP_OPEN_URL", (Object[]) null, userLocale);
						urlDisplayComponent.setToolTip(openUrlTooltip);
						guiComponents.addGUIComponent(urlDisplayComponent);
					} else {
						logger.debug("Render the displayName");
						guiComponents.addGUIComponent(new Label(displayName));
					}

					iconComponent = new IconComponent();
					iconComponent.setSrc(serverCodebase + detailsIconPath);
					iconComponent.setUrl(detailsUrl);
					iconComponent.setTooltip(tooltip);
					guiComponents.addGUIComponent(iconComponent);
					contentFormatIcon = AttachmentsDataUtilityHelper.getContentFormatIcon(primaryContentItem,
							userLocale);
					guiComponents.addGUIComponent(contentFormatIcon);
				}
			} else {
				String noContentText = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(),
						"NO_CONTENT_TEXT", (Object[]) null, userLocale);
				TextDisplayComponent textDisplayComponent = new TextDisplayComponent((String) null, true);
				textDisplayComponent.setValue(noContentText);
				guiComponents.addGUIComponent(textDisplayComponent);
			}
		}

		return guiComponents;
	}

	protected ContentItem getPrimaryContentItem(ContentHolder contentHolder) throws WTException {
		return AttachmentsValidationHelper.getPrimaryContentItem(contentHolder);
	}

	protected String getOidString(ContentItem contentItem) {
		return contentItem.getPersistInfo().getObjectIdentifier().getStringValue();
	}

	@Override
	public Object getPlainDataValue(String dataKey, Object contentHolder, ModelContext modelContext)
			throws WTException {
		WTProperties serverProperties = null;

		try {
			serverProperties = WTProperties.getServerProperties();
		} catch (IOException e) {
			throw new WTException(e);
		}

		String serverCodebase = serverProperties.getProperty("wt.server.codebase");
		NmAction viewAction = NmActionServiceHelper.service.getAction("object", "view");
		String viewUrl = viewAction.getUrl();
		String detailsIconPath = "/netmarkets/images/details.gif";
		Locale userLocale = SessionHelper.getLocale();
		boolean isExternalStoredData = false;
		JSONArray jsonArray = new JSONArray();
		if (contentHolder instanceof FormatContentHolder) {
			FormatContentHolder formatContentHolder = (FormatContentHolder) contentHolder;
			ContentItem primaryContentItem = this.getPrimaryContentItem(formatContentHolder);
			String tooltipDetails = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(),
					"TOOL_TIP_DETAILS", (Object[]) null, userLocale);
			String detailsUrl;
			JSONObject jsonObject;
			if (primaryContentItem != null) {
				detailsUrl = serverCodebase + viewUrl;

				detailsUrl = detailsUrl + "?oid=OR:";
				detailsUrl = detailsUrl + this.getOidString(primaryContentItem);
				jsonObject = new JSONObject();
				String displayName = AttachmentsDataUtilityHelper.getDisplayName(primaryContentItem);
				String downloadUrl = "";
				String tooltip = "";
				String contentType = "url";
				if (primaryContentItem instanceof ApplicationData) {
					logger.debug("Primary content is ApplicationData");
					detailsUrl = detailsUrl + "&chOid=";
					detailsUrl = detailsUrl + formatContentHolder.toString();
					downloadUrl = detailsUrl;
					if (AttachmentsValidationHelper.hasPermission(formatContentHolder, AccessPermission.DOWNLOAD)) {
						downloadUrl = serverCodebase + "/servlet/AttachmentsDownloadDirectionServlet?oid=OR:";
						downloadUrl = downloadUrl
								+ formatContentHolder.getPersistInfo().getObjectIdentifier().getStringValue();
						downloadUrl = downloadUrl + "&oid=OR:";
						downloadUrl = downloadUrl + this.getOidString(primaryContentItem);
						downloadUrl = downloadUrl + "&role=";
						downloadUrl = downloadUrl + primaryContentItem.getRole();
					}

					tooltip = this.getTooltip(primaryContentItem);
				} else if (primaryContentItem instanceof URLData) {
					logger.debug("Primary content is URLData");
					URLData urlData = (URLData) primaryContentItem;
					downloadUrl = urlData.getUrlLocation();
					tooltip = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(), "TOOL_TIP_OPEN_URL",
							(Object[]) null, userLocale);
				} else if (primaryContentItem instanceof ExternalStoredData) {
					logger.debug("Primary content is ExternalStoredData");
					ExternalStoredData externalStoredData = (ExternalStoredData) primaryContentItem;
					tooltip = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(), "TOOL_TIP_OPEN_URL",
							(Object[]) null, userLocale);
					contentType = displayName;
					isExternalStoredData = true;
				}

				try {
					if (!isExternalStoredData) {
						jsonObject.put("type", contentType);
						jsonObject.put("label", displayName);
						jsonObject.put("link", downloadUrl);
						jsonObject.put("target", "ContentDownloadPopup");
						jsonObject.put("tooltip", tooltip);
					} else {
						jsonObject.put("type", "text");
						jsonObject.put("text", displayName);
						jsonObject.put("tooltip", tooltip);
					}
				} catch (Exception e) {
					throw new WTException(e);
				}

				jsonArray.put(jsonObject);
				JSONObject iconJsonObject = new JSONObject();

				try {
					iconJsonObject.put("type", "icon");
					iconJsonObject.put("iconPath", serverCodebase + detailsIconPath);
					iconJsonObject.put("link", detailsUrl);
					iconJsonObject.put("tooltip", tooltipDetails);
				} catch (Exception e) {
					throw new WTException(e);
				}

				jsonArray.put(iconJsonObject);
				AbstractGuiComponent contentFormatIcon = AttachmentsDataUtilityHelper
						.getContentFormatIcon(primaryContentItem, userLocale);
				JSONObject contentFormatIconJson = new JSONObject();

				try {
					if (!(contentFormatIcon instanceof UrlDisplayComponent)) {
						if (contentFormatIcon instanceof IconComponent) {
							IconComponent iconComponent = (IconComponent) contentFormatIcon;
							contentFormatIconJson.put("type", "icon");
							contentFormatIconJson.put("iconPath", iconComponent.getSrc());
							contentFormatIconJson.put("link", iconComponent.getUrl());
							contentFormatIconJson.put("tooltip", iconComponent.getTooltip());
						}
					} else {
						UrlDisplayComponent urlDisplayComponent = (UrlDisplayComponent) contentFormatIcon;
						String label = urlDisplayComponent.getLabel();
						String image = urlDisplayComponent.getImage();
						if (label == null && image != null) {
							contentFormatIconJson.put("type", "icon");
							contentFormatIconJson.put("iconPath", image);
						} else if (label != null && image == null) {
							contentFormatIconJson.put("type", "url");
							contentFormatIconJson.put("label", label);
						}

						contentFormatIconJson.put("link", urlDisplayComponent.getLink());
						contentFormatIconJson.put("tooltip", urlDisplayComponent.getToolTip());
						String target = urlDisplayComponent.getTarget();
						if (target != null) {
							contentFormatIconJson.put("target", target);
						}
					}
				} catch (Exception e) {
					throw new WTException(e);
				}

				jsonArray.put(contentFormatIconJson);
			} else {
				String noContentText = WTMessage.getLocalizedMessage(attachmentsResource.class.getName(),
						"NO_CONTENT_TEXT", (Object[]) null, userLocale);
				jsonObject = new JSONObject();

				try {
					jsonObject.put("type", "text");
					jsonObject.put("text", noContentText);
				} catch (Exception e) {
					throw new WTException(e);
				}

				jsonArray.put(jsonObject);
			}
		}

		return jsonArray.toJSONString();
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
