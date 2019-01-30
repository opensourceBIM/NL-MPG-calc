package org.opensourcebim.nmd;

import java.util.Set;

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

	/*
	 * Flag to indicate whether this profile covers for all of its children profiles
	 */
	Boolean getIsFullProfile();
	
	/*
	 * reference to ProfielId of parent profiel
	 */
	Integer getParentProfielId();
	
	String getUnit();
	
	/**
	 * Unlike density this is the mass per construction unit. (mass per wall area or mass per meter piping)
	 * @return the mass per volume, area or unit length of construction material
	 */
	double getMassPerUnit();

	Integer getProductLifeTime();
	
	NmdFaseProfiel getFaseProfiel(String fase);

	Set<String> getDefinedProfiles();

	Boolean getIsMaintenanceSpec();

	void setIsMaintenanceSpec(boolean b);
}
