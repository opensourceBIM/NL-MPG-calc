package org.opensourcebim.nmd;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class UserConfigImpl implements UserConfig {

	private String token;
	private int id;
	private String mappingDbPath;
	private String nmd2path;
	private String nlsfbAlternativesPath;
	private String keyWordFilePath;
	private String commonWordFilePath;
	private String rootPath;
	private static final String rootNode = "nmd";
	
	private UserConfigImpl(Map<String, String> nmdPropertyMap) {
		this.setToken(nmdPropertyMap.get("token"));
		this.setClientId(Integer.parseInt(nmdPropertyMap.get("id")));
		this.setNmd2DbPath(nmdPropertyMap.get("nmd2Path"));
		this.setMappingDbPath(nmdPropertyMap.get("mappingDbPath"));
		this.setNlsfbAlterantivesFilePath(nmdPropertyMap.get("nlsfbAlternativesPath"));
		this.setCommonWordFilePath(nmdPropertyMap.get("commonWordExclusionPath"));
	}
	
	public UserConfigImpl() {
		this(loadResources(Paths.get(System.getProperty("user.dir")), rootNode));
		
		String parentPath = (new File(System.getProperty("user.dir"))).getParentFile().toString();
		this.rootPath = parentPath;
	}
	
	public UserConfigImpl(Path rootPath) {
		this(loadResources(rootPath, rootNode));
		
		String parentPath = (new File( rootPath.toAbsolutePath().toString())).getParentFile().toString();
		this.rootPath = parentPath;
	}

	private static Map<String, String> loadResources(Path rootPath, String rootNode) {
		Map<String, String> res = new HashMap<String, String>();
				
		Path configPath = rootPath.resolve("nmdConfig.xml");
		if (Files.exists(configPath)) {
			try {
				
				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
				try (InputStream newInputStream = Files.newInputStream(configPath)) {
					Document doc = documentBuilder.parse(newInputStream);
					Consumer<Node> addNodedata = (node) -> {
						res.put(node.getNodeName(), node.getTextContent());
					};
					
					for(int i = 0; i < doc.getChildNodes().getLength(); i++) {
						Node node = doc.getChildNodes().item(i);
						performMethodForRecursiveNode(node, addNodedata);
					}
				}					
			} catch (Exception e) {
				System.err.println(e.getMessage());
			} 
		} else {
			System.err.println("nmdConfig.xml not found");
		}
		
		return res;
	}
	
	private static void performMethodForRecursiveNode(Node node, Consumer<Node> method) {
		method.accept(node);
		for(int i = 0; i < node.getChildNodes().getLength(); i++) {
			Node child = node.getChildNodes().item(i);
			performMethodForRecursiveNode(child, method);
		}
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
		return Paths.get(this.rootPath + "/" + this.nlsfbAlternativesPath).toAbsolutePath().toString();
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
