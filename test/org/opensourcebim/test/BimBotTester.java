package org.opensourcebim.test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bimserver.bimbots.BimBotsServiceInterface;
import org.bimserver.client.json.JsonBimServerClientFactory;
import org.bimserver.shared.BimServerClientFactory;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.bimserver.shared.exceptions.BimServerClientException;
import org.opensourcebim.ifccollection.IfcToMpgCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BimBotTester {
	private Path basePath;
	private static final Logger LOGGER = LoggerFactory.getLogger(BimBotTester.class);
	private BimServerClientFactory bimServerClientFactory;
	private UsernamePasswordAuthenticationInfo credentials;

	public BimBotTester(Path basePath, BimServerClientFactory bimServerClientFactory,
			UsernamePasswordAuthenticationInfo credentials) {
		this.basePath = basePath;
		this.bimServerClientFactory = bimServerClientFactory;
		this.credentials = credentials;
	}

	/**
	 * @param args 0: Local path to directory containing IFC files 1: Address of
	 *             BIMserver (including http(s):// and port) 2: Username 3: Password
	 */
	public static void main(String[] args) {
		try {
			BimBotTester bimBotTester = new BimBotTester(Paths.get(args[0]), new JsonBimServerClientFactory(args[1]),
					new UsernamePasswordAuthenticationInfo(args[2], args[3]));
			bimBotTester.start();
		} catch (BimServerClientException e) {
			LOGGER.error("", e);
		}
	}

	public void start() {
		BimBotsServiceInterface bimBot = new IfcToMpgCollectionService();

		// Increment the first 2 to enable concurrent processing for faster processing
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.HOURS,
				new ArrayBlockingQueue<>(1000));

		// walk recursively through all folders in 
		try {
			Files.walk(Paths.get(basePath.toUri()))
				.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ifc")).forEach(p -> {
					threadPoolExecutor.submit(new BimBotTest(p, bimServerClientFactory, credentials, bimBot));
				});
		} catch (IOException e) {
			e.printStackTrace();
		}

		threadPoolExecutor.shutdown();
		try {
			threadPoolExecutor.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		}
	}
}
