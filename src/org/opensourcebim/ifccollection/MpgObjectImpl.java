package org.opensourcebim.ifccollection;


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
	private List<MpgSubObject> mpgLayers;
	private String objectType;
	private String parentId;
	
	private Map<String, Object> properties;

	@JsonIgnore
	private Supplier<MpgObjectStore> getStore;
	private List<MaterialSource> listedMaterials;

	private double volume;

	public MpgObjectImpl(long objectId, String globalId, String objectName, String objectType, String parentId,
			MpgObjectStore objectStore) {

		mpgLayers = new BasicEList<MpgSubObject>();
		this.objectId = objectId;
		this.setGlobalId(globalId);
		this.setObjectName(objectName);
		if (objectType != null) {
			objectType = objectType.replaceAll("Impl$", "");
			objectType = objectType.replaceAll("^Ifc", "");
			this.setObjectType(objectType);
		}
		this.parentId = parentId;

		properties = new HashMap<String, Object>();
		listedMaterials = new BasicEList<MaterialSource>();

		this.getStore = () -> {
			return objectStore;
		};
	}

	@Override
	public void addLayer(MpgSubObject mpgLayer) {
		mpgLayers.add(mpgLayer);
	}

	@Override
	public List<MpgSubObject> getLayers() {
		return mpgLayers;
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
	public Map<String, Object> getProperties() {
		return this.properties;
	}
	
	public void addProperty(String name, Object value) {
		this.properties.put(name, value);
		
	}

	@Override
	public void addMaterialSource(String materialName, String materialGuid, String source) {
		this.listedMaterials.add(new MaterialSource(materialGuid, materialName, source));
		this.getStore.get().addMaterial(materialName);
	}
	
	@Override
	public List<String> getMaterialNamesBySource(String source) {
		return this.listedMaterials.stream()
				.filter(m -> source == null ? true : m.getSource() == source)
				.map(m -> m.getName())
				.collect(Collectors.toList());
	}

	@Override
	public String print() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.objectType + " : " + this.objectName + " with id:" + objectId);
		sb.append(System.getProperty("line.separator"));
		sb.append(">> GUID: " + this.getGlobalId());
		sb.append(System.getProperty("line.separator"));
		mpgLayers.forEach(o -> sb.append(o.print()));
		return sb.toString();
	}

	@Override
	public boolean hasDuplicateMaterialNames() {
		return this.listedMaterials.stream().distinct().collect(Collectors.toSet())
				.size() < this.listedMaterials.size();
	}

	/**
	 * Recursive check method to validate whether a material or any of its children
	 * have undefined materials
	 */
	@Override
	public boolean hasUndefinedMaterials(boolean includeChildren) {
		long numLayers = this.getLayers().size();
		boolean ownCheck = (numLayers + getMaterialNamesBySource(null).size()) == 0;

		// anyMatch returns false on an empty list, so if children should be included,
		// but no
		// children are present it will still return false
		boolean hasChildren = getStore.get().getChildren(this.getGlobalId()).count() > 0;
		boolean childCheck = includeChildren && getStore.get().getChildren(this.getGlobalId())
				.anyMatch(o -> o.hasUndefinedMaterials(includeChildren));

		// if the own item has undefined items at least the children should have full
		// definitions
		return ownCheck && !hasChildren ? true : childCheck;
	}

	@Override
	public boolean hasUndefinedVolume(boolean includeChildren) {
		boolean ownCheck = getVolume() == 0;
		boolean hasChildren = getStore.get().getChildren(this.getGlobalId()).count() > 0;
		boolean childCheck = includeChildren
				&& getStore.get().getChildren(this.getGlobalId()).anyMatch(o -> o.hasUndefinedVolume(includeChildren));

		return ownCheck && !hasChildren ? true : childCheck;
	}

	@Override
	public boolean hasRedundantMaterials(boolean includeChildren) {
		long numLayers = getLayers().size();
		boolean ownCheck = (numLayers == 0) && (getMaterialNamesBySource(null).size() > 1) || hasDuplicateMaterialNames();
		boolean childCheck = includeChildren && getStore.get().getChildren(this.getGlobalId())
				.anyMatch(o -> o.hasRedundantMaterials(includeChildren));
		return ownCheck || childCheck;
	}

	@Override
	public boolean hasUndefinedLayers(boolean includeChildren) {
		long numLayers = getLayers().size();
		long unresolvedLayers = getLayers().stream()
				.filter(l -> l.getMaterialName() == "" || l.getMaterialName() == null).collect(Collectors.toList())
				.size();

		boolean ownCheck = (numLayers > 0) && (unresolvedLayers > 0);
		boolean childCheck = includeChildren
				&& getStore.get().getChildren(this.getGlobalId()).anyMatch(o -> o.hasUndefinedLayers(includeChildren));
		return ownCheck || childCheck;
	}
}
