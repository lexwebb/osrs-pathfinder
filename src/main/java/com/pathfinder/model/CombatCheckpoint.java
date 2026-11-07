package com.pathfinder.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

@Value
@Builder
public class CombatCheckpoint
{
	String id;
	String name;
	String style; // MELEE, RANGED, MAGIC
	String tier;  // EARLY, MID, LATE, ENDGAME
	String description;
	String highlight;
	int recommendedCombat;

	@Builder.Default
	Map<Skill, Integer> recommendedSkills = Collections.emptyMap();

	@Builder.Default
	List<Integer> coreItemIds = Collections.emptyList();

	@Builder.Default
	List<String> coreItemNames = Collections.emptyList();
}
