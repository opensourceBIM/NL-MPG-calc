package org.opensourcebim.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.ifccollection.MpgObjectStore;
import org.opensourcebim.test.BimBotTest;

@RunWith(Parameterized.class)
public class NMDSelectionIntegrationTest extends BaseServiceIntegrationTest<IfcToMpgCollectionService> {

	private MpgObjectStore referencemodel;

	public NMDSelectionIntegrationTest(String relPath, String filename, Object referenceData) {
		super(relPath, filename, referenceData);
		this.bimbot = new IfcToMpgCollectionService();
		if (referenceData instanceof MpgObjectStore) {
			this.referencemodel = (MpgObjectStore) referenceData;
		} else {
			fail("referenceData needs to be of type: " + MpgObjectStore.class.getSimpleName());
		}
	}

	@Test
	public void TestResultJsonContainsReferenceData() {
		BimBotTest test = new BimBotTest(this.getFullIfcModelPath(), factory, authInfo, this.bimbot);
		test.run();
		MpgObjectStore results = this.getService().getStore();
		results.getElements().forEach(el -> {
			// get reference element by name
			MpgElement refEl = referencemodel.getElementByName(el.getMpgObject().getObjectName());
			assertEquals("expected MPG coefficient sum deviates too much from actual sum",
					refEl.getNmdProductCards().stream().mapToDouble(pc -> pc.getProfileSetsCoeficientSum()).sum(),
					el.getNmdProductCards().stream().mapToDouble(pc -> pc.getProfileSetsCoeficientSum()).sum(),
					0.001);
		});
	}
}
