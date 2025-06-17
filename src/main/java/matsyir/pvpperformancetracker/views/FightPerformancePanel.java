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
package matsyir.pvpperformancetracker.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List; // Added import
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN_ICON;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import matsyir.pvpperformancetracker.controllers.AnalyzedFightPerformance;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.Fighter;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import java.awt.Graphics;
import java.awt.FontMetrics;

// Panel to display fight performance. The first line shows player stats while the second is the opponent.
// There is a skull icon beside a player's name if they died. The usernames are fixed to the left and the
// stats are fixed to the right.

public class FightPerformancePanel extends JPanel
{
	private static JFrame fightLogFrame; // save frame as static instance so there's only one at a time, to avoid window clutter.
	private static JFrame attackSummaryFrame; // save frame as static instance so there's only one at a time, to avoid window clutter.
	private static ImageIcon deathIcon;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss 'on' yyyy/MM/dd");
	private static final NumberFormat nf2 = NumberFormat.getInstance();
	private static final NumberFormat nfP1 = NumberFormat.getPercentInstance(); // For KO Chance %
	private static final Border normalBorder;
	private static final Border hoverBorder;
	static
	{
		// initialize number format
		nf2.setMaximumFractionDigits(2);
		nf2.setRoundingMode(RoundingMode.HALF_UP);

		// initialize percent format
		nfP1.setMaximumFractionDigits(1);
		nfP1.setRoundingMode(RoundingMode.HALF_UP);

		// main border used when not hovering:
		// outer border: matte border with 4px bottom, with same color as the panel behind FightPerformancePanels. Used as invisible 4px offset
		// inner border: padding for the inner content of the panel.
		normalBorder = BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(4, 6, 4, 6));

		// border used while hovering:
		// outer border: matte border with 4px bottom, with same color as the panel behind FightPerformancePanels. Used as invisible 4px offset
		// "middle" border: outline for the main panel
		// inner border: padding for the inner content of the panel, reduced by 1px to account for the outline
		hoverBorder = BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR),
			BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_HOVER_COLOR),
				new EmptyBorder(3, 5, 3, 5)));
	}

	private FightPerformance fight;
	private AnalyzedFightPerformance analyzedFight;
	private boolean showBorders;

	// Panel to display previous fight performance data.
	// intended layout:
	//
	// Player name ; 									Opponent Name
	// Player off-pray hit stats ; 						Opponent off-pray hit stats
	// Player deserved dps stats ; 						Opponent deserved dps stats
	// Player damage dealt ; 							Opponent damage dealt
	// Player magic hits/deserved magic hits ; 			Opponent magic hits/deserved magic hits
	// Player offensive pray stats ; 					N/A (no data, client only)
	// Player hp healed ; 								N/A (no data, client only)
	// Player Hits on robes ;							Opponent Hits on robes
	// Player Total KO Chances ;						Opponent Total KO Chances
	// Player Ghost barrages & extra deserved damage ; 	N/A (no data, client only)
	// The greater stats will be highlighted green. In this example, the player would have all the green highlights.
	// example:
	//
	//     PlayerName      OpponentName
	//	   32/55 (58%)      28/49 (57%) // off-pray
	//     176 (+12)          164 (-12) // deserved
	//     156 (-28)          184 (+28) // dealt
	//     10/16 (92.7)   12/20 (80.7%) // deserved magic
	//     27/55 (49%)              N/A // offensive pray
	//     100                      N/A // hp healed
	//	   11/31 (35.5%)  13/34 (38.2%) // hits on robes
	//     2 (110.0%)         1 (65.0%) // ko chances
	//     4 G.B. (37)				N/A // ghost barrages
	//
	// these are the params to use for a normal panel.
	public FightPerformancePanel(FightPerformance fight)
	{
		this(fight, true, true, false, null);
	}

	public FightPerformancePanel(FightPerformance fight, boolean showActions, boolean showBorders, boolean showOpponentClientStats, FightPerformance oppFight)
	{
		this.showBorders = showBorders;
		if (deathIcon == null)
		{
			// load & rescale red skull icon used to show if a player/opponent died in a fight and as the frame icon.
			deathIcon = new ImageIcon(PLUGIN_ICON.getScaledInstance(12, 12,  Image.SCALE_DEFAULT));
		}

		this.fight = fight;
		// save Fighters temporarily for more direct access
		Fighter competitor = fight.getCompetitor();
		Fighter opponent = fight.getOpponent();

		setLayout(new BorderLayout(5, 0));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		String tooltipText = "This fight ended at " + DATE_FORMAT.format(Date.from(Instant.ofEpochMilli(fight.getLastFightTime()))) + (fight.getWorld() > 0 ? ", on W" + fight.getWorld() : "");
		setToolTipText(tooltipText);

		if (showBorders)
		{
			setBorder(normalBorder);
		}

		// boxlayout panel to hold each of the lines.
		JPanel fightPanel = new JPanel();
		fightPanel.setLayout(new BoxLayout(fightPanel, BoxLayout.Y_AXIS));
		fightPanel.setBackground(null);

		// FIRST LINE: both player names, with centered world label
		JPanel playerNamesLine = new JPanel(new BorderLayout())
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				if (CONFIG.showWorldInSummary() && fight.getWorld() > 0)
				{
					String w = "W" + fight.getWorld();
					FontMetrics fm = g.getFontMetrics(getFont());
					int x = (getWidth() - fm.stringWidth(w)) / 2;
					int y = (getHeight() + fm.getAscent()) / 2 - fm.getDescent();
					g.setColor(Color.LIGHT_GRAY);
					g.drawString(w, x, y);
				}
			}
		};
		playerNamesLine.setBackground(null);

		// player names
		JLabel playerStatsName = new JLabel();

		// player name LEFT: player name
		if (competitor.isDead())
		{
			playerStatsName.setIcon(deathIcon);
		}
		playerStatsName.setText(competitor.getName());
		playerStatsName.setForeground(Color.WHITE);
		playerNamesLine.add(playerStatsName, BorderLayout.WEST);

		// player name RIGHT: opponent name
		JLabel opponentStatsName = new JLabel();
		if (opponent.isDead())
		{
			opponentStatsName.setIcon(deathIcon);
		}
		opponentStatsName.setText(opponent.getName());
		opponentStatsName.setForeground(Color.WHITE);
		playerNamesLine.add(opponentStatsName, BorderLayout.EAST);

		// Off-pray: both player's off-pray hit stats
		JPanel offPrayStatsLine = new JPanel();
		offPrayStatsLine.setLayout(new BorderLayout());
		offPrayStatsLine.setBackground(null);

		// Off-pray LEFT: player's off-pray hit stats
		JLabel playerOffPrayStats = new JLabel();
		playerOffPrayStats.setText(competitor.getOffPrayStats());
		playerOffPrayStats.setToolTipText(competitor.getName() + " hit " + competitor.getOffPraySuccessCount() + " successful off-pray attacks out of " +
			competitor.getAttackCount() + " total attacks (" +
			nf2.format(competitor.calculateOffPraySuccessPercentage()) + "%)");
		playerOffPrayStats.setForeground(fight.competitorOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		offPrayStatsLine.add(playerOffPrayStats, BorderLayout.WEST);

		// Off-pray RIGHT:, opponent's off-pray hit stats
		JLabel opponentOffPrayStats = new JLabel();
		opponentOffPrayStats.setText(opponent.getOffPrayStats());
		opponentOffPrayStats.setToolTipText(opponent.getName() + " hit " + opponent.getOffPraySuccessCount() + " successful off-pray attacks out of " +
			opponent.getAttackCount() + " total attacks (" +
			nf2.format(opponent.calculateOffPraySuccessPercentage()) + "%)");
		opponentOffPrayStats.setForeground(fight.opponentOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		offPrayStatsLine.add(opponentOffPrayStats, BorderLayout.EAST);

		// Deserved DMG: both player's deserved dps stats
		JPanel deservedDpsStatsLine = new JPanel();
		deservedDpsStatsLine.setLayout(new BorderLayout());
		deservedDpsStatsLine.setBackground(null);

		// Deserved DMG LEFT: player's deserved dps stats
		JLabel playerDeservedDpsStats = new JLabel();
		playerDeservedDpsStats.setText(competitor.getDeservedDmgString(opponent));
		playerDeservedDpsStats.setToolTipText(
			competitor.getName() + " deserved to deal " + nf2.format(competitor.getDeservedDamage()) +
			" damage based on gear & overheads (" + competitor.getDeservedDmgString(opponent, 1, true) + " vs opponent)");
		playerDeservedDpsStats.setForeground(fight.competitorDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);
		deservedDpsStatsLine.add(playerDeservedDpsStats, BorderLayout.WEST);

		// Deserved DMG RIGHT: opponent's deserved dps stats
		JLabel opponentDeservedDpsStats = new JLabel();
		opponentDeservedDpsStats.setText(opponent.getDeservedDmgString(competitor));
		opponentDeservedDpsStats.setToolTipText(
			opponent.getName() + " deserved to deal " + nf2.format(opponent.getDeservedDamage()) +
			" damage based on gear & overheads (" + opponent.getDeservedDmgString(competitor, 1, true) + " vs you)");
		opponentDeservedDpsStats.setForeground(fight.opponentDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);
		deservedDpsStatsLine.add(opponentDeservedDpsStats, BorderLayout.EAST);

		// DMG DEALT: both player's damage dealt
		JPanel dmgDealtStatsLine = new JPanel();
		dmgDealtStatsLine.setLayout(new BorderLayout());
		dmgDealtStatsLine.setBackground(null);

		// DMG DEALT LEFT: player's damage dealt
		JLabel playerDmgDealtStats = new JLabel();
		playerDmgDealtStats.setText(competitor.getDmgDealtString(opponent));
		playerDmgDealtStats.setToolTipText(competitor.getName() + " dealt " + competitor.getDamageDealt() +
			" damage (" + competitor.getDmgDealtString(opponent, true) + " vs opponent)");
		playerDmgDealtStats.setForeground(fight.competitorDmgDealtIsGreater() ? Color.GREEN : Color.WHITE);
		dmgDealtStatsLine.add(playerDmgDealtStats, BorderLayout.WEST);

		// DMG DEALT RIGHT: opponent's damage dealt
		JLabel opponentDmgDealtStats = new JLabel();
		opponentDmgDealtStats.setText(opponent.getDmgDealtString(competitor));
		opponentDmgDealtStats.setToolTipText(opponent.getName() + " dealt " + opponent.getDamageDealt() +
			" damage (" + opponent.getDmgDealtString(competitor, true) + " vs you)");
		opponentDmgDealtStats.setForeground(fight.opponentDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);
		dmgDealtStatsLine.add(opponentDmgDealtStats, BorderLayout.EAST);

		// MAGIC HITS: both player's magic hit stats (successful magic attacks/deserved successful magic attacks)
		JPanel magicHitStatsLine = new JPanel();
		magicHitStatsLine.setLayout(new BorderLayout());
		magicHitStatsLine.setBackground(null);

		// MAGIC HITS LEFT: player's magic hit stats
		JLabel playerMagicHitStats = new JLabel();
		playerMagicHitStats.setText(String.valueOf(competitor.getMagicHitStats()));
		playerMagicHitStats.setToolTipText("<html>" + competitor.getName() + " successfully hit " +
			competitor.getMagicHitCount() + " of " + competitor.getMagicAttackCount() + " magic attacks, but deserved to hit " +
			nf2.format(competitor.getMagicHitCountDeserved()) + ".<br>Luck percentage: 100% = expected hits, &gt;100% = lucky, &lt;100% = unlucky</html>");
		playerMagicHitStats.setForeground(fight.competitorMagicHitsLuckier() ? Color.GREEN : Color.WHITE);
		magicHitStatsLine.add(playerMagicHitStats, BorderLayout.WEST);

		// MAGIC HITS RIGHT: opponent's magic hit stats
		JLabel opponentMagicHitStats = new JLabel();
		opponentMagicHitStats.setText(String.valueOf(opponent.getMagicHitStats()));
		opponentMagicHitStats.setToolTipText("<html>" + opponent.getName() + " successfully hit " +
			opponent.getMagicHitCount() + " of " + opponent.getMagicAttackCount() + " magic attacks, but deserved to hit " +
			nf2.format(opponent.getMagicHitCountDeserved()) + ".<br>Luck percentage: 100% = expected hits, &gt;100% = lucky, &lt;100% = unlucky</html>");
		opponentMagicHitStats.setForeground(fight.opponentMagicHitsLuckier() ? Color.GREEN : Color.WHITE);
		magicHitStatsLine.add(opponentMagicHitStats, BorderLayout.EAST);

		// OFFENSIVE PRAYS: player's offensive pray stats (only player's, no data for opponent)
		JPanel offensivePrayStatsLine = new JPanel();
		offensivePrayStatsLine.setLayout(new BorderLayout());
		offensivePrayStatsLine.setBackground(null);

		// OFFENSIVE PRAYS LEFT: player's offensive pray stats
		JLabel playerOffensivePrayStats = new JLabel();
		playerOffensivePrayStats.setText(String.valueOf(competitor.getOffensivePrayStats()));
		playerOffensivePrayStats.setToolTipText(competitor.getName() + " did " + competitor.getOffensivePraySuccessCount() + " successful offensive prayers out of " +
			competitor.getAttackCount() + " total attacks (" +
			nf2.format(competitor.calculateOffensivePraySuccessPercentage()) + "%)");

		playerOffensivePrayStats.setForeground(
			(showOpponentClientStats && competitor.calculateOffensivePraySuccessPercentage() >
				oppFight.getCompetitor().calculateOffensivePraySuccessPercentage()) ?
				Color.GREEN : Color.WHITE);

		offensivePrayStatsLine.add(playerOffensivePrayStats, BorderLayout.WEST);

		// OFFENSIVE PRAYS RIGHT: "N/A", no data.
		JLabel opponentOffensivePrayStats = new JLabel();
		if (showOpponentClientStats)
		{
			Fighter oppComp = oppFight.getCompetitor();

			opponentOffensivePrayStats.setText(String.valueOf(oppComp.getOffensivePrayStats()));
			opponentOffensivePrayStats.setToolTipText(oppComp.getName() + " did " + oppComp.getOffensivePraySuccessCount() + " successful offensive prayers out of " +
				oppComp.getAttackCount() + " total attacks (" +
				nf2.format(oppComp.calculateOffensivePraySuccessPercentage()) + "%)");
			opponentOffensivePrayStats.setForeground(
				oppFight.getCompetitor().calculateOffensivePraySuccessPercentage() > competitor.calculateOffensivePraySuccessPercentage()
					? Color.GREEN : Color.WHITE);
		}
		else
		{
			opponentOffensivePrayStats.setText("N/A");
			opponentOffensivePrayStats.setToolTipText("No data is available for the opponent's offensive prayers");
			opponentOffensivePrayStats.setForeground(Color.WHITE);
		}
		offensivePrayStatsLine.add(opponentOffensivePrayStats, BorderLayout.EAST);

		// HP healed (only player's, no data for opponent)
		JPanel hpHealedLine = new JPanel();
		hpHealedLine.setLayout(new BorderLayout());
		hpHealedLine.setBackground(null);

		// HP healed LEFT: player's HP healed
		JLabel playerHpHealed = new JLabel();
		playerHpHealed.setText(String.valueOf(competitor.getHpHealed()));
		playerHpHealed.setToolTipText(competitor.getName() + " recovered " + competitor.getHpHealed() + " hitpoints during the fight");

		playerHpHealed.setForeground(
			(showOpponentClientStats && competitor.getHpHealed() > oppFight.getCompetitor().getHpHealed()) ?
				Color.GREEN : Color.WHITE);

		hpHealedLine.add(playerHpHealed, BorderLayout.WEST);

		// HP healed RIGHT: "N/A", no data.
		JLabel opponentHpHealed = new JLabel();
		if (showOpponentClientStats)
		{
			Fighter oppComp = oppFight.getCompetitor();

			opponentHpHealed.setText(String.valueOf(oppComp.getHpHealed()));
			opponentHpHealed.setToolTipText(oppComp.getName() + " recovered " + oppComp.getHpHealed() + " hitpoints during the fight");
			opponentHpHealed.setForeground(oppFight.getCompetitor().getHpHealed() > competitor.getHpHealed() ?
				Color.GREEN : Color.WHITE);
		}
		else
		{
			opponentHpHealed.setText("N/A");
			opponentHpHealed.setToolTipText("No data is available for the opponent's hp healed");
			opponentHpHealed.setForeground(Color.WHITE);
		}
		hpHealedLine.add(opponentHpHealed, BorderLayout.EAST);

		// Hits on Robes line (count/total and percentage)
		JPanel robeHitsLine = new JPanel(new BorderLayout());
		robeHitsLine.setBackground(null);
		// Competitor's hits on robes
		int compHits = fight.getCompetitor().getRobeHits();
		int compTotal = fight.getOpponent().getAttackCount() - fight.getOpponent().getTotalMagicAttackCount();
		double compRatio = compTotal > 0 ? (double) compHits / compTotal : 0.0;
		JLabel playerRobeHitsLabel = new JLabel(compHits + "/" + compTotal + " (" + nfP1.format(compRatio) + ")");
		playerRobeHitsLabel.setToolTipText("<html>" + fight.getCompetitor().getName() + " was hit with range/melee while wearing robes: " +
				compHits + "/" + compTotal + " (" + nfP1.format(compRatio) + ")<br>" +
				"In other words, of his opponent's " + compTotal + " range/melee attacks, " +
				fight.getCompetitor().getName() + " tanked " + compHits + " of them with robes."
		);
		playerRobeHitsLabel.setForeground(compRatio < ((double)(fight.getOpponent().getRobeHits()) / Math.max(1, fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount())) ? Color.GREEN : Color.WHITE);
		robeHitsLine.add(playerRobeHitsLabel, BorderLayout.WEST);
		// Opponent's hits on robes
		int oppHits = fight.getOpponent().getRobeHits();
		int oppTotal = fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount();
		double oppRatio = oppTotal > 0 ? (double) oppHits / oppTotal : 0.0;
		JLabel opponentRobeHitsLabel = new JLabel(oppHits + "/" + oppTotal + " (" + nfP1.format(oppRatio) + ")");
		opponentRobeHitsLabel.setToolTipText("<html>" + fight.getOpponent().getName() + " was hit with range/melee while wearing robes: " +
				oppHits + "/" + oppTotal + " (" + nfP1.format(oppRatio) + ")<br>" +
				"In other words, of his opponent's " + oppTotal + " range/melee attacks, " +
				fight.getOpponent().getName() + " tanked " + oppHits + " of them with robes."
		);
		opponentRobeHitsLabel.setForeground(oppRatio < compRatio ? Color.GREEN : Color.WHITE);
		robeHitsLine.add(opponentRobeHitsLabel, BorderLayout.EAST);

		// Total KO Chances
		JPanel totalKoChanceLine = new JPanel();
		totalKoChanceLine.setLayout(new BorderLayout());
		totalKoChanceLine.setBackground(null);

		// Calculate total KO chances and overall probability // MODIFIED Calculation
		int competitorKoChances = 0;
		double competitorSurvivalProb = 1.0; // Start with 100% survival chance
		int opponentKoChances = 0;
		double opponentSurvivalProb = 1.0; // Start with 100% survival chance
		List<FightLogEntry> logs = fight.getAllFightLogEntries();
		for (FightLogEntry log : logs) {
			Double koChance = log.getKoChance();
			if (koChance != null) {
				if (log.attackerName.equals(competitor.getName())) {
					competitorKoChances++;
					competitorSurvivalProb *= (1.0 - koChance); // Calculate survival prob
				} else {
					opponentKoChances++;
					opponentSurvivalProb *= (1.0 - koChance); // Calculate survival prob
				}
			}
		}

		// Calculate overall KO probability
		Double competitorOverallKoProb = (competitorKoChances > 0) ? (1.0 - competitorSurvivalProb) : null;
		Double opponentOverallKoProb = (opponentKoChances > 0) ? (1.0 - opponentSurvivalProb) : null;

		// KOCHANCE LEFT: Competitor Total KO Chance (Using Overall Probability)
		JLabel playerTotalKoChance = new JLabel();
		String compTotalKoChanceText = competitorKoChances + (competitorOverallKoProb != null ? " (" + nfP1.format(competitorOverallKoProb) + ")" : ""); // Use overall prob
		playerTotalKoChance.setText(compTotalKoChanceText);
		playerTotalKoChance.setToolTipText(competitor.getName() + " got " + competitorKoChances + " KO attempts with an overall KO probability of " + (competitorOverallKoProb != null ? nfP1.format(competitorOverallKoProb) : "0%")); // Updated tooltip
		playerTotalKoChance.setForeground(Color.WHITE);
		totalKoChanceLine.add(playerTotalKoChance, BorderLayout.WEST);

		// KOCHANCE RIGHT: Opponent Total KO Chance (Using Overall Probability)
		JLabel opponentTotalKoChance = new JLabel();
		String oppTotalKoChanceText = opponentKoChances + (opponentOverallKoProb != null ? " (" + nfP1.format(opponentOverallKoProb) + ")" : ""); // Use overall prob
		opponentTotalKoChance.setText(oppTotalKoChanceText);
		opponentTotalKoChance.setToolTipText(opponent.getName() + " got " + opponentKoChances + " KO attempts with an overall KO probability of " + (opponentOverallKoProb != null ? nfP1.format(opponentOverallKoProb) : "0%")); // Updated tooltip
		opponentTotalKoChance.setForeground(Color.WHITE);
		totalKoChanceLine.add(opponentTotalKoChance, BorderLayout.EAST);

		// let's keep ghost barrage as the bottom-most statistic:
		// It's only relevant to people fighting in PvP Arena, and it's mostly only relevant
		// to people who can share their tracker with each-other - so pretty rarely useful.
		// line container for player's Ghost barrages (only player's, no data for opponent)
		JPanel ghostBarragesLine = new JPanel();
		ghostBarragesLine.setLayout(new BorderLayout());
		ghostBarragesLine.setBackground(null);

		// GBs LEFT: player's ghost barrage stats
		JLabel playerGhostBarrages = new JLabel();
		playerGhostBarrages.setText(competitor.getGhostBarrageStats());
		playerGhostBarrages.setToolTipText("<html>(Advanced): " + competitor.getName() + " hit " + competitor.getGhostBarrageCount()
				+ " ghost barrages during the fight, worth an extra " + nf2.format(competitor.getGhostBarrageDeservedDamage())
				+ " deserved damage.<br>Unless fighting in PvP Arena, your opponent likely had a similar value.</html>");

		playerGhostBarrages.setForeground(
				(showOpponentClientStats
						&& competitor.getGhostBarrageDeservedDamage() > oppFight.getCompetitor().getGhostBarrageDeservedDamage())
						? Color.GREEN : ColorScheme.BRAND_ORANGE);

		ghostBarragesLine.add(playerGhostBarrages, BorderLayout.WEST);

		// GBs RIGHT: "N/A", no data.
		JLabel opponentGhostBarrages = new JLabel();
		if (showOpponentClientStats)
		{
			Fighter oppComp = oppFight.getCompetitor();

			opponentGhostBarrages.setText(oppComp.getGhostBarrageStats());
			opponentGhostBarrages.setToolTipText("<html>(Advanced): " + oppComp.getName() + " hit " + oppComp.getGhostBarrageCount()
					+ " ghost barrages during the fight, worth an extra " + nf2.format(oppComp.getGhostBarrageDeservedDamage())
					+ " deserved damage.<br>Unless fighting in PvP Arena, your opponent likely had a similar value.</html>");
			opponentGhostBarrages.setForeground(
					oppFight.getCompetitor().getGhostBarrageDeservedDamage() > competitor.getGhostBarrageDeservedDamage()
							? Color.GREEN : ColorScheme.BRAND_ORANGE);
		}
		else
		{
			opponentGhostBarrages.setText("N/A");
			opponentGhostBarrages.setToolTipText("No data is available for the opponent's ghost barrages");
			opponentGhostBarrages.setForeground(ColorScheme.BRAND_ORANGE);
		}
		ghostBarragesLine.add(opponentGhostBarrages, BorderLayout.EAST);


		fightPanel.add(playerNamesLine);
		fightPanel.add(offPrayStatsLine);
		fightPanel.add(deservedDpsStatsLine);
		fightPanel.add(dmgDealtStatsLine);
		fightPanel.add(magicHitStatsLine);
		fightPanel.add(offensivePrayStatsLine);
		fightPanel.add(hpHealedLine);
		fightPanel.add(robeHitsLine);
		fightPanel.add(totalKoChanceLine);
		fightPanel.add(ghostBarragesLine); // keep as bottom-most

		add(fightPanel, BorderLayout.NORTH);

		// setup mouse events for hovering and clicking to open the fight log
		MouseAdapter fightPerformanceMouseListener = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				setFullBackgroundColor(ColorScheme.DARK_GRAY_COLOR);
				setOutline(true);
				setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setFullBackgroundColor(ColorScheme.DARKER_GRAY_COLOR);
				setOutline(false);
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				// ignore right clicks since that should be used for context menus/popup menus.
				// btn1: left click, btn2: middle click, btn3: right click
				if (e.getButton() == MouseEvent.BUTTON3)
				{
					return;
				}

				createFightLogFrame();
			}
		};
		addMouseListener(fightPerformanceMouseListener);

		// skip the remaining code if we aren't showing actions.
		if (!showActions) { return; }

		JPopupMenu popupMenu = new JPopupMenu();

		// Create "Show fight log" menu (same action as left click)
		final JMenuItem displayFightLog = new JMenuItem("Display Fight Log");
		displayFightLog.addActionListener(e -> createFightLogFrame());

		// Create "Show attack summary" menu
		final JMenuItem displayAttackSummary = new JMenuItem("Display Attack Summary");
		displayAttackSummary.addActionListener(e -> createAttackSummaryFrame());

		// Create "Copy Fight Data" popup menu/context menu
		final JMenuItem copyFight = new JMenuItem("Copy Fight Data (Advanced)");
		copyFight.addActionListener(e -> PLUGIN.exportFight(fight));
		copyFight.setForeground(ColorScheme.BRAND_ORANGE);

		final JMenuItem openFightAnalysis = new JMenuItem("Fight Analysis (Advanced)");
		openFightAnalysis.addActionListener(e -> new FightAnalysisFrame(fight, this.getRootPane()));
		openFightAnalysis.setForeground(ColorScheme.BRAND_ORANGE);

		// Create "Remove Fight" popup menu/context menu
		final JMenuItem removeFight = new JMenuItem("Remove Fight");
		removeFight.addActionListener(e ->
		{
			int dialogResult = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove this fight? This cannot be undone.", "Warning", JOptionPane.YES_NO_OPTION);
			if (dialogResult == JOptionPane.YES_OPTION)
			{
				PLUGIN.removeFight(fight);
			}
		});
		removeFight.setForeground(Color.RED);

		popupMenu.add(displayFightLog);
		popupMenu.add(displayAttackSummary);
		popupMenu.add(copyFight);
		popupMenu.add(openFightAnalysis);
		popupMenu.add(removeFight);
		setComponentPopupMenu(popupMenu);

		setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, (int)getPreferredSize().getHeight()));
	}

	public FightPerformancePanel(AnalyzedFightPerformance aFight)
	{
		this(aFight, false, true, true, aFight.getOpposingFight());

		this.analyzedFight = aFight;
	}

	private void setFullBackgroundColor(Color color)
	{
		this.setBackground(color);
		for (Component c : getComponents())
		{
			c.setBackground(color);
		}
	}

	private void setOutline(boolean visible)
	{
		if (showBorders)
		{
			this.setBorder(visible ? hoverBorder : normalBorder);
		}
	}

	private void createFightLogFrame()
	{
		// destroy current frame if it exists so we only have one at a time (static field)
		if (fightLogFrame != null)
		{
			fightLogFrame.dispose();
		}

		// show error modal if the fight has no log entries to display.
		ArrayList<FightLogEntry> fightLogEntries = new ArrayList<>(fight.getAllFightLogEntries());
		fightLogEntries.removeIf(e -> !e.isFullEntry());
		if (fightLogEntries.isEmpty())
		{
			PLUGIN.createConfirmationModal(false, "This fight has no attack logs to display, or the data is outdated.");
		}
		else if (analyzedFight != null) // if analyzed fight is set, then show an analyzed fight's fightLogFrame.
		{
			fightLogFrame = new FightLogFrame(analyzedFight, getRootPane());

		}
		else
		{
			fightLogFrame = new FightLogFrame(fight,
				fightLogEntries,
				getRootPane());
		}
	}

	private void createAttackSummaryFrame()
	{
		// destroy current frame if it exists so we only have one at a time (static field)
		if (attackSummaryFrame != null)
		{
			attackSummaryFrame.dispose();
		}
		// show error modal if the fight has no log entries to display.
		ArrayList<FightLogEntry> fightLogEntries = new ArrayList<>(fight.getAllFightLogEntries());
		fightLogEntries.removeIf(e -> !e.isFullEntry());
		if (fightLogEntries.isEmpty())
		{
			PLUGIN.createConfirmationModal(false, "This fight has no attack summary to display, or the data is outdated.");
			return;
		}

		attackSummaryFrame = new AttackSummaryFrame(fight, fightLogEntries, getRootPane());
	}
}
