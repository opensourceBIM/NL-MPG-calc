package org.opensourcebim.bcfexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.opensourcebim.bcf.BcfFile;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.nmd.ObjectStoreBuilder;

public class BcfConverterTest {

	ObjectStoreBuilder builder;
	ObjectStoreToBcfConverter converter;

	public BcfConverterTest() {
		builder = new ObjectStoreBuilder();
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testConverterCanHandleEmptyStore() {
		converter = new ObjectStoreToBcfConverter(builder.getStore(), null);
		BcfFile bcf = converter.write();
		assertEquals(0, bcf.getTopicFolders().size());
	}

	@Test
	public void testConverterCreatesTopicsForMissingmaterials() {
		builder.AddUnmappedMpgElement("dummy element", false, new HashMap<String, Double>(),
				new Double[] { 1.0, 1.0, 1.0 }, "11.11", "IfcDummy", "");
		converter = new ObjectStoreToBcfConverter(builder.getStore(), null);
		BcfFile bcf = converter.write();
		
		int expectedTopics = 2; // no material and not mapped
		assertEquals(expectedTopics, bcf.getTopicFolders().size());
		assertTrue(bcf.getTopicFolders().parallelStream()
				.allMatch(t -> !t.getMarkup().getTopic().getDescription().isEmpty()));
	}

	@SuppressWarnings("serial")
	@Test
	public void testConverterCreatesTopicForRedundantMaterials() {
		
		builder.AddUnmappedMpgElement("dummy element", false,
				new HashMap<String, Double>() { {put("mat1", 2.0);}  {put("mat2", 1.0);} },
				new Double[] {1.0, 1.0, 1.0},
				"11.11", "IfcDummy", "");
		converter = new ObjectStoreToBcfConverter(builder.getStore(), null);
		BcfFile bcf = converter.write();
		int expectedTopics = 2; // both unmapped and redundant
		assertEquals(expectedTopics, bcf.getTopicFolders().size());
		assertTrue(bcf.getTopicFolders().parallelStream().allMatch(t -> !t.getMarkup().getTopic().getDescription().isEmpty()));
	}

	@SuppressWarnings("serial")
	@Test
	public void testConverterCreatesTopicForUnMappedObjects() {
		builder.AddUnmappedMpgElement("dummy element", false, new HashMap<String, Double>() {
			{
				put("mat1", 2.0);
			}
		}, new Double[] { 1.0, 1.0, 1.0 }, "11.11", "IfcDummy", "");

		converter = new ObjectStoreToBcfConverter(builder.getStore(), null);
		BcfFile bcf = converter.write();
		int expectedTopics = 1; // only non mapped topic
		assertEquals(expectedTopics, bcf.getTopicFolders().size());
		assertTrue(bcf.getTopicFolders().parallelStream()
				.allMatch(t -> !t.getMarkup().getTopic().getDescription().isEmpty()));
	}

	@Test
	public void testConverterCreatesEmptyBcfWhenNoErrorsEncountered() {
		builder.addMappedMpgElement("dummy element", "dummy material", "m", 1, 100);
		converter = new ObjectStoreToBcfConverter(builder.getStore(), null);
		BcfFile bcf = converter.write();
		
		int expectedTopics = 0;
		assertEquals(expectedTopics, bcf.getTopicFolders().size());
	}

}
