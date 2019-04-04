package org.opensourcebim.dataservices;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bimserver.shared.reflector.KeyValuePair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestDataService {

	protected ObjectMapper mapper = new ObjectMapper();
	
	private String scheme;
	private String host;
	private Integer port;
	protected HttpClient httpClient = HttpClients.createDefault();
	HttpUriRequest request;

	protected HttpResponse performGetRequestWithParams(String path, List<KeyValuePair> params) {
		HttpResponse response = null;
		request = this.createHttpGetRequest(path, params);

		// Execute and get the response.
		try {
			response = this.httpClient.execute(request);
		} catch (Exception e) {
			System.out.println("failed to perform get request: " + e.getMessage());
		}

		return response;
	}

	protected HttpResponse performPostRequestWithParams(String path, List<KeyValuePair> params, List<NameValuePair> headers, Object body) {
		// Try add a Post request to the base class.
		request = new HttpPost(createUri(path, params));

		if (headers != null) {
			for (NameValuePair header : headers) {
				request.addHeader(header.getName(), header.getValue().toString());
			}
		}
	
		// Execute and get the response.
		HttpResponse response = null;
		try {
			if(body != null) {
				String jsonBody = mapper.writeValueAsString(body);
				((HttpPost)request).setEntity(new StringEntity(jsonBody,"UTF-8"));
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json;charset=UTF-8");
			}
			
			response = httpClient.execute(request);
		} catch(Exception e) {
			System.out.println("post request failed " + response.getStatusLine().toString());
		}
		return response;
	}

	protected HttpGet createHttpGetRequest(String path, List<KeyValuePair> params){
		HttpGet request = new HttpGet(createUri(path, params));
		return request;
	}
	
	protected URI createUri(String path, List<KeyValuePair> params) {
		URIBuilder builder = new URIBuilder();
		builder.setScheme(this.getScheme()).setHost(this.host).setPort(this.port).setPath(path);
		if (params != null) {
			params.forEach(p -> builder.addParameter(p.getFieldName(), p.getValue().toString()));
		}

		try {
			return builder.build();
		} catch (URISyntaxException e) {
			System.out.println("encountered error in creating URI: " + e.getMessage());
			return null;
		}
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

	public void setHost(String host) {
		this.host = host;
	}
	
	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}
	
	public String getScheme() {
		return scheme;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	
	public URI getConnectionString() {
		URIBuilder builder = new URIBuilder();
		builder.setScheme(this.getScheme()).setHost(this.host).setPort(this.port);
		try {
			return builder.build();
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
	protected JsonNode responseToJson(HttpResponse response) {
		HttpEntity entity = response.getEntity();

		// do something with the entity to get the token
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
}
