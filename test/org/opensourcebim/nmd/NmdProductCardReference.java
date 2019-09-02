package org.opensourcebim.nmd;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import nl.tno.bim.nmd.domain.NlsfbCode;
import nl.tno.bim.nmd.domain.NmdProductCard;
import nl.tno.bim.nmd.domain.NmdProfileSet;

/**
 * Product card implementation for validation purposes
 * @author vijj
 */
public class NmdProductCardReference implements NmdProductCard {

	private Double coefficientSum;
	private Integer productId;

	public NmdProductCardReference(Double sum) {
		this.productId = -42;
		this.coefficientSum = sum;
	}
	
	@Override
	public String getDescription() {
		return "reference card";
	}

	@Override
	public Boolean getIsTotaalProduct() { return false;}

	@Override
	public Set<NmdProfileSet> getProfileSets() { return new HashSet<>();}

	@Override
	public void addProfileSet(NmdProfileSet spec) { }

	@Override
	public void addProfileSets(Collection<NmdProfileSet> specs) { }

	@Override
	public NlsfbCode getNlsfbCode() {
		return null;
	}

	@Override
	public String getUnit() {
		return null;
	}

	@Override
	public Integer getCategory() {
		return null;
	}

	@Override
	public Integer getLifetime() {
		return 1000;
	}

	@Override
	public Integer getParentProductId() {
		return 0;
	}

	@Override
	public Integer getProductId() {
		return productId;
	}
	
	@Override
	public Double getProfileSetsCoeficientSum() {
		return coefficientSum;
	}
}
