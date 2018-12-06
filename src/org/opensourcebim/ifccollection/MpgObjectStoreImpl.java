package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.emf.common.util.BasicEList;
import org.opensourcebim.ifcanalysis.GuidCollection;
import org.opensourcebim.nmd.NmdProductCard;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MpgObjectStoreImpl implements MpgObjectStore {

	private HashMap<String, MpgMaterial> mpgMaterials;
	
	private List<MpgObject> mpgObjects;
	
	private List<MpgSubObject> spaces;
	
	/**
	 * stores which object GUIDs have no material or no layers and have
	 * neither related objects that have materials
	 */
	private GuidCollection objectGUIDsWithoutMaterial;
	
	/**
	 * stores which object GUIDs have no material or no layers and have
	 * neither related objects that have materials
	 */
	private GuidCollection objectGUIDsWithoutVolume;

	/**
	 * stores which objects have layers that cannot be resolved to a
	 * material
	 */
	private GuidCollection objectGuidsWithUndefinedLayerMats;

	/**
	 * stores materials have a single object but multiple materials
	 */
	private GuidCollection objectGUIDsWithRedundantMats;
	
	/**
	 *  list to store the guids with any MpgObjects that the object linked to hat guid decomposes
	 */
	@JsonIgnore
	private List<ImmutablePair<String, MpgObject>> decomposedRelations;


	public MpgObjectStoreImpl() {
		setMaterials(new HashMap<>());
		setObjects(new BasicEList<MpgObject>());
		setSpaces(new BasicEList<MpgSubObject>());

		setObjectGUIDsWithoutMaterial(new GuidCollection(this, "# of objects that have missing materials"));
		setObjectGUIDsWithoutVolume(new GuidCollection(this, "# of objects that have missing volumes"));
		setObjectGuidsWithUndefinedLayerMats(new GuidCollection(this, "# of objects that have undefined layers"));
		setObjectGUIDsWithRedundantMaterialSpecs(new GuidCollection(this, "# of objects that cannot be linked to materials 1-on-1"));
	}

	public void reset() {
		mpgObjects.clear();
		mpgMaterials.clear();
		spaces.clear();

		getObjectGUIDsWithoutMaterial().reset();
		getObjectGUIDsWithoutVolume().reset();
		getObjectGuidsWithUndefinedLayerMats().reset();
		getObjectGUIDsWithRedundantMaterialSpecs().reset();
	}

	@Override
	public void addMaterial(String name) {
		if (name != null && !name.isEmpty()) {
			mpgMaterials.putIfAbsent(name, new MpgMaterial(name));
		}
	}

	@Override
	public HashMap<String, MpgMaterial> getMaterials() {
		return mpgMaterials;
	}

	private void setMaterials(HashMap<String, MpgMaterial> mpgMaterials) {
		this.mpgMaterials = mpgMaterials;
	}

	@Override
	public void setProductCardForMaterial(String name, NmdProductCard specs) {
		// TODO catch null reference exceptions
		getMaterialByName(name).setMaterialSpecs(specs);
	}

	@Override
	public List<MpgObject> getObjects() {
		return mpgObjects;
	}

	private void setObjects(List<MpgObject> mpgObjects) {
		this.mpgObjects = mpgObjects;
	}

	@Override
	public List<MpgSubObject> getSpaces() {
		return spaces;
	}

	private void setSpaces(List<MpgSubObject> spaces) {
		this.spaces = spaces;
	}

	@Override
	public void addObject(MpgObject mpgObject) {
		this.getObjects().add(mpgObject);
	}

	@Override
	public Set<String> getDistinctProductTypes() {
		return mpgObjects.stream().map(group -> group.getObjectType()).distinct().collect(Collectors.toSet());
	}

	@Override
	public List<MpgObject> getObjectsByProductType(String productType) {
		return mpgObjects.stream().filter(g -> g.getObjectType().equals(productType)).collect(Collectors.toList());
	}

	@Override
	public List<MpgObject> getObjectsByProductName(String productName) {
		return mpgObjects.stream().filter(g -> g.getObjectName() == productName).collect(Collectors.toList());
	}

	@Override
	public List<MpgSubObject> getObjectsByMaterialName(String materialName) {
		return mpgObjects.stream().flatMap(g -> g.getLayers().stream()).filter(o -> o.getMaterialName() != null)
				.filter(o -> o.getMaterialName().equals(materialName)).collect(Collectors.toList());
	}

	@Override
	public Optional<MpgObject> getObjectByGuid(String guidId) {
		return mpgObjects.stream().filter(o -> o.getGlobalId().equals(guidId)).findFirst();
	}

	@Override
	public List<MpgObject> getObjectsByGuids(HashSet<String> guidIds) {
		return mpgObjects.stream().filter(o -> guidIds.contains(o.getGlobalId())).collect(Collectors.toList());
	}

	@JsonIgnore
	@Override
	public Set<String> getAllMaterialNames() {
		return getMaterials().keySet();
	}

	@Override
	public MpgMaterial getMaterialByName(String name) {
		return getMaterials().getOrDefault(name, null);
	}

	@Override
	public List<MpgMaterial> getMaterialsByProductType(String productType) {
		List<MpgObject> objectsByProductType = this.getObjectsByProductType(productType);

		List<String> materialNames = objectsByProductType.stream().flatMap(o -> o.getMaterialNamesBySource(null).stream())
				.distinct().collect(Collectors.toList());

		return materialNames.stream().map(mat -> this.getMaterialByName(mat)).collect(Collectors.toList());
	}

	@Override
	public double getTotalVolumeOfMaterial(String name) {
		return getObjectsByMaterialName(name).stream()
				.collect(Collectors.summingDouble(o -> o == null ? 0.0 : o.getVolume()));
	}

	@Override
	public double getTotalVolumeOfProductType(String productType) {
		return getObjectsByProductType(productType).stream().collect(Collectors.summingDouble(o -> o.getVolume()));
	}

	@Override
	public void addSpace(MpgSubObject space) {
		spaces.add(space);
	}

	@Override
	public double getTotalFloorArea() {
		return spaces.stream().map(s -> s.getArea()).collect(Collectors.summingDouble(a -> a == null ? 0.0 : a));
	}

	/**
	 * Create links from and to Decomposing and Decomposed products based on a guid map
	 * @param isDecomposedByrelationMap hashMap containing the guids of decomposed and decomposing objects.
	 */
	public void recreateParentChildMap(Map<String, String> isDecomposedByrelationMap) {

		this.getObjects().forEach(o -> {
			if (isDecomposedByrelationMap.containsKey(o.getGlobalId())) {
				o.setParentId(isDecomposedByrelationMap.get(o.getGlobalId()));
			}
		});

		// create a list with parent ids linked to child objects
		this.decomposedRelations = this.getObjects().stream()
				.filter(o -> StringUtils.isBlank(o.getParentId()))
				.map(o -> new ImmutablePair<String, MpgObject>(o.getParentId(), o)).collect(Collectors.toList());
	}
		

	/**
	 * Do a general check over all objects to check whether there are floating
	 * materials (not linked to any MpgObject) or if there are MpgObjects that do no
	 * match with any material
	 * 
	 * @return
	 */
	public void validateIfcDataCollection() {
		// check for objects that have not a single material defined or objects without
		// layers and multiple materials
		getObjectGUIDsWithoutMaterial().setCollection(mpgObjects.stream().filter(o -> o.hasUndefinedMaterials(false))
				.map(o -> o.getGlobalId()).collect(Collectors.toList()));

		// some objects might not have a volume defined. check whether any object or any
		// of its children have undefined volumes.
		getObjectGUIDsWithoutVolume().setCollection(mpgObjects.stream().filter(o -> o.hasUndefinedVolume(false))
				.map(o -> o.getGlobalId()).collect(Collectors.toList()));

		// check for materials without layers that have more than a single material
		// added to them and as such cannot be resolved automatically
		getObjectGUIDsWithRedundantMaterialSpecs().setCollection(mpgObjects.stream().filter(o -> o.hasRedundantMaterials(false))
				.map(o -> o.getGlobalId()).collect(Collectors.toList()));

		// check for objects that have more than 0 materials linked, but are still
		// missing 1 to n materials
		getObjectGuidsWithUndefinedLayerMats().setCollection(mpgObjects.stream().filter(o -> o.hasUndefinedLayers(false))
				.map(g -> g.getGlobalId()).collect(Collectors.toList()));
	}
	
	@Override
	public boolean isIfcDataComplete() {
		return getObjectGUIDsWithoutMaterial().getSize() == 0
				&& getObjectGUIDsWithoutVolume().getSize() == 0 && getObjectGUIDsWithRedundantMaterialSpecs().getSize() == 0
				&& getObjectGuidsWithUndefinedLayerMats().getSize() == 0;
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
	 * Check if all the MpgMaterials have matched Nmd ProductCards linked
	 */
	@Override
	public boolean isMaterialDataComplete() {
		return getMaterials().values().stream().allMatch(mat -> mat.getNmdMaterialSpecs() != null);
	}

	@Override
	public GuidCollection getObjectGUIDsWithoutMaterial() {
		return objectGUIDsWithoutMaterial;
	}

	public void setObjectGUIDsWithoutMaterial(GuidCollection coll) {
		this.objectGUIDsWithoutMaterial = coll;
	}

	@Override
	public GuidCollection getObjectGUIDsWithoutVolume() {
		return objectGUIDsWithoutVolume;
	}

	public void setObjectGUIDsWithoutVolume(GuidCollection coll) {
		this.objectGUIDsWithoutVolume = coll;
	}

	@Override
	public GuidCollection getObjectGUIDsWithRedundantMaterialSpecs() {
		return objectGUIDsWithRedundantMats;
	}

	public void setObjectGUIDsWithRedundantMaterialSpecs(GuidCollection coll) {
		this.objectGUIDsWithRedundantMats = coll;
	}

	@Override
	public GuidCollection getObjectGuidsWithUndefinedLayerMats() {
		return objectGuidsWithUndefinedLayerMats;
	}

	public void setObjectGuidsWithUndefinedLayerMats(GuidCollection coll) {
		this.objectGuidsWithUndefinedLayerMats = coll;
	}

	@Override
	public void SummaryReport() {
		System.out.println("----------------------------");
		System.out.println("Summary report for ifc file:");
		System.out.println("Materials found : " + mpgMaterials.size());
		System.out.println(String.join(", ", getAllMaterialNames()));

		System.out.println();
		System.out.println("Total objects found : " + mpgObjects.size());
		System.out.println("object details per product type:");

		List<MpgObject> products;
		for (String productType : getDistinctProductTypes()) {
			products = getObjectsByProductType(productType);
			System.out.println("#product type : " + productType);
			System.out.println(" - number found : " + products.size());
			System.out.println(" - with total volume : " + getTotalVolumeOfProductType(productType));
			System.out.println("Materials found relating to product: ");
			for (MpgMaterial mpgMaterial : getMaterialsByProductType(productType)) {
				System.out.println(" - " + mpgMaterial == null ? "" : mpgMaterial.getIfcName());
			}
			System.out.println();
		}

		System.out.println("#Total open space objects: " + this.getSpaces().size());
		System.out.println(" - total floor area : " + this.getTotalFloorArea());
		System.out.println();

		getObjectGUIDsWithoutMaterial().SummaryOfGuids();
		getObjectGUIDsWithoutVolume().SummaryOfGuids();
		getObjectGUIDsWithRedundantMaterialSpecs().SummaryOfGuids();
		getObjectGuidsWithUndefinedLayerMats().SummaryOfGuids();

		System.out.println("End of Summary");
		System.out.println("----------------------------");
	}
}
