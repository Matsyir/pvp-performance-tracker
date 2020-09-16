/*
 * Copyright (c)  2020, Matsyir <https://github.com/Matsyir>
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
import java.awt.Color;
import java.awt.GridLayout;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.client.ui.ColorScheme;

// basic panel with 3 rows to show a title, total fight performance stats, and kills/deaths
public class TotalStatsPanel extends JPanel
{
	// number format for 0 decimal digit (mostly for commas in large numbers)
	private static final NumberFormat nf = NumberFormat.getInstance();
	static // initialize number format
	{
		nf.setMaximumFractionDigits(1);
		nf.setRoundingMode(RoundingMode.HALF_UP);
	}
	// number format for 1 decimal digit
	private static final NumberFormat nf1 = NumberFormat.getInstance();
	static // initialize number format
	{
		nf1.setMaximumFractionDigits(1);
		nf1.setRoundingMode(RoundingMode.HALF_UP);
	}

	// number format for 2 decimal digits
	private static final NumberFormat nf2 = NumberFormat.getInstance();
	static // initialize number format
	{
		nf2.setMaximumFractionDigits(2);
		nf2.setRoundingMode(RoundingMode.HALF_UP);
	}

	private JLabel killsLabel;
	private JLabel deathsLabel;
	private JLabel offPrayStatsLabel;
	private JLabel deservedDmgStatsLabel;
	private JLabel dmgDealtStatsLabel;
	private JLabel magicHitCountStatsLabel;
	private JLabel offensivePrayCountStatsLabel;

	private Fighter totalStats;

	private int numKills = 0;
	private int numDeaths = 0;

	private int numFights = 0;

	private double totalDeservedDmg = 0;
	private double totalDeservedDmgDiff = 0;
	private double avgDeservedDmg = 0;
	private double avgDeservedDmgDiff = 0;

	private double killTotalDeservedDmg = 0;
	private double killTotalDeservedDmgDiff = 0;
	private double killAvgDeservedDmg = 0;
	private double killAvgDeservedDmgDiff = 0;

	private double deathTotalDeservedDmg = 0;
	private double deathTotalDeservedDmgDiff = 0;
	private double deathAvgDeservedDmg = 0;
	private double deathAvgDeservedDmgDiff = 0;

	private double totalDmgDealt = 0;
	private double totalDmgDealtDiff = 0;
	private double avgDmgDealt = 0;
	private double avgDmgDealtDiff = 0;

	private double killTotalDmgDealt = 0;
	private double killTotalDmgDealtDiff = 0;
	private double killAvgDmgDealt = 0;
	private double killAvgDmgDealtDiff = 0;

	private double deathTotalDmgDealt = 0;
	private double deathTotalDmgDealtDiff = 0;
	private double deathAvgDmgDealt = 0;
	private double deathAvgDmgDealtDiff = 0;

	TotalStatsPanel()
	{
		totalStats = new Fighter("Player");

		setLayout(new GridLayout(7, 1));
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// FIRST LINE
		// basic label to display a title.
		JLabel titleLabel = new JLabel();
		titleLabel.setText("PvP Performance Tracker");
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		titleLabel.setForeground(Color.WHITE);
		add(titleLabel);

		// SECOND LINE
		// panel to show total kills/deaths
		JPanel killDeathPanel = new JPanel(new BorderLayout());

		// left label to show kills
		killsLabel = new JLabel();
		killsLabel.setText(numKills + " Kills");
		killsLabel.setForeground(Color.WHITE);
		killDeathPanel.add(killsLabel, BorderLayout.WEST);

		// right label to show deaths
		deathsLabel = new JLabel();
		deathsLabel.setText(numDeaths + " Deaths");
		deathsLabel.setForeground(Color.WHITE);
		killDeathPanel.add(deathsLabel, BorderLayout.EAST);

		killDeathPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(killDeathPanel);

		// THIRD LINE
		// panel to show the total off-pray stats (successful hits/total attacks)
		JPanel offPrayStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's off-pray stats
		JLabel leftLabel = new JLabel();
		leftLabel.setText("Total Off-Pray:");
		leftLabel.setForeground(Color.WHITE);
		offPrayStatsPanel.add(leftLabel, BorderLayout.WEST);

		// right shows off-pray stats
		offPrayStatsLabel = new JLabel();
		offPrayStatsLabel.setForeground(Color.WHITE);
		offPrayStatsPanel.add(offPrayStatsLabel, BorderLayout.EAST);

		offPrayStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(offPrayStatsPanel);

		// FOURTH LINE
		// panel to show the average deserved damage stats (average damage & average diff)
		JPanel deservedDmgStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's deserved dmg stats
		JLabel deservedDmgStatsLeftLabel = new JLabel();
		deservedDmgStatsLeftLabel.setText("Avg Deserved Dmg:");
		deservedDmgStatsLeftLabel.setForeground(Color.WHITE);
		deservedDmgStatsPanel.add(deservedDmgStatsLeftLabel, BorderLayout.WEST);

		// label to show deserved dmg stats
		deservedDmgStatsLabel = new JLabel();
		deservedDmgStatsLabel.setForeground(Color.WHITE);
		deservedDmgStatsPanel.add(deservedDmgStatsLabel, BorderLayout.EAST);

		deservedDmgStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(deservedDmgStatsPanel);

		// FIFTH LINE
		// panel to show the average damage dealt stats (average damage & average diff)
		JPanel dmgDealtStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's avg dmg dealt
		JLabel dmgDealtStatsLeftLabel = new JLabel();
		dmgDealtStatsLeftLabel.setText("Avg Damage Dealt:");
		dmgDealtStatsLeftLabel.setForeground(Color.WHITE);
		dmgDealtStatsPanel.add(dmgDealtStatsLeftLabel, BorderLayout.WEST);

		// label to show avg dmg dealt
		dmgDealtStatsLabel = new JLabel();
		dmgDealtStatsLabel.setForeground(Color.WHITE);
		dmgDealtStatsPanel.add(dmgDealtStatsLabel, BorderLayout.EAST);

		dmgDealtStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(dmgDealtStatsPanel);

		// SIXTH LINE
		// panel to show the total magic hit count and deserved hit count
		JPanel magicHitStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's magic hit count stats
		JLabel magicHitStatsLeftLabel = new JLabel();
		magicHitStatsLeftLabel.setText("Magic Luck:");
		magicHitStatsLeftLabel.setForeground(Color.WHITE);
		magicHitStatsPanel.add(magicHitStatsLeftLabel, BorderLayout.WEST);

		// label to show magic hit count stats
		magicHitCountStatsLabel = new JLabel();
		magicHitCountStatsLabel.setForeground(Color.WHITE);
		magicHitStatsPanel.add(magicHitCountStatsLabel, BorderLayout.EAST);

		magicHitStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(magicHitStatsPanel);

		// SEVENTH LINE
		// panel to show the offensive prayer success count
		JPanel offensivePrayStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's offensive pray stats
		JLabel offensivePrayStatsLeftLabel = new JLabel();
		offensivePrayStatsLeftLabel.setText("Offensive Pray:");
		offensivePrayStatsLeftLabel.setForeground(Color.WHITE);
		offensivePrayStatsPanel.add(offensivePrayStatsLeftLabel, BorderLayout.WEST);

		// label to show offensive pray stats
		offensivePrayCountStatsLabel = new JLabel();
		offensivePrayCountStatsLabel.setForeground(Color.WHITE);
		offensivePrayStatsPanel.add(offensivePrayCountStatsLabel, BorderLayout.EAST);

		offensivePrayStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(offensivePrayStatsPanel);

		JPopupMenu popupMenu = new JPopupMenu();

		// Create "Reset All" popup menu/context menu item
		final JMenuItem resetAllFights = new JMenuItem("Reset All");
		resetAllFights.addActionListener(e ->
		{
			int dialogResult = JOptionPane.showConfirmDialog(this, "Are you sure you want to reset all fight history data? This cannot be undone.", "Warning", JOptionPane.YES_NO_OPTION);
			if (dialogResult == JOptionPane.YES_OPTION)
			{
				PLUGIN.resetFightHistory();
			}
		});

		// Create "Export Fight History" popup menu/context menu item
		final JMenuItem exportFightHistory = new JMenuItem("Export Fight History");
		exportFightHistory.addActionListener(e -> PLUGIN.exportFightHistory());

		// Create "Import Fight History" popup menu/context menu item
		final JMenuItem importFightHistory = new JMenuItem("Import Fight History");
		importFightHistory.addActionListener(e ->
		{
			// display a simple input dialog to request json data to import.
			String fightHistoryData = JOptionPane.showInputDialog(this, "Enter the fight history data you wish to import:", "Import Fight History", JOptionPane.INFORMATION_MESSAGE);

			// if the string is less than 2 chars, it is definitely invalid (or they pressed Cancel), so skip.
			if (fightHistoryData == null || fightHistoryData.length() < 2) { return; }

			PLUGIN.importUserFightHistoryData(fightHistoryData);
		});

		popupMenu.add(resetAllFights);
		popupMenu.add(exportFightHistory);
		popupMenu.add(importFightHistory);
		setComponentPopupMenu(popupMenu);

		setLabels();
	}

	private void setLabels()
	{
		String avgDeservedDmgDiffOneDecimal = nf1.format(avgDeservedDmgDiff);
		String avgDmgDealtDiffOneDecimal = nf1.format(avgDmgDealtDiff);

		killsLabel.setText(nf.format(numKills) + " Kill" + (numKills != 1 ? "s" : ""));
		deathsLabel.setText(nf.format(numDeaths) + " Death" + (numDeaths != 1 ? "s" : ""));

		if (totalStats.getAttackCount() >= 10000)
		{
			offPrayStatsLabel.setText(nf1.format(totalStats.getOffPraySuccessCount() / 1000.0) + "K/" +
				nf1.format(totalStats.getAttackCount() / 1000.0) + "K (" +
				Math.round(totalStats.calculateOffPraySuccessPercentage()) + "%)");
		}
		else
		{
			offPrayStatsLabel.setText(totalStats.getOffPrayStats());
		}
		offPrayStatsLabel.setToolTipText(nf.format(totalStats.getOffPraySuccessCount()) + " successful off-pray attacks/" +
			nf.format(totalStats.getAttackCount()) + " total attacks (" +
			nf2.format(totalStats.calculateOffPraySuccessPercentage()) + "%)");

		deservedDmgStatsLabel.setText(nf.format(avgDeservedDmg) + " (" +
			(avgDeservedDmgDiff > 0 ? "+" : "") + avgDeservedDmgDiffOneDecimal + ")");
		deservedDmgStatsLabel.setToolTipText("Avg of " + nf1.format(avgDeservedDmg) +
			" deserved damage per fight with avg diff of " + (avgDeservedDmgDiff > 0 ? "+" : "") +
			avgDeservedDmgDiffOneDecimal + ". On kills: " + nf1.format(killAvgDeservedDmg) +
			" (" + (killAvgDeservedDmgDiff > 0 ? "+" : "") + nf1.format(killAvgDeservedDmgDiff) +
			"), on deaths: " + nf1.format(deathAvgDeservedDmg) +
			" (" + (deathAvgDeservedDmgDiff > 0 ? "+" : "") + nf1.format(deathAvgDeservedDmgDiff) + ")");

		dmgDealtStatsLabel.setText(nf.format(avgDmgDealt) + " (" +
			(avgDmgDealtDiff > 0 ? "+" : "") + avgDmgDealtDiffOneDecimal + ")");
		dmgDealtStatsLabel.setToolTipText("Avg of " + nf1.format(avgDmgDealt) +
			" damage per fight with avg diff of " + (avgDmgDealtDiff > 0 ? "+" : "") +
			avgDmgDealtDiffOneDecimal + ". On kills: " + nf1.format(killAvgDmgDealt) +
			" (" + (killAvgDmgDealtDiff > 0 ? "+" : "") + nf1.format(killAvgDmgDealtDiff) +
			"), on deaths: " + nf1.format(deathAvgDmgDealt) +
			" (" + (deathAvgDmgDealtDiff > 0 ? "+" : "") + nf1.format(deathAvgDmgDealtDiff) + ")");

		if (totalStats.getMagicHitCountDeserved() >= 10000)
		{
			magicHitCountStatsLabel.setText(nf1.format(totalStats.getMagicHitCount() / 1000.0) + "K/" +
				nf1.format(totalStats.getMagicHitCountDeserved() / 1000.0) + "K");
		}
		else
		{
			magicHitCountStatsLabel.setText(totalStats.getMagicHitStats());
		}
		magicHitCountStatsLabel.setToolTipText("You hit " + nf1.format(totalStats.getMagicHitCount()) +
			" magic attacks, but deserved to hit " + nf1.format(totalStats.getMagicHitCountDeserved()));

		if (totalStats.getAttackCount() >= 10000)
		{
			offensivePrayCountStatsLabel.setText(nf1.format(totalStats.getOffensivePraySuccessCount() / 1000.0) + "K/" +
				nf1.format(totalStats.getAttackCount() / 1000.0) + "K (" +
				Math.round(totalStats.calculateOffensivePraySuccessPercentage()) + "%)");
		}
		else
		{
			offensivePrayCountStatsLabel.setText(totalStats.getOffensivePrayStats());
		}
		offensivePrayCountStatsLabel.setToolTipText(nf.format(totalStats.getOffensivePraySuccessCount()) + " successful offensive prayers/" +
			nf.format(totalStats.getAttackCount()) + " total attacks (" +
			nf2.format(totalStats.calculateOffensivePraySuccessPercentage()) + "%)");
	}

	public void addFight(FightPerformance fight)
	{
		numFights++;

		totalDeservedDmg += fight.getCompetitor().getDeservedDamage();
		totalDeservedDmgDiff += fight.getCompetitorDeservedDmgDiff();

		totalDmgDealt += fight.getCompetitor().getDamageDealt();
		totalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();

		avgDeservedDmg = totalDeservedDmg / numFights;
		avgDeservedDmgDiff = totalDeservedDmgDiff / numFights;

		avgDmgDealt = totalDmgDealt / numFights;
		avgDmgDealtDiff = totalDmgDealtDiff / numFights;

		if (fight.getCompetitor().isDead())
		{
			numDeaths++;

			deathTotalDeservedDmg += fight.getCompetitor().getDeservedDamage();
			deathTotalDeservedDmgDiff += fight.getCompetitorDeservedDmgDiff();

			deathTotalDmgDealt += fight.getCompetitor().getDamageDealt();
			deathTotalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();

			deathAvgDeservedDmg = deathTotalDeservedDmg / numDeaths;
			deathAvgDeservedDmgDiff = deathTotalDeservedDmgDiff / numDeaths;

			deathAvgDmgDealt = deathTotalDmgDealt / numDeaths;
			deathAvgDmgDealtDiff = deathTotalDmgDealtDiff / numDeaths;
		}

		if (fight.getOpponent().isDead())
		{
			numKills++;

			killTotalDeservedDmg += fight.getCompetitor().getDeservedDamage();
			killTotalDeservedDmgDiff += fight.getCompetitorDeservedDmgDiff();

			killTotalDmgDealt += fight.getCompetitor().getDamageDealt();
			killTotalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();

			killAvgDeservedDmg = killTotalDeservedDmg / numKills;
			killAvgDeservedDmgDiff = killTotalDeservedDmgDiff / numKills;

			killAvgDmgDealt = killTotalDmgDealt / numKills;
			killAvgDmgDealtDiff = killTotalDmgDealtDiff / numKills;
		}

		totalStats.addAttacks(fight.getCompetitor().getOffPraySuccessCount(), fight.getCompetitor().getAttackCount(),
			fight.getCompetitor().getDeservedDamage(), fight.getCompetitor().getDamageDealt(),
			fight.getCompetitor().getMagicHitCount(), fight.getCompetitor().getMagicHitCountDeserved(),
			fight.getCompetitor().getOffensivePraySuccessCount());

		SwingUtilities.invokeLater(this::setLabels);
	}

	public void addFights(ArrayList<FightPerformance> fights)
	{
		numFights += fights.size();
		for (FightPerformance fight : fights)
		{
			totalDeservedDmg += fight.getCompetitor().getDeservedDamage();
			totalDeservedDmgDiff += fight.getCompetitorDeservedDmgDiff();

			totalDmgDealt += fight.getCompetitor().getDamageDealt();
			totalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();

			if (fight.getCompetitor().isDead())
			{
				numDeaths++;

				deathTotalDeservedDmg += fight.getCompetitor().getDeservedDamage();
				deathTotalDeservedDmgDiff += fight.getCompetitorDeservedDmgDiff();

				deathTotalDmgDealt += fight.getCompetitor().getDamageDealt();
				deathTotalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();
			}
			if (fight.getOpponent().isDead())
			{
				numKills++;

				killTotalDeservedDmg += fight.getCompetitor().getDeservedDamage();
				killTotalDeservedDmgDiff += fight.getCompetitorDeservedDmgDiff();

				killTotalDmgDealt += fight.getCompetitor().getDamageDealt();
				killTotalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();
			}
			totalStats.addAttacks(fight.getCompetitor().getOffPraySuccessCount(), fight.getCompetitor().getAttackCount(),
				fight.getCompetitor().getDeservedDamage(), fight.getCompetitor().getDamageDealt(),
				fight.getCompetitor().getMagicHitCount(), fight.getCompetitor().getMagicHitCountDeserved(),
				fight.getCompetitor().getOffensivePraySuccessCount());
		}

		avgDeservedDmg = totalDeservedDmg / numFights;
		avgDeservedDmgDiff = totalDeservedDmgDiff / numFights;

		avgDmgDealt = totalDmgDealt / numFights;
		avgDmgDealtDiff = totalDmgDealtDiff / numFights;

		killAvgDeservedDmg = killTotalDeservedDmg / numKills;
		killAvgDeservedDmgDiff = killTotalDeservedDmgDiff / numKills;

		deathAvgDeservedDmg = deathTotalDeservedDmg / numDeaths;
		deathAvgDeservedDmgDiff = deathTotalDeservedDmgDiff / numDeaths;

		killAvgDmgDealt = killTotalDmgDealt / numKills;
		killAvgDmgDealtDiff = killTotalDmgDealtDiff / numKills;

		deathAvgDmgDealt = deathTotalDmgDealt / numDeaths;
		deathAvgDmgDealtDiff = deathTotalDmgDealtDiff / numDeaths;

		SwingUtilities.invokeLater(this::setLabels);
	}

	public void reset()
	{
		numFights = 0;
		numDeaths = 0;
		numKills = 0;

		totalDeservedDmg = 0;
		totalDeservedDmgDiff = 0;
		killTotalDeservedDmg = 0;
		killTotalDeservedDmgDiff = 0;
		deathTotalDeservedDmg = 0;
		deathTotalDeservedDmgDiff = 0;
		totalDmgDealt = 0;
		totalDmgDealtDiff = 0;
		killTotalDmgDealt = 0;
		killTotalDmgDealtDiff = 0;
		deathTotalDmgDealt = 0;
		deathTotalDmgDealtDiff = 0;

		avgDeservedDmg = 0;
		avgDeservedDmgDiff = 0;
		killAvgDeservedDmg = 0;
		killAvgDeservedDmgDiff = 0;
		deathAvgDeservedDmg = 0;
		deathAvgDeservedDmgDiff = 0;
		avgDmgDealt = 0;
		avgDmgDealtDiff = 0;
		killAvgDmgDealt = 0;
		killAvgDmgDealtDiff = 0;
		deathAvgDmgDealt = 0;
		deathAvgDmgDealtDiff = 0;

		totalStats = new Fighter("Player");
		SwingUtilities.invokeLater(this::setLabels);
	}
}
