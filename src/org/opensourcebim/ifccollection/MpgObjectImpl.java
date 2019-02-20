package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicEList;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MpgObjectImpl implements MpgObject {

	private long objectId;
	private String globalId;
	private String objectName;
	private List<MpgLayer> mpgLayers;
	private String objectType;
	private String parentId;
	
	@JsonIgnore
	private Map<String, Object> properties;

	@JsonIgnore
	private Supplier<MpgObjectStore> store;
	private List<MaterialSource> listedMaterials;

	private MpgGeometry geometry;
	private String nlsfb;
	private List<MpgInfoTag> tags;
	private Set<String> nlsfbAlternatives;

	public MpgObjectImpl(long objectId, String globalId, String objectName, String objectType, String parentId,
			MpgObjectStore objectStore) {

		mpgLayers = new BasicEList<MpgLayer>();
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
		tags = new ArrayList<MpgInfoTag>();
		this.listedMaterials = new BasicEList<MaterialSource>();
		this.nlsfbAlternatives = new HashSet<String>();

		this.store = () -> {
			return objectStore;
		};
	}
	
	@JsonIgnore
	private MpgObjectStore getStore() {
		return this.store.get();
	}

	@Override
	public void addLayer(MpgLayer mpgLayer) {
		mpgLayers.add(mpgLayer);
	}

	@Override
	public List<MpgLayer> getLayers() {
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
	public MpgGeometry getGeometry() {
		return this.geometry;
	}
	
	public void setGeometry(MpgGeometry geom) {
		this.geometry = geom;
	}
	
	@Override
	public String getNLsfbCode() {
		return this.nlsfb;
	}
	
	@Override
	public void setNLsfbCode(String code) {
		this.nlsfb = code;
		nlsfbAlternatives.add(code);
	}
	
	@Override
	public boolean hasNlsfbCode() {
		return !(getNLsfbCode() == "" || getNLsfbCode() == null);
	}
	
	@Override
	public Set<String> getNLsfbAlternatives() {
		Set<String> allCodes = new HashSet<String>();
		allCodes.add(getNLsfbCode());
		allCodes.addAll(nlsfbAlternatives);
		
		return allCodes;
	}
	
	@Override
	public void addNlsfbAlternatives(Set<String> alternatives) {
		this.nlsfbAlternatives.addAll(alternatives);
	}

	@Override
	public String getParentId() {
		return this.parentId;
	}

	@Override
	public void setParentId(String value) {
		this.parentId = value;

	}
	
	@JsonIgnore
	@Override
	public Map<String, Object> getProperties() {
		return this.properties;
	}
	
	public void addProperty(String name, Object value) {
		this.properties.put(name, value);
	}
	
	@Override
	public List<MpgInfoTag> getAllTags() {
		return this.tags;
	}
	
	@Override
	public  List<MpgInfoTag> getTagsByType(MpgInfoTagType type) {
		return tags.stream().filter(t -> t.getType().equals(type)).collect(Collectors.toList());
	}
	
	@Override
	public void addTag(MpgInfoTagType tagType, String message) {
		this.tags.add(new MpgInfoTag(tagType, message));
	}

	@Override
	public void addMaterialSource(String materialName, String materialGuid, String source) {
		this.getListedMaterials().add(new MaterialSource(materialGuid, materialName, source));
	}
	

	@Override
	public List<MaterialSource> getListedMaterials() {
		return listedMaterials;
	}
	
	@Override
	public List<String> getMaterialNamesBySource(String source) {
		return this.getListedMaterials().stream()
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
		return this.getListedMaterials().stream().distinct().collect(Collectors.toSet())
				.size() < this.getListedMaterials().size();
	}

	/**
	 * Recursive check method to validate whether a material or any of its children
	 * have undefined materials
	 */
	@Override
	public boolean hasUndefinedMaterials(boolean includeChildren) {
		long numLayers = this.getLayers().size();
		boolean thisIsUndefined = (numLayers + getMaterialNamesBySource(null).size()) == 0;

		// anyMatch returns false on an empty list, so if children should be included,
		// but no children are present it will still return false
		boolean hasChildren = getStore().getChildren(this.getGlobalId()).count() > 0;
		boolean childrenAreUndefined = includeChildren && getStore().getChildren(this.getGlobalId())
				.anyMatch(o -> o.hasUndefinedMaterials(includeChildren));

		return thisIsUndefined && !hasChildren ? thisIsUndefined : childrenAreUndefined;
	}

	@Override
	public boolean hasUndefinedVolume(boolean includeChildren) {
		boolean ownCheck = getGeometry().getVolume() == 0;
		boolean hasChildren = getStore().getChildren(this.getGlobalId()).count() > 0;
		boolean childCheck = includeChildren
				&& getStore().getChildren(this.getGlobalId()).anyMatch(o -> o.hasUndefinedVolume(includeChildren));

		return ownCheck && !hasChildren ? true : childCheck;
	}

	@Override
	public boolean hasRedundantMaterials(boolean includeChildren) {
		long numLayers = getLayers().size();
		boolean ownCheck = (numLayers == 0) && (getMaterialNamesBySource(null).size() > 1) || hasDuplicateMaterialNames();
		boolean childCheck = includeChildren && getStore().getChildren(this.getGlobalId())
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
				&& getStore().getChildren(this.getGlobalId()).anyMatch(o -> o.hasUndefinedLayers(includeChildren));
		return ownCheck || childCheck;
	}
}
