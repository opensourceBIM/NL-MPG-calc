package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.List;

/**
 * Basis Profiel data van Nmd een enkel materiaal kan meerdere
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

                                   