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
package com.pvpperformancetracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

// basic panel with 3 rows to show a title, total fight performance stats, and kills/deaths
public class TotalStatsPanel extends JPanel
{
	private JLabel offPrayStatsLabel;
	private JLabel deservedDmgStatsLabel;
	private JLabel killsLabel;
	private JLabel deathsLabel;
	private Fighter totalStats;

	private int numKills = 0;
	private int numDeaths = 0;

	private int numFights = 0;

	private int totalDeservedDamage = 0;
	private int totalDeservedDamageDiff = 0;

	private double averageDeservedDamage = 0;
	private double averageDeservedDamageDiff = 0;

	TotalStatsPanel()
	{
		totalStats = new Fighter("Player");

		setLayout(new GridLayout(4, 1));
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// basic label to display a title.
		JLabel titleLabel = new JLabel();
		titleLabel.setText("PvP Performance Tracker");
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		titleLabel.setForeground(Color.WHITE);
		add(titleLabel);

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

		setLabels();
	}

	private void setLabels()
	{
		double avgDeservedDmgDiffOneDecimal = Math.round(averageDeservedDamageDiff * 10) / 10.0;

		killsLabel.setText(numKills + " Kill" + (numKills != 1 ? "s" : ""));
		deathsLabel.setText(numDeaths + " Death" + (numDeaths != 1 ? "s" : ""));

		if (totalStats.getAttackCount() >= 10000)
		{
			offPrayStatsLabel.setText(Math.round(totalStats.getSuccessCount() / 100.0) / 10.0 + "K/" +
				Math.round(totalStats.getAttackCount() / 100.0) / 10.0 + "(" +
				Math.round(totalStats.calculateSuccessPercentage()) + "%)");
		}
		else
		{
			offPrayStatsLabel.setText(totalStats.getOffPrayStats());
		}
		offPrayStatsLabel.setToolTipText(totalStats.getSuccessCount() + " successful off-pray attacks/" +
			totalStats.getAttackCount() + " total attacks (" +
			(Math.round(totalStats.calculateSuccessPercentage() * 100) / 100.0) + "%)");

		deservedDmgStatsLabel.setText((int)averageDeservedDamage + " (" +
			(avgDeservedDmgDiffOneDecimal > 0 ? "+" : "") + avgDeservedDmgDiffOneDecimal + ")");
		deservedDmgStatsLabel.setToolTipText("Average of " + Math.round(averageDeservedDamage * 10) / 10.0 +
			" deserved damage per fight with avg diff of " + (avgDeservedDmgDiffOneDecimal > 0 ? "+" : "") + avgDeservedDmgDiffOneDecimal);
	}

	public void addFight(FightPerformance fight)
	{
		numFights++;
		totalDeservedDamage += fight.getCompetitor().getTotalDamage();
		totalDeservedDamageDiff += fight.getCompetitorDeservedDmgDiff();

		averageDeservedDamage = (double)totalDeservedDamage / numFights;
		averageDeservedDamageDiff = (double)totalDeservedDamageDiff / numFights;

		if (fight.getCompetitor().isDead())
		{
			numDeaths++;
		}
		if (fight.getOpponent().isDead())
		{
			numKills++;
		}
		totalStats.addAttacks(fight.getCompetitor().getSuccessCount(), fight.getCompetitor().getAttackCount(), fight.getCompetitor().getTotalDamage());

		SwingUtilities.invokeLater(this::setLabels);
	}

	public void addFights(FightPerformance[] fights)
	{
		numFights += fights.length;
		for (FightPerformance fight : fights)
		{
			totalDeservedDamage += fight.getCompetitor().getTotalDamage();
			totalDeservedDamageDiff += fight.getCompetitorDeservedDmgDiff();

			if (fight.getCompetitor().isDead())
			{
				numDeaths++;
			}
			if (fight.getOpponent().isDead())
			{
				numKills++;
			}
			totalStats.addAttacks(fight.getCompetitor().getSuccessCount(), fight.getCompetitor().getAttackCount(), fight.getCompetitor().getTotalDamage());
		}

		averageDeservedDamage = (double) totalDeservedDamage / numFights;
		averageDeservedDamageDiff = (double) totalDeservedDamageDiff / numFights;

		SwingUtilities.invokeLater(this::setLabels);
	}

	public void reset()
	{
		numFights = 0;
		numDeaths = 0;
		numKills = 0;

		totalDeservedDamage = 0;
		totalDeservedDamageDiff = 0;

		averageDeservedDamage = 0;
		averageDeservedDamageDiff = 0;

		totalStats = new Fighter("Player");
		SwingUtilities.invokeLater(this::setLabels);
	}
}
