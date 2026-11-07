package com.pathfinder.model;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Activity
{
	String id;
	String title;
	String wikiUrl;
	ActivityCategory category;
	String description;
	String unlockHighlight; // e.g. "Unlocks Fairy Rings (free teleports across Gielinor)"
	int recommendedCombat;
	boolean ironmanFriendly;
	boolean hiddenGem; // Highlight off-the-beaten-path content

	@Builder.Default
	List<SkillRequirement> skillRequirements = Collections.emptyList();

	@Builder.Default
	List<String> questRequirements = Collections.emptyList();

	@Builder.Default
	List<String> tags = Collections.emptyList();

	@Builder.Default
	List<net.runelite.api.Skill> trainedSkills = Collections.emptyList();

	public String getSafeWikiUrl()
	{
		if (wikiUrl != null && !wikiUrl.isEmpty())
		{
			return wikiUrl;
		}
		return "https://oldschool.runescape.wiki/w/" + title.replace(" ", "_");
	}
}
