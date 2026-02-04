package ext.ptpl.load;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LoadEnum {

	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("D:\\enum\\part2\\downloadClassification.csv"));
		BufferedWriter buffer = new BufferedWriter(new FileWriter("D:\\\\enum\\\\part2\\downloadClassification.xml"));

		String line = "";

		StringBuilder sb = new StringBuilder();

		while ((line = br.readLine()) != null) {
			String[] str = line.split(":");

			for (int i = 0; i < 1; i++) {

				sb.append(
						"<csvPropertyValue handler=\"com.ptc.core.lwc.server.BaseDefinitionLoader.processEnumerationPropertyValue\">\r\n"
								+ "      <csvname>autoSort</csvname>\r\n"
								+ "      <csvisDefault>false</csvisDefault>\r\n"
								+ "      <csvvalue>false</csvvalue>\r\n" + "   </csvPropertyValue>\r\n"
								+ "   <csvBeginEnumMemberView handler=\"com.ptc.core.lwc.server.BaseDefinitionLoader.beginProcessEnumMembership\">\r\n"
								+ "      <csvname>" + str[0].toString().trim() + "</csvname>\r\n"
								+ "   </csvBeginEnumMemberView>\r\n"
								+ "   <csvPropertyValue handler=\"com.ptc.core.lwc.server.BaseDefinitionLoader.processEnumEntryPropertyValue\">\r\n"
								+ "      <csvname>displayName</csvname>\r\n"
								+ "      <csvisDefault>false</csvisDefault>\r\n" + "      <csvvalue>"
								+ str[1].toString().trim() + "</csvvalue>\r\n" + "   </csvPropertyValue>\r\n"
								+ "   <csvPropertyValue handler=\"com.ptc.core.lwc.server.BaseDefinitionLoader.processEnumEntryPropertyValue\">\r\n"
								+ "      <csvname>selectable</csvname>\r\n"
								+ "      <csvisDefault>false</csvisDefault>\r\n" + "      <csvvalue>true</csvvalue>\r\n"
								+ "   </csvPropertyValue>\r\n"
								+ "   <csvPropertyValue handler=\"com.ptc.core.lwc.server.BaseDefinitionLoader.processEnumMembershipPropertyValue\">\r\n"
								+ "      <csvname>sort_order</csvname>\r\n"
								+ "      <csvisDefault>false</csvisDefault>\r\n" + "      <csvvalue>0</csvvalue>\r\n"
								+ "   </csvPropertyValue>\r\n"
								+ "   <csvEndEnumMemberView handler=\"com.ptc.core.lwc.server.BaseDefinitionLoader.endProcessEnumMembership\"/>\r\n");

			}
		}
		sb.append(
				"<csvEndEnumDefView handler=\"com.ptc.core.lwc.server.BaseDefinitionLoader.endProcessEnumerationDefinition\"/>\r\n"
						+ "   <csvEndBaseDefsRbInfos handler=\"com.ptc.core.lwc.server.BaseDefinitionLoader.endProcessRbInfos\"/>\r\n"
						+ "   </NmLoader>\r\n");

		buffer.write(sb.toString());
		System.out.println("Check ProfitOutput file");
		buffer.close();
		System.out.println();
	}

}