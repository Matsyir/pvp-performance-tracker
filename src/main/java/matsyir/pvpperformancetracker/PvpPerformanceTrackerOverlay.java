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
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.HashMap;
import javax.inject.Inject;

import matsyir.pvpperformancetracker.controllers.FightPerformance;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;

import matsyir.pvpperformancetracker.models.TrackedStatistic;
import matsyir.pvpperformancetracker.views.PanelFactory;
import matsyir.pvpperformancetracker.views.TableComponent;
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
	private static final String NO_DATA = "-";
	private static final NumberFormat nfP1 = NumberFormat.getPercentInstance(); // For KO Chance %
	static
	{
		nfP1.setMaximumFractionDigits(1);
		nfP1.setRoundingMode(RoundingMode.HALF_UP);
	}

	private final PanelComponent panelComponent = new PanelComponent();
	private final PvpPerformanceTrackerPlugin plugin;
	private final PvpPerformanceTrackerConfig config;

	private final TitleComponent overlayTitle;

	// The main overlay is like the panel, each line is optionally turned off.
	private final LineComponent ovlPlayerNamesLine; // Left: player's RSN, Right: Opponent RSN
	private final HashMap<TrackedStatistic, TableComponent> statisticLines = new HashMap<>();

	// weird overlay-only statistic that isn't on the panel nor considered its own "TrackedStatistic"
	private final TableComponent ovlLastKoChanceLine; // Combined last KO chance

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

		for (TrackedStatistic stat : TrackedStatistic.values())
		{
			statisticLines.put(stat, stat.getOverlayComponent());
		}

		// weird overlay-only statistic that isn't on the panel
		ovlLastKoChanceLine = PanelFactory.createOverlayStatsLine("pKO", 50, 50, NO_DATA, Color.WHITE, NO_DATA, Color.WHITE);

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

		statisticLines.forEach((stat, component) -> {
			stat.updateOverlayComponent(fight, component);
		});

		// Format Last KO Chance Line
		if (fight.getCompetitorLastKoChance() != null)
		{
			ovlLastKoChanceLine.updateLeftCellText(nfP1.format(fight.getCompetitorLastKoChance()));
		}
		if (fight.getOpponentLastKoChance() != null)
		{
			ovlLastKoChanceLine.updateRightCellText(nfP1.format(fight.getOpponentLastKoChance()));
		}

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
			panelComponent.getChildren().add(statisticLines.get(TrackedStatistic.OFF_PRAY));
		}
		if (config.showOverlayDeservedDmg())
		{
			panelComponent.getChildren().add(statisticLines.get(TrackedStatistic.AVG_DMG));
		}
		if (config.showOverlayDmgDealt())
		{
			panelComponent.getChildren().add(statisticLines.get(TrackedStatistic.DMG_DEALT));
		}
		if (config.showOverlayMagicHits())
		{
			panelComponent.getChildren().add(statisticLines.get(TrackedStatistic.MAGIC_HITS));
		}
		if (config.showOverlayOffensivePray())
		{
			panelComponent.getChildren().add(statisticLines.get(TrackedStatistic.OFFENSIVE_PRAY));
		}
		if (config.showOverlayHpHealed())
		{
			panelComponent.getChildren().add(statisticLines.get(TrackedStatistic.HP_HEALED));
		}
		if (config.showOverlayRobeHits())
		{
			panelComponent.getChildren().add(statisticLines.get(TrackedStatistic.ROBE_HITS));
		}
		// Add new KO chance lines based on config
		if (config.showOverlayTotalKoChance())
		{
			panelComponent.getChildren().add(statisticLines.get(TrackedStatistic.KO_CHANCES));
		}
		if (config.showOverlayLastKoChance())
		{
			panelComponent.getChildren().add(ovlLastKoChanceLine);
		}
		if (config.showOverlayGhostBarrage())
		{
			panelComponent.getChildren().add(statisticLines.get(TrackedStatistic.GHOST_BARRAGES));
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
