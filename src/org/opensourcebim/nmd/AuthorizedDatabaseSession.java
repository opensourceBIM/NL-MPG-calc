package org.opensourcebim.nmd;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.bimserver.shared.reflector.KeyValuePair;
import org.opensourcebim.dataservices.RestDataService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class to handle authorization to a rest API for the NMD connectionn
 * @author vijj
 * ToDo: there are currently a lot of methods in here that should be in a lower level class. move these.
 */
public class AuthorizedDatabaseSession extends RestDataService  {

	private String token;		
	
	@Override
	protected HttpGet createHttpGetRequest(String path, List<KeyValuePair> params) throws URISyntaxException {
		URIBuilder builder = new URIBuilder();
		builder.setScheme(this.scheme).setHost(this.host).setPath(path);
		
		params.forEach(p -> builder.addParameter(p.getFieldName(), p.getValue().toString()));
		
		URI uri = builder.build();
		HttpGet request = new HttpGet(uri);
		request.addHeader("Access_Token", this.token);
		
		return request;
	}

	
	protected JsonNode responseToJson(HttpResponse response) {
		HttpEntity entity = response.getEntity();

		// do something with the entity to get the token
		ObjectMapper mapper = new ObjectMapper();
		JsonNode responseNode = null;
		try {
			responseNode = mapper.readTree(EntityUtils.toString(entity));
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			return null;
		}
		return responseNode;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
}
