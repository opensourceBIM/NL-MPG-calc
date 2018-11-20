package org.opensourcebim.ifccollection;

public interface MpgObject {
	/**
	 * Reference to a MpgMaterial object
	 */
	public String getMaterialName();
	
	/**
	 * The volume of the object
	 * @return
	 */
	public double getVolume(); // volume calculated based on layer ratios etc.
	
	public double getArea();
	
	public String print();


}
