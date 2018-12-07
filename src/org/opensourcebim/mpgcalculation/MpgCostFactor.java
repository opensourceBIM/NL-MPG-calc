package org.opensourcebim.mpgcalculation;

public class MpgCostFactor {
	private NmdLifeCycleStage stage;
	private NmdImpactFactor factor;
	private String productName;
	private String specName;
	private double value;

	public MpgCostFactor(NmdLifeCycleStage stage, NmdImpactFactor factor, double value) {
		this.stage = stage;
		this.factor = factor;
		this.value = value;
	}

	public NmdLifeCycleStage getStage() {
		return stage;
	}

	public NmdImpactFactor getFactor() {
		return this.factor;
	}

	public String getProductName() {
		return this.productName;
	}
	
	public void setProductName(String product) {
		this.productName = product;
	}

	public double getValue() {
		return value;
	}
	
	public String getSpecName() {
		return specName;
	}

	public void setSpecName(String specName) {
		this.specName = specName;
	}

	/**
	 * override the equals method to avoid checking for value
	 */
	@Override
	public boolean equals(Object otherFactor) {
		if (!(otherFactor instanceof MpgCostFactor)) {
			return false;
		}
		MpgCostFactor testFactor = (MpgCostFactor) otherFactor;

		return testFactor.getFactor() == this.getFactor() && testFactor.getStage() == this.getStage()
				&& testFactor.getProductName() == this.getProductName();
	}

	@Override
	public int hashCode() {
		return (this.getFactor().toString() + this.getStage().toString() + this.getProductName()).hashCode();
	}
}
