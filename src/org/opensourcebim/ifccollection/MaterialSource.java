package org.opensourcebim.ifccollection;

public class MaterialSource {

	public MaterialSource(String oid, String name, String Source) {
		this.setOid(oid);
		this.setName(name);
		this.setSource(Source);
	}
	
	private String oid;
	private String name;
	private String source;
	
	public String getOid() {
		return oid;
	}
	private void setOid(String oid) {
		this.oid = oid;
	}
	public String getName() {
		return name;
	}
	private void setName(String name) {
		this.name = name;
	}
	public String getSource() {
		return source;
	}
	private void setSource(String source) {
		this.source = source;
	}
	
}
