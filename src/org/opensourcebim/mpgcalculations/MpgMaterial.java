package org.opensourcebim.mpgcalculations;

import java.util.HashMap;

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
	private HashMap<String, Object> properties;
	
	public MpgMaterial(String name)
	{
		ifcName = name;
		properties = new HashMap<String, Object>();
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

	public <T> T getProperty(String key, Class<T> type) {
		Object value = properties.getOrDefault(key.toLowerCase(), null);
		return type.isInstance(value) ? (T)value : null;
	}

	public void setProperty(String key, Object value) {
		this.properties.put(key.toLowerCase(), value);
	}
	
	public String print() {
		StringBuilder sb = new StringBuilder();
		sb.append("material : " + ifcName + " with poperties");
		sb.append(System.getProperty("line.separator"));
		properties.forEach((name, value) -> sb.append(name + " : " + value + System.getProperty("line.separator")) );
		return sb.toString();
	}
}
