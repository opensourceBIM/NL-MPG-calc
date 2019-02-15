package org.opensourcebim.ifccollection;

import java.util.List;
import java.util.Map;

public interface MpgObject {
	
	long getObjectId();
	List<MpgLayer> getLayers();
	String getObjectName();
	String getObjectType();
	String getGlobalId();
	String getParentId();
	void setParentId(String value);
	
	String getNLsfbCode();
	
	MpgGeometry getGeometry();
	
	void addMaterialSource(String name, String guid, String source);
	List<MaterialSource> getListedMaterials();
	List<String> getMaterialNamesBySource(String source);

	Map<String, Object> getProperties();
	
	List<MpgInfoTag> getAllTags();
	List<MpgInfoTag> getTagsByType(MpgInfoTagType type);
	
	void addLayer(MpgLayer layer);
	
	String print();
	boolean hasDuplicateMaterialNames();
	
	boolean hasUndefinedMaterials(boolean includeChildren);
	boolean hasUndefinedVolume(boolean includeChildren);
	boolean hasRedundantMaterials(boolean includeChildren);
	boolean hasUndefinedLayers(boolean includeChildren);
	void setNLsfbCode(String parentCode);
	void addTag(MpgInfoTagType tagType, String message);
}
