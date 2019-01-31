package org.opensourcebim.services;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.plugins.PluginConfiguration;
import org.opensourcebim.ifccollection.MpgIfcObjectCollector;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.mpgcalculation.MpgCalculationResults;
import org.opensourcebim.mpgcalculation.MpgCalculator;
import org.opensourcebim.nmd.NmdDataResolver;
import org.opensourcebim.nmd.NmdDataResolverImpl;

public class MpgCalculationResultsService extends IfcObjectCollectionBaseService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		IfcModelInterface ifcModel = input.getIfcModel();

		// Get properties from ifcModel
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		MpgObjectStore ifcResults = matParser.collectIfcModelObjects(ifcModel);
		
		// resolve any ifc to nmd coupling
		NmdDataResolver resolver = new NmdDataResolverImpl();
		MpgObjectStore resolvedData = resolver.NmdToMpg(ifcResults);
		
		// calculate the mpg scores
		MpgCalculator calculator = new MpgCalculator();
		calculator.setObjectStore(resolvedData);
		MpgCalculationResults calcResults = calculator.calculate(75.0);
		
		return this.toBimBotsJsonOutput(calcResults, "mpg calculation results");
	}

	@Override
	public String getOutputSchema() {
		return "MPG_RESULTS_JSON_0_0_1";
	}
}