package com.pathfinder.model;

import lombok.Value;
import net.runelite.api.Skill;

@Value
public class SkillRequirement
{
	Skill skill;
	int level;
}
