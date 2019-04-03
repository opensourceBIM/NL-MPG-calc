package org.opensourcebim.mapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.BasicResponseHandler;
import org.opensourcebim.dataservices.ResponseWrapper;
import org.opensourcebim.dataservices.RestDataService;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.nmd.MappingDataService;
import org.opensourcebim.nmd.NmdUserDataConfig;

import com.fasterxml.jackson.databind.type.CollectionType;

import nl.tno.bim.mapping.domain.CommonWord;
import nl.tno.bim.mapping.domain.IfcMaterialKeyword;
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

	public NmdUserDataConfig getConfig() {
		return config;
	}

	@Override
	public ResponseWrapper<Mapping> postMapping(Mapping map) {
		String path = "/api/mapping";
		HttpResponse resp = this.performPostRequestWithParams(path, null, null, map);
		return this.handleHttpResponse(resp, Mapping.class);
	}

	@Override
	public ResponseWrapper<Mapping> getMappingById(Long id) {
		String path = "/api/mapping/" + id;
		HttpResponse resp = this.performGetRequestWithParams(path, null);
		return this.handleHttpResponse(resp, Mapping.class);
	}

	@Override
	public ResponseWrapper<MappingSet> postMappingSet(MappingSet set) {
		String path = "/api/mappingset";
		HttpResponse resp = this.performPostRequestWithParams(path, null, null, set);
		return this.handleHttpResponse(resp, MappingSet.class);
	}

	@Override
	public ResponseWrapper<MappingSet> getMappingSetByProjectIdAndRevisionId(Long pid, Long rid) {
		String path = "/api/mappingset/" + rid + "/" + pid + "/mappingsetmap";
		HttpResponse resp = this.performGetRequestWithParams(path, null);
		return this.handleHttpResponse(resp, MappingSet.class);
	}

	@Override
	public ResponseWrapper<Mapping> getApproximateMapForObject(MpgObject object) {
		return null;
	}

	@Override
	public Map<String, List<String>> getNlsfbMappings() {
		String path = "/api/ifctonlsfb";
		HttpResponse resp = this.performGetRequestWithParams(path, null);
		ResponseWrapper<List<IfcToNlsfb>> maps = this.handleHttpResponse(resp, IfcToNlsfb.class,
				mapper.getTypeFactory().constructCollectionType(List.class, IfcToNlsfb.class));

		Map<String, List<String>> res = null;
		if (maps.succes()) {
			res = new HashMap<>();

			for (IfcToNlsfb dbMap : maps.getObject()) {

				String nlsfbCode = dbMap.getNlsfbCode();
				String productType = dbMap.getIfcProductType();

				if (res.containsKey(productType)) {
					res.get(productType).add(nlsfbCode);
				} else {
					List<String> vals = new ArrayList<>();
					vals.add(nlsfbCode);
					res.put(productType, vals);
				}
			}
			;
		}
		return res;
	}

	@Override
	public Map<String, Long> getKeyWordMappings(Integer minOccurence) {
		String path = "/api/ifcmaterialkeyword";
		HttpResponse resp = this.performGetRequestWithParams(path, null);
		ResponseWrapper<List<IfcMaterialKeyword>> maps = this.handleHttpResponse(resp, IfcMaterialKeyword.class,
				mapper.getTypeFactory().constructCollectionType(List.class, IfcMaterialKeyword.class));

		Map<String, Long> res = null;
		if (maps.succes()) {
			res = new HashMap<>();
			for (IfcMaterialKeyword dbMap : maps.getObject()) {
				if (dbMap.getCount() >= minOccurence) {
					res.putIfAbsent(dbMap.getKeyword(), dbMap.getCount());
				}
			}
		}
		return res;
	}

	@Override
	public List<String> getCommonWords() {
		String path = "/api/commonword";
		HttpResponse resp = this.performGetRequestWithParams(path, null);
		List<String> res = null;
		
		ResponseWrapper<List<CommonWord>> maps = this.handleHttpResponse(resp, CommonWord.class,
				mapper.getTypeFactory().constructCollectionType(List.class, CommonWord.class));
		if (maps.succes()) {
			res = maps.getObject().stream().map(w -> w.getWord()).collect(Collectors.toList());
		}
		return res;
	}

	@Override
	public void connect() {
	}

	@Override
	public void disconnect() {
	}

	public boolean postNlsfbMappings(List<String[]> entries) {
		String[] headers = entries.get(0);
		List<String[]> values = entries.subList(1, entries.size());

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
			if (resp.getStatusLine().getStatusCode() == 200) {
				return true;
			}
			System.out.println("error encountered posting nlsfb alternative map");
		} else {
			// our import file does not seem to be in the assumed structure.
			System.out.println("incorrect input data encountered.");
		}
		return false;
	}

	@Override
	public boolean postKeyWords(List<Entry<String, Long>> words) {
		try {
			List<IfcMaterialKeyword> kWords = new ArrayList<>();

			for (Entry<String, Long> word : words) {
				kWords.add(new IfcMaterialKeyword(word.getKey(), word.getValue()));
			}
			String apiPath = "/api/ifcmaterialkeyword";
			HttpResponse resp = this.performPostRequestWithParams(apiPath, null, null, kWords);
			if (resp.getStatusLine().getStatusCode() == 200) {
				return true;
			}
		} catch (Exception e) {
			System.out.println("error occured posting keywords to database: " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean postCommonWords(List<String[]> words) {
		return false;
	}
	
	private <T> ResponseWrapper<T> handleHttpResponse(HttpResponse resp, Class<T> classType) {
		T obj = null;
		if (resp.getStatusLine().getStatusCode() == 200) {
			try {
				String body = respHandler.handleResponse(resp);
				obj = mapper.readValue(body, classType);
			} catch (IOException e) {
				System.out.println("Error encountered retrieving response " + e.getMessage());
			}
		}
		return new ResponseWrapper<T>(obj, resp.getStatusLine());
	}

	private <T> ResponseWrapper<List<T>> handleHttpResponse(HttpResponse resp, Class<T> classType,
			CollectionType collType) {
		List<T> obj = null;
		if (resp.getStatusLine().getStatusCode() == 200) {
			try {
				String body = respHandler.handleResponse(resp);
				obj = mapper.readValue(body, collType);
			} catch (IOException e) {
				System.out.println("Error encountered retrieving response " + e.getMessage());
			}
		}
		return new ResponseWrapper<List<T>>(obj, resp.getStatusLine());
	}

}
