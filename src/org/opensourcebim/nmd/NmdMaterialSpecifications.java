package org.opensourcebim.nmd;

import java.util.List;

/**
 * Storage container to contain the lifecycle coefficients to do the MPG
 * calculations. A single instance of this interface can return 1 to n materials
 * linked to it. For isntance: a bric wall might have individual
 * MaterialSpecification objects for Brick, Mortar and Cementing While the
 * materials have individual envronmental factors it is assumed that they are
 * transported and replaced in a single action
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
