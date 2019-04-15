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
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import com.opencsv.CSVReader;

public class MapRunner {

	/**
	 * @param args: not required. PREREQUISITE: make sure a mappingservice is
	 *        runnning as otherwise you will get no response errors
	 */
	public static void main(String[] args) {

		Options options = new Options();

		Option rootPathOption = new Option("root", "rootfolder", true,
				"root file path from where the rest of the files should be available");
		rootPathOption.setRequired(true);
		options.addOption(rootPathOption);

		Option wordPathOption = new Option("words", "wordsFilePath", true,
				"file location with common words csv file relative to rootPath");
		wordPathOption.setRequired(false);
		options.addOption(wordPathOption);

		Option nlsfbAlternativesPathOption = new Option("nlsfb", "nlsfbFilePath", true,
				"file location with nlsfb alternatives csv file relative to rootPath");
		nlsfbAlternativesPathOption.setRequired(false);
		options.addOption(nlsfbAlternativesPathOption);

		Option reprocessKeywords = new Option("keyWords", "reprocessKeyword", false,
				"flag to determine whether material keywords will be overwritten. "
				+ "Presence of this keyword will trigger a search for all IFC files "
				+ "within a (sub-) directory of the rootpath");
		reprocessKeywords.setRequired(false);
		options.addOption(reprocessKeywords);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("MapRunner", options);

			System.exit(1);
		}

		MappingDataServiceRestImpl service = new MappingDataServiceRestImpl();

		// we're not checking for whether the service is available or not.
		// the first method is rather lightweight however and will throw a timeout
		// quickly if the service is not available
		String rootPath = cmd.getOptionValue("root");

		if (cmd.hasOption("words")) {
			MapRunner.regenerateCommonWordsList(service, rootPath + cmd.getOptionValue("words"));
		}

		if (cmd.hasOption("keyWords")) {
			MapRunner.regenerateMaterialKeyWords(service, rootPath);
		}

		if (cmd.hasOption("nlsfb")) {
			MapRunner.regenerateIfcToNlsfbMappings(service, rootPath + cmd.getOptionValue("nlsfb"));
		}

		service.disconnect();
	}

	/**
	 * load common word files into the database. These can be used to remove often
	 * used words from keyword selection
	 */
	private static void regenerateCommonWordsList(MappingDataService service, String path) {
		try {

			CSVReader reader = new CSVReader(new FileReader(path));
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
	private static void regenerateMaterialKeyWords(MappingDataService service, String pathToFolder) {
		List<Path> foundFiles = new ArrayList<Path>();
		List<String> allMaterials = new ArrayList<String>();
		try {
			Files.walk(Paths.get(pathToFolder)).filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ifc"))
					.filter(p -> {
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
	private static void regenerateIfcToNlsfbMappings(MappingDataService service, String path) {
		try {
			CSVReader reader = new CSVReader(new FileReader(path));
			List<String[]> entries = reader.readAll();
			reader.close();

			service.postNlsfbMappings(entries);

		} catch (IOException e) {
			System.err.println("error occured with creating NLSfb alternatives map: " + e.getMessage());
		}
	}

}
