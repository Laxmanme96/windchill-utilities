package ext.emerson.migration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class ReplaceEmptyFilesWithDummy {

public static void main(String[] args) throws IOException {

    File txtFile = new File("C:\\Users\\E1494388\\Documents\\errFiles.txt");
	try(BufferedReader br = new BufferedReader(new FileReader(txtFile))) {
	    for(String line; (line = br.readLine()) != null; ) {
	    	//File directory
	    	File fileNew = new File("E:\\A.txt");

	          File origfile = new File(line.trim());
	            // Reading directory contents
	            String name = null;
	            FileUtils.copyFileToDirectory(fileNew, origfile.getParentFile());

	         //        Getting file name and deleting
	                if(origfile.getName() != null ){
	                     name = origfile.getAbsolutePath();
	                     origfile.delete();
	    System.out.println("new name :" + name);
	    fileNew = new File(origfile.getParent() + "\\A.txt");
	    System.out.println("old name : " + fileNew.getAbsolutePath());
	                    fileNew.renameTo(new File(name));
	                  //   fileNew.delete();
	                }


	        }
	    }
	    // line is not visible here.
	}





}