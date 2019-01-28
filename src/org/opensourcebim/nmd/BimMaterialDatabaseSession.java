package org.opensourcebim.nmd;

import java.util.Date;
import java.util.List;

import org.opensourcebim.ifccollection.MpgElement;


/**
 * Implementation of the EditableDataService to collect edits to the NMD material data
 * @author vijj
 *
 */
public class BimMaterialDatabaseSession implements EditableDataService {

	private Date requestDate;
	
	@Override
	public void login() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setRequestDate(Date newDate) {
		this.requestDate = newDate;
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

	@Override
	public Date getRequestDate() {
		return this.requestDate;
	}

	@Override
	public Boolean getIsConnected() {
		return true;
	}
}
