package org.opensourcebim.nmdcalculation;

import org.opensourcebim.ifccollection.MpgMaterial;
import org.opensourcebim.ifccollection.MpgObjectStore;

public class NmdCalculator {

	private MpgObjectStore objectStore = null;
	private DesignLife referenceDesignLife;
	
	public NmdCalculator() {
		referenceDesignLife = DesignLife.Residential;
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

	public void setReferenceDesignLife() {
		for (MpgMaterial mpgMaterial : objectStore.getMaterials().values()) {
			mpgMaterial.setProperty("replacements", referenceDesignLife.calculateReplacements(mpgMaterial.getProperty("lifecycle", Double.class)));
		}
	}
	
}
