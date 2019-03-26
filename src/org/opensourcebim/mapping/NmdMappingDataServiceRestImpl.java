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
import org.opensourcebim.dataservices.RestDataService;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.nmd.NmdMappingDataService;
import org.opensourcebim.nmd.NmdUserDataConfig;

import com.opencsv.CSVReader;

/**
 * Class to provide an interface between java code and a mapping database
 * The mapping database will store any data that is required to resolve what (Nmd) products to choose
 * based on ifc file data.
 * @author vijj
 * 
 */
public class NmdMappingDataServiceRestImpl extends RestDataService implements NmdMappingDataService {


	public NmdMappingDataServiceRestImpl() {
	}

	@Override
	public void addUserMap(NmdUserMap map) {
		return null;
	}
	
	@Override
	public void addMappingSet(NMdMappingSet set) {
		return null;
	}

	@Override
	public NmdUserMap getApproximateMapForObject(MpgObject object) {
		return null;
	}

	@Override
	public HashMap<String, List<String>> getNlsfbMappings() {
		return null;
		
	}
	
	@Override
	public Map<String, Long> getKeyWordMappings(Integer minOccurence) {
		return null;
	}

	private List<String> getCommonWords() {
		return null;
	}

	@Override
	public void connect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}
}
