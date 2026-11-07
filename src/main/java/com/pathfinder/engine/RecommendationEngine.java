package com.pathfinder.engine;

import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.Activity;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.Recommendation;
import com.pathfinder.model.SkillRequirement;
import java.util.ArrayList;
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

		for (Activity activity : activityDatabase.getAllActivities())
		{
			recommendations.add(evaluate(profile, activity));
		}

		// Sort: READY first, then ALMOST_READY, then by score descending
		recommendations.sort(Comparator
			.comparing((Recommendation r) -> r.getStatus() == Recommendation.Status.READY ? 0 :
				r.getStatus() == Recommendation.Status.ALMOST_READY ? 1 : 2)
			.thenComparing(Recommendation::getMissingLevelSum)
			.thenComparing((r1, r2) -> Integer.compare(r2.getScore(), r1.getScore())));

		return recommendations;
	}

	public Recommendation evaluate(AccountProfile profile, Activity activity)
	{
		List<String> missing = new ArrayList<>();
		int missingLevelSum = 0;

		// Check skills
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

		// Check quests
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

		// Calculate status
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

		// Calculate dynamic score (higher = better recommendation)
		int score = 50;
		if (status == Recommendation.Status.READY)
		{
			score += 100;
		}
		else if (status == Recommendation.Status.ALMOST_READY)
		{
			score += (40 - (missingLevelSum * 5));
		}

		if (activity.isHiddenGem())
		{
			score += 25; // Bonus for off-the-beaten-path content!
		}

		return Recommendation.builder()
			.activity(activity)
			.status(status)
			.missingRequirements(missing)
			.missingLevelSum(missingLevelSum)
			.score(score)
			.build();
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
			// Fallback to almost ready
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
