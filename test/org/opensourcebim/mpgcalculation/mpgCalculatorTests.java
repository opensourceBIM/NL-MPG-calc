package org.opensourcebim.mpgcalculation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MaterialSource;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.nmd.NmdProductCardImpl;
import org.opensourcebim.nmd.ObjectStoreBuilder;

public class mpgCalculatorTests {

	private MpgCalculator calculator;
	private ObjectStoreBuilder builder;
	private MpgCalculationResults results;

	@Before
	public void setUp() throws Exception {
		calculator = new MpgCalculator();
		builder = new ObjectStoreBuilder();
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
		builder.getStore().addElement("steel");

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}

	@Test
	public void testReturnIncompleteDataStatusWhenNMdDataIsIncomplete() {
		// no material spec is added which should return a warning
		builder.getStore().addElement("steel");

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}
	
	@Test
	public void testReturnIncompleteDataStatusWhenMpgObjectsAreNotFullyCovered() {
		// adding a profile set that is not a totaalproduct will trigger a warning
		builder.addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}
	@Test
	public void testReturnSuccessWhenMpgObjectFullyCoveredWithDeelProducten() {
		// adding a profile set that is not a totaalproduct will trigger a warning
		String ifcName = "steel";
		MpgElement el = builder.getStore().addElement(ifcName);
		
		String name = "steel beam";
		String unit = "m3";
		int category = 1;
		
		NmdProductCardImpl card = new NmdProductCardImpl();
		card.setUnit(unit);
		card.setCategory(category);
		card.setLifetime(1);
		// since we're not adding a totaalproduct we need to cover every CUAS stage individually
		card.addProfileSet(builder.createProfileSet(name, unit, 1));
		card.setIsTotaalProduct(false);
		
		el.mapProductCard(new MaterialSource("1", "steel", "dummy"), card);

		builder.addUnitIfcObjectForElement(ifcName, 1.0, 1.0);

		startCalculations(1.0);
		assertEquals(ResultStatus.Success, results.getStatus());
	}

	@Test
	public void testResultsReturnSuccessStatusWhenCalculationsSucceed() {
		builder.addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertEquals(ResultStatus.Success, results.getStatus());
	}

	@Test
	public void testTotalCostCannotBeNaN() {
		builder.addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertFalse(results.getTotalCost().isNaN());
	}
	
	@Test
	public void testTotalCostIsNonZeroOnCompleteProduct() {
		builder.addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertFalse(results.getTotalCost() == 0);
	}

	@Test
	public void testCategory3DataIncreasesTotalCost() {
		builder.addMaterialWithproductCard("steel", "Stainless Steel", "m2", 3, 1);

		startCalculations(1.0);
		// 30% increase in cost for category 3 data
		assertEquals(1.3 * (double) (builder.getDummyReferences().getMilieuCategorieMapping().size()),
				results.getCostPerLifeCycle("TransportToSite"), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerSquareMeterFloorArea() {
		builder.addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		Double totalArea = 10.0;
		builder.addSpace(totalArea);

		startCalculations(1.0);

		assertEquals(results.getTotalCost() / totalArea, results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerOperationYear() {
		Double totalLifeTime = 10.0;
		builder.addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, totalLifeTime.intValue());
		builder.addSpace(1.0);


		startCalculations(totalLifeTime);

		assertEquals(results.getTotalCost() / totalLifeTime, results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerOperatingYearAndFloorArea() {
		Double factor = 10.0;
		builder.addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, factor.intValue());
		builder.addSpace(factor); // 10m2 floor
		startCalculations(factor); // 10 years of designlife
		assertEquals(results.getTotalCost() / (factor * factor), results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCostPerMaterialReturnsZeroWhenMaterialNotPresent() {
		builder.addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1);

		assertEquals(0, results.getCostPerProductName("dummy material"), 1e-8);
	}

	@Test
	public void testTotalCostPerMaterialReturnsOnlyCostOfRelevantMaterial() {
		builder.addMaterialWithproductCard("steel", "Stainless Steel", "m2", 1, 1);
		builder.addMaterialWithproductCard("brick", "brick and mortar", "m2", 1, 1);
		builder.addMaterialWithproductCard("brick2", "brick and mortar", "m2", 1, 1);
		
		startCalculations(1);

		assertEquals(results.getTotalCost() / 3.0, results.getCostPerProductName("Stainless Steel"), 1e-8);
	}

	/**
	 * wrap adding the object store and starting the calculation to reduce code
	 * duplication
	 * 
	 * @param lifecycleduration
	 */
	private void startCalculations(double lifecycleduration) {
		calculator.reset();
		calculator.setObjectStore(builder.getStore());
		calculator.calculate(lifecycleduration);
		this.results = calculator.getResults();
	}

	
}