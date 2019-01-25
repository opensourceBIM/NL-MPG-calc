package org.opensourcebim.nmd;

import java.util.Date;
import java.util.List;

import org.opensourcebim.ifccollection.MpgMaterial;

/**
 * Standard interface to provide material data from the source to the user.
 * 
 * @author vijj
 *
 */
public interface NmdDataService {

	void login();

	void logout();
	
	void setRequestDate(Date newDate);

	List<NmdProductCard> getAllProductSets();
	
	List<MaterialSpecification> getSpecsForProducts(List<NmdProductCard> products);
	
	List<NmdBasisProfiel> getPhaseProfiles(List<Integer> ids);
	
	NmdProductCard retrieveMaterial(MpgMaterial material);

}
