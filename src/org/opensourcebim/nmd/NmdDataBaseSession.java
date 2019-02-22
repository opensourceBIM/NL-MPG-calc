package org.opensourcebim.nmd;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.bimserver.shared.reflector.KeyValuePair;
import org.eclipse.jdt.core.compiler.InvalidInputException;
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

	private static final DateFormat dbDateFormat = new SimpleDateFormat("yyyyMMdd");
	private String apiPath;
	private Calendar requestDate;
	private NmdDatabaseConfig config = null;
	private boolean isConnected = false;
	private NmdReferenceResources resources;

	NmdScalerFactory scalerFactor = new NmdScalerFactory();

	private List<NmdElement> data;

	public NmdDataBaseSession(NmdDatabaseConfig config) {
		this.setHost("www.Milieudatabase-datainvoer.nl");
		this.apiPath = "/NMD_30_API_v0.2/api/NMD30_Web_API/";
		this.config = config;
		this.requestDate = Calendar.getInstance();
		this.data = new ArrayList<NmdElement>();
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
	public List<NmdElement> getData() {
		return data;
	}

	@SuppressWarnings("null")
	@Override
	public void login() {
		// Try add a Post request to the base class.
		HttpPost httppost = new HttpPost("https://www.milieudatabase-datainvoer.nl/NMD_30_AuthenticationServer/"
				+ "NMD_30_API_Authentication/getToken");

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
		this.data = this.getAllElements();

		this.data.forEach(el -> el.addProductCards(getProductsForElement(el)));
	}

	@Override
	public void logout() {
		this.setToken("");
		this.isConnected = false;
		this.resources = null;
	}

	private void loadResources() {
		if (this.resources == null) {
			this.resources = this.loadReferenceResources();
		}
	}

	public NmdReferenceResources getResources() {
		loadResources();
		return this.resources;
	}

	/*
	 * Get the reference resources to map database ids to meaningful fieldnames
	 */
	public NmdReferenceResources loadReferenceResources() {
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
		// rekenregels reference data
		HttpResponse scalingResponse = this.performGetRequestWithParams(apiPath + "SchalingsFormules", params);
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
		// cuasCodes reference data
		HttpResponse cuasResponse = this.performGetRequestWithParams(this.apiPath + "CUAScategorien", params);
		JsonNode cuas_node = this.responseToJson(cuasResponse).get("results");
		HashMap<Integer, String> cuasCategorien = new HashMap<Integer, String>();
		cuas_node.forEach(cuas_code -> {

			cuasCategorien.putIfAbsent(TryParseJsonNode(cuas_code.get("ID"), -1),
					TryParseJsonNode(cuas_code.get("CUAS_code"), ""));

		});
		resources.setCuasCategorieMapping(cuasCategorien);
	}

	private void loadUnitDefinitions(List<KeyValuePair> params) {
		// eenheden reference data
		HttpResponse eenheidResponse = this.performGetRequestWithParams(this.apiPath + "Eenheden", params);
		JsonNode eenheid_node = this.responseToJson(eenheidResponse).get("results");
		HashMap<Integer, String> eenheden = new HashMap<Integer, String>();
		eenheid_node.forEach(eenheid -> {

			eenheden.putIfAbsent(TryParseJsonNode(eenheid.get("EenheidID"), -1),
					TryParseJsonNode(eenheid.get("Code"), ""));

		});
		resources.setUnitMapping(eenheden);
	}

	private void loadMilieuCategorien(List<KeyValuePair> params) {
		// milieucategorien reference data
		HttpResponse categorieResponse = this.performGetRequestWithParams(this.apiPath + "MilieuCategorien", params);
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
		// lifecycle fasen reference data
		HttpResponse faseResponse = this.performGetRequestWithParams(this.apiPath + "Fasen", params);
		JsonNode fasen_node = this.responseToJson(faseResponse).get("results");
		HashMap<Integer, String> fasen = new HashMap<Integer, String>();
		fasen_node.forEach(fase -> {
			fasen.putIfAbsent(Integer.parseInt(fase.get("FaseID").asText()), fase.get("FaseNaam").asText());

		});
		resources.setFaseMapping(fasen);
	}

	@Override
	public List<NmdElement> getAllElements() {

		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));

		HttpResponse response = this.performGetRequestWithParams(this.apiPath + "NLsfB_RAW_Elementen", params);
		// do something with the entity to get the token
		JsonNode respNode = this.responseToJson(response);

		// convert reponseNode to NmdProductCard info.
		List<NmdElement> results = new ArrayList<NmdElement>();
		respNode.get("results").forEach(f -> results.add(this.getElementDataFromJson(f)));
		List<Integer> ids = results.stream().map(e -> e.getElementId()).collect(Collectors.toList());
		results.addAll(getChildElements(ids));

		return results;
	}
	
	private List<NmdElement> getChildElements(List<Integer> parentIds) {
		
		List<NmdElement> results = new ArrayList<NmdElement>();
		
		parentIds.forEach(id -> {
			
			List<KeyValuePair> params_el = new ArrayList<KeyValuePair>();
			params_el.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));
			params_el.add(new KeyValuePair("ElementID", id));
			
			// load the json
			HttpResponse responseEl = this.performGetRequestWithParams(this.apiPath + "ElementOnderdelen", params_el);
			JsonNode respNodeEl = this.responseToJson(responseEl);
			if (respNodeEl != null) {
				// parse json to objects
				List<NmdElement> newResults = new ArrayList<NmdElement>();
				respNodeEl.get("results").forEach(f -> newResults.add(this.getElementDataFromJson(f)));
				results.addAll(newResults);
				
				// repeat above process for new finds.
				List<Integer> newResultIds = newResults.stream().map(r -> r.getElementId()).collect(Collectors.toList());
				results.addAll(getChildElements(newResultIds));
			}
		});
		
		return results;
	}

	@Override
	public List<NmdProductCard> getProductsForElement(NmdElement element) {
		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));
		params.add(new KeyValuePair("ElementID", element.getElementId()));

		HttpResponse response = this.performGetRequestWithParams(this.apiPath + "ProductenBijElement", params);

		JsonNode resp_node = this.responseToJson(response);
		if (resp_node == null) {
			return new ArrayList<NmdProductCard>();
		}

		JsonNode producten = resp_node.get("results");
		List<NmdProductCard> products = new ArrayList<NmdProductCard>();

		if (producten != null) {
			producten.forEach(p -> {
				// only get items that are relevant for us.
				if (TryParseJsonNode(p.get("ProfielSetGekoppeld"), false)) {
					products.add(this.getProductCardDataFromJson(p));
				}
			});
		}
		return products;
	}
	
	/**
	 * Quick lookup for preloaded product cards
	 */
	@Override
	public List<NmdProductCard> getProductsForNLsfbCodes(Set<String> codes) {
		if (getData().size() == 0) {
			preLoadData();
		}
				
		List<NmdProductCard> res = getElementsForNLsfbCodes(codes).stream()
				.flatMap(el -> el.getProducts().stream()).collect(Collectors.toList());
		
		return res;
	}
	
	/**
	 * Quick lookup for preloaded elements
	 */
	@Override
	public List<NmdElement> getElementsForNLsfbCodes(Set<String> codes) {
		if (getData().size() == 0) {
			preLoadData();
		}
				
		return getData().stream()
				.filter(el -> codes.stream()
						.anyMatch(code -> code == null ? false : code.equals(el.getNLsfbCode())))
				.collect(Collectors.toList());
	}

	@Override
	public HashMap<Integer, Double> getQuantitiesOfProfileSetsForProduct(Integer productId) {
		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));
		params.add(new KeyValuePair("ProductID", productId.toString()));
		params.add(new KeyValuePair("includeNULLs", true));

		HttpResponse response = this.performGetRequestWithParams(this.apiPath + "ProfielsetsEnSchalingBijProduct",
				params);

		HashMap<Integer, Double> res = new HashMap<Integer, Double>();
		// do something with the entity to get the token
		JsonNode resp_node = this.responseToJson(response);

		JsonNode psNodes = resp_node.get("results");
		if (psNodes != null) {
			psNodes.forEach(ps -> {
				Integer id = ps.get("ProfielSetID").asInt(-1);
				Double q = ps.get("Hoeveelheid").asDouble(1.0);
				q = q > 0.0 ? q : 1.0;
				res.put(id, q);
			});
		}

		return res;
	}

	/*
	 * Add missing data to the NmdProfileSet
	 */
	@Override
	public Boolean getAdditionalProfileDataForCard(NmdProductCard c) {

		try {
			HashMap<Integer, NmdProfileSet> setData = getProfileSetsByIds(Arrays.asList(c.getProductId()));

			c.addProfileSets(setData.values());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public HashMap<Integer, NmdProfileSet> getProfileSetsByIds(List<Integer> ids) {
		String idsString = String.join(",", ids.stream().map(id -> id.toString()).collect(Collectors.toList()));
		List<KeyValuePair> params = new ArrayList<KeyValuePair>();
		params.add(new KeyValuePair("ZoekDatum", dbDateFormat.format(this.getRequestDate().getTime())));
		params.add(new KeyValuePair("ProductIDs", idsString));
		params.add(new KeyValuePair("includeNULLs", true));

		HttpResponse response = this.performGetRequestWithParams(this.apiPath + "ProductenProfielWaarden", params);

		// do something with the entity to get the token
		JsonNode resp_node = this.responseToJson(response);

		HashMap<Integer, NmdProfileSet> profileSets = new HashMap<Integer, NmdProfileSet>();

		if (resp_node.get("results") != null) {
			JsonNode profielSetNodes = resp_node.get("results").get(0).get("ProfielSet");
			profielSetNodes.forEach(profielSetNode -> {

				Integer profielSetId = TryParseJsonNode(profielSetNode.get("ProfielSetID"), -1);

				// load set specific data
				NmdProfileSet set = this.getDetailedProfielSetData(profielSetNode);

				profileSets.put(profielSetId, set);
			});
		}

		return profileSets;
	}

	private void loadFaseProfielDataForSet(JsonNode profielSetNode, NmdProfileSetImpl set) {
		// laad faseprofiel specifieke data
		JsonNode profielen = profielSetNode.get("Profiel");
		if (profielen != null) {
			profielen.forEach(p -> {
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
		}
	}

	private NmdProfileSetImpl getDetailedProfielSetData(JsonNode psNode) {
		NmdProfileSetImpl set = new NmdProfileSetImpl();
		set.setProfileLifetime(TryParseJsonNode(psNode.get("Levensduur"), -1));
		set.setUnit(this.getResources().getUnitMapping().get(TryParseJsonNode(psNode.get("ProfielSetEenheidID"), -1)));

		set.setQuantity(TryParseJsonNode(psNode.get("Hoeveelheid"), 1));
		set.setProfielId(TryParseJsonNode(psNode.get("ProfielSetID"), -1));
		set.setName(TryParseJsonNode(psNode.get("ProfielSetNaam"), ""));

		int scalerType = TryParseJsonNode(psNode.get("SchalingsFormuleID"), -1);
		if (scalerType > 0) {
			set.setIsScalable(true);
			double scalerDim1 = TryParseJsonNode(psNode.get("SchalingsMaatX1"), Double.NaN);
			double scalerDim2 = TryParseJsonNode(psNode.get("SchalingsMaatX2"), Double.NaN);
			int scalerUnit = TryParseJsonNode(psNode.get("EenheidID_SchalingsMaat"), -1);
			double scalerCoeffA = TryParseJsonNode(psNode.get("SchalingA"), Double.NaN);
			double scalerCoeffB = TryParseJsonNode(psNode.get("SchalingB"), Double.NaN);
			double scalerCoeffC = TryParseJsonNode(psNode.get("SchalingC"), Double.NaN);
			double scalerMinDim1 = TryParseJsonNode(psNode.get("MinX1"), Double.NaN);
			double scalerMinDim2 = TryParseJsonNode(psNode.get("MinX2"), Double.NaN);
			double scalerMaxDim1 = TryParseJsonNode(psNode.get("MaxX1"), Double.NaN);
			double scalerMaxDim2 = TryParseJsonNode(psNode.get("MaxX2"), Double.NaN);

			String scalerTypeName = this.getResources().getScalingFormula().get(scalerType);
			String scalerUnitName = this.getResources().getUnitMapping().get(scalerUnit);
			NmdScaler scaler;
			try {
				scaler = scalerFactor.create(scalerTypeName, scalerUnitName,
						new Double[] { scalerCoeffA, scalerCoeffB, scalerCoeffC },
						new Double[] { scalerMinDim1, scalerMaxDim1, scalerMinDim2, scalerMaxDim2 },
						new Double[] { scalerDim1, scalerDim2 });
				set.setScaler(scaler);
			} catch (InvalidInputException e) {
				System.out.println("encountered invalid input combinations in scaler creation");
			}
		} else {
			set.setIsScalable(false);
		}

		this.loadFaseProfielDataForSet(psNode, set);

		return set;
	}

	/*
	 * Try to get a set of profiel set data from the json node to populate the
	 * NmdProfielSetobject
	 */
	private NmdProductCardImpl getProductCardDataFromJson(JsonNode prodNode) {
		NmdProductCardImpl prod = new NmdProductCardImpl();

		prod.setLifetime(TryParseJsonNode(prodNode.get("Levensduur"), -1));
		prod.setUnit(
				this.getResources().getUnitMapping().get(TryParseJsonNode(prodNode.get("FunctioneleEenheidID"), -1)));
		prod.setProductId(TryParseJsonNode(prodNode.get("ProductID"), -1));
		prod.setParentProductId(TryParseJsonNode(prodNode.get("OuderProductID"), -1));

		prod.setIsTotaalProduct(TryParseJsonNode(prodNode.get("IsElementDekkend"), false));
		prod.setDescription(TryParseJsonNode(prodNode.get("ProductNaam"), ""));
		prod.setCategory(TryParseJsonNode(prodNode.get("CategorieID"), 3));
		prod.setIsScalable(TryParseJsonNode(prodNode.get("IsSchaalbaar"), false));

		// TODO add product attributes

		return prod;
	}

	/*
	 * Get the essential product card info from an element JsonNode. With this info
	 * profielSet info can be retrieved
	 */
	private NmdElement getElementDataFromJson(JsonNode elementInfo) {
		NmdElementImpl newElement = new NmdElementImpl();
		newElement.setElementId(TryParseJsonNode(elementInfo.get("ElementID"), -1));
		newElement.setNlsfbCode(TryParseJsonNode(elementInfo.get("ElementCode"), ""));
		newElement.setElementName(TryParseJsonNode(elementInfo.get("ElementNaam"), ""));
		newElement.setParentId(TryParseJsonNode(elementInfo.get("OuderID"), -1));
		newElement.setIsMandatory(TryParseJsonNode(elementInfo.get("Verplicht"), false));
		
		return newElement;
	}
}
