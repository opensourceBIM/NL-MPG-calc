package org.opensourcebim.nmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgObjectStore;

/**
 * This implementation allows one of the services to be an editable data service
 * that also allows the user to add new data to be reused in other choices.
 * 
 * @author vijj
 *
 */
public class NmdDataResolverImpl implements NmdDataResolver {

	private List<NmdDataService> services;

	public NmdDataResolverImpl() {
		services = new ArrayList<NmdDataService>();
		NmdDatabaseConfig config = new NmdDatabaseConfigImpl();
		this.addService(new NmdDataBaseSession(config));

	}

	@Override
	public MpgObjectStore NmdToMpg(MpgObjectStore ifcResults) {

		MpgObjectStore nmdResults = ifcResults;

		try {
			// start any subscribed services
			for (NmdDataService nmdDataService : services) {
				nmdDataService.login();
				nmdDataService.preLoadData();
			}

			
			
			// get data per material - run through services in order
			for (MpgElement element : ifcResults.getElements()) {
				tryGetMaterialProperties(element);
			}

		} catch (ArrayIndexOutOfBoundsException ex) {
			System.out.println("Error occured in retrieving material data");
		}

		finally {
			for (NmdDataService nmdDataService : services) {
				nmdDataService.logout();
			}
		}

		return nmdResults;
	}

	private void tryGetMaterialProperties(MpgElement mpgElement) {
		NmdProductCard retrievedMaterial = null;
		HashMap<String, String[]> map = getProductToNmdMap();
		String[] emptyMap = null;
		for (NmdDataService nmdDataService : services) {
			
			// resolve which product card to retrieve based on the input MpgElement
			if (mpgElement.getMpgObject() == null)
			{
				return;
			}
			
			String ifcProductType = mpgElement.getMpgObject().getObjectType();
			String[] foundMap = map.getOrDefault(ifcProductType, emptyMap);
			if (foundMap == null) { break; }
			
			Optional<NmdProductCard> dbProduct = nmdDataService.getData().stream()
				.filter(el -> Arrays.stream(foundMap).anyMatch(code -> code == el.getNLsfbCode()))
				.flatMap(el -> el.getProducts().stream())
				.findFirst();
			if (!dbProduct.isPresent()) { break; }
			
			// create a copy
			retrievedMaterial = new NmdProductCardImpl(dbProduct.get());

			if (nmdDataService.getAdditionalProfileDataForCard(retrievedMaterial)) {
				mpgElement.setProductCard(retrievedMaterial);
				mpgElement.setMappingMethod(NmdMapping.Direct);
				break;
			}
		}
	}

	@Override
	public void addService(NmdDataService nmdDataService) {
		// check if same service is not already present?
		services.add(nmdDataService);

		if (nmdDataService instanceof EditableDataService) {
		// mark it as the editor of choice. only 1 editor should be available.
		}
	}
	
	private HashMap<String, String[]> getProductToNmdMap() {
		HashMap<String, String[]> productMap = new HashMap<String, String[]>();
		productMap.put("Footing", new String[]{"16."});
		productMap.put("Wall", new String[]{"21.", "22."});
		productMap.put("Slab", new String[]{"23."});
		productMap.put("Stair", new String[]{"24."});
		productMap.put("Roof", new String[]{"27."});
		productMap.put("Beam", new String[]{"28."});
		productMap.put("Window", new String[]{"31.2", "32.2"});
		productMap.put("Door", new String[]{"31.3", "32.3"});
	
		return productMap;
	}
	
}
