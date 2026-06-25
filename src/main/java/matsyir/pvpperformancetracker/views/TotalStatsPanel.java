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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.Fighter;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;

import matsyir.pvpperformancetracker.models.TrackedStatistic;
import matsyir.pvpperformancetracker.views.PanelFactory.ForwardingLabel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

// basic panel with 3 rows to show a title, total fight performance stats, and kills/deaths
@Slf4j
public class TotalStatsPanel extends JPanel
{
	private static final String WIKI_HELP_URL = "https://github.com/Matsyir/pvp-performance-tracker/wiki#pvp-performance-tracker-wiki";
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

	static
	{
		nfPercent.setMaximumFractionDigits(1);
		nfPercent.setRoundingMode(RoundingMode.HALF_UP);
	}

	private static final int LAYOUT_ROWS_WITH_WARNING = 12; // Increased count to include Avg Robe Hits
	private static final int LAYOUT_ROWS_WITHOUT_WARNING = 11; // Increased count to include Avg Robe Hits

	// labels to be updated
	private final ForwardingLabel killsLabel;
	private final ForwardingLabel deathsLabel;
	private final ForwardingLabel offPrayStatsLabel;
	private final ForwardingLabel expectedDmgStatsLabel;
	private final ForwardingLabel dmgDealtStatsLabel;
	private final ForwardingLabel magicHitCountStatsLabel;
	private final ForwardingLabel offensivePrayCountStatsLabel;
	private final ForwardingLabel hpHealedStatsLabel;
	private final ForwardingLabel ghostBarrageStatsLabel;
	private final ForwardingLabel avgRobeHitsStatsLabel; // Added label for avg robe hits
	private final ForwardingLabel avgKoChanceStatsLabel; // Added label for avg KO chances

	private ForwardingLabel settingsWarningLabel; // to be hidden/shown

	private Fighter totalStats;

	private int numFights = 0;

	private int numKills = 0;
	private int numDeaths = 0;

	private double totalExpectedDmg = 0;
	private double totalExpectedDmgDiff = 0;
	private double avgExpectedDmg = 0;
	private double avgExpectedDmgDiff = 0;

	private double killTotalExpectedDmg = 0;
	private double killTotalExpectedDmgDiff = 0;
	private double killAvgExpectedDmg = 0;
	private double killAvgExpectedDmgDiff = 0;

	private double deathTotalExpectedDmg = 0;
	private double deathTotalExpectedDmgDiff = 0;
	private double deathAvgExpectedDmg = 0;
	private double deathAvgExpectedDmgDiff = 0;

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
	private double avgGhostBarrageExpectedDamage = 0;

	private JPopupMenu popupMenu;
	private JMenuItem resetPvpHubAnonymousId;
	private final int pvpHupPopupMenuItemIdx = 1;
	private int popupMenuDefaultItemCount;
	private int popupMenuItemCountWithPvpHub;

	public TotalStatsPanel()
	{
		totalStats = new Fighter("Player");

		setLayout(new GridLayout(CONFIG.settingsConfigured() ? LAYOUT_ROWS_WITHOUT_WARNING : LAYOUT_ROWS_WITH_WARNING, 1));
		setBorder(new EmptyBorder(4, 6, 4, 6));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Create right click popup menu/context menu with various general actions
		popupMenu = new JPopupMenu();
		// Create "View Wiki" URL popup menu/context menu item
		final JMenuItem viewWiki = new JMenuItem("<html><u>View Wiki</u>&nbsp;&#8599;</html>");
		viewWiki.addActionListener(e -> LinkBrowser.browse(WIKI_HELP_URL));
		viewWiki.setForeground(ColorScheme.GRAND_EXCHANGE_LIMIT);

		// Create "Reset All" popup menu/context menu item
		final JMenuItem removeAllFights = new JMenuItem("Remove All Fights");
		removeAllFights.setForeground(Color.RED);
		removeAllFights.addActionListener(e ->
		{
			int dialogResult = JOptionPane.showConfirmDialog(this, "Are you sure you want to reset all fight history data? This cannot be undone.", "Warning", JOptionPane.YES_NO_OPTION);
			if (dialogResult == JOptionPane.YES_OPTION)
			{
				PLUGIN.resetFightHistory();
			}
		});

		// create "reset PvP-Hub hidden name" right click option. Only display this if opted-in + name hidden.
		resetPvpHubAnonymousId = new JMenuItem("<html>&#8635; Regenerate <u>PvP-Hub</u> Hidden Name");
		resetPvpHubAnonymousId.setForeground(new Color(30, 230, 30));
		resetPvpHubAnonymousId.addActionListener(e ->
		{
			int dialogResult = JOptionPane.showConfirmDialog(this, "Are you sure you want " +
					"to regenerate your Hidden Name for PvP-Hub? You will not be able to go back to your current " +
					"hidden name. Existing fights will remain uploaded using your previous hidden name.",
				"Warning", JOptionPane.YES_NO_OPTION);
			if (dialogResult == JOptionPane.YES_OPTION)
			{
				PLUGIN.resetPvpHubHiddenName();
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
			if (fightHistoryData == null || fightHistoryData.length() < 2)
			{
				return;
			}

			PLUGIN.importUserFightHistoryData(fightHistoryData);
		});

		popupMenu.add(viewWiki);
		// popupMenuWithPvpHubOptions.add(resetPvpHubAnonymousId);
		popupMenu.add(exportFightHistory);
		popupMenu.add(importFightHistory);
		popupMenu.add(removeAllFights);
		popupMenuDefaultItemCount = popupMenu.getComponentCount();
		popupMenuItemCountWithPvpHub = popupMenuDefaultItemCount + 1;

		// Now initializing all lines:
		// FIRST LINE
		// basic label to display a title.
		ForwardingLabel titleLabel = new ForwardingLabel();
		titleLabel.setText("PvP Performance Tracker v" + PvpPerformanceTrackerPlugin.PLUGIN_VERSION);
		titleLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BRAND_ORANGE));
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
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
		killDeathPanel.setInheritsPopupMenu(true);

		// left label to show kills
		killsLabel = new ForwardingLabel();
		killsLabel.setText(numKills + " Kills");
		killsLabel.setForeground(Color.WHITE);
		killsLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		killDeathPanel.add(killsLabel, BorderLayout.WEST);

		// right label to show deaths
		deathsLabel = new ForwardingLabel();
		deathsLabel.setText(numDeaths + " Deaths");
		deathsLabel.setForeground(Color.WHITE);
		deathsLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		killDeathPanel.add(deathsLabel, BorderLayout.EAST);
		add(killDeathPanel);

		// THIRD LINE
		// panel to show the total off-pray stats (successful hits/total attacks)
		JPanel offPrayStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's off-pray stats
		ForwardingLabel leftLabel = new ForwardingLabel();
		leftLabel.setText("Total Off-Pray:");
		leftLabel.setForeground(Color.WHITE);
		offPrayStatsPanel.add(leftLabel, BorderLayout.WEST);

		// right shows off-pray stats
		offPrayStatsLabel = new ForwardingLabel();
		offPrayStatsLabel.setForeground(Color.WHITE);
		offPrayStatsPanel.add(offPrayStatsLabel, BorderLayout.EAST);
		add(offPrayStatsPanel);

		// FOURTH LINE
		// panel to show the average expected damage stats (average damage & average diff)
		JPanel expectedDmgStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's expected dmg stats
		ForwardingLabel expectedDmgStatsLeftLabel = new ForwardingLabel();
		expectedDmgStatsLeftLabel.setText("Avg Expected Dmg:");
		expectedDmgStatsLeftLabel.setForeground(Color.WHITE);
		expectedDmgStatsLeftLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		expectedDmgStatsPanel.add(expectedDmgStatsLeftLabel, BorderLayout.WEST);

		// label to show expected dmg stats
		expectedDmgStatsLabel = new ForwardingLabel();
		expectedDmgStatsLabel.setForeground(Color.WHITE);
		expectedDmgStatsLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		expectedDmgStatsPanel.add(expectedDmgStatsLabel, BorderLayout.EAST);
		add(expectedDmgStatsPanel);

		// FIFTH LINE
		// panel to show the average damage dealt stats (average damage & average diff)
		JPanel dmgDealtStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's avg dmg dealt
		ForwardingLabel dmgDealtStatsLeftLabel = new ForwardingLabel();
		dmgDealtStatsLeftLabel.setText("Avg Damage Dealt:");
		dmgDealtStatsLeftLabel.setForeground(Color.WHITE);
		dmgDealtStatsLeftLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		dmgDealtStatsPanel.add(dmgDealtStatsLeftLabel, BorderLayout.WEST);

		// label to show avg dmg dealt
		dmgDealtStatsLabel = new ForwardingLabel();
		dmgDealtStatsLabel.setForeground(Color.WHITE);
		dmgDealtStatsPanel.add(dmgDealtStatsLabel, BorderLayout.EAST);

		add(dmgDealtStatsPanel);

		// SIXTH LINE
		// panel to show the total magic hit count and expected hit count
		JPanel magicHitStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's magic hit count stats
		ForwardingLabel magicHitStatsLeftLabel = new ForwardingLabel();
		magicHitStatsLeftLabel.setText("Magic Luck:");
		magicHitStatsLeftLabel.setForeground(Color.WHITE);
		magicHitStatsLeftLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		magicHitStatsPanel.add(magicHitStatsLeftLabel, BorderLayout.WEST);

		// label to show magic hit count stats
		magicHitCountStatsLabel = new ForwardingLabel();
		magicHitCountStatsLabel.setForeground(Color.WHITE);
		magicHitCountStatsLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		magicHitStatsPanel.add(magicHitCountStatsLabel, BorderLayout.EAST);

		add(magicHitStatsPanel);

		// SEVENTH LINE
		// panel to show the offensive prayer success count
		JPanel offensivePrayStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's offensive pray stats
		ForwardingLabel offensivePrayStatsLeftLabel = new ForwardingLabel();
		offensivePrayStatsLeftLabel.setText("Offensive Pray:");
		offensivePrayStatsLeftLabel.setForeground(Color.WHITE);
		offensivePrayStatsLeftLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		offensivePrayStatsPanel.add(offensivePrayStatsLeftLabel, BorderLayout.WEST);

		// label to show offensive pray stats
		offensivePrayCountStatsLabel = new ForwardingLabel();
		offensivePrayCountStatsLabel.setForeground(Color.WHITE);
		offensivePrayCountStatsLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		offensivePrayStatsPanel.add(offensivePrayCountStatsLabel, BorderLayout.EAST);

		add(offensivePrayStatsPanel);

		// EIGTH LINE
		// panel to show the total hp healed
		JPanel hpHealedPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's avg hp healed stats
		ForwardingLabel hpHealedLeftLabel = new ForwardingLabel();
		hpHealedLeftLabel.setText("Avg HP Healed:");
		hpHealedLeftLabel.setForeground(Color.WHITE);
		hpHealedLeftLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		hpHealedPanel.add(hpHealedLeftLabel, BorderLayout.WEST);

		// label to show hp healed stats
		hpHealedStatsLabel = new ForwardingLabel();
		hpHealedStatsLabel.setForeground(Color.WHITE);
		hpHealedPanel.add(hpHealedStatsLabel, BorderLayout.EAST);

		add(hpHealedPanel);

		// TENTH LINE: Avg Hits on Robes
		JPanel robeHitsStatsPanel = new JPanel(new BorderLayout());
		ForwardingLabel robeHitsStatsLeftLabel = new ForwardingLabel("Avg Hits on Robes:");
		robeHitsStatsLeftLabel.setForeground(Color.WHITE);
		robeHitsStatsPanel.add(robeHitsStatsLeftLabel, BorderLayout.WEST);
		avgRobeHitsStatsLabel = new ForwardingLabel();
		avgRobeHitsStatsLabel.setForeground(Color.WHITE);
		robeHitsStatsPanel.add(avgRobeHitsStatsLabel, BorderLayout.EAST);
		robeHitsStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(robeHitsStatsPanel);

		// TENTH LINE (NEW)
		// panel to show the avg KO chance stats
		JPanel avgKoChanceStatsPanel = new JPanel(new BorderLayout());

		// left label
		ForwardingLabel avgKoChanceStatsLeftLabel = new ForwardingLabel();
		avgKoChanceStatsLeftLabel.setText("Avg KO Chances:");
		avgKoChanceStatsLeftLabel.setForeground(Color.WHITE);
		avgKoChanceStatsPanel.add(avgKoChanceStatsLeftLabel, BorderLayout.WEST);

		// right label (value)
		avgKoChanceStatsLabel = new ForwardingLabel();
		avgKoChanceStatsLabel.setForeground(Color.WHITE);
		avgKoChanceStatsPanel.add(avgKoChanceStatsLabel, BorderLayout.EAST);

		avgKoChanceStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(avgKoChanceStatsPanel);

		// panel to show the avg ghost barrage stats
		JPanel ghostBarrageStatsPanel = new JPanel(new BorderLayout());

		// left label with a label to say it's avg ghost barrage stats
		ForwardingLabel ghostBarrageStatsLeftLabel = new ForwardingLabel();
		ghostBarrageStatsLeftLabel.setText("Avg Ghost Barrages:");
		ghostBarrageStatsLeftLabel.setForeground(Color.WHITE);
		ghostBarrageStatsPanel.add(ghostBarrageStatsLeftLabel, BorderLayout.WEST);

		ghostBarrageStatsLabel = new ForwardingLabel();
		ghostBarrageStatsLabel.setForeground(ColorScheme.BRAND_ORANGE);
		ghostBarrageStatsPanel.add(ghostBarrageStatsLabel, BorderLayout.EAST);
		ghostBarrageStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(ghostBarrageStatsPanel);


		setLabels();

		setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, (int) getPreferredSize().getHeight()));

		applyPopupRecursively(this, popupMenu);
		updatePopupMenuForPvpHubConfig();
	}

	private void setLabels()
	{
		String avgExpectedDmgDiffOneDecimal = nf1.format(avgExpectedDmgDiff);
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
		((JPanel) offPrayStatsLabel.getParent()).setToolTipText("<html>" + nf.format(totalStats.getOffPraySuccessCount()) + " successful off-pray attacks/" +
			nf.format(totalStats.getAttackCount()) + " total attacks (" +
			nf2.format(totalStats.calculateOffPraySuccessPercentage()) + "%)" +
			TrackedStatistic.OFF_PRAY.getPrefixedAcronymTooltip());

		expectedDmgStatsLabel.setText(nf.format(avgExpectedDmg) + " (" +
			(avgExpectedDmgDiff > 0 ? "+" : "") + avgExpectedDmgDiffOneDecimal + ")");
		((JPanel) expectedDmgStatsLabel.getParent()).setToolTipText("<html>Avg of " + nf1.format(avgExpectedDmg) +
			" expected damage per fight with avg diff of " + (avgExpectedDmgDiff > 0 ? "+" : "") +
			avgExpectedDmgDiffOneDecimal + ".<br>On kills: " + nf1.format(killAvgExpectedDmg) +
			" (" + (killAvgExpectedDmgDiff > 0 ? "+" : "") + nf1.format(killAvgExpectedDmgDiff) +
			"), on deaths: " + nf1.format(deathAvgExpectedDmg) +
			" (" + (deathAvgExpectedDmgDiff > 0 ? "+" : "") + nf1.format(deathAvgExpectedDmgDiff) + ")" +
			TrackedStatistic.EXPECTED_DMG.getPrefixedAcronymTooltip());

		dmgDealtStatsLabel.setText(nf.format(avgDmgDealt) + " (" +
			(avgDmgDealtDiff > 0 ? "+" : "") + avgDmgDealtDiffOneDecimal + ")");
		((JPanel) dmgDealtStatsLabel.getParent()).setToolTipText("<html>Avg of " + nf1.format(avgDmgDealt) +
			" damage per fight with avg diff of " + (avgDmgDealtDiff > 0 ? "+" : "") +
			avgDmgDealtDiffOneDecimal + ".<br>On kills: " + nf1.format(killAvgDmgDealt) +
			" (" + (killAvgDmgDealtDiff > 0 ? "+" : "") + nf1.format(killAvgDmgDealtDiff) +
			"), on deaths: " + nf1.format(deathAvgDmgDealt) +
			" (" + (deathAvgDmgDealtDiff > 0 ? "+" : "") + nf1.format(deathAvgDmgDealtDiff) + ")" +
			TrackedStatistic.DMG_DEALT.getPrefixedAcronymTooltip());

		if (totalStats.getMagicHitCountExpected() >= 10000)
		{
			magicHitCountStatsLabel.setText(nfWithK(totalStats.getMagicHitCount()) + "/" +
				nfWithK((int) totalStats.getMagicHitCountExpected()));
		}
		else
		{
			magicHitCountStatsLabel.setText(totalStats.getMagicHitStats());
		}
		((JPanel) magicHitCountStatsLabel.getParent()).setToolTipText("<html>You successfully hit " +
			totalStats.getMagicHitCount() + " of " + totalStats.getMagicAttackCount() + " magic attacks, but expected to hit " +
			nf1.format(totalStats.getMagicHitCountExpected()) + ".<br>Luck percentage: 100% = expected hits, &gt;100% = lucky, &lt;100% = unlucky" +
			TrackedStatistic.MAGIC_HITS.getPrefixedAcronymTooltip());

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
		((JPanel) offensivePrayCountStatsLabel.getParent()).setToolTipText("<html>" + nf.format(totalStats.getOffensivePraySuccessCount()) + " successful offensive prayers/" +
			nf.format(totalStats.getAttackCount()) + " total attacks (" +
			nf2.format(totalStats.calculateOffensivePraySuccessPercentage()) + "%)" +
			TrackedStatistic.OFFENSIVE_PRAY.getPrefixedAcronymTooltip());

		hpHealedStatsLabel.setText(nf.format(avgHpHealed));
		((JPanel) hpHealedStatsLabel.getParent()).setToolTipText("<html>" + "A total of " + nf.format(totalStats.getHpHealed())
			+ " hitpoints were recovered, with an average of " + nf.format(avgHpHealed) + " HP per fight." +
			TrackedStatistic.HP_HEALED.getPrefixedAcronymTooltip());

		// Avg Hits on Robes label
		if (numFights > 0)
		{
			avgRobeHitsStatsLabel.setText(nf1.format(avgCompetitorRobeHits) + " / " + nf1.format(avgOpponentRobeHits));
			((JPanel) avgRobeHitsStatsLabel.getParent()).setToolTipText("<html>Average melee/range hits taken while wearing robes per fight:<br>" +
				"Player: " + nf1.format(avgCompetitorRobeHits) + " (" + nf1.format(avgCompetitorRobeHitsPercentage) + "% of melee/range hits taken were on robes)<br>" +
				"Opponent: " + nf1.format(avgOpponentRobeHits) + " (" + nf1.format(avgOpponentRobeHitsPercentage) + "% of melee/range hits taken were on robes)" +
				TrackedStatistic.ROBE_HITS.getPrefixedAcronymTooltip());
		}
		else
		{
			avgRobeHitsStatsLabel.setText("- / -");
			((JPanel) avgRobeHitsStatsLabel.getParent()).setToolTipText("No robe hits data available for calculation.");
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
			((JPanel) avgKoChanceStatsLabel.getParent())
				.setToolTipText("<html>Average KO Chances per fight:<br>Player: "
					+ nf1.format(avgCompetitorKoChances) + " (" + nfPercent.format(avgCompetitorKoProb)
					+ ")<br>Opponent: "
					+ nf1.format(avgOpponentKoChances) + " (" + nfPercent.format(avgOpponentKoProb)
					+ ")<br>Total KO Chances: Player: "
					+ nf.format(totalCompetitorKoChances) + ", Opponent: " + nf.format(totalOpponentKoChances)
					+ TrackedStatistic.KO_CHANCES.getPrefixedAcronymTooltip());
		}
		else
		{
			avgKoChanceStatsLabel.setText("- / -");
			((JPanel) avgKoChanceStatsLabel.getParent()).setToolTipText("No KO chance data available for calculation.");
		}

		ghostBarrageStatsLabel.setText(nf.format(avgGhostBarrageCount) + " G.B. (" + nf.format(avgGhostBarrageExpectedDamage) + ")");
		((JPanel) ghostBarrageStatsLabel.getParent()).setToolTipText("<html>You had an average of " + nf.format(avgGhostBarrageCount)
			+ " Ghost Barrages per fight, each worth an extra " + nf.format(avgGhostBarrageExpectedDamage)
			+ " expected damage.<br>In total, you had " + totalStats.getGhostBarrageStats() + ".<br>"
			+ "Unless fighting in PvP Arena, your opponents likely had a similar value."
			+ TrackedStatistic.GHOST_BARRAGES.getPrefixedAcronymTooltip());
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
			fight.getCompetitor().getExpectedDamage(), fight.getCompetitor().getDamageDealt(),
			fight.getCompetitor().getMagicAttackCount(), fight.getCompetitor().getMagicHitCount(),
			fight.getCompetitor().getMagicHitCountExpected(), fight.getCompetitor().getOffensivePraySuccessCount(),
			fight.getCompetitor().getHpHealed(), fight.getCompetitor().getGhostBarrageCount(),
			fight.getCompetitor().getGhostBarrageExpectedDamage());

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

			deathTotalExpectedDmg += fight.getCompetitor().getExpectedDamage();
			deathTotalExpectedDmgDiff += fight.getCompetitorExpectedDmgDiff();

			deathTotalDmgDealt += fight.getCompetitor().getDamageDealt();
			deathTotalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();

			deathAvgExpectedDmg = deathTotalExpectedDmg / numDeaths;
			deathAvgExpectedDmgDiff = deathTotalExpectedDmgDiff / numDeaths;

			deathAvgDmgDealt = deathTotalDmgDealt / numDeaths;
			deathAvgDmgDealtDiff = deathTotalDmgDealtDiff / numDeaths;
		}

		if (fight.getOpponent().isDead())
		{
			numKills++;

			killTotalExpectedDmg += fight.getCompetitor().getExpectedDamage();
			killTotalExpectedDmgDiff += fight.getCompetitorExpectedDmgDiff();

			killTotalDmgDealt += fight.getCompetitor().getDamageDealt();
			killTotalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();

			killAvgExpectedDmg = killTotalExpectedDmg / numKills;
			killAvgExpectedDmgDiff = killTotalExpectedDmgDiff / numKills;

			killAvgDmgDealt = killTotalDmgDealt / numKills;
			killAvgDmgDealtDiff = killTotalDmgDealtDiff / numKills;
		}

		totalExpectedDmg += fight.getCompetitor().getExpectedDamage();
		totalExpectedDmgDiff += fight.getCompetitorExpectedDmgDiff();

		totalDmgDealt += fight.getCompetitor().getDamageDealt();
		totalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();

		// calculate avg stats based on total/numFights
		avgExpectedDmg = totalExpectedDmg / numFights;
		avgExpectedDmgDiff = totalExpectedDmgDiff / numFights;

		avgDmgDealt = totalDmgDealt / numFights;
		avgDmgDealtDiff = totalDmgDealtDiff / numFights;

		avgHpHealed = (double) totalStats.getHpHealed() / numFights;

		// Calculate KO chances & sum % for this fight and add to totals
		int fightCompetitorKoChances = 0;
		double fightCompetitorSurvivalProb = 1.0;
		int fightOpponentKoChances = 0;
		double fightOpponentSurvivalProb = 1.0;
		boolean fightHasKoData = false;
		List<FightLogEntry> logs = fight.getAllFightLogEntries();
		for (FightLogEntry log : logs)
		{
			Double koChance = log.getKoChance();
			if (koChance != null)
			{
				fightHasKoData = true; // Mark that this fight has KO data
				if (log.attackerName.equals(fight.getCompetitor().getName()))
				{
					fightCompetitorKoChances++;
					fightCompetitorSurvivalProb *= (1.0 - koChance);
				}
				else
				{
					fightOpponentKoChances++;
					fightOpponentSurvivalProb *= (1.0 - koChance);
				}
			}
		}

		// Only include this fight in KO averages if it had KO data
		if (fightHasKoData)
		{
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

		avgGhostBarrageCount = (double) totalStats.getGhostBarrageCount() / numFights;
		avgGhostBarrageExpectedDamage = totalStats.getGhostBarrageCount() != 0 ? totalStats.getGhostBarrageExpectedDamage() / totalStats.getGhostBarrageCount() : 0;

		SwingUtilities.invokeLater(this::setLabels);
	}

	public void addFights(ArrayList<FightPerformance> fights)
	{
		if (fights == null || fights.isEmpty())
		{
			return;
		}

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
				fight.getCompetitor().getExpectedDamage(), fight.getCompetitor().getDamageDealt(),
				fight.getCompetitor().getMagicAttackCount(), fight.getCompetitor().getMagicHitCount(),
				fight.getCompetitor().getMagicHitCountExpected(), fight.getCompetitor().getOffensivePraySuccessCount(),
				fight.getCompetitor().getHpHealed(), fight.getCompetitor().getGhostBarrageCount(),
				fight.getCompetitor().getGhostBarrageExpectedDamage());

			// Accumulate robe hits
			totalCompetitorRobeHits += fight.getCompetitor().getRobeHits();
			totalCompetitorRobeHitsAttempted += fight.getOpponent().getAttackCount() - fight.getOpponent().getTotalMagicAttackCount();
			totalOpponentRobeHits += fight.getOpponent().getRobeHits();
			totalOpponentRobeHitsAttempted += fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount();

			if (fight.getCompetitor().isDead())
			{
				numDeaths++;

				deathTotalExpectedDmg += fight.getCompetitor().getExpectedDamage();
				deathTotalExpectedDmgDiff += fight.getCompetitorExpectedDmgDiff();

				deathTotalDmgDealt += fight.getCompetitor().getDamageDealt();
				deathTotalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();
			}
			if (fight.getOpponent().isDead())
			{
				numKills++;

				killTotalExpectedDmg += fight.getCompetitor().getExpectedDamage();
				killTotalExpectedDmgDiff += fight.getCompetitorExpectedDmgDiff();

				killTotalDmgDealt += fight.getCompetitor().getDamageDealt();
				killTotalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();
			}

			totalExpectedDmg += fight.getCompetitor().getExpectedDamage();
			totalExpectedDmgDiff += fight.getCompetitorExpectedDmgDiff();

			totalDmgDealt += fight.getCompetitor().getDamageDealt();
			totalDmgDealtDiff += fight.getCompetitorDmgDealtDiff();

			// Calculate KO chances & sum % for this fight and add to totals if applicable
			int fightCompetitorKoChances = 0;
			double fightCompetitorSurvivalProb = 1.0;
			int fightOpponentKoChances = 0;
			double fightOpponentSurvivalProb = 1.0;
			boolean fightHasKoData = false;
			List<FightLogEntry> logs = fight.getAllFightLogEntries();
			for (FightLogEntry log : logs)
			{
				Double koChance = log.getKoChance();
				if (koChance != null)
				{
					fightHasKoData = true;
					if (log.attackerName.equals(fight.getCompetitor().getName()))
					{
						fightCompetitorKoChances++;
						fightCompetitorSurvivalProb *= (1.0 - koChance);
					}
					else
					{
						fightOpponentKoChances++;
						fightOpponentSurvivalProb *= (1.0 - koChance);
					}
				}
			}
			if (fightHasKoData)
			{
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
		avgExpectedDmg = numFights != 0 ? totalExpectedDmg / numFights : 0;
		avgExpectedDmgDiff = numFights != 0 ? totalExpectedDmgDiff / numFights : 0;

		avgDmgDealt = numFights != 0 ? totalDmgDealt / numFights : 0;
		avgDmgDealtDiff = numFights != 0 ? totalDmgDealtDiff / numFights : 0;

		killAvgExpectedDmg = numKills != 0 ? killTotalExpectedDmg / numKills : 0;
		killAvgExpectedDmgDiff = numKills != 0 ? killTotalExpectedDmgDiff / numKills : 0;

		deathAvgExpectedDmg = numDeaths != 0 ? deathTotalExpectedDmg / numDeaths : 0;
		deathAvgExpectedDmgDiff = numDeaths != 0 ? deathTotalExpectedDmgDiff / numDeaths : 0;

		killAvgDmgDealt = numKills != 0 ? killTotalDmgDealt / numKills : 0;
		killAvgDmgDealtDiff = numKills != 0 ? killTotalDmgDealtDiff / numKills : 0;

		deathAvgDmgDealt = numDeaths != 0 ? deathTotalDmgDealt / numDeaths : 0;
		deathAvgDmgDealtDiff = numDeaths != 0 ? deathTotalDmgDealtDiff / numDeaths : 0;

		avgHpHealed = numFights != 0 ? (double) totalStats.getHpHealed() / numFights : 0;

		// Recalculate KO averages using the count of fights with data
		avgCompetitorKoChances = numFightsWithKoChance != 0 ? totalCompetitorKoChances / numFightsWithKoChance : 0;
		avgOpponentKoChances = numFightsWithKoChance != 0 ? totalOpponentKoChances / numFightsWithKoChance : 0;
		avgCompetitorKoProb = numFightsWithKoChance != 0 ? totalCompetitorKoProbSum / numFightsWithKoChance : 0;
		avgOpponentKoProb = numFightsWithKoChance != 0 ? totalOpponentKoProbSum / numFightsWithKoChance : 0;

		avgGhostBarrageCount = numFights != 0 ? (double) totalStats.getGhostBarrageCount() / numFights : 0;
		avgGhostBarrageExpectedDamage = totalStats.getGhostBarrageCount() != 0 ? totalStats.getGhostBarrageExpectedDamage() / totalStats.getGhostBarrageCount() : 0;

		SwingUtilities.invokeLater(this::setLabels);
	}

	public void reset()
	{
		numFights = 0;
		numDeaths = 0;
		numKills = 0;

		totalExpectedDmg = 0;
		totalExpectedDmgDiff = 0;
		killTotalExpectedDmg = 0;
		killTotalExpectedDmgDiff = 0;
		deathTotalExpectedDmg = 0;
		deathTotalExpectedDmgDiff = 0;
		totalDmgDealt = 0;
		totalDmgDealtDiff = 0;
		killTotalDmgDealt = 0;
		killTotalDmgDealtDiff = 0;
		deathTotalDmgDealt = 0;
		deathTotalDmgDealtDiff = 0;

		avgExpectedDmg = 0;
		avgExpectedDmgDiff = 0;
		killAvgExpectedDmg = 0;
		killAvgExpectedDmgDiff = 0;
		deathAvgExpectedDmg = 0;
		deathAvgExpectedDmgDiff = 0;
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
		avgGhostBarrageExpectedDamage = 0;

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
		settingsWarningLabel = new ForwardingLabel();
		settingsWarningLabel.setText("Check plugin config for setup options!");
		settingsWarningLabel.setToolTipText("Please verify that the plugin options are configured according to your needs in the plugin's Configuration Panel.");
		settingsWarningLabel.setForeground(Color.RED);

		// make the warning font bold & smaller font size so we can fit more text.
		Font newFont = settingsWarningLabel.getFont();
		newFont = newFont.deriveFont(newFont.getStyle() | Font.BOLD, 12f);
		settingsWarningLabel.setFont(newFont);

		settingsWarningLabel.setHorizontalAlignment(SwingConstants.CENTER);
	}

	public void updatePopupMenuForPvpHubConfig()
	{
		// it should have the pvp hub option, so make sure it does
		if (CONFIG.uploadFightsToPvpHub() && CONFIG.hideRsnOnPvpHub() && popupMenu.getComponentCount() < popupMenuItemCountWithPvpHub)
		{
			popupMenu.add(resetPvpHubAnonymousId, pvpHupPopupMenuItemIdx);
		}
		else if (popupMenu.getComponentCount() >= popupMenuItemCountWithPvpHub)
		{
			popupMenu.remove(pvpHupPopupMenuItemIdx);
		}

	}

	private void applyPopupRecursively(Component c, JPopupMenu menu)
	{
		if (c instanceof JComponent)
		{
			((JComponent) c).setComponentPopupMenu(menu);
		}

		if (c instanceof Container)
		{
			for (Component child : ((Container) c).getComponents())
			{
				applyPopupRecursively(child, menu);
			}
		}
	}

}
