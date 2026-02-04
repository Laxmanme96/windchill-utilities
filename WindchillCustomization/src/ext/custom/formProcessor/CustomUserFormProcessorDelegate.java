package ext.custom.formProcessor;

/* bcwti
*
*  Copyright (c) 2016 Parametric Technology Corporation (PTC). All Rights
*  Reserved.
*
*  This software is the confidential and proprietary information of PTC.
*  You shall not disclose such confidential information and shall use it
*  only in accordance with the terms of the license agreement.
*
*  ecwti
*/


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.ptc.core.components.beans.ObjectBean;
import com.ptc.core.components.forms.DefaultObjectFormProcessorDelegate;
import com.ptc.core.components.forms.FormProcessingStatus;
import com.ptc.core.components.forms.FormResult;
import com.ptc.core.components.util.FeedbackMessage;
import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.fc.Persistable;
import wt.util.WTException;

/**
*
* @author 
*/
public class CustomUserFormProcessorDelegate extends DefaultObjectFormProcessorDelegate {

	public static final String PACKAGE_CREATOR = "PackageCreator";
 @Override
 public FormResult doOperation(NmCommandBean nmCommandBean, List<ObjectBean> objectBeans) throws WTException {
   FormResult formResult = new FormResult(FormProcessingStatus.SUCCESS);
   try {
     if (objectBeans != null) {
       for (ObjectBean objectBean : objectBeans) {
         Object createdObject = objectBean.getObject();
         String attributeName = null;
       
           attributeName = PACKAGE_CREATOR;
         
         if (attributeName != null) {
           String attributeValue = getUserData(nmCommandBean, attributeName, objectBean.getObjectHandle());
           if (attributeValue != null && !"".equals(attributeValue)) {
             persistValue(createdObject, attributeName, attributeValue);
           }
         }
       }
     }
   } catch (Exception ex) {
     formResult.setStatus(FormProcessingStatus.FAILURE);
     FeedbackMessage feedback = new FeedbackMessage();
     feedback.addMessage(ex.getLocalizedMessage());
     formResult.addFeedbackMessage(feedback);
   }
   return formResult;
 }



 private String getUserData(NmCommandBean cb, String attributeName, String objectHandle) {
   HashMap<?, ?> textParameters = cb.getText();
   Iterator<?> it = textParameters.keySet().iterator();
   while (it.hasNext()) {
     Object key = it.next();
     if (key instanceof String) {
       String keyAsString = (String) key;
       if (keyAsString.contains(attributeName) && keyAsString.contains(objectHandle)) {
         return (String) textParameters.get(key);
       }
     }
   }
   return null;
 }

 private void persistValue(Object object, String attributeName, Object attributeValue) throws WTException {
   if ((attributeValue != null) && (object instanceof Persistable)) {
     PersistableAdapter localPersistableAdapter = new PersistableAdapter((Persistable) object, null, null, null);
     localPersistableAdapter.load(attributeName);
     localPersistableAdapter.set(attributeName, attributeValue);
     localPersistableAdapter.apply();
   }
 }

}

