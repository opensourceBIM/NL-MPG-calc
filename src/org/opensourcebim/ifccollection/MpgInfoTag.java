package org.opensourcebim.ifccollection;

public class MpgInfoTag {
	private MpgInfoTagType type;
	private String message;
	
	public MpgInfoTag(MpgInfoTagType type, String message) {
		this.type = type;
		this.message = message;
	}

	public MpgInfoTagType getType() {
		return this.type;
	}
	
	public String getMessage() {
		return this.message;
	}
}
