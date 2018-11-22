package org.opensourcebim.ifccollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.MpgSubObjectImpl;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.ifccollection.MpgObjectStoreImpl;

public class MpgObjectStoreTests {

	private MpgObjectStore objectStore;
	
	@Before
	public void setUp() throws Exception {
		objectStore = new MpgObjectStoreImpl();
	}

	@After
	public void tearDown() throws Exception { }

	@Test 
	public void testObjectStoreIsInitiallReturnsNoMaterials()
	{
		assertEquals(0, objectStore.getAllMaterialNames().size());
	}
	
	@Test
	public void testObjectStoreCanAddMaterials() {
		objectStore.addMaterial("steel");
		assertEquals(1, objectStore.getAllMaterialNames().size());
	}
	
	@Test 
	public void testObjectStoreContainsOnlyUniqueMaterials() {
		objectStore.addMaterial("steel");
		objectStore.addMaterial("steel");
		
		assertEquals(1, objectStore.getAllMaterialNames().size());
	}
		
	@Test
	public void testChangingAMaterialWillChangeAnyRelatedObjectMaterials() {
		objectStore.addMaterial("dummyMaterial");
		
		MpgObject group = new MpgObjectImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgSubObjectImpl(2, "dummyMaterial"));
		
		objectStore.addObject(group);
		objectStore.getMaterialByName("dummyMaterial").setBimBotIdentifier("some id");
		
		assertEquals("some id", objectStore.getMaterialsByProductType("Wall").get(0).getBimBotIdentifier());
	}
	
	@Test
	public void testVolumePerMaterialReturnsZeroOnNonExistingMaterial() {
		assertEquals(0.0, objectStore.getTotalVolumeOfMaterial("some non existing material"), 1e-8);
	}
	
	@Test 
	public void testVolumePerMaterialWithNoVolumesReturnZero() {
		objectStore.addMaterial("dummyMaterial");
		
		MpgObject group = new MpgObjectImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgSubObjectImpl(0, "dummyMaterial"));
		group.addObject(new MpgSubObjectImpl(0, "dummyMaterial"));
		
		objectStore.addObject(group);
		assertEquals(0.0, objectStore.getTotalVolumeOfMaterial("dummyMaterial"), 1e-8);
		
		objectStore.addObject(group);
	}
	
	@Test
	public void testTotalVolumeWithDifferentMaterialsReturnsOnlySelectedMaterialSum() {
		objectStore.addMaterial("dummyMaterial");
		objectStore.addMaterial("ignoredMaterial");
		
		MpgObject group = new MpgObjectImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgSubObjectImpl(10, "dummyMaterial"));
		group.addObject(new MpgSubObjectImpl(10, "ignoredMaterial" ));
		
		objectStore.addObject(group);
		assertEquals(10, objectStore.getTotalVolumeOfMaterial("dummyMaterial"), 1e-8);
		
		objectStore.addObject(group);
	}
	
	@Test
	public void testTotalAreaOfSpacesReturnsZeroOnNoSpaces() {
		assertEquals(0, objectStore.getTotalFloorArea(), 1e-8);
	}
	
	@Test
	public void testTotalAreaOfSpacesReturnsAreaOfSingleSpace() {
		objectStore.addSpace(new MpgSubObjectImpl(36, 12));
		assertEquals(12, objectStore.getTotalFloorArea(), 1e-8);
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnOrphanMaterials() {
		objectStore.addMaterial("orphan material");
		objectStore.addMaterial("a linked material");
		MpgObject group = new MpgObjectImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgSubObjectImpl(10, "a linked material"));
		objectStore.addObject(group);
		
		assertFalse("warning checker did not find the ophan material",
				objectStore.isIfcDataComplete());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnObjectWithoutLinkedMaterial() {
		MpgObject group = new MpgObjectImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgSubObjectImpl(10, null));
		objectStore.addObject(group);
		
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.isIfcDataComplete());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnObjectWithoutLinkedMaterialAndOrphanMaterial() {
		objectStore.addMaterial("orphan material");
		MpgObject group = new MpgObjectImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgSubObjectImpl(10, null));
		objectStore.addObject(group);
		
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.isIfcDataComplete());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnPartiallyUndefinedMaterial() {
		objectStore.addMaterial("steel");
		MpgObject mpgObject1 = new MpgObjectImpl(1, "aaaa", "custom wall", "Wall", objectStore);
		mpgObject1.addObject(new MpgSubObjectImpl(10, null));
		mpgObject1.addObject(new MpgSubObjectImpl(10, "steel"));
		objectStore.addObject(mpgObject1);
		
		MpgObject mpgObject2 = new MpgObjectImpl(2, "bbbb", "custom wall", "Wall", objectStore);
		mpgObject2.addObject(new MpgSubObjectImpl(10, "steel"));
		objectStore.addObject(mpgObject2);
		
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.isIfcDataComplete());
		assertEquals(1, objectStore.getObjectGuidsWithPartialMaterialDefinition().size());
	}
	
	@Test
	public void testWarningCheckReturnsTrueWhenInformationIsComplete() {
		objectStore.addMaterial("test material");
		objectStore.addSpace(new MpgSubObjectImpl(20, 60));

		MpgObject group = new MpgObjectImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgSubObjectImpl(10, "test material"));
		objectStore.addObject(group);
		
		assertTrue("warning found in objectstore while it should not be there.",
				objectStore.isIfcDataComplete());
	}
}
