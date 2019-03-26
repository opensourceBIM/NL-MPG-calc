package org.opensourcebim.nmd;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class NmdUserDataConfigImpl implements NmdUserDataConfig {

	private String token;
	private int id;
	private String mappingDbPath;
	private String nmd2path;
	private String nlsfbAlternativesPath;
	private String keyWordFilePath;
	private String commonWordFilePath;
	private String rootPath;
	private static final String rootNode = "nmd";
	
	private NmdUserDataConfigImpl(Map<String, String> nmdPropertyMap) {
		this.setToken(nmdPropertyMap.get("token"));
		this.setClientId(Integer.parseInt(nmdPropertyMap.get("id")));
		this.setNmd2DbPath(nmdPropertyMap.get("nmd2Path"));
		this.setMappingDbPath(nmdPropertyMap.get("mappingDbPath"));
		this.setNlsfbAlterantivesFilePath(nmdPropertyMap.get("nlsfbAlternativesPath"));
		this.setCommonWordFilePath(nmdPropertyMap.get("commonWordExclusionPath"));
	}
	
	public NmdUserDataConfigImpl() {
		this(loadResources(Paths.get(System.getProperty("user.dir")), rootNode));
		this.rootPath = Paths.get(System.getProperty("user.dir")).getParent().toAbsolutePath().toString();
	}
	
	public NmdUserDataConfigImpl(Path rootPath) {
		this(loadResources(rootPath, rootNode));
		this.rootPath = rootPath.toAbsolutePath().toString();
	}

	private static Map<String, String> loadResources(Path rootPath, String rootNode) {
		Map<String, String> res = new HashMap<String, String>();
		
		List<String> keyNames = Arrays.asList(new String[]
				{"token", "id", "nmd2Path" , "mappingDbPath", "nlsfbAlternativesPath", "rootPath", "commonWordExclusionPath"});
		
		Path configPath = rootPath.resolve("nmdConfig.xml");
		if (Files.exists(configPath)) {
			try {
				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
				try (InputStream newInputStream = Files.newInputStream(configPath)) {
					Document doc = documentBuilder.parse(newInputStream);
					keyNames.forEach(key -> {
						res.put(key, doc.getElementsByTagName(key).item(0).getTextContent());	
					});
				}					
			} catch (Exception e) {
				System.err.println(e.getMessage());
			} 
		} else {
			System.err.println("nmdConfig.xml not found");
		}
		
		return res;
	}

	@Override
	public String getToken() {
		return this.token;
	}
	
	@Override
	public Integer getClientId() {
		return this.id;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setClientId(int id) {
		this.id = id;
	}

	/**
	 * Only for testing purposes. This path stores where the NMD2.X database file is stored
	 * @return file path string to database
	 */
	@Override
	public String getNmd2DbPath() {
		return this.rootPath + "//" + this.nmd2path;
	}
	
	public void setNmd2DbPath(String path) {
		this.nmd2path = path;
	}
	
	/**
	 * sqlite database file path where all the mapping data is stored. 
	 * @return file path string
	 */
	@Override
	public String getMappingDbPath() {
		return this.rootPath + "//" + this.mappingDbPath;
	}
	
	
	public void setMappingDbPath(String path) {
		this.mappingDbPath = path;
	}
	
	/**
	 * file path for .csv file where the hard coded ifc to NLsfb mappings are stored
	 * @return file path string
	 */
	@Override
	public String getNlsfbAlternativesFilePath() {
		return this.rootPath + "//" + this.nlsfbAlternativesPath;
	}
	
	public void setNlsfbAlterantivesFilePath(String path) {
		this.nlsfbAlternativesPath = path;
	}
	
	/**
	 * Folder where to start looking for ifc files in order to generate the keyword dictionary
	 * @return folder path string
	 */
	@Override
	public String getIfcFilesForKeyWordMapRootPath() {
		return this.rootPath + "//" + this.keyWordFilePath;
	}
	
	public void setIfcFilesForKeyWordMapRootPath(String path) {
		this.keyWordFilePath = path;
	}
	

	@Override
	public String getCommonWordFilePath() {
		return this.rootPath + "//" + this.commonWordFilePath;
	}
	
	public void setCommonWordFilePath(String path) {
		this.commonWordFilePath = path;
	}
	
}
