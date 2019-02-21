package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.NotImplementedException;
import org.opensourcebim.nmd.NmdMapping;
import org.opensourcebim.nmd.NmdProductCard;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Storage container to map mpgOject to the Nmdproducts
 * 
 * @author Jasper Vijverberg
 *
 */
public class MpgElement {

	private String ifcName;
	private List<NmdProductCard> productCards;
	private MpgObject mpgObject;

	private MpgObjectStore store;

	private NmdMapping mappingMethod;

	public MpgElement(String name, MpgObjectStore store) {
		ifcName = name;
		this.productCards = new ArrayList<NmdProductCard>();
		this.mappingMethod = NmdMapping.None;
		this.store = store;
	}

	public void setMpgObject(MpgObject mpgObject) {
		this.mpgObject = mpgObject;
	}

	public MpgObject getMpgObject() {
		return this.mpgObject;
	}

	@JsonIgnore
	public MpgObjectStore getStore() {
		return store;
	}

	/**
	 * Get the name of the material as found in the IFC file
	 * 
	 * @return
	 */
	public String getIfcName() {
		return this.ifcName;
	}

	public void setMappingMethod(NmdMapping mapping) {
		if (mapping != this.mappingMethod) {
			this.mappingMethod = mapping;
			if (mapping == NmdMapping.None) {
				// remove any mappngs that were added through hierarchical constraints
				store.toggleMappingDependencies(this.getMpgObject().getGlobalId(), false);
			} else if (!(mapping == NmdMapping.IndirectThroughChildren
					|| mapping == NmdMapping.IndirectThroughParent)) {
				// add hierarchical constraint mappings, 
				// but not when this items is already set through hierarchical constaints
				store.toggleMappingDependencies(this.getMpgObject().getGlobalId(), true);
			}
		}
	}

	public NmdMapping getMappingMethod() {
		return mappingMethod;
	}

	public boolean hasMapping() {
		return this.mappingMethod != NmdMapping.None;
	}

	public String print() {
		StringBuilder sb = new StringBuilder();
		sb.append("material : " + ifcName + " with properties" + System.getProperty("line.separator"));
		sb.append("nmd material(s) linked to MpgMaterial: " + this.productCards.size()
				+ System.getProperty("line.separator"));
		sb.append("specs undefined " + System.getProperty("line.separator"));

		return sb.toString();
	}

	public List<NmdProductCard> getNmdProductCards() {
		return productCards;
	}

	public void addProductCard(NmdProductCard productCard) {
		this.productCards.add(productCard);
	}

	public void removeProductCards() {
		this.productCards.clear();
	}

	/**
	 * returns a flag inidcating if the element needs to be scaled
	 * 
	 * @return see above
	 */
	public boolean requiresScaling() {
		return this.getNmdProductCards().stream().flatMap(pc -> pc.getProfileSets().stream())
				.anyMatch(ps -> ps.getIsScalable() && ps.getScaler() != null);
	}

	public boolean getIsFullyCovered() {
		// ToDO: check with NMD interface whether all mandatory elements are present
		return false;
	}
}
