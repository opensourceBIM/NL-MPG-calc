package org.opensourcebim.mpgcalculations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicEList;

import com.google.common.collect.Lists;

public class MpgObjectStoreImpl implements MpgObjectStore {

	private HashMap<String, MpgMaterial> mpgMaterials;
	private List<MpgObjectGroup> mpgObjectLinks;
	private List<MpgObject> spaces;

	public MpgObjectStoreImpl() {
		setMaterials(new HashMap<>());
		setObjectGroups(new BasicEList<MpgObjectGroup>());
		setSpaces(new BasicEList<MpgObject>());
	}

	public void Reset() {
		mpgObjectLinks.clear();
		mpgMaterials.clear();
		spaces.clear();
	}

	// ---------- Standard getters and setters -------------
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
		return mpgObjectLinks;
	}

	private void setObjectGroups(List<MpgObjectGroup> mpgObjectLinks) {
		this.mpgObjectLinks = mpgObjectLinks;
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
		// TODO Auto-generated method stub
		return mpgObjectLinks.stream().filter(g -> g.getObjectType() == productType).collect(Collectors.toList());
	}

	@Override
	public List<MpgObjectGroup> getObjectsByProductName(String productName) {
		// TODO Auto-generated method stub
		return mpgObjectLinks.stream().filter(g -> g.getObjectName() == productName).collect(Collectors.toList());
	}

	@Override
	public List<MpgObject> getObjectsByMaterial(String materialName) {
		// TODO Auto-generated method stub
		return mpgObjectLinks.stream().flatMap(g -> g.getObjects().stream())
				.filter(o -> o.getMaterial().getIfcName() == materialName).collect(Collectors.toList());
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
		// TODO Auto-generated method stub
		return getObjectsByMaterial(name).stream().map(o -> o.getVolume()).collect(Collectors.summingDouble(o -> o));
	}

	@Override
	public void addSpace(MpgObject space) {
		spaces.add(space);

	}
	
	public Double getTotalFloorArea() {
		return spaces.stream().map(s -> s.getArea()).collect(Collectors.summingDouble(a -> a));
	}
}
