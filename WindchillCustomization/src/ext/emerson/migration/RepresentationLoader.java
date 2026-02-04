package ext.emerson.migration;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class RepresentationLoader {

	public static void main(String args[]){

				try {
					String inDirectoryPath = "D:\\RH01_Migration\\Appleton_Document\\Doc";
					String outDirectoryPath = "D:\\RH01_Migration\\Appleton_Document\\XML";

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
				        outputFilename =outputFilename+"representation.xml";
				        BufferedReader br = new BufferedReader(new FileReader(inDirectoryPath+"\\"+inputFileName));
				        BufferedWriter bw =  new BufferedWriter(new FileWriter(outDirectoryPath+"\\"+outputFilename));

						String line ="";
						String repDir="",repFile="";


						StringBuilder sb = new StringBuilder();
						String loadFilePath = ""; //add Full Path of loadfiles
						sb.append("<?xml version=\"1.0\" ?><!DOCTYPE NmLoader SYSTEM \"standard12_0.dtd\">\r\n"
								+ "<NmLoader>"
								+ "\n");

			while((line = br.readLine()) != null) {
					String[] str = line.split(",",-1);

					try {
						for(int i = 0 ; i< 1 ; i++) {
							repFile =str[44].toString().trim();

							if((!repFile.equals(""))&&(!repFile.equals("NULL")))
							{
								repDir=str[45].toString().trim();
								repDir  =repDir.substring(0,(repDir.lastIndexOf("\\")));


								sb.append("	<csvBeginAPPLRepLoader handler=\"ext.emerson.migration.APPLRepLoader.createDocRepresentation\">\r\n"

										+ "    <csvdocNumber>"+ str[3].toString().trim()+"</csvdocNumber>\r\n"
										+ "    <csvdocVersion>"+str[13].toString().trim()+"</csvdocVersion>\r\n"
										+ "    <csvdocIteration>"+str[14].toString().trim()+"</csvdocIteration>\r\n"
										+"     <csvrepDirectory>"+repDir+"</csvrepDirectory> \r\n "
										+ "    <csvrepName>"+repFile+"</csvrepName>\r\n"
										+ "    <csvrepDescription>repDescription1</csvrepDescription>\r\n"
										+ "    <csvrepDefault>TRUE</csvrepDefault>\r\n"
										+ "    <csvrepCreateThumbnail>TRUE</csvrepCreateThumbnail>\r\n"
										+ "    <csvorganizationName>asconumatics</csvorganizationName>\r\n"
										+ "    <csvorganizationID></csvorganizationID>\r\n"

										+ "</csvBeginAPPLRepLoader>\r\n");
									}

								}

							}catch(Exception e) {}
						}
						sb.append("</NmLoader>" );
						bw.write(sb.toString());
						System.out.println("Check output file ");
						bw.close();

				      }

				    }

				}catch(IOException io) {
					System.out.println("IOException "+io);
				}
		}


}
