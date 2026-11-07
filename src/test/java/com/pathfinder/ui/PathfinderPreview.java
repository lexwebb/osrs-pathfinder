package com.pathfinder.ui;

import com.google.gson.Gson;
import com.pathfinder.PathfinderPlugin;
import com.pathfinder.engine.ActivityDatabase;
import com.pathfinder.engine.RecommendationEngine;
import com.pathfinder.model.AccountProfile;
import com.pathfinder.model.BankSnapshot;
import com.pathfinder.model.CombatCheckpoint;
import com.pathfinder.model.NorthStarGoal;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;

/**
 * Standalone preview runner for Pathfinder UI.
 * Launches immediately in <1 second without booting RuneLite client or game engine.
 */
public class PathfinderPreview
{
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(() -> {
			try
			{
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			}
			catch (Exception ignored)
			{
			}

			ActivityDatabase database = new ActivityDatabase();
			RecommendationEngine engine = new RecommendationEngine(database);

			// Mock Account Profile (BMMApollo, Combat 104, Bronzeman)
			Map<Skill, Integer> skills = new HashMap<>();
			for (Skill s : Skill.values())
			{
				skills.put(s, 70);
			}
			skills.put(Skill.ATTACK, 80);
			skills.put(Skill.STRENGTH, 82);
			skills.put(Skill.DEFENCE, 75);
			skills.put(Skill.SLAYER, 65);
			skills.put(Skill.PRAYER, 70);
			skills.put(Skill.MAGIC, 77);

			Set<Integer> owned = new HashSet<>();
			owned.add(28810); // Zombie Axe
			owned.add(10828); // Helm of Neitiznot
			owned.add(1704);  // Amulet of Glory
			owned.add(3105);  // Climbing Boots

			BankSnapshot bankSnapshot = BankSnapshot.builder()
				.timestamp(System.currentTimeMillis())
				.ownedItemIds(owned)
				.totalLiquidCash(4800000)
				.estimatedTotalValue(14600000)
				.build();

			Set<String> completedQuests = new HashSet<>();
			completedQuests.add("Lost City");
			completedQuests.add("Death Plateau");
			completedQuests.add("Dragon Slayer I");

			AccountProfile profile = AccountProfile.builder()
				.username("BMMApollo")
				.combatLevel(104)
				.totalLevel(1620)
				.isBronzeman(true)
				.skills(skills)
				.completedQuests(completedQuests)
				.bankSnapshot(bankSnapshot)
				.build();

			// Instantiate PathfinderPanel with null-safe mock plugin
			PathfinderPanel panel = new PathfinderPanel(null, engine);

			// Set active goal to Mid-Game Melee Setup
			CombatCheckpoint midMelee = database.getCombatCheckpoint("melee-mid");
			if (midMelee != null)
			{
				NorthStarGoal goal = database.createGoalFromCombatCheckpoint(midMelee);
				panel.setNorthStar(goal);
			}

			panel.updateProfile(profile);

			JFrame frame = new JFrame("Pathfinder UI Preview (Sidebar 225px)");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().setBackground(ColorScheme.DARK_GRAY_COLOR);
			frame.setLayout(new BorderLayout());
			frame.add(panel, BorderLayout.CENTER);

			// RuneLite standard sidebar width is strictly 225px
			panel.setPreferredSize(new Dimension(225, 750));
			panel.setMinimumSize(new Dimension(225, 750));
			panel.setMaximumSize(new Dimension(225, Integer.MAX_VALUE));

			frame.getContentPane().setPreferredSize(new Dimension(225, 750));
			frame.pack();
			frame.setResizable(false);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);

			// Render and save snapshot for immediate visual verification
			SwingUtilities.invokeLater(() -> {
				try
				{
					BufferedImage image = new BufferedImage(225, 750, BufferedImage.TYPE_INT_RGB);
					Graphics2D g2 = image.createGraphics();
					panel.setSize(new Dimension(225, 750));
					panel.doLayout();
					panel.paint(g2);
					g2.dispose();
					File outFile = new File("/home/lex/.gemini/antigravity-cli/brain/955b6d6c-7e1a-4995-855c-8d162ce0b828/preview.png");
					ImageIO.write(image, "png", outFile);
					System.out.println("Preview snapshot saved to: " + outFile.getAbsolutePath());
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			});
		});
	}
}
