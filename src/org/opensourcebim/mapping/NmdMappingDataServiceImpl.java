package org.opensourcebim.mapping;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import com.opencsv.CSVReader;

import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.nmd.NmdUserDataConfig;
import org.opensourcebim.nmd.NmdMappingDataService;

public class NmdMappingDataServiceImpl implements NmdMappingDataService {

	private Connection connection;
	private NmdUserDataConfig config;

	public NmdMappingDataServiceImpl(NmdUserDataConfig config) {
		this.config = config;
		connect();
	}

	@Override
	public void connect() {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + config.getMappingDbPath());
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			System.err.println(e.getMessage());
		}
	}

	@Override
	public void disconnect() {
		try {
			if (this.connection == null || this.connection.isClosed()) {
				this.connection.close();
			}
		} catch (SQLException e) {
			System.err.println("Error occured in disocnnecting from mapping service: " + e.getMessage());
		}
	}

	@Override
	public void addUserMap(NmdUserMap map) {
		// TODO Auto-generated method stub

	}

	@Override
	public NmdUserMap getApproximateMapForObject(MpgObject object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putNlsfbMapping(String ifcProductType, String[] codes, Boolean append) {
		// TODO Auto-generated method stub

	}

	@Override
	public HashMap<String, List<String>> getNlsfbMappings() {

		HashMap<String, List<String>> elementMap = new HashMap<String, List<String>>();
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);

			ResultSet rs = statement.executeQuery("Select * from ifc_to_nlsfb_map");
			System.out.println("");
			while (rs.next()) {
				String prodName = rs.getString("ifcproducttype").trim();
				String nlsfbCode = rs.getString("code").trim();
				elementMap.putIfAbsent(prodName, new ArrayList<String>());
				elementMap.get(prodName).add(nlsfbCode);
			}
			
			statement.close();
		} catch (SQLException e) {
			System.err.println("error querying nlsfb map : " + e.getMessage());
		}

		return elementMap;
	}

	public void regenerateMappingData() {
		regenerateIfcToNlsfbMappings();
		regenerateMaterialKeyWords();
	}

	/**
	 * re-evaluate a series of ifcFiles and check what kind of materials are present
	 * in these files. Based on the result determine the most common/important
	 * keywords to be used in the mapping
	 * 
	 * @throws FileNotFoundException
	 */
	private void regenerateMaterialKeyWords() {

		List<Path> foundFiles = new ArrayList<Path>();
		List<String> allMaterials = new ArrayList<String>();
		try {
			Files.walk(Paths.get(config.getIFcFilesForKeyWordMapRootPath()))
					.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ifc")).forEach(p -> {
						foundFiles.add(p);
					});

			for (Path path : foundFiles) {

				List<String> fileMaterials = new ArrayList<String>();
				Scanner scanner = new Scanner(new File(path.toAbsolutePath().toString()));
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					if (line.contains("IFCMATERIAL(")) {
						String[] matRes = StringUtils.substringsBetween(line, "\'", "\'");
						if (matRes != null) {
							for (int i = 0; i < matRes.length; i++) {
								if (matRes[i] != null) {
									List<String> words = Arrays.asList(matRes[i].split(" |-|,")).stream()
											.filter(w -> w != null && !w.isEmpty()).map(w -> w.toLowerCase())
											.collect(Collectors.toList());
									fileMaterials.addAll(words);
								}
							}
						}
					}
				}
				allMaterials.addAll(fileMaterials);
				scanner.close();
			}
		} catch (IOException e1) {
			System.err.println(e1.getMessage());
		}

		Map<String, Long> wordCount = allMaterials.stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		List<Entry<String, Long>> sortedWordCount = wordCount.entrySet().stream()
				.filter(w -> w.getKey().toCharArray().length > 2) // remove items that are simply too short
				.filter(w -> w.getKey().replaceAll("[^a-zA-Z]", "").length() != 0) // remove non textual words
				.sorted((w1, w2) -> {
					return w1.getValue().compareTo(w2.getValue());
				}).collect(Collectors.toList());
		System.out.println("");
	}

	/**
	 * reload the ifc to NLsfb mapping based on a csv of mapping codes.
	 */
	private void regenerateIfcToNlsfbMappings() {
		try {
			String tableName = "ifc_to_nlsfb_map";
			CSVReader reader = new CSVReader(new FileReader(config.getKeyWordCsvPath()));
			List<String[]> myEntries = reader.readAll();
			reader.close();

			String[] headers = myEntries.get(0);
			List<String[]> values = myEntries.subList(1, myEntries.size());
			if (headers.length == 2) {
				deleteTable(tableName);

				String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (\n"
						+ "	id integer PRIMARY KEY AUTOINCREMENT,\n" + " code text NOT NULL,\n"
						+ "	ifcproducttype text NOT NULL\n" + ");";
				this.executeAndCommitCommand(createTableSql);
				this.executeAndCommitCommand(createSqlInsertStatement(tableName, headers, values));

			} else {
				// our import file does not seem to be in the assumed structure.
				System.out.println("incorrect input data encountered.");
			}
		} catch (SQLException e) {
			System.err.println("incorrect sql command");
		} catch (FileNotFoundException e) {
			System.err.println("");
		} catch (IOException e) {

		}
	}

	private String createSqlInsertStatement(String table, String[] headers, List<String[]> values) {

		String valueString = StringUtils.join(
				values.stream().map(ar -> "('" + StringUtils.join(ar, "','") + "')").collect(Collectors.toList()), ",");
		return "INSERT INTO " + table + " (" + StringUtils.join(headers, ",") + ") VALUES " + valueString;
	}

	private void deleteTable(String tableName) throws SQLException {
		this.executeAndCommitCommand("DROP TABLE IF EXISTS " + tableName);
	}

	private void executeAndCommitCommand(String sqlCommand) throws SQLException {
		Statement statement = connection.createStatement();
		statement.setQueryTimeout(30);
		statement.executeUpdate(sqlCommand);
		statement.close();
	}
}
