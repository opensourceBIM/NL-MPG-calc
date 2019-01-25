package org.opensourcebim.nmd;

import java.util.HashSet;
import java.util.Set;

public class NmdProductCardImpl implements NmdProductCard {
	private String name;
	private String description;
	private int dataCategory;
	private String productCode;
	private String elementCode;
	private String elementName;
	private String unit;
	private double distanceToProducer;
	private double lifeTime;
	private NmdFaseProfiel transportProfile;
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
	public String getProductCode() {
		return this.productCode;
	}

	@Override
	public String getElementCode() {
		return this.elementCode;
	}

	@Override
	public String getElementName() {
		return this.elementName;
	}

	@Override
	public double getDistanceFromProducer() {
		return this.distanceToProducer;
	}

	@Override
	public double getLifeTime() {
		return this.lifeTime;
	}

	@Override
	public NmdFaseProfiel getTransportProfile() {
		return this.transportProfile;
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

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public void setElementCode(String elementCode) {
		this.elementCode = elementCode;
	}

	public void setElementName(String elementName) {
		this.elementName = elementName;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public double getDistanceToProducer() {
		return distanceToProducer;
	}

	public void setDistanceToProducer(double distanceToProducer) {
		this.distanceToProducer = distanceToProducer;
	}

	public void setLifeTime(double lifeTime) {
		this.lifeTime = lifeTime;
	}

	public void setTransportProfile(NmdFaseProfiel transportProfile) {
		this.transportProfile = transportProfile;
	}
}
