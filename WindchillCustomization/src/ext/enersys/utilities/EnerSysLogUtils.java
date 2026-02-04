package ext.enersys.utilities;

import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.windchill.uwgm.common.pdm.retriever.RevisionIterationInfoHelper;

import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeIssue;
import wt.change2.WTChangeOrder2;
import wt.change2.WTChangeRequest2;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.epm.EPMDocument;
import wt.epm.EPMDocumentMaster;
import wt.epm.build.EPMBuildRule;
import wt.fc.Persistable;
import wt.maturity.PromotionNotice;
import wt.org.WTUser;
import wt.part.WTPart;
import wt.part.WTPartMaster;
import wt.part.WTPartUsageLink;

public class EnerSysLogUtils {
	private static char DELIM = '\t';

	public static String format(Persistable per) {
		StringBuilder sb = new StringBuilder();
		if (per != null) {
			if (per instanceof WTPartUsageLink) {
				WTPartUsageLink usageLink = (WTPartUsageLink) per;
				sb.append("WTPartUsageLink : ROLE A -"+usageLink.getRoleAObject()).append(" - ").append("ROLE B-"+usageLink.getRoleBObject()).append(DELIM).append(TypeIdentifierHelper.getType(usageLink));
			} else if (per instanceof EPMDocument) {
				EPMDocument epm = (EPMDocument) per;
				sb.append(epm.getName()).append('/').append(epm.getNumber()).append(DELIM).append(RevisionIterationInfoHelper.displayInfo(epm)).append(DELIM)
						.append(epm.getLifeCycleState()).append(DELIM).append("CAD-NAME:").append(epm.getCADName()).append(DELIM).append(TypeIdentifierHelper.getType(epm));
			} else if (per instanceof WTPart) {
				WTPart prt = (WTPart) per;
				sb.append(prt.getName()).append('/').append(prt.getNumber()).append(DELIM).append(RevisionIterationInfoHelper.displayInfo(prt)).append(DELIM)
						.append(prt.getLifeCycleState()).append(DELIM).append(TypeIdentifierHelper.getType(prt));
			} else if (per instanceof WTDocument) {
				WTDocument doc = (WTDocument) per;
				sb.append(doc.getName()).append('/').append(doc.getNumber()).append(DELIM).append(RevisionIterationInfoHelper.displayInfo(doc)).append(DELIM)
						.append(doc.getLifeCycleState()).append(DELIM).append(TypeIdentifierHelper.getType(doc));
			} else if (per instanceof WTUser) {
				WTUser usr = (WTUser) per;
				sb.append(usr.getName()).append('/').append(usr.getFullName()).append(DELIM).append(usr.getAuthenticationName()).append(DELIM)
						.append(TypeIdentifierHelper.getType(usr));
			} else if (per instanceof PromotionNotice) {
				PromotionNotice pn = (PromotionNotice) per;
				sb.append(pn.getName()).append('/').append(pn.getNumber()).append(DELIM).append("MATURITY-STATE:").append(pn.getMaturityState()).append(DELIM).append("CREATOR:")
						.append(pn.getCreator()).append(DELIM).append(pn.getLifeCycleState()).append(DELIM).append(TypeIdentifierHelper.getType(pn));
			} else if (per instanceof WTChangeIssue) {
				WTChangeIssue ci = (WTChangeIssue) per;
				sb.append(ci.getName()).append('/').append(ci.getNumber()).append(DELIM).append(RevisionIterationInfoHelper.displayInfo(ci)).append(DELIM)
						.append(ci.getLifeCycleState()).append(DELIM).append(TypeIdentifierHelper.getType(ci));
			} else if (per instanceof WTChangeRequest2) {
				WTChangeRequest2 cr = (WTChangeRequest2) per;
				sb.append(cr.getName()).append('/').append(cr.getNumber()).append(DELIM).append(RevisionIterationInfoHelper.displayInfo(cr)).append(DELIM)
						.append(cr.getLifeCycleState()).append(DELIM).append(TypeIdentifierHelper.getType(cr));
			} else if (per instanceof WTChangeOrder2) {
				WTChangeOrder2 co = (WTChangeOrder2) per;
				sb.append(co.getName()).append('/').append(co.getNumber()).append(DELIM).append(RevisionIterationInfoHelper.displayInfo(co)).append(DELIM)
						.append(co.getLifeCycleState()).append(DELIM).append(TypeIdentifierHelper.getType(co));
			} else if (per instanceof WTChangeActivity2) {
				WTChangeActivity2 ca = (WTChangeActivity2) per;
				sb.append(ca.getName()).append('/').append(ca.getNumber()).append(DELIM).append(RevisionIterationInfoHelper.displayInfo(ca)).append(DELIM)
						.append(ca.getLifeCycleState()).append(DELIM).append(TypeIdentifierHelper.getType(ca));
			} else if (per instanceof WTPartMaster) {
				WTPartMaster prtM = (WTPartMaster) per;
				sb.append(prtM.getName()).append('/').append(prtM.getNumber()).append(DELIM).append("-").append(DELIM).append("-").append(DELIM)
						.append(TypeIdentifierHelper.getType(prtM));
			} else if (per instanceof EPMDocumentMaster) {
				EPMDocumentMaster epmM = (EPMDocumentMaster) per;
				sb.append(epmM.getName()).append('/').append(epmM.getNumber()).append(DELIM).append("-").append(DELIM).append("-").append(DELIM)
						.append(TypeIdentifierHelper.getType(epmM));
			} else if (per instanceof WTDocumentMaster) {
				WTDocumentMaster docM = (WTDocumentMaster) per;
				sb.append(docM.getName()).append('/').append(docM.getNumber()).append(DELIM).append("-").append(DELIM).append("-").append(DELIM)
						.append(TypeIdentifierHelper.getType(docM));
			} else if (per instanceof EPMBuildRule) {
				EPMBuildRule epmBR = (EPMBuildRule) per;
				EPMDocument ownerEPMObj = (EPMDocument) epmBR.getRoleAObject();
				WTPart partObj = (WTPart) epmBR.getRoleBObject();

				sb.append("ROLE-A (EPM):").append(ownerEPMObj.getName()).append('/').append(ownerEPMObj.getNumber()).append(DELIM).append("ROLE-B (WTPART):")
						.append(partObj.getName()).append('/').append(partObj.getNumber()).append(DELIM).append("EPM BR (BuildType):").append(epmBR.getBuildType()).append(DELIM)
						.append(TypeIdentifierHelper.getType(epmBR));
			} else {
				sb.append("UNKNOWN PERSISTABLE : " + TypeIdentifierHelper.getType(per));
			}
		}
		return sb.toString();

	}
}
