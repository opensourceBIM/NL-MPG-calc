package org.opensourcebim.mpgcalculation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgGeometry;
import org.opensourcebim.ifccollection.MpgLayer;
import org.opensourcebim.ifccollection.MpgLayerImpl;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.MpgObjectStoreImpl;
import org.opensourcebim.ifccollection.MpgScalingOrientation;
import org.opensourcebim.ifccollection.MpgSpaceImpl;
import org.opensourcebim.nmd.NmdFaseProfiel;
import org.opensourcebim.nmd.NmdFaseProfielImpl;
import org.opensourcebim.nmd.NmdMileuCategorie;
import org.opensourcebim.nmd.NmdProductCard;
import org.opensourcebim.nmd.NmdProductCardImpl;
import org.opensourcebim.nmd.NmdProfileSet;
import org.opensourcebim.nmd.NmdProfileSetImpl;
import org.opensourcebim.nmd.NmdReferenceResources;
import org.opensourcebim.nmd.scaling.NmdLinearScaler;

public class mpgCalculatorTests {

	private MpgCalculator calculator;
	private MpgObjectStoreImpl store;
	private MpgCalculationResults results;

	@Before
	public void setUp() throws Exception {
		calculator = new MpgCalculator();
		store = new MpgObjectStoreImpl();
	}

	@After
	public void tearDown() throws Exception {
		calculator = null;
		results = null;
	}

	@Test
	public void testReturnNotRunStatusWhenCalculatorHasNotBeenRunYet() {
		this.results = calculator.getResults();
		assertEquals(ResultStatus.NotRun, results.getStatus());
	}

	@Test
	public void testReturnNoDataStatusWhenNoObjectModelIsSet() {
		calculator.calculate(1.0);
		this.results = calculator.getResults();
		assertEquals(ResultStatus.NoData, results.getStatus());
	}

	@Test
	public void testReturnIncompleteDataStatusWhenIfcModelIsNotComplete() {
		// no objects are linked to the material which should return an warning
		this.store.addElement("steel");

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}

	@Test
	public void testReturnIncompleteDataStatusWhenNMdDataIsIncomplete() {
		// no material spec is added which should return a warning
		store.addElement("steel");

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}
	
	@Test
	public void testReturnIncompleteDataStatusWhenMpgObjectsAreNotFullyCovered() {
		// adding a profile set that is not a totaalproduct will trigger a warning
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}
	@Test
	public void testReturnSuccessWhenMpgObjectFullyCoveredWithDeelProducten() {
		// adding a profile set that is not a totaalproduct will trigger a warning
		String ifcName = "steel";
		store.addElement(ifcName);
		String name = "steel beam";
		String unit = "m3";
		int category = 1;
		
		NmdProductCardImpl card = new NmdProductCardImpl();
		card.setUnit(unit);
		card.setCategory(category);
		card.setLifetime(1);
		// since we're not adding a totaalproduct we need to cover every CUAS stage individually
		card.addProfileSet(createProfileSet(name, unit, 1));
		card.setIsTotaalProduct(false);
		store.addProductCardToElement(ifcName, card);

		addUnitIfcObjectForElement(ifcName, 1.0, 1.0);

		startCalculations(1.0);
		assertEquals(ResultStatus.Success, results.getStatus());
	}

	@Test
	public void testResultsReturnSuccessStatusWhenCalculationsSucceed() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertEquals(ResultStatus.Success, results.getStatus());
	}

	@Test
	public void testTotalCostCannotBeNaN() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertFalse(results.getTotalCost().isNaN());
	}
	
	@Test
	public void testTotalCostIsNonZeroOnCompleteProduct() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertFalse(results.getTotalCost() == 0);
	}

	@Test
	public void testCategory3DataIncreasesTotalCost() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 3, 1);

		startCalculations(1.0);
		// 30% increase in cost for category 3 data
		assertEquals(1.3 * (double) (getDummyReferences().getMilieuCategorieMapping().size()),
				results.getCostPerLifeCycle("TransportToSite"), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerSquareMeterFloorArea() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		Double totalArea = 10.0;
		addSpace(totalArea);

		startCalculations(1.0);

		assertEquals(results.getTotalCost() / totalArea, results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerOperationYear() {
		Double totalLifeTime = 10.0;
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, totalLifeTime.intValue());
		addSpace(1.0);


		startCalculations(totalLifeTime);

		assertEquals(results.getTotalCost() / totalLifeTime, results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerOperatingYearAndFloorArea() {
		Double factor = 10.0;
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, factor.intValue());
		addSpace(factor); // 10m2 floor
		startCalculations(factor); // 10 years of designlife
		assertEquals(results.getTotalCost() / (factor * factor), results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCostPerMaterialReturnsZeroWhenMaterialNotPresent() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1);

		assertEquals(0, results.getCostPerProductName("dummy material"), 1e-8);
	}

	@Test
	public void testTotalCostPerMaterialReturnsOnlyCostOfRelevantMaterial() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);
		addMaterialWithproductCard("brick", "brick and mortar", "m2", 1, 1);
		addMaterialWithproductCard("brick2", "brick and mortar", "m2", 1, 1);
		
		startCalculations(1);

		assertEquals(results.getTotalCost() / 3.0, results.getCostPerProductName("Stainless Steel"), 1e-8);
	}

	/**
	 * When there are multiple deel producten linked to a single product set the replacement calculations should
	 * be done for the entire product based on the construction profiel that is first encountered 
	 * TODO : why not longest duration? the current approach would result in different scores based on the selection order.
	 */
	@Test
	public void testProductCardWithDeelProductenWillUseReplacementsOfFirstConstructionProfile() {		
		
		store.addElement("Brick Wall");
		NmdProductCardImpl productCard = new NmdProductCardImpl();
		productCard.setUnit("m2");
		productCard.setLifetime(10);
		productCard.setCategory(1);
		productCard.addProfileSet(createProfileSet("bricks", "m2", 10));
		productCard.addProfileSet(createProfileSet("mortar", "m2", 1));
		store.addProductCardToElement("Brick Wall", productCard);
		this.addUnitIfcObjectForElement("Brick Wall", 1.0, 1.0);
		
		startCalculations(10);
		Double totalCostBrickFirst = results.getTotalCost();
		
		store.reset();
		
		// repeat the calculation with mortar added first - different replacement time
		store.addElement("Brick Wall");
		productCard = new NmdProductCardImpl();
		productCard.setUnit("m2");
		productCard.setLifetime(10);
		productCard.setCategory(1);
		productCard.addProfileSet(createProfileSet("mortar", "m2", 1));
		productCard.addProfileSet(createProfileSet("bricks", "m2", 10));

		store.addProductCardToElement("Brick Wall", productCard);
		this.addUnitIfcObjectForElement("Brick Wall", 1.0, 1.0);
		
		startCalculations(10);
		Double totalCostMortarFirst = results.getTotalCost();

		// the first construction element lifespan should be taken for all of the elements
		// changing the order should therefore change the results (bad design?)
		assertFalse((totalCostBrickFirst - totalCostMortarFirst) > 1e-8);
	}

	/**
	 * wrap adding the object store and starting the calculation to reduce code
	 * duplication
	 * 
	 * @param lifecycleduration
	 */
	private void startCalculations(double lifecycleduration) {
		calculator.reset();
		calculator.setObjectStore(store);
		calculator.calculate(lifecycleduration);
		this.results = calculator.getResults();
	}

	private void addMaterialWithproductCard(String ifcMatName, String nmdMatName, String unit, int category, int lifetime) {
		store.addElement(ifcMatName);
		store.addProductCardToElement(ifcMatName, createUnitProductCard(nmdMatName, unit, category, lifetime));
		addUnitIfcObjectForElement(ifcMatName, 1.0, 1.0);
	}
	
	private void addUnitIfcObjectForElement(String ifcMatName, double volume, double area) {
		MpgObjectImpl mpgObject = new MpgObjectImpl(1, UUID.randomUUID().toString(), ifcMatName + " element", "Slab",
				"");
		mpgObject.setGeometry(createDummyGeometry());

		MpgLayer testObject = new MpgLayerImpl(volume, area, ifcMatName, Integer.toString(ifcMatName.hashCode()));
		mpgObject.addLayer(testObject);

		store.addObject(mpgObject);

		store.setObjectForElement(ifcMatName, mpgObject);
	}

	private MpgGeometry createDummyGeometry() {
		MpgGeometry g = new MpgGeometry();
		g.setVolume(5.0);
		g.setFloorArea(1.0);
		g.setDimensions(1.0, 1.0, 5.0);
				
		return g;
	}

	/**
	 * Create some random space with set floor area
	 * 
	 * @param floorArea
	 */
	private void addSpace(Double floorArea) {

		store.addSpace(new MpgSpaceImpl(UUID.randomUUID().toString(), floorArea * 3, floorArea));
	}

	private NmdProductCard createUnitProductCard(String name, String unit, int category, int lifetime) {
		NmdProductCardImpl specs = new NmdProductCardImpl();
		specs.setLifetime(lifetime);
		specs.setDescription(name);
		specs.setCategory(category);
		specs.setUnit(unit);
		specs.addProfileSet(createProfileSet(name, unit, lifetime));

		return specs;
	}

	private NmdProfileSet createProfileSet(String name, String unit, int lifetime) {
		NmdProfileSetImpl spec = new NmdProfileSetImpl();
		
		spec.setProfileLifetime(lifetime);
		spec.addFaseProfiel("TransportToSite", createUnitProfile("TransportToSite"));
		spec.addFaseProfiel("ConstructionAndReplacements", createUnitProfile("ConstructionAndReplacements"));
		spec.addFaseProfiel("Disposal", createUnitProfile("Disposal"));
		spec.addFaseProfiel("Incineration", createUnitProfile("Incineration"));
		spec.addFaseProfiel("Recycling", createUnitProfile("Recycling"));
		spec.addFaseProfiel("Reuse", createUnitProfile("Reuse"));
		spec.addFaseProfiel("OwnDisposalProfile", createUnitProfile("OwnDisposalProfile"));
		spec.addFaseProfiel("TransportForRemoval", createUnitProfile("TransportForRemoval"));
		spec.addFaseProfiel("Operation", createUnitProfile("Operation"));

		spec.setUnit(unit);
		spec.setProfielId(1);
		spec.setIsScalable(true);
		// as a dummy add a scaler that will do no adjustment
		spec.setScaler(new NmdLinearScaler("m", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, 1.0}));
		spec.setName(name);

		return spec;
	}

	private NmdFaseProfiel createUnitProfile(String fase) {
		NmdFaseProfielImpl profile = new NmdFaseProfielImpl(fase, this.getDummyReferences());
		profile.setAll(1.0);
		return profile;
	}

	private NmdReferenceResources getDummyReferences() {
		NmdReferenceResources resources = new NmdReferenceResources();
		HashMap<Integer, NmdMileuCategorie> milieuCats = new HashMap<Integer, NmdMileuCategorie>();
		milieuCats.put(1, new NmdMileuCategorie("AbioticDepletionNonFuel", "kg antimoon", 1.0));
		milieuCats.put(2, new NmdMileuCategorie("AbioticDepletionFuel", "kg antimoon", 1.0));
		milieuCats.put(3, new NmdMileuCategorie("GWP100", "kg CO2", 1.0));
		milieuCats.put(4, new NmdMileuCategorie("ODP", "kg CFC11", 1.0));
		milieuCats.put(5, new NmdMileuCategorie("PhotoChemicalOxidation", "kg etheen", 1.0));
		milieuCats.put(6, new NmdMileuCategorie("Acidifcation", "kg SO2", 1.0));
		milieuCats.put(7, new NmdMileuCategorie("Eutrophication", "kg (PO4)^3-", 1.0));
		milieuCats.put(8, new NmdMileuCategorie("HumanToxicity", "kg 1,4 dichloor benzeen", 1.0));
		milieuCats.put(9, new NmdMileuCategorie("FreshWaterAquaticEcoToxicity", "kg 1,4 dichloor benzeen", 1.0));
		milieuCats.put(10, new NmdMileuCategorie("MarineAquaticEcoToxicity", "kg 1,4 dichloor benzeen", 1.0));
		milieuCats.put(11, new NmdMileuCategorie("TerrestrialEcoToxocity", "kg 1,4 dichloor benzeen", 1.0));
		milieuCats.put(12, new NmdMileuCategorie("TotalRenewableEnergy", "MJ", 1.0));
		milieuCats.put(13, new NmdMileuCategorie("TotalNonRenewableEnergy", "MJ", 1.0));
		milieuCats.put(14, new NmdMileuCategorie("TotalEnergy", "MJ", 1.0));
		milieuCats.put(15, new NmdMileuCategorie("FreshWaterUse", "m3", 1.0));
		resources.setMilieuCategorieMapping(milieuCats);

		HashMap<Integer, String> fasen = new HashMap<Integer, String>();
		fasen.put(1, "productie");
		fasen.put(2, "transport -> bouwplaats");
		fasen.put(3, "bouwfase");
		fasen.put(4, "gebruik van product");
		fasen.put(5, "onderhoud");
		fasen.put(6, "reparatie");
		fasen.put(7, "vervangen");
		fasen.put(8, "opknappen");
		fasen.put(9, "deconstructie of sloop");
		fasen.put(10, "transport -> afval");
		fasen.put(11, "afvalverwerking");
		fasen.put(12, "afvalverwijdering");
		fasen.put(13, "Baten en lasten voorbij de systeemgrenzen");
		resources.setFaseMapping(fasen);

		HashMap<Integer, String> units = new HashMap<Integer, String>();
		units.put(1, "MJ");
		units.put(2, "kWh");
		units.put(3, "kg");
		units.put(4, "kg*jaar");
		units.put(5, "m1");
		units.put(6, "m1*jaar");
		units.put(7, "m2");
		units.put(8, "m2*jaar");
		units.put(9, "m2*km");
		units.put(10, "m2K/W");
		units.put(11, "m2K/W*jaar");
		units.put(12, "m2gbo");
		units.put(13, "m2gbo*jaar");
		units.put(14, "m3");
		units.put(15, "m3*jaar");
		units.put(16, "mm");
		units.put(17, "p");
		units.put(18, "p*jaar");
		units.put(19, "t*km");
		units.put(20, "tkm");
		units.put(21, "onbekend");
		units.put(22, "Samengesteld");
		resources.setUnitMapping(units);
		
		HashMap<Integer, String> cuasCategorien = new HashMap<Integer, String>();
		cuasCategorien.put(1, "Constructie");
		cuasCategorien.put(2, "Uitrusting");
		cuasCategorien.put(1, "Afwerking");
		cuasCategorien.put(1, "Schilderwerk");
		resources.setCuasCategorieMapping(cuasCategorien);
		
		return resources;
	}
}