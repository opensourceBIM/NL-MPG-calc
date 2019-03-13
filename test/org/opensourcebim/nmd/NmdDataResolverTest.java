package org.opensourcebim.nmd;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.mapping.NmdDataResolverImpl;
import org.opensourcebim.mapping.ResolverSettings;

public class NmdDataResolverTest {

	private ObjectStoreBuilder builder;
	private NmdDataResolverImpl resolver;
	private List<NmdElement> testElements;
	
	public NmdDataResolverTest() {
	}

	@Before
	public void setUp() throws Exception {
		builder = new ObjectStoreBuilder();
		resolver = new NmdDataResolverImpl();
		resolver.setService(getMockNmdDb());
		resolver.setMappingService(getMockMappingService());
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
	
	@SuppressWarnings("serial")
	private NmdMappingDataService getMockMappingService() {
		// TODO Auto-generated method stub
		NmdMappingDataService service = mock(NmdMappingDataService.class);
		when(service.getKeyWordMappings(ResolverSettings.keyWordOccurenceMininum)).thenReturn(new HashMap<String, Long>() {
			{put("baksteen", (long)22);}
			{put("hout", (long)44);}
			{put("steel", (long)42);}
			{put("staal", (long)44);}
		});
		when(service.getApproximateMapForObject(any(MpgObject.class))).thenReturn(null);
		
		return service;
	}
}
