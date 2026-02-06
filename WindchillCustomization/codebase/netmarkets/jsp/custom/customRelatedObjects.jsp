<%@ include file="/netmarkets/jsp/components/standardAttributeConfigs.jspf"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/components" prefix="jca"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/wrappers" prefix="w"%>
<%@ taglib uri="http://www.ptc.com/windchill/taglib/carambola" prefix="cmb"%>
<%@ include file="/netmarkets/jsp/components/beginWizard.jspf"%>


<jca:wizard buttonList="DefaultWizardButtons" title="Custom Related Objects" >
	<jca:wizardStep action="customRelatedObjectsTable" label="Custom Related Object Table " type="part" />
	<jca:wizardStep action="customRelatedObjectsTree" label="Custom Related Object Tree " type="part" />
</jca:wizard>

<%@ include file="/netmarkets/jsp/util/end.jspf"%>