package org.opensourcebim.mpgcalculation;

import static org.junit.Assert.assertEquals;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgSubObject;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.MpgSubObjectImpl;
import org.opensourcebim.ifccollection.MpgObjectStoreImpl;
import org.opensourcebim.nmd.MaterialSpecification;
import org.opensourcebim.nmd.MaterialSpecificationImpl;
import org.opensourcebim.nmd.NmdProductCard;
import org.opensourcebim.nmd.NmdProductCardImpl;
import org.opensourcebim.nmd.NmdBasisProfiel;
import org.opensourcebim.nmd.NmdBasisProfielImpl;


public class mpgCalculatorTests {

	private MpgCalculator calculator;
	private MpgObjectStoreImpl store;
	private MpgCalculationResults results;
	
	@Before
	public void setUp() throws Exception {
		calculator = new MpgCalculator();
		store = new MpgObjectStoreImpl();
		store.addSpace(new MpgSubObjectImpl(1,1));
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
		addMaterialWithproductCard("steel", 0.0, 0.0, 1);
		addUnitObject("steel");
		
		startCalculations(1.0);
		assertEquals(ResultStatus.Success, results.getStatus());
	}
	
	@Test
	public void testZeroDistanceToProducerResultsInNoTransportCost() {
		addMaterialWithproductCard("steel", 0.0, 0.0, 1);
		addUnitObject("steel");
		
		startCalculations(1.0);
		assertEquals(0.0, results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite), 1e-8);
	}
	
	@Test
	public void testUnitDistanceToProducerResultsInTransportCostForDoubleTheDistance() {
		addMaterialWithproductCard("steel", 1.0, 0.0, 1);
		addUnitObject("steel");
		
		startCalculations(1.0);
		// we have added a unit value for every ImpactFactor
		assertEquals(2.0 * (double)(NmdImpactFactor.values().length) / 1000.0,
				results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite), 1e-8);
	}
	
	@Test
	public void testLossFactorOfMaterialInducesExtraTransportCost() {
		double loss = 0.5;
		addMaterialWithproductCard("steel", 1.0, loss, 1);
		addUnitObject("steel");
		
		startCalculations(1.0);
		assertEquals(2.0 * (double)(NmdImpactFactor.values().length) * (1.0 + loss) / 1000.0,
				results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite), 1e-8);
	}
	
	@Test
	public void testCategory3DataIncreasesTotalCost() {
		addMaterialWithproductCard("steel", 1.0, 0.0, 3);
		addUnitObject("steel");
		
		startCalculations(1.0);
		assertEquals(2.0 * 1.3 * (double)(NmdImpactFactor.values().length),
				results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite) / 1000, 1e-8);
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
	
	private void addMaterialWithproductCard(String matName, double producerDistance, double lossFactor, int category) {
		store.addMaterial(matName);
		store.setProductCardForMaterial(matName, createUnitProductCard(producerDistance, lossFactor, category));
	}
		
	private void addUnitObject(String material) {
		MpgObject group = new MpgObjectImpl(1, "test", material + " element", "Slab", "", store);
		MpgSubObject testObject = new MpgSubObjectImpl(1.0, "steel", Integer.toString("steel".hashCode()));
		group.addSubObject(testObject);
		store.addObject(group);
	}
	
	private NmdProductCard createUnitProductCard(double transportDistance, double lossFactor, int category) {
		NmdProductCardImpl specs = new NmdProductCardImpl();
		specs.setDataCategory(category);
		specs.setDistanceToProducer(transportDistance);
		specs.setTransportProfile(createUnitProfile(NmdLifeCycleStage.TransportToSite));
		specs.addSpecification(createDummySpec(1.0, lossFactor));
		return specs;
	}
	
	private MaterialSpecification createDummySpec(double massPerUnit, double lossFactor) {
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
			spec.addBasisProfiel(NmdLifeCycleStage.Production, createUnitProfile(NmdLifeCycleStage.Production));
			spec.addBasisProfiel(NmdLifeCycleStage.Operation, createUnitProfile(NmdLifeCycleStage.Operation));
			spec.addBasisProfiel(NmdLifeCycleStage.CyclicMaintenance, createUnitProfile(NmdLifeCycleStage.CyclicMaintenance));
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
		NmdBasisProfielImpl profile = new NmdBasisProfielImpl(stage);
		profile.setAll(1.0);
		return profile;
	}
}