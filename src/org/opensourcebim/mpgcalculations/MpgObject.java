package org.opensourcebim.mpgcalculations;

public interface MpgObject {
	/**
	 * Reference to a MpgMaterial object
	 */
	public MpgMaterial getMaterial();
	
	/**
	 * The volume of the object
	 * @return
	 */
	public double getVolume(); // volume calculated based on layer ratios etc.
}
