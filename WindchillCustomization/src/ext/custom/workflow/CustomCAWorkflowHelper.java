package ext.custom.workflow;



import java.util.Vector;
import org.apache.logging.log4j.Logger;

import ext.custom.resource.CommonMessagesRB;
import wt.change2.ChangeHelper2;
import wt.change2.ChangeRecord2;
import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeOrder2;
import wt.fc.ObjectReference;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTList;
import wt.lifecycle.Transition;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.type.TypedUtility;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.VersionControlHelper;

/**
 * The class CustomCAWorkflowHelper.
 */
public class CustomCAWorkflowHelper {

	/* Added for WTDEV-664 Stop user in case release target is missing */
	private static final Logger LOGGER = LogR.getLogger(CustomCAWorkflowHelper.class.getName());



	public static void addPRPreviousVersiontoResulting(final WTObject pbo) throws WTException, WTPropertyVetoException {
		System.out.println("---------------------Inside addPRPreviousVersiontoResulting Method Starts-----------------");
		if (pbo instanceof wt.change2.ChangeOrder2) {//We are calling this method from Change Notice WorkFlow
			WTChangeOrder2 ecr = (WTChangeOrder2) pbo;  //so primary Business object is Change Notice

			QueryResult changeActivity = ChangeHelper2.service.getChangeActivities(ecr); //Get All CA from ChangeNotice
			while (changeActivity.hasMoreElements()) { // we can have multiple CA in one CN so we will iterate
				WTChangeActivity2 activity = (WTChangeActivity2) changeActivity.nextElement();
				try {
					final QueryResult changeablesAfter = ChangeHelper2.service.getChangeablesAfter(activity); // Get Resulting Object from CA
					while (changeablesAfter.hasMoreElements()) {
						final Object object = changeablesAfter.nextElement();
						WTList list = new WTArrayList();
						list.add(object);

						WTList changeablesAfter2 = ChangeHelper2.service.getChangeablesAfter(activity, list);
						for (Object o : changeablesAfter2) {
							ObjectReference ref = (ObjectReference) o;
							if (ref.getObject() instanceof ChangeRecord2) {
								ChangeRecord2 record = (ChangeRecord2) ref.getObject();
								if (object instanceof wt.part.WTPart) {
									String transition = record.getTargetTransition().toString();
									System.out.println("----------transition-----------"+transition);
									if ("PRODUCTION_RELEASED".equalsIgnoreCase(transition)) {
										WTPart part1 = (WTPart) record.getRoleBObject();
										String lcType = TypedUtility.getTypeIdentifier(part1).getTypename();
										if (lcType.contains("StandardParts") || (lcType.contains("WTPart") )) {
											String version = part1.getVersionDisplayIdentifier().toString();
											System.out.println("Change object created on Part -------" + part1.getName() + "  " + version);
											QueryResult result = VersionControlHelper.service.allVersionsOf(part1.getMaster());
											final Object latestObject = result.hasMoreElements() ? result.nextElement() : null;
											while (result.hasMoreElements()) {
												WTPart part2 = (WTPart) result.nextElement();
												if (latestObject != null) {
													if (!latestObject.equals(part2) && (!part1.equals(part2))) {
														String strState = part2.getState().toString();
														// If part is released and type=StandardParts , then add into Resulting Object
														// table
														if (strState.equalsIgnoreCase("Approved") || strState.equalsIgnoreCase("RELEASED")) {
															Vector<WTObject> partVec = new Vector<>();
															partVec.addElement(part2);
															partVec = ChangeHelper2.service.storeAssociations(ChangeRecord2.class, activity, partVec);
															wt.change2.ChangeRecord2 resultingData = (wt.change2.ChangeRecord2) partVec
																	.firstElement();
															wt.change2.ChangeHelper2.service.saveChangeRecord(partVec);
															resultingData.setTargetTransition(Transition.toTransition("HISTORY"));
															wt.fc.PersistenceHelper.manager.save(resultingData);
															ecr = (wt.change2.WTChangeOrder2) wt.fc.PersistenceHelper.manager.refresh(ecr);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				} catch (WTException ex) {
					LOGGER.error("CAWorkflowHelper.addPRPreviousVersiontoResulting Exception" + ex.getMessage());
				}
				System.out.println("---------------------Inside addPRPreviousVersiontoResulting Method Ends-----------------");

			}
		}
	}

	
}

