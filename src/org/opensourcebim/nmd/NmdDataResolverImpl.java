package org.opensourcebim.nmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgGeometry;
import org.opensourcebim.ifccollection.MpgInfoTagType;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectStore;

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

		// try to find the correct scaling dimensions for objects that could not have
		// their
		// geometry resolved - ToDo: check whether this still needs to be done or do we
		// need to resolve
		// this on selecting the NMD mapping?
		this.resolveUnknownGeometries();

		try {
			// start any subscribed services
			for (NmdDataService nmdDataService : services) {
				nmdDataService.login();
				nmdDataService.preLoadData();
			}

			// get data per material
			for (MpgElement element : getStore().getElements()) {
				tryGetNmdDataForElement(element);
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

	private void tryGetNmdDataForElement(MpgElement mpgElement) {
		NmdProductCard retrievedMaterial = null;
		NmdDataService nmdDataService = services.get(0);

		// resolve which product card to retrieve based on the input MpgElement
		if (mpgElement.getMpgObject() == null) {
			return;
		}

		if (mpgElement.getMpgObject().getNLsfbAlternatives().size() == 0) {
			return;
		}

		// find all the product cards that match with any of the mapped NLsfb codes.
		List<NmdProductCard> candidates = nmdDataService.getData().stream()
				.filter(el -> mpgElement.getMpgObject().getNLsfbAlternatives().stream()
						.anyMatch(code -> code == el.getNLsfbCode()))
				.flatMap(el -> el.getProducts().stream()).collect(Collectors.toList());
		if (candidates.size() == 0) {
			mpgElement.getMpgObject().addTag(MpgInfoTagType.nmdProductCardWarning,
					"No NMD product card matching any of the NLsfb codes");
			return;
		}

		// find the most suitable candidate out of the earlier made selection
		for (NmdProductCard card : candidates) {
			// create a copy
			retrievedMaterial = new NmdProductCardImpl(card);

			if (nmdDataService.getAdditionalProfileDataForCard(retrievedMaterial)) {
				// check if the item needs scaling and if yes in which direction and check
				// whether it can be scaled.
				// and find alternatives when this is not the case
				// requires scaling?

				// no: add item

				// yes:
				// add scale dimensions based on product card
				// check dimensions within NMD scaler bounds
				// yes: add item and set matching NLSfb code in product
				// no: go to next cadidate

				mpgElement.addProductCard(retrievedMaterial);
				mpgElement.setMappingMethod(NmdMapping.Direct);

				// suitable element found: exit the loop
				break;
			}
		}
	}

	/**
	 * go through all objets and try to find an appropriate element that matches the
	 * NLsfb code. If no code can be found try resolving the NLsfb code by looking
	 * at decomposing elements and/or a list of alternative IfcProduct to NLsfb
	 * mappings.
	 */
	public void resolveNlsfbCodes() {

		HashMap<String, String[]> map = getProductToNmdMap();
		String[] emptyMap = null;

		this.getStore().getElements().forEach(el -> {

			// first resolve NLsfb codes using decomposing ids if there is no NLsfb code at
			// all.
			MpgObject o = el.getMpgObject();
			if (o.hasNlsfbCode()) {
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
				o.addNlsfbAlternatives(Arrays.asList(foundMap));
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
				o.getGeometry().addScalingTypesFromGeometry(geom);
				o.getGeometry().setIsComplete(true);
				o.addTag(MpgInfoTagType.geometryFromResolvedType, "resolved by NLsfb match");
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
					o.getGeometry().addScalingTypesFromGeometry(geom);
					o.getGeometry().setIsComplete(true);
					o.addTag(MpgInfoTagType.geometryFromResolvedType, "resolved by IfcProduct type match");
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

		Optional<MpgGeometry> geom = candidates.stream().filter(g -> g.getScalerTypes().size() > 1).findFirst();
		if (geom.isPresent()) {
			return geom.get();
		} else {
			return null;
		}
	}

	private HashMap<String, String[]> getProductToNmdMap() {
		HashMap<String, String[]> productMap = new HashMap<String, String[]>();
		productMap.put("Footing", new String[] { "16." });
		productMap.put("Wall", new String[] { "21.", "22." });
		productMap.put("Slab", new String[] { "23." });
		productMap.put("Stair", new String[] { "24." });
		productMap.put("Roof", new String[] { "27." });
		productMap.put("Beam", new String[] { "28." });
		productMap.put("Window", new String[] { "31.2", "32.2" });
		productMap.put("Door", new String[] { "31.3", "32.3" });

		return productMap;
	}
}
