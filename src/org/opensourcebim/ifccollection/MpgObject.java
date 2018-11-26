package org.opensourcebim.ifccollection;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface MpgObject {
	
	long getObjectId();
	List<MpgSubObject> getSubObjects();
	String getObjectName();
	String getObjectType();
	String getGlobalId();
	String getParentId();
	
	double getVolume();
	
	@JsonIgnore
	List<MpgMaterial> getMaterials();
	
	Set<String> getListedMaterials();
	
	void addListedMaterial(String materialName);

	void addSubObject(MpgSubObject mpgSubObject);
	
	String print();


}
