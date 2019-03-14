package org.opensourcebim.bcfexport;


import org.bimserver.bimbots.BimBotsInput;
import org.opensourcebim.bcf.BcfFile;
import org.opensourcebim.bcf.TopicFolder;
import org.opensourcebim.bcf.markup.Markup;
import org.opensourcebim.bcf.markup.Topic;
import org.opensourcebim.ifcanalysis.GuidCollection;
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
			TopicFolder noMats = createTopicFolderFromGuidCollection(coll);
			file.addTopicFolder(noMats);
		}

		coll = store.getGuidsWithRedundantMaterials();
		if (coll.getGuids().size() > 0) {
			TopicFolder redundantMats = createTopicFolderFromGuidCollection(coll);
			file.addTopicFolder(redundantMats);
		}
		
		coll = store.getGuidsWithoutMappings();
		if (coll.getGuids().size() > 0) {
			TopicFolder redundantMats = createTopicFolderFromGuidCollection(coll);
			file.addTopicFolder(redundantMats);
		}
		
		return file;
	}

	private TopicFolder createTopicFolderFromGuidCollection(GuidCollection coll) {
		TopicFolder folder = new TopicFolder();
		Markup markup = folder.getMarkup();

		Topic topic = new Topic();
		topic.setCreationAuthor(settings.getAuthor());
		topic.setDescription(String.join(", ", coll.getGuids()));
		topic.setPriority(BcfPriority.Normal.toString());
		topic.setStage("ifc model evaluation");
		topic.setTitle(coll.getDescription());
		topic.setTopicStatus(BcfTopicStatus.Active.toString());
		topic.setTopicType(BcfTopicType.Issue.toString());

		markup.setTopic(topic);

		return folder;
	}

}
