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
	
	List<NmdProductCard> getChildProductsForProductCard(NmdProductCard product) throws FileNotFoundException;
	
	Boolean getTotaalProfielSetForProductCard(NmdProductCard product);

	List<NmdProductCard> getData();

	HashMap<Integer, NmdProfileSet> getProfileSetsByIds(List<String> ids);
}
