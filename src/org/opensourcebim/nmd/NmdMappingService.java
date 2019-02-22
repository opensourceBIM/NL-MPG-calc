package org.opensourcebim.nmd;

import org.opensourcebim.ifccollection.MpgObject;

public interface NmdMappingService {

	void connect();
	
	void addUserMap(NmdUserMap map);
	
	NmdUserMap getApproximateMapForObject(MpgObject object);
	
}
