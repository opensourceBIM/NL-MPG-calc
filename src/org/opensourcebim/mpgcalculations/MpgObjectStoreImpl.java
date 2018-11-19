package org.opensourcebim.mpgcalculations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicEList;

public class MpgObjectStoreImpl implements MpgObjectStore {

	private HashMap<String, MpgMaterial> mpgMaterials;
	private List<MpgObjectGroup> mpgObjectGroups;
	private List<MpgObject> spaces;

	public MpgObjectStoreImpl() {
		setMaterials(new HashMap<>());
		setObjectGroups(new BasicEList<MpgObjectGroup>());
		setSpaces(new BasicEList<MpgObject>());
	}

	public void Reset() {
		mpgObjectGroups.clear();
		mpgMaterials.clear();
		spaces.clear();
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
	public List<MpgObjectGroup> getObjectGroups() {
		return mpgObjectGroups;
	}

	private void setObjectGroups(List<MpgObjectGroup> mpgObjectLinks) {
		this.mpgObjectGroups = mpgObjectLinks;
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
	public List<MpgObjectGroup> getObjectsByProductType(String productType) {
		return mpgObjectGroups.stream().filter(g -> g.getObjectType() == productType).collect(Collectors.toList());
	}

	@Override
	public List<MpgObjectGroup> getObjectsByProductName(String productName) {
		return mpgObjectGroups.stream().filter(g -> g.getObjectName() == productName).collect(Collectors.toList());
	}

	@Override
	public List<MpgObject> getObjectsByMaterialName(String materialName) {
		return mpgObjectGroups.stream().flatMap(g -> g.getObjects().stream())
				.filter(o -> o.getMaterial() != null)
				.filter(o -> o.getMaterial().getIfcName() == materialName).collect(Collectors.toList());
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
	public Double GetTotalVolumeOfMaterial(String name) {
		return getObjectsByMaterialName(name).stream().filter(mat -> mat != null).map(o -> o.getVolume())
				.filter(o -> o != null).collect(Collectors.summingDouble(o -> o));
	}

	@Override
	public void addSpace(MpgObject space) {
		spaces.add(space);
	}

	@Override
	public Double getTotalFloorArea() {
		return spaces.stream().map(s -> s.getArea()).filter(a -> a != null).collect(Collectors.summingDouble(a -> a));
	}

	/**
	 * Do a general check over all objects to check whether there are floating materials (not linked to any MpgObject)
	 * or if there are MpgObjects that do no match with any material
	 * @return
	 */
	@Override
	public boolean CheckForWarningsAndErrors() {
		// first: check if there are any orphaned materials
		List<String> orphanMaterialNames = new ArrayList<String>();
		for (String key : this.getMaterials().keySet()) {
			if (getObjectsByMaterialName(key).size() == 0) {
				orphanMaterialNames.add(key);
			}
		}
		
		// second: check for objectgroups with objects not linked to a material
		List<MpgObject> unlinkedObjects = mpgObjectGroups.stream()
				.flatMap(g -> g.getObjects().stream())
				.filter(o -> o.getMaterial() == null)
				.collect(Collectors.toList());
		
		return orphanMaterialNames.size() == 0 && unlinkedObjects.size() == 0;
	}

	@Override
	public void FullReport() {

		System.out.println("----------------------------");
		System.out.println(">> found materials : " + getMaterials().size());
		getMaterials().forEach((key, mat) -> {
			System.out.println(mat.print());
			System.out.println("");
		});

		System.out.println();
		System.out.println(">> ifc physical object collection: ");
		getObjectGroups().forEach(group -> {
			System.out.println(group.print());
			System.out.println("");
		});

		System.out.println();
		System.out.println(">> open spaces: ");
		getSpaces().forEach(space -> {
			System.out.println("space with volume : " + space.getVolume() + " and area : " + space.getArea());
		});

		System.out.println("----------------------------");
	}

	@Override
	public void SummaryReport() {
		// TODO Auto-generated method stub
	}

}
