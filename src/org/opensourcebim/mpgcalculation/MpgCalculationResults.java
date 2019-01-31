package org.opensourcebim.mpgcalculation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
		this.reset();
	}

	public void reset() {
		costFactors.clear();
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

	public Double getTotalCost() {
		return costFactors.stream().collect(Collectors.summingDouble(f -> f.getValue()));
	}

	/**
	 * Total cost corrected for floor area and total lifetime
	 * 
	 * @return
	 */
	public double getTotalCorrectedCost() {
		return getTotalCost() / totalFloorArea / totalLifeTime;
	}

	public double getCostPerLifeCycle(String fase) {
		return costFactors.stream().filter(f -> f.getFase().equals(fase))
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}

	public double getCostPerImpactFactor(String factor) {
		return costFactors.stream().filter(f -> f.getMilieuCategorie().equals(factor))
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}

	public double getCostPerProductName(String name) {
		return costFactors.stream().filter(f -> name.equals(f.getProductName()))
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}

	public Double getCostPerSpecification(String specName) {
		return costFactors.stream().filter(f -> specName.equals(f.getProfielSetName()))
				.collect(Collectors.summingDouble(f -> f.getValue()));
	}

	public void incrementCostFactors(Set<MpgCostFactor> factors, String product, String specName) {
		for (MpgCostFactor costFactor : factors) {
			this.incrementCostFactor(costFactor, product, specName);
		}
	}

	/**
	 * Increment the value fo a costfactor if it is present in the current set.
	 * otherwise create a new factor
	 * 
	 * CostFactors should be unique per calculation according to it's equals (method)
	 * 
	 * @param mpgCostFactor the value to add
	 * @param product       product card name of the factor
	 * @param specName      material spec name of the factor
	 */
	public void incrementCostFactor(MpgCostFactor mpgCostFactor, String product, String specName) {

		mpgCostFactor.setProductName(product);
		mpgCostFactor.setProfielSetName(specName);

		Optional<MpgCostFactor> foundFactor = costFactors.stream().filter(f -> f.equals(mpgCostFactor)).findFirst();
		if (foundFactor.isPresent()) {
			foundFactor.get().setValue(foundFactor.get().getValue() + mpgCostFactor.getValue());
		} else {
			costFactors.add(mpgCostFactor);
		}
	}

	public Set<MpgCostFactor> getCostFactors() {
		return this.costFactors;
	}
}
