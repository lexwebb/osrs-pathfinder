package com.pathfinder;

import com.pathfinder.engine.ActivityDatabase;
import com.pathfinder.engine.RecommendationEngine;
import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.BankSnapshot;
import com.pathfinder.model.Recommendation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RecommendationEngineTest
{
	private RecommendationEngine engine;
	private ActivityDatabase database;

	@Before
	public void setUp()
	{
		database = new ActivityDatabase();
		engine = new RecommendationEngine(database);
	}

	@Test
	public void testDatabaseLoadsFromBundledJson()
	{
		Assert.assertTrue("Should load activities from activities.json", database.getAllActivities().size() >= 15);
		Assert.assertTrue("Should load gear from gear_upgrades.json", database.getGearUpgrades().size() >= 5);
	}

	@Test
	public void testBronzemanRequires85SlayerForWhip()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}
		// Attack 75, but Slayer only 65 (needs 85 for Whip drop)
		skills.put(Skill.ATTACK, 75);
		skills.put(Skill.SLAYER, 65);

		AccountProfile bronzemanProfile = AccountProfile.builder()
			.combatLevel(104)
			.totalLevel(1500)
			.isIronman(false)
			.isBronzeman(true)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.unlockedItemIds(new HashSet<>())
			.build();

		List<Recommendation> recs = engine.evaluateAll(bronzemanProfile);

		Recommendation whipRec = recs.stream()
			.filter(r -> "gear-abyssal-whip".equals(r.getActivity().getId()))
			.findFirst()
			.orElse(null);

		Assert.assertNotNull(whipRec);
		// Must be LOCKED because Slayer is 65 vs 85 (20 levels away)
		Assert.assertEquals(Recommendation.Status.LOCKED, whipRec.getStatus());
		Assert.assertTrue(whipRec.getMissingRequirements().stream()
			.anyMatch(req -> req.contains("Slayer")));
	}

	@Test
	public void testBronzemanRequiresCraftingAndMagicForFury()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 60);
		}

		AccountProfile bronzemanProfile = AccountProfile.builder()
			.combatLevel(104)
			.totalLevel(1500)
			.isIronman(false)
			.isBronzeman(true)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.unlockedItemIds(new HashSet<>())
			.build();

		List<Recommendation> recs = engine.evaluateAll(bronzemanProfile);

		Recommendation furyRec = recs.stream()
			.filter(r -> "gear-amulet-of-fury".equals(r.getActivity().getId()))
			.findFirst()
			.orElse(null);

		Assert.assertNotNull(furyRec);
		// Must be LOCKED because Crafting is 60 vs 90 and Magic is 60 vs 87
		Assert.assertEquals(Recommendation.Status.LOCKED, furyRec.getStatus());
		Assert.assertTrue(furyRec.getMissingRequirements().stream()
			.anyMatch(req -> req.contains("Crafting") || req.contains("Magic")));
	}

	@Test
	public void testHideLockedFilter()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 30);
		}

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(40)
			.totalLevel(600)
			.isIronman(false)
			.isBronzeman(true)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.build();

		List<Recommendation> all = engine.evaluateAll(profile);
		List<Recommendation> unlockedOnly = engine.filter(all, ActivityCategory.ALL, false, true, false);

		for (Recommendation r : unlockedOnly)
		{
			Assert.assertNotEquals("Should not contain LOCKED items when hideLocked is true",
				Recommendation.Status.LOCKED, r.getStatus());
		}
	}
}
