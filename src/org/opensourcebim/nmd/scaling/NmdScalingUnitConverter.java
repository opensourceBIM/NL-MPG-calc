package org.opensourcebim.nmd.scaling;

import org.bimserver.utils.LengthUnit;
import org.opensourcebim.ifccollection.MpgObjectStore;

/**
 * Static functions to support dimension checks and unit conversion
 * 
 * @author vijj
 *
 */
public final class NmdScalingUnitConverter {

	private NmdScalingUnitConverter() {
	}

	public static int getUnitDimension(String unit) {
		if (unit == null) return -1;
		
		switch (unit.toLowerCase()) {
		case "mm":
		case "cm":
		case "m1":
		case "m":
		case "meter":
			return 1;
		case "mm2":
		case "mm^2":
		case "square_millimeter":
		case "cm2":
		case "cm^2":
		case "m2":
		case "m^2":
		case "square_meter":
			return 2;
		case "mm3":
		case "mm^3":
		case "cubic_millimeter":
		case "cm3":
		case "cm^3":
		case "m3":
		case "m^3":
		case "cubic_meter":
			return 3;
		default:
			return -1;
		}
	}

	/**
	 * Converts the This might be slightly counter intuititve, but when scaling over
	 * a single dimension we need a 2D conversionfactor while when we are scaling
	 * over 2 dimensions the scaling is done per axis and therefore the conversion
	 * is 1 D first figure out the quantity of the input unit and get the right
	 * store unit
	 * 
	 * @param unit - working unit of the nmd scaler
	 * @return conversion factor to convert from mpgObject dimensions to NMD scaler dimensions
	 *         unit.
	 */
	public static Double getScalingUnitConversionFactor(String unit, MpgObjectStore store) {	
		Double factor = 1.0;

		switch (unit.toLowerCase()) {
		case "mm":
		case "millimeter":
			factor = LengthUnit.MILLI_METER.convert(1.0, store.getLengthUnit());
			break;
		case "m":
		case "meter":
			factor = LengthUnit.METER.convert(1.0, store.getLengthUnit());
		default:
			break;
		}

		return factor;
	}

}
