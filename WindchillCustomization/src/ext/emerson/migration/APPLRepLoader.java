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

import java.beans.PropertyVetoException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.logging.log4j.Logger;

import com.ptc.core.lwc.server.LoadAttValues;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.TypeIdentifierHelper;
import com.ptc.core.meta.type.mgmt.server.impl.association.AssociationConstraintHelper;
import com.ptc.core.meta.type.mgmt.server.impl.association.associationResource;

import wt.access.AccessControlServerHelper;
import wt.access.SecurityLabeled;
import wt.content.ApplicationData;
import wt.content.ContentHelper;
import wt.content.ContentItem;
import wt.content.ContentServerHelper;
import wt.doc.DepartmentList;
import wt.doc.DocumentType;
import wt.doc.WTDocument;
import wt.doc.WTDocumentHelper;
import wt.doc.WTDocumentMaster;
import wt.doc.WTDocumentUsageLink;
import wt.fc.ObjectReference;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.ReferenceFactory;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.folder.FolderNotFoundException;
import wt.iba.value.service.LoadValue;
import wt.inf.container.WTContained;
import wt.inf.container.WTContainerRef;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleManaged;
import wt.lifecycle.LifeCycleServerHelper;
import wt.lifecycle.State;
import wt.load.LoadContent;
import wt.load.LoadServerHelper;
import wt.log4j.LogR;
import wt.method.MethodContext;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.series.MultilevelSeries;
import wt.series.Series;
import wt.team.TeamHelper;
import wt.team.TeamManaged;
import wt.type.TypeManaged;
import wt.type.Typed;
import wt.ufid.FederatableInfo;
import wt.util.WTContext;
import wt.util.WTException;
import wt.util.WTMessage;
import wt.util.WTProperties;
import wt.util.WTPropertyVetoException;
import wt.vc.Iterated;
import wt.vc.IterationIdentifier;
import wt.vc.IterationInfo;
import wt.vc.Mastered;
import wt.vc.VersionControlHelper;
import wt.vc.VersionControlServerHelper;
import wt.vc.VersionIdentifier;
import wt.vc.VersionInfo;
import wt.vc.Versioned;
import wt.vc.config.LatestConfigSpec;
import wt.vc.wip.CheckoutLink;
import wt.vc.wip.WorkInProgressHelper;

/**
 * Creates and persists documents objects based on input from a comma seperated value
 * (csv) file.  Method names and parameters are defined in csvmapfile.txt.
 * <p>
 * The load methods use a wt.load.StandardLoadService cache to cache
 * document masters and document version objects to improve performance of creating
 * structures and updating attrbiutes.
 *
 * <BR><BR><B>Supported API: </B>true
 * <BR><BR><B>Extendable: </B>false
 *
 * @see wt.load.StandardLoadService
 * @see wt.part.loadPart
 **/
public class APPLRepLoader {
    private static final Logger LOGGER = LogR.getLoggerInternal("ext.emerson.migration.representation");

    /*
     *   Set the keys for the cache hashtable.  The keys are
     *   used for indexes into the cache for the various object and values.
     */
   private static String CURRENT_CONTENT_HOLDER = "Current ContentHolder";

   private static final String DOCUMENT_CACHE_KEY = "DOCUMENT_CACHE_KEY:";

   private static final String DOCUMENT_PREVIOUS_USER = "DOCUMENT_PREVIOUS_USER:";

   private static final String ASSOCIATION_RESOURCE = "com.ptc.core.meta.type.mgmt.server.impl.association.associationResource";

   /*
    *  Flag for showing if a new iteration or version was created.
    */
   private static final String DOCUMENT_NEW_VERSION = "DOCUMENT_NEW_VERSION";

   // SHOULD NOT BE USED.  PRESERVED TO SUPPORT OTHER LOADERS THAT ASSUME THIS IS USED.
   // USE LoadDoc.getDocument() INSTEAD
   private static String CURRENT_DOCUMENT = "Current Document";

   private static String RESOURCE         = "wt.doc.docResource";
	private static String DOCRESOURCE = "ext.emerson.migration.docResource";

	private static final String REPHELPER_CLASS = "com.ptc.wvs.server.ui.RepHelper";
	private static final String REPHELPER_METHOD = "loadRepresentation";

   /**
    * Resource bundle object for localizing message text
    **/
   private static ResourceBundle rb;

  /**
   * Flag to control vervbose debugging output during document loading.
   * This constant is controlled via <code>wt.properties</code> file entry
   * <br>
   * <code>wt.doc.load.verbose</code>
   * <p>
   * The default value is <code>false</code>.
   **/
   public static  boolean VERBOSE;

	private static String WTHOME;

	private static String DIRSEP;
   static {
      try {

         rb = ResourceBundle.getBundle(RESOURCE,
                                       WTContext.getContext().getLocale());
         WTProperties properties = WTProperties.getLocalProperties();
       //  VERBOSE = properties.getProperty("wt.doc.load.verbose", false);

		VERBOSE = true;
	WTHOME = properties.getProperty("wt.home", "");
			DIRSEP = properties.getProperty("dir.sep", "");
      }
      catch (Throwable t)
      {
         System.err.println("Error initializing " + APPLRepLoader.class.getName ());
         t.printStackTrace(System.err);
         throw new ExceptionInInitializerError(t);
      }
   }

   /**
   * Processes the "General" or "Document" directive in the csv load file.
   * Creates a General document object, persists it in the database, checks it out,
   * applies the default attribute values associated with the specified type
   * definition, persists the document, checks it back in, and caches it in
   * the loader's memory.
   * <p>
   * Subsequent <code>IBAValue</code> load file lines may be used to associate
   * soft attribute values with the document.  These values will not
   * be persisted until a "EndWTDocument" load file line is processed.
   * <p>
   * Establishes the document as the CURRENT_CONTENT_HOLDER for use by "ContentFile" lines.
   * <p>
   * Supports versioning which allows a document to be created at a specified version and
   * iteration.  Multiple document versions imply an "order".  I.E. subsequent bulk load
   * runs can "fill in the gaps", but it does so by attaching to the latest iteration of the
   * previous version.  If a newer iteration is added to the previous version, the new version
   * will attached to the new latest iteration.  For example:  Load set 1 (E.1, A.1, C.2) will
   * result in (A.1, C.2, E.1).  The predecssors of: C.2 is A.1, E.1 is C.2.  Load set 2
   * (B.1, A.2., C.1, C.3) will result in (A.1, A.2, B.1, C.1, C.2, C.3, E.1).
   * The predecessors of: B.1 is A.2, C.1 is B.1, E.1 is C.3.  Any new version/iterations added
   * will continue to change the predessor links to the new latest iteration of the previous version.
   * <p>
   * Versioning <B>does</B> support gaps in the ordering.
   * <p>
   * Examples of valid versioning are: (A.1,A.3,B.2,B.5,E.4,E.5)
   * <p>
   * NOTE TO USERS OF THE SOURCE CODE: The ability to load document versions out of order is implemented
   * by using the VersionControlHelper.service.insertNode() method.  Calls to this method must not
   * be removed from the code.  To turn off this behavior, you should instead set the
   * insert_on_latest_iteration flag to false in the constructDocument() method.
   * <p>
   * WARNING:  By default on loading iterations/versions of a document all of the content is removed
   * from the document on the new iteration/version creation.  This is to stop excess content
   * from accumulating on the document from iteration to iteration.  So any new content both
   * primary or secondary will be the only content.  The previous iteratin/version is not
   * touched, only the content on the new document that is created.  If you want to load documents
   * that retain their secondary content from the previous iteration/version and then replace any
   * of the old files with the new files of the same name use either DocumentRetainContent or
   * BeginWTDocumentRetainContent/EndWTDocumentRetainContent with ReplaceContentFile.
   * <p>
   * @param nv Name/value pairs of meta data to set on the general document.
   *  The attributes are as follows: (arguments in &lt;&gt; are optional)
   *                         <ul>
   *                            <li>&lt;user&gt;
   *                            <li>name
   *                            <li>title
   *                            <li>number
   *                            <li>type
   *                            <li>&lt;description&gt;
   *                            <li>department
   *                            <li>saveIn
   *                            <li>&lt;teamTemplate&gt;
   *                            <li>&lt;domain&gt;
   *                            <li>&lt;lifecycletemplate&gt;
   *                            <li>&lt;lifecyclestate&gt;
   *                            <li>&lt;typedef&gt;
   *                            <li>&lt;primarycontenttype&gt; (used by wt.load.LoadContent)
   *                            <li>&lt;path&gt;     (used by wt.load.LoadContent)
   *                            <li>format     (used by wt.load.LoadContent)
   *                            <li>contdesc   (used by wt.load.LoadContent)
   *                            <li>&lt;version&gt;
   *                            <li>&lt;iteration&gt;
   *                         </ul>
   * @param cmd_line command line argument that can be substituted into the load data.
   * @param return_objects Object(s) created by this method used by
   * <code>wt.load.StandardLoadService</code> for user feedback messages.
   * <BR><BR><B>Supported API: </B>true
   * <BR><BR><B>Extendable: </B>false
   **/
   public static boolean createGeneral( Hashtable nv, Hashtable cmd_line, Vector return_objects ) {
      return createDocumentObject(nv,cmd_line,return_objects) &&
             updateDocumentObject(nv,cmd_line,return_objects);
   }

   /**
   * WARNING DO NOT USE THIS OPTION UNLESS YOU ARE HANDLING THE ISSUE OF MULTIPLE SECONDARY CONTENT
   * ITEMS ON NEW ITERATIONS/VERSIONS.  The content service automatically copies all content forward
   * on a new version/iteration.  This is obvious in the UI when the user modifies the new iteration but
   * here during a load situation this is not the most desireable.  The load file can have multiple
   * iterations/versions of a document with the content files to be loaded for each.  For example
   * if A.1 has file1.doc as secondary content and A.2 has file1.doc, but modified contents, this load
   * will create A.2 with file1.doc and file1-2.doc (new version of file) when used with ContentFile.
   * The other document loads will remove all content and then create A.2 with
   * file1.doc (new version of file1.doc).  Use ReplaceContentFile instead of ContentFile with this
   * method to carry forward the content but then replace file1.doc when the filename matches a new
   * file for the new iteration.
   * <p>
   * Creates a document object, persists it in the database, checks it out,
   * applies the default attribute values associated with the specified type
   * definition, persists the document, checks it back in, and caches it in
   * the loader's memory.
   * <p>
   * Subsequent <code>IBAValue</code> load file lines may be used to associate
   * soft attribute values with the document.  These values will not
   * be persisted until a "EndWTDocument" load file line is processed.
   * <p>
   * Establishes the document as the CURRENT_CONTENT_HOLDER for use by "ContentFile" lines.
   * <p>
   * Supports versioning which allows a document to be created at a specified version and
   * iteration.  Multiple document versions imply an "order".  I.E. subsequent bulk load
   * runs can "fill in the gaps", but it does so by attaching to the latest iteration of the
   * previous version.  If a newer iteration is added to the previous version, the new version
   * will attached to the new latest iteration.  For example:  Load set 1 (E.1, A.1, C.2) will
   * result in (A.1, C.2, E.1).  The predecssors of: C.2 is A.1, E.1 is C.2.  Load set 2
   * (B.1, A.2., C.1, C.3) will result in (A.1, A.2, B.1, C.1, C.2, C.3, E.1).
   * The predecessors of: B.1 is A.2, C.1 is B.1, E.1 is C.3.  Any new version/iterations added
   * will continue to change the predessor links to the new latest iteration of the previous version.
   * <p>
   * Versioning <B>does</B> support gaps in the ordering.
   * <p>
   * Examples of valid versioning are: (A.1,A.3,B.2,B.5,E.4,E.5)
   * <p>
   * NOTE TO USERS OF THE SOURCE CODE: The ability to load document versions out of order is implemented
   * by using the VersionControlHelper.service.insertNode() method.  Calls to this method must not
   * be removed from the code.  To turn off this behavior, you should instead set the
   * insert_on_latest_iteration flag to false in the constructDocument() method.
   * <p>
   * @param nv Name/value pairs of meta data to set on the general document.
   *  The attributes are as follows: (arguments in &lt;&gt; are optional)
   *                         <ul>
   *                            <li>&lt;user&gt;
   *                            <li>name
   *                            <li>title
   *                            <li>number
   *                            <li>type (set programatically, not in csv)
   *                            <li>&lt;description&gt;
   *                            <li>department
   *                            <li>saveIn
   *                            <li>&lt;teamTemplate&gt;
   *                            <li>&lt;domain&gt;
   *                            <li>&lt;lifecycletemplate&gt;
   *                            <li>&lt;lifecyclestate&gt;
   *                            <li>&lt;typedef&gt;
   *                            <li>&lt;primarycontenttype&gt; (used by wt.load.LoadContent)
   *                            <li>&lt;path&gt;     (used by wt.load.LoadContent)
   *                            <li>format     (used by wt.load.LoadContent)
   *                            <li>contdesc   (used by wt.load.LoadContent)
   *                            <li>&lt;version&gt;
   *                            <li>&lt;iteration&gt;
   *                         </ul>
   * @param cmd_line command line argument that can be substituted into the load data.
   * @param return_objects Object(s) created by this method used by
   * <code>wt.load.StandardLoadService</code> for user feedback messages.
   * <BR><BR><B>Supported API: </B>true
   * <BR><BR><B>Extendable: </B>false
   **/
   public static boolean createDocumentRetainContent( Hashtable nv, Hashtable cmd_line, Vector return_objects ) {
      return createDocumentObject(nv,cmd_line,return_objects,false) &&
             updateDocumentObject(nv,cmd_line,return_objects);
   }

   /**
   * Processes "BeginWTDocument" lines from a csv load file.
   * Creates a document object, persists it in the database, and caches it
   * in the loader's memory.
   * <p>
   * Subsequent <code>IBAValue</code> load file lines may be used to associate
   * soft attribute values with the document.  These values will not
   * be persisted until a "EndWTDocument" load file line is processed.
   * <p>
   * Establishes the document as the CURRENT_CONTENT_HOLDER for use by "ContentFile" lines.
   * <p>
   * A typical sequence using this directive might be
   * <blockquote>
   *       <tt>BeginWTDocument,name,number,...</tt>
   *   <br><tt>IBAValue,definition1,value1,...</tt>
   *   <br><tt>IBAValue,definition2,value2,...</tt>
   *   <br><tt>EndWTDocument</tt>
   *   <br><tt>ContentFile,...</tt>
   * </blockquote>
   * <p>
   * Supports versioning which allows a document to be created at a specified version and
   * iteration.  Multiple document versions imply an "order".  I.E. subsequent bulk load
   * runs can "fill in the gaps", but it does so by attaching to the latest iteration of the
   * previous version.  If a newer iteration is added to the previous version, the new version
   * will attached to the new latest iteration.  For example:  Load set 1 (E.1, A.1, C.2) will
   * result in (A.1, C.2, E.1).  The predecssors of: C.2 is A.1, E.1 is C.2.  Load set 2
   * (B.1, A.2., C.1, C.3) will result in (A.1, A.2, B.1, C.1, C.2, C.3, E.1).
   * The predecessors of: B.1 is A.2, C.1 is B.1, E.1 is C.3.  Any new version/iterations added
   * will continue to change the predessor links to the new latest iteration of the previous version.
   * <p>
   * Versioning <B>does</B> support gaps in the ordering.
   * <p>
   * Examples of valid versioning are: (A.1,A.3,B.2,B.5,E.4,E.5)
   * <p>
   * NOTE TO USERS OF THE SOURCE CODE: The ability to load document versions out of order is implemented
   * by using the VersionControlHelper.service.insertNode() method.  Calls to this method must not
   * be removed from the code.  To turn off this behavior, you should instead set the
   * insert_on_latest_iteration flag to false in the constructDocument() method.
   * <p>
   * WARNING:  By default on loading iterations/versions of a document all of the content is removed
   * from the document on the new iteration/version creation.  This is to stop excess content
   * from accumulating on the document from iteration to iteration.  So any new content both
   * primary or secondary will be the only content.  The previous iteratin/version is not
   * touched, only the content on the new document that is created.  If you want to load documents
   * that retain their secondary content from the previous iteration/version and then replace any
   * of the old files with the new files of the same name use
   * BeginWTDocumentRetainContent/EndWTDocumentRetainContent with ReplaceContentFile.
   * <p>
   * @see #endCreateWTDocument
   *
   * @param nv               Name/Value pairs of document attributes.
   *      The attributes are as follows: (arguments in &lt;&gt; are optional)
   *                         <ul>
   *                            <li>&lt;user&gt;
   *                            <li>name
   *                            <li>title
   *                            <li>number
   *                            <li>type (set programatically, not in csv)
   *                            <li>&lt;description&gt;
   *                            <li>department
   *                            <li>saveIn
   *                            <li>&lt;teamTemplate&gt;
   *                            <li>&lt;domain&gt;
   *                            <li>&lt;lifecycletemplate&gt;
   *                            <li>&lt;lifecyclestate&gt;
   *                            <li>&lt;typedef&gt;
   *                            <li>&lt;version&gt;
   *                            <li>&lt;iteration&gt;
   *                         </ul>
   * @param cmd_line         command line argument that can contain supplemental load data
   * @param return_objects   <code>Vector</code> of the object(s) created by this method.
   *                         Used by <code>wt.load.StandardLoadService</code>
   *                         for accurate user feedback messages.
   **/
   public static boolean beginCreateWTDocument( Hashtable nv, Hashtable cmd_line, Vector return_objects ) {
      resetAttDirectiveFlags();
      return createDocumentObject(nv,cmd_line,return_objects);
   }

   /**
   * Processes "EndWTDocument" lines from a csv load file.
   * Causes the cached document to be checked-out, associates soft attribues from preceding
   * <code>IBAValue</code> load file lines with the document, applies the default attribute values
   * associated with the specified type definition, persists the document, checks it back in,
   * and caches it in the loader's memory.
   * <p>
   * Establishes the document as the CURRENT_CONTENT_HOLDER for use by "ContentFile" lines.
   * <p>
   * @see #beginCreateWTDocument
   *
   * @param nv               Name/Value pairs of document attributes.
   *      The attributes are as follows: (arguments in &lt;&gt; are optional)
   *                         <ul>
   *                            <li>&lt;primarycontenttype&gt; (used by wt.load.LoadContent)
   *                            <li>&lt;path&gt;     (used by wt.load.LoadContent)
   *                            <li>format     (used by wt.load.LoadContent)
   *                            <li>contdesc   (used by wt.load.LoadContent)
   *                         </ul>
   * @param cmd_line         command line argument that can contain supplemental load data
   * @param return_objects   <code>Vector</code> of the object(s) created by this method.
   *                         Used by <code>wt.load.StandardLoadService</code>
   *                         for accurate user feedback messages.
   **/
   public static boolean endCreateWTDocument( Hashtable nv, Hashtable cmd_line, Vector return_objects ) {
      return updateDocumentObject(nv,cmd_line,return_objects);
   }

   /**
   * WARNING DO NOT USE THIS OPTION UNLESS YOU ARE HANDLING THE ISSUE OF MULTIPLE SECONDARY CONTENT
   * ITEMS ON NEW ITERATIONS/VERSIONS.  The content service automatically copies all content forward
   * on a new version/iteration.  This is obvious in the UI when the user modifies the new iteration but
   * here during a load situation this is not the most desireable.  The load file can have multiple
   * iterations/versions of a document with the content files to be loaded for each.  For example
   * if A.1 has file1.doc as secondary content and A.2 has file1.doc, but modified contents, this load
   * will create A.2 with file1.doc and file1-2.doc (new version of file) when used with ContentFile.
   * The other document loads will remove all content and then create A.2 with
   * file1.doc (new version of file1.doc).  Use ReplaceContentFile instead of ContentFile with this
   * method to carry forward the content but then replace file1.doc when the filename matches a new
   * file for the new iteration.
   * <p>
   * Processes "BeginWTDocumentRetainContent" lines from a csv load file.
   * Creates a document object, persists it in the database, and caches it
   * in the loader's memory.
   * <p>
   * Subsequent <code>IBAValue</code> load file lines may be used to associate
   * soft attribute values with the document.  These values will not
   * be persisted until a "EndWTDocumentRetainContent" load file line is processed.
   * <p>
   * Establishes the document as the CURRENT_CONTENT_HOLDER for use by "ContentFile" lines.
   * <p>
   * A typical sequence using this directive might be
   * <blockquote>
   *       <tt>BeginWTDocument,name,number,...</tt>
   *   <br><tt>IBAValue,definition1,value1,...</tt>
   *   <br><tt>IBAValue,definition2,value2,...</tt>
   *   <br><tt>EndWTDocument</tt>
   *   <br><tt>ContentFile,...</tt>
   * </blockquote>
   * <p>
   * Supports versioning which allows a document to be created at a specified version and
   * iteration.  Multiple document versions imply an "order".  I.E. subsequent bulk load
   * runs can not "fill in the gaps".  Versioning <B>does</B> support gaps in the ordering.
   * <p>
   * Examples of valid versioning are: (A.1,A.3,B.2,B.5,E.4,E.5)
   * <p>
   * NOTE TO USERS OF THE SOURCE CODE: The ability to load document versions out of order is implemented
   * by using the VersionControlHelper.service.insertNode() method.  Calls to this method must not
   * be removed from the code.  To turn off this behavior, you should instead set the
   * insert_on_latest_iteration flag to false in the constructDocument() method.
   * <p>
   * @see #endCreateWTDocumentRetainContent
   *
   * @param nv               Name/Value pairs of document attributes.
   *      The attributes are as follows: (arguments in &lt;&gt; are optional)
   *                         <ul>
   *                            <li>&lt;user&gt;
   *                            <li>name
   *                            <li>title
   *                            <li>number
   *                            <li>type (set programatically, not in csv)
   *                            <li>&lt;description&gt;
   *                            <li>department
   *                            <li>saveIn
   *                            <li>&lt;teamTemplate&gt;
   *                            <li>&lt;domain&gt;
   *                            <li>&lt;lifecycletemplate&gt;
   *                            <li>&lt;lifecyclestate&gt;
   *                            <li>&lt;typedef&gt;
   *                            <li>&lt;version&gt;
   *                            <li>&lt;iteration&gt;
   *                         </ul>
   * @param cmd_line         command line argument that can contain supplemental load data
   * @param return_objects   <code>Vector</code> of the object(s) created by this method.
   *                         Used by <code>wt.load.StandardLoadService</code>
   *                         for accurate user feedback messages.
   **/
   public static boolean beginCreateWTDocumentRetainContent( Hashtable nv, Hashtable cmd_line, Vector return_objects ) {
      resetAttDirectiveFlags();
      return createDocumentObject(nv,cmd_line,return_objects,false);
   }

   /**
   * Processes "EndWTDocumentRetainContent" lines from a csv load file.
   * Causes the cached document to be checked-out, associates soft attribues from preceding
   * <code>IBAValue</code> load file lines with the document, applies the default attribute values
   * associated with the specified type definition, persists the document, checks it back in,
   * and caches it in the loader's memory.
   * <p>
   * Establishes the document as the CURRENT_CONTENT_HOLDER for use by "ContentFile" lines.
   * <p>
   * @see #beginCreateWTDocumentRetainContent
   *
   * @param nv               Name/Value pairs of document attributes.
   *      The attributes are as follows: (arguments in &lt;&gt; are optional)
   *                         <ul>
   *                            <li>&lt;primarycontenttype&gt; (used by wt.load.LoadContent)
   *                            <li>&lt;path&gt;     (used by wt.load.LoadContent)
   *                            <li>format     (used by wt.load.LoadContent)
   *                            <li>contdesc   (used by wt.load.LoadContent)
   *                         </ul>
   * @param cmd_line         command line argument that can contain supplemental load data
   * @param return_objects   <code>Vector</code> of the object(s) created by this method.
   *                         Used by <code>wt.load.StandardLoadService</code>
   *                         for accurate user feedback messages.
   **/
   public static boolean endCreateWTDocumentRetainContent( Hashtable nv, Hashtable cmd_line, Vector return_objects ) {
      return endCreateWTDocument(nv,cmd_line,return_objects);
   }


   /**
   * Add document to a structured document.
   *
   * @param nv Name/value pairs to identify documents for the relationship.
   * @param cmd_line command line argument that can be substituted into the load data.
   * @param return_objects Object(s) created by this method used by
   * <code>wt.load.StandardLoadService</code> for user feedback messages.
   * <BR><BR><B>Supported API: </B>true
   **/
   public static boolean addToDocumentStructure( Hashtable nv, Hashtable cmd_line, Vector return_objects ) {

      try {

         WTDocument parent_document = getDocument(getValue("parent",nv,cmd_line,true));
         WTDocument child_document = getDocument(getValue("child",nv,cmd_line,true));

         if((parent_document!=null)&&(child_document!=null)) {
            if ((getUsageLink(parent_document,(WTDocumentMaster)child_document.getMaster()))==null)
            {
               WTDocumentUsageLink usage_link = WTDocumentUsageLink.newWTDocumentUsageLink(parent_document,
                                             (WTDocumentMaster)child_document.getMaster());

               // Added WTDocument Constraint Check
               TypeIdentifier usage_link_type = TypeIdentifierHelper.getType(usage_link);
               LOGGER.debug("Association Link Type :: " + usage_link_type);
               if (!AssociationConstraintHelper.service.isValidAssociation(parent_document, usage_link_type, child_document)) {
               	Object[]params = {  parent_document.getIdentity(),
                                    child_document.getIdentity(),
                                    usage_link_type.getTypename(),
                                    TypeIdentifierHelper.getType(parent_document).getTypename(),
                                    TypeIdentifierHelper.getType(child_document).getTypename()
                                 };
               	String message = WTMessage.getLocalizedMessage(ASSOCIATION_RESOURCE, associationResource.INVALID_ROLE_B_TYPE, params);
               	LOGGER.debug(message);
               	throw new WTException(message);
               }
               LOGGER.debug("Valid relationship for " + parent_document.getIdentity() + ", and " + child_document.getIdentity() + ". Relationship is " + usage_link_type.getTypename() + " between " + TypeIdentifierHelper.getType(parent_document).getTypename() + " and " + TypeIdentifierHelper.getType(child_document).getTypename() );

               usage_link.setStructureOrder(0);
               // Only use the insert when access control is being ignored.  Should not use
               // in a regular client.

               PersistenceServerHelper.manager.insert(usage_link);
               return true;
            }
            else
            {
               LoadServerHelper.printMessage("\nWTDocumentUsageLink already exists between " + parent_document.getName()+ " and " + child_document.getName());
            }
         }
      }
      catch( WTException e ) {
         LoadServerHelper.printMessage("\nAdd To Document Structure Failed: " + e.getLocalizedMessage());
         e.printStackTrace();
      }
      catch( Exception e ) {
         LoadServerHelper.printMessage("\nAdd To Document Structure Failed: " + e.getMessage());
         e.printStackTrace();
      }
      return false;

   }

   /**
   * Add documents to a dependency link.
   *
   * @param nv Name/value pairs to identify documents for the relationship.
   * @param cmd_line command line argument that can be substituted into the load data.
   * @param return_objects Object(s) created by this method used by
   * <code>wt.load.StandardLoadService</code> for user feedback messages.
   * <BR><BR><B>Supported API: </B>true
   **/
   public static boolean addDocumentDependency( Hashtable nv, Hashtable cmd_line, Vector return_objects ) {

      try {
         WTDocument doc = getDocument(getValue("doc",nv,cmd_line,true));
         if(doc!=null) {
            WTDocument referenceddoc = getDocument(getValue("referenceddoc",nv,cmd_line,true));
            if(referenceddoc!=null) {
               String comment = getValue("comment",nv,cmd_line,false);

               Folder checkedOutFolder = WorkInProgressHelper.service.getCheckoutFolder();
               CheckoutLink checkedoutLink = WorkInProgressHelper.service.checkout(doc, checkedOutFolder ,"Checking out to create DocumentDependency using LoadFromFile utility");
               WTDocument workingCopy = (wt.doc.WTDocument)(checkedoutLink).getWorkingCopy();
               WTDocumentHelper.service.createDependencyLink(workingCopy,referenceddoc,comment);
               doc = (WTDocument)WorkInProgressHelper.service.checkin(workingCopy,"Created DocumentDependency using LoadFromFile utility");
               doc = cacheDocument(doc);

               return true;
            }
         }
      }
      catch( WTException e ) {
         LoadServerHelper.printMessage("\nAdd Document Dependency Failed: " + e.getLocalizedMessage());
         e.printStackTrace();
      }
      catch( Exception e ) {
         LoadServerHelper.printMessage("\nAdd Document Dependency Failed: " + e.getMessage());
         e.printStackTrace();
      }
      return false;
   }

   /**
   * RETRIEVE DOCUMENT MOST RECENTLY ADDED TO THE CACHE
   *
   * <BR><BR><B>Supported API: </B>false
   **/
   public static WTDocument getDocument() throws WTException {
      return getDocument(null,null,null);
   }

   /**
   * RETRIEVE A DOCUMENT BASED ON DOCUMENT NUMBER (CACHED)
   * IF number IS null, RETURNS DOCUMENT MOST RECENTLY ADDED TO THE CACHE
   *
   * @param number Document number.
   *
   * <BR><BR><B>Supported API: </B>false
   **/
   public static WTDocument getDocument( String number ) throws WTException {
      return getDocument(number,null,null);
   }

   /**
   * RETRIEVE A DOCUMENT BASED ON DOCUMENT NUMBER AND VERSION (CACHED)
   * IF number IS null, RETURNS DOCUMENT MOST RECENTLY ADDED TO THE CACHE
   * IF version IS null, RETURNS DOCUMENT BASED ON DOCUMENT NUMBER ONLY
   *
   * @param number Document number.
   * @param version Document version.
   *
   * <BR><BR><B>Supported API: </B>false
   **/
   public static WTDocument getDocument( String number, String version ) throws WTException {
      return getDocument(number,version,null);
   }

   /**
   * RETRIEVE A DOCUMENT BASED ON DOCUMENT NUMBER, VERSION, AND ITERATION (CACHED)
   * IF number IS null, RETURNS DOCUMENT MOST RECENTLY ADDED TO THE CACHE
   * IF version IS null, RETURNS DOCUMENT BASED ON DOCUMENT NUMBER ONLY
   * IF iteration IS null, RETURNS DOCUMENT BASED ON DOCUMENT NUMBER AND VERSION ONLY
   *
   * @param number Document number.
   * @param version Document version.
   * @param iteration Document iteration.
   *
   * <BR><BR><B>Supported API: </B>false
   **/
   public static WTDocument getDocument( String number, String version, String iteration ) throws WTException {

      WTDocument document = getCachedDocument(number,version,iteration);
      LatestConfigSpec config_spec = null;

      if (document == null) {
         if (number != null) {
            QuerySpec qs = new QuerySpec(WTDocument.class);
            qs.appendWhere(new SearchCondition( WTDocument.class,WTDocument.NUMBER,SearchCondition.EQUAL,
                                                number.toUpperCase(),false));
            if (version == null) {
               qs.appendAnd();
               qs.appendWhere(new SearchCondition( WTDocument.class,Iterated.ITERATION_INFO+"."+IterationInfo.LATEST,
                                                   SearchCondition.IS_TRUE));
               config_spec = new LatestConfigSpec();
            }
            else {
               if (version != null){
                    qs.appendAnd();
                  qs.appendWhere(new SearchCondition( WTDocument.class,Versioned.VERSION_INFO+"."+VersionInfo.IDENTIFIER+"."+"versionId",
                                                   SearchCondition.EQUAL,version,false));
                  if (iteration == null) {
                      qs.appendAnd();
                       qs.appendWhere(new SearchCondition(WTDocument.class,Iterated.ITERATION_INFO + "." + IterationInfo.LATEST,
                                                SearchCondition.IS_TRUE));
                  }
                  else if (iteration != null) {
                     qs.appendAnd();
                     qs.appendWhere(new SearchCondition( WTDocument.class,Iterated.ITERATION_INFO+"."+IterationInfo.IDENTIFIER+"."+"iterationId",
                                                      SearchCondition.EQUAL,iteration,false));
                  }
               }
            }
            if (config_spec != null)
               qs = config_spec.appendSearchCriteria(qs);
            QueryResult qr = PersistenceHelper.manager.find(qs);
            if (config_spec != null)
               qr = config_spec.process(qr);
            int results = qr.size();

            if (results == 1){
               document = (WTDocument)qr.nextElement();
               if (WorkInProgressHelper.isCheckedOut(document)) {
                  String vers_strg = document.getVersionDisplayIdentifier().getLocalizedMessage(WTContext.getContext().getLocale()) + "." +
                                     document.getIterationIdentifier().getValue();
                  String checkoutstate = WorkInProgressHelper.getState(document).getDisplay();
                  String msg = "Operation failed because the document is checked out, " + number + " " + vers_strg + " with state of " + checkoutstate;
                  throw new WTException(msg);
               }
               document = cacheDocument(document);
            }
            else if (results > 1) {
              document = (WTDocument)qr.nextElement();
              if (WorkInProgressHelper.isCheckedOut(document)) {
                 String vers_strg = document.getVersionDisplayIdentifier().getLocalizedMessage(WTContext.getContext().getLocale()) + "." +
                                    document.getIterationIdentifier().getValue();
                 String checkoutstate = WorkInProgressHelper.getState(document).getDisplay();
                 String msg = "Operation failed because the document is checked out, " + number + " " + vers_strg + " with state of " + checkoutstate;
                 throw new WTException(msg);
              }
              else {
                 StringBuffer criteria = new StringBuffer(number);
                 if (version != null) {
                    criteria.append(", " + version);
                    if (iteration != null)
                       criteria.append("." + iteration);
                 }
                 String msg = "Searching for document returned " + results + " documents, document criteria = " +
                              criteria + " Only one document expected.";
                 throw new WTException(msg);
              }
            }
         }
      }
      return document;
   }

   /////////////////////
   // PRIVATE METHODS //
   /////////////////////

   private static boolean createDocumentObject( Hashtable nv, Hashtable cmd_line, Vector return_objects ) {
      return createDocumentObject(nv,cmd_line,return_objects,true);
   }

   private static boolean createDocumentObject( Hashtable nv, Hashtable cmd_line, Vector return_objects, boolean removeContent ) {
      try {
         setUser(nv,cmd_line);

         WTDocument document = constructDocument(nv,cmd_line);

         if (removeContent && ((Boolean)LoadServerHelper.getCacheValue(DOCUMENT_NEW_VERSION)).booleanValue())
            document = clearContent(document);
         document = cacheDocument(document);

         return true;
      }
      catch (WTException wte){
         LoadServerHelper.printMessage("\nCreate Document Failed (" + getDisplayInfo(nv,cmd_line) + "): " +
                                        wte.getLocalizedMessage());
         wte.printStackTrace();
      }
      catch (Exception e){
         LoadServerHelper.printMessage("\nCreate Document Failed (" + getDisplayInfo(nv,cmd_line) + "): " +
                                        e.getMessage());
         e.printStackTrace();
      }
      return false;
   }

   private static String getDisplayInfo(Hashtable nv, Hashtable cmd_line) {
      String number = null;
      String version = null;
      String iteration = null;
      try {
         number = getValue("number",nv,cmd_line,false);
         version = getValue("version",nv,cmd_line,false);
         iteration = getValue("iteration",nv,cmd_line,false);
      }
      catch (WTException wte) {
         // This is only used in exception cases to display more info so this shouldn't throw a new one
         // it should just move on valiantly defaulting the the value that has a problem.
      }
      if (number == null) number = "<no number>";
      if (version == null) version = "<version>";
      if (iteration == null) iteration = "<iteration>";
      return number + " " + version + "." + iteration;
   }


   private static boolean updateDocumentObject( Hashtable nv, Hashtable cmd_line, Vector return_objects ) {
      try {

         WTDocument document;

         final boolean loadValDirectiveUsed = Boolean.TRUE.equals( MethodContext.getContext().get(LoadAttValues.LOAD_VAL_DIRECTIVE_USED_KEY) );
         if ( loadValDirectiveUsed ) {
             document = (WTDocument) LoadAttValues.getCurrentTypeManaged();
             document = (WTDocument)PersistenceHelper.manager.modify(document);
             if ( LOGGER.isDebugEnabled() )
                 LOGGER.error( "new soft att directive was used for document: " + document.getName() );
         }
         else {
             document = getDocument();
             if ( LOGGER.isDebugEnabled() )
                 LOGGER.error( "new soft att value directive was *not* used for document: " + document.getName() );
         }

         final boolean ibaValDirectiveUsed = Boolean.TRUE.equals( MethodContext.getContext().get(LoadValue.IBA_VAL_DIRECTIVE_USED_KEY) );
         if ( ibaValDirectiveUsed ) {
             document = (WTDocument)LoadValue.applySoftAttributes(document);
             if ( LOGGER.isDebugEnabled() )
                 LOGGER.debug( "old iba att value directive was used for document: " + document.getName() );
         }
         else {
             if ( LOGGER.isDebugEnabled() )
                 LOGGER.debug( "old iba att value directive was *not* used for document: " + document.getName() );
         }

         document = setPrimaryContent(document,nv,cmd_line,return_objects);
         document = cacheDocument(document);

         return_objects.addElement(document);

         return true;
      }
      catch (WTException wte){
         LoadServerHelper.printMessage("\nUpdate Document Failed: " + wte.getLocalizedMessage());
         wte.printStackTrace();
      }
      catch (Exception e){
         LoadServerHelper.printMessage("\nUpdate Document Failed: " + e.getMessage());
         e.printStackTrace();
      }
      finally {
         try {
            resetUser();
         }
         catch( WTException e ) {
         }

         resetAttDirectiveFlags();
      }
      return false;
   }

   private static void resetAttDirectiveFlags() {
       // clean up these flags once we're done processing the soft attributes on this instance
       final MethodContext mc = MethodContext.getContext();
       mc.remove( LoadAttValues.LOAD_VAL_DIRECTIVE_USED_KEY );
       mc.remove( LoadValue.IBA_VAL_DIRECTIVE_USED_KEY );
   }

   // CONSTRUCTS A DOCUMENT
   // CONSTRUCTS A NEW DOCUMENT IF A DOCUMENT WITH THE SUPPLIED DOCUMENT NUMBER DOES NOT EXIST
   // OTHERWISE IF A VERSION IS SUPPLIED
   //    CONSTRUCTS A NEW VERSION OF THE EXISTING DOCUMENT IF THE SUPPLIED VERSION DOES NOT EXIST
   //    OTHERWISE CONSTRUCTS A NEW ITERATION OF THE SUPPLIED VERSION
   // OTHERWISE CONSTRUCTS A NEW ITERATION OF THE LATEST VERSION
   // Clears content on iterations/versions of an existing document.
   private static WTDocument constructDocument( Hashtable nv, Hashtable cmd_line ) throws WTException {

      String number = getValue("number",nv,cmd_line,false);
      String version = getValue("version",nv,cmd_line,false);
      String iteration = getValue("iteration",nv,cmd_line,false);

      // A flag that indicates support for the ability to load iterations/versions out of sequence,
      // connecting on the latest iteration.  Changing the value to "false" means you cannot load documents out
      // of iteration sequence.  If you want to be able to load them out of sequence this value must be "true".
      // WARNING: Do not delete any insertNode method calls
      boolean insert_on_latest_iteration = true;

      WTDocument document = null;
      //Only try and find document if number is specified; otherwise, assume this is going to create a document.
      if( number != null ) {
          //TRY TO FIND DOCUMENT BASED ON DOCUMENT NUMBER, VERSION, AND ITERATION
          document = getDocument(number,version,iteration);

          //TRY TO FIND DOCUMENT BASED ON DOCUMENT NUMBER, VERSION
          if((document==null)&&(version!=null)&&(iteration!=null)) document = getDocument(number,version,null);

          //TRY TO FIND DOCUMENT BASED ON DOCUMENT NUMBER
          if((document==null)&&(version!=null)) document = getDocument(number,null,null);
      }

      //CONSTRUCT DOCUMENT
      if(document==null) {
          document = WTDocument.newWTDocument();
          try{
             // Construct a default IterationInfo object.  Cannot do the same for the VersionInfo
             // object because there isn't enough information in the object to look up the correct
             // version series from the OIRs yet.
            if (document.getIterationInfo() == null)
               document.setIterationInfo( IterationInfo.newIterationInfo() );
         }
         catch (WTPropertyVetoException error) {
            throw new WTException(error);
         }
         document = applyConstructionTimeAttributes(document,nv,cmd_line);
         if (VERBOSE) LOGGER.debug("Creating a new document = " + number);
         LoadServerHelper.setCacheValue(DOCUMENT_NEW_VERSION,Boolean.FALSE);
      }
      else {
         if (insert_on_latest_iteration && !VersionControlHelper.isLatestIteration(document))
            document = (WTDocument)VersionControlHelper.getLatestIteration(document);
         if (VERBOSE) {
            String vers_strg = document.getVersionDisplayIdentifier().getLocalizedMessage(WTContext.getContext().getLocale()) + "." +
                               document.getIterationIdentifier().getValue();
            String checkoutstate = WorkInProgressHelper.getState(document).getDisplay();
            LOGGER.debug("Iterating on an existing document = " + number + " " + vers_strg + " with state of " +
                                checkoutstate);
         }
         try {
             //If wt document already exist and target Container is different then existing container, abort create
             WTContainerRef wtdocContainerRef = document.getContainerReference();
             WTContainerRef targetContainerRef = LoadServerHelper.getTargetContainer( nv, cmd_line );
             if(!wtdocContainerRef.equals(targetContainerRef)) {
                 throw new WTException("Can not create '" + number + " " + document.getName()+ " - " + version + "' in Container: '" + targetContainerRef.getName()
                     + "', because it already exists in Container: '" + wtdocContainerRef.getName() + "'" );
             }

            if (WorkInProgressHelper.isCheckedOut(document)) {
               String vers_strg = document.getVersionDisplayIdentifier().getLocalizedMessage(WTContext.getContext().getLocale()) + "." +
                                  document.getIterationIdentifier().getValue();
               String checkoutstate = WorkInProgressHelper.getState(document).getDisplay();
               throw new WTException("Creating the new version/iteration failed because the previous document is checked out, " + number + " " + vers_strg + " with state of " +
                                      checkoutstate);
            }
            else if (insert_on_latest_iteration) {
               // All non-creates should be inserts now, so the rest is just here for documentation purposes.
               // Even though newIteration is used, if needed a new revision will be created during the insertNode below.
               String calc_iter = null;
               boolean new_version = isNewVersion(document,version);
               if (iteration == null) {
                  // Need a little help if no iteration is given.
                  if (!new_version) {
                     int old_iter = Integer.parseInt(document.getIterationIdentifier().getValue());
                     calc_iter = Integer.toString(old_iter + 1);
                  }
                  else {
                     calc_iter = Integer.toString(1);
                  }
               }
               document = (WTDocument)VersionControlHelper.service.newIteration(document);
               LoadServerHelper.setCacheValue(DOCUMENT_NEW_VERSION,Boolean.TRUE);
               if (calc_iter != null)
                  setIteration(document,calc_iter);
            }
            // the next two elses are not reachable with the insert flag set to true, are here only if
            // someone wants to revert back to the old behavior of this code.
            else if((version==null)||version.equals(VersionControlHelper.getVersionIdentifier(document).getValue())) {
               WTDocument temp = (WTDocument)VersionControlHelper.service.newIteration(document);
               document = (WTDocument)PersistenceHelper.manager.refresh(document);
               document = (WTDocument)VersionControlHelper.service.supersede(document,temp);
               LoadServerHelper.setCacheValue(DOCUMENT_NEW_VERSION,Boolean.TRUE);
            }
            else {
               document = (WTDocument)VersionControlHelper.service.newVersion(document);
               LoadServerHelper.setCacheValue(DOCUMENT_NEW_VERSION,Boolean.TRUE);
            }

         }
         catch( WTPropertyVetoException e ) {
            throw new WTException(e);
         }
      }
      document = applyHardAttributes(document,nv,cmd_line);

      if (insert_on_latest_iteration) {
         try {
            document.setFederatableInfo(new FederatableInfo());
         }
         catch (WTPropertyVetoException wtpve) {
            throw new WTException(wtpve,"Error creating the FederatableInfo required for inserting versions/iterations");
         }
         // Use null for the ufids of this object and the branch, because we want the default
         // insert behavior of using the latest iteration as the branch point on all new versions.
         //
         // The following call to insertNode MUST NOT BE REMOVED.
         // To turn off the "insert out of order" behavior, you should instead set the
         // insert_on_latest_iteration flag to false
         document = (WTDocument) VersionControlHelper.service.insertNode(document,null,null,null,null);
      }
      else {
         // This else is not reachable with the insert flag set to true, this store is now
         // done in the insertNode for both the new object, new iteration, and new version cases.
         document = (WTDocument)PersistenceHelper.manager.store(document);
      }

      if (VERBOSE) {
         String vers_strg = document.getVersionDisplayIdentifier().getLocalizedMessage(WTContext.getContext().getLocale()) + "." +
                            document.getIterationIdentifier().getValue();
         String checkoutstate = WorkInProgressHelper.getState(document).getDisplay();
         LOGGER.debug("New document = " + number + " " + vers_strg + " with state of " +
                            checkoutstate);
      }
      return document;
   }

   private static boolean isNewVersion(WTDocument document, String version) throws WTException {
      if (version == null)
         return false;
      if (version.equals(VersionControlHelper.getVersionIdentifier(document).getValue()))
         return false;
      else
         return true;
   }


   ////////////////////
   // DOCUMENT CACHE //
   ////////////////////

   private static WTDocument getCachedDocument() throws WTException {
      return getCachedDocument(null,null,null);
   }

   private static WTDocument getCachedDocument( String number ) throws WTException {
      return getCachedDocument(number,null,null);
   }

   private static WTDocument getCachedDocument( String number, String version ) throws WTException {
      return getCachedDocument(number,version,null);
   }

   private static WTDocument getCachedDocument( String number, String version, String iteration ) throws WTException {
      return (WTDocument)LoadServerHelper.getCacheValue(getDocumentCacheKey(number,version,iteration));
   }

   private static String getDocumentCacheKey( String number, String version, String iteration ) throws WTException {
      StringBuffer key = new StringBuffer(DOCUMENT_CACHE_KEY);
      if(number!=null) {
         key.append(number.toUpperCase());
         if(version!=null) {
            key.append("|").append(version);
            if(iteration!=null) {
               key.append("|").append(iteration);
            }
         }
      }
      return key.toString();
   }

   private static WTDocument cacheDocument( WTDocument document ) throws WTException {

      if(document==null) {
         LoadServerHelper.removeCacheValue(getDocumentCacheKey(null,null,null));
         LoadServerHelper.removeCacheValue(CURRENT_CONTENT_HOLDER);
         LoadValue.establishCurrentIBAHolder(null);
         LoadAttValues.establishCurrentTypeManaged(null);
         //KEPT FOR LEGACY SUPPORT
         LoadServerHelper.removeCacheValue(CURRENT_DOCUMENT);
      }
      else {
         String number = document.getNumber();
         String version = VersionControlHelper.getVersionIdentifier(document).getValue();
         String iteration = VersionControlHelper.getIterationIdentifier(document).getValue();

         LoadServerHelper.setCacheValue(getDocumentCacheKey(null,null,null),document);
         LoadServerHelper.setCacheValue(getDocumentCacheKey(number,null,null),document);
         LoadServerHelper.setCacheValue(getDocumentCacheKey(number,version,null),document);
         LoadServerHelper.setCacheValue(getDocumentCacheKey(number,version,iteration),document);

         LoadServerHelper.setCacheValue(CURRENT_CONTENT_HOLDER,document);

         //KEPT FOR LEGACY SUPPORT
         LoadServerHelper.setCacheValue(CURRENT_DOCUMENT,document);
      }
      LoadValue.establishCurrentIBAHolder(document);
      LoadAttValues.establishCurrentTypeManaged((TypeManaged)document);
      LoadValue.beginIBAContainer();
      return document;
   }


   //////////////////////////////////////
   // DOCUMENT SPECIFIC HELPER METHODS //
   //////////////////////////////////////

   private static WTDocument applyHardAttributes( WTDocument document, Hashtable nv, Hashtable cmd_line ) throws WTException {

      WTContainerRef containerRef = LoadServerHelper.getTargetContainer( nv, cmd_line );

      setContainer(document,containerRef);

      setType(document,getValue("typedef",nv,cmd_line,false));

      setIteration(document,getValue("iteration",nv,cmd_line,false));

      // Proj.14220950: load SecurityLabels attribute
      setSecurityLabels(document, getValue("securityLabels", nv, cmd_line, false));

      setTitle(document,getValue("title",nv,cmd_line,false));

      setDescription(document,getValue("description",nv,cmd_line,false));

      setDepartment(document,getValue("department",nv,cmd_line,true));

      setFolder(containerRef,document,getValue("saveIn",nv,cmd_line,true));

      setLifeCycle(containerRef,document,getValue("lifecycletemplate",nv,cmd_line,false));

      setTeamTemplate(containerRef,document,getValue("teamTemplate",nv,cmd_line,false),getValue("domain",nv,cmd_line,false));

      setState(document,getValue("lifecyclestate",nv,cmd_line,false));

      setVersion(document,getValue("version",nv,cmd_line,false));

      return document;
   }

   private static WTDocument applyConstructionTimeAttributes( WTDocument document, Hashtable nv, Hashtable cmd_line ) throws WTException {

      setName(document,getValue("name",nv,cmd_line,true));

      setNumber(document,getValue("number",nv,cmd_line,false));

      setDocType(document,getValue("type",nv,cmd_line,true));

      return document;
   }

   private static void setName( WTDocument the_document, String name ) throws WTException {
      try {
         the_document.setName(name);
      }
      catch( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetName: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   private static void setNumber( WTDocument the_document, String number ) throws WTException {
      try {
         if( number != null ) {
            the_document.setNumber(number);
         }
      }
      catch( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetNumber: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   private static void setDocType( WTDocument the_document, String type ) throws WTException {
      try {
         try {
            the_document.setDocType(DocumentType.toDocumentType(type));
         }
         catch( wt.util.WTInvalidParameterException ipe ) {
            try {
               the_document.setDocType(DocumentType.toDocumentType("$$"+type));
            }
            catch( wt.util.WTInvalidParameterException ipe2 ) {
               throw new WTException("Unknown document type <" + type + ">");
            }
         }
      }
      catch( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetDocType: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   private static void setTitle( WTDocument the_document, String title ) throws WTException {
      try {
         the_document.setTitle(title);
      }
      catch( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetTitle: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   private static void setDescription( WTDocument the_document, String description ) throws WTException {
      try {
         if(description!=null) the_document.setDescription(description);
      }
      catch( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetDescription: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   private static void setDepartment( WTDocument the_document, String department ) throws WTException {
      try {
         the_document.setDepartment(DepartmentList.toDepartmentList(department));
      }
      catch( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetDepartment: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   private static WTDocument setPrimaryContent( WTDocument document, Hashtable nv, Hashtable cmd_line, Vector return_objects ) throws WTException {
      LoadServerHelper.setCacheValue(CURRENT_CONTENT_HOLDER,document);
      String file_path = getValue("path",nv,cmd_line,false);
      if(file_path!=null) {
         if(!LoadContent.createPrimary(nv,cmd_line,return_objects)) {
            throw new WTException("LoadDoc - Failed to save content for file_path = " + file_path);
         }
      }
      return (WTDocument)LoadServerHelper.getCacheValue(CURRENT_CONTENT_HOLDER);
   }

   private static void setUser( Hashtable nv, Hashtable cmd_line ) throws WTException {
      LoadServerHelper.setCacheValue(DOCUMENT_PREVIOUS_USER,wt.session.SessionMgr.getPrincipal().getName());
      String user = getValue("user",nv,cmd_line,false);
      if(user!=null) LoadServerHelper.changePrincipal(user);
   }

   private static void resetUser() throws WTException {
      String user = (String)LoadServerHelper.getCacheValue(DOCUMENT_PREVIOUS_USER);
      if(user!=null) LoadServerHelper.changePrincipal(user);
   }

   ////////////////////////////
   // GENERIC HELPER METHODS //
   ////////////////////////////

   // THESE METHODS SHOULD BE KEPT THE SAME AS THOSE FOUND IN wt.doc.LoadDoc
   // (THEY SHOULD REALLY BE MOVED TO A COMMON UTILITY CLASS)

   private static String getValue( String name, Hashtable nv, Hashtable cmd_line, boolean required ) throws WTException {

      String value = LoadServerHelper.getValue(name,nv,cmd_line,required?LoadServerHelper.REQUIRED:LoadServerHelper.NOT_REQUIRED);

      if(required && value == null) throw new WTException("\nRequired value for " + name + " not provided in input file.");

      return value;
   }

   private static void setContainer( WTContained the_contained, WTContainerRef containerRef ) throws WTException {
      try {
         if( containerRef != null ) the_contained.setContainerReference( containerRef );
      }
      catch( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetContainer: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   private static void setType( Typed the_typed, String subtypedef ) throws WTException {
      LoadValue.setType(the_typed,subtypedef);
   }

   private static void setSecurityLabels( SecurityLabeled obj, String securityLabels )
   throws WTException {
      if ((securityLabels!=null) && (securityLabels.length() > 0)) {
         try {
               AccessControlServerHelper.manager.setSecurityLabels(obj, securityLabels, false);
         } catch (WTPropertyVetoException wtpve) {
            throw new WTException (wtpve);
         }
      }
   }

   private static void setVersion( Versioned the_versioned, String version ) throws WTException {
      try {
         if (version == null  ||  version.trim().length() == 0) {
            // If the version ID string is null then the load file did not specify it.
            version = null;
            if (the_versioned.getVersionInfo() != null)
               // If the object already has a VersionInfo object then assume it is correct
               // and no further action is needed.  Otherwise, make a default VersionInfo object.
               return;
         }

         // Get the version series of the object.
         MultilevelSeries mls = null;
         final Mastered master = the_versioned.getMaster();
         if (master != null) {
            final String masterSeriesName = master.getSeries();
            if (masterSeriesName == null) {
               if (the_versioned instanceof WTContained  &&  ((WTContained) the_versioned).getContainer() != null) {
                  // Retrieve the series based on the OIR in effect for the container and object type/soft type.
                  mls = VersionControlHelper.getVersionIdentifierSeries(the_versioned);
                  wt.vc.VersionControlServerHelper.changeSeries(master, mls.getUniqueSeriesName());
               }
            }
            else {
               // Series name was already set in the master, just use it.
               mls = MultilevelSeries.newMultilevelSeries(masterSeriesName);
            }
         }
         if (mls == null) {
            // Unable to get the series from the master, just use the default series.
            mls = MultilevelSeries.newMultilevelSeries("wt.vc.VersionIdentifier",version);
         }

         if (version != null) {
            // Set the revision ID value if it was given in the load file.
            mls.setValueWithoutValidating(version.trim());
         }

         // Replace the default VID object (if there is one) with the correct one.
         VersionIdentifier vid = VersionIdentifier.newVersionIdentifier(mls);
         VersionControlServerHelper.setVersionIdentifier(the_versioned, vid, false /* validateIncreasing */);
      }
      catch ( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetVersion: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
      catch (Exception e) {
         throw new WTException(e);
      }
   }

   private static void setIteration( Iterated the_iterated, String iteration ) throws WTException {
      try {
         if(iteration != null){
            Series ser = Series.newSeries("wt.vc.IterationIdentifier", iteration);
            IterationIdentifier iid = IterationIdentifier.newIterationIdentifier(ser);
            VersionControlHelper.setIterationIdentifier(the_iterated, iid);
         }
      }
      catch ( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetIteration: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   private static void setFolder( WTContainerRef containerRef, FolderEntry the_folder_entry, String folderpath ) throws WTException {
      if(folderpath!=null) {
         Folder folder;
         try {
            folder = FolderHelper.service.getFolder(folderpath, containerRef);
         }
         catch(FolderNotFoundException e) {
            folder = null;
         }

         if( folder == null ) {
            folder = FolderHelper.service.createSubFolder( folderpath, containerRef );
         }

         FolderHelper.assignLocation(the_folder_entry, folder);
      }
   }

   private static void setLifeCycle(WTContainerRef containerRef, LifeCycleManaged the_lifecycle_managed, String lctemplate ) throws WTException {
      try {
         if(lctemplate!=null) {
            LifeCycleHelper.setLifeCycle(the_lifecycle_managed,LifeCycleHelper.service.getLifeCycleTemplate(lctemplate, containerRef));
         }
      }
      catch ( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetLifeCycle: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   // FOR SOME REASON THIS METHOD IS NOT THE SAME AS IT IS IN wt.part.LoadPart
   // SHOULD THEY BE DIFFERENT???
   private static void setState( LifeCycleManaged the_lifecycle_managed, String state ) throws WTException {
      try {
         if(state!=null) {
            LifeCycleServerHelper.setState(the_lifecycle_managed,State.toState(state));
         }
      }
      catch ( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetState: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   private static void setTeamTemplate( WTContainerRef containerRef, TeamManaged the_team_managed, String teamTemplate, String domain ) throws WTException {
      try {
         if((teamTemplate!=null)&&(domain!=null)) {
            TeamHelper.service.setTeamTemplate(containerRef,the_team_managed,teamTemplate,domain);
         }
      }
      catch ( WTPropertyVetoException e ) {
        LoadServerHelper.printMessage("\nsetTeamTemplate: " + e.getMessage());
        e.printStackTrace();
        throw new WTException(e);
      }
   }

   private static WTDocument clearContent(WTDocument document) throws WTException {
      try {
         document = (WTDocument)ContentHelper.service.getContents(document);
         String number = document.getNumber();
         Vector contents = ContentHelper.getContentListAll(document);
         int num_of_files = contents.size();
         if (VERBOSE) LOGGER.debug("Removing " + num_of_files + " content items from " + number);
         for (int i = 0; i < num_of_files; i++) {
            ContentItem ci = (ContentItem)contents.elementAt(i);
            if (VERBOSE) {
               if (ci instanceof ApplicationData)
                  LOGGER.debug("Removing file " + ((ApplicationData)ci).getFileName());
               else
                  LOGGER.debug("Removing content item, but not a file " + ci.getDescription());
            }
            ContentServerHelper.service.deleteContent(document,ci,true);
         }
         return document;
      }
      catch (WTException wte) {
         throw new WTException(wte);
      }
      catch (WTPropertyVetoException wtpve) {
         throw new WTException(wtpve);
      }
      catch (PropertyVetoException pve) {
         throw new WTException(pve);
      }
   }
   private static WTDocumentUsageLink getUsageLink(WTDocument parent,WTDocumentMaster child_master) throws WTException {
         if(VERBOSE){
           LOGGER.debug("getUsageLink - parent:" + parent + " child_master:" + child_master);
           LOGGER.debug("This is the getUsageLink query:");
         }
         QueryResult qr = PersistenceHelper.manager.find(WTDocumentUsageLink.class,parent,
                                                         WTDocumentUsageLink.USED_BY_ROLE,child_master);
         if (qr == null || qr.size() == 0) {
            return null;
         }
         else {
            WTDocumentUsageLink link = (WTDocumentUsageLink)qr.nextElement();
            return link;
         }
      }
   // PLURAL MODIFICATIONS START
	public static boolean createDocRepresentation(Hashtable nv, Hashtable cmd_line, Vector return_objects) {
	      try {
	         String[] messageArgs = new String[2];
	         String message;
				LOGGER.debug("*********Document Representation Started*******************");

				String docNumber = getValue("docNumber", nv, cmd_line, false);
				String docVersion = getValue("docVersion", nv, cmd_line, false);
				String docIteration = getValue("docIteration", nv, cmd_line, false);
	         String orgName = getValue("organizationName",nv,cmd_line,false);
	         String orgID = getValue("organizationID",nv,cmd_line,false);
	         LOGGER.debug("*********Processing for representation doc : " + docNumber +":"+ docVersion + ":" + docIteration);


	         if (orgName != null && orgName.equals("")) orgName = null;
	         if (orgID != null && orgID.equals("")) orgID = null;

	       // TODO get Doc instead of Part. create a new method like below
			WTDocument docObject = getDoc(docNumber, docVersion, docIteration);
			if( docObject != null ) {	LOGGER.debug(
					"*********Document Object found in Windchill: " + docObject.getNumber() + ", Document Name : " + docObject.getName());
		  } if( docObject == null ) {
	            messageArgs[0] = getDisplayInfo(nv,cmd_line);
				message = WTMessage.getLocalizedMessage(DOCRESOURCE, docResource.LOAD_NO_DOCUMENT_REPRESENTATION,
						messageArgs);
	            LoadServerHelper.printMessage(message);
	         }
	         else {
	            String inDir = getValue("repDirectory",nv,cmd_line, true);
	            // The input directory should be relative to WTHOME
	            inDir = WTHOME + DIRSEP + inDir;

	           // TODO check if method exists
	            String poid = getRefFromObject(docObject);
	            boolean republishable = false;
	            String repName = getValue("repName",nv,cmd_line,false);
	            String repDesc = getValue("repDescription",nv,cmd_line,false);
	            boolean repDefault = getBooleanValue("repDefault",nv,cmd_line, false, false);
	            boolean createThumbnail = getBooleanValue("repCreateThumbnail",nv,cmd_line, false, true);
	            boolean storeEDZ = getBooleanValue("repStoreEdz",nv,cmd_line, false, false);
	            LOGGER.debug("*********Processing for representation : " + repName +":"+ repDesc + ":" + inDir);

	            // This pervents double nodes being shown when performing a dynamic Part Structure launch into PV
	            Vector options = new Vector();

	            options.addElement( "ignoreonmerge=true" );

				Boolean ret = loadRepresentation(inDir, poid, republishable, repName, repDesc, repDefault,
						createThumbnail, storeEDZ, options);

	            messageArgs[0] = repName;
	            messageArgs[1] = docObject.getIdentity();

	            // TODO add proper log messages... change from partResource to customised resource file
	            if ( ret.booleanValue() ) {
	            	message = WTMessage.getLocalizedMessage(DOCRESOURCE, docResource.LOAD_REPRESENTATION_ADDED,
							messageArgs);
	            	LOGGER.debug(message);
	            }
	            else {
	            	message = WTMessage.getLocalizedMessage(DOCRESOURCE, docResource.LOAD_REPRESENTATION_FAILED,
							messageArgs);
	            	LOGGER.debug(message);

	            }
	            //LOGGER.debug(message);
	            return_objects.addElement(message);
	            return ret.booleanValue();
	         }
	      }
	      catch (WTException wte){
	         LoadServerHelper.printMessage("\ncreateDOCRepresentation: " + wte.getLocalizedMessage());
	         wte.printStackTrace();
	      }
	      catch (Exception e) {
	         LoadServerHelper.printMessage("\ncreateDOCRepresentation: " + e.getMessage());
	         e.printStackTrace();
	      }
	      return true;
	   }

		public static WTDocument getDoc(String number, String version, String iteration) throws WTException {

			LatestConfigSpec configSpec = null;
			QuerySpec qs = new QuerySpec(WTDocument.class);
			qs.appendWhere(new SearchCondition(WTDocument.class, "master>number", "=", number.toUpperCase(), false));

			if (version != null) {
				qs.appendAnd();
				qs.appendWhere(
						new SearchCondition(WTDocument.class, "versionInfo.identifier.versionId", "=", version, false));
				if (iteration != null) {
					qs.appendAnd();
					qs.appendWhere(new SearchCondition(WTDocument.class, "iterationInfo.identifier.iterationId", "=",
							iteration, false));
				} else {
					qs.appendAnd();
					qs.appendWhere(new SearchCondition(WTDocument.class, "iterationInfo.latest", "TRUE"));
				}
			} else {
				configSpec = new LatestConfigSpec();
				configSpec.appendSearchCriteria(qs);
			}

			QueryResult qr = PersistenceHelper.manager.find(qs);
if(qr!=null && qr.hasMoreElements()) {
			WTDocument doc = (WTDocument) qr.nextElement();
			return doc;
}
return null;
		}

		protected static String getRefFromObject(Persistable obj) {
			try {
				ReferenceFactory refFactory = new ReferenceFactory();
				return refFactory.getReferenceString(
						ObjectReference.newObjectReference(obj.getPersistInfo().getObjectIdentifier()));
			} catch (Exception var2) {
				return null;
			}
		}

		protected static boolean getBooleanValue(String name, Hashtable nv, Hashtable cmd_line, boolean required,
				boolean defaultValue) throws WTException {
			boolean ret = defaultValue;
			String value = LoadServerHelper.getValue(name, nv, cmd_line, required ? 0 : 1);
			if (required && value == null) {
				throw new WTException("Required value for " + name + " not provided in input file.");
			} else {
				if (value != null) {
					if (value.trim().equalsIgnoreCase("true")) {
						ret = true;
					} else if (value.trim().equalsIgnoreCase("false")) {
						ret = false;
					}
				}

				return ret;
			}
		}

		protected static Boolean loadRepresentation(String directory, String poid, boolean republishable,
				String repName, String repDesc, boolean repDefault, boolean createThumbnail, boolean storeEDZ,
				Vector options) {
			Class[] argTypes = new Class[] { String.class, String.class, Boolean.TYPE, String.class, String.class,
					Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Vector.class };
			Object[] args = new Object[] { directory, poid, republishable, repName, repDesc, repDefault,
					createThumbnail, storeEDZ, options };

			try {
//				Class cls = Class.forName("com.ptc.wvs.server.ui.RepHelper");
//				Method m = cls.getMethod("loadRepresentation", argTypes);
//				return (Boolean) m.invoke((Object) null, args);

				Class cls = Class.forName(REPHELPER_CLASS);
				Method m = cls.getMethod(REPHELPER_METHOD, argTypes);
				return (Boolean) m.invoke(null, args);

			} catch (Exception var13) {
				var13.printStackTrace();
				return Boolean.FALSE;
			}
		}

}
