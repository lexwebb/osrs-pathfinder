package com.pathfinder.model;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

@Value
@Builder
public class NorthStarProgress
{
	@Value
	@Builder
	public static class SkillGap
	{
		Skill skill;
		int currentLevel;
		int targetLevel;
		int levelsRemaining;
	}

	@Value
	@Builder
	public static class ItemGap
	{
		int itemId;
		String itemName;
	}

	NorthStarGoal goal;
	boolean isFullyReady;
	int totalRequirements;
	int satisfiedRequirements;
	int progressPercent; // 0 - 100%

	@Builder.Default
	List<SkillGap> missingSkills = Collections.emptyList();

	@Builder.Default
	List<String> missingQuests = Collections.emptyList();

	@Builder.Default
	List<ItemGap> missingItems = Collections.emptyList();

	public boolean hasSkillGap(Skill skill)
	{
		for (SkillGap gap : missingSkills)
		{
			if (gap.getSkill() == skill)
			{
				return true;
			}
		}
		return false;
	}

	public SkillGap getSkillGap(Skill skill)
	{
		for (SkillGap gap : missingSkills)
		{
			if (gap.getSkill() == skill)
			{
				return gap;
			}
		}
		return null;
	}

	public boolean hasQuestGap(String quest)
	{
		for (String q : missingQuests)
		{
			if (q.equalsIgnoreCase(quest))
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasItemGap(int itemId)
	{
		for (ItemGap item : missingItems)
		{
			if (item.getItemId() == itemId)
			{
				return true;
			}
		}
		return false;
	}
}
