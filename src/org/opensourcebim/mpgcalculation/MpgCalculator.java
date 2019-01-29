package org.opensourcebim.mpgcalculation;

import java.util.HashMap;
import java.util.Map.Entry;

import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.nmd.NmdProductCard;
import org.opensourcebim.nmd.NmdProfileSet;

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

		if (!(objectStore.isIfcDataComplete() && objectStore.isElementDataComplete())) {
			results.SetResultsStatus(ResultStatus.IncompleteData);
			return results;
		}

		try {
			results.setTotalLifeTime(designLife);
			results.setTotalFloorArea(objectStore.getTotalFloorArea());

			// for each building material found:
			for (MpgElement mpgMaterial : objectStore.getElements()) {

				// this total volume can be in m3 or possible also in kWh depending on the unit
				// in the product card
				double totalVolume = objectStore.getTotalVolumeOfMaterial(mpgMaterial.getIfcName());
				NmdProductCard specs = mpgMaterial.getNmdProductCard();

				// category 3 data requires a 30% penalty
				double categoryMultiplier = specs.getDataCategory() == 3 ? 1.3 : 1.0;

				// a single building material can be composed of individual materials.
				double specsMatSumKg = 0.0;

				for (NmdProfileSet matSpec : specs.getProfileSets()) {

					// Determine replacements required.
					// this is usually 1 for regular materials and > 1 for cyclic maintenance
					// materials
					double replacements = this.calculateReplacements(designLife, matSpec.getProductLifeTime(),
							matSpec.getIsMaintenanceSpec());

					// calculate total mass taking into account construction losses and replacements
					// TODO: how does the current approach tackle issues with plate materials etc.?
					double lifeTimeVolumePerSpec = replacements * totalVolume;
					double lifeTimeMassPerSpec = lifeTimeVolumePerSpec * matSpec.getMassPerUnit();
					double lifeTimeTotalMassKg = lifeTimeMassPerSpec * (1 + matSpec.getConstructionLosses());

					// ----- Production ----
					results.incrementCostFactors(
							matSpec.getFaseProfiel(NmdLifeCycleStage.ConstructionAndReplacements)
									.calculateFactors(lifeTimeTotalMassKg * categoryMultiplier, costWeightFactors),
							specs.getName(), matSpec.getName());

					// ----- DISPOSAL ----
					// assumptions are the the category multiplioer does not need to be applied for
					// disposal stages
					// see: Rekenregels_materiaalgebonden_milieuprestatie_gebouwen.pdf for more info
					for (Entry<NmdLifeCycleStage, Double> entry : matSpec.getDisposalRatios().entrySet()) {
						double cost = lifeTimeTotalMassKg * entry.getValue();

						results.incrementCostFactors(
								matSpec.getFaseProfiel(entry.getKey()).calculateFactors(cost, costWeightFactors),
								specs.getName(), matSpec.getName());

						// DISPOSALTRANSPORT - done per individual material and disposaltype rather than
						// per product
						results.incrementCostFactors(
								matSpec.getFaseProfiel(NmdLifeCycleStage.TransportForRemoval).calculateFactors(
										cost / 1000.0 * matSpec.getDisposalDistance(entry.getKey()), costWeightFactors),
								specs.getName(), matSpec.getName());
					}

					// ----- OPERATION COST ---- - apply different units of measure (l / m3 / kWh
					// etc.)

					// add the individual material to the composite material mass for transport
					// calculations
					specsMatSumKg += lifeTimeTotalMassKg;
				}

				// TODO: add correction factor for volume transport (standard 1)?
				// determine tranport costs per composed material in tonnes * km .
				// the factor 2 has been removed since May 2015
				results.incrementCostFactors(specs.getTransportProfile().calculateFactors(
						categoryMultiplier * specs.getDistanceFromProducer() * (specsMatSumKg / 1000.0),
						costWeightFactors), specs.getName());
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
	 * Calculate the number of replacements required during the building design
	 * life. Omit the initial application for Maintenance materials. source:
	 * https://www.milieudatabase.nl/imgcms/20141125_SBK_BepMeth_vs_2_0_inclusief_Wijzigingsblad_1_juni_2017_&_1_augustus_2017.pdf
	 * 
	 * @param designLife  total duration that building should be usable in years
	 * @param productLife design life of the material in years
	 * @return number of replacements. number is alsways larger or equal to 1
	 */
	private double calculateReplacements(double designLife, double productLife, boolean isMaintenanceMaterial) {
		if (isMaintenanceMaterial) {
			return Math.max(2.0, designLife / Math.max(1.0, productLife) - 1.0) - 1.0;
		} else {
			return Math.max(1.0, designLife / Math.max(1.0, productLife));
		}
	}

	public MpgCalculationResults getResults() {
		return results;
	}

	public void setResults(MpgCalculationResults results) {
		this.results = results;
	}

}
