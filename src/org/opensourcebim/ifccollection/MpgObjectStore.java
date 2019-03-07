package org.opensourcebim.ifccollection;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	
	Stream<String> getAllMaterialNames();
	
	void setObjectForElement(String name, MpgObject mpgObject);
	void addObject(MpgObject mpgObject);
	List<MpgObject> getObjectsByGuids(HashSet<String> guids);
	Optional<MpgObject> getObjectByGuid(String guid);
	Stream<MpgObject> getChildren(String parentGuid);
	
	
	LengthUnit getLengthUnit();
	AreaUnit getAreaUnit();
	VolumeUnit getVolumeUnit();
	
	
	MpgElement addElement(String string);
	MpgElement getElementByName(String name);
	List<MpgElement> getElementsByProductType(String productType);
	
	
	void addProductCard(NmdProductCard card);
	NmdProductCard getProductCard(Integer id);
	List<NmdProductCard> getProductCards(Collection<Integer> ids);
	Map<Integer, NmdProductCard> getProductCards();
	
	void addSpace(MpgSpace space);
	double getTotalVolumeOfMaterial(String name);
	double getTotalFloorArea();
	
	
	boolean isIfcDataComplete();
	
	void toggleMappingDependencies(String globalId, boolean flag);
	
	@JsonIgnore
	GuidCollection getGuidsWithoutMaterial();
	@JsonIgnore
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
}
