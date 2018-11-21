package org.opensourcebim.nmd;

import java.util.HashMap;

import org.opensourcebim.mpgcalculation.NmdImpactFactor;

public class NmdBasisProfielImpl implements NmdBasisProfiel {

	private HashMap<NmdImpactFactor, Double> factors;
	
	public NmdBasisProfielImpl() {
		factors = new HashMap<NmdImpactFactor, Double>();
		this.setAll(0);
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

}
