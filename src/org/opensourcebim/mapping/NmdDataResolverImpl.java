package org.opensourcebim.mapping;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opensourcebim.ifccollection.MaterialSource;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgGeometry;
import org.opensourcebim.ifccollection.MpgInfoTagType;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.ifccollection.MpgScalingOrientation;
import org.opensourcebim.nmd.MappingDataService;
import org.opensourcebim.nmd.NmdDataService;
import org.opensourcebim.nmd.NmdElement;
import org.opensourcebim.nmd.NmdMapping;
import org.opensourcebim.nmd.NmdProductCard;
import org.opensourcebim.nmd.NmdProfileSet;
import org.opensourcebim.nmd.NmdUserDataConfig;
import org.opensourcebim.nmd.NmdUserDataConfigImpl;
import org.opensourcebim.nmd.scaling.NmdScaler;
import org.opensourcebim.nmd.scaling.NmdScalingUnitConverter;

import nl.tno.bim.mapping.domain.Mapping;
import nl.tno.bim.mapping.domain.MappingSet;
import nl.tno.bim.mapping.domain.MappingSetMap;
import nl.tno.bim.mapping.domain.MaterialMapping;

/**
 * This implementation allows one of the services to be an editable data service
 * that also allows the user to add new data to be reused in other choices.
 * 
 * @author vijj
 *
 */
public class NmdDataResolverImpl implements NmdDataResolver {

	private NmdDataService service;
	private NmdUserDataConfig config;
	private MappingDataService mappingService;
	private MpgObjectStore store;
	private Set<String> keyWords;

	public NmdDataResolverImpl(Path rootPath) {
		config = new NmdUserDataConfigImpl(rootPath);
	}

	public NmdDataResolverImpl() {
		config = new NmdUserDataConfigImpl();
	}

	public MpgObjectStore getStore() {
		return store;
	}

	public void setStore(MpgObjectStore store) {
		this.store = store;
	}
	
	@Override
	public NmdUserDataConfig getConfig() {
		return config;
	}

	public MappingDataService getMappingService() {
		return mappingService;
	}

	@Override
	public void setMappingService(MappingDataService mappingService) {
		this.mappingService = mappingService;
		keyWords = mappingService.getKeyWordMappings(ResolverSettings.keyWordOccurenceMininum).keySet();
	}

	@Override
	public void setNmdService(NmdDataService nmdDataService) {
		// check if same service is not already present?
		this.service = nmdDataService;
	}

	public NmdDataService getService() {
		return this.service;
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

		// pre process: identify material keywords and/or nlsfb codes from material
		// and name descriptions.
		this.tryFindAdditionalInfo();

		// pre process: try to fill in missing Nlsfb codes based on hierarchy in the
		// model
		this.resolveNlsfbCodes();

		// pre process: try fill in dimensions by producttype relations for products
		// without parsed geometry
		this.resolveUnknownGeometries();
		
		this.tryApplyEarlierMappings();
		
		// start nmd service and
		try {
			getService().login();
			getService().preLoadData();

			MappingSet newMappings = new MappingSet();
			newMappings.setProjectId(store.getProjectId());
			newMappings.setRevisionId(store.getRevisionId());
			newMappings.setDate(new Date());
			
			// ToDo: group the elements that are 'equal' for sake of the mapping process
			// (geometry, type, ..) to avoid a lot of duplication
			for (MpgElement element : getStore().getElements()) {
				// element could already have a mapping through a decomposes relation
				// in that case skip to the next one.
				if (!element.hasMapping()) {
					resolveNmdMappingForElement(element);
					
					// add the newly created mapping to the mappingset
					NmdDataResolverImpl.addMappingToMappingSet(newMappings, element);
				}
			}
			
			// tried to map a new item on every unmapped nmd element. now push it to the db
			getMappingService().postMappingSet(newMappings);
		} catch (Exception e) {
			System.out.println("Error occured in retrieving material data");
		} finally {
			getService().logout();
		}
	}
	
	/**
	 * Check whether there is already a mappingset available for the given project/revision combination and
	 * apply any earlier stored mappings based on the ifc GUID matches.
	 */
	private void tryApplyEarlierMappings() {
		MappingSet set = null;
		try {
			set = getMappingService().getMappingSetByProjectIdAndRevisionId(store.getProjectId(), store.getRevisionId());
			if (set != null) {
				for (MappingSetMap map : set.getMappingSetMaps()) {
					Mapping nmdMap = map.getMapping();
					if(nmdMap != null) {
						// check whether the element still exists and the nmd product references are valid
						MpgElement el = store.getElementByObjectGuid(map.getElementGuid());
						if (el != null) {
							List<Long> ids = nmdMap.getAllNmdProductIds();
							if (ids.size() > 0) {
								List<NmdProductCard> cards = this.getService().getProductCardsByIds(ids);
								
								// first check if a totaal product needs to be mapped
								Long totId = nmdMap.getNmdTotaalProductId();
								Optional<NmdProductCard> totCard = cards.parallelStream().filter(c -> (long)c.getProductId() == totId).findFirst();
								if (totId != null && totId > 0 && totCard.isPresent()) {
									el.mapProductCard(new MaterialSource("-1", "totaal map", "mapService"), totCard.get());									
								}
										
								// next check for the material mappings and apply these
								nmdMap.getMaterialMappings().forEach(mMap -> {
									el.getMpgObject().getListedMaterials().forEach(mat -> {
										if (mat.getName().toLowerCase().trim().equals(mMap.getMaterialName().toLowerCase().trim())) {
											Optional<NmdProductCard> matCard = cards.parallelStream().filter(c -> (long)c.getProductId() == totId).findFirst();
											if(matCard.isPresent()) {
												el.mapProductCard(mat, matCard.get());
												el.setMappingMethod(NmdMapping.UserMapping);
											}
										}
									});
								});
							}
						}
					}
				}
			}
		} catch(Exception e) {
			System.err.println("Map service error: " + e.getMessage());
		}	
	}

	/**
	 * ToDo: add logic that checks to which mappingsetMap the maps hould be added.
	 * For now add a new set for each guid
	 * @param set MappingSet to add the Mapping to.
	 * @param el NmdElement that has just been mapped
	 */
	public static void addMappingToMappingSet(MappingSet set, MpgElement el) {
		// create the mapping from the element
		Mapping map = new Mapping();
		
		String nlsfb = el.getMpgObject().getNLsfbCode() ==  null ? "" : el.getMpgObject().getNLsfbCode().toString();
		map.setNlsfbCode(nlsfb);
		map.setOwnIfcType(el.getMpgObject().getObjectType());
		if (!el.getMpgObject().getParentId().isEmpty()) {
			map.setQueryIfcType("different");
		} else {
			map.setQueryIfcType(el.getMpgObject().getObjectType());
		}
		
		List<MaterialMapping> matMaps = new ArrayList<>();
		for(MaterialSource mat : el.getMpgObject().getListedMaterials()) {
			MaterialMapping matMap = new MaterialMapping();
			matMap.setMaterialName(mat.getName());
			matMap.setNmdProductId((long)mat.getMapId());
			matMaps.add(matMap);
		}
		map.setMaterialMappings(matMaps);	
		
		// if this is a new mapping create a new mappingset map and add it
		MappingSetMap msm = new MappingSetMap();
		msm.setElementGuid(el.getMpgObject().getGlobalId());
				
		msm.setMapping(map);
		if (set.getMappingSetMaps() == null) {
			set.setMappingSetMaps(new ArrayList<>());
		}
		set.getMappingSetMaps().add(msm);
	}
	
	/**
	 * Try find Nlsfb codes in the ifcName and IFcMaterials and try find materials
	 * in the the IFcName when these fields are not defined already.
	 */
	private void tryFindAdditionalInfo() {

		for (MpgElement el : store.getElements().stream().filter(el -> !el.getMpgObject().hasNlsfbCode())
				.collect(Collectors.toList())) {
			// get NLSfb codes from product name and material descriptions
			Set<String> nlsfbCodes = NmdDataResolverImpl.tryGetNlsfbCodes(el.getMpgObject().getObjectName());
			
			// add a material for any material not already present
			Set<String> matNames = new HashSet<String>(el.getMpgObject().getMaterialNamesBySource(null).stream()
					.map(name -> name.toLowerCase().trim()).collect(Collectors.toList()));
			
			matNames.forEach(mat -> nlsfbCodes.addAll(NmdDataResolverImpl.tryGetNlsfbCodes(mat)));

			// add the first item to the nlsfb code if not already set and all of them to
			// the alternatives list.
			if (!nlsfbCodes.isEmpty() && !el.getMpgObject().hasNlsfbCode()) {
				el.getMpgObject().setNLsfbCode(nlsfbCodes.iterator().next());
				el.getMpgObject().addNlsfbAlternatives(nlsfbCodes);
			} else if (!nlsfbCodes.isEmpty()) {
				el.getMpgObject().addNlsfbAlternatives(nlsfbCodes);
			}
		}

		for (MpgElement el : store.getElements().stream().filter(el -> el.getMpgObject().getListedMaterials().isEmpty())
				.collect(Collectors.toList())) {

			Set<String> foundMaterials = this.tryGetKeyMaterials(el.getMpgObject().getObjectName());

			// add the found materials as aa single material item only if there are no
			// materials already defined.
			if (!foundMaterials.isEmpty() && el.getMpgObject().getListedMaterials().isEmpty()) {

				el.getMpgObject().addMaterialSource(
						new MaterialSource("-1", String.join(" ", foundMaterials), "from description"));
			}
		}
	}

	/**
	 * try find a code that matches the 4 digit nlsfb pattern of 2 numbers a period
	 * and then 2 more numbers.
	 * 
	 * @param inputString a character string
	 * @return the nlsfb codes found in the input string
	 */
	public static Set<String> tryGetNlsfbCodes(String inputString) {
		Matcher m = Pattern.compile("(\\d{2}\\.\\d{2})").matcher(inputString);
		Set<String> res = new HashSet<String>();
		while (m.find()) {
			res.add(m.group(1));
		}
		return res;
	}

	/**
	 * Check whether a generic string contains a material keyword from the mapping
	 * db
	 * 
	 * @param objectName
	 * @return
	 */
	public Set<String> tryGetKeyMaterials(String objectName) {
		Set<String> objectDescription = NmdDataResolverImpl.parseStringForWords(objectName);
		Set<String> res = new HashSet<String>();
		if (!objectDescription.isEmpty()) {
			for (String word : objectDescription) {
				for (String key : keyWords) {
					if (word.contains(key)) {
						res.add(word);
					}
				}
			}
		}
		return res;
	}

	private static Set<String> parseStringForWords(String objectName) {
		return Arrays.asList(objectName.split(ResolverSettings.splitChars)).stream()
				.map(w -> w.replaceAll(ResolverSettings.numericReplacePattern, "").toLowerCase().trim())
				.filter(w -> w.length() >= ResolverSettings.minWordLengthForSimilarityCheck)
				.collect(Collectors.toSet());
	}

	private void resolveNmdMappingForElement(MpgElement mpgElement) {

		// resolve which product card to retrieve based on the input MpgElement
		if (mpgElement.getMpgObject() == null) {
			return;
		}

		// ToDo: implement correct user mapping....
		// first try to find any user defined mappings
		Mapping map = mappingService.getApproximateMapForObject(mpgElement.getMpgObject());
		if (map != null) {
			// get products in map and exit if succeeded.
		}

		// first try to resolve for explicitly indicated nlsfb code
		FindProductsForNlsfbCodes(mpgElement,
				new HashSet<NlsfbCode>(Arrays.asList(mpgElement.getMpgObject().getNLsfbCode())));
		if (!mpgElement.hasMapping()) {
			// if that doesn't work. try it for all the alternatives (no order of
			// precendence)
			FindProductsForNlsfbCodes(mpgElement, mpgElement.getMpgObject().getNLsfbAlternatives());
		}
	}

	/**
	 * Find a mapping for the element by looking at a selection of nlsfbCodes
	 * 
	 * @param mpgElement
	 * @param codeSet
	 */
	private void FindProductsForNlsfbCodes(MpgElement mpgElement, Set<NlsfbCode> codeSet) {
		// STEP 1: check if there are relevant nlsfbcodes present in the inp[ut set
		if (codeSet.size() == 0 || codeSet.stream().allMatch(c -> c == null)) {
			mpgElement.getMpgObject().addTag(MpgInfoTagType.nmdProductCardWarning,
					"No NLsfbcodes linked to the product");
			return;
		}

		// STEP 2: find all the elements that we **could** include based on NLsfb
		// match..
		List<NmdElement> allMatchedElements = getService().getElementsForNLsfbCodes(codeSet);
		if (allMatchedElements.size() == 0) {
			mpgElement.getMpgObject().addTag(MpgInfoTagType.nmdProductCardWarning,
					"No NMD element match for listed NLsfb codes");
			return;
		}

		// STEP3: select the elements we want to include based on mandatory flags and/or
		// other constraints
		List<NmdElement> candidateElements = selectCandidateElements(mpgElement, allMatchedElements);
		if (candidateElements.size() == 0) {
			mpgElement.getMpgObject().addTag(MpgInfoTagType.nmdProductCardWarning,
					"None of the candidate NmdElements matching the selection criteria.");
			return;
		}

		// STEP4: from every candidate element we should pick 0:1 productcards per
		// material and add a mapping
		Set<NmdProductCard> selectedProducts = selectProductsForElements(mpgElement, candidateElements);
		if (selectedProducts.size() > 0) {
			mpgElement.setMappingMethod(NmdMapping.DirectDeelProduct);
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

		for (MaterialSource mat : mats) {
			List<NmdProductCard> productOptions = selectProductsBasedOnStringSimilarity(mat.getName(), allProducts);

			// check if a decent enough filter has been made. if not tag that there are too
			// many options.
			// ToDo: make warning settings variable
			if (allProducts.size() * ResolverSettings.tooManyOptionsRatio <= productOptions.size()
					|| productOptions.size() > ResolverSettings.tooManyOptionsAbsNum) {
				mpgElement.getMpgObject().addTag(MpgInfoTagType.mappingWarning,
						"large uncertainty for mapping material: " + mat.getName());
			}

			for (NmdProductCard card : productOptions) {
				// per found element we should try to select a fitting productCard
				int dims = NmdScalingUnitConverter.getUnitDimension(card.getUnit());
				if (!card.getProfileSets().isEmpty()) {
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
			if (!productOptions.isEmpty()) {
				NmdProductCard chosenCard = selectCard.apply(productOptions);
				viableCandidates.add(chosenCard);
				mpgElement.mapProductCard(mat, chosenCard);
				mpgElement.getMpgObject().setNLsfbCode(chosenCard.getNlsfbCode());
			}
		}

		return viableCandidates;
	}

	/**
	 * Detemrine the top list of products based on a string similarity score
	 * 
	 * @param name        material name
	 * @param allProducts preselected list of cadidate productcards
	 * @return selection of productcards based on similarity score and cutoff
	 *         criteria
	 */
	private List<NmdProductCard> selectProductsBasedOnStringSimilarity(String name, List<NmdProductCard> allProducts) {
		List<Pair<NmdProductCard, Double>> prods = new ArrayList<Pair<NmdProductCard, Double>>();
		List<NmdProductCard> res = new ArrayList<NmdProductCard>();

		Set<String> materialDescription = parseStringForWords(name);

		if (materialDescription.isEmpty()) {
			return res;
		}

		allProducts.forEach(p -> prods
				.add(new ImmutablePair<NmdProductCard, Double>(p, getProductSimilarityScore(materialDescription, p))));

		// determine the best product and benchmark with the other remaining candidates.
		prods.sort((p1, p2) -> Double.compare(p1.getValue(), p2.getValue()));
		Double benchMark = prods.get(0).getValue();
		for (Pair<NmdProductCard, Double> pv : prods) {
			if (pv.getValue() <= benchMark * (1 + ResolverSettings.cutOffSimilarityRatio)) {
				res.add(pv.getKey());
			}
		}
		return res;
	}

	/**
	 * Determine the similarity of the material wrt the various descriptions within
	 * a productCard
	 * 
	 * @param refWord reference word to search for within the product card
	 * @param card    NmdProductCard object with > 0 profileSets
	 * @return the minimum levensthein distance of the reference word wrt any of the
	 *         profileSet names
	 */
	private Double getProductSimilarityScore(Set<String> refWords, NmdProductCard card) {
		// get all words in the profileSet names and card description and clean them
		Set<String> map = card.getProfileSets().stream().map(ps -> ps.getName()).collect(Collectors.toSet());
		map.add(card.getDescription());
		String totalDescription = String.join(" ", map);
		Set<String> keyWords = NmdDataResolverImpl.parseStringForWords(totalDescription);

		return calculateSimilarityScore(refWords, keyWords.stream().collect(Collectors.toList()));
	}

	/**
	 * Determine the similarity based on a a list of materials descriptions and a
	 * list of product card descriptions
	 * 
	 * @param materialDescriptors list of words found in the material
	 * @param productCardKeyWords list of words found in the product card
	 * @return a score to indicate word similarity. lwoer scores indicate a larger
	 *         similarity
	 */
	@SuppressWarnings("deprecation")
	private Double calculateSimilarityScore(Set<String> materialDescriptors, List<String> productCardKeyWords) {

		BiFunction<String, String, Double> score = (ref, check) -> {
			return (double) StringUtils.getLevenshteinDistance((CharSequence) ref, check);
		};

		Double sum = 0.0;
		for (String ref : materialDescriptors) {
			productCardKeyWords.sort((w1, w2) -> Double.compare(score.apply(ref, w1), score.apply(ref, w2)));

			sum += score.apply(ref, productCardKeyWords.get(0));
		}

		// penalize on word count difference
		sum += ResolverSettings.descriptionLengthPenaltyCoefficient
				* Math.abs(productCardKeyWords.size() - materialDescriptors.size());
		return sum;
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
						// cannot scale a wall (in m2) on more than 1 dimension
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
	 * go through all objects and try to find an appropriate element that matches
	 * the NLsfb code. If no code can be found try resolving the NLsfb code by
	 * looking at decomposing elements and/or a list of alternative IfcProduct to
	 * NLsfb mappings.
	 */
	public void resolveNlsfbCodes() {

		Map<String, List<String>> map = getMappingService().getNlsfbMappings();

		this.getStore().getElements().forEach(el -> {

			// find NLsfb codes for child objects that have no code themselves.
			MpgObject o = el.getMpgObject();
			List<String> foundMap = map.getOrDefault(o.getObjectType(), null);

			boolean hasParent = (o.getParentId() != null) && !o.getParentId().isEmpty();
			MpgObject p = new MpgObjectImpl();
			if (hasParent) {
				try {
					p = this.getStore().getObjectByGuid(o.getParentId()).get();
				} catch (Exception e) {
					System.out.println("encountered GUID that should have a parent, but is not mapped correctly : "
							+ o.getParentId());
				}
				if (!o.hasNlsfbCode()) {

					if (p.hasNlsfbCode()) {
						o.setNLsfbCode(p.getNLsfbCode().print());
						o.addTag(MpgInfoTagType.nlsfbCodeFromResolvedType, "Resolved from: " + p.getGlobalId());
					}
				}

				// Now add all aternatives to fall back on should the first mapping give no
				// results
				if (foundMap == null) {
					foundMap = map.getOrDefault(p.getObjectType(), null);
				}
			}

			if (foundMap != null && foundMap.size() > 0) {
				o.addNlsfbAlternatives(new HashSet<String>(foundMap));
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
			return e.getNLsfbCode() == null ? null : e.getNLsfbCode().print();
		};
		Function<MpgObject, String> getProdTypeProp = (MpgObject e) -> {
			return e.getObjectType();
		};

		// TODO: make this more abstract to run on generic property
		this.getStore().getObjects().stream().filter(o -> !o.getGeometry().getIsComplete()).forEach(o -> {
			MpgGeometry geom = null;
			String NLsfbKey = o.getNLsfbCode() != null ? o.getNLsfbCode().print() : "";

			if (foundGeometries.containsKey(NLsfbKey)) {
				geom = foundGeometries.get(NLsfbKey);
			} else {
				geom = findGeometryForMpgObjectProperty(getNLsfbProp, NLsfbKey);
				foundGeometries.put(NLsfbKey, geom);
			}
			if (geom != null) {
				o.getGeometry().setDimensionsByVolumeRatio(geom);
				o.getGeometry().setIsComplete(true);
				o.addTag(MpgInfoTagType.geometryFromResolvedType, "Dimensions resolved by NLsfb match");
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
					o.addTag(MpgInfoTagType.geometryFromResolvedType, "Dimensions resolved by IfcProduct type match");
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
}
