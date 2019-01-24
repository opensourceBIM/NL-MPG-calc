package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.opensourcebim.mpgcalculation.NmdLifeCycleStage;

/**
 * Material specification. contains Basis Profiel data for every lifecycle stage
 * relevant for the material
 * 
 * @author vijj
 *
 */
public interface MaterialSpecification {
	String getName();

	String getCode();

	String getUnit();
	
	/**
	 * Unlike density this is the mass per construction unit. (mass per wall area or mass per meter piping)
	 * @return the mass per volume, area or unit length of construction material
	 */
	double getMassPerUnit();

	double getProductLifeTime();

	/**
	 * factor between 0 and < 1 where 0 is no losses
	 * @return loss factor of material during construction
	 */
	double getConstructionLosses();

	HashMap<NmdLifeCycleStage, Double> getDisposalRatios();
	
	void setDisposalRatio(NmdLifeCycleStage stage, double ratio) throws InvalidInputException;

	double getDisposalDistance(NmdLifeCycleStage stage);
	
	NmdBasisProfiel getBasisProfiel(NmdLifeCycleStage lifeCycleStage);

	Set<NmdLifeCycleStage> getDefinedProfiles();

	void setDisposalDistance(NmdLifeCycleStage lifeCycleStage, double disposalDistance) throws InvalidInputException;
}
