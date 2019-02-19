package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MpgGeometry {
	private double volume;
	private double floorArea;
	private double faceArea;

	private Boolean isComplete;
	
	private List<MpgScalingType> scaleParams;

	public MpgGeometry() {
		volume = Double.NaN;
		floorArea = Double.NaN;
		faceArea = Double.NaN;

		setIsComplete(false);
		
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

	public Double getFaceArea() {
		return faceArea;
	}

	public void setFaceArea(Double largestFaceArea) {
		this.faceArea = largestFaceArea;
	}

	// return the largest axis
	@JsonIgnore
	public Double getPrincipalDimension() {
		if (getScalerTypes().size() == 0) {
			return Double.NaN;
		}
	    return getScalerTypes().get(0).getUnitDims()[0];
	}
	
	public void addScalingType(MpgScalingType scaleData) {
		this.scaleParams.add(scaleData);
	}
	
	// add a scaler type based on another geometry
	public void addScalingTypesFromGeometry(MpgGeometry geom) {
		// determine length scale factor based on geometry ratio
		Double scaleFactor = Math.pow(this.getVolume() / geom.getVolume(), (1.0/3.0));
		
		geom.scaleParams.forEach(st -> {
			MpgScalingType newScale = new MpgScalingType(st, scaleFactor.isNaN() ? 1.0 : scaleFactor);
			this.addScalingType(newScale);	
		});
	}

	/**
	 * Return the dimensions of the geometry that require scaling.
	 * @param dim the dimensionality of the profileSet that is linked to this geometry
	 * @return The dimensions of the geometry that require scaling
	 */
	public Double[] getScaleDims(int dim) {
		// this is a bit counter-intuitive, but we need the scaler that belongs to an
		// area object to scale over 1 dimension (thickness only) while we need the
		// scaler that belongs to a slender object (pipes etc. to scale over a cross
		// sectional area		
		int scalerIndex = dim % 2;
		return  scaleParams.get(scalerIndex).getScaleDims();
	}

	public Boolean getIsComplete() {
		return isComplete;
	}

	public void setIsComplete(Boolean isComplete) {
		this.isComplete = isComplete;
	}

	public List<MpgScalingType> getScalerTypes() {
		return this.scaleParams;
	}
}
