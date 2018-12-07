package org.opensourcebim.ifccollection;

import org.opensourcebim.nmd.NmdProductCard;

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
	
	// properties relevant from 
	private NmdProductCard nmdProductCard;
	
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
	
	public String print() {
		StringBuilder sb = new StringBuilder();
		sb.append("material : " + ifcName + " with properties" + System.getProperty("line.separator"));
		sb.append("NMD ID: " + nmdIdentifier + System.getProperty("line.separator"));
		sb.append("nmd material(s) linked to MpgMaterial: " + System.getProperty("line.separator"));
		sb.append("specs undefined " + System.getProperty("line.separator"));
		
		return sb.toString();
	}

	public NmdProductCard getNmdProductCard() {
		return nmdProductCard;
	}

	public void setMaterialSpecs(NmdProductCard productCard) {
		this.nmdProductCard = productCard;
	}
}
