package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.ifc2x3tc1.IfcAnnotation;
import org.bimserver.models.ifc2x3tc1.IfcBoolean;
import org.bimserver.models.ifc2x3tc1.IfcBuilding;
import org.bimserver.models.ifc2x3tc1.IfcBuildingStorey;
import org.bimserver.models.ifc2x3tc1.IfcClassificationNotationSelect;
import org.bimserver.models.ifc2x3tc1.IfcClassificationReference;
import org.bimserver.models.ifc2x3tc1.IfcElementQuantity;
import org.bimserver.models.ifc2x3tc1.IfcFurnishingElement;
import org.bimserver.models.ifc2x3tc1.IfcGrid;
import org.bimserver.models.ifc2x3tc1.IfcIdentifier;
import org.bimserver.models.ifc2x3tc1.IfcLabel;
import org.bimserver.models.ifc2x3tc1.IfcMaterial;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayer;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSet;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSetUsage;
import org.bimserver.models.ifc2x3tc1.IfcMaterialList;
import org.bimserver.models.ifc2x3tc1.IfcMaterialSelect;
import org.bimserver.models.ifc2x3tc1.IfcObjectDefinition;
import org.bimserver.models.ifc2x3tc1.IfcOpeningElement;
import org.bimserver.models.ifc2x3tc1.IfcPhysicalQuantity;
import org.bimserver.models.ifc2x3tc1.IfcPhysicalSimpleQuantity;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcProperty;
import org.bimserver.models.ifc2x3tc1.IfcPropertySet;
import org.bimserver.models.ifc2x3tc1.IfcPropertySetDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPropertySingleValue;
import org.bimserver.models.ifc2x3tc1.IfcQuantityArea;
import org.bimserver.models.ifc2x3tc1.IfcQuantityLength;
import org.bimserver.models.ifc2x3tc1.IfcQuantityVolume;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociates;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociatesClassification;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociatesMaterial;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByProperties;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByType;
import org.bimserver.models.ifc2x3tc1.IfcSite;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.models.ifc2x3tc1.IfcTypeObject;
import org.bimserver.models.ifc2x3tc1.IfcTypeProduct;
import org.bimserver.models.ifc2x3tc1.IfcValue;
import org.bimserver.models.ifc2x3tc1.IfcVirtualElement;
import org.bimserver.models.ifc2x3tc1.impl.IfcAnnotationImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcBuildingImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcBuildingStoreyImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcFurnishingElementImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcGridImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcOpeningElementImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcSiteImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcSpaceImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcVirtualElementImpl;

import org.eclipse.emf.common.util.EList;

/**
 * Class to retrieve the material properties from the IfcModel
 * 
 * @author Jasper Vijverberg
 */
public class MpgIfcObjectCollector {

	private MpgObjectStoreImpl objectStore;
	private MpgGeometryParser geometryParser;
	// products that should not be included in the material calculations.
	private List<Class<? extends IfcProduct>> ignoredProducts;

	public MpgIfcObjectCollector() {
		objectStore = new MpgObjectStoreImpl();
		objectStore.setUnits(MpgGeometryParser.getVolumeUnit(),
				MpgGeometryParser.getAreaUnit(),
				MpgGeometryParser.GetLengthUnit());	
				
		// List products that are not going to be analyzed for material mappings
		ignoredProducts = Arrays.asList(IfcSite.class, IfcSiteImpl.class, IfcBuilding.class, IfcBuildingImpl.class,
				IfcBuildingStorey.class, IfcBuildingStoreyImpl.class, IfcFurnishingElement.class,
				IfcFurnishingElementImpl.class, IfcOpeningElement.class, IfcOpeningElementImpl.class,
				IfcVirtualElement.class, IfcVirtualElementImpl.class, IfcSpace.class, IfcSpaceImpl.class,
				IfcAnnotation.class, IfcAnnotationImpl.class, IfcGrid.class, IfcGridImpl.class);
	}

	public MpgObjectStore results() {
		return this.objectStore;
	}

	public MpgObjectStore collectIfcModelObjects(IfcModelInterface model, String pId) {
		return collectIfcModelObjects(model, pId, null);
	}

	public MpgObjectStore collectIfcModelObjects(BimBotsInput input, String pId) {
		return collectIfcModelObjects(input.getIfcModel(), pId, input.getData());
	}
	
	/**
	 * method to read in a IfcModel and retrieve material properties for MPG
	 * calculations using different types of data retrieval availabel for the IFC standard
	 * 
	 * @param ifcModel for now only a ifc2x3tc1 IfcModel object
	 */
	public MpgObjectStore collectIfcModelObjects(IfcModelInterface ifcModel, String pId, byte[] data) {		
		objectStore.reset();
		geometryParser = new MpgGeometryParser(ifcModel);
		objectStore.setProjectId(pId);

		this.geometryParser.tryParseFloorArea(ifcModel, this.objectStore, data);

		Map<String, String> childToParentMap = new HashMap<String, String>();

		// loop through IfcProducts that constitute the physical building.
		for (IfcProduct product : ifcModel.getAllWithSubTypes(IfcProduct.class)) {

			// ignore any elements that are irrelevant for the mpg calculations
			if (!this.ignoredProducts.contains(product.getClass())) {

				if (StringUtils.isBlank(product.getGlobalId())) {
					continue;
				}

				// collect child to parent relations
				product.getDecomposes().stream()
					.map(rel -> rel.getRelatingObject())
					.filter(o -> o instanceof IfcProduct).map(o -> (IfcProduct) o).forEach(o -> {
						if (!childToParentMap.containsKey(product.getGlobalId())
								&& o.getGlobalId() != product.getGlobalId()) {

							childToParentMap.put(product.getGlobalId(), o.getGlobalId());
						}
					});
				
				MpgObjectImpl mpgObject = new MpgObjectImpl(product.getOid(), 
						product.getGlobalId(), 
						product.getName(),
						product.getClass().getSimpleName(), "");

				this.getPropertySetsFromIfcProduct(product, mpgObject);
				MpgGeometry geom = geometryParser.getGeometryFromProduct(product);
				if (geom.getVolume().isNaN()) {
					// if the geomServer does not return a volume we have to try it through properties.
					mpgObject.addTag(MpgInfoTagType.geometrySourceType, "Geometry from property set");
					mpgObject.setGeometry(this.getGeometryFromPropertySet(product, mpgObject));
				} else {
					mpgObject.addTag(MpgInfoTagType.geometrySourceType, "Geometry from ifcopenShell");
					mpgObject.setGeometry(geom);
				}

				// set Pset materials
//				if (mpgObject.getProperties().containsKey("material")) {
//					String mat = (String) (mpgObject.getProperties().get("material"));
//					mpgObject.addMaterialSource(mat, null, "P_Set");
//				}

				// retrieve information and add found values to the various data objects
				this.getMaterialsFromIfcProduct(product, mpgObject);
				this.getProductClassications(product, mpgObject);
				
				// all properties are set. add it to the store.
				// create the mpg element and link it to the object
				String newMpgElementId = product.getName() + "-" + product.getGlobalId();
				MpgElement newMpgElement = this.objectStore.addElement(newMpgElementId);
				objectStore.getObjects().add(mpgObject);
				newMpgElement.setMpgObject(mpgObject);
			}
		}

		// set all parent child relations for elements
		objectStore.reloadParentChildRelationShips(childToParentMap);
		return objectStore;
	}

	/**
	 * Alternative method to get geometry parameters based on the property sets. Should be discarded!
	 * 
	 * @param product   IfcProduct object
	 * @param mpgObject mpgObject to add parsed properties to.
	 * @return mpgGeometry object
	 */
	private MpgGeometry getGeometryFromPropertySet(IfcProduct product, MpgObjectImpl mpgObject) {
		MpgGeometry geom = new MpgGeometry();

		// first try to set the geometry by properties
		Double vol = null;
		if (mpgObject.getProperties().containsKey("volume")) {
			vol = ((double) mpgObject.getProperties().get("volume"));
		}
		if (mpgObject.getProperties().containsKey("netvolume") && vol == null) {
			vol = ((double) mpgObject.getProperties().get("netvolume"));
		}
		if (vol != null) {
			geom.setVolume(vol);
		}

		Double area = null;
		if (mpgObject.getProperties().containsKey("grosssidearea")) {
			area = ((double) mpgObject.getProperties().get("grosssidearea"));
		}
		if (mpgObject.getProperties().containsKey("area")) {
			area = ((double) mpgObject.getProperties().get("area"));
		}
		if (mpgObject.getProperties().containsKey("netarea") && area == null) {
			area = ((double) mpgObject.getProperties().get("netarea"));
		}
		if (area != null) {
			geom.setFloorArea(area);
		}
		geom.setIsComplete(false);
		return geom;
	}

	/**
	 * retrieve the property sets from the ifc product and any present templates
	 * 
	 * @param product
	 * @param mpgObject
	 */
	private void getPropertySetsFromIfcProduct(IfcProduct product, MpgObjectImpl mpgObject) {
		// try get the materials from the relating type
		for (IfcRelDefines def : product.getIsDefinedBy()) {
			if (def instanceof IfcRelDefinesByType) {
				IfcRelDefinesByType typeDefRel = (IfcRelDefinesByType) def;
				IfcTypeObject relatingType = typeDefRel.getRelatingType();
				getPropertySetFromTypeObject(relatingType, mpgObject);
			}
			if (def instanceof IfcRelDefinesByProperties) {
				IfcRelDefinesByProperties props = (IfcRelDefinesByProperties) def;
				IfcPropertySetDefinition propSet = props.getRelatingPropertyDefinition();
				resolvePropertySetAndAddProperties(propSet, mpgObject);
			}
		}
	}

	/**
	 * Retrieve the Property sets from any linked IfcTypeObject and pass this on to
	 * the Property collection method
	 * 
	 * @param typeObjecttemplate type to retrieve
	 * @param mpgObject          mpgObject to add properties to
	 */
	private void getPropertySetFromTypeObject(IfcTypeObject typeObject, MpgObjectImpl mpgObject) {
		EList<IfcPropertySetDefinition> propertySets = typeObject.getHasPropertySets();
		if (!propertySets.isEmpty()) {
			for (IfcPropertySetDefinition propSet : propertySets) {
				resolvePropertySetAndAddProperties(propSet, mpgObject);
			}
		}
	}

	private void resolvePropertySetAndAddProperties(IfcPropertySetDefinition propSet, MpgObjectImpl mpgObject) {
		if (propSet instanceof IfcElementQuantity) {
			addPropertiesFromPropertySetDefinition((IfcElementQuantity) propSet, mpgObject);
		} else if (propSet instanceof IfcPropertySet) {
			addPropertiesFromPropertySetDefinition((IfcPropertySet) propSet, mpgObject);
		} else {
			// System.out.println("found unidentified propertyset definition");
		}
	}

	private void addPropertiesFromPropertySetDefinition(IfcElementQuantity quantities, MpgObjectImpl mpgObject) {
		for (IfcPhysicalQuantity physQuant : quantities.getQuantities()) {
			if (physQuant instanceof IfcPhysicalSimpleQuantity) {
				IfcPhysicalSimpleQuantity simpleQuant = (IfcPhysicalSimpleQuantity) physQuant;
				String name = simpleQuant.getName().toLowerCase();
				Object value = null;

				if (simpleQuant instanceof IfcQuantityVolume) {
					value = ((IfcQuantityVolume) simpleQuant).getVolumeValue();
				} else if (simpleQuant instanceof IfcQuantityArea) {
					value = ((IfcQuantityArea) simpleQuant).getAreaValue();
				} else if (simpleQuant instanceof IfcQuantityLength) {
					value = ((IfcQuantityLength) simpleQuant).getLengthValue();
				}

				if (value != null) {
					mpgObject.addProperty(name, value);
				}
			}
		}
	}

	private void addPropertiesFromPropertySetDefinition(IfcPropertySet defs, MpgObjectImpl mpgObject) {

		for (IfcProperty prop : defs.getHasProperties()) {
			if (prop instanceof IfcPropertySingleValue) {
				IfcPropertySingleValue valProp = (IfcPropertySingleValue) prop;
				String name = valProp.getName().toLowerCase();

				IfcValue ifcValue = (valProp.getNominalValue());
				Object value = null;

				if (ifcValue instanceof IfcBoolean) {
					value = ((IfcBoolean) ifcValue).getWrappedValue().getLiteral();
				} else if (ifcValue instanceof IfcLabel) {
					value = ((IfcLabel) ifcValue).getWrappedValue();
				} else if (ifcValue instanceof IfcIdentifier) {
					value = ((IfcIdentifier) ifcValue).getWrappedValue();
				}

				if (value != null) {
					mpgObject.addProperty(name, value);
				}
			}
		}
	}

	/**
	 * Retrieve the materials and layers from the IfcProduct object and store these
	 * as MpgMaterial objects
	 * 
	 * @param ifcProduct The ifcProduct object to retrieve the material names from
	 * @param mpgObject  The object to add the found materials to.
	 */
	private void getMaterialsFromIfcProduct(IfcProduct ifcProduct, MpgObjectImpl mpgObject) {

		// try get the materials directly from the product
		getMaterialsFromObject(ifcProduct, mpgObject);

		// try get the materials from the relating type
		for (IfcRelDefines def : ifcProduct.getIsDefinedBy()) {
			if (def instanceof IfcRelDefinesByType) {
				IfcRelDefinesByType typeDefRel = (IfcRelDefinesByType) def;
				IfcTypeObject relatingType = typeDefRel.getRelatingType();
				getMaterialsFromObject(relatingType, mpgObject);
			}
		}
	}

	/**
	 * Try find the NLsfb classification of an object and add it to the MpgObject
	 * @param sourceObject IfcObjectDefinition from ifc file
	 * @param targetObject MpgObject to add the NLsfb code to
	 */
	private void getProductClassications(IfcObjectDefinition sourceObject, MpgObjectImpl targetObject) {

		EList<IfcRelAssociates> associates = sourceObject.getHasAssociations();
		if (associates != null && !associates.isEmpty()) {
			for (IfcRelAssociates ifcRelAssociates : associates) {
				if (ifcRelAssociates instanceof IfcRelAssociatesClassification) {
					IfcRelAssociatesClassification classes = (IfcRelAssociatesClassification) ifcRelAssociates;
					IfcClassificationNotationSelect relClass = classes.getRelatingClassification();

					if (relClass instanceof IfcClassificationReference) {
						IfcClassificationReference relRef = (IfcClassificationReference) relClass;
						if (relRef.getReferencedSource().getName().toLowerCase().contains("sfb")) {
							targetObject.setNLsfbCode(relRef.getItemReference());
						}
					}

				}
			}
		}
	}

	private void getMaterialsFromObject(IfcObjectDefinition sourceObject, MpgObjectImpl targetObject) {

		EList<IfcRelAssociates> associates = sourceObject.getHasAssociations();
		if (associates != null && !associates.isEmpty()) {

			String matSource = null;
			if (sourceObject instanceof IfcTypeProduct) {
				matSource = "type";
			}

			List<Triple<String, String, Double>> productLayers = new ArrayList<Triple<String, String, Double>>();
			Map<String, String> productMaterials = new HashMap<String, String>();

			for (IfcRelAssociates ifcRelAssociates : associates) {

				if (ifcRelAssociates instanceof IfcRelAssociatesMaterial) {
					IfcRelAssociatesMaterial matRelation = (IfcRelAssociatesMaterial) ifcRelAssociates;
					IfcMaterialSelect relatingMaterial = matRelation.getRelatingMaterial();

					// try determine what the derived interface of the IfcMaterialSelect is
					if (relatingMaterial instanceof IfcMaterial) {
						IfcMaterial mat = (IfcMaterial) relatingMaterial;
						productMaterials.put(Long.toString(mat.getOid()), mat.getName());
					} else if (relatingMaterial instanceof IfcMaterialList) {
						IfcMaterialList mats = (IfcMaterialList) relatingMaterial;
						mats.getMaterials()
								.forEach((mat) -> productMaterials.put(Long.toString(mat.getOid()), mat.getName()));
					} else if (relatingMaterial instanceof IfcMaterialLayerSetUsage) {
						productLayers.addAll(getMaterialLayerList((IfcMaterialLayerSetUsage) relatingMaterial));
					} else if (relatingMaterial instanceof IfcMaterialLayerSet) {
						productLayers.addAll(getMaterialLayerList((IfcMaterialLayerSet) relatingMaterial));
					} else if (relatingMaterial instanceof IfcMaterialLayer) {
						productLayers.addAll(getMaterialLayer((IfcMaterialLayer) relatingMaterial));
					}
				}
			}

			// check total volume matches up with found materials and thickness sums and
			// adjust accordingly.
			double totalThickness = productLayers.stream().collect(Collectors.summingDouble(o -> o.getRight()));

			String matSourceDirect = (matSource != null) ? matSource : "direct";
			// add separately listed materials
			productMaterials.forEach((key, value) -> {
				targetObject.addMaterialSource(value, key, matSourceDirect);
			});

			String matSourceLayer = (matSource != null) ? matSource : "layer";
			// add layers and any materials that have been found with those layers
			productLayers.forEach(layer -> {
				String materialName = layer.getLeft();
				String materialGuid = layer.getMiddle();
				double vol = targetObject.getGeometry().getVolume();
				double volumeRatio = (layer.getRight() / totalThickness) * vol;
				double area = vol * volumeRatio / layer.getRight();
				targetObject.addLayer(new MpgLayerImpl(volumeRatio, area, materialName, materialGuid));
				targetObject.addMaterialSource(materialName, materialGuid, matSourceLayer);
			});
		}
	}

	/**
	 * get the relevant data from a material layer object
	 * 
	 * @param layer the material layer object to parse
	 * @return an object with material layer information. 
	 * return empty values for matname and matid when no material is defined
	 */
	private List<Triple<String, String, Double>> getMaterialLayer(IfcMaterialLayer layer) {
		IfcMaterial material = layer.getMaterial();
		List<Triple<String, String, Double>> res = new ArrayList<Triple<String, String, Double>>();
		MutableTriple<String, String, Double> triple = new MutableTriple<String, String, Double>(
				material != null ? material.getName() : "",
				material != null ? Long.toString(material.getOid()) : "",
				layer.getLayerThickness());

		res.add(triple);
		return res;
	}

	/**
	 * Get the material names from a generic ifcMaterialLayerSet
	 * 
	 * @param layerSet ifcLayerSet object
	 * @return a list of material names and matching thickness.
	 */
	private List<Triple<String, String, Double>> getMaterialLayerList(IfcMaterialLayerSet layerSet) {
		return layerSet.getMaterialLayers().stream().flatMap((layer) -> getMaterialLayer(layer).stream())
				.collect(Collectors.toList());
	}

	/**
	 * polymorphic method of the MaterialLayerSet implementation.
	 * 
	 * @param layerSetUsage ifcLayerSetUsage object
	 * @return a list of material names
	 */
	private List<Triple<String, String, Double>> getMaterialLayerList(IfcMaterialLayerSetUsage layerSetUsage) {
		return getMaterialLayerList(layerSetUsage.getForLayerSet());
	}
}
