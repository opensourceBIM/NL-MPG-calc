package org.opensourcebim.dataservices;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import nl.tno.bim.nmd.config.UserConfig;

public abstract class SqliteDataService {

	protected Connection connection;
	protected UserConfig config;
	
	public SqliteDataService(UserConfig config) {
		this.config = config;
		connect();
	}
	
	public UserConfig getConfig() {return config;}

	public void connect() {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + config.getMappingDbPath());
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			System.err.println(e.getMessage());
		}
	}
	
	public void disconnect() {
		try {
			if (this.connection == null || this.connection.isClosed()) {
				this.connection.close();
			}
		} catch (SQLException e) {
			System.err.println("Error occured in disocnnecting from mapping service: " + e.getMessage());
		}
	}

	protected String createSqlInsertStatement(String table, String[] headers, List<String[]> values) {

		String valueString = StringUtils.join(
				values.stream().map(ar -> "('" + StringUtils.join(ar, "','") + "')").collect(Collectors.toList()), ",");
		return "INSERT INTO " + table + " (" + StringUtils.join(headers, ",") + ") VALUES " + valueString;
	}

	protected void deleteTable(String tableName) throws SQLException {
		this.executeAndCommitCommand("DROP TABLE IF EXISTS " + tableName);
	}

	protected void executeAndCommitCommand(String sqlCommand) throws SQLException {
		Statement statement = connection.createStatement();
		statement.setQueryTimeout(30);
		statement.executeUpdate(sqlCommand);
		statement.close();
	}

}
