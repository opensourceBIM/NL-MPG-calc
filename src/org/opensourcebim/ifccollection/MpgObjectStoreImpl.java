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

/**
 * Storage container for collected ifc objects. Only data relevant for Mpg
 * calculations should be stored here
 * 
 * @author vijj
 *
 */
public class MpgObjectStoreImpl implements MpgObjectStore {

	private HashMap<String, MpgMaterial> mpgMaterials;

	private List<MpgObject> mpgObjects;

	private List<MpgSpace> spaces;

	/**
	 * stores which object GUIDs have no material
	 */
	private GuidCollection guidsWithoutMaterial;

	/**
	 * stores which object GUIDs have no material or no layers and have neither
	 * related objects that have materials
	 */
	private GuidCollection guidsWithoutMaterialAndWithoutFullDecomposedMaterials;

	/**
	 * stores which object GUIDs have no volume
	 */
	private GuidCollection guidsWithoutVolume;

	/**
	 * stores which object GUIDs have no volume and have any of their decomposed
	 * objects wthout volume
	 */
	private GuidCollection guidsWithoutVolumeAndWithoutFullDecomposedVolumes;

	/**
	 * stores which objects have layers that cannot be resolved to a material
	 */
	private GuidCollection guidsWithUndefinedLayerMats;

	/**
	 * stores materials have a single object but multiple materials
	 */
	private GuidCollection guidsWithRedundantMats;

	/**
	 * list to store the guids with any MpgObjects that the object linked to hat
	 * guid decomposes
	 */
	@JsonIgnore
	private List<ImmutablePair<String, MpgObject>> decomposedRelations;

	public MpgObjectStoreImpl() {
		setMaterials(new HashMap<>());
		setObjects(new BasicEList<MpgObject>());
		setSpaces(new BasicEList<MpgSpace>());

		setGuidsWithoutMaterial(new GuidCollection(this, "# of objects that have missing materials"));
		setGuidsWithoutMaterialAndWithoutFullDecomposedMaterials(new GuidCollection(this,
				"# of objects without material and any of the decomposed objects without material"));
		setGuidsWithoutVolume(new GuidCollection(this, "# of objects that have missing volumes"));
		setGuidsWithoutVolumeAndWithoutFullDecomposedVolumes(new GuidCollection(this,
				"# of objects without volume and any of the decomposed objects without volume"));
		setGuidsWithUndefinedLayerMats(new GuidCollection(this, "# of objects that have undefined layers"));
		setGuidsWithRedundantMaterialSpecs(
				new GuidCollection(this, "# of objects that cannot be linked to materials 1-on-1"));
	}

	public void reset() {
		mpgObjects.clear();
		mpgMaterials.clear();
		spaces.clear();

		getGuidsWithoutMaterial().reset();
		getGuidsWithoutVolume().reset();
		getGuidsWithUndefinedLayerMats().reset();
		getGuidsWithRedundantMaterials().reset();
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
	public List<MpgSpace> getSpaces() {
		return spaces;
	}

	private void setSpaces(List<MpgSpace> spaces) {
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
	public List<MpgSpace> getObjectsByMaterialName(String materialName) {
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

		List<String> materialNames = objectsByProductType.stream()
				.flatMap(o -> o.getMaterialNamesBySource(null).stream()).distinct().collect(Collectors.toList());

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
	public void recreateParentChildMap(Map<String, String> isDecomposedByrelationMap) {

		this.getObjects().forEach(o -> {
			if (isDecomposedByrelationMap.containsKey(o.getGlobalId())) {
				o.setParentId(isDecomposedByrelationMap.get(o.getGlobalId()));
			}
		});

		// create a list with parent ids linked to child objects
		this.decomposedRelations = this.getObjects().stream().filter(o -> StringUtils.isBlank(o.getParentId()))
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
	 * Check if all the MpgMaterials have matched Nmd ProductCards linked
	 */
	@Override
	public boolean isMaterialDataComplete() {
		return getMaterials().values().stream().allMatch(mat -> mat.getNmdMaterialSpecs() != null);
	}

	@Override
	public GuidCollection getGuidsWithoutMaterial() {
		return guidsWithoutMaterial;
	}

	public void setGuidsWithoutMaterial(GuidCollection coll) {
		this.guidsWithoutMaterial = coll;
	}

	@Override
	public GuidCollection getGuidsWithoutMaterialAndWithoutFullDecomposedMaterials() {
		return guidsWithoutMaterialAndWithoutFullDecomposedMaterials;
	}

	public void setGuidsWithoutMaterialAndWithoutFullDecomposedMaterials(
			GuidCollection guidsWithoutMaterialAndWithoutFullDecomposedMaterials) {
		this.guidsWithoutMaterialAndWithoutFullDecomposedMaterials = guidsWithoutMaterialAndWithoutFullDecomposedMaterials;
	}

	@Override
	public GuidCollection getGuidsWithoutVolume() {
		return guidsWithoutVolume;
	}

	public void setGuidsWithoutVolume(GuidCollection coll) {
		this.guidsWithoutVolume = coll;
	}

	@Override
	public GuidCollection getGuidsWithoutVolumeAndWithoutFullDecomposedVolumes() {
		return guidsWithoutVolumeAndWithoutFullDecomposedVolumes;
	}

	public void setGuidsWithoutVolumeAndWithoutFullDecomposedVolumes(
			GuidCollection guidsWithoutVolumeAndWithoutFullDecomposedVolumes) {
		this.guidsWithoutVolumeAndWithoutFullDecomposedVolumes = guidsWithoutVolumeAndWithoutFullDecomposedVolumes;
	}

	@Override
	public GuidCollection getGuidsWithRedundantMaterials() {
		return guidsWithRedundantMats;
	}

	public void setGuidsWithRedundantMaterialSpecs(GuidCollection coll) {
		this.guidsWithRedundantMats = coll;
	}

	@Override
	public GuidCollection getGuidsWithUndefinedLayerMats() {
		return guidsWithUndefinedLayerMats;
	}

	public void setGuidsWithUndefinedLayerMats(GuidCollection coll) {
		this.guidsWithUndefinedLayerMats = coll;
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
		getGuidsWithoutMaterial().setCollection(mpgObjects.stream().filter(o -> o.hasUndefinedMaterials(false))
				.map(o -> o.getGlobalId()).collect(Collectors.toList()));

		getGuidsWithoutMaterialAndWithoutFullDecomposedMaterials().setCollection(mpgObjects.stream()
				.filter(o -> o.hasUndefinedMaterials(true)).map(o -> o.getGlobalId()).collect(Collectors.toList()));

		// some objects might not have a volume defined. check whether any object or any
		// of its children have undefined volumes.
		getGuidsWithoutVolume().setCollection(mpgObjects.stream().filter(o -> o.hasUndefinedVolume(false))
				.map(o -> o.getGlobalId()).collect(Collectors.toList()));

		getGuidsWithoutVolumeAndWithoutFullDecomposedVolumes().setCollection(mpgObjects.stream()
				.filter(o -> o.hasUndefinedVolume(true)).map(o -> o.getGlobalId()).collect(Collectors.toList()));

		// check for materials without layers that have more than a single material
		// added to them and as such cannot be resolved automatically
		getGuidsWithRedundantMaterials().setCollection(mpgObjects.stream().filter(o -> o.hasRedundantMaterials(false))
				.map(o -> o.getGlobalId()).collect(Collectors.toList()));

		// check for objects that have more than 0 materials linked, but are still
		// missing 1 to n materials
		getGuidsWithUndefinedLayerMats().setCollection(mpgObjects.stream().filter(o -> o.hasUndefinedLayers(false))
				.map(g -> g.getGlobalId()).collect(Collectors.toList()));
	}

	@Override
	public boolean isIfcDataComplete() {
		return getGuidsWithoutMaterial().getSize() == 0 && getGuidsWithoutVolume().getSize() == 0
				&& getGuidsWithRedundantMaterials().getSize() == 0 && getGuidsWithUndefinedLayerMats().getSize() == 0;
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

		getGuidsWithoutMaterial().SummaryOfGuids();
		getGuidsWithoutVolume().SummaryOfGuids();
		getGuidsWithoutMaterialAndWithoutFullDecomposedMaterials().SummaryOfGuids();
		getGuidsWithoutVolumeAndWithoutFullDecomposedVolumes().SummaryOfGuids();
		getGuidsWithRedundantMaterials().SummaryOfGuids();
		getGuidsWithUndefinedLayerMats().SummaryOfGuids();

		System.out.println("End of Summary");
		System.out.println("----------------------------");
	}
}
