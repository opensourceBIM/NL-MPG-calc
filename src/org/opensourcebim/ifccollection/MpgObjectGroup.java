package org.opensourcebim.ifccollection;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface MpgObjectGroup {
	
	long getObjectId();
	List<MpgObject> getObjects();
	String getObjectName();
	String getObjectType();
	String getGlobalId();
	
	@JsonIgnore
	List<MpgMaterial> getMaterials();
	
	void addObject(MpgObject mpgObject);
	
	String print();
}
