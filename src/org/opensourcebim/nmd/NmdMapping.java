package org.opensourcebim.nmd;

/*
 * Indicator of how the mapping from mpgObject to NmdProduct was done.
 */
public enum NmdMapping {
	None,
	DirectTotaalProduct,
	DirectDeelProduct,
	Estimated,
	UserMapping,
}
