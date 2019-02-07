package org.opensourcebim.ifccollection;

/**
 * Scaling type for slender objects. the item is defined in Length units (pipes etc.)
 * and any scaling is therefore done over the cross sectional area
 * @author vijj
 *
 */
public class MpgScalingType {

	String unit;
	private int[] unitAxes;
	private int[] scaleAxes;
	
	public MpgScalingType() {
		
	}
	
	public int[] getUnitAxes() {
		return unitAxes;
	}

	public void setUnitAxes(int[] unitAxes) {
		this.unitAxes = unitAxes;
	}

	public int[] getScaleAxes() {
		return scaleAxes;
	}

	public void setScaleAxes(int[] scaleAxes) {
		this.scaleAxes = scaleAxes;
	}
	
}
