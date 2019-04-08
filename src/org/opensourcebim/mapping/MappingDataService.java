package org.opensourcebim.mapping;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opensourcebim.dataservices.ResponseWrapper;
import org.opensourcebim.ifccollection.MpgObject;

import nl.tno.bim.mapping.domain.Mapping;
import nl.tno.bim.mapping.domain.MappingSet;
import nl.tno.bim.nmd.config.UserConfig;

/**
 * Interface to provide generic read and write access to a Mapping Dataservice
 * ToDo: move regenerate methods to separate class
 * @author vijj
 *
 */
public interface MappingDataService {

	void connect();

	void disconnect();

	ResponseWrapper<Mapping> postMapping(Mapping map);

	ResponseWrapper<Mapping> getMappingById(Long id);

	ResponseWrapper<MappingSet> postMappingSet(MappingSet set);

	ResponseWrapper<MappingSet> getMappingSetByProjectIdAndRevisionId(Long pid, Long rid);

	ResponseWrapper<Mapping> getApproximateMapForObject(MpgObject object);

	Map<String, List<String>> getNlsfbMappings();

	Map<String, Long> getKeyWordMappings(Integer minOccurence);

	List<String> getCommonWords();
	
	boolean postNlsfbMappings(List<String[]> entries);

	boolean postKeyWords(List<Entry<String, Long>> words);
	
	boolean postCommonWords(List<String[]> words);

}
