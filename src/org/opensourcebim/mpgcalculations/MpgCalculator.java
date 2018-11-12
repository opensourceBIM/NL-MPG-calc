package org.opensourcebim.mpgcalculations;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.models.ifc2x3tc1.IfcMaterialSelect;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociates;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociatesMaterial;
import org.bimserver.plugins.services.BimBotAbstractService;

public class MpgCalculator extends BimBotAbstractService{

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, SObjectType settings)
			throws BimBotsException {
		
		IfcModelInterface ifcModel = input.getIfcModel();
		
		for (IfcProduct ifcProduct : ifcModel.getAllWithSubTypes(IfcProduct.class)) {
			System.out.println(ifcProduct);

			for (IfcRelAssociates ifcRelAssociates : ifcProduct.getHasAssociations()) {
				if(ifcRelAssociates instanceof IfcRelAssociatesMaterial){
					IfcRelAssociatesMaterial matRelation = (IfcRelAssociatesMaterial)ifcRelAssociates;
					IfcMaterialSelect relatingMaterial = matRelation.getRelatingMaterial();
					System.out.println(relatingMaterial);
				}
			}
		}
		// TODO Auto-generated method stub
		return new BimBotsOutput("some useful content", new byte[0]);
	}

	@Override
	public String getOutputSchema() {
		// TODO Auto-generated method stub
		return "some useful content";
	}

}
