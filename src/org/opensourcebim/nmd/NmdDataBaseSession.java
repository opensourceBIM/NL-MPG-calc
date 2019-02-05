package org.opensourcebim.nmd;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.bimserver.shared.reflector.KeyValuePair;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.opensourcebim.mpgcalculation.NmdMileuCategorie;
import org.opensourcebim.nmd.scaling.NmdScaler;
import org.opensourcebim.nmd.scaling.NmdScalerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Implementation of the NmdDataService to retrieve data from the NMD see :
 * https://www.milieudatabase.nl/ for mor information
 * 
 * @author vijj
 *
 */
public class NmdDataBaseSession extends AuthorizedDatabaseSession implements NmdDataService {

	private static final DateFormat dbDateFormat = new SimpleDateFormat("yyyyMMdd"); // according to docs thi should be
																						// correct
	private Calendar requestDate;
	private NmdDatabaseConfig config = null;
	private boolean isConnected = false;
	private NmdReferenceResources resources;

	NmdScalerFactory scalerFactor = new NmdScalerFactory();

	private List<NmdProductCard> data;

	public NmdDataBaseSession(NmdDatabaseConfig config) {
		this.setHost("www.Milieudatabase-datainvoer.nl") ;
		this.config = config;
		this.requestDate = Calendar.getInstance();
		this.data = new ArrayList<NmdProductCard>();
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
	public List<NmdProductCard> getData() {
		return data;
	}

	@Override
	public void login() {
		// Try add a Post request to the base class.
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
			response = httpClient.execute(httppost);
			JsonNode responseNode = this.responseToJson(response);
			JsonNode tokenNode = responseNode.get("TOKEN");
			this.setToken(tokenNode.asText());

			this.isConnected = true;
			httppost.releaseConnection();
		} catch (IOException e1) {
			this.isConnected = false;
			System.out.println("authentication failed: " + response.getStatusLine().toString());
		}

		loadResources();
	}

	@Override
	public void preLoadData() {
		this.data = this.getAllProductSets();
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
		resources = new NmdReferenceResources();

		try {
			loadLifeCycleFasen(params);

			loadMilieuCategorien(params);

			loadUnitDefinitions(params);

			loadCUASCodes(params);

			loadScalingTypes(params);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resources;
	}

	private void loadScalingTypes(List<KeyValuePair> params) {
		// rekenregels
		HttpResponse scalingResponse = this.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/SchalingsFormules", params);
		JsonNode scaling_node = this.responseToJson(scalingResponse).get("results");
		HashMap<Integer, String> scalingFormula = new HashMap<Integer, String>();
		scaling_node.forEach(scaling_formula -> {

			scalingFormula.putIfAbsent(TryParseJsonNode(scaling_formula.get("SchalingsFormuleID"), -1),
					TryParseJsonNode(scaling_formula.get("SoortFormule"), "") + " : "
							+ TryParseJsonNode(scaling_formula.get("Formule"), ""));

		});
		resources.setScalingFormula(scalingFormula);

	}

	private void loadCUASCodes(List<KeyValuePair> params) {
		// cuasCodes
		HttpResponse cuasResponse = this
				.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/CUAScategorien", params);
		JsonNode cuas_node = this.responseToJson(cuasResponse).get("results");
		HashMap<Integer, String> cuasCategorien = new HashMap<Integer, String>();
		cuas_node.forEach(cuas_code -> {

			cuasCategorien.putIfAbsent(TryParseJsonNode(cuas_code.get("ID"), -1),
					TryParseJsonNode(cuas_code.get("CUAS_code"), ""));

		});
		resources.setCuasCategorieMapping(cuasCategorien);

	}

	private void loadUnitDefinitions(List<KeyValuePair> params) {
		// eenheden
		HttpResponse eenheidResponse = this.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/Eenheden",
				params);
		JsonNode eenheid_node = this.responseToJson(eenheidResponse).get("results");
		HashMap<Integer, String> eenheden = new HashMap<Integer, String>();
		eenheid_node.forEach(eenheid -> {

			eenheden.putIfAbsent(TryParseJsonNode(eenheid.get("EenheidID"), -1),
					TryParseJsonNode(eenheid.get("Code"), ""));

		});
		resources.setUnitMapping(eenheden);
	}

	private void loadMilieuCategorien(List<KeyValuePair> params) {
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
	}

	private void loadLifeCycleFasen(List<KeyValuePair> params) {
		// lifecycle fasen
		HttpResponse faseResponse = this.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/Fasen",
				params);
		JsonNode fasen_node = this.responseToJson(faseResponse).get("results");
		HashMap<Integer, String> fasen = new HashMap<Integer, String>();
		fasen_node.forEach(fase -> {
			fasen.putIfAbsent(Integer.parseInt(fase.get("FaseID").asText()), fase.get("FaseNaam").asText());

		});
		resources.setFaseMapping(fasen);
	}

	@Override
	public List<NmdProductCard> getAllProductSets() {

		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));

		HttpResponse response = this
				.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/NLsfB_RAW_Elementen", params);
		// do something with the entity to get the token
		JsonNode resp_node = this.responseToJson(response);

		// convert reponseNode to NmdProductCard info.
		List<NmdProductCard> results = new ArrayList<NmdProductCard>();
		resp_node.get("results").forEach(f -> results.add(this.getProductCardIdsFromJson(f)));

		return results;
	}

	@Override
	public List<NmdProductCard> getChildProductSetsForProductSet(NmdProductCard product) {
		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));
		params.add(new KeyValuePair("ElementID", product.getRAWCode()));

		HttpResponse response = this.performGetRequestWithParams("/NMD_30_API_v0.2/api/NMD30_Web_API/ElementOnderdelen",
				params);

		// do something with the entity to get the token
		JsonNode resp_node = this.responseToJson(response);
		List<NmdProductCard> childCards = new ArrayList<NmdProductCard>();
		if (resp_node != null) {

			resp_node.get("results").forEach(res -> {
				childCards.add(getProductCardIdsFromJson(res));
			});
		}
		return childCards;
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
			if (resp_node == null) {
				return false;
			}

			JsonNode profielsets = resp_node.get("results");

			// determine candidate sets that could be matched to this product
			List<NmdProfileSet> candidateSets = new ArrayList<NmdProfileSet>();
			profielsets.forEach(set -> {
				// only get items that are relevant for us.
				if (TryParseJsonNode(set.get("ProfielSetGekoppeld"), false)) {
					NmdProfileSetImpl profielSet = new NmdProfileSetImpl();
					this.getProfielSetDataFromJson(set, profielSet);
					candidateSets.add(profielSet);
				}
			});

			// ToDo: recursively determine full set of profiles. For now just take the first
			// one that has a profile and is 'dekkend'.
			// See issue BOUW-42
			Optional<NmdProfileSet> chosenSet = candidateSets.stream().findFirst();
			if (chosenSet.isPresent()) {
				NmdProfileSet fullSet = this.getAdditionalProfileDataForSet((NmdProfileSetImpl) chosenSet.get());
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

		HashMap<Integer, NmdProfileSet> setData = getProfileSetsByIds(
				Arrays.asList(chosenSet.getProfielId().toString()));

		NmdProfileSet data = setData.entrySet().stream().findFirst().get().getValue();
		data.getDefinedProfiles().forEach(fase -> {
			chosenSet.addFaseProfiel(fase, data.getFaseProfiel(fase));
		});

		return chosenSet;
	}

	@Override
	public HashMap<Integer, NmdProfileSet> getProfileSetsByIds(List<String> ids) {
		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));
		params.add(new KeyValuePair("ProductIDs", String.join(",", ids)));
		params.add(new KeyValuePair("includeNULLs", true));

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
			this.getProfielSetDataFromJson(profielSetNode, set);

			// laad faseprofiel specifieke data
			JsonNode profielen = profielSetNode.get("Profiel");
			profielen.forEach(p -> {
				Integer category = TryParseJsonNode(p.get("CategorieID"), -1);
				Integer fase = TryParseJsonNode(p.get("FaseID"), -1);
				String faseName = this.getResources().getFaseMapping().get(fase);

				NmdFaseProfielImpl profiel = new NmdFaseProfielImpl(faseName, this.getResources());

				p.get("ProfielMilieuEffecten").forEach(val -> {
					Integer catId = TryParseJsonNode(val.get("MilieuCategorieID"), -1);
					Double catVal = TryParseJsonNode(val.get("MilieuWaarde"), Double.NaN);

					profiel.setProfielCoefficient(
							this.getResources().getMilieuCategorieMapping().get(catId).getDescription(), catVal);
				});

				set.addFaseProfiel(faseName, profiel);

			});
			profileSets.put(profielSetId, set);
		});

		return profileSets;
	}

	/*
	 * Get the essential product card info from an element JsonNode. With this info
	 * profielSet info can be retrieved
	 */
	private NmdProductCard getProductCardIdsFromJson(JsonNode cardInfo) {
		NmdProductCardImpl newCard = new NmdProductCardImpl();
		newCard.setRAWCode(cardInfo.get("ElementID").asText());
		newCard.setNLsfbCode(cardInfo.get("ElementCode").asText());
		newCard.setElementName(cardInfo.get("ElementNaam").asText());

		newCard.setIsTotaalProduct(TryParseJsonNode(cardInfo.get("OuderID"), -1) == 0);

		if (cardInfo.has("FunctioneleBeschrijving")) {
			newCard.setDescription(cardInfo.get("FunctioneleBeschrijving").asText());
		}

		return newCard;
	}

	/*
	 * Try to get a set of profiel set data from the json node to populate the
	 * NmdProfielSetobject
	 */
	private void getProfielSetDataFromJson(JsonNode profielSetNode, NmdProfileSetImpl set) {
		set.setProductLifeTime(TryParseJsonNode(profielSetNode.get("Levensduur"), -1));
		set.setUnit(this.getReferenceResources().getUnitMapping()
				.get(TryParseJsonNode(profielSetNode.get("FunctioneleEenheidID"), -1)));
		set.setProfielId(TryParseJsonNode(profielSetNode.get("ProductID"), -1));
		set.setParentProfielId(TryParseJsonNode(profielSetNode.get("OuderProductID"), -1));
		set.setIsFullProfile(TryParseJsonNode(profielSetNode.get("IsElementDekkend"), false));
		set.setName(TryParseJsonNode(profielSetNode.get("ProductNaam"), ""));
		set.setCategory(TryParseJsonNode(profielSetNode.get("CategorieID"), 3));

		// two types of profile set nodes.
		if (profielSetNode.get("IsSchaalbaar") != null) {
			set.setIsScalable(TryParseJsonNode(profielSetNode.get("IsSchaalbaar"), false));
		} else {
			int scalerType = TryParseJsonNode(profielSetNode.get("SchalingsFormuleID"), -1);
			if (scalerType > 0) {
				set.setIsScalable(true);
				double scalerDim1 = TryParseJsonNode(profielSetNode.get("SchalingsMaatX1"), Double.NaN);
				double scalerDim2 = TryParseJsonNode(profielSetNode.get("SchalingsMaatX2"), Double.NaN);
				int scalerUnit = TryParseJsonNode(profielSetNode.get("EenheidID_SchalingsMaat"), -1);
				double scalerCoeffA = TryParseJsonNode(profielSetNode.get("SchalingA"), Double.NaN);
				double scalerCoeffB = TryParseJsonNode(profielSetNode.get("SchalingB"), Double.NaN);
				double scalerCoeffC = TryParseJsonNode(profielSetNode.get("SchalingC"), Double.NaN);
				double scalerMinDim1 = TryParseJsonNode(profielSetNode.get("MinX1"), Double.NaN);
				double scalerMinDim2 = TryParseJsonNode(profielSetNode.get("MinX2"), Double.NaN);
				double scalerMaxDim1 = TryParseJsonNode(profielSetNode.get("MaxX1"), Double.NaN);
				double scalerMaxDim2 = TryParseJsonNode(profielSetNode.get("MaxX2"), Double.NaN);

				String scalerTypeName = this.getReferenceResources().getScalingFormula().get(scalerType);
				String scalerUnitName = this.getReferenceResources().getUnitMapping().get(scalerUnit);
				NmdScaler scaler;
				try {
					scaler = scalerFactor.create(scalerTypeName, scalerUnitName,
							new Double[] { scalerCoeffA, scalerCoeffB, scalerCoeffC },
							new Double[] { scalerMinDim1, scalerMaxDim1, scalerMinDim2 , scalerMaxDim2 },
							new Double[] { scalerDim1, scalerDim2});
					set.setScaler(scaler);
				} catch (InvalidInputException e) {
					System.out.println("encountered invalid input combinations in scaler creation");
				}



			}
		}
	}
}
