package org.opensourcebim.nmd;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.nmd.NmdDatabaseConfig;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of the NmdDataService to retrieve data from the NMD see :
 * https://www.milieudatabase.nl/ for mor information
 * 
 * @author vijj
 *
 */
public class NmdDataBaseSession implements NmdDataService {

	private static final DateFormat dbDateFormat = new SimpleDateFormat("yyyyMMdd"); // according to docs thi should be
																						// correct
	private Calendar requestDate;
	private NmdDatabaseConfig config = null;
	private HttpClient httpclient;
	private boolean isConnected = false;
	private String token;

	public NmdDataBaseSession(NmdDatabaseConfig config) {
		this.config = config;
		this.requestDate = Calendar.getInstance();
		httpclient = HttpClients.createDefault();
	}

	public void setToken(String token) {
		this.token = token;
	}

	@Override
	public Calendar getRequestDate() {
		return this.requestDate;
	}

	@Override
	public void setRequestDate(Calendar newDate) {
		this.requestDate = newDate;
	}

	@Override
	public Boolean getIsConnected() {
		// TODO Auto-generated method stub
		return this.isConnected;
	}

	@Override
	public void login() {

		HttpPost httppost = new HttpPost(
				"https://www.milieudatabase-datainvoer.nl/NMD_30_AuthenticationServer/NMD_30_API_Authentication/getToken");

		httppost.addHeader("refreshToken", config.getToken());
		httppost.addHeader("API_ID", "1");
		httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("grant_type", "client_credentials"));

		// Execute and get the response.
		HttpResponse response = null;
		try {
			httppost.setEntity(new UrlEncodedFormEntity(params));
			response = httpclient.execute(httppost);
			JsonNode responseNode = this.responseToJson(response);
			JsonNode tokenNode = responseNode.get("TOKEN");
			this.token = tokenNode.asText();

			this.isConnected = true;
			httppost.releaseConnection();
		} catch (IOException e1) {
			this.isConnected = false;
			System.out.println("authentication failed: " + response.getStatusLine().toString());
		}
	}

	@Override
	public void logout() {
		this.setToken("");
		this.isConnected = false;
	}

	@Override
	public List<NmdProductCard> getAllProductSets() {
		HttpResponse response = this.performGetRequestWithParams(
				"https://www.Milieudatabase-datainvoer.nl/NMD_30_API_v0.2/api/NMD30_Web_API/NLsfB_RAW_Elementen?ZoekDatum=20190105",
				new BasicHttpParams());

		// do something with the entity to get the token
		JsonNode resp_node = this.responseToJson(response);

		// convert reponseNode to NmdProductCard info.
		List<NmdProductCard> results = new ArrayList<NmdProductCard>();
		resp_node.get("results")
			.forEach(f -> results.add(this.getIdsFromJson(f)));
		
		return results;
	}

	@Override
	public NmdProductCard retrieveMaterial(MpgElement material) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getProfileSetForProductCard(NmdProductCard product) {
		
	}

	@Override
	public List<NmdFaseProfiel> getFaseProfilesByIds(List<Integer> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	private HttpResponse performGetRequestWithParams(String url, HttpParams params) {
		HttpGet request = new HttpGet(url);
		request.addHeader("Access_Token", this.token);
		
		// Execute and get the response.
		HttpResponse response = null;
		try {
			request.setParams(params);
			response = this.httpclient.execute(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	private JsonNode responseToJson(HttpResponse response) {
		HttpEntity entity = response.getEntity();

		// do something with the entity to get the token
		ObjectMapper mapper = new ObjectMapper();
		JsonNode responseNode = null;
		try {
			responseNode = mapper.readTree(EntityUtils.toString(entity));
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return responseNode;
	}
	
	private NmdProductCard getIdsFromJson(JsonNode cardInfo) {
		NmdProductCardImpl newCard = new NmdProductCardImpl();
		newCard.setRAWCode(cardInfo.get("ElementID").asText());
		newCard.setNLsfbCode(cardInfo.get("ElementCode").asText());
		newCard.setElementName(cardInfo.get("ElementNaam").asText());
		newCard.setDescription(cardInfo.get("FunctioneleBeschrijving").asText());
		if (cardInfo.has("FunctioneleEenheidID")) {			
			int unitid = cardInfo.get("FunctioneleEenheidID").asInt();
		}
		return newCard;
	}
}
