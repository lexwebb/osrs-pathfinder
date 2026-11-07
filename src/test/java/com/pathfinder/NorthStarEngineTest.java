package com.pathfinder;

import com.pathfinder.engine.ActivityDatabase;
import com.pathfinder.engine.RecommendationEngine;
import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.Activity;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.BankSnapshot;
import com.pathfinder.model.BossCheckpoint;
import com.pathfinder.model.CombatCheckpoint;
import com.pathfinder.model.NorthStarGoal;
import com.pathfinder.model.NorthStarProgress;
import com.pathfinder.model.Recommendation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NorthStarEngineTest
{
	private ActivityDatabase database;
	private RecommendationEngine engine;

	@Before
	public void setUp()
	{
		database = new ActivityDatabase();
		engine = new RecommendationEngine(database);
	}

	@Test
	public void testCheckpointsLoaded()
	{
		Assert.assertTrue("Should load combat checkpoints", database.getCombatCheckpoints().size() >= 6);
		Assert.assertTrue("Should load boss checkpoints", database.getBossCheckpoints().size() >= 4);
	}

	@Test
	public void testSetAnyIncompleteQuestAsNorthStar()
	{
		// Mock account with 52 Construction, 61 Mining, 70 Hunter, but missing Underground Pass
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}
		skills.put(Skill.CONSTRUCTION, 52);
		skills.put(Skill.MINING, 61);

		Set<String> quests = new HashSet<>();
		quests.add("Plague City");
		quests.add("Biohazard");
		// Missing Underground Pass & Mourning's End

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(100)
			.totalLevel(1500)
			.skills(skills)
			.completedQuests(quests)
			.build();

		// Song of the Elves as North Star
		Map<Skill, Integer> soteSkills = new HashMap<>();
		soteSkills.put(Skill.CONSTRUCTION, 70);
		soteSkills.put(Skill.MINING, 70);
		soteSkills.put(Skill.HERBLORE, 70);
		soteSkills.put(Skill.HUNTER, 70);

		NorthStarGoal soteGoal = NorthStarGoal.builder()
			.id("quest-sote")
			.title("Song of the Elves")
			.type(NorthStarGoal.GoalType.ACTIVITY)
			.category("Grandmaster Quest")
			.targetSkills(soteSkills)
			.prerequisiteQuests(List.of("Underground Pass", "Mourning's End Part I"))
			.build();

		NorthStarProgress progress = engine.evaluateNorthStar(profile, soteGoal);
		Assert.assertNotNull(progress);
		Assert.assertFalse("Should not be fully ready yet", progress.isFullyReady());

		// Total requirements: 4 skills + 2 quests = 6
		// Satisfied: Herblore (70), Hunter (70) = 2
		Assert.assertEquals(6, progress.getTotalRequirements());
		Assert.assertEquals(2, progress.getSatisfiedRequirements());
		Assert.assertEquals(33, progress.getProgressPercent());

		// Verify skill gaps: Construction 52/70 (18 away), Mining 61/70 (9 away)
		Assert.assertTrue(progress.hasSkillGap(Skill.CONSTRUCTION));
		Assert.assertTrue(progress.hasSkillGap(Skill.MINING));
		Assert.assertFalse(progress.hasSkillGap(Skill.HUNTER));

		NorthStarProgress.SkillGap conGap = progress.getSkillGap(Skill.CONSTRUCTION);
		Assert.assertEquals(18, conGap.getLevelsRemaining());

		// Verify quest gaps
		Assert.assertTrue(progress.hasQuestGap("Underground Pass"));
		Assert.assertTrue(progress.hasQuestGap("Mourning's End Part I"));
	}

	@Test
	public void testSetCombatGearCheckpointAsNorthStar()
	{
		CombatCheckpoint midMelee = database.getCombatCheckpoint("melee-mid");
		Assert.assertNotNull(midMelee);

		NorthStarGoal goal = database.createGoalFromCombatCheckpoint(midMelee);
		Assert.assertNotNull(goal);

		// Account owns Zombie Axe (28810) and Neitiznot (10828), but missing Torso (10551) and Defender (12954)
		Set<Integer> owned = new HashSet<>();
		owned.add(28810);
		owned.add(10828);

		BankSnapshot snapshot = BankSnapshot.builder()
			.timestamp(System.currentTimeMillis())
			.ownedItemIds(owned)
			.build();

		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 75);
		}

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(95)
			.totalLevel(1600)
			.skills(skills)
			.bankSnapshot(snapshot)
			.build();

		NorthStarProgress progress = engine.evaluateNorthStar(profile, goal);
		Assert.assertNotNull(progress);

		// Owned items should not be in missing items
		Assert.assertFalse("Should NOT be missing Zombie Axe", progress.hasItemGap(28810));
		Assert.assertFalse("Should NOT be missing Helm of Neitiznot", progress.hasItemGap(10828));

		// Missing items should include Fighter Torso and Dragon Defender
		Assert.assertTrue("Should be missing Fighter Torso", progress.hasItemGap(10551));
		Assert.assertTrue("Should be missing Dragon Defender", progress.hasItemGap(12954));
	}

	@Test
	public void testSetBossReadyCheckpointAsNorthStar()
	{
		BossCheckpoint zulrah = database.getBossCheckpoint("boss-zulrah");
		Assert.assertNotNull(zulrah);

		NorthStarGoal goal = database.createGoalFromBossCheckpoint(zulrah);
		Assert.assertNotNull(goal);

		// Player with 75 Magic (needs 80), 75 Ranged (meets 75), Regicide not done
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}
		skills.put(Skill.MAGIC, 75);
		skills.put(Skill.RANGED, 75);
		skills.put(Skill.DEFENCE, 70);
		skills.put(Skill.PRAYER, 50);

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(88)
			.totalLevel(1500)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.build();

		NorthStarProgress progress = engine.evaluateNorthStar(profile, goal);
		Assert.assertNotNull(progress);

		Assert.assertTrue("Should have Magic gap for Zulrah", progress.hasSkillGap(Skill.MAGIC));
		Assert.assertEquals(5, progress.getSkillGap(Skill.MAGIC).getLevelsRemaining());
		Assert.assertFalse("Should NOT have Ranged gap", progress.hasSkillGap(Skill.RANGED));
		Assert.assertTrue("Should be missing Regicide", progress.hasQuestGap("Regicide"));
	}

	@Test
	public void testPathwayActivitiesSurfacedAndPrioritized()
	{
		// Construction is 40, Song of the Elves needs 70
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}
		skills.put(Skill.CONSTRUCTION, 40);

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(95)
			.totalLevel(1500)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.build();

		NorthStarGoal soteGoal = NorthStarGoal.builder()
			.id("quest-sote")
			.title("Song of the Elves")
			.targetSkills(Map.of(Skill.CONSTRUCTION, 70))
			.prerequisiteQuests(Collections.emptyList())
			.build();

		List<Recommendation> recs = engine.evaluateAll(profile, soteGoal);

		// Mahogany Homes trains Construction -> must be marked as a pathway!
		Recommendation mhRec = recs.stream()
			.filter(r -> "mahogany-homes".equals(r.getActivity().getId()))
			.findFirst()
			.orElse(null);

		Assert.assertNotNull("Mahogany Homes should be present", mhRec);
		Assert.assertTrue("Mahogany Homes must be tagged as a pathway", mhRec.isNorthStarPathway());
		Assert.assertTrue("Pathway reason should mention Song of the Elves and Construction",
			mhRec.getPathwayReason().contains("Song of the Elves") && mhRec.getPathwayReason().contains("Construction"));

		// Pathways should be sorted to the very top in Smart Score
		Assert.assertTrue("Top recommendation should be a pathway", recs.get(0).isNorthStarPathway());
	}

	@Test
	public void testPathwaysOnlyFilter()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}
		skills.put(Skill.CONSTRUCTION, 40);

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(95)
			.totalLevel(1500)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.build();

		NorthStarGoal soteGoal = NorthStarGoal.builder()
			.id("quest-sote")
			.title("Song of the Elves")
			.targetSkills(Map.of(Skill.CONSTRUCTION, 70))
			.prerequisiteQuests(Collections.emptyList())
			.build();

		List<Recommendation> all = engine.evaluateAll(profile, soteGoal);
		List<Recommendation> pathwaysOnly = engine.filter(all, ActivityCategory.ALL, false, false, false, true);

		Assert.assertFalse("Pathways list should not be empty", pathwaysOnly.isEmpty());
		for (Recommendation r : pathwaysOnly)
		{
			Assert.assertTrue("Every filtered item must be an active pathway", r.isNorthStarPathway());
		}
	}

	@Test
	public void testBronzemanOrganicGearRequirements()
	{
		CombatCheckpoint lateMelee = database.getCombatCheckpoint("melee-late");
		Assert.assertNotNull(lateMelee);

		NorthStarGoal goal = database.createGoalFromCombatCheckpoint(lateMelee);
		Assert.assertNotNull(goal);

		// Account is Bronzeman, stats 70 all, missing Amulet of Fury (6585)
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}

		AccountProfile bronzeman = AccountProfile.builder()
			.isBronzeman(true)
			.skills(skills)
			.bankSnapshot(BankSnapshot.builder().build())
			.build();

		NorthStarProgress progress = engine.evaluateNorthStar(bronzeman, goal);
		Assert.assertNotNull(progress);

		// In Bronzeman mode, Fury's organic requirements (90 Crafting, 87 Magic) must be enforced!
		Assert.assertTrue("Bronzeman should have Crafting gap for unowned Fury", progress.hasSkillGap(Skill.CRAFTING));
		Assert.assertEquals("Target Crafting should be 90 for Fury", 90, progress.getSkillGap(Skill.CRAFTING).getTargetLevel());

		Assert.assertTrue("Bronzeman should have Magic gap for unowned Fury", progress.hasSkillGap(Skill.MAGIC));
		Assert.assertEquals("Target Magic should be 87 for Fury", 87, progress.getSkillGap(Skill.MAGIC).getTargetLevel());

		// Now test if Bronzeman has already unlocked Fury organically
		Set<Integer> unlocked = new HashSet<>();
		unlocked.add(6585);
		AccountProfile unlockedBronzeman = AccountProfile.builder()
			.isBronzeman(true)
			.skills(skills)
			.unlockedItemIds(unlocked)
			.bankSnapshot(BankSnapshot.builder().build())
			.build();

		NorthStarProgress unlockedProgress = engine.evaluateNorthStar(unlockedBronzeman, goal);
		Assert.assertFalse("Unlocked Bronzeman should NOT be forced to 90 Crafting for Fury",
			unlockedProgress.hasSkillGap(Skill.CRAFTING));
	}

	@Test
	public void testSlotUpgradeSatisfaction()
	{
		// Mid melee requires Amulet of Glory (1704) and Zombie Axe (28810)
		CombatCheckpoint midMelee = database.getCombatCheckpoint("melee-mid");
		Assert.assertNotNull(midMelee);

		NorthStarGoal goal = database.createGoalFromCombatCheckpoint(midMelee);

		// Player owns higher tier: Amulet of Fury (6585) and Abyssal Whip (4151)
		Set<Integer> owned = new HashSet<>();
		owned.add(6585);
		owned.add(4151);

		AccountProfile profile = AccountProfile.builder()
			.skills(Collections.emptyMap())
			.bankSnapshot(BankSnapshot.builder()
				.timestamp(System.currentTimeMillis())
				.ownedItemIds(owned)
				.build())
			.build();

		NorthStarProgress progress = engine.evaluateNorthStar(profile, goal);
		Assert.assertFalse("Owning Fury should satisfy Glory neck requirement", progress.hasItemGap(1704));
		Assert.assertFalse("Owning Whip should satisfy Zombie Axe weapon requirement", progress.hasItemGap(28810));
	}

	@Test
	public void testSlayerPathwayGeneration()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}
		skills.put(Skill.SLAYER, 65); // Slayer 65

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(100)
			.skills(skills)
			.completedQuests(Set.of("Lost City", "Shilo Village"))
			.build();

		CombatCheckpoint midMelee = database.getCombatCheckpoint("melee-mid"); // requires Slayer 70
		NorthStarGoal goal = database.createGoalFromCombatCheckpoint(midMelee);

		List<Recommendation> all = engine.evaluateAll(profile, goal);
		List<Recommendation> pathwaysOnly = engine.filter(all, ActivityCategory.ALL, false, false, false, true);

		boolean foundSlayerMaster = false;
		for (Recommendation r : pathwaysOnly)
		{
			if (r.getActivity().getTrainedSkills().contains(Skill.SLAYER))
			{
				foundSlayerMaster = true;
				Assert.assertTrue("Slayer activity must be tagged as North Star pathway", r.isNorthStarPathway());
				Assert.assertTrue("Pathway reason should mention Slayer", r.getPathwayReason().contains("Slayer"));
			}
		}

		Assert.assertTrue("Pathways must include Slayer training activities when Slayer gap exists", foundSlayerMaster);
	}
}
