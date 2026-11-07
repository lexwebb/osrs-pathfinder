package com.pathfinder.model;

public enum ActivityCategory
{
	ALL("All Activities"),
	GEAR("Gear Upgrades"),
	QUEST("Quests & Unlocks"),
	SKILLING("Engaging Skilling"),
	BOSSING("Entry & Mid Bossing"),
	MINIGAME("Minigames & Novelty"),
	DIARY("Achievement Diaries");

	private final String displayName;

	ActivityCategory(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
