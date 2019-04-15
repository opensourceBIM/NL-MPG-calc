package org.opensourcebim.mpgcalculation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MaterialSource;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.ObjectStoreBuilder;

import nl.tno.bim.nmd.domain.NmdProductCardImpl;

public class MpgCalculatorTest {

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
		// no objects are linked to the element which should return a warning
		builder.getStore().addElement("steel");

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}

	@Test
	public void testReturnIncompleteDataStatusWhenNMdDataIsIncomplete() {
		// no material spec is added which should return a warning
		builder.getStore().addElement("steel");
		builder.getStore().setObjectForElement("steel", builder.createDummyObject("steel element", "IfcBeam", "", "42.42"));

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}
	
	@Test
	public void testReturnIncompleteDataStatusWhenMpgObjectsAreNotFullyCovered() {
		// adding a profile set that is not a totaalproduct will trigger a warning
		
		MpgElement el = builder.getStore().addElement("coated steel beam");
		MpgObjectImpl obj = builder.createDummyObject("coated steel beam", "IfcBeam", "", "11.11");
		obj.addMaterialSource(new MaterialSource("1", "steel", "direct"));
		builder.getStore().setObjectForElement("coated steel beam", obj);
		el.mapProductCard(new MaterialSource("2", "coating", "direct"), 
				builder.createDummyProductCard("steel beam", 3, "m", 100, null));

		startCalculations(1.0);
		assertEquals(ResultStatus.IncompleteData, results.getStatus());
	}
	@Test
	public void testReturnSuccessWhenMpgObjectFullyCoveredWithDeelProducten() {
		String ifcName = "steel";
		MpgElement el = builder.getStore().addElement(ifcName);
		
		String name = "steel beam";
		String unit = "m3";
		int category = 1;
		
		el.setMpgObject(builder.createDummyObject(name, "IfcBeam", "", "42.42"));
		NmdProductCardImpl card = builder.createDummyProductCard(name, category, unit, 1, null);

		card.setIsTotaalProduct(false);
		
		el.mapProductCard(new MaterialSource("1", "steel", "dummy"), card);

		startCalculations(1.0);
		assertEquals(ResultStatus.Success, results.getStatus());
	}

	@Test
	public void testResultsReturnSuccessStatusWhenCalculationsSucceed() {
		builder.addMappedMpgElement("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertEquals(ResultStatus.Success, results.getStatus());
	}

	@Test
	public void testTotalCostCannotBeNaN() {
		builder.addMappedMpgElement("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertFalse(results.getTotalCost().isNaN());
	}
	
	@Test
	public void testTotalCostIsNonZeroOnCompleteProduct() {
		builder.addMappedMpgElement("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1.0);
		assertFalse(results.getTotalCost() == 0);
	}

	@Test
	public void testCategory3DataIncreasesTotalCost() {
		builder.addMappedMpgElement("steel", "Stainless Steel", "m2", 3, 1);

		startCalculations(1.0);
		// 30% increase in cost for category 3 data
		assertEquals(1.3 * (double) (builder.getDummyReferences().getMilieuCategorieMapping().size()),
				results.getCostPerLifeCycle("TransportToSite"), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerSquareMeterFloorArea() {
		builder.addMappedMpgElement("steel", "Stainless Steel", "m2", 1, 1);

		Double totalArea = 10.0;
		builder.addSpace(totalArea, 3.0);

		startCalculations(1.0);

		assertEquals(results.getTotalCost() / totalArea, results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerOperationYear() {
		Double totalLifeTime = 10.0;
		builder.addMappedMpgElement("steel", "Stainless Steel", "m2", 1, totalLifeTime.intValue());
		builder.addSpace(1.0, 3.0);

		startCalculations(totalLifeTime);

		assertEquals(results.getTotalCost() / totalLifeTime, results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCorrectedCostIsGivenPerOperatingYearAndFloorArea() {
		Double factor = 10.0;
		builder.addMappedMpgElement("steel", "Stainless Steel", "m2", 1, factor.intValue());
		builder.addSpace(factor, 3.0); // 10m2 floor
		startCalculations(factor); // 10 years of designlife
		assertEquals(results.getTotalCost() / (factor * factor), results.getTotalCorrectedCost(), 1e-8);
	}

	@Test
	public void testTotalCostPerMaterialReturnsZeroWhenMaterialNotPresent() {
		builder.addMappedMpgElement("steel", "Stainless Steel", "m2", 1, 1);

		startCalculations(1);

		assertEquals(0, results.getCostPerProductName("dummy material"), 1e-8);
	}

	@Test
	public void testTotalCostPerMaterialReturnsOnlyCostOfRelevantMaterial() {
		builder.addMappedMpgElement("steel", "Stainless Steel", "m2", 1, 1);
		builder.addMappedMpgElement("brick", "brick and mortar", "m2", 1, 1);
		builder.addMappedMpgElement("brick2", "brick and mortar", "m2", 1, 1);
		
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