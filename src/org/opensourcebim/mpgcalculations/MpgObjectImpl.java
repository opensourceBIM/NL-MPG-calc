package org.opensourcebim.mpgcalculations;

public class MpgObjectImpl implements MpgObject {
	private MpgMaterial mpgMaterial;
	private double volume;
	private double area;
	
	protected MpgObjectImpl() { }
	protected MpgObjectImpl(double volume, MpgMaterial mat) {
		this.setVolume(volume);
		this.setMaterial(mat);
	}
	
	protected MpgObjectImpl(double volume, double area) {
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
		// TODO Auto-generated method stub
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
		StringBuilder sb = new StringBuilder();
		sb.append("- " + getMaterial().getIfcName() + " object");
		sb.append(System.getProperty("line.separator"));
		sb.append(">> Volume: " + getVolume());
		sb.append(System.getProperty("line.separator"));
		sb.append(">> Area: " + "TBD");
		sb.append(System.getProperty("line.separator"));
		
		return sb.toString();
	}
}
