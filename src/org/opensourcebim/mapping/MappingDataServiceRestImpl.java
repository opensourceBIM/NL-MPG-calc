package org.opensourcebim.mapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.BasicResponseHandler;
import org.opensourcebim.dataservices.RestDataService;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.nmd.MappingDataService;

import nl.tno.bim.mapping.domain.Mapping;
import nl.tno.bim.mapping.domain.MappingSet;

/**
 * Class to provide an interface between java code and a mapping database The
 * mapping database will store any data that is required to resolve what (Nmd)
 * products to choose based on ifc file data.
 * 
 * @author vijj
 * 
 */
public class MappingDataServiceRestImpl extends RestDataService implements MappingDataService {

	private BasicResponseHandler respHandler  = new BasicResponseHandler();
	
	public MappingDataServiceRestImpl() {
		setScheme("http");
		setHost("localhost");
		setPort(8090);
	}

	@Override
	public Mapping postMapping(Mapping map) {
		String path = "/api/mapping";
		HttpResponse resp = this.performPostRequestWithParams(path, null, null, map);
		return this.handleHttpResponse(resp, Mapping.class);
	}
	
	@Override
	public Mapping getMappingById(Long id) {
		String path = "/api/mapping/" + id;
		HttpResponse resp = this.performGetRequestWithParams(path, null);
		return this.handleHttpResponse(resp, Mapping.class);
	}

	@Override
	public MappingSet postMappingSet(MappingSet set) {
		String path = "/api/mappingset";
		HttpResponse resp = this.performPostRequestWithParams(path, null, null, set);
		return this.handleHttpResponse(resp, MappingSet.class);
	}
	
	@Override
	public MappingSet getMappingSetByProjectIdAndRevisionId(Long pid, Long rid) {
		String path = "/api/mappingset/" + rid + "/" + pid + "/mappingsetmap";
		HttpResponse resp = this.performGetRequestWithParams(path, null);
		return handleHttpResponse(resp, MappingSet.class);
	}
	
	private <T> T handleHttpResponse(HttpResponse resp, Class<T> classType) {
		T obj = null;
		if (resp.getStatusLine().getStatusCode() != 200) {
			System.err.println("error encountered posting " + classType.getName());
		} else {
			try {
				String body = respHandler.handleResponse(resp);
				obj = mapper.readValue(body, classType);
			} catch (IOException e) {
				System.out.println("Error encountered retrieving response " + e.getMessage());
			}
		}
		return obj;
	}

	@Override
	public Mapping getApproximateMapForObject(MpgObject object) {
		return null;
	}

	@Override
	public Map<String, List<String>> getNlsfbMappings() {
		return new HashMap<>();

	}

	@Override
	public Map<String, Long> getKeyWordMappings(Integer minOccurence) {
		return new HashMap<>();
	}

	@Override
	public List<String> getCommonWords() {
		return new ArrayList<>();
	}

	@Override
	public void connect() {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}
}
