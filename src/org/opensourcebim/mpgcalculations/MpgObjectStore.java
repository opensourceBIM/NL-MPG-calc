package org.opensourcebim.mpgcalculations;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public interface MpgObjectStore {

	public HashMap<String, MpgMaterial> getMaterials();
	public List<MpgObjectGroup> getObjectGroups();
	public List<MpgObject> getSpaces();
	public void Reset();
	
	public void addObjectGroup(MpgObjectGroup group);
	public List<MpgObjectGroup> getObjectsByProductType(String productType);
	public List<MpgObjectGroup> getObjectsByProductName(String productName);
	public List<MpgObject> getObjectsByMaterial(String materialName);
	
	public void addMaterial(String string);
	public Set<String> getAllMaterialNames();
	public MpgMaterial getMaterialByName(String name);
	public Double GetTotalVolumeOfMaterial(String name);
	
	public void addSpace(MpgObject space);
	
	public void FullReport();
	public void SummaryReport();


}
