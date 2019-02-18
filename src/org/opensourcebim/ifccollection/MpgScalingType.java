package org.opensourcebim.ifccollection;

/**
 * Scaling type for slender objects. the item is defined in Length units (pipes etc.)
 * and any scaling is therefore done over the cross sectional area
 * @author vijj
 *
 */
public class MpgScalingType {

	private Double[] unitDims;
	private Double[] scaleDims;
	
	public MpgScalingType() {
		
	}
	
	public Double[] getUnitDims() {
		return unitDims;
	}

	public void setUnitDims(Double[] unitDims) {
		this.unitDims = unitDims;
	}

	public Double[] getScaleDims() {
		return scaleDims;
	}

	public void setScaleDims(Double[] scaleDims) {
		this.scaleDims = scaleDims;
	}
	
}
