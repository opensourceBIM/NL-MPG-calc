package org.opensourcebim.ifccollection;

import org.opensourcebim.nmd.NmdProductCard;

public class MaterialSource {

	public MaterialSource(String oid, String name, String Source) {
		this.setOid(oid);
		this.setName(name);
		this.setSource(Source);
		clearMap();
	}
	
	private String oid;
	private String name;
	private String source;
	private Integer mapId;
	private String mapName;
	
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
	
	public Integer getMapId() {
		return this.mapId;
	}

	public String getMapName() {
		return this.mapName;
	}
	
	public void setMapping(NmdProductCard card) {
		this.mapId = card.getProductId();
		this.mapName = card.getDescription();
	}
	
	public void clearMap() {
		this.mapId = -1;
		this.mapName = "";
	}
	
}
