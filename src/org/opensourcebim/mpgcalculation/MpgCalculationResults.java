package org.opensourcebim.mpgcalculation;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

public class MpgCalculationResults {

	private ResultStatus status;
	
	private HashMap<Pair<NmdLifeCycleStage, NmdImpactFactor>, Double> costFactors;
	
	public MpgCalculationResults() {
		status = ResultStatus.NotRun;
		costFactors = new HashMap<Pair<NmdLifeCycleStage,NmdImpactFactor>, Double>();
	}
	
	public void SetResultsStatus(ResultStatus status) {
		this.status = status;
	}

	public ResultStatus getStatus() {
		return this.status;
	}

	public double getCostPerLifeCycle(NmdLifeCycleStage stage) {
		double sum = 0.0;
		for (Entry<Pair<NmdLifeCycleStage, NmdImpactFactor>, Double> entry : costFactors.entrySet()) {
			Pair<NmdLifeCycleStage, NmdImpactFactor> key = entry.getKey();
			Double value = entry.getValue();
			
			sum += (key.getLeft() == stage) ? value : 0.0;
		}
		return sum;
	}
	
	public double getCostPerImpactFactor(NmdImpactFactor factor) {
		double sum = 0.0;
		for (Entry<Pair<NmdLifeCycleStage, NmdImpactFactor>, Double> entry : costFactors.entrySet()) {
			Pair<NmdLifeCycleStage, NmdImpactFactor> key = entry.getKey();
			Double value = entry.getValue();
			
			sum += (key.getRight() == factor) ? value : 0.0;
		}
		return sum;
	}
}
