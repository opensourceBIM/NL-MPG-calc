package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.opensourcebim.mpgcalculation.NmdLifeCycleStage;

public class MaterialSpecificationImpl implements MaterialSpecification {
	private String name;
	private String code;
	private String unit;
	private double massPerUnit;
	private double productLifeTime;
	private double constructionLossFactor;
	private HashMap<NmdLifeCycleStage, Double> disposalRatios;
	private HashMap<NmdLifeCycleStage, NmdBasisProfiel> profiles;

	public MaterialSpecificationImpl() {
		this.disposalRatios = new HashMap<NmdLifeCycleStage, Double>();
		this.disposalRatios.put(NmdLifeCycleStage.Disposal, 0.0);
		this.disposalRatios.put(NmdLifeCycleStage.Incineration, 0.0);
		this.disposalRatios.put(NmdLifeCycleStage.Recycling, 0.0);
		
		this.profiles = new HashMap<NmdLifeCycleStage, NmdBasisProfiel>();
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getCode() {
		return this.code;
	}

	public void setCode(String code) {
		this.code = code;
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
	public double getProductLifeTime() {
		return this.productLifeTime;
	}

	public void setProductLifeTime(double lifetime) throws InvalidInputException {
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
	public HashMap<NmdLifeCycleStage, Double> GetDisposalRatios() {
		return this.disposalRatios;
	}

	public void setDisposalRatio(NmdLifeCycleStage stage, double value) throws InvalidInputException {
		if (!disposalRatios.containsKey(stage)) {
			throw new InvalidInputException("lifecycleStage has to be a disposal stage");
		}
		if (value < 0 || value > 1) {
			throw new InvalidInputException("disposal factor has to be in range [0 , 1]");
		}
		disposalRatios.put(stage, value);
	}

	@Override
	public NmdBasisProfiel getBasisProfiel(NmdLifeCycleStage lifeCycleStage) {
		return this.profiles.getOrDefault(lifeCycleStage, null);
	}
	
	public void addBasisProfiel(NmdLifeCycleStage stage, NmdBasisProfiel profile) throws InvalidInputException {
		if (stage == NmdLifeCycleStage.Transport) {
			throw new InvalidInputException("cannot add transport profile to a single material");
		}
		this.profiles.put(stage, profile);
	}

	@Override
	public Set<NmdLifeCycleStage> getDefinedProfiles() {
		return profiles.keySet();
	}



}
