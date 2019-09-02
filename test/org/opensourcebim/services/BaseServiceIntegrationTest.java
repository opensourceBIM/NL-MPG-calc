package org.opensourcebim.services;

import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import org.bimserver.bimbots.BimBotsServiceInterface;
import org.bimserver.client.json.JsonBimServerClientFactory;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.bimserver.shared.exceptions.BimServerClientException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.opensourcebim.ifccollection.MaterialSource;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgObjectStoreImpl;
import org.opensourcebim.ifccollection.ObjectStoreBuilder;
import org.opensourcebim.nmd.NmdProductCardReference;

@RunWith(Parameterized.class)
public class BaseServiceIntegrationTest<T extends BimBotsServiceInterface> {

	String rootDir = Paths.get(System.getProperty("user.dir")).getParent() + File.separator
			+ "2018 BouwBesluit test files" + File.separator;
	JsonBimServerClientFactory factory = null;
	UsernamePasswordAuthenticationInfo authInfo = null;
	protected String fileName;
	protected String relPath;
	protected Object referenceData;

	protected BimBotsServiceInterface bimbot;

	@SuppressWarnings("unchecked")
	protected T getService() {
		return (T) this.bimbot;
	}

	public BaseServiceIntegrationTest(String relPath, String fileName, Object referenceData) {
		this.relPath = relPath;
		this.fileName = fileName;
		this.referenceData = referenceData;
		try {
			factory = new JsonBimServerClientFactory("http://localhost:8080");
		} catch (BimServerClientException e) {
			fail("bimserver not running");
		}
		authInfo = new UsernamePasswordAuthenticationInfo("admin@bimserver.org", "admin");
	}

	@Parameterized.Parameters(name = "{0}/{1} with reference data {2}")
	public static Iterable<Object[]> data() {
	    return Arrays.asList(new Object[][] {
	        {"project a", "3d bwk_K01K05_04-12-2017.ifc", null},
	        {"Project B", "063_AP_DSB_BIM_CONSTR.ifc", null},
	        {"Project C (BasisILS)", "model voorbeeld BasisILS.ifc", getProjectCReferenceStore()},
	        {"Project D", "18014_BWK_totaal.ifc", null},
	    });
	}

	protected Path getFullIfcModelPath() {
		return Paths.get(rootDir + this.relPath + File.separator + this.fileName);
	}
	
	@SuppressWarnings("serial")
	private static MpgObjectStoreImpl getProjectCReferenceStore() {
		ObjectStoreBuilder factory = new ObjectStoreBuilder();
		MpgElement el;
		MaterialSource refSource = new MaterialSource(UUID.randomUUID().toString(), "dummy name", "reference source");
		el = factory.AddUnmappedMpgElement("16_Fundering_funderingsbalk", false,
				new HashMap<String, Double>() { {put("Beton gewapend C", 1.0);} },
				new Double[] {5.0, 0.4, 0.5}, "16.12", "IfcFooting", null);
		el.mapProductCard(refSource, new NmdProductCardReference(1000D));
		
		//String doorUUID = UUID.randomUUID().toString();
		factory.AddUnmappedMpgElement("32_Deur_S-01", false, 
				new HashMap<String, Double>(), // { {put("32_Loofhout", 1.0);} }
				new Double[] {0.986, 0.114, 2.1}, "32.31", "IfcDoor", null);
		
		String floorUUID = UUID.randomUUID().toString();
		factory.AddUnmappedMpgElement("Cement", false, 
				new HashMap<String, Double>() { {put("Cement", 1.0);} }, 
				new Double[] {4.88, 5.0, 0.05}, "23.21", "IfcBuildingElementPart", floorUUID);
		factory.AddUnmappedMpgElement("Beton gewapend prefab vloer", false, 
				new HashMap<String, Double>() { {put("Beton gewapend prefab vloer", 1.0);} }, 
				new Double[] {5.0, 5.0, 0.15}, "23.21", "IfcBuildingElementPart", floorUUID);
		factory.AddUnmappedMpgElement("Isolatie - Kunststof hard", false, 
				new HashMap<String, Double>() { {put("Isolatie - Kunststof hard", 1.0);} }, 
				new Double[] {5.0, 5.0, 0.10}, "23.21", "IfcBuildingElementPart", floorUUID);
		factory.AddUnmappedMpgElement("23_Vloer_begane grondvloer", false, 
				new HashMap<String, Double>(), // { {put("Kanaalplaat+dv iso 300", 1.0);} }
				new Double[] {5.0, 5.0, 0.3}, "23.21", "IfcSlab", null);
		
		String wallUUID = UUID.randomUUID().toString();
		factory.AddUnmappedMpgElement("Steen - Kalkzandsteen C", false, 
				new HashMap<String, Double>() { {put("Steen - Kalkzandsteen C", 1.0);} }, 
				new Double[] {5.0, 0.12, 2.98}, "21.22", "IfcBuildingElementPart", wallUUID);
		factory.AddUnmappedMpgElement("Steen - Baksteen AFW", false, 
				new HashMap<String, Double>() { {put("Steen - Baksteen AFW", 1.0);} }, 
				new Double[] {5.0, 0.1, 3.23}, "21.22", "IfcBuildingElementPart", wallUUID);
		factory.AddUnmappedMpgElement("Isolatie - Steenwol zacht", false, 
				new HashMap<String, Double>() { {put("Isolatie - Steenwol zacht", 1.0);} }, 
				new Double[] {5.0, 0.12, 3.23}, "21.22", "IfcBuildingElementPart", wallUUID);
		factory.AddUnmappedMpgElement("21_Wand_spouwmuur", false, 
				new HashMap<String, Double>(),
				new Double[] {5.0, 0.12, 3.23}, "21.22", "IfcWall", null);
		
		factory.AddUnmappedMpgElement("22_Wand_binnenwand", false, 
				new HashMap<String, Double>() {{put("Gipsblokken", 1.0);}},
				new Double[] {4.88, 0.1, 2.78}, "22.11", "IfcWall", null);
		
		factory.AddUnmappedMpgElement("31_Raam_H-01", false, 
				new HashMap<String, Double>(), // { {put("32_Loofhout", 1.0);} }
				new Double[] {0.9, 0.45, 1.638},  "31.22", "IfcWindow", null);
		
		
		factory.addSpace(25d, 2.5);
		
		return factory.getStore();
	}
}
