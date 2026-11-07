package com.pathfinder.ui;

import com.pathfinder.PathfinderPlugin;
import com.pathfinder.engine.ActivityDatabase;
import com.pathfinder.engine.RecommendationEngine;
import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.Activity;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.BankSnapshot;
import com.pathfinder.model.BossCheckpoint;
import com.pathfinder.model.CombatCheckpoint;
import com.pathfinder.model.NorthStarGoal;
import com.pathfinder.model.NorthStarProgress;
import com.pathfinder.model.Recommendation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

public class PathfinderPanel extends PluginPanel
{
	private static final Color BRONZE_COLOR = new Color(205, 127, 50);

	private final PathfinderPlugin plugin;
	private final RecommendationEngine engine;
	private final ActivityDatabase database;

	private final JLabel headerTitle = new JLabel("OSRS Pathfinder");
	private final JLabel modeBadge = new JLabel("");
	private final JLabel statusSummary = new JLabel("Log in to scan account");
	private final JLabel bankSummary = new JLabel("💰 Open bank to scan wealth & gear");

	private final JComboBox<GoalOption> goalCombo = new JComboBox<>();
	private final JComboBox<ActivityCategory> categoryCombo = new JComboBox<>(ActivityCategory.values());
	private final JComboBox<RecommendationEngine.SortOrder> sortCombo = new JComboBox<>(RecommendationEngine.SortOrder.values());

	private final JCheckBox hideLockedCheck = new JCheckBox("Hide locked activities", true);
	private final JCheckBox readyOnlyCheck = new JCheckBox("Ready to do only", false);
	private final JCheckBox hiddenGemsCheck = new JCheckBox("Hidden gems only", false);

	private final JButton surpriseBtn = new JButton("🎲 Surprise");
	private final JButton refreshBtn = new JButton("🔄 Scan");

	private final NorthStarCard northStarCard;
	private final JPanel cardsContainer = new JPanel();

	private AccountProfile lastProfile;
	private NorthStarGoal activeGoal;
	private List<Recommendation> currentEvaluations = new ArrayList<>();

	public static class GoalOption
	{
		private final String label;
		private final NorthStarGoal goal;

		public GoalOption(String label, NorthStarGoal goal)
		{
			this.label = label;
			this.goal = goal;
		}

		public NorthStarGoal getGoal()
		{
			return goal;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	public PathfinderPanel(PathfinderPlugin plugin, RecommendationEngine engine)
	{
		super(false);
		this.plugin = plugin;
		this.engine = engine;
		this.database = engine.getActivityDatabase();

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Initialize North Star Card widget
		this.northStarCard = new NorthStarCard(this::setNorthStar, this::updateDisplay);

		// Top Control Container using DynamicGridLayout (fills 100% width, zero left offset bugs)
		JPanel topContainer = new JPanel(new DynamicGridLayout(0, 1, 0, 3));
		topContainer.setBorder(new EmptyBorder(6, 6, 4, 6));
		topContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// 1. Header Title (Line 1)
		headerTitle.setFont(FontManager.getRunescapeBoldFont());
		headerTitle.setForeground(ColorScheme.BRAND_ORANGE);
		headerTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

		// 2. Account Mode & Combat Level (Line 2 - full width, no horizontal collision)
		modeBadge.setFont(FontManager.getRunescapeSmallFont());
		modeBadge.setForeground(BRONZE_COLOR);
		modeBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

		// 3. Ready Activities Summary (Line 3 - full width)
		statusSummary.setFont(FontManager.getRunescapeSmallFont());
		statusSummary.setForeground(new Color(180, 180, 180));
		statusSummary.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Bank Banner Box
		JPanel bankBox = new JPanel(new BorderLayout());
		bankBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bankBox.setBorder(new EmptyBorder(3, 5, 3, 5));
		bankBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		bankBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

		bankSummary.setFont(FontManager.getRunescapeSmallFont());
		bankSummary.setForeground(new Color(170, 170, 170));
		bankBox.add(bankSummary, BorderLayout.CENTER);

		// Action Buttons Row: [ 🎲 Surprise ] [ 🔄 Scan ]
		JPanel actionRow = new JPanel(new GridLayout(1, 2, 4, 0));
		actionRow.setOpaque(false);
		actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

		surpriseBtn.setFont(FontManager.getRunescapeSmallFont());
		surpriseBtn.setForeground(new Color(241, 196, 15));
		surpriseBtn.setMargin(new Insets(2, 2, 2, 2));
		surpriseBtn.setFocusable(false);
		surpriseBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		surpriseBtn.addActionListener(e -> triggerSurprise());

		refreshBtn.setFont(FontManager.getRunescapeSmallFont());
		refreshBtn.setMargin(new Insets(2, 2, 2, 2));
		refreshBtn.setFocusable(false);
		refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		refreshBtn.addActionListener(e -> {
			if (plugin != null)
			{
				plugin.requestAccountScan();
			}
		});

		actionRow.add(surpriseBtn);
		actionRow.add(refreshBtn);

		// North Star Goal Selector Dropdown (Full width 200px)
		populateGoalDropdown();
		goalCombo.setFont(FontManager.getRunescapeSmallFont());
		goalCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
		goalCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		goalCombo.addActionListener(e -> {
			GoalOption opt = (GoalOption) goalCombo.getSelectedItem();
			if (opt != null)
			{
				setNorthStar(opt.getGoal(), false);
			}
		});

		// Filters: Category & Sort (Each full width so labels never truncate)
		categoryCombo.setFont(FontManager.getRunescapeSmallFont());
		categoryCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
		categoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		categoryCombo.addActionListener(e -> updateDisplay());

		sortCombo.setFont(FontManager.getRunescapeSmallFont());
		sortCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
		sortCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		sortCombo.addActionListener(e -> updateDisplay());

		// Toggles: Stacked vertically with full width so labels never truncate
		hideLockedCheck.setOpaque(false);
		hideLockedCheck.setForeground(Color.WHITE);
		hideLockedCheck.setFont(FontManager.getRunescapeSmallFont());
		hideLockedCheck.setFocusable(false);
		hideLockedCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
		hideLockedCheck.addActionListener(e -> updateDisplay());

		readyOnlyCheck.setOpaque(false);
		readyOnlyCheck.setForeground(Color.WHITE);
		readyOnlyCheck.setFont(FontManager.getRunescapeSmallFont());
		readyOnlyCheck.setFocusable(false);
		readyOnlyCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
		readyOnlyCheck.addActionListener(e -> updateDisplay());

		hiddenGemsCheck.setOpaque(false);
		hiddenGemsCheck.setForeground(new Color(241, 196, 15));
		hiddenGemsCheck.setFont(FontManager.getRunescapeSmallFont());
		hiddenGemsCheck.setFocusable(false);
		hiddenGemsCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
		hiddenGemsCheck.addActionListener(e -> updateDisplay());

		// Assemble Header Controls
		topContainer.add(headerTitle);
		topContainer.add(modeBadge);
		topContainer.add(statusSummary);
		topContainer.add(bankBox);
		topContainer.add(actionRow);
		topContainer.add(goalCombo);
		topContainer.add(categoryCombo);
		topContainer.add(sortCombo);
		topContainer.add(hideLockedCheck);
		topContainer.add(readyOnlyCheck);
		topContainer.add(hiddenGemsCheck);
		topContainer.add(northStarCard);

		// Card Container inside ScrollPane
		cardsContainer.setLayout(new DynamicGridLayout(0, 1, 0, 6));
		cardsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		cardsContainer.setBorder(new EmptyBorder(3, 4, 6, 4));

		ScrollablePanel listWrapper = new ScrollablePanel();
		listWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listWrapper.add(cardsContainer, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(listWrapper);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		add(topContainer, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	private void populateGoalDropdown()
	{
		goalCombo.removeAllItems();
		goalCombo.addItem(new GoalOption("⭐ Goal: Discovery Mode", null));

		// Combat Checkpoints
		for (CombatCheckpoint cp : database.getCombatCheckpoints())
		{
			goalCombo.addItem(new GoalOption(cp.getStyle() + ": " + cp.getName(), database.createGoalFromCombatCheckpoint(cp)));
		}

		// Boss Checkpoints
		for (BossCheckpoint bp : database.getBossCheckpoints())
		{
			goalCombo.addItem(new GoalOption("Boss: " + bp.getBossName(), database.createGoalFromBossCheckpoint(bp)));
		}

		// Landmark Quests
		for (Activity act : database.getAllActivities())
		{
			if (act.getCategory() == ActivityCategory.QUEST && (
				act.getTitle().contains("Elves") ||
				act.getTitle().contains("Treasure") ||
				act.getTitle().contains("Dragon Slayer") ||
				act.getTitle().contains("Disaster") ||
				act.getTitle().contains("Madness") ||
				act.getTitle().contains("Night at the Theatre")
			))
			{
				goalCombo.addItem(new GoalOption("Quest: " + act.getTitle(), database.createGoalFromActivity(act)));
			}
		}
	}

	public void setNorthStar(NorthStarGoal goal)
	{
		setNorthStar(goal, true);
	}

	public void setNorthStar(NorthStarGoal goal, boolean syncDropdown)
	{
		this.activeGoal = goal;

		// Persist active North Star goal
		if (plugin != null && plugin.getConfigManager() != null)
		{
			plugin.getConfigManager().setConfiguration("pathfinder", "activeNorthStar", goal != null ? goal.getId() : "");
		}

		if (syncDropdown)
		{
			if (goal == null)
			{
				goalCombo.setSelectedIndex(0);
			}
			else
			{
				boolean matched = false;
				for (int i = 0; i < goalCombo.getItemCount(); i++)
				{
					GoalOption opt = goalCombo.getItemAt(i);
					if (opt != null && opt.getGoal() != null && opt.getGoal().getId().equalsIgnoreCase(goal.getId()))
					{
						goalCombo.setSelectedIndex(i);
						matched = true;
						break;
					}
				}
				if (!matched)
				{
					// Add dynamic goal option (e.g. pinned directly from a card)
					GoalOption customOpt = new GoalOption("⭐ " + goal.getTitle(), goal);
					goalCombo.addItem(customOpt);
					goalCombo.setSelectedItem(customOpt);
				}
			}
		}

		if (lastProfile != null && activeGoal != null)
		{
			NorthStarProgress progress = engine.evaluateNorthStar(lastProfile, activeGoal);
			northStarCard.updateProgress(progress);
			this.currentEvaluations = engine.evaluateAll(lastProfile, activeGoal);
		}
		else if (lastProfile != null)
		{
			northStarCard.updateProgress(null);
			this.currentEvaluations = engine.evaluateAll(lastProfile, null);
		}
		else
		{
			northStarCard.updateProgress(null);
		}

		updateDisplay();
	}

	public void updateProfile(AccountProfile profile)
	{
		this.lastProfile = profile;
		this.currentEvaluations = engine.evaluateAll(profile, activeGoal);

		long readyCount = currentEvaluations.stream()
			.filter(r -> r.getStatus() == Recommendation.Status.READY)
			.count();

		long nearCount = currentEvaluations.stream()
			.filter(r -> r.getStatus() == Recommendation.Status.ALMOST_READY)
			.count();

		SwingUtilities.invokeLater(() -> {
			boolean isOffline = plugin == null || !plugin.isClientLoggedIn();

			if (profile.isBronzeman())
			{
				modeBadge.setText("🛡️ Bronzeman Mode (Combat " + profile.getCombatLevel() + ")");
				modeBadge.setForeground(BRONZE_COLOR);
			}
			else if (profile.isIronman())
			{
				modeBadge.setText("⚔️ Ironman Mode (Combat " + profile.getCombatLevel() + ")");
				modeBadge.setForeground(ColorScheme.BRAND_ORANGE);
			}
			else
			{
				modeBadge.setText("Combat " + profile.getCombatLevel() + " • Standard Account");
				modeBadge.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}

			statusSummary.setText(readyCount + " Activities Ready to Tackle");
			if (isOffline)
			{
				statusSummary.setToolTipText("Viewing cached offline profile for " + profile.getUsername()
					+ " (" + nearCount + " almost ready)");
			}
			else
			{
				statusSummary.setToolTipText(String.format("%d ready, %d almost ready", readyCount, nearCount));
			}

			if (profile.hasScannedBank())
			{
				BankSnapshot bs = profile.getBankSnapshot();
				bankSummary.setText(String.format("💰 %s GP  •  🏦 ~%s",
					BankSnapshot.formatGpValue(bs.getTotalLiquidCash()),
					BankSnapshot.formatGpValue(bs.getEstimatedTotalValue())));
				bankSummary.setForeground(new Color(241, 196, 15));
			}
			else
			{
				bankSummary.setText("💰 Open bank to scan wealth & gear");
				bankSummary.setForeground(new Color(170, 170, 170));
			}

			if (activeGoal != null)
			{
				NorthStarProgress progress = engine.evaluateNorthStar(profile, activeGoal);
				northStarCard.updateProgress(progress);
			}

			updateDisplay();
		});
	}

	private void updateDisplay()
	{
		cardsContainer.removeAll();

		if (currentEvaluations.isEmpty())
		{
			JLabel emptyLabel = new JLabel("<html><center style='color:#888;'><br/>Log into the game to<br/>view your pathfinder guide.</center></html>");
			emptyLabel.setFont(FontManager.getRunescapeSmallFont());
			emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			cardsContainer.add(emptyLabel);
			cardsContainer.revalidate();
			cardsContainer.repaint();
			return;
		}

		ActivityCategory selectedCategory = (ActivityCategory) categoryCombo.getSelectedItem();
		RecommendationEngine.SortOrder selectedSort = (RecommendationEngine.SortOrder) sortCombo.getSelectedItem();
		boolean hideLocked = hideLockedCheck.isSelected();
		boolean readyOnly = readyOnlyCheck.isSelected();
		boolean hiddenGemsOnly = hiddenGemsCheck.isSelected();
		boolean pathwaysOnly = northStarCard.isPathwaysOnlySelected();

		List<Recommendation> filtered = engine.filter(
			currentEvaluations,
			selectedCategory != null ? selectedCategory : ActivityCategory.ALL,
			readyOnly,
			hideLocked,
			hiddenGemsOnly,
			pathwaysOnly
		);

		engine.sort(filtered, selectedSort != null ? selectedSort : RecommendationEngine.SortOrder.SMART_SCORE);

		if (filtered.isEmpty())
		{
			String emptyMsg = pathwaysOnly
				? "<html><center style='color:#888;'><br/>No pathway activities match.<br/>Uncheck 'Show Pathways Only' to browse all.</center></html>"
				: "<html><center style='color:#888;'><br/>No activities match<br/>your current filters.</center></html>";
			JLabel noMatchLabel = new JLabel(emptyMsg);
			noMatchLabel.setFont(FontManager.getRunescapeSmallFont());
			noMatchLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			cardsContainer.add(noMatchLabel);
		}
		else
		{
			for (Recommendation rec : filtered)
			{
				RecommendationCard card = new RecommendationCard(rec, activeGoal, this::setNorthStar, database);
				cardsContainer.add(card);
			}
		}

		cardsContainer.revalidate();
		cardsContainer.repaint();
	}

	private void triggerSurprise()
	{
		if (currentEvaluations.isEmpty())
		{
			return;
		}

		Recommendation picked = engine.pickSurprise(currentEvaluations);
		if (picked == null)
		{
			return;
		}

		String[] options = {"Open Wiki Guide ↗", "Roll Again 🎲", "Close"};
		int choice = JOptionPane.showOptionDialog(
			this,
			String.format("🎲 Pathfinder Roulette Pick:\n\n%s\n%s\n\n%s",
				picked.getActivity().getTitle(),
				picked.getActivity().getUnlockHighlight(),
				picked.getActivity().getDescription()),
			"🎲 Pathfinder Roulette",
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			options,
			options[0]
		);

		if (choice == 0)
		{
			LinkBrowser.browse(picked.getActivity().getSafeWikiUrl());
		}
		else if (choice == 1)
		{
			triggerSurprise();
		}
	}

	private static class ScrollablePanel extends JPanel implements Scrollable
	{
		ScrollablePanel()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 48;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true; // Enforces child container strictly adheres to viewport visible safe width
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
