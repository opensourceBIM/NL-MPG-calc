package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.opensourcebim.nmd.NmdMapping;
import org.opensourcebim.nmd.NmdProductCard;

/**
 * Storage container to map mpgOject to the Nmdproducts
 * @author Jasper Vijverberg
 *
 */
public class MpgElement {
	// id's
	private String BimBotIdentifier;
	
	private String ifcName;
	private List<NmdProductCard> productCards;
	private MpgObject mpgObject;

	private NmdMapping mappingMethod;
	
	public MpgElement(String name)
	{
		ifcName = name;
		this.productCards = new ArrayList<NmdProductCard>();
	}
	
	public void setMpgObject(MpgObject mpgObject) {
		this.mpgObject = mpgObject;
	}
	
	public MpgObject getMpgObject() {
		return this.mpgObject;
	}
	
	/**
	 * Get the name of the material as found in the IFC file
	 * @return
	 */
	public String getIfcName() {
		return this.ifcName;
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
	
	public void setMappingMethod(NmdMapping mapping) {
		this.mappingMethod = mapping;
	}
	
	public NmdMapping getMappingMethod() {
		return mappingMethod;
	}
	
	public String print() {
		StringBuilder sb = new StringBuilder();
		sb.append("material : " + ifcName + " with properties" + System.getProperty("line.separator"));
		sb.append("nmd material(s) linked to MpgMaterial: " + this.productCards.size() + System.getProperty("line.separator"));
		sb.append("specs undefined " + System.getProperty("line.separator"));
		
		return sb.toString();
	}

	public List<NmdProductCard> getNmdProductCards() {
		return productCards;
	}

	public void addProductCard(NmdProductCard productCard) {
		this.productCards.add(productCard);
		
		// check with the store which child elements will also be mapped with this action
	}
	
	public void removeProductCard() {
		// unmap any child elements.
		throw new NotImplementedException("still needs to be done");
	}

	/**
	 * returns a flag inidcating if the element needs to be scaled
	 * @return see above
	 */
	public boolean requiresScaling() {
		return this.getNmdProductCards().stream()
				.flatMap(pc -> pc.getProfileSets().stream())
				.anyMatch(ps -> ps.getIsScalable() && ps.getScaler() != null);
	}

	public boolean getIsFullyCovered() {
		// ToDO: check with NMD interface whether all mandatory elements are present
		return false;
	}
}
