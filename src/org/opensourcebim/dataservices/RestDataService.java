package org.opensourcebim.dataservices;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.bimserver.shared.reflector.KeyValuePair;

import com.fasterxml.jackson.databind.JsonNode;

public class RestDataService {

	protected String scheme = "https";
	protected String host;
	protected HttpClient httpClient = HttpClients.createDefault();

	protected HttpResponse performGetRequestWithParams(String path, List<KeyValuePair> params) {
		HttpResponse response = null;
		try {
			HttpGet request = this.createHttpGetRequest(path, params);

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

	protected HttpResponse performPostRequestWithParams(String path, List<NameValuePair> params, List<NameValuePair> headers) {
		// Try add a Post request to the base class.
		HttpPost httppost = new HttpPost(path);

		for (NameValuePair header : headers) {
			httppost.addHeader(header.getName(), header.getValue().toString());
		}

		// Execute and get the response.
		HttpResponse response = null;
		try {
			httppost.setEntity(new UrlEncodedFormEntity(params));
			response = httpClient.execute(httppost);
			httppost.releaseConnection();
		} catch(Exception e) {
			System.out.println("post request failed " + response.getStatusLine().toString());
		}
		return response;
	}

	protected HttpGet createHttpGetRequest(String path, List<KeyValuePair> params) throws URISyntaxException {
		URIBuilder builder = new URIBuilder();
		builder.setScheme(this.scheme).setHost(this.host).setPath(path);

		params.forEach(p -> builder.addParameter(p.getFieldName(), p.getValue().toString()));

		URI uri = builder.build();
		HttpGet request = new HttpGet(uri);

		return request;
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

}
