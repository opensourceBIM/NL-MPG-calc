package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicEList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import nl.tno.bim.nmd.domain.NlsfbCode;

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

		this.objectId = objectId;
		this.setGlobalId(globalId);
		this.setObjectName(objectName);
		if (objectType != null) {
			objectType = objectType.replaceAll("Impl$", "");
			this.setObjectType(objectType);
		}
		this.parentId = parentId;

		initializeCollections();
	}
	
	public MpgObjectImpl() {
		initializeCollections();
	}
	
	private void initializeCollections() {
		mpgLayers = new BasicEList<MpgLayer>();
		properties = new HashMap<String, Object>();
		tags = new ArrayList<MpgInfoTag>();
		this.listedMaterials = new BasicEList<MaterialSource>();
		this.nlsfbAlternatives = new HashSet<NlsfbCode>();
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

	public void setObjectName(String objectName) {
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
	public void setNLsfbCode(NlsfbCode code) {
		this.nlsfb = code;
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
	public void clearTagsOfType(MpgInfoTagType type) {
		List<MpgInfoTag> typeTags = this.tags.parallelStream()
				.filter(t -> t.getType() == type)
				.collect(Collectors.toList());
		this.tags.removeAll(typeTags);
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
		if (!source.getName().isEmpty()) {
			this.listedMaterials.add(source);
		}
	}

	@Override
	public String getMappedGroupHash() {
		String nlsfbToText = this.nlsfb == null ? "" : this.getNLsfbCode().print();
		return this.getUnMappedGroupHash()
		+ nlsfbToText;
	}
	
	@Override
	public String getUnMappedGroupHash() {
		String matNames = this.getListedMaterials().stream()
				.map(MaterialSource::getName)
				.collect(Collectors.joining( "_" ) );
		return this.getObjectName() + this.getObjectType() + matNames;
	}

	@Override
	public boolean copyMappingFromObject(MpgObject mpgObject) {
		// the earlier applied value hash should already confirm that the two lists are equals so no need to do another check
		for (int i = 0; i < this.getListedMaterials().size(); i++) {
			this.getListedMaterials().set(i, mpgObject.getListedMaterials().get(i).copy());
		}	
		return true;
	}

	@Override
	public MpgLayer getLayerByProductId(Integer productId) {
		Optional<MaterialSource> matSource = this.getListedMaterials().stream().filter(mat -> mat.getMapId() == productId).findFirst();
		if(matSource.isPresent()) {
			Optional<MpgLayer> layer = this.getLayers().stream().filter(l -> l.getMaterialName().equals(matSource.get().getName())).findFirst();
			return layer.isPresent() ? layer.get() : null;
		}
		return null;
	}
}
