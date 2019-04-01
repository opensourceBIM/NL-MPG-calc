package org.opensourcebim.mapping;

import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.BasicResponseHandler;
import org.opensourcebim.dataservices.RestDataService;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.nmd.MappingDataService;
import org.opensourcebim.nmd.NmdUserDataConfig;

import com.fasterxml.jackson.databind.type.CollectionType;
import com.opencsv.CSVReader;

import nl.tno.bim.mapping.domain.IfcToNlsfb;
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

	private BasicResponseHandler respHandler = new BasicResponseHandler();
	private NmdUserDataConfig config;

	public MappingDataServiceRestImpl(NmdUserDataConfig config) {
		this.config = config;
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
		String path = "/api/ifctonlsfb";
		HttpResponse resp = this.performGetRequestWithParams(path, null);
		List<IfcToNlsfb> maps = new ArrayList<>();
		maps = this.handleHttpResponse(resp, IfcToNlsfb.class,
				mapper.getTypeFactory().constructCollectionType(List.class, IfcToNlsfb.class));

		Map<String, List<String>> res = new HashMap<>();

		for (IfcToNlsfb dbMap : maps) {

			String nlsfbCode = dbMap.getNlsfbCode();
			String productType = dbMap.getIfcProductType();

			if (res.containsKey(productType)) {
				res.get(productType).add(nlsfbCode);
			} else {
				List<String> vals = new ArrayList<>();
				vals.add(nlsfbCode);
				res.put(productType, vals);
			}
		};

		return res;
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

	public void regenerateMappingData() {
		regenerateIfcToNlsfbMappings();

	}

	@Override
	public void regenerateIfcToNlsfbMappings() {

		try {
			System.err.println("");
			FileReader fReader = new FileReader(config.getNlsfbAlternativesFilePath());
			CSVReader reader = new CSVReader(fReader);
			List<String[]> myEntries = reader.readAll();
			reader.close();

			String[] headers = myEntries.get(0);
			List<String[]> values = myEntries.subList(1, myEntries.size());

			if (headers.length == 2) {
				List<IfcToNlsfb> nlsfbMaps = new ArrayList<>();
				for (String[] entry : values) {
					IfcToNlsfb map = new IfcToNlsfb();
					map.setIfcProductType(entry[1]);
					map.setNlsfbCode(entry[0]);
					nlsfbMaps.add(map);
				}

				String path = "/api/ifctonlsfb";
				HttpResponse resp = this.performPostRequestWithParams(path, null, null, nlsfbMaps);
				if (resp.getStatusLine().getStatusCode() != 200) {
					System.out.println("error encountered posting nlsfb alternative map");
				}

			} else {
				// our import file does not seem to be in the assumed structure.
				System.out.println("incorrect input data encountered.");
			}
		} catch (Exception e) {
			System.err.println("error encountered regenerating mappings: " + e.getMessage());
		}

	}

}
