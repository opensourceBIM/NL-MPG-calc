package org.opensourcebim.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.opensourcebim.ifccollection.MaterialSource;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgGeometry;
import org.opensourcebim.ifccollection.MpgInfoTagType;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.ifccollection.MpgScalingOrientation;
import org.opensourcebim.ifccollection.TotalMaterialSource;
import org.opensourcebim.nmd.scaling.NmdScalingUnitConverter;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import nl.tno.bim.mapping.domain.Mapping;
import nl.tno.bim.mapping.domain.MappingSet;
import nl.tno.bim.mapping.domain.MappingSetMap;
import nl.tno.bim.mapping.domain.MaterialMapping;
import nl.tno.bim.nmd.domain.NlsfbCode;
import nl.tno.bim.nmd.domain.NmdElement;
import nl.tno.bim.nmd.domain.NmdProductCard;
import nl.tno.bim.nmd.domain.NmdProfileSet;
import nl.tno.bim.nmd.scaling.NmdScaler;
import nl.tno.bim.nmd.services.NmdDataService;

/**
 * This implementation allows one of the services to be an editable data service
 * that also allows the user to add new data to be reused in other choices.
 * 
 * @author vijj
 *
 */
public class NmdDataResolverImpl implements NmdDataResolver {

	private NmdDataService service;
	private MappingDataService mappingService;
	private MpgObjectStore store;
	private Set<String> keyWords;

	protected static Logger log = LoggerFactory.getLogger(NmdDataResolverImpl.class);

	public NmdDataResolverImpl() {
	}

	public MpgObjectStore getStore() {
		return store;
	}

	public void setStore(MpgObjectStore store) {
		this.store = store;
	}

	public MappingDataService getMappingService() {
		return mappingService;
	}

	@Override
	public void setMappingService(MappingDataService mappingService) {
		this.mappingService = mappingService;
		if (mappingService != null) {
			log.info("Mappingservice set. Loading keyword mappings");
			keyWords = this.mappingService.getKeyWordMappings(ResolverSettings.keyWordOccurenceMininum).keySet();
			if (keyWords != null) {
				log.info("keywords loaded.");
			}
		}
	}

	@Override
	public void setNmdService(NmdDataService nmdDataService) {
		this.service = nmdDataService;
		log.info("NMD service set");
	}

	public NmdDataService getService() {
		return this.service;
	}

	/**
	 * Start the various subscribed services and try get the most viable
	 * productcards for every MpgObject found
	 */
	@Override
	public void nmdToMpg() {

		try {
			if (this.getStore() == null) {
				log.warn("No Object Store set for retrieving NMD data");
				return;
			}

			// do several pre-processing step imputating properties using the mapping data.
			this.tryFindAdditionalInfo();
			this.resolveAlternativeNlsfbCodes();
			this.resolveUnknownGeometries();

			// start nmd service
			getService().login();
			getService().preLoadData();

			// first check if there are already mappings available for this dataset
			MappingSet set = this.tryGetMappingSet();
			if (set == null) {
				set = new MappingSet();
				set.setProjectId(store.getProjectId());
				set.setDate(new Date());
			}

			// first try to get mappings for the children. (mapping them all covers the
			// parent by default)
			Map<String, List<MpgElement>> childGroups = store.getChildElementGroups();
			boolean addedNewMapping = false;
			for (List<MpgElement> elGroup : childGroups.values()) {
				if (this.findMappingForElementGroup(elGroup, set))
					addedNewMapping = true;
			}

			Map<String, List<MpgElement>> parentGroups = store.getParentElementGroups();
			for (List<MpgElement> elGroup : parentGroups.values()) {
				if (this.findMappingForElementGroup(elGroup, set))
					addedNewMapping = true;
			}

			// finally: post all the new mappings to the object store.
			if (addedNewMapping) {
				getMappingService().postMappingSet(set);
			}
		} catch (Exception e) {
			log.error("Error encountered during mapping process :" + e.getMessage());
		} finally {
			getService().logout();
		}
	}

	/**
	 * For each set of similar elements find a uniqe mapping if not already present
	 * and apply the most recent on on the rest of the group
	 * 
	 * @param elGroup a list of elements that may or may not have mappings.
	 * @param set The MappingSet to check against
	 * @return a flag indicating whether a product in the group has an updated mapping
	 */
	private boolean findMappingForElementGroup(List<MpgElement> elGroup, MappingSet set) {
		Boolean newMapping = false;
		Mapping map = null;
		List<MpgElement> toBeMappedElements = null;
		List<String> elGuids = elGroup.stream().map(el -> el.getMpgObject().getGlobalId()).collect(Collectors.toList());

		// apply the most recent mappingsetmap found in the mapping per group
		MappingSetMap msm = this.getLatestMappingSetMapForElementGuids(elGuids, set);

		if (msm != null) {
			// found a mappingsetmap. apply it to the rest
			toBeMappedElements = elGroup;
		} else {
			// did not find a mappingsetmap. try find a suitable product
			List<MpgElement> elementsRequireMapping = elGroup.stream().filter(el -> !el.hasMapping())
					.collect(Collectors.toList());
			if (elementsRequireMapping.size() > 0) {
				MpgElement element = elGroup.get(0);
				String guid = element.getMpgObject().getGlobalId();

				// found no preexisting mappings. find a fitting product card and apply on the
				// rest.
				resolveNmdMappingForElement(element);
				if (element.hasMapping()) {
					newMapping = true;
					map = createMappingFromMappedElement(element, "");
					map = mappingService.postMapping(map).getObject();
					set.addMappingToMappingSet(map, guid);

					toBeMappedElements = elGroup.subList(1, elGroup.size());
				}
			}
		}

		// apply the found or created mapping on the not yet mapped elements
		if (toBeMappedElements != null) {
			msm = this.getLatestMappingSetMapForElementGuids(elGuids, set);
			map = msm.getMapping();
			for (MpgElement el : toBeMappedElements) {
				if (!el.getMappingMethod().isIndirectMapping()) {
					setNmdProductCardForElement(msm, el);

					// also check if the mapping for this element requires an update
					String elGuid = el.getMpgObject().getGlobalId();
					Optional<MappingSetMap> elMsm = set.getMappingSetMaps().stream()
							.filter(elmsm -> elmsm.getElementGuid().equals(elGuid)).findFirst();
					if (!elMsm.isPresent() || (msm.getMappingRevisionId() != null
							&& elMsm.get().getMappingRevisionId() < msm.getMappingRevisionId())) {
						set.addMappingToMappingSet(map, elGuid);
						newMapping = true;
					}
				}
			}
		}

		return newMapping;
	}

	/**
	 * Return the MappingSetMap that has the largest revision id based on list of
	 * element guids and a mappingset
	 * 
	 * @param elGuids Element guids that are eligible for evaluation
	 * @param set     Mappngset to retrieve the MappingSetMap from
	 * @return the MappingSetMpa with lthe largest revision id in the MappingSet
	 *         belogning to one of the element guids
	 */
	private MappingSetMap getLatestMappingSetMapForElementGuids(@Nonnull List<String> elGuids,
			@Nonnull MappingSet set) {
		if (set.getMappingSetMaps() != null) {
			List<MappingSetMap> latestMsm = set.getMappingSetMaps().stream()
					.filter(msm -> elGuids.contains(msm.getElementGuid())).collect(Collectors.toList());

			// get the elementguid of the MappingSetMap with the latest revision and return
			// the related MpgElement
			if (latestMsm.size() > 0) {
				return Collections.max(latestMsm, Comparator
						.comparing(msm -> msm.getMappingRevisionId() == null ? 0 : msm.getMappingRevisionId()));
			}
		}
		return null;
	}

	/**
	 * Check whether there is already a mappingset available for the given
	 * project/revision combination and apply any earlier stored mappings based on
	 * the ifc GUID matches.
	 */
	private MappingSet tryGetMappingSet() {
		ResponseWrapper<MappingSet> respSet = null;
		try {
			respSet = getMappingService().getMappingSetByProjectId(store.getProjectId());
			if (respSet.succes() && respSet.getObject().getMappingSetMaps() != null) {
				return respSet.getObject();
			}
		} catch (Exception e) {
			log.error("Map service error: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Based on the Mapping and the element we check if there is a nmd product card
	 * available.
	 * 
	 * @param nmdMap Mapping object that contains NMDproductcard ids (totaal and/or
	 *               per material) and a guid reference to the input element
	 * @param el     MpgElement that does not yet have a product card
	 * @return a flag to indicate whether setting the mapping was succesful
	 */
	private boolean setNmdProductCardForElement(MappingSetMap nmdMap, MpgElement el) {
		Mapping mapping = nmdMap.getMapping();
		List<Long> ids = mapping.getAllNmdProductIds();
		Boolean isOriginalMapping = nmdMap.getMappingRevisionId() == null || nmdMap.getMappingRevisionId() == 0;
		if (ids.size() > 0) {
			List<NmdProductCard> cards = this.getService().getProductCardsByIds(ids);
			return this.setNmdProductCardForElement(mapping, cards, isOriginalMapping, el);
		}
		return false;
	}

	private boolean setNmdProductCardForElement(Mapping mapping, List<NmdProductCard> cards, Boolean isOriginalMapping,
			MpgElement el) {

		Function<Long, Optional<NmdProductCard>> getCard = (id) -> {
			return cards.parallelStream().filter(c -> (long) c.getProductId() == id).findFirst();
		};

		if (cards != null) {
			// first check if a totaal product needs to be mapped
			Long totId = mapping.getNmdTotaalProductId();
			if (totId != null && totId > 0) {
				Optional<NmdProductCard> foundCard = getCard.apply(totId);
				if (foundCard.isPresent()) {
					el.mapProductCard(new TotalMaterialSource("mapService"), foundCard.get());
					el.setMappingMethod(
							isOriginalMapping ? NmdMappingType.DirectTotaalProduct : NmdMappingType.UserMapping);
				}
			}

			// next check for the material mappings and apply these
			if (mapping.getMaterialMappings() != null) {
				mapping.getMaterialMappings().forEach(mMap -> {
					el.getMpgObject().getListedMaterials().forEach(mat -> {

						if (mat.getName().toLowerCase().trim().equals(mMap.getMaterialName().toLowerCase().trim())) {
							Optional<NmdProductCard> foundCard = getCard.apply(mMap.getNmdProductId());
							if (foundCard.isPresent()) {
								el.mapProductCard(mat, foundCard.get());
								el.setMappingMethod(isOriginalMapping ? NmdMappingType.DirectDeelProduct
										: NmdMappingType.UserMapping);
							}
						}
					});
				});
			}
		}
		return el.getProductIds().size() > 0;
	}

	/**
	 * based on an input mapped mpgElement this method creates a Mapping object to
	 * be send to the mapService
	 * 
	 * @param el
	 * @return
	 */
	private static Mapping createMappingFromMappedElement(MpgElement el, String nlsfb) {
		Mapping map = new Mapping();

		nlsfb = nlsfb == null || nlsfb.isBlank()
				? el.getMpgObject().getNLsfbCode() == null ? "" : el.getMpgObject().getNLsfbCode().print()
				: nlsfb;
		map.setNlsfbCode(nlsfb);
		map.setOwnIfcType(el.getMpgObject().getObjectType());
		if (!el.getMpgObject().getParentId().isEmpty()) {
			map.setQueryIfcType("different");
		} else {
			map.setQueryIfcType(el.getMpgObject().getObjectType());
		}

		Long totalId = (long) (el.getTotalMap() != null ? el.getTotalMap().getMapId() : -1);
		if (totalId > 0) {
			map.setNmdTotaalProductId(totalId);
		} else {
			List<MaterialMapping> matMaps = new ArrayList<>();
			for (MaterialSource mat : el.getMpgObject().getListedMaterials()) {
				MaterialMapping matMap = new MaterialMapping();
				matMap.setMaterialName(mat.getName());
				matMap.setNmdProductId((long) mat.getMapId());
				matMaps.add(matMap);
			}
			map.setMaterialMappings(matMaps);
		}
		return map;
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

		// try get material keywords from the object description
		store.getElements().stream().filter(el -> el.getMpgObject().getListedMaterials().isEmpty()).forEach(el -> {
			Set<String> foundMaterials = this.tryGetKeyMaterials(el.getMpgObject().getObjectName());

			// add the found materials as aa single material item only if there are no
			// materials already defined.
			if (!foundMaterials.isEmpty()) {
				el.getMpgObject().addMaterialSource(
						new MaterialSource("-1", String.join(" ", foundMaterials), "from description"));
			}
		});
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
	 * @return a set with any words that are deemed a keyword.
	 */
	public Set<String> tryGetKeyMaterials(String objectName) {
		Set<String> objectDescription = NmdDataResolverImpl.parseStringForWords(objectName);
		Set<String> res = new HashSet<String>();
		if (!objectDescription.isEmpty()) {
			for (String word : objectDescription) {
				for (String key : keyWords) {
					if (key != null && word != null && word.contains(key)) {
						res.add(word);
					}
				}
			}
		}
		return res;
	}

	/**
	 * Clean up a description based on predefined delimiters and clean up strange
	 * characters
	 * 
	 * @param description
	 * @return a set of separate unique words.
	 */
	private static Set<String> parseStringForWords(String description) {
		return Arrays.asList(description.split(ResolverSettings.splitChars)).stream()
				.map(w -> w.replaceAll(ResolverSettings.numericReplacePattern, "").toLowerCase().trim())
				.filter(w -> w.length() >= ResolverSettings.minWordLengthForSimilarityCheck)
				.collect(Collectors.toSet());
	}

	private void resolveNmdMappingForElement(@NotNull MpgElement mpgElement) {
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
		// clear earlier warnings
		mpgElement.getMpgObject().clearTagsOfType(MpgInfoTagType.nmdProductCardWarning);

		// STEP 1: check if there are relevant nlsfbcodes present in the input set
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
		Boolean mapFound = getProductCardsForElement(mpgElement, candidateElements);
		if (mapFound && mpgElement.getTotalMap() == null) {
			mpgElement.setMappingMethod(NmdMappingType.DirectDeelProduct);
		} else if (mpgElement.getTotalMap() != null) {
			mpgElement.setMappingMethod(NmdMappingType.DirectTotaalProduct);
		} else {
			mpgElement.setMappingMethod(NmdMappingType.None);
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

		List<NmdElement> filteredCandidates = candidates.stream().filter(ce -> (ce.getProducts().size() > 0))
				.collect(Collectors.toList());

		return filteredCandidates;
	}

	/**
	 * Find out wich candidate product should be mapped to the mpgElement.
	 * 
	 * @param mpgElement mpgElement to add product cards to
	 * @param candidates possible nmProductCard matches for the mpgElement
	 */
	private Boolean getProductCardsForElement(MpgElement mpgElement, List<NmdElement> candidates) {

		List<NmdProductCard> allProducts = candidates.stream().flatMap(e -> e.getProducts().stream())
				.collect(Collectors.toList());
		List<MaterialSource> mats = mpgElement.getMpgObject().getListedMaterials();
		Set<NmdProductCard> viableCandidates = new HashSet<>();

		// Find per material the most likely candidates that fall within the
		// specifications
		allProducts.forEach(p -> getService().getAdditionalProfileDataForCard(p));

		// 3 options. map for each layer separately, map on all mats in one go or map per material.
		if (mpgElement.getMpgObject().getLayers().size() > 0) {
			// map on each material individually
			for (MaterialSource mat : mats) {
				this.findViableCandidateForElement(mat.getName(), mpgElement, allProducts, viableCandidates,
						mat);
			}

		} else if (mpgElement.getMpgObject().getListedMaterials().size() >= 1) {
			// try map on the full material description
			String description = mpgElement.getMpgObject().getListedMaterials().stream().map(MaterialSource::getName)
					.collect(Collectors.joining(" "));
			this.findViableCandidateForElement(description, mpgElement, allProducts, viableCandidates,
					new TotalMaterialSource("resolver"));
		}

		return mpgElement.getTotalMap() != null || 
				mpgElement.getMpgObject().getListedMaterials().stream().anyMatch(m -> m.getMapId() > 0);
	}

	private void findViableCandidateForElement(String description, MpgElement mpgElement, List<NmdProductCard> allProducts,
			Set<NmdProductCard> viableCandidates,
			MaterialSource mat) {

		// currently: select most favorable card
		Function<List<NmdProductCard>, NmdProductCard> selectCard = (list) -> {
			list.sort((pc1, pc2) -> pc2.getProfileSetsCoeficientSum().compareTo(pc1.getProfileSetsCoeficientSum()));
			return list.get(0);
		};
		
		
		List<NmdProductCard> productOptions = selectProductsBasedOnStringSimilarity(description, allProducts);

		List<NmdProductCard> removeCards = new ArrayList<>();
		for (NmdProductCard card : productOptions) {
			// per found element we should try to select a fitting productCard
			int dims = NmdScalingUnitConverter.getUnitDimension(card.getUnit());
			if (!card.getProfileSets().isEmpty()) {
				// determine scaling dimension
				MpgScalingOrientation orientation = mpgElement.getMpgObject().getGeometry().getScalerOrientation(dims);

				// check if the productcard does not constrain the element in its physical
				// dimensions.
				if (!canProductBeUsedForElement(card, orientation)) {
					removeCards.add(card);
				}
			}
		}
		productOptions.removeAll(removeCards);

		// check if a decent enough filter has been made. if not tag that there are too
		// many options.
		// ToDo: make warning settings variable
		if (allProducts.size() * ResolverSettings.tooManyOptionsRatio <= productOptions.size()
				|| productOptions.size() > ResolverSettings.tooManyOptionsAbsNum) {
			mpgElement.getMpgObject().addTag(MpgInfoTagType.mappingWarning,
					"large uncertainty for mapping material(s): " + description);
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

	/**
	 * Determine the top list of products based on a string similarity score
	 * 
	 * @param description description to match product on.
	 * @param allProducts preselected list of cadidate productcards
	 * @return selection of productcards based on similarity score and cutoff
	 *         criteria
	 */
	private List<NmdProductCard> selectProductsBasedOnStringSimilarity(String description,
			List<NmdProductCard> allProducts) {
		List<Pair<NmdProductCard, Double>> prods = new ArrayList<Pair<NmdProductCard, Double>>();
		List<NmdProductCard> res = new ArrayList<NmdProductCard>();

		Set<String> materialDescription = parseStringForWords(description);

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
	private Double calculateSimilarityScore(Set<String> materialDescriptors, List<String> productCardKeyWords) {
		LevenshteinDistance dist = new LevenshteinDistance();
		BiFunction<String, String, Double> score = (ref, check) -> {
			return (double) dist.apply((CharSequence) ref, check);
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
	 * @param prod       the candidate NmdProductCard
	 * @param mpgElement mpgElement that we want to add the productCard to
	 * @return a boolean to indicate whether the ProductCard is a viable option
	 */
	private boolean canProductBeUsedForElement(NmdProductCard prod, MpgScalingOrientation orientation) {
		int numDims = NmdScalingUnitConverter.getUnitDimension(prod.getUnit());
		if (prod.requiresScaling()) {

			for (NmdProfileSet profielSet : prod.getProfileSets()) {

				// if there is no scaler defined, but the item is marked as scalable return true
				// by default.
				if (profielSet.getIsScalable()) {
					NmdScaler scaler = prod.getScalerForProfileSet(profielSet.getProfielId());
					if (numDims < 3 && scaler != null) {
						String unit = scaler.getUnit();

						Double[] dims = orientation.getScaleDims();
						if (scaler.getNumberOfDimensions() > dims.length) {
							// cannot scale a wall (in m2) on more than 1 dimension
							return false;
						}

						Double convFactor = NmdScalingUnitConverter.getScalingUnitConversionFactor(unit,
								this.getStore());
						if (!scaler.areDimsWithinBounds(dims, convFactor)) {
							return false;
						}
					}
				} else {
					return false;
				}
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
	public void resolveAlternativeNlsfbCodes() {

		Map<String, List<String>> map = getMappingService().getNlsfbMappings();

		this.getStore().getElements().forEach(el -> {

			// find NLsfb codes for child objects that have no code themselves.
			MpgObject o = el.getMpgObject();
			List<String> foundMap = map.getOrDefault(o.getObjectType(), null);

			if (foundMap != null && foundMap.size() > 0) {
				o.addNlsfbAlternatives(new HashSet<String>(foundMap));
			} else {
				// get possible parent and retrieve nlsfb and nlsfb alternatives from there
				Optional<MpgObject> parentObj = this.getStore().getObjectByGuid(o.getParentId());
				if (parentObj.isPresent()) {
					MpgObject p = parentObj.get();
					foundMap = map.getOrDefault(p.getObjectType(), null);
					if (foundMap != null && foundMap.size() > 0) {
						o.addNlsfbAlternatives(new HashSet<String>(foundMap));
					}
					if (p.hasNlsfbCode() && !o.hasNlsfbCode()) {
						o.setNLsfbCode(p.getNLsfbCode().print());
						o.addTag(MpgInfoTagType.nlsfbCodeFromResolvedType, "Resolved from: " + p.getGlobalId());
					}
				}
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
