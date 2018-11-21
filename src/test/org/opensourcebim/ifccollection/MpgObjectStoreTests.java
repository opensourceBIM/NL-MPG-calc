package test.org.opensourcebim.ifccollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgObjectGroup;
import org.opensourcebim.ifccollection.MpgObjectGroupImpl;
import org.opensourcebim.ifccollection.MpgObjectImpl;
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
		
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgObjectImpl(2, "dummyMaterial"));
		
		objectStore.addObjectGroup(group);
		objectStore.getMaterialByName("dummyMaterial").setBimBotIdentifier("some id");
		
		assertEquals("some id", objectStore.getMaterialsByProductType("Wall").get(0).getBimBotIdentifier());
	}
	
	@Test
	public void testVolumePerMaterialReturnsZeroOnNonExistingMaterial() {
		assertEquals(0.0, objectStore.GetTotalVolumeOfMaterial("some non existing material"), 1e-8);
	}
	
	@Test 
	public void testVolumePerMaterialWithNoVolumesReturnZero() {
		objectStore.addMaterial("dummyMaterial");
		
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgObjectImpl(0, "dummyMaterial"));
		group.addObject(new MpgObjectImpl(0, "dummyMaterial"));
		
		objectStore.addObjectGroup(group);
		assertEquals(0.0, objectStore.GetTotalVolumeOfMaterial("dummyMaterial"), 1e-8);
		
		objectStore.addObjectGroup(group);
	}
	
	@Test
	public void testTotalVolumeWithDifferentMaterialsReturnsOnlySelectedMaterialSum() {
		objectStore.addMaterial("dummyMaterial");
		objectStore.addMaterial("ignoredMaterial");
		
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgObjectImpl(10, "dummyMaterial"));
		group.addObject(new MpgObjectImpl(10, "ignoredMaterial" ));
		
		objectStore.addObjectGroup(group);
		assertEquals(10, objectStore.GetTotalVolumeOfMaterial("dummyMaterial"), 1e-8);
		
		objectStore.addObjectGroup(group);
	}
	
	@Test
	public void testTotalAreaOfSpacesReturnsZeroOnNoSpaces() {
		assertEquals(0, objectStore.getTotalFloorArea(), 1e-8);
	}
	
	@Test
	public void testTotalAreaOfSpacesReturnsAreaOfSingleSpace() {
		objectStore.addSpace(new MpgObjectImpl(36, 12));
		assertEquals(12, objectStore.getTotalFloorArea(), 1e-8);
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnOrphanMaterials() {
		objectStore.addMaterial("orphan material");
		objectStore.addMaterial("a linked material");
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgObjectImpl(10, "a linked material"));
		objectStore.addObjectGroup(group);
		
		assertFalse("warning checker did not find the ophan material",
				objectStore.CheckForWarningsAndErrors());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnObjectWithoutLinkedMaterial() {
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgObjectImpl(10, null));
		objectStore.addObjectGroup(group);
		
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.CheckForWarningsAndErrors());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnObjectWithoutLinkedMaterialAndOrphanMaterial() {
		objectStore.addMaterial("orphan material");
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgObjectImpl(10, null));
		objectStore.addObjectGroup(group);
		
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.CheckForWarningsAndErrors());
	}
	
	@Test
	public void testWarningCheckReturnsTrueWhenInformationIsComplete() {
		objectStore.addMaterial("test material");
		objectStore.addSpace(new MpgObjectImpl(20, 60));

		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall", objectStore);
		group.addObject(new MpgObjectImpl(10, "test material"));
		objectStore.addObjectGroup(group);
		
		assertTrue("warning found in objectstore while it should not be there.",
				objectStore.CheckForWarningsAndErrors());
	}
}
