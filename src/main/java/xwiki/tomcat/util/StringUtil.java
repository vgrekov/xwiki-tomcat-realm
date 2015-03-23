package xwiki.tomcat.util;

public class StringUtil {

	public static boolean isBlank(String str) {
		return str == null || str.trim().equals("");
	}

	private StringUtil() {
		throw new UnsupportedOperationException(
				"Utility class should not be instantiated.");
	}

}
