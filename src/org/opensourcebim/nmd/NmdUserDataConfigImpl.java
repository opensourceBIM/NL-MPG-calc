package org.opensourcebim.nmd;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class NmdUserDataConfigImpl implements NmdUserDataConfig {

	private String token;
	private int id;
	private String mappingDbPath;
	private String nmd2path;
	private String nlsfbAlternativesPath;
	private String keyWordFilePath;
	private String commonWordFilePath;
	private String rootPath;
	
	public NmdUserDataConfigImpl() {
		Map<String, String> nmdPropertyMap = loadResources("nmd");
		this.setToken(nmdPropertyMap.get("token"));
		this.setClientId(Integer.parseInt(nmdPropertyMap.get("id")));
		this.setNmd2DbPath(nmdPropertyMap.get("nmd2Path"));
		this.setMappingDbPath(nmdPropertyMap.get("mappingDbPath"));
		this.setNlsfbAlterantivesFilePath(nmdPropertyMap.get("nlsfbAlternativesPath"));
		this.setCommonWordFilePath(nmdPropertyMap.get("commonWordExclusionPath"));
		
		this.rootPath = nmdPropertyMap.get("rootPath");
	}
	
	private Map<String, String> loadResources(String rootNode) {
		Map<String, String> res = new HashMap<String, String>();
		
		List<String> keyNames = Arrays.asList(new String[]
			{"token", "id", "nmd2Path" , "mappingDbPath", "nlsfbAlternativesPath", "rootPath", "commonWordExclusionPath"});
		
		URL urlRes = getClass().getClassLoader().getResource("nmdConfig.xml");
		if (urlRes != null) {
			try {
				File file = Paths.get(urlRes.toURI()).toFile();
				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				        .newInstance();
				DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
				Document doc = documentBuilder.parse(file);
				
				keyNames.forEach(key -> {
					res.put(key, doc.getElementsByTagName(key).item(0).getTextContent());	
				});

			} catch (Exception e) {
				System.err.println(e.getMessage());
			} 
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
