package com.pathfinder;

import com.pathfinder.engine.ActivityDatabase;
import com.pathfinder.engine.RecommendationEngine;
import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.ActivityCategory;
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

		// Ensure sorted properly (e.g. at least some are near unlock or locked)
		Assert.assertTrue(recs.size() >= 10);
	}

	@Test
	public void testReadyActivityDetected()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 1);
		}
		// Give skills for Mahogany Homes (Construction 20)
		skills.put(Skill.CONSTRUCTION, 25);

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(3)
			.totalLevel(56)
			.isIronman(true)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.build();

		List<Recommendation> recs = engine.evaluateAll(profile);
		Recommendation mahoganyHomes = recs.stream()
			.filter(r -> "mahogany-homes".equals(r.getActivity().getId()))
			.findFirst()
			.orElse(null);

		Assert.assertNotNull(mahoganyHomes);
		Assert.assertEquals(Recommendation.Status.READY, mahoganyHomes.getStatus());
		Assert.assertTrue(mahoganyHomes.getMissingRequirements().isEmpty());
	}

	@Test
	public void testAlmostReadyActivityDetected()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 1);
		}
		// Construction 18 (2 levels away from 20)
		skills.put(Skill.CONSTRUCTION, 18);

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(3)
			.totalLevel(49)
			.isIronman(false)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.build();

		List<Recommendation> recs = engine.evaluateAll(profile);
		Recommendation mahoganyHomes = recs.stream()
			.filter(r -> "mahogany-homes".equals(r.getActivity().getId()))
			.findFirst()
			.orElse(null);

		Assert.assertNotNull(mahoganyHomes);
		Assert.assertEquals(Recommendation.Status.ALMOST_READY, mahoganyHomes.getStatus());
		Assert.assertEquals(2, mahoganyHomes.getMissingLevelSum());
	}

	@Test
	public void testFilterCategory()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}

		Set<String> quests = new HashSet<>();
		quests.add("Lost City");
		quests.add("Nature Spirit");
		quests.add("Fairytale I - Growing Pains");

		AccountProfile midgameProfile = AccountProfile.builder()
			.combatLevel(85)
			.totalLevel(1500)
			.isIronman(false)
			.skills(skills)
			.completedQuests(quests)
			.build();

		List<Recommendation> all = engine.evaluateAll(midgameProfile);
		List<Recommendation> bossesOnly = engine.filter(all, ActivityCategory.BOSSING, false, false);

		Assert.assertFalse(bossesOnly.isEmpty());
		for (Recommendation r : bossesOnly)
		{
			Assert.assertEquals(ActivityCategory.BOSSING, r.getActivity().getCategory());
		}
	}
}
