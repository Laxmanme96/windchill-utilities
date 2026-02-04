package ext.enersys.utilities;

import org.apache.logging.log4j.Logger;

import com.ptc.wvs.common.ui.PublishResultImpl;
import com.ptc.wvs.server.publish.Publish;

import wt.log4j.LogR;
import wt.representation.Representation;

public class EnerSysPublishingUtilities {

	private final static Logger LOGGER = LogR.getLoggerInternal(EnerSysPublishingUtilities.class.getName()); 

	private EnerSysPublishingUtilities() {
	}

	public static boolean redoPublishing(Representation rep) {
		boolean ret = false;
		try {
			if (rep != null) {
				LOGGER.debug("Sending republish job to queue");
				PublishResultImpl result = (PublishResultImpl) Publish.republishRepresentation(rep);
				if (result.isSuccessful()) {
					ret = true;
				}
				LOGGER.debug("Sent to queue");
			}
		} catch (Exception e) {
		}
		return ret;
	}
}
