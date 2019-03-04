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
import org.opensourcebim.mapping.NmdDataResolverImpl;

public class NmdDataResolverTests {

	private ObjectStoreBuilder builder;
	private Nmd3DataService db;
	private NmdDataResolverImpl resolver;
	private List<NmdElement> testElements;

	public NmdDataResolverTests() {
	}

	@Before
	public void setUp() throws Exception {
		builder = new ObjectStoreBuilder();
		resolver = new NmdDataResolverImpl();
		db = getMockNmdDb();
		resolver.setService(db);
		this.resolver.setStore(builder.getStore());
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
		builder.addDummyElement1();
		this.resolver.NmdToMpg();
	}

	private Nmd3DataService getMockNmdDb() {

		this.testElements = new ArrayList<NmdElement> ();
		this.testElements.add(builder.createDummyElement());
		
		Nmd3DataService db = mock(Nmd3DataService.class);
		when(db.getIsConnected()).thenReturn(true);
		when(db.getData()).thenReturn(this.testElements);
		return db;
	}
}
