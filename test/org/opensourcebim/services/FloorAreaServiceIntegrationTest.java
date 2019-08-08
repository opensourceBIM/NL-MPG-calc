package org.opensourcebim.services;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.bimserver.client.json.JsonBimServerClientFactory;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.bimserver.shared.exceptions.BimServerClientException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opensourcebim.test.BimBotTest;
import org.opensourcebim.services.FloorAreaService;

import com.fasterxml.jackson.databind.JsonNode;

import junit.framework.Assert;

@RunWith(Parameterized.class)
public class FloorAreaServiceIntegrationTest {

	String rootDir = Paths.get(System.getProperty("user.dir")).getParent()
			+ File.separator + "2018 BouwBesluit test files" + File.separator; 
	JsonBimServerClientFactory factory = null;
	UsernamePasswordAuthenticationInfo authInfo = null;
	private String fileName;
	private String relPath;
	
	
	@Parameterized.Parameters(name = "{0}/{1}")
	public static Iterable<Object[]> data() {
	    return Arrays.asList(new Object[][] {
	        {"project a", "3d bwk_K01K05_04-12-2017.ifc"},
	        {"Project B", "063_AP_DSB_BIM_CONSTR.ifc"},
	        {"Project C (BasisILS)", "model voorbeeld BasisILS.ifc"},
	        {"Project D", "18014_BWK_totaal.ifc"},
	    });
	}
	
	private Path getFullIfcModelPath() {
		return Paths.get(rootDir + this.relPath + File.separator + this.fileName);
	}
		
	public FloorAreaServiceIntegrationTest(String relPath, String filename) {
		this.relPath = relPath;
		this.fileName = filename;
		try {
			factory = new JsonBimServerClientFactory("http://localhost:8080");
		} catch (BimServerClientException e) {
			fail("bimserver not running");
		}
		authInfo = new UsernamePasswordAuthenticationInfo("admin@bimserver.org", "admin");
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testCanGetJsonFromFloorAreaService() {
		BimBotTest test = new BimBotTest(this.getFullIfcModelPath(), factory, authInfo,
				new FloorAreaService());
		test.run();
		JsonNode res = test.getResultsAsJson();
		Assert.assertTrue("Json not created or in incorrect format.",
				res != null);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testModelHasNonZeroFloorArea() {
		JsonNode res = null;
		try {
			res = FloorAreaService.getJsonFromBinaryData(Files.readAllBytes(this.getFullIfcModelPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertTrue("no response received", res != null);
		Assert.assertTrue("Floor area is zero", res.get("floor_area").asDouble() > 0);
	}
}
	
	
	