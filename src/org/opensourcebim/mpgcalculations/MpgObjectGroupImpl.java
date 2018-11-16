package org.opensourcebim.mpgcalculations;

import java.util.List;

import org.eclipse.emf.common.util.BasicEList;

public class MpgObjectGroupImpl implements MpgObjectGroup {

	private long objectId;
	private String globalId;
	private List<MpgObject> mpgObjects;
	
	public MpgObjectGroupImpl(long objectId, String globalId) {
		mpgObjects = new BasicEList<MpgObject>();
		this.objectId = objectId;
		this.globalId = globalId;
	}
	
	@Override
	public long getObjectId() {
		return objectId;
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
	public String print() {
		StringBuilder sb = new StringBuilder();
		sb.append("object group with id:" + objectId);
		sb.append(System.getProperty("line.separator"));
		sb.append(">> GUID: " + this.globalId);
		sb.append(System.getProperty("line.separator"));
		mpgObjects.forEach(o -> sb.append(o.print()));
		return sb.toString();
	}
}
