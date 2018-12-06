package org.opensourcebim.ifccollection;

public class MpgSpaceImpl implements MpgSpace {

	private double volume;
	private double area;
	protected String id;
	
	/**
	 * Object constructor for spaces
	 * 
	 * @param volume Volume of space
	 * @param area   Floor area of space
	 */
	public MpgSpaceImpl(double volume, double area) {
		this.setArea(area);
		this.setVolume(volume);
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
	public String getId() {
		return this.id;
	}

	@Override
	public String print() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(">> Volume: " + getVolume());
		sb.append(System.getProperty("line.separator"));
		sb.append(">> Area: " + "TBD");
		sb.append(System.getProperty("line.separator"));
		
		return sb.toString();
	}



}
