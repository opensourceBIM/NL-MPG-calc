package org.opensourcebim.nmd;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.opensourcebim.ifccollection.MaterialSource;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgGeometry;
import org.opensourcebim.ifccollection.MpgLayer;
import org.opensourcebim.ifccollection.MpgLayerImpl;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.MpgObjectStoreImpl;
import org.opensourcebim.ifccollection.MpgSpaceImpl;
import org.opensourcebim.mapping.NlsfbCode;
import org.opensourcebim.nmd.scaling.NmdLinearScaler;

public class ObjectStoreBuilder {
	private MpgObjectStoreImpl store;
	private int lastIdCreated;

	public ObjectStoreBuilder() {
		this.setStore(new MpgObjectStoreImpl());
		this.getStore().setProjectId(1L);
		this.getStore().setRevisionId(1L);
		lastIdCreated = 0;
	}

	private Integer getNewUniqueId() {
		return lastIdCreated++;
	}

	private String getUUID() {
		return UUID.randomUUID().toString();
	}

	/**
	 * add element for calculation purposes (not for resolving as it is missing
	 * relevant info for that purpose.
	 * 
	 * @param ifcMatName
	 * @param nmdMatName
	 * @param unit
	 * @param category
	 * @param lifetime
	 */
	public void addMappedMpgElement(String ifcMatName, String nmdMatName, String unit, int category, int lifetime) {
		MpgElement el = getStore().addElement(ifcMatName);
		addUnitIfcObjectForElement(ifcMatName, 1.0, 1.0);
		NmdElement nmdEl = this.createDummyNmdElement(nmdMatName, "99.99", 0);
		NmdProductCard card = createDummyProductCard(nmdMatName, category, unit, lifetime, nmdEl);
		card.addProfileSet(createUnitProfileSet(nmdMatName, "kg", lifetime, 1.0));

		el.mapProductCard(new MaterialSource(getNewUniqueId().toString(), ifcMatName, "builder"), card);
	}

	/**
	 * Add element that will still need to be mapped. contains a single MpgObject
	 * and any information needed for mapping
	 * @param ifcName - the description of the IfcProduct
	 * @param createLayers flag to indicate whether to create a layer for each material
	 * @param ifcMatSpecs map with name and volume ratio of each material spec
	 * @param dims dimensions of the IfcProduct
	 * @param nlsfb nlsfb code 
	 * @param type IfcProduct type (IfcWall, IfcSlab etc.)
	 * @param parentUUID guid of the object it should be decomposed by in the IfcProject file
	 * @return an Mpgelement object with above properties
	 */
	public MpgElement AddUnmappedMpgElement(String ifcName, Boolean createLayers, Map<String, Double> ifcMatSpecs,
			Double[] dims, String nlsfb, String type, String parentUUID) {
		MpgGeometry geom = this.createDummyGeom(dims[0], dims[1], dims[2]);
		MpgElement el = getStore().addElement(ifcName);
		MpgObjectImpl obj = new MpgObjectImpl();
		obj.setObjectName(ifcName);
		obj.setGeometry(geom);
		obj.setNLsfbCode(nlsfb);
		obj.setGlobalId(getUUID());
		obj.setObjectType(type);
		obj.setParentId(parentUUID);

		ifcMatSpecs.forEach((name, ratio) -> {
			String id = getUUID();
			if (createLayers) {
				obj.addLayer(new MpgLayerImpl(geom.getVolume() * ratio, geom.getFaceArea(), name, id));
				obj.addMaterialSource(new MaterialSource(id, name, "layer"));
			} else {
				obj.addMaterialSource(name, id, "direct");
			}
		});

		this.getStore().addObject(obj);
		el.setMpgObject(obj);

		return el;
	}

	public void addUnitIfcObjectForElement(String ifcMatName, double volume, double area) {
		MpgObjectImpl mpgObject = new MpgObjectImpl(1, UUID.randomUUID().toString(), ifcMatName + " element", "Slab",
				"");
		mpgObject.setGeometry(createDummyGeom(1, 1, 1));

		MpgLayer testObject = new MpgLayerImpl(volume, area, ifcMatName, Integer.toString(ifcMatName.hashCode()));
		mpgObject.addLayer(testObject);

		getStore().addObject(mpgObject);

		getStore().setObjectForElement(ifcMatName, mpgObject);
	}

	public MpgGeometry createDummyGeom(double x, double y, double z) {
		MpgGeometry geom = new MpgGeometry();
		geom.setDimensions(x, y, z);
		geom.setVolume(x * y * z);
		geom.setFloorArea(x * y);
		geom.setIsComplete(true);
		return geom;
	}

	/**
	 * Create some random space with set floor area and height
	 * 
	 * @param floorArea
	 * @param height
	 */
	public void addSpace(Double floorArea, Double height) {
		getStore().addSpace(new MpgSpaceImpl(getUUID(), floorArea * 3, floorArea));
	}

	// NMD OBJECT CREATION METHODS

	/**
	 * Create an empty nmd element that has no product cards linked to it.
	 * 
	 * @param name
	 * @param nlsfbCode
	 * @param parentId
	 * @return
	 */
	public NmdElement createDummyNmdElement(String name, String nlsfbCode, int parentId) {
		NmdElementImpl el = new NmdElementImpl();
		el.setNlsfbCode(new NlsfbCode(nlsfbCode));
		el.setElementId(getNewUniqueId());
		el.setParentId(parentId);
		el.setElementName(name);
		el.setIsMandatory(true);
		return el;
	}

	public NmdProductCardImpl createDummyProductCard(String description, int category, String unit, int lifetime,
			NmdElement el) {
		NmdProductCardImpl card = new NmdProductCardImpl();
		card.setProductId(getNewUniqueId());
		card.setCategory(category);
		card.setDescription(description);
		card.setIsScalable(true);
		card.setIsTotaalProduct(false);
		card.setLifetime(lifetime);
		if (el != null) {
			card.setNlsfbCode(el.getNlsfbCode());
		}
		card.setParentProductId(0);
		card.setUnit(unit);
		return card;
	}

	public NmdProfileSet createUnitProfileSet(String name, String unit, int lifetime, double quantityPerProductCard) {
		NmdProfileSetImpl spec = new NmdProfileSetImpl();

		spec.setProfileLifetime(lifetime);
		spec.addFaseProfiel("TransportToSite", createUnitProfile("TransportToSite"));
		spec.addFaseProfiel("ConstructionAndReplacements", createUnitProfile("ConstructionAndReplacements"));
		spec.addFaseProfiel("Disposal", createUnitProfile("Disposal"));
		spec.addFaseProfiel("Incineration", createUnitProfile("Incineration"));
		spec.addFaseProfiel("Recycling", createUnitProfile("Recycling"));
		spec.addFaseProfiel("Reuse", createUnitProfile("Reuse"));
		spec.addFaseProfiel("OwnDisposalProfile", createUnitProfile("OwnDisposalProfile"));
		spec.addFaseProfiel("TransportForRemoval", createUnitProfile("TransportForRemoval"));
		spec.addFaseProfiel("Operation", createUnitProfile("Operation"));

		spec.setUnit(unit);
		spec.setProfielId(getNewUniqueId());
		spec.setIsScalable(true);
		spec.setQuantity(quantityPerProductCard);

		// as a dummy add a scaler that will do no adjustment
		spec.setScaler(new NmdLinearScaler("m", new Double[] { 1.0, 0.0, 0.0 },
				new Double[] { 0.0, Double.POSITIVE_INFINITY, Double.NaN, Double.NaN },
				new Double[] { 1.0, Double.NaN }));
		spec.setName(name);

		return spec;
	}

	public NmdFaseProfiel createUnitProfile(String fase) {
		NmdFaseProfielImpl profile = new NmdFaseProfielImpl(fase, this.getDummyReferences());
		profile.setAll(1.0);
		return profile;
	}

	public MpgObjectImpl createDummyObject(String name, String type, String parentId, String nlsfb) {
		MpgObjectImpl obj = new MpgObjectImpl(getNewUniqueId(), UUID.randomUUID().toString(), name, type, parentId);
		obj.setNLsfbCode(nlsfb);
		obj.setGeometry(createDummyGeom(1.0, 1.0, 1.0));
		return obj;
	}

	public NmdReferenceResources getDummyReferences() {
		NmdReferenceResources resources = new NmdReferenceResources();
		HashMap<Integer, NmdMilieuCategorie> milieuCats = new HashMap<Integer, NmdMilieuCategorie>();
		milieuCats.put(1, new NmdMilieuCategorie("AbioticDepletionNonFuel", "kg antimoon", 1.0));
		milieuCats.put(2, new NmdMilieuCategorie("AbioticDepletionFuel", "kg antimoon", 1.0));
		milieuCats.put(3, new NmdMilieuCategorie("GWP100", "kg CO2", 1.0));
		milieuCats.put(4, new NmdMilieuCategorie("ODP", "kg CFC11", 1.0));
		milieuCats.put(5, new NmdMilieuCategorie("PhotoChemicalOxidation", "kg etheen", 1.0));
		milieuCats.put(6, new NmdMilieuCategorie("Acidifcation", "kg SO2", 1.0));
		milieuCats.put(7, new NmdMilieuCategorie("Eutrophication", "kg (PO4)^3-", 1.0));
		milieuCats.put(8, new NmdMilieuCategorie("HumanToxicity", "kg 1,4 dichloor benzeen", 1.0));
		milieuCats.put(9, new NmdMilieuCategorie("FreshWaterAquaticEcoToxicity", "kg 1,4 dichloor benzeen", 1.0));
		milieuCats.put(10, new NmdMilieuCategorie("MarineAquaticEcoToxicity", "kg 1,4 dichloor benzeen", 1.0));
		milieuCats.put(11, new NmdMilieuCategorie("TerrestrialEcoToxocity", "kg 1,4 dichloor benzeen", 1.0));
		milieuCats.put(12, new NmdMilieuCategorie("TotalRenewableEnergy", "MJ", 1.0));
		milieuCats.put(13, new NmdMilieuCategorie("TotalNonRenewableEnergy", "MJ", 1.0));
		milieuCats.put(14, new NmdMilieuCategorie("TotalEnergy", "MJ", 1.0));
		milieuCats.put(15, new NmdMilieuCategorie("FreshWaterUse", "m3", 1.0));
		resources.setMilieuCategorieMapping(milieuCats);

		HashMap<Integer, String> fasen = new HashMap<Integer, String>();
		fasen.put(1, "productie");
		fasen.put(2, "transport -> bouwplaats");
		fasen.put(3, "bouwfase");
		fasen.put(4, "gebruik van product");
		fasen.put(5, "onderhoud");
		fasen.put(6, "reparatie");
		fasen.put(7, "vervangen");
		fasen.put(8, "opknappen");
		fasen.put(9, "deconstructie of sloop");
		fasen.put(10, "transport -> afval");
		fasen.put(11, "afvalverwerking");
		fasen.put(12, "afvalverwijdering");
		fasen.put(13, "Baten en lasten voorbij de systeemgrenzen");
		resources.setFaseMapping(fasen);

		HashMap<Integer, String> units = new HashMap<Integer, String>();
		units.put(1, "MJ");
		units.put(2, "kWh");
		units.put(3, "kg");
		units.put(4, "kg*jaar");
		units.put(5, "m1");
		units.put(6, "m1*jaar");
		units.put(7, "m2");
		units.put(8, "m2*jaar");
		units.put(9, "m2*km");
		units.put(10, "m2K/W");
		units.put(11, "m2K/W*jaar");
		units.put(12, "m2gbo");
		units.put(13, "m2gbo*jaar");
		units.put(14, "m3");
		units.put(15, "m3*jaar");
		units.put(16, "mm");
		units.put(17, "p");
		units.put(18, "p*jaar");
		units.put(19, "t*km");
		units.put(20, "tkm");
		units.put(21, "onbekend");
		units.put(22, "Samengesteld");
		resources.setUnitMapping(units);

		HashMap<Integer, String> cuasCategorien = new HashMap<Integer, String>();
		cuasCategorien.put(1, "Constructie");
		cuasCategorien.put(2, "Uitrusting");
		cuasCategorien.put(1, "Afwerking");
		cuasCategorien.put(1, "Schilderwerk");
		resources.setCuasCategorieMapping(cuasCategorien);

		return resources;
	}

	public MpgObjectStoreImpl getStore() {
		return store;
	}

	public void setStore(MpgObjectStoreImpl store) {
		this.store = store;
	}

}
