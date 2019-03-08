package org.opensourcebim.mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.opensourcebim.nmd.NmdDataService;
import org.opensourcebim.nmd.NmdElement;
import org.opensourcebim.nmd.NmdProductCard;

public abstract class BaseDataService implements NmdDataService {
	
	public abstract List<NmdElement> getData();
	
	public abstract void preLoadData();
	
	public List<NmdProductCard> getProductsForNLsfbCodes(Set<NlsfbCode> codes) {
		if (getData().size() == 0) {
			preLoadData();
		}
				
		List<NmdProductCard> res = getElementsForNLsfbCodes(codes).stream()
				.flatMap(el -> el.getProducts().stream()).collect(Collectors.toList());
		
		return res;
	}

	/**
	 * Quick lookup for preloaded elements
	 */
	public List<NmdElement> getElementsForNLsfbCodes(Set<NlsfbCode> codes) {
		if (getData().size() == 0) {
			preLoadData();
		}
				
		return getData().stream()
				.filter(el -> codes.stream()
						.anyMatch(code -> code == null ? false : el.getNlsfbCode().isSubCategoryOf(code) ))
				.collect(Collectors.toList());
	}
}
