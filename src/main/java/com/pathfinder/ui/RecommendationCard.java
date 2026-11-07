package com.pathfinder.ui;

import com.pathfinder.model.Activity;
import com.pathfinder.model.Recommendation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

public class RecommendationCard extends JPanel
{
	private static final Color READY_COLOR = new Color(76, 175, 80);
	private static final Color ALMOST_READY_COLOR = new Color(255, 167, 38);
	private static final Color LOCKED_COLOR = new Color(229, 57, 53);
	private static final Color UNLOCK_COLOR = new Color(241, 196, 15);

	public RecommendationCard(Recommendation rec)
	{
		Activity activity = rec.getActivity();

		setLayout(new BorderLayout(5, 5));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, getStatusColor(rec.getStatus())),
			new EmptyBorder(8, 8, 8, 8)
		));

		// Top header: Title & Status badge
		JPanel headerPanel = new JPanel(new BorderLayout(5, 0));
		headerPanel.setOpaque(false);

		JLabel titleLabel = new JLabel(activity.getTitle());
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(Color.WHITE);

		JLabel statusBadge = new JLabel(getStatusText(rec.getStatus()));
		statusBadge.setFont(FontManager.getRunescapeSmallFont());
		statusBadge.setForeground(getStatusColor(rec.getStatus()));

		headerPanel.add(titleLabel, BorderLayout.CENTER);
		headerPanel.add(statusBadge, BorderLayout.EAST);

		// Center body: Highlight unlock & description
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setOpaque(false);

		if (activity.getUnlockHighlight() != null && !activity.getUnlockHighlight().isEmpty())
		{
			JLabel unlockLabel = new JLabel("<html><b>★ " + activity.getUnlockHighlight() + "</b></html>");
			unlockLabel.setFont(FontManager.getRunescapeSmallFont());
			unlockLabel.setForeground(UNLOCK_COLOR);
			unlockLabel.setBorder(new EmptyBorder(2, 0, 4, 0));
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

			JLabel reqHeader = new JLabel("Missing requirements:");
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
			tagPanel.setBorder(new EmptyBorder(4, 0, 0, 0));

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
