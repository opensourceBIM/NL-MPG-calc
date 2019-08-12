package org.opensourcebim.services;

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
import org.opensourcebim.mpgcalculation.MpgCalculationResults;
import org.opensourcebim.mpgcalculation.MpgCalculator;

import nl.tno.bim.nmd.config.NmdConfigImpl;
import nl.tno.bim.nmd.services.Nmd2DataService;
import nl.tno.bim.nmd.services.Nmd3DataService;

public class MpgCalculationResultsService extends IfcObjectCollectionBaseService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		// Get properties from ifcModel
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		this.setStore(matParser.collectIfcModelObjects(input, bimBotContext.getContextId()));
		
		// resolve any ifc to nmd coupling
		NmdDataResolver resolver = new NmdDataResolverImpl();
		
		resolver.setNmdService(new Nmd3DataService(new NmdConfigImpl(getPluginContext().getRootPath())));
		resolver.setMappingService(new MappingDataServiceRestImpl());
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