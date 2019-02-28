package org.opensourcebim.nmd;

import java.util.HashMap;

import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.mapping.NmdUserMap;

public interface NmdMappingDataService {

	void connect();
	
	void disconnect();
	
	void addUserMap(NmdUserMap map);
	
	NmdUserMap getApproximateMapForObject(MpgObject object);
	
	HashMap<String, String[]> getNlsfbMappings();
	
	void putNlsfbMapping(String ifcProductType, String[] codes, Boolean append);
}
