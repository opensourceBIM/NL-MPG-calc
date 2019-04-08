package org.opensourcebim.nmd;
import java.util.Collection;
import java.util.Set;

import org.opensourcebim.mapping.NlsfbCode;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
public interface NmdProductCard{
		
	String getDescription();

	Boolean getIsTotaalProduct();
	
	Set<NmdProfileSet> getProfileSets();

	void addProfileSet(NmdProfileSet spec);
	void addProfileSets(Collection<NmdProfileSet> specs);

	NlsfbCode getNlsfbCode();
	
	String getUnit();

	Integer getCategory();

	Boolean getIsScalable();

	Integer getLifetime();

	Integer getParentProductId();

	Integer getProductId();

	@JsonIgnore
	Double getProfileSetsCoeficientSum();
}
