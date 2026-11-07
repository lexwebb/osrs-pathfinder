package com.pathfinder.engine;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pathfinder.model.Activity;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.GearUpgrade;
import com.pathfinder.model.SkillRequirement;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;

@Slf4j
@Singleton
public class ActivityDatabase
{
	private final List<Activity> activities = new ArrayList<>();
	private final List<GearUpgrade> gearUpgrades = new ArrayList<>();
	private final Gson gson = new Gson();

	public ActivityDatabase()
	{
		loadActivities();
		loadGearUpgrades();

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
			.build());
	}
}
