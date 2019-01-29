package org.opensourcebim.mpgcalculation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;
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
import org.opensourcebim.nmd.MaterialSpecificationImpl;
import org.opensourcebim.nmd.NmdFaseProfielImpl;
import org.opensourcebim.nmd.NmdFaseProfiel;
import org.opensourcebim.nmd.NmdProductCard;
import org.opensourcebim.nmd.NmdProductCardImpl;
import org.opensourcebim.nmd.NmdProfileSet;
import org.opensourcebim.nmd.NmdReferenceResources;
import org.opensourcebim.nmd.NmdUnit;

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
		// no objects are linked to the material which shoudl return an warning
		this.store.addElement("steel");

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}

	@Test
	public void testReturnIncompleteDataStatusWhenNMdDataIsIncomplete() {
		// no material spec is added which should return a warning
		store.addElement("steel");
		this.addUnitObject("steel");

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}

	@Test
	public void testResultsReturnSuccessStatusWhenCalculationsSucceed() {
		addMaterialWithproductCard("steel", "Stainless Steel", 0.0, 0.0, 1);
		addUnitObject("steel");

		startCalculations(1.0);
		assertEquals(ResultStatus.Success, results.getStatus());
	}

	@Test
	public void testZeroDistanceToProducerResultsInNoTransportCost() {
		addMaterialWithproductCard("steel", "Stainless Steel", 0.0, 0.0, 1);
		addUnitObject("steel");

		startCalculations(1.0);
		assertEquals(0.0, results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite), 1e-8);
	}

	@Test
	public void testUnitDistanceToProducerResultsInNonZeroTransportCost() {
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 1);
		addUnitObject("steel");

		startCalculations(1.0);
		// we have added a unit value for every ImpactFactor
		assertEquals((double) (getDummyReferences().getMilieuCategorieMapping().size()) / 1000.0,
				results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite), 1e-8);
	}

	@Test
	public void testLossFactorOfMaterialInducesExtraTransportCost() {
		double loss = 0.5;
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, loss, 1);
		addUnitObject("steel");

		startCalculations(1.0);
		assertEquals((double) (getDummyReferences().getMilieuCategorieMapping().size()) * (1.0 + loss) / 1000.0,
				results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite), 1e-8);
	}

	@Test
	public void testCategory3DataIncreasesTotalCost() {
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 3);
		addUnitObject("steel");

		startCalculations(1.0);
		assertEquals(1.3 * (double) (getDummyReferences().getMilieuCategorieMapping().size()) / 1000.0,
				results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerSquareMeterFloorArea() {
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 1);
		addUnitObject("steel");
		Double totalArea = 10.0;
		addSpace(totalArea);

		startCalculations(1.0);

		assertEquals(results.getTotalCost() / totalArea, results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerOperationYear() {
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 1);
		addUnitObject("steel");
		addSpace(1.0);

		Double totalLifeTime = 10.0;
		startCalculations(totalLifeTime);

		assertEquals(results.getTotalCost() / totalLifeTime, results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerOperatingYearAndFloorArea() {
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 1);
		addUnitObject("steel");

		Double factor = 10.0;
		addSpace(factor);
		startCalculations(factor);
		assertEquals(results.getTotalCost() / (factor * factor), results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCostPerMaterialReturnsZeroWhenMaterialNotPresent() {
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 1);
		addUnitObject("steel");

		startCalculations(1);

		assertEquals(0, results.getCostPerProductName("dummy material"), 1e-8);
	}

	@Test
	public void testTotalCostPerMaterialReturnsOnlyCostOfRelevantMaterial() {
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 1);
		addUnitObject("steel");

		addMaterialWithproductCard("brick", "brick and mortar", 1.0, 0.0, 1);
		addUnitObject("brick");
		addUnitObject("brick");

		String nmdMatName = store.getElementByName("steel").getNmdProductCard().getName();

		startCalculations(1);

		assertEquals(results.getTotalCost() / 3.0, results.getCostPerProductName(nmdMatName), 1e-8);
	}

	@Test
	public void testDisposalRatioOfZeroWillReturnNoCostForRelevantLifeCycle() {
		addMaterialsWithDisposalProductCard("steel", "Stainless Steel", 1.0, 1);
		addUnitObject("steel");

		startCalculations(1);

		Double totalCost = results.getTotalCost();

		Optional<NmdProfileSet> spec = store.getElementByName("steel").getNmdProductCard().getProfileSets()
				.stream().findFirst();
		for (Entry<NmdLifeCycleStage, Double> entry : spec.get().getDisposalRatios().entrySet()) {
			assertEquals(totalCost * entry.getValue(), results.getCostPerLifeCycle(entry.getKey()), 1e-8);
		}
	}

	@Test
	public void testMaterialWithEqualDisposalRatiosSplitBetweenTwoDisposalTypes() {
		addMaterialsWithDisposalProductCard("steel", "Stainless Steel", 0.0, 1);
		addUnitObject("steel");

		// set some disposal ratios and leave all other items empty (no production or
		// tranport cost)
		NmdProfileSet mat = store.getElementByName("steel").getNmdProductCard().getProfileSets().iterator()
				.next();
		try {
			mat.setDisposalRatio(NmdLifeCycleStage.Disposal, 0.5);
			mat.setDisposalRatio(NmdLifeCycleStage.Incineration, 0.5);
		} catch (InvalidInputException e) {
			e.printStackTrace();
		}

		startCalculations(1);

		Double totalCost = results.getTotalCost();
		// test that the disposal stages have the correct values.
		assertEquals(0.5 * totalCost, results.getCostPerLifeCycle(NmdLifeCycleStage.Disposal), 1e-8);
		assertEquals(0.5 * totalCost, results.getCostPerLifeCycle(NmdLifeCycleStage.Incineration), 1e-8);
		assertEquals(0.0, results.getCostPerLifeCycle(NmdLifeCycleStage.Recycling), 1e-8);
		assertEquals(0.0, results.getCostPerLifeCycle(NmdLifeCycleStage.Reuse), 1e-8);
	}

	@Test
	public void testMaterialWithZeroDisposalRatiosHasNoDisposalTransportCost() {
		addMaterialWithproductCard("steel", "Stainless Steel", 0.0, 0.0, 1);
		addUnitObject("steel");

		// set some disposal ratios and leave all other items empty (no production or
		// tranport cost)
		NmdProfileSet mat = store.getElementByName("steel").getNmdProductCard().getProfileSets().iterator()
				.next();
		try {
			mat.setDisposalRatio(NmdLifeCycleStage.Disposal, 0.0);
		} catch (InvalidInputException e) {
			e.printStackTrace();
		}

		startCalculations(1);
		assertEquals(0, results.getCostPerLifeCycle(NmdLifeCycleStage.TransportForRemoval), 1e-8);
	}

	@Test
	public void testMaterialWithOnlyReuseHasNoDisposalTransportCost() {
		addMaterialWithproductCard("steel", "Stainless Steel", 0.0, 0.0, 1);
		addUnitObject("steel");

		// set some disposal ratios and leave all other items empty (no production or
		// tranport cost)
		NmdProfileSet mat = store.getElementByName("steel").getNmdProductCard().getProfileSets().iterator()
				.next();
		try {
			mat.setDisposalRatio(NmdLifeCycleStage.Disposal, 0.0);
			mat.setDisposalRatio(NmdLifeCycleStage.Reuse, 1.0);
		} catch (InvalidInputException e) {
			e.printStackTrace();
		}

		startCalculations(1);
		assertTrue(0 < results.getTotalCost());
		assertEquals(0, results.getCostPerLifeCycle(NmdLifeCycleStage.TransportForRemoval), 1e-8);
	}

	@Test
	public void testMaterialWithNonReuseHasNonZeroDisposalTransportCost() {
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 1);
		addUnitObject("steel");

		// set some disposal ratios and leave all other items empty (no production or
		// tranport cost)
		NmdProfileSet mat = store.getElementByName("steel").getNmdProductCard().getProfileSets().iterator()
				.next();

		startCalculations(1);
		double dispTranspCost = results.getCostPerLifeCycle(NmdLifeCycleStage.TransportForRemoval);
		assertTrue(0.0 < dispTranspCost);
	}

	@Test
	public void testProductCardWithMultipleMaterialSpecsWillReturnCostBasedOnDensityRatio() {
		// create a product card without transport costs and split out over two material
		// specifications evenly
		store.addElement("Brick");
		NmdProductCardImpl productCard = new NmdProductCardImpl();
		productCard.setName("Brick and mortar");
		productCard.setDataCategory(1);
		productCard.setDistanceToProducer(1.0);
		productCard.setTransportProfile(createZeroProfile(NmdLifeCycleStage.TransportToSite));
		// mortar to bricks mass ratio per unit mass is assumed to be 1 to 10
		productCard.addProfileSet(createNamedMaterialSpec("bricks", 10.0, 0.0, 1));
		productCard.addProfileSet(createNamedMaterialSpec("mortar", 1.0, 0.0, 1));
		store.setProductCardForElement("Brick", productCard);

		addUnitObject("Brick");

		startCalculations(1);

		Double totalCost = results.getTotalCost();
		double totalWeight = store.getElementByName("Brick").getNmdProductCard().getDensity();

		assertEquals(totalCost, results.getCostPerProductName("Brick and mortar"), 1e-8);

		for (NmdProfileSet spec : store.getElementByName("Brick").getNmdProductCard().getProfileSets().stream()
				.collect(Collectors.toList())) {
			Double specCost = results.getCostPerSpecification(spec.getName());
			Double density = productCard.getDensityOfProfile(spec.getName());
			assertEquals(totalCost * density / totalWeight, specCost, 1e-8);
		}
	}

	@Test
	public void TestCyclicMaintenanceMaterialsDoNotHaveInitialApplication() {
		// create a product card with a regular material
		store.addElement("Paint");
		NmdProductCardImpl productCard = new NmdProductCardImpl();
		productCard.setName("Verflaag");
		productCard.setDataCategory(1);
		productCard.setDistanceToProducer(1.0);
		productCard.addProfileSet(createNamedMaterialSpec("verf", 1.0, 0.0, 5));
		store.setProductCardForElement("Paint", productCard);
		addUnitObject("Paint");
		// paint will last 5 year, so will need to be applied 2 during lifetime
		startCalculations(10);
		double totalCost = results.getTotalCost();
		results.reset();

		store.reset();
		store.addElement("Paint");
		productCard = new NmdProductCardImpl();
		productCard.setName("Verflaag");
		productCard.setDataCategory(1);
		productCard.setDistanceToProducer(1.0);
		NmdProfileSet maintenanceSpec = createNamedMaterialSpec("verf", 1.0, 0.0, 5);
		maintenanceSpec.setIsMaintenanceSpec(true);
		productCard.addProfileSet(maintenanceSpec);
		store.setProductCardForElement("Paint", productCard);
		addUnitObject("Paint");
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
		productCard.setDistanceToProducer(1.0);
		productCard.addProfileSet(createNamedMaterialSpec("verf", 1.0, 0.0, 10));
		store.setProductCardForElement("Paint", productCard);
		addUnitObject("Paint");
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
		productCard.setDistanceToProducer(1.0);
		productCard.addProfileSet(createNamedMaterialSpec("verf", 1.0, 0.0, 10));

		NmdProfileSet maintenanceSpec = createNamedMaterialSpec("verf", 1.0, 0.0, 5);
		maintenanceSpec.setIsMaintenanceSpec(true);
		productCard.addProfileSet(maintenanceSpec);

		store.setProductCardForElement("Paint", productCard);
		addUnitObject("Paint");
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

	private void addMaterialWithproductCard(String ifcMatName, String nmdMatName, double producerDistance,
			double lossFactor, int category) {
		store.addElement(ifcMatName);
		store.setProductCardForElement(ifcMatName,
				createUnitProductCard(nmdMatName, producerDistance, lossFactor, category));
	}

	private void addMaterialsWithDisposalProductCard(String ifcMatName, String nmdMatName, double producerDistance,
			int category) {
		store.addElement(ifcMatName);
		store.setProductCardForElement(ifcMatName, createDisposalProductCard(nmdMatName, producerDistance, category));
	}

	private void addUnitObject(String material) {
		MpgObject mpgObject = new MpgObjectImpl(1, UUID.randomUUID().toString(), material + " element", "Slab", "",
				store);
		MpgLayer testObject = new MpgLayerImpl(1.0, material, Integer.toString(material.hashCode()));
		mpgObject.addLayer(testObject);
		store.addObject(mpgObject);
	}

	/**
	 * Create some random space with set floor area
	 * 
	 * @param floorArea
	 */
	private void addSpace(Double floorArea) {

		store.addSpace(new MpgSpaceImpl(UUID.randomUUID().toString(), floorArea * 3, floorArea));
	}

	private NmdProductCard createUnitProductCard(String name, double transportDistance, double lossFactor,
			int category) {
		NmdProductCardImpl specs = new NmdProductCardImpl();
		specs.setName(name);
		specs.setDataCategory(category);
		specs.setDistanceToProducer(transportDistance);
		specs.setTransportProfile(createUnitProfile(NmdLifeCycleStage.TransportToSite));
		specs.addProfileSet(createDummySpec(1.0, lossFactor));
		return specs;
	}

	private NmdProductCard createDisposalProductCard(String name, double transportDistance, int category) {
		NmdProductCardImpl specs = new NmdProductCardImpl();
		specs.setName(name);
		specs.setDataCategory(category);
		specs.setDistanceToProducer(transportDistance);
		specs.setTransportProfile(createZeroProfile(NmdLifeCycleStage.TransportToSite));
		specs.addProfileSet(createOnlyDisposalSpec(1.0));
		return specs;
	}

	private NmdProfileSet createDummySpec(double massPerUnit, double lossFactor) {
		return createNamedMaterialSpec("dummy spec", massPerUnit, lossFactor, 1);
	}

	private NmdProfileSet createNamedMaterialSpec(String name, double massPerUnit, double lossFactor,
			int lifetime) {
		MaterialSpecificationImpl spec = new MaterialSpecificationImpl();
		try {
			spec.setDisposalRatio(NmdLifeCycleStage.Disposal, 1.0);
			spec.setConstructionLossFactor(lossFactor);
			spec.setProductLifeTime(lifetime);
			spec.addBasisProfiel(NmdLifeCycleStage.ConstructionAndReplacements,
					createUnitProfile(NmdLifeCycleStage.ConstructionAndReplacements));
			spec.addBasisProfiel(NmdLifeCycleStage.Disposal, createUnitProfile(NmdLifeCycleStage.Disposal));
			spec.addBasisProfiel(NmdLifeCycleStage.Incineration, createUnitProfile(NmdLifeCycleStage.Incineration));
			spec.addBasisProfiel(NmdLifeCycleStage.Recycling, createUnitProfile(NmdLifeCycleStage.Recycling));
			spec.addBasisProfiel(NmdLifeCycleStage.Reuse, createUnitProfile(NmdLifeCycleStage.Reuse));
			spec.addBasisProfiel(NmdLifeCycleStage.OwnDisposalProfile,
					createUnitProfile(NmdLifeCycleStage.OwnDisposalProfile));
			spec.addBasisProfiel(NmdLifeCycleStage.TransportForRemoval,
					createUnitProfile(NmdLifeCycleStage.TransportForRemoval));
			spec.addBasisProfiel(NmdLifeCycleStage.Operation, createUnitProfile(NmdLifeCycleStage.Operation));
		} catch (InvalidInputException e) {
			// do nothing as we should be able not to mess it up ourselves
			System.out.println("test input is incorrect.");
		}

		spec.setMassPerUnit(massPerUnit);
		spec.setUnit("kg/m3");
		spec.setCode("1.1.1");
		spec.setName(name);

		return spec;
	}

	private NmdProfileSet createOnlyDisposalSpec(double massPerUnit) {
		MaterialSpecificationImpl spec = new MaterialSpecificationImpl();
		try {
			spec.setDisposalRatio(NmdLifeCycleStage.Disposal, 1.0);
			spec.setConstructionLossFactor(0.0);
			spec.setProductLifeTime(1);
			spec.addBasisProfiel(NmdLifeCycleStage.ConstructionAndReplacements,
					createZeroProfile(NmdLifeCycleStage.ConstructionAndReplacements));
			spec.addBasisProfiel(NmdLifeCycleStage.Disposal, createUnitProfile(NmdLifeCycleStage.Disposal));
			spec.addBasisProfiel(NmdLifeCycleStage.Incineration, createUnitProfile(NmdLifeCycleStage.Incineration));
			spec.addBasisProfiel(NmdLifeCycleStage.Recycling, createUnitProfile(NmdLifeCycleStage.Recycling));
			spec.addBasisProfiel(NmdLifeCycleStage.Reuse, createUnitProfile(NmdLifeCycleStage.Reuse));
			spec.addBasisProfiel(NmdLifeCycleStage.OwnDisposalProfile,
					createUnitProfile(NmdLifeCycleStage.OwnDisposalProfile));
			spec.addBasisProfiel(NmdLifeCycleStage.TransportForRemoval,
					createZeroProfile(NmdLifeCycleStage.TransportForRemoval));
			spec.addBasisProfiel(NmdLifeCycleStage.Operation, createZeroProfile(NmdLifeCycleStage.Operation));
		} catch (InvalidInputException e) {
			// do nothing as we should be able not to mess it up ourselves
			System.out.println("test input is incorrect.");
		}

		spec.setMassPerUnit(massPerUnit);
		spec.setUnit("kg/m3");
		spec.setCode("1.1.1");
		spec.setName("unitMaterialSpec");

		return spec;
	}

	private NmdFaseProfiel createUnitProfile(NmdLifeCycleStage stage) {
		return createConstantValueProfile(stage, 1.0);
	}

	private NmdFaseProfiel createZeroProfile(NmdLifeCycleStage stage) {
		return createConstantValueProfile(stage, 0.0);
	}

	private NmdFaseProfiel createConstantValueProfile(NmdLifeCycleStage stage, Double constantValue) {
		NmdFaseProfielImpl profile = new NmdFaseProfielImpl(stage, NmdUnit.Kg, this.getDummyReferences());
		profile.setAll(constantValue);
		return profile;
	}
	
	private NmdReferenceResources getDummyReferences() {
		NmdReferenceResources resources = new NmdReferenceResources();
		HashMap<Integer, NmdMileuCategorie> milieuCats = new HashMap<Integer, NmdMileuCategorie>();
		milieuCats.put(1, new NmdMileuCategorie("AbioticDepletionNonFuel", "kg antimoon", 1.0 ));
		milieuCats.put(2, new NmdMileuCategorie("AbioticDepletionFuel", "kg antimoon", 1.0 ));
		milieuCats.put(3, new NmdMileuCategorie("GWP100", "kg CO2", 1.0 ));
		milieuCats.put(4, new NmdMileuCategorie("ODP", "kg CFC11", 1.0 ));
		milieuCats.put(5, new NmdMileuCategorie("PhotoChemicalOxidation", "kg etheen", 1.0 ));
		milieuCats.put(6, new NmdMileuCategorie("Acidifcation", "kg SO2", 1.0 ));
		milieuCats.put(7, new NmdMileuCategorie("Eutrophication", "kg (PO4)^3-", 1.0 ));
		milieuCats.put(8, new NmdMileuCategorie("HumanToxicity", "kg 1,4 dichloor benzeen", 1.0 ));
		milieuCats.put(9, new NmdMileuCategorie("FreshWaterAquaticEcoToxicity", "kg 1,4 dichloor benzeen", 1.0 ));
		milieuCats.put(10, new NmdMileuCategorie("MarineAquaticEcoToxicity", "kg 1,4 dichloor benzeen", 1.0 ));
		milieuCats.put(11, new NmdMileuCategorie("TerrestrialEcoToxocity", "kg 1,4 dichloor benzeen", 1.0 ));
		milieuCats.put(12, new NmdMileuCategorie("TotalRenewableEnergy", "MJ", 1.0 ));
		milieuCats.put(13, new NmdMileuCategorie("TotalNonRenewableEnergy", "MJ", 1.0 ));
		milieuCats.put(14, new NmdMileuCategorie("TotalEnergy", "MJ", 1.0 ));
		milieuCats.put(15, new NmdMileuCategorie("FreshWaterUse", "m3", 1.0 ));
		
		resources.setMilieuCategorieMapping(milieuCats);
		return resources;
	}
}