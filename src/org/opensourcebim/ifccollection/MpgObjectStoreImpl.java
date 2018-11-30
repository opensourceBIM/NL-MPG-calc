package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.emf.common.util.BasicEList;
import org.opensourcebim.nmd.NmdProductCard;

public class MpgObjectStoreImpl implements MpgObjectStore {

	private HashMap<String, MpgMaterial> mpgMaterials;
	private List<MpgObject> mpgObjects;
	private List<MpgSubObject> spaces;

	/**
	 * List that states which materials have no objects linked
	 */
	private List<String> orphanedMaterials;

	/**
	 * List that states which object GUIDs have no material or no layers and have
	 * neither related objects that have materials
	 */
	private List<String> objectGUIDsWithoutMaterial;

	/**
	 * List that states which object GUIDs have no material or no layers and have
	 * neither related objects that have materials
	 */
	private List<String> objectGUIDsWithoutGeometry;

	/**
	 * List that states which objects have layers that cannot be resolved to a
	 * material
	 */
	private List<String> objectGuidsWithPartialMaterialDefinition;

	/**
	 * List to store which materials have a single object but multiple materials
	 */
	private List<String> objectGUIDsWithRedundantMats;

	// list that has parent GUIDs mapped to child objects
	private List<ImmutablePair<String, MpgObject>> decomposedRelations;

	public MpgObjectStoreImpl() {
		setMaterials(new HashMap<>());
		setObjects(new BasicEList<MpgObject>());
		setSpaces(new BasicEList<MpgSubObject>());

		setOrphanedMaterials(new ArrayList<String>());
		setObjectGUIDsWithoutMaterial(new ArrayList<String>());
		setObjectGUIDsWithoutGeometry(new BasicEList<String>());
		setObjectGuidsWithPartialMaterialDefinition(new BasicEList<String>());
		setObjectGUIDsWithRedundantMaterialSpecs(new BasicEList<String>());
	}

	public void Reset() {
		mpgObjects.clear();
		mpgMaterials.clear();
		spaces.clear();

		getOrphanedMaterials().clear();
		getObjectGUIDsWithoutMaterial().clear();
		getObjectGUIDsWithoutGeometry().clear();
		getObjectGuidsWithPartialMaterialDefinition().clear();
		getObjectGUIDsWithRedundantMaterialSpecs().clear();
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

	private Optional<MpgObject> getObjectByGuid(String guidId) {
		return mpgObjects.stream().filter(o -> o.getGlobalId().equals(guidId)).findFirst();
	}

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

		List<String> materialNames = objectsByProductType.stream().flatMap(o -> o.getListedMaterials().stream())
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

	@Override
	public void recreateParentChildMap(Map<String, String> childToParentMap) {

		this.getObjects().forEach(o -> {
			if (childToParentMap.containsKey(o.getGlobalId())) {
				o.setParentId(childToParentMap.get(o.getGlobalId()));
			}
		});

		// create a list with parent ids linked to child objects
		this.decomposedRelations = this.getObjects().stream()
				.filter(o -> o.getParentId() != null && !o.getParentId().isEmpty())
				.map(o -> new ImmutablePair<String, MpgObject>(o.getParentId(), o)).collect(Collectors.toList());
	}

	/**
	 * Do a general check over all objects to check whether there are floating
	 * materials (not linked to any MpgObject) or if there are MpgObjects that do no
	 * match with any material
	 * 
	 * @return
	 */
	@Override
	public boolean isIfcDataComplete() {
		// first: check if there are any orphaned materials
		for (String materialName : this.getMaterials().keySet()) {
			if (!(mpgObjects.stream().anyMatch(o -> o.getListedMaterials().contains(materialName)))) {
				getOrphanedMaterials().add(materialName);
			}
		}

		// check for objects that have not a single material defined or objects without
		// layers and multiple materials
		setObjectGUIDsWithoutMaterial(
				mpgObjects.stream().filter(o -> o.hasUndefinedMaterials(true))
						.map(o -> o.getGlobalId()).collect(Collectors.toList()));

		// some objects might not have a volume defined. check whether any object or any
		// of its children have undefined volumes.
		setObjectGUIDsWithoutGeometry(mpgObjects.stream()
				.filter(o -> o.hasUndefinedVolume(true))
				.map(o -> o.getGlobalId()).collect(Collectors.toList()));

		// check for materials without layers that have more than a single material
		// added to them and as such cannot be resolved automatically
		setObjectGUIDsWithRedundantMaterialSpecs(
				mpgObjects.stream().filter(o -> o.hasRedundantMaterials(true))
						.map(o -> o.getGlobalId()).collect(Collectors.toList()));

		// check for objects that have more than 0 materials linked, but are still
		// missing 1 to n materials
		setObjectGuidsWithPartialMaterialDefinition(mpgObjects.stream()
				.filter(o -> o.hasUndefinedLayers(true))
				.map(g -> g.getGlobalId()).collect(Collectors.toList()));

		return getOrphanedMaterials().size() == 0 
			&& getObjectGUIDsWithoutMaterial().size() == 0
			&& getObjectGUIDsWithoutGeometry().size() == 0 
			&& getObjectGUIDsWithRedundantMaterialSpecs().size() == 0
			&& getObjectGuidsWithPartialMaterialDefinition().size() == 0;
	}

	@Override
	public Stream<MpgObject> getChildren(String parentGuid) {
		return this.decomposedRelations.parallelStream().filter(p -> p.getLeft() == parentGuid).map(p -> p.getRight());
	}

	/**
	 * Check if all the MpgMaterials have matched Nmd ProductCards linked
	 */
	@Override
	public boolean isMaterialDataComplete() {
		return getMaterials().values().stream().allMatch(mat -> mat.getNmdMaterialSpecs() != null);
	}

	@Override
	public List<String> getOrphanedMaterials() {
		return orphanedMaterials;
	}

	public void setOrphanedMaterials(List<String> orphanedMaterials) {
		this.orphanedMaterials = orphanedMaterials;
	}

	@Override
	public List<String> getObjectGUIDsWithoutMaterial() {
		return objectGUIDsWithoutMaterial;
	}

	public void setObjectGUIDsWithoutMaterial(List<String> objectGUIDsWithoutMaterial) {
		this.objectGUIDsWithoutMaterial = objectGUIDsWithoutMaterial;
	}

	public List<String> getObjectGUIDsWithoutGeometry() {
		return objectGUIDsWithoutGeometry;
	}

	public void setObjectGUIDsWithoutGeometry(List<String> objectGUIDsWithoutGeometry) {
		this.objectGUIDsWithoutGeometry = objectGUIDsWithoutGeometry;
	}

	@Override
	public List<String> getObjectGUIDsWithRedundantMaterialSpecs() {
		return objectGUIDsWithRedundantMats;
	}

	public void setObjectGUIDsWithRedundantMaterialSpecs(List<String> objectGUIDsWithRedundantMats) {
		this.objectGUIDsWithRedundantMats = objectGUIDsWithRedundantMats;
	}

	@Override
	public List<String> getObjectGuidsWithPartialMaterialDefinition() {
		return objectGuidsWithPartialMaterialDefinition;
	}

	public void setObjectGuidsWithPartialMaterialDefinition(List<String> objectGuidsWithPartialMaterialDefinition) {
		this.objectGuidsWithPartialMaterialDefinition = objectGuidsWithPartialMaterialDefinition;
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
				System.out.println(" - " + mpgMaterial.getIfcName());
			}
			System.out.println();
		}

		System.out.println("#Total open space objects: " + this.getSpaces().size());
		System.out.println(" - total floor area : " + this.getTotalFloorArea());
		System.out.println();

		System.out.println("# Materials that do not link to any object : " + getOrphanedMaterials().size());
		System.out.println();
		System.out.println(
				"# Objects that have no materials listed and no layers linked, nor any related objects with materials: "
						+ getObjectGUIDsWithoutMaterial().size());
		System.out.println();
		System.out.println(
				"# Objects that have redundant or undefined geometry (looking at this object and any related embedded objects): "
						+ getObjectGUIDsWithoutGeometry().size());
		System.out.println();
		System.out.println("# Objects that have redundant material definitions: "
				+ getObjectGUIDsWithRedundantMaterialSpecs().size());
		System.out.println();
		System.out.println("# Objects that have one or more missing material links: "
				+ getObjectGuidsWithPartialMaterialDefinition().size());

		System.out.println();

		System.out.println("End of Summary");
		System.out.println("----------------------------");
	}
}
