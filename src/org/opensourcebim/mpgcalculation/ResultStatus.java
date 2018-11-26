package org.opensourcebim.mpgcalculation;

/**
 * enumerator for the different result outcomes
 * @author vijj
 *
 */
public enum ResultStatus {
	NotRun, // calcualtor not run yet
	NoData, // no data found in object model
	IncompleteData, // warnings or errors encountered in object data
	ValueError, // could not complete some of the calculations
	Success // finished calculations.
}
