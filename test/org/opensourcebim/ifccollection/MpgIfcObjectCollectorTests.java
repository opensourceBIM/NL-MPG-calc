package org.opensourcebim.ifccollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.ifc2x3tc1.IfcFurnishingElement;
import org.bimserver.models.ifc2x3tc1.IfcOpeningElement;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociates;
import org.bimserver.models.ifc2x3tc1.IfcSIPrefix;
import org.eclipse.emf.common.util.BasicEList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgIfcObjectCollector;

public class MpgIfcObjectCollectorTests {

	private MpgIfcObjectCollector collector;
	IfcMockFactory factory;
	IfcModelInterface ifcModel;
	
	@Before
	public void setUp() throws Exception {
		factory = new IfcMockFactory();
		ifcModel = factory.getModelMock();
		collector = new MpgIfcObjectCollector();
	}

	@After
	public void tearDown() throws Exception {
		ifcModel = null;
		collector = null;
	}

	@Test
	public void testNewCollectorHasNoParsedMaterials() {
		assertEquals(0, collector.results().getMaterials().size());
	}
	
	@Test
	public void testParseModelWithoutProducts() {
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(0, collector.results().getMaterials().size());
	}
	
	@Test
	public void testParseModelWithoutMaterialAssociatesWillNotThrowError() {		
		factory.setAssociations(null);
		factory.addProductToModel(ifcModel);
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(0, collector.results().getMaterials().size());
	}
	
	@Test
	public void testParseModelWithoutGeometryWillNotThrowError() {
		factory.setGeometry(null);
		factory.addProductToModel(ifcModel);
		collector.collectIfcModelObjects(ifcModel);
		
		assertEquals(0, collector.results().getMaterials().size());
	}
	
	@Test
	public void testCollectObjectsWillGatherObjectNamesofProduct() {
		String name = "a structural beam";
		factory.addProductToModel(ifcModel, name);
		collector.collectIfcModelObjects(ifcModel);
		
		assertEquals(name, collector.results().getObjects().get(0).getObjectName());
	}
	
	@Test
	public void testCollectObjectsWillGatherObjectTypeOfProduct() {
		String type = "Door";
		factory.addProductToModel(ifcModel, null, type);
		collector.collectIfcModelObjects(ifcModel);
		
		// as getClass() is a final method it cannot be mocked to return 'Door'.
		assertTrue(collector.results().getObjects().get(0).getObjectType().contains("Mockito"));
	}
	
	@Test
	public void testCanParseModelWithSingleMaterial() {
		factory.addMaterial("aluminium");
		factory.addProductToModel(ifcModel);
		
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(1, collector.results().getMaterials().size());
	}
	
	@Test
	public void testCanParseModelWithDuplicateMaterial() {
		factory.addMaterial("aluminium");
		factory.addMaterial("aluminium");
		factory.addProductToModel(ifcModel);
		
		collector.collectIfcModelObjects(ifcModel);
		// if the materials are the same we should only store it once.
		assertEquals(1, collector.results().getMaterials().size());
	}
	
	@Test
	public void testCanParseModelWithMaterialsInDifferentProducts() {
		factory.addMaterial("aluminium");
		factory.addProductToModel(ifcModel);
		
		// first clean the associates to avoid adding aluminium twice.
		factory.setAssociations(new BasicEList<>());
		factory.setGeometry(factory.getGeometryInfoMock(2, 2));
		factory.addMaterial("steel");
		factory.addProductToModel(ifcModel);
		
		collector.collectIfcModelObjects(ifcModel);
		// if the materials are the same we should only store it once.
		assertEquals(2, collector.results().getMaterials().size());
	}
	
	@Test
	public void testOmitMaterialsThatAreNotLinkedToGeometry() {
		factory.addMaterial("aluminium");		
		factory.addMaterial("steel");
		factory.setGeometry(null);
		factory.addProductToModel(ifcModel);
		collector.collectIfcModelObjects(ifcModel);
		// if the materials are the same we should only store it once.
		assertEquals(0, collector.results().getMaterials().size());
	}
	
	@Test
	public void testCanParseModelWithMaterialList() {
		factory.addMaterialList(Arrays.asList("Steel", "Aluminium", "Aluminium"));
		factory.addProductToModel(ifcModel);
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(2, collector.results().getMaterials().size());
	}
	
	@Test
	public void testCanParseModelWithEmptyMaterialList() {
		factory.addMaterialList(new ArrayList<String>());
		factory.addProductToModel(ifcModel);
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(0, collector.results().getMaterials().size());
	}
	
	@Test
	public void testCanParseModelWithMaterialLayer() {
		factory.addMaterialLayer("brick", 0.15);
		factory.addProductToModel(ifcModel);
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(1, collector.results().getMaterials().size());
	}
	
	@Test
	public void testCanParseModelWithMaterialLayerSet() {
		List<Entry<String, Double>> layers = new ArrayList<Entry<String, Double>>();

		layers.add(new AbstractMap.SimpleEntry<>("brick", 0.15));
		layers.add(new AbstractMap.SimpleEntry<>("rockwool", 0.1));
		layers.add(new AbstractMap.SimpleEntry<>("mylar", 0.001));
		layers.add(new AbstractMap.SimpleEntry<>("brick", 0.15));
		
		factory.addMaterialLayerSet(layers);
		factory.addProductToModel(ifcModel);
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(3, collector.results().getMaterials().size());
	}
	
	@Test
	public void testCanParseModelWithMaterialLayerSetUsage() {
		List<Entry<String, Double>> layers = new ArrayList<Entry<String, Double>>();

		layers.add(new AbstractMap.SimpleEntry<>("brick", 0.15));
		layers.add(new AbstractMap.SimpleEntry<>("rockwool", 0.1));
		layers.add(new AbstractMap.SimpleEntry<>("mylar", 0.001));
		layers.add(new AbstractMap.SimpleEntry<>("brick", 0.15));
		
		factory.addMaterialLayerSetUsage(layers);
		factory.addProductToModel(ifcModel);
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(3, collector.results().getMaterials().size());
	}
	
	@Test
	public void testCanParseModelWithDifferentMaterials() {
		List<Entry<String, Double>> layers = new ArrayList<Entry<String, Double>>();

		layers.add(new AbstractMap.SimpleEntry<>("brick", 0.15));
		layers.add(new AbstractMap.SimpleEntry<>("rockwool", 0.1));
		
		factory.addMaterialLayerSetUsage(layers);
		
		factory.addMaterial("steel");
		factory.addMaterialLayer("styrofoam", .3);
		
		factory.addProductToModel(ifcModel);
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(4, collector.results().getMaterials().size());
	}
	
	@Test
	public void testCanConvertIfcModelToToBeReportedUnits() {
		// set the units before any model is created. (for that reason we have to reinstantiate the model here.)
		factory.setProjectUnitPrefix(IfcSIPrefix.MILLI);
		ifcModel = factory.getModelMock();
		factory.addMaterial("aluminium");
		
		factory.addProductToModel(ifcModel);
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(1.0e9, collector.results().getObjects().get(0).getSubObjects().get(0).getVolume(), 1e-8);
	}
	
	@Test
	public void testCollectorStoresVolumeOfObjects()
	{
		factory.setGeometry(factory.getGeometryInfoMock(1, 3));
		factory.addMaterial("brick");
		factory.addProductToModel(ifcModel);
		
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(3, collector.results().getObjects().get(0).getSubObjects().get(0).getVolume(), 1e-8);
	}
	
	@Test
	public void testCollectorDeterminesVolumeOfObjectsPerProduct()
	{
		factory.setGeometry(factory.getGeometryInfoMock(1, 3));
		factory.addMaterial("brick");
		factory.addProductToModel(ifcModel);

		// add another product with a different geometry
		factory.setGeometry(factory.getGeometryInfoMock(1, 2));
		factory.addProductToModel(ifcModel);
		
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(3, collector.results().getObjects().get(0).getSubObjects().get(0).getVolume(), 1e-8);
	}
	
	@Test
	public void testCollectorDeterminesVolumeOnLayerThicknessRatio()
	{
		factory.setGeometry(factory.getGeometryInfoMock(1, 1));
		factory.addMaterialLayer("brick", 1);
		factory.addMaterialLayer("rockwool", 3);
		factory.addProductToModel(ifcModel);
		
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(.25, collector.results().getObjects().get(0).getSubObjects().get(0).getVolume(), 1e-8);
		assertEquals(.75, collector.results().getObjects().get(0).getSubObjects().get(1).getVolume(), 1e-8);
	}
	
	@Test
	public void testCollectorDeterminesVolumesOfLayersPerProduct()
	{
		factory.setGeometry(factory.getGeometryInfoMock(1, 1));
		factory.addMaterialLayer("brick", 1);
		factory.addMaterialLayer("rockwool", 3);
		factory.addProductToModel(ifcModel);
		
		factory.setGeometry(factory.getGeometryInfoMock(1, 10));
		factory.setAssociations(new BasicEList<IfcRelAssociates>());
		List<Entry<String, Double>> layers = new ArrayList<Entry<String, Double>>();
		layers.add(new AbstractMap.SimpleEntry<>("brick", 0.25));
		layers.add(new AbstractMap.SimpleEntry<>("rockwool", 0.5));
		layers.add(new AbstractMap.SimpleEntry<>("brick", 0.25));
		factory.addMaterialLayerSet(layers);
		factory.addProductToModel(ifcModel);
		
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(.25, collector.results().getObjects().get(0).getSubObjects().get(0).getVolume(), 1e-8);
		assertEquals(.75, collector.results().getObjects().get(0).getSubObjects().get(1).getVolume(), 1e-8);

		assertEquals(2.5, collector.results().getObjects().get(1).getSubObjects().get(0).getVolume(), 1e-8);
		assertEquals(5.0, collector.results().getObjects().get(1).getSubObjects().get(1).getVolume(), 1e-8);
		assertEquals(2.5, collector.results().getObjects().get(1).getSubObjects().get(2).getVolume(), 1e-8);
	}
	
	@Test
	public void testMaterialLayerSetWithoutMaterialDefinitionsReturnsNullMaterial()
	{
		factory.setGeometry(factory.getGeometryInfoMock(1, 10));
		factory.setAssociations(new BasicEList<IfcRelAssociates>());
		
		// add a materiallayerset without materials, just the thicknesses
		List<Entry<String, Double>> layers = new ArrayList<Entry<String, Double>>();
		layers.add(new AbstractMap.SimpleEntry<>(null, 0.25));
		layers.add(new AbstractMap.SimpleEntry<>(null, 0.5));
		layers.add(new AbstractMap.SimpleEntry<>(null, 0.25));
		factory.addMaterialLayerSet(layers);
		factory.addProductToModel(ifcModel);
		
		// add a materialList with just two materials
		factory.addMaterialList(Arrays.asList("Steel", "Brick"));
		factory.addProductToModel(ifcModel);
		
		collector.collectIfcModelObjects(ifcModel);
		
		// material names of layers are null
		assertEquals(null, collector.results().getObjects().get(0).getMaterials().get(0).getIfcName());
		assertEquals(null, collector.results().getObjects().get(0).getMaterials().get(0).getIfcName());
	}
	
	@Test
	public void testCollectorSetsVolumeToNaNForNonLayersWhenLayersArePresent()
	{
		factory.setGeometry(factory.getGeometryInfoMock(1, 1));
		factory.addMaterialLayer("brick", 1);
		factory.addMaterialLayer("rockwool", 3);
		factory.addMaterial("steel");
		factory.addProductToModel(ifcModel);
		
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(.25,collector.results().getObjects().get(0).getSubObjects().get(0).getVolume(), 1e-8);
		assertEquals(.75,collector.results().getObjects().get(0).getSubObjects().get(1).getVolume(), 1e-8);
		
		// as there are layers present and this material is added separately we cannot assign a volume to it.
		assertTrue(Double.isNaN(collector.results().getObjects().get(0).getSubObjects().get(2).getVolume()));
	}
	
	@Test
	public void testCollectorCollectsSpacesSeparately() {
		factory.setGeometry(factory.getGeometryInfoMock(1, 1));
		factory.addMaterialLayer("brick", 1);
		factory.addProductToModel(ifcModel);
		factory.setGeometry(factory.getGeometryInfoMock(1, 4.0));
		factory.addSpaceToModel(ifcModel);
		
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(4.0, collector.results().getSpaces().get(0).getVolume(), 1e-8);
		assertEquals(1.0, collector.results().getSpaces().get(0).getArea(), 1e-8);
	}
	
	@Test
	public void testCollectorOmitsChildSpacesFromVolumeTotal() {
		factory.setGeometry(factory.getGeometryInfoMock(1, 1));
		factory.addMaterialLayer("brick", 1);
		factory.addProductToModel(ifcModel);
		factory.setGeometry(factory.getGeometryInfoMock(1, 4.0));
		factory.addSpaceToModel(ifcModel);
		
		// add another space that is a child of the first one.
		factory.addSpaceToModel(ifcModel, ifcModel.getAllWithSubTypes(IfcProduct.class).get(1));
		
		collector.collectIfcModelObjects(ifcModel);
		assertEquals(4.0, collector.results().getSpaces().get(0).getVolume(), 1e-8);
		assertEquals(1, collector.results().getSpaces().size());
	}
	
	@Test
	public void testCollectorOmitsFurnishingElements() {
		factory.addGenericIfcProductToModel(ifcModel, IfcFurnishingElement.class, null);
		collector.collectIfcModelObjects(ifcModel);
		
		assertEquals(0, collector.results().getSpaces().size());
		assertEquals(0, collector.results().getObjects().size());
	}
	
	@Test
	public void testCollectorOmitsOpeningElements() {
		// door and window openings are not included in the floor plan area calculation
		factory.addGenericIfcProductToModel(ifcModel, IfcOpeningElement.class, null);
		assertEquals(0, collector.results().getSpaces().size());
		assertEquals(0, collector.results().getObjects().size());
	}
}
