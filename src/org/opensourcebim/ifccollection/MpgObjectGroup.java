package org.opensourcebim.ifccollection;

import java.util.List;

public interface MpgObjectGroup {
	
	public long getObjectId();
	public List<MpgObject> getObjects();
	public String getObjectName();
	public String getObjectType();
	
	void addObject(MpgObject mpgObject);
	
	public String print();
}
