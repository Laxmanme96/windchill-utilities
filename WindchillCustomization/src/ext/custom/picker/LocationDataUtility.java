package ext.custom.picker;

/* bcwti
*
* Copyright (c) 2012 Parametric Technology Corporation (PTC). All Rights Reserved.
*
* This software is the confidential and proprietary information of PTC
* and is subject to the terms of a software license agreement. You shall
* not disclose such confidential information and shall use it only in accordance
* with the terms of the license agreement.
*
* ecwti
*/


import java.util.Locale;
import java.util.regex.Matcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ptc.core.components.beans.CreateAndEditWizBean;
import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.AttributeDataUtilityHelper;
import com.ptc.core.components.factory.dataUtilities.AttributeGuiComponentHelper;
import com.ptc.core.components.forms.FolderContextHelper;
import com.ptc.core.components.rendering.GuiComponent;
import com.ptc.core.components.rendering.guicomponents.AttributeInputComponent.InputMode;
import com.ptc.core.components.util.JCADebugHelper;
import com.ptc.core.meta.type.common.TypeInstance;
import com.ptc.netmarkets.util.beans.NmCommandBean;
import com.ptc.windchill.classproxy.WorkPackageClassProxy;
import com.ptc.windchill.enterprise.folder.LocationConstants;
import com.ptc.windchill.enterprise.folder.folderResource;
import com.ptc.windchill.enterprise.folder.dataUtilities.FolderedDataUtility;
import com.ptc.windchill.enterprise.folder.rendering.LocationInputComponent;
import com.ptc.windchill.enterprise.folder.rendering.LocationInputComponentRenderer;

import wt.access.NotAuthorizedException;
import wt.folder.Folder;
import wt.folder.FolderHelper;
import wt.inf.container.WTContainerRef;
import wt.util.WTException;
import wt.util.WTMessage;
import wt.util.WTRuntimeException;

/**
 * Setup the location picker to allow choosing from any PDMLink container.
 *
 * <BR>
 * <BR>
 * <B>Supported API: </B>false <BR>
 * <BR>
 * <B>Extendable: </B>false
 */
public class LocationDataUtility extends FolderedDataUtility {

  private static final Logger log = LogManager.getLogger(LocationDataUtility.class.getName());
  private static final String FOLDER_FORM_PROCESSOR_DELEGATE = "com.ptc.windchill.enterprise.folder.LocationPropertyProcessor";

  @Override
  protected GuiComponent createInputComponent(String component_id, Object datum, ModelContext mc) throws WTException {

    GuiComponent comp = createCustomInputComponent(component_id, datum, mc);

    LocationInputComponentRenderer renderer = new LocationInputComponentRenderer();
    renderer.setContainerVisibilityMask("PDMLink"); // PickerUtilities.PDMLINK_CONTAINERS_CONTEXT

    LocationInputComponent location = (LocationInputComponent) comp;
    location.setRenderer(renderer);

    return comp;
  }

  protected GuiComponent createCustomInputComponent(String component_id, Object obj, ModelContext mc)
      throws WTException {
    // in create mode, we want to return a modifiable component, initialized
    // appropriately
    // based on the rules service and the set of sub folders based on the seed
    // folder passed in
    // Modifying of the folder for a persisted object is not allowed through the
    // Update action
    // so handling for edit mode is not done.

    log.debug("ENTERING createInputComponent");
    NmCommandBean commandBean = mc.getNmCommandBean();
    if (commandBean.IsJCADebug()) {
      JCADebugHelper.addDataUtilityMessage(
          "Enable TRACE level logging of these log4j loggers to see detailed info on how location was determined/rendered:<br/>"
              + log.getName() + "<br/>" + FolderContextHelper.class.getName());
    }
    // Get the Object Initialization Rules specified folder name to display next to
    // the Auto Select radio button
    Folder oirDefaultFolder = getOIRFolder(mc);

    String oirFolderDisplayString = null;
    if (oirDefaultFolder != null) {
      oirFolderDisplayString = replaceCabinetName(oirDefaultFolder, mc.getLocale());
    }

    // Get the folder to display in the "Select Folder" text field.
    //
    // SPR 2017473
    // There's a permissions issue here. If the user does not have MODIFY access to
    // the
    // folder identified by the launch context, then the target folder will be
    // re-targeted
    // to the default cabinet of the launch context, unless we prohibit that by
    // setting a
    // parameter in the commandBean.
    commandBean.addRequestDataParam("RedirectDefaultToModifiableFolder", "false", false);

    Folder folder = getLaunchContextFolder(mc);

    // SPR 2017473
    // Note: we do not check for MODIFY permission on the folder selected above
    // here.
    // If the user doesn't have Modify permission for the selected folder, this will
    // be
    // trapped when the action is attempted. If we throw a NotAuthorizedException
    // here
    // the create wizards will fail with an ugly traceback. :(

    // If the user does not have READ permission to the container of the obtained
    // folder, we'll
    // get an exception here. Trap it, clear the default folder selection and do not
    // select a
    // default folder (i.e., set 'folder' to null). The user will be forced to
    // manually select
    // a folder in the location picker. If he fails to do so, he'll get a pop-up
    // error saying
    // that a folder must be selected.

    String selectFolderDisplayString = "";
    try {
      selectFolderDisplayString = replaceCabinetName(folder, mc.getLocale());
    } catch (WTRuntimeException wtre) {
      Throwable cause = wtre.getCause();
      log.trace("createInputComponent(): exception while generating folder name string", wtre);
      if (cause instanceof NotAuthorizedException) {
        folder = null;
      }
    }

    log.trace(" -->Auto select folder: " + oirDefaultFolder + " displayName: " + oirFolderDisplayString);
    log.trace(" -->select folder: " + folder + " displayName: " + selectFolderDisplayString);

    // Create the gui component
    String folderContext = "";
    if (folder != null) {
      WTContainerRef containerRef = folder.getContainerReference();
      if (containerRef != null) {
        folderContext = containerRef.toString();
      }
    }

    String locationLabel = WTMessage.getLocalizedMessage("com.ptc.windchill.enterprise.folder.folderResource",
        folderResource.LOCATION_LABEL, null);
    LocationInputComponent comp = new LocationInputComponent(locationLabel, oirFolderDisplayString,
        selectFolderDisplayString, folderContext);
    // All the constraints defined for the folder.id attribute in the OIR (object
    // initialization rules)
    // will be loaded into the attribute type summary by the type instance layer
    // AttributeTypeSummary ats = mc.getATS ();
    comp.setEditable(true);
    comp.setInputMode(AttributeGuiComponentHelper.getInputMode(mc.getDescriptor(), mc.getJCAObject(), mc));
    if (comp.isEditable()) {
      if (!hasCreateAndModifyPermission(obj, mc, oirDefaultFolder)) {
        log.debug(" -->user can't create in the OIR folder, set InputMode to MANUAL to hide the autoSelect option");
        comp.setInputMode(InputMode.MANUAL);
      } else {
        // String rendererConstraint = ats.getRenderer ();
        // the renderer constraint determines which option to select ("Autoselect
        // Folder" or "Select Folder")
        // boolean useGenerated =
        // AttributeTypeSummary.SELECT_GENERATED_FOLDER_BY_DEFAULT.equals
        // (rendererConstraint);
        // log.debug (" -->the renderer constraint: " + rendererConstraint + "
        // useGenerated: " + useGenerated);
        comp.selectAutoOption(true);
      }
    } else {
      /*
       * Location is not editable.
       *
       * If the OIR folder is null, its the case that displays as "(Generated)" and
       * location isn't known until we try to persist. We can't check create/modify
       * permission here; if the user can't create in the generated location they'll
       * get a message after they click the Ok button.
       *
       * If the OIR folder is not null, it will be displayed read only like:
       * 'Location: /GOLF_CART'. We could check create permission here and throw some
       * alert so the user knows they are unable to create in the OIR folder. However,
       * for creates with type pickers shown on the first step, users won't even get
       * to the step with the location picker since the "No Creatable Types Found"
       * message pops up and the wizard is closed.
       *
       * One wizard that doesn't have a type picker, but does have a location picker,
       * is the "New Baseline" client, but currently when the user can't create
       * baselines this message shown:
       * "You do not have the appropriate permissions to add this object to a baseline"
       * which doesn't seem like an appropriate message since its not the
       * "Add to Baseline" client.
       */
      log.debug(" --> location is not editable");
    }

    String rulesFolder = LocationConstants.NO_RULES_FOLDER;
    if (oirDefaultFolder != null) {
      rulesFolder = getFolderOid(oirDefaultFolder);
    }
    comp.addHiddenField(CreateAndEditWizBean.FORM_PROCESSOR_DELEGATE, FOLDER_FORM_PROCESSOR_DELEGATE);
    comp.addHiddenField(LocationConstants.FormParameterKeys.RULES_ENGINE_FOLDER, rulesFolder);
    comp.addHiddenField(LocationConstants.FormParameterKeys.SELECTED_FOLDER, getFolderOid(folder));
    comp.setColumnName(LocationConstants.FormParameterKeys.LOCATION_PICKER);

    comp.setRequired(AttributeDataUtilityHelper.isInputRequired(mc));
    log.debug("EXITING createInputComponent: " + comp);

    return comp;
  }

  // OOTB methods below
  /**
   * Does the current principal have create and modify permission for the type
   * being created in the given Folder?
   *
   * If the given Folder is null, returns true since that is the "(Generated)"
   * case and we can't tell whether the user can create/modify in that location so
   * just assume they can. (If they cannot, we'll find out later.)
   *
   * @param obj
   * @param mc
   * @param folder
   * @return true if the given folder is null or if the user has create and modify
   * permission for the type being created in the given folder, returns false
   * otherwise
   * @throws WTException
   */
  boolean hasCreateAndModifyPermission(Object obj, ModelContext mc, Folder folder) throws WTException {
    boolean result = true;
    if (folder == null) {
      log.debug("this case displays as '(Generated)', location is unknown so just assume user can create/modify");
    } else if (!(obj instanceof TypeInstance)) {
      log.debug("checking if the user has permission to create, datum should always be a type instance?!");
    } else {
      result = FolderContextHelper.hasCreateAndModifyPermission(folder, (TypeInstance) obj);
    }

    log.debug("hasCreateAndModifyPermission returning " + result);
    return result;
  }

  /**
   * Return oid string for a Folder, for example: wt.folder.SubFolder:12345
   *
   * @param folder
   * @return
   */
  String getFolderOid(Folder folder) {
    String oidStr = "";
    if (folder != null) {
      oidStr = folder.getPersistInfo().getObjectIdentifier().getStringValue();
    }
    return oidStr;
  }

  /**
   * Replace a Cabinet name appearing as the root folder of a path with the name
   * of its WTContainer.
   *
   * @param folder
   * @param locale
   * @return
   * @throws WTException
   */
  String replaceCabinetName(Folder folder, Locale locale) throws WTException {
    String folderPath = FolderHelper.getFolderPath(folder);

    if (folderPath != null) {
      if (WorkPackageClassProxy.isWorkPackageFolder(folder)) {
        folderPath = WorkPackageClassProxy.getFolderDisplayName(folder, locale);
      } else {
        folderPath = replaceCabinetName(folder, folderPath);
      }
    }

    return folderPath;
  }

  private static String replaceCabinetName(Folder folder, String folderPath) {
    // SPR 2095959 - quoteReplacement fixes issue when container name contains $
    String replacement = Matcher.quoteReplacement("/" + folder.getContainerName());
    String newPath = folderPath.replaceFirst("\\A/?Default", replacement);
    if (folderPath.equals(newPath)) {
      newPath = folderPath.replaceFirst("\\A/?Agreements", replacement);
    }
    return newPath;
  }

  /**
   * Gets the pregenerated OIR folder for the currently 'active' datum in the
   * given model context.
   *
   * @param mc
   * @return pregenerated OIR folder or null if no pregen folder
   * @throws WTException
   */
  Folder getOIRFolder(ModelContext mc) throws WTException {
    log.debug("ENTERING getOIRFolder");
    Folder oirFolder = FolderContextHelper.getPregeneratedOIRFolder(mc.getJCAObject().getTypeInstance());
    log.debug("EXITING getOIRFolder: " + oirFolder);
    return oirFolder;
  }

}
