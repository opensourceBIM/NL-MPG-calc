package org.opensourcebim.ifccollection;

import java.util.List;
import java.util.Map;

public interface MpgObject {
	
	long getObjectId();
	List<MpgSubObject> getLayers();
	String getObjectName();
	String getObjectType();
	String getGlobalId();
	String getParentId();
	void setParentId(String value);
	
	double getVolume();
	
	void addMaterialSource(String name, String guid, String source);
	List<String> getMaterialNamesBySource(String source);

	Map<String, Object> getProperties();
	
	void addLayer(MpgSubObject mpgSubObject);
	
	String print();
	boolean hasDuplicateMaterialNames();
	
	boolean hasUndefinedMaterials(boolean includeChildren);
	boolean hasUndefinedVolume(boolean includeChildren);
	boolean hasRedundantMaterials(boolean includeChildren);
	boolean hasUndefinedLayers(boolean includeChildren);
	



}
