package test.org.opensourcebim.mpgcalculations;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.mpgcalculations.MpgMaterial;
import org.opensourcebim.mpgcalculations.MpgObjectGroup;
import org.opensourcebim.mpgcalculations.MpgObjectGroupImpl;
import org.opensourcebim.mpgcalculations.MpgObjectImpl;
import org.opensourcebim.mpgcalculations.MpgObjectStore;
import org.opensourcebim.mpgcalculations.MpgObjectStoreImpl;

public class MpgObjectStoreTests {

	private MpgObjectStore objectStore;
	
	@Before
	public void setUp() throws Exception {
		setObjectStore(new MpgObjectStoreImpl());
	}

	@After
	public void tearDown() throws Exception { }

	@Test
	public void testObjectStoreCanAddMaterials() {
		getObjectStore().addMaterial("steel");
		assertEquals(1, getObjectStore().getAllMaterialNames().size());
	}
	
	@Test 
	public void testObjectStoreContainsOnlyUniqueMaterials() {
		getObjectStore().addMaterial("steel");
		getObjectStore().addMaterial("steel");
		
		assertEquals(1, getObjectStore().getAllMaterialNames().size());
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
	
	

	public MpgObjectStore getObjectStore() {
		return objectStore;
	}

	public void setObjectStore(MpgObjectStore objectStore) {
		this.objectStore = objectStore;
	}
	

}
