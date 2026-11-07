package com.pathfinder.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

@Value
@Builder
public class AccountProfile
{
	int combatLevel;
	int totalLevel;
	boolean isIronman;

	@Builder.Default
	Map<Skill, Integer> skills = new HashMap<>();

	@Builder.Default
	Set<String> completedQuests = new HashSet<>();

	public int getSkillLevel(Skill skill)
	{
		return skills.getOrDefault(skill, 1);
	}

	public boolean hasCompletedQuest(String questName)
	{
		return completedQuests.contains(questName);
	}
}
