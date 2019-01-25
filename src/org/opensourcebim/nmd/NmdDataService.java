package org.opensourcebim.nmd;

import java.util.Date;
import java.util.List;

import org.opensourcebim.ifccollection.MpgElement;

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
	
	List<NmdProfileSet> getSpecsForProducts(List<NmdProductCard> products);
	
	List<NmdFaseProfiel> getPhaseProfiles(List<Integer> ids);
	
	NmdProductCard retrieveMaterial(MpgElement material);

}
