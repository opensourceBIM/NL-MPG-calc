package org.opensourcebim.nmd.scaling;

public class NmdLinearScaler extends NmdBaseScaler implements NmdScaler {
	
	public NmdLinearScaler(String unit, Double[] coefficients, Double[] bounds, Double[] currentValues) {
		super(unit, coefficients, bounds, currentValues);
	}
	
	@Override
	protected Double calculate(Double x) {
		return coefficients[0] * x + coefficients[2];
	}

}
