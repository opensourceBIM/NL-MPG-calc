package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcFurnishingElement;
import org.bimserver.models.ifc2x3tc1.IfcMaterial;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayer;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSet;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSetUsage;
import org.bimserver.models.ifc2x3tc1.IfcMaterialList;
import org.bimserver.models.ifc2x3tc1.IfcMaterialSelect;
import org.bimserver.models.ifc2x3tc1.IfcOpeningElement;
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

	// reporting units
	private AreaUnit areaUnit = AreaUnit.SQUARED_METER;
	private VolumeUnit volumeUnit = VolumeUnit.CUBIC_METER;

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
		VolumeUnit modelVolumeUnit = IfcUtils.getVolumeUnit(ifcModel);
		AreaUnit modelAreaUnit = IfcUtils.getAreaUnit(ifcModel);

		double volume;
		double area;

		for (IfcProduct ifcProduct : ifcModel.getAllWithSubTypes(IfcProduct.class)) {

			if (ifcProduct instanceof IfcFurnishingElement || ifcProduct instanceof IfcOpeningElement) {
				// we're not taking furnishing into account
				continue;
			}

			GeometryInfo geometry = ifcProduct.getGeometry();
			volume = 0.0;
			area = 0.0;

			if (geometry != null) {
				area = modelAreaUnit.convert(geometry.getArea(), this.getAreaUnit());
				volume = modelVolumeUnit.convert(geometry.getVolume(), this.getVolumeUnit());

				// check space is referenced by another space or is geometrically included in
				// another space
				if (ifcProduct instanceof IfcSpace) {
					EList<IfcRelDecomposes> parentDecomposedProduct = ifcProduct.getIsDecomposedBy();

					// if there is any space that decomposes in this space we can omit the addition
					// of the volume
					boolean isIncludedSemantically = false;
					if (parentDecomposedProduct != null) {
						isIncludedSemantically = parentDecomposedProduct.stream()
								.filter(relation -> relation.getRelatingObject() instanceof IfcSpace).count() > 0;
					}

					// ToDo: include geometric check
					boolean isIncludedGeometrically = false;

					if (!isIncludedGeometrically && !isIncludedSemantically) {
						objectStore.getSpaces().add(new MpgSubObjectImpl(volume, area));
					}

				} else {
					// retrieve information and add found values to the various data objects
					this.createMpgObjectFromIfcProduct(ifcProduct, volume);
				}
			}
		}
				
		return objectStore;
	}

	/**
	 * Retrieve the materials and layers from the IfcProduct object and store these as
	 * MpgMaterial objects
	 * 
	 * @param ifcProduct  The ifcProduct object to retrieve the material names from
	 * @param totalVolume The total volume of the product. used to determine ratios
	 *                    of volume in case there are multiple materials defined
	 */
	private void createMpgObjectFromIfcProduct(IfcProduct ifcProduct, double totalVolume) {
		
		MpgObjectImpl mpgObject = new MpgObjectImpl(ifcProduct.getOid(), ifcProduct.getGlobalId(),
				ifcProduct.getName(), ifcProduct.getClass().getSimpleName(), objectStore);
		mpgObject.setVolume(totalVolume);

		EList<IfcRelAssociates> associates = ifcProduct.getHasAssociations();
		if (associates != null) {

			
			List<Pair<String, Double>> productLayers = new ArrayList<Pair<String, Double>>();
			List<String> productMaterials = new ArrayList<String>();
			
			for (IfcRelAssociates ifcRelAssociates : associates) {

				if (ifcRelAssociates instanceof IfcRelAssociatesMaterial) {
					IfcRelAssociatesMaterial matRelation = (IfcRelAssociatesMaterial) ifcRelAssociates;
					IfcMaterialSelect relatingMaterial = matRelation.getRelatingMaterial();

					// try determine what the derived interface of the IfcMaterialSelect is
					if (relatingMaterial instanceof IfcMaterial) {
						IfcMaterial mat = (IfcMaterial) relatingMaterial;
						productMaterials.add(mat.getName());
					} else if (relatingMaterial instanceof IfcMaterialList) {
						IfcMaterialList mats = (IfcMaterialList) relatingMaterial;
						mats.getMaterials().forEach((mat) -> productMaterials.add(mat.getName()));
					} else if (relatingMaterial instanceof IfcMaterialLayerSetUsage) {
						productLayers.addAll(GetMaterialLayerList((IfcMaterialLayerSetUsage) relatingMaterial));
					} else if (relatingMaterial instanceof IfcMaterialLayerSet) {
						productLayers.addAll(GetMaterialLayerList((IfcMaterialLayerSet) relatingMaterial));
					} else if (relatingMaterial instanceof IfcMaterialLayer) {
						productLayers.addAll(GetMaterialLayer((IfcMaterialLayer) relatingMaterial));
					}
					
				}
			}

			// check total volume matches up with found materials and thickness sums and
			// adjust accordingly.
			double totalThickness = productLayers.stream().collect(Collectors.summingDouble(o -> o.getRight()));		
			
			// add separately listed materials
			productMaterials.forEach((mat) -> {
				objectStore.addMaterial(mat);
				mpgObject.addListedMaterial(mat);
			});
			// add layers and any materials that have been found with those layers
			productLayers.forEach(layer -> {
				String materialName = layer.getKey();
				double volumeRatio = layer.getValue() / totalThickness * totalVolume;
				mpgObject.addSubObject(new MpgSubObjectImpl(volumeRatio, materialName));
				mpgObject.addListedMaterial(materialName);
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
	private List<Pair<String, Double>> GetMaterialLayer(IfcMaterialLayer layer) {
		IfcMaterial material = layer.getMaterial();
		List<Pair<String, Double>> res = new ArrayList<Pair<String, Double>>();
		res.add(new MutablePair<String, Double>(material != null ? material.getName() : "", layer.getLayerThickness()));
		return res;
	}

	/**
	 * Get the material names from a generic ifcMaterialLayerSet
	 * 
	 * @param layerSet ifcLayerSet object
	 * @return a list of material names and matching thickness.
	 */
	private List<Pair<String, Double>> GetMaterialLayerList(IfcMaterialLayerSet layerSet) {	
		return layerSet.getMaterialLayers().stream().flatMap((layer) -> GetMaterialLayer(layer).stream())
				.collect(Collectors.toList());
	}

	/**
	 * polymorphic method of the MaterialLayerSet implementation.
	 * 
	 * @param layerSetUsage ifcLayerSetUsage object
	 * @return a list of material names
	 */
	private List<Pair<String, Double>> GetMaterialLayerList(IfcMaterialLayerSetUsage layerSetUsage) {
		return GetMaterialLayerList(layerSetUsage.getForLayerSet());
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
