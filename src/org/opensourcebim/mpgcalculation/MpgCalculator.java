package org.opensourcebim.mpgcalculation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.opensourcebim.ifccollection.MaterialSource;
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
							if (product.requiresScaling() && profielSet.getIsScalable()) {
								NmdScaler scaler = product.getScalerForProfileSet(profielSet.getProfielId());
								// no need to scale bulk goods (in volume units)
								int numDims = NmdScalingUnitConverter.getUnitDimension(product.getUnit());
								if (numDims < 3 && scaler != null) {
									MpgScalingOrientation or = element.getMpgObject().getGeometry()
											.getScalerOrientation(numDims);
									Double[] dims = or.getScaleDims();
									Double unitConversionFactor = NmdScalingUnitConverter
											.getScalingUnitConversionFactor(scaler.getUnit(), this.getObjectStore());

									scaleFactor = scaler.scaleWithConversion(dims, unitConversionFactor);
								}
							}

							// calculate total units required taking into account category modifiers.
							// replacements, # of profielSet items per productCard and scaling
							double lifeTimeUnitsPerProfile = replacements * profielSet.getQuantity() * unitsRequired
									* categoryMultiplier * scaleFactor;

							// example for production
							profielSet.getAllFaseProfielen().values().forEach(fp -> {
								Set<NmdCostFactor> factors = fp.calculateFactors(lifeTimeUnitsPerProfile);
								Long objectId = element.getMpgObject().getObjectId();
								results.addCostFactors(factors, product.getDescription(), profielSet.getName(), objectId);
							});
						}
					}
				}
			}

			this.generateBillOfMaterials();
			
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
	
	/**
	 * 
	 * @param store
	 * @return void, but edits the results object to contain a BoM object
	 */
	public void generateBillOfMaterials() {
		BillOfMaterials bom = new BillOfMaterials();
		if (this.objectStore != null && this.objectStore.getProductCards().size() > 0) {
			Collection<List<MpgElement>> elementGroups = this.objectStore.getElementGroups().values();
			HashMap<Long, Double> mkiContributions = this.results.getCostPerObjectId();
			
			for (List<MpgElement> elements : elementGroups) {
				// get the total score contributed by these elements
				Map<String, Object> bomEntry = new HashMap<String, Object>();
				List<NmdProductCard> cards = elements.stream()
						.flatMap(el -> el.getNmdProductCards().stream())
						.distinct().collect(Collectors.toList());
				
				String unit = "";
				String cardDescription = "";
				Integer nmdId = null;
				if (!elements.get(0).getMappingMethod().isIndirectMapping()) {
					if (cards.size() >= 1) {
						if (cards.size() > 1) {
							bom.addAnnotation(String.format(
									"found more than one product card for element group %s",
									elements.get(0).getUnMappedGroupHash()));
						}
						unit = cards.get(0).getUnit();
						cardDescription = String.join("|", cards.stream().map(c -> c.getDescription()).collect(Collectors.toList()));
						nmdId = cards.get(0).getProductId();
					} else {
						bom.addAnnotation(String.format(
								"found no product card for element group %s",
								elements.get(0).getUnMappedGroupHash()));
					} 
				}
				
				String materialNames = elements.get(0).getMpgObject().getListedMaterials().stream()
						.map(MaterialSource::getName)
						.collect(Collectors.joining("|"));
				
				Double totalVolume = elements.stream()
						.mapToDouble(el -> el.getMpgObject().getGeometry().getVolume())
						.filter(vol -> Double.isFinite(vol)).sum();
				Double totalArea = elements.stream()
						.mapToDouble(el -> el.getMpgObject().getGeometry().getFaceArea())
						.filter(vol -> Double.isFinite(vol)).sum();
				
				List<Long> ids = elements.stream().map(el -> el.getMpgObject().getObjectId()).collect(Collectors.toList());
				
				Double mkiContribution = ids.stream()
				        .filter(mkiContributions::containsKey)
				        .collect(Collectors.toMap(Function.identity(), mkiContributions::get))
				        .values().stream().mapToDouble(d -> d).sum();
					
				// report the total score together with metadata such as total volume
				// num elements, material description etc.
				bomEntry.put("nmd card", cardDescription);
				bomEntry.put("nmd id", nmdId);
				bomEntry.put("mapping type", elements.get(0).getMappingMethod());
				
				bomEntry.put("Ifc type", elements.get(0).getMpgObject().getObjectType());
				bomEntry.put("Ifc object name", elements.get(0).getMpgObject().getObjectName());
				bomEntry.put("IFC materials", materialNames);
				bomEntry.put("number of elements", elements.size());
				bomEntry.put("unit", unit);
				bomEntry.put("volume", totalVolume);
				bomEntry.put("area", totalArea);
				bomEntry.put("mki contribution", mkiContribution);
				bom.addEntry(bomEntry);
			}
		}
		this.results.setBillOfMaterials(bom);
	}
}
