package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.jdt.core.compiler.InvalidInputException;

public class NmdProfileSetImpl implements NmdProfileSet {
	private String name;
	private Integer profielId;
	private String unit;
	private double massPerUnit;
	private int productLifeTime;

	private Boolean isMaintenanceSpec;
	
	/**
	 * NmdBasisProfiles available for this material specification.
	 * Can be different for specs within the same productcard
	 */
	private HashMap<String, NmdFaseProfiel> profiles;
	private Boolean isFullProfile;
	private Integer parentProfileId;

	public NmdProfileSetImpl() {		
		this.profiles = new HashMap<String, NmdFaseProfiel>();
	
		this.setIsMaintenanceSpec(false);
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Integer getProfielId() {
		return this.profielId;
	}

	public void setProfielId(Integer id) {
		this.profielId = id;
	}

	@Override
	public String getUnit() {
		return this.unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	@Override
	public double getMassPerUnit() {
		return this.massPerUnit;
	}

	public void setMassPerUnit(double mass) {
		this.massPerUnit = mass;
	}

	@Override
	public Integer getProductLifeTime() {
		return this.productLifeTime;
	}

	public void setProductLifeTime(Integer lifetime) throws InvalidInputException {
		if (lifetime <= 0) {
			throw new InvalidInputException("lifetime has to be larger than 0");
		}

		this.productLifeTime = lifetime;
	}

	@Override
	public NmdFaseProfiel getFaseProfiel(String fase) {
		return this.profiles.getOrDefault(fase, null);
	}
	
	public void addBasisProfiel(String fase, NmdFaseProfiel profile) {
		this.profiles.put(fase, profile);
	}

	@Override
	public Set<String> getDefinedProfiles() {
		return profiles.keySet();
	}

	@Override
	public Boolean getIsMaintenanceSpec() {
		return isMaintenanceSpec;
	}

	@Override
	public void setIsMaintenanceSpec(boolean flag) {
		this.isMaintenanceSpec = flag;
	}

	@Override
	public Boolean getIsFullProfile() {
		return this.isFullProfile;
	}
	
	public void setIsFullProfile(Boolean flag) {
		this.isFullProfile = flag;
	}

	@Override
	public Integer getParentProfielId() {
		return this.parentProfileId;
	}
	
	public void setParentProfielId(Integer id) {
		this.parentProfileId = id;
	}
}
