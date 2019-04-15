package org.opensourcebim.mpgcalculation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import nl.tno.bim.nmd.domain.NmdCostFactor;

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
	private Set<NmdCostFactor> costFactors;
	private double totalFloorArea;
	private double totalLifeTime;

	public MpgCalculationResults() {
		status = ResultStatus.NotRun;
		costFactors = new HashSet<NmdCostFactor>();
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
	public void addCostFactor(NmdCostFactor mpgCostFactor, String product, String specName, Long objectId) {

		mpgCostFactor.setProductName(product);
		mpgCostFactor.setProfielSetName(specName);
		mpgCostFactor.setObjectId(objectId);
		if (!mpgCostFactor.getValue().isNaN()) {
			costFactors.add(mpgCostFactor);
		}
	}

	@JsonIgnore
	public Set<NmdCostFactor> getCostFactors() {
		return this.costFactors;
	}
	
	public HashMap<Long, Double> getCostPerObjectId() {
		HashMap<Long, Double> grouping = new HashMap<Long, Double>();
		this.costFactors.stream()
			.collect(Collectors.groupingBy(NmdCostFactor::getObjectId))
			.forEach((key, g) -> grouping.put(key, g.stream().collect(Collectors.summingDouble(cf -> cf.getValue()))));
		return grouping;
	}
	
	public HashMap<String, Double> getCostPerMilieuCategorie() {
		return this.getCostPerCategory(NmdCostFactor::getMilieuCategorie);
	}
	
	public HashMap<String, Double> getCostPerFase() {
		return this.getCostPerCategory(NmdCostFactor::getFase);
	}
	
	public HashMap<String, Double> getCostPerProduct() {
		return this.getCostPerCategory(NmdCostFactor::getProductName);
	}
	

	public HashMap<String, Double> getCostPerProfiel() {
		return this.getCostPerCategory(NmdCostFactor::getProfielSetName);
	}
	

	private HashMap<String, Double> getCostPerCategory(Function<? super NmdCostFactor, ? extends String> groupingFunc) {
		HashMap<String, Double> grouping = new HashMap<String, Double>();
		this.costFactors.stream()
			.collect(Collectors.groupingBy(groupingFunc))
			.forEach((key, g) -> grouping.put(key, g.stream().collect(Collectors.summingDouble(cf -> cf.getValue()))));
		return grouping;
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

	public void addCostFactors(Set<NmdCostFactor> factors, String product, String specName, Long objectId) {
		for (NmdCostFactor costFactor : factors) {
			this.addCostFactor(costFactor, product, specName, objectId);
		}
	}
}
