package org.opensourcebim.test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bimserver.bimbots.BimBotDummyContext;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.bimbots.BimBotsServiceInterface;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SDeserializerPluginConfiguration;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.shared.BimServerClientFactory;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

public class BimBotTest implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(BimBotTest.class);
	private BimServerClientFactory bimServerClientFactory;
	private Path path;
	private BimBotsServiceInterface bimBot;
	private UsernamePasswordAuthenticationInfo credentials;
	
	private JsonNode res;

	public BimBotTest(Path path, BimServerClientFactory bimServerClientFactory,
			UsernamePasswordAuthenticationInfo credentials, BimBotsServiceInterface bimBot) {
		this.path = path;
		this.bimServerClientFactory = bimServerClientFactory;
		this.credentials = credentials;
		this.bimBot = bimBot;
		this.res = null;
	}
	
	public JsonNode getResultsAsJson() {
		return this.res;
	}

	@Override
	public void run() {
		try (BimServerClientInterface client = bimServerClientFactory.create(credentials)) {
			String projectName = path.getFileName().toString();
			SProject project = client.getServiceInterface().getTopLevelProjectByName(projectName);

			if (project == null) {
				LOGGER.info("Create new project " + projectName);
				project = client.getServiceInterface().addProject(projectName, "ifc2x3tc1");
			} else {
				LOGGER.info("Project " + projectName + " already existed");
			}

			if (project.getLastRevisionId() == -1) {
				LOGGER.info("Checking-in " + projectName);
				SDeserializerPluginConfiguration deserializer = client.getServiceInterface()
						.getSuggestedDeserializerForExtension("ifc", project.getOid());
				client.checkinSync(project.getOid(), "Auto checkin for BIMbot test", deserializer.getOid(), false,
						path);
				project = client.getServiceInterface().getTopLevelProjectByName(projectName);
			} else {
				LOGGER.info("Revision already available");
			}

			try (IfcModelInterface model = client.getModel(project, project.getLastRevisionId(), true, false, true)) {
				BimBotsInput input = new BimBotsInput("", Files.readAllBytes(path));
				input.setIfcModel(model);
				BimBotsOutput bimBotsOutput = bimBot.runBimBot(input, new BimBotDummyContext(), null);

				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
				ObjectWriter writer = mapper.writer(printer);
				this.res = mapper.readTree(bimBotsOutput.getData());
				writer.writeValue(new File(path.toString().replace(".ifc", ".json")), this.res);

				LOGGER.info(bimBotsOutput.getTitle() + " " + bimBotsOutput.getData().length + "bytes");
				LOGGER.info("");
			}
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}
}