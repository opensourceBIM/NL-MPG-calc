package org.opensourcebim.nmd;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bimserver.shared.reflector.KeyValuePair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthorizedDatabaseSession {
	protected String scheme = "https";
	private String host;
	protected HttpClient httpClient = HttpClients.createDefault();
	private String token;		
	
	protected HttpResponse performGetRequestWithParams(String path, List<KeyValuePair> params) {
		URIBuilder builder = new URIBuilder();
		builder.setScheme(this.scheme).setHost(this.host).setPath(path);

		params.forEach(p -> builder.addParameter(p.getFieldName(), p.getValue().toString()));
		HttpResponse response = null;
		URI uri;
		try {
			uri = builder.build();
			HttpGet request = new HttpGet(uri);
			request.addHeader("Access_Token", this.token);

			// Execute and get the response.

			try {
				response = this.httpClient.execute(request);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (URISyntaxException e1) {
			System.out.println("Incorrect URI for NMD request");
			e1.printStackTrace();
		}
		return response;
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

	protected Integer TryParseJsonNode(JsonNode node, Integer defaultValue) {
		return (node == null) ? defaultValue : node.asInt(defaultValue);
	}

	protected Double TryParseJsonNode(JsonNode node, Double defaultValue) {
		return (node == null) ? defaultValue : node.asDouble(defaultValue);
	}

	protected String TryParseJsonNode(JsonNode node, String defaultValue) {
		return (node == null) ? defaultValue : node.asText(defaultValue);
	}

	protected Boolean TryParseJsonNode(JsonNode node, Boolean defaultValue) {
		return (node == null) ? defaultValue : node.asBoolean(defaultValue);
	}

	public String getHost() {
		return host;
	}

	protected void setHost(String host) {
		this.host = host;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
}
