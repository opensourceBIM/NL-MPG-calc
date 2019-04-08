package org.opensourcebim.mapping;

import org.opensourcebim.ifccollection.MpgObjectStore;

import nl.tno.bim.nmd.config.UserConfig;
import nl.tno.bim.nmd.services.NmdDataService;

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
	
	void setMappingService(MappingDataService nmdMappingService);

	MpgObjectStore getStore();
	
	void setStore(MpgObjectStore store);
}
