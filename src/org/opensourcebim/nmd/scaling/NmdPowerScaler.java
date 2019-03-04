package org.opensourcebim.nmd.scaling;

public class NmdPowerScaler extends NmdBaseScaler implements NmdScaler {

	public NmdPowerScaler(String scaleUnit, Double[] coefficients, Double[] bounds, Double[] currentValues) {
		super(scaleUnit, coefficients, bounds, currentValues);
	}

	@Override 
	protected Double calculate(Double x) {
		return coefficients[0] * Math.pow(x, coefficients[1]) + coefficients[2];
	}
	
}
