package org.opensourcebim.validationtest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.test.WorkSheetReader;

public class NMDSelectionValidationTest {
	List<String> columnData = new ArrayList<>(Arrays.asList("Bouwdeel", "key SfB element", "SfB element", "Toelichting", "material key",
					"Materiaal", "breedte (m)", "lengte (m)", "dikte (m)", "hoeveelheid ", "eenheid",
					"oppervlak per woning", "opmerkingen", "volume m³/m (muur)", "volume/woning (m³)",
					"dichtheid (kg/m³)", "kg/woning", "kg/m2", "ID NMD", "NMD productkaart", "MKI/m²", "MKI/woning"));
	
	Path rootDir = Paths.get(System.getProperty("user.dir")).resolve("test").resolve("ReferenceExcelFiles");
	String relPath = "Bob - gem_MKI_per_m2.xlsx";
	
	protected Path getFullIfcModelPath() {
		return rootDir.resolve(this.relPath);
	}
	
	protected MpgObjectStore CreateBillOfMaterialsFromReferenceFile() throws InvalidFormatException, IOException {
		WorkSheetReader reader = new WorkSheetReader();
		reader.loadWorkBook(getFullIfcModelPath());
		Map<String, List<Object>> res = reader.readSheetToMap(0, 0, 0, 0);
		
		return null;
	}

}
