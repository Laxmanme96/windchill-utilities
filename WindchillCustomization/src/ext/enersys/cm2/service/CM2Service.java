package ext.enersys.cm2.service;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.ptc.core.meta.common.TypeIdentifier;

import wt.change2.WTChangeActivity2;
import wt.change2.WTChangeOrder2;
import wt.fc.ObjectReference;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTHashSet;
import wt.fc.collections.WTKeyedHashMap;
import wt.fc.collections.WTSet;
import wt.lifecycle.State;
import wt.method.RemoteInterface;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

@RemoteInterface
public interface CM2Service {

	// Build v2.1
	public String getChangeTypeString(TypeIdentifier objTI) throws WTException;

	public void addParticipantsOnActivityBasedOnTeamInstance(ObjectReference process, WTObject pbo, String roleStr, String required) throws WTException;

	// DESCRIBED ON 17th JUNE 2021

	public WTKeyedHashMap returnCurrentStateMapOfAllResultingObjects(WTObject pbo) throws WTException;

	public void restoreStateMapOfAllResultingObjects(WTObject pbo, WTKeyedHashMap map) throws WTException;

	public WTHashSet getAllResultingObjects(WTObject changeObj) throws WTException;

	public boolean setStateAllResultingObjects(WTObject pbo, String toState);

	//

	public boolean setStateLatestVerAffectedObjects(WTObject pbo, String toState);

	public Set<String> getDisplayNamesOfStates(Set<String> stateSet);

	// TODO : DESCRIBED ON 7th SEPT 2020

	public WTKeyedHashMap returnCurrentStateMapOfAllAffectedObjects(WTObject pbo) throws WTException;

	public void restoreStateMapOfAllAffectedObjects(WTObject pbo, WTKeyedHashMap map) throws WTException;

	public WTHashSet getAllAffectedObjects(WTObject changeObj) throws WTException;

	public boolean setStateAllAffectedObjects(WTObject pbo, String toState);

	// TODO : DESCRIBED ON 2nd SEPT 2020

	public boolean isEveryObjectInSameState(WTObject pbo) throws WTException;

	public void validateRoleUserNotEmpty(ObjectReference self) throws WTException;

	public void releaseEnerSysObject(WTObject pbo) throws WTException;

	public void addRolesAndParticipantsOnActivity(ObjectReference process, WTObject pbo, String roleUserMap, String required) throws WTException;

	public String selectDesignAuthorityUser(String roleMap);

	// TODO : DESCRIBED ON 31st AUG 2020
	public String getLifeCycleStateFromFirstObj(WTObject pbo) throws WTException;

	public String getLifeCycleDisplayStateFromFirstObj(WTObject pbo) throws WTException;

	public void commentsRequiredTask(WTObject pbo) throws WTException;

	public String getAffectedObjectsName(WTObject pbo) throws WTException;

	public String getAffectedObjectsNumber(WTObject pbo) throws WTException;

	// TODO : ADDED FROM OTHER CLASSES 30th AUG 2020 -- DONE

	public String getReleasedState(String intialState);

	public boolean setStateAffectedObjects(WTObject pbo, String toState);

	public HashSet<String> getSetOfDistinctStates(WTObject pbo);

	public String getInitialState(String currentState);

	public QueryResult getAffectedObjects(WTObject changeObj) throws WTException;

	public String getChangeTypeString(Object changeObj) throws WTException;

	// Jira - 641
	public WTKeyedHashMap returnInitialStateMapOfAllAffectedObjects(WTObject pbo, WTKeyedHashMap map);

	public void restoreStateMapOfAllLatestAffObjects(WTObject pbo, WTKeyedHashMap map);
	
	//Admin CN Build 3.3
	public WTSet getAssociatedCT(@NotNull WTChangeOrder2 changeNotice, @NotNull State state);
	public void copyAllAffectedObjectIntoResultingObject(WTChangeActivity2 changeActivity) throws WTPropertyVetoException;
	public WTKeyedHashMap returnCurrentStateMapOfNewlyAddedAffectedObjects(WTObject pbo) throws WTException;
	public WTKeyedHashMap returnInitialStateMapOfNewlyAddedAffectedObjects(WTObject pbo, WTKeyedHashMap map);
	public void restoreStateMapOfNewlyAddedAllAffectedObjects(WTObject pbo, WTKeyedHashMap map) throws WTException;
	public void setStateOfAssociatedCT(@NotNull WTChangeOrder2 changeNotice, @NotNull String toState);
	
	// #ADO:14768
	public HashSet<String> getDownStreamPartViews(WTObject changeObj) throws WTException;
	public boolean isECNStandalone(WTObject pbo) throws WTException;
	public boolean checkViewAndContainer(WTObject changeObj) throws WTException;
	public WTHashSet getAllAffectedObjectsForMCNCreation(WTObject changeObj) throws WTException;
	boolean isParticipantInTask(ObjectReference process) throws WTException;
}