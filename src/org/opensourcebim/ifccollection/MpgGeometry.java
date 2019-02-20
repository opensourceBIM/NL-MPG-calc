package org.opensourcebim.ifccollection;
import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MpgGeometry {
	private double volume;
	private double floorArea;
	private double faceArea;

	private Boolean isComplete;
	
	private Double[] principalDimensions;
	private Double[] sortedDims;

	public MpgGeometry() {
		volume = Double.NaN;
		floorArea = Double.NaN;
		faceArea = Double.NaN;

		setIsComplete(false);
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
		if (this.sortedDims.length == 0) {
			return Double.NaN;
		}
	    return this.sortedDims[0];
	}
	

	public Boolean getIsComplete() {
		return isComplete;
	}

	public void setIsComplete(Boolean isComplete) {
		this.isComplete = isComplete;
	}

	public Double[] getDimensions() {
		return this.principalDimensions;
	}

	public void setDimensions(double x_dir, double y_dir, double z_dir) {
		this.principalDimensions = new Double[] {x_dir, y_dir, z_dir};
		this.sortedDims = new Double[] {x_dir, y_dir, z_dir};
		Arrays.sort(sortedDims, Collections.reverseOrder());
	}
	
	public MpgScalingOrientation getScalerOrientation(int numProductDimenions) {
		MpgScalingOrientation scaler = new MpgScalingOrientation();
		if (numProductDimenions == 1) {
			scaler.setUnitDims(new Double[] {sortedDims[0]});
			scaler.setScaleDims(new Double[] {sortedDims[1], sortedDims[2]});
		} else {
			scaler.setUnitDims(new Double[] {sortedDims[0], sortedDims[1]});
			scaler.setScaleDims(new Double[] {sortedDims[2]});
		}
		return scaler;
	}

	/*
	 * Assume equal shape of the two geometries and define the dimensions of the object by scaling 
	 * the input geometry.
	 */
	public void setDimensionsByVolumeRatio(MpgGeometry geom) {
		if (geom.getDimensions().length != 3) {
			System.out.println("wut?");
			return;
		}
		Double lengthRatio = Math.pow(this.getVolume() / geom.getVolume(), 1.0/3.0);
		this.setDimensions(geom.getDimensions()[0] * lengthRatio,
				geom.getDimensions()[1] * lengthRatio,
				geom.getDimensions()[2] * lengthRatio);
	}
}
