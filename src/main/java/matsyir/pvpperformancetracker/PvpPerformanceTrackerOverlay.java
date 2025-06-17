/*
 * Copyright (c)  2020, Matsyir <https://github.com/matsyir>
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.math.RoundingMode; // Added import
import java.text.NumberFormat; // Added import
import java.util.ArrayList; // Added import
import javax.inject.Inject;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.models.FightLogEntry; // Added import
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.Overlay;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class PvpPerformanceTrackerOverlay extends Overlay
{
	private static final NumberFormat nfPercent = NumberFormat.getPercentInstance(); // For KO Chance %
	static
	{
		nfPercent.setMaximumFractionDigits(1);
		nfPercent.setRoundingMode(RoundingMode.HALF_UP);
	}

	private final PanelComponent panelComponent = new PanelComponent();
	private final PvpPerformanceTrackerPlugin plugin;
	private final PvpPerformanceTrackerConfig config;

	private TitleComponent overlayTitle;

	// The main overlay is like the panel, each line is optionally turned off.
	private LineComponent ovlPlayerNamesLine; // Left: player's RSN, Right: Opponent RSN
	private LineComponent ovlOffPrayLine; // left: player's off-pray stats, right: opponent's off-pray stats
	private LineComponent ovlDeservedDmgLine; // left: player's deserved dps stats, right: opponent's deserved dps stats
	private LineComponent ovlDmgDealtLine; // left: player's damage dealt stats, right: opponent's damage dealt stats
	private LineComponent ovlMagicLuckLine; // left: player's magic attacks hit stats, right: opponent's magic attacks hit stats
	private LineComponent ovlOffensivePrayLine; // left: player's offensive pray stats, right: opponent's offensive pray stats
	private LineComponent ovlHpHealedLine; // left: player's hp healed pray stats, right: opponent's hp healed
	private LineComponent ovlRobeHitsLine; // Line for hits on robes
	private LineComponent ovlTotalKoChanceLine; // Combined total/sum KO chance
	private LineComponent ovlLastKoChanceLine; // Combined last KO chance

	// let's keep ghost barrage as the bottom-most statistic:
	// It's only relevant to people fighting in PvP Arena, and it's mostly only relevant
	// to people who can share their tracker with each-other - so pretty rarely useful.
	private LineComponent ovlGhostBarrageLine; // left: player's ghost barrage stats, right: opponent's ghost barrage stats

	@Inject
	private PvpPerformanceTrackerOverlay(PvpPerformanceTrackerPlugin plugin, PvpPerformanceTrackerConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.BOTTOM_RIGHT);
		setPriority(OverlayPriority.LOW);
		getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "PvP Performance Tracker"));
		panelComponent.setPreferredSize(new Dimension(ComponentConstants.STANDARD_WIDTH, 0));

		overlayTitle = TitleComponent.builder().text("PvP Performance").build();

		ovlPlayerNamesLine = LineComponent.builder().build();
		ovlOffPrayLine = LineComponent.builder().build();
		ovlDeservedDmgLine = LineComponent.builder().build();
		ovlDmgDealtLine = LineComponent.builder().build();
		ovlMagicLuckLine = LineComponent.builder().build();
		ovlOffensivePrayLine = LineComponent.builder().build();
		ovlOffensivePrayLine.setLeftColor(Color.WHITE); // this is static so set onload
		ovlOffensivePrayLine.setRight("N/A"); // static
		ovlOffensivePrayLine.setRightColor(Color.WHITE); // static
		ovlHpHealedLine = LineComponent.builder().build();
		ovlHpHealedLine.setLeftColor(Color.WHITE); // this is static so set onload
		ovlHpHealedLine.setRight("N/A"); // static
		ovlHpHealedLine.setRightColor(Color.WHITE); // static
		ovlTotalKoChanceLine = LineComponent.builder().build(); // Initialize new lines
		ovlLastKoChanceLine = LineComponent.builder().build(); // Initialize new lines
		ovlRobeHitsLine = LineComponent.builder().build(); // Initialize robes hits line

		ovlGhostBarrageLine = LineComponent.builder().build();
		ovlGhostBarrageLine.setLeft("N/A"); // not static but possibly unused for some full fights
		ovlGhostBarrageLine.setLeftColor(ColorScheme.BRAND_ORANGE); // static
		ovlGhostBarrageLine.setRight("N/A"); // static
		ovlGhostBarrageLine.setRightColor(ColorScheme.BRAND_ORANGE); // static

		setLines();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Rebuild overlay lines in case config toggles have changed
		setLines();
		FightPerformance fight = plugin.getCurrentFight();
		if (!config.showFightOverlay() || fight == null || !fight.fightStarted() ||
			(config.restrictToLms() && !plugin.isAtLMS()))
		{
			return null;
		}

		// off-pray hit success stats
		ovlOffPrayLine.setLeft(fight.getCompetitor().getOffPrayStats(true));
		ovlOffPrayLine.setLeftColor(fight.competitorOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		ovlOffPrayLine.setRight(fight.getOpponent().getOffPrayStats(true));
		ovlOffPrayLine.setRightColor(fight.opponentOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);

		// Deserved damage stats
		// only show deserved damage difference on the competitor, since space is restricted here and having both
		// differences is redundant since the sign is simply flipped.
		ovlDeservedDmgLine.setLeft(fight.getCompetitor().getDeservedDmgString(fight.getOpponent()));
		ovlDeservedDmgLine.setLeftColor(fight.competitorDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);

		ovlDeservedDmgLine.setRight(String.valueOf((int)Math.round(fight.getOpponent().getDeservedDamage())));
		ovlDeservedDmgLine.setRightColor(fight.opponentDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);

		// Damage dealt stats
		// same thing for damage dealt, the difference is only on the competitor.
		ovlDmgDealtLine.setLeft(String.valueOf(fight.getCompetitor().getDmgDealtString(fight.getOpponent())));
		ovlDmgDealtLine.setLeftColor(fight.competitorDmgDealtIsGreater() ? Color.GREEN : Color.WHITE);

		ovlDmgDealtLine.setRight(String.valueOf(fight.getOpponent().getDamageDealt()));
		ovlDmgDealtLine.setRightColor(fight.opponentDmgDealtIsGreater() ? Color.GREEN : Color.WHITE);

		// magic hit stats/luck
		ovlMagicLuckLine.setLeft(String.valueOf(fight.getCompetitor().getShortMagicHitStats()));
		ovlMagicLuckLine.setLeftColor(fight.competitorMagicHitsLuckier() ? Color.GREEN : Color.WHITE);

		ovlMagicLuckLine.setRight(String.valueOf(fight.getOpponent().getShortMagicHitStats()));
		ovlMagicLuckLine.setRightColor(fight.opponentMagicHitsLuckier() ? Color.GREEN : Color.WHITE);

		ovlOffensivePrayLine.setLeft(String.valueOf(fight.getCompetitor().getOffensivePrayStats(true)));

		ovlHpHealedLine.setLeft(String.valueOf(fight.getCompetitor().getHpHealed()));

		// Update Hits on Robes overlay line
		if (config.showOverlayRobeHits())
		{
			int compHits = fight.getCompetitor().getRobeHits();
			int compTotal = fight.getOpponent().getAttackCount() - fight.getOpponent().getTotalMagicAttackCount();
			double compRatio = compTotal > 0 ? (double) compHits / compTotal : 0.0;
			String compStr = compHits + "/" + compTotal;
			ovlRobeHitsLine.setLeft(compStr);
			ovlRobeHitsLine.setLeftColor(compRatio < ((double) fight.getOpponent().getRobeHits() / Math.max(1, fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount())) ? Color.GREEN : Color.WHITE);

			int oppHits = fight.getOpponent().getRobeHits();
			int oppTotal = fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount();
			double oppRatio = oppTotal > 0 ? (double) oppHits / oppTotal : 0.0;
			String oppStr = oppHits + "/" + oppTotal;
			ovlRobeHitsLine.setRight(oppStr);
			ovlRobeHitsLine.setRightColor(oppRatio < compRatio ? Color.GREEN : Color.WHITE);
		}

		// --- KO Chance Calculation START ---
		// Format Total KO Chance Line (Using Overall Probability)
		String totalCompStr = fight.getCompetitorKoChanceCount()
				+ (fight.getCompetitorTotalKoChance() > 0 ? " (" + nfPercent.format(fight.getCompetitorTotalKoChance()) + ")" : "");
		String totalOppStr = fight.getOpponentKoChanceCount()
				+ (fight.getOpponentTotalKoChance() > 0 ? " (" + nfPercent.format(fight.getOpponentTotalKoChance()) + ")" : "");
		ovlTotalKoChanceLine.setLeft(totalCompStr);
		ovlTotalKoChanceLine.setRight(totalOppStr);

		// Format Last KO Chance Line
		String lastCompStr = (fight.getCompetitorLastKoChance() != null ? nfPercent.format(fight.getCompetitorLastKoChance()) : "-");
		String lastOppStr = (fight.getOpponentLastKoChance() != null ? nfPercent.format(fight.getOpponentLastKoChance()) : "-");
		ovlLastKoChanceLine.setLeft(lastCompStr);
		ovlLastKoChanceLine.setRight(lastOppStr);
		// --- KO Chance Calculation END ---

		ovlGhostBarrageLine.setLeft(fight.getCompetitor().getGhostBarrageStats());

		return panelComponent.render(graphics);
	}

	void setLines()
	{
		panelComponent.getChildren().clear();

		// Only display the title if it's enabled (pointless in my opinion, since you can just see
		// what the panel is displaying, but I can see it being useful if you have lots of overlays)
		if (config.showOverlayTitle())
		{
			panelComponent.getChildren().add(overlayTitle);
		}

		if (config.showOverlayNames())
		{
			panelComponent.getChildren().add(ovlPlayerNamesLine);
		}
		if (config.showOverlayOffPray())
		{
			panelComponent.getChildren().add(ovlOffPrayLine);
		}
		if (config.showOverlayDeservedDmg())
		{
			panelComponent.getChildren().add(ovlDeservedDmgLine);
		}
		if (config.showOverlayDmgDealt())
		{
			panelComponent.getChildren().add(ovlDmgDealtLine);
		}
		if (config.showOverlayMagicHits())
		{
			panelComponent.getChildren().add(ovlMagicLuckLine);
		}
		if (config.showOverlayOffensivePray())
		{
			panelComponent.getChildren().add(ovlOffensivePrayLine);
		}
		if (config.showOverlayHpHealed())
		{
			panelComponent.getChildren().add(ovlHpHealedLine);
		}
		if (config.showOverlayRobeHits())
		{
			panelComponent.getChildren().add(ovlRobeHitsLine);
		}
		// Add new KO chance lines based on config
		if (config.showOverlayTotalKoChance())
		{
			panelComponent.getChildren().add(ovlTotalKoChanceLine);
		}
		if (config.showOverlayLastKoChance())
		{
			panelComponent.getChildren().add(ovlLastKoChanceLine);
		}
		if (config.showOverlayGhostBarrage())
		{
			panelComponent.getChildren().add(ovlGhostBarrageLine);
		}
	}

	void setFight(FightPerformance fight)
	{
		String cName = fight.getCompetitor().getName();
		ovlPlayerNamesLine.setLeft(cName.substring(0, Math.min(6, cName.length())));
		String oName = fight.getOpponent().getName();
		ovlPlayerNamesLine.setRight(oName.substring(0, Math.min(6, oName.length())));
	}
}
