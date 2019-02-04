package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.Set;

import org.opensourcebim.ifccollection.MpgObject;

public class NmdProfileSetImpl implements NmdProfileSet {
	private String name;
	private Integer profielId;
	private String unit;

	private int productLifeTime;
	private int category;
	
	/**
	 * NmdBasisProfiles available for this material specification.
	 * Can be different for specs within the same productcard
	 */
	private HashMap<String, NmdFaseProfiel> profiles;
	private Boolean isFullProfile;
	private Integer parentProfileId;
	private int cuasCode;

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
	public Integer getCategory() {
		return this.category;
	}

	public void setCategory(Integer category) {
		this.category = category;
	}

	@Override
	public Integer getProductLifeTime() {
		return this.productLifeTime;
	}

	public void setProductLifeTime(Integer lifetime) {
		this.productLifeTime = lifetime;
	}
	
	@Override
	public Integer getCuasCode() {
		return this.cuasCode;
	}
	
	public void setCuasCode(int cuasCode) {
		this.cuasCode = cuasCode;
	}

	@Override
	public NmdFaseProfiel getFaseProfiel(String fase) {
		return this.profiles.getOrDefault(fase, null);
	}
	
	public void addFaseProfiel(String fase, NmdFaseProfiel profile) {
		this.profiles.put(fase, profile);
	}

	@Override
	public Set<String> getDefinedProfiles() {
		return profiles.keySet();
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
	
	public Double getRequiredNumberOfUnits(MpgObject object) {
		if (object == null || this.profiles.size() == 0) {
			return Double.NaN;
		}
		
		String productUnit = this.getUnit();
		if (productUnit.equals("m1")) {
			return object.getVolume() / object.getArea();
		}
		if (productUnit.equals("m2")) {
			return object.getArea();
		}
		if (productUnit.equals("m3")) {
			return object.getVolume();
		}
		if (productUnit.equals("p")) {
			return 1.0; // product per piece. always return 1 per profielset.
		}
		if (productUnit.equals("kg")) {
			return Double.NaN; // we do not have densities of products, we will need to figure out how to fix this.
		}
		
		return Double.NaN;
		
	}
}
