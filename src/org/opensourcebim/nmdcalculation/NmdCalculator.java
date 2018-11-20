package org.opensourcebim.nmdcalculation;

import org.opensourcebim.ifccollection.MpgMaterial;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.nmd.NmdMaterialSpecifications;

public class NmdCalculator {

	private MpgObjectStore objectStore = null;

	public NmdCalculator() {
	}

	public NmdCalculationResults calculate(Double designLife) {
		// for each NmmMaterialSpec:
		for (MpgMaterial mpgMaterial : objectStore.getMaterials().values()) {
			NmdMaterialSpecifications specs = mpgMaterial.getNmdMaterialSpecs();
			// calculate the # of replacements required

			// calculate total mass
			
			// adjust the required material for production losses

			// determine tranport costs

			// determine construction and replacement cost

			// determine disposal ratios and determine recycle and disposal cost.
		}
		return new NmdCalculationResults();
	}

	public MpgObjectStore getObjectStore() {
		return objectStore;
	}

	public void setObjectStore(MpgObjectStore objectStore) {
		this.objectStore = objectStore;
	}

	/**
	 * Calculate the number of replacements required during the building design life
	 * source: https://www.milieudatabase.nl/imgcms/20141125_SBK_BepMeth_vs_2_0_inclusief_Wijzigingsblad_1_juni_2017_&_1_augustus_2017.pdf
	 * @param designLife total duration that building should be usable in years
	 * @param productLife design life of the material in years
	 * @return number of replacements. number is alsways larger or equal to 1
	 */
	private double calculateReplacements(double designLife, double productLife) {
		return Math.max(1.0, designLife / Math.max(1.0, productLife) - 1.0);
	}

}
