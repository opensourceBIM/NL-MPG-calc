package org.opensourcebim.nmd;

import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Standard interface to provide material data from the source to the user.
 * 
 * @author vijj
 *
 */
public interface NmdDataService {

	void login();

	void logout();
	
	Boolean getIsConnected();

	void preLoadData();
	
	Calendar getRequestDate();

	void setRequestDate(Calendar newDate);

	List<NmdProductCard> getAllProductSets();
	
	List<NmdProductCard> getChildProductSetsForProductSet(NmdProductCard product) throws FileNotFoundException;
	
	Boolean getProfielSetsByProductCard(NmdProductCard product);
	
	HashMap<Integer, NmdProfileSet> getProfileSetsByIds(List<String> ids);

	List<NmdProductCard> getData();
}
