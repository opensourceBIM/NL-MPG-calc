package org.opensourcebim.mpgcalculation;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.openmbean.KeyAlreadyExistsException;

/**
 * Class to store all the calculation results broken down by a series of
 * properties as defined in the CostFactor class. This class will also
 * adminstrate the calcualtion process: i.e.: whether it succeeded or not.
 * 
 * @author vijj
 *
 */
public class MpgCalculationResults {

	private ResultStatus status;
	private Set<MpgCostFactor> costFactors;

	public MpgCalculationResults() {
		status = ResultStatus.NotRun;
		costFactors = new HashSet<MpgCostFactor>();
	}

	public void SetResultsStatus(ResultStatus status) {
		this.status = status;
	}

	public ResultStatus getStatus() {
		return this.status;
	}

	public double getTotalCost() {
		return costFactors.stream().collect(Collectors.summingDouble(f -> f.getValue()));
	}

	public double getCostPerLifeCycle(NmdLifeCycleStage stage) {
		return costFactors.stream().filter(f -> f.getStage() == stage)
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}

	public double getCostPerImpactFactor(NmdImpactFactor factor) {
		return costFactors.stream().filter(f -> f.getFactor() == factor)
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}

	public double getCosPerMaterialName(String name) {
		return costFactors.stream().filter(f -> f.getMaterialName().equals(name))
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}

	/**
	 * CostFactors should be unique per calculation. Therefore check for existence
	 * and throw an error when a to be added factor already exists.
	 * 
	 * @param newFactors Collection of CostFactors
	 */
	public void addCostFactors(Set<MpgCostFactor> newFactors) {
		for (MpgCostFactor costFactor : newFactors) {
			this.addCostFactor(costFactor);
		}
	}

	public void addCostFactor(MpgCostFactor mpgCostFactor) {
		if (costFactors.contains(mpgCostFactor)) {
			throw new KeyAlreadyExistsException("Cannot add a cost factor that already exists");
		}
		costFactors.add(mpgCostFactor);
	}

	public Set<MpgCostFactor> getCostFactors() {
		return this.costFactors;
	}
}
