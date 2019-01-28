package org.opensourcebim.nmd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
		config = new NmdDatabaseConfigImpl();
		db = new NmdDataBaseSession(config);
	}

	@After
	public void tearDown() throws Exception {
		db = null;
	}

	@Test
	public void testDbIsInitiallyNotConnected() {
		assertFalse(db.getIsConnected());
	}
	
	@Test
	public void testDefaultRequestDateIsCurrentDate() {
		assertEquals(new Date(), db.getRequestDate());
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
