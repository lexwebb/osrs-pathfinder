package com.pathfinder.ui;

import com.pathfinder.PathfinderPlugin;
import com.pathfinder.engine.RecommendationEngine;
import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.BankSnapshot;
import com.pathfinder.model.Recommendation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
	private final PathfinderPlugin plugin;
	private final RecommendationEngine engine;

	private final JLabel headerTitle = new JLabel("OSRS Pathfinder");
	private final JLabel statusSummary = new JLabel("Please log in to scan account");
	private final JLabel bankSummary = new JLabel("Open bank to enable gear/GP recommendations");

	private final JComboBox<ActivityCategory> categoryCombo = new JComboBox<>(ActivityCategory.values());
	private final JComboBox<RecommendationEngine.SortOrder> sortCombo = new JComboBox<>(RecommendationEngine.SortOrder.values());

	private final JCheckBox readyOnlyCheck = new JCheckBox("Ready to do now", false);
	private final JCheckBox hiddenGemsCheck = new JCheckBox("Hidden gems only", false);

	private final JButton surpriseBtn = new JButton("🎲 Surprise Me!");
	private final JButton refreshBtn = new JButton("🔄 Scan");

	private final JPanel cardsContainer = new JPanel();
	private List<Recommendation> currentEvaluations = new ArrayList<>();
	private AccountProfile currentProfile;

	public PathfinderPanel(PathfinderPlugin plugin, RecommendationEngine engine)
	{
		super(false);
		this.plugin = plugin;
		this.engine = engine;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel topContainer = new JPanel();
		topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
		topContainer.setBorder(new EmptyBorder(10, 10, 8, 10));
		topContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header Title
		headerTitle.setFont(FontManager.getRunescapeBoldFont());
		headerTitle.setForeground(ColorScheme.BRAND_ORANGE);
		headerTitle.setAlignmentX(LEFT_ALIGNMENT);

		// Status summary
		statusSummary.setFont(FontManager.getRunescapeSmallFont());
		statusSummary.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusSummary.setAlignmentX(LEFT_ALIGNMENT);

		// Bank summary banner
		bankSummary.setFont(FontManager.getRunescapeSmallFont());
		bankSummary.setForeground(new Color(170, 170, 170));
		bankSummary.setAlignmentX(LEFT_ALIGNMENT);

		JPanel bankBanner = new JPanel(new BorderLayout());
		bankBanner.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bankBanner.setBorder(new EmptyBorder(4, 6, 4, 6));
		bankBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		bankBanner.setAlignmentX(LEFT_ALIGNMENT);
		bankBanner.add(bankSummary, BorderLayout.CENTER);

		// Category & Sort row
		categoryCombo.setAlignmentX(LEFT_ALIGNMENT);
		categoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		categoryCombo.addActionListener(e -> updateDisplay());

		sortCombo.setAlignmentX(LEFT_ALIGNMENT);
		sortCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		sortCombo.addActionListener(e -> updateDisplay());

		JLabel filterLabel = new JLabel("Category & Sorting:");
		filterLabel.setFont(FontManager.getRunescapeSmallFont());
		filterLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		filterLabel.setAlignmentX(LEFT_ALIGNMENT);

		// Toggles
		readyOnlyCheck.setOpaque(false);
		readyOnlyCheck.setForeground(Color.WHITE);
		readyOnlyCheck.setFont(FontManager.getRunescapeSmallFont());
		readyOnlyCheck.addActionListener(e -> updateDisplay());

		hiddenGemsCheck.setOpaque(false);
		hiddenGemsCheck.setForeground(new Color(241, 196, 15));
		hiddenGemsCheck.setFont(FontManager.getRunescapeSmallFont());
		hiddenGemsCheck.addActionListener(e -> updateDisplay());

		// Action buttons panel
		JPanel buttonRow = new JPanel(new BorderLayout(5, 0));
		buttonRow.setOpaque(false);
		buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

		surpriseBtn.setFont(FontManager.getRunescapeSmallFont());
		surpriseBtn.addActionListener(e -> triggerSurprise());

		refreshBtn.setFont(FontManager.getRunescapeSmallFont());
		refreshBtn.addActionListener(e -> plugin.requestAccountScan());

		buttonRow.add(surpriseBtn, BorderLayout.CENTER);
		buttonRow.add(refreshBtn, BorderLayout.EAST);

		// Assemble top panel
		topContainer.add(headerTitle);
		topContainer.add(Box.createRigidArea(new Dimension(0, 3)));
		topContainer.add(statusSummary);
		topContainer.add(Box.createRigidArea(new Dimension(0, 6)));
		topContainer.add(bankBanner);
		topContainer.add(Box.createRigidArea(new Dimension(0, 8)));
		topContainer.add(filterLabel);
		topContainer.add(Box.createRigidArea(new Dimension(0, 2)));
		topContainer.add(categoryCombo);
		topContainer.add(Box.createRigidArea(new Dimension(0, 4)));
		topContainer.add(sortCombo);
		topContainer.add(Box.createRigidArea(new Dimension(0, 4)));
		topContainer.add(readyOnlyCheck);
		topContainer.add(hiddenGemsCheck);
		topContainer.add(Box.createRigidArea(new Dimension(0, 8)));
		topContainer.add(buttonRow);

		// Scrollable cards list
		cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
		cardsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(cardsContainer, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(wrapper);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		add(topContainer, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	public void updateProfile(AccountProfile profile)
	{
		this.currentProfile = profile;
		this.currentEvaluations = engine.evaluateAll(profile);

		long readyCount = currentEvaluations.stream()
			.filter(r -> r.getStatus() == Recommendation.Status.READY)
			.count();
		long nearCount = currentEvaluations.stream()
			.filter(r -> r.getStatus() == Recommendation.Status.ALMOST_READY)
			.count();

		SwingUtilities.invokeLater(() -> {
			statusSummary.setText(String.format("Combat %d | %d Ready | %d Near Unlock",
				profile.getCombatLevel(), readyCount, nearCount));

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
				bankSummary.setText("Open bank to enable gear/GP recommendations");
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
			emptyLabel.setAlignmentX(CENTER_ALIGNMENT);
			cardsContainer.add(emptyLabel);
			cardsContainer.revalidate();
			cardsContainer.repaint();
			return;
		}

		ActivityCategory selectedCategory = (ActivityCategory) categoryCombo.getSelectedItem();
		RecommendationEngine.SortOrder selectedSort = (RecommendationEngine.SortOrder) sortCombo.getSelectedItem();
		boolean readyOnly = readyOnlyCheck.isSelected();
		boolean hiddenGemsOnly = hiddenGemsCheck.isSelected();

		List<Recommendation> filtered = engine.filter(currentEvaluations, selectedCategory, readyOnly, hiddenGemsOnly);
		engine.sort(filtered, selectedSort != null ? selectedSort : RecommendationEngine.SortOrder.SMART_SCORE);

		if (filtered.isEmpty())
		{
			JLabel noMatchLabel = new JLabel("<html><center style='color:#888;'><br/>No activities found for current filters.</center></html>");
			noMatchLabel.setFont(FontManager.getRunescapeSmallFont());
			noMatchLabel.setAlignmentX(CENTER_ALIGNMENT);
			cardsContainer.add(noMatchLabel);
		}
		else
		{
			for (Recommendation rec : filtered)
			{
				cardsContainer.add(new RecommendationCard(rec));
				cardsContainer.add(Box.createRigidArea(new Dimension(0, 8)));
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

		String[] options = {"Open Wiki Guide", "Roll Again 🎲", "Close"};
		int choice = JOptionPane.showOptionDialog(
			this,
			"<html><body style='width: 260px;'>" +
				"<h2 style='color:#f39c12; margin-bottom: 2px;'>🎲 Next Adventure</h2>" +
				"<h3 style='margin-top:0;'>" + picked.getActivity().getTitle() + "</h3>" +
				"<p style='color:#f1c40f;'><b>" + (picked.getActivity().getUnlockHighlight() != null ? picked.getActivity().getUnlockHighlight() : "") + "</b></p>" +
				"<p style='color:#ccc;'>" + picked.getActivity().getDescription() + "</p>" +
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
