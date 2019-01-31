package org.opensourcebim.nmd;
import java.util.Set;

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
public interface NmdProductCard {
	String getName();

	String getDescription();

	int getDataCategory();

	String getNLsfbCode();

	String getRAWCode();

	String getElementName();
	
	double getLifeTime();

	Set<NmdProfileSet> getProfileSets();

	void addProfileSet(NmdProfileSet spec);

	String print();
}
