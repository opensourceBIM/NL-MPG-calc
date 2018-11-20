package org.opensourcebim.nmd;

import java.util.List;

/**
 * Sotrage container to contain the lifecycle coefficients to do the MPG
 * calculations.
 * 
 * @author vijj
 *
 */
public interface NmdMaterialSpecifications {
	public String getName();

	public String getDescription();

	public int getDataCategory();

	public String getProductCode();

	public String getElementCode();

	public String getElementName();

	public double getDensity();

	public String getUnit();

	public double getDistanceFromProducer();

	public NmdBasisProfiel getTransportProfile();

	public List<NmdMaterialSpecification> getNMDMaterials();

	public String print();

}
