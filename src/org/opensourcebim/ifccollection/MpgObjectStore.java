package org.opensourcebim.ifccollection;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.opensourcebim.nmd.MaterialSpecifications;

public interface MpgObjectStore {

	HashMap<String, MpgMaterial> getMaterials();
	List<MpgObjectGroup> getObjectGroups();
	List<MpgObject> getSpaces();
	void Reset();
	
	void addObjectGroup(MpgObjectGroup group);
	Set<String> getDistinctProductTypes();
	List<MpgObjectGroup> getObjectsByProductType(String productType);
	List<MpgObjectGroup> getObjectsByProductName(String productName);
	List<MpgObject> getObjectsByMaterialName(String materialName);
	
	void addMaterial(String string);
	Set<String> getAllMaterialNames();
	MpgMaterial getMaterialByName(String name);
	List<MpgMaterial> getMaterialsByProductType(String productType);
	double getTotalVolumeOfMaterial(String name);
	double getTotalVolumeOfProductType(String productType);
	
	void addSpace(MpgObject space);
	double getTotalFloorArea();
	
	boolean isIfcDataComplete();
	List<String> getOrphanedMaterials();
	List<String> getObjectGUIDsWithoutMaterial();
	List<String> getObjectGuidsWithPartialMaterialDefinition();
	
	void SummaryReport();
	void setSpecsForMaterial(String string, MaterialSpecifications specs);
	boolean isMaterialDataComplete();



}
