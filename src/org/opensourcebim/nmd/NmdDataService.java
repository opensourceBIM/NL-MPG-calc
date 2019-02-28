package org.opensourcebim.nmd;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.opensourcebim.mapping.NlsfbCode;

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

	List<NmdElement> getAllElements();
			
	List<NmdElement> getData();

	HashMap<Integer, NmdProfileSet> getProfileSetsByIds(List<Integer> ids);

	List<NmdProductCard> getProductsForElement(NmdElement element);
	List<NmdProductCard> getProductsForNLsfbCodes(Set<NlsfbCode> codes);
	List<NmdElement> getElementsForNLsfbCodes(Set<NlsfbCode> codes); 

	Boolean getAdditionalProfileDataForCard(NmdProductCard c);
}
