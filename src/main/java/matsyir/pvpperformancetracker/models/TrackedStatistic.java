/*
 * Copyright (c) 2025, Matsyir <https://github.com/matsyir>
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
package matsyir.pvpperformancetracker.models;

import lombok.Getter;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.Fighter;
import static matsyir.pvpperformancetracker.utils.NumberFormatter.*;
import matsyir.pvpperformancetracker.utils.PvpColorScheme;
import matsyir.pvpperformancetracker.views.FightPerformancePanel;
import matsyir.pvpperformancetracker.views.PanelFactory;
import matsyir.pvpperformancetracker.views.TableComponent;

import javax.swing.JPanel;
import java.awt.Color;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public enum TrackedStatistic
{ 
	OFF_PRAY("Off-pray", "OP",
		"Off-pray statistic, # of times you correctly used a different style than your opponent's overhead pray." +
			"<br>For example, when you use melee or ranged vs. protect from magic, that's a successful off-pray hit."),
	EXPECTED_DMG("Expected damage", "eD", // NOTE: previously referred to as "Deserved damage"
		"Expected damage statistic, # of damage you would've dealt if the game had averaged rng, including misses." +
			"<br>Due to the high variance in this game, it's normal for fights to commonly stray from this." +
			"<br><br>This used to be referred to as 'deserved damage'. The functionality was not changed."),
	DMG_DEALT("Damage dealt", "D",
		"Damage dealt statistic, sum of your actual damage hitsplats on your opponent."),
	MAGIC_HITS("Magic hits luck", "M",
		"Magic luck statistic, tracks expected hits vs. # of actual magic hits (as opposed to splashes)."),
	OFFENSIVE_PRAY("Offensive pray", "P",
		"Offensive prayer statistic, tracks # of offensive prays you used correctly, e.g piety for melee."),
	HP_HEALED("HP healed", "HP",
		"HP healed statistic, tracks amount of HP recovered during the fight (from all sources)."),
	ROBE_HITS("Hits on robes", "rH",
		"Hits on robes statistic, tracks # of times you got range/melee'd in robes (don't)."),
	KO_CHANCES("KO chances", "KO",
		"KO chance statistic, tracks every KO chance you got and gives you a total chance of KO." +
			"<br>When you spec your opponent on 20hp and hit a 12, you can find the KO % chance in the fight log."),

	// NOTE: let's keep ghost barrage as the bottom-most statistic:
	// It's only relevant to people fighting in PvP Arena, and it's mostly only relevant
	// to people who can share their tracker with each-other - so pretty rarely useful.
	// ... also, more often than not it literally has 0 data. 0 hits, nothing. so let's just not show
	// this line when there is no data, since it's also the bottom row now, anyway.
	GHOST_BARRAGES("Ghost barrages", "GB",
		"Ghost-barrage statistic, for when you're animation stalled while barraging." +
			"<br>This statistic is hidden if both players have 0gb, which is common." +
			"<br>Weird and for advanced users only.");

	private static final String NO_DATA_SHORT = "-";
	private static final String NO_DATA = "N/A";

	static
	{


		// for every TrackedStatistic, init behaviors/functions/providers:
		// 1) Init/generate FightPerformancePanel line for this statistic
		// 2) Init/generate FightPerformanceOverlay line for this statistic (TableComponent)
		// 3) Update TableComponent from step 2 from ongoing fight data
		// these have to be static in order to self-reference themselves and use things like numberFormats
		OFF_PRAY.init(
			(fight, oppFight) -> PanelFactory.createStatsLine(
				OFF_PRAY.acronym, OFF_PRAY.acronymTooltip
				, fight.competitor.getOffPrayStats()
				, (fight.competitor.getName() + " hit " + fight.competitor.getOffPraySuccessCount() + " successful off-pray attacks out of " +
					fight.competitor.getAttackCount() + " total attacks (" +
					nf2.format(fight.competitor.calculateOffPraySuccessPercentage()) + "%)")
				, fight.competitorOffPraySuccessIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()

				, fight.opponent.getOffPrayStats()
				, (fight.opponent.getName() + " hit " + fight.opponent.getOffPraySuccessCount() + " successful off-pray attacks out of " +
					fight.opponent.getAttackCount() + " total attacks (" +
					nf2.format(fight.opponent.calculateOffPraySuccessPercentage()) + "%)")
				, fight.opponentOffPraySuccessIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
			),
			() -> PanelFactory.createOverlayStatsLine(OFF_PRAY.acronym, 50, 50,
				NO_DATA_SHORT, PvpColorScheme.unsuccessColor(), NO_DATA_SHORT, PvpColorScheme.unsuccessColor()),
			(fight, component) -> component.updateLeftRightCells(
				fight.getCompetitor().getOffPrayStats(true)
				, fight.competitorOffPraySuccessIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
				, fight.getOpponent().getOffPrayStats(true)
				, fight.opponentOffPraySuccessIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
			)
		);

		EXPECTED_DMG.init(
			(fight, oppFight) -> PanelFactory.createStatsLine(EXPECTED_DMG.acronym, EXPECTED_DMG.acronymTooltip
				, fight.competitor.getExpectedDmgString(fight.opponent)
				, "On average, " + (fight.competitor.getName() + " could expect to deal " + nf2.format(fight.competitor.getExpectedDamage()) +
					" damage based on gear & overheads (" + fight.competitor.getExpectedDmgString(fight.opponent, 1, true) + " vs opponent)")
				, fight.competitorExpectedDmgIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()

				, fight.opponent.getExpectedDmgString(fight.competitor)
				, "On average, " + (fight.opponent.getName() + " could expect to deal " + nf2.format(fight.opponent.getExpectedDamage()) +
					" damage based on gear & overheads (" + fight.opponent.getExpectedDmgString(fight.competitor, 1, true) + " vs you)")
				, fight.opponentExpectedDmgIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
			),
			() -> PanelFactory.createOverlayStatsLine(EXPECTED_DMG.acronym, 70, 30,
				NO_DATA_SHORT, PvpColorScheme.unsuccessColor(), NO_DATA_SHORT, PvpColorScheme.unsuccessColor()),
			(fight, component) -> component.updateLeftRightCells(
				fight.getCompetitor().getExpectedDmgString(fight.getOpponent())
				, fight.competitorExpectedDmgIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
				, String.valueOf((int) Math.round(fight.getOpponent().getExpectedDamage()))
				, fight.opponentExpectedDmgIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
			)
		);

		DMG_DEALT.init(
			(fight, oppFight) -> PanelFactory.createStatsLine(DMG_DEALT.acronym, DMG_DEALT.acronymTooltip
				, fight.competitor.getDmgDealtString(fight.opponent)
				, fight.competitor.getName() + " dealt " + fight.competitor.getDamageDealt() +
					" damage (" + fight.competitor.getDmgDealtString(fight.opponent, true) + " vs opponent)"
				, fight.competitorDmgDealtIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()

				, fight.opponent.getDmgDealtString(fight.competitor)
				, fight.opponent.getName() + " dealt " + fight.opponent.getDamageDealt() +
					" damage (" + fight.opponent.getDmgDealtString(fight.competitor, true) + " vs you)"
				, fight.opponentExpectedDmgIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
				, true
				, fight.competitor.isDead() ? FightPerformancePanel.deathIcon : null
				, fight.opponent.isDead() ? FightPerformancePanel.deathIcon : null

			),
			() -> PanelFactory.createOverlayStatsLine(DMG_DEALT.acronym, 70, 30,
				NO_DATA_SHORT, PvpColorScheme.unsuccessColor(), NO_DATA_SHORT, PvpColorScheme.unsuccessColor()),
			(fight, component) -> component.updateLeftRightCells(
				String.valueOf(fight.getCompetitor().getDmgDealtString(fight.getOpponent()))
				, fight.competitorDmgDealtIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
				, String.valueOf(fight.getOpponent().getDamageDealt())
				, fight.opponentDmgDealtIsGreater() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
			)
		);

		MAGIC_HITS.init(
			(fight, oppFight) -> PanelFactory.createStatsLine(MAGIC_HITS.acronym, MAGIC_HITS.acronymTooltip
				, String.valueOf(fight.competitor.getMagicHitStats())
				, fight.competitor.getName() + " successfully hit " +
					fight.competitor.getMagicHitCount() + " of " + fight.competitor.getMagicAttackCount() + " magic attacks, but expected to hit " +
					nf2.format(fight.competitor.getMagicHitCountExpected()) + " (" + fight.competitor.getMagicHitLuckPercentage(nf2) + "%).<br>" +
					"Luck percentage: 100% = expected hits, &gt;100% = lucky, &lt;100% = unlucky"
				, fight.competitorMagicHitsLuckier() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()

				, String.valueOf(fight.opponent.getMagicHitStats())
				, fight.opponent.getName() + " successfully hit " +
					fight.opponent.getMagicHitCount() + " of " + fight.opponent.getMagicAttackCount() + " magic attacks, but expected to hit " +
					nf2.format(fight.opponent.getMagicHitCountExpected()) + " (" + fight.opponent.getMagicHitLuckPercentage(nf2) + "%)." +
					"<br>Luck percentage: 100% = expected hits, &gt;100% = lucky, &lt;100% = unlucky" +
					""
				, fight.opponentMagicHitsLuckier() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()

			),
			() -> PanelFactory.createOverlayStatsLine(MAGIC_HITS.acronym, 70, 30,
				NO_DATA_SHORT, PvpColorScheme.unsuccessColor(), NO_DATA_SHORT, PvpColorScheme.unsuccessColor()),
			(fight, component) -> component.updateLeftRightCells(
				String.valueOf(fight.getCompetitor().getShortMagicHitStats())
				, fight.competitorMagicHitsLuckier() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
				, String.valueOf(fight.getOpponent().getShortMagicHitStats())
				, fight.opponentMagicHitsLuckier() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
			)
		);

		OFFENSIVE_PRAY.init(
			(fight, oppFight) -> { // returns FightPerformancePanel component
				// OFFENSIVE PRAYS RIGHT: prepare opponent data if its available
				String oppOffensivePrayStats = NO_DATA;
				String oppOffensivePrayTooltip = "No data is available for the opponent's offensive prayers";
				Color oppOffensivePrayColor = PvpColorScheme.neutralColor();
				if (oppFight != null)
				{
					Fighter oppComp = oppFight.getCompetitor();

					oppOffensivePrayStats = String.valueOf(oppComp.getOffensivePrayStats());
					oppOffensivePrayTooltip = (oppComp.getName() + " did " + oppComp.getOffensivePraySuccessCount() + " successful offensive prayers out of " +
						oppComp.getAttackCount() + " total attacks (" +
						nf2.format(oppComp.calculateOffensivePraySuccessPercentage()) + "%)");
					oppOffensivePrayColor = (
						oppFight.getCompetitor().calculateOffensivePraySuccessPercentage() > fight.competitor.calculateOffensivePraySuccessPercentage()
							? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor());
				}

				// OFFENSIVE PRAYS: player's offensive pray stats (only player's, usually no data for opponent)
				return PanelFactory.createStatsLine(OFFENSIVE_PRAY.acronym, OFFENSIVE_PRAY.acronymTooltip
					, String.valueOf(fight.competitor.getOffensivePrayStats())
					, (fight.competitor.getName() + " did " + fight.competitor.getOffensivePraySuccessCount() + " successful offensive prayers out of " +
						fight.competitor.getAttackCount() + " total attacks (" +
						nf2.format(fight.competitor.calculateOffensivePraySuccessPercentage()) + "%)")
					, oppFight == null ? PvpColorScheme.neutralColor() : (fight.competitor.calculateOffensivePraySuccessPercentage() >
						oppFight.getCompetitor().calculateOffensivePraySuccessPercentage() ?
						PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor())

					, oppOffensivePrayStats
					, oppOffensivePrayTooltip
					, oppOffensivePrayColor
				);
			},
			() -> PanelFactory.createOverlayStatsLine(OFFENSIVE_PRAY.acronym, 80, 20,
				NO_DATA_SHORT, PvpColorScheme.unsuccessColor(), NO_DATA_SHORT, PvpColorScheme.unsuccessColor()),
			(fight, component) -> component.updateLeftCellText(String.valueOf(fight.getCompetitor().getOffensivePrayStats(true)))
		);

		HP_HEALED.init(
			(fight, oppFight) -> { // returns FightPerformancePanel component
				// hp healed, right
				String oppHpHealedStats = NO_DATA;
				String oppHpHealedTooltip = "No data is available for the opponent's hp healed";
				Color oppHpHealedColor = PvpColorScheme.neutralColor();
				if (oppFight != null)
				{
					Fighter oppComp = oppFight.getCompetitor();

					oppHpHealedStats = (String.valueOf(oppComp.getHpHealed()));
					oppHpHealedTooltip = (oppComp.getName() + " recovered " + oppComp.getHpHealed() + " hitpoints during the fight");
					oppHpHealedColor = (oppFight.getCompetitor().getHpHealed() > fight.competitor.getHpHealed() ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor());
				}

				// HP healed (only player's, no data for opponent usually)
				return PanelFactory.createStatsLine(HP_HEALED.acronym, HP_HEALED.acronymTooltip
					, String.valueOf(fight.competitor.getHpHealed())
					, (fight.competitor.getName() + " recovered " + fight.competitor.getHpHealed() + " hitpoints during the fight")
					, oppFight == null ? PvpColorScheme.neutralColor() : (fight.competitor.getHpHealed() > oppFight.getCompetitor().getHpHealed() ?
						PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor())

					, oppHpHealedStats
					, oppHpHealedTooltip
					, oppHpHealedColor
				);
			},
			() -> PanelFactory.createOverlayStatsLine(HP_HEALED.acronym, 80, 20,
				NO_DATA_SHORT, PvpColorScheme.unsuccessColor(), NO_DATA_SHORT, PvpColorScheme.unsuccessColor()),
			(fight, component) -> component.updateLeftCellText(String.valueOf(fight.getCompetitor().getHpHealed()))
		);

		ROBE_HITS.init(
			(fight, oppFight) -> { // returns FightPerformancePanel component
				// Competitor's hits on robes
				int compHits = fight.getCompetitor().getRobeHits();
				int compTotal = fight.getOpponent().getAttackCount() - fight.getOpponent().getTotalMagicAttackCount();
				double compRatio = compTotal > 0 ? (double) compHits / compTotal : 0.0;
				// Opponent's hits on robes
				int oppHits = fight.getOpponent().getRobeHits();
				int oppTotal = fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount();
				double oppRatio = oppTotal > 0 ? (double) oppHits / oppTotal : 0.0;

				return PanelFactory.createStatsLine(ROBE_HITS.acronym, ROBE_HITS.acronymTooltip
					, (compHits + " (" + nfP.format(compRatio) + ")")
					, (fight.getCompetitor().getName() + " was hit with range/melee while wearing robes: " +
						compHits + "/" + compTotal + " (" + nfP1.format(compRatio) + ")<br>" +
						"In other words, of his opponent's " + compTotal + " range/melee attacks, " +
						fight.getCompetitor().getName() + " tanked " + compHits + " of them with robes.")
					, (compRatio < ((double) (fight.getOpponent().getRobeHits()) /
						Math.max(1, fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount())) ?
						PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor())

					, (oppHits + " (" + nfP.format(oppRatio) + ")")
					, (fight.getOpponent().getName() + " was hit with range/melee while wearing robes: " +
						oppHits + "/" + oppTotal + " (" + nfP1.format(oppRatio) + ")<br>" +
						"In other words, of his opponent's " + oppTotal + " range/melee attacks, " +
						fight.getOpponent().getName() + " tanked " + oppHits + " of them with robes.")
					, (oppRatio < compRatio ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor())
				);
			},
			() -> PanelFactory.createOverlayStatsLine(ROBE_HITS.acronym, 50, 50,
				NO_DATA_SHORT, PvpColorScheme.unsuccessColor(), NO_DATA_SHORT, PvpColorScheme.unsuccessColor()),
			(fight, component) -> {
				int compHits = fight.getCompetitor().getRobeHits();
				int compTotal = fight.getOpponent().getAttackCount() - fight.getOpponent().getTotalMagicAttackCount();
				double compRatio = compTotal > 0 ? (double) compHits / compTotal : 0.0;
				component.updateLeftCell(String.valueOf(compHits)
					, compRatio < ((double) fight.getOpponent().getRobeHits() / Math.max(1, fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount())) ?
						PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
				);

				int oppHits = fight.getOpponent().getRobeHits();
				int oppTotal = fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount();
				double oppRatio = oppTotal > 0 ? (double) oppHits / oppTotal : 0.0;
				component.updateRightCell(String.valueOf(oppHits)
					, oppRatio < compRatio ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor()
				);
			}
		);

		KO_CHANCES.init(
			(fight, oppFight) -> { // returns FightPerformancePanel component
				// Total KO Chances
				// Calculate total KO chances and overall probability // MODIFIED Calculation
				int competitorKoChances = 0;
				double competitorSurvivalProb = 1.0; // Start with 100% survival chance
				int opponentKoChances = 0;
				double opponentSurvivalProb = 1.0; // Start with 100% survival chance
				List<FightLogEntry> logs = fight.getAllFightLogEntries();
				for (FightLogEntry log : logs)
				{
					Double koChance = log.getKoChance();
					if (koChance != null)
					{
						if (log.attackerName.equals(fight.competitor.getName()))
						{
							competitorKoChances++;
							competitorSurvivalProb *= (1.0 - koChance); // Calculate survival prob
						}
						else
						{
							opponentKoChances++;
							opponentSurvivalProb *= (1.0 - koChance); // Calculate survival prob
						}
					}
				}

				// Calculate overall KO probability
				Double competitorOverallKoProb = (competitorKoChances > 0) ? (1.0 - competitorSurvivalProb) : 0;
				Double opponentOverallKoProb = (opponentKoChances > 0) ? (1.0 - opponentSurvivalProb) : 0;

				String compTotalKoChanceText = competitorKoChances + (competitorOverallKoProb > 0 ? " (" + nfP.format(competitorOverallKoProb) + ")" : ""); // Use overall prob
				String oppTotalKoChanceText = opponentKoChances + (opponentOverallKoProb > 0 ? " (" + nfP.format(opponentOverallKoProb) + ")" : ""); // Use overall prob

				boolean showGhostBarrages = fight.competitor.getGhostBarrageCount() > 0 || fight.opponent.getGhostBarrageCount() > 0;

				return PanelFactory.createStatsLine(KO_CHANCES.acronym, KO_CHANCES.acronymTooltip
					, compTotalKoChanceText
					, fight.competitor.getName() + " got " + competitorKoChances + " KO attempts with an overall KO probability of " + nfP1.format(competitorOverallKoProb)
					, (competitorOverallKoProb > opponentOverallKoProb ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor())

					, oppTotalKoChanceText
					, fight.opponent.getName() + " got " + opponentKoChances + " KO attempts with an overall KO probability of " + nfP1.format(opponentOverallKoProb)
					, (opponentOverallKoProb > competitorOverallKoProb ? PvpColorScheme.successColor() : PvpColorScheme.unsuccessColor())
					, showGhostBarrages // only show bottom border if we're showing ghost barrages, otherwise it's last statistic so don't need it.
				);
			},
			() -> PanelFactory.createOverlayStatsLine(KO_CHANCES.acronym, 50, 50,
				NO_DATA_SHORT, PvpColorScheme.unsuccessColor(), NO_DATA_SHORT, PvpColorScheme.unsuccessColor()),
			(fight, component) -> component.updateLeftRightCells(
				fight.getCompetitorKoChanceCount() + (fight.getCompetitorKoChanceCount() > 0
					? " (" + nfP.format(fight.getCompetitorTotalKoChance()) + ")"
					: ""),
				PvpColorScheme.unsuccessColor(),
				fight.getOpponentKoChanceCount() + (fight.getOpponentKoChanceCount() > 0
					? " (" + nfP.format(fight.getOpponentTotalKoChance()) + ")"
					: ""),
				PvpColorScheme.unsuccessColor()
			)
		);

		GHOST_BARRAGES.init(
			(fight, oppFight) -> { // returns FightPerformancePanel component
				String oppGhostBarrageText = NO_DATA;
				String oppGhostBarrageTooltipText = "No data is available for the opponent's ghost barrages";
				Color oppGhostBarrageColor = PvpColorScheme.getGbColor();

				if (oppFight != null)
				{
					Fighter oppComp = oppFight.getCompetitor();

					oppGhostBarrageText = (oppComp.getGhostBarrageStats());
					oppGhostBarrageTooltipText = ("(Advanced): " + oppComp.getName() + " hit " + oppComp.getGhostBarrageCount()
						+ " ghost barrages during the fight, worth an extra " + nf2.format(oppComp.getGhostBarrageExpectedDamage())
						+ " expected damage.<br>Unless fighting in PvP Arena, your opponent likely had a similar value.");
					oppGhostBarrageColor = (oppFight.getCompetitor().getGhostBarrageExpectedDamage() > fight.competitor.getGhostBarrageExpectedDamage()
						? PvpColorScheme.successColor() : PvpColorScheme.getGbColor());
				}

				return PanelFactory.createStatsLine(GHOST_BARRAGES.acronym, GHOST_BARRAGES.acronymTooltip
					, fight.competitor.getGhostBarrageStats()
					, ("(Advanced): " + fight.competitor.getName() + " hit " + fight.competitor.getGhostBarrageCount()
						+ " ghost barrages during the fight, worth an extra " + nf2.format(fight.competitor.getGhostBarrageExpectedDamage())
						+ " expected damage.<br>Unless fighting in PvP Arena, your opponent likely had a similar value.")
					, ((oppFight != null
						&& fight.competitor.getGhostBarrageExpectedDamage() > oppFight.getCompetitor().getGhostBarrageExpectedDamage())
						? PvpColorScheme.successColor() : PvpColorScheme.getGbColor())

					, oppGhostBarrageText
					, oppGhostBarrageTooltipText
					, oppGhostBarrageColor
					, false // TODO: if we move ghost barrage from being the bottom-most statistic,
					// then move this to only exclude the bottom border on the new last line.
				);
			},
			() -> PanelFactory.createOverlayStatsLine(GHOST_BARRAGES.acronym, 80, 20,
				NO_DATA_SHORT, PvpColorScheme.getGbColor(), NO_DATA_SHORT, PvpColorScheme.getGbColor()),
			(fight, component) -> component.updateLeftCellText(fight.getCompetitor().getGhostBarrageStats())
		);

		// ensure all statistics have been initialized, or else plenty of things will break.
		for (TrackedStatistic stat : TrackedStatistic.values())
		{
			if (!stat.initialized)
			{
				throw new InvalidParameterException("TrackedStatistic: An enum value failed to be initialized.");
			}
		}
	}


	private boolean initialized = false;

	@Getter
	private String name;
	@Getter
	private String acronym;
	@Getter
	private String acronymTooltip;

	private BiFunction<FightPerformance, FightPerformance, JPanel> getPanelComponent;

	public JPanel getPanelComponent(FightPerformance f, FightPerformance oppFight)
	{
		return this.getPanelComponent.apply(f, oppFight);
	}

	private Supplier<TableComponent> getOverlayComponent;

	public TableComponent getOverlayComponent()
	{
		return this.getOverlayComponent.get();
	}

	private BiConsumer<FightPerformance, TableComponent> updateOverlayComponent;

	public void updateOverlayComponent(FightPerformance f, TableComponent t)
	{
		this.updateOverlayComponent.accept(f, t);
	}

	TrackedStatistic(String name, String acronym, String acronymTooltip)
	{
		this.name = name;
		this.acronym = acronym;
		this.acronymTooltip = acronymTooltip;

	}

	private void init(BiFunction<FightPerformance, FightPerformance, JPanel> getPanelComponent,
					  Supplier<TableComponent> getOverlayComponent,
					  BiConsumer<FightPerformance, TableComponent> updateOverlayComponent)
	{
		this.getPanelComponent = getPanelComponent;
		this.getOverlayComponent = getOverlayComponent;
		this.updateOverlayComponent = updateOverlayComponent;
		this.initialized = true;
	}

	public String getPrefixedAcronymTooltip()
	{
		return ("<br><br><b><i>" + this.acronym + "</i></b>: " + this.acronymTooltip);
	}
}
