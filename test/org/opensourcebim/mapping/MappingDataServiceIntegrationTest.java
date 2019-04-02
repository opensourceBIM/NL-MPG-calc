package org.opensourcebim.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.nmd.NmdUserDataConfigImpl;

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
		mapService = new MappingDataServiceRestImpl(new NmdUserDataConfigImpl());
		// make sure we're not adding data to a dev or prod
		mapService.setHost("localhost");
		mapService.setPort(8090);
	}
	
	@Before
	public void setUp() throws Exception { }

	@After
	public void tearDown() throws Exception {

        Statement stmt = null;
        Connection con = null;

        //Creates connection to database and clean up ables that we've just edited making sure each test starts with a clean db
        try {
        	Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bim-mapping-db", 
            		 "bbAdmin",  "bbPass_8737812hoih98h");
            stmt = con.createStatement();

            stmt.executeUpdate("DELETE FROM material_mappings");
            stmt.executeUpdate("DELETE FROM mapping_set_map");
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
		map = mapService.postMapping(map);
		
		assertTrue(map.getId() != null && map.getId() > 0);
	}
	
	@Test
	public void testMapServiceCanGetMappingById() {
		// first we need to add a map
		Mapping map = createDummyMap();
		map = mapService.postMapping(map);
		Long id = map.getId();
				
		Mapping sameMap = mapService.getMappingById(id);
		
		assertTrue(map.getNlsfbCode().equals(sameMap.getNlsfbCode()));
	}

	@Test
	public void testMapServiceCanAddMappingSet() {
		MappingSet set = new MappingSet();
		set.setProjectId((long)1);
		set.setRevisionId((long)1);
		
		set.addMappingToMappingSet(createDummyMap(), "some guid");

		set = mapService.postMappingSet(set);
		assertTrue(set != null);
		assertTrue(set.getId() != null && set.getId() > 0);
	}
	
	@Test
	public void testTwoMappingSetMapsWithSameMappingGetSameMappingRefence() {
		MappingSet set = new MappingSet();
		set.setProjectId((long)1);
		set.setRevisionId((long)1);
		
		// have to post the map first to get an id (otherwise two different records will be created)
		Mapping map = createDummyMap();
		map = mapService.postMapping(map);
		
		set.addMappingToMappingSet(map, "some guid");
		set.addMappingToMappingSet(map, "another guid");
		
		set = mapService.postMappingSet(set);
		assertEquals(2, set.getMappingSetMaps().size());
		assertEquals(set.getMappingSetMaps().get(0).getMapping().getId(), set.getMappingSetMaps().get(1).getMapping().getId());
	}
	
	@Test
	public void testGetMappingSetByProjectIdAndRevisionId() {
		MappingSet set = new MappingSet();
		set.setProjectId((long)1);
		set.setRevisionId((long)1);
		set.setDate(new Date());

		Mapping map = createDummyMap();
		set.addMappingToMappingSet(map, UUID.randomUUID().toString());
		set = mapService.postMappingSet(set);
		
		MappingSet sameSet = mapService.getMappingSetByProjectIdAndRevisionId((long)1, (long)1);
		
		assertEquals(set.getId(), sameSet.getId());
		assertEquals(set.getMappingSetMaps().get(0).getMapping().getId(), sameSet.getMappingSetMaps().get(0).getMapping().getId());
	}
	
	@Test
	public void testSetNewMappingSetMapUpdatesMappingRevisionId() {
		MappingSet set = new MappingSet();
		set.setProjectId((long)1);
		set.setRevisionId((long)1);
		set.setDate(new Date());

		String guid = UUID.randomUUID().toString();
		Mapping map = createDummyMap();
		set.addMappingToMappingSet(map, guid);
		set = mapService.postMappingSet(set);

		// add another mapping
		Mapping newMap = createDummyMap();
		newMap.setNmdTotaalProductId((long)99);
		set.addMappingToMappingSet(newMap, guid);
		
		// this post will return the full mappingset
		set = mapService.postMappingSet(set);
		
		assertEquals(2, set.getMappingSetMaps().size());
		assertFalse(set.getMappingSetMaps().get(0).getId() == set.getMappingSetMaps().get(1).getId());
		
		MappingSetMap revisedMapping = set.getMappingSetMaps().stream()
				.filter(msm -> msm.getMapping().getNmdTotaalProductId() != null )
				.findFirst().get();
		
		assertTrue(1 == revisedMapping.getMappingRevisionId());
		assertTrue(revisedMapping.getElementGuid().equals(guid));
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
