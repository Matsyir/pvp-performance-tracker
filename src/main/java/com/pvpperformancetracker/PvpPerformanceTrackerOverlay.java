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
package com.pvpperformancetracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
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
	private final PanelComponent panelComponent = new PanelComponent();
	private final PvpPerformanceTrackerPlugin plugin;
	private final PvpPerformanceTrackerConfig config;

	private TitleComponent overlayTitle;

	private LineComponent simpleConfigOverlayFirstLine; // Left: player's RSN, Right: off-pray %
	private LineComponent simpleConfigOverlaySecondLine; // Same as above but for opponent

	// The main overlay is like the panel.
	private LineComponent overlayFirstLine; // Left: player's RSN, Right: Opponent RSN
	private LineComponent overlaySecondLine; // left: player's off-pray stats, right: opponent's off-pray stats
	private LineComponent overlayThirdLine; // right: player's deserved dps stats, right: opponent's deserved dps stats

	private boolean didRender;

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

		simpleConfigOverlayFirstLine = LineComponent.builder().build();
		simpleConfigOverlaySecondLine = LineComponent.builder().build();

		overlayFirstLine = LineComponent.builder().build();
		overlaySecondLine = LineComponent.builder().build();
		overlayThirdLine = LineComponent.builder().build();

		setLines();

		didRender = false;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		FightPerformance fight = plugin.getCurrentFight();
		if (fight == null || !fight.fightStarted() || !config.showFightOverlay() ||
			(config.restrictToLms() && !plugin.isAtLMS()))
		{
			didRender = false;
			return null;
		}

		// Adjust size to fix potential text overlap due to long RSN if displaying full RSN, on first render.
		if (!didRender)
		{
			if (config.useSimpleOverlay())
			{
				FontMetrics metrics = graphics.getFontMetrics();
				panelComponent.setPreferredSize(new Dimension(
					Math.max(ComponentConstants.STANDARD_WIDTH,
						Math.max(metrics.stringWidth(fight.getCompetitor().getName()),
							metrics.stringWidth(fight.getOpponent().getName()))
							+ metrics.stringWidth("100%") + 6),
					0));
			}
			else
			{
				panelComponent.setPreferredSize(new Dimension(ComponentConstants.STANDARD_WIDTH, 0));
			}

			didRender = true;
		}

		if (config.useSimpleOverlay())
		{
			simpleConfigOverlayFirstLine.setRight(Math.round(fight.getCompetitor().calculateSuccessPercentage()) + "%");
			simpleConfigOverlayFirstLine.setRightColor(fight.competitorOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);

			simpleConfigOverlaySecondLine.setRight(Math.round(fight.getOpponent().calculateSuccessPercentage()) + "%");
			simpleConfigOverlaySecondLine.setRightColor(fight.opponentOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		}
		else
		{
			overlaySecondLine.setLeft(fight.getCompetitor().getOffPrayStats(true));
			overlaySecondLine.setLeftColor(fight.competitorOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
			overlaySecondLine.setRight(fight.getOpponent().getOffPrayStats(true));
			overlaySecondLine.setRightColor(fight.opponentOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);

			overlayThirdLine.setLeft(fight.getCompetitorDeservedDpsString());
			overlayThirdLine.setLeftColor(fight.competitorDeservedDpsIsGreater() ? Color.GREEN : Color.WHITE);

			// only show damage for the opponent, since space is restricted here and having both differences
			// is redundant since the sign is simply flipped.
			overlayThirdLine.setRight(String.valueOf(fight.getOpponent().getTotalDamage()));
			overlayThirdLine.setRightColor(fight.opponentDeservedDpsIsGreater() ? Color.GREEN : Color.WHITE);
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

		if (config.useSimpleOverlay())
		{
			panelComponent.getChildren().add(simpleConfigOverlayFirstLine);
			panelComponent.getChildren().add(simpleConfigOverlaySecondLine);
		}
		else
		{
			panelComponent.getChildren().add(overlayFirstLine);
			panelComponent.getChildren().add(overlaySecondLine);
			panelComponent.getChildren().add(overlayThirdLine);
		}
	}

	void setFight(FightPerformance fight)
	{
		simpleConfigOverlayFirstLine.setLeft(fight.getCompetitor().getName());
		simpleConfigOverlaySecondLine.setLeft(fight.getOpponent().getName());

		String cName = fight.getCompetitor().getName();
		overlayFirstLine.setLeft(cName.substring(0, Math.min(6, cName.length())));
		String oName = fight.getOpponent().getName();
		overlayFirstLine.setRight(oName.substring(0, Math.min(6, oName.length())));

		didRender = false;
	}
}
