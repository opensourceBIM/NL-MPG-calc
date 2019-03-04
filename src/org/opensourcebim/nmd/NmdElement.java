package org.opensourcebim.nmd;

import java.util.Collection;
import java.util.List;

import org.opensourcebim.mapping.NlsfbCode;

public interface NmdElement {

	NlsfbCode getNlsfbCode();

	Integer getElementId();

	Integer getParentId();
	
	Boolean getIsElementPart();

	String getElementName();
	
	Boolean getIsMandatory();
	
	Collection<NmdProductCard> getProducts();

	void addProductCards(List<NmdProductCard> products);

	void addProductCard(NmdProductCard fullCard);



}
