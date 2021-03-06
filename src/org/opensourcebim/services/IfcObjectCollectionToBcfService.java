package org.opensourcebim.services;

import java.io.IOException;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.plugins.PluginConfiguration;
import org.opensourcebim.bcf.BcfException;
import org.opensourcebim.bcfexport.ObjectStoreToBcfConverter;
import org.opensourcebim.ifccollection.MpgIfcObjectCollector;

public class IfcObjectCollectionToBcfService extends IfcObjectCollectionBaseService {
	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		// Get properties from ifcModel
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		this.setStore(matParser.collectIfcModelObjects(input, bimBotContext.getContextId()));
		
		ObjectStoreToBcfConverter converter = new ObjectStoreToBcfConverter(this.getStore(), input);
		
		BimBotsOutput output = null;
		try {
			output = new BimBotsOutput(getOutputSchema(), converter.write().toBytes());
			output.setContentType("application/bcf");
			output.setTitle("ifc collection bcf output");
		} catch (BcfException | IOException e) {
			System.err.println("error occurred while exporting object store to BCF: " + e.getMessage());
		}
		
		return output;
	}

	@Override
	public String getOutputSchema() {
		return "BCF_ZIP_2_1";
	}
}
