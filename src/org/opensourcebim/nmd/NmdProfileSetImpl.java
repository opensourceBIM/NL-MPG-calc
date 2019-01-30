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
	private double constructionLossFactor;
	private HashMap<String, Double> disposalDistances;
	private HashMap<String, Double> disposalRatios;
	private Boolean isMaintenanceSpec;
	
	/**
	 * NmdBasisProfiles available for this material specification.
	 * Can be different for specs within the same productcard
	 */
	private HashMap<String, NmdFaseProfiel> profiles;
	private Boolean isFullProfile;
	private Integer parentProfileId;

	public NmdProfileSetImpl() {
		this.disposalRatios = new HashMap<String, Double>();
		this.disposalRatios.put("Disposal", 0.0);
		this.disposalRatios.put("Incineration", 0.0);
		this.disposalRatios.put("Recycling", 0.0);
		this.disposalRatios.put("Reuse", 0.0);
		this.disposalRatios.put("OwnDisposalProfile", 0.0);
		
		this.disposalDistances = new HashMap<String, Double>();
		try {
			this.setDisposalDistance("Disposal", 100.0);
			this.setDisposalDistance("Incineration", 150.0);
			this.setDisposalDistance("Recycling", 15.0);
			this.setDisposalDistance("Reuse", 0.0);
			this.setDisposalDistance("OwnDisposalProfile", 100.0);
		} catch (InvalidInputException e) {
			e.printStackTrace();
		}
		
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
	public double getConstructionLosses() {
		return this.constructionLossFactor;
	}

	public void setConstructionLossFactor(double lossFactor) throws InvalidInputException {
		if (lossFactor < 0 || lossFactor >= 1) {
			throw new InvalidInputException("loss factor has to be in range [0, 1)");
		}

		this.constructionLossFactor = lossFactor;
	}

	@Override
	public HashMap<String, Double> getDisposalRatios() {
		return this.disposalRatios;
	}

	@Override
	public void setDisposalRatio(String fase, double value) throws InvalidInputException {
		if (!disposalRatios.containsKey(fase)) {
			throw new InvalidInputException("lifecycleStage has to be a disposal stage");
		}
		if (value < 0 || value > 1) {
			throw new InvalidInputException("disposal factor has to be in range [0 , 1]");
		}
		disposalRatios.put(fase, value);
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
	public double getDisposalDistance(String fase) {
		return disposalDistances.getOrDefault(fase, 0.0);
	}

	@Override
	public void setDisposalDistance(String fase, double disposalDistance) throws InvalidInputException {
		if (!disposalRatios.containsKey(fase)) {
			throw new InvalidInputException("lifecycleStage has to be a disposal stage");
		}
		if (disposalDistance < 0) {
			throw new InvalidInputException("disposal distance has to be larger than 0");
		}
		disposalDistances.put(fase, disposalDistance);
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
