package org.opensourcebim.nmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opensourcebim.ifccollection.NlsfbCode;

public class NmdElementImpl implements NmdElement {

	private NlsfbCode nlsfbCode;
	private Integer elementId;
	private String elementName;
	private Collection<NmdProductCard> products;
	private Integer parentId;
	private Boolean isMandatory;
	
	public NmdElementImpl() {
		this.nlsfbCode = null;
		this.elementId = -1;
		this.elementName = "";
		this.products = new ArrayList<NmdProductCard>();
	}
	
	@Override
	public NlsfbCode getNlsfbCode() {
		return this.nlsfbCode;
	}
	
	public void setNlsfbCode(NlsfbCode nlsfbCode) {
		this.nlsfbCode = nlsfbCode;
	}

	@Override
	public Integer getElementId() {
		return this.elementId;
	}
	
	public void setElementId(Integer id) {
		this.elementId = id;
	}

	@Override
	public Integer getParentId() {
		return this.parentId;
	}
	
	@Override
	public Boolean getIsElementPart() {
		return this.parentId > 0;
	}
	
	public void setParentId(Integer id) {
		this.parentId = id;
	}

	@Override
	public String getElementName() {
		return this.elementName;
	}
	
	public void setElementName(String name) {
		this.elementName = name;
	}
		
	@Override
	public Boolean getIsMandatory() {
		return this.isMandatory;
	}
	
	public void setIsMandatory(Boolean flag) {
		this.isMandatory = flag;
	}
	
	@Override
	public Collection<NmdProductCard> getProducts() {
		return this.products;
	}
	
	@Override
	public void addProductCard(NmdProductCard product) {
		this.products.add(product);
	}

	@Override
	public void addProductCards(List<NmdProductCard> cards) {
		this.products.addAll(cards);
	}
}
