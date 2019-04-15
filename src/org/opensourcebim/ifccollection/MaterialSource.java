package org.opensourcebim.ifccollection;

import nl.tno.bim.nmd.domain.NmdProductCard;

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
	
	/**
	 * the object id of the IfcMaterial object 
	 */
	public String getOid() {
		return oid;
	}
	private void setOid(String oid) {
		this.oid = oid;
	}
	
	/**
	 * the material description as in the IfcMaterial object
	 */
	public String getName() {
		return name;
	}
	private void setName(String name) {
		this.name = name;
	}
	
	/**
	 * 
	 */
	public String getSource() {
		return source;
	}
	private void setSource(String source) {
		this.source = source;
	}
	
	/**
	 * the NMD product card id that has been used for the map
	 */
	public Integer getMapId() {
		return this.mapId;
	}

	/**
	 * descrption of the nmd product card used fir the mapping
	 */
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
	public MaterialSource copy() {
		MaterialSource res = new MaterialSource(this.getOid(), this.getName(), this.getSource());
		res.mapId = this.getMapId();
		res.mapName = this.getMapName();
		return res;
	}
	
}
