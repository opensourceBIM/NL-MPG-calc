package org.opensourcebim.mapping;

import org.opensourcebim.nmd.NmdUserDataConfigImpl;

public class MapRunner {
	/**
	 * @param args: not required.
	 */
	public static void main(String[] args) {
		
		MappingDataServiceSqliteImpl mapper = new MappingDataServiceSqliteImpl(new NmdUserDataConfigImpl());

		mapper.regenerateMappingData();

		mapper.disconnect();
	}
}
