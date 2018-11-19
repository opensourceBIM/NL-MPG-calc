package org.opensourcebim.nmd;

import org.opensourcebim.ifccollection.MpgMaterial;

public interface NmdDataService {

	void start();

	void stop();

	MpgMaterial retrieveMaterial(MpgMaterial material);

}
