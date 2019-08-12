package org.opensourcebim.services;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.plugins.PluginConfiguration;
import org.opensourcebim.ifccollection.MpgIfcObjectCollector;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.mapping.MappingDataServiceRestImpl;
import org.opensourcebim.mapping.NmdDataResolver;
import org.opensourcebim.mapping.NmdDataResolverImpl;

import nl.tno.bim.nmd.config.NmdConfigImpl;
import nl.tno.bim.nmd.services.Nmd2DataService;
import nl.tno.bim.nmd.services.Nmd3DataService;

public class IfcToMpgCollectionService extends IfcObjectCollectionBaseService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		// Get properties from ifcModel
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		this.setStore(matParser.collectIfcModelObjects(input, bimBotContext.getContextId()));
		
		// resolve any ifc to nmd coupling
		Path pPath = Paths.get(getPluginContext().getRootPath().toString());
		NmdDataResolver resolver = new NmdDataResolverImpl();
		resolver.setNmdService(new Nmd2DataService(new NmdConfigImpl(pPath)));
		resolver.setMappingService(new MappingDataServiceRestImpl());
		resolver.setStore(this.getStore());
		resolver.nmdToMpg();
				
		return this.toBimBotsJsonOutput(resolver.getStore(), "results object collection");
	}

	@Override
	public String getOutputSchema() {
		return "MPG_OBJECT_JSON_0_0_3";
	}
}
