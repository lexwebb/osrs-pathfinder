package com.pathfinder;

import com.pathfinder.model.ActivityCategory;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pathfinder")
public interface PathfinderConfig extends Config
{
	enum AccountMode
	{
		AUTO_DETECT("Auto Detect"),
		STANDARD("Standard Account"),
		BRONZEMAN("Bronzeman Mode"),
		IRONMAN("Ironman Mode");

		private final String name;

		AccountMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	@ConfigItem(
		keyName = "accountMode",
		name = "Account Mode",
		description = "Enables custom rules like Bronzeman mode (must unlock items organically before buying on GE)"
	)
	default AccountMode accountMode()
	{
		return AccountMode.AUTO_DETECT;
	}

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
}
