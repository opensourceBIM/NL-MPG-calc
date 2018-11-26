package org.opensourcebim.nmd;

import org.opensourcebim.ifccollection.MpgMaterial;

/**
 * Stadard interface to provide material data from the source to the user.
 * 
 * @author vijj
 *
 */
public interface NmdDataService {

	void start();

	void stop();

	MpgMaterial retrieveMaterial(MpgMaterial material);

}
