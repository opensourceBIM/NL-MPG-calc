package org.opensourcebim.ifccollection;

/**
 * Class to store geometric and material data of objects found in IFC files.
 * This object can be a layer of a MpgObject or a representation of a IfcSpace (no material, just volume and area)
 * @author vijj
 */
public class MpgLayerImpl extends MpgSpaceImpl implements MpgLayer {
	private String materialName = null;

	/**
	 * constructor for physical layers of an MpgObject
	 * 
	 * @param volume Volume of object
	 * @param mat    material of object
	 */
	public MpgLayerImpl(double volume, String mat, String guid) {
		super(volume, 0.0);
		this.setVolume(volume);
		this.setMaterialName(mat);
		this.id = guid;
	}

	@Override
	public String getMaterialName() {
		return this.materialName;
	}

	protected void setMaterialName(String mpgMaterial) {
		this.materialName = mpgMaterial;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public String print() {		
		StringBuilder sb = new StringBuilder();
		sb.append("- " + materialName + " sub object");
		sb.append(System.getProperty("line.separator"));
		sb.append(">> Volume: " + getVolume());
		sb.append(System.getProperty("line.separator"));
		sb.append(">> Area: " + "TBD");
		sb.append(System.getProperty("line.separator"));

		return sb.toString();
	}
}
