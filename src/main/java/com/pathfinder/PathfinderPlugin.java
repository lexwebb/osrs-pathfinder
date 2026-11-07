package com.pathfinder;

import com.google.inject.Provides;
import com.pathfinder.engine.RecommendationEngine;
import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.BankSnapshot;
import com.pathfinder.ui.PathfinderPanel;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.vars.AccountType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Pathfinder",
	description = "Intelligent next-step adventure advisor and unlock discovery engine",
	tags = {"pathfinder", "quests", "goals", "suggestions", "skilling", "bossing", "gear", "bronzeman"}
)
public class PathfinderPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PathfinderConfig config;

	@Inject
	private RecommendationEngine recommendationEngine;

	private PathfinderPanel panel;
	private NavigationButton navButton;
	private BankSnapshot lastBankSnapshot;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Starting Pathfinder plugin...");

		panel = new PathfinderPanel(this, recommendationEngine);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Pathfinder")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		clientThread.invokeLater(() -> {
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				scanAccount();
			}
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Stopping Pathfinder plugin...");
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		lastBankSnapshot = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::scanAccount);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::scanAccount);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("pathfinder".equals(event.getGroup()))
		{
			clientThread.invokeLater(this::scanAccount);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK.getId())
		{
			ItemContainer bankContainer = event.getItemContainer();
			if (bankContainer != null)
			{
				processBankSnapshot(bankContainer);
				scanAccount();
			}
		}
	}

	private void processBankSnapshot(ItemContainer bankContainer)
	{
		long coins = 0;
		long platinumTokens = 0;
		long estimatedTotal = 0;
		Set<Integer> ownedItemIds = new HashSet<>();

		for (Item item : bankContainer.getItems())
		{
			if (item == null || item.getId() <= 0)
			{
				continue;
			}

			int id = item.getId();
			int qty = item.getQuantity();

			ownedItemIds.add(id);

			if (id == ItemID.COINS_995)
			{
				coins += qty;
			}
			else if (id == ItemID.PLATINUM_TOKEN)
			{
				platinumTokens += qty;
			}

			long price = itemManager.getItemPrice(id);
			estimatedTotal += (price * qty);
		}

		long liquidCash = coins + (platinumTokens * 1000L);

		lastBankSnapshot = BankSnapshot.builder()
			.coins(coins)
			.platinumTokens(platinumTokens)
			.totalLiquidCash(liquidCash)
			.estimatedTotalValue(estimatedTotal)
			.ownedItemIds(ownedItemIds)
			.timestamp(System.currentTimeMillis())
			.build();

		log.debug("Pathfinder bank scanned: Cash {}, Bank Total {}", liquidCash, estimatedTotal);
	}

	public void requestAccountScan()
	{
		clientThread.invokeLater(this::scanAccount);
	}

	private void scanAccount()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Map<Skill, Integer> skills = new HashMap<>();
		int totalLevel = 0;
		for (Skill skill : Skill.values())
		{
			int level = client.getRealSkillLevel(skill);
			skills.put(skill, level);
			totalLevel += level;
		}

		int combatLevel = client.getLocalPlayer() != null
			? client.getLocalPlayer().getCombatLevel()
			: 3;

		AccountType accountType = client.getAccountType();

		// Account restriction modes (Standard, Ironman, Bronzeman)
		boolean isIronman = false;
		boolean isBronzeman = false;

		PathfinderConfig.AccountMode mode = config.accountMode();
		if (mode == PathfinderConfig.AccountMode.BRONZEMAN)
		{
			isBronzeman = true;
		}
		else if (mode == PathfinderConfig.AccountMode.IRONMAN)
		{
			isIronman = true;
		}
		else if (mode == PathfinderConfig.AccountMode.STANDARD)
		{
			isIronman = false;
			isBronzeman = false;
		}
		else // AUTO_DETECT
		{
			isIronman = accountType != null && accountType.isIronman();
			// Auto-detect Bronzeman if user has bronzeman plugin configuration
			String bmConfig = configManager.getConfiguration("bronzemanmode", "unlockedItems");
			if (bmConfig == null)
			{
				bmConfig = configManager.getConfiguration("anotherbronzemanmode", "unlockedItems");
			}
			if (bmConfig != null && !bmConfig.isEmpty())
			{
				isBronzeman = true;
			}
		}

		// Read Bronzeman unlocked items from config
		Set<Integer> unlockedItemIds = new HashSet<>();
		readBronzemanUnlocks("bronzemanmode", unlockedItemIds);
		readBronzemanUnlocks("anotherbronzemanmode", unlockedItemIds);

		Set<String> completedQuests = new HashSet<>();
		for (Quest quest : Quest.values())
		{
			try
			{
				if (quest.getState(client) == QuestState.FINISHED)
				{
					completedQuests.add(quest.getName());
				}
			}
			catch (Exception e)
			{
				// Ignored
			}
		}

		// Inventory & equipment items
		BankSnapshot snapshot = lastBankSnapshot;
		if (snapshot != null)
		{
			Set<Integer> allOwned = new HashSet<>(snapshot.getOwnedItemIds());
			addItemsFromContainer(InventoryID.INVENTORY, allOwned);
			addItemsFromContainer(InventoryID.EQUIPMENT, allOwned);

			snapshot = BankSnapshot.builder()
				.coins(snapshot.getCoins())
				.platinumTokens(snapshot.getPlatinumTokens())
				.totalLiquidCash(snapshot.getTotalLiquidCash())
				.estimatedTotalValue(snapshot.getEstimatedTotalValue())
				.ownedItemIds(allOwned)
				.timestamp(snapshot.getTimestamp())
				.build();

			// In Bronzeman mode, any item ever found in bank/inventory counts as unlocked
			unlockedItemIds.addAll(allOwned);
		}

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(combatLevel)
			.totalLevel(totalLevel)
			.isIronman(isIronman)
			.isBronzeman(isBronzeman)
			.skills(skills)
			.completedQuests(completedQuests)
			.unlockedItemIds(unlockedItemIds)
			.bankSnapshot(snapshot)
			.build();

		if (panel != null)
		{
			panel.updateProfile(profile);
		}
	}

	private void readBronzemanUnlocks(String group, Set<Integer> targetSet)
	{
		String data = configManager.getConfiguration(group, "unlockedItems");
		if (data != null && !data.isEmpty())
		{
			for (String part : data.split("[,;\\s]+"))
			{
				try
				{
					targetSet.add(Integer.parseInt(part.trim()));
				}
				catch (NumberFormatException ignored)
				{
				}
			}
		}
	}

	private void addItemsFromContainer(InventoryID containerId, Set<Integer> targetSet)
	{
		ItemContainer container = client.getItemContainer(containerId);
		if (container != null)
		{
			for (Item item : container.getItems())
			{
				if (item != null && item.getId() > 0)
				{
					targetSet.add(item.getId());
				}
			}
		}
	}

	@Provides
	PathfinderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PathfinderConfig.class);
	}
}
