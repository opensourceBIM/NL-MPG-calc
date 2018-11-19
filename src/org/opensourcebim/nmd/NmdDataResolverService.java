package org.opensourcebim.nmd;

import org.opensourcebim.ifccollection.MpgObjectStore;

public interface NmdDataResolverService {

	MpgObjectStore NmdToMpg(MpgObjectStore ifcResults);

	void addService(NmdDataService nmdDataService);
}
