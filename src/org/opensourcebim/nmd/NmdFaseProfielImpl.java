package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import org.opensourcebim.mpgcalculation.MpgCostFactor;
import org.opensourcebim.mpgcalculation.NmdMileuCategorie;
import org.opensourcebim.mpgcalculation.NmdMileuCategorie;
import org.opensourcebim.mpgcalculation.NmdLifeCycleStage;

public class NmdFaseProfielImpl implements NmdFaseProfiel {

	private String description;
	private HashMap<String, Double> profielCoefficienten;
	private NmdLifeCycleStage stage;
	private NmdUnit unit;
	private NmdReferenceResources refData;

	public NmdFaseProfielImpl(NmdLifeCycleStage stage, NmdUnit unit, NmdReferenceResources referenceData) {
		profielCoefficienten = new HashMap<String, Double>();
		this.setAll(0);
		this.stage = stage;
		this.unit = unit;
		this.refData = referenceData;
	}

	public void setAll(double value) {
		for (Entry<Integer, NmdMileuCategorie> factor : this.refData.getMilieuCategorieMapping().entrySet()) {
			setProfielCoefficient(factor.getValue().getDescription(), value);
		}
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public double getProfielCoefficient(String milieuCategorie) {
		return profielCoefficienten.getOrDefault(milieuCategorie, 0.0);
	}

	public void setProfielCoefficient(String milieuCategorie, double value) {
		this.profielCoefficienten.put(milieuCategorie, value);
	}

	@Override
	public NmdLifeCycleStage getStage() {
		return this.stage;
	}

	@Override
	public NmdUnit getUnit() {
		return this.unit;
	}

	@Override
	public Set<MpgCostFactor> calculateFactors(double cost) {
		Set<MpgCostFactor> results = new HashSet<MpgCostFactor>();
		for (Entry<Integer, NmdMileuCategorie> entry : this.refData.getMilieuCategorieMapping().entrySet()) {
			String description = entry.getValue().getDescription();
			Double profielValue = profielCoefficienten.getOrDefault(description, Double.NaN);
			if (!profielValue.isNaN()) {
				results.add(
						new MpgCostFactor(this.stage, description, cost * profielValue * entry.getValue().getWeight()));
			}
		}
		return results;
	}
}
