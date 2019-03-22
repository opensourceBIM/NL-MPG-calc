package org.opensourcebim.bcfexport;

import java.util.Optional;

import org.bimserver.bimbots.BimBotsInput;
import org.opensourcebim.bcf.BcfFile;
import org.opensourcebim.bcf.TopicFolder;
import org.opensourcebim.bcf.markup.Markup;
import org.opensourcebim.bcf.markup.Topic;
import org.opensourcebim.bcf.visinfo.Component;
import org.opensourcebim.bcf.visinfo.ComponentSelection;
import org.opensourcebim.bcf.visinfo.Components;
import org.opensourcebim.bcf.visinfo.VisualizationInfo;
import org.opensourcebim.ifcanalysis.GuidCollection;
import org.opensourcebim.ifccollection.MpgObject;
import org.opensourcebim.ifccollection.MpgObjectStore;

public class ObjectStoreToBcfConverter {
	MpgObjectStore store;
	BcfExportSettings settings;

	public ObjectStoreToBcfConverter(MpgObjectStore store, BimBotsInput botInput) {
		this.store = store;

		settings = BcfExportSettings.getInstance();

		BcfExportSettings.getInstance().setAuthor("bim bot bcf export service");
	}

	public BcfFile write() {
		BcfFile file = new BcfFile();
		System.err.println();
		GuidCollection coll = store.getGuidsWithoutMaterialAndWithoutFullDecomposedMaterials();
		if (coll.getGuids().size() > 0) {
			TopicFolder noMats = createTopicFolderFromGuidCollection(coll, store);
			file.addTopicFolder(noMats);
		}

		coll = store.getGuidsWithRedundantMaterials();
		if (coll.getGuids().size() > 0) {
			TopicFolder redundantMats = createTopicFolderFromGuidCollection(coll, store);
			file.addTopicFolder(redundantMats);
		}

		coll = store.getGuidsWithoutMappings();
		if (coll.getGuids().size() > 0) {
			TopicFolder noMappings = createTopicFolderFromGuidCollection(coll, store);
			file.addTopicFolder(noMappings);
		}

		coll = store.getGuidsWithUndefinedLayerMats();
		if (coll.getGuids().size() > 0) {
			TopicFolder undefinedLayers = createTopicFolderFromGuidCollection(coll, store);
			file.addTopicFolder(undefinedLayers);
		}

		return file;
	}

	private TopicFolder createTopicFolderFromGuidCollection(GuidCollection coll, MpgObjectStore store) {
		TopicFolder folder = new TopicFolder();
		Markup markup = folder.getMarkup();

		ComponentSelection selection = new ComponentSelection();
		Components comps = new Components();
		comps.setSelection(selection);
		
		for (String guid : coll.getGuids()) {
			Component comp = new Component();
			comp.setIfcGuid(guid);

			Long oId = (long) -1;
			Optional<MpgObject> obj = store.getObjectByGuid(guid);
			if (obj.isPresent()) {
				oId = obj.get().getObjectId();
			}

			comp.setAuthoringToolId(oId.toString());
			comp.setOriginatingSystem("");
			selection.getComponent().add(comp);
		}


		VisualizationInfo vi = new VisualizationInfo();
		vi.setComponents(comps);
		folder.setVisualizationInfo(vi);

		Topic topic = new Topic();
		topic.setCreationAuthor(settings.getAuthor());
		topic.setDescription("ifc guids can be found in viewpoint file. "
				+ "AuthoringToolId field references to the bimserver generated object IDs. "
				+ "OriginatingSystem of IFC is unknown, but geometry and IFC are parsed by BimServer");
		topic.setPriority(BcfPriority.Normal.toString());
		topic.setStage("ifc model evaluation");
		topic.setTitle(coll.getDescription());
		topic.setTopicStatus(BcfTopicStatus.Active.toString());
		topic.setTopicType(BcfTopicType.Issue.toString());

		markup.setTopic(topic);

		return folder;
	}
}
