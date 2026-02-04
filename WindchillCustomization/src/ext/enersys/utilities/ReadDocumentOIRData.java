package ext.enersys.utilities;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ptc.core.meta.server.TypeIdentifierUtility;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import ext.enersys.listeners.RequiredFieldsValidationListenerHandler;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.fc.WTReference;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerRef;
import wt.log4j.LogR;
import wt.rule.RuleHelper;
import wt.rule.TypeBasedRule;
import wt.util.WTException;
import wt.util.WTProperties;

/**
 * JIRA-331, Simple-Java class with static methods. Used by custom JSP to calculate security label visibility.
 * 
 * @author CGI Team
 * @since Build v2.1
 *
 */
public class ReadDocumentOIRData {

	private static final String CLASSNAME = ReadDocumentOIRData.class.getName();
	private static final Logger LOGGER = LogR.getLoggerInternal(CLASSNAME);
	private static final String SECURITY_LABEL_CONFIG_FILENAME = "securityLabelsConfiguration.xml";
	private static HashSet<String> securityLabelInternalName = null;

	private ReadDocumentOIRData() {

	}

	static {
		loadSecurityLabelInternalNames();
	}

	private static void loadSecurityLabelInternalNames() {
		if (securityLabelInternalName == null) {
			securityLabelInternalName = new HashSet<>();
		}
		securityLabelInternalName.clear();
		try {
			WTProperties wtproperties = null;
			wtproperties = WTProperties.getLocalProperties();
			String windchillHome = wtproperties.getProperty("wt.home", "");
			String securityFilePath = windchillHome + File.separator + "conf" + File.separator + SECURITY_LABEL_CONFIG_FILENAME;
			File xmlFile = new File(securityFilePath);

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("SecurityLabel");
			for (int i = 0; i < nList.getLength(); ++i) {
				Element f = (Element) nList.item(i);
				if (f.getAttribute("enabled").equalsIgnoreCase("true")) {
					String securityLabelName = f.getAttribute("name");
					securityLabelInternalName.add(securityLabelName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean getsecurityLabelvalues(NmCommandBean bean) throws WTException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		LOGGER.debug(CLASSNAME + ":getsecurityLabelvalues():START");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		String objTypeSelected = (String) ((Object[]) bean.getParameterMap().get("value"))[0];
		String containerOidStr = (String) ((Object[]) bean.getParameterMap().get("ContainerOid"))[0];
		LOGGER.debug("objTypeSelected:"+objTypeSelected);
		LOGGER.debug("containerOidStr:"+containerOidStr);

		ReferenceFactory rf = new ReferenceFactory();
		WTReference refObj = rf.getReference(containerOidStr);
		WTContainerRef contRefObj = WTContainerRef.newWTContainerRef((WTContainer) refObj.getObject());

		QueryResult qr = RuleHelper.service.findAllRules(contRefObj);

		while (qr.hasMoreElements()) {
			TypeBasedRule obj = (TypeBasedRule) qr.nextElement();
			if (obj.getContainerName().equals(contRefObj.getName()) && obj.getEnabledFlag() == 0
					&& (TypeIdentifierUtility.getTypeIdentifier(obj.getObjType()).toExternalForm().equalsIgnoreCase("WCTYPE|" + objTypeSelected))) {
				for (String securityLabelName : securityLabelInternalName) {
					XPathExpression xpathExpression = xpath.compile("/AttributeValues/AttrValue[@id='" + securityLabelName + "']/Arg");

					Document oirDoc = dBuilder.parse(obj.getSpecification().getInputStream(obj.getContents()));
					oirDoc.getDocumentElement().normalize();

					NodeList appMatElems = (NodeList) xpathExpression.evaluate(oirDoc, XPathConstants.NODESET);
					if (appMatElems.getLength() > 0) {
						return true;
					}
				}
				return false;
			}
		}
		return false;
	}
}