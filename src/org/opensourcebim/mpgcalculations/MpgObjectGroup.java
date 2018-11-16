package org.opensourcebim.mpgcalculations;

import java.util.List;

public interface MpgObjectGroup {
	
	public long getObjectId();
	public List<MpgObject> getObjects();
	void addObject(MpgObject mpgObject);
	
	public String print();
}
