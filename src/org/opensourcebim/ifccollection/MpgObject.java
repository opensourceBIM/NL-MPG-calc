package org.opensourcebim.ifccollection;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import nl.tno.bim.nmd.domain.NlsfbCode;

public interface MpgObject {
	
	long getObjectId();
	String getObjectName();
	String getObjectType();
	String getGlobalId();
	String getParentId();
	void setParentId(String value);
	
	NlsfbCode getNLsfbCode();	
	void setNLsfbCode(String code);
	void setNLsfbCode(NlsfbCode code);
	boolean hasNlsfbCode();
	@JsonIgnore
	Set<NlsfbCode> getNLsfbAlternatives();
	void addNlsfbAlternatives(Set<String> alternatives);
	
	MpgGeometry getGeometry();
	
	List<MpgLayer> getLayers();
	void addMaterialSource(String name, String guid, String source);
	void addMaterialSource(MaterialSource source);
	List<MaterialSource> getListedMaterials();
	List<String> getMaterialNamesBySource(String source);

	Map<String, Object> getProperties();
	
	List<MpgInfoTag> getAllTags();
	List<MpgInfoTag> getTagsByType(MpgInfoTagType type);
	
	void addLayer(MpgLayer layer);
	
	boolean hasDuplicateMaterialNames();
	
	void addTag(MpgInfoTagType tagType, String message);
	void clearTagsOfType(MpgInfoTagType nmdproductcardwarning);
	
	/**
	 * Identifier for an MpgObject by which similarity for NMD grouping is determined.
	 * @return
	 */
	String getUnMappedGroupHash();
	MpgLayer getLayerByProductId(Integer productId);
}
