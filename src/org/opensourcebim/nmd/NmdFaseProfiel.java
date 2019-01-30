package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.Set;

import org.opensourcebim.mpgcalculation.MpgCostFactor;
import org.opensourcebim.mpgcalculation.NmdMileuCategorie;

/**
 * Interface class to provide the impact factor coefficients for a single
 * Lifecycle stage. For instance: every NmdMaterialSpecification will have
 * several implementations of the NmdBasisProfiel class added. one for
 * construction, one or more for disposal, etc.
 * 
 * @author vijj
 *
 */
public interface NmdFaseProfiel {

	String getDescription();
	
	String getFase();

	NmdUnit getUnit();
	
	Set<MpgCostFactor> calculateFactors(double cost);

	double getProfielCoefficient(String milieuCategorie);
}
