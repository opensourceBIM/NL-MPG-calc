package org.opensourcebim.nmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bimserver.utils.AreaUnit;
import org.bimserver.utils.LengthUnit;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgGeometry;
import org.opensourcebim.ifccollection.MpgInfoTagType;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.ifccollection.MpgScalingOrientation;
import org.opensourcebim.mpgcalculation.MpgCostFactor;
import org.opensourcebim.nmd.scaling.NmdScaler;
import org.opensourcebim.nmd.scaling.NmdScalingUnitConverter;

/**
 * This implementation allows one of the services to be an editable data service
 * that also allows the user to add new data to be reused in other choices.
 * 
 * @author vijj
 *
 */
public class NmdDataResolverImpl implements NmdDataResolver {

	private List<NmdDataService> services;
	private MpgObjectStore store;

	public NmdDataResolverImpl() {
		services = new ArrayList<NmdDataService>();
		NmdDatabaseConfig config = new NmdDatabaseConfigImpl();
		this.addService(new NmdDataBaseSession(config));
	}

	public MpgObjectStore getStore() {
		return store;
	}

	public void setStore(MpgObjectStore store) {
		this.store = store;
	}

	/**
	 * Start the various subscribed services and try get the most viable
	 * productcards for every MpgObject found
	 */
	@Override
	public void NmdToMpg() {

		if (this.getStore() == null) {
			return;
		}

		// try to find all possible nlsfb codes for a mpgObject
		this.resolveNlsfbCodes();

		// try to find the correct dimensions for objects that could not have
		// their geometries resolved
		this.resolveUnknownGeometries();

		try {
			// start any subscribed services
			for (NmdDataService nmdDataService : services) {
				nmdDataService.login();
				nmdDataService.preLoadData();
			}

			// get data per material
			// ToDo: group the elements that are equal for sake of the mapping process (geometry, type, ..) to avoid a lot of duplication
			for (MpgElement element : getStore().getElements()) {
				// element could already have a mapping through a decomposes relation
				// in that case skip to the next one.
				if (!element.hasMapping()) {
					resolveNmdMappingForElement(element);
				}
			}

		} catch (ArrayIndexOutOfBoundsException ex) {
			System.out.println("Error occured in retrieving material data");
		}

		finally {
			for (NmdDataService nmdDataService : services) {
				nmdDataService.logout();
			}
		}
	}

	@Override
	public void addService(NmdDataService nmdDataService) {
		// check if same service is not already present?
		services.add(nmdDataService);

		if (nmdDataService instanceof EditableDataService) {
			// mark it as the editor of choice. only 1 editor should be available.
		}
	}

	public void clearServices() {
		this.services.clear();
	}

	private void resolveNmdMappingForElement(MpgElement mpgElement) {

		// resolve which product card to retrieve based on the input MpgElement
		if (mpgElement.getMpgObject() == null) {
			return;
		}

		// STEP 1: find any relevant NLsfb codes
		Set<String> alternatives = mpgElement.getMpgObject().getNLsfbAlternatives();
		if (alternatives.size() == 0 || alternatives.stream().allMatch(c -> c == null)) {
			mpgElement.getMpgObject().addTag(MpgInfoTagType.nmdProductCardWarning,
					"No NLsfbcodes linked to the product");
			return;
		}

		// STEP 2: find all the elements that we **could** include based on NLsfb match..
		NmdDataService service = services.get(0);
		List<NmdElement> allMatchedElements = service.getElementsForNLsfbCodes(alternatives);

		if (allMatchedElements.size() == 0) {
			mpgElement.getMpgObject().addTag(MpgInfoTagType.nmdProductCardWarning,
					"No NMD element match for any of the listed NLsfb codes");
			return;
		}

		// STEP3: select the elements we want to include
		List<NmdElement> candidateElements = selectCandidateElements(mpgElement, allMatchedElements);
		if (candidateElements.size() == 0) {
			mpgElement.getMpgObject().addTag(MpgInfoTagType.nmdProductCardWarning,
					"None of the candidate NmdElements matching the selection criteria.");
		}

		// STEP4: from every candidate element we should pick 0:1 productcards and add the results to mapping object
		List<NmdProductCard> selectedProducts = selectProductsForElements(mpgElement, candidateElements, service);
		if (selectedProducts.size() > 0) {
			selectedProducts.forEach(c -> mpgElement.addProductCard(c));
			mpgElement.setMappingMethod(NmdMapping.DirectDeelProduct);
			if (selectedProducts.size() == candidateElements.size() || selectedProducts.stream().anyMatch(pc -> pc.getIsTotaalProduct())) {
				mpgElement.setIsFullyCovered(true);
			}
			
		} else {
			mpgElement.setMappingMethod(NmdMapping.None);
			mpgElement.getMpgObject().addTag(MpgInfoTagType.nmdProductCardWarning,
					"No NMD productCard matching the selection criteria.");
		}
	}

	/**
	 * Determine which elements should be included based on restrictions and ifc
	 * object properties
	 * 
	 * @param mpgElement object containing non mapped ifc product properties
	 * @param candidates possible nmd elements to choose from
	 * @return a list of elements of which at least one product should be selected
	 *         for the mapping
	 */
	private List<NmdElement> selectCandidateElements(MpgElement mpgElement, List<NmdElement> candidates) {
		// due to lack of test data now only apply a simple filter..
		// ToDo: improve selection with most likely material match etc.
		return candidates.stream(
				).filter(ce -> (ce.getIsMandatory() && ce.getProducts().size() > 0) 
						|| ce.getProducts().stream().anyMatch(pc -> pc.getIsTotaalProduct()))
				.collect(Collectors.toList());
	}

	/**
	 * Find out wich candiate product should be mapped to the mpgElement.
	 * 
	 * @param mpgElement mpgElement to add product cards to
	 * @param candidates possible nmProductCard matches for the mpgElement
	 */
	private List<NmdProductCard> selectProductsForElements(MpgElement mpgElement, List<NmdElement> candidates,
			NmdDataService service) {
		NmdProductCard prod = null;
		List<NmdProductCard> viableCandidates = new ArrayList<NmdProductCard>();
		for (NmdElement el : candidates) {

			for (NmdProductCard card : el.getProducts()) {
				// per found element we should try to select a fitting productCard
				prod = new NmdProductCardImpl(card);
				int dims = NmdScalingUnitConverter.getUnitDimension(prod.getUnit());
				if (service.getAdditionalProfileDataForCard(prod)) {
					// determine scaling dimension
					MpgScalingOrientation orientation = mpgElement.getMpgObject().getGeometry()
							.getScalerOrientation(dims);

					// check if the productcard does not constrain the element in its physical dimensions.
					if (canProductBeUsedForElement(prod, orientation)) {
						// if we found a matching totaal product we can stop our search and return the prod.
						if (prod.getIsTotaalProduct()) {
							viableCandidates.clear();
							return Arrays.asList(prod);
						}
						viableCandidates.add(card);
					}
				}
			}
			// ToDo: add selection of worst/best candidate (sum coefficientSum of profilesets?
		}
		
		return viableCandidates;
	}

	/**
	 * Check if the product card can be mapped on the element based on scaling
	 * restrictions
	 * 
	 * @param prod       the canidate NmdProductCard
	 * @param mpgElement mpgElement that we want to add the productCard to
	 * @return a boolean to indicate whether the ProductCard is a viable option
	 */
	private boolean canProductBeUsedForElement(NmdProductCard prod, MpgScalingOrientation orientation) {
		boolean res = true;
		int numDims = NmdScalingUnitConverter.getUnitDimension(prod.getUnit());
		for (NmdProfileSet profielSet : prod.getProfileSets()) {

			// if there is no scaler defined, but the item is marked as scalable return true
			// by default.
			if (profielSet.getIsScalable() && profielSet.getScaler() != null) {
				NmdScaler scaler = profielSet.getScaler();

				String unit = scaler.getUnit();
				if (numDims < 3) {

					Double[] dims = orientation.getScaleDims();
					Double convFactor = NmdScalingUnitConverter.getScalingUnitConversionFactor(unit, dims.length,
							this.getStore());
					if (!scaler.areDimsWithinBounds(dims, convFactor)) {
						res = false;
					}
				}
			} else if (!profielSet.getIsScalable()) {
				return false;
			}
		}

		return res;
	}

	/**
	 * go through all objets and try to find an appropriate element that matches the
	 * NLsfb code. If no code can be found try resolving the NLsfb code by looking
	 * at decomposing elements and/or a list of alternative IfcProduct to NLsfb
	 * mappings.
	 */
	public void resolveNlsfbCodes() {

		HashMap<String, String[]> map = getProductTypeToNmdElementMap();
		String[] emptyMap = null;

		this.getStore().getElements().forEach(el -> {

			// find NLsfb codes for child objects that have no cde themselves.
			MpgObject o = el.getMpgObject();
			if (!o.hasNlsfbCode()) {
				if (!o.getParentId().isEmpty()) {
					MpgObject p = this.getStore().getObjectByGuid(o.getParentId()).get();
					String parentCode = p.getNLsfbCode();
					if (parentCode != null && !parentCode.isEmpty()) {
						o.setNLsfbCode(parentCode);
						o.addTag(MpgInfoTagType.nlsfbCodeFromResolvedType, "resolved from: " + p.getGlobalId());
					}
				}
			}

			// Now add all aternatives to fall back on should the first mapping give no or
			// non feasible results
			String[] foundMap = map.getOrDefault(o.getObjectType(), emptyMap);
			if (foundMap == null) {
				return;
			} else {
				o.addNlsfbAlternatives(new HashSet<String>(Arrays.asList(foundMap)));
			}
		});
	}

	/**
	 * run through mpgObjects where no geometry could be found and match these with
	 * first: similar nlsfb code objects and second (Not implemented yet)
	 */
	public void resolveUnknownGeometries() {
		// maintain a map with already found scalers to boost performance.
		HashMap<String, MpgGeometry> foundGeometries = new HashMap<String, MpgGeometry>();
		Function<MpgObject, String> getNLsfbProp = (MpgObject e) -> {
			return e.getNLsfbCode();
		};
		Function<MpgObject, String> getProdTypeProp = (MpgObject e) -> {
			return e.getObjectType();
		};

		// TODO: we can make this a lot more abstract to run this for any set of
		// properties,
		// but let's skip that for now.
		this.getStore().getObjects().stream().filter(o -> !o.getGeometry().getIsComplete()).forEach(o -> {
			MpgGeometry geom = null;
			String NLsfbKey = o.getNLsfbCode();

			if (foundGeometries.containsKey(NLsfbKey)) {
				geom = foundGeometries.get(NLsfbKey);
			} else {
				geom = findGeometryForMpgObjectProperty(getNLsfbProp, NLsfbKey);
				foundGeometries.put(NLsfbKey, geom);
			}
			if (geom != null) {
				o.getGeometry().setDimensionsByVolumeRatio(geom);
				o.getGeometry().setIsComplete(true);
				o.addTag(MpgInfoTagType.geometryFromResolvedType, "dimensions resolved by NLsfb match");
			} else {
				// fallback option is to look at similar IfcProducts
				String prodTypeKey = o.getObjectType();
				if (foundGeometries.containsKey(prodTypeKey)) {
					geom = foundGeometries.get(prodTypeKey);
				} else {
					geom = findGeometryForMpgObjectProperty(getProdTypeProp, prodTypeKey);
					foundGeometries.put(prodTypeKey, geom);
				}

				if (geom != null) {
					o.getGeometry().setDimensionsByVolumeRatio(geom);
					o.getGeometry().setIsComplete(true);
					o.addTag(MpgInfoTagType.geometryFromResolvedType, "dimensions resolved by IfcProduct type match");
				}
			}
		});
	}

	/**
	 * Get the scalers matching a referenceProperty value by looking for scalers in
	 * all objects with an equal property value
	 * 
	 * @param propMethod        Function that returns the requested property from an
	 *                          MpgObject
	 * @param referenceProperty the reference property value that the other objects
	 *                          should match with.
	 * @return the MpgScalingType that is most likely to match
	 */
	private MpgGeometry findGeometryForMpgObjectProperty(Function<MpgObject, String> propMethod,
			String referenceProperty) {
		List<MpgGeometry> candidates = this.getStore().getObjects().stream().filter(o -> propMethod.apply(o) != null)
				.filter(o -> propMethod.apply(o).equals(referenceProperty)).filter(o -> o.getGeometry().getIsComplete())
				.map(o -> o.getGeometry()).distinct().collect(Collectors.toList());

		Optional<MpgGeometry> geom = candidates.stream().filter(g -> g.getDimensions().length == 3).findFirst();
		if (geom.isPresent()) {
			return geom.get();
		} else {
			return null;
		}
	}

	private HashMap<String, String[]> getProductTypeToNmdElementMap() {
		HashMap<String, String[]> elementMap = new HashMap<String, String[]>();
		elementMap.put("BuildingElementProxy", new String[] { "11.", "32.35", "32.36", "32.37", "32.38", "32.39" });
		elementMap.put("Footing", new String[] { "16." });
		elementMap.put("Slab", new String[] { "13.", "23.", "28.2", "28.3", "33.21" });
		elementMap.put("Pile", new String[] { "17." });
		elementMap.put("Column", new String[] { "17.", "28.1" });
		elementMap.put("Wall", new String[] { "21.", "22." });
		elementMap.put("CurtainWall", new String[] { "21.24", "32.4" });
		elementMap.put("Stair", new String[] { "24." });
		elementMap.put("Roof", new String[] { "27." });
		elementMap.put("Beam", new String[] { "28." });
		elementMap.put("Window", new String[] { "31.2", "32.12", "32.2", "37.2" });
		elementMap.put("Door", new String[] { "31.3", "32.11", "32.3" });
		elementMap.put("Railing", new String[] { "34." });
		elementMap.put("Covering", new String[] { "41.", "42.", "43.", "44.", "45.", "47.", "48." });
		elementMap.put("FlowSegment", new String[] { "52.", "53.", "55.", "56.", "57." }); // many more
		elementMap.put("FlowTerminal", new String[] { "74.1", "74.2" }); // many more

		return elementMap;
	}

}
