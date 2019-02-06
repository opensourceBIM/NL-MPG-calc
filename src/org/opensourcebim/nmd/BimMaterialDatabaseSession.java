package org.opensourcebim.nmd;

import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;


/**
 * Implementation of the EditableDataService to collect edits to the NMD material data
 * @author vijj
 *
 */
public class BimMaterialDatabaseSession implements EditableDataService {

	private Calendar requestDate;
	
	@Override
	public void login() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setRequestDate(Calendar newDate) {
		this.requestDate = newDate;
	}
	
	@Override
	public Calendar getRequestDate() {
		return this.requestDate;
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
	public List<NmdProductCard> getAllProductSets() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdProductCard> getChildProductsForProductCard(NmdProductCard product) throws FileNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean getTotaalProfielSetForProductCard(NmdProductCard product) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdProductCard> getData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<Integer, NmdProfileSet> getProfileSetsByIds(List<String> ids) {
		// TODO Auto-generated method stub
		return null;
	}
}
