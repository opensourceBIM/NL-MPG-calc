package org.opensourcebim.nmd;

import java.util.HashSet;
import java.util.Set;

public class NmdProductCardImpl implements NmdProductCard {
	private String name;
	private String description;
	private int dataCategory;
	private String nlsfbCode;
	private String rawCode;
	private String elementName;
	private String unit;
	private double lifeTime;
	private Set<NmdProfileSet> specifications;
	
	public NmdProductCardImpl() {
		this.specifications = new HashSet<NmdProfileSet>();
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public int getDataCategory() {
		return this.dataCategory;
	}

	@Override
	public String getNLsfbCode() {
		return this.nlsfbCode;
	}

	@Override
	public String getRAWCode() {
		return this.rawCode;
	}
	
	@Override
	public String getUnit() {
		return this.unit;
	}

	@Override
	public String getElementName() {
		return this.elementName;
	}


	@Override
	public double getLifeTime() {
		return this.lifeTime;
	}


	@Override
	public Set<NmdProfileSet> getProfileSets() {
		return this.specifications;
	}
	
	@Override
	public void addProfileSet(NmdProfileSet spec) {
		this.specifications.add(spec);
	}

	@Override
	public String print() {
		return "not implemented";
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDataCategory(int dataCategory) {
		this.dataCategory = dataCategory;
	}

	public void setNLsfbCode(String productCode) {
		this.nlsfbCode = productCode;
	}

	public void setRAWCode(String elementCode) {
		this.rawCode = elementCode;
	}

	public void setElementName(String elementName) {
		this.elementName = elementName;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public void setLifeTime(double lifeTime) {
		this.lifeTime = lifeTime;
	}
}
