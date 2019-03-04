package org.opensourcebim.mapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

/**
 * Class to provide an interface between java code and a mapping database
 * The mapping database will store any data that is required to resolve what (Nmd) products to choose
 * based on ifc file data.
 * @author vijj
 * 
 */
public class NmdMappingDataServiceImpl extends SqliteDataService implements NmdMappingDataService {

	private String mat_keyword_table = "ifc_material_keywords";
	private String ifc_to_nlsfb_table = "ifc_to_nlsfb_map";
	private String common_word_dutch_table = "common_words_dutch_table";

	public NmdMappingDataServiceImpl(NmdUserDataConfig config) {
		super(config);
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
	public HashMap<String, List<String>> getNlsfbMappings() {

		HashMap<String, List<String>> elementMap = new HashMap<String, List<String>>();
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);

			ResultSet rs = statement.executeQuery("Select * from " + this.ifc_to_nlsfb_table);
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
	
	@Override
	public Map<String, Long> getKeyWordMappings(Integer minOccurence) {
		HashMap<String, Long> keyWordMap = new HashMap<String, Long>();
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);

			ResultSet rs = statement.executeQuery(
					"Select * from " + this.mat_keyword_table + " "
					+ "where count >= " + minOccurence.toString());
			System.out.println("");
			while (rs.next()) {
				String keyWord = rs.getString("keyword").trim();
				Long keyCount = (long)rs.getInt("count");
				keyWordMap.putIfAbsent(keyWord, keyCount);
			}

			statement.close();
		} catch (SQLException e) {
			System.err.println("error querying keyword map : " + e.getMessage());
		}
		return keyWordMap;
	}

	public void regenerateMappingData() {
		createCommonWordsTable();
		regenerateMaterialKeyWords();
		regenerateIfcToNlsfbMappings();
	}

	/**
	 * load common word files into the database. These can be used to remove often used words from keyword selection
	 */
	private void createCommonWordsTable() {
		try {
			String tableName = this.common_word_dutch_table ;
			CSVReader reader = new CSVReader(new FileReader(config.getCommonWordDutchFilePath()));
			List<String[]> myEntries = reader.readAll();
			reader.close();

			String[] headers = myEntries.get(0);
			List<String[]> values = myEntries.subList(1, myEntries.size());
			if (headers.length == 1) {
				deleteTable(tableName);

				String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (\n"
						+ "	id integer PRIMARY KEY AUTOINCREMENT,\n"
						+ "	word text NOT NULL\n" + ");";
				this.executeAndCommitCommand(createTableSql);
				this.executeAndCommitCommand(createSqlInsertStatement(tableName, headers, values));

			} else {
				// our import file does not seem to be in the assumed structure.
				System.out.println("incorrect input data encountered.");
			}
		} catch (SQLException|IOException e) {
			System.err.println("error occured with creating common word table " + e.getMessage());
		}
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
					.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ifc"))
					.filter(p -> {
						// filter on max file size. example: 50 MB limit on 5e7
						return (new File(p.toAbsolutePath().toString())).length() < 1e8; 
					})
					.forEach(p -> {
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
									List<String> words = Arrays.asList(matRes[i].split(" |-|,|_|\\|(|)|<|>|:|;")).stream()
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
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		
		List<String> common_words = getCommonWordsDutch();
		Map<String, Long> wordCount = allMaterials.stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		List<Entry<String, Long>> filteredWordCount = wordCount.entrySet().stream()
				.filter(w -> w.getKey().toCharArray().length > 2) // remove items that are simply too short
				.filter(w -> 2 * w.getKey().replaceAll("[^a-zA-Z]", "").length() >= w.getKey().length() ) // remove items with too large ratio of non-text content
				.filter(w -> !common_words.contains(w.getKey()))
				.sorted((w1, w2) -> {
					return w1.getValue().compareTo(w2.getValue());
				}).collect(Collectors.toList());
		

		
		
		writeKeyWordsToDB(filteredWordCount);
	}

	private List<String> getCommonWordsDutch() {
		List<String> commonWords = new ArrayList<String>();
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);

			ResultSet rs = statement.executeQuery("Select * from " + this.common_word_dutch_table);
			System.out.println("");
			while (rs.next()) {
				String word = rs.getString("word").trim();
				commonWords.add(word);
			}

			statement.close();
		} catch (SQLException e) {
			System.err.println("error querying common word store : " + e.getMessage());
		}
		return commonWords;
	}

	private Boolean writeKeyWordsToDB(List<Entry<String, Long>> words) {
		String tableName = this.mat_keyword_table;
		String[] headers = new String[] { "keyword", "count" };
		
		List<String[]> values = words.stream()
				.map(w -> new String[] { w.getKey(), w.getValue().toString() })
				.collect(Collectors.toList());
		
		try {
			deleteTable(tableName);
	
			String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (\n"
					+ "	id integer PRIMARY KEY AUTOINCREMENT,\n" + " keyword text NOT NULL,\n"
					+ "	count integer NOT NULL\n" + ");";
			this.executeAndCommitCommand(createTableSql);
			this.executeAndCommitCommand(createSqlInsertStatement(tableName, headers, values));
		} catch(SQLException e) {
			System.err.println("Error occured while cleaning or writing data to " + this.mat_keyword_table + " : " + e.getMessage());
			return false;
		}
		
		return true;
	}

	/**
	 * reload the ifc to NLsfb mapping based on a csv of mapping codes.
	 */
	private void regenerateIfcToNlsfbMappings() {
		try {
			String tableName = this.ifc_to_nlsfb_table;
			CSVReader reader = new CSVReader(new FileReader(config.getNlsfbAlternativesFilePath()));
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
		} catch (SQLException|IOException e) {
			System.err.println("error occured with creating NLSfb alternatives map: " + e.getMessage());
		}
	}
}
