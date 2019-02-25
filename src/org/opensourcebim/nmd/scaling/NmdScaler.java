package org.opensourcebim.nmd.scaling;

public interface NmdScaler {
	
	Double scaleWithConversion(Double[] dims, double unitConversionFactor);
	
	Integer getNumberOfDimensions();
	
	Boolean areDimsWithinBounds(Double[] dims, double unitConversionFactor);

	String getUnit();
}
