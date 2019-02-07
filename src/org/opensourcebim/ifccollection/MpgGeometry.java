package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MpgGeometry {
	private double volume;
	private double floorArea;
	private double largestFaceArea;

	private Double[] maxDimensions;
	private double angleOfLargestFaceAreaWrtHorizon;

	private List<MpgScalingType> scaleParams;

	public MpgGeometry() {
		volume = Double.NaN;
		floorArea = Double.NaN;
		largestFaceArea = Double.NaN;

		angleOfLargestFaceAreaWrtHorizon = Double.NaN;
		maxDimensions = new Double[3];
		setMaxXDimension(Double.NaN);
		setMaxYDimension(Double.NaN);
		setMaxZDimension(Double.NaN);
		scaleParams = new ArrayList<MpgScalingType>();
	}

	public Double getVolume() {
		return volume;
	}

	public void setVolume(Double volume) {
		this.volume = volume;
	}

	public Double getFloorArea() {
		return floorArea;
	}

	public void setFloorArea(Double floorArea) {
		this.floorArea = floorArea;
	}

	public Double getLargestFaceArea() {
		return largestFaceArea;
	}

	public void setLargestFaceArea(Double largestFaceArea) {
		this.largestFaceArea = largestFaceArea;
	}

	public Double getAngleOfLargestFaceAreaWrtHorizon() {
		return angleOfLargestFaceAreaWrtHorizon;
	}

	// angle in radians
	public void setAngleOfLargestFaceAreaWrtHorizon(Double angleOfLargestFaceAreaWrtHorizon) {
		this.angleOfLargestFaceAreaWrtHorizon = angleOfLargestFaceAreaWrtHorizon;
	}

	public Double getMaxXDimension() {
		return maxDimensions[0];
	}

	public Double getMaxYDimension() {
		return maxDimensions[1];
	}

	public Double getMaxZDimension() {
		return maxDimensions[2];
	}

	public void setMaxXDimension(Double val) {
		this.maxDimensions[0] = val;
	}

	public void setMaxYDimension(Double val) {
		this.maxDimensions[1] = val;
	}

	public void setMaxZDimension(Double val) {
		this.maxDimensions[2] = val;
	}

	// return the largest axis - change for diagonal elements.
	@JsonIgnore
	public Double getPrincipalDimension() {
		return maxDimensions[scaleParams.get(0).getUnitAxes()[0]];
	}

	public void addScalingType(MpgScalingType scaleData) {
		this.scaleParams.add(scaleData);
	}

	public Double[] getScaleDims(int dim) {
		// this is a bit counter-intuitive, but we need the scaler that belongs to an
		// area object to scale over 1 dimension (thickness only) while we need the
		// scaler that belongs to a slender object (pipes etc. to scale over a cross
		// sectional area
		int scalerIndex = dim % 2;
		int[] scaleAxes = scaleParams.get(scalerIndex).getScaleAxes();
		Double[] dims = new Double[scaleAxes.length];
		for (int i = 0; i < scaleAxes.length; i++) {
			dims[i] = maxDimensions[scaleAxes[i]];
		}
		return dims;
	}
}
