package xwiki.tomcat.util;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import xwiki.tomcat.realm.XWikiRealm;

public class DbUtil {

	private static final String XWIKI_PREFIX = "XWiki.";

	private static final String QUERY_SELECT_PASSWORD = " select xwikistrings.XWS_VALUE as PASSWORD "
			+ " from xwikiobjects "
			+ " inner join xwikistrings on xwikistrings.XWS_ID = xwikiobjects.XWO_ID "
			+ " where xwikiobjects.XWO_CLASSNAME = 'XWiki.XWikiUsers' "
			+ " and xwikiobjects.XWO_NAME = ? "
			+ " and xwikistrings.XWS_NAME = 'password'; ";

	private static final String QUERY_SELECT_GROUPS = " select distinct xwikiobjects.XWO_NAME "
			+ " from xwikiobjects "
			+ " inner join xwikistrings on xwikistrings.XWS_ID = xwikiobjects.XWO_ID "
			+ " where xwikiobjects.XWO_CLASSNAME= 'XWiki.XWikiGroups' "
			+ " and xwikistrings.XWS_NAME = 'member' "
			+ " and xwikistrings.XWS_VALUE = ?; ";

	public static Connection openConnection(XWikiRealm realm)
			throws SQLException {
		if (realm.getConnection() != null) {
			return realm.getConnection();
		} else {
			if (realm.getDriver() == null) {
				try {
					Class<?> clazz = Class.forName(realm.getDriverClass());
					realm.setDriver((Driver) clazz.newInstance());
				} catch (Throwable e) {
					realm.warn(String.format(
							"Failed to instantiate driver class %s.",
							realm.getDriverClass()), e);
					throw new SQLException(e.getMessage(), e);
				}
			}

			Properties properties = new Properties();

			if (realm.getConnectionUsername() != null) {
				properties.put("user", realm.getConnectionUsername());
			}

			if (realm.getConnectionPassword() != null) {
				properties.put("password", realm.getConnectionPassword());
			}

			realm.setConnection(realm.getDriver().connect(
					realm.getConnectionUrl(), properties));
			if (realm.getConnection() == null) {
				throw new SQLException(String.format(
						"Failed to connect to '%s' using driver %s.",
						realm.getDriverClass(), realm.getConnectionUrl()));
			}
			realm.getConnection().setAutoCommit(false);

			return realm.getConnection();
		}
	}

	public static void closeConnection(XWikiRealm realm) {
		if (realm.getConnection() != null) {
			try {
				realm.getConnection().close();
			} catch (SQLException e) {
				realm.warn("Failed to close db connection.", e);
			} finally {
				realm.setConnection(null);
			}
		}
	}

	private static void releaseResultSet(XWikiRealm realm, ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				realm.warn("Failed to close result set.", e);
			}
		}
		closeConnection(realm);
	}

	public static String getPassword(XWikiRealm realm, String username) {
		String password = null;
		ResultSet resultSet = null;
		try {
			Connection connection = openConnection(realm);
			PreparedStatement statement = connection
					.prepareStatement(QUERY_SELECT_PASSWORD);
			if (!username.startsWith(XWIKI_PREFIX)) {
				username = XWIKI_PREFIX + username;
			}
			statement.setString(1, username);
			resultSet = statement.executeQuery();

			if (resultSet.next()) {
				password = resultSet.getString(1);
			}

			connection.commit();
		} catch (SQLException e) {
			realm.error(
					String.format("Failed to get password for '%s'.", username),
					e);
		} finally {
			releaseResultSet(realm, resultSet);
		}

		return password;
	}

	public static List<String> getGroups(XWikiRealm realm, String username) {
		List<String> groups;
		ResultSet resultSet = null;
		try {
			Connection connection = openConnection(realm);
			PreparedStatement statement = connection
					.prepareStatement(QUERY_SELECT_GROUPS);
			if (!username.startsWith(XWIKI_PREFIX)) {
				username = XWIKI_PREFIX + username;
			}
			statement.setString(1, username);
			resultSet = statement.executeQuery();

			groups = new ArrayList<String>();
			while (resultSet.next()) {
				String group = resultSet.getString(1);
				if (group.startsWith(XWIKI_PREFIX)) {
					group = group.substring(XWIKI_PREFIX.length());
				}
				groups.add(group);
			}

			connection.commit();
		} catch (SQLException e) {
			realm.error(
					String.format("Failed to get groups for '%s'.", username),
					e);
			groups = Collections.emptyList();
		} finally {
			releaseResultSet(realm, resultSet);
		}

		return groups;
	}

	private DbUtil() {
		throw new UnsupportedOperationException(
				"Utility class should not be instantiated.");
	}

}
