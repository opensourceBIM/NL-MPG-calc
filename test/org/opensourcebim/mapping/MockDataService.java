package org.opensourcebim.mapping;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import nl.tno.bim.nmd.domain.NmdElement;
import nl.tno.bim.nmd.domain.NmdProductCard;
import nl.tno.bim.nmd.domain.NmdProfileSet;
import nl.tno.bim.nmd.services.BaseNmdDataService;

public class MockDataService implements BaseNmdDataService {

	private List<NmdElement> elements;
	private Boolean connected;

	@Override
	public void login() {
		connected = true;
	}

	@Override
	public void logout() {
		connected = false;
	}

	@Override
	public Boolean getIsConnected() {
		return connected;
	}

	@Override
	public void preLoadData() {

	}

	@Override
	public Calendar getRequestDate() {
		return Calendar.getInstance();
	}

	@Override
	public void setRequestDate(Calendar newDate) {
	}

	@Override
	public List<NmdElement> getAllElements() {
		return this.elements;
	}

	public void setElements(List<NmdElement> elements) {
		this.elements = elements;
	}

	@Override
	public List<NmdElement> getData() {
		return this.elements;
	}

	@Override
	public HashMap<Integer, NmdProfileSet> getProfileSetsByIds(List<Integer> ids) {
		return new HashMap<Integer, NmdProfileSet>();
	}

	@Override
	public List<NmdProductCard> getProductsForElement(NmdElement element) {
		return element.getProducts().stream().collect(Collectors.toList());
	}
	
	@Override
	public Boolean getAdditionalProfileDataForCard(NmdProductCard c) {
		return true;
	}
}
