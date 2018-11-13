package org.opensourcebim.mpgcalculations;

import org.bimserver.models.ifc2x3tc1.IfcProduct;

public interface MpgObjectInterface {
	
	public MpgMaterial getMaterial();
	public IfcProduct getIfcProduct();
}
