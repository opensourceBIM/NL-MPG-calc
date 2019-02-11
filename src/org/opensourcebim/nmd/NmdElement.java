package org.opensourcebim.nmd;

import java.util.Collection;
import java.util.List;

public interface NmdElement {

	String getNLsfbCode();

	String getRAWCode();

	String getElementName();
	
	Integer getCUASId();

	Collection<NmdProductCard> getProducts();

	void addProductCards(List<NmdProductCard> products);

	void addProductCard(NmdProductCard fullCard);
}
