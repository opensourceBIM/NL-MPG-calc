package org.opensourcebim.nmd.scaling;

import org.eclipse.jdt.core.compiler.InvalidInputException;

public class NmdLogarithmicScaler extends NmdBaseScaler implements NmdScaler {

	public NmdLogarithmicScaler(String scaleUnit, Double[] coefficients, Double[] bounds, Double[] currentValues) throws InvalidInputException {
		super(scaleUnit, coefficients, bounds, currentValues);

		if (currentValues[0] == 1 || currentValues[1] == 1) {
			throw new InvalidInputException(
					"Cannot have a unit value for a logarithmic scaler as it creates a division by 0 error on scaling");
		}
	}

	@Override
	protected Double calculate(Double x) {
		if (x == 0.0) {
			return Double.NaN;
		}
		return coefficients[0] * Math.log(x) + coefficients[2];
	}

}
