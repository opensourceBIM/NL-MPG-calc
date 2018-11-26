package org.opensourcebim.nmd;

import org.opensourcebim.ifccollection.MpgObjectStore;

/**
 * Interface to combine different data sources and help the user seelct the right
 * data. A single resolver can have multiple data services (NMD or our own
 * database for now) that each offer suggestions.
 * 
 * 
 * @author vijj
 *
 */
public interface NmdDataResolverService {

	MpgObjectStore NmdToMpg(MpgObjectStore ifcResults);

	void addService(NmdDataService nmdDataService);
}
