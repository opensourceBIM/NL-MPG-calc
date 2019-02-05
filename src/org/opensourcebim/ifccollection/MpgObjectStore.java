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

	HashSet<MpgElement> getElements();
	
	@JsonIgnore
	List<MpgObject> getObjects();
	List<MpgSpace> getSpaces();
	
	void reset();
	
	Set<String> getDistinctProductTypes();

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
	
	
	void validateIfcDataCollection();
	boolean isIfcDataComplete();
	
	@JsonIgnore
	GuidCollection getGuidsWithoutMaterial();
	GuidCollection getGuidsWithoutMaterialAndWithoutFullDecomposedMaterials();
	@JsonIgnore
	GuidCollection getGuidsWithoutVolume();
	GuidCollection getGuidsWithoutVolumeAndWithoutFullDecomposedVolumes();
	GuidCollection getGuidsWithRedundantMaterials();
	GuidCollection getGuidsWithUndefinedLayerMats();
	
	void setProductCardForElement(String string, NmdProductCard specs);
	void setObjectForElement(String name, MpgObject mpgObject);
	
	boolean isElementDataComplete();
	Stream<String> getAllMaterialNames();

	void SummaryReport();
}
