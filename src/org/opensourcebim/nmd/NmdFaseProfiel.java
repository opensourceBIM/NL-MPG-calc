package org.opensourcebim.nmd;

import java.util.Set;

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
	
	String getFase();
	
	Set<NmdCostFactor> calculateFactors(double cost);

	double getProfielCoefficient(String milieuCategorie);
	
	void setProfielCoefficient(String milieuCategorie, double value);
	
	Double getCoefficientSum();
}
