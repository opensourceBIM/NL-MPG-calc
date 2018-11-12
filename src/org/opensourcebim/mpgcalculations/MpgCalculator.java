package org.opensourcebim.mpgcalculations;

import java.util.ArrayList;
import java.util.List;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.*;
import org.bimserver.plugins.services.BimBotAbstractService;
import org.bimserver.utils.IfcUtils;
import org.bimserver.utils.VolumeUnit;

import com.google.common.base.Joiner;

public class MpgCalculator extends BimBotAbstractService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, SObjectType settings)
			throws BimBotsException {

		IfcModelInterface ifcModel = input.getIfcModel();

		// get model generic stuff
		VolumeUnit volumeUnit = IfcUtils.getVolumeUnit(ifcModel);

		

		for (IfcProduct ifcProduct : ifcModel.getAllWithSubTypes(IfcProduct.class)) {
			System.out.println(ifcProduct);

			
			// ToDo: get units?			
			GeometryInfo geometry = ifcProduct.getGeometry();
			if (geometry != null) {
				System.out.println(volumeUnit.convert(geometry.getVolume(), VolumeUnit.CUBIC_METER));
			}
			
			List<String> mats = this.getMaterials(ifcProduct);

			System.out.println(Joiner.on(",").join(mats));
		}
		// TODO Auto-generated method stub
		return new BimBotsOutput("some useful content", new byte[0]);
	}

	@Override
	public String getOutputSchema() {
		// TODO Auto-generated method stub
		return "some useful content";
	}
	
	@Override
	public boolean preloadCompleteModel() {
		return true;
	}
	
	@Override
	public boolean requiresGeometry() {
		return true;
	}

	/**
	 * Retrieve the materials from the IfcProduct object
	 * 
	 * @param ifcProduct
	 * @return
	 */
	private List<String> getMaterials(IfcProduct ifcProduct) {
		List<String> materialNames = new ArrayList<>();
		for (IfcRelAssociates ifcRelAssociates : ifcProduct.getHasAssociations()) {

			if (ifcRelAssociates instanceof IfcRelAssociatesMaterial) {
				IfcRelAssociatesMaterial matRelation = (IfcRelAssociatesMaterial) ifcRelAssociates;
				IfcMaterialSelect relatingMaterial = matRelation.getRelatingMaterial();

				// from the interface try determine what the implementation class is.
				if (relatingMaterial instanceof IfcMaterial) {
					IfcMaterial mat = (IfcMaterial) relatingMaterial;
					materialNames.add(mat.getName());
				} else if (relatingMaterial instanceof IfcMaterialList) {
					IfcMaterialList mats = (IfcMaterialList) relatingMaterial;
					for (IfcMaterial mat : mats.getMaterials()) {
						materialNames.add(mat.getName());
					}
				} else if (relatingMaterial instanceof IfcMaterialLayerSetUsage) {

				} else if (relatingMaterial instanceof IfcMaterialLayerSet) {

				} else if (relatingMaterial instanceof IfcMaterialLayer) {

				}
			}
		}
		return materialNames;
	}
}
