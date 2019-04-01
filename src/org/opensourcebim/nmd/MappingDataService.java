package org.opensourcebim.nmd;

import java.util.List;
import java.util.Map;

import org.opensourcebim.ifccollection.MpgObject;

import nl.tno.bim.mapping.domain.Mapping;
import nl.tno.bim.mapping.domain.MappingSet;

public interface MappingDataService {

	void connect();
	
	void disconnect();
	
	Mapping postMapping(Mapping map);
	
	Mapping getMappingById(Long id);
	
	MappingSet postMappingSet(MappingSet set);
	
	MappingSet getMappingSetByProjectIdAndRevisionId(Long pid, Long rid);
	
	Mapping getApproximateMapForObject(MpgObject object);
	
	Map<String, List<String>> getNlsfbMappings();
	
	Map<String, Long> getKeyWordMappings(Integer minOccurence);

	List<String> getCommonWords();


}
