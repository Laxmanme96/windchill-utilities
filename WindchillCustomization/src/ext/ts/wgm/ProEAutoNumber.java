package ext.ts.wgm;

import com.ptc.windchill.uwgm.proesrv.c11n.DocIdentifier;
import com.ptc.windchill.uwgm.proesrv.c11n.EPMDocumentNamingDelegate;

import wt.util.WTException;

public final class ProEAutoNumber implements EPMDocumentNamingDelegate {
	String prefix = "CAD";
	String seq = null;
	String objNumber = null;
	String suffix = null;
	public void validateDocumentIdentifier(DocIdentifier docIdentifier) {
		System.out.println("*******ProEAutoNumber validateDocumentIdentifier Started *****");
		docIdentifier.setDocNumber(null);

		System.out.println("<> validateDocumentIdentifier : Model Name is: " + docIdentifier.getModelName());
		System.out.println("<> validateDocumentIdentifier : Doc Name is: " + docIdentifier.getDocName());
		System.out.println("<> validateDocumentIdentifier : Doc Number is: " + docIdentifier.getDocNumber());
		System.out.println("<> validateDocumentIdentifier : Doc Paramaters are: " + docIdentifier.getParameters());

	}

	public ProEAutoNumber() {
		System.out.println("*******setCADDocNumber Started *****");

		try {
			seq = wt.fc.PersistenceHelper.manager.getNextSequence("EPM_seq");
		} catch (WTException e) {
			e.printStackTrace();
		}
		suffix = "A000";
		objNumber = prefix + seq + suffix;

	}

}