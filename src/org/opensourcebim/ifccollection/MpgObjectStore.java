package org.opensourcebim.ifccollection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.opensourcebim.nmd.NmdProductCard;

public interface MpgObjectStore {

	HashMap<String, MpgMaterial> getMaterials();
	List<MpgObject> getObjects();
	List<MpgSubObject> getSpaces();
	void Reset();
	
	void addObject(MpgObject group);
	Set<String> getDistinctProductTypes();
	List<MpgObject> getObjectsByProductType(String productType);
	List<MpgObject> getObjectsByProductName(String productName);
	List<MpgSubObject> getObjectsByMaterialName(String materialName);
	
	void recreateParentChildMap(Map<String, String> childToParentMap);
	Stream<MpgObject> getChildren(String parentGuid);
	
	void addMaterial(String string);
	Set<String> getAllMaterialNames();
	MpgMaterial getMaterialByName(String name);
	List<MpgMaterial> getMaterialsByProductType(String productType);
	double getTotalVolumeOfMaterial(String name);
	double getTotalVolumeOfProductType(String productType);
	
	void addSpace(MpgSubObject space);
	double getTotalFloorArea();
	
	boolean isIfcDataComplete();
	List<String> getOrphanedMaterials();
	List<String> getObjectGUIDsWithoutMaterial();
	List<String> getObjectGUIDsWithRedundantMaterialSpecs();
	List<String> getObjectGuidsWithPartialMaterialDefinition();
	
	void SummaryReport();
	void setProductCardForMaterial(String string, NmdProductCard specs);
	boolean isMaterialDataComplete();




}
