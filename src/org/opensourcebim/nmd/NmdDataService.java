package org.opensourcebim.nmd;

import java.util.Calendar;
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
	
	Calendar getRequestDate();

	void setRequestDate(Calendar newDate);

	List<NmdProductCard> getAllProductSets();
	
	void getProfileSetForProductCard(NmdProductCard product);
	
	List<NmdFaseProfiel> getFaseProfilesByIds(List<Integer> ids);
	
	NmdProductCard retrieveMaterial(MpgElement material);

	Boolean getIsConnected();

}
