package com.pathfinder.ui;

import com.pathfinder.model.Activity;
import com.pathfinder.model.Recommendation;
import java.awt.BorderLayout;
import java.awt.Color;
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
	private static final Color ALMOST_READY_COLOR = new Color(255, 167, 38);
	private static final Color LOCKED_COLOR = new Color(229, 57, 53);
	private static final Color UNLOCK_COLOR = new Color(241, 196, 15);
	private static final Color HOVER_COLOR = new Color(48, 48, 48);

	public RecommendationCard(Recommendation rec)
	{
		Activity activity = rec.getActivity();

		setLayout(new BorderLayout(5, 5));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 4, 0, 0, getStatusColor(rec.getStatus())),
			new EmptyBorder(8, 8, 8, 8)
		));

		// Top header: Title + Status badge + Wiki link button
		JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
		headerPanel.setOpaque(false);

		JLabel titleLabel = new JLabel("<html><u>" + activity.getTitle() + "</u></html>");
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		titleLabel.setToolTipText("Open OSRS Wiki article: " + activity.getSafeWikiUrl());
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

		JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		rightHeader.setOpaque(false);

		JLabel statusBadge = new JLabel(getStatusText(rec.getStatus()));
		statusBadge.setFont(FontManager.getRunescapeSmallFont());
		statusBadge.setForeground(getStatusColor(rec.getStatus()));

		JButton wikiBtn = new JButton("Wiki ↗");
		wikiBtn.setFont(FontManager.getRunescapeSmallFont());
		wikiBtn.setMargin(new java.awt.Insets(1, 4, 1, 4));
		wikiBtn.setFocusable(false);
		wikiBtn.setToolTipText("View guide on OSRS Wiki");
		wikiBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		wikiBtn.addActionListener(e -> LinkBrowser.browse(activity.getSafeWikiUrl()));

		rightHeader.add(statusBadge);
		rightHeader.add(wikiBtn);

		headerPanel.add(titleLabel, BorderLayout.CENTER);
		headerPanel.add(rightHeader, BorderLayout.EAST);

		// Center body: Highlight unlock & description
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setOpaque(false);

		if (activity.getUnlockHighlight() != null && !activity.getUnlockHighlight().isEmpty())
		{
			JLabel unlockLabel = new JLabel("<html><b>★ " + activity.getUnlockHighlight() + "</b></html>");
			unlockLabel.setFont(FontManager.getRunescapeSmallFont());
			unlockLabel.setForeground(UNLOCK_COLOR);
			unlockLabel.setBorder(new EmptyBorder(3, 0, 4, 0));
			contentPanel.add(unlockLabel);
		}

		JLabel descLabel = new JLabel("<html><body style='width: 175px; color: #a5a5a5;'>" + activity.getDescription() + "</body></html>");
		descLabel.setFont(FontManager.getRunescapeSmallFont());
		contentPanel.add(descLabel);

		// Missing requirements
		if (!rec.getMissingRequirements().isEmpty())
		{
			JPanel reqPanel = new JPanel();
			reqPanel.setLayout(new BoxLayout(reqPanel, BoxLayout.Y_AXIS));
			reqPanel.setOpaque(false);
			reqPanel.setBorder(new EmptyBorder(6, 0, 2, 0));

			JLabel reqHeader = new JLabel("Requirements / Prerequisites:");
			reqHeader.setFont(FontManager.getRunescapeSmallFont());
			reqHeader.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			reqPanel.add(reqHeader);

			for (String miss : rec.getMissingRequirements())
			{
				JLabel itemLabel = new JLabel("<html><body style='width: 175px;'>• " + miss + "</body></html>");
				itemLabel.setFont(FontManager.getRunescapeSmallFont());
				itemLabel.setForeground(rec.getStatus() == Recommendation.Status.ALMOST_READY ? ALMOST_READY_COLOR : LOCKED_COLOR);
				reqPanel.add(itemLabel);
			}
			contentPanel.add(reqPanel);
		}

		// Tags row
		if (!activity.getTags().isEmpty())
		{
			JPanel tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2));
			tagPanel.setOpaque(false);
			tagPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

			for (String tag : activity.getTags())
			{
				JLabel tagBadge = new JLabel(" " + tag + " ");
				tagBadge.setFont(FontManager.getRunescapeSmallFont());
				tagBadge.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				tagBadge.setBackground(ColorScheme.DARK_GRAY_COLOR);
				tagBadge.setOpaque(true);
				tagPanel.add(tagBadge);
			}
			contentPanel.add(tagPanel);
		}

		add(headerPanel, BorderLayout.NORTH);
		add(contentPanel, BorderLayout.CENTER);
	}

	private Color getStatusColor(Recommendation.Status status)
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

	private String getStatusText(Recommendation.Status status)
	{
		switch (status)
		{
			case READY:
				return "READY";
			case ALMOST_READY:
				return "NEAR UNLOCK";
			case LOCKED:
			default:
				return "LOCKED";
		}
	}
}
