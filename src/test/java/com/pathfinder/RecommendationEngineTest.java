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
	public void testNewAccountHasValidRecommendations()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 1);
		}
		skills.put(Skill.HITPOINTS, 10);

		AccountProfile freshAccount = AccountProfile.builder()
			.combatLevel(3)
			.totalLevel(32)
			.isIronman(false)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.build();

		List<Recommendation> recs = engine.evaluateAll(freshAccount);
		Assert.assertNotNull(recs);
		Assert.assertFalse(recs.isEmpty());
	}

	@Test
	public void testAffordableGearUpgradeRecommended()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 1);
		}
		// 70 Attack for Abyssal Whip
		skills.put(Skill.ATTACK, 70);

		// Bank snapshot with 2.5M GP liquid cash (Whip costs ~1.8M)
		BankSnapshot bank = BankSnapshot.builder()
			.coins(2_500_000L)
			.totalLiquidCash(2_500_000L)
			.estimatedTotalValue(10_000_000L)
			.timestamp(System.currentTimeMillis())
			.ownedItemIds(new HashSet<>())
			.build();

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(80)
			.totalLevel(800)
			.isIronman(false)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.bankSnapshot(bank)
			.build();

		List<Recommendation> recs = engine.evaluateAll(profile);

		Recommendation whip = recs.stream()
			.filter(r -> "gear-abyssal-whip".equals(r.getActivity().getId()))
			.findFirst()
			.orElse(null);

		Assert.assertNotNull(whip);
		Assert.assertEquals(Recommendation.Status.READY, whip.getStatus());
		Assert.assertEquals(ActivityCategory.GEAR, whip.getActivity().getCategory());
		Assert.assertTrue(whip.getActivity().getSafeWikiUrl().contains("runescape.wiki"));
	}

	@Test
	public void testAlreadyOwnedGearNotRecommended()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}

		// Whip item ID is 4151
		Set<Integer> owned = new HashSet<>();
		owned.add(4151);

		BankSnapshot bank = BankSnapshot.builder()
			.coins(5_000_000L)
			.totalLiquidCash(5_000_000L)
			.estimatedTotalValue(20_000_000L)
			.timestamp(System.currentTimeMillis())
			.ownedItemIds(owned)
			.build();

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(85)
			.totalLevel(1200)
			.isIronman(false)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.bankSnapshot(bank)
			.build();

		List<Recommendation> recs = engine.evaluateAll(profile);

		boolean hasWhip = recs.stream().anyMatch(r -> "gear-abyssal-whip".equals(r.getActivity().getId()));
		Assert.assertFalse("Should not recommend whip if player already owns it in bank", hasWhip);
	}

	@Test
	public void testSortingOrders()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 40);
		}

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(50)
			.totalLevel(900)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.build();

		List<Recommendation> recs = engine.evaluateAll(profile);

		// Alphabetical
		engine.sort(recs, RecommendationEngine.SortOrder.ALPHABETICAL);
		for (int i = 0; i < recs.size() - 1; i++)
		{
			String a = recs.get(i).getActivity().getTitle();
			String b = recs.get(i + 1).getActivity().getTitle();
			Assert.assertTrue(a.compareToIgnoreCase(b) <= 0);
		}

		// Closest unlock
		engine.sort(recs, RecommendationEngine.SortOrder.CLOSEST_UNLOCK);
		for (int i = 0; i < recs.size() - 1; i++)
		{
			Assert.assertTrue(recs.get(i).getMissingLevelSum() <= recs.get(i + 1).getMissingLevelSum());
		}
	}
}
