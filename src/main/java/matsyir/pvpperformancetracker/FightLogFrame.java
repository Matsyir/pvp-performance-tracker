/*
 * Copyright (c) 2021, Mazhar <https://twitter.com/maz_rs>
 * Copyright (c) 2021, Matsyir <https://github.com/Matsyir>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package matsyir.pvpperformancetracker;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.api.HeadIcon;
import net.runelite.api.SpriteID;
import net.runelite.client.util.ImageUtil;

public class FightLogFrame extends JFrame
{
	static Image frameIcon;
	private static final NumberFormat nf = NumberFormat.getInstance();

	static
	{
		frameIcon = new ImageIcon(ImageUtil.getResourceStreamFromClass(FightLogFrame.class, "/skull_red.png")).getImage();

		// initialize number format
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);
	}

	FightLogFrame(FightPerformance fight, JRootPane rootPane)
	{
		//String title = fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName();
		super(fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName());

		ArrayList<FightLogEntry> fightLogEntries = fight.getAllFightLogEntries();
		if (fightLogEntries == null || fightLogEntries.size() < 1)
		{
			PLUGIN.createConfirmationModal("Error", "There are no fight log entries available for this fight.");
			return;
		}

		fightLogEntries.removeIf(e -> !e.isFullEntry());

		// if always on top is supported, and the core RL plugin has "always on top" set, make the frame always
		// on top as well so it can be above the client.
		if (isAlwaysOnTopSupported())
		{
			setAlwaysOnTop(PLUGIN.getRuneliteConfig().gameAlwaysOnTop());
		}

		setIconImage(frameIcon);
		setSize(765, 503); // default to same as osrs on fixed
		setLocation(rootPane.getLocationOnScreen());

		JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
		Object[][] stats = new Object[fightLogEntries.size()][11];
		int i = 0;
		long initialTime = 0;

		for (FightLogEntry fightEntry : fightLogEntries)
		{
			if (i == 0)
			{
				initialTime = fightEntry.getTime();
			}
			int styleIcon;
			if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.RANGED)
			{
				styleIcon = SpriteID.SKILL_RANGED;
			}
			else if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC)
			{
				styleIcon = SpriteID.SKILL_MAGIC;
			}
			else
			{
				styleIcon = SpriteID.SKILL_ATTACK;
			}
			int prayIcon = 0;
			boolean noOverhead = false;
			if (fightEntry.getDefenderOverhead() == HeadIcon.RANGED)
			{
				prayIcon = SpriteID.PRAYER_PROTECT_FROM_MISSILES;
			}
			else if (fightEntry.getDefenderOverhead() == HeadIcon.MAGIC)
			{
				prayIcon = SpriteID.PRAYER_PROTECT_FROM_MAGIC;
			}
			else if (fightEntry.getDefenderOverhead() == HeadIcon.MELEE)
			{
				prayIcon = SpriteID.PRAYER_PROTECT_FROM_MELEE;
			}
			else
			{
				noOverhead = true;
			}

			BufferedImage styleIconRendered = PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(styleIcon, 0);
			stats[i][0] = fightEntry.getAttackerName();
			stats[i][1] = styleIconRendered;
			stats[i][2] = fightEntry.getHitRange();
			stats[i][3] = nf.format(fightEntry.getAccuracy() * 100) + '%';
			stats[i][4] = nf.format(fightEntry.getDeservedDamage());
			stats[i][5] = fightEntry.getAnimationData().isSpecial ? "✔" : "";
			stats[i][6] = fightEntry.success() ? "✔" : "";
			stats[i][7] = noOverhead ? "" : PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(prayIcon, 0);

			if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC)
			{
				int freezeIcon = fightEntry.isSplash() ? SpriteID.SPELL_ICE_BARRAGE_DISABLED : SpriteID.SPELL_ICE_BARRAGE;
				BufferedImage freezeIconRendered = PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(freezeIcon, 0);
				stats[i][8] = freezeIconRendered;
			}
			else
			{
				stats[i][8] = "";
			}

			// offensive pray shown as icon or blank if none(0)
			stats[i][9] = fightEntry.getAttackerOffensivePray() <= 0 ? "" :
				PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(fightEntry.getAttackerOffensivePray(), 0);

			long durationLong = fightEntry.getTime() - initialTime;
			Duration duration = Duration.ofMillis(durationLong);
			String time = String.format("%02d:%02d.%01d",
				duration.toMinutes(),
				duration.getSeconds() % 60,
				durationLong % 1000 / 100);
			stats[i][10] = time;

			i++;
		}

		String[] header = { "Attacker", "Style", "Hit Range", "Accuracy", "Avg Hit", "Special?", "Off-Pray?", "Def Prayer", "Splash", "Offensive Pray", "Time" };
		JTable table = new JTable(stats, header);
		table.setRowHeight(30);
		table.setDefaultEditor(Object.class, null);

		table.getColumnModel().getColumn(1).setCellRenderer(new BufferedImageCellRenderer());
		table.getColumnModel().getColumn(7).setCellRenderer(new BufferedImageCellRenderer());
		table.getColumnModel().getColumn(8).setCellRenderer(new BufferedImageCellRenderer());
		table.getColumnModel().getColumn(9).setCellRenderer(new BufferedImageCellRenderer());

		mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

		add(mainPanel);
		setVisible(true);
	}

	static class BufferedImageCellRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setText("");
			setIcon(value instanceof BufferedImage ? new ImageIcon((BufferedImage)value) : null);

			return this;
		}
	}
}
