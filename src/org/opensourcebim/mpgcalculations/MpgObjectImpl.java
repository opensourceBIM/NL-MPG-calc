package org.opensourcebim.mpgcalculations;

public class MpgObjectImpl implements MpgObject {
	private MpgMaterial mpgMaterial;
	private Double volume;
	
	protected MpgObjectImpl() { }
	protected MpgObjectImpl(double volume, MpgMaterial mat) {
		this.setVolume(volume);
		this.setMaterial(mat);
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

}
