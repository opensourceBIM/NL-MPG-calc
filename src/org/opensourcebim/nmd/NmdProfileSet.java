package org.opensourcebim.nmd;

import java.util.Set;

import org.opensourcebim.ifccollection.MpgObject;

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
	
	Integer getProductLifeTime();
	
	Integer getCuasCode();
	
	NmdFaseProfiel getFaseProfiel(String fase);

	Set<String> getDefinedProfiles();

	Integer getCategory();
	
	Double getRequiredNumberOfUnits(MpgObject mpgObject);

}
