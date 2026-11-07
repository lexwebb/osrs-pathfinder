package com.pathfinder.ui;

import com.pathfinder.PathfinderPlugin;
import com.pathfinder.engine.RecommendationEngine;
import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.ActivityCategory;
import com.pathfinder.model.Recommendation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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

public class PathfinderPanel extends PluginPanel
{
	private final PathfinderPlugin plugin;
	private final RecommendationEngine engine;

	private final JLabel headerTitle = new JLabel("OSRS Pathfinder");
	private final JLabel statusSummary = new JLabel("Please log in to scan account");
	private final JComboBox<ActivityCategory> categoryCombo = new JComboBox<>(ActivityCategory.values());
	private final JCheckBox readyOnlyCheck = new JCheckBox("Ready to do now", false);
	private final JCheckBox hiddenGemsCheck = new JCheckBox("Hidden gems only", false);
	private final JButton surpriseBtn = new JButton("🎲 Surprise Me!");
	private final JButton refreshBtn = new JButton("🔄 Scan Now");

	private final JPanel cardsContainer = new JPanel();
	private List<Recommendation> currentEvaluations = new ArrayList<>();

	public PathfinderPanel(PathfinderPlugin plugin, RecommendationEngine engine)
	{
		super(false);
		this.plugin = plugin;
		this.engine = engine;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel topContainer = new JPanel();
		topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
		topContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
		topContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header
		headerTitle.setFont(FontManager.getRunescapeBoldFont());
		headerTitle.setForeground(ColorScheme.BRAND_ORANGE);
		headerTitle.setAlignmentX(LEFT_ALIGNMENT);

		statusSummary.setFont(FontManager.getRunescapeSmallFont());
		statusSummary.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusSummary.setAlignmentX(LEFT_ALIGNMENT);

		// Category Combo
		categoryCombo.setAlignmentX(LEFT_ALIGNMENT);
		categoryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		categoryCombo.addActionListener(e -> updateDisplay());

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

		topContainer.add(headerTitle);
		topContainer.add(Box.createRigidArea(new Dimension(0, 4)));
		topContainer.add(statusSummary);
		topContainer.add(Box.createRigidArea(new Dimension(0, 8)));
		topContainer.add(categoryCombo);
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
		boolean readyOnly = readyOnlyCheck.isSelected();
		boolean hiddenGemsOnly = hiddenGemsCheck.isSelected();

		List<Recommendation> filtered = engine.filter(currentEvaluations, selectedCategory, readyOnly, hiddenGemsOnly);

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
		if (picked != null)
		{
			JOptionPane.showMessageDialog(
				this,
				"<html><body style='width: 250px;'>" +
					"<h2>🎲 Next Adventure: " + picked.getActivity().getTitle() + "</h2>" +
					"<p><b>" + (picked.getActivity().getUnlockHighlight() != null ? picked.getActivity().getUnlockHighlight() : "") + "</b></p>" +
					"<p>" + picked.getActivity().getDescription() + "</p>" +
					"</body></html>",
				"Pathfinder Roulette",
				JOptionPane.INFORMATION_MESSAGE
			);
		}
	}
}
