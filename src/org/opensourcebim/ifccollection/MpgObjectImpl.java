package org.opensourcebim.ifccollection;

/**
 * Class to store data of objects found in IFC files.
 * For many IFC objects a single object group can contain many objects (insulated walls etc.)
 * @author vijj
 */
public class MpgObjectImpl implements MpgObject {
	private MpgMaterial mpgMaterial;
	private double volume;
	private double area;

	public MpgObjectImpl() {
	}

	/**
	 * Object constructor for physical objects
	 * 
	 * @param volume Volume of object
	 * @param mat    material of object
	 */
	public MpgObjectImpl(double volume, MpgMaterial mat) {
		this.setVolume(volume);
		this.setMaterial(mat);
	}

	/**
	 * Object constructor for spaces
	 * 
	 * @param volume Volume of space
	 * @param area   Floor area of space
	 */
	public MpgObjectImpl(double volume, double area) {
		this.setArea(area);
		this.setVolume(volume);
		this.setMaterial(null);
	}

	@Override
	public MpgMaterial getMaterial() {
		return this.mpgMaterial;
	}

	protected void setMaterial(MpgMaterial mpgMaterial) {
		this.mpgMaterial = mpgMaterial;
	}

	@Override
	public double getVolume() {
		return volume;
	}

	protected void setVolume(double volume) {
		this.volume = volume;
	}

	@Override
	public double getArea() {
		return area;
	}

	public void setArea(double area) {
		this.area = area;
	}

	@Override
	public String print() {
		String materialName = "unknown material";
		if (getMaterial() != null) {
			materialName = getMaterial().getIfcName();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("- " + materialName + " object");
		sb.append(System.getProperty("line.separator"));
		sb.append(">> Volume: " + getVolume());
		sb.append(System.getProperty("line.separator"));
		sb.append(">> Area: " + "TBD");
		sb.append(System.getProperty("line.separator"));

		return sb.toString();
	}
}
