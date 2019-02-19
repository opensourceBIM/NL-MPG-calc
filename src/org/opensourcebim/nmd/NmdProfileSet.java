package org.opensourcebim.nmd;

import java.util.HashMap;

import org.opensourcebim.nmd.scaling.NmdScaler;

/**
 * Material specification. contains Basis Profiel data for every lifecycle stage
 * relevant for the material
 * 
 * @author vijj
 *
 */
public interface NmdProfileSet {
	String getName();

	Integer getProfielId();
	
	String getUnit();
	
	Double getQuantity();
	
	Integer getProfileLifeTime();
	
	NmdFaseProfiel getFaseProfiel(String fase);
	
	HashMap<String, NmdFaseProfiel> getAllFaseProfielen();

	void addFaseProfiel(String fase, NmdFaseProfiel faseProfiel);
		
	Boolean getIsScalable();

	NmdScaler getScaler();
}
