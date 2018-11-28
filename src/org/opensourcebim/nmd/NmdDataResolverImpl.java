package org.opensourcebim.nmd;

import java.util.ArrayList;
import java.util.List;

import org.opensourcebim.ifccollection.MpgMaterial;
import org.opensourcebim.ifccollection.MpgObjectStore;

/**
 * This implementation allows one of the services to be an editable data service
 * that also allows the user to add new data to be reused in other choices.
 * 
 * @author vijj
 *
 */
public class NmdDataResolverImpl implements NmdDataResolverService {

	private List<NmdDataService> services;
	private EditableDataService editor;

	public NmdDataResolverImpl() {
		services = new ArrayList<NmdDataService>();

		this.addService(new NmdDataBaseSession());
		this.addService(new BimMaterialDatabaseSession());

	}

	@Override
	public MpgObjectStore NmdToMpg(MpgObjectStore ifcResults) {

		MpgObjectStore nmdResults = ifcResults;

		try {
			// start any subscribed services
			for (NmdDataService nmdDataService : services) {
				nmdDataService.start();
			}

			// get data per material - run through services in order
			for (MpgMaterial material : ifcResults.getMaterials().values()) {
				NmdProductCard nmdMaterial = tryGetMaterialProperties(material);
				
				if (nmdMaterial == null)
				{
					
				} else {
					ifcResults.setProductCardForMaterial(material.getIfcName(), nmdMaterial);
				}
					
			}

		} catch (ArrayIndexOutOfBoundsException ex) {
			System.out.println("Error occured in retrieving material data");
		}

		finally {
			for (NmdDataService nmdDataService : services) {
				nmdDataService.stop();
			}
		}

		return nmdResults;
	}

	private NmdProductCard tryGetMaterialProperties(MpgMaterial material) {
		NmdProductCard retrievedMaterial = null;
		for (NmdDataService nmdDataService : services) {
			retrievedMaterial = nmdDataService.retrieveMaterial(material);
			if (retrievedMaterial != null) {
				break;
			}
		}

		return retrievedMaterial;
	}

	@Override
	public void addService(NmdDataService nmdDataService) {
		// check if same service is not already present?
		services.add(nmdDataService);

		if (nmdDataService instanceof EditableDataService) {
			editor = (EditableDataService) nmdDataService;
		}
	}

}
