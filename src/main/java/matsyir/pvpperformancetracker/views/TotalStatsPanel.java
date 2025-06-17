/*
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
package matsyir.pvpperformancetracker.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List; // Added import
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.Fighter;
import matsyir.pvpperformancetracker.models.FightLogEntry; // Added import
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

// basic panel with 3 rows to show a title, total fight performance stats, and kills/deaths
public class TotalStatsPanel extends JPanel
{
	private static final String WIKI_HELP_URL = "https://github.com/Matsyir/pvp-performance-tracker/wiki#pvp-performance-tracker";
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
	// number format for percentages
	private static final NumberFormat nfPercent = NumberFormat.getPercentInstance();
	static {
		nfPercent.setMaximumFractionDigits(1);
		nfPercent.setRoundingMode(RoundingMode.HALF_UP);
	}

	private static final int LAYOUT_ROWS_WITH_WARNING = 12; // Increased count to include Avg Robe Hits
	private static final int LAYOUT_ROWS_WITHOUT_WARNING = 11; // Increased count to include Avg Robe Hits

	// labels to be updated
	private final JLabel killsLabel;
	private final JLabel deathsLabel;
	private final JLabel offPrayStatsLabel;
	private final JLabel deservedDmgStatsLabel;
	private final JLabel dmgDealtStatsLabel;
	private final JLabel magicHitCountStatsLabel;
	private final JLabel offensivePrayCountStatsLabel;
	private final JLabel hpHealedStatsLabel;
	private final JLabel ghostBarrageStatsLabel;
	private final JLabel avgRobeHitsStatsLabel; // Added label for avg robe hits
	private final JLabel avgKoChanceStatsLabel; // Added label for avg KO chances

	private JLabel settingsWarningLabel; // to be hidden/shown

	private Fighter totalStats;

	private int numFights = 0;

	private int numKills = 0;
	private int numDeaths = 0;

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

	private double avgHpHealed = 0;

	// KO Chance totals/averages
	private double totalCompetitorKoChances = 0;
	private double totalOpponentKoChances = 0;
	private double totalCompetitorKoProbSum = 0; // Sum of overall KO probabilities from each fight
	private double totalOpponentKoProbSum = 0; // Sum of overall KO probabilities from each fight
	private double avgCompetitorKoChances = 0;
	private double avgOpponentKoChances = 0;
	private double avgCompetitorKoProb = 0; // Average overall KO probability per fight
	private double avgOpponentKoProb = 0; // Average overall KO probability per fight
	private int numFightsWithKoChance = 0; // Counter for fights that have KO chance data

	// Accumulators for robe hits
	private double totalCompetitorRobeHits = 0;
	private double totalCompetitorRobeHitsAttempted = 0; // 'total attacks attempted', i.e the statistic shows totalCompetitorRobeHits / totalCompetitorRobeHitsAttempted
	private double totalOpponentRobeHits = 0;
	private double totalOpponentRobeHitsAttempted = 0;
	private double avgCompetitorRobeHits = 0;
	private double avgCompetitorRobeHitsPercentage = 0;
	private double avgOpponentRobeHits = 0;
	private double avgOpponentRobeHitsPercentage = 0;

	// let's keep ghost barrage as the bottom-most statistic:
	// It's only relevant to people fighting in PvP Arena, and it's mostly only relevant
	// to people who can share their tracker with each-other - so pretty rarely useful.
	private double avgGhostBarrageCount = 0;
	private double avgGhostBarrageDeservedDamage = 0;

	public TotalStatsPanel()
	{
		totalStats = new Fighter("Player");

		setLayout(new GridLayout(CONFIG.settingsConfigured() ? LAYOUT_ROWS_WITHOUT_WARNING : LAYOUT_ROWS_WITH_WARNING, 1));
		setBorder(new EmptyBorder(4, 6, 4, 6));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Create contextMenu/popupMenu with various general actions
		JPopupMenu contextMenu = new JPopupMenu();
		// Create "View Wiki" URL popup menu/context menu item
		final JMenuItem viewWiki = new JMenuItem("<html><u>View Wiki</u>&nbsp;&#8599;</html>");
		viewWiki.addActionListener(e -> LinkBrowser.browse(WIKI_HELP_URL));
		viewWiki.setForeground(ColorScheme.GRAND_EXCHANGE_LIMIT);

		// Create "Reset All" popup menu/context menu item
		final JMenuItem removeAllFights = new JMenuItem("Remove All Fights");
		removeAllFights.addActionListener(e ->
		{
			int dialogResult = JOptionPane.showConfirmDialog(this, "Are you sure you want to reset all fight history data? This cannot be undone.", "Warning", JOptionPane.YES_NO_OPTION);
			if (dialogResult == JOptionPane.YES_OPTION)
			{
				PLUGIN.resetFightHistory();
			}
		});

		// Create "Configure Settings" popup menu/context menu item
		// TODO? Can't figure out how but would like to in the future. Esp. since there is a warning to setup config.
		//final JMenuItem configureSettings = new JMenuItem("Configure Settings");
		//configureSettings.addActionListener(e -> );

		// Create "Copy Fight History Data" popup menu/context menu item
		final JMenuItem exportFightHistory = new JMenuItem("Copy Fight History Data");
		exportFightHistory.addActionListener(e -> PLUGIN.exportFightHistory());

		// Create "Import Fight History Data" popup menu/context menu item
		final JMenuItem importFightHistory = new JMenuItem("Import Fight History Data");
		importFightHistory.addActionListener(e ->
		{
			// display a simple input dialog to request json data to import.
			String fightHistoryData = JOptionPane.showInputDialog(this, "Enter the fight history data you wish to import:", "Import Fight History", JOptionPane.INFORMATION_MESSAGE);

			// if the string is less than 2 chars, it is definitely invalid (or they pressed Cancel), so skip.
			if (fightHistoryData == null || fightHistoryData.length() < 2) { return; }

			PLUGIN.importUserFightHistoryData(fightHistoryData);
		});

		// Create "Fight Analysis (Advanced)" popup menu/context menu item
		final JMenuItem fightAnalysis = new JMenuItem("Fight Analysis (Advanced)");
		fightAnalysis.addActionListener(e -> new FightAnalysisFrame(TotalStatsPanel.this.getRootPane()));
		fightAnalysis.setForeground(ColorScheme.BRAND_ORANGE);

		contextMenu.add(viewWiki);
		contextMenu.add(removeAllFights);
		contextMenu.add(exportFightHistory);
		contextMenu.add(importFightHistory);
		contextMenu.add(fightAnalysis);
		setComponentPopupMenu(contextMenu);

		// Now initializing all lines:
		// FIRST LINE
		// basic label to display a title.
		JLabel titleLabel = new JLabel();
		titleLabel.setText("PvP Performance Tracker v" + PvpPerformanceTrackerPlugin.PLUGIN_VERSION);
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		titleLabel.setForeground(Color.WHITE);
		add(titleLabel);

		// if settings haven't been configured, add a red label to display that they should be.
		if (!CONFIG.settingsConfigured())
		{
			initializeSettingsWarningLabel();
			add(settingsWarningLabel);
		}

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
		killDeathPanel.setComponentPopupMenu(contextMenu);
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
		offPrayStatsPanel.setComponentPopupMenu(contextMenu);
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
		deservedDmgStatsPanel.setComponentPopupMenu(contextMenu);
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
		dmgDealtStatsPanel.setComponentPopupMenu(contextMenu);
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
		magicHitStatsPanel.setComponentPopupMenu(contextMenu);
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
		offensivePrayStatsPanel.setComponentPopupMenu(contextMenu);
		add(offensivePrayStatsPanel);

		// EIGTH LINE
		// panel to show the total hp healed
		JPanel hpHealedPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's avg hp healed stats
		JLabel hpHealedLeftLabel = new JLabel();
		hpHealedLeftLabel.setText("Avg HP Healed:");
		hpHealedLeftLabel.setForeground(Color.WHITE);
		hpHealedPanel.add(hpHealedLeftLabel, BorderLayout.WEST);

		// label to show hp healed stats
		hpHealedStatsLabel = new JLabel();
		hpHealedStatsLabel.setForeground(Color.WHITE);
		hpHealedPanel.add(hpHealedStatsLabel, BorderLayout.EAST);

		hpHealedPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		hpHealedPanel.setComponentPopupMenu(contextMenu);
		add(hpHealedPanel);

		// TENTH LINE: Avg Hits on Robes
		JPanel robeHitsStatsPanel = new JPanel(new BorderLayout());
		JLabel robeHitsStatsLeftLabel = new JLabel("Avg Hits on Robes:");
		robeHitsStatsLeftLabel.setForeground(Color.WHITE);
		robeHitsStatsPanel.add(robeHitsStatsLeftLabel, BorderLayout.WEST);
		avgRobeHitsStatsLabel = new JLabel();
		avgRobeHitsStatsLabel.setForeground(Color.WHITE);
		robeHitsStatsPanel.add(avgRobeHitsStatsLabel, BorderLayout.EAST);
		robeHitsStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		robeHitsStatsPanel.setComponentPopupMenu(contextMenu);
		add(robeHitsStatsPanel);

		// TENTH LINE (NEW)
		// panel to show the avg KO chance stats
		JPanel avgKoChanceStatsPanel = new JPanel(new BorderLayout());

		// left label
		JLabel avgKoChanceStatsLeftLabel = new JLabel();
		avgKoChanceStatsLeftLabel.setText("Avg KO Chances:");
		avgKoChanceStatsLeftLabel.setForeground(Color.WHITE);
		avgKoChanceStatsPanel.add(avgKoChanceStatsLeftLabel, BorderLayout.WEST);

		// right label (value)
		avgKoChanceStatsLabel = new JLabel();
		avgKoChanceStatsLabel.setForeground(Color.WHITE);
		avgKoChanceStatsPanel.add(avgKoChanceStatsLabel, BorderLayout.EAST);

		avgKoChanceStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		avgKoChanceStatsPanel.setComponentPopupMenu(contextMenu);
		add(avgKoChanceStatsPanel);

		// panel to show the avg ghost barrage stats
		JPanel ghostBarrageStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's avg ghost barrage stats
		JLabel ghostBarrageStatsLeftLabel = new JLabel();
		ghostBarrageStatsLeftLabel.setText("Avg Ghost Barrages:");
		ghostBarrageStatsLeftLabel.setForeground(Color.WHITE);
		ghostBarrageStatsPanel.add(ghostBarrageStatsLeftLabel, BorderLayout.WEST);

		ghostBarrageStatsLabel = new JLabel();
		ghostBarrageStatsLabel.setForeground(ColorScheme.BRAND_ORANGE);
		ghostBarrageStatsPanel.add(ghostBarrageStatsLabel, BorderLayout.EAST);
		ghostBarrageStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		ghostBarrageStatsPanel.setComponentPopupMenu(contextMenu);
		add(ghostBarrageStatsPanel);


		setLabels();

		setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, (int)getPreferredSize().getHeight()));
	}

	private void setLabels()
	{
		String avgDeservedDmgDiffOneDecimal = nf1.format(avgDeservedDmgDiff);
		String avgDmgDealtDiffOneDecimal = nf1.format(avgDmgDealtDiff);

		killsLabel.setText(nf.format(numKills) + " Kill" + (numKills != 1 ? "s" : ""));
		killsLabel.setToolTipText("From a total of " + numFights + " fights, you got " + nf.format(numKills)
			+ " kill" + (numKills != 1 ? "s" : ""));
		deathsLabel.setText(nf.format(numDeaths) + " Death" + (numDeaths != 1 ? "s" : ""));
		deathsLabel.setToolTipText("From a total of " + numFights + " fights, you died "
			+ (numDeaths != 1 ? (nf.format(numDeaths) + " times") : "once"));

		if (totalStats.getAttackCount() >= 10000)
		{
			offPrayStatsLabel.setText(nfWithK(totalStats.getOffPraySuccessCount()) + "/" +
				nfWithK(totalStats.getAttackCount()) + " (" +
				Math.round(totalStats.calculateOffPraySuccessPercentage()) + "%)");
		}
		else
		{
			offPrayStatsLabel.setText(totalStats.getOffPrayStats());
		}

		// put tooltip on parent JPanel so that you can hover anywhere on the line to get the tooltip,
		// rather than having to hover exactly on the statistic label
		((JPanel)offPrayStatsLabel.getParent()).setToolTipText(nf.format(totalStats.getOffPraySuccessCount()) + " successful off-pray attacks/" +
			nf.format(totalStats.getAttackCount()) + " total attacks (" +
			nf2.format(totalStats.calculateOffPraySuccessPercentage()) + "%)");

		deservedDmgStatsLabel.setText(nf.format(avgDeservedDmg) + " (" +
			(avgDeservedDmgDiff > 0 ? "+" : "") + avgDeservedDmgDiffOneDecimal + ")");
		((JPanel)deservedDmgStatsLabel.getParent()).setToolTipText("<html>Avg of " + nf1.format(avgDeservedDmg) +
			" deserved damage per fight with avg diff of " + (avgDeservedDmgDiff > 0 ? "+" : "") +
			avgDeservedDmgDiffOneDecimal + ".<br>On kills: " + nf1.format(killAvgDeservedDmg) +
			" (" + (killAvgDeservedDmgDiff > 0 ? "+" : "") + nf1.format(killAvgDeservedDmgDiff) +
			"), on deaths: " + nf1.format(deathAvgDeservedDmg) +
			" (" + (deathAvgDeservedDmgDiff > 0 ? "+" : "") + nf1.format(deathAvgDeservedDmgDiff) + ")</html>");

		dmgDealtStatsLabel.setText(nf.format(avgDmgDealt) + " (" +
			(avgDmgDealtDiff > 0 ? "+" : "") + avgDmgDealtDiffOneDecimal + ")");
		((JPanel)dmgDealtStatsLabel.getParent()).setToolTipText("<html>Avg of " + nf1.format(avgDmgDealt) +
			" damage per fight with avg diff of " + (avgDmgDealtDiff > 0 ? "+" : "") +
			avgDmgDealtDiffOneDecimal + ".<br>On kills: " + nf1.format(killAvgDmgDealt) +
			" (" + (killAvgDmgDealtDiff > 0 ? "+" : "") + nf1.format(killAvgDmgDealtDiff) +
			"), on deaths: " + nf1.format(deathAvgDmgDealt) +
			" (" + (deathAvgDmgDealtDiff > 0 ? "+" : "") + nf1.format(deathAvgDmgDealtDiff) + ")</html>");

		if (totalStats.getMagicHitCountDeserved() >= 10000)
		{
			magicHitCountStatsLabel.setText(nfWithK(totalStats.getMagicHitCount()) + "/" +
				nfWithK((int)totalStats.getMagicHitCountDeserved()));
		}
		else
		{
			magicHitCountStatsLabel.setText(totalStats.getMagicHitStats());
		}
		((JPanel)magicHitCountStatsLabel.getParent()).setToolTipText("<html>You successfully hit " +
			totalStats.getMagicHitCount() + " of " + totalStats.getMagicAttackCount() + " magic attacks, but deserved to hit " +
		nf1.format(totalStats.getMagicHitCountDeserved()) + ".<br>Luck percentage: 100% = expected hits, &gt;100% = lucky, &lt;100% = unlucky</html>");

		if (totalStats.getAttackCount() >= 10000)
		{
			offensivePrayCountStatsLabel.setText(nfWithK(totalStats.getOffensivePraySuccessCount()) + "/" +
				nfWithK(totalStats.getAttackCount()) + " (" +
				Math.round(totalStats.calculateOffensivePraySuccessPercentage()) + "%)");
		}
		else
		{
			offensivePrayCountStatsLabel.setText(totalStats.getOffensivePrayStats());
		}
		((JPanel)offensivePrayCountStatsLabel.getParent()).setToolTipText(nf.format(totalStats.getOffensivePraySuccessCount()) + " successful offensive prayers/" +
			nf.format(totalStats.getAttackCount()) + " total attacks (" +
			nf2.format(totalStats.calculateOffensivePraySuccessPercentage()) + "%)");

		hpHealedStatsLabel.setText(nf.format(avgHpHealed));
		((JPanel)hpHealedStatsLabel.getParent()).setToolTipText("A total of " + nf.format(totalStats.getHpHealed())
			+ " hitpoints were recovered, with an average of " + nf.format(avgHpHealed) + " HP per fight.");

		// Avg Hits on Robes label
		if (numFights > 0)
		{
			avgRobeHitsStatsLabel.setText(nf1.format(avgCompetitorRobeHits) + " / " + nf1.format(avgOpponentRobeHits));
			((JPanel)avgRobeHitsStatsLabel.getParent()).setToolTipText("<html>Average melee/range hits taken while wearing robes per fight:<br>" +
					"Player: " + nf1.format(avgCompetitorRobeHits) + " (" + nf1.format(avgCompetitorRobeHitsPercentage) + "% of melee/range hits taken were on robes)<br>" +
					"Opponent: " + nf1.format(avgOpponentRobeHits) + " (" + nf1.format(avgOpponentRobeHitsPercentage) + "% of melee/range hits taken were on robes)");
		}
		else
		{
			avgRobeHitsStatsLabel.setText("- / -");
			((JPanel)avgRobeHitsStatsLabel.getParent()).setToolTipText("No robe hits data available for calculation.");
		}

		// Set Avg KO Chance label
		if (numFightsWithKoChance > 0)
		{
			avgCompetitorKoChances = totalCompetitorKoChances / numFightsWithKoChance;
			avgOpponentKoChances = totalOpponentKoChances / numFightsWithKoChance;
			avgCompetitorKoProb = totalCompetitorKoProbSum / numFightsWithKoChance;
			avgOpponentKoProb = totalOpponentKoProbSum / numFightsWithKoChance;

			// too long of a line to include both chances & percent/sum, so only include those in tooltip
			avgKoChanceStatsLabel.setText(nf1.format(avgCompetitorKoChances) + " / " + nf1.format(avgOpponentKoChances));
			((JPanel)avgKoChanceStatsLabel.getParent())
				.setToolTipText("<html>Average KO Chances per fight:<br>Player: "
						+ nf1.format(avgCompetitorKoChances) + " (" + nfPercent.format(avgCompetitorKoProb)
						+ ")<br>Opponent: "
						+ nf1.format(avgOpponentKoChances) + " (" + nfPercent.format(avgOpponentKoProb)
						+ ")<br>Total KO Chances: Player: "
						+ nf.format(totalCompetitorKoChances) + ", Opponent: " + nf.format(totalOpponentKoChances)
						+ "</html>");
		}
		else
		{
			avgKoChanceStatsLabel.setText("- / -");
			((JPanel) avgKoChanceStatsLabel.getParent()).setToolTipText("No KO chance data available for calculation.");
		}

		ghostBarrageStatsLabel.setText(nf.format(avgGhostBarrageCount) + " G.B. (" + nf.format(avgGhostBarrageDeservedDamage) + ")");
		((JPanel)ghostBarrageStatsLabel.getParent()).setToolTipText("<html>You had an average of " + nf.format(avgGhostBarrageCount)
				+ " Ghost Barrages per fight, each worth an extra " + nf.format(avgGhostBarrageDeservedDamage)
				+ " deserved damage.<br>In total, you had " + totalStats.getGhostBarrageStats() + ".<br>"
				+ "Unless fighting in PvP Arena, your opponents likely had a similar value.");
	}

	// number format which adds K (representing 1,000) if the given number is over the threshold (10k),
	// with 1 decimal.
	// Ex. could turn 172,308 into 172.3k
	private String nfWithK(int number)
	{
		return nf1.format(number / 1000.0) + "k";
	}

	public void addFight(FightPerformance fight)
	{
		numFights++;

		totalStats.addAttacks(fight.getCompetitor().getOffPraySuccessCount(), fight.getCompetitor().getAttackCount(),
			fight.getCompetitor().getDeservedDamage(), fight.getCompetitor().getDamageDealt(),
			fight.getCompetitor().getMagicAttackCount(), fight.getCompetitor().getMagicHitCount(),
			fight.getCompetitor().getMagicHitCountDeserved(), fight.getCompetitor().getOffensivePraySuccessCount(),
			fight.getCompetitor().getHpHealed(), fight.getCompetitor().getGhostBarrageCount(),
			fight.getCompetitor().getGhostBarrageDeservedDamage());

		// Accumulate robe hits
		totalCompetitorRobeHits += fight.getCompetitor().getRobeHits();
		totalCompetitorRobeHitsAttempted += fight.getOpponent().getAttackCount() - fight.getOpponent().getTotalMagicAttackCount();
		totalOpponentRobeHits += fight.getOpponent().getRobeHits();
		totalOpponentRobeHitsAttempted += fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount();

		avgCompetitorRobeHits = totalCompetitorRobeHits / numFights;
		avgCompetitorRobeHitsPercentage = totalCompetitorRobeHitsAttempted != 0 ? (totalCompetitorRobeHits / totalCompetitorRobeHitsAttempted) * 100.0 : 0;
		avgOpponentRobeHits = totalOpponentRobeHits / numFights;
		avgOpponentRobeHitsPercentage = totalOpponentRobeHitsAttempted != 0 ? (totalOpponentRobeHits / totalOpponentRobeHitsAttempted) * 100.0 : 0;

		// add kill-specific or death-specific stats
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

		totalDeservedDmg += fight.getCompetitor().getDeservedDamage();
		totalDeservedDmgDiff += fight.getCompetitorDeservedDmgDiff();

		totalDmgDealt += fight.getCompetitor().getDamageDealt();
		totalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();

		// calculate avg stats based on total/numFights
		avgDeservedDmg = totalDeservedDmg / numFights;
		avgDeservedDmgDiff = totalDeservedDmgDiff / numFights;

		avgDmgDealt = totalDmgDealt / numFights;
		avgDmgDealtDiff = totalDmgDealtDiff / numFights;

		avgHpHealed = (double)totalStats.getHpHealed() / numFights;

		// Calculate KO chances & sum % for this fight and add to totals
		int fightCompetitorKoChances = 0;
		double fightCompetitorSurvivalProb = 1.0;
		int fightOpponentKoChances = 0;
		double fightOpponentSurvivalProb = 1.0;
		boolean fightHasKoData = false;
		List<FightLogEntry> logs = fight.getAllFightLogEntries();
		for (FightLogEntry log : logs) {
			Double koChance = log.getKoChance();
			if (koChance != null) {
				fightHasKoData = true; // Mark that this fight has KO data
				if (log.attackerName.equals(fight.getCompetitor().getName())) {
					fightCompetitorKoChances++;
					fightCompetitorSurvivalProb *= (1.0 - koChance);
				} else {
					fightOpponentKoChances++;
					fightOpponentSurvivalProb *= (1.0 - koChance);
				}
			}
		}

		// Only include this fight in KO averages if it had KO data
		if (fightHasKoData) {
			numFightsWithKoChance++;
			totalCompetitorKoChances += fightCompetitorKoChances;
			totalOpponentKoChances += fightOpponentKoChances;
			double fightCompetitorKoProb = (fightCompetitorKoChances > 0) ? (1.0 - fightCompetitorSurvivalProb) : 0.0;
			double fightOpponentKoProb = (fightOpponentKoChances > 0) ? (1.0 - fightOpponentSurvivalProb) : 0.0;
			totalCompetitorKoProbSum += fightCompetitorKoProb;
			totalOpponentKoProbSum += fightOpponentKoProb;
		}

		// Recalculate averages using the count of fights with data
		avgCompetitorKoChances = numFightsWithKoChance != 0 ? totalCompetitorKoChances / numFightsWithKoChance : 0;
		avgOpponentKoChances = numFightsWithKoChance != 0 ? totalOpponentKoChances / numFightsWithKoChance : 0;
		avgCompetitorKoProb = numFightsWithKoChance != 0 ? totalCompetitorKoProbSum / numFightsWithKoChance : 0;
		avgOpponentKoProb = numFightsWithKoChance != 0 ? totalOpponentKoProbSum / numFightsWithKoChance : 0;

		avgGhostBarrageCount = (double)totalStats.getGhostBarrageCount() / numFights;
		avgGhostBarrageDeservedDamage = totalStats.getGhostBarrageCount() != 0 ? totalStats.getGhostBarrageDeservedDamage() / totalStats.getGhostBarrageCount() : 0;

		SwingUtilities.invokeLater(this::setLabels);
	}

	public void addFights(ArrayList<FightPerformance> fights)
	{
		if (fights == null || fights.isEmpty()) { return; }

		numFights += fights.size();

		// Reset robe hits totals
		totalCompetitorRobeHits = 0;
		totalCompetitorRobeHitsAttempted = 0;
		totalOpponentRobeHits = 0;
		totalOpponentRobeHitsAttempted = 0;

		// Reset KO chance totals before recalculating for all fights
		totalCompetitorKoChances = 0;
		totalOpponentKoChances = 0;
		totalCompetitorKoProbSum = 0;
		totalOpponentKoProbSum = 0;
		numFightsWithKoChance = 0;

		for (FightPerformance fight : fights)
		{
			totalStats.addAttacks(fight.getCompetitor().getOffPraySuccessCount(), fight.getCompetitor().getAttackCount(),
				fight.getCompetitor().getDeservedDamage(), fight.getCompetitor().getDamageDealt(),
				fight.getCompetitor().getMagicAttackCount(), fight.getCompetitor().getMagicHitCount(),
				fight.getCompetitor().getMagicHitCountDeserved(), fight.getCompetitor().getOffensivePraySuccessCount(),
				fight.getCompetitor().getHpHealed(), fight.getCompetitor().getGhostBarrageCount(),
				fight.getCompetitor().getGhostBarrageDeservedDamage());

			// Accumulate robe hits
			totalCompetitorRobeHits += fight.getCompetitor().getRobeHits();
			totalCompetitorRobeHitsAttempted += fight.getOpponent().getAttackCount() - fight.getOpponent().getTotalMagicAttackCount();
			totalOpponentRobeHits += fight.getOpponent().getRobeHits();
			totalOpponentRobeHitsAttempted += fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount();

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

			totalDeservedDmg += fight.getCompetitor().getDeservedDamage();
			totalDeservedDmgDiff += fight.getCompetitorDeservedDmgDiff();

			totalDmgDealt += fight.getCompetitor().getDamageDealt();
			totalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();

			// Calculate KO chances & sum % for this fight and add to totals if applicable
			int fightCompetitorKoChances = 0;
			double fightCompetitorSurvivalProb = 1.0;
			int fightOpponentKoChances = 0;
			double fightOpponentSurvivalProb = 1.0;
			boolean fightHasKoData = false;
			List<FightLogEntry> logs = fight.getAllFightLogEntries();
			for (FightLogEntry log : logs) {
				Double koChance = log.getKoChance();
				if (koChance != null) {
					fightHasKoData = true;
					if (log.attackerName.equals(fight.getCompetitor().getName())) {
						fightCompetitorKoChances++;
						fightCompetitorSurvivalProb *= (1.0 - koChance);
					} else {
						fightOpponentKoChances++;
						fightOpponentSurvivalProb *= (1.0 - koChance);
					}
				}
			}
			if (fightHasKoData) {
				numFightsWithKoChance++;
				totalCompetitorKoChances += fightCompetitorKoChances;
				totalOpponentKoChances += fightOpponentKoChances;
				double fightCompetitorKoProb = (fightCompetitorKoChances > 0) ? (1.0 - fightCompetitorSurvivalProb) : 0.0;
				double fightOpponentKoProb = (fightOpponentKoChances > 0) ? (1.0 - fightOpponentSurvivalProb) : 0.0;
				totalCompetitorKoProbSum += fightCompetitorKoProb;
				totalOpponentKoProbSum += fightOpponentKoProb;
			}
		}

		// Calculate average robe hits
		avgCompetitorRobeHits = numFights != 0 ? totalCompetitorRobeHits / numFights : 0;
		avgCompetitorRobeHitsPercentage = totalCompetitorRobeHitsAttempted != 0 ? (totalCompetitorRobeHits / totalCompetitorRobeHitsAttempted) * 100.0 : 0;
		avgOpponentRobeHits = numFights != 0 ? totalOpponentRobeHits / numFights : 0;
		avgOpponentRobeHitsPercentage = totalOpponentRobeHitsAttempted != 0 ? (totalOpponentRobeHits / totalOpponentRobeHitsAttempted) * 100.0 : 0;

		// Recalculate averages for all stats
		avgDeservedDmg = numFights != 0 ? totalDeservedDmg / numFights : 0;
		avgDeservedDmgDiff = numFights != 0 ? totalDeservedDmgDiff / numFights: 0;

		avgDmgDealt = numFights != 0 ? totalDmgDealt / numFights : 0;
		avgDmgDealtDiff = numFights != 0 ? totalDmgDealtDiff / numFights : 0;

		killAvgDeservedDmg = numKills != 0 ? killTotalDeservedDmg / numKills : 0;
		killAvgDeservedDmgDiff = numKills != 0 ? killTotalDeservedDmgDiff / numKills : 0;

		deathAvgDeservedDmg = numDeaths != 0 ? deathTotalDeservedDmg / numDeaths : 0;
		deathAvgDeservedDmgDiff = numDeaths != 0 ? deathTotalDeservedDmgDiff / numDeaths : 0;

		killAvgDmgDealt = numKills != 0 ? killTotalDmgDealt / numKills : 0;
		killAvgDmgDealtDiff = numKills != 0 ? killTotalDmgDealtDiff / numKills : 0;

		deathAvgDmgDealt = numDeaths != 0 ? deathTotalDmgDealt / numDeaths : 0;
		deathAvgDmgDealtDiff = numDeaths != 0 ? deathTotalDmgDealtDiff / numDeaths : 0;

		avgHpHealed = numFights != 0 ? (double)totalStats.getHpHealed() / numFights : 0;

		// Recalculate KO averages using the count of fights with data
		avgCompetitorKoChances = numFightsWithKoChance != 0 ? totalCompetitorKoChances / numFightsWithKoChance : 0;
		avgOpponentKoChances = numFightsWithKoChance != 0 ? totalOpponentKoChances / numFightsWithKoChance : 0;
		avgCompetitorKoProb = numFightsWithKoChance != 0 ? totalCompetitorKoProbSum / numFightsWithKoChance : 0;
		avgOpponentKoProb = numFightsWithKoChance != 0 ? totalOpponentKoProbSum / numFightsWithKoChance : 0;

		avgGhostBarrageCount = numFights != 0 ? (double)totalStats.getGhostBarrageCount() / numFights : 0;
		avgGhostBarrageDeservedDamage = totalStats.getGhostBarrageCount() != 0 ? totalStats.getGhostBarrageDeservedDamage() / totalStats.getGhostBarrageCount() : 0;

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

		avgHpHealed = 0;

		totalCompetitorRobeHits = 0;
		totalCompetitorRobeHitsAttempted = 0;
		totalOpponentRobeHits = 0;
		totalOpponentRobeHitsAttempted = 0;
		avgCompetitorRobeHits = 0;
		avgCompetitorRobeHitsPercentage = 0;
		avgOpponentRobeHits = 0;
		avgOpponentRobeHitsPercentage = 0;

		// Reset KO chance stats
		totalCompetitorKoChances = 0;
		totalOpponentKoChances = 0;
		totalCompetitorKoProbSum = 0;
		totalOpponentKoProbSum = 0;
		avgCompetitorKoChances = 0;
		avgOpponentKoChances = 0;
		avgCompetitorKoProb = 0;
		avgOpponentKoProb = 0;
		numFightsWithKoChance = 0; // Reset new counter

		avgGhostBarrageCount = 0;
		avgGhostBarrageDeservedDamage = 0;

		totalStats = new Fighter("Player");
		SwingUtilities.invokeLater(this::setLabels);
	}

	public void setConfigWarning(boolean enable)
	{
		if (enable)
		{
			setLayout(new GridLayout(LAYOUT_ROWS_WITH_WARNING, 1));

			if (settingsWarningLabel == null)
			{
				initializeSettingsWarningLabel();
			}
			add(settingsWarningLabel, 1);
		}
		else
		{
			if (getComponentCount() > LAYOUT_ROWS_WITHOUT_WARNING)
			{
				remove(settingsWarningLabel);
				settingsWarningLabel = null;
			}
			setLayout(new GridLayout(LAYOUT_ROWS_WITHOUT_WARNING, 1));
		}

		validate();
	}

	private void initializeSettingsWarningLabel()
	{
		settingsWarningLabel = new JLabel();
		settingsWarningLabel.setText("Check plugin config for setup options!");
		settingsWarningLabel.setToolTipText("Please verify that the plugin options are configured according to your needs in the plugin's Configuration Panel.");
		settingsWarningLabel.setForeground(Color.RED);

		// make the warning font bold & smaller font size so we can fit more text.
		Font newFont = settingsWarningLabel.getFont();
		newFont = newFont.deriveFont(newFont.getStyle() | Font.BOLD, 12f);
		settingsWarningLabel.setFont(newFont);

		settingsWarningLabel.setHorizontalAlignment(SwingConstants.CENTER);
	}
}
