package org.opensourcebim.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.util.buf.StringUtils;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.test.BimBotTest;


@RunWith(Parameterized.class)
public class ObjectCollectionIntegrationTest extends BaseServiceIntegrationTest<IfcToJsonDatasetService> {

	private MpgObjectStore referencemodel;

	public ObjectCollectionIntegrationTest(String relPath, String filename, Object referenceData) {
		super(relPath, filename, referenceData);
		this.bimbot = new IfcToJsonDatasetService();
		if (referenceData instanceof MpgObjectStore) {
			this.referencemodel = (MpgObjectStore) referenceData;
		} else {
			try {
				throw new InvalidInputException(
						"referenceData needs to be of type: " + MpgObjectStore.class.getSimpleName());
			} catch (InvalidInputException e) {
				fail();
			}
		}
	}

	@Test
	public void TestResultJsonContainsReferenceData() {
		BimBotTest test = new BimBotTest(this.getFullIfcModelPath(), factory, authInfo, this.bimbot);
		test.run();
		MpgObjectStore results = this.getService().getStore();

		Map<String, List<MpgElement>> resultGroups = results.getCleanedElementGroups();
		Map<String, List<MpgElement>> referenceGroups = this.referencemodel.getElementGroups();

		// copy the keys to separate sets
		Set<String> resultKeys = new HashSet<>();
		resultKeys.addAll(resultGroups.keySet());
		Set<String> refKeys = new HashSet<>();
		refKeys.addAll(referenceGroups.keySet());

		// clean up keys back and forth and check that no keys are unique in both sets.
		refKeys.removeAll(resultGroups.keySet());
		resultKeys.removeAll(referenceGroups.keySet());
		String onlyInRefKeys = StringUtils.join(refKeys, ';');
		String onlyInResKeys = StringUtils.join(resultKeys, ';');
		// accept less than 5% non matching keys
		assertTrue(String.format(
				"found too many mismatching elements: \n > reference keys: %s \n > result keys: %s \n ",
				onlyInRefKeys, onlyInResKeys),
				(double)(refKeys.size() + resultKeys.size()) /
				(resultGroups.keySet().size() + referenceGroups.keySet().size()) <= 1.05);

		resultGroups.forEach((key, elements) -> {
			if (referenceGroups.containsKey(key)) {
				assertEquals(referenceGroups.get(key).size(), elements.size());
			}
		});

	}
}
