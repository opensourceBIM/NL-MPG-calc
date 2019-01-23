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
	private double totalFloorArea;
	private double totalLifeTime;

	public MpgCalculationResults() {
		status = ResultStatus.NotRun;
		costFactors = new HashSet<MpgCostFactor>();
		totalFloorArea = 1.0;
		totalLifeTime = 1.0;
	}
	
	public void setTotalLifeTime(double designLife) {
		this.totalLifeTime = designLife;
	}

	public void setTotalFloorArea(double totalFloorArea) {
		this.totalFloorArea = totalFloorArea;
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
	
	/**
	 * Total cost corrected for floor area and total lifetime
	 * @return
	 */
	public double getTotalCorrectedCost() {
		return getTotalCost() / totalFloorArea / totalLifeTime;
	}

	public double getCostPerLifeCycle(NmdLifeCycleStage stage) {
		return costFactors.stream().filter(f -> f.getStage() == stage)
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}

	public double getCostPerImpactFactor(NmdImpactFactor factor) {
		return costFactors.stream().filter(f -> f.getFactor() == factor)
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}

	public double getCostPerProductName(String name) {
		return costFactors.stream().filter(f -> name.equals(f.getProductName()))
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}
	
	public Double getCostPerSpecification(String specName) {
		return costFactors.stream().filter(f -> specName.equals(f.getSpecName()))
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}

	/**
	 * CostFactors should be unique per calculation. Therefore check for existence
	 * and throw an error when a to be added factor already exists.
	 * 
	 * @param newFactors Collection of CostFactors
	 */
	public void addCostFactors(Set<MpgCostFactor> newFactors, String product) {
		for (MpgCostFactor costFactor : newFactors) {
			this.addCostFactor(costFactor, product, "");
		}
	}
	
	public void addCostFactors(Set<MpgCostFactor> newFactors, String product, String specName) {
		for (MpgCostFactor costFactor : newFactors) {
			this.addCostFactor(costFactor, product, specName);
		}
	}

	public void addCostFactor(MpgCostFactor mpgCostFactor, String product, String specName) {
		
		mpgCostFactor.setProductName(product);
		mpgCostFactor.setSpecName(specName);
		
		if (costFactors.stream().anyMatch(f -> f.equals(mpgCostFactor))) {
			throw new KeyAlreadyExistsException("Cannot add a cost factor that already exists");
		}

		costFactors.add(mpgCostFactor);
	}

	public Set<MpgCostFactor> getCostFactors() {
		return this.costFactors;
	}
}
