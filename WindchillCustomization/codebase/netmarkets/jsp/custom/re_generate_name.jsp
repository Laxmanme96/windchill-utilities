<%@ page import="com.ptc.netmarkets.model.NmOid" %>
<%@ page import="com.ptc.netmarkets.util.beans.NmCommandBean" %>
<%@ page import="wt.part.WTPart" %>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/mvc" prefix="mvc" %>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/components" prefix="jca" %>
<%@ page import="java.util.Locale" %>
<%@ page import="ext.ptpl.customAction.CustomDesign"%>
<%@ page import="java.util.Locale" %>
<%@ include file="/netmarkets/jsp/components/beginWizard.jspf"%>

<%
	
	NmCommandBean cb = new NmCommandBean();
    cb.setCompContext(nmcontext.getContext().toString());
    cb.setRequest(request);
	
	NmOid oid = cb.getActionOid();
	NmOid pageOid = cb.getPageOid();

	
	Object pageRefObject = (Object) pageOid.getRefObject();
    Object refObject = (Object)oid.getRefObject();
	
	//System.out.println("Type oid: "+refObject.toString());
	   if (refObject instanceof WTPart) {
        WTPart part = (WTPart) refObject;
				CustomDesign.regenerateName((WTPart)refObject);
	   }
%>

<script> 
window.close();
window.opener.location.close();

</script>

<%@ include file="/netmarkets/jsp/util/end.jspf"%>