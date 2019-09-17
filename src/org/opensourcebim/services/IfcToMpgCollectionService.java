package org.opensourcebim.services;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.plugins.PluginConfiguration;
import org.opensourcebim.ifccollection.MpgIfcObjectCollector;
import org.opensourcebim.mapping.NmdDataResolver;

public class IfcToMpgCollectionService extends IfcObjectCollectionBaseService {
	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		LOGGER.warn("initializing object store and resolvers.");
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		NmdDataResolver resolver = getNmdResolver();
		
		LOGGER.warn("start collection of ifc objects for mpg calculation");
		this.setStore(matParser.collectIfcModelObjects(input, bimBotContext.getContextId()));
		resolver.setStore(this.getStore());
		
		LOGGER.warn("start resolving product cards");
		resolver.nmdToMpg();
				
		return this.toBimBotsJsonOutput(resolver.getStore(), "results object collection");
	}

	@Override
	public String getOutputSchema() {
		return "MPG_OBJECT_JSON_0_0_3";
	}
}
