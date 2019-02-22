package org.opensourcebim.nmd;

import java.util.List;
import java.util.stream.Collectors;

import org.opensourcebim.ifccollection.MpgElement;

public class NmdUserMap {

	private Double[] dimensions;
	private List<String> materials;
	private String ifcProductType;
	
	private List<Integer> productIds;
	
	private NmdUserMap() {}
	
	public static NmdUserMap createUserMapFromElement(MpgElement element) {
		NmdUserMap map = new NmdUserMap();
		map.setDimensions(element.getMpgObject().getGeometry().getDimensions());
		map.setMaterials(element.getMpgObject().getMaterialNamesBySource(null));
		map.setIfcProductType(element.getMpgObject().getObjectType());
		map.setProductIds(element.getNmdProductCards().stream()
				.map(pc -> pc.getProductId())
				.collect(Collectors.toList()));
		return map;
	}

	public List<Integer> getProductIds() {
		return productIds;
	}

	public void setProductIds(List<Integer> productIds) {
		this.productIds = productIds;
	}

	public String getIfcProductType() {
		return ifcProductType;
	}

	public void setIfcProductType(String ifcProductType) {
		this.ifcProductType = ifcProductType;
	}

	public List<String> getMaterials() {
		return materials;
	}

	public void setMaterials(List<String> materials) {
		this.materials = materials;
	}

	public Double[] getDimensions() {
		return dimensions;
	}

	public void setDimensions(Double[] dimensions) {
		this.dimensions = dimensions;
	}
	
	
}
