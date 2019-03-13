package org.opensourcebim.bcf;

import java.util.List;

public class BcfTopic extends BcfModifiableObject {

	private String Title;
	private BcfTopicType type;
	private BcfTopicStatus status;
	private BcfPriority priority;
	
	private List<BcfComment> comments;
	private List<BcfViewPoint> viewPoints;
}
