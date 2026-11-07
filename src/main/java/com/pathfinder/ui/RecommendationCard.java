package com.pathfinder.ui;

import com.pathfinder.model.Activity;
import com.pathfinder.model.Recommendation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

public class RecommendationCard extends JPanel
{
	private static final Color READY_COLOR = new Color(76, 175, 80);
	private static final Color READY_BG = new Color(20, 50, 22);

	private static final Color ALMOST_READY_COLOR = new Color(255, 179, 0);
	private static final Color ALMOST_READY_BG = new Color(55, 40, 10);

	private static final Color LOCKED_COLOR = new Color(229, 57, 53);
	private static final Color LOCKED_BG = new Color(50, 20, 20);

	private static final Color GOLD_HIGHLIGHT = new Color(241, 196, 15);

	public RecommendationCard(Recommendation rec)
	{
		Activity activity = rec.getActivity();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, getStatusBorderColor(rec.getStatus())),
			new EmptyBorder(7, 8, 7, 8)
		));

		// 1. Header Row (Title on left, Status badge on right)
		JPanel headerRow = new JPanel(new BorderLayout(4, 0));
		headerRow.setOpaque(false);
		headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

		JLabel titleLabel = new JLabel(activity.getTitle());
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		titleLabel.setToolTipText("Click to open OSRS Wiki guide");
		titleLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				LinkBrowser.browse(activity.getSafeWikiUrl());
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				titleLabel.setForeground(Color.WHITE);
			}
		});

		// Status Badge Pill
		JLabel statusBadge = new JLabel(" " + getStatusText(rec.getStatus()) + " ");
		statusBadge.setFont(FontManager.getRunescapeSmallFont());
		statusBadge.setForeground(getStatusBorderColor(rec.getStatus()));
		statusBadge.setBackground(getStatusBgColor(rec.getStatus()));
		statusBadge.setOpaque(true);
		statusBadge.setBorder(BorderFactory.createLineBorder(getStatusBorderColor(rec.getStatus()), 1));

		headerRow.add(titleLabel, BorderLayout.CENTER);
		headerRow.add(statusBadge, BorderLayout.EAST);
		add(headerRow);

		// 2. Unlock Highlight (if present)
		if (activity.getUnlockHighlight() != null && !activity.getUnlockHighlight().isEmpty())
		{
			add(Box.createRigidArea(new Dimension(0, 3)));
			JLabel unlockLabel = new JLabel("<html><body style='width: 190px; color: #f1c40f;'><b>★ "
				+ activity.getUnlockHighlight() + "</b></body></html>");
			unlockLabel.setFont(FontManager.getRunescapeSmallFont());
			unlockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			add(unlockLabel);
		}

		// 3. Description
		if (activity.getDescription() != null && !activity.getDescription().isEmpty())
		{
			add(Box.createRigidArea(new Dimension(0, 3)));
			JLabel descLabel = new JLabel("<html><body style='width: 190px; color: #b0b0b0; line-height: 1.15;'>"
				+ activity.getDescription() + "</body></html>");
			descLabel.setFont(FontManager.getRunescapeSmallFont());
			descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			add(descLabel);
		}

		// 4. Missing Requirements / Prerequisites (if any)
		if (!rec.getMissingRequirements().isEmpty())
		{
			add(Box.createRigidArea(new Dimension(0, 4)));

			JLabel reqTitle = new JLabel("Prerequisites:");
			reqTitle.setFont(FontManager.getRunescapeSmallFont());
			reqTitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			reqTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
			add(reqTitle);

			Color bulletColor = rec.getStatus() == Recommendation.Status.ALMOST_READY ? ALMOST_READY_COLOR : LOCKED_COLOR;
			for (String miss : rec.getMissingRequirements())
			{
				JLabel reqItem = new JLabel("<html><body style='width: 185px;'>• " + miss + "</body></html>");
				reqItem.setFont(FontManager.getRunescapeSmallFont());
				reqItem.setForeground(bulletColor);
				reqItem.setAlignmentX(Component.LEFT_ALIGNMENT);
				add(reqItem);
			}
		}

		// 5. Footer Row (Tags on left, Wiki Link on right)
		add(Box.createRigidArea(new Dimension(0, 5)));
		JPanel footerRow = new JPanel(new BorderLayout(4, 0));
		footerRow.setOpaque(false);
		footerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		footerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

		// Tags panel (first 2-3 tags to fit neatly)
		JPanel tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		tagPanel.setOpaque(false);
		int tagCount = 0;
		for (String tag : activity.getTags())
		{
			if (tagCount >= 3) break;
			JLabel tagBadge = new JLabel(" " + tag + " ");
			tagBadge.setFont(FontManager.getRunescapeSmallFont());
			tagBadge.setForeground(new Color(175, 175, 175));
			tagBadge.setBackground(ColorScheme.DARK_GRAY_COLOR);
			tagBadge.setOpaque(true);
			tagPanel.add(tagBadge);
			tagCount++;
		}

		// Wiki Link button
		JLabel wikiLink = new JLabel("Wiki ↗");
		wikiLink.setFont(FontManager.getRunescapeSmallFont());
		wikiLink.setForeground(new Color(130, 180, 255));
		wikiLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		wikiLink.setToolTipText("Open wiki: " + activity.getSafeWikiUrl());
		wikiLink.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				LinkBrowser.browse(activity.getSafeWikiUrl());
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				wikiLink.setForeground(Color.WHITE);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				wikiLink.setForeground(new Color(130, 180, 255));
			}
		});

		footerRow.add(tagPanel, BorderLayout.CENTER);
		footerRow.add(wikiLink, BorderLayout.EAST);
		add(footerRow);
	}

	private Color getStatusBorderColor(Recommendation.Status status)
	{
		switch (status)
		{
			case READY:
				return READY_COLOR;
			case ALMOST_READY:
				return ALMOST_READY_COLOR;
			case LOCKED:
			default:
				return LOCKED_COLOR;
		}
	}

	private Color getStatusBgColor(Recommendation.Status status)
	{
		switch (status)
		{
			case READY:
				return READY_BG;
			case ALMOST_READY:
				return ALMOST_READY_BG;
			case LOCKED:
			default:
				return LOCKED_BG;
		}
	}

	private String getStatusText(Recommendation.Status status)
	{
		switch (status)
		{
			case READY:
				return "READY";
			case ALMOST_READY:
				return "NEAR";
			case LOCKED:
			default:
				return "LOCKED";
		}
	}
}
