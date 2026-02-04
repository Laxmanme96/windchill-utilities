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
package ext.emerson.migration;

import wt.util.resource.RBComment;
import wt.util.resource.RBEntry;
import wt.util.resource.RBUUID;
import wt.util.resource.WTListResourceBundle;

@RBUUID("com.plural.javafiles")
public final class docResource extends WTListResourceBundle {

	@RBEntry("\ncreatePartRepresentation: DOcument \"{0}\" doesn't exist in the load file or it was not created successfully.")
	@RBComment("Method name \"createDocRepresentation\" at beginning of this message should not be translated.  The rest of the message text can be translated. This message is displayed only in the output from running the command line tool LoadFromFile.")
	public static final String LOAD_NO_DOCUMENT_REPRESENTATION = "180";
	@RBEntry("Added Representation \"{0}\" to Document \"{1}\"")
	public static final String LOAD_REPRESENTATION_ADDED = "181";
	@RBEntry("Failed to add Representation \"{0}\" to Document \"{1}\"")
	public static final String LOAD_REPRESENTATION_FAILED = "182";
	
	 
}

