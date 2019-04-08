package org.opensourcebim.mpgcalculation;

import java.util.Set;

import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.ifccollection.MpgScalingOrientation;
import org.opensourcebim.nmd.scaling.NmdScalingUnitConverter;

import nl.tno.bim.nmd.domain.NmdCostFactor;
import nl.tno.bim.nmd.domain.NmdProductCard;
import nl.tno.bim.nmd.domain.NmdProfileSet;
import nl.tno.bim.nmd.scaling.NmdScaler;

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
		reset();
	}

	public void reset() {
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

				for (NmdProductCard product : element.getNmdProductCards()) {

					// Determine replacements required based on lifetime of productcard
					// this is usually 1 for regular materials and > 1 for cyclic maintenance
					double replacements = this.calculateReplacements(designLife, product);

					// category 3 data requires a 30% penalty
					double categoryMultiplier = product.getCategory() == 3 ? 1.3 : 1.0;

					// get number of product units based on geometry of ifcproduct and unit of
					// productcard
					double unitsRequired = element.getRequiredNumberOfUnits(product);

					for (NmdProfileSet profielSet : product.getProfileSets()) {
						if (profielSet.getQuantity() > 0.0) {
							double scaleFactor = 1.0;
							// determine scale factor based on scaler. if no scaler is present the
							// unitsRequired is sufficient (and no scaling is applied)
							if (element.requiresScaling() && profielSet.getIsScalable()) {

								if (profielSet.getScaler() != null) {
									NmdScaler scaler = profielSet.getScaler();
									int numDims = NmdScalingUnitConverter.getUnitDimension(product.getUnit());
									if (numDims < 3) {

										MpgScalingOrientation or = element.getMpgObject().getGeometry()
												.getScalerOrientation(numDims);
										Double[] dims = or.getScaleDims();
										Double unitConversionFactor = NmdScalingUnitConverter
												.getScalingUnitConversionFactor(scaler.getUnit(), this.getObjectStore());

										scaleFactor = scaler.scaleWithConversion(dims, unitConversionFactor);
									}
								}
							}

							// calculate total units required taking into account category modifiers.
							// replacements, # of profielSet items per productCard and scaling
							double lifeTimeUnitsPerProfiel = replacements * profielSet.getQuantity() * unitsRequired
									* categoryMultiplier * scaleFactor;

							// example for production
							profielSet.getAllFaseProfielen().values().forEach(fp -> {
								Set<NmdCostFactor> factors = fp.calculateFactors(lifeTimeUnitsPerProfiel);
								Long objectId = element.getMpgObject().getObjectId();
								results.addCostFactors(factors, product.getDescription(), profielSet.getName(), objectId);
							});
						}
					}
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
	 * @param designLife total duration that building should be usable in years
	 * @param card       productcard with product lifetime
	 * @return number of replacements. number is alsways larger or equal to 1
	 */
	private Double calculateReplacements(double designLife, NmdProductCard card) {
		double productLife = -1.0;
		productLife = card.getLifetime();

		return Math.max(1.0, designLife / Math.max(1.0, productLife));
	}

	public MpgCalculationResults getResults() {
		return results;
	}

	public void setResults(MpgCalculationResults results) {
		this.results = results;
	}
}
