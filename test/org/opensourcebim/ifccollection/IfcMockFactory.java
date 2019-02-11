package org.opensourcebim.ifccollection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcBuildingElement;
import org.bimserver.models.ifc2x3tc1.IfcMaterial;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayer;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSet;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSetUsage;
import org.bimserver.models.ifc2x3tc1.IfcMaterialList;
import org.bimserver.models.ifc2x3tc1.IfcMaterialSelect;
import org.bimserver.models.ifc2x3tc1.IfcObjectDefinition;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociates;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociatesMaterial;
import org.bimserver.models.ifc2x3tc1.IfcRelDecomposes;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelSpaceBoundary;
import org.bimserver.models.ifc2x3tc1.IfcSIPrefix;
import org.bimserver.models.ifc2x3tc1.IfcSIUnit;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.models.ifc2x3tc1.IfcUnit;
import org.bimserver.models.ifc2x3tc1.IfcUnitAssignment;
import org.bimserver.models.ifc2x3tc1.IfcUnitEnum;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

/**
 * Factory class to create IfcModel objects to test various methods
 * 
 * @author Jasper Vijverberg
 */
public class IfcMockFactory {

	private GeometryInfo geometry = null;
	private EList<IfcRelAssociates> associations = null;
	private IfcSIPrefix projectUnitPrefix = IfcSIPrefix.NULL;

	public IfcMockFactory() {
		setGeometry(getGeometryInfoMock(1, 1));
		setAssociations(new BasicEList<>());
	}

	public IfcModelInterface getModelMock() {
		IfcModelInterface model = mock(IfcModelInterface.class);
		addIfcProjectMock(model);
		when(model.getAllWithSubTypes(IfcBuildingElement.class)).thenReturn(new ArrayList<IfcBuildingElement>());

		return model;
	}

	public void addProductToModel(IfcModelInterface mockModel, String name, String parentId) {
		List<IfcProduct> products = mockModel.getAllWithSubTypes(IfcProduct.class);

		IfcProduct newProduct = addIfcProductToModel(null, name);
		BasicEList<IfcRelDecomposes> relations = new BasicEList<IfcRelDecomposes>();
		// add potential children as decomposed elements
		if (parentId != null) {
			IfcObjectDefinition parentObject = mockModel.getAllWithSubTypes(IfcProduct.class).stream()
					.filter(o -> o.getGlobalId().equals(parentId)).collect(Collectors.toList()).get(0);

			IfcRelDecomposes mockRel = mock(IfcRelDecomposes.class);
			when(mockRel.getRelatingObject()).thenReturn(parentObject);
			relations.add(mockRel);
		}

		when(newProduct.getDecomposes()).thenReturn(relations);

		// for now add empty reldefinedBy
		when(newProduct.getIsDefinedBy()).thenReturn(new BasicEList<IfcRelDefines>());

		products.add(newProduct);
		when(mockModel.getAllWithSubTypes(IfcProduct.class)).thenReturn(products);
	}

	public void addSpaceToModel(IfcModelInterface mockModel, IfcProduct parent) {

		List<IfcSpace> products = mockModel.getAllWithSubTypes(IfcSpace.class);
		IfcSpace space = createGenericIfcProduct(IfcSpace.class, parent);

		// add some boundedBy relation to mock that the space is an internalspace
		BasicEList<IfcRelSpaceBoundary> boundedBy = new BasicEList<IfcRelSpaceBoundary>();
		boundedBy.add(mock(IfcRelSpaceBoundary.class));
		when(space.getBoundedBy()).thenReturn(boundedBy);

		products.add(space);
		when(mockModel.getAllWithSubTypes(IfcSpace.class)).thenReturn(products);

	}

	public <T extends IfcProduct> void addGenericIfcProductToModel(IfcModelInterface mockModel, Class<T> productClass,
			IfcProduct parent) {
		List<T> products = mockModel.getAllWithSubTypes(productClass);
		products.add(createGenericIfcProduct(productClass, parent));
		when(mockModel.getAllWithSubTypes(productClass)).thenReturn(products);
	}

	/**
	 * Get Mock product with Geometry, the difference with the generic method is
	 * that products can be linked to materials.
	 * 
	 * @return a Mocked IfcProduct object
	 */
	private IfcProduct addIfcProductToModel(IfcObjectDefinition parent, String name) {
		IfcProduct mockProduct = createGenericIfcProduct(IfcBuildingElement.class, parent);
		when(mockProduct.getHasAssociations()).thenReturn(associations);
		when(mockProduct.getName()).thenReturn(name == null ? "" : name);

		String id = UUID.randomUUID().toString();
		when(mockProduct.getGlobalId()).thenReturn(id);

		return mockProduct;
	}

	/**
	 * base IfcProduct mock constructor
	 * 
	 * @param productClass
	 * @return
	 */
	private <T extends IfcProduct> T createGenericIfcProduct(Class<T> productClass, IfcObjectDefinition parent) {

		T mockProduct = mock(productClass);
		when(mockProduct.getGeometry()).thenReturn(geometry);

		if (parent != null) {
			EList<IfcRelDecomposes> relationList = new BasicEList<IfcRelDecomposes>();
			IfcRelDecomposes decompositionRelation = mock(IfcRelDecomposes.class);
			when(decompositionRelation.getRelatingObject()).thenReturn(parent);
			relationList.add(decompositionRelation);
			when(mockProduct.getIsDecomposedBy()).thenReturn(relationList);
		}

		return mockProduct;
	}

	/**
	 * Adds an IfcRelAssociatesMaterial to the associates
	 * 
	 * @param mat Material object to add to the associates list
	 */
	public void addMaterial(IfcMaterialSelect mat) {
		if (associations != null) {
			associations.add(getRelAssociatesMaterialMock(mat));
		}
	}

	public void addMaterial(String matName) {
		IfcMaterial mat = getIfcMaterialMock(matName);
		addMaterial(mat);
	}

	public void addMaterialList(List<String> matNames) {
		IfcMaterialSelect mat = getIfcMaterialListMock(matNames);
		addMaterial(mat);
	}

	public void addMaterialLayer(String matName, double thickness) {
		IfcMaterialSelect layer = getIfcMaterialLayerMock(matName, thickness);
		addMaterial(layer);
	}

	public void addMaterialLayerSet(List<Entry<String, Double>> layers) {
		IfcMaterialSelect layerSet = getIfcMaterialLayerSetMock(layers);
		addMaterial(layerSet);
	}

	public void addMaterialLayerSetUsage(List<Entry<String, Double>> layers) {
		IfcMaterialSelect layerSet = getIfcMaterialLayerSetUsageMock(layers);
		addMaterial(layerSet);
	}

	public void addIfcProjectMock(IfcModelInterface mockModel) {
		IfcProject project = mock(IfcProject.class);
		IfcUnitAssignment units = mock(IfcUnitAssignment.class);
		EList<IfcUnit> unittypes = new BasicEList<>();

		unittypes.add(getUnitMock(IfcUnitEnum.VOLUMEUNIT, this.projectUnitPrefix));
		unittypes.add(getUnitMock(IfcUnitEnum.AREAUNIT, this.projectUnitPrefix));
		unittypes.add(getUnitMock(IfcUnitEnum.LENGTHUNIT, this.projectUnitPrefix));
		
		when(units.getUnits()).thenReturn(unittypes);
		when(project.getUnitsInContext()).thenReturn(units);

		List<IfcProject> projects = new BasicEList<IfcProject>();
		projects.add(project);
		when(mockModel.getAll(IfcProject.class)).thenReturn(projects);
	}

	public IfcSIUnit getUnitMock(IfcUnitEnum type, IfcSIPrefix prefix) {
		IfcSIUnit unit = mock(IfcSIUnit.class);
		when(unit.getUnitType()).thenReturn(type);
		when(unit.getPrefix()).thenReturn(prefix);
		return unit;
	}

	public GeometryInfo getGeometryInfoMock(double area, double volume) {
		GeometryInfo geom = mock(GeometryInfo.class);
		when(geom.getVolume()).thenReturn(volume);
		when(geom.getArea()).thenReturn(area);
		Double faceArea = Math.pow(volume, 2.0/3.0);
		when(geom.getAdditionalData()).thenReturn(
				"{\"TOTAL_SURFACE_AREA\":" + String.valueOf(faceArea * 6.0)
				+ ",\"TOTAL_SHAPE_VOLUME\":" + volume
				+ ",\"SURFACE_AREA_ALONG_X\":" + faceArea 
				+ ",\"SURFACE_AREA_ALONG_Y\":" + faceArea 
				+ ",\"SURFACE_AREA_ALONG_Z\":" + faceArea+ "}");
		return geom;
	}

	public IfcRelAssociatesMaterial getRelAssociatesMaterialMock(IfcMaterialSelect material) {
		IfcRelAssociatesMaterial association = mock(IfcRelAssociatesMaterial.class);
		when(association.getRelatingMaterial()).thenReturn(material);
		return association;
	}

	public IfcMaterial getIfcMaterialMock(String name) {
		if (name != null && !name.isEmpty()) {
			IfcMaterial mat = mock(IfcMaterial.class);
			when(mat.getName()).thenReturn(name);
			long oid = Integer.toUnsignedLong(name.hashCode());
			when(mat.getOid()).thenReturn(oid);
			return mat;
		}
		return null;
	}

	public IfcMaterialList getIfcMaterialListMock(List<String> names) {
		IfcMaterialList list = mock(IfcMaterialList.class);
		BasicEList<IfcMaterial> mats = new BasicEList<IfcMaterial>();
		names.forEach((name) -> mats.add(getIfcMaterialMock(name)));

		when(list.getMaterials()).thenReturn(mats);

		return list;
	}

	public IfcMaterialLayer getIfcMaterialLayerMock(String name, double thickness) {
		IfcMaterialLayer layer = mock(IfcMaterialLayer.class);
		when(layer.getLayerThickness()).thenReturn(thickness);

		IfcMaterial mat = getIfcMaterialMock(name);
		when(layer.getMaterial()).thenReturn(mat);

		return layer;
	}

	public IfcMaterialLayerSet getIfcMaterialLayerSetMock(List<Entry<String, Double>> layers) {

		IfcMaterialLayerSet layerSet = mock(IfcMaterialLayerSet.class);
		EList<IfcMaterialLayer> layerList = new BasicEList<IfcMaterialLayer>();
		layers.forEach((layer) -> {
			layerList.add(getIfcMaterialLayerMock(layer.getKey() == null ? "" : layer.getKey(), layer.getValue()));
		});

		when(layerSet.getMaterialLayers()).thenReturn(layerList);
		return layerSet;
	}

	public IfcMaterialLayerSetUsage getIfcMaterialLayerSetUsageMock(List<Entry<String, Double>> layers) {

		IfcMaterialLayerSetUsage layerSetUsage = mock(IfcMaterialLayerSetUsage.class);
		IfcMaterialLayerSet layerSet = getIfcMaterialLayerSetMock(layers);
		when(layerSetUsage.getForLayerSet()).thenReturn(layerSet);
		return layerSetUsage;
	}

	// ------------- auto-generated setters and getters ---------------
	public GeometryInfo getGeometry() {
		return geometry;
	}

	public void setGeometry(GeometryInfo geometry) {
		this.geometry = geometry;
	}

	public EList<IfcRelAssociates> getAssociations() {
		return associations;
	}

	public void setAssociations(EList<IfcRelAssociates> associations) {
		this.associations = associations;
	}

	public IfcSIPrefix getProjectUnitPrefix() {
		return projectUnitPrefix;
	}

	public void setProjectUnitPrefix(IfcSIPrefix projectUnitPrefix) {
		this.projectUnitPrefix = projectUnitPrefix;
	}
}
