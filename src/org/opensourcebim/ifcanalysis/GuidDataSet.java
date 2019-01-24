package org.opensourcebim.ifcanalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.common.util.BasicEList;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectStore;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GuidDataSet {
	
	@JsonIgnore
	private HashSet<String> columnDefinitions;
	
	private HashMap<String, GuidPropertyRecord> records;
		
	public GuidDataSet(MpgObjectStore store) {
		
		records = new HashMap<String, GuidPropertyRecord>();
		columnDefinitions = new HashSet<String>();
		
		for (MpgObject obj : store.getObjects()) {
			String guid = obj.getGlobalId();
			if(StringUtils.isBlank(guid)) {
				continue;
			}

			this.addRecord(guid);
			this.setRecordValue(guid, "name", obj.getObjectName());
			this.setRecordValue(guid, "productType", obj.getObjectType());
			this.setRecordValue(guid, "volume", obj.getVolume());
			this.setRecordValue(guid, "directMats", obj.getMaterialNamesBySource("direct"));
			this.setRecordValue(guid, "layerMats", obj.getMaterialNamesBySource("layer"));
			this.setRecordValue(guid, "typeMats", obj.getMaterialNamesBySource("type"));
			this.setRecordValue(guid, "PsetMats", obj.getMaterialNamesBySource("P_Set"));
			this.setRecordValue(guid, "isAssembly",
					obj.getObjectType().equals("ElementAssembly"));
			this.setRecordValue(guid, "IsDecomposedById", obj.getParentId());
			this.setRecordValue(guid, "hasDecomposedProducts", store.getChildren(obj.getGlobalId()).count() > 0);
			
			Boolean isAssembly = false;
			List<String> parentMats = new BasicEList<String>();
			if (!obj.getParentId().isEmpty()) {
				Optional<MpgObject> parent = store.getObjectByGuid(obj.getParentId());
				if (parent.isPresent()) {
					parentMats = parent.get().getMaterialNamesBySource(null);

					if (parent.get().getObjectType().equals("ElementAssembly")) {
						isAssembly = true;
					}
				}
			}
			this.setRecordValue(guid, "IsDecomposedByMats", parentMats);
			this.setRecordValue(guid, "isPartOfAssembly", isAssembly);
			this.setRecordValue(guid, "allMaterials", obj.getMaterialNamesBySource(null));
		}

	}
	
	public HashMap<String, GuidPropertyRecord> getRecords() {
		return records;
	}
	
	public GuidPropertyRecord getRecordByGuid(String guid) {
		return this.records.getOrDefault(guid, null);
	}
	
	
	public void addRecord(String guid) {
		this.records.putIfAbsent(guid, createNewRecord());
	}
	
	private GuidPropertyRecord createNewRecord() {
		GuidPropertyRecord rec = new GuidPropertyRecord();
		for (String colTitle : columnDefinitions) {
			rec.addOrSetColumn(colTitle, null);
		}
		return rec;
	}
	
	
	public void setRecordValue(String guid, String columnTitle, Object value) {
		
		if (!columnDefinitions.contains(columnTitle)) {
			addColumn(columnTitle);
		}
		
		this.records.get(guid).addOrSetColumn(columnTitle, value);
	}
	
	@JsonIgnore
	public HashSet<String> getColumnDefinitions() {
		return columnDefinitions;
	}
	
	public void addColumn(String title) {
		this.columnDefinitions.add(title);
		// add the new column for all records that already exist
		for (GuidPropertyRecord record : records.values()) {
			record.addOrSetColumn(title, null);
		}
	}
	
	public void reset() {
		getRecords().clear();
		getColumnDefinitions().clear();
	}	
}
