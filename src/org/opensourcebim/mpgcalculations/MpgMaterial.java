package org.opensourcebim.mpgcalculations;

/**
 * Storage container class to archive all material properties.
 * @author Jasper Vijverberg
 *
 */
public class MpgMaterial {
	// id's
	private String ifcName;
	private String nmdIdentifier;
	private String BimBotIdentifier;
	
	// add any other properties that can be relevant for the mpg calculations
	
	public MpgMaterial(String name)
	{
		ifcName = name;
	}
	
	/**
	 * Get the name of the material as found in the IFC file
	 * @return
	 */
	public String getIfcName() {
		return ifcName;
	}

	/**
	 * get the name of the material as found in NMD
	 * @return a string with the nmd identifier
	 */
	public String getNmdIdentifier() {
		return nmdIdentifier;
	}

	/**
	 * the id of the material for internal BimBot use.
	 * @return a unique material identifier string
	 */
	public String getBimBotIdentifier() {
		return BimBotIdentifier;
	}
	
	/**
	 * set the BimBot ID 
	 * @param bimBotIdentifier - value to set the id to.
	 */
	public void setBimBotIdentifier(String bimBotIdentifier) {
		BimBotIdentifier = bimBotIdentifier;
	}
}
