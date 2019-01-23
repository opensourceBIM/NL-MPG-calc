package org.opensourcebim.mpgcalculation;

import static org.junit.Assert.assertEquals;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.MpgObjectStoreImpl;
import org.opensourcebim.ifccollection.MpgSpaceImpl;
import org.opensourcebim.ifccollection.MpgSpace;
import org.opensourcebim.ifccollection.MpgLayerImpl;
import org.opensourcebim.ifccollection.MpgLayer;
import org.opensourcebim.nmd.MaterialSpecification;
import org.opensourcebim.nmd.MaterialSpecificationImpl;
import org.opensourcebim.nmd.NmdBasisProfiel;
import org.opensourcebim.nmd.NmdBasisProfielImpl;
import org.opensourcebim.nmd.NmdProductCard;
import org.opensourcebim.nmd.NmdProductCardImpl;

import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator;


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
		this.store.addMaterial("steel");
		
		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}
	
	@Test
	public void testReturnIncompleteDataStatusWhenNMdDataIsIncomplete() {
		// no material spec is added which should return a warning
		store.addMaterial("steel");
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
	public void testUnitDistanceToProducerResultsInTransportCostForDoubleTheDistance() {
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 1);
		addUnitObject("steel");
		
		startCalculations(1.0);
		// we have added a unit value for every ImpactFactor
		assertEquals(2.0 * (double)(NmdImpactFactor.values().length) / 1000.0,
				results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite), 1e-8);
	}
	
	@Test
	public void testLossFactorOfMaterialInducesExtraTransportCost() {
		double loss = 0.5;
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, loss, 1);
		addUnitObject("steel");
		
		startCalculations(1.0);
		assertEquals(2.0 * (double)(NmdImpactFactor.values().length) * (1.0 + loss) / 1000.0,
				results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite), 1e-8);
	}
	
	@Test
	public void testCategory3DataIncreasesTotalCost() {
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 3);
		addUnitObject("steel");
		
		startCalculations(1.0);
		assertEquals(2.0 * 1.3 * (double)(NmdImpactFactor.values().length) / 1000.0,
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
	public void testTotalCostPerMaterialReturnsZeroWhenMaterialNotPresent()
	{
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 1);
		addUnitObject("steel");
		
		startCalculations(1);

		assertEquals(0, results.getCostPerProductName("dummy material"), 1e-8);
	}
	
	@Test
	public void testTotalCostPerMaterialReturnsOnlyCostOfRelevantMaterial()
	{
		addMaterialWithproductCard("steel", "Stainless Steel", 1.0, 0.0, 1);
		addUnitObject("steel");
		
		addMaterialWithproductCard("brick", "brick and mortar", 1.0, 0.0, 1);
		addUnitObject("brick");
		addUnitObject("brick");
		
		String nmdMatName = store.getMaterialByName("steel").getNmdProductCard().getName();
		
		startCalculations(1);

		assertEquals(results.getTotalCost() / 3.0, results.getCostPerProductName(nmdMatName), 1e-8);
	}
	
	@Test
	public void testDisposalRatioOfZeroWillReturnNoCostForRelevantLifeCycle() {
		addMaterialsWithDisposalProductCard("steel", "Stainless Steel", 1.0,  1);
		addUnitObject("steel");
		
		startCalculations(1);
		
		Double totalCost = results.getTotalCost();
		
		Optional<MaterialSpecification> spec = store.getMaterialByName("steel").getNmdProductCard().getMaterials().stream().findFirst();
		for (Entry<NmdLifeCycleStage, Double> entry : spec.get().GetDisposalRatios().entrySet()) {
			assertEquals(totalCost * entry.getValue(), results.getCostPerLifeCycle(entry.getKey()), 1e-8);
		}
	}
	
	
	@Test
	public void testProductCardWithMultipleMaterialSpecsWillReturnCostBasedOnDensityRatio() {
		// create a product card without transport costs and split out over two material specifications evenly
		store.addMaterial("Brick");
		NmdProductCardImpl productCard = new NmdProductCardImpl();
		productCard.setName("Brick and mortar");
		productCard.setDataCategory(1);
		productCard.setDistanceToProducer(1.0);
		productCard.setTransportProfile(createZeroProfile(NmdLifeCycleStage.TransportToSite));
		// mortar to bricks mass ratio per unit mass is assumed to be 1 to 10
		productCard.addSpecification(createNamedMaterialSpec("bricks", 10.0, 0.0));
		productCard.addSpecification(createNamedMaterialSpec("mortar", 1.0, 0.0));
		store.setProductCardForMaterial("Brick", productCard);
		
		addUnitObject("Brick");
		
		startCalculations(1);
		
		Double totalCost = results.getTotalCost();
		double totalWeight = store.getMaterialByName("Brick").getNmdProductCard().getDensity();
		
		assertEquals(totalCost, results.getCostPerProductName("Brick and mortar"), 1e-8);
		
		for (MaterialSpecification spec : store.getMaterialByName("Brick").getNmdProductCard().getMaterials().stream().collect(Collectors.toList())) {
			Double specCost = results.getCostPerSpecification(spec.getName());
			Double density = productCard.getDensityOfSpec(spec.getName());
			assertEquals(totalCost * density / totalWeight, specCost, 1e-8);
		}

	}
	
	/**
	 * wrap adding the object store and starting the calculation to reduce code duplication
	 * @param lifecycleduration
	 */
	private void startCalculations(double lifecycleduration) {
		calculator.setObjectStore(store);
		calculator.calculate(lifecycleduration);
		this.results = calculator.getResults();
	}
	
	private void addMaterialWithproductCard(String ifcMatName, String nmdMatName, double producerDistance, double lossFactor, int category) {
		store.addMaterial(ifcMatName);
		store.setProductCardForMaterial(ifcMatName, createUnitProductCard(nmdMatName, producerDistance, lossFactor, category));
	}
	
	private void addMaterialsWithDisposalProductCard(String ifcMatName, String nmdMatName, double producerDistance,int category) {
		store.addMaterial(ifcMatName);
		store.setProductCardForMaterial(ifcMatName, createDisposalProductCard(nmdMatName, producerDistance, category));
		
	}
		
	private void addUnitObject(String material) {
		MpgObject mpgObject = new MpgObjectImpl(1, UUID.randomUUID().toString(), material + " element", "Slab", "", store);
		MpgLayer testObject = new MpgLayerImpl(1.0, material, Integer.toString(material.hashCode()));
		mpgObject.addLayer(testObject);
		store.addObject(mpgObject);
	}
	
	/**
	 * Create some random space with set floor area
	 * @param floorArea
	 */
	private void addSpace(Double floorArea) {

		store.addSpace(new MpgSpaceImpl(UUID.randomUUID().toString(), floorArea*3, floorArea));
	}
	
	private NmdProductCard createUnitProductCard(String name, double transportDistance, double lossFactor, int category) {
		NmdProductCardImpl specs = new NmdProductCardImpl();
		specs.setName(name);
		specs.setDataCategory(category);
		specs.setDistanceToProducer(transportDistance);
		specs.setTransportProfile(createUnitProfile(NmdLifeCycleStage.TransportToSite));
		specs.addSpecification(createDummySpec(1.0, lossFactor));
		return specs;
	}
	
	private NmdProductCard createDisposalProductCard(String name, double transportDistance, int category) {
		NmdProductCardImpl specs = new NmdProductCardImpl();
		specs.setName(name);
		specs.setDataCategory(category);
		specs.setDistanceToProducer(transportDistance);
		specs.setTransportProfile(createZeroProfile(NmdLifeCycleStage.TransportToSite));
		specs.addSpecification(createOnlyDisposalSpec(1.0));
		return specs;
	}
	
	private MaterialSpecification createDummySpec(double massPerUnit, double lossFactor) {
		return createNamedMaterialSpec("dummy spec", massPerUnit, lossFactor);
	}
	
	private MaterialSpecification createNamedMaterialSpec(String name, double massPerUnit, double lossFactor) {
		MaterialSpecificationImpl spec = new MaterialSpecificationImpl();
		try {
			spec.setDisposalRatio(NmdLifeCycleStage.Disposal, 1.0);
			spec.setConstructionLossFactor(lossFactor);
			spec.setProductLifeTime(1);
			spec.addBasisProfiel(NmdLifeCycleStage.ConstructionAndReplacements, createUnitProfile(NmdLifeCycleStage.ConstructionAndReplacements));
			spec.addBasisProfiel(NmdLifeCycleStage.Disposal, createUnitProfile(NmdLifeCycleStage.Disposal));
			spec.addBasisProfiel(NmdLifeCycleStage.Incineration, createUnitProfile(NmdLifeCycleStage.Incineration));
			spec.addBasisProfiel(NmdLifeCycleStage.Recycling, createUnitProfile(NmdLifeCycleStage.Recycling));
			spec.addBasisProfiel(NmdLifeCycleStage.Reuse, createUnitProfile(NmdLifeCycleStage.Reuse));
			spec.addBasisProfiel(NmdLifeCycleStage.OwnDisposalProfile, createUnitProfile(NmdLifeCycleStage.OwnDisposalProfile));
			spec.addBasisProfiel(NmdLifeCycleStage.TransportForRemoval, createUnitProfile(NmdLifeCycleStage.TransportForRemoval));
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
	
	private MaterialSpecification createOnlyDisposalSpec(double massPerUnit) {
		MaterialSpecificationImpl spec = new MaterialSpecificationImpl();
		try {
			spec.setDisposalRatio(NmdLifeCycleStage.Disposal, 1.0);
			spec.setConstructionLossFactor(0.0);
			spec.setProductLifeTime(1);
			spec.addBasisProfiel(NmdLifeCycleStage.ConstructionAndReplacements, createZeroProfile(NmdLifeCycleStage.ConstructionAndReplacements));
			spec.addBasisProfiel(NmdLifeCycleStage.Disposal, createUnitProfile(NmdLifeCycleStage.Disposal));
			spec.addBasisProfiel(NmdLifeCycleStage.Incineration, createUnitProfile(NmdLifeCycleStage.Incineration));
			spec.addBasisProfiel(NmdLifeCycleStage.Recycling, createUnitProfile(NmdLifeCycleStage.Recycling));
			spec.addBasisProfiel(NmdLifeCycleStage.Reuse, createUnitProfile(NmdLifeCycleStage.Reuse));
			spec.addBasisProfiel(NmdLifeCycleStage.OwnDisposalProfile, createUnitProfile(NmdLifeCycleStage.OwnDisposalProfile));
			spec.addBasisProfiel(NmdLifeCycleStage.TransportForRemoval, createUnitProfile(NmdLifeCycleStage.TransportForRemoval));
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
	
	private NmdBasisProfiel createUnitProfile(NmdLifeCycleStage stage) {
		return createConstantValueProfile(stage, 1.0);
	}
	
	private NmdBasisProfiel createZeroProfile(NmdLifeCycleStage stage) {
		return createConstantValueProfile(stage, 0.0);
	}
	
	private NmdBasisProfiel createConstantValueProfile(NmdLifeCycleStage stage, Double constantValue) {
		NmdBasisProfielImpl profile = new NmdBasisProfielImpl(stage);
		profile.setAll(constantValue);
		return profile;
	}
}