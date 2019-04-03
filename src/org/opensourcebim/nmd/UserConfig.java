package org.opensourcebim.nmd;

/*
 * Interface to avoid devs having to check in the actual connection string and location of the nmd database.
 * In order to get the tests working implement this class (Named NmdDatabaseConfigImpl) and make 
 * sure the implementation is on the git ignore list.
 */
public interface UserConfig {

	/*
	 * refresh token for authorization to request a new OAuth token
	 */
	public String getToken();

	/*
	 * Company specific client id for authorization
	 */
	Integer getClientId();

	public String getNmd2DbPath();

	String getIfcFilesForKeyWordMapRootPath();

	String getNlsfbAlternativesFilePath();

	String getMappingDbPath();

	public String getCommonWordFilePath();

}
