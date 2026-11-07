package com.pathfinder.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

@Value
@Builder
public class BossCheckpoint
{
	String id;
	String bossName;
	String tier; // MID, LATE, ENDGAME
	String description;
	String unlockHighlight;
	String wikiUrl;
	int recommendedCombat;

	@Builder.Default
	Map<Skill, Integer> recommendedSkills = Collections.emptyMap();

	@Builder.Default
	List<String> prerequisiteQuests = Collections.emptyList();

	@Builder.Default
	List<Integer> recommendedItemIds = Collections.emptyList();

	@Builder.Default
	List<String> recommendedItemNames = Collections.emptyList();
}
