package org.opensourcebim.mpgcalculation;

import org.opensourcebim.ifccollection.MpgMaterial;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.nmd.MaterialSpecification;
import org.opensourcebim.nmd.MaterialSpecifications;

/**
 * Do the MPG calculations based on a read in object model. with material data
 * gathered from NMD with user input and assumption combined
 * 
 * @author vijj
 *
 */
public class MpgCalculator {

	private MpgObjectStore objectStore = null;
	private MpgCalculationResults results;

	public MpgCalculator() {
		setResults(new MpgCalculationResults());
	}

	public MpgCalculationResults calculate(double designLife) {

		if (objectStore == null) {
			results.SetResultsStatus(ResultStatus.NoData);
			return results;
		}

		if (!(objectStore.isIfcDataComplete() && objectStore.isMaterialDataComplete())) {
			results.SetResultsStatus(ResultStatus.IncompleteData);
			return results;
		}

		try {
			// for each building material found:
			for (MpgMaterial mpgMaterial : objectStore.getMaterials().values()) {
				double totalVolume = objectStore.getTotalVolumeOfMaterial(mpgMaterial.getIfcName());
				MaterialSpecifications specs = mpgMaterial.getNmdMaterialSpecs();

				// calculate the # of replacements required
				double replacements = this.calculateReplacements(designLife, specs.getLifeTime());
				double specsDensity = specs.getDensity();

				// a single building material can be composed of individual materials.
				double specsMatSum = 0.0;
				for (MaterialSpecification matSpec : specs.getMaterials()) {
					// calculate total mass taking into account construction losses and replacements
					// during the lifetime. this is relevant for transport of the material
					double lifeTimeDesignVolume = replacements * totalVolume * matSpec.getMassPerUnit() / specsDensity;
					double lifeTimeDesignMass = lifeTimeDesignVolume * matSpec.getMassPerUnit();
					double lifeTimeTotalMass = lifeTimeDesignMass / (1 - matSpec.getConstructionLosses());
					specsMatSum += lifeTimeTotalMass;

					// per individual material define construction and disposal impact factors
				}

				// determine tranport costs per composed material
				results.addCostFactors(specs.getTransportProfile()
						.calculateFactors(specs.getDistanceFromProducer(), mpgMaterial.getIfcName()));
			}

			results.SetResultsStatus(ResultStatus.Success);

		} catch (Exception e) {
			results.SetResultsStatus(ResultStatus.ValueError);
		}
		return results;
	}

	public MpgObjectStore getObjectStore() {
		return objectStore;
	}

	public void setObjectStore(MpgObjectStore objectStore) {
		this.objectStore = objectStore;
	}

	/**
	 * Calculate the number of replacements required during the building design life
	 * source:
	 * https://www.milieudatabase.nl/imgcms/20141125_SBK_BepMeth_vs_2_0_inclusief_Wijzigingsblad_1_juni_2017_&_1_augustus_2017.pdf
	 * 
	 * @param designLife  total duration that building should be usable in years
	 * @param productLife design life of the material in years
	 * @return number of replacements. number is alsways larger or equal to 1
	 */
	private double calculateReplacements(double designLife, double productLife) {
		return Math.max(1.0, designLife / Math.max(1.0, productLife) - 1.0);
	}

	public MpgCalculationResults getResults() {
		return results;
	}

	public void setResults(MpgCalculationResults results) {
		this.results = results;
	}

}
