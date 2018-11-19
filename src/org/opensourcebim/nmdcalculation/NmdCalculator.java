package org.opensourcebim.nmdcalculation;

import org.opensourcebim.ifccollection.MpgMaterial;
import org.opensourcebim.ifccollection.MpgObjectStore;

public class NmdCalculator {

	private MpgObjectStore objectStore = null;
	private Double referenceDesignLife;
	
	public NmdCalculator() {
		// load relevant modules.
	}
	
	public NmdCalculationResults calculate() {
		return new NmdCalculationResults();
	}

	public MpgObjectStore getObjectStore() {
		return objectStore;
	}

	public void setObjectStore(MpgObjectStore objectStore) {
		this.objectStore = objectStore;
	}

	/**
	 * Reference design life is by default 75 years for residential purposes and 50 years for commercial purposes.
	 * a building that has mixed purposes can be set to 75 years. GWW projects can be set to 100 years.
	 * @return
	 */
	public Double getReferenceDesignLife() {
		return referenceDesignLife;
	}

	public void setReferenceDesignLife(Double referenceDesignLife) {
		this.referenceDesignLife = referenceDesignLife;
		for (MpgMaterial mpgMaterial : objectStore.getMaterials().values()) {
			mpgMaterial.setProperty("replacements",
					Math.max(1, referenceDesignLife / mpgMaterial.getProperty("lifecycleduration", Double.class) - 1 ));
		}
		
	}
	
}
