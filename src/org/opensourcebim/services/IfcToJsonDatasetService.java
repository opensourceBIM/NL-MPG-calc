package org.opensourcebim.services;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.plugins.PluginConfiguration;
import org.opensourcebim.ifcanalysis.GuidDataSet;
import org.opensourcebim.ifccollection.MpgIfcObjectCollector;
import org.opensourcebim.ifccollection.MpgObjectStore;

public class IfcToJsonDatasetService extends IfcObjectCollectionBaseService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		IfcModelInterface ifcModel = input.getIfcModel();

		// Get properties from ifcModel
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		MpgObjectStore store = matParser.collectIfcModelObjects(ifcModel);
		
		GuidDataSet dataset = new GuidDataSet(store);
		
		return this.toBimBotsJsonOutput(dataset, "guid property dataset results");
	}

	@Override
	public String getOutputSchema() {
		return "GUID_PROPERTIES_DATASET_0_0_1";
	}
}
