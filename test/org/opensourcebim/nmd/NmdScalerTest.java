package org.opensourcebim.nmd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opensourcebim.nmd.scaling.NmdExponentialScaler;
import org.opensourcebim.nmd.scaling.NmdLinearScaler;
import org.opensourcebim.nmd.scaling.NmdLogarithmicScaler;
import org.opensourcebim.nmd.scaling.NmdPowerScaler;
import org.opensourcebim.nmd.scaling.NmdScalerFactory;

public class NmdScalerTest {

	NmdScalerFactory factory;
	
	// TODO test unit scaling etc.
	
	public NmdScalerTest() {
	}

	@Before
	public void setUp() throws Exception {
		factory = new NmdScalerFactory();
	}

	@After
	public void tearDown() throws Exception {}
	
	
	@Test
	public void testBoundCheckReturnsFalseWhenFirstDimsIsOutsideSingleBound() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, Double.NaN, Double.NaN}, 
				new Double[] {1.0, Double.NaN});
		
		// only x bounds are defined
		assertFalse(scaler.areDimsWithinBounds(new Double[] {-1.0, Double.NaN}, 1.0));
	}
	
	@Test
	public void testBoundCheckReturnsFalseAnyOfInputDimsIsOutsideSingleBound() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, Double.NaN, Double.NaN}, 
				new Double[] {1.0, Double.NaN});
		
		// only x bounds are defined and as such we do not chekc on the y dim within bounds
		assertFalse(scaler.areDimsWithinBounds(new Double[] {1.0, -1.0}, 1.0));
	}
	
	@Test
	public void testBoundCheckReturnsTrueInBothInputsWithinSingleBound() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, Double.NaN, Double.NaN}, 
				new Double[] {1.0, Double.NaN});
		
		// only x bounds are defined and as such we do not chekc on the y dim within bounds
		assertTrue(scaler.areDimsWithinBounds(new Double[] {1.0, 1e12}, 1.0));
	}
	
	@Test
	public void testScalerDimCheckReturnsFalseOnOnlyXDimOutOfRange() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, 10.0}, 
				new Double[] {1.0, 0.0});
		
		// only x bounds are out of bounds
		assertFalse(scaler.areDimsWithinBounds(new Double[] {-1.0, 1.0}, 1));
	}
	
	@Test
	public void testScalerDimCheckReturnsTrueOnXAndYValueWithinRange() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, 10.0}, 
				new Double[] {1.0, 1.0});
		
		// only x bounds are defined and as such we do not check on the y dim within bounds
		assertTrue(scaler.areDimsWithinBounds(new Double[] {0.0, 1.0}, 1));
	}
	
	@Test
	public void testScalerReturnsNaNWhenRequestedXValueOutOfRange() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, 10.0}, 
				new Double[] {1.0, 1.0});
		
		assertTrue(scaler.scaleWithConversion(new Double[] {-1.0, 1.0},  1.0).isNaN());
	}
	
	@Test
	public void testScalerReturnsNaNWhenRequestedYValueOutOfRange() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, 10.0}, 
				new Double[] {1.0, 1.0});
		
		assertTrue(scaler.scaleWithConversion(new Double[] {1.0, -1.0},  1.0).isNaN());
	}
	
	@Test
	public void testScalerReturnsNanWhenRequestedYValueOutOfRangeOnSingleBound() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, Double.NaN});
		
		assertTrue(scaler.scaleWithConversion(new Double[] {1.0, -1.0},  1.0).isNaN());
	}
	
	@Test
	public void testScalerReturnsScaleFactorWhenBothInputsWithinSingleBoundRange() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, Double.NaN});
		
		assertEquals(12.0 , scaler.scaleWithConversion(new Double[] {1.0, 12.0},  1.0), 1e-8);
	}
	
	@Test
	public void testScalerFlipsInputsBasedOnBounds() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, 5.0, 10.0, 20.0}, 
				new Double[] {1.0, 11.0});
		
		// the inputs will be flipped such that 12 falls within the 2nd bound range and the 4.0 value within the first bound
		assertEquals(4.0 * (12.0/11.0),  scaler.scaleWithConversion(new Double[] {12.0, 4.0},  1.0), 1e-8);
	}
	
	@Test
	public void testScalerReturnsUnitScaleFactorWhenNoScalingIsNeededOverBothDims() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, 1.0});
		
		assertEquals(1.0 , scaler.scaleWithConversion(new Double[] {1.0, 1.0},  1.0), 1e-8);
	}
	
	@Test
	public void testScalerReturnsZeroWhenXScaleFactorIsZero() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, 1.0});
		
		assertEquals(0.0, scaler.scaleWithConversion(new Double[] {0.0, 1.0},  1.0), 1e-8);
	}
	
	@Test
	public void testScalerReturnsZeroWhenYScaleFactorIsZero() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, 1.0});
		
		assertEquals(0.0, scaler.scaleWithConversion(new Double[] {1.0, 0.0},  1.0), 1e-8);
	}
	
	@Test
	public void testLinearScalerIsIndependentOfSecondCoefficient() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, Double.POSITIVE_INFINITY, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, 1.0});
		
		assertEquals(1.0, scaler.scaleWithConversion(new Double[] {1.0, 1.0},  1.0), 1e-8);
	}
	
	@Test
	public void testLinearScalerIsLinearlyDependentOnFirstCoefficient() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {2.0, 0.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, 1.0});
		
		assertEquals(2.0, scaler.scaleWithConversion(new Double[] {2.0, 1.0},  1.0), 1e-8);
	}
	
	@Test
	public void testLinearScalerUsesThirdCoefficientAsConstantValue() {
		NmdLinearScaler scaler = factory.createLinScaler("mm", 
				new Double[] {1.0, 0.0, 10.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, Double.NaN, Double.NaN}, 
				new Double[] {1.0, Double.NaN});
		
		// scale factor will be (2+ 10)/(1+10)
		assertEquals(12.0/11.0, scaler.scaleWithConversion(new Double[] {2.0},  1.0), 1e-8);
	}
	
	@Test
	public void testPowerWithSecondCoefficientAsOneIsEqualToLinearScaler() {
		Double[] coeffs = new Double[] {42.123, 1.0, 1.23451};
		Double[] bounds = new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY};
		Double[] initVals = new Double[] {1.0, Double.NaN};
		
		NmdLinearScaler linScaler = factory.createLinScaler("mm", coeffs, bounds, initVals);
		NmdLinearScaler powerScaler = factory.createLinScaler("mm", coeffs, bounds, initVals);
		
		// scale factor will be (2+ 10)/(1+10)
		assertEquals(linScaler.scaleWithConversion(new Double[] {45.0, 8.0},  1.0),
				powerScaler.scaleWithConversion(new Double[] {45.0, 8.0},  1.0), 1e-8);
	}
	
	@Test
	public void testPowerScalerUsesSecondCoefficientAsExponent() {
		NmdPowerScaler scaler = factory.createPowScaler("mm", 
				new Double[] {1.0, 2.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, 1.0});
		
		assertEquals(4.0, scaler.scaleWithConversion(new Double[] {2.0, 1.0},  1.0), 1e-8);
	}
	
	@Test
	public void testLogarithmicScalerReturnsNaNOnZeroXValue() throws InvalidInputException {
		NmdLogarithmicScaler scaler = factory.createLogScaler("mm", 
				new Double[] {1.0, 2.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {2.0, 2.0});
		
		assertTrue(scaler.scaleWithConversion(new Double[] {0.0, 1.0},  1.0).isNaN());
	}
	
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@SuppressWarnings("unused")
	@Test
	public void testLogarithmicScalerCannotByCreatedWithUnitCurrentValues() throws InvalidInputException {
		
		exception.expect(InvalidInputException.class);
		
		NmdLogarithmicScaler scaler = new NmdLogarithmicScaler( "mm",				
				new Double[] {1.0, 2.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, 1.0});
	}
	
	@Test
	public void testLogarithmicScalerReturnsZeroOnUnitInputValue() throws InvalidInputException {
		NmdLogarithmicScaler scaler = factory.createLogScaler("mm", 
				new Double[] {1.0, 2.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {2.0, 2.0});
		
		// the requested scaling dimension is 1.0 which would return a 0.0 value from the log scaler
		assertEquals(0.0, scaler.scaleWithConversion(new Double[] {12.0, 1.0},  1.0), 1e-8);
	}
	
	@Test
	public void testExponentialScalerUsesDesiredInputasExponent() {
		NmdExponentialScaler scaler = factory.createExpScaler("mm", 
				new Double[] {1.0, 1.0, 0.0}, 
				new Double[] {0.0, Double.POSITIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY}, 
				new Double[] {1.0, 1.0});
		
		// e^3 / e
		assertEquals(Math.pow(Math.E, 2.0), scaler.scaleWithConversion(new Double[] {3.0, 1.0},  1.0), 1e-8);
	}
}