package org.opensourcebim.nmd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bytebuddy.asm.Advice.Thrown;

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
		connect();
		assertTrue(db.getIsConnected());
	}
	
	@Test
	public void testCanRetrieveResponseSets() {
		connect();
		assertTrue(4 <= db.getAllProductSets().size());
	}
	
	@Test
	public void testGetChildProductsWillReturnNullOnNonExistenProductCode() {
		connect();
		
		NmdProductCardImpl card = new NmdProductCardImpl();
		card.setRAWCode("-42");
		assertTrue(null == db.getChildProductSetsForProductSet(card));
	}
	
	@Test
	public void testGetChildProductsWillGetArrayWithProductSetsOnSuccesfulRetrieval() {
		connect();
		
		NmdProductCardImpl card = new NmdProductCardImpl();
		card.setRAWCode("155");
		assertTrue(0 < db.getChildProductSetsForProductSet(card).size());
	}
	
	@Test
	public void testCanRetrieveProfileSetsBySingleId() {
		connect();
		List<String> ids = Arrays.asList("19");
		db.getFaseProfielenByIds(ids);
	}
	
	@Test
	public void getReferenceResourcesReturnsProfielFaseMapping() {
		connect();
		NmdReferenceResources mappings = db.getReferenceResources();
		assertTrue(0 < mappings.getFaseMapping().size()); 
	}
	
	@Test
	public void getReferenceResourcesReturnsMilieuCategorienMapping() {
		connect();
		NmdReferenceResources mappings = db.getReferenceResources();
		assertTrue(0 < mappings.getMilieuCategorieMapping().size()); 
	}
	
	@Test
	public void getReferenceResourcesReturnsEenhedenMapping() {
		connect();
		NmdReferenceResources mappings = db.getReferenceResources();
		assertTrue(0 < mappings.getUnitMapping().size()); 
	}
}
