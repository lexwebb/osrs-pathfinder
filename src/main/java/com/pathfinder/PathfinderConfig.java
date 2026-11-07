package com.pathfinder;

import com.pathfinder.model.ActivityCategory;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pathfinder")
public interface PathfinderConfig extends Config
{
	@ConfigItem(
		keyName = "defaultCategory",
		name = "Default Category",
		description = "Initial category shown when opening Pathfinder"
	)
	default ActivityCategory defaultCategory()
	{
		return ActivityCategory.ALL;
	}

	@ConfigItem(
		keyName = "preferHiddenGems",
		name = "Prioritize Hidden Gems",
		description = "Scores off-the-beaten-path minigames and alternative skilling methods higher"
	)
	default boolean preferHiddenGems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ironmanFriendlyOnly",
		name = "Ironman Only Mode",
		description = "Only recommend activities and methods that work well for Ironmen"
	)
	default boolean ironmanFriendlyOnly()
	{
		return false;
	}
}
