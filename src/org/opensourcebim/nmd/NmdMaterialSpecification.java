package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.List;

import org.opensourcebim.mpgcalculation.NmdLifeCycleStage;

/**
 * Material specification. contains Basis Profiel data for every lifecycle stage
 * relevant for the material
 * 
 * @author vijj
 *
 */
public interface NmdMaterialSpecification {
	public String getName();

	public String getCode();

	public String getUnit();

	public String getMasssPerConstructionUnit();

	public Double getProductLifeTime();

	public Double getConstructionLosses();

	public HashMap<NmdLifeCycleStage, Double> GetDisposalRatios();

	public NmdBasisProfiel getBasisProfiel(NmdLifeCycleStage lifeCycleStage);

	public List<NmdLifeCycleStage> getDefinedProfiles();
}
