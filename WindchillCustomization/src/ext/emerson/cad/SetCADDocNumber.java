package ext.emerson.cad;

import org.apache.logging.log4j.Logger;

import com.ptc.windchill.uwgm.proesrv.c11n.DocIdentifier;
import com.ptc.windchill.uwgm.proesrv.c11n.EPMDocumentNamingDelegate;

import wt.log4j.LogR;

public final class SetCADDocNumber implements EPMDocumentNamingDelegate {
	protected static final Logger LOGGER = LogR.getLogger(SetCADDocNumber.class.getName());
	String prefixCAD = "CAD";
	String prefixNCC = "NCC";
	String nccNumber = null;
	String cadNumber = null;

	public void validateDocumentIdentifier(DocIdentifier docIdentifier) {
		LOGGER.debug("*******validateDocumentIdentifier Started *****");
		String objectNumber = docIdentifier.getDocNumber(); // Get the object number
		LOGGER.debug("Raw Object Number: " + objectNumber);
		LOGGER.debug("<> Model Name: " + docIdentifier.getModelName());

		// Define the regex pattern for 6 digits followed by 'A' and "000"
		// (?:\\.\\w+)? ignores file extension
		String pattern = "\\d{6}[Aa]000(?:\\.\\w+)?";
		// Logger.debug("Regex Pattern: " + pattern);

		// Validate objectNumber against the pattern
		if (objectNumber != null && objectNumber.matches(pattern)) {
			LOGGER.debug("Object Number matches the pattern!");

			if (docIdentifier.getModelName().contains(".drw")) {
				cadNumber = prefixCAD + objectNumber;
				LOGGER.debug("CAD Number is: " + cadNumber);
				docIdentifier.setDocNumber(cadNumber);
				docIdentifier.setDocName(cadNumber);
			} else {
				nccNumber = prefixNCC + objectNumber;
				LOGGER.debug("NCC Number is: " + nccNumber);
				docIdentifier.setDocNumber(nccNumber);
				docIdentifier.setDocName(nccNumber);
			}
		} else {
			LOGGER.debug("Object Number does NOT match the pattern!");
		}

		LOGGER.debug("*******validateDocumentIdentifier Ended *****");
	}
}
