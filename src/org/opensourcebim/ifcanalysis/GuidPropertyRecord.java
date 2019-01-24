package org.opensourcebim.ifcanalysis;

import java.util.HashMap;

public class GuidPropertyRecord {
		
	private HashMap<String, Object> values;
	
	public GuidPropertyRecord() {
		values = new HashMap<String, Object> ();
	}
	
	public HashMap<String, Object> getValues() {
		return values;
	}

	public void addOrSetColumn(String title, Object value) {
		values.put(title, value);
	}
}
