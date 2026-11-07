package com.pathfinder.ui;

import com.pathfinder.model.NorthStarGoal;
import com.pathfinder.model.NorthStarProgress;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.SwingUtil;

public class NorthStarCard extends JPanel
{
	private static final Color GOLD_ACCENT = new Color(241, 196, 15);
	private static final Color CARD_BG = new Color(30, 27, 20); // Subtle warm dark gold tint
	private static final Color PROGRESS_BAR_BG = new Color(45, 45, 45);

	private final Consumer<NorthStarGoal> onGoalCleared;
	private final Runnable onTogglePathways;

	private final JLabel goalTitleLabel = new JLabel();
	private final JLabel categoryLabel = new JLabel();
	private final JLabel highlightLabel = new JLabel();
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final JPanel gapsPanel = new JPanel();
	private final JCheckBox pathwayOnlyCheck = new JCheckBox("Show Pathways Only", false);
	private final JButton clearBtn = new JButton("✕");

	private NorthStarProgress currentProgress;

	public NorthStarCard(Consumer<NorthStarGoal> onGoalCleared, Runnable onTogglePathways)
	{
		this.onGoalCleared = onGoalCleared;
		this.onTogglePathways = onTogglePathways;

		setLayout(new DynamicGridLayout(0, 1, 0, 4));
		setBackground(CARD_BG);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, GOLD_ACCENT),
			new EmptyBorder(6, 7, 6, 7)
		));

		// 1. Header: [ ⭐ Title ] [ ✕ Clear ]
		JPanel headerRow = new JPanel(new BorderLayout(4, 0));
		headerRow.setOpaque(false);

		goalTitleLabel.setFont(FontManager.getRunescapeBoldFont());
		goalTitleLabel.setForeground(GOLD_ACCENT);

		clearBtn.setFont(FontManager.getRunescapeSmallFont());
		clearBtn.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		clearBtn.setToolTipText("Clear North Star Goal");
		clearBtn.setFocusable(false);
		clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		SwingUtil.removeButtonDecorations(clearBtn);
		clearBtn.addActionListener(e -> {
			if (onGoalCleared != null)
			{
				onGoalCleared.accept(null);
			}
		});

		headerRow.add(goalTitleLabel, BorderLayout.CENTER);
		headerRow.add(clearBtn, BorderLayout.EAST);
		add(headerRow);

		// 2. Subtitle: Category & Highlight
		categoryLabel.setFont(FontManager.getRunescapeSmallFont());
		categoryLabel.setForeground(new Color(180, 180, 180));
		add(categoryLabel);

		highlightLabel.setFont(FontManager.getRunescapeSmallFont());
		highlightLabel.setForeground(new Color(230, 230, 230));
		add(highlightLabel);

		// 3. Progress Bar
		progressBar.setStringPainted(true);
		progressBar.setFont(FontManager.getRunescapeSmallFont());
		progressBar.setForeground(GOLD_ACCENT);
		progressBar.setBackground(PROGRESS_BAR_BG);
		progressBar.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1));
		progressBar.setPreferredSize(new Dimension(180, 18));
		add(progressBar);

		// 4. Missing Gaps Chips
		gapsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 2));
		gapsPanel.setOpaque(false);
		add(gapsPanel);

		// 5. Toggle Row: [x] Show Pathways Only
		pathwayOnlyCheck.setFont(FontManager.getRunescapeSmallFont());
		pathwayOnlyCheck.setForeground(Color.WHITE);
		pathwayOnlyCheck.setOpaque(false);
		pathwayOnlyCheck.setFocusable(false);
		pathwayOnlyCheck.addActionListener(e -> {
			if (onTogglePathways != null)
			{
				onTogglePathways.run();
			}
		});
		add(pathwayOnlyCheck);

		setVisible(false); // Hidden until a goal is set
	}

	public boolean isPathwaysOnlySelected()
	{
		return pathwayOnlyCheck.isSelected();
	}

	public void updateProgress(NorthStarProgress progress)
	{
		this.currentProgress = progress;

		if (progress == null || progress.getGoal() == null)
		{
			setVisible(false);
			return;
		}

		NorthStarGoal goal = progress.getGoal();
		setVisible(true);

		goalTitleLabel.setText("⭐ " + goal.getTitle());
		categoryLabel.setText(goal.getCategory());

		if (goal.getHighlight() != null && !goal.getHighlight().isEmpty())
		{
			highlightLabel.setText("<html><body style='width: 170px; word-wrap: break-word; color: #f1c40f;'>★ "
				+ goal.getHighlight() + "</body></html>");
			highlightLabel.setVisible(true);
		}
		else
		{
			highlightLabel.setVisible(false);
		}

		progressBar.setValue(progress.getProgressPercent());
		progressBar.setString(progress.getProgressPercent() + "% Ready ("
			+ progress.getSatisfiedRequirements() + "/" + progress.getTotalRequirements() + ")");

		// Populate gap chips
		gapsPanel.removeAll();
		int chipsAdded = 0;

		// Skills
		for (NorthStarProgress.SkillGap gap : progress.getMissingSkills())
		{
			if (chipsAdded >= 4) break;
			String text = gap.getSkill().getName() + " " + gap.getCurrentLevel() + "/" + gap.getTargetLevel();
			gapsPanel.add(createGapChip(text, new Color(255, 179, 0), new Color(55, 40, 10)));
			chipsAdded++;
		}

		// Quests
		for (String q : progress.getMissingQuests())
		{
			if (chipsAdded >= 5) break;
			String text = "Quest: " + q;
			gapsPanel.add(createGapChip(text, new Color(229, 57, 53), new Color(50, 20, 20)));
			chipsAdded++;
		}

		// Items
		for (NorthStarProgress.ItemGap item : progress.getMissingItems())
		{
			if (chipsAdded >= 5) break;
			String text = "Gear: " + item.getItemName();
			gapsPanel.add(createGapChip(text, new Color(77, 208, 225), new Color(15, 45, 50)));
			chipsAdded++;
		}

		if (progress.isFullyReady())
		{
			JLabel completeLabel = new JLabel("✨ All requirements complete! Ready to tackle goal!");
			completeLabel.setFont(FontManager.getRunescapeSmallFont());
			completeLabel.setForeground(new Color(76, 175, 80));
			gapsPanel.add(completeLabel);
		}

		revalidate();
		repaint();
	}

	private JLabel createGapChip(String text, Color fg, Color bg)
	{
		JLabel chip = new JLabel(" " + text + " ");
		chip.setFont(FontManager.getRunescapeSmallFont());
		chip.setForeground(fg);
		chip.setBackground(bg);
		chip.setOpaque(true);
		chip.setBorder(BorderFactory.createLineBorder(fg, 1));
		return chip;
	}
}
