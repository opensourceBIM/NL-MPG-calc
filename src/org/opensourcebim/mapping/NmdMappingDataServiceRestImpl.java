package org.opensourcebim.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensourcebim.dataservices.RestDataService;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.nmd.NmdMappingDataService;

/**
 * Class to provide an interface between java code and a mapping database
 * The mapping database will store any data that is required to resolve what (Nmd) products to choose
 * based on ifc file data.
 * @author vijj
 * 
 */
public class NmdMappingDataServiceRestImpl extends RestDataService implements NmdMappingDataService {


	public NmdMappingDataServiceRestImpl() {
	}

	@Override
	public void addUserMap(NmdUserMap map) {
	}
	
	@Override
	public void addMappingSet(NmdMappingSet set) {
	}

	@Override
	public NmdUserMap getApproximateMapForObject(MpgObject object) {
		return null;
	}

	@Override
	public HashMap<String, List<String>> getNlsfbMappings() {
		return null;
		
	}
	
	@Override
	public Map<String, Long> getKeyWordMappings(Integer minOccurence) {
		return null;
	}

	@Override
	public List<String> getCommonWords() {
		return null;
	}

	@Override
	public void connect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}
}
