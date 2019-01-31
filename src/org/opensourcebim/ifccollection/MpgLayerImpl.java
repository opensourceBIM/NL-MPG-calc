package org.opensourcebim.ifccollection;

/**
 * Store layer information such as material and volume.
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
	public MpgLayerImpl(double volume, double area,  String mat, String guid) {
		super(guid, volume, area); // TODO: what to do with area?
		this.setMaterialName(mat);
	}

	@Override
	public String getMaterialName() {
		return this.materialName;
	}

	protected void setMaterialName(String mpgMaterial) {
		this.materialName = mpgMaterial;
	}
	
	@Override
	public String print() {		
		StringBuilder sb = new StringBuilder();
		sb.append("- " + materialName + " sub object");
		sb.append(System.getProperty("line.separator"));
		sb.append(super.print());

		return sb.toString();
	}
}
