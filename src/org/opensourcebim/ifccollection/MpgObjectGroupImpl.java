package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicEList;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MpgObjectGroupImpl implements MpgObjectGroup {

	private long objectId;
	private String globalId;
	private String objectName;
	private List<MpgObject> mpgObjects;
	private String objectType;
	
	@JsonIgnore
	private Supplier<MpgObjectStore> getStore;
	
	public MpgObjectGroupImpl(long objectId, String globalId, String objectName, String objectType, MpgObjectStore objectStore) {
		mpgObjects = new BasicEList<MpgObject>();
		this.objectId = objectId;
		this.setGlobalId(globalId);
		this.setObjectName(objectName);
		objectType = objectType.replaceAll("Impl$", "");
		objectType = objectType.replaceAll("^Ifc", "");
		this.setObjectType(objectType);
		
		this.getStore = () -> {return objectStore;};
	}
		
	@Override
	public void addObject(MpgObject mpgObject) {
		mpgObjects.add(mpgObject);
	}
	
	@Override
	public List<MpgObject> getObjects() {
		return mpgObjects;
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
	public List<MpgMaterial> getMaterials() {
		// gte material names
		List<String> materialNames = this.getObjects().stream().map(obj -> obj.getMaterialName()).collect(Collectors.toList());
		// retrieve the materials from the store
		List<MpgMaterial> materials = new ArrayList<MpgMaterial>();
		for(String matName : materialNames) {
			materials.add(this.getStore.get().getMaterialByName(matName));
		}
		return materials;
	}
	
	
	@Override
	public String print() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.objectType + " : " + this.objectName +" with id:" + objectId);
		sb.append(System.getProperty("line.separator"));
		sb.append(">> GUID: " + this.getGlobalId());
		sb.append(System.getProperty("line.separator"));
		mpgObjects.forEach(o -> sb.append(o.print()));
		return sb.toString();
	}
}
