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
import org.opensourcebim.mpgcalculation.MpgCalculationResults;
import org.opensourcebim.mpgcalculation.MpgCalculator;

import nl.tno.bim.nmd.config.NmdConfigImpl;
import nl.tno.bim.nmd.services.Nmd2DataService;

public class MpgCalculationResultsService extends IfcObjectCollectionBaseService {

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
		
		// calculate the mpg scores
		MpgCalculator calculator = new MpgCalculator();
		calculator.setObjectStore(resolver.getStore());
		MpgCalculationResults calcResults = calculator.calculate(75.0);
		
		return this.toBimBotsJsonOutput(calcResults, "mpg calculation results");
	}

	@Override
	public String getOutputSchema() {
		return "MPG_RESULTS_JSON_0_0_1";
	}
}