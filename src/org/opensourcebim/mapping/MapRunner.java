package org.opensourcebim.mapping;

import org.opensourcebim.nmd.NmdUserDataConfigImpl;

public class MapRunner {
	/**
	 * @param args: not required.
	 */
	public static void main(String[] args) {
		try {
			NmdMappingDataServiceImpl mapper = new NmdMappingDataServiceImpl(new NmdUserDataConfigImpl());
			mapper.regenerateMappingData();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}
