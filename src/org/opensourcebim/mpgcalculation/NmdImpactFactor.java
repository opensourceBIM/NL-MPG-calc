package org.opensourcebim.mpgcalculation;

/**
 * Enumerator for the various impact factors that can be present in a single
 * basisprofiel These coeficients can be multiplied with the total mass, volume
 * or energy equivalent and summed to get a single number (per basisprofiel)
 * 
 * @author vijj
 *
 */
public enum NmdImpactFactor {
	AbioticDepletionNonFuel, // in kg antimoon
	AbioticDepletionFuel, // in kg antimoon, or 4,81E-4 kg antimoon/MJ to convert from MJ to kg
	GWP100, // kg CO2
	ODP, // in kg CFC11
	PhotoChemicalOxidation, // kg etheen
	Acidifcation, // kg SO2
	Eutrophication, // kg (PO4)^3-
	HumanToxicity, // kg 1,4 dichloor benzeen
	FreshWaterAquaticEcoToxicity, // kg 1,4 dichloor benzeen
	MarineAquaticEcoToxicity, // kg 1,4 dichloor benzeen
	TerrestrialEcoToxocity, // kg 1,4 dichloor benzeen
	TotalRenewableEnergy, // MJ, netto calorische waarde
	TotalNonRenewableEnergy, // MJ, netto calorische waarde
	TotalEnergy, // MJ, netto calorische waarde
	FreshWaterUse, // m3
	NonHazardousWaste, // kg
	HazardousWaste, // kg
}
