package com.pathfinder.engine;

import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.Activity;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.BankSnapshot;
import com.pathfinder.model.GearUpgrade;
import com.pathfinder.model.NorthStarGoal;
import com.pathfinder.model.NorthStarProgress;
import com.pathfinder.model.Recommendation;
import com.pathfinder.model.SkillRequirement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

	public ActivityDatabase getActivityDatabase()
	{
		return activityDatabase;
	}

	public List<Recommendation> evaluateAll(AccountProfile profile)
	{
		return evaluateAll(profile, null);
	}

	public List<Recommendation> evaluateAll(AccountProfile profile, NorthStarGoal activeGoal)
	{
		List<Recommendation> recommendations = new ArrayList<>();
		NorthStarProgress progress = activeGoal != null ? evaluateNorthStar(profile, activeGoal) : null;

		// 1. Standard activities (quests, skilling, minigames, bosses, diaries)
		for (Activity activity : activityDatabase.getAllActivities())
		{
			// Skip quests that the player has already finished
			if (activity.getCategory() == ActivityCategory.QUEST &&
				(profile.hasCompletedQuest(activity.getTitle()) || profile.hasCompletedQuest(activity.getId())))
			{
				continue;
			}

			// Skip achievement diaries that the player has already finished
			if (activity.getCategory() == ActivityCategory.DIARY &&
				profile.getCompletedDiaries().contains(activity.getId()))
			{
				continue;
			}

			recommendations.add(evaluateActivity(profile, activity, activeGoal, progress));
		}

		// 2. Gear upgrades (evaluated against bank, cash stack, and Bronzeman/Ironman rules)
		for (GearUpgrade gear : activityDatabase.getGearUpgrades())
		{
			// Skip if player currently owns the item in bank/inventory
			if (profile.hasScannedBank() && profile.getBankSnapshot().ownsItem(gear.getItemId()))
			{
				continue;
			}

			recommendations.add(evaluateGearUpgrade(profile, gear, activeGoal, progress));
		}

		sort(recommendations, SortOrder.SMART_SCORE);
		return recommendations;
	}

	public boolean isItemSatisfied(AccountProfile profile, int itemId)
	{
		if (!profile.hasScannedBank())
		{
			return false;
		}

		BankSnapshot snapshot = profile.getBankSnapshot();
		if (snapshot.ownsItem(itemId))
		{
			return true;
		}

		// Slot tier upgrades: owning an equivalent or higher tier item satisfies lower/mid tier requirements
		switch (itemId)
		{
			case 1704: // Amulet of Glory -> Fury, Blood Fury, Torture
				return snapshot.ownsItem(6585)
					|| snapshot.ownsItem(24780)
					|| snapshot.ownsItem(19553);
			case 28810: // Zombie Axe -> Whip, Fang, Saeldor, Scythe
				return snapshot.ownsItem(4151)
					|| snapshot.ownsItem(26219)
					|| snapshot.ownsItem(23995)
					|| snapshot.ownsItem(22325);
			case 4151: // Abyssal Whip -> Fang, Saeldor, Scythe
				return snapshot.ownsItem(26219)
					|| snapshot.ownsItem(23995)
					|| snapshot.ownsItem(22325);
			case 6570: // Fire Cape -> Infernal Cape, Dizana's Quiver
				return snapshot.ownsItem(21295)
					|| snapshot.ownsItem(28822);
			case 12954: // Dragon Defender -> Avernic Defender
				return snapshot.ownsItem(22322);
			case 11840: // Dragon Boots -> Primordial Boots
				return snapshot.ownsItem(13239);
			case 10828: // Helm of Neitiznot -> Faceguard, Torva
				return snapshot.ownsItem(24271)
					|| snapshot.ownsItem(26382);
			case 9185: // Rune Crossbow -> Hunter's Sunlight Crossbow, Dragon Crossbow, Armadyl Crossbow, ZCB, Blowpipe, Bowfa
				return snapshot.ownsItem(28869)
					|| snapshot.ownsItem(21902)
					|| snapshot.ownsItem(11785)
					|| snapshot.ownsItem(26374)
					|| snapshot.ownsItem(12926)
					|| snapshot.ownsItem(25865);
			case 28585: // Warped Sceptre -> Trident of the Seas, Trident of the Swamp, Sang Staff, Shadow
				return snapshot.ownsItem(11907)
					|| snapshot.ownsItem(12899)
					|| snapshot.ownsItem(22323)
					|| snapshot.ownsItem(27275);
			default:
				return false;
		}
	}

	public NorthStarProgress evaluateNorthStar(AccountProfile profile, NorthStarGoal goal)
	{
		if (goal == null || profile == null)
		{
			return null;
		}

		Map<Skill, Integer> effectiveSkills = new LinkedHashMap<>(goal.getTargetSkills());
		List<String> effectiveQuests = new ArrayList<>(goal.getPrerequisiteQuests());
		List<NorthStarProgress.ItemGap> missingItems = new ArrayList<>();

		int totalReqs = 0;
		int satisfiedReqs = 0;

		// 1. Process Required Items to check ownership and discover organic requirements
		for (int i = 0; i < goal.getRequiredItemIds().size(); i++)
		{
			int itemId = goal.getRequiredItemIds().get(i);
			String name = i < goal.getRequiredItemNames().size() ? goal.getRequiredItemNames().get(i) : "Item #" + itemId;
			totalReqs++;

			boolean isSatisfied = isItemSatisfied(profile, itemId);
			if (isSatisfied)
			{
				satisfiedReqs++;
			}
			else
			{
				GearUpgrade gear = activityDatabase != null ? activityDatabase.getGearUpgradeByItemId(itemId) : null;
				boolean isBronzemanUnlocked = profile.isBronzeman() && profile.hasUnlockedItem(itemId);

				// For Ironman, or Bronzeman without organic unlock, extract organic skill & quest dependencies
				if ((profile.isIronman() || (profile.isBronzeman() && !isBronzemanUnlocked)) && gear != null)
				{
					for (SkillRequirement orgReq : gear.getOrganicSkillReqs())
					{
						int existingLvl = effectiveSkills.getOrDefault(orgReq.getSkill(), 0);
						if (orgReq.getLevel() > existingLvl)
						{
							effectiveSkills.put(orgReq.getSkill(), orgReq.getLevel());
						}
					}

					if (gear.getQuestReq() != null && !gear.getQuestReq().isEmpty() && !effectiveQuests.contains(gear.getQuestReq()))
					{
						effectiveQuests.add(gear.getQuestReq());
					}
				}

				missingItems.add(NorthStarProgress.ItemGap.builder()
					.itemId(itemId)
					.itemName(name)
					.build());
			}
		}

		// 2. Skill Requirements (combining base target skills + organic gear requirements)
		List<NorthStarProgress.SkillGap> skillGaps = new ArrayList<>();
		for (Map.Entry<Skill, Integer> entry : effectiveSkills.entrySet())
		{
			Skill skill = entry.getKey();
			int targetLvl = entry.getValue();
			int currentLvl = profile.getSkillLevel(skill);
			totalReqs++;

			if (currentLvl >= targetLvl)
			{
				satisfiedReqs++;
			}
			else
			{
				skillGaps.add(NorthStarProgress.SkillGap.builder()
					.skill(skill)
					.currentLevel(currentLvl)
					.targetLevel(targetLvl)
					.levelsRemaining(targetLvl - currentLvl)
					.build());
			}
		}

		// 3. Prerequisite Quests (combining base quests + gear quest requirements like RFD)
		List<String> missingQuests = new ArrayList<>();
		for (String quest : effectiveQuests)
		{
			totalReqs++;
			if (profile.hasCompletedQuest(quest))
			{
				satisfiedReqs++;
			}
			else
			{
				missingQuests.add(quest);
			}
		}

		int progressPercent = totalReqs > 0 ? (satisfiedReqs * 100 / totalReqs) : 100;
		boolean isFullyReady = (satisfiedReqs == totalReqs);

		return NorthStarProgress.builder()
			.goal(goal)
			.isFullyReady(isFullyReady)
			.totalRequirements(totalReqs)
			.satisfiedRequirements(satisfiedReqs)
			.progressPercent(progressPercent)
			.missingSkills(skillGaps)
			.missingQuests(missingQuests)
			.missingItems(missingItems)
			.build();
	}

	public Recommendation evaluateActivity(AccountProfile profile, Activity activity)
	{
		return evaluateActivity(profile, activity, null, null);
	}

	public Recommendation evaluateActivity(
		AccountProfile profile,
		Activity activity,
		NorthStarGoal activeGoal,
		NorthStarProgress progress)
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
				missing.add(String.format("%s: %d/%d (%d away)",
					req.getSkill().getName(), currentLevel, req.getLevel(), diff));
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
			score += 30; // Bonus for variety
		}

		// North Star Pathway Analysis
		boolean isPathway = false;
		String pathwayReason = "";

		if (activeGoal != null && progress != null)
		{
			// Check if this activity is a direct prerequisite quest
			if (activity.getCategory() == ActivityCategory.QUEST &&
				(progress.hasQuestGap(activity.getTitle()) || progress.hasQuestGap(activity.getId())))
			{
				isPathway = true;
				pathwayReason = "🎯 Required Quest for " + activeGoal.getTitle();
				score += 120; // Major priority boost
			}
			else
			{
				// Check if this activity trains any skill with an active gap
				for (Skill trained : activity.getTrainedSkills())
				{
					if (progress.hasSkillGap(trained))
					{
						NorthStarProgress.SkillGap gap = progress.getSkillGap(trained);
						isPathway = true;
						pathwayReason = String.format("🎯 Pathway for %s: %s %d → %d (+%d lvls)",
							activeGoal.getTitle(), trained.getName(), gap.getCurrentLevel(), gap.getTargetLevel(), gap.getLevelsRemaining());
						score += 80;
						break;
					}
				}
			}
		}

		return Recommendation.builder()
			.activity(activity)
			.status(status)
			.missingRequirements(missing)
			.missingLevelSum(missingLevelSum)
			.score(score)
			.isNorthStarPathway(isPathway)
			.pathwayReason(pathwayReason)
			.build();
	}

	public Recommendation evaluateGearUpgrade(AccountProfile profile, GearUpgrade gear)
	{
		return evaluateGearUpgrade(profile, gear, null, null);
	}

	public Recommendation evaluateGearUpgrade(
		AccountProfile profile,
		GearUpgrade gear,
		NorthStarGoal activeGoal,
		NorthStarProgress progress)
	{
		List<String> missing = new ArrayList<>();
		int missingLevelSum = 0;

		boolean isBronzemanUnlocked = profile.isBronzeman() && profile.hasUnlockedItem(gear.getItemId());

		// In Bronzeman (unlocked=false) and Ironman mode, enforce BOTH equip and organic drop requirements
		List<SkillRequirement> reqsToCheck = new ArrayList<>(gear.getSkillReqs());
		if ((profile.isIronman() || (profile.isBronzeman() && !isBronzemanUnlocked))
			&& !gear.getOrganicSkillReqs().isEmpty())
		{
			for (SkillRequirement orgReq : gear.getOrganicSkillReqs())
			{
				boolean exists = false;
				for (int i = 0; i < reqsToCheck.size(); i++)
				{
					if (reqsToCheck.get(i).getSkill() == orgReq.getSkill())
					{
						if (orgReq.getLevel() > reqsToCheck.get(i).getLevel())
						{
							reqsToCheck.set(i, orgReq);
						}
						exists = true;
						break;
					}
				}
				if (!exists)
				{
					reqsToCheck.add(orgReq);
				}
			}
		}

		for (SkillRequirement req : reqsToCheck)
		{
			int current = profile.getSkillLevel(req.getSkill());
			if (current < req.getLevel())
			{
				int diff = req.getLevel() - current;
				missingLevelSum += diff;
				missing.add(String.format("%s: %d/%d (%d away)",
					req.getSkill().getName(), current, req.getLevel(), diff));
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

		boolean canAfford = true;
		List<String> tags = new ArrayList<>();
		tags.add("Gear");
		if (gear.getStyle() != null && !gear.getStyle().isEmpty())
		{
			tags.add(gear.getStyle());
		}
		tags.add(gear.getSlot());

		String desc;
		if (profile.isBronzeman() && !isBronzemanUnlocked)
		{
			tags.add("Bronzeman Unlock");
			desc = "Bronzeman Goal: Obtain organically to unlock GE purchases!";
			if (gear.getIronmanSource() != null && !gear.getIronmanSource().isEmpty())
			{
				missing.add("Source: " + gear.getIronmanSource());
			}
		}
		else if (profile.isIronman())
		{
			tags.add("Ironman Grind");
			desc = gear.getHighlight();
			if (gear.getIronmanSource() != null && !gear.getIronmanSource().isEmpty())
			{
				missing.add("Source: " + gear.getIronmanSource());
			}
		}
		else
		{
			tags.add("Tradeable");
			desc = gear.getHighlight();
			if (profile.hasScannedBank() && gear.getEstimatedPrice() > 0)
			{
				long cash = profile.getBankSnapshot().getTotalLiquidCash();
				if (cash < gear.getEstimatedPrice())
				{
					canAfford = false;
					long needed = gear.getEstimatedPrice() - cash;
					missing.add(String.format("Cost: %s GP (Need %s more)",
						BankSnapshot.formatGpValue(gear.getEstimatedPrice()),
						BankSnapshot.formatGpValue(needed)));
				}
				else
				{
					desc += " (Affordable with ~" + BankSnapshot.formatGpValue(gear.getEstimatedPrice()) + " GP)";
				}
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

		int score = 40;
		if (status == Recommendation.Status.READY)
		{
			score += 90;
		}
		else if (status == Recommendation.Status.ALMOST_READY)
		{
			score += (35 - (missingLevelSum * 4));
		}

		// North Star Pathway Analysis
		boolean isPathway = false;
		String pathwayReason = "";

		if (activeGoal != null && progress != null)
		{
			if (progress.hasItemGap(gear.getItemId()))
			{
				isPathway = true;
				pathwayReason = "🎯 Target Gear for " + activeGoal.getTitle();
				score += 90;
			}
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
			.tags(tags)
			.build();

		return Recommendation.builder()
			.activity(gearActivity)
			.status(status)
			.missingRequirements(missing)
			.missingLevelSum(missingLevelSum)
			.score(score)
			.isNorthStarPathway(isPathway)
			.pathwayReason(pathwayReason)
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
					.comparing((Recommendation r) -> r.isNorthStarPathway() ? 0 : 1) // Pathways always float to top!
					.thenComparing((Recommendation r) -> r.getStatus() == Recommendation.Status.READY ? 0 :
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
		boolean hideLocked,
		boolean hiddenGemsOnly)
	{
		return filter(list, category, readyOnly, hideLocked, hiddenGemsOnly, false);
	}

	public List<Recommendation> filter(
		List<Recommendation> list,
		ActivityCategory category,
		boolean readyOnly,
		boolean hideLocked,
		boolean hiddenGemsOnly,
		boolean pathwaysOnly)
	{
		return list.stream()
			.filter(r -> !pathwaysOnly || r.isNorthStarPathway())
			.filter(r -> category == ActivityCategory.ALL || r.getActivity().getCategory() == category)
			.filter(r -> !readyOnly || r.getStatus() == Recommendation.Status.READY)
			.filter(r -> !hideLocked || r.getStatus() != Recommendation.Status.LOCKED)
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
