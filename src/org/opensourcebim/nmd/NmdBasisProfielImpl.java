package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.opensourcebim.mpgcalculation.MpgCostFactor;
import org.opensourcebim.mpgcalculation.NmdImpactFactor;
import org.opensourcebim.mpgcalculation.NmdLifeCycleStage;

public class NmdBasisProfielImpl implements NmdFaseProfiel {

	private String description;
	private HashMap<NmdImpactFactor, Double> factors;
	private NmdLifeCycleStage stage;
	private NmdUnit unit;
	
	public NmdBasisProfielImpl(NmdLifeCycleStage stage, NmdUnit unit) {
		factors = new HashMap<NmdImpactFactor, Double>();
		this.setAll(0);
		this.stage = stage;
		this.unit = unit;
	}
	
	public void setAll(double value) {
		for (NmdImpactFactor factor : NmdImpactFactor.values()) {
			setImpactFactor(factor, value);
		}
	}
	

	@Override
	public String getDescription() {
		return this.description;
	}
	
	@Override
	public double getImpactFactor(NmdImpactFactor factor) {
		return factors.getOrDefault(factor, 0.0);
	}
	
	public void setImpactFactor(NmdImpactFactor factor, double value) {
		this.factors.put(factor, value);
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
	public Set<MpgCostFactor> calculateFactors(double cost, HashMap<NmdImpactFactor, Double> weightFactors) {
		Set<MpgCostFactor> results = new HashSet<MpgCostFactor>();
		for (Entry<NmdImpactFactor, Double> entry : factors.entrySet()) {
			results.add(new MpgCostFactor(this.stage, entry.getKey(), entry.getValue() * cost * weightFactors.get(entry.getKey())));
		}
		return results;
	}
}
