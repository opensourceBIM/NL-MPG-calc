package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.opensourcebim.mpgcalculation.MpgCostFactor;
import org.opensourcebim.mpgcalculation.NmdImpactFactor;
import org.opensourcebim.mpgcalculation.NmdLifeCycleStage;

public class NmdBasisProfielImpl implements NmdBasisProfiel {

	private HashMap<NmdImpactFactor, Double> factors;
	private NmdLifeCycleStage stage;
	
	public NmdBasisProfielImpl(NmdLifeCycleStage stage) {
		factors = new HashMap<NmdImpactFactor, Double>();
		this.setAll(0);
		this.stage = stage;
	}
	
	public void setAll(double value) {
		for (NmdImpactFactor factor : NmdImpactFactor.values()) {
			setImpactFactor(factor, value);
		}
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
	public Set<MpgCostFactor> calculateFactors(double distanceFromProducer, String material) {
		Set<MpgCostFactor> results = new HashSet<MpgCostFactor>();
		for (Entry<NmdImpactFactor, Double> entry : factors.entrySet()) {
			results.add(new MpgCostFactor(this.stage, entry.getKey(), material, entry.getValue()));
		}
		return results;
	}

}
