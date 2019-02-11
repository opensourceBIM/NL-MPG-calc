package org.opensourcebim.nmd;

import java.util.HashMap;

import org.opensourcebim.ifccollection.MpgGeometry;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.nmd.scaling.NmdScaler;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NmdProfileSetImpl implements NmdProfileSet {
	private String name;
	private Integer profielId;
	private String unit;

	private int profileLifeTime;
	
	/**
	 * NmdBasisProfiles available for this material specification.
	 * Can be different for specs within the same productcard
	 */
	private HashMap<String, NmdFaseProfiel> profiles;
	private Boolean isScalable;
	private NmdScaler scaler;

	public NmdProfileSetImpl() {		
		this.profiles = new HashMap<String, NmdFaseProfiel>();
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
	
	public Double getRequiredNumberOfUnits(MpgObject object) {
		if (object == null || this.profiles.size() == 0) {
			return Double.NaN;
		}
		MpgGeometry geom = object.getGeometry();
		
		
		String productUnit = this.getUnit();
		if (productUnit.equals("m1")) {
			return geom.getPrincipalDimension();
		}
		if (productUnit.equals("m2")) {
			return geom.getLargestFaceArea();
		}
		if (productUnit.equals("m3")) {
			return geom.getVolume();
		}
		if (productUnit.equals("p")) {
			return 1.0; // product per piece. always return 1 per profielset.
		}
		if (productUnit.equals("kg")) {
			return Double.NaN; // we do not have densities of products, we will need to figure out how to fix this.
		}
		
		return Double.NaN;
		
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

	@Override
	@JsonIgnore
	public int getUnitDimension() {
		String productUnit = this.getUnit();
		
		switch (productUnit.toLowerCase()) {
		case "mm":
		case "cm":
		case "m1":
		case "m":
		case "meter":
			return 1;
		case "mm2":
		case "mm^2":
		case "square_millimeter":
		case "cm2":
		case "cm^2":
		case "m2":
		case "m^2":
		case "square_meter":
			return 2;
		case "mm3":
		case "mm^3":
		case "cubic_millimeter":
		case "cm3":
		case "cm^3":
		case "m3":
		case "m^3":
		case "cubic_meter":
			return 3;
		default:
			return -1;
		}
	}
}
