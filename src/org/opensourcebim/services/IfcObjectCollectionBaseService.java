package org.opensourcebim.services;

import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.plugins.services.BimBotAbstractService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class IfcObjectCollectionBaseService extends BimBotAbstractService {
	
	@Override
	public boolean preloadCompleteModel() {
		return true;
	}
	
	@Override
	public boolean requiresGeometry() {
		return true;
	}
	
	@Override
    public boolean needsRawInput() {
        return true;
    }
	
	protected BimBotsOutput toBimBotsJsonOutput(Object results, String outputDescription) throws BimBotsException {
		// convert output with Jackon
		byte[] ifcJsonResults;
		try {
			ObjectMapper mapper = new ObjectMapper();
			ifcJsonResults = mapper.writeValueAsBytes(results);
			
			BimBotsOutput output = new BimBotsOutput(getOutputSchema(), ifcJsonResults);
			output.setContentType("application/json");
			output.setTitle(outputDescription);
			return output;
			
		} catch (JsonProcessingException e) {
			throw new BimBotsException("Unable to convert retrieved objects to Json", 500);
		}
	}
}
