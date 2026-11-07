package com.pathfinder.engine;

import com.pathfinder.model.Activity;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.SkillRequirement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

@Singleton
public class ActivityDatabase
{
	private final List<Activity> activities = new ArrayList<>();

	public ActivityDatabase()
	{
		initDefaultActivities();
	}

	public List<Activity> getAllActivities()
	{
		return Collections.unmodifiableList(activities);
	}

	private void initDefaultActivities()
	{
		// --- ENGAGING SKILLING & MINIGAMES ---
		activities.add(Activity.builder()
			.id("mahogany-homes")
			.title("Mahogany Homes")
			.category(ActivityCategory.SKILLING)
			.description("Contract-based home renovations. Far more relaxed and GP-efficient than standard spam construction.")
			.unlockHighlight("Carpenter Outfit (+2.5% XP) & Plank Sack")
			.recommendedCombat(1)
			.ironmanFriendly(true)
			.isHiddenGem(true)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.CONSTRUCTION, 20)
			))
			.tags(Arrays.asList("Construction", "Contract", "Chill", "Cost-effective"))
			.build());

		activities.add(Activity.builder()
			.id("giants-foundry")
			.title("Giants' Foundry")
			.category(ActivityCategory.SKILLING)
			.description("Forge colossal weapons using temperature and mold mechanics. Recycles old armor and weapons into massive XP.")
			.unlockHighlight("Smithing Outfit & Colossal Blade")
			.recommendedCombat(1)
			.ironmanFriendly(true)
			.isHiddenGem(false)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.SMITHING, 15)
			))
			.questRequirements(List.of("Sleeping Giants"))
			.tags(Arrays.asList("Smithing", "Minigame", "Profit", "Ironman Favorite"))
			.build());

		activities.add(Activity.builder()
			.id("guardians-of-the-rift")
			.title("Guardians of the Rift")
			.category(ActivityCategory.MINIGAME)
			.description("Defend the Great Guardian against the abyssal creatures while crafting runes collaboratively.")
			.unlockHighlight("Raiments of the Eye (up to +60% runes crafted) & Abyssal Needle")
			.recommendedCombat(1)
			.ironmanFriendly(true)
			.isHiddenGem(false)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.RUNECRAFT, 27)
			))
			.questRequirements(List.of("Temple of the Eye"))
			.tags(Arrays.asList("Runecraft", "Co-op", "Outfit", "High Profit"))
			.build());

		activities.add(Activity.builder()
			.id("tempoross")
			.title("Tempoross (Spirit of the Open Sea)")
			.category(ActivityCategory.BOSSING)
			.description("Skilling boss fight against an enraged water spirit. Harpoon fish, load cannons, and evade waves.")
			.unlockHighlight("Fish Barrel (holds 28 raw fish) & Tackle Box")
			.recommendedCombat(1)
			.ironmanFriendly(true)
			.isHiddenGem(false)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.FISHING, 35)
			))
			.tags(Arrays.asList("Fishing", "Boss", "Safe Death", "Food Supplies"))
			.build());

		activities.add(Activity.builder()
			.id("hunter-rumours")
			.title("Hunter Rumours (Varlamore)")
			.category(ActivityCategory.SKILLING)
			.description("Guild-master task system for hunting specific creatures. Variety hunter gameplay with great loot sacks.")
			.unlockHighlight("Huntsman's Kit, Guild Hunter Outfit & Quetzal Whistle")
			.recommendedCombat(1)
			.ironmanFriendly(true)
			.isHiddenGem(true)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.HUNTER, 46)
			))
			.questRequirements(List.of("Children of the Sun"))
			.tags(Arrays.asList("Hunter", "Varlamore", "Variety", "Whistle"))
			.build());

		activities.add(Activity.builder()
			.id("volcanic-mine")
			.title("Volcanic Mine (Fossil Island)")
			.category(ActivityCategory.MINIGAME)
			.description("Fast-paced co-op mining inside an active volcano. Great alternative to dull iron or granite mining.")
			.unlockHighlight("Ash Covered Tome & 80k+ Mining XP/hr")
			.recommendedCombat(50)
			.ironmanFriendly(true)
			.isHiddenGem(true)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.MINING, 50)
			))
			.questRequirements(List.of("Bone Voyage"))
			.tags(Arrays.asList("Mining", "High XP", "Teamwork", "Alternative"))
			.build());

		activities.add(Activity.builder()
			.id("hallowed-sepulchre")
			.title("Hallowed Sepulchre")
			.category(ActivityCategory.SKILLING)
			.description("Intense skill-based agility obstacle courses inside an ancient tomb. Dodging darts, statues, and fire.")
			.unlockHighlight("Ring of Endurance, Dark Dye & Best Agility XP/GP in game")
			.recommendedCombat(60)
			.ironmanFriendly(true)
			.isHiddenGem(false)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.AGILITY, 52),
				new SkillRequirement(Skill.THIEVING, 52)
			))
			.questRequirements(List.of("Sins of the Father"))
			.tags(Arrays.asList("Agility", "High Skill", "Huge Profit", "Action"))
			.build());

		// --- ENTRY & MID-GAME BOSSES ---
		activities.add(Activity.builder()
			.id("scurrius")
			.title("Scurrius, The Rat King")
			.category(ActivityCategory.BOSSING)
			.description("Introductory PvM encounter teaching prayer switching, falling debris dodging, and add management.")
			.unlockHighlight("Bone weapons (massive combat XP boost in lair) & Scurrius' Spine")
			.recommendedCombat(40)
			.ironmanFriendly(true)
			.isHiddenGem(false)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.PRAYER, 43)
			))
			.tags(Arrays.asList("PvM Tutorial", "Prayer Flicking", "Fast XP", "Solo"))
			.build());

		activities.add(Activity.builder()
			.id("barrows")
			.title("Barrows Brothers")
			.category(ActivityCategory.BOSSING)
			.description("Explore the six burial crypts of the fallen Morytania heroes and loot the subterranean chest.")
			.unlockHighlight("Iconic Barrows Armor (Dharok, Ahrim, Karil, Guthan, Torag, Verac)")
			.recommendedCombat(60)
			.ironmanFriendly(true)
			.isHiddenGem(false)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.MAGIC, 50),
				new SkillRequirement(Skill.PRAYER, 43)
			))
			.questRequirements(List.of("Priest in Peril"))
			.tags(Arrays.asList("Classic", "Midgame Armor", "High Value", "Solo"))
			.build());

		activities.add(Activity.builder()
			.id("perilous-moons")
			.title("Perilous Moons (Varlamore)")
			.category(ActivityCategory.BOSSING)
			.description("Trio of lunar bosses: Blood Moon, Blue Moon, and Eclipse Moon. Generous supply rooms provided inside!")
			.unlockHighlight("Blood Moon, Blue Moon, & Eclipse Moon Gear Sets (level 75 armors)")
			.recommendedCombat(75)
			.ironmanFriendly(true)
			.isHiddenGem(false)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.DEFENCE, 70),
				new SkillRequirement(Skill.ATTACK, 65)
			))
			.questRequirements(List.of("Perilous Moons", "Children of the Sun"))
			.tags(Arrays.asList("Varlamore", "Boss Trio", "Free Supplies", "Midgame BiS"))
			.build());

		activities.add(Activity.builder()
			.id("sarachnis")
			.title("Sarachnis (Forthos Dungeon)")
			.category(ActivityCategory.BOSSING)
			.description("Mid-level spider boss that teaches ranged/melee prayer reactions and minion clearing.")
			.unlockHighlight("Sarachnis Cudgel (great mid-tier crush weapon) & Egg Sacs (red spider eggs)")
			.recommendedCombat(70)
			.ironmanFriendly(true)
			.isHiddenGem(true)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.PRAYER, 43),
				new SkillRequirement(Skill.ATTACK, 60),
				new SkillRequirement(Skill.STRENGTH, 60)
			))
			.tags(Arrays.asList("Spider", "Herblore Supplies", "Crush Weapon", "Midgame"))
			.build());

		// --- ESSENTIAL QUESTS & EXPLORATION ---
		activities.add(Activity.builder()
			.id("fairy-rings")
			.title("Unlock Fairy Rings (Fairytale II)")
			.category(ActivityCategory.QUEST)
			.description("Start Fairytale II to gain access to the fairy ring network. You only need to START the quest!")
			.unlockHighlight("Instant teleportation access across almost all corners of Gielinor")
			.recommendedCombat(30)
			.ironmanFriendly(true)
			.isHiddenGem(false)
			.questRequirements(List.of("Lost City", "Nature Spirit", "Fairytale I - Growing Pains"))
			.tags(Arrays.asList("Essential", "Teleportation", "Huge QoL"))
			.build());

		activities.add(Activity.builder()
			.id("bone-voyage")
			.title("Bone Voyage (Fossil Island)")
			.category(ActivityCategory.QUEST)
			.description("Help the museum build a seaworthy barge and navigate the cursed archipelago to discover Fossil Island.")
			.unlockHighlight("Fossil Island, Birdhouse Runs (passive hunter/seeds), Ammonite Crabs & Volcanic Mine")
			.recommendedCombat(25)
			.ironmanFriendly(true)
			.isHiddenGem(false)
			.questRequirements(List.of("The Dig Site"))
			.tags(Arrays.asList("Birdhouses", "Passive Hunter", "Top Skilling Hub"))
			.build());

		activities.add(Activity.builder()
			.id("ardougne-easy-diary")
			.title("Ardougne Easy Diary")
			.category(ActivityCategory.DIARY)
			.description("Complete a simple tour of easy tasks around the kingdom of East and West Ardougne.")
			.unlockHighlight("Ardougne Cloak 1: Unlimited teleports near an altar and fairy ring, +10% pickpocket chance")
			.recommendedCombat(20)
			.ironmanFriendly(true)
			.isHiddenGem(false)
			.skillRequirements(List.of(
				new SkillRequirement(Skill.THIEVING, 15),
				new SkillRequirement(Skill.FISHING, 15)
			))
			.questRequirements(List.of("Biohazard", "Rune Mysteries"))
			.tags(Arrays.asList("Diary", "Unlimited Teleport", "Early Game Game-Changer"))
			.build());
	}
}
