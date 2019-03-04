package org.opensourcebim.mpgcalculation;

public class MpgCostFactor {
	private Long objectId;
	private String fase;
	private String milieuCategorie;
	private String productName; // name of productCard
	private String profielSetName; // name of meterialSpec
	private double value;

	public MpgCostFactor(String fase, String milieuCategorie, double value) {
		this.fase = fase;
		this.milieuCategorie = milieuCategorie;
		this.value = value;
	}

	public String getFase() {
		return fase;
	}

	public String getMilieuCategorie() {
		return this.milieuCategorie;
	}

	public String getProductName() {
		return this.productName;
	}
	
	public void setProductName(String product) {
		this.productName = product;
	}

	public Double getValue() {
		return value;
	}
	public void setValue(double val) {
		this.value = val;
	}
	
	public String getProfielSetName() {
		return profielSetName;
	}

	public void setProfielSetName(String specName) {
		this.profielSetName = specName;
	}
	

	public Long getObjectId() {
		return this.objectId;
	}
	
	public void setObjectId(Long id) {
		this.objectId = id;
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

		return testFactor.getObjectId() == this.getObjectId()
				&& testFactor.getMilieuCategorie() == this.getMilieuCategorie() 
				&& testFactor.getFase() == this.getFase()
				&& testFactor.getProductName() == this.getProductName() 
				&& testFactor.getProfielSetName() == this.getProfielSetName();
	}
}
