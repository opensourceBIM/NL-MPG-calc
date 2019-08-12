package org.opensourcebim.services;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opensourcebim.test.BimBotTest;
import org.opensourcebim.services.FloorAreaService;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Assert;

@RunWith(Parameterized.class)
public class FloorAreaServiceIntegrationTest extends BaseServiceIntegrationTest<FloorAreaService> {
	
	public FloorAreaServiceIntegrationTest(String relPath, String filename, Object referenceData) {
		super(relPath, filename, referenceData);
		this.bimbot = new FloorAreaService();
	}
	
	@Test
	public void testCanGetJsonFromFloorAreaService() {
		BimBotTest test = new BimBotTest(this.getFullIfcModelPath(), factory, authInfo,
				this.bimbot);
		test.run();
		JsonNode res = test.getResultsAsJson();
		Assert.assertTrue("Json not created or in incorrect format.",
				res != null);
	}
	
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
	
	
	