package ext.ptpl.datautility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import wt.util.WTProperties;



public class TgsPropertyHelper {
	public static String getPropertyValue(String key){
        String value = null;
        try {
            
            WTProperties props = WTProperties.getLocalProperties();
            Properties customProperties = new Properties();
            String codebase = props.getProperty("wt.codebase.location");
           // String customPropertiesPath = codebase + File.separator + "ext"+ File.separator + "rule" + File.separator  + "countryproperties.properties";
            customProperties.load(new FileInputStream("C:/ptc/Windchill_12.1/Windchill/custom/properties/CountryProperties.properties"));
            value = (String) customProperties.getProperty(key).trim();
        } catch (IOException e) {
        	e.printStackTrace(); 
        } catch(Exception e) {
        	e.printStackTrace();
        }

        return value;
    }
	
	/*
	 * @ this method return the int Value	 * 
	 */
    public static int getPropertyValues(String key){
        String value = null;
        int invalue = 0;
        try {
            key = key.replace(" ", "_");
            WTProperties props = WTProperties.getLocalProperties();
            Properties customProperties = new Properties();
            String codebase = props.getProperty("wt.codebase.location");
            String customPropertiesPath = codebase + File.separator + "ext"
            	+ File.separator + "rule" +  File.separator + "CountryProperties.properties";
            customProperties.load(new FileInputStream(customPropertiesPath));
            value = customProperties.getProperty(key).trim();
            invalue = Integer.parseInt(value);
        } catch (IOException e) {
        	e.printStackTrace();
        } catch(Exception e) {
        	e.printStackTrace();
        }

        return invalue;
    }
	

}
