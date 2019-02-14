package org.opensourcebim.nmd.scaling;

public interface NmdScaler {
	
	Double scaleWithConversion(Double[] dims, double unitConversionFactor);

	Double scale(double dim1Val, double dim2Val);
	
	Double scale(double dim1Val);
	
	String getUnit();
	
	Integer getNumberOfDimensions();
}
