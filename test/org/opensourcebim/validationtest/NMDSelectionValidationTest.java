package org.opensourcebim.validationtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.ifccollection.ObjectStoreBuilder;
import org.opensourcebim.mapping.MappingDataServiceRestImpl;
import org.opensourcebim.mapping.NmdDataResolver;
import org.opensourcebim.mapping.NmdDataResolverImpl;
import org.opensourcebim.mpgcalculation.BillOfMaterials;
import org.opensourcebim.mpgcalculation.MpgCalculator;
import org.opensourcebim.test.WorkSheetReader;

import nl.tno.bim.nmd.services.Nmd3DataService;

public class NMDSelectionValidationTest {
	List<String> columnData = new ArrayList<>(Arrays.asList("Bouwdeel", "key SfB element", "SfB element", "Toelichting",
			"material key", "Materiaal", "breedte (m)", "lengte (m)", "dikte (m)", "hoeveelheid", "eenheid",
			"oppervlak per woning", "opmerkingen", "volume m³/m (muur)", "volume/woning (m³)", "dichtheid (kg/m³)",
			"kg/woning", "kg/m2", "ID NMD", "NMD productkaart", "MKI/m²", "MKI/woning"));

	Path rootDir = Paths.get(System.getProperty("user.dir")).resolve("test").resolve("ReferenceExcelFiles");
	String relPath = "Bob - gem_MKI_per_m2_edited_for_analysis.xlsx";

	protected Path getFullIfcModelPath() {
		return rootDir.resolve(this.relPath);
	}

	protected MpgObjectStore createBillOfMaterialsFromReferenceFile() {
		try {
			WorkSheetReader reader = new WorkSheetReader();
			reader.loadWorkBook(getFullIfcModelPath());
			Map<String, List<Object>> res = reader.readSheetToMap(0, 0, 0, 0);
			return createObjectStoreFromWorkSheetData(res);
		} catch (InvalidFormatException | IOException e) {
			fail(e.getMessage());
		}
		return null;
	}

	private MpgObjectStore createObjectStoreFromWorkSheetData(Map<String, List<Object>> data) {
		// get max length of the columsn
		ObjectStoreBuilder builder = new ObjectStoreBuilder();
		if (data != null && data.size() > 0) {
			Integer max_length = data.values().stream().map(l -> l.size()).max(Comparator.comparingInt(i -> i)).get();

			for (int rowIdx = 0; rowIdx < max_length; rowIdx++) {
				String ifcName = this.getStringValueFromColumn("Toelichting", data, rowIdx);

				String qVal = this.getStringValueFromColumn("hoeveelheid", data, rowIdx);
				Double quantity = qVal != null && qVal != "" ? Double.parseDouble(qVal) : 1d;
				Map<String, Double> matSpecs = this.getMaterialSpecs(data, rowIdx);
				Double[] dims = this.getDimensions(data, rowIdx);
				String nlsfb = "";
				String type = this.getIfcType(data, rowIdx);
				String parentUUID = "";

				for (int i = 0; i < quantity; i++) {
					builder.AddUnmappedMpgElement(ifcName, false, matSpecs, dims, nlsfb, type, parentUUID);
				}
			}
		}
		return builder.getStore();
	}

	private String getStringValueFromColumn(String columnName, Map<String, List<Object>> data, int index) {
		List<Object> column = data.get(columnName);

		if (column.size() > index) {
			Object obj = column.get(index);
			return obj != null ? obj.toString() : "";
		}
		return "";
	}

	/**
	 * Get dimensions from excel sheet by looking at the total surface area
	 * 
	 * @param data  excel data object
	 * @param index row to read
	 * @return 3D Double array with dimensions x*y*z
	 */
	private Double[] getDimensions(Map<String, List<Object>> data, int index) {
		Double[] dims = new Double[3];
		List<Object> areaColumn = data.get("oppervlak per woning");

		// first try using the principal dimensions
		Object widthObj = data.get("breedte (m)").get(index);
		dims[0] = widthObj != null ? Double.parseDouble(widthObj.toString()) : 0.0;
		Object lengthObj = data.get("lengte (m)").get(index);
		dims[1] = lengthObj != null ? Double.parseDouble(lengthObj.toString()) : 0.0;
		Object thicknessObj = data.get("dikte (m)").get(index);
		dims[2] = thicknessObj != null ? Double.parseDouble(thicknessObj.toString()) : 0.05;

		// if not all principal dimensions are defined try it through deduciton by
		// element
		if (areaColumn.size() > index && (dims[0] == 0.0 || dims[1] == 0.0 || dims[2] == 0.0)) {
			Object areaObj = areaColumn.get(index);
			Double area = areaObj != null ? Double.parseDouble(areaObj.toString()) : 0.0;
			dims[0] = Math.sqrt(area);
			dims[1] = dims[0];
		}

		return dims;
	}

	private Map<String, Double> getMaterialSpecs(Map<String, List<Object>> data, int index) {
		Map<String, Double> mats = new HashMap<>();
		mats.put(this.getStringValueFromColumn("Materiaal", data, index), 1.0);
		return mats;
	}

	private String getIfcType(Map<String, List<Object>> data, int index) {
		List<Object> column = data.get("Bouwdeel");
		if (column.size() <= index) {
			return "";
		} else
			switch (column.get(index).toString()) {
			case "Fundering":
				return "IfcFooting";
			case "Balken":
				return "IfcBeam";
			case "Begane grond vloeren":
			case "Verdiepingsvloeren":
				return "IfcSlab";
			case "Buiten- en zijmuren":
			case "Muurisolatie":
			case "Binnenmuren":
				return "IfcWall";
			case "Daken":
			case "Dakisolatie":
				return "IfcRoof";
			case "Ramen":
			case "Kozijnen":
				return "IfcWindow";
			case "Deuren":
				return "IfcDoor";
			case "Dakwaterafvoer":
			case "Leidingen":
				return "IfcFlowSegment";
			default:
				return "IfcBuildingElementPart";
			}
	}

	@Test
	public void CanCreateObjectStoreFromReferenceFile() {
		MpgObjectStore store = this.createBillOfMaterialsFromReferenceFile();
		NmdDataResolver resolver = new NmdDataResolverImpl();
		try {
			resolver.setNmdService(Nmd3DataService.getInstance());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		resolver.setMappingService(new MappingDataServiceRestImpl());
		resolver.setStore(store);
		// this will edit the store object with results of NMD product card selection
		resolver.nmdToMpg();
		MpgCalculator calc = new MpgCalculator();
		calc.setObjectStore(resolver.getStore());
		calc.calculate(75d);
		BillOfMaterials bom = calc.getResults().getBillOfMaterials();
		System.out.println(bom.printToCsv("|"));
		assertEquals(37, store.getElements().size());
		assertTrue(store.getProductCards().size() > 0);
	}

}
