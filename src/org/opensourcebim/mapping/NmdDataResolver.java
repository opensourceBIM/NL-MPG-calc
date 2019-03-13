package org.opensourcebim.mapping;

import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.nmd.NmdDataService;
import org.opensourcebim.nmd.NmdMappingDataService;
import org.opensourcebim.nmd.NmdUserDataConfig;

/**
 * Interface to combine different data sources and help the user seelct the right
 * data. A single resolver can have multiple data services (NMD or our own
 * database for now) that each offer suggestions.
 * 
 * 
 * @author vijj
 *
 */
public interface NmdDataResolver {

	void NmdToMpg();

	void setNmdService(NmdDataService nmdDataService);
	
	void setMappingService(NmdMappingDataService nmdMappingService);

	MpgObjectStore getStore();
	
	void setStore(MpgObjectStore store);

	NmdUserDataConfig getConfig();
}
