package org.opensourcebim.test;


import java.nio.file.Path;

import org.bimserver.bimbots.BimBotDummyContext;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.bimbots.BimBotsServiceInterface;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SDeserializerPluginConfiguration;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.Flow;
import org.bimserver.shared.BimServerClientFactory;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BimBotTest implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(BimBotTest.class);
	private BimServerClientFactory bimServerClientFactory;
	private Path path;
	private BimBotsServiceInterface bimBot;
	private UsernamePasswordAuthenticationInfo credentials;

	public BimBotTest(Path path, BimServerClientFactory bimServerClientFactory, UsernamePasswordAuthenticationInfo credentials, BimBotsServiceInterface bimBot) {
		this.path = path;
		this.bimServerClientFactory = bimServerClientFactory;
		this.credentials = credentials;
		this.bimBot = bimBot;
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
				SDeserializerPluginConfiguration deserializer = client.getServiceInterface().getSuggestedDeserializerForExtension("ifc", project.getOid());
				client.checkin(project.getOid(), "Auto checkin for BIMbot test", deserializer.getOid(), false, Flow.SYNC, path);
				project = client.getServiceInterface().getTopLevelProjectByName(projectName);
			} else {
				LOGGER.info("Revision already available");
			}
			try (IfcModelInterface model = client.getModel(project, project.getLastRevisionId(), true, false, true)) {
				BimBotsInput input = new BimBotsInput("", null);
				input.setIfcModel(model);
				BimBotsOutput bimBotsOutput = bimBot.runBimBot(input, new BimBotDummyContext(), null);
				LOGGER.info(bimBotsOutput.getTitle() + " " + bimBotsOutput.getData().length + "bytes");
				LOGGER.info("");
			}
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}
}