package org.opensourcebim.mpgcalculation;

import static org.junit.Assert.assertEquals;

import javax.management.openmbean.KeyAlreadyExistsException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MpgCalculationResultsTests{

	private MpgCalculationResults results;

	public MpgCalculationResultsTests() {
	}

	@Before
	public void setUp() throws Exception {
		results = new MpgCalculationResults();
	}

	@After
	public void tearDown() throws Exception {
		results = null;
	}

	@Test 
	public void testResultsAreInitiallyNotRun() {
		assertEquals(ResultStatus.NotRun, results.getStatus());
	}
	
	@Test 
	public void testTotalCostIsInitiallyZero() {
		assertEquals(0.0, results.getTotalCost(), 1e-8);
	}
	
	@Test
	public void testCanAddSingleFactorToResults()
	{
		results.addCostFactor(new MpgCostFactor(NmdLifeCycleStage.TransportToSite, NmdImpactFactor.AbioticDepletionFuel, "concrete" , 1.0));
		assertEquals(1.0, results.getTotalCost(), 1e-8);
	}
	
	@Test
	public void testGetResultsByStageReturnsZeroOnNoFactorsFound()
	{
		results.addCostFactor(new MpgCostFactor(NmdLifeCycleStage.TransportToSite, NmdImpactFactor.AbioticDepletionFuel, "concrete" , 1.0));
		assertEquals(0.0, results.getCostPerLifeCycle(NmdLifeCycleStage.ConstructionAndReplacements), 1e-8);
	}
	
	@Test
	public void testGetResultsByImpcatFactorReturnsZeroOnNoFactorsFound()
	{
		results.addCostFactor(new MpgCostFactor(NmdLifeCycleStage.TransportToSite, NmdImpactFactor.AbioticDepletionFuel, "concrete" , 1.0));
		assertEquals(0.0, results.getCostPerImpactFactor(NmdImpactFactor.Acidifcation), 1e-8);
	}
	
	@Test
	public void testGetResultsByNameReturnsZeroOnNoFactorsFound()
	{
		results.addCostFactor(new MpgCostFactor(NmdLifeCycleStage.TransportToSite, NmdImpactFactor.AbioticDepletionFuel, "concrete" , 1.0));
		assertEquals(0.0, results.getCosPerMaterialName("no concrete"), 1e-8);
	}
	
	@Test(expected = KeyAlreadyExistsException.class)
	public void testIndexOutOfBoundsException() {
		results.addCostFactor(new MpgCostFactor(NmdLifeCycleStage.TransportToSite, NmdImpactFactor.AbioticDepletionFuel, "concrete" , 1.0));
		results.addCostFactor(new MpgCostFactor(NmdLifeCycleStage.TransportToSite, NmdImpactFactor.AbioticDepletionFuel, "concrete" , 1.0));
	}
	
	@Test
	public void testCanCollectResultsByStage() {
		addFactorsTestSet();
		assertEquals(3.0, results.getCostPerLifeCycle(NmdLifeCycleStage.TransportToSite), 1e-8);
	}
	
	@Test
	public void testCanCollectResultsByFactor() {
		addFactorsTestSet();
		assertEquals(6.0, results.getCostPerImpactFactor(NmdImpactFactor.AbioticDepletionFuel), 1e-8);
	}
	
	@Test
	public void testCanCollectResultsByMaterialName() {
		addFactorsTestSet();
		assertEquals(12.0, results.getCosPerMaterialName("steel"), 1e-8);
	}
		
	private void addFactorsTestSet() {
		results.addCostFactor(new MpgCostFactor(NmdLifeCycleStage.TransportToSite, NmdImpactFactor.Acidifcation, "concrete" , 1.0));
		results.addCostFactor(new MpgCostFactor(NmdLifeCycleStage.TransportToSite, NmdImpactFactor.AbioticDepletionFuel, "brick" , 2.0));
		results.addCostFactor(new MpgCostFactor(NmdLifeCycleStage.Disposal, NmdImpactFactor.AbioticDepletionFuel, "steel" , 4.0));
		results.addCostFactor(new MpgCostFactor(NmdLifeCycleStage.Recycling, NmdImpactFactor.Eutrophication, "steel" , 8.0));
	}
	
}