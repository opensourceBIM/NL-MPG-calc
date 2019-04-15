package org.opensourcebim.mapping;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicStatusLine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgInfoTagType;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.ObjectStoreBuilder;

import nl.tno.bim.mapping.domain.Mapping;
import nl.tno.bim.mapping.domain.MappingSet;
import nl.tno.bim.nmd.domain.NmdElement;
import nl.tno.bim.nmd.domain.NmdProductCard;
import nl.tno.bim.nmd.services.NmdDataService;

@SuppressWarnings("serial")
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
		
		resolver.setNmdService(getMockNmdDb());
		this.resolver.setStore(builder.getStore());
		resolver.setMappingService(getMockMappings());
	}

	@After
	public void tearDown() throws Exception {
		this.testElements.clear();
	}

	@Test
	public void testResolverDoesNotBreakOnEmptyStore() {
		this.resolver.setStore(null);
		this.resolver.nmdToMpg();
	}
	
	@Test
	public void testCanResolveSingleProduct() {
		builder.AddUnmappedMpgElement("baksteen muur", false,
				new HashMap<String, Double>() {{
				    put("baksteen", 1.0);
				}},
				new Double[] {1.0, 1.0, 1.0}, "21.12", "IfcWall", "");
		this.resolver.nmdToMpg();
		MpgElement el = this.resolver.getStore().getElementByName("baksteen muur");
		assertTrue(el.hasMapping());
	}
	
	@Test
	public void testCannotResolveProductWhenNoNlsfbCodeMatches() {
		builder.AddUnmappedMpgElement("baksteen muur", false,
				new HashMap<String, Double>() {{
				    put("baksteen", 1.0);
				}},
				new Double[] {1.0, 1.0, 1.0}, "21.99", "IfcWall", "");
		this.resolver.nmdToMpg();
		MpgElement el = this.resolver.getStore().getElementByName("baksteen muur");
		assertTrue(el.getMpgObject().getAllTags().stream().anyMatch(t -> t.getType().equals(MpgInfoTagType.nmdProductCardWarning)));
		assertFalse(el.hasMapping());
	}
	
	@Test
	public void testCannotResolveProductWhenThereIsNoProductForMappedElement() {
		builder.AddUnmappedMpgElement("test mpg element", false,
				new HashMap<String, Double>() {{
				    put("empty", 1.0);
				}},
				new Double[] {1.0, 1.0, 1.0}, "99.99", "IfcTest", "");
		this.resolver.nmdToMpg();
		MpgElement el = this.resolver.getStore().getElementByName("test mpg element");
		assertTrue(el.getMpgObject().getAllTags().stream().anyMatch(t -> t.getType().equals(MpgInfoTagType.nmdProductCardWarning)));
		assertFalse(el.hasMapping());
	}
	
	@Test
	public void testCanResolveProductWhenNlsfbCodeIsInProductNameDescription() {
		// Even though there is no NLsfb code defined we can still find one in the product name
		builder.AddUnmappedMpgElement("baksteen muur-21.12", false,
				new HashMap<String, Double>() {{
				    put("baksteen", 1.0);
				}},
				new Double[] {1.0, 1.0, 1.0}, "", "IfcWall", "");
		this.resolver.nmdToMpg();
		MpgElement el = this.resolver.getStore().getElementByName("baksteen muur-21.12");
		assertTrue(el.hasMapping());
	}
	
	@Test
	public void testCanResolveProductWhenNlsfbCodeIsInMaterialDescription() {
		// Even though there is no NLsfb code defined we can still find one in the product name
		builder.AddUnmappedMpgElement("baksteen muur", false,
				new HashMap<String, Double>() {{
				    put("21.12 baksteen", 1.0);
				}},
				new Double[] {1.0, 1.0, 1.0}, "", "IfcWall", "");
		this.resolver.nmdToMpg();
		MpgElement el = this.resolver.getStore().getElementByName("baksteen muur");
		assertTrue(el.hasMapping());
	}
	
	@Test
	public void testCanResolveProductWithoutMaterialButDescriptionHasKeyword() {
		// product name ahas baksteen as mapping keyword and therefore a material can be created
		builder.AddUnmappedMpgElement("baksteen muur", false,
				new HashMap<String, Double>(),
				new Double[] {1.0, 1.0, 1.0}, "21.12", "IfcWall", "");
		this.resolver.nmdToMpg();
		MpgElement el = this.resolver.getStore().getElementByName("baksteen muur");
		assertTrue(el.hasMapping());
	}
	
	
	private NmdDataService getMockNmdDb() {

		this.testElements = new ArrayList<NmdElement> ();
		
		NmdElement el1 = builder.createDummyNmdElement("heipalen", "28.1", -1);
		NmdProductCard card1 = builder.createDummyProductCard("standaard heipaal", 3, "m", 100, el1);
		card1.addProfileSet(builder.createUnitProfileSet("beton", "kg", 100, 400.0));
		card1.addProfileSet(builder.createUnitProfileSet("wapeningsstaal", "kg", 1000, 300.0));
		el1.addProductCard(card1);
		this.testElements.add(el1);	
		
		
		NmdElement el2 = builder.createDummyNmdElement("muren", "21.12", -1);
		NmdProductCard card21 = builder.createDummyProductCard("baksteen muur", 3, "m2", 75, el2);
		card21.addProfileSet(builder.createUnitProfileSet("baksteen", "kg", 75, 120.0));
		card21.addProfileSet(builder.createUnitProfileSet("metselspecie", "kg", 75, 24.0));
		el2.addProductCard(card21);
		NmdProductCard card22 = builder.createDummyProductCard("baksteen muur zwaar", 3, "m2", 75, el2);
		card22.addProfileSet(builder.createUnitProfileSet("baksteen", "kg", 75, 240.0));
		card22.addProfileSet(builder.createUnitProfileSet("metselspecie", "kg", 75, 24.0));
		el2.addProductCard(card22);
		NmdProductCard card23 = builder.createDummyProductCard("gipswand", 3, "m2", 50, el2);
		card23.addProfileSet(builder.createUnitProfileSet("gipspaneel", "kg", 50, 12));
		el2.addProductCard(card23);
		this.testElements.add(el2);
		
		NmdElement el3 = builder.createDummyNmdElement("empty element", "99.99", -1);
		this.testElements.add(el3);
		
		MockDataService db = new MockDataService();
		db.setElements(this.testElements);
		return db;
	}
	
	private MappingDataService getMockMappings() {
		MappingDataService mapService = mock(MappingDataService.class);
		when(mapService.getNlsfbMappings()).thenReturn(new HashMap<String, List<String>>());
		when(mapService.getKeyWordMappings(ResolverSettings.keyWordOccurenceMininum)).thenReturn(new HashMap<String, Long>() {{
			put("baksteen", (long)4321);
			put("beton", (long)1234);
			put("staal", (long)42);
			}
		});
		
		// for now omit any mappings.
		ResponseWrapper<Mapping> emptyMap = new ResponseWrapper<>(null, new BasicStatusLine(new ProtocolVersion("http", 1, 1), 404, ""));
		ResponseWrapper<MappingSet> emptyMapSet = new ResponseWrapper<>(null, new BasicStatusLine(new ProtocolVersion("http", 1, 1), 404, ""));
		when(mapService.getMappingById(any(Long.class))).thenReturn(emptyMap);
		when(mapService.getMappingSetByProjectIdAndRevisionId(any(Long.class), any(Long.class))).thenReturn(emptyMapSet);
		when(mapService.postMapping(any(Mapping.class))).thenReturn(emptyMap);
		when(mapService.postMappingSet(any(MappingSet.class))).thenReturn(emptyMapSet);
		when(mapService.getApproximateMapForObject(any(MpgObject.class))).thenReturn(emptyMap);
		return mapService;
	}
}
