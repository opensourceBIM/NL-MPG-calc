package org.opensourcebim.mpgcalculation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.bimserver.shared.interfaces.async.AsyncPluginInterface.AddObjectIDMCallback;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectGroup;
import org.opensourcebim.ifccollection.MpgObjectGroupImpl;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.ifccollection.MpgObjectStoreImpl;
import org.opensourcebim.nmd.MaterialSpecification;
import org.opensourcebim.nmd.MaterialSpecificationImpl;
import org.opensourcebim.nmd.MaterialSpecifications;
import org.opensourcebim.nmd.MaterialSpecificationsImpl;
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
		store.addSpace(new MpgObjectImpl(1,1));
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
		this.addUnitGroup("steel");
				
		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}
		
	@Test
	public void testResultsReturnSuccessStatusWhenCalculationsSucceed() {

		addMaterialWithSpec("steel", 1.0);
		addUnitGroup("steel");
		
		startCalculations(1.0);
		assertEquals(ResultStatus.Success, results.getStatus());
	}
	
	@Test
	public void testZeroDistanceToProducerResultsInNoTransportCost() {

		addMaterialWithSpec("steel", 0.0);
		addUnitGroup("steel");
		
		startCalculations(1.0);
		assertEquals(0.0, results.getCostPerLifeCycle(NmdLifeCycleStage.Transport), 1e-8);
	}
	
	@Test
	public void testUnitDistanceToProducerResultsInNonZeroTransportCost() {

		addMaterialWithSpec("steel", 1.0);
		addUnitGroup("steel");
		
		startCalculations(1.0);
		
		assertTrue((double)(results.getCostFactors().size()) > 0);
		assertEquals((double)(results.getCostFactors().size()), results.getCostPerLifeCycle(NmdLifeCycleStage.Transport), 1e-8);
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
	
	private void addMaterialWithSpec(String matName, double producerDistance) {
		store.addMaterial(matName);
		store.setSpecsForMaterial(matName, createUnitSpec(producerDistance));
	}
	
	private void addUnitGroup(String material) {
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "test", material + " element", "Slab", store);
		MpgObject testObject = new MpgObjectImpl(1.0, "steel");
		group.addObject(testObject);
		store.addObjectGroup(group);
	}
	
	private MaterialSpecifications createUnitSpec(double transportDistance) {
		MaterialSpecificationsImpl specs = new MaterialSpecificationsImpl();
		specs.setDistanceToProducer(0.0);
		specs.addSpecification(createDummySpec(1.0));
		return specs;
	}
	
	private MaterialSpecification createDummySpec(double massPerUnit) {
		MaterialSpecificationImpl spec = new MaterialSpecificationImpl();
		try {
			spec.setDisposalRatio(NmdLifeCycleStage.Disposal, 1.0);
			spec.setConstructionLossFactor(0.0);
			spec.setProductLifeTime(1);
			spec.addBasisProfiel(NmdLifeCycleStage.ConstructionAndReplacements, createUnitProfile(NmdLifeCycleStage.ConstructionAndReplacements));
			spec.addBasisProfiel(NmdLifeCycleStage.Disposal, createUnitProfile(NmdLifeCycleStage.Disposal));
			spec.addBasisProfiel(NmdLifeCycleStage.Incineration, createUnitProfile(NmdLifeCycleStage.Incineration));
			spec.addBasisProfiel(NmdLifeCycleStage.Recycling, createUnitProfile(NmdLifeCycleStage.Recycling));
			spec.addBasisProfiel(NmdLifeCycleStage.Production, createUnitProfile(NmdLifeCycleStage.Production));
			spec.addBasisProfiel(NmdLifeCycleStage.Operation, createUnitProfile(NmdLifeCycleStage.Operation));
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