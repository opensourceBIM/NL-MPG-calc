package org.opensourcebim.nmd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
		db = new NmdDataBaseSession(config.getToken());
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
}
