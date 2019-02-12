package org.opensourcebim.mpgcalculation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bimserver.utils.AreaUnit;
import org.bimserver.utils.LengthUnit;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.nmd.NmdProductCard;
import org.opensourcebim.nmd.NmdProfileSet;
import org.opensourcebim.nmd.scaling.NmdScaler;

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

				// Determine replacements required.
				// this is usually 1 for regular materials and > 1 for cyclic maintenance
				// For a product card with composed profielsets (not a single totaalproduct) the
				// replacement of the
				// first encountered Construction (Cuas code 1) profielset will be used.
				double replacements = this.calculateReplacements(designLife, element);
				
				List<NmdProductCard> products = element.getNmdProductCards().stream()
						.map(p -> p.getValue()).collect(Collectors.toList());
				for (NmdProductCard product : products) {

					// category 3 data requires a 30% penalty
					double categoryMultiplier = product.getCategory() == 3 ? 1.3 : 1.0;

					// get number of product units based on geometry of ifcproduct and unit of
					// productcard
					// TODO: currently there is a very basic method implemented. should be improved.
					// This should include density conversions and/or energy water use conversion
					double unitsRequired = product.getRequiredNumberOfUnits(element.getMpgObject());
					
					for (NmdProfileSet profielSet : product.getProfileSets()) {
						double scaleFactor = 1.0;
						// determine scale factor based on scaler. if no scaler is present the
						// unitsRequired is sufficient (and no scaling is applied)
						if (element.requiresScaling() && profielSet.getIsScalable() && profielSet.getScaler() != null) {

							NmdScaler scaler = profielSet.getScaler();
							String unit = scaler.getUnit();
							int numDims = getUnitDimension(product.getUnit());
							if (numDims < 3) {
								
								Double[] dims = element.getMpgObject().getGeometry().getScaleDims(numDims);
								Double unitConversionFactor = getScalingUnitConversionFactor(unit, dims.length);
								
								scaleFactor = profielSet.getScaler().scaleWithConversion(dims, unitConversionFactor);
							}
						}

						// calculate total units required taking into account category modifiers.
						// replacements, # of profielSet items per productCard and scaling
						double lifeTimeUnitsPerProfiel = replacements * profielSet.getQuantity()
								* unitsRequired * categoryMultiplier * scaleFactor;

						// example for production
						profielSet.getAllFaseProfielen().values().forEach(fp -> {
							Set<MpgCostFactor> factors = fp.calculateFactors(lifeTimeUnitsPerProfiel);
							results.incrementCostFactors(factors, product.getDescription(), profielSet.getName());
						});
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

	/**
	 * Converts the This might be slightly counter intuititve, but when scaling over
	 * a single dimension we need a 2D conversionfactor while when we are scaling
	 * over 2 dimensions the scaling is done per axis and therefore the conversion
	 * is 1 D first figure out the quantity of the input unit and get the right
	 * store unit
	 * 
	 * @param unit working unit of the nmd scaler
	 * @param dims dimensionality of the object scaling dimensions (1 or 2)
	 * @return conversion factor to convert from scaling unit mpgObject geometry
	 *         unit.
	 */
	private Double getScalingUnitConversionFactor(String unit, int dims) {
		Double factor = 1.0;
		if (dims == 2) {
			switch (unit.toLowerCase()) {
			case "mm":
			case "millimiter":
				factor = this.objectStore.getLengthUnit().convert(1.0, LengthUnit.MILLI_METER);
				break;
			case "m":
			case "meter":
				factor = this.objectStore.getLengthUnit().convert(1.0, LengthUnit.METER);
			default:
				break;
			}
		} else if (dims == 1) {
			switch (unit.toLowerCase()) {
			case "mm":
			case "millimiter":
				factor = this.objectStore.getAreaUnit().convert(1.0, AreaUnit.SQUARED_MILLI_METER);
				break;
			case "m":
			case "meter":
				factor = this.objectStore.getAreaUnit().convert(1.0, AreaUnit.SQUARED_METER);
			default:
				break;
			}
		}
		return factor;
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
	 * @param element element with products linked to it
	 * @return number of replacements. number is alsways larger or equal to 1
	 */
	private Double calculateReplacements(double designLife, MpgElement element) {
		double productLife = -1.0;
		if (element.getNmdProductCards().size() == 0) {return Double.NaN;}
		
		// TODO: select first construction element rather than just the first element
		productLife = element.getNmdProductCards().get(0).getValue().getLifetime();

		return Math.max(1.0, designLife / Math.max(1.0, productLife));
	}

	public MpgCalculationResults getResults() {
		return results;
	}

	public void setResults(MpgCalculationResults results) {
		this.results = results;
	}
	
	private int getUnitDimension(String unit) {	
		switch (unit.toLowerCase()) {
		case "mm":
		case "cm":
		case "m1":
		case "m":
		case "meter":
			return 1;
		case "mm2":
		case "mm^2":
		case "square_millimeter":
		case "cm2":
		case "cm^2":
		case "m2":
		case "m^2":
		case "square_meter":
			return 2;
		case "mm3":
		case "mm^3":
		case "cubic_millimeter":
		case "cm3":
		case "cm^3":
		case "m3":
		case "m^3":
		case "cubic_meter":
			return 3;
		default:
			return -1;
		}
	}

}
