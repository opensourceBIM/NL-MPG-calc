package org.opensourcebim.nmd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.nmd.scaling.NmdScaler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
		assertTrue((now - req_now) / 60000 < 1);
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
		assertTrue(4 <= db.getAllElements().size());
	}

	@Test
	public void testGetChildProductsWillReturnEmptyArrayOnNonExistentProductCode() {
		connect();

		NmdElementImpl el = new NmdElementImpl();
		el.setRawCode("-42");
		assertTrue(0 == db.getProductsForElement(el).size());
	}

	@Test
	public void testGetChildProductsWillGetArrayWithProductSetsOnSuccesfulRetrieval() {
		connect();

		NmdElementImpl el = new NmdElementImpl();
		el.setRawCode("155");
		assertTrue(0 < db.getProductsForElement(el).size());
	}

	@Test
	public void testCanRetrieveProfileSetsBySingleId() {
		connect();
		List<String> ids = Arrays.asList("19");
		HashMap<Integer, NmdProfileSet> profileSets = db.getProfileSetsByIds(ids);
		assertTrue(0 < profileSets.size());
	}

	@Test
	public void testpreLoadDataWillReturnAllTotaalAndDeelProducten() {
		connect();
		db.preLoadData();

		assertTrue(db.getData().stream()
				.flatMap(pc -> pc.getProducts().stream())
				.filter(pc -> !pc.getIsTotaalProduct()).count() > 0);
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

		assertTrue(0.0 < factor);
	}

	@Test
	public void testCanRetrieveFullProductByProductId() {
		NmdElementImpl el = new NmdElementImpl();
		el.setRawCode("204");

		connect();
		assertTrue(db.getProductsForElement(el).size() > 0);
	}

	@Test
	public void getReferenceResourcesReturnsProfielFaseMapping() {
		connect();
		NmdReferenceResources mappings = db.loadReferenceResources();
		assertTrue(0 < mappings.getFaseMapping().size());
	}

	@Test
	public void getReferenceResourcesReturnsMilieuCategorienMapping() {
		connect();
		NmdReferenceResources mappings = db.loadReferenceResources();
		assertTrue(0 < mappings.getMilieuCategorieMapping().size());
	}

	@Test
	public void getReferenceResourcesReturnsEenhedenMapping() {
		connect();
		NmdReferenceResources mappings = db.loadReferenceResources();
		assertTrue(0 < mappings.getUnitMapping().size());
	}

	@Test
	public void getReferenceResourcesReturnsCuasMapping() {
		connect();
		NmdReferenceResources mappings = db.loadReferenceResources();
		// only 5 categories should be in there CUAST (T = Totaal)
		assertTrue(5 == mappings.getCuasCategorieMapping().size());
	}

	@Test
	public void getReferenceScalingFormulaeReturnsScalingInformation() {
		connect();
		NmdReferenceResources mappings = db.loadReferenceResources();
		// only 5 categories should be in there CUAST (T = Totaal)
		assertTrue(0 < mappings.getScalingFormula().size());
	}
	
	@Test
	public void getFullProductCardById() {
		connect();
		Integer id = 9;
		HashMap<Integer, Double> maps = db.getQuantitiesOfProfileSetsForProduct(id);
		assertTrue(maps.size() > 0);
		assertTrue(maps.values().stream().allMatch(v -> v > 0.0));
	}

	@Test
	public void getCompleteDataSet() {
		connect();
		this.db.preLoadData();
		db.getData().forEach(el -> el.getProducts().forEach(pc -> {
			db.getAdditionalProfileDataForCard(pc);
		}));
	
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.valueToTree(db.getData());
		System.out.println("test");
		assertTrue(db.getData().stream()
				.flatMap(e -> e.getProducts().stream()
						.flatMap(pc -> pc.getProfileSets().stream())).count() > 0);
	}
}
