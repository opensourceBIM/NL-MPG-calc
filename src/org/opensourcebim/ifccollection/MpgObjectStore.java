package org.opensourcebim.ifccollection;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.bimserver.utils.AreaUnit;
import org.bimserver.utils.LengthUnit;
import org.bimserver.utils.VolumeUnit;
import org.opensourcebim.ifcanalysis.GuidCollection;
import org.opensourcebim.nmd.NmdProductCard;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface MpgObjectStore {

	void reset();
	
	HashSet<MpgElement> getElements();
	boolean isElementDataComplete();
	
	@JsonIgnore
	List<MpgObject> getObjects();
	List<MpgSpace> getSpaces();
	
	void addProductCardToElement(String string, NmdProductCard specs);
	void setObjectForElement(String name, MpgObject mpgObject);
	
	Stream<String> getAllMaterialNames();
	
	Set<String> getDistinctIfcProductTypes();

	void addObject(MpgObject mpgObject);
	
	List<MpgObject> getObjectsByProductType(String productType);
	
	List<MpgObject> getObjectsByProductName(String productName);
	
	List<MpgSpace> getObjectsByMaterialName(String materialName);
	
	List<MpgObject> getObjectsByGuids(HashSet<String> guids);
	
	Optional<MpgObject> getObjectByGuid(String guid);
	
	Stream<MpgObject> getChildren(String parentGuid);
	
	LengthUnit getLengthUnit();
	AreaUnit getAreaUnit();
	VolumeUnit getVolumeUnit();
	
	void addElement(String string);

	MpgElement getElementByName(String name);
	List<MpgElement> getElementsByProductType(String productType);
	
	double getTotalVolumeOfMaterial(String name);
	double getTotalVolumeOfProductType(String productType);
	
	void addSpace(MpgSpace space);
	double getTotalFloorArea();
	
	boolean isIfcDataComplete();
	
	GuidCollection getGuidsWithoutMaterial();
	GuidCollection getGuidsWithoutMaterialAndWithoutFullDecomposedMaterials();
	
	@JsonIgnore
	GuidCollection getGuidsWithoutVolume();
	@JsonIgnore
	GuidCollection getGuidsWithoutVolumeAndWithoutFullDecomposedVolumes();
	@JsonIgnore
	GuidCollection getGuidsWithRedundantMaterials();
	@JsonIgnore
	GuidCollection getGuidsWithUndefinedLayerMats();
	
	boolean hasUndefinedVolume(MpgObject obj, boolean includeChildren);

	boolean hasRedundantMaterials(MpgObject obj, boolean includeChildren);

	boolean hasUndefinedLayers(MpgObject obj, boolean includeChildren);

	boolean hasUndefinedMaterials(MpgObject obj, boolean includeChildren);

	void checkForMappingDependencies(String globalId, boolean flag);
}
