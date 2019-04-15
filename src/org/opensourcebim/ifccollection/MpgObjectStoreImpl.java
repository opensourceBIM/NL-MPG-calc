package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bimserver.utils.AreaUnit;
import org.bimserver.utils.LengthUnit;
import org.bimserver.utils.VolumeUnit;
import org.eclipse.emf.common.util.BasicEList;
import org.opensourcebim.ifcanalysis.GuidCollection;
import org.opensourcebim.mapping.NmdMappingType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import nl.tno.bim.nmd.domain.NmdProductCard;

/**
 * Storage container for collected ifc objects. Only data relevant for Mpg
 * calculations should be stored here
 * 
 * @author vijj
 *
 */
public class MpgObjectStoreImpl implements MpgObjectStore {

	private HashSet<MpgElement> mpgElements;

	@JsonIgnore
	private List<MpgObject> mpgObjects;
	
	private HashMap<Integer, NmdProductCard> productCards;

	private List<MpgSpace> spaces;
	
	private Long projectId;
	private Long revisionId;

	/**
	 * list to store the guids with any MpgObjects that the object linked to that
	 * guid decomposes
	 */
	@JsonIgnore
	private List<ImmutablePair<String, MpgObject>> decomposedRelations;

	private VolumeUnit volumeUnit;
	private AreaUnit areaUnit;
	private LengthUnit lengthUnit;

	public MpgObjectStoreImpl() {
		productCards = new HashMap<Integer, NmdProductCard>();
		setElements(new HashSet<>());
		setObjects(new BasicEList<MpgObject>());
		setSpaces(new BasicEList<MpgSpace>());
		setUnits(VolumeUnit.CUBIC_METER, AreaUnit.SQUARED_METER, LengthUnit.METER);
		decomposedRelations = new ArrayList<ImmutablePair<String,MpgObject>>();
	}

	public void reset() {
		decomposedRelations.clear();
		mpgObjects.clear();
		mpgElements.clear();
		spaces.clear();
	}

	@Override
	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	@Override
	public Long getRevisionId() {
		return revisionId;
	}

	public void setRevisionId(Long revisionId) {
		this.revisionId = revisionId;
	}
	
	@Override
	public VolumeUnit getVolumeUnit() {
		return this.volumeUnit;
	}

	@Override
	public AreaUnit getAreaUnit() {
		return this.areaUnit;
	}

	@Override
	public LengthUnit getLengthUnit() {
		return this.lengthUnit;
	}

	/**
	 * Set the units used when storing the mpgObjects
	 * 
	 * @param volumeUnit volume unit fo ifcProduct
	 * @param areaUnit   area unit of ifcProducts
	 * @param lengthUnit length unit of ifcProducts
	 */
	public void setUnits(VolumeUnit volumeUnit, AreaUnit areaUnit, LengthUnit lengthUnit) {
		this.volumeUnit = volumeUnit;
		this.areaUnit = areaUnit;
		this.lengthUnit = lengthUnit;
	}

	@Override
	public MpgElement addElement(String name) {
		MpgElement el = null;
		if (name != null && !name.isEmpty()) {
			el = new MpgElement(name, this);
			mpgElements.add(el);
		}
		return el;
	}

	@Override
	public HashSet<MpgElement> getElements() {
		return mpgElements;
	}

	private void setElements(HashSet<MpgElement> mpgElements) {
		this.mpgElements = mpgElements;
	}

	@Override
	public void setObjectForElement(String name, MpgObject mpgObject) {
		MpgElement el = getElementByName(name);
		if (el != null) {
			el.setMpgObject(mpgObject);
		}
	}

	@Override
	public void addObject(MpgObject mpgObject) {
		this.getObjects().add(mpgObject);
	}
	
	@Override
	public List<MpgObject> getObjects() {
		return mpgObjects;
	}

	private void setObjects(List<MpgObject> mpgObjects) {
		this.mpgObjects = mpgObjects;
	}

	@Override
	public List<MpgSpace> getSpaces() {
		return spaces;
	}

	private void setSpaces(List<MpgSpace> spaces) {
		this.spaces = spaces;
	}
	
	@Override
	public void addProductCard(NmdProductCard card) {
		this.productCards.putIfAbsent(card.getProductId(), card);
	}
	
	public void removeProductCard(Integer id) {
		this.productCards.remove(id);
	}
	
	@Override
	public NmdProductCard getProductCard(Integer id) {
		return this.productCards.getOrDefault(id, null);
	}
	
	@Override
	public Map<Integer, NmdProductCard> getProductCards() {
		return this.productCards;
	}
	
	@Override
	public List<NmdProductCard> getProductCards(Collection<Integer> ids) {
		return this.productCards.entrySet().stream()
				.filter(es -> ids.contains(es.getKey()))
				.map(es -> es.getValue())
				.collect(Collectors.toList());
	}

	private List<MpgObject> getObjectsByProductType(String productType) {
		return mpgObjects.stream().filter(g -> g.getObjectType().equals(productType)).collect(Collectors.toList());
	}

	private List<MpgSpace> getObjectsByMaterialName(String materialName) {
		return mpgObjects.stream().flatMap(g -> g.getLayers().stream()).filter(o -> o.getMaterialName() != null)
				.filter(o -> o.getMaterialName().equals(materialName)).collect(Collectors.toList());
	}

	@Override
	public Optional<MpgObject> getObjectByGuid(String guidId) {
		return mpgObjects.stream().filter(o -> guidId.equals(o.getGlobalId())).findFirst();
	}

	@Override
	public List<MpgObject> getObjectsByGuids(HashSet<String> guidIds) {
		return mpgObjects.stream().filter(o -> guidIds.contains(o.getGlobalId())).collect(Collectors.toList());
	}

	@Override
	public MpgElement getElementByName(String name) {
		Optional<MpgElement> element = getElements().stream().filter(e -> e.getIfcName().equalsIgnoreCase(name))
				.findFirst();
		return element.isPresent() ? element.get() : null;
	}

	@Override
	public List<MpgElement> getElementsByProductType(String productType) {
		List<MpgObject> objectsByProductType = this.getObjectsByProductType(productType);
		List<String> materialNames = objectsByProductType.stream()
				.flatMap(o -> o.getMaterialNamesBySource(null).stream()).distinct().collect(Collectors.toList());

		return materialNames.stream().map(mat -> this.getElementByName(mat)).collect(Collectors.toList());
	}

	@Override
	public MpgElement getElementByObjectGuid(String guid) {
		Optional<MpgElement> el = this.getElements().stream().filter(e -> e.getMpgObject().getGlobalId().equals(guid))
				.findFirst();
		if (el.isPresent()) {
			return el.get();
		} else {
			return null;
		}
	}
	
	/**
	 * Group the elements by an equalByValues
	 */
	@JsonIgnore
	@Override
	public Map<String, List<MpgElement>> getElementGroups() {
		return this.mpgElements.stream().collect(Collectors.groupingBy(el -> {
			return el.getValueHash();
		}));
	}

	@Override
	public double getTotalVolumeOfMaterial(String name) {
		return getObjectsByMaterialName(name).stream()
				.collect(Collectors.summingDouble(o -> o == null ? 0.0 : o.getVolume()));
	}

	@Override
	public void addSpace(MpgSpace space) {
		spaces.add(space);
	}

	@Override
	public double getTotalFloorArea() {
		return spaces.stream().map(s -> s.getArea()).collect(Collectors.summingDouble(a -> a == null ? 0.0 : a));
	}

	/**
	 * Create links from and to Decomposing and Decomposed products based on a guid
	 * map
	 * 
	 * @param isDecomposedByrelationMap hashMap containing the guids of decomposed
	 *                                  and decomposing objects.
	 */
	public void reloadParentChildRelationShips(Map<String, String> isDecomposedByrelationMap) {

		this.getObjects().forEach(o -> {
			if (isDecomposedByrelationMap.containsKey(o.getGlobalId())) {
				o.setParentId(isDecomposedByrelationMap.get(o.getGlobalId()));
			}
		});

		// create a list with parent ids linked to child objects
		this.decomposedRelations = this.getObjects().stream().filter(o -> !StringUtils.isBlank(o.getParentId()))
				.map(o -> new ImmutablePair<String, MpgObject>(o.getParentId(), o)).collect(Collectors.toList());
	}

	@Override
	public Stream<MpgObject> getChildren(String parentGuid) {
		if (this.decomposedRelations != null) {
			return this.decomposedRelations.parallelStream().filter(p -> p.getLeft() == parentGuid)
					.map(p -> p.getRight());
		} else {
			return (new ArrayList<MpgObject>()).stream();
		}
	}

	/**
	 * Check if all the MpgElements have matched Nmd ProductCards linked
	 */
	@Override
	public boolean isElementDataComplete() {
		return this.mpgElements.stream().allMatch(e -> {
			return e.getNmdProductCards().size() > 0 && e.getMpgObject() != null && e.getIsFullyCovered();
		});
	}

	@Override
	public boolean isIfcDataComplete() {
		return getGuidsWithoutMaterial().getSize() == 0 && getGuidsWithoutVolume().getSize() == 0
				&& getGuidsWithRedundantMaterials().getSize() == 0 && getGuidsWithUndefinedLayerMats().getSize() == 0;
	}

	/**
	 * set the mapping of an element if the parent or child relation ship mapping
	 * has changed
	 * 
	 * @param globalId guid of the mpgobject to start from
	 * @param flag     flag to indicate whether the map was set (true) or unset
	 *                 (false)
	 */
	@Override
	public void toggleMappingDependencies(String globalId, boolean flag) {
		List<MpgElement> children = this.allChildElementsByGuid(globalId);
		List<MpgElement> parents = this.allParentElementsByGuid(globalId);

		if (flag) {
			// override all the children mappings
			children.forEach(el -> {
				el.setMappingMethod(NmdMappingType.IndirectThroughParent);
				el.removeProductCards();
			});
			
			parents.forEach(el -> {
				if (el.getMappingMethod() == NmdMappingType.None && allChildrenAreMapped(el)) {
					el.setMappingMethod(NmdMappingType.IndirectThroughChildren);
				}
			});
		} else {
			// revert all hierarchical child mappings 
			// (as there can only be a single parent with a direct mapping)
			children.forEach(el -> {
				if (el.getMappingMethod() == NmdMappingType.IndirectThroughParent) {
					el.setMappingMethod(NmdMappingType.None);
				}
			});
			// any parents that had a indirect through children mapping will be reverted
			parents.forEach(el -> {
				if (el.getMappingMethod() == NmdMappingType.IndirectThroughChildren) {
					el.setMappingMethod(NmdMappingType.None);
				}
			});
		}

	}

	/**
	 * Recursively check whether all children are mapped.
	 * @param el MpgElement to check hierarchy of
	 * @return a flag to indicate that all chidren have a mapping
	 */
	private boolean allChildrenAreMapped(MpgElement el) {
		List<MpgElement> childrenOfElement = decomposedRelations.stream()
				.filter(kvp -> kvp.getKey().equals(el.getMpgObject().getGlobalId()))
				.map(v -> v.getValue().getGlobalId())
				.map(guid -> this.getElementByObjectGuid(guid)).collect(Collectors.toList());
		
		if (childrenOfElement.size() > 0) {
			return childrenOfElement.stream().allMatch(ce -> ce.hasMapping() || allChildrenAreMapped(ce));
		} else {
			return false;
		}
	}
	
	/**
	 * get a collection of MpgElements that are higher up in the hierarchy than the
	 * input element guid
	 * 
	 * @param globalId guid to start search
	 * @return collection of elements that have the input guid as a (recursive)
	 *         child
	 */
	private List<MpgElement> allParentElementsByGuid(String globalId) {
		List<MpgElement> elements = new ArrayList<MpgElement>();
		Optional<String> parentId = this.decomposedRelations.stream()
				.filter(v -> v.getValue().getGlobalId().equals(globalId)).map(v -> v.getKey()).findFirst();
		if (parentId.isPresent() && !parentId.get().isEmpty()) {
			MpgElement parent = this.getElementByObjectGuid(parentId.get());
			elements.add(parent);
			elements.addAll(allParentElementsByGuid(parent.getMpgObject().getGlobalId()));
		}
		return elements;
	}

	/**
	 * Get a collection of elements that are down in the hierarchy than the input guid
	 * 
	 * @param globalId guid to start from
	 * @return a list of elements that are a (recursive) child of the input guid
	 */
	private List<MpgElement> allChildElementsByGuid(String globalId) {
		List<MpgElement> elements = this.mpgElements.stream()
				.filter(el -> el.getMpgObject().getParentId().equals(globalId)).collect(Collectors.toList());
		List<MpgElement> childEl = new ArrayList<MpgElement>();
		elements.forEach(el -> {
			childEl.addAll(allChildElementsByGuid(el.getMpgObject().getGlobalId()));
		});
		elements.addAll(childEl);
		return elements;
	}

	@Override
	@JsonIgnore
	public Stream<String> getAllMaterialNames() {
		return this.getElements().stream().flatMap(e -> e.getMpgObject().getListedMaterials().stream())
				.map(s -> s.getName()).filter(n -> {
					return (n != null && !n.isEmpty());
				}).distinct();
	}

	@Override
	@JsonIgnore
	public GuidCollection getGuidsWithoutMaterial() {
		GuidCollection coll = new GuidCollection(this, "Object GUIDs that have missing materials");
		coll.setCollection(mpgObjects.stream().filter(o -> hasUndefinedMaterials(o, false)).map(o -> o.getGlobalId())
				.collect(Collectors.toList()));
		return coll;
	}

	@Override
	@JsonIgnore
	public GuidCollection getGuidsWithoutMaterialAndWithoutFullDecomposedMaterials() {

		GuidCollection coll = new GuidCollection(this,
				"Object GUIDs without material and any of the decomposed objects without material");
		coll.setCollection(mpgObjects.stream().filter(o -> hasUndefinedMaterials(o, true)).map(o -> o.getGlobalId())
				.collect(Collectors.toList()));
		return coll;
	}

	@Override
	@JsonIgnore
	public GuidCollection getGuidsWithoutVolume() {
		GuidCollection coll = new GuidCollection(this, "Object GUIDs that have missing volumes");
		coll.setCollection(mpgObjects.stream().filter(o -> hasUndefinedVolume(o, false)).map(o -> o.getGlobalId())
				.collect(Collectors.toList()));
		return coll;
	}

	@Override
	@JsonIgnore
	public GuidCollection getGuidsWithoutVolumeAndWithoutFullDecomposedVolumes() {
		GuidCollection coll = new GuidCollection(this,
				"Object GUIDs without volume and any of the decomposed objects without volume");
		coll.setCollection(mpgObjects.stream().filter(o -> hasUndefinedVolume(o, true)).map(o -> o.getGlobalId())
				.collect(Collectors.toList()));
		return coll;
	}

	@Override
	@JsonIgnore
	public GuidCollection getGuidsWithRedundantMaterials() {
		GuidCollection coll = new GuidCollection(this, "Object GUIDs that cannot be linked to materials 1-on-1");
		coll.setCollection(mpgObjects.stream().filter(o -> hasRedundantMaterials(o, false)).map(o -> o.getGlobalId())
				.collect(Collectors.toList()));
		return coll;
	}

	@Override
	@JsonIgnore
	public GuidCollection getGuidsWithUndefinedLayerMats() {
		GuidCollection coll = new GuidCollection(this, "Object GUIDsthat have undefined layers");
		coll.setCollection(mpgObjects.stream().filter(o -> hasUndefinedLayers(o, false)).map(g -> g.getGlobalId())
				.collect(Collectors.toList()));
		return coll;
	}
	
	@Override
	@JsonIgnore
	public GuidCollection getGuidsWithoutMappings() {
		GuidCollection coll = new GuidCollection(this, "Object GUIDs for objects with incomplete NMD mapping");
		coll.setCollection(this.getElements().stream().filter(el -> !el.getIsFullyCovered()).map(el -> el.getMpgObject().getGlobalId())
				.collect(Collectors.toList()));
		return coll;
	}
	
	/**
	 * Recursive check method to validate whether a material or any of its children
	 * have undefined materials
	 */
	@Override
	public boolean hasUndefinedMaterials(MpgObject obj, boolean includeChildren) {
		long numLayers = obj.getLayers().size();
		boolean objIsUndefined = (numLayers + obj.getMaterialNamesBySource(null).size()) == 0;

		// anyMatch returns false on an empty list, so if children should be included,
		// but no children are present it will still return false
		boolean hasChildren = getChildren(obj.getGlobalId()).count() > 0;
		boolean childrenAreUndefined = includeChildren
				&& getChildren(obj.getGlobalId()).anyMatch(o -> hasUndefinedMaterials(o, includeChildren));

		return objIsUndefined && !hasChildren ? objIsUndefined : childrenAreUndefined;
	}

	@Override
	public boolean hasUndefinedVolume(MpgObject obj, boolean includeChildren) {
		boolean ownCheck = obj.getGeometry() == null || obj.getGeometry().getVolume() == 0;
		boolean hasChildren = getChildren(obj.getGlobalId()).count() > 0;
		boolean childCheck = includeChildren
				&& getChildren(obj.getGlobalId()).anyMatch(o -> hasUndefinedVolume(o, includeChildren));

		return ownCheck && !hasChildren ? true : childCheck;
	}

	@Override
	public boolean hasRedundantMaterials(MpgObject obj, boolean includeChildren) {
		long numLayers = obj.getLayers().size();
		boolean ownCheck = (numLayers == 0) && (obj.getMaterialNamesBySource(null).size() > 1)
				|| obj.hasDuplicateMaterialNames();
		boolean childCheck = includeChildren
				&& getChildren(obj.getGlobalId()).anyMatch(o -> hasRedundantMaterials(o, includeChildren));
		return ownCheck || childCheck;
	}

	@Override
	public boolean hasUndefinedLayers(MpgObject obj, boolean includeChildren) {
		long numLayers = obj.getLayers().size();
		long unresolvedLayers = obj.getLayers().stream()
				.filter(l -> l.getMaterialName() == "" || l.getMaterialName() == null).collect(Collectors.toList())
				.size();

		boolean ownCheck = (numLayers > 0) && (unresolvedLayers > 0);
		boolean childCheck = includeChildren
				&& getChildren(obj.getGlobalId()).anyMatch(o -> hasUndefinedLayers(o, includeChildren));
		return ownCheck || childCheck;
	}	
}
