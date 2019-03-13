package org.opensourcebim.ifccollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.nmd.NmdMapping;
import org.opensourcebim.nmd.ObjectStoreBuilder;

public class MpgObjectStoreTest {

	private MpgObjectStore objectStore;
	private ObjectStoreBuilder builder;
	
	@Before
	public void setUp() throws Exception {
		objectStore = new MpgObjectStoreImpl();
		builder = new ObjectStoreBuilder();
	}

	@After
	public void tearDown() throws Exception { }

	@Test 
	public void testObjectStoreIsInitiallReturnsNoMaterials()
	{
		assertEquals(0, objectStore.getElements().size());
	}
	
	@Test
	public void testObjectStoreCanAddMaterials() {
		objectStore.addElement("steel");
		assertEquals(1, objectStore.getElements().size());
	}
			
	@Test
	public void testChangingAMaterialWillChangeAnyRelatedObjectMaterials() {
		MpgElement el = objectStore.addElement("dummyMaterial");
		
		MpgObject mpgObject = new MpgObjectImpl(1, "a", "custom wall", "Wall", "");
		mpgObject.addLayer(new MpgLayerImpl(2, 1.0, "dummyMaterial", Integer.toString("dummyMaterial".hashCode())));
		mpgObject.addMaterialSource("dummyMaterial", "", "layer");
		el.setMpgObject(mpgObject);
		objectStore.addObject(mpgObject);
		
		el.setMappingMethod(NmdMapping.DirectTotaalProduct);
		
		assertEquals(NmdMapping.DirectTotaalProduct,
				objectStore.getElementsByProductType("Wall").get(0).getMappingMethod());
	}
	
	@Test
	public void testVolumePerMaterialReturnsZeroOnNonExistingMaterial() {
		assertEquals(0.0, objectStore.getTotalVolumeOfMaterial("some non existing material"), 1e-8);
	}
	
	@Test 
	public void testVolumePerMaterialWithNoVolumesReturnZero() {
		objectStore.addElement("dummyMaterial");
		
		MpgObject mpgObject = new MpgObjectImpl(1, "a", "custom wall", "Wall", "");
		mpgObject.addLayer(new MpgLayerImpl(0, 1.0, "dummyMaterial", Integer.toString("dummyMaterial".hashCode())));
		mpgObject.addLayer(new MpgLayerImpl(0, 1.0, "dummyMaterial", Integer.toString("dummyMaterial".hashCode())));
		
		objectStore.addObject(mpgObject);
		assertEquals(0.0, objectStore.getTotalVolumeOfMaterial("dummyMaterial"), 1e-8);
		
		objectStore.addObject(mpgObject);
	}
	
	@Test
	public void testTotalVolumeWithDifferentMaterialsReturnsOnlySelectedMaterialSum() {
		objectStore.addElement("dummyMaterial");
		objectStore.addElement("ignoredMaterial");
		
		MpgObject mpgObject = new MpgObjectImpl(1, "a", "custom wall", "Wall", "");
		mpgObject.addLayer(new MpgLayerImpl(10, 1.0, "dummyMaterial", Integer.toString("dummyMaterial".hashCode())));
		mpgObject.addLayer(new MpgLayerImpl(10, 1.0, "ignoredMaterial", Integer.toString("ignoredMaterial".hashCode())));
		
		objectStore.addObject(mpgObject);
		assertEquals(10, objectStore.getTotalVolumeOfMaterial("dummyMaterial"), 1e-8);
		
		objectStore.addObject(mpgObject);
	}
	
	@Test
	public void testTotalAreaOfSpacesReturnsZeroOnNoSpaces() {
		assertEquals(0, objectStore.getTotalFloorArea(), 1e-8);
	}
	
	@Test
	public void testTotalAreaOfSpacesReturnsAreaOfSingleSpace() {
		objectStore.addSpace(new MpgSpaceImpl("a", 36, 12));
		assertEquals(12, objectStore.getTotalFloorArea(), 1e-8);
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnOrphanMaterials() {
		objectStore.addElement("orphan material");
		objectStore.addElement("a linked material");
		MpgObject mpgObject = new MpgObjectImpl(1, "a", "custom wall", "Wall", "");
		mpgObject.addLayer(new MpgLayerImpl(10, 1.0, "a linked material", Integer.toString("a linked material".hashCode())));
		objectStore.addObject(mpgObject);
				
		assertFalse("warning checker did not find the orphan material",
				objectStore.isIfcDataComplete());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnObjectWithoutLinkedMaterial() {
		MpgObject mpgObject = new MpgObjectImpl(1, "a", "custom wall", "Wall", "");
		mpgObject.addLayer(new MpgLayerImpl(10, 1.0, null, null));
		objectStore.addObject(mpgObject);
			
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.isIfcDataComplete());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnObjectWithoutLinkedMaterialAndOrphanMaterial() {
		objectStore.addElement("orphan material element");
		MpgObject mpgObject = new MpgObjectImpl(1, "a", "custom wall", "Wall", "");
		// we now add a layer with no material defined.
		mpgObject.addLayer(new MpgLayerImpl(10, 1.0, null, null));
		objectStore.addObject(mpgObject);
		
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.isIfcDataComplete());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnObjectWithRedundantMaterials() {
		MpgObject mpgObject = new MpgObjectImpl(1, "a", "custom wall", "Wall", "");
		mpgObject.addMaterialSource("steel", Integer.toString("steel".hashCode()), null );
		mpgObject.addMaterialSource("brick", Integer.toString("brick".hashCode()), null );
		objectStore.addObject(mpgObject);
		
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.isIfcDataComplete());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnPartiallyUndefinedMaterial() {
		objectStore.addElement("steel");
		MpgObject mpgObject1 = new MpgObjectImpl(1, "aaaa", "custom wall", "Wall", "");
		mpgObject1.addLayer(new MpgLayerImpl(10, 1.0, null, null));
		mpgObject1.addLayer(new MpgLayerImpl(10, 1.0, "steel", Integer.toString("steel".hashCode())));
		objectStore.addObject(mpgObject1);
		
		MpgObject mpgObject2 = new MpgObjectImpl(2, "bbbb", "custom wall", "Wall", "");
		mpgObject2.addLayer(new MpgLayerImpl(10, 1.0, "steel", Integer.toString("steel".hashCode())));
		objectStore.addObject(mpgObject2);
		
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.isIfcDataComplete());
		assertEquals(1, objectStore.getGuidsWithUndefinedLayerMats().getSize());
	}
	
	@Test
	public void testWarningCheckReturnsTrueWhenInformationIsComplete() {
		objectStore.addElement("test material");
		objectStore.addSpace(new MpgSpaceImpl("space_guid", 20, 60));

		MpgObjectImpl obj = new MpgObjectImpl(1, "a", "custom wall", "Wall", "");
		MpgLayerImpl layer = new MpgLayerImpl(10, 1.0, "test material", Integer.toString("test material".hashCode()));
		obj.addLayer(layer);
		
		// mock volume as this will not be added in this way
		obj.setGeometry(builder.createDummyGeom(1.0, 1.0, 1.0));
		objectStore.addObject(obj);
		
		assertTrue("warning found in objectstore while it should not be there.",
				objectStore.isIfcDataComplete());
	}
}
