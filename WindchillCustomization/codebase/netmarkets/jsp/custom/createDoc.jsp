<%@ include file="/netmarkets/jsp/components/standardAttributeConfigs.jspf"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/components" prefix="jca"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/wrappers" prefix="w"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/carambola" prefix="cmb"%>
<%@ include file="/netmarkets/jsp/components/beginWizard.jspf"%>


<jca:wizard buttonList="DefaultWizardButtons" title="Custom Create Document" >
	<jca:wizardStep action="setDocNameWizardStep" label="Set Document Name" type="doc" />
	<jca:wizardStep action="setDocNumberWizardStep" label="Set Document Number" type="doc" />
</jca:wizard>

<%@ include file="/netmarkets/jsp/util/end.jspf"%>