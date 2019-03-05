package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private MpgObject mpgObject;

	private MpgObjectStore store;

	private NmdMapping mappingMethod;
	private boolean coveredFlag;

	public MpgElement(String name, MpgObjectStore store) {
		ifcName = name;
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
				// remove any mappings that were added through hierarchical constraints
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

	public List<Integer> getProductIds() {
		return this.getMpgObject().getListedMaterials().stream()
				.filter(m -> m.getMapId() > 0)
				.map(m ->m.getMapId())
				.collect(Collectors.toList());
	}
	
	@JsonIgnore
	public List<NmdProductCard> getNmdProductCards() {
		return this.getStore().getProductCards(this.getProductIds());
	}

	private void addProductCard(NmdProductCard productCard) {
		this.getProductIds().add(productCard.getProductId());
		this.getStore().addProductCard(productCard);
	}

	public void removeProductCards() {
		this.getMpgObject().getListedMaterials().forEach(mat -> mat.clearMap());
	}
	
	public void mapProductCard(MaterialSource mat, NmdProductCard card) {
		// add the material to the mpgObject if it has not been added yet.
		if (this.getMpgObject().getListedMaterials().stream().filter(m -> m.getOid().equals(mat.getOid())).count() == 0) {
			this.getMpgObject().addMaterialSource(mat);
		}
		
		mat.setMapping(card);
		this.addProductCard(card);
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
		return coveredFlag;
	}

	public void setIsFullyCovered(boolean flag) {
		this.coveredFlag = flag;
	}
}
