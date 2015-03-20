package xwiki.tomcat.realm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.List;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;

import xwiki.tomcat.util.DbUtil;

public class XWikiRealm extends RealmBase {

	protected String driverClass;

	protected String connectionUrl;

	protected String connectionUsername;

	protected String connectionPassword;

	protected Driver driver;

	protected Connection connection;

	public String getDriverClass() {
		return driverClass;
	}

	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	public String getConnectionUrl() {
		return connectionUrl;
	}

	public void setConnectionUrl(String connectionUrl) {
		this.connectionUrl = connectionUrl;
	}

	public String getConnectionUsername() {
		return connectionUsername;
	}

	public void setConnectionUsername(String connectionUsername) {
		this.connectionUsername = connectionUsername;
	}

	public String getConnectionPassword() {
		return connectionPassword;
	}

	public void setConnectionPassword(String connectionPassword) {
		this.connectionPassword = connectionPassword;
	}

	public Driver getDriver() {
		return driver;
	}

	public void setDriver(Driver driver) {
		this.driver = driver;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	@Override
	protected String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	protected synchronized String getPassword(String username) {
		if (username == null) {
			return null;
		}

		return DbUtil.getPassword(this, username);
	}

	@Override
	protected synchronized Principal getPrincipal(String username) {
		if (username == null) {
			return null;
		}

		String password = getPassword(username);
		Principal principal = null;
		if (password != null) {
			List<String> roles = DbUtil.getGroups(this, username);
			principal = new GenericPrincipal(username, password, roles);
		}
		return principal;
	}

	@Override
	public synchronized Principal authenticate(String username, String password) {
		if (username == null || password == null) {
			return null;
		}

		String passwordActual = password;
		String passwordExpected = getPassword(username);
		String[] parts = passwordExpected.split(":");
		if (parts.length > 0) {
			String type = parts[0];
			if ("hash".equals(type)) {
				String algorithm = parts[1];
				String salt = parts[2];
				passwordExpected = parts[3];
				passwordActual = calculateHash(password, algorithm, salt);
			}
		}

		if (compareCredentials(passwordActual, passwordExpected)) {
			List<String> roles = DbUtil.getGroups(this, username);
			return new GenericPrincipal(username, password, roles);
		} else {
			return null;
		}
	}

	private String calculateHash(String password, String algorithm, String salt) {
		try {
			String saltedPassword = salt + password;

			MessageDigest hashAlgorithm = MessageDigest.getInstance(algorithm);
			hashAlgorithm.update(saltedPassword.getBytes());
			byte[] digest = hashAlgorithm.digest();

			StringBuilder builder = new StringBuilder();
			for (byte element : digest) {
				int b = element & 0xFF;
				if (b < 0x10) {
					builder.append('0');
				}
				builder.append(Integer.toHexString(b));
			}

			return builder.toString();
		} catch (NoSuchAlgorithmException e) {
			error(String.format("Wrong hash algorithm '%s'.", algorithm), e);
		} catch (Exception e) {
			error("Error hashing password", e);
		}
		return password;
	}

	@Override
	protected void startInternal() throws LifecycleException {
		try {
			DbUtil.openConnection(this);
		} catch (SQLException e) {
			error(this.getClass().getCanonicalName() + ".startInternal()", e);
		}
		super.startInternal();
	}

	@Override
	protected void stopInternal() throws LifecycleException {
		super.stopInternal();
		DbUtil.closeConnection(this);
	}

	public void error(String msg, Throwable error) {
		msg = "XWikiRealm: " + msg;
		containerLog.error(msg, error);
	}

	public void warn(String msg, Throwable error) {
		msg = "XWikiRealm: " + msg;
		containerLog.warn(msg, error);
	}

	public void trace(String msg, Throwable error) {
		msg = "XWikiRealm: " + msg;
		containerLog.trace(msg, error);
	}

	public void debug(String msg, Throwable error) {
		msg = "XWikiRealm: " + msg;
		containerLog.debug(msg, error);
	}

}
