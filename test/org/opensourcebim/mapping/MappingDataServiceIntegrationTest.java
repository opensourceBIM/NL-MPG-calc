package org.opensourcebim.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.tno.bim.mapping.domain.Mapping;
import nl.tno.bim.mapping.domain.MappingSet;
import nl.tno.bim.mapping.domain.MappingSetMap;
import nl.tno.bim.mapping.domain.MaterialMapping;

/**
 * Test adding and retrieving objects from the Mapping Service 
 * 
 * @Prerequisite a BimMapService has to run and available on localhost and port given in constructor
 */
public class MappingDataServiceIntegrationTest {

	private MappingDataServiceRestImpl mapService;

	public MappingDataServiceIntegrationTest() {
		mapService = new MappingDataServiceRestImpl();
		// make sure we're not adding data to a dev or prod
		mapService.setHost("localhost");
		mapService.setPort(8085);

	}
	
	@Before
	public void setUp() throws Exception {
		mapService.connect();
	}

	@After
	public void tearDown() throws Exception {

        Statement stmt = null;
        Connection con = null;

        //Creates connection to database and clean up ables that we've just edited making sure each test starts with a clean db
        try {
        	Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bim-mapping-db", 
            		 "dummy_admin",  "dummy_pass");
            stmt = con.createStatement();

            stmt.executeUpdate("DELETE FROM mapping_set_map");
            stmt.executeUpdate("DELETE FROM material_mappings");
            stmt.executeUpdate("DELETE FROM mappings");
            stmt.executeUpdate("DELETE FROM mapping_set");
            con.close();
        } catch (Exception e) {
            System.out.println("error encountered cleaning the test db: " + e.getMessage());
        }
	}
	
	@Test
	public void testMapServiceCanAddMapping() {
		Mapping map = createDummyMap();
		ResponseWrapper<Mapping> res = mapService.postMapping(map);
		
		assertTrue(res.getObject().getId() != null && res.getObject().getId() > 0);
	}
	
	@Test
	public void testMapServiceCanGetMappingById() {
		// first we need to add a map
		Mapping map = createDummyMap();
		ResponseWrapper<Mapping> firstMap = mapService.postMapping(map);
		Long id = firstMap.getObject().getId();
				
		ResponseWrapper<Mapping> res = mapService.getMappingById(id);
		
		assertTrue(firstMap.getObject().getNlsfbCode().equals(res.getObject().getNlsfbCode()));
	}

	@Test
	public void testMapServiceCanAddMappingSet() {
		MappingSet set = new MappingSet();
		set.setProjectId("SomeProjectUUID");
		
		set.addMappingToMappingSet(createDummyMap(), "some guid");

		set = mapService.postMappingSet(set).getObject();
		assertTrue(set != null);
		assertTrue(set.getId() != null && set.getId() > 0);
	}
	
	@Test
	public void testMapServiceCannotAddMappingSetWithSameProjectAndRevisionId() {
		MappingSet set = new MappingSet();
		set.setProjectId("SomeProjectUUID");
		
		set.addMappingToMappingSet(createDummyMap(), "some guid");

		mapService.postMappingSet(set);
		ResponseWrapper<MappingSet> respSet = mapService.postMappingSet(set);
		
		assertFalse(respSet.succes());
	}
	
	@Test
	public void testMapServiceWillAddNewMappingSetWhenMappingSetIdIsNotPresent() {
		Mapping map = createDummyMap();
		
		MappingSet set1 = new MappingSet();
		set1.setProjectId("SomeProjectUUID");
		set1.addMappingToMappingSet(map, "some guid");
		ResponseWrapper<MappingSet> respSet1 = mapService.postMappingSet(set1);
		
		MappingSet set2 = new MappingSet();
		set1.setProjectId("SomeProjectUUID");
		set2.addMappingToMappingSet(map, "some other guid");
		ResponseWrapper<MappingSet> respSet2 = mapService.postMappingSet(set2);
		
		// check that there are two unique mappingsets created
		assertTrue(respSet2.succes());
		assertNotEquals(respSet1.getObject().getId(), respSet2.getObject().getId());
	}
	
	@Test
	public void testTwoMappingSetMapsWithSameMappingGetSameMappingRefence() {
		MappingSet set = new MappingSet();
		set.setProjectId("someProjectUUID");
		
		// have to post the map first to get an id (otherwise two different records will be created)
		Mapping map = createDummyMap();
		ResponseWrapper<Mapping> resp = mapService.postMapping(map);
		
		set.addMappingToMappingSet(resp.getObject(), "some guid");
		set.addMappingToMappingSet(resp.getObject(), "another guid");
		
		ResponseWrapper<MappingSet> respSet = mapService.postMappingSet(set);
		assertEquals(2, respSet.getObject().getMappingSetMaps().size());
		assertEquals(respSet.getObject().getMappingSetMaps().get(0).getMapping().getId(),
				respSet.getObject().getMappingSetMaps().get(1).getMapping().getId());
	}
	
	@Test
	public void testGetMappingSetByProjectIdAndRevisionId() {
		MappingSet set = new MappingSet();
		set.setProjectId("SomeProjectUUID");
		set.setDate(new Date());

		Mapping map = createDummyMap();
		set.addMappingToMappingSet(map, UUID.randomUUID().toString());
		set = mapService.postMappingSet(set).getObject();
		
		MappingSet sameSet = mapService.getMappingSetByProjectId("SomeProjectUUId").getObject();
		
		assertEquals(set.getId(), sameSet.getId());
		assertEquals(set.getMappingSetMaps().get(0).getMapping().getId(), sameSet.getMappingSetMaps().get(0).getMapping().getId());
	}
	
	@Test
	public void testSetNewMappingSetMapUpdatesMappingRevisionId() {
		MappingSet set = new MappingSet();
		set.setProjectId("SomeProjectUUID");
		set.setDate(new Date());

		String guid = UUID.randomUUID().toString();
		Mapping map = createDummyMap();
		set.addMappingToMappingSet(map, guid);
		set = mapService.postMappingSet(set).getObject();

		// add another mapping
		Mapping newMap = createDummyMap();
		newMap.setNmdTotaalProductId((long)99);
		set.addMappingToMappingSet(newMap, guid);
		
		// this post will return the full mappingset
		set = mapService.postMappingSet(set).getObject();
		
		assertEquals(2, set.getMappingSetMaps().size());
		assertFalse(set.getMappingSetMaps().get(0).getId() == set.getMappingSetMaps().get(1).getId());
		
		MappingSetMap revisedMapping = set.getMappingSetMaps().stream()
				.filter(msm -> msm.getMapping().getNmdTotaalProductId() != null )
				.findFirst().get();
		
		assertTrue(1 == revisedMapping.getMappingRevisionId());
		assertTrue(revisedMapping.getElementGuid().equals(guid));
	}
	
	@Test
	public void testCanRetrieveNLsfbMappings() {
		Map<String, List<String>> nlsfbMappings = mapService.getNlsfbMappings();
		assertTrue(nlsfbMappings != null);
	}
	
	@Test
	public void testCanRetrieveKeyWords() {
		Map<String, Long> keyWords = mapService.getKeyWordMappings(1);
		assertTrue(keyWords != null);
	}
	
	@Test
	public void testCanRetrieveCommonWords() {
		List<String> words = mapService.getCommonWords();
		assertTrue(words != null);
	}
	
	
	private Mapping createDummyMap() {
		Mapping map = new Mapping();
		map.setNlsfbCode("11.22");
		map.setNmdTotaalProductId(null);
		map.setOwnIfcType("IfcBuildingElementPart");
		map.setQueryIfcType("IfcWall");
		MaterialMapping mMap = new MaterialMapping();
		mMap.setMaterialName("prefab concrete");
		mMap.setNmdProductId((long)173);
		map.setMaterialMappings(Arrays.asList(mMap));
		return map;
	}
	
}
