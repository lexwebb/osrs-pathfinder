package com.pathfinder.engine;

import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.Activity;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.BankSnapshot;
import com.pathfinder.model.GearUpgrade;
import com.pathfinder.model.Recommendation;
import com.pathfinder.model.SkillRequirement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

@Singleton
public class RecommendationEngine
{
	public enum SortOrder
	{
		SMART_SCORE("Smart Score"),
		CLOSEST_UNLOCK("Closest Unlock"),
		COMBAT_LEVEL("Combat Level"),
		ALPHABETICAL("Alphabetical");

		private final String label;

		SortOrder(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final ActivityDatabase activityDatabase;
	private final Random random = new Random();

	@Inject
	public RecommendationEngine(ActivityDatabase activityDatabase)
	{
		this.activityDatabase = activityDatabase;
	}

	public List<Recommendation> evaluateAll(AccountProfile profile)
	{
		List<Recommendation> recommendations = new ArrayList<>();

		// 1. Standard activities (quests, skilling, minigames, bosses, diaries)
		for (Activity activity : activityDatabase.getAllActivities())
		{
			recommendations.add(evaluateActivity(profile, activity));
		}

		// 2. Gear upgrades (evaluated against bank & cash stack)
		for (GearUpgrade gear : activityDatabase.getGearUpgrades())
		{
			// Skip if player already owns the item in bank
			if (profile.hasScannedBank() && profile.getBankSnapshot().ownsItem(gear.getItemId()))
			{
				continue;
			}

			recommendations.add(evaluateGearUpgrade(profile, gear));
		}

		sort(recommendations, SortOrder.SMART_SCORE);
		return recommendations;
	}

	public Recommendation evaluateActivity(AccountProfile profile, Activity activity)
	{
		List<String> missing = new ArrayList<>();
		int missingLevelSum = 0;

		// Skills check
		for (SkillRequirement req : activity.getSkillRequirements())
		{
			int currentLevel = profile.getSkillLevel(req.getSkill());
			if (currentLevel < req.getLevel())
			{
				int diff = req.getLevel() - currentLevel;
				missingLevelSum += diff;
				missing.add(String.format("%s: %d/%d (%d level%s away)",
					req.getSkill().getName(), currentLevel, req.getLevel(), diff, diff == 1 ? "" : "s"));
			}
		}

		// Quests check
		int missingQuestCount = 0;
		for (String quest : activity.getQuestRequirements())
		{
			if (!profile.hasCompletedQuest(quest))
			{
				missingQuestCount++;
				missing.add("Quest: " + quest);
			}
		}

		// Combat level notice
		if (profile.getCombatLevel() > 0 && activity.getRecommendedCombat() > profile.getCombatLevel())
		{
			missing.add(String.format("Combat %d+ recommended (currently %d)",
				activity.getRecommendedCombat(), profile.getCombatLevel()));
		}

		// Status calculation
		Recommendation.Status status;
		if (missing.isEmpty())
		{
			status = Recommendation.Status.READY;
		}
		else if (missingLevelSum <= 4 && missingQuestCount <= 1)
		{
			status = Recommendation.Status.ALMOST_READY;
		}
		else
		{
			status = Recommendation.Status.LOCKED;
		}

		// Dynamic score
		int score = 50;
		if (status == Recommendation.Status.READY)
		{
			score += 100;
		}
		else if (status == Recommendation.Status.ALMOST_READY)
		{
			score += (45 - (missingLevelSum * 5));
		}

		if (activity.isHiddenGem())
		{
			score += 30; // Bonus for variety/off-the-beaten-path
		}

		return Recommendation.builder()
			.activity(activity)
			.status(status)
			.missingRequirements(missing)
			.missingLevelSum(missingLevelSum)
			.score(score)
			.build();
	}

	public Recommendation evaluateGearUpgrade(AccountProfile profile, GearUpgrade gear)
	{
		List<String> missing = new ArrayList<>();
		int missingLevelSum = 0;

		// Skill requirements
		for (SkillRequirement req : gear.getSkillReqs())
		{
			int current = profile.getSkillLevel(req.getSkill());
			if (current < req.getLevel())
			{
				int diff = req.getLevel() - current;
				missingLevelSum += diff;
				missing.add(String.format("%s: %d/%d (%d level%s away)",
					req.getSkill().getName(), current, req.getLevel(), diff, diff == 1 ? "" : "s"));
			}
		}

		// Quest requirement
		int missingQuestCount = 0;
		if (gear.getQuestReq() != null && !gear.getQuestReq().isEmpty())
		{
			if (!profile.hasCompletedQuest(gear.getQuestReq()))
			{
				missingQuestCount++;
				missing.add("Quest: " + gear.getQuestReq());
			}
		}

		// Economy / Ironman acquisition
		boolean canAfford = true;
		if (profile.isIronman())
		{
			if (gear.getIronmanSource() != null && !gear.getIronmanSource().isEmpty())
			{
				missing.add("Ironman Source: " + gear.getIronmanSource());
			}
		}
		else if (profile.hasScannedBank() && gear.getEstimatedPrice() > 0)
		{
			long cash = profile.getBankSnapshot().getTotalLiquidCash();
			if (cash < gear.getEstimatedPrice())
			{
				canAfford = false;
				long needed = gear.getEstimatedPrice() - cash;
				missing.add(String.format("Cost: %s GP (You have %s — need %s more)",
					BankSnapshot.formatGpValue(gear.getEstimatedPrice()),
					BankSnapshot.formatGpValue(cash),
					BankSnapshot.formatGpValue(needed)));
			}
		}

		// Status calculation
		Recommendation.Status status;
		if (missingLevelSum == 0 && missingQuestCount == 0 && canAfford)
		{
			status = Recommendation.Status.READY;
		}
		else if (missingLevelSum <= 4 && missingQuestCount <= 1)
		{
			status = Recommendation.Status.ALMOST_READY;
		}
		else
		{
			status = Recommendation.Status.LOCKED;
		}

		// Description & Highlights
		String desc = gear.getHighlight();
		if (gear.getEstimatedPrice() > 0 && !profile.isIronman())
		{
			desc += " (~" + BankSnapshot.formatGpValue(gear.getEstimatedPrice()) + " GP on Grand Exchange)";
		}

		Activity gearActivity = Activity.builder()
			.id("gear-" + gear.getId())
			.title(gear.getName())
			.category(ActivityCategory.GEAR)
			.description(desc)
			.unlockHighlight("Slot: " + gear.getSlot() + " | " + gear.getHighlight())
			.recommendedCombat(gear.getRecommendedCombat())
			.ironmanFriendly(true)
			.hiddenGem(false)
			.wikiUrl(gear.getWikiUrl())
			.skillRequirements(gear.getSkillReqs())
			.questRequirements(gear.getQuestReq() != null && !gear.getQuestReq().isEmpty() ? List.of(gear.getQuestReq()) : Collections.emptyList())
			.tags(Arrays.asList("Gear", gear.getSlot(), profile.isIronman() ? "Untradable/Grind" : "Tradeable"))
			.build();

		int score = 60;
		if (status == Recommendation.Status.READY)
		{
			score += 80;
		}
		else if (status == Recommendation.Status.ALMOST_READY)
		{
			score += 30;
		}

		return Recommendation.builder()
			.activity(gearActivity)
			.status(status)
			.missingRequirements(missing)
			.missingLevelSum(missingLevelSum)
			.score(score)
			.build();
	}

	public void sort(List<Recommendation> list, SortOrder sortOrder)
	{
		switch (sortOrder)
		{
			case CLOSEST_UNLOCK:
				list.sort(Comparator
					.comparing(Recommendation::getMissingLevelSum)
					.thenComparing(r -> r.getStatus() == Recommendation.Status.READY ? 0 : 1));
				break;
			case COMBAT_LEVEL:
				list.sort(Comparator
					.comparing((Recommendation r) -> r.getActivity().getRecommendedCombat())
					.thenComparing(Recommendation::getMissingLevelSum));
				break;
			case ALPHABETICAL:
				list.sort(Comparator.comparing(r -> r.getActivity().getTitle()));
				break;
			case SMART_SCORE:
			default:
				list.sort(Comparator
					.comparing((Recommendation r) -> r.getStatus() == Recommendation.Status.READY ? 0 :
						r.getStatus() == Recommendation.Status.ALMOST_READY ? 1 : 2)
					.thenComparing(Recommendation::getMissingLevelSum)
					.thenComparing((r1, r2) -> Integer.compare(r2.getScore(), r1.getScore())));
				break;
		}
	}

	public List<Recommendation> filter(
		List<Recommendation> list,
		ActivityCategory category,
		boolean readyOnly,
		boolean hiddenGemsOnly)
	{
		return list.stream()
			.filter(r -> category == ActivityCategory.ALL || r.getActivity().getCategory() == category)
			.filter(r -> !readyOnly || r.getStatus() == Recommendation.Status.READY)
			.filter(r -> !hiddenGemsOnly || r.getActivity().isHiddenGem())
			.collect(Collectors.toList());
	}

	public Recommendation pickSurprise(List<Recommendation> list)
	{
		List<Recommendation> candidates = list.stream()
			.filter(r -> r.getStatus() == Recommendation.Status.READY)
			.collect(Collectors.toList());

		if (candidates.isEmpty())
		{
			candidates = list.stream()
				.filter(r -> r.getStatus() == Recommendation.Status.ALMOST_READY)
				.collect(Collectors.toList());
		}

		if (candidates.isEmpty())
		{
			return list.isEmpty() ? null : list.get(0);
		}

		return candidates.get(random.nextInt(candidates.size()));
	}
}
