package org.opensourcebim.mapping;

/*
 * Indicator of how the mapping from mpgObject to NmdProduct was done.
 */
public enum NmdMappingType {
	None,
	DirectTotaalProduct,
	DirectDeelProduct,
	IndirectThroughParent,
	IndirectThroughChildren,
	Estimated,
	UserMapping,
}
