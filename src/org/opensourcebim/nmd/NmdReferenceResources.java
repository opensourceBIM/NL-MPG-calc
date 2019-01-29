package org.opensourcebim.nmd;

import java.util.HashMap;

import org.opensourcebim.mpgcalculation.NmdMileuCategorie;

/*
 * Datastore for mappings from the NMD that only have to be retrieved once per session. 
 */
public class NmdReferenceResources {

	private HashMap<Integer, String> faseMapping;
	private HashMap<Integer, String> unitMapping;
	private HashMap<Integer, NmdMileuCategorie> milieuCategorieMapping;
	
	public NmdReferenceResources() {
		setFaseMapping(new HashMap<Integer, String>());
		setUnitMapping(new HashMap<Integer, String>());
		setMilieuCategorieMapping(new HashMap<Integer, NmdMileuCategorie>());
	}

	public HashMap<Integer, String> getFaseMapping() {
		return faseMapping;
	}

	public void setFaseMapping(HashMap<Integer, String> faseMapping) {
		this.faseMapping = faseMapping;
	}

	public HashMap<Integer, String> getUnitMapping() {
		return unitMapping;
	}

	public void setUnitMapping(HashMap<Integer, String> unitMapping) {
		this.unitMapping = unitMapping;
	}

	public HashMap<Integer, NmdMileuCategorie> getMilieuCategorieMapping() {
		return milieuCategorieMapping;
	}

	public void setMilieuCategorieMapping(HashMap<Integer, NmdMileuCategorie> milieuWaardeMapping) {
		this.milieuCategorieMapping = milieuWaardeMapping;
	}
	
}
