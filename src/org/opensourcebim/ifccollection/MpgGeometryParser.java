package org.opensourcebim.ifccollection;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.Bounds;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcRelDecomposes;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.models.ifc2x3tc1.impl.IfcCoveringImpl;
import org.bimserver.utils.AreaUnit;
import org.bimserver.utils.IfcUtils;
import org.bimserver.utils.LengthUnit;
import org.bimserver.utils.VolumeUnit;
import org.eclipse.emf.common.util.EList;
import org.opensourcebim.services.FloorAreaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class to determine dimensions from IfcProduct classes
 * @author vijj
 *
 */
public class MpgGeometryParser {

	// reporting units and imported units to help convert measurements
	private static AreaUnit areaUnit = AreaUnit.SQUARED_METER;
	private static VolumeUnit volumeUnit = VolumeUnit.CUBIC_METER;
	private static LengthUnit lengthUnit = LengthUnit.METER;
	private AreaUnit modelAreaUnit;
	private VolumeUnit modelVolumeUnit;
	private LengthUnit modelLengthUnit;

	protected static Logger LOGGER = LoggerFactory.getLogger(MpgGeometryParser.class);
	private ObjectMapper mapper = new ObjectMapper();
	
	public MpgGeometryParser(IfcModelInterface ifcModel) {
		// get project wide parameters
		modelVolumeUnit = IfcUtils.getVolumeUnit(ifcModel);
		modelAreaUnit = IfcUtils.getAreaUnit(ifcModel);
		modelLengthUnit = IfcUtils.getLengthUnit(ifcModel);
		
	}
	
	// ---------- Standard getters and setters -------------
	public static AreaUnit getAreaUnit() {
		return areaUnit;
	}

	public static VolumeUnit getVolumeUnit() {
		return volumeUnit;
	}

	public static LengthUnit GetLengthUnit() {
		return lengthUnit;
	}
	
	/**
	 * retrieve the geometric properties of an ifcProduct
	 * 
	 * @param prod the product to evaluate
	 * @return a MpgGeometry object with relevant data stored
	 */
	public MpgGeometry getGeometryFromProduct(IfcProduct prod) {
		GeometryInfo geometry = prod.getGeometry();

		MpgGeometry geom = new MpgGeometry();

		if (geometry != null) {

			geom.setVolume(this.convertVolume(geometry.getVolume()));

			try {
				JsonNode geomData = mapper.readTree(geometry.getAdditionalData());
				if (geomData != null && geomData.size() > 0) {
					geom.setIsComplete(true);
					Bounds bounds = geometry.getBoundsUntransformed();
					double x_dir = this.convertLength(bounds.getMax().getX() - bounds.getMin().getX());
					double y_dir = this.convertLength(bounds.getMax().getY() - bounds.getMin().getY());
					double z_dir = this.convertLength(bounds.getMax().getZ() - bounds.getMin().getZ());
					
					geom.setFloorArea(this.convertArea(geomData.get("SURFACE_AREA_ALONG_Z").asDouble()));				
					
					Function<Double, Double> dimCheck = (dim) -> {
						return Double.isInfinite(dim) || Double.isNaN(dim) ? 0.0 : dim;
					};
					
					x_dir = dimCheck.apply(x_dir);
					y_dir = dimCheck.apply(y_dir);
					z_dir = dimCheck.apply(z_dir);
					geom.setDimensions(x_dir, y_dir, z_dir);
					
					// check if there are exceptional cases where there are leaks in the geometry
					// i.e.: stairs often show this behaviour
					if (x_dir * y_dir * z_dir <= geom.getVolume() / 1e5 || geom.getVolume() == 0) {
						geom.setVolume(x_dir * y_dir * z_dir);
						geom.setFloorArea(x_dir * y_dir);
					}					

					// check cases where the bounding box doesn't snap correctly around the object.
					// This is a large issues with roofs.
					double largest_face_area = geomData.get("LARGEST_FACE_AREA").asDouble();
					double face_area_ratio = geom.getFaceArea() / largest_face_area;
					if ((face_area_ratio < 0.5 || face_area_ratio > 2) 
							&& (prod instanceof IfcCoveringImpl)) {
						double max_area_dim = geom.getPrincipalDimension();
						double min_area_dim =geom.getFaceArea() / max_area_dim;
						
						// xyz order doesn't really matter!
						double new_z_dir = geom.getVolume() / geom.getFaceArea();
						double new_x_dir = Math.sqrt(Math.pow(max_area_dim, 2) + Math.pow(min_area_dim, 2));
						double new_y_dir = geom.getVolume() / new_x_dir/ new_z_dir;

						geom.setDimensions(new_x_dir, new_y_dir, new_z_dir);						
					}
				}
			} catch (IOException e) {
				LOGGER.error("Error encountered while retrieving geometry from product: " + e.getMessage());
			}
		}
		return geom;
	}

	private Double convertVolume(Double value) {
		return MpgGeometryParser.getVolumeUnit().convert(value, modelVolumeUnit);
	}

	private Double convertArea(Double value) {
		return MpgGeometryParser.getAreaUnit().convert(value, modelAreaUnit);
	}

	private Double convertLength(Double value) {
		return MpgGeometryParser.lengthUnit.convert(value, modelLengthUnit);
	}
	
	
	/**
	 * Try parse the floor area by different means:
	 *  - When there are IfcSpaces defined apply this method
	 *  - try get it through the voxel bot
	 *  - try get it through other means
	 * @param ifcModel the ifc data to parse
	 * @param objectStore container to store the floor area data.
	 */
	public void tryParseFloorArea(IfcModelInterface ifcModel, MpgObjectStoreImpl objectStore, byte[] data) {

		// first loop through IfcSpaces
		List<IfcSpace> allSpaces = ifcModel.getAllWithSubTypes(IfcSpace.class);
		if (allSpaces.size() > 0) {
			for (IfcSpace space : allSpaces) {

				// omit any external spaces.
				if (space.getBoundedBy().size() == 0) {
					continue;
				}

				EList<IfcRelDecomposes> parentDecomposedProduct = space.getIsDecomposedBy();

				// if there is any space that decomposes in this space we can omit the addition
				// of the volume
				boolean isIncludedSemantically = false;
				if (parentDecomposedProduct != null) {
					isIncludedSemantically = parentDecomposedProduct.stream()
							.filter(relation -> relation.getRelatingObject() instanceof IfcSpace).count() > 0;
				}

				MpgGeometry geom = getGeometryFromProduct(space);

				// ToDo: also include geometric check?
				if (!isIncludedSemantically) {
					objectStore.getSpaces()
						.add(new MpgSpaceImpl(space.getGlobalId(), geom.getVolume(), geom.getFloorArea()));
				}
			}
		}
		
		if (data != null && objectStore.getTotalFloorArea() == 0.0){
			JsonNode res;
			try {
				res = FloorAreaService.getJsonFromBinaryData(data);
				objectStore.getSpaces()
					.add(new MpgSpaceImpl("area from floor area voxel service", 0.0, res.get("floor_area").asDouble()));
		
			} catch (IOException e) {
				LOGGER.error(e.getMessage());
			}
		} else {
			objectStore.getSpaces()
			.add(new MpgSpaceImpl("no floor area found", 0.0, -1));
			LOGGER.warn("failed to find a floor area");
		}
	}

}
