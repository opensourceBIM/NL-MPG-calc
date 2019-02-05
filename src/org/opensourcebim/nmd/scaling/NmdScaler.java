package org.opensourcebim.nmd.scaling;

public interface NmdScaler {

	Double scale(double dim1Val, double dim2Val);
	
	Double scale(double dim1Val);

	String getUnit();
	
}
