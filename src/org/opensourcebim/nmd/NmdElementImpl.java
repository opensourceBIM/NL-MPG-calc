package org.opensourcebim.nmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NmdElementImpl implements NmdElement {

	private String nlsfbCode;
	private String rawCode;
	private String elementName;
	private Collection<NmdProductCard> products;
	private Integer cuasId;
	
	public NmdElementImpl() {
		this.nlsfbCode = "";
		this.rawCode = "";
		this.elementName = "";
		this.products = new ArrayList<NmdProductCard>();
	}
	
	@Override
	public String getNLsfbCode() {
		return this.nlsfbCode;
	}
	
	public void setNlsfbCode(String nlsfbCode) {
		this.nlsfbCode = nlsfbCode;
	}

	@Override
	public String getRAWCode() {
		return this.rawCode;
	}
	
	public void setRawCode(String rawCode) {
		this.rawCode = rawCode;
	}

	@Override
	public String getElementName() {
		return this.elementName;
	}
	
	public void setElementName(String name) {
		this.elementName = name;
	}
	
	@Override
	public Integer getCUASId() {
		return cuasId;
	}

	public void setCUASId(Integer val) {
		this.cuasId = val;
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
