package com.pathfinder.model;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GearUpgrade
{
	String id;
	String name;
	String slot;
	int itemId;
	int recommendedCombat;
	long estimatedPrice;
	String ironmanSource;
	String wikiUrl;
	String highlight;
	String questReq;

	@Builder.Default
	List<SkillRequirement> skillReqs = Collections.emptyList();
}
