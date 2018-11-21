package org.opensourcebim.nmd;

import org.opensourcebim.mpgcalculation.NmdImpactFactor;

/**
 * Interface class to provide the impact factor coefficients for a single
 * Lifecycle stage. For instance: every NmdMaterialSpecification will have
 * several implementations of the NmdBasisProfiel class added. one for
 * construction, one or more for disposal, etc.
 * 
 * @author vijj
 *
 */
public interface NmdBasisProfiel {
	double getImpactFactor(NmdImpactFactor factor);
}
