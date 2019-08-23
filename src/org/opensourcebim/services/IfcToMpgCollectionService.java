package org.opensourcebim.services;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.plugins.PluginConfiguration;
import org.opensourcebim.ifccollection.MpgIfcObjectCollector;
import org.opensourcebim.mapping.MappingDataServiceRestImpl;
import org.opensourcebim.mapping.NmdDataResolver;
import org.opensourcebim.mapping.NmdDataResolverImpl;

import nl.tno.bim.nmd.config.NmdConfigImpl;
import nl.tno.bim.nmd.services.Nmd2DataService;
import nl.tno.bim.nmd.services.Nmd3DataService;
import nl.tno.bim.nmd.services.NmdDataService;

public class IfcToMpgCollectionService extends IfcObjectCollectionBaseService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		// Get properties from ifcModel
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		NmdDataResolver resolver;
		try {
			resolver = getNmdResolver();
		} catch (FileNotFoundException e) {
			return this.toBimBotsJsonOutput(e, "bot failed. Config not found");
		}
		
		this.setStore(matParser.collectIfcModelObjects(input, bimBotContext.getContextId()));
		resolver.setStore(this.getStore());
		resolver.nmdToMpg();
				
		return this.toBimBotsJsonOutput(resolver.getStore(), "results object collection");
	}

	@Override
	public String getOutputSchema() {
		return "MPG_OBJECT_JSON_0_0_3";
	}
}
