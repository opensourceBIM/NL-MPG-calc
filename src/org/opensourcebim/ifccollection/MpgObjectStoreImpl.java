package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.input.NullReader;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.opensourcebim.nmd.MaterialSpecifications;

public class MpgObjectStoreImpl implements MpgObjectStore {

	private HashMap<String, MpgMaterial> mpgMaterials;
	private List<MpgObjectGroup> mpgObjectGroups;
	private List<MpgObject> spaces;

	// lists to find any problem with materials
	private List<String> orphanedMaterials;
	private List<String> objectGUIDsWithoutMaterial;
	private List<String> objectGuidsWithPartialMaterialDefinition;

	public MpgObjectStoreImpl() {
		setMaterials(new HashMap<>());
		setObjectGroups(new BasicEList<MpgObjectGroup>());
		setSpaces(new BasicEList<MpgObject>());

		setOrphanedMaterials(new ArrayList<String>());
		setObjectGUIDsWithoutMaterial(new ArrayList<String>());
	}

	public void Reset() {
		mpgObjectGroups.clear();
		mpgMaterials.clear();
		spaces.clear();

		getOrphanedMaterials().clear();
		getObjectGUIDsWithoutMaterial().clear();
	}

	@Override
	public void addMaterial(String name) {
		mpgMaterials.putIfAbsent(name, new MpgMaterial(name));
	}

	@Override
	public HashMap<String, MpgMaterial> getMaterials() {
		return mpgMaterials;
	}

	private void setMaterials(HashMap<String, MpgMaterial> mpgMaterials) {
		this.mpgMaterials = mpgMaterials;
	}
	
	@Override
	public void setSpecsForMaterial(String name, MaterialSpecifications specs) {
		// TODO catch null reference exceptions
		getMaterialByName(name).setMaterialSpecs(specs);
	}

	@Override
	public List<MpgObjectGroup> getObjectGroups() {
		return mpgObjectGroups;
	}

	private void setObjectGroups(List<MpgObjectGroup> mpgObjectGroups) {
		this.mpgObjectGroups = mpgObjectGroups;
	}

	@Override
	public List<MpgObject> getSpaces() {
		return spaces;
	}

	private void setSpaces(List<MpgObject> spaces) {
		this.spaces = spaces;
	}

	@Override
	public void addObjectGroup(MpgObjectGroup group) {
		this.getObjectGroups().add(group);
	}

	@Override
	public Set<String> getDistinctProductTypes() {
		return mpgObjectGroups.stream().map(group -> group.getObjectType()).distinct().collect(Collectors.toSet());
	}

	@Override
	public List<MpgObjectGroup> getObjectsByProductType(String productType) {
		return mpgObjectGroups.stream().filter(g -> g.getObjectType().equals(productType)).collect(Collectors.toList());
	}

	@Override
	public List<MpgObjectGroup> getObjectsByProductName(String productName) {
		return mpgObjectGroups.stream().filter(g -> g.getObjectName() == productName).collect(Collectors.toList());
	}

	@Override
	public List<MpgObject> getObjectsByMaterialName(String materialName) {
		return mpgObjectGroups.stream().flatMap(g -> g.getObjects().stream()).filter(o -> o.getMaterialName() != null)
				.filter(o -> o.getMaterialName().equals(materialName)).collect(Collectors.toList());
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
		List<MpgObjectGroup> objectsByProductType = this.getObjectsByProductType(productType);
		List<String> materialNames = objectsByProductType.stream().flatMap(g -> g.getObjects().stream())
				.map(obj -> obj.getMaterialName()).distinct().collect(Collectors.toList());
		return mpgMaterials.values().stream().filter(mat -> materialNames.contains(mat.getIfcName()))
				.collect(Collectors.toList());
	}

	@Override
	public double getTotalVolumeOfMaterial(String name) {
		return getObjectsByMaterialName(name).stream().filter(mat -> mat != null).map(o -> o.getVolume())
				.filter(o -> o != null).collect(Collectors.summingDouble(o -> o));
	}

	@Override
	public double getTotalVolumeOfProductType(String productType) {
		return getObjectsByProductType(productType).stream().flatMap(g -> g.getObjects().stream())
				.filter(o -> o != null).collect(Collectors.summingDouble(o -> o.getVolume()));
	}

	@Override
	public void addSpace(MpgObject space) {
		spaces.add(space);
	}

	@Override
	public double getTotalFloorArea() {
		return spaces.stream().map(s -> s.getArea()).filter(a -> a != null).collect(Collectors.summingDouble(a -> a));
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
		for (String key : this.getMaterials().keySet()) {
			if (getObjectsByMaterialName(key).size() == 0) {
				getOrphanedMaterials().add(key);
			}
		}

		// check for objectgroups that have not a single material defined
		setObjectGUIDsWithoutMaterial(mpgObjectGroups.stream()
				.filter(group -> group.getObjects().size() ==  0 || (
						group.getObjects().size() > 0 && 
						group.getObjects().size() == group.getObjects().stream().filter(o -> o.getMaterialName() == null).count()))
				.map(g -> g.getGlobalId()).collect(Collectors.toList()));
		
		// check for objectgroups that have more than 0 materials linked, but are still missing 1 to n materials
		setObjectGuidsWithPartialMaterialDefinition(mpgObjectGroups.stream()
			.filter(group -> group.getObjects().size() > 0)
			.filter(group -> group.getObjects().stream().filter(o -> o.getMaterialName() == null).count() < group.getObjects().size())
			.map(g -> g.getGlobalId()).collect(Collectors.toList()));

		return getOrphanedMaterials().size() == 0 &&
				getObjectGUIDsWithoutMaterial().size() == 0 &&
				getObjectGUIDsWithoutMaterial().size() == 0;
	}
	
	/**
	 * Check if all the MpgMaterials have Materia
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
		System.out.println("Total objects found : " + mpgObjectGroups.size());
		System.out.println("object details per product type:");

		List<MpgObjectGroup> products;
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

		System.out.println("#Materials that do not link to any object : " + getOrphanedMaterials().size());
		if (getOrphanedMaterials().size() > 0) {
			System.out.println(String.join(",\n", getOrphanedMaterials()));
		}
		System.out.println();
		System.out.println("#object groups that have no materials or objects linked: " + getObjectGUIDsWithoutMaterial().size());
		if (getObjectGUIDsWithoutMaterial().size() > 0) {
			System.out.println(String.join(",\n", getObjectGUIDsWithoutMaterial()));
		}
		System.out.println();
		
		System.out.println("#object groups that have one or more missing material links: " + getObjectGuidsWithPartialMaterialDefinition().size());
		if (getObjectGuidsWithPartialMaterialDefinition().size() > 0) {
			System.out.println(String.join(",\n", getObjectGuidsWithPartialMaterialDefinition()));
		}
		System.out.println();

		System.out.println("End of Summary");
		System.out.println("----------------------------");
	}
}
