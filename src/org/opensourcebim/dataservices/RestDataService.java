package org.opensourcebim.dataservices;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;

public class RestDataService {

	protected String scheme = "https";
	protected String host;
	protected HttpClient httpClient = HttpClients.createDefault();
	
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
