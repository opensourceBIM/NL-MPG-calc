package org.opensourcebim.mpgcalculation;

import java.util.HashMap;
import java.util.Map.Entry;

import org.opensourcebim.ifccollection.MpgMaterial;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.nmd.MaterialSpecification;
import org.opensourcebim.nmd.NmdProductCard;

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
	private HashMap<NmdImpactFactor, Double> costWeightFactors = new HashMap<NmdImpactFactor, Double>();
	
	public MpgCalculator() {
		setResults(new MpgCalculationResults());
		for (NmdImpactFactor nmdImpactFactor : NmdImpactFactor.values()) {
			costWeightFactors.put(nmdImpactFactor, 1.0);
		}
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
				NmdProductCard specs = mpgMaterial.getNmdMaterialSpecs();
				
				// category 3 data requires a 30% penalty
				double categoryMultiplier = specs.getDataCategory() == 3 ? 1.3 : 1.0;

				// calculate the # of replacements required
				double replacements = this.calculateReplacements(designLife, specs.getLifeTime());
				double specsDensity = specs.getDensity();

				// a single building material can be composed of individual materials.
				double specsMatSumKg = 0.0;
				for (MaterialSpecification matSpec : specs.getMaterials()) {
					// calculate total mass taking into account construction losses and replacements
					double lifeTimeDesignVolume = replacements * totalVolume * matSpec.getMassPerUnit() / specsDensity;
					double lifeTimeDesignMassKg = lifeTimeDesignVolume * matSpec.getMassPerUnit();
					double lifeTimeTotalMassKg = lifeTimeDesignMassKg * (1 + matSpec.getConstructionLosses());

					// per individual material define construction factors
					results.addCostFactors(matSpec.getBasisProfiel(NmdLifeCycleStage.ConstructionAndReplacements)
							.calculateFactors(lifeTimeTotalMassKg * categoryMultiplier, costWeightFactors, matSpec.getName()));

					// disposal impact factors have to be adjusted for disposal ratios
					for (Entry<NmdLifeCycleStage, Double> entry : matSpec.GetDisposalRatios().entrySet()) {
						double cost = lifeTimeTotalMassKg * entry.getValue() * categoryMultiplier;
						
						results.addCostFactors(matSpec.getBasisProfiel(entry.getKey()).calculateFactors(
								cost, costWeightFactors, matSpec.getName()));
					}

					// TODO: calculate cyclic maintenance stage - apply different replacement factor

					// TODO: calculate energy and water use impact factors - apply different units
					// of measure

					// add the individual material to the composite material mass for transport
					// calculations
					specsMatSumKg += lifeTimeTotalMassKg;
				}

				// TODO: add correction factor for trasnport packign (standard 1)?
				// TODO: determine disposal transport phase - determine composite recycling
				// factor based on mayterial disposal factors.

				// determine tranport costs per composed material in tonnes * km.
				results.addCostFactors(specs.getTransportProfile().calculateFactors(
						2 * specs.getDistanceFromProducer() * (specsMatSumKg / 1000.0), costWeightFactors, mpgMaterial.getIfcName()));
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
