package org.opensourcebim.test;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class WorkSheetReader {
	XSSFWorkbook wb = null;

	public void loadWorkBook(Path file) throws IOException, InvalidFormatException {
		this.wb = (XSSFWorkbook) WorkbookFactory.create(file.toFile());
	}

	public Map<String, List<Object>> readFile(int sheetNum) {
		return this.readSheetToMap(sheetNum, 0, 0, 0);
	}

	public Map<String, List<Object>> readSheetToMap(int sheetNum, int headerRow, int skipRows, int startCol) {
		Map<String, List<Object>> data = new HashMap<>();
		
		try {
			XSSFSheet sheet = wb.getSheetAt(sheetNum);
			XSSFCell cell;
			XSSFRow row;
			
			int rows = sheet.getPhysicalNumberOfRows();

			int cols = 0; // No of columns
			int tmp = 0;
			
			// define the row where to start reading
			int startRow = headerRow + 1 + skipRows;
			startRow = startRow < 0 ? 0 : startRow; 
			
			// Find the maximum # of columns
			for (int i = startRow; i < rows; i++) {
				row = sheet.getRow(i);
				if (row != null) {
					tmp = sheet.getRow(i).getPhysicalNumberOfCells();
					if (tmp > cols)
						cols = tmp;
				}
			}

			// read in every column separately
			for (int c = startCol; c < cols; c++) {
								
				// first get the header
				cell = sheet.getRow(headerRow).getCell(c);
				// ToDo: check what to do with missing headers. fill in a generated value?
				if (cell == null) {
					continue;
				}
				String header = this.readCell(cell).toString();
				
				List<Object> columnData = readColumn(sheet, c, startRow, rows);

				if (!data.containsKey(header)) {
					data.put(header, columnData);
				} else {
					throw new KeyException("duplicate column found");
				}
			}
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
		return data;
	}

	private List<Object> readColumn(XSSFSheet sheet, int c, int startRow, int rows) {
		XSSFRow row;
		XSSFCell cell;
		List<Object> columnData = new ArrayList<>();
		for (int r = startRow; r < rows; r++) {
			row = sheet.getRow(r);
			if (row != null) {
				cell = row.getCell(c);
				if (cell != null) {
					columnData.add(this.readCell(cell));
				}
			}
		}
		
		// cull any trailing null entries before adding the data
		Optional<Object> lastObject = columnData.stream().filter(Objects::nonNull).reduce((a, b) -> b);
		List<Object> cleanedData = new ArrayList<>();
		if (lastObject.isPresent()) {
			int idx = columnData.lastIndexOf(lastObject.get());
			cleanedData = columnData.subList(0, idx);
		}
		return cleanedData;
	}

	/**
	 * Read value of excel cell neglecting formulas, errors etc.
	 * @param cell Excel cell object
	 * @return value of cell as generic Object
	 */
	private Object readCell(XSSFCell cell) {
		if (cell == null) return null;
		// check cell type and expplictly add null values for in between empty cells.
		if (cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
			return cell.getNumericCellValue();
		} else if (cell.getCellType() == XSSFCell.CELL_TYPE_STRING) {
			return cell.getStringCellValue();
		} else if (cell.getCellType() == XSSFCell.CELL_TYPE_BOOLEAN) {
			return cell.getBooleanCellValue();
		} else {
			return null;
		}
	}

}
