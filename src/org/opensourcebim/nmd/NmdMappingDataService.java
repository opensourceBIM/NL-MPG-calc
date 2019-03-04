package org.opensourcebim.nmd;

import java.util.List;
import java.util.Map;

import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.mapping.NmdUserMap;

public interface NmdMappingDataService {

	void connect();
	
	void disconnect();
	
	void addUserMap(NmdUserMap map);
	
	NmdUserMap getApproximateMapForObject(MpgObject object);
	
	Map<String, List<String>> getNlsfbMappings();
	
	Map<String, Long> getKeyWordMappings(Integer minOccurence);
}
