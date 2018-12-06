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
	public MpgLayerImpl(double volume, String mat, String guid) {
		super(volume, 0.0); // TODO: what to do with area?
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
		sb.append(super.print());

		return sb.toString();
	}
}
