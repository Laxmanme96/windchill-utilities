/* bcwti
 *
 * Copyright (c) 2010 Parametric Technology Corporation (PTC). All Rights Reserved.
 *
 * This software is the confidential and proprietary information of PTC
 * and is subject to the terms of a software license agreement. You shall
 * not disclose such confidential information and shall use it only in accordance
 * with the terms of the license agreement.
 *
 * ecwti
 */
package ext.ptpl.rb;

import wt.util.resource.RBComment;
import wt.util.resource.RBEntry;
import wt.util.resource.RBUUID;
import wt.util.resource.WTListResourceBundle;

@RBUUID("ext.ptpl.rb")
public final class customRB extends WTListResourceBundle {
	
	@RBEntry("Custom Related Objects")
	@RBComment("This string is used for the name label for Custom Create Part")
	public static final String Custom_Create_Part_title = "part.CustomRelatedObjects.title";
	
	@RBEntry("Custom Related Objects")
	@RBComment("This string is used for the name label for Custom Create Part")
	public static final String Custom_Create_Part_description = "part.CustomRelatedObjects.description";
	
	@RBEntry("Custom Related Objects")
	@RBComment("This string is used for the name label for Custom Create Part")
	public static final String Custom_Create_Part_Tooltip = "part.CustomRelatedObjectTable.tooltip";
	
	@RBEntry("Custom Create Document")
	@RBComment("This string is used for the name label for Custom Create Part")
	public static final String Custom_Create_Document_title = "doc.createDoc.title";
	
	@RBEntry("Custom Create Document")
	@RBComment("This string is used for the name label for Custom Create Part")
	public static final String Custom_Document_description = "doc.createDoc.description";
		
	@RBEntry("Custom Create Document")
	@RBComment("This string is used for the name label for Custom Create Part")
	public static final String Custom_Create_Document_tooltip = "doc.createDoc.tooltip";
	
	 
}

