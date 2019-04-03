package org.opensourcebim.mapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.opensourcebim.nmd.MappingDataService;
import org.opensourcebim.nmd.UserConfigImpl;

import com.opencsv.CSVReader;

public class MapRunner {
	/**
	 * @param args: not required.
	 */
	public static void main(String[] args) {

		MappingDataServiceRestImpl service = new MappingDataServiceRestImpl(new UserConfigImpl());
		// we're not checking for whether the service is available or not.
		// the first method is rather lightweight however and will throw a timeout
		// quickly if the service is not available
		MapRunner.regenerateCommonWordsList(service);
		MapRunner.regenerateMaterialKeyWords(service);
		MapRunner.regenerateIfcToNlsfbMappings(service);
		service.disconnect();
	}

	/**
	 * load common word files into the database. These can be used to remove often
	 * used words from keyword selection
	 */
	private static void regenerateCommonWordsList(MappingDataService service) {
		try {

			CSVReader reader = new CSVReader(new FileReader(service.getConfig().getCommonWordFilePath()));
			List<String[]> myEntries = reader.readAll();
			reader.close();

			service.postCommonWords(myEntries);

		} catch (IOException e) {
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
	private static void regenerateMaterialKeyWords(MappingDataService service) {
		List<Path> foundFiles = new ArrayList<Path>();
		List<String> allMaterials = new ArrayList<String>();
		try {
			Files.walk(Paths.get(service.getConfig().getIfcFilesForKeyWordMapRootPath()))
					.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ifc")).filter(p -> {
						// filter on max file size. example: 50 MB limit on 5e7
						return (new File(p.toAbsolutePath().toString())).length() < 1e8;
					}).forEach(p -> {
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
									List<String> words = Arrays.asList(matRes[i].split(" |-|,|_|\\|(|)|<|>|:|;"))
											.stream().filter(w -> w != null && !w.isEmpty()).map(w -> w.toLowerCase())
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

		// group the words by occurence and remove any common words from the found
		// keywords
		List<String> common_words = service.getCommonWords();
		Map<String, Long> wordCount = allMaterials.stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		// remove items with too large ratio of non-text content or items that are
		// simply too short
		List<Entry<String, Long>> filteredWordCount = wordCount.entrySet().stream()
				.filter(w -> w.getKey().toCharArray().length > 2) // remove items that are simply too short
				.filter(w -> 2 * w.getKey().replaceAll("[^a-zA-Z]", "").length() >= w.getKey().length())
				.filter(w -> !common_words.contains(w.getKey())).sorted((w1, w2) -> {
					return w1.getValue().compareTo(w2.getValue());
				}).collect(Collectors.toList());

		service.postKeyWords(filteredWordCount);
	}

	/**
	 * reload the ifc to NLsfb mapping based on a csv of mapping codes.
	 */
	private static void regenerateIfcToNlsfbMappings(MappingDataService service) {
		try {
			CSVReader reader = new CSVReader(new FileReader(service.getConfig().getNlsfbAlternativesFilePath()));
			List<String[]> entries = reader.readAll();
			reader.close();

			service.postNlsfbMappings(entries);

		} catch (IOException e) {
			System.err.println("error occured with creating NLSfb alternatives map: " + e.getMessage());
		}
	}

}
