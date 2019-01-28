package org.opensourcebim.nmd;

/*
 * Interface to avoid devs having to check in the actual connection string and location of the nmd database.
 * In order to get the tests working implement this class (Named NmdDatabaseConfigImpl) and make 
 * sure the implementation is on the git ignore list.
 */
public interface NmdDatabaseConfig {
	public String getToken();

	public String getConnectionString();
}
