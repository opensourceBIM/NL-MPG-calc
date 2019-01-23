package org.opensourcebim.mpgcalculation;

/**
 * Enumerator for the various stages in the product lifecycle. Every individual
 * material will have several lifecyce stages
 * 
 * @author vijj
 *
 */
public enum NmdLifeCycleStage {
	TransportToSite, // transport from producer to building site per [tonnne * kg] of material
	TransportForRemoval, // transport from building site to disposal site. 
	ConstructionAndReplacements, // production impact corrected for losses during production and replacements per kg of material
	Operation, // emissions and operation impact (water, energy etc.) during building operation per m3 or MJ of usage
	Disposal, // disposal to landfill per kg of material
	Incineration, // incineration per kg of material
	Recycling, // recycling of materials per kg of material
	Reuse, // complete reuse of the product as is without any other operations.
	OwnDisposalProfile // custom disposal profile.
}