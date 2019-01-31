package org.opensourcebim.mpgcalculation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgLayer;
import org.opensourcebim.ifccollection.MpgLayerImpl;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.MpgObjectStoreImpl;
import org.opensourcebim.ifccollection.MpgSpaceImpl;
import org.opensourcebim.nmd.NmdFaseProfiel;
import org.opensourcebim.nmd.NmdFaseProfielImpl;
import org.opensourcebim.nmd.NmdProductCard;
import org.opensourcebim.nmd.NmdProductCardImpl;
import org.opensourcebim.nmd.NmdProfileSet;
import org.opensourcebim.nmd.NmdProfileSetImpl;
import org.opensourcebim.nmd.NmdReferenceResources;

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
	public void testResultsReturnSuccessStatusWhenCalculationsSucceed() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1);

		startCalculations(1.0);
		assertEquals(ResultStatus.Success, results.getStatus());
	}

	@Test
	public void testUnitDistanceToProducerResultsInNonZeroTransportCost() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1);

		startCalculations(1.0);
		// we have added a unit value for every ImpactFactor
		assertEquals((double) (getDummyReferences().getMilieuCategorieMapping().size()),
				results.getCostPerLifeCycle("TransportToSite"), 1e-8);
	}

	@Test
	public void testTotalCostCannotBeNaN() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1);

		startCalculations(1.0);
		assertFalse(results.getTotalCost().isNaN());
	}

	@Test
	public void testCategory3DataIncreasesTotalCost() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 3);

		startCalculations(1.0);
		assertEquals(1.3 * (double) (getDummyReferences().getMilieuCategorieMapping().size()),
				results.getCostPerLifeCycle("TransportToSite"), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerSquareMeterFloorArea() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1);

		Double totalArea = 10.0;
		addSpace(totalArea);

		startCalculations(1.0);

		assertEquals(results.getTotalCost() / totalArea, results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerOperationYear() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1);
		addSpace(1.0);

		Double totalLifeTime = 10.0;
		startCalculations(totalLifeTime);

		assertEquals(results.getTotalCost() / totalLifeTime, results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerOperatingYearAndFloorArea() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1);

		Double factor = 10.0;
		addSpace(factor);
		startCalculations(factor);
		assertEquals(results.getTotalCost() / (factor * factor), results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCostPerMaterialReturnsZeroWhenMaterialNotPresent() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1);

		startCalculations(1);

		assertEquals(0, results.getCostPerProductName("dummy material"), 1e-8);
	}

	@Test
	public void testTotalCostPerMaterialReturnsOnlyCostOfRelevantMaterial() {
		addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1);
		addMaterialWithproductCard("brick", "brick and mortar", "m2", 1);
		addMaterialWithproductCard("brick2", "brick and mortar", "m2", 1);
		
		startCalculations(1);

		assertEquals(results.getTotalCost() / 3.0, results.getCostPerProductName("Stainless Steel"), 1e-8);
	}

	@Test
	public void testProductCardWithMultipleMaterialSpecsWillReturnCostBasedOnDensityRatio() {
		// create a product card without transport costs and split out over two material
		// specifications evenly
		store.addElement("Brick");
		NmdProductCardImpl productCard = new NmdProductCardImpl();
		productCard.setName("Brick and mortar");
		productCard.setDataCategory(1);

		// mortar to bricks mass ratio per unit mass is assumed to be 1 to 10
		productCard.addProfileSet(createProfileSet("bricks", "m2", 1, 1));
		productCard.addProfileSet(createProfileSet("mortar", "m2", 1, 1));
		store.setProductCardForElement("Brick", productCard);

		startCalculations(1);

		Double totalCost = results.getTotalCost();

		assertEquals(totalCost, results.getCostPerProductName("Brick and mortar"), 1e-8);

		for (NmdProfileSet profileSet : store.getElementByName("Brick").getNmdProductCard().getProfileSets().stream()
				.collect(Collectors.toList())) {
			Double specCost = results.getCostPerSpecification(profileSet.getName());
			assertEquals(totalCost , specCost, 1e-8);
		}
	}

	@Test
	public void TestCyclicMaintenanceMaterialsDoNotHaveInitialApplication() {
		// create a product card with a regular material
		store.addElement("Paint");
		NmdProductCardImpl productCard = new NmdProductCardImpl();
		productCard.setName("Verflaag");
		productCard.setDataCategory(1);

		productCard.addProfileSet(createProfileSet("verf", "m2", 5, 1));
		store.setProductCardForElement("Paint", productCard);
		// paint will last 5 year, so will need to be applied 2 during lifetime
		startCalculations(10);
		double totalCost = results.getTotalCost();
		results.reset();

		store.reset();
		store.addElement("Paint");
		productCard = new NmdProductCardImpl();
		productCard.setName("Verflaag");
		productCard.setDataCategory(1);

		NmdProfileSet maintenanceSpec = createProfileSet("verf", "m2", 5, 1);
		maintenanceSpec.setIsMaintenanceSpec(true);
		productCard.addProfileSet(maintenanceSpec);
		store.setProductCardForElement("Paint", productCard);
		// as this is a miantenance material it only needs to be applied once (after 5
		// year)
		startCalculations(10);
		double totalMaintenanceCost = results.getTotalCost();

		assertEquals(totalCost, 2 * totalMaintenanceCost, 1e-8);
	}

	@Test
	public void TestCyclicMaintenanceCostIsAddedToRegularCost() {

		// create a product card with a initial paint layer
		store.addElement("Paint");
		NmdProductCardImpl productCard = new NmdProductCardImpl();
		productCard.setName("Verflaag");
		productCard.setDataCategory(1);

		productCard.addProfileSet(createProfileSet("verf", "m2", 10, 1));
		store.setProductCardForElement("Paint", productCard);

		// paint will last 5 year, so will need to be applied 2 during lifetime
		startCalculations(10);
		double totalCost = results.getTotalCost();

		results.reset();
		store.reset();

		// now add a first layer and maintenance for every 5 year
		store.addElement("Paint");
		productCard = new NmdProductCardImpl();
		productCard.setName("Verflaag");
		productCard.setDataCategory(1);

		productCard.addProfileSet(createProfileSet("verf", "m2", 10, 1));

		NmdProfileSet maintenanceSpec = createProfileSet("verf", "m2", 5, 1);
		maintenanceSpec.setIsMaintenanceSpec(true);
		productCard.addProfileSet(maintenanceSpec);

		store.setProductCardForElement("Paint", productCard);
		// as this is a miantenance material it only needs to be applied once (after 5
		// year)
		startCalculations(10);
		double totalCostWithMaintenance = results.getTotalCost();

		// instead of the single paint layer another maintenance layer is applied
		// halfway during is designLife
		assertEquals(totalCostWithMaintenance, 2 * totalCost, 1e-8);
	}

	/**
	 * wrap adding the object store and starting the calculation to reduce code
	 * duplication
	 * 
	 * @param lifecycleduration
	 */
	private void startCalculations(double lifecycleduration) {
		calculator.setObjectStore(store);
		calculator.calculate(lifecycleduration);
		this.results = calculator.getResults();
	}

	private void addMaterialWithproductCard(String ifcMatName, String nmdMatName, String unit, int category) {
		store.addElement(ifcMatName);
		store.setProductCardForElement(ifcMatName, createUnitProductCard(nmdMatName, unit, category));

		MpgObjectImpl mpgObject = new MpgObjectImpl(1, UUID.randomUUID().toString(), ifcMatName + " element", "Slab",
				"", store);
		mpgObject.setArea(1.0);
		mpgObject.setVolume(1.0);

		MpgLayer testObject = new MpgLayerImpl(1.0, 1.0, ifcMatName, Integer.toString(ifcMatName.hashCode()));
		mpgObject.addLayer(testObject);

		store.addObject(mpgObject);

		store.setObjectForElement(ifcMatName, mpgObject);
	}

	/**
	 * Create some random space with set floor area
	 * 
	 * @param floorArea
	 */
	private void addSpace(Double floorArea) {

		store.addSpace(new MpgSpaceImpl(UUID.randomUUID().toString(), floorArea * 3, floorArea));
	}

	private NmdProductCard createUnitProductCard(String name, String unit, int category) {
		NmdProductCardImpl specs = new NmdProductCardImpl();
		specs.setName(name);
		specs.setDataCategory(category);

		specs.addProfileSet(createDummySpec(1.0, unit, category));
		return specs;
	}

	private NmdProfileSet createDummySpec(double massPerUnit, String unit, int category) {
		return createProfileSet("dummy spec", unit, 1, category);
	}

	private NmdProfileSet createProfileSet(String name, String unit, int lifetime, int category) {
		NmdProfileSetImpl spec = new NmdProfileSetImpl();
		spec.setCategory(category);
		spec.setProductLifeTime(lifetime);
		spec.addBasisProfiel("TransportToSite", createUnitProfile("TransportToSite"));
		spec.addBasisProfiel("ConstructionAndReplacements", createUnitProfile("ConstructionAndReplacements"));
		spec.addBasisProfiel("Disposal", createUnitProfile("Disposal"));
		spec.addBasisProfiel("Incineration", createUnitProfile("Incineration"));
		spec.addBasisProfiel("Recycling", createUnitProfile("Recycling"));
		spec.addBasisProfiel("Reuse", createUnitProfile("Reuse"));
		spec.addBasisProfiel("OwnDisposalProfile", createUnitProfile("OwnDisposalProfile"));
		spec.addBasisProfiel("TransportForRemoval", createUnitProfile("TransportForRemoval"));
		spec.addBasisProfiel("Operation", createUnitProfile("Operation"));

		spec.setUnit(unit);
		spec.setProfielId(1);
		spec.setName(name);

		return spec;
	}

	private NmdFaseProfiel createUnitProfile(String fase) {
		return createConstantValueProfile(fase, 1.0);
	}

	private NmdFaseProfiel createConstantValueProfile(String fase, Double constantValue) {
		NmdFaseProfielImpl profile = new NmdFaseProfielImpl(fase, this.getDummyReferences());
		profile.setAll(constantValue);
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

		return resources;
	}
}