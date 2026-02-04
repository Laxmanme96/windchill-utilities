package ext.enersys.utilities;

public interface Debuggable {
	String START = "START ";
	String END = "END ";
	String LINE = " -----~~~~~----- ";

	void checkAndWriteDebug(String prefix, String middle, Object... args);
}
