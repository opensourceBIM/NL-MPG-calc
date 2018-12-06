package org.opensourcebim.ifccollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.*;
import org.bimserver.models.ifc2x3tc1.impl.IfcAnnotationImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcBuildingImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcBuildingStoreyImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcFurnishingElementImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcOpeningElementImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcSiteImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcSpaceImpl;
import org.bimserver.models.ifc2x3tc1.impl.IfcVirtualElementImpl;
import org.bimserver.utils.AreaUnit;
import org.bimserver.utils.IfcUtils;
import org.bimserver.utils.VolumeUnit;
import org.eclipse.emf.common.util.EList;

/**
 * Class to retrieve the material properties from the IfcModel
 * 
 * @author Jasper Vijverberg
 */
public class MpgIfcObjectCollector {

	private MpgObjectStoreImpl objectStore;

	// products that should not be included in the material calculations.
	private List<Class<? extends IfcProduct>> ignoredProducts;

	// reporting units and imported units to help convert measurements
	private AreaUnit areaUnit = AreaUnit.SQUARED_METER;
	private VolumeUnit volumeUnit = VolumeUnit.CUBIC_METER;
	private AreaUnit modelAreaUnit;
	private VolumeUnit modelVolumeUnit;

	public MpgIfcObjectCollector() {
		objectStore = new MpgObjectStoreImpl();

		ignoredProducts = Arrays.asList(
				IfcSite.class, IfcSiteImpl.class, 
				IfcBuilding.class, IfcBuildingImpl.class,
				IfcBuildingStorey.class, IfcBuildingStoreyImpl.class, 
				IfcFurnishingElement.class, IfcFurnishingElementImpl.class,
				IfcOpeningElement.class, IfcOpeningElementImpl.class, 
				IfcVirtualElement.class, IfcVirtualElementImpl.class,
				IfcSpace.class, IfcSpaceImpl.class,
				IfcAnnotation.class, IfcAnnotationImpl.class);
	}

	public MpgObjectStore results() {
		return this.objectStore;
	}

	/**
	 * method to read in a IfcModel and retrieve material properties for MPG
	 * calculations
	 * 
	 * @param ifcModel for now only a ifc2x3tc1 IfcModel object
	 */
	public MpgObjectStore collectIfcModelObjects(IfcModelInterface ifcModel) {
		objectStore.reset();

		// get project wide parameters
		modelVolumeUnit = IfcUtils.getVolumeUnit(ifcModel);
		modelAreaUnit = IfcUtils.getAreaUnit(ifcModel);

		// loop through IfcSpaces
		for (IfcSpace space : ifcModel.getAllWithSubTypes(IfcSpace.class)) {
			
			// omit any eternal spaces.
			if (space.getBoundedBy().size() == 0) {
				continue;
			}
				
			EList<IfcRelDecomposes> parentDecomposedProduct = space.getIsDecomposedBy();

			// if there is any space that decomposes in this space we can omit the addition
			// of the volume
			boolean isIncludedSemantically = false;
			if (parentDecomposedProduct != null) {
				isIncludedSemantically = parentDecomposedProduct.stream()
						.filter(relation -> relation.getRelatingObject() instanceof IfcSpace).count() > 0;
			}

			// ToDo: include geometric check
			boolean isIncludedGeometrically = false;

			Pair<Double, Double> geom = getGeometryFromProduct(space);

			if (!isIncludedGeometrically && !isIncludedSemantically) {
				objectStore.getSpaces().add(new MpgSpaceImpl(space.getGlobalId(), geom.getRight(), geom.getLeft()));
			}
		}

		Map<String, String> childToParentMap = new HashMap<String, String>();

		// loop through IfcProduct that constitute the physical building.
		for (IfcProduct element : ifcModel.getAllWithSubTypes(IfcProduct.class)) {

			// ignore any elements that are irrelevant for the mpg calculations
			if (!this.ignoredProducts.contains(element.getClass())) {

				// collect child to parent relations
				element.getDecomposes().stream()
					.map(rel -> rel.getRelatingObject())
					.filter(o -> o instanceof IfcProduct).map(o -> (IfcProduct) o).forEach(o -> {
						if (!childToParentMap.containsKey(element.getGlobalId())
								&& o.getGlobalId() != element.getGlobalId()) {

							childToParentMap.put(element.getGlobalId(), o.getGlobalId());

						} else {
							if (o.getGlobalId() != element.getGlobalId()) {
								System.out.println(">> " + element.getGlobalId() + ", " + o.getGlobalId());
							}
						}
					});

				MpgObjectImpl mpgObject = new MpgObjectImpl(element.getOid(), element.getGlobalId(), element.getName(),
						element.getClass().getSimpleName(), "", objectStore);

				Pair<Double, Double> geom = getGeometryFromProduct(element);
				mpgObject.setVolume(geom.getRight());

				this.getPropertySetsFromIfcProduct(element, mpgObject);

				// set volume if only defined in properties
				Double vol = null;
				if(mpgObject.getProperties().containsKey("volume")) {
					vol = ((double)mpgObject.getProperties().get("volume"));
				}
				if(mpgObject.getProperties().containsKey("netvolume") && vol == null) {
					vol = ((double)mpgObject.getProperties().get("netvolume"));
				}
				if (vol != null && mpgObject.getVolume() == 0.0) {
					mpgObject.setVolume(vol);
				}
					
				// set Pset materials
				if (mpgObject.getProperties().containsKey("material")) {
					String mat = (String)(mpgObject.getProperties().get("material"));
					mpgObject.addMaterialSource(mat, null, "P_Set");
				}
				
				// retrieve information and add found values to the various data objects
				this.getMaterialsFromIfcProduct(element, mpgObject);

				// all properties are set. add it to the store.
				objectStore.getObjects().add(mpgObject);
			}
		}

		// set all parent child relations for elements
		objectStore.recreateParentChildMap(childToParentMap);
		objectStore.validateIfcDataCollection();

		return objectStore;
	}

	/**
	 * retireve the volume of an element based on itself, the template type and any
	 * decomposed elements
	 * 
	 * @param prod the product to evaluate
	 * @return a Tuple with are and volume of the input product
	 */
	private Pair<Double, Double> getGeometryFromProduct(IfcProduct prod) {
		GeometryInfo geometry = prod.getGeometry();
		double area = 0.0;
		double volume = 0.0;

		if (geometry != null) {
			area = this.getAreaUnit().convert(geometry.getArea(), modelAreaUnit);
			volume = this.getVolumeUnit().convert(geometry.getVolume(), modelVolumeUnit);
		} else {
			// get volume of product by summing the volumes of its children
//			if (prod.getIsDecomposedBy().size() > 0) {
//				Double totalVolume = prod.getIsDecomposedBy().stream().flatMap(rel -> rel.getRelatedObjects().stream())
//						.filter(o -> o instanceof IfcProduct).map(o -> ((IfcProduct) o).getGeometry())
//						.map(g -> (g != null) ? g.getVolume() : 0.0).collect(Collectors.summingDouble(v -> v));
//
//				volume = this.getVolumeUnit().convert(totalVolume, modelVolumeUnit);
//			} else {
//				System.out.println(prod.getGlobalId());
//			}
		}

		return new ImmutablePair<Double, Double>(area, volume);
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
	 * @param mpgObject mpgObject to add properties to
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
			addPropertiesFromPropertySetDefinition((IfcElementQuantity)propSet, mpgObject);
		} else if (propSet instanceof IfcPropertySet) {
			addPropertiesFromPropertySetDefinition((IfcPropertySet)propSet, mpgObject);
		} else {
			//System.out.println("found unidentified propertyset definition");
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

	private void getMaterialsFromObject(IfcObjectDefinition sourceObject, MpgObjectImpl targetObject) {

		EList<IfcRelAssociates> associates = sourceObject.getHasAssociations();
		if (!associates.isEmpty()) {

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
				double volumeRatio = layer.getRight() / totalThickness * targetObject.getVolume();
				targetObject.addLayer(new MpgLayerImpl(volumeRatio, materialName, materialGuid));
				targetObject.addMaterialSource(materialName, materialGuid, matSourceLayer);
			});
		}
	}

	/**
	 * get the relevant data from a material layer object
	 * 
	 * @param layer the material layer object to parse
	 * @return an object with material layer information
	 */
	private List<Triple<String, String, Double>> getMaterialLayer(IfcMaterialLayer layer) {
		IfcMaterial material = layer.getMaterial();
		List<Triple<String, String, Double>> res = new ArrayList<Triple<String, String, Double>>();
		MutableTriple<String, String, Double> triple = new MutableTriple<String, String, Double>(
				material != null ? material.getName() : "", material != null ? Long.toString(material.getOid()) : "",
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

	// ---------- Standard getters and setters -------------
	public AreaUnit getAreaUnit() {
		return areaUnit;
	}

	public void setAreaUnit(AreaUnit areaUnit) {
		this.areaUnit = areaUnit;
	}

	public VolumeUnit getVolumeUnit() {
		return volumeUnit;
	}

	public void setVolumeUnit(VolumeUnit volumeUnit) {
		this.volumeUnit = volumeUnit;
	}
}
