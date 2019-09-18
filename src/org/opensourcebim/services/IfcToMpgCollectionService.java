package org.opensourcebim.services;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.plugins.PluginConfiguration;
import org.opensourcebim.ifccollection.MpgIfcObjectCollector;
import org.opensourcebim.mapping.NmdDataResolver;

public class IfcToMpgCollectionService extends IfcObjectCollectionBaseService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		// Get properties from ifcModel
		MpgIfcObjectCollector matParser = new MpgIfcObjectCollector();
	    IfcProject proj = input.getIfcModel().getAllWithSubTypes(IfcProject.class).get(0);
        String pid = "";
        if (proj != null) {
        	pid = Long.toString(proj.getOid());
        }
        else {
        	pid = bimBotContext.getContextId();
        }
	
		this.setStore(matParser.collectIfcModelObjects(input, pid));
		NmdDataResolver resolver = getNmdResolver();
		resolver.setStore(this.getStore());
		LOGGER.info("Start NMD resolving process");
		resolver.nmdToMpg();
				
		return this.toBimBotsJsonOutput(resolver.getStore(), "results object collection");
	}

	@Override
	public String getOutputSchema() {
		return "MPG_OBJECT_JSON_0_0_3";
	}
}
