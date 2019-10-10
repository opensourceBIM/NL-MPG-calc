package org.opensourcebim.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.plugins.PluginConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.istack.logging.Logger;


public class FloorAreaService extends IfcObjectCollectionBaseService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration)
			throws BimBotsException {

		try {
			JsonNode json = FloorAreaService.getJsonFromBinaryData(input.getData());
        	return this.toBimBotsJsonOutput(json, "floor area");
		}
		catch (Exception e) { 
			Logger.getLogger(FloorAreaService.class).warning("could not parse floor area through voxel service: " + e.getMessage());
		}

		return this.toBimBotsJsonOutput(-1.0, "floor area");
	}

	@Override
	public String getOutputSchema() {
		return "VOXEL_FLOORAREA_JSON_0_0_1";
	}
	
	public static JsonNode getJsonFromBinaryData(byte[] ifcData) throws IOException {
		
		File f = File.createTempFile("bimbot_floorarea", ".ifc");
		f.deleteOnExit();
        OutputStream os = new FileOutputStream(f); 
        os.write(ifcData); 
        os.close(); 
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
		Properties pluginProperties = IfcObjectCollectionBaseService.loadProperties();
		String host_name = pluginProperties.get("voxelservice.host").toString();
		HttpPost uploadFile = new HttpPost("http://" + host_name + ":5000/gross_floor_area");
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("ifc", f,
                ContentType.MULTIPART_FORM_DATA, f.getAbsolutePath());
        builder.setContentType(ContentType.MULTIPART_FORM_DATA);
        HttpEntity multipart = builder.build();
        uploadFile.setEntity(multipart);
        HttpResponse response = httpClient.execute(uploadFile);
        
        JsonNode json = null;
        if (response != null && response.getStatusLine().getStatusCode() == 200) {
            HttpEntity entity = response.getEntity();
            ObjectMapper mapper = new ObjectMapper();
            json = mapper.readTree(EntityUtils.toString(entity, "UTF-8"));        	
        }
        return json;
	}
}
