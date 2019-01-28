package org.opensourcebim.nmd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NmdInterfaceTests {

	private NmdDataBaseSession db;
	private NmdDatabaseConfig config;

	public NmdInterfaceTests() {
	}

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {
		db = null;
	}
	
	public void connect() {
		config = new NmdDatabaseConfigImpl();
		db = new NmdDataBaseSession(config);
		db.login();
	}
	
	@Test
	public void testDefaultRequestDateIsCurrentDate() {
		this.connect();
		long now = Calendar.getInstance().getTimeInMillis();
		long req_now = db.getRequestDate().getTimeInMillis();
		// check that the time difference is less than a minute
		assertTrue((now - req_now) / 60000 < 1 );
	}
	
	@Test
	public void testDbIsInitiallyNotLoggedIn() {
		config = new NmdDatabaseConfigImpl();
		db = new NmdDataBaseSession(config);
		assertFalse(db.getIsConnected());
	}
	
	@Test
	public void testDatabaseCannotConnectWithoutCorrectRefreshToken() {
		// recreate the connection and check that the login failed
		NmdDatabaseConfigImpl wrong = new NmdDatabaseConfigImpl();
		wrong.setToken("wrong token");
		
		db = new NmdDataBaseSession(wrong);
		assertFalse(db.getIsConnected());
	}
	
	@Test
	public void testDatabaseCannotConnectWithoutCorrectClientId() {
		// recreate the connection and check that the login failed
		NmdDatabaseConfigImpl wrong = new NmdDatabaseConfigImpl();
		wrong.setClientId(42);
		
		db = new NmdDataBaseSession(wrong);
		assertFalse(db.getIsConnected());
	}
	
	@Test
	public void testDatabaseIsConnected() {
		// the default connection should login as intended
		assertTrue(db.getIsConnected());
	}
}
