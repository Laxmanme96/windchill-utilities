package ext.ptpl.customAction;
 
import java.rmi.RemoteException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.DefaultObjectFormProcessor;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.common.impl.WCTypeIdentifier;
import com.ptc.core.ui.resources.FeedbackType;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.inf.container.WTContainerHelper;
import wt.inf.container.WTContainerRef;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleTemplate;
import wt.type.TypeDefinitionReference;
import wt.type.TypedUtilityServiceHelper;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
 
public class CustomDocFormProcessor extends DefaultObjectFormProcessor {
    @Override
	public FormResult doOperation(NmCommandBean nmCommandBean, List<ObjectBean> objectBean) throws WTException {
 
        super.doOperation(nmCommandBean, objectBean);
 
        StringBuilder message = new StringBuilder();
        FormResult formResult = new FormResult();
 
        Map<String, String> chnagedTextArea = nmCommandBean.getChangedText();
        if (chnagedTextArea.containsValue("setName")) {
        try {
        	TypeIdentifier myCustomType = com.ptc.core.foundation.type.server.impl.TypeHelper.getTypeIdentifier("WCTYPE|wt.doc.WTDocument|com.pluraltech.windchill.PLURALDOC");
        	WCTypeIdentifier myCustomWCType = (WCTypeIdentifier) myCustomType;
        	TypeDefinitionReference myCustomTDR = TypedUtilityServiceHelper.service.getTypeDefinitionReference(myCustomWCType.getTypename());
        	
            WTDocument doc = WTDocument.newWTDocument();
            doc.setTypeDefinitionReference(myCustomTDR);
        	
            String name = chnagedTextArea.get("setName");
            String number = chnagedTextArea.get("setNumber");
            String lcName = chnagedTextArea.get("setlcName");
            message.append("Name: " + name + " | Number: " + number + " | LC: " + lcName);
         
            doc.setNumber(number);
            doc.setName(name);
         
            String container_path = "/wt.inf.container.OrgContainer=demo organization/wt.pdmlink.PDMLinkProduct=GOLF_CART";
            WTContainerRef containerRef = WTContainerHelper.service.getByPath(container_path);
            doc.setContainerReference(containerRef);
         
            String folder_path = "/Default";
            Folder folder = FolderHelper.service.getFolder(folder_path, containerRef);
            FolderHelper.assignLocation((FolderEntry) doc, folder);
         
            LifeCycleTemplate lct = LifeCycleHelper.service.getLifeCycleTemplate(lcName, doc.getContainerReference());
            doc = (WTDocument) LifeCycleHelper.setLifeCycle(doc, lct);
         
            PersistenceHelper.manager.save(doc);
        } catch (WTPropertyVetoException | RemoteException e) {
            e.printStackTrace();
        }
        
        }
        else {
        	return formResult;
        }
		FeedbackMessage fb = new FeedbackMessage(FeedbackType.SUCCESS, Locale.getDefault(), "Doc Created Successfuly",
				null, message.toString());
        formResult.setStatus(FormProcessingStatus.SUCCESS);
        formResult.addFeedbackMessage(fb);
		return formResult;
	}

}