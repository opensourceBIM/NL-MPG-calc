package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.stream.Collectors;

import org.opensourcebim.nmd.scaling.NmdScaler;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NmdProfileSetImpl implements NmdProfileSet {
	private String name;
	private int profielId;
	private String unit;

	private int profileLifeTime;
	
	/**
	 * NmdBasisProfiles available for this material specification.
	 * Can be different for specs within the same productcard
	 */
	private HashMap<String, NmdFaseProfiel> profiles;
	private boolean isScalable;
	private NmdScaler scaler;
	private double quantity;

	public NmdProfileSetImpl() {		
		this.profiles = new HashMap<String, NmdFaseProfiel>();
		this.quantity = 1.0;
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
	public Double getQuantity() {
		return this.quantity;
	}
	
	public void setQuantity(double q) {
		this.quantity = q;
	}

	@Override
	public Integer getProfileLifeTime() {
		return this.profileLifeTime;
	}

	public void setProfileLifetime(Integer lifetime) {
		this.profileLifeTime = lifetime;
	}
	
	@Override
	public NmdFaseProfiel getFaseProfiel(String fase) {
		return this.profiles.getOrDefault(fase, null);
	}
	
	public void addFaseProfiel(String fase, NmdFaseProfiel profile) {
		this.profiles.put(fase, profile);
	}

	@Override
	public HashMap<String, NmdFaseProfiel> getAllFaseProfielen() {
		return this.profiles;
	}
	
	@Override
	public Boolean getIsScalable() {
		return this.isScalable;
	}
	
	public void setIsScalable(Boolean scaleFlag) {
		this.isScalable = scaleFlag;
	}

	public void setScaler(NmdScaler scaler) {
		this.scaler = scaler;
	}

	@Override
	public NmdScaler getScaler() {
		return this.scaler;
	}

	@JsonIgnore
	@Override
	public Double getCoefficientSum() {
		return this.getAllFaseProfielen().values().stream()
			.collect(Collectors.summingDouble(fp -> fp.getCoefficientSum()));
	}
}
