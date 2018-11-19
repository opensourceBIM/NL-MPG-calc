package org.opensourcebim.nmdcalculation;

import org.opensourcebim.ifccollection.MpgMaterial;

public enum DesignLife{
	
	Residential(75.0),
	Commercial(50.0),
	GWW(100.0),
	Custom(-1.0);
	
	private Double designLife;
	
	private DesignLife(Double years) {
		this.designLife = years;
	}
	
	public Double getDesignLife() {
		return this.designLife;
	}
	public void setDesignLife(Double years) {
		this.designLife = years;
	}
	
	public Double calculateReplacements(Double materialLifeCycle) {
		return Math.max(1, this.getDesignLife() / materialLifeCycle - 1 );
	}
}
