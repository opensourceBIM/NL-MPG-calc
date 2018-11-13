package test.org.opensourcebim.mpgcalculations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.*;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

/**
 * Factory class to create IfcModel objects to test various methods
 * @author Jasper Vijverberg
 */
public class IfcMockFactory {

	private GeometryInfo geometry = null;
	private EList<IfcRelAssociates> associations = null;

	public IfcMockFactory() {
		setGeometry(getGeometryInfoMock(1,1));
		setAssociations(new BasicEList<>());
	}
	
	public IfcModelInterface getModelMock() {
		IfcModelInterface model = mock(IfcModelInterface.class);
		addIfcProjectMock(model);
		when(model.getAllWithSubTypes(IfcProduct.class)).thenReturn(new ArrayList<IfcProduct>());
		
		return model;
	}
	
	public void addProductToModel(IfcModelInterface mockModel) {
		// TODO Auto-generated method stub
		List<IfcProduct> products = mockModel.getAllWithSubTypes(IfcProduct.class);
		products.add(getIfcProductMock());
		when(mockModel.getAllWithSubTypes(IfcProduct.class)).thenReturn(products);
	}	
	
	/**
	 * Adds an IfcRelAssociatesMaterial to the associates
	 * @param mat
	 */
	public void addMaterial(IfcMaterialSelect mat){
		if (associations != null) {
			associations.add(getRelAssociatesMaterialMock(mat));
		}
	}
	
	public void addMaterial(String matName){
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
	
	/**
	 * Get Mock product with Geometry
	 * @return a Mocked IfcProduct object
	 */
	public IfcProduct getIfcProductMock() {
		IfcProduct mockProduct = mock(IfcProduct.class);
				
		when(mockProduct.getGeometry()).thenReturn(geometry);
		when(mockProduct.getHasAssociations()).thenReturn(associations);
		
		return mockProduct;
	}
	
	public void addIfcProjectMock(IfcModelInterface mockModel) {
		IfcProject project = mock(IfcProject.class);
		IfcUnitAssignment units = mock(IfcUnitAssignment.class);
		EList<IfcUnit> unittypes = new BasicEList<>();
		
		unittypes.add(getUnitMock(IfcUnitEnum.VOLUMEUNIT, IfcSIPrefix.NULL ));
		unittypes.add(getUnitMock(IfcUnitEnum.AREAUNIT, IfcSIPrefix.NULL ));
		
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
		return geom;
	}
	
	public IfcRelAssociatesMaterial getRelAssociatesMaterialMock(IfcMaterialSelect material) {
		IfcRelAssociatesMaterial association = mock(IfcRelAssociatesMaterial.class);
		when(association.getRelatingMaterial()).thenReturn(material);
		return association;
	}
	
	public IfcMaterial getIfcMaterialMock(String name) {
		IfcMaterial mat = mock(IfcMaterial.class);
		when(mat.getName()).thenReturn(name);
		return mat;
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
		layers.forEach((layer) -> layerList.add(getIfcMaterialLayerMock(layer.getKey(), layer.getValue())));
		
		when(layerSet.getMaterialLayers()).thenReturn(layerList);
		return layerSet;
	}
	
	public IfcMaterialLayerSetUsage getIfcMaterialLayerSetUsageMock(List<Entry<String, Double>> layers) {
		
		IfcMaterialLayerSetUsage layerSetUsage = mock(IfcMaterialLayerSetUsage.class);
		IfcMaterialLayerSet layerSet =  getIfcMaterialLayerSetMock(layers);
		when(layerSetUsage.getForLayerSet()).thenReturn(layerSet);
		return layerSetUsage;
	}
	
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
}
