package org.opensourcebim.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class WorksheetReaderTest {
	
	private String relPath = null;
	private Integer startCol;
	private Integer skipRows;
	private Integer sheetNum;
	private Integer headerRow;

	private boolean fileExists;
	private Integer expectedRows;
	private Integer expectedCols;
	private String description;

	Path rootDir = Paths.get(System.getProperty("user.dir")).resolve("test").resolve("ReferenceExcelFiles");
	
	public WorksheetReaderTest(String relPath, Boolean fileExists, Integer sheetNum, Integer headerRow,
			Integer startCol, Integer skipRows, Integer expectedColumns, Integer expectedRows, String testDescription) {
		this.relPath = relPath;
		this.fileExists = fileExists;
		this.sheetNum = sheetNum;
		this.headerRow = headerRow;
		this.startCol = startCol;
		this.skipRows = skipRows;
		this.expectedCols = expectedColumns;
		this.expectedRows = expectedRows;
		this.description = testDescription;
	}

	/**
	 * Create test cases based on preset combinations of parameters
	 * @return Object array to be used in constructor for input parameters
	 */
	@Parameterized.Parameters(name = "file {0} with sheet {2}, headers at {3}, starting at column {4} and skipping {5} rows to start with data collection")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{ "fileDoesNotExist.xlsx", false, 0, 0, 0, 0, 10, 41, "file is not a valid filename"},
			{ "testReference1.xlsx", true, 0, 0, 0, 0, 10, 41, "blue sky test with default settings" },
			{ "testReference1.xlsx", true, 1, 0, 0, 0, 1, 4, "reading wrong sheet" },
			{ "testReference1.xlsx", true, 0, 2, 0, 0, 10, 39, "set header row to 3rd line"},
			{ "testReference1.xlsx", true, 0, 0, 3, 0, 7, 41, "skip the first 3 columns" },
			{ "testReference1.xlsx", true, 0, 0, 0, 11, 10, 30, "skip the first 11 rows" },
			{ "testReference1.xlsx", true, 0, 0, 0, 42, 10, 0, "skip more rows than present" },
			{ "testReference1.xlsx", true, 0, 0, 12, 0, 0, 0, "skip more columns than present" },});
	}

	protected Path getFullIfcModelPath() {
		return rootDir.resolve(this.relPath);
	}

	@Test
	public void TestThrowsIOExceptionWhenFileDoesNotExist() {
		try {
			WorkSheetReader reader = new WorkSheetReader();
			reader.loadWorkBook(getFullIfcModelPath());
		} catch (IOException ioe) {
			assertFalse(this.fileExists);
			return;
		} catch (InvalidFormatException ife) {
			fail();
		}
		assertTrue(fileExists);
	}

	@Test
	public void TestCanReadWorksheet() throws IOException, InvalidFormatException {
		if (!fileExists) {
			return;
		}
		WorkSheetReader reader = new WorkSheetReader();
		reader.loadWorkBook(getFullIfcModelPath());
		Map<String, List<Object>> res = reader.readSheetToMap(sheetNum, headerRow, skipRows, startCol);
		assertEquals("expected # of columns does not match read in # of columns when testing: " + this.description,
				this.expectedCols, (Integer)res.size());
		
		List<Integer> rowCounts = res.values().stream().map(col -> col == null ? -1 : col.size())
				.collect(Collectors.toList());
		Integer maxRows = rowCounts.size() > 0 ? Collections.max(rowCounts) : 0;
		assertEquals("expected # of rows does not match result # of rows when testing: " + this.description,
				this.expectedRows, maxRows);
	}

}
