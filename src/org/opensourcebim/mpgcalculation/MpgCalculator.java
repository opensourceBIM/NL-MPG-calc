package org.opensourcebim.mpgcalculation;

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

	public MpgCalculator() {
		setResults(new MpgCalculationResults());
	}

	public MpgCalculationResults calculate(double designLife) {

		if (objectStore == null) {
			results.SetResultsStatus(ResultStatus.NoData);
			return results;
		}

		if (!(objectStore.isIfcDataComplete() && objectStore.isElementDataComplete())) {
			results.SetResultsStatus(ResultStatus.IncompleteData);
		}

		try {
			results.setTotalLifeTime(designLife);
			results.setTotalFloorArea(objectStore.getTotalFloorArea());

			// for each building material found:
			for (MpgElement element : objectStore.getElements()) {
				if (element.getNmdProductCard() == null) {continue;}
				
				NmdProductCard product = element.getNmdProductCard();

				for (NmdProfileSet profielSet : product.getProfileSets()) {
					
					// get number of product units based on geometry of ifcproduct and unit of productcard
					double unitsRequired = profielSet.getRequiredNumberOfUnits(element.getMpgObject());
					
					// category 3 data requires a 30% penalty
					double categoryMultiplier = profielSet.getCategory() == 3 ? 1.3 : 1.0;
					
					// Determine replacements required.
					// this is usually 1 for regular materials and > 1 for cyclic maintenance
					// materials. ToDo: check whether these calculations are still correct
					double replacements = this.calculateReplacements(designLife, profielSet.getProductLifeTime(),
							profielSet.getIsMaintenanceSpec());

					// calculate total mass taking into account construction losses and replacements
					// ToDo: how does the current approach tackle issues with plate materials etc.?
					// ToDo: We do not have desnities of materials in the DB. How to get the masses of products?
					double lifeTimeUnitsPerSpec = replacements * unitsRequired;

					// example for production
					profielSet.getDefinedProfiles().forEach(profileName -> {
						results.incrementCostFactors(profielSet.getFaseProfiel(profileName).calculateFactors(
								lifeTimeUnitsPerSpec * categoryMultiplier), product.getName(), profielSet.getName());
					});
				}
			}
			if (results.getStatus() != ResultStatus.IncompleteData) {
				results.SetResultsStatus(ResultStatus.Success);
			}

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
