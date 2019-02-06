package org.opensourcebim.nmd;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NmdProductCardImpl implements NmdProductCard {
	private String name;
	private String description;
	private int dataCategory;
	private String nlsfbCode;
	private String rawCode;
	private String elementName;
	private double lifeTime;
	private Set<NmdProfileSet> specifications;
	private Boolean isTotaalProduct;
	
	public NmdProductCardImpl() {
		this.specifications = new HashSet<NmdProfileSet>();
		this.setRAWCode("");
		this.setNLsfbCode("");
		this.isTotaalProduct = false;
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
	
	@Override 
	public Boolean getIsTotaalProduct() {
		return this.isTotaalProduct;
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

	public void setLifeTime(double lifeTime) {
		this.lifeTime = lifeTime;
	}

	public void setIsTotaalProduct(boolean b) {
		this.isTotaalProduct = b;
	}

	
	/**
	 * Make sure that all cuas phases are covered
	 * TODO: what if there are multiple sets for a single cuas phase?
	 * @return boolean to indicate
	 */
	@Override
	public boolean isFullyCovered() {	
		List<Integer> cuasCodes = this.getProfileSets().stream().map(ps -> ps.getCuasCode()).collect(Collectors.toList());
		return this.getIsTotaalProduct() || 
				(cuasCodes.size() == 4 && cuasCodes.stream().mapToInt(Integer::intValue).sum() == 10);
	}
}
