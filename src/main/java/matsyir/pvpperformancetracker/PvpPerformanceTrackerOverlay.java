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
import javax.inject.Inject;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
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

	// The main overlay is like the panel, each line is optionally turned off.
	private LineComponent overlayFirstLine; // Left: player's RSN, Right: Opponent RSN
	private LineComponent overlaySecondLine; // left: player's off-pray stats, right: opponent's off-pray stats
	private LineComponent overlayThirdLine; // left: player's deserved dps stats, right: opponent's deserved dps stats
	private LineComponent overlayFourthLine; // left: player's damage dealt stats, right: opponent's damage dealt stats
	private LineComponent overlayFifthLine; // left: player's magic attacks hit stats, right: opponent's magic attacks hit stats
	private LineComponent overlaySixthLine; // left: player's offensive pray stats, right: opponent's offensive pray stats
	private LineComponent overlaySeventhLine; // left: player's hp healed pray stats, right: opponent's hp healed

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

		overlayFirstLine = LineComponent.builder().build();
		overlaySecondLine = LineComponent.builder().build();
		overlayThirdLine = LineComponent.builder().build();
		overlayFourthLine = LineComponent.builder().build();
		overlayFifthLine = LineComponent.builder().build();
		overlaySixthLine = LineComponent.builder().build();
		overlaySixthLine.setLeftColor(Color.WHITE); // this is static so set onload
		overlaySixthLine.setRight("N/A"); // static
		overlaySixthLine.setRightColor(Color.WHITE); // static
		overlaySeventhLine = LineComponent.builder().build();
		overlaySeventhLine.setLeftColor(Color.WHITE); // this is static so set onload
		overlaySeventhLine.setRight("N/A"); // static
		overlaySeventhLine.setRightColor(Color.WHITE); // static

		setLines();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		FightPerformance fight = plugin.getCurrentFight();
		if (fight == null || !fight.fightStarted() || !config.showFightOverlay() ||
			(config.restrictToLms() && !plugin.isAtLMS()))
		{
			return null;
		}

		// Second line: off-pray hit success stats
		overlaySecondLine.setLeft(fight.getCompetitor().getOffPrayStats(true));
		overlaySecondLine.setLeftColor(fight.competitorOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		overlaySecondLine.setRight(fight.getOpponent().getOffPrayStats(true));
		overlaySecondLine.setRightColor(fight.opponentOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);

		// Third line: Deserved damage stats
		// only show deserved damage difference on the competitor, since space is restricted here and having both
		// differences is redundant since the sign is simply flipped.
		overlayThirdLine.setLeft(fight.getCompetitor().getDeservedDmgString(fight.getOpponent()));
		overlayThirdLine.setLeftColor(fight.competitorDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);

		overlayThirdLine.setRight(String.valueOf((int)Math.round(fight.getOpponent().getDeservedDamage())));
		overlayThirdLine.setRightColor(fight.opponentDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);

		// Fouth line: Damage dealt stats
		// same thing for damage dealt, the difference is only on the competitor.
		overlayFourthLine.setLeft(String.valueOf(fight.getCompetitor().getDmgDealtString(fight.getOpponent())));
		overlayFourthLine.setLeftColor(fight.competitorDmgDealtIsGreater() ? Color.GREEN : Color.WHITE);

		overlayFourthLine.setRight(String.valueOf(fight.getOpponent().getDamageDealt()));
		overlayFourthLine.setRightColor(fight.opponentDmgDealtIsGreater() ? Color.GREEN : Color.WHITE);

		// Fifth line: magic hit stats/luck
		overlayFifthLine.setLeft(String.valueOf(fight.getCompetitor().getMagicHitStats()));
		overlayFifthLine.setLeftColor(fight.competitorMagicHitsLuckier() ? Color.GREEN : Color.WHITE);

		overlayFifthLine.setRight(String.valueOf(fight.getOpponent().getMagicHitStats()));
		overlayFifthLine.setRightColor(fight.opponentMagicHitsLuckier() ? Color.GREEN : Color.WHITE);

		overlaySixthLine.setLeft(String.valueOf(fight.getCompetitor().getOffensivePrayStats(true)));

		overlaySeventhLine.setLeft(String.valueOf(fight.getCompetitor().getHpHealed()));

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
			panelComponent.getChildren().add(overlayFirstLine);
		}
		if (config.showOverlayOffPray())
		{
			panelComponent.getChildren().add(overlaySecondLine);
		}
		if (config.showOverlayDeservedDmg())
		{
			panelComponent.getChildren().add(overlayThirdLine);
		}
		if (config.showOverlayDmgDealt())
		{
			panelComponent.getChildren().add(overlayFourthLine);
		}
		if (config.showOverlayMagicHits())
		{
			panelComponent.getChildren().add(overlayFifthLine);
		}
		if (config.showOverlayOffensivePray())
		{
			panelComponent.getChildren().add(overlaySixthLine);
		}
		if (config.showOverlayHpHealed())
		{
			panelComponent.getChildren().add(overlaySeventhLine);
		}

	}

	void setFight(FightPerformance fight)
	{
		String cName = fight.getCompetitor().getName();
		overlayFirstLine.setLeft(cName.substring(0, Math.min(6, cName.length())));
		String oName = fight.getOpponent().getName();
		overlayFirstLine.setRight(oName.substring(0, Math.min(6, oName.length())));
	}
}
