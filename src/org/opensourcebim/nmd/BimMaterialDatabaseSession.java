package org.opensourcebim.nmd;

import java.util.Date;
import java.util.List;

import org.opensourcebim.ifccollection.MpgMaterial;


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
	public NmdProductCard retrieveMaterial(MpgMaterial material) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdProductCard> getAllProductSets() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MaterialSpecification> getSpecsForProducts(List<NmdProductCard> products) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdBasisProfiel> getPhaseProfiles(List<Integer> ids) {
		// TODO Auto-generated method stub
		return null;
	}
}
