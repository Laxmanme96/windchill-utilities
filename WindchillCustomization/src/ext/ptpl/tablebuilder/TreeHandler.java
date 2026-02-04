package ext.ptpl.tablebuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.ptc.core.components.beans.TreeHandlerAdapter;
import com.ptc.netmarkets.model.NmOid;
import com.ptc.netmarkets.util.beans.NmCommandBean;

import wt.fc.Persistable;
import wt.fc.collections.WTArrayList;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.util.WTException;
import wt.vc.config.ConfigHelper;
import wt.vc.config.ConfigSpec;

public class TreeHandler extends TreeHandlerAdapter {

	private ConfigSpec configSpec;

	/**
	 * Get the root node from the command bean, and get a config spec based on the
	 * root node
	 **/

	public List getRootNodes() throws WTException {

		return getRootAsParts();
	}

	/**
	 * Get the child parents for the given list of parent parts
	 **/
	public Map<Object, List> getNodes(List parents) throws WTException {

		if (configSpec == null) {
			configSpec = getDefaultConfigSpec();
		}
		// Object - Parent Part , List - Child Parts
		Map<Object, List> result = new HashMap<Object, List>();
		// API returns a 3D array where the 1st dim is the parent parts,
		// the 2nd dim is the list of children for a given parent,
		// and the 3rd dim is 2 element array w/the link obj at 0 and the child part at
		// 1
		Persistable[][][] all_children = WTPartHelper.service.getUsesWTParts(new WTArrayList(parents), configSpec);
		for (ListIterator i = parents.listIterator(); i.hasNext();) {
			WTPart parent = (WTPart) i.next();
			Persistable[][] branch = all_children[i.previousIndex()];
			if (branch == null) {
				continue;
			}
			List children = new ArrayList(branch.length);
			result.put(parent, children);
			for (Persistable[] child : branch) {
				children.add(child[1]);
			}
		}

		return result;
	}

	protected List getRootAsParts() throws WTException {

		List result = new ArrayList();
		/*
		 * WTPart golfCart = getGolfCart(); result.add(golfCart);
		 */
		result = getGolfCart1();

		return result;

	}

	protected List<WTPart> getGolfCart1() throws WTException {
		NmCommandBean cb = getModelContext().getNmCommandBean();
		NmOid oid = cb.getActionOid();
		Object object = oid.getRefObject();
		WTPart parentPart = (WTPart) object;
		System.out.println("Parent Part Number :" + parentPart.getNumber());
		List<WTPart> list = null;
		list = new ArrayList<WTPart>();
		list.add((WTPart) parentPart);
		return list;
	}

	protected ConfigSpec getDefaultConfigSpec() throws WTException {

		return ConfigHelper.service.getDefaultConfigSpecFor(WTPart.class);
	}

}