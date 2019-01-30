package org.opensourcebim.mpgcalculation;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MpgCalculationResultsTests {

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
	public void testCanAddSingleFactorToResults() {
		results.incrementCostFactor(
				new MpgCostFactor("TransportToSite", "AbioticDepletionFuel", 1.0),
				"concrete", "");
		assertEquals(1.0, results.getTotalCost(), 1e-8);
	}

	@Test
	public void testGetResultsByStageReturnsZeroOnNoFactorsFound() {
		results.incrementCostFactor(
				new MpgCostFactor("TransportToSite", "AbioticDepletionFuel", 1.0),
				"concrete", "");
		assertEquals(0.0, results.getCostPerLifeCycle("ConstructionAndReplacements"), 1e-8);
	}

	@Test
	public void testGetResultsByImpcatFactorReturnsZeroOnNoFactorsFound() {
		results.incrementCostFactor(
				new MpgCostFactor("TransportToSite", "AbioticDepletionFuel", 1.0),
				"concrete", "");
		assertEquals(0.0, results.getCostPerImpactFactor("Acidifcation"), 1e-8);
	}

	@Test
	public void testGetResultsByNameReturnsZeroOnNoFactorsFound() {
		results.incrementCostFactor(
				new MpgCostFactor("TransportToSite", "AbioticDepletionFuel", 1.0),
				"concrete", "");
		assertEquals(0.0, results.getCostPerProductName("no concrete"), 1e-8);
	}

	@Test
	public void testCostFactorsAddToOriginalOnDuplicateKey() {
		results.incrementCostFactor(
				new MpgCostFactor("TransportToSite", "AbioticDepletionFuel", 1.0),
				"concrete", "");
		results.incrementCostFactor(
				new MpgCostFactor("TransportToSite", "AbioticDepletionFuel", 1.0),
				"concrete", "");
		assertEquals(2.0, results.getTotalCost(), 1e-8);
	}

	@Test
	public void testCanCollectResultsByStage() {
		addFactorsTestSet();
		assertEquals(3.0, results.getCostPerLifeCycle("TransportToSite"), 1e-8);
	}

	@Test
	public void testCanCollectResultsByFactor() {
		addFactorsTestSet();
		assertEquals(6.0, results.getCostPerImpactFactor("AbioticDepletionFuel"), 1e-8);
	}

	@Test
	public void testCanCollectResultsByMaterialName() {
		addFactorsTestSet();
		assertEquals(12.0, results.getCostPerProductName("steel"), 1e-8);
	}

	private void addFactorsTestSet() {
		results.incrementCostFactor(
				new MpgCostFactor("TransportToSite", "Acidifcation", 1.0), "concrete",
				"");
		results.incrementCostFactor(
				new MpgCostFactor("TransportToSite", "AbioticDepletionFuel", 2.0),
				"brick", "");
		results.incrementCostFactor(
				new MpgCostFactor("Disposal", "AbioticDepletionFuel", 4.0), "steel", "");
		results.incrementCostFactor(new MpgCostFactor("Recycling", "Eutrophication", 8.0),
				"steel", "");
	}

}