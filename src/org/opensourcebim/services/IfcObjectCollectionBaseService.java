package org.opensourcebim.services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.plugins.services.BimBotAbstractService;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.mapping.MappingDataServiceRestImpl;
import org.opensourcebim.mapping.NmdDataResolver;
import org.opensourcebim.mapping.NmdDataResolverImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.tno.bim.nmd.services.Nmd3DataService;

public abstract class IfcObjectCollectionBaseService extends BimBotAbstractService {

	private MpgObjectStore store = null;
    protected static Logger LOGGER = LoggerFactory.getLogger(IfcObjectCollectionBaseService.class);
    protected Properties pluginProperties = IfcObjectCollectionBaseService.loadProperties();
    
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
			LOGGER.info("write output to json");
			ObjectMapper mapper = new ObjectMapper();
			ifcJsonResults = mapper.writeValueAsBytes(results);

			BimBotsOutput output = new BimBotsOutput(getOutputSchema(), ifcJsonResults);
			output.setContentType("application/json");
			output.setTitle(outputDescription);
			return output;

		} catch (JsonProcessingException e) {
			LOGGER.warn("failed to convert object to Json");
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
		
		loadProperties();
		LOGGER.info("Initializing services");
		NmdDataResolver resolver = new NmdDataResolverImpl();
		try {
			resolver.setNmdService(Nmd3DataService.getInstance());
			MappingDataServiceRestImpl mapService = new MappingDataServiceRestImpl();
			mapService.setHost(this.pluginProperties.get("bimmapservice.host").toString());
			resolver.setMappingService(mapService);
		} catch (Exception e){
			LOGGER.warn("Could not initialize services for BimBots Service. Error encountered" );
			LOGGER.warn(e.getMessage());
		}
		
		return resolver;
	}
	
	private static Properties loadProperties() {
		Properties props = new Properties();
		try {
			Path propertyPath = getPropertyRootPath("mpg").resolve("application.properties");
			props.load(new FileInputStream(propertyPath.toAbsolutePath().toString()));
		} catch (IOException e) {
		  LOGGER.warn("Could not find properties file.");
		}
		return props;
	}
	
	protected static Path getPropertyRootPath(String service) {
		Path rootPath = null;
		if (System.getProperty("os.name").contains("Windows")) {
			rootPath = Paths.get("C:\\tmp\\" + service + "\\");
		} else {
			rootPath = Paths.get("/etc/" + service + "/");
		}
		return rootPath;
	}

}
