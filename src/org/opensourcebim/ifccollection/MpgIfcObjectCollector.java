package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcBuildingElement;
import org.bimserver.models.ifc2x3tc1.IfcMaterial;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayer;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSet;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSetUsage;
import org.bimserver.models.ifc2x3tc1.IfcMaterialList;
import org.bimserver.models.ifc2x3tc1.IfcMaterialSelect;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociates;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociatesMaterial;
import org.bimserver.models.ifc2x3tc1.IfcRelDecomposes;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.utils.AreaUnit;
import org.bimserver.utils.IfcUtils;
import org.bimserver.utils.VolumeUnit;
import org.eclipse.emf.common.util.EList;

/**
 * Class to retrieve the material properties from the IfcModel
 * 
 * @author Jasper Vijverberg
 */
public class MpgIfcObjectCollector {

	private MpgObjectStore objectStore;

	// reporting units and imported units to help convert measurements
	private AreaUnit areaUnit = AreaUnit.SQUARED_METER;
	private VolumeUnit volumeUnit = VolumeUnit.CUBIC_METER;
	private AreaUnit modelAreaUnit;
	private VolumeUnit modelVolumeUnit;
	
	public MpgIfcObjectCollector() {
		objectStore = new MpgObjectStoreImpl();
	}

	public MpgObjectStore results() {
		return this.objectStore;
	}

	/**
	 * method to read in a IfcModel and retrieve material properties for MPG
	 * calculations
	 * 
	 * @param ifcModel for now only a ifc2x3tc1 IfcModel object
	 */
	public MpgObjectStore collectIfcModelObjects(IfcModelInterface ifcModel) {
		objectStore.Reset();

		// get project wide parameters
		modelVolumeUnit = IfcUtils.getVolumeUnit(ifcModel);
		modelAreaUnit = IfcUtils.getAreaUnit(ifcModel);

		// loop through IFcSpaces
		for (IfcSpace space : ifcModel.getAllWithSubTypes(IfcSpace.class)) {

			EList<IfcRelDecomposes> parentDecomposedProduct = space.getIsDecomposedBy();

			// if there is any space that decomposes in this space we can omit the addition
			// of the volume
			boolean isIncludedSemantically = false;
			if (parentDecomposedProduct != null) {
				isIncludedSemantically = parentDecomposedProduct.stream()
						.filter(relation -> relation.getRelatingObject() instanceof IfcSpace).count() > 0;
			}

			// ToDo: include geometric check
			boolean isIncludedGeometrically = false;
			
			Pair<Double, Double> geom = getGeometryFromProduct(space);
			
			if (!isIncludedGeometrically && !isIncludedSemantically) {
				objectStore.getSpaces().add(new MpgSubObjectImpl(geom.getRight(), geom.getLeft()));
			}
		}

		Map<String, String> childToParentMap = new HashMap<String,String>();
		// loop through IfcBuildingElements recursively.
		for (IfcBuildingElement element : ifcModel.getAllWithSubTypes(IfcBuildingElement.class)) {

			// manage child to parent mapping
			element.getDecomposes().stream()
				.map(rel -> rel.getRelatingObject()).filter(o -> o instanceof IfcBuildingElement)
				.map(o -> (IfcBuildingElement)o)
				.forEach(o -> {
					if (!childToParentMap.containsKey(element.getGlobalId()) && o.getGlobalId() != element.getGlobalId()) {
						childToParentMap.put(element.getGlobalId(), o.getGlobalId());
					}else
					{
						if(o.getGlobalId() != element.getGlobalId()) {
							System.out.println(">> " + element.getGlobalId() + ", " + o.getGlobalId());
						}
					}

				});
			
			Pair<Double, Double> geom = getGeometryFromProduct(element);
			
			// retrieve information and add found values to the various data objects
			this.createMpgObjectFromIfcProduct(element, geom.getRight());
		}
		
		// set all parent child relations for elements
		objectStore.getObjects().forEach(o -> {
			if (childToParentMap.containsKey(o.getGlobalId())) {
				o.setParentId(childToParentMap.get(o.getGlobalId()));
			}
		});
		
		return objectStore;
	}
	
	private Pair<Double, Double> getGeometryFromProduct(IfcProduct prod){
		GeometryInfo geometry = prod.getGeometry();
		double area;
		double volume;
		if (geometry != null) {
			area = this.getAreaUnit().convert(geometry.getArea(), modelAreaUnit);
			volume = this.getVolumeUnit().convert(geometry.getVolume(), modelVolumeUnit);
		} else {
			area = 0.0;
			volume = 0.0;
		}
		return new ImmutablePair<Double, Double>(area,  volume);
		
	}

	/**
	 * Retrieve the materials and layers from the IfcProduct object and store these
	 * as MpgMaterial objects
	 * 
	 * @param ifcProduct  The ifcProduct object to retrieve the material names from
	 * @param totalVolume The total volume of the product. used to determine ratios
	 *                    of volume in case there are multiple materials defined
	 */
	private void createMpgObjectFromIfcProduct(IfcProduct ifcProduct, double totalVolume) {

		MpgObjectImpl mpgObject = new MpgObjectImpl(
				ifcProduct.getOid(), ifcProduct.getGlobalId(), ifcProduct.getName(), ifcProduct.getClass().getSimpleName(), "", objectStore);
		mpgObject.setVolume(totalVolume);

		EList<IfcRelAssociates> associates = ifcProduct.getHasAssociations();
		if (associates != null) {

			List<Triple<String, String, Double>> productLayers = new ArrayList<Triple<String, String, Double>>();
			Map<String, String> productMaterials = new HashMap<String, String>();

			for (IfcRelAssociates ifcRelAssociates : associates) {

				if (ifcRelAssociates instanceof IfcRelAssociatesMaterial) {
					IfcRelAssociatesMaterial matRelation = (IfcRelAssociatesMaterial) ifcRelAssociates;
					IfcMaterialSelect relatingMaterial = matRelation.getRelatingMaterial();

					// try determine what the derived interface of the IfcMaterialSelect is
					if (relatingMaterial instanceof IfcMaterial) {
						IfcMaterial mat = (IfcMaterial) relatingMaterial;
						productMaterials.put(Long.toString(mat.getOid()), mat.getName());
					} else if (relatingMaterial instanceof IfcMaterialList) {
						IfcMaterialList mats = (IfcMaterialList) relatingMaterial;
						mats.getMaterials()
								.forEach((mat) -> productMaterials.put(Long.toString(mat.getOid()), mat.getName()));
					} else if (relatingMaterial instanceof IfcMaterialLayerSetUsage) {
						productLayers.addAll(getMaterialLayerList((IfcMaterialLayerSetUsage) relatingMaterial));
					} else if (relatingMaterial instanceof IfcMaterialLayerSet) {
						productLayers.addAll(getMaterialLayerList((IfcMaterialLayerSet) relatingMaterial));
					} else if (relatingMaterial instanceof IfcMaterialLayer) {
						productLayers.addAll(getMaterialLayer((IfcMaterialLayer) relatingMaterial));
					}
				}
			}

			// check total volume matches up with found materials and thickness sums and
			// adjust accordingly.
			double totalThickness = productLayers.stream().collect(Collectors.summingDouble(o -> o.getRight()));

			// add separately listed materials
			productMaterials.forEach((key, value) -> {
				objectStore.addMaterial(value);
				mpgObject.addListedMaterial(value, key);
			});
			// add layers and any materials that have been found with those layers
			productLayers.forEach(layer -> {
				String materialName = layer.getLeft();
				String materialGuid = layer.getMiddle();
				double volumeRatio = layer.getRight() / totalThickness * totalVolume;
				mpgObject.addSubObject(new MpgSubObjectImpl(volumeRatio, materialName, materialGuid));
				objectStore.addMaterial(materialName);
			});

			objectStore.getObjects().add(mpgObject);
		}
	}

	/**
	 * get the relevant data from a material layer object
	 * 
	 * @param layer the material layer object to parse
	 * @return an object with material layer information
	 */
	private List<Triple<String, String, Double>> getMaterialLayer(IfcMaterialLayer layer) {
		IfcMaterial material = layer.getMaterial();
		List<Triple<String, String, Double>> res = new ArrayList<Triple<String, String, Double>>();
		MutableTriple<String, String, Double> triple = new MutableTriple<String, String, Double>(
				material != null ? material.getName() : "", material != null ? Long.toString(material.getOid()) : "",
				layer.getLayerThickness());

		res.add(triple);
		return res;
	}

	/**
	 * Get the material names from a generic ifcMaterialLayerSet
	 * 
	 * @param layerSet ifcLayerSet object
	 * @return a list of material names and matching thickness.
	 */
	private List<Triple<String, String, Double>> getMaterialLayerList(IfcMaterialLayerSet layerSet) {
		return layerSet.getMaterialLayers().stream().flatMap((layer) -> getMaterialLayer(layer).stream())
				.collect(Collectors.toList());
	}

	/**
	 * polymorphic method of the MaterialLayerSet implementation.
	 * 
	 * @param layerSetUsage ifcLayerSetUsage object
	 * @return a list of material names
	 */
	private List<Triple<String, String, Double>> getMaterialLayerList(IfcMaterialLayerSetUsage layerSetUsage) {
		return getMaterialLayerList(layerSetUsage.getForLayerSet());
	}

	// ---------- Standard getters and setters -------------
	public AreaUnit getAreaUnit() {
		return areaUnit;
	}

	public void setAreaUnit(AreaUnit areaUnit) {
		this.areaUnit = areaUnit;
	}

	public VolumeUnit getVolumeUnit() {
		return volumeUnit;
	}

	public void setVolumeUnit(VolumeUnit volumeUnit) {
		this.volumeUnit = volumeUnit;
	}
}
