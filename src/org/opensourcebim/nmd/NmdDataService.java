package org.opensourcebim.nmd;

import java.io.FileNotFoundException;
import java.util.Calendar;
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
	
	List<NmdProductCard> getChildProductSetsForProductSet(NmdProductCard product) throws FileNotFoundException;
	
	List<NmdFaseProfiel> getFaseProfielenByIds(List<String> ids);
	
	Boolean getIsConnected();



}
