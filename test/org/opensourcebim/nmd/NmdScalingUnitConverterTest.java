package org.opensourcebim.nmd;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.ifccollection.MpgObjectStoreImpl;
import org.opensourcebim.nmd.scaling.NmdScalingUnitConverter;

public class NmdScalingUnitConverterTest {

	private MpgObjectStore store;

	public NmdScalingUnitConverterTest() {
	}

	@Before
	public void setUp() throws Exception {
		store = new MpgObjectStoreImpl();
	}

	@Test
	public void testConvertToStoreUnitReturnsOne() {
		Double factor = NmdScalingUnitConverter.getScalingUnitConversionFactor("m", store);
		assertEquals("scaling to the same unit should return a unit value.", 1.0, factor, 1e-8);
	}
	
	@Test
	public void testConverterCanHandleAliases() {
		Double f1 = NmdScalingUnitConverter.getScalingUnitConversionFactor("millimeter", store);
		Double f2 = NmdScalingUnitConverter.getScalingUnitConversionFactor("mm", store);
		assertEquals("alias for millimeters shoudl return same factor", f1, f2, 1e-8);
	}
	
	@Test
	public void testConverterIsCaseInsensitive() {
		Double f1 = NmdScalingUnitConverter.getScalingUnitConversionFactor("MM", store);
		Double f2 = NmdScalingUnitConverter.getScalingUnitConversionFactor("mm", store);
		assertEquals("aliases mm and MM should return same factor", f1, f2, 1e-8);
	}
	
	@Test
	public void testConvertToMillimeterUnitReturnsThousandForSingleDimension() {
		// double scaling dimension means 
		Double factor = NmdScalingUnitConverter.getScalingUnitConversionFactor("mm", store);
		assertEquals("scaling from meter to mm should return factor 1000", 1e3, factor, 1e-8);
	}
}
