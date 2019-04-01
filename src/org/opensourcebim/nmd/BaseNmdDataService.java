package org.opensourcebim.nmd;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.opensourcebim.mapping.NlsfbCode;

public interface BaseNmdDataService extends NmdDataService {
	
	abstract List<NmdElement> getData();
	
	abstract void preLoadData();
	
	default List<NmdProductCard> getProductsForNLsfbCodes(Set<NlsfbCode> codes) {
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
	default List<NmdElement> getElementsForNLsfbCodes(Set<NlsfbCode> codes) {
		if (getData().size() == 0) {
			preLoadData();
		}
				
		return getData().stream()
				.filter(el -> codes.stream()
						.anyMatch(code -> code == null ? false : el.getNlsfbCode().isSubCategoryOf(code) ))
				.collect(Collectors.toList());
	}
	
	default List<NmdProductCard> getProductCardsByIds(List<Long> ids) {
		return getData().stream().flatMap(el -> el.getProducts().stream())
				.filter(pc -> ids.contains((long)pc.getProductId()))
				.collect(Collectors.toList());
	}
}
