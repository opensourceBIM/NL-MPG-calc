package org.opensourcebim.nmd;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Storage container to contain the lifecycle coefficients to do the MPG
 * calculations. A single instance of this interface can return 1 to n materials
 * linked to it. For isntance: a brick wall MaterialsSpecifications instance
 * might have individual MaterialSpecification objects for Brick, Mortar and
 * Cementing. While the materials have individual envronmental factors it is
 * assumed that they are transported and replaced in a single action
 * 
 * @author vijj
 *
 */
public interface MaterialSpecifications {
	String getName();

	String getDescription();

	int getDataCategory();

	String getProductCode();

	String getElementCode();

	String getElementName();

	default double getDensity() {
		return getMaterials().stream().collect(Collectors.summingDouble(mat -> mat.getMassPerUnit()));
	}

	String getUnit();

	double getDistanceFromProducer();

	double getLifeTime();

	NmdBasisProfiel getTransportProfile();

	Set<MaterialSpecification> getMaterials();

	void addSpecification(MaterialSpecification spec);

	String print();

}
