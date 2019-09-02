package org.opensourcebim.services;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.plugins.services.BimBotAbstractService;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.mapping.MappingDataServiceRestImpl;
import org.opensourcebim.mapping.NmdDataResolver;
import org.opensourcebim.mapping.NmdDataResolverImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.tno.bim.nmd.services.Nmd3DataService;

public abstract class IfcObjectCollectionBaseService extends BimBotAbstractService {

	private MpgObjectStore store = null;

	@Override
	public boolean preloadCompleteModel() {
		return true;
	}

	@Override
	public boolean requiresGeometry() {
		return true;
	}

	@Override
	public boolean needsRawInput() {
		return true;
	}

	protected BimBotsOutput toBimBotsJsonOutput(Object results, String outputDescription) throws BimBotsException {
		// convert output with Jackon
		byte[] ifcJsonResults;
		try {
			ObjectMapper mapper = new ObjectMapper();
			ifcJsonResults = mapper.writeValueAsBytes(results);

			BimBotsOutput output = new BimBotsOutput(getOutputSchema(), ifcJsonResults);
			output.setContentType("application/json");
			output.setTitle(outputDescription);
			return output;

		} catch (JsonProcessingException e) {
			throw new BimBotsException("Unable to convert retrieved objects to Json", 500);
		}
	}

	public MpgObjectStore getStore() {
		return store;
	}

	public void setStore(MpgObjectStore store) {
		this.store = store;
	}

	/**
	 * Get the NMD resolver with the specific config required for storing the
	 * resolved elements.
	 * 
	 * @return a preconfigured NMDDataResolver
	 * @throws FileNotFoundException
	 */
	protected NmdDataResolver getNmdResolver() {
		// the path is relative to the project it is called from. therefore 
		// some existence checks need to be done to make sure we can find a config file.
		Path pPath;
        if (getPluginContext() == null) {
        	pPath = Paths.get(System.getProperty("user.dir"));
        } else {
            pPath = getPluginContext().getRootPath().getParent();
            if (Files.notExists(pPath.resolve("config.xml"))) {
                pPath = getPluginContext().getRootPath();
            }
        }
		
		NmdDataResolver resolver = new NmdDataResolverImpl();
		resolver.setNmdService(Nmd3DataService.getInstance(pPath));
		resolver.setMappingService(new MappingDataServiceRestImpl());
		return resolver;
	}

}
