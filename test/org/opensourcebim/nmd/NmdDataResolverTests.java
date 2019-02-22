package org.opensourcebim.nmd;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgGeometry;
import org.opensourcebim.ifccollection.MpgObjectImpl;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.ifccollection.MpgObjectStoreImpl;

public class NmdDataResolverTests {

	private MpgObjectStore store;
	private NmdDataBaseSession db;
	private NmdDatabaseConfig config;
	private NmdDataResolverImpl resolver;
	private List<NmdElement> testElements;

	public NmdDataResolverTests() {
	}

	@Before
	public void setUp() throws Exception {
		resolver = new NmdDataResolverImpl();
		store = new MpgObjectStoreImpl();
		db = getMockNmdDb();
		resolver.setService(db);
		store = new MpgObjectStoreImpl();
		this.resolver.setStore(store);
	}

	@After
	public void tearDown() throws Exception {
		this.testElements.clear();
	}

	@Test
	public void testResolverDoesNotBreakOnEmptyStore() {
		this.resolver.setStore(null);
		this.resolver.NmdToMpg();
	}
	
	@Test
	public void testCanResolveSingleProduct() {
		addDummyElement1();
		
		this.resolver.NmdToMpg();
	}


	private void addDummyElement1() {
		store.addElement("baksteen");
		MpgObjectImpl obj = new MpgObjectImpl(1, "a", "heipaal", "Column", "");
		obj.setNLsfbCode("11.11");

		obj.setGeometry(createDummyGeom(1.0, 1.0, 5.0));
		store.addObject(obj);
		store.setObjectForElement("baksteen", obj);
	}

	private MpgGeometry createDummyGeom(double x, double y, double z) {
		MpgGeometry geom = new MpgGeometry();
		geom.setDimensions(x, y, z);
		geom.setVolume(x*y*z);
		geom.setFloorArea(x*y);
		geom.setIsComplete(true);
		return geom;
	}

	private NmdDataBaseSession getMockNmdDb() {

		this.testElements = new ArrayList<NmdElement> ();
		this.testElements.add(createDummyElement());
		
		NmdDataBaseSession db = mock(NmdDataBaseSession.class);
		when(db.getIsConnected()).thenReturn(true);
		when(db.getData()).thenReturn(this.testElements);
		return db;
	}

	private NmdElement createDummyElement() {
		NmdElementImpl el = new NmdElementImpl();
		el.setNlsfbCode("11.11");
		el.setElementId(11);
		el.setParentId(10);
		el.setElementName("heipalen");
		el.setIsMandatory(true);
		el.addProductCard(createDummyProductCard());
		return el;
	}

	private NmdProductCard createDummyProductCard() {
		return mock(NmdProductCard.class);
	}
}
