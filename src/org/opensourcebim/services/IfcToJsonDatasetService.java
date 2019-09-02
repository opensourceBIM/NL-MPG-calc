package org.opensourcebim.services;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.plugins.PluginConfiguration;
import org.opensourcebim.ifcanalysis.GuidDataSet;
import org.opensourcebim.ifccollection.MpgIfcObjectCollector;

public class IfcToJsonDatasetService extends IfcObjectCollectionBaseService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		// Get properties from ifcModel
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		this.setStore(matParser.collectIfcModelObjects(input, bimBotContext.getContextId()));
		
		GuidDataSet dataset = new GuidDataSet(this.getStore());
		
		return this.toBimBotsJsonOutput(dataset, "guid property dataset results");
	}

	@Override
	public String getOutputSchema() {
		return "GUID_PROPERTIES_DATASET_0_0_1";
	}
}
