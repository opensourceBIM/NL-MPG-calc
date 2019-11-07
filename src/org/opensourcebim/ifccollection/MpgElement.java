package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.opensourcebim.mapping.NmdMappingType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import nl.tno.bim.nmd.domain.NmdProductCard;

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
	private NmdMappingType mappingMethod;

	public MpgElement(String name, MpgObjectStore store) {
		ifcName = name;
		this.mappingMethod = NmdMappingType.None;
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

	public void setMappingMethod(NmdMappingType mapping) {
		if (mapping != this.mappingMethod) {
			this.mappingMethod = mapping;
			if (mapping == NmdMappingType.None) {
				// remove any mappings that were added through hierarchical constraints
				store.toggleMappingDependencies(this.getMpgObject().getGlobalId(), false);
			} else if (!(mapping == NmdMappingType.IndirectThroughChildren
					|| mapping == NmdMappingType.IndirectThroughParent)) {
				// add hierarchical constraint mappings,
				// but not when this items is already set through hierarchical constaints
				store.toggleMappingDependencies(this.getMpgObject().getGlobalId(), true);
			}
		}
	}

	public NmdMappingType getMappingMethod() {
		return mappingMethod;
	}

	public boolean hasMapping() {
		return this.mappingMethod != NmdMappingType.None;
	}

	public List<Integer> getProductIds() {

		return this.getMpgObject() == null ? new ArrayList<Integer>()
				: this.getMpgObject().getListedMaterials().stream().filter(m -> m.getMapId() > 0).map(m -> m.getMapId())
						.collect(Collectors.toList());
	}

	@JsonIgnore
	public List<NmdProductCard> getNmdProductCards() {
		return this.getStore().getProductCards(this.getProductIds());
	}

	public void removeProductCards() {
		this.getMpgObject().getListedMaterials().forEach(mat -> mat.clearMap());
	}

	public void mapProductCard(MaterialSource mat, NmdProductCard card) {
		// add the material to the mpgObject if it has not been added yet.
		if (this.getMpgObject().getListedMaterials().isEmpty() || this.getMpgObject().getListedMaterials().stream()
				.filter(m -> m.getOid().equals(mat.getOid())).count() == 0) {
			this.getMpgObject().addMaterialSource(mat);
		}

		mat.setMapping(card);
		this.getStore().addProductCard(card);
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

	/**
	 * Indicates that all the materials have a mapping and that the material has at
	 * least a single material
	 * 
	 * @return flag to indicate all materials are mapped to an nmdProductCard
	 */
	public boolean getIsFullyCovered() {
		return this.getMpgObject() != null
				&& ((this.getMpgObject().getListedMaterials().stream().allMatch(m -> m.getMapId() > 0)
						&& this.getMpgObject().getListedMaterials().size() > 0)
						|| this.getNmdProductCards().stream().anyMatch(pc -> pc.getIsTotaalProduct()));
	}

	/**
	 * determines whether an element is equal in values to another element for the
	 * sake of mapping grouping This will therefore not include an equality
	 * 
	 * @return
	 */
	public String getValueHash() {
		return this.getMpgObject().getValueHash();
	}

	public boolean copyMappingFromElement(MpgElement element) {
			this.setMappingMethod(element.getMappingMethod());
			return this.getMpgObject().copyMappingFromObject(element.getMpgObject());
	}

	/**
	 * Returns the amount of product card needed based on the geometry of the object and
	 * the unti fo the product card.
	 * 
	 * @example: A door of 2 x 0.9 x 0.1 meters can be (theoretically) mapped on any product card
	 * When it is mapped on a card with a m^2 unit it will return 1.8 (2 8 0.9). If it is mapped on a  
	 * product card with unit m it will return 2 units and for a card with unit m^3 it will return 0.18 units.
	 * 
	 * When the product is stated per piece (p) it will always return 1, but be sure to check reference dimensions!
	 * 
	 * @param card The product card mapped on a mgp element
	 * @return The number of units required to for the product card to cover the mpg object.
	 */
	public double getRequiredNumberOfUnits(NmdProductCard card) {

		if (this.getMpgObject() == null || card.getProfileSets().size() == 0) {
			return Double.NaN;
		}
		
		MpgGeometry geom = this.getMpgObject().getGeometry();

		String productUnit = card.getUnit().toLowerCase();
		if (productUnit.equals("m1")) {
			return geom.getPrincipalDimension();
		}
		if (productUnit.equals("m2")) {
			return geom.getFaceArea();
		}
		if (productUnit.equals("m3")) {
			// In case the card has a volume unit and the object has layers we need to get the ratio
			if(this.mpgObject.getLayers().size() > 0) {
				MpgLayer layer = this.getMpgObject().getLayerByProductId(card.getProductId());
				return (layer == null ? 0.0 : layer.getVolume()) * geom.getVolume();
			}
			
			return geom.getVolume();
		}
		if (productUnit.equals("p")) {
			return 1.0; // product per piece. always return 1 per profielset.
		}
		if (productUnit.equals("kg")) {
			return Double.NaN; // we do not have densities of products, we will need to figure out how to fix
								// this.
		}

		return Double.NaN;
	}
}
