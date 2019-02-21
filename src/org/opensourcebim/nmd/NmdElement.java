package org.opensourcebim.nmd;

import java.util.Collection;
import java.util.List;

public interface NmdElement {

	String getNLsfbCode();

	Integer getElementId();

	Integer getParentId();
	
	Boolean getIsElementPart();

	String getElementName();
	
	Boolean getIsMandatory();
	
	Collection<NmdProductCard> getProducts();

	void addProductCards(List<NmdProductCard> products);

	void addProductCard(NmdProductCard fullCard);



}
