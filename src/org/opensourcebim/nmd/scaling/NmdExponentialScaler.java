package org.opensourcebim.nmd.scaling;

public class NmdExponentialScaler extends NmdBaseScaler implements NmdScaler {

	public NmdExponentialScaler(String unit, Double[] coefficients, Double[] bounds, Double[] currentValues) {
		super(unit, coefficients, bounds, currentValues);
	}
	
	@Override 
	protected Double calculate(Double x) {
		return coefficients[0] * Math.exp(coefficients[1] * x) + coefficients[2];
	}
}
