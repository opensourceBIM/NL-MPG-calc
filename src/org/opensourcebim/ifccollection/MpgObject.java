package org.opensourcebim.ifccollection;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface MpgObject {
	
	long getObjectId();
	List<MpgSubObject> getSubObjects();
	String getObjectName();
	String getObjectType();
	String getGlobalId();
	
	@JsonIgnore
	List<MpgMaterial> getMaterials();
	
	void addObject(MpgSubObject mpgObject);
	
	String print();
}
