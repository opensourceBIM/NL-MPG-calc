package org.opensourcebim.ifccollection;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface MpgObject {
	
	long getObjectId();
	String getObjectName();
	String getObjectType();
	String getGlobalId();
	String getParentId();
	void setParentId(String value);
	
	String getNLsfbCode();	
	void setNLsfbCode(String code);
	boolean hasNlsfbCode();
	@JsonIgnore
	Set<NlsfbCode> getNLsfbAlternatives();
	void addNlsfbAlternatives(Set<String> alternatives);
	
	MpgGeometry getGeometry();
	
	List<MpgLayer> getLayers();
	void addMaterialSource(String name, String guid, String source);
	void addMaterialSource(MaterialSource source);
	List<MaterialSource> getListedMaterials();
	List<String> getMaterialNamesBySource(String source);

	Map<String, Object> getProperties();
	
	List<MpgInfoTag> getAllTags();
	List<MpgInfoTag> getTagsByType(MpgInfoTagType type);
	
	void addLayer(MpgLayer layer);
	
	boolean hasDuplicateMaterialNames();
	
	void addTag(MpgInfoTagType tagType, String message);


}
