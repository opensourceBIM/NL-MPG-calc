package org.opensourcebim.mapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicStatusLine;
import org.opensourcebim.ifccollection.MpgObject;

import com.fasterxml.jackson.databind.type.CollectionType;

import nl.tno.bim.mapping.domain.CommonWord;
import nl.tno.bim.mapping.domain.IfcMaterialKeyword;
import nl.tno.bim.mapping.domain.IfcToNlsfb;
import nl.tno.bim.mapping.domain.Mapping;
import nl.tno.bim.mapping.domain.MappingSet;
import nl.tno.bim.nmd.services.RestDataService;

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

	public MappingDataServiceRestImpl() {
		setScheme("http");
		setHost("localhost");
		setPort(8085);
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
		return new ResponseWrapper<Mapping>(null,  new BasicStatusLine(new ProtocolVersion("http",1, 1), 404, "not implemented yet"));
	}

	@Override
	public Map<String, List<String>> getNlsfbMappings() {
		String path = "/api/ifctonlsfb";
		HttpResponse resp = this.performGetRequestWithParams(path, null);
		ResponseWrapper<List<IfcToNlsfb>> maps = this.handleHttpResponse(resp, IfcToNlsfb.class,
				mapper.getTypeFactory().constructCollectionType(List.class, IfcToNlsfb.class));

		Map<String, List<String>> res = new HashMap<>();
		if (maps.succes()) {
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
		}
		return res;
	}

	@Override
	public Map<String, Long> getKeyWordMappings(Integer minOccurence) {
		String path = "/api/ifcmaterialkeyword";
		HttpResponse resp = this.performGetRequestWithParams(path, null);
		ResponseWrapper<List<IfcMaterialKeyword>> maps = this.handleHttpResponse(resp, IfcMaterialKeyword.class,
				mapper.getTypeFactory().constructCollectionType(List.class, IfcMaterialKeyword.class));

		Map<String, Long> res = new HashMap<>();
		if (maps.succes()) {
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
		List<String> res = new ArrayList<>();

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
	public boolean postCommonWords(List<String[]> entries) {
		try {
			List<CommonWord> cWords = new ArrayList<>();

			String[] headers = entries.get(0);
			List<String[]> values = entries.subList(1, entries.size());
			if (headers.length == 1) {
				for (String[] word : values) {
					cWords.add(new CommonWord(word[0]));
				}
				String apiPath = "/api/commonword";
				HttpResponse resp = this.performPostRequestWithParams(apiPath, null, null, cWords);
				if (resp.getStatusLine().getStatusCode() == 200) {
					return true;
				}
			} else {
				System.out.println("incorrect input data encountered.");
			}
		} catch (Exception e) {
			System.out.println("error encountered posting common words: " + e.getMessage());
		}
		return false;
	}

	/**
	 * Convert the body of the httpResponse to a java object of an input class and wraps that with the response status
	 * @param resp HttpResponse returned from api call (json)
	 * @param classType class to convert the json body to
	 * @return a Wrapper object combining parsed class and status of the http response
	 */
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

	/**
	 * Similar to the regular handleHttpResponse method, but then used for deserializing collections of objects
	 * @param resp HttpResponse returned from api call (json)
	 * @param classType class to convert the json body to
	 * @param collType collection type to serialize from <? extends Iterable>
	 * @return a Wrapper object combining parsed List<Class> collections and status of the http response
	 */
	private <T> ResponseWrapper<List<T>> handleHttpResponse(HttpResponse resp, Class<T> classType,
			CollectionType collType) {
		List<T> obj = null;
		
		if (resp != null && resp.getStatusLine().getStatusCode() == 200) {
			try {
				String body = respHandler.handleResponse(resp);
				obj = mapper.readValue(body, collType);
			} catch (IOException e) {
				System.out.println("Error encountered retrieving response " + e.getMessage());
			}
		}
		StatusLine line;
		if (resp != null) {
			line = resp.getStatusLine();
		} else {
			line = new BasicStatusLine(new ProtocolVersion(this.getScheme(), 1, 1), 500, "no response received");
		}
		
		return new ResponseWrapper<List<T>>(obj, line);
	}

}
