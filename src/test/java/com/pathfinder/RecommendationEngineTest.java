package com.pathfinder;

import com.google.gson.Gson;
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
	private final Gson gson = new Gson();

	@Before
	public void setUp()
	{
		database = new ActivityDatabase(gson);
		engine = new RecommendationEngine(database);
	}

	@Test
	public void testDatabaseLoadsFromBundledJson()
	{
		Assert.assertTrue("Should load activities from activities.json", database.getAllActivities().size() >= 25);
		Assert.assertTrue("Should load gear from gear_upgrades.json", database.getGearUpgrades().size() >= 35);
	}

	@Test
	public void testProfileSerializationAndOfflineRestore()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		skills.put(Skill.ATTACK, 80);
		skills.put(Skill.STRENGTH, 85);
		skills.put(Skill.DEFENCE, 75);

		Set<String> quests = new HashSet<>();
		quests.add("Dragon Slayer I");
		quests.add("Monkey Madness I");

		AccountProfile profile = AccountProfile.builder()
			.username("IronLegend")
			.cachedTimestamp(System.currentTimeMillis())
			.combatLevel(104)
			.totalLevel(1550)
			.isIronman(false)
			.isBronzeman(true)
			.skills(skills)
			.completedQuests(quests)
			.build();

		String json = gson.toJson(profile);
		Assert.assertNotNull(json);

		AccountProfile restored = gson.fromJson(json, AccountProfile.class);
		Assert.assertNotNull(restored);
		Assert.assertEquals("IronLegend", restored.getUsername());
		Assert.assertEquals(104, restored.getCombatLevel());
		Assert.assertTrue(restored.isBronzeman());
		Assert.assertTrue(restored.hasCompletedQuest("Dragon Slayer I"));
		Assert.assertEquals(80, restored.getSkillLevel(Skill.ATTACK));
	}

	@Test
	public void testCompletedQuestsAreNeverRecommended()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}

		Set<String> completedQuests = new HashSet<>();
		completedQuests.add("Dragon Slayer I");
		completedQuests.add("Bone Voyage");

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(104)
			.totalLevel(1500)
			.isIronman(false)
			.isBronzeman(true)
			.skills(skills)
			.completedQuests(completedQuests)
			.build();

		List<Recommendation> recs = engine.evaluateAll(profile);

		boolean hasDragonSlayer = recs.stream()
			.anyMatch(r -> "Dragon Slayer I".equalsIgnoreCase(r.getActivity().getTitle())
				|| "dragon-slayer-i".equalsIgnoreCase(r.getActivity().getId()));

		boolean hasBoneVoyage = recs.stream()
			.anyMatch(r -> "Bone Voyage".equalsIgnoreCase(r.getActivity().getTitle())
				|| "bone-voyage".equalsIgnoreCase(r.getActivity().getId()));

		Assert.assertFalse("Completed quest Dragon Slayer I must NOT be recommended", hasDragonSlayer);
		Assert.assertFalse("Completed quest Bone Voyage must NOT be recommended", hasBoneVoyage);
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

	@Test
	public void testBronzemanUnlockedItemAllowsEquipWithoutOrganicSlayer()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 70);
		}
		skills.put(Skill.ATTACK, 75);
		skills.put(Skill.SLAYER, 50); // Under 85 Slayer!

		Set<Integer> unlocked = new HashSet<>();
		unlocked.add(4151); // Abyssal Whip unlocked organically in Bronzeman mode

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(104)
			.totalLevel(1500)
			.isIronman(false)
			.isBronzeman(true)
			.skills(skills)
			.completedQuests(new HashSet<>())
			.unlockedItemIds(unlocked)
			.build();

		List<Recommendation> recs = engine.evaluateAll(profile);
		Recommendation whipRec = recs.stream()
			.filter(r -> "gear-abyssal-whip".equals(r.getActivity().getId()))
			.findFirst()
			.orElse(null);

		Assert.assertNotNull(whipRec);
		// Since it is already unlocked in Bronzeman, it only requires Attack 70 to equip
		Assert.assertEquals(Recommendation.Status.READY, whipRec.getStatus());
	}

	@Test
	public void testOwnedItemsInBankAreNotRecommended()
	{
		Map<Skill, Integer> skills = new HashMap<>();
		for (Skill s : Skill.values())
		{
			skills.put(s, 80);
		}

		Set<Integer> owned = new HashSet<>();
		owned.add(28810); // Zombie Axe in bank

		BankSnapshot snapshot = BankSnapshot.builder()
			.timestamp(System.currentTimeMillis())
			.totalLiquidCash(5_000_000)
			.ownedItemIds(owned)
			.build();

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(100)
			.totalLevel(1600)
			.skills(skills)
			.bankSnapshot(snapshot)
			.build();

		List<Recommendation> recs = engine.evaluateAll(profile);
		boolean hasZombieAxe = recs.stream()
			.anyMatch(r -> "gear-zombie-axe".equals(r.getActivity().getId()));

		Assert.assertFalse("Owned Zombie Axe (28810) must NOT be recommended", hasZombieAxe);
	}
}
