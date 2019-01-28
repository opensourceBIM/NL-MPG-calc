package org.opensourcebim.nmd;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.opensourcebim.ifccollection.MpgElement;


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
		return true;
	}

	@Override
	public NmdProductCard retrieveMaterial(MpgElement material) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdProductCard> getAllProductSets() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdProfileSet> getSpecsForProducts(List<NmdProductCard> products) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdFaseProfiel> getFaseProfilesByIds(List<Integer> ids) {
		// TODO Auto-generated method stub
		return null;
	}
}
