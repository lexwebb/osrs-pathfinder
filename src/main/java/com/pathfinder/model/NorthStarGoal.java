package com.pathfinder.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

@Value
@Builder
public class NorthStarGoal
{
	public enum GoalType
	{
		ACTIVITY,        // Quests, diaries, minigames, skilling methods
		COMBAT_CHECKPOINT, // Tiered gear setup (Melee/Ranged/Magic)
		BOSS_CHECKPOINT,   // Boss readiness (Zulrah, Vorkath, CG, etc.)
		GEAR_ITEM        // Specific individual gear goal (e.g. Fire Cape, Whip)
	}

	String id;
	String title;
	GoalType type;
	String category; // "Grandmaster Quest", "Mid-Game Melee", "Boss Encounter", etc.
	String description;
	String highlight;
	String wikiUrl;
	int targetCombat;

	@Builder.Default
	Map<Skill, Integer> targetSkills = Collections.emptyMap();

	@Builder.Default
	List<String> prerequisiteQuests = Collections.emptyList();

	@Builder.Default
	List<Integer> requiredItemIds = Collections.emptyList();

	@Builder.Default
	List<String> requiredItemNames = Collections.emptyList();
}
