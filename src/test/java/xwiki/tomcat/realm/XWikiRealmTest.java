package xwiki.tomcat.realm;

import java.security.Principal;

import junit.framework.TestCase;

public class XWikiRealmTest extends TestCase {

	public void testAuthenticate() {
		XWikiRealm realm = new XWikiRealm();
		realm.setDriverClass(com.mysql.jdbc.Driver.class.getCanonicalName());
		realm.setConnectionUrl("jdbc:mysql://localhost/xwiki");
		realm.setConnectionUsername("xwiki");
		realm.setConnectionPassword("xwiki1");
		Principal principal = realm.authenticate("Admin", "admin");
		assertNotNull(principal);
	}

}
