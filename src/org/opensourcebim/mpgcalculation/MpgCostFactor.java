package org.opensourcebim.mpgcalculation;

public class MpgCostFactor {
	private NmdLifeCycleStage stage;
	private NmdImpactFactor factor;
	private String materialName;
	private double value;

	public MpgCostFactor(NmdLifeCycleStage stage, NmdImpactFactor factor, String materialName, double value) {
		this.stage = stage;
		this.factor = factor;
		this.materialName = materialName;
		this.value = value;
	}

	public NmdLifeCycleStage getStage() {
		return stage;
	}

	public NmdImpactFactor getFactor() {
		return this.factor;
	}

	public String getMaterialName() {
		return this.materialName;
	}

	public double getValue() {
		return value;
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
				&& testFactor.getMaterialName() == this.getMaterialName();
	}

	@Override
	public int hashCode() {
		return (this.getFactor().toString() + this.getStage().toString() + this.getMaterialName()).hashCode();
	}

}
