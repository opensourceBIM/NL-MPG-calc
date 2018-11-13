package org.opensourcebim.mpgcalculations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcMaterial;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayer;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSet;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSetUsage;
import org.bimserver.models.ifc2x3tc1.IfcMaterialList;
import org.bimserver.models.ifc2x3tc1.IfcMaterialSelect;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociates;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociatesMaterial;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.utils.AreaUnit;
import org.bimserver.utils.IfcUtils;
import org.bimserver.utils.VolumeUnit;
import org.eclipse.emf.common.util.EList;

import com.google.common.base.Joiner;

/**
 * Class to retrieve the material properties from the IfcModel
 * 
 * @author Jasper Vijverberg
 */
public class MpgIfcParser {

	private HashMap<String, MpgMaterial> mpgMaterials;
	private List<MpgObjectInterface> mpgObjectLinks;

	public MpgIfcParser() {
		setMpgMaterials(new HashMap<>());
	}

	public void parseIfcModel(IfcModelInterface ifcModel) {
		getMpgMaterials().clear();
		// get model generic stuff
		VolumeUnit volumeUnit = IfcUtils.getVolumeUnit(ifcModel);
		AreaUnit areaUnit = IfcUtils.getAreaUnit(ifcModel);

		for (IfcProduct ifcProduct : ifcModel.getAllWithSubTypes(IfcProduct.class)) {
			
			if (ifcProduct instanceof IfcSpace) {
				
			}
			
			GeometryInfo geometry = ifcProduct.getGeometry();
			if (geometry != null) {
				double area = areaUnit.convert(geometry.getArea(), AreaUnit.SQUARED_METER);
				double volume = volumeUnit.convert(geometry.getVolume(), VolumeUnit.CUBIC_METER);
				System.out.println("Area: " + area + ", Volume: " + volume);
			}

			// ToDo: get layer thickness?
			List<String> mats = this.getMaterials(ifcProduct);
			mats.forEach((name) -> mpgMaterials.put(name, new MpgMaterial(name)));
			
			
			System.out.println(mats.size());
		}
		
		//ReportResults();
	}

	/**
	 * Retrieve the materials from the IfcProduct object and store these as
	 * MpgMaterial objects
	 * 
	 * @param ifcProduct the ifcProduct object to retrieve the material names from
	 * @return a list of material names
	 */
	private List<String> getMaterials(IfcProduct ifcProduct) {
		List<String> materialNames = new ArrayList<>();
		EList<IfcRelAssociates> associates = ifcProduct.getHasAssociations();
		if (associates != null) {
			for (IfcRelAssociates ifcRelAssociates : associates) {

				if (ifcRelAssociates instanceof IfcRelAssociatesMaterial) {
					IfcRelAssociatesMaterial matRelation = (IfcRelAssociatesMaterial) ifcRelAssociates;
					IfcMaterialSelect relatingMaterial = matRelation.getRelatingMaterial();

					// from the interface try determine what the implementation class is.
					if (relatingMaterial instanceof IfcMaterial) {
						IfcMaterial mat = (IfcMaterial) relatingMaterial;
						materialNames.add(mat.getName());
					} else if (relatingMaterial instanceof IfcMaterialList) {
						IfcMaterialList mats = (IfcMaterialList) relatingMaterial;
						mats.getMaterials().forEach((mat) -> materialNames.add(mat.getName()));
					} else if (relatingMaterial instanceof IfcMaterialLayerSetUsage) {
						materialNames.addAll(GetMaterialLayerList((IfcMaterialLayerSetUsage) relatingMaterial));
					} else if (relatingMaterial instanceof IfcMaterialLayerSet) {
						materialNames.addAll(GetMaterialLayerList((IfcMaterialLayerSet) relatingMaterial));
					} else if (relatingMaterial instanceof IfcMaterialLayer) {
						materialNames.add(((IfcMaterialLayer) relatingMaterial).getMaterial().getName());
					}
				}
			}
		}
		return materialNames;
	}

	/**
	 * Get the material names from a generic ifcMaterialLayerSet
	 * 
	 * @param layerSet ifcLayerSet object
	 * @return a list of material names.
	 */
	private List<String> GetMaterialLayerList(IfcMaterialLayerSet layerSet) {
		return layerSet.getMaterialLayers().stream().map((layer) -> layer.getMaterial().getName())
				.collect(Collectors.toList());
	}

	private List<String> GetMaterialLayerList(IfcMaterialLayerSetUsage layerSetUsage) {
		;
		return GetMaterialLayerList(layerSetUsage.getForLayerSet());
	}

	public HashMap<String, MpgMaterial> getMpgMaterials() {
		return mpgMaterials;
	}

	public void setMpgMaterials(HashMap<String, MpgMaterial> mpgMaterials) {
		this.mpgMaterials = mpgMaterials;
	}

	private void ReportResults() {
		System.out.println("-------");
		System.out.println("ifc file parse report: ");
		System.out.println(">> found materials : " + mpgMaterials.size());
		mpgMaterials.forEach((key, val) -> {
			System.out.println(key);

		});
		System.out.println("");
	}

}
