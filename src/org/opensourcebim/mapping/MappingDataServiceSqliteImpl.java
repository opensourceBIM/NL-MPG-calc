package org.opensourcebim.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.http.message.BasicStatusLine;
import org.opensourcebim.dataservices.ResponseWrapper;
import org.opensourcebim.dataservices.SqliteDataService;
import org.opensourcebim.ifccollection.MpgObject;

import nl.tno.bim.mapping.domain.Mapping;
import nl.tno.bim.mapping.domain.MappingSet;
import nl.tno.bim.nmd.config.UserConfig;

/**
 * Class to provide an interface between java code and a mapping database The
 * mapping database will store any data that is required to resolve what (Nmd)
 * products to choose based on ifc file data.
 * 
 * @author vijj
 * 
 */
public class MappingDataServiceSqliteImpl extends SqliteDataService implements MappingDataService {

	private final String mat_keyword_table = "ifc_material_keywords";
	private final String ifc_to_nlsfb_table = "ifc_to_nlsfb_map";
	private final String common_word_dutch_table = "common_words_dutch_table";

	public MappingDataServiceSqliteImpl(UserConfig config) {
		super(config);
	}

	@Override
	public ResponseWrapper<Mapping> postMapping(Mapping map) {
		return new ResponseWrapper<>(null, new BasicStatusLine(null, 404, "method not implemented"));
	}

	@Override
	public ResponseWrapper<Mapping> getMappingById(Long id) {
		return new ResponseWrapper<>(null, new BasicStatusLine(null, 404, "method not implemented"));
	}

	@Override
	public ResponseWrapper<MappingSet> postMappingSet(MappingSet set) {
		return new ResponseWrapper<>(null, new BasicStatusLine(null, 404, "method not implemented"));
	}

	@Override
	public ResponseWrapper<MappingSet> getMappingSetByProjectIdAndRevisionId(Long pid, Long rid) {
		return new ResponseWrapper<>(null, new BasicStatusLine(null, 404, "method not implemented"));
	}

	@Override
	public ResponseWrapper<Mapping> getApproximateMapForObject(MpgObject object) {
		return new ResponseWrapper<>(null, new BasicStatusLine(null, 404, "method not implemented"));
	}

	@Override
	public HashMap<String, List<String>> getNlsfbMappings() {

		HashMap<String, List<String>> elementMap = new HashMap<String, List<String>>();
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);

			ResultSet rs = statement.executeQuery("Select * from " + this.ifc_to_nlsfb_table);
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
					"Select * from " + this.mat_keyword_table + " " + "where count >= " + minOccurence.toString());
			while (rs.next()) {
				String keyWord = rs.getString("keyword").trim();
				Long keyCount = (long) rs.getInt("count");
				keyWordMap.putIfAbsent(keyWord, keyCount);
			}

			statement.close();
		} catch (SQLException e) {
			System.err.println("error querying keyword map : " + e.getMessage());
		}
		return keyWordMap;
	}

	@Override
	public List<String> getCommonWords() {
		List<String> commonWords = new ArrayList<String>();
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);

			ResultSet rs = statement.executeQuery("Select * from " + this.common_word_dutch_table);
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

	/**
	 * write the found entries to the database
	 * 
	 * @param entries
	 */
	public boolean postNlsfbMappings(List<String[]> entries) {
		try {
			String tableName = this.ifc_to_nlsfb_table;
			String[] headers = entries.get(0);
			List<String[]> values = entries.subList(1, entries.size());
			if (headers.length == 2) {
				deleteTable(tableName);

				String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (\n"
						+ "	id integer PRIMARY KEY AUTOINCREMENT,\n" + " code text NOT NULL,\n"
						+ "	ifcproducttype text NOT NULL\n" + ");";
				this.executeAndCommitCommand(createTableSql);
				this.executeAndCommitCommand(createSqlInsertStatement(tableName, headers, values));
				return true;
			} else {
				// our import file does not seem to be in the assumed structure.
				System.out.println("incorrect input data encountered.");
			}
		} catch (Exception e) {
			System.out.println("unaccounted error occured in posting nlsfb mappings to sqlite db: " + e.getMessage());
		}
		return false;
	}

	@Override
	public boolean postKeyWords(List<Entry<String, Long>> words) {
		String tableName = this.mat_keyword_table;
		String[] headers = new String[] { "keyword", "count" };

		List<String[]> values = words.stream().map(w -> new String[] { w.getKey(), w.getValue().toString() })
				.collect(Collectors.toList());

		try {
			deleteTable(tableName);

			String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (\n"
					+ "	id integer PRIMARY KEY AUTOINCREMENT,\n" + " keyword text NOT NULL,\n"
					+ "	count integer NOT NULL\n" + ");";
			this.executeAndCommitCommand(createTableSql);
			this.executeAndCommitCommand(createSqlInsertStatement(tableName, headers, values));
		} catch (SQLException e) {
			System.err.println("Error occured while cleaning or writing data to " + this.mat_keyword_table + " : "
					+ e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public boolean postCommonWords(List<String[]> entries) {

		try {
			String[] headers = entries.get(0);
			List<String[]> values = entries.subList(1, entries.size());
			if (headers.length == 1) {
				String tableName = this.common_word_dutch_table;
				deleteTable(tableName);

				String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (\n"
						+ "	id integer PRIMARY KEY AUTOINCREMENT,\n" + "	word text NOT NULL\n" + ");";
				this.executeAndCommitCommand(createTableSql);
				this.executeAndCommitCommand(createSqlInsertStatement(tableName, headers, values));
				return true;
			} else {
				// our import file does not seem to be in the assumed structure.
				System.out.println("incorrect input data encountered.");
			}
		} catch (Exception e) {
			System.out.println("error encountered posting common words: " + e.getMessage());
		}
		return false;
	}
}
