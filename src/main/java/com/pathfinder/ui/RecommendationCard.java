package com.pathfinder.ui;

import com.pathfinder.model.Activity;
import com.pathfinder.model.Recommendation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
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

	private static final Color BRONZEMAN_COPPER = new Color(205, 127, 50);
	private static final Color BRONZEMAN_BG = new Color(50, 30, 15);

	// Target width for text wrapping strictly inside RuneLite's scrollable safe area (225 - 17 scrollbar - 16 padding)
	private static final String BODY_STYLE = "width: 172px; word-wrap: break-word;";

	public RecommendationCard(Recommendation rec)
	{
		Activity activity = rec.getActivity();

		setLayout(new DynamicGridLayout(0, 1, 0, 3));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, getStatusBorderColor(rec.getStatus())),
			new EmptyBorder(5, 6, 5, 5)
		));

		// 1. Header Row (Title on left, Status badge on right)
		JPanel headerRow = new JPanel(new BorderLayout(4, 0));
		headerRow.setOpaque(false);

		JLabel titleLabel = new JLabel("<html><body style='width: 118px; word-wrap: break-word;'><b>"
			+ activity.getTitle() + "</b></body></html>");
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
		JLabel statusBadge = new JLabel(" " + getStatusText(rec.getStatus()) + " ", javax.swing.SwingConstants.CENTER);
		statusBadge.setFont(FontManager.getRunescapeSmallFont());
		statusBadge.setForeground(getStatusBorderColor(rec.getStatus()));
		statusBadge.setBackground(getStatusBgColor(rec.getStatus()));
		statusBadge.setOpaque(true);
		statusBadge.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(getStatusBorderColor(rec.getStatus()), 1),
			new EmptyBorder(1, 3, 1, 3)
		));

		headerRow.add(titleLabel, BorderLayout.CENTER);
		headerRow.add(statusBadge, BorderLayout.EAST);
		add(headerRow);

		// 2. Unlock Highlight (if present)
		if (activity.getUnlockHighlight() != null && !activity.getUnlockHighlight().isEmpty())
		{
			JLabel unlockLabel = new JLabel("<html><body style='" + BODY_STYLE + " color: #f1c40f;'><b>★ "
				+ activity.getUnlockHighlight() + "</b></body></html>");
			unlockLabel.setFont(FontManager.getRunescapeSmallFont());
			add(unlockLabel);
		}

		// 3. Description
		if (activity.getDescription() != null && !activity.getDescription().isEmpty())
		{
			JLabel descLabel = new JLabel("<html><body style='" + BODY_STYLE + " color: #b0b0b0; line-height: 1.15;'>"
				+ activity.getDescription() + "</body></html>");
			descLabel.setFont(FontManager.getRunescapeSmallFont());
			add(descLabel);
		}

		// 4. Missing Requirements / Prerequisites (if any)
		if (!rec.getMissingRequirements().isEmpty())
		{
			JPanel reqPanel = new JPanel();
			reqPanel.setLayout(new BoxLayout(reqPanel, BoxLayout.Y_AXIS));
			reqPanel.setOpaque(false);

			JLabel reqTitle = new JLabel("Prerequisites:");
			reqTitle.setFont(FontManager.getRunescapeSmallFont());
			reqTitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			reqPanel.add(reqTitle);

			Color bulletColor = rec.getStatus() == Recommendation.Status.ALMOST_READY ? ALMOST_READY_COLOR : LOCKED_COLOR;
			for (String miss : rec.getMissingRequirements())
			{
				JLabel reqItem = new JLabel("<html><body style='" + BODY_STYLE + " color: " + toHexColor(bulletColor) + ";'>• " + miss + "</body></html>");
				reqItem.setFont(FontManager.getRunescapeSmallFont());
				reqPanel.add(reqItem);
			}
			add(reqPanel);
		}

		// 5. Footer Row (Tags on left, Wiki Link on right)
		JPanel footerRow = new JPanel(new BorderLayout(4, 0));
		footerRow.setOpaque(false);

		// Tags panel
		JPanel tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		tagPanel.setOpaque(false);
		int tagCount = 0;
		for (String tag : activity.getTags())
		{
			if (tagCount >= 2) break;
			JLabel tagBadge = new JLabel(" " + tag + " ");
			tagBadge.setFont(FontManager.getRunescapeSmallFont());

			if ("Bronzeman Unlock".equalsIgnoreCase(tag))
			{
				tagBadge.setForeground(BRONZEMAN_COPPER);
				tagBadge.setBackground(BRONZEMAN_BG);
				tagBadge.setBorder(BorderFactory.createLineBorder(BRONZEMAN_COPPER, 1));
			}
			else
			{
				tagBadge.setForeground(new Color(175, 175, 175));
				tagBadge.setBackground(ColorScheme.DARK_GRAY_COLOR);
			}

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

	private String toHexColor(Color c)
	{
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
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
