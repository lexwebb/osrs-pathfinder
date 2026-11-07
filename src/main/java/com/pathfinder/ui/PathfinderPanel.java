package com.pathfinder.ui;

import com.pathfinder.PathfinderPlugin;
import com.pathfinder.engine.RecommendationEngine;
import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.BankSnapshot;
import com.pathfinder.model.Recommendation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

public class PathfinderPanel extends PluginPanel
{
	private static final Color BRONZE_COLOR = new Color(205, 127, 50);

	private final PathfinderPlugin plugin;
	private final RecommendationEngine engine;

	private final JLabel headerTitle = new JLabel("OSRS Pathfinder");
	private final JLabel modeBadge = new JLabel("");
	private final JLabel statusSummary = new JLabel("Log in to scan account");
	private final JLabel bankSummary = new JLabel("💰 Open bank to scan wealth & gear");

	private final JComboBox<ActivityCategory> categoryCombo = new JComboBox<>(ActivityCategory.values());
	private final JComboBox<RecommendationEngine.SortOrder> sortCombo = new JComboBox<>(RecommendationEngine.SortOrder.values());

	private final JCheckBox readyOnlyCheck = new JCheckBox("Ready now", false);
	private final JCheckBox hiddenGemsCheck = new JCheckBox("Hidden gems", false);

	private final JButton surpriseBtn = new JButton("🎲 Surprise Me!");
	private final JButton refreshBtn = new JButton("🔄 Scan");

	private final JPanel cardsContainer = new JPanel();
	private List<Recommendation> currentEvaluations = new ArrayList<>();

	public PathfinderPanel(PathfinderPlugin plugin, RecommendationEngine engine)
	{
		super(false);
		this.plugin = plugin;
		this.engine = engine;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Top Control Container
		JPanel topContainer = new JPanel();
		topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
		topContainer.setBorder(new EmptyBorder(8, 8, 6, 8));
		topContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		topContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

		// 1. Header Title Row with Mode Badge
		JPanel titleRow = new JPanel(new BorderLayout(4, 0));
		titleRow.setOpaque(false);
		titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

		headerTitle.setFont(FontManager.getRunescapeBoldFont());
		headerTitle.setForeground(ColorScheme.BRAND_ORANGE);

		modeBadge.setFont(FontManager.getRunescapeSmallFont());
		modeBadge.setForeground(BRONZE_COLOR);

		titleRow.add(headerTitle, BorderLayout.WEST);
		titleRow.add(modeBadge, BorderLayout.EAST);

		statusSummary.setFont(FontManager.getRunescapeSmallFont());
		statusSummary.setForeground(Color.WHITE);
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

		// Action Buttons Row: [ Surprise Me! ] [ Scan ]
		JPanel actionRow = new JPanel(new BorderLayout(4, 0));
		actionRow.setOpaque(false);
		actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

		surpriseBtn.setFont(FontManager.getRunescapeBoldFont());
		surpriseBtn.setForeground(new Color(241, 196, 15));
		surpriseBtn.setFocusable(false);
		surpriseBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		surpriseBtn.addActionListener(e -> triggerSurprise());

		refreshBtn.setFont(FontManager.getRunescapeSmallFont());
		refreshBtn.setFocusable(false);
		refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		refreshBtn.addActionListener(e -> plugin.requestAccountScan());

		actionRow.add(surpriseBtn, BorderLayout.CENTER);
		actionRow.add(refreshBtn, BorderLayout.EAST);

		// Category & Sort Dropdowns
		categoryCombo.setFont(FontManager.getRunescapeSmallFont());
		categoryCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
		categoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		categoryCombo.addActionListener(e -> updateDisplay());

		sortCombo.setFont(FontManager.getRunescapeSmallFont());
		sortCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
		sortCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		sortCombo.addActionListener(e -> updateDisplay());

		// Toggles Row (2 checkboxes side by side)
		JPanel toggleRow = new JPanel(new GridLayout(1, 2, 2, 0));
		toggleRow.setOpaque(false);
		toggleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		toggleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

		readyOnlyCheck.setOpaque(false);
		readyOnlyCheck.setForeground(Color.WHITE);
		readyOnlyCheck.setFont(FontManager.getRunescapeSmallFont());
		readyOnlyCheck.setFocusable(false);
		readyOnlyCheck.addActionListener(e -> updateDisplay());

		hiddenGemsCheck.setOpaque(false);
		hiddenGemsCheck.setForeground(new Color(241, 196, 15));
		hiddenGemsCheck.setFont(FontManager.getRunescapeSmallFont());
		hiddenGemsCheck.setFocusable(false);
		hiddenGemsCheck.addActionListener(e -> updateDisplay());

		toggleRow.add(readyOnlyCheck);
		toggleRow.add(hiddenGemsCheck);

		// Assemble Header
		topContainer.add(titleRow);
		topContainer.add(Box.createRigidArea(new Dimension(0, 2)));
		topContainer.add(statusSummary);
		topContainer.add(Box.createRigidArea(new Dimension(0, 4)));
		topContainer.add(bankBox);
		topContainer.add(Box.createRigidArea(new Dimension(0, 6)));
		topContainer.add(actionRow);
		topContainer.add(Box.createRigidArea(new Dimension(0, 6)));
		topContainer.add(categoryCombo);
		topContainer.add(Box.createRigidArea(new Dimension(0, 3)));
		topContainer.add(sortCombo);
		topContainer.add(Box.createRigidArea(new Dimension(0, 3)));
		topContainer.add(toggleRow);

		// Card Container inside ScrollPane
		cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
		cardsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		cardsContainer.setBorder(new EmptyBorder(4, 6, 6, 6));

		JPanel listWrapper = new JPanel(new BorderLayout());
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

	public void updateProfile(AccountProfile profile)
	{
		this.currentEvaluations = engine.evaluateAll(profile);

		long readyCount = currentEvaluations.stream()
			.filter(r -> r.getStatus() == Recommendation.Status.READY)
			.count();
		long nearCount = currentEvaluations.stream()
			.filter(r -> r.getStatus() == Recommendation.Status.ALMOST_READY)
			.count();

		SwingUtilities.invokeLater(() -> {
			statusSummary.setText(String.format("Combat %d • %d Ready • %d Near",
				profile.getCombatLevel(), readyCount, nearCount));

			if (profile.isBronzeman())
			{
				modeBadge.setText("🛡️ Bronzeman");
			}
			else if (profile.isIronman())
			{
				modeBadge.setText("🛡️ Ironman");
			}
			else
			{
				modeBadge.setText("");
			}

			if (profile.hasScannedBank())
			{
				BankSnapshot bs = profile.getBankSnapshot();
				bankSummary.setText(String.format("💰 Cash: %s | Bank: ~%s GP",
					BankSnapshot.formatGpValue(bs.getTotalLiquidCash()),
					BankSnapshot.formatGpValue(bs.getEstimatedTotalValue())));
				bankSummary.setForeground(new Color(241, 196, 15));
			}
			else
			{
				bankSummary.setText("💰 Open bank to scan wealth & gear");
				bankSummary.setForeground(new Color(170, 170, 170));
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
		boolean readyOnly = readyOnlyCheck.isSelected();
		boolean hiddenGemsOnly = hiddenGemsCheck.isSelected();

		List<Recommendation> filtered = engine.filter(
			currentEvaluations,
			selectedCategory != null ? selectedCategory : ActivityCategory.ALL,
			readyOnly,
			hiddenGemsOnly
		);

		engine.sort(filtered, selectedSort != null ? selectedSort : RecommendationEngine.SortOrder.SMART_SCORE);

		if (filtered.isEmpty())
		{
			JLabel noMatchLabel = new JLabel("<html><center style='color:#888;'><br/>No activities match<br/>your current filters.</center></html>");
			noMatchLabel.setFont(FontManager.getRunescapeSmallFont());
			noMatchLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			cardsContainer.add(noMatchLabel);
		}
		else
		{
			for (Recommendation rec : filtered)
			{
				RecommendationCard card = new RecommendationCard(rec);
				cardsContainer.add(card);
				cardsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
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
			"<html><body style='width: 250px; font-family: sans-serif;'>" +
				"<h2 style='color:#f39c12; margin-bottom: 2px;'>🎲 Next Adventure</h2>" +
				"<h3 style='margin-top:0; color:#ffffff;'>" + picked.getActivity().getTitle() + "</h3>" +
				"<p style='color:#f1c40f;'><b>" + (picked.getActivity().getUnlockHighlight() != null ? picked.getActivity().getUnlockHighlight() : "") + "</b></p>" +
				"<p style='color:#cccccc; line-height: 1.2;'>" + picked.getActivity().getDescription() + "</p>" +
				"</body></html>",
			"Pathfinder Roulette",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.PLAIN_MESSAGE,
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
}
