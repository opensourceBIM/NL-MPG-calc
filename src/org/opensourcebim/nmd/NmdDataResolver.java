package org.opensourcebim.nmd;

import org.opensourcebim.ifccollection.MpgObjectStore;

public interface NmdDataResolver {

	MpgObjectStore NmdToMpg(MpgObjectStore ifcResults);

	void addService(NmdDataService nmdDataService);
}
