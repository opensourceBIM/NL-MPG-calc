package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicEList;
import org.opensourcebim.mapping.NlsfbCode;

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
	private List<MaterialSource> listedMaterials;

	private MpgGeometry geometry;
	private NlsfbCode nlsfb;
	private List<MpgInfoTag> tags;
	private Set<NlsfbCode> nlsfbAlternatives;

	public MpgObjectImpl(long objectId, String globalId, String objectName, String objectType, String parentId) {

		mpgLayers = new BasicEList<MpgLayer>();
		this.objectId = objectId;
		this.setGlobalId(globalId);
		this.setObjectName(objectName);
		if (objectType != null) {
			objectType = objectType.replaceAll("Impl$", "");
			this.setObjectType(objectType);
		}
		this.parentId = parentId;

		properties = new HashMap<String, Object>();
		tags = new ArrayList<MpgInfoTag>();
		this.listedMaterials = new BasicEList<MaterialSource>();
		this.nlsfbAlternatives = new HashSet<NlsfbCode>();
	}
	
	public MpgObjectImpl() { }

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
	public NlsfbCode getNLsfbCode() {
		return this.nlsfb;
	}

	@Override
	public void setNLsfbCode(String code) {
		if (NlsfbCode.isNlsfbCode(code)) {
			this.nlsfb = new NlsfbCode(code);
		}
	}

	@Override
	public boolean hasNlsfbCode() {
		return getNLsfbCode() != null && getNLsfbCode().getMajorId() != null;
	}

	@Override
	public Set<NlsfbCode> getNLsfbAlternatives() {
		return nlsfbAlternatives;
	}

	@Override
	public void addNlsfbAlternatives(Set<String> alternatives) {
		alternatives.forEach(c -> {
			if (NlsfbCode.isNlsfbCode(c)) {
				nlsfbAlternatives.add(new NlsfbCode(c));
			}
		});
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
	public List<MpgInfoTag> getTagsByType(MpgInfoTagType type) {
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
		return this.getListedMaterials().stream().filter(m -> source == null ? true : m.getSource() == source)
				.map(m -> m.getName()).collect(Collectors.toList());
	}

	@Override
	public boolean hasDuplicateMaterialNames() {
		return this.getListedMaterials().stream().distinct().collect(Collectors.toSet()).size() < this
				.getListedMaterials().size();
	}

	@Override
	public void addMaterialSource(MaterialSource source) {
		this.listedMaterials.add(source);
	}
}
