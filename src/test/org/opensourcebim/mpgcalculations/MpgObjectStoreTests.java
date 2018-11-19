package test.org.opensourcebim.mpgcalculations;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.mpgcalculations.MpgMaterial;
import org.opensourcebim.mpgcalculations.MpgObject;
import org.opensourcebim.mpgcalculations.MpgObjectGroup;
import org.opensourcebim.mpgcalculations.MpgObjectGroupImpl;
import org.opensourcebim.mpgcalculations.MpgObjectImpl;
import org.opensourcebim.mpgcalculations.MpgObjectStore;
import org.opensourcebim.mpgcalculations.MpgObjectStoreImpl;

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
	public void testChangingAMaterialWillChangeAnyLinkedObjectMaterials() {
		objectStore.addMaterial("dummyMaterial");
		MpgMaterial mat = objectStore.getMaterialByName("dummyMaterial");
		mat.setProperty("dummyProperty", 10.0);
				
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall");
		group.addObject(new MpgObjectImpl(2, mat));
		
		objectStore.addObjectGroup(group);
		assertEquals(10.0, objectStore.getObjectGroups().get(0).getObjects().get(0).getMaterial().getProperty("dummyProperty", Double.class), 1e-8);

		Double newVal = 20.0;
		mat.setProperty("dummyProperty", newVal);
		assertEquals(newVal, objectStore.getObjectGroups().get(0).getObjects().get(0).getMaterial().getProperty("dummyProperty", Double.class), 1e-8);
	}
	
	@Test
	public void testVolumePerMaterialReturnsZeroOnNonExistingMaterial() {
		assertEquals(0.0, objectStore.GetTotalVolumeOfMaterial("some non existing material"), 1e-8);
	}
	
	@Test 
	public void testVolumePerMaterialWithNoVolumesReturnZero() {
		objectStore.addMaterial("dummyMaterial");
		MpgMaterial mat = objectStore.getMaterialByName("dummyMaterial");
		
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall");
		group.addObject(new MpgObjectImpl(0, mat));
		group.addObject(new MpgObjectImpl(0, mat));
		
		objectStore.addObjectGroup(group);
		assertEquals(0.0, objectStore.GetTotalVolumeOfMaterial("dummyMaterial"), 1e-8);
		
		objectStore.addObjectGroup(group);
	}
	
	@Test
	public void testTotalVolumeWithDifferentMaterialsReturnsOnlyRSelectedMaterialSum() {
		objectStore.addMaterial("dummyMaterial");
		objectStore.addMaterial("ignoredMaterial");
		MpgMaterial mat = objectStore.getMaterialByName("dummyMaterial");
		MpgMaterial matIgnore = objectStore.getMaterialByName("ignoredMaterial");
		
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall");
		group.addObject(new MpgObjectImpl(10, mat));
		group.addObject(new MpgObjectImpl(10, matIgnore ));
		
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
		MpgMaterial mat = objectStore.getMaterialByName("a linked material");
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall");
		group.addObject(new MpgObjectImpl(10, mat));
		objectStore.addObjectGroup(group);
		
		assertFalse("warning checker did not find the ophan material",
				objectStore.CheckForWarningsAndErrors());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnObjectWithoutLinkedMaterial() {
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall");
		group.addObject(new MpgObjectImpl(10, null));
		objectStore.addObjectGroup(group);
		
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.CheckForWarningsAndErrors());
	}
	
	@Test
	public void testWarningCheckReturnsFalseOnObjectWithoutLinkedMaterialAndOrphanMaterial() {
		objectStore.addMaterial("orphan material");
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall");
		group.addObject(new MpgObjectImpl(10, null));
		objectStore.addObjectGroup(group);
		
		assertFalse("warning checker did not find an object with no material linked",
				objectStore.CheckForWarningsAndErrors());
	}
	
	@Test
	public void testWarningCheckReturnsTrueWhenInformationIsComplete() {
		objectStore.addMaterial("test material");
		objectStore.addSpace(new MpgObjectImpl(20, 60));
		MpgMaterial mat = objectStore.getMaterialByName("test material");
		MpgObjectGroup group = new MpgObjectGroupImpl(1, "a", "custom wall", "Wall");
		group.addObject(new MpgObjectImpl(10, mat));
		objectStore.addObjectGroup(group);
		
		assertTrue("warning found in objectstore while it should not be there.",
				objectStore.CheckForWarningsAndErrors());
	}
}
