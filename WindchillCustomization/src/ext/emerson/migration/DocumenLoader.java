package ext.emerson.migration;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.Logger;

import ext.emerson.properties.CustomProperties;

public class DocumenLoader {

	private static final Logger logger = CustomProperties.getlogger("ext.emerson.migration");

		public static void main(String args[]){

			try {
				String inDirectoryPath = "D:\\Prod\\csv";
				String outDirectoryPath = "D:\\Prod\\xml";

			      // Using File class create an object for specific directory
			      File directory = new File(inDirectoryPath);

			      // Using listFiles method we get all the files of a directory
			      // return type of listFiles is array
			      File[] files = directory.listFiles();
			        int rowcount =0;
			      // Print name of the all files present in that path
			      String inputFileName,outputFilename;
			    if (files != null) {
			      for (File file : files) {
			        inputFileName = file.getName();
			        outputFilename = inputFileName.substring(0,inputFileName.lastIndexOf("."));
			        outputFilename =outputFilename+".xml";

			        logger.debug("Processing"+inputFileName);

					BufferedReader br = new BufferedReader(new FileReader(inDirectoryPath+"\\"+inputFileName));
					BufferedWriter bw =  new BufferedWriter(new FileWriter(outDirectoryPath+"\\"+outputFilename));

					String line ="";

					StringBuilder sb = new StringBuilder();
					String loadFilePath = ""; //add Full Path of loadfiles
					sb.append("<?xml version=\"1.0\" ?><!DOCTYPE NmLoader SYSTEM \"standard12_0.dtd\">\r\n"
							+ "<NmLoader>"
							+ "\n");

					while((line = br.readLine()) != null) {
						String[] str = line.split(",", -1);

						try {
						for(int i = 0 ; i< 1 ; i++) {

							sb.append("	<csvBeginAPPLDocLoader handler=\"ext.emerson.migration.APPLDocLoader.beginCreateWTDocument\">\r\n"
									+ "    <csvname>"+str[1].toString().trim()+"</csvname>\r\n"
									+ "    <csvtitle>"+ str[2].toString().trim()+"</csvtitle>\r\n"
									+ "    <csvnumber>"+ str[3].toString().trim()+"</csvnumber>\r\n"
									+ "    <csvtype>Document</csvtype>\r\n"
									+ "    <csvdescription>"+str[5].toString().trim()+"</csvdescription>\r\n"
									+ "    <csvdepartment>DESIGN</csvdepartment>\r\n"
									+ "    <csvsaveIn>"+str[7].toString().trim()+"</csvsaveIn>\r\n"
									+ "    <csvteamTemplate></csvteamTemplate>\r\n"
									+ "    <csvdomain></csvdomain>\r\n"
								    + "    <csvlifecycletemplate>"+str[10].toString().trim()+"</csvlifecycletemplate>\r\n"
									+ "    <csvlifecyclestate>"+str[11].toString().trim()+"</csvlifecyclestate>\r\n"

		/* Need confirmation*/
									+ "    <csvtypedef>"+str[12].toString().trim()+"</csvtypedef>\r\n"
									+ "    <csvversion>"+str[13].toString().trim()+"</csvversion>\r\n"
									//+ "    <csvversion>A</csvversion>\r\n"
									+ "    <csviteration>"+str[14].toString().trim()+"</csviteration>\r\n"
									+ "    <csvsecurityLabels></csvsecurityLabels>\r\n"
									+ "    <csvcreateTimestamp>"+str[16].toString().trim()+"</csvcreateTimestamp>\r\n"
									+ "    <csvmodifyTimestamp>"+str[17].toString().trim()+"</csvmodifyTimestamp>\r\n"
									+ "    <csvcustomcreatedby>"+str[18].toString().trim()+"</csvcustomcreatedby>\r\n"
									+ "    <csvcustommodifiedby>"+str[19].toString().trim()+"</csvcustommodifiedby>\r\n"


									+ "</csvBeginAPPLDocLoader>\r\n");

								if(!str[20].toString().trim().equals(""))
									sb.append( "<csvIBAValue handler=\"wt.iba.value.service.LoadValue.createIBAValue\" >\r\n"
									+ "    <csvdefinition>LEGACY_ID</csvdefinition>\r\n"
									+ "	   <csvvalue1>"+str[20].toString().trim()+"</csvvalue1>\r\n"
									+ "	   <csvvalue2></csvvalue2>\r\n"
									+ "	   <csvdependency_id></csvdependency_id>\r\n"
									+ "	  </csvIBAValue>\r\n");

								if(!str[21].toString().trim().equals(""))
									sb.append("<csvIBAValue handler=\"wt.iba.value.service.LoadValue.createIBAValue\" >\r\n"
										+ "    <csvdefinition>DIVISION</csvdefinition>\r\n"
										+ "	   <csvvalue1>"+str[21].toString().trim()+"</csvvalue1>\r\n"
										+ "	   <csvvalue2></csvvalue2>\r\n"
										+ "	   <csvdependency_id></csvdependency_id>\r\n"
										+ "	 </csvIBAValue>\r\n");

									if(!str[22].toString().trim().equals(""))
									sb.append( "<csvIBAValue handler=\"wt.iba.value.service.LoadValue.createIBAValue\" >\r\n"
									+ "    <csvdefinition>DESIGN_NUMBER</csvdefinition>\r\n"
									+ "	   <csvvalue1>"+str[22].toString().trim()+"</csvvalue1>\r\n"
									+ "	   <csvvalue2></csvvalue2>\r\n"
									+ "	   <csvdependency_id></csvdependency_id>\r\n"
									+ "	  </csvIBAValue>\r\n");

								if(!str[23].toString().trim().equals(""))
									sb.append("<csvIBAValue handler=\"wt.iba.value.service.LoadValue.createIBAValue\" >\r\n"
									+ "    <csvdefinition>DRAWN_BY</csvdefinition>\r\n"
									+ "	   <csvvalue1>"+str[23].toString().trim()+"</csvvalue1>\r\n"
									+ "	   <csvvalue2></csvvalue2>\r\n"
									+ "	   <csvdependency_id></csvdependency_id>\r\n"
								    +"</csvIBAValue>\r\n");
								if(!str[24].toString().trim().equals(""))
									sb.append("<csvIBAValue handler=\"wt.iba.value.service.LoadValue.createIBAValue\" >\r\n"
									+ "    <csvdefinition>ECN_BY</csvdefinition>\r\n"
									+ "	   <csvvalue1>"+str[24].toString().trim()+"</csvvalue1>\r\n"
									+ "	   <csvvalue2></csvvalue2>\r\n"
									+ "	   <csvdependency_id></csvdependency_id>\r\n"
									+"		</csvIBAValue>\r\n");
								if(!str[25].toString().trim().equals(""))
									sb.append("<csvIBAValue handler=\"wt.iba.value.service.LoadValue.createIBAValue\" >\r\n"
									+ "    <csvdefinition>APPRVD_BY</csvdefinition>\r\n"
									+ "	   <csvvalue1>"+str[25].toString().trim()+"</csvvalue1>\r\n"
									+ "	   <csvvalue2></csvvalue2>\r\n"
									+ "	   <csvdependency_id></csvdependency_id>\r\n"
									+"</csvIBAValue>\r\n");
								if(!str[26].toString().trim().equals(""))
									sb.append( "<csvIBAValue handler=\"wt.iba.value.service.LoadValue.createIBAValue\" >\r\n"
									+ "    <csvdefinition>CERT_REQUIRED</csvdefinition>\r\n"
									+ "	   <csvvalue1>"+str[26].toString().trim()+"</csvvalue1>\r\n"
									+ "	   <csvvalue2></csvvalue2>\r\n"
									+ "	   <csvdependency_id></csvdependency_id>\r\n"
									+"     </csvIBAValue>\r\n");
								if(!str[27].toString().trim().equals(""))
								sb.append( "<csvIBAValue handler=\"wt.iba.value.service.LoadValue.createIBAValue\" >\r\n"
									+ "    <csvdefinition>PROJECT_NUMBER</csvdefinition>\r\n"
									+ "	   <csvvalue1>"+str[27].toString().trim()+"</csvvalue1>\r\n"
									+ "	   <csvvalue2></csvvalue2>\r\n"
									+ "	   <csvdependency_id></csvdependency_id>\r\n"
									+"		</csvIBAValue>\r\n");
								if(!str[28].toString().trim().equals(""))
								sb.append("<csvIBAValue handler=\"wt.iba.value.service.LoadValue.createIBAValue\" >\r\n"
									+ "    <csvdefinition>CHECK_BY</csvdefinition>\r\n"
									+ "	   <csvvalue1>"+str[28].toString().trim()+"</csvvalue1>\r\n"
									+ "	   <csvvalue2></csvvalue2>\r\n"
									+ "	   <csvdependency_id></csvdependency_id>\r\n"
									+ "	 </csvIBAValue>\r\n");
								sb.append("<csvendIBAHolder handler=\"wt.iba.value.service.LoadValue.endIBAHolder\"/>\r\n");




								if (!str[30].toString().trim().isEmpty())
							sb.append("<csvEndWTDocument handler=\"wt.doc.LoadDoc.endCreateWTDocument\">	\r\n"
									+ "	<csvprimarycontenttype>ApplicationData</csvprimarycontenttype>\r\n"
									+ "    <csvpath>"+loadFilePath+str[30].toString().trim()+"</csvpath>\r\n"
									+ "    <csvformat></csvformat>\r\n"
									+ "    <csvcontdesc></csvcontdesc>\r\n"
									+ "    <csvparentContainerPath></csvparentContainerPath>\r\n"
									+ "		</csvEndWTDocument> \n");
								if (!str[33].toString().trim().isEmpty()) {
								sb.append("<csvContentFile handler=\"wt.load.LoadContent.createContentFile\" >	\r\n"
									+ "		<csvuser></csvuser> \r\n"
									+ "    <csvpath>"+loadFilePath+str[33].toString().trim()+"</csvpath>\r\n"
									+ "</csvContentFile>" + "\n");
							}

							if (!str[35].toString().trim().isEmpty()) {

							sb.append("<csvContentFile handler=\"wt.load.LoadContent.createContentFile\" >	\r\n"
									+ "		<csvuser></csvuser> \r\n"
									+ "    <csvpath>"+loadFilePath+str[35].toString().trim()+"</csvpath>\r\n"
									+ "</csvContentFile>" + "\n");
							}
							if (!str[37].toString().trim().isEmpty()) {

								sb.append("<csvContentFile handler=\"wt.load.LoadContent.createContentFile\" >	\r\n"
										+ "		<csvuser></csvuser> \r\n"
										+ "    <csvpath>"+loadFilePath+""+str[37].toString().trim()+"</csvpath>\r\n"
										+ "</csvContentFile>" + "\n");
							}
							if (!str[39].toString().trim().isEmpty()) {

								sb.append("<csvContentFile handler=\"wt.load.LoadContent.createContentFile\" >	\r\n"
										+ "		<csvuser></csvuser> \r\n"
										+ "    <csvpath>"+loadFilePath+str[39].toString().trim()+"</csvpath>\r\n"
										+ "</csvContentFile>" + "\n");
							}
							if (!str[41].toString().trim().isEmpty()) {

								sb.append("<csvContentFile handler=\"wt.load.LoadContent.createContentFile\" >	\r\n"
										+ "		<csvuser></csvuser> \r\n"
										+ "    <csvpath>"+loadFilePath+str[41].toString().trim()+"</csvpath>\r\n"
										+ "</csvContentFile>" + "\n");
							}

							if (!str[43].toString().trim().isEmpty()) {

								sb.append("<csvContentFile handler=\"wt.load.LoadContent.createContentFile\" >	\r\n"
										+ "		<csvuser></csvuser> \r\n"
										+ "    <csvpath>"+loadFilePath+str[43].toString().trim()+"</csvpath>\r\n"
										+ "</csvContentFile>" + "\n");
							}

							rowcount++;
						}

					}catch(Exception e) {


					}
					}
					sb.append("</NmLoader>" );

					bw.write(sb.toString());
					logger.debug("Check output file ");
					bw.close();
			    }
		}

			}catch(IOException io) {
				logger.debug("------------"+io);

			}

		}


}
