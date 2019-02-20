package org.opensourcebim.ifccollection;

/**
 * Scaling type for slender objects. the item is defined in Length units (pipes etc.)
 * and any scaling is therefore done over the cross sectional area
 * @author vijj
 *
 */
public class MpgScalingOrientation {

	private Double[] unitDims;
	private Double[] scaleDims;
	
	public MpgScalingOrientation() {
		
	}
	
	public MpgScalingOrientation(MpgScalingOrientation st, double scaleFactor) {
		this.setUnitDims(applyScale(st.getUnitDims(), scaleFactor));
		this.setScaleDims(applyScale(st.getScaleDims(), scaleFactor));
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
	
	private Double[] applyScale(Double[] in, Double factor) {
		Double[] out = in.clone();
		for (int i=0; i<in.length; i++) {
			out[i] = in[i] * factor;
		}
		return out;
	}
	
}
