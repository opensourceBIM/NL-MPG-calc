package org.opensourcebim.nmd.scaling;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

public abstract class NmdBaseScaler implements NmdScaler {

	protected String unit;
	protected Double[] coefficients;
	protected Double[] bounds;
	private Double[] currentValues;

	/**
	 * Create a scaler by defining standard coefficients and bounds
	 * 
	 * @param unit          the physical unit that the input values should be in.
	 * @param coefficients  scaling coefficients with different meaning per scaler
	 *                      implementation. shoud have maximum of 3 coefficients
	 * @param bounds        the min and max values for 1 or 2 dimensions. in order:
	 *                      [x_min, x_max, y_min, y_max]
	 * @param currentValues the scaling dimensions that belong to the current
	 *                      profileset values.
	 */
	public NmdBaseScaler(String unit, Double[] coefficients, Double[] bounds, Double[] currentValues) {
		this.unit = unit;
		this.coefficients = coefficients;
		this.bounds = bounds;
		this.currentValues = currentValues;
	}

	private Double scale(double dim1Val, double dim2Val) {
		if (areDimsWithinBounds(dim1Val,  dim2Val)) {
			return this.getNumberOfDimensions() == 1 ? scale(dim1Val) * scale(dim2Val)
					: scale(dim1Val) * getScaleFactor(dim2Val, currentValues[1]);
		} else {
			return Double.NaN;
		}
	}

	private Double scale(double dim1Val) {
		return getScaleFactor(dim1Val, currentValues[0]);
	}
	
	@Override
	public Double scaleWithConversion(Double[] dims, double conversionFactor) {
		// determine how to match input dimension on scaler dimensions
		Double[] sortedDims =this.matchInputDimensionsOnCurrentDimensions(dims);
		
		if (sortedDims.length == 1) {
			return this.scale(sortedDims[0] * conversionFactor);
		} else if (sortedDims.length == 2) {
			return this.scale(sortedDims[0] * conversionFactor, sortedDims[1] * conversionFactor);
		} else {
			return Double.NaN;
		}
	}

	private Double[] matchInputDimensionsOnCurrentDimensions(Double[] dims) {
		// check if we need to order at all
		if (this.currentValues.length > 1 && dims.length > 1) {
			Arrays.sort(dims);
			// array is sorted ascending. check if we need to reverse that
			if (currentValues[1] <= currentValues[0]) {
				ArrayUtils.reverse(dims);
			}
		}
		return dims;
	}

	/**
	 * return a flag to indicate that the desired scaling dimensions are within
	 * bounds
	 * 
	 * @param x first dimension to check
	 * @param y second dimension to check
	 * @return Boolean that inidicates that the desired dims are within the scaling
	 *         bounds
	 */
	private Boolean areDimsWithinBounds(Double x, Double y) {
		// check whether to check the bounds for 1 or 2 dimensions
		if (getNumberOfDimensions() == 1) {
			return isWithinBounds(x, bounds[0], bounds[1]) && isWithinBounds(y, bounds[0], bounds[1]);
		} else {
			return isWithinBounds(x, bounds[0], bounds[1]) && isWithinBounds(y, bounds[2], bounds[3]);
		}
	}
	
	@Override
	public Boolean areDimsWithinBounds(Double[] dims, double conversionFactor) {
		if (dims.length == 1) {
			return areDimsWithinBounds(dims[0] * conversionFactor, null);
		} else if (dims.length == 2) {
			return areDimsWithinBounds(dims[0] * conversionFactor, dims[1] * conversionFactor);
		} else {
			return false;
		}
	}

	/**
	 * Check if a single parameter is within bounds
	 * 
	 * @param x     desired dimensions
	 * @param x_min minimum dim value
	 * @param x_max maximum dim value
	 * @return a flag to indicate x is within the min and max values.
	 */
	private Boolean isWithinBounds(double x, double x_min, double x_max) {
		return x <= x_max && x >= x_min;
	}

	/**
	 * Simple check to determine if scaling should be done over 1 or 2 dimensions
	 * 
	 * @return Number of dimensions of the scaler
	 */
	@Override
	public Integer getNumberOfDimensions() {
		return (bounds.length == 4 && !bounds[3].isNaN() &&
				currentValues.length == 2 && !currentValues[1].isNaN()) 
				? 2
				: 1;
	}

	/**
	 * Determine the correction factor for scaling from current value to desired
	 * value
	 * 
	 * @param x_desired the desired dimension value
	 * @param x_current the current value wiht which the profileSet is defined
	 * @return a scaling factor
	 */
	protected double getScaleFactor(double x_desired, Double x_current) {
		return calculate(x_desired) / calculate(x_current);
	}
	
	public String getUnit() {
		return this.unit;
	}

	/**
	 * abstract method to override by derived classes. will define the type of scaler
	 * 
	 * @param x value to calculate the y value (y(x))
	 * @return y value from implemented method.
	 */
	protected abstract Double calculate(Double x);
}
