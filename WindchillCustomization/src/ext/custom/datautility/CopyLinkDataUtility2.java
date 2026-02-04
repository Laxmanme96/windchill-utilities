package ext.custom.datautility;

import com.ptc.core.components.descriptor.ModelContext;
import com.ptc.core.components.factory.dataUtilities.DefaultDataUtility;
import com.ptc.core.components.rendering.guicomponents.AttributeDisplayCompositeComponent;
import com.ptc.core.components.rendering.guicomponents.UrlDisplayComponent;
import com.ptc.core.ui.resources.ComponentMode;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.misc.NmAction;
import com.ptc.netmarkets.util.misc.NmActionServiceHelper;

import wt.doc.WTDocument;
import wt.enterprise.MadeFromLink;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;

public class CopyLinkDataUtility2 extends DefaultDataUtility {

	@SuppressWarnings("deprecation")
	@Override
	public Object getDataValue(String arg0, Object arg1, ModelContext arg2) throws WTException {

		Object superobject = super.getDataValue(arg0, arg1, arg2);
		WTDocument document = null;

		if (arg1 instanceof WTDocument) {
			document = (WTDocument) arg1;
			System.out.println("Document Number: " + document.getNumber());
		}

		if (arg2.getDescriptorMode().equals(ComponentMode.VIEW)) {

			long idA2A2 = document.getPersistInfo().getObjectIdentifier().getId();
			System.out.println("IdA2A2: " + idA2A2);
			System.out.println("View mode: " + arg2.getDescriptorMode());

			QuerySpec qs = new QuerySpec(MadeFromLink.class);
			qs.appendWhere(
					new SearchCondition(MadeFromLink.class, "roleAObjectRef.key.id", SearchCondition.EQUAL, idA2A2));

			QueryResult result = PersistenceServerHelper.manager.query(qs);
			System.out.println("Result:" + result.toString());

			if (result.hasMoreElements()) {

				MadeFromLink link = (MadeFromLink) result.nextElement();

				System.out.println("link" + link);

				WTDocument originalDocument = (WTDocument) link.getRoleBObject();
				System.out.println("Original Document Number: " + originalDocument.getNumber());

				String href = null;
				wt.doc.Document pn = originalDocument;

				try {

					// get URL link from document
					NmOid tgtOid = new NmOid(PersistenceHelper.getObjectIdentifier((Persistable) pn));
					System.out.println("targetOid" + tgtOid);
					NmAction infoPageAction = NmActionServiceHelper.service.getAction(NmAction.Type.OBJECT, "view");
					System.out.println("Info Action" + infoPageAction);

					infoPageAction.setContextObject(tgtOid);
					infoPageAction.setIcon(null);
					href = infoPageAction.getActionUrlExternal();
					System.out.println("Info page action" + infoPageAction);

				} catch (java.lang.Exception e) {
					e.printStackTrace();
					throw new wt.util.WTException(e);
				}

				System.out.println("link: " + href);
				UrlDisplayComponent url = new UrlDisplayComponent();
				url.setLabelForTheLink("Original Document: " + originalDocument.getNumber());
				url.setLink(href);
				System.out.println("link" + url.getLink());
				return url;
			} else {

				AttributeDisplayCompositeComponent displayCompositeComponent = (AttributeDisplayCompositeComponent) superobject;
				displayCompositeComponent.setComponentHidden(true);
				return displayCompositeComponent;
			}
		}

		return superobject;
	}
}
