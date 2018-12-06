package org.opensourcebim.ifccollection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.opensourcebim.ifcanalysis.GuidCollection;
import org.opensourcebim.nmd.NmdProductCard;

public interface MpgObjectStore {

	HashMap<String, MpgMaterial> getMaterials();
	List<MpgObject> getObjects();
	List<MpgSubObject> getSpaces();
	
	void reset();
	
	Set<String> getDistinctProductTypes();

	void addObject(MpgObject mpgObject);
	List<MpgObject> getObjectsByProductType(String productType);
	List<MpgObject> getObjectsByProductName(String productName);
	List<MpgSubObject> getObjectsByMaterialName(String materialName);
	List<MpgObject> getObjectsByGuids(HashSet<String> guids);
	Optional<MpgObject> getObjectByGuid(String guid);
	
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
	GuidCollection getObjectGUIDsWithoutMaterial();
	GuidCollection getObjectGUIDsWithoutVolume();
	GuidCollection getObjectGUIDsWithRedundantMaterialSpecs();
	GuidCollection getObjectGuidsWithUndefinedLayerMats();
	
	void setProductCardForMaterial(String string, NmdProductCard specs);
	boolean isMaterialDataComplete();
	
	void SummaryReport();
}
