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
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
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
	public HashMap<String, String[]> getNlsfbMappings() {

		HashMap<String, String[]> elementMap = new HashMap<String, String[]>();
		elementMap.put("BuildingElementProxy", new String[] { "11.", "32.35", "32.36", "32.37", "32.38", "32.39" });
		elementMap.put("Footing", new String[] { "16." });
		elementMap.put("Slab", new String[] { "13.", "23.", "28.2", "28.3", "33.21" });
		elementMap.put("Pile", new String[] { "17." });
		elementMap.put("Column", new String[] { "17.", "28.1" });
		elementMap.put("Wall", new String[] { "21.", "22." });
		elementMap.put("WallStandardCase", new String[] { "21.", "22." });
		elementMap.put("CurtainWall", new String[] { "21.24", "32.4" });
		elementMap.put("Stair", new String[] { "24." });
		elementMap.put("Roof", new String[] { "27." });
		elementMap.put("Beam", new String[] { "28." });
		elementMap.put("Window", new String[] { "31.2", "32.12", "32.2", "37.2" });
		elementMap.put("Door", new String[] { "31.3", "32.11", "32.3" });
		elementMap.put("Railing", new String[] { "34." });
		elementMap.put("Covering", new String[] { "41.", "42.", "43.", "44.", "45.", "47.", "48." });
		elementMap.put("FlowSegment", new String[] { "52.", "53.", "55.", "56.", "57." }); // many more
		elementMap.put("FlowTerminal", new String[] { "74.1", "74.2" }); // many more

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
	 */
	private void regenerateMaterialKeyWords() {

		List<Path> listFiles = new ArrayList<Path>();
		try {
			Files.walk(Paths.get(config.getIFcFilesForKeyWordMapRootPath()))
					.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ifc")).forEach(p -> {
						listFiles.add(p);
					});
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		List<String[]> allList = new ArrayList<String[]>();
		for (Path path : listFiles) {

			List<String[]> fileList = new ArrayList<String[]>();
			Scanner scanner = new Scanner(path.toUri().toString());
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.contains("IFCMATERIAL")) {

					fileList.add(StringUtils.substringsBetween(line, "\'", "\'"));
				}
			}
			allList.addAll(fileList);
			scanner.close();
		}
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
						+ "	id integer PRIMARY KEY AUTOINCREMENT,\n" 
						+ " code text NOT NULL,\n"
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

		String valueString = StringUtils
				.join(values.stream().map(ar -> "('" + StringUtils.join(ar, "','") + "')").collect(Collectors.toList()), ",");
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
