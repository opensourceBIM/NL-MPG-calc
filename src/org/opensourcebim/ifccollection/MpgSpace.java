package org.opensourcebim.ifccollection;

/**
 * class to store a space
 * @author vijj
 */
public interface MpgSpace {
	
	public double getVolume(); // volume calculated based on layer ratios etc.
	
	public double getArea();
	
	public String getId();
	
	public String print();


}
