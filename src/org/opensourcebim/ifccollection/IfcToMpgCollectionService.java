package org.opensourcebim.ifccollection;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.plugins.services.BimBotAbstractService;
import org.opensourcebim.nmd.BimMaterialDatabaseSession;
import org.opensourcebim.nmd.NmdDataBaseSession;
import org.opensourcebim.nmd.NmdDataResolver;
import org.opensourcebim.nmd.NmdDataResolverImpl;

public class IfcToMpgCollectionService extends BimBotAbstractService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, SObjectType settings)
			throws BimBotsException {

		IfcModelInterface ifcModel = input.getIfcModel();

		// Get properties from ifcModel
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
		MpgObjectStore ifcResults = matParser.collectIfcModelObjects(ifcModel);
		
		// notify user of any warnings:
		if(ifcResults.CheckForWarningsAndErrors()) {
			// return list of warnings
		} else {
			// return model has been read correctly.
		}
		
		// find matching material properties from Material DB
		NmdDataResolver nmdDataProvider = new NmdDataResolverImpl();
		nmdDataProvider.addService(new NmdDataBaseSession());
		nmdDataProvider.addService(new BimMaterialDatabaseSession());
		
		
		MpgObjectStore nmdResults = nmdDataProvider.NmdToMpg(ifcResults);
		
		// notify user of unknown materials
		
		
		// retrieve user input 
		
		// do calculations
		
		// TODO Auto-generated method stub
		return new BimBotsOutput(getOutputSchema(), new byte[0]);
	}

	@Override
	public String getOutputSchema() {
		// TODO put a more meaningful schema name here
		return "some useful content";
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
