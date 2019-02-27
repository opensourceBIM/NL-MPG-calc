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
import org.apache.commons.lang3.StringUtils;
import org.opensourcebim.ifccollection.MaterialSource;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgGeometry;
import org.opensourcebim.ifccollection.MpgInfoTagType;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.ifccollection.MpgScalingOrientation;
import org.opensourcebim.ifccollection.NlsfbCode;
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

	private NmdDataService service;
	private NmdMappingService mappingService;
	private MpgObjectStore store;

	public NmdDataResolverImpl() {
		// NmdDatabaseConfig config = new NmdDatabaseConfigImpl();
		setService(new Nmd2DataService());
		setMappingService(new NmdUserMappingService());
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
			// start subscribed services
			getService().login();
			getService().preLoadData();

			// get data per material
			// ToDo: group the elements that are equal for sake of the mapping process
			// (geometry, type, ..) to avoid a lot of duplication
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
			getService().logout();
		}
	}

	@Override
	public void setService(NmdDataService nmdDataService) {
		// check if same service is not already present?
		this.service = nmdDataService;
	}

	public NmdDataService getService() {
		return this.service;
	}

	private void resolveNmdMappingForElement(MpgElement mpgElement) {

		// resolve which product card to retrieve based on the input MpgElement
		if (mpgElement.getMpgObject() == null) {
			return;
		}
		// first try to find any user defined mappings
		NmdUserMap map = mappingService.getApproximateMapForObject(mpgElement.getMpgObject());
		if (map != null) {
			// get products in map and exit if succeeded.
		}

		// STEP 1: find any relevant NLsfb codes
		Set<NlsfbCode> alternatives = mpgElement.getMpgObject().getNLsfbAlternatives();
		if (alternatives.size() == 0 || alternatives.stream().allMatch(c -> c == null)) {
			mpgElement.getMpgObject().addTag(MpgInfoTagType.nmdProductCardWarning,
					"No NLsfbcodes linked to the product");
			return;
		}

		// STEP 2: find all the elements that we **could** include based on NLsfb
		// match..
		List<NmdElement> allMatchedElements = getService().getElementsForNLsfbCodes(alternatives);

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

		// STEP4: from every candidate element we should pick 0:1 productcards and add
		// the results to mapping object
		Set<NmdProductCard> selectedProducts = selectProductsForElements(mpgElement, candidateElements);

		if (selectedProducts.size() > 0) {
			selectedProducts.forEach(c -> mpgElement.addProductCard(c));
			mpgElement.setMappingMethod(NmdMapping.DirectDeelProduct);
			if (selectedProducts.size() == candidateElements.size()
					|| selectedProducts.stream().anyMatch(pc -> pc.getIsTotaalProduct())) {
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

		List<NmdElement> filteredCandidates = candidates.stream()
				.filter(ce -> (ce.getIsMandatory() && ce.getProducts().size() > 0)
						|| ce.getProducts().stream().anyMatch(pc -> pc.getIsTotaalProduct()))
				.collect(Collectors.toList());

		return filteredCandidates;
	}

	/**
	 * Find out wich candidate product should be mapped to the mpgElement.
	 * 
	 * @param mpgElement mpgElement to add product cards to
	 * @param candidates possible nmProductCard matches for the mpgElement
	 */
	private Set<NmdProductCard> selectProductsForElements(MpgElement mpgElement, List<NmdElement> candidates) {

		List<NmdProductCard> allProducts = candidates.stream().flatMap(e -> e.getProducts().stream())
				.collect(Collectors.toList());
		List<MaterialSource> mats = mpgElement.getMpgObject().getListedMaterials();
		Set<NmdProductCard> viableCandidates = new HashSet<NmdProductCard>();

		// currently: select most favorable card
		Function<List<NmdProductCard>, NmdProductCard> selectCard = (list) -> {
			list.sort((pc1, pc2) -> pc1.getProfileSetsCoeficientSum().compareTo(pc2.getProfileSetsCoeficientSum()));
			return list.get(0);
		};

		// Find per material the most likely candidates that fall within the
		// specifications
		allProducts.forEach(p -> getService().getAdditionalProfileDataForCard(p));
		
		mats.forEach(mat -> {
			List<NmdProductCard> productOptions = sortProductsBasedOnStringSimilarity(mat.getName(), allProducts, 3);

			for (NmdProductCard card : productOptions) {
				// per found element we should try to select a fitting productCard
				int dims = NmdScalingUnitConverter.getUnitDimension(card.getUnit());
				if (card.getProfileSets().size() > 0) {
					// determine scaling dimension
					MpgScalingOrientation orientation = mpgElement.getMpgObject().getGeometry()
							.getScalerOrientation(dims);

					// check if the productcard does not constrain the element in its physical
					// dimensions.
					if (!canProductBeUsedForElement(card, orientation)) {
						productOptions.remove(card);
					}
				}
			}

			// determine which product card should be returned based on an input filter
			// function and add this one to the results list
			// ToDo: currently this is a set Function, but this can be replaced with any
			// user defined selection method.
			if (productOptions.size() > 0) {
				viableCandidates.add(selectCard.apply(productOptions));
			}

		});

		return viableCandidates;
	}

	private List<NmdProductCard> sortProductsBasedOnStringSimilarity(String name, List<NmdProductCard> allProducts,
			Integer cutOff) {
		// sort the found products for every entry in the material list
		List<NmdProductCard> selectedProducts = new ArrayList<NmdProductCard>();
		allProducts.sort((p1, p2) -> Integer.compare(getMinLevenshteinDistance(name, p1),
				getMinLevenshteinDistance(name, p2)));

		for (int i = allProducts.size() - 1; i >= 0; i--) {
			// ToDo cutoff items that have nothing to do with the actul naming rather than a
			// hard coded cutoff
			if (i < cutOff) {
				selectedProducts.add(new NmdProductCardImpl(allProducts.get(i)));
			}
		}
		return selectedProducts;
	}

	/**
	 * Determine the minimum levenshstein distance with respect to the profiel set descriptions of the product card
	 * @param refWord reference word to search for within the product card
	 * @param card NmdProductCard object with > 0 profileSets
	 * @return the minimum levensthein distance of the reference word wrt any of the profileSet names
	 */
	private Integer getMinLevenshteinDistance(String refWord, NmdProductCard card) {
		List<String> keyWords = card.getProfileSets().stream()
				.flatMap(ps -> Arrays.asList(ps.getName().split(" ")).stream())
				.filter(w -> !w.isEmpty()).collect(Collectors.toList());
		keyWords.forEach(word -> word.replaceAll("[^a-zA-Z]", ""));
		keyWords.sort((w1, w2) -> Integer.compare(StringUtils.getLevenshteinDistance((CharSequence) refWord, w1),
				StringUtils.getLevenshteinDistance((CharSequence) refWord, w2)));
		return StringUtils.getLevenshteinDistance((CharSequence) refWord, keyWords.get(0));
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
		int numDims = NmdScalingUnitConverter.getUnitDimension(prod.getUnit());
		for (NmdProfileSet profielSet : prod.getProfileSets()) {

			// if there is no scaler defined, but the item is marked as scalable return true
			// by default.
			if (profielSet.getIsScalable()) {
				// should there be no scaler defined, we can assume linear scaling
				if (profielSet.getScaler() == null) {
					return true;
				}

				NmdScaler scaler = profielSet.getScaler();

				String unit = scaler.getUnit();
				if (numDims < 3) {

					Double[] dims = orientation.getScaleDims();
					if (scaler.getNumberOfDimensions() > dims.length) {
						return false;
					}

					Double convFactor = NmdScalingUnitConverter.getScalingUnitConversionFactor(unit, this.getStore());
					if (!scaler.areDimsWithinBounds(dims, convFactor)) {
						return false;
					}
				}
			} else if (!profielSet.getIsScalable()) {
				return false;
			}
		}

		return true;
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
			
			// find NLsfb codes for child objects that have no code themselves.
			MpgObject o = el.getMpgObject();
			String[] foundMap = map.getOrDefault(o.getObjectType(), emptyMap);
			
			boolean hasParent = o.getParentId() != null && !o.getParentId().isEmpty();
			MpgObject p = new MpgObjectImpl();
			if (hasParent) {
				p = this.getStore().getObjectByGuid(o.getParentId()).get();
				
				if (!o.hasNlsfbCode()) {
					
					String parentCode = p.getNLsfbCode();
					if (parentCode != null && !parentCode.isEmpty()) {
						o.setNLsfbCode(parentCode);
						o.addTag(MpgInfoTagType.nlsfbCodeFromResolvedType, "resolved from: " + p.getGlobalId());
					}
				}
				
				// Now add all aternatives to fall back on should the first mapping give no or
				if (foundMap == null) {
					foundMap = map.getOrDefault(p.getObjectType(), emptyMap);
				} 
			}
			
			if(foundMap != null && foundMap.length > 0) {
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
		elementMap.put("WallStandardCase", new String[] { "21.", "22." });
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

	public NmdMappingService getMappingService() {
		return mappingService;
	}

	public void setMappingService(NmdMappingService mappingService) {
		this.mappingService = mappingService;
	}

}
