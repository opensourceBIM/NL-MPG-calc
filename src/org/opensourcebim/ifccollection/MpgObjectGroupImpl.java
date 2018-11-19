package org.opensourcebim.ifccollection;

import java.util.List;

import org.eclipse.emf.common.util.BasicEList;

public class MpgObjectGroupImpl implements MpgObjectGroup {

	private long objectId;
	private String globalId;
	private String objectName;
	private List<MpgObject> mpgObjects;
	private String objectType;
	
	public MpgObjectGroupImpl(long objectId, String globalId, String objectName, String objectType) {
		mpgObjects = new BasicEList<MpgObject>();
		this.objectId = objectId;
		this.globalId = globalId;
		this.setObjectName(objectName);
		objectType = objectType.replaceAll("Impl$", "");
		objectType = objectType.replaceAll("^Ifc", "");
		this.setObjectType(objectType);
	}
		
	@Override
	public void addObject(MpgObject mpgObject) {
		mpgObjects.add(mpgObject);
	}
	
	@Override
	public List<MpgObject> getObjects() {
		return mpgObjects;
	}

	@Override
	public long getObjectId() {
		return objectId;
	}
	
	@Override
	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType == null ? "undefined type" : objectType;
	}
	
	@Override
	public String getObjectName() {
		return this.objectName;
	}
	
	private void setObjectName(String objectName) {
		this.objectName = objectName == null ? "undefined name" : objectName;
	}
	
	@Override
	public String print() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.objectType + " : " + this.objectName +" with id:" + objectId);
		sb.append(System.getProperty("line.separator"));
		sb.append(">> GUID: " + this.globalId);
		sb.append(System.getProperty("line.separator"));
		mpgObjects.forEach(o -> sb.append(o.print()));
		return sb.toString();
	}
}
