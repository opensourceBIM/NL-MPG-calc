package org.opensourcebim.nmd;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class Nmd2DataServiceIntegrationTest {

	private NmdDataService db;

	public Nmd2DataServiceIntegrationTest() {
	}

	@Before
	public void setUp() throws Exception {
		this.connect();
	}

	@After
	public void tearDown() throws Exception {
		db = null;
	}

	public void connect() {
		db = new Nmd2DataService(new UserConfigImpl());
		db.login();
		db.preLoadData();
	}

	@Test
	public void testCanConnect() {
		assertTrue(this.db.getIsConnected());
	}
	
	@Test
	public void testCanPreloadData() {
		assertTrue(this.db.getData().size() > 0);
	}
	
	@Test
	public void testpreLoadDataContainsProductCards() {
		assertTrue(this.db.getData().stream().flatMap(el -> el.getProducts().stream()).count() > 0);
	}
	
	@Test
	public void testLoadDataForProductWillLoadFullProfileSet() {
		NmdProductCard pc = db.getData().get(0).getProducts().iterator().next();
		db.getAdditionalProfileDataForCard(pc);
		assertTrue(pc.getProfileSets().size() > 0);
		HashMap<String, NmdFaseProfiel> fps = pc.getProfileSets().iterator().next().getAllFaseProfielen();
		assertTrue(fps.size() > 0);
		assertTrue(fps.values().stream().allMatch(fp -> fp.getCoefficientSum() > 0.0));
	}
	
}
