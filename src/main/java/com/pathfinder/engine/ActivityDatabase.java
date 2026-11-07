package com.pathfinder.engine;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pathfinder.model.Activity;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.BossCheckpoint;
import com.pathfinder.model.CombatCheckpoint;
import com.pathfinder.model.GearUpgrade;
import com.pathfinder.model.NorthStarGoal;
import com.pathfinder.model.SkillRequirement;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;

@Slf4j
@Singleton
public class ActivityDatabase
{
	private final List<Activity> activities = new ArrayList<>();
	private final List<GearUpgrade> gearUpgrades = new ArrayList<>();
	private final List<CombatCheckpoint> combatCheckpoints = new ArrayList<>();
	private final List<BossCheckpoint> bossCheckpoints = new ArrayList<>();
	private final Gson gson;

	@Inject
	public ActivityDatabase(Gson gson)
	{
		this.gson = gson;
		loadActivities();
		loadGearUpgrades();
		loadCombatCheckpoints();
		loadBossCheckpoints();

		if (activities.isEmpty())
		{
			log.warn("Pathfinder fallback: initializing hardcoded activities");
			initFallbackActivities();
		}
	}

	public List<Activity> getAllActivities()
	{
		return Collections.unmodifiableList(activities);
	}

	public List<GearUpgrade> getGearUpgrades()
	{
		return Collections.unmodifiableList(gearUpgrades);
	}

	public List<CombatCheckpoint> getCombatCheckpoints()
	{
		return Collections.unmodifiableList(combatCheckpoints);
	}

	public List<BossCheckpoint> getBossCheckpoints()
	{
		return Collections.unmodifiableList(bossCheckpoints);
	}

	public Activity getActivity(String id)
	{
		for (Activity a : activities)
		{
			if (a.getId().equalsIgnoreCase(id))
			{
				return a;
			}
		}
		return null;
	}

	public GearUpgrade getGearUpgrade(String id)
	{
		for (GearUpgrade g : gearUpgrades)
		{
			if (g.getId().equalsIgnoreCase(id))
			{
				return g;
			}
		}
		return null;
	}

	public GearUpgrade getGearUpgradeByItemId(int itemId)
	{
		for (GearUpgrade g : gearUpgrades)
		{
			if (g.getItemId() == itemId)
			{
				return g;
			}
		}
		return null;
	}

	public CombatCheckpoint getCombatCheckpoint(String id)
	{
		for (CombatCheckpoint cp : combatCheckpoints)
		{
			if (cp.getId().equalsIgnoreCase(id))
			{
				return cp;
			}
		}
		return null;
	}

	public BossCheckpoint getBossCheckpoint(String id)
	{
		for (BossCheckpoint bp : bossCheckpoints)
		{
			if (bp.getId().equalsIgnoreCase(id))
			{
				return bp;
			}
		}
		return null;
	}

	public NorthStarGoal resolveGoal(String goalId)
	{
		if (goalId == null || goalId.trim().isEmpty() || "none".equalsIgnoreCase(goalId))
		{
			return null;
		}

		// 1. Check Combat Checkpoints
		CombatCheckpoint cp = getCombatCheckpoint(goalId);
		if (cp != null)
		{
			return createGoalFromCombatCheckpoint(cp);
		}

		// 2. Check Boss Checkpoints
		BossCheckpoint bp = getBossCheckpoint(goalId);
		if (bp != null)
		{
			return createGoalFromBossCheckpoint(bp);
		}

		// 3. Check Activities (quests, minigames, skilling, etc.)
		String cleanActId = goalId.startsWith("activity-") ? goalId.substring("activity-".length()) : goalId;
		Activity a = getActivity(cleanActId);
		if (a == null)
		{
			a = getActivity(goalId);
		}
		if (a != null)
		{
			return createGoalFromActivity(a);
		}

		// 4. Check Gear Upgrades
		String cleanGearId = goalId.startsWith("gear-") ? goalId.substring("gear-".length()) : goalId;
		GearUpgrade g = getGearUpgrade(cleanGearId);
		if (g == null)
		{
			g = getGearUpgrade(goalId);
		}
		if (g != null)
		{
			return createGoalFromGear(g);
		}

		// 5. Fallback search by title
		for (Activity act : activities)
		{
			if (act.getTitle().equalsIgnoreCase(goalId))
			{
				return createGoalFromActivity(act);
			}
		}

		return null;
	}

	public NorthStarGoal createGoalFromActivity(Activity activity)
	{
		Map<Skill, Integer> targetSkills = new HashMap<>();
		for (SkillRequirement sr : activity.getSkillRequirements())
		{
			targetSkills.put(sr.getSkill(), sr.getLevel());
		}

		String cat = activity.getCategory() == ActivityCategory.QUEST ? "Quest Goal"
			: activity.getCategory() == ActivityCategory.BOSSING ? "Boss Encounter"
			: activity.getCategory() == ActivityCategory.DIARY ? "Achievement Diary"
			: "Milestone Goal";

		return NorthStarGoal.builder()
			.id("activity-" + activity.getId())
			.title(activity.getTitle())
			.type(NorthStarGoal.GoalType.ACTIVITY)
			.category(cat)
			.description(activity.getDescription())
			.highlight(activity.getUnlockHighlight())
			.wikiUrl(activity.getSafeWikiUrl())
			.targetCombat(activity.getRecommendedCombat())
			.targetSkills(targetSkills)
			.prerequisiteQuests(activity.getQuestRequirements())
			.build();
	}

	public NorthStarGoal createGoalFromGear(GearUpgrade gear)
	{
		Map<Skill, Integer> targetSkills = new HashMap<>();
		for (SkillRequirement sr : gear.getSkillReqs())
		{
			targetSkills.put(sr.getSkill(), sr.getLevel());
		}
		for (SkillRequirement sr : gear.getOrganicSkillReqs())
		{
			Integer existing = targetSkills.get(sr.getSkill());
			if (existing == null || sr.getLevel() > existing)
			{
				targetSkills.put(sr.getSkill(), sr.getLevel());
			}
		}

		List<String> quests = gear.getQuestReq() != null && !gear.getQuestReq().isEmpty()
			? Collections.singletonList(gear.getQuestReq())
			: Collections.emptyList();

		return NorthStarGoal.builder()
			.id("gear-" + gear.getId())
			.title(gear.getName())
			.type(NorthStarGoal.GoalType.GEAR_ITEM)
			.category("Gear Upgrade (" + gear.getSlot() + ")")
			.description(gear.getHighlight())
			.highlight("Slot: " + gear.getSlot() + " | " + gear.getTier() + " Tier")
			.wikiUrl(gear.getWikiUrl())
			.targetCombat(gear.getRecommendedCombat())
			.targetSkills(targetSkills)
			.prerequisiteQuests(quests)
			.requiredItemIds(Collections.singletonList(gear.getItemId()))
			.requiredItemNames(Collections.singletonList(gear.getName()))
			.build();
	}

	public NorthStarGoal createGoalFromCombatCheckpoint(CombatCheckpoint cp)
	{
		return NorthStarGoal.builder()
			.id(cp.getId())
			.title(cp.getName())
			.type(NorthStarGoal.GoalType.COMBAT_CHECKPOINT)
			.category(cp.getStyle() + " Checkpoint (" + cp.getTier() + ")")
			.description(cp.getDescription())
			.highlight(cp.getHighlight())
			.targetCombat(cp.getRecommendedCombat())
			.targetSkills(cp.getRecommendedSkills())
			.requiredItemIds(cp.getCoreItemIds())
			.requiredItemNames(cp.getCoreItemNames())
			.build();
	}

	public NorthStarGoal createGoalFromBossCheckpoint(BossCheckpoint bp)
	{
		return NorthStarGoal.builder()
			.id(bp.getId())
			.title(bp.getBossName())
			.type(NorthStarGoal.GoalType.BOSS_CHECKPOINT)
			.category("Boss Readiness (" + bp.getTier() + ")")
			.description(bp.getDescription())
			.highlight(bp.getUnlockHighlight())
			.wikiUrl(bp.getWikiUrl())
			.targetCombat(bp.getRecommendedCombat())
			.targetSkills(bp.getRecommendedSkills())
			.prerequisiteQuests(bp.getPrerequisiteQuests())
			.requiredItemIds(bp.getRecommendedItemIds())
			.requiredItemNames(bp.getRecommendedItemNames())
			.build();
	}

	private void loadActivities()
	{
		try (InputStream is = getClass().getResourceAsStream("/com/pathfinder/activities.json"))
		{
			if (is == null)
			{
				log.warn("activities.json not found in resources");
				return;
			}

			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
			{
				JsonArray array = gson.fromJson(reader, JsonArray.class);
				for (JsonElement el : array)
				{
					JsonObject obj = el.getAsJsonObject();
					Activity.ActivityBuilder builder = Activity.builder()
						.id(obj.get("id").getAsString())
						.title(obj.get("title").getAsString())
						.wikiUrl(obj.has("wikiUrl") ? obj.get("wikiUrl").getAsString() : "")
						.category(ActivityCategory.valueOf(obj.get("category").getAsString()))
						.description(obj.get("description").getAsString())
						.unlockHighlight(obj.get("unlockHighlight").getAsString())
						.recommendedCombat(obj.get("recommendedCombat").getAsInt())
						.ironmanFriendly(obj.get("ironmanFriendly").getAsBoolean())
						.hiddenGem(obj.get("hiddenGem").getAsBoolean());

					List<SkillRequirement> skillReqs = new ArrayList<>();
					if (obj.has("skillRequirements"))
					{
						for (JsonElement reqEl : obj.getAsJsonArray("skillRequirements"))
						{
							JsonObject reqObj = reqEl.getAsJsonObject();
							Skill skill = Skill.valueOf(reqObj.get("skill").getAsString());
							int level = reqObj.get("level").getAsInt();
							skillReqs.add(new SkillRequirement(skill, level));
						}
					}
					builder.skillRequirements(skillReqs);

					List<String> questReqs = new ArrayList<>();
					if (obj.has("questRequirements"))
					{
						for (JsonElement qEl : obj.getAsJsonArray("questRequirements"))
						{
							questReqs.add(qEl.getAsString());
						}
					}
					builder.questRequirements(questReqs);

					List<String> tags = new ArrayList<>();
					if (obj.has("tags"))
					{
						for (JsonElement tEl : obj.getAsJsonArray("tags"))
						{
							tags.add(tEl.getAsString());
						}
					}
					builder.tags(tags);

					// Infer or parse trained skills
					List<Skill> trainedSkills = new ArrayList<>();
					if (obj.has("trainedSkills"))
					{
						for (JsonElement tsEl : obj.getAsJsonArray("trainedSkills"))
						{
							try
							{
								trainedSkills.add(Skill.valueOf(tsEl.getAsString().toUpperCase()));
							}
							catch (Exception ignored) {}
						}
					}
					else
					{
						// Automatically map from tags and skill requirements
						for (String tag : tags)
						{
							for (Skill s : Skill.values())
							{
								if (tag.equalsIgnoreCase(s.getName()) && !trainedSkills.contains(s))
								{
									trainedSkills.add(s);
								}
							}
						}
						for (SkillRequirement sr : skillReqs)
						{
							if (!trainedSkills.contains(sr.getSkill()))
							{
								trainedSkills.add(sr.getSkill());
							}
						}
					}
					builder.trainedSkills(trainedSkills);

					activities.add(builder.build());
				}
				log.info("Loaded {} activities from activities.json", activities.size());
			}
		}
		catch (Exception e)
		{
			log.error("Failed to load activities.json", e);
		}
	}

	private void loadGearUpgrades()
	{
		try (InputStream is = getClass().getResourceAsStream("/com/pathfinder/gear_upgrades.json"))
		{
			if (is == null)
			{
				log.warn("gear_upgrades.json not found in resources");
				return;
			}

			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
			{
				JsonArray array = gson.fromJson(reader, JsonArray.class);
				for (JsonElement el : array)
				{
					JsonObject obj = el.getAsJsonObject();
					GearUpgrade.GearUpgradeBuilder builder = GearUpgrade.builder()
						.id(obj.get("id").getAsString())
						.name(obj.get("name").getAsString())
						.slot(obj.get("slot").getAsString())
						.style(obj.has("style") ? obj.get("style").getAsString() : "ALL")
						.tier(obj.has("tier") ? obj.get("tier").getAsString() : "MID")
						.itemId(obj.get("item_id").getAsInt())
						.recommendedCombat(obj.has("recommended_combat") ? obj.get("recommended_combat").getAsInt() : 1)
						.estimatedPrice(obj.has("estimated_price") ? obj.get("estimated_price").getAsLong() : 0)
						.ironmanSource(obj.has("ironman_source") ? obj.get("ironman_source").getAsString() : "")
						.wikiUrl(obj.has("wiki_url") ? obj.get("wiki_url").getAsString() : "")
						.highlight(obj.has("highlight") ? obj.get("highlight").getAsString() : "")
						.questReq(obj.has("quest_req") ? obj.get("quest_req").getAsString() : "");

					List<SkillRequirement> skillReqs = new ArrayList<>();
					if (obj.has("skill_reqs"))
					{
						for (JsonElement reqEl : obj.getAsJsonArray("skill_reqs"))
						{
							JsonObject reqObj = reqEl.getAsJsonObject();
							Skill skill = Skill.valueOf(reqObj.get("skill").getAsString());
							int level = reqObj.get("level").getAsInt();
							skillReqs.add(new SkillRequirement(skill, level));
						}
					}
					builder.skillReqs(skillReqs);

					List<SkillRequirement> organicReqs = new ArrayList<>();
					if (obj.has("organic_skill_reqs"))
					{
						for (JsonElement reqEl : obj.getAsJsonArray("organic_skill_reqs"))
						{
							JsonObject reqObj = reqEl.getAsJsonObject();
							Skill skill = Skill.valueOf(reqObj.get("skill").getAsString());
							int level = reqObj.get("level").getAsInt();
							organicReqs.add(new SkillRequirement(skill, level));
						}
					}
					builder.organicSkillReqs(organicReqs);

					gearUpgrades.add(builder.build());
				}
				log.info("Loaded {} gear milestones from gear_upgrades.json", gearUpgrades.size());
			}
		}
		catch (Exception e)
		{
			log.error("Failed to load gear_upgrades.json", e);
		}
	}

	private void loadCombatCheckpoints()
	{
		try (InputStream is = getClass().getResourceAsStream("/com/pathfinder/combat_checkpoints.json"))
		{
			if (is == null)
			{
				return;
			}

			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
			{
				JsonArray array = gson.fromJson(reader, JsonArray.class);
				for (JsonElement el : array)
				{
					JsonObject obj = el.getAsJsonObject();
					CombatCheckpoint.CombatCheckpointBuilder builder = CombatCheckpoint.builder()
						.id(obj.get("id").getAsString())
						.name(obj.get("name").getAsString())
						.style(obj.get("style").getAsString())
						.tier(obj.get("tier").getAsString())
						.description(obj.get("description").getAsString())
						.highlight(obj.get("highlight").getAsString())
						.recommendedCombat(obj.has("recommended_combat") ? obj.get("recommended_combat").getAsInt() : 1);

					Map<Skill, Integer> skills = new HashMap<>();
					if (obj.has("recommended_skills"))
					{
						JsonObject sObj = obj.getAsJsonObject("recommended_skills");
						for (String k : sObj.keySet())
						{
							try
							{
								skills.put(Skill.valueOf(k), sObj.get(k).getAsInt());
							}
							catch (Exception ignored) {}
						}
					}
					builder.recommendedSkills(skills);

					List<Integer> itemIds = new ArrayList<>();
					if (obj.has("core_item_ids"))
					{
						for (JsonElement idEl : obj.getAsJsonArray("core_item_ids"))
						{
							itemIds.add(idEl.getAsInt());
						}
					}
					builder.coreItemIds(itemIds);

					List<String> itemNames = new ArrayList<>();
					if (obj.has("core_item_names"))
					{
						for (JsonElement nameEl : obj.getAsJsonArray("core_item_names"))
						{
							itemNames.add(nameEl.getAsString());
						}
					}
					builder.coreItemNames(itemNames);

					combatCheckpoints.add(builder.build());
				}
				log.info("Loaded {} combat checkpoints", combatCheckpoints.size());
			}
		}
		catch (Exception e)
		{
			log.error("Failed to load combat_checkpoints.json", e);
		}
	}

	private void loadBossCheckpoints()
	{
		try (InputStream is = getClass().getResourceAsStream("/com/pathfinder/boss_checkpoints.json"))
		{
			if (is == null)
			{
				return;
			}

			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8))
			{
				JsonArray array = gson.fromJson(reader, JsonArray.class);
				for (JsonElement el : array)
				{
					JsonObject obj = el.getAsJsonObject();
					BossCheckpoint.BossCheckpointBuilder builder = BossCheckpoint.builder()
						.id(obj.get("id").getAsString())
						.bossName(obj.get("boss_name").getAsString())
						.tier(obj.get("tier").getAsString())
						.description(obj.get("description").getAsString())
						.unlockHighlight(obj.get("unlock_highlight").getAsString())
						.wikiUrl(obj.has("wiki_url") ? obj.get("wiki_url").getAsString() : "")
						.recommendedCombat(obj.has("recommended_combat") ? obj.get("recommended_combat").getAsInt() : 1);

					Map<Skill, Integer> skills = new HashMap<>();
					if (obj.has("recommended_skills"))
					{
						JsonObject sObj = obj.getAsJsonObject("recommended_skills");
						for (String k : sObj.keySet())
						{
							try
							{
								skills.put(Skill.valueOf(k), sObj.get(k).getAsInt());
							}
							catch (Exception ignored) {}
						}
					}
					builder.recommendedSkills(skills);

					List<String> quests = new ArrayList<>();
					if (obj.has("prerequisite_quests"))
					{
						for (JsonElement qEl : obj.getAsJsonArray("prerequisite_quests"))
						{
							quests.add(qEl.getAsString());
						}
					}
					builder.prerequisiteQuests(quests);

					List<Integer> itemIds = new ArrayList<>();
					if (obj.has("recommended_item_ids"))
					{
						for (JsonElement idEl : obj.getAsJsonArray("recommended_item_ids"))
						{
							itemIds.add(idEl.getAsInt());
						}
					}
					builder.recommendedItemIds(itemIds);

					List<String> itemNames = new ArrayList<>();
					if (obj.has("recommended_item_names"))
					{
						for (JsonElement nameEl : obj.getAsJsonArray("recommended_item_names"))
						{
							itemNames.add(nameEl.getAsString());
						}
					}
					builder.recommendedItemNames(itemNames);

					bossCheckpoints.add(builder.build());
				}
				log.info("Loaded {} boss checkpoints", bossCheckpoints.size());
			}
		}
		catch (Exception e)
		{
			log.error("Failed to load boss_checkpoints.json", e);
		}
	}

	private void initFallbackActivities()
	{
		activities.add(Activity.builder()
			.id("mahogany-homes")
			.title("Mahogany Homes")
			.category(ActivityCategory.SKILLING)
			.description("Contract-based home renovations. Far more relaxed and GP-efficient than standard construction.")
			.unlockHighlight("Carpenter Outfit (+2.5% XP) & Plank Sack")
			.recommendedCombat(1)
			.ironmanFriendly(true)
			.hiddenGem(true)
			.skillRequirements(List.of(new SkillRequirement(Skill.CONSTRUCTION, 20)))
			.tags(Arrays.asList("Construction", "Contract", "Chill"))
			.trainedSkills(Collections.singletonList(Skill.CONSTRUCTION))
			.build());
	}
}
