package org.opensourcebim.nmd;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bimserver.shared.reflector.KeyValuePair;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.opensourcebim.mpgcalculation.NmdMileuCategorie;

import com.fasterxml.jackson.core.JsonParseException;
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
	private NmdReferenceResources resources;

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

		loadResources();
	}

	@Override
	public void logout() {
		this.setToken("");
		this.isConnected = false;
		this.resources = null;
	}

	private void loadResources() {
		if (this.resources == null) {
			this.resources = this.getReferenceResources();
		}
	}

	public NmdReferenceResources getResources() {
		loadResources();
		return this.resources;
	}

	/*
	 * Get the reference resources to map database ids to meaningful fieldnames
	 */
	public NmdReferenceResources getReferenceResources() {
		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));
		NmdReferenceResources resources = new NmdReferenceResources();

		try {
			// lifecycle fasen
			HttpResponse faseResponse = this.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/Fasen",
					params);
			JsonNode fasen_node = this.responseToJson(faseResponse).get("results");
			HashMap<Integer, String> fasen = new HashMap<Integer, String>();
			fasen_node.forEach(fase -> {
				fasen.putIfAbsent(Integer.parseInt(fase.get("FaseID").asText()), fase.get("FaseNaam").asText());

			});
			resources.setFaseMapping(fasen);

			// milieucategorien
			HttpResponse categorieResponse = this
					.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/MilieuCategorien", params);
			JsonNode categorie_node = this.responseToJson(categorieResponse).get("results");
			HashMap<Integer, NmdMileuCategorie> categorien = new HashMap<Integer, NmdMileuCategorie>();
			categorie_node.forEach(categorie -> {
				NmdMileuCategorie factor = new NmdMileuCategorie(categorie.get("Milieueffect").asText(),
						categorie.get("Eenheid").asText(),
						categorie.get("Wegingsfactor") != null ? categorie.get("Wegingsfactor").asDouble() : 0.0);
				categorien.putIfAbsent(Integer.parseInt(categorie.get("MilieuCategorieID").asText()), factor);

			});
			resources.setMilieuCategorieMapping(categorien);

			// eenheden
			HttpResponse eenheidResponse = this
					.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/Eenheden", params);
			JsonNode eenheid_node = this.responseToJson(eenheidResponse).get("results");
			HashMap<Integer, String> eenheden = new HashMap<Integer, String>();
			eenheid_node.forEach(eenheid -> {

				eenheden.putIfAbsent(TryParseJsonNode(eenheid.get("EenheidID"), -1),
						TryParseJsonNode(eenheid.get("Code"), ""));

			});
			resources.setUnitMapping(eenheden);

			// rekenregels

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resources;
	}

	@Override
	public List<NmdProductCard> getAllProductSets() {

		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));

		try {
			HttpResponse response = this
					.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/NLsfB_RAW_Elementen", params);
			// do something with the entity to get the token
			JsonNode resp_node = this.responseToJson(response);

			// convert reponseNode to NmdProductCard info.
			List<NmdProductCard> results = new ArrayList<NmdProductCard>();
			resp_node.get("results").forEach(f -> results.add(this.getIdsFromJson(f)));

			return results;

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public List<NmdProductCard> getChildProductSetsForProductSet(NmdProductCard product) {
		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));
		params.add(new KeyValuePair("ElementID", product.getRAWCode()));

		try {
			HttpResponse response = this
					.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/ElementOnderdelen", params);

			// do something with the entity to get the token
			JsonNode resp_node = this.responseToJson(response);
			if (resp_node != null) {
				List<NmdProductCard> childCards = new ArrayList<NmdProductCard>();
				resp_node.get("results").forEach(res -> {
					childCards.add(getIdsFromJson(res));
				});

				return childCards;
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	@Override
	public Boolean getProfielSetsByProductCard(NmdProductCard product) {
		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));
		params.add(new KeyValuePair("ElementID", product.getRAWCode()));

		try {
			HttpResponse response = this
					.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/ProductenBijElement", params);

			// do something with the entity to get the token
			JsonNode resp_node = this.responseToJson(response);
			JsonNode profielsets = resp_node.get("results");
			
			// determine candidate sets that could be matched to this product
			List<NmdProfileSet> candidateSets = new ArrayList<NmdProfileSet>();
			profielsets.forEach(set -> {
				// only get items that are relevant for us.
				if(TryParseJsonNode(set.get("ProfielSetGekoppeld"), false)) {
					NmdProfileSetImpl profielSet = new NmdProfileSetImpl();
					profielSet.setProfielId(TryParseJsonNode(set.get("ProductID"), -1));
					profielSet.setParentProfielId(TryParseJsonNode(set.get("OuderProductID"), -1));
					profielSet.setIsFullProfile(TryParseJsonNode(set.get("IsElementDekkend"), false));
					profielSet.setName(TryParseJsonNode(set.get("ProductNaam"), ""));

					try {
						profielSet.setProductLifeTime(TryParseJsonNode(set.get("Levensduur"), 0));
					} catch (InvalidInputException e) {
						System.out.println("Could not find levensduur for profile");
						e.printStackTrace();
					}
					candidateSets.add(profielSet);
				}
			});
			
			// ToDo: recursively determine full set of profiles. For now just take the first one that has a profile and is 'dekkend'.
			// See issue BOUW-42
			Optional<NmdProfileSet> chosenSet = candidateSets.stream().findFirst();
			if (chosenSet.isPresent()) {
				NmdProfileSet fullSet = this.getAdditionalProfileDataForSet((NmdProfileSetImpl)chosenSet.get());
				product.addProfileSet(fullSet);
				return true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	/*
	 * Add missing data to the NmdProfileSet
	 */
	private NmdProfileSet getAdditionalProfileDataForSet(NmdProfileSetImpl chosenSet) {
		
		HashMap<Integer, NmdProfileSet> setData = getProfileSetsByIds(Arrays.asList(chosenSet.getProfielId().toString()));
		
		NmdProfileSet data = setData.entrySet().stream().findFirst().get().getValue();
		data.getDefinedProfiles().forEach(fase -> {
			chosenSet.addBasisProfiel(fase, data.getFaseProfiel(fase));
			chosenSet.setUnit(data.getUnit());
		});

		return chosenSet;
	}

	@Override
	public HashMap<Integer, NmdProfileSet> getProfileSetsByIds(List<String> ids) {
		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));
		params.add(new KeyValuePair("ProductIDs", String.join(",", ids)));
		params.add(new KeyValuePair("includeNULLs", true));

		try {
			HttpResponse response = this
					.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/ProductenProfielWaarden", params);

			// do something with the entity to get the token
			JsonNode resp_node = this.responseToJson(response);

			HashMap<Integer, NmdProfileSet> profileSets = new HashMap<Integer, NmdProfileSet>();

			JsonNode profielSetNodes = resp_node.get("results").get(0).get("ProfielSet");
			profielSetNodes.forEach(profielSetNode -> {

				NmdProfileSetImpl set = new NmdProfileSetImpl();
				Integer profielSetId = TryParseJsonNode(profielSetNode.get("ProfielSetID"), -1);
				
				// laad set specifieke data
				try {
					set.setProductLifeTime(TryParseJsonNode(profielSetNode.get("Levensduur"), -1));
					set.setUnit(this.getReferenceResources().getUnitMapping()
							.get(TryParseJsonNode(profielSetNode.get("ProfielSetEenheidID"), -1)));
					set.setName(TryParseJsonNode(profielSetNode, ""));
					set.setProfielId(profielSetId);
				} catch (InvalidInputException e) {
					System.out.println("error parsing general profielset data");
				}

				// laad faseprofiel specifieke data
				JsonNode profielen = profielSetNode.get("Profiel");
				profielen.forEach(p -> {
					Integer category = TryParseJsonNode(p.get("CategorieID"), -1);
					Integer fase = TryParseJsonNode(p.get("FaseID"), -1);
					String faseName = this.getResources().getFaseMapping().get(fase);
					
					NmdFaseProfielImpl profiel = new NmdFaseProfielImpl(
							faseName,
							this.getResources());

					profiel.setCategory(category);

					p.get("ProfielMilieuEffecten").forEach(val -> {
						Integer catId = TryParseJsonNode(val.get("MilieuCategorieID"), -1);
						Double catVal = TryParseJsonNode(val.get("MilieuWaarde"), Double.NaN);

						profiel.setProfielCoefficient(
								this.getResources().getMilieuCategorieMapping().get(catId).getDescription(), catVal);
					});

					set.addBasisProfiel(faseName, profiel);

				});
				profileSets.put(profielSetId, set);
			});

			return profileSets;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private HttpResponse performGetRequestWithParams(String url, List<KeyValuePair> params) throws URISyntaxException {
		URIBuilder builder = new URIBuilder();
		builder.setScheme("https").setHost("www.Milieudatabase-datainvoer.nl").setPath(url);

		params.forEach(p -> builder.addParameter(p.getFieldName(), p.getValue().toString()));

		URI uri = builder.build();

		HttpGet request = new HttpGet(uri);
		request.addHeader("Access_Token", this.token);

		// Execute and get the response.
		HttpResponse response = null;
		try {
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

		if (cardInfo.has("FunctioneleBeschrijving")) {
			newCard.setDescription(cardInfo.get("FunctioneleBeschrijving").asText());
		}

		if (cardInfo.has("FunctioneleEenheidID")) {
			int unitid = cardInfo.get("FunctioneleEenheidID").asInt();
		}
		return newCard;
	}

	private Integer TryParseJsonNode(JsonNode node, Integer defaultValue) {
		return (node == null) ? defaultValue : node.asInt(defaultValue);
	}

	private Double TryParseJsonNode(JsonNode node, Double defaultValue) {
		return (node == null) ? defaultValue : node.asDouble(defaultValue);
	}

	private String TryParseJsonNode(JsonNode node, String defaultValue) {
		return (node == null) ? defaultValue : node.asText(defaultValue);
	}
	
	private Boolean TryParseJsonNode(JsonNode node, Boolean defaultValue) {
		return (node == null) ? defaultValue : node.asBoolean(defaultValue);
	}
}
