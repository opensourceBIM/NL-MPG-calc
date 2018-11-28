package org.opensourcebim.ifccollection;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicEList;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MpgObjectImpl implements MpgObject {

	private long objectId;
	private String globalId;
	private String objectName;
	private List<MpgSubObject> mpgSubObjects;
	private String objectType;
	private String parentId;

	@JsonIgnore
	private Supplier<MpgObjectStore> getStore;
	private Map<String, String> listedMaterials;
	private double volume;

	public MpgObjectImpl(long objectId, String globalId, String objectName, String objectType, String parentId,
			MpgObjectStore objectStore) {

		mpgSubObjects = new BasicEList<MpgSubObject>();
		this.objectId = objectId;
		this.setGlobalId(globalId);
		this.setObjectName(objectName);
		if (objectType != null) {
			objectType = objectType.replaceAll("Impl$", "");
			objectType = objectType.replaceAll("^Ifc", "");
			this.setObjectType(objectType);
		}
		this.parentId = parentId;

		listedMaterials = new HashMap<String, String>();
		this.getStore = () -> {
			return objectStore;
		};
	}

	@Override
	public void addSubObject(MpgSubObject mpgSubObject) {
		mpgSubObjects.add(mpgSubObject);
		String matName = mpgSubObject.getMaterialName();
		if (matName != null && !matName.equals("")) {
			addListedMaterial(matName, mpgSubObject.getId());
		}
	}

	@Override
	public List<MpgSubObject> getSubObjects() {
		return mpgSubObjects;
	}

	@Override
	public long getObjectId() {
		return objectId;
	}

	@Override
	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType == null ? "undefined type" : objectType;
	}

	@Override
	public String getObjectName() {
		return this.objectName;
	}

	private void setObjectName(String objectName) {
		this.objectName = objectName == null ? "undefined name" : objectName;
	}

	@Override
	public String getGlobalId() {
		return globalId;
	}

	public void setGlobalId(String globalId) {
		this.globalId = globalId;
	}

	@Override
	public double getVolume() {
		return this.volume;
	}

	public void setVolume(double value) {
		this.volume = value;
	}

	@Override
	public String getParentId() {
		return this.parentId;
	}

	@Override
	public void setParentId(String value) {
		this.parentId = value;

	}

	@Override
	public Collection<String> getListedMaterials() {
		return this.listedMaterials.values();
	}

	@Override
	public void addListedMaterial(String materialName, String GUID) {
		this.listedMaterials.put(GUID, materialName);
	}

	@Override
	public String print() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.objectType + " : " + this.objectName + " with id:" + objectId);
		sb.append(System.getProperty("line.separator"));
		sb.append(">> GUID: " + this.getGlobalId());
		sb.append(System.getProperty("line.separator"));
		mpgSubObjects.forEach(o -> sb.append(o.print()));
		return sb.toString();
	}

	@Override
	public boolean hasDuplicateMaterialNames() {
		return this.listedMaterials.values().stream().distinct().collect(Collectors.toSet())
				.size() < this.listedMaterials.size();
	}
}
