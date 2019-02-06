package org.opensourcebim.nmd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.nmd.scaling.NmdScaler;

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
	public void testGetChildProductsWillReturnEmptyArrayOnNonExistentProductCode() {
		connect();
		
		NmdProductCardImpl card = new NmdProductCardImpl();
		card.setRAWCode("-42");
		assertTrue(0 == db.getChildProductsForProductCard(card).size());
	}
	
	@Test
	public void testGetChildProductsWillGetArrayWithProductSetsOnSuccesfulRetrieval() {
		connect();
		
		NmdProductCardImpl card = new NmdProductCardImpl();
		card.setRAWCode("155");
		assertTrue(0 < db.getChildProductsForProductCard(card).size());
	}
	
	@Test
	public void testCanRetrieveProfileSetsBySingleId() {
		connect();
		List<String> ids = Arrays.asList("19");
		HashMap<Integer, NmdProfileSet> profileSets = db.getProfileSetsByIds(ids);
		assertTrue(0 < profileSets.size());
	}
	
	@Test
	public void testGetAllPRofielSetsForProductCard() {
		connect();
		db.preLoadData();
		fail();
	}
	
	@Test
	public void testpreLoadDataWillReturnAllTotaalAndDeelProducten() {
		connect();
		db.preLoadData();
		
		List<NmdProfileSet> sets = new ArrayList<NmdProfileSet>();
		assertTrue(db.getData().stream().map(pc -> !pc.getIsTotaalProduct()).count() > 0);;
	}
	
	@Test
	public void testCanRetrieveProfileSetsWithScalerInformation() {
		connect();
		List<String> ids = Arrays.asList("9", "19");
		HashMap<Integer, NmdProfileSet> profileSets = db.getProfileSetsByIds(ids);
		assertTrue(2 == profileSets.size());
		Entry<Integer, NmdProfileSet> set = profileSets.entrySet().iterator().next();
		assertTrue(set.getValue().getIsScalable());
		NmdScaler scaler = set.getValue().getScaler();
		Double factor = scaler.scale(400);
		
		assertTrue(0.0 > factor);
	}
		
	@Test
	public void testCanRetrieveFullProductByProductId()
	{
		NmdProductCardImpl product = new NmdProductCardImpl();
		product.setRAWCode("204");
		
		connect();
		assertTrue(db.getTotaalProfielSetForProductCard(product));
		assertTrue(0 < product.getProfileSets().size());
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
	
	@Test
	public void getReferenceResourcesReturnsCuasMapping() {
		connect();
		NmdReferenceResources mappings = db.getReferenceResources();
		// only 5 categories should be in there CUAST (T = Totaal)
		assertTrue(5 == mappings.getCuasCategorieMapping().size()); 
	}
	
	@Test
	public void getReferenceScalingFormulaeReturnsScalingInformation() {
		connect();
		NmdReferenceResources mappings = db.getReferenceResources();
		// only 5 categories should be in there CUAST (T = Totaal)
		assertTrue(0 < mappings.getScalingFormula().size()); 
	}
}
