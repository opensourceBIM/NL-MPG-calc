package org.opensourcebim.mpgcalculations;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

/**
 * Class to retrieve the material properties from the IfcModel
 * 
 * @author Jasper Vijverberg
 */
public class MpgIfcObjectCollector {

	private HashMap<String, MpgMaterial> mpgMaterials;
	private List<MpgObjectGroup> mpgObjectLinks;
	private List<MpgObject> spaces;

	// reporting units
	private AreaUnit areaUnit = AreaUnit.SQUARED_METER;
	private VolumeUnit volumeUnit = VolumeUnit.CUBIC_METER;

	public MpgIfcObjectCollector() {
		setMpgMaterials(new HashMap<>());
		setMpgObjectLinks(new BasicEList<MpgObjectGroup>());
		setSpaces(new BasicEList<MpgObject>());
	}

	/**
	 * method to read in a IfcModel and retrieve material properties for MPG
	 * calculations
	 * 
	 * @param ifcModel for now only a ifc2x3tc1 IfcModel object
	 */
	public void collectIfcModelObjects(IfcModelInterface ifcModel) {
		getMpgMaterials().clear();

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

				MpgObjectGroup mpgObjectGroup = new MpgObjectGroupImpl(ifcProduct.getOid(), ifcProduct.getGlobalId(),
						ifcProduct.getName(), ifcProduct.getClass().getSimpleName());

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
						getSpaces().add(new MpgObjectImpl(volume, area));
					}

				} else {
					// retrieve information and add found values to the various data objects
					List<Pair<String, Double>> mats = this.getMaterials(ifcProduct, volume);
					mats.forEach((mat) -> {
						// make sure there are only unique values in the material list
						// - multiple objects can have a reference to a single material
						if (mat.getLeft() == "Leer") {
							System.out.println();
						}

						mpgMaterials.put(mat.getLeft(), new MpgMaterial(mat.getLeft()));
						if (geometry != null) {
							mpgObjectGroup
									.addObject(new MpgObjectImpl(mat.getRight(), mpgMaterials.get(mat.getLeft())));
						}
					});
					mpgObjectLinks.add(mpgObjectGroup);
				}
			}
		}
		ReportResults();
	}

	/**
	 * Retrieve the materials from the IfcProduct object and store these as
	 * MpgMaterial objects
	 * 
	 * @param ifcProduct  The ifcProduct object to retrieve the material names from
	 * @param totalVolume The total volume of the product. used to determine ratios
	 *                    of volume in case there are multiple materials defined
	 * @return a list of material names and estimated volume
	 */
	private List<Pair<String, Double>> getMaterials(IfcProduct ifcProduct, double totalVolume) {
		List<Pair<String, Double>> materials = new ArrayList<>();

		EList<IfcRelAssociates> associates = ifcProduct.getHasAssociations();
		if (associates != null) {

			for (IfcRelAssociates ifcRelAssociates : associates) {

				if (ifcRelAssociates instanceof IfcRelAssociatesMaterial) {
					IfcRelAssociatesMaterial matRelation = (IfcRelAssociatesMaterial) ifcRelAssociates;
					IfcMaterialSelect relatingMaterial = matRelation.getRelatingMaterial();

					// try determine what the derived interface of the IfcMaterialSelect is
					if (relatingMaterial instanceof IfcMaterial) {
						IfcMaterial mat = (IfcMaterial) relatingMaterial;
						Pair<String, Double> item = new MutablePair<String, Double>(mat.getName(), 0.0);
						materials.add(item);
					} else if (relatingMaterial instanceof IfcMaterialList) {
						IfcMaterialList mats = (IfcMaterialList) relatingMaterial;
						mats.getMaterials().forEach((mat) -> {
							Pair<String, Double> item = new MutablePair<String, Double>(mat.getName(), 0.0);
							materials.add(item);
						});
					} else if (relatingMaterial instanceof IfcMaterialLayerSetUsage) {
						materials.addAll(GetMaterialLayerList((IfcMaterialLayerSetUsage) relatingMaterial));
					} else if (relatingMaterial instanceof IfcMaterialLayerSet) {
						materials.addAll(GetMaterialLayerList((IfcMaterialLayerSet) relatingMaterial));
					} else if (relatingMaterial instanceof IfcMaterialLayer) {
						materials.add(GetMaterialLayer((IfcMaterialLayer) relatingMaterial));
					}
				}
			}

			// check total volume matches up with found materials and thickness sums and
			// adjust accordingly.
			Double totalThickness = materials.stream().map(mat -> mat.getValue())
					.collect(Collectors.summingDouble(o -> o));
			// if there are layers present the material definitions should relate to the
			// layers
			double volumePerMaterial = totalThickness == 0 ? totalVolume / Math.max(1.0, materials.size())
					: totalVolume;

			materials.forEach(mat -> {
				if (mat.getValue() <= 0.0) {
					mat.setValue(totalThickness == 0 ? volumePerMaterial : Double.NaN);
				} else if (totalThickness != 0) {
					mat.setValue(volumePerMaterial * (mat.getValue() / totalThickness));
				}
			});
		}
		return materials;
	}

	/**
	 * get the relevant data from a material layer object
	 * 
	 * @param layer the material layer object to parse
	 * @return an object with material layer information
	 */
	private Pair<String, Double> GetMaterialLayer(IfcMaterialLayer layer) {
		IfcMaterial material = layer.getMaterial();
		return new MutablePair<String, Double>(material != null ? material.getName() : "unknown",
				layer.getLayerThickness());
	}

	/**
	 * Get the material names from a generic ifcMaterialLayerSet
	 * 
	 * @param layerSet ifcLayerSet object
	 * @return a list of material names and matching thickness.
	 */
	private List<Pair<String, Double>> GetMaterialLayerList(IfcMaterialLayerSet layerSet) {
		return layerSet.getMaterialLayers().stream().map((layer) -> GetMaterialLayer(layer))
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

	private void ReportResults() {
		System.out.println("----------------------------");
		System.out.println("ifc object collection report: ");
		System.out.println(">> found objects : " + mpgMaterials.size());
		mpgObjectLinks.forEach(group -> {
			System.out.println(group.print());
			System.out.println("");
		});
		System.out.println("----------------------------");
	}

	// ---------- Standard getters and setters -------------
	public HashMap<String, MpgMaterial> getMpgMaterials() {
		return mpgMaterials;
	}

	public void setMpgMaterials(HashMap<String, MpgMaterial> mpgMaterials) {
		this.mpgMaterials = mpgMaterials;
	}

	public List<MpgObjectGroup> getMpgObjectLinks() {
		return mpgObjectLinks;
	}

	public void setMpgObjectLinks(List<MpgObjectGroup> mpgObjectLinks) {
		this.mpgObjectLinks = mpgObjectLinks;
	}

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

	public List<MpgObject> getSpaces() {
		return spaces;
	}

	public void setSpaces(List<MpgObject> spaces) {
		this.spaces = spaces;
	}
}
