package org.opensourcebim.services;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.plugins.PluginConfiguration;
import org.opensourcebim.ifccollection.MpgIfcObjectCollector;
import org.opensourcebim.mapping.NmdDataResolver;
import org.opensourcebim.mpgcalculation.MpgCalculationResults;
import org.opensourcebim.mpgcalculation.MpgCalculator;


public class MpgCalculationResultsService extends IfcObjectCollectionBaseService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		// Get properties from ifcModel
		
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		NmdDataResolver resolver = getNmdResolver();
		this.setStore(matParser.collectIfcModelObjects(input, bimBotContext.getContextId()));
		
		LOGGER.warn("start resolving product cards");
		resolver.setStore(this.getStore());
		resolver.nmdToMpg();
		
		// calculate the mpg scores
		LOGGER.warn("start collection of ifc objects for mpg calculation");
		MpgCalculator calculator = new MpgCalculator();
		calculator.setObjectStore(resolver.getStore());
		
		LOGGER.warn("start mpg calculation");
		MpgCalculationResults calcResults = calculator.calculate(75.0);
		
		return this.toBimBotsJsonOutput(calcResults, "mpg calculation results");
	}

	@Override
	public String getOutputSchema() {
		return "MPG_RESULTS_JSON_0_0_1";
	}
}