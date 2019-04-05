package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

public class NmdFaseProfielImpl implements NmdFaseProfiel {

	private HashMap<String, Double> profielCoefficienten;
	private String fase;
	private NmdReferenceResources refData;

	public NmdFaseProfielImpl(String fase, NmdReferenceResources referenceData) {
		profielCoefficienten = new HashMap<String, Double>();
		this.refData = referenceData;
		this.setAll(0);
		this.fase = fase;

	}

	public void setAll(double value) {
		for (Entry<Integer, NmdMilieuCategorie> factor : this.refData.getMilieuCategorieMapping().entrySet()) {
			setProfielCoefficient(factor.getValue().getDescription(), value);
		}
	}

	@Override
	public double getProfielCoefficient(String milieuCategorie) {
		return profielCoefficienten.getOrDefault(milieuCategorie, 0.0);
	}

	@Override
	public void setProfielCoefficient(String milieuCategorie, double value) {
		this.profielCoefficienten.put(milieuCategorie, value);
	}

	@Override
	public String getFase() {
		return this.fase;
	}

	@Override
	public Set<NmdCostFactor> calculateFactors(double cost) {
		Set<NmdCostFactor> results = new HashSet<NmdCostFactor>();
		for (Entry<Integer, NmdMilieuCategorie> entry : this.refData.getMilieuCategorieMapping().entrySet()) {
			String description = entry.getValue().getDescription();
			Double profielValue = profielCoefficienten.getOrDefault(description, Double.NaN);
			if (!profielValue.isNaN()) {
				results.add(
						new NmdCostFactor(this.fase, description, cost * profielValue * entry.getValue().getWeight()));
			}
		}
		return results;
	}

	@Override
	public Double getCoefficientSum() {
		return this.profielCoefficienten.values().stream()
				.filter(v -> !v.isNaN())
				.mapToDouble(v -> v.doubleValue())
				.sum();
	}
}
