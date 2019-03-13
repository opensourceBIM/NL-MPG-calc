package org.opensourcebim.bcf;

import java.util.UUID;

public class BcfObject {

	private String guid;
	
	public BcfObject() {
		guid = UUID.randomUUID().toString();
	}

	public String getGuid() {
		return guid;
	}	
}
