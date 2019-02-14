package org.opensourcebim.nmd.scaling;

import org.eclipse.jdt.core.compiler.InvalidInputException;

public final class NmdScalerFactory {

	public NmdScaler create(String type, String scaleUnit, Double[] coefficients, Double[] bounds, Double[] currentValues) throws InvalidInputException {
		String lcType = type.toLowerCase();
		if (lcType.contains("linear")) {
			return createLinScaler(scaleUnit, coefficients, bounds, currentValues);
		} else if (lcType.contains("macht")) {
			return createPowScaler(scaleUnit, coefficients, bounds, currentValues);
		} else if (lcType.contains("logaritmisch")) {
			return createLogScaler(scaleUnit, coefficients, bounds, currentValues);
		} else if (lcType.contains("exponentieel")) {
			return createExpScaler(scaleUnit, coefficients, bounds, currentValues);
		} else {
			return null;
		}
	}
	
	public NmdExponentialScaler createExpScaler(String scaleUnit, Double[] coefficients, Double[] bounds, Double[] currentValues) {
		return new NmdExponentialScaler(scaleUnit, coefficients, bounds, currentValues);
	}

	public NmdLogarithmicScaler createLogScaler(String scaleUnit, Double[] coefficients, Double[] bounds, Double[] currentValues) throws InvalidInputException {
		return new NmdLogarithmicScaler(scaleUnit, coefficients, bounds, currentValues);
	}

	public NmdPowerScaler createPowScaler(String scaleUnit, Double[] coefficients, Double[] bounds, Double[] currentValues) {
		return new NmdPowerScaler(scaleUnit, coefficients, bounds, currentValues);
	}

	public NmdLinearScaler createLinScaler(String scaleUnit, Double[] coefficients, Double[] bounds, Double[] currentValues) {
		return new NmdLinearScaler(scaleUnit, coefficients, bounds, currentValues);
	}
	
}
