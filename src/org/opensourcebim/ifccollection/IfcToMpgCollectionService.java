package org.opensourcebim.ifccollection;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.services.BimBotAbstractService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IfcToMpgCollectionService extends BimBotAbstractService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		IfcModelInterface ifcModel = input.getIfcModel();

		// Get properties from ifcModel
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		MpgObjectStore ifcResults = matParser.collectIfcModelObjects(ifcModel);
		
		// notify user of any warnings:
		ifcResults.isIfcDataComplete();
		
		ifcResults.SummaryReport();
		
		// convert output with Jackon
		byte[] ifcJsonResults;
		try {
			ObjectMapper mapper = new ObjectMapper();
			ifcJsonResults = mapper.writeValueAsBytes(ifcResults);
			
			BimBotsOutput output = new BimBotsOutput(getOutputSchema(), ifcJsonResults);
			output.setContentType("application/json");
			output.setTitle("results mpg check");
			return output;
			
		} catch (JsonProcessingException e) {
			throw new BimBotsException("Unable to convert retrieved objects to Json", 500);
		}
	}

	@Override
	public String getOutputSchema() {
		return "MPG_OBJECT_JSON_V0.0.1";
	}
	
	@Override
	public boolean preloadCompleteModel() {
		return true;
	}
	
	@Override
	public boolean requiresGeometry() {
		return true;
	}
}
