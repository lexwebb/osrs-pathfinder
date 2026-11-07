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
	boolean isBronzeman;

	BankSnapshot bankSnapshot;

	@Builder.Default
	Map<Skill, Integer> skills = new HashMap<>();

	@Builder.Default
	Set<String> completedQuests = new HashSet<>();

	@Builder.Default
	Set<String> completedDiaries = new HashSet<>();

	@Builder.Default
	Set<Integer> unlockedItemIds = new HashSet<>(); // For Bronzeman Mode tracking

	public int getSkillLevel(Skill skill)
	{
		return skills.getOrDefault(skill, 1);
	}

	public boolean hasCompletedQuest(String questName)
	{
		if (questName == null || questName.isEmpty())
		{
			return false;
		}

		if (completedQuests.contains(questName))
		{
			return true;
		}

		String normalized = normalize(questName);
		for (String completed : completedQuests)
		{
			if (normalize(completed).equalsIgnoreCase(normalized))
			{
				return true;
			}
		}

		return false;
	}

	public boolean hasScannedBank()
	{
		return bankSnapshot != null && bankSnapshot.hasScanned();
	}

	public boolean hasUnlockedItem(int itemId)
	{
		if (unlockedItemIds.contains(itemId))
		{
			return true;
		}
		return bankSnapshot != null && bankSnapshot.ownsItem(itemId);
	}

	private static String normalize(String s)
	{
		return s.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
	}
}
