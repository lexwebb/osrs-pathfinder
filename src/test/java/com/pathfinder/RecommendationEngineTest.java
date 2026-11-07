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
	public void testBronzemanModeForcesOrganicDropObjective()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 75);
		}

		// Whip item ID is 4151
		// Player has 10M cash, but has NEVER unlocked Abyssal Whip in Bronzeman mode
		BankSnapshot bank = BankSnapshot.builder()
			.coins(10_000_000L)
			.totalLiquidCash(10_000_000L)
			.estimatedTotalValue(50_000_000L)
			.timestamp(System.currentTimeMillis())
			.ownedItemIds(new HashSet<>())
			.build();

		AccountProfile bronzemanProfile = AccountProfile.builder()
			.combatLevel(100)
			.totalLevel(1500)
			.isIronman(false)
			.isBronzeman(true)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.unlockedItemIds(new HashSet<>())
			.bankSnapshot(bank)
			.build();

		List<Recommendation> recs = engine.evaluateAll(bronzemanProfile);

		Recommendation whipRec = recs.stream()
			.filter(r -> "gear-abyssal-whip".equals(r.getActivity().getId()))
			.findFirst()
			.orElse(null);

		Assert.assertNotNull(whipRec);
		// Should have Bronzeman Unlock tag
		Assert.assertTrue("Should contain Bronzeman Unlock tag",
			whipRec.getActivity().getTags().contains("Bronzeman Unlock"));
		// Should mention the organic drop source rather than just buying on GE
		Assert.assertTrue(whipRec.getMissingRequirements().stream()
			.anyMatch(req -> req.contains("Abyssal Demons")));
	}

	@Test
	public void testBronzemanModeAllowsGEPurchaseOnceUnlocked()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 75);
		}

		// Player has unlocked Whip (4151) previously, but doesn't currently own it in bank
		Set<Integer> unlocked = new HashSet<>();
		unlocked.add(4151);

		BankSnapshot bank = BankSnapshot.builder()
			.coins(5_000_000L)
			.totalLiquidCash(5_000_000L)
			.estimatedTotalValue(20_000_000L)
			.timestamp(System.currentTimeMillis())
			.ownedItemIds(new HashSet<>())
			.build();

		AccountProfile bronzemanProfile = AccountProfile.builder()
			.combatLevel(100)
			.totalLevel(1500)
			.isIronman(false)
			.isBronzeman(true)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.unlockedItemIds(unlocked)
			.bankSnapshot(bank)
			.build();

		List<Recommendation> recs = engine.evaluateAll(bronzemanProfile);

		Recommendation whipRec = recs.stream()
			.filter(r -> "gear-abyssal-whip".equals(r.getActivity().getId()))
			.findFirst()
			.orElse(null);

		Assert.assertNotNull(whipRec);
		// Once unlocked, it is READY to buy on GE
		Assert.assertEquals(Recommendation.Status.READY, whipRec.getStatus());
	}
}
