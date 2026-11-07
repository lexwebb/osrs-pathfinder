package com.pathfinder;

import com.google.inject.Provides;
import com.pathfinder.engine.RecommendationEngine;
import com.pathfinder.model.AccountProfile;
import com.pathfinder.ui.PathfinderPanel;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.vars.AccountType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Pathfinder",
	description = "Intelligent next-step adventure advisor and unlock discovery engine",
	tags = {"pathfinder", "quests", "goals", "suggestions", "skilling", "bossing"}
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
	private PathfinderConfig config;

	@Inject
	private RecommendationEngine recommendationEngine;

	private PathfinderPanel panel;
	private NavigationButton navButton;

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
		// Live refresh when skill levels increase
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::scanAccount);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		// Cache bank contents when the player accesses their bank
		if (event.getContainerId() == InventoryID.BANK.getId())
		{
			ItemContainer bankContainer = event.getItemContainer();
			if (bankContainer != null)
			{
				log.debug("Pathfinder cached {} bank items", bankContainer.getItems().length);
				// Bank snapshot can be wired into economy-aware recommendations
			}
		}
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
		boolean isIronman = accountType != null && accountType.isIronman();

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
				// Some quest states may not be initialized depending on server state
			}
		}

		AccountProfile profile = AccountProfile.builder()
			.combatLevel(combatLevel)
			.totalLevel(totalLevel)
			.isIronman(isIronman)
			.skills(skills)
			.completedQuests(completedQuests)
			.build();

		log.debug("Scanned account profile: Combat {}, Total {}, Quests completed: {}",
			combatLevel, totalLevel, completedQuests.size());

		if (panel != null)
		{
			panel.updateProfile(profile);
		}
	}

	@Provides
	PathfinderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PathfinderConfig.class);
	}
}
