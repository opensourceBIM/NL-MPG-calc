package org.opensourcebim.nmd;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.opensourcebim.mapping.NlsfbCode;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NmdProductCardImpl implements NmdProductCard {
	
	private String description;
	private Set<NmdProfileSet> specifications;
	private Boolean isTotaalProduct;
	private String unit;
	private Integer category;
	private Boolean isScalable;
	private Integer productLifetime;
	private Integer parentId;
	private Integer id;
	private NlsfbCode nlsfbCode;

	public NmdProductCardImpl() {
		this.specifications = new HashSet<NmdProfileSet>();
		this.isTotaalProduct = false;
		this.description = "";
	}

	/**
	 * Copy constructor
	 * 
	 * @param input constructor
	 */
	public NmdProductCardImpl(NmdProductCard p) {

		if (p == null) {
			p = new NmdProductCardImpl();
		}

		this.id = p.getProductId();
		this.isScalable = p.getIsScalable();
		this.parentId = p.getParentProductId();
		this.specifications = new HashSet<NmdProfileSet>();
		this.specifications.addAll(p.getProfileSets());
		this.setLifetime(p.getLifetime());
		this.setCategory(p.getCategory());
		this.setNlsfbCode(p.getNlsfbCode());
		this.setUnit(p.getUnit());
		this.setDescription(p.getDescription());
		this.setIsTotaalProduct(p.getIsTotaalProduct());
	}

	@Override
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	@JsonIgnore
	@Override
	public NlsfbCode getNlsfbCode() {
		return this.nlsfbCode;
	}
	
	public void setNlsfbCode(NlsfbCode code) {
		this.nlsfbCode = code;
	}

	@Override
	public String getUnit() {
		return this.unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	@Override
	public Set<NmdProfileSet> getProfileSets() {
		return this.specifications;
	}

	@Override
	public void addProfileSet(NmdProfileSet spec) {
		if (spec != null) {
			this.specifications.add(spec);
		}
	}

	@Override
	public void addProfileSets(Collection<NmdProfileSet> specs) {
		if (specs != null && specs.size() > 0) {
			this.specifications.addAll(specs);
		}
	}

	@Override
	public Boolean getIsTotaalProduct() {
		return this.isTotaalProduct;
	}
	
	public void setIsTotaalProduct(boolean b) {
		this.isTotaalProduct = b;
	}
	
	@Override
	public Integer getCategory() {
		return this.category;
	}

	public void setCategory(Integer cat) {
		this.category = cat;
	}

	@Override
	public Boolean getIsScalable() {
		return this.isScalable;
	}

	public void setIsScalable(Boolean scalable) {
		this.isScalable = scalable;
	}

	@Override
	public Integer getLifetime() {
		return this.productLifetime;
	}

	public void setLifetime(Integer lifetime) {
		this.productLifetime = lifetime;
	}

	@Override
	public Integer getParentProductId() {
		return this.parentId;
	}

	public void setParentProductId(Integer id) {
		this.parentId = id;
	}

	@Override
	public Integer getProductId() {
		return this.id;
	}
	
	public void setProductId(Integer id) {
		this.id = id;
	}

	@Override
	public Double getProfileSetsCoeficientSum() {
		return this.getProfileSets().stream()
		.collect(Collectors.summingDouble(ps -> ps.getCoefficientSum()));
	}
}
