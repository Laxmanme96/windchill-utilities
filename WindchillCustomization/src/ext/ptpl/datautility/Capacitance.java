package ext.ptpl.datautility;

import org.apache.logging.log4j.Logger;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.ComboBox;
import com.ptc.core.components.rendering.guicomponents.TextBox;
import com.ptc.core.ui.resources.ComponentMode;

import wt.inf.container.WTContainer;
import wt.log4j.LogR;
import wt.util.WTException;

import java.util.ArrayList;

public class Capacitance extends DefaultDataUtility {

	protected static final Logger logger = LogR.getLogger(Capacitance.class.getName());

	@Override
	public TextBox getDataValue(String component_id, Object object, ModelContext modelContext) throws WTException {

		System.out.println("******* Capacitance DataUtility Started ***********");

		// Create TextBox for capacitance value input
		TextBox textBox = new TextBox();
		textBox.setId("CAPACITANCE");
		textBox.setName("CAPACITANCE_VALUE");
		textBox.setPlaceHolder("Enter capacitance value");
		textBox.setTooltip("Enter the numeric capacitance value (e.g., 10)");
		textBox.setMaxLength(10); // Limit the number of characters

//		// Create lists for ComboBox (unit selection)
//		ArrayList<String> displayValues = new ArrayList<>();
//		ArrayList<String> internalValues = new ArrayList<>();
//		ArrayList<String> selectedValues = new ArrayList<>();
//
//		// Add unit options
//		displayValues.add("F:Farad");
//		displayValues.add("mF:Millifarad");
//		displayValues.add("nF:Nanofarad");
//		displayValues.add("pF:Picofarad");
//		displayValues.add("uF:Microfarad");
//
//		internalValues.add("Farad");
//		internalValues.add("Millifarad");
//		internalValues.add("Nanofarad");
//		internalValues.add("Picofarad");
//		internalValues.add("Microfarad");
//
//		// Add default selected value
//		selectedValues.add("Farad"); // Default to "Farad"
//
//		// Create ComboBox
//		ComboBox comboBox = new ComboBox(internalValues, displayValues, selectedValues);
//		comboBox.setId("CAPACITANCE");
//		comboBox.setName("CAPACITANCE_UNIT");
//		comboBox.setMultiSelect(false); // Single selection
//		comboBox.setSize(5); // Show all options without scrolling
//		comboBox.setTooltip("Select the capacitance unit");

		// Set behavior based on mode
		if (modelContext.getDescriptorMode().equals(ComponentMode.CREATE)) {
			WTContainer container = modelContext.getNmCommandBean().getContainer();
			textBox.setEnabled(true);
//			comboBox.setEnabled(true);
			System.out.println("###### ComboBox Created For Product :" + container.getName());
		} else {
			textBox.setEditable(false);
//			comboBox.setEditable(false);

		}

		// Log debug information
		System.out.println("******* Capacitance DataUtility End ***********");

		// Return a composite object (both TextBox and ComboBox)
		return  textBox;
	}
}
