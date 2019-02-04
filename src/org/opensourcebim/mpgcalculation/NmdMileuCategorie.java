package org.opensourcebim.mpgcalculation;

/**
 * class for the various impact factors that can be present in a single
 * basisprofiel These coeficients can be multiplied with the total mass, volume
 * or energy equivalent and summed to get a single number (per basisprofiel)
 * 
 * @author vijj
 *
*/
public class NmdMileuCategorie {
	private String description;
	private String unit;
	private Double weight;
	
	public NmdMileuCategorie(String description, String unit, Double weight) {
		setDescription(description);
		setUnit(unit);
		setWeight(weight);
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getUnit() {
		return unit;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}
	public Double getWeight() {
		return weight;
	}
	public void setWeight(Double weight) {
		this.weight = weight;
	}
	
	
}
