package org.opensourcebim.nmd;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;


/**
 * Implementation of the EditableDataService to collect edits to the NMD material data
 * @author vijj
 *
 */
public class BimMaterialDatabaseSession implements EditableDataService {

	@Override
	public void login() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Boolean getIsConnected() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void preLoadData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Calendar getRequestDate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRequestDate(Calendar newDate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<NmdElement> getData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<Integer, NmdProfileSet> getProfileSetsByIds(List<String> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdProductCard> getProductsForElement(NmdElement element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdElement> getAllElements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean getAdditionalProfileDataForCard(NmdProductCard c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NmdProductCard getFullProductCardById(Integer productId) {
		// TODO Auto-generated method stub
		return null;
	}

}
