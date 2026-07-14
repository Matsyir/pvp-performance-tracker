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
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.IntSupplier;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import lombok.Getter;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPanel;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.FightPerformanceSerializer;
import matsyir.pvpperformancetracker.controllers.Fighter;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.models.TrackedStatistic;
import matsyir.pvpperformancetracker.utils.PvpColorScheme;
import matsyir.pvpperformancetracker.utils.PvpUtils;
import matsyir.pvpperformancetracker.utils.WorldFlag;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.WorldService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

// Panel to display fight performance. The first line shows player stats while the second is the opponent.
// There is a skull icon beside a player's name if they died. The usernames are fixed to the left and the
// stats are fixed to the right
public class FightPerformancePanel extends JPanel
{
	private static final String PVP_HUB_HOST = "osrs.pvp-hub.com";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss 'on' yyyy-MM-dd");

	public static final Color BG_COLOR_FOR_IMG = PvpColorScheme.EMPTY;
	public static final Color BG_COLOR = ColorScheme.DARKER_GRAY_COLOR;
	public static Color CURRENT_BG_COLOR = BG_COLOR_FOR_IMG;
	public static void updateCurrentBgColor()
	{
		if (CONFIG == null) { return; }

		CURRENT_BG_COLOR = !CONFIG.hidePanelBgImages()
			? BG_COLOR_FOR_IMG
			: BG_COLOR;
	}

	private static final int BOTTOM_SPACING_PX = 4; // vertical px gap between fights

	// border size: 0px left, 0px right, 8px top, 4px bottom (plus extra 4px bottom invisible offset)
	// The primary background color under the panels is ColorScheme.SCROLL_TRACK_COLOR, but
	// the main background, on PvpPerformanceTrackerPanel, is BORDER_COLOR


	// enum of background styles "bgStyle" images to be rendered as backgrounds, below all the stats.
	// currently sized for what the panel size is with all statistics shown (so including GB). This is so
	// that we only ever scale the image down, instead of stretch it. Scaling it down can still come with
	// its issues, but since we're only doing it a bit, it seems fine so far. We could always include a normal
	// version alongside the GB version in the future, if we really want.
	//
	// Uses a basic background by default, detects "special" or "rare" events in fights to apply fancier
	// backgrounds or borders. You can also filter/search for these names (bgStyle.name) in the filter
	// textbox on the panel. For example, you can search for "maxhit" or "max hit" to see max hit KOs.
	// You could also search for just "max" to see max hit ko + max spec hit ko
	@Getter
	public enum BackgroundStyle
	{
		DEFAULT("Default", ColorScheme.TEXT_COLOR, "GB_Grayscale", "GB_Orange_Hovered"),
		MAX_HIT_KO("Max Hit KO", Color.RED, "GB_Red"),
		SPEC_KO("Spec KO", new Color(17, 146, 178), "GB_Cyan"),
		MAX_SPEC_KO("Max Spec Ko", Color.MAGENTA.darker(), "GB_Purple"),
		PUNCH_KO("Punch KO", DEFAULT, false),
		KICK_KO("Kick KO", DEFAULT, false),
		STAFF_KO("Staff KO", DEFAULT, false);

		// if your filter is exactly equal to one of these filter words, then we'll display any fights with a
		// BackgroundStyle, provided it's enabled & not Default.
		public static final String PRESET_FILTER_STYLE_KEYWORD = "::borders";
		public static final String PRESET_FILTER_STYLE_KEYWORD_NO_SPEC = "::border";

		final String name;
		final Color highlightColor;
		final BufferedImage bgImg;
		final BufferedImage hoverBgImg;
		final boolean enabled;

		BackgroundStyle(String name, BackgroundStyle copyingStyle, boolean enabled)
		{
			this(name, copyingStyle.highlightColor, copyingStyle.bgImg, copyingStyle.hoverBgImg, enabled);
		}
		BackgroundStyle(String name, Color highlightColor, String fname)
		{
			this(name, highlightColor, fname, fname + "_Hovered");
		}
		BackgroundStyle(String name, Color highlightColor, String fname, String fnameHovered)
		{
			this(name, highlightColor, fname, fnameHovered, true);
		}
		BackgroundStyle(String name, Color highlightColor, String fname, String fnameHovered, boolean enabled)
		{
			this(name,
				highlightColor,
				ImageUtil.loadImageResource(PLUGIN.getClass(),
					"/panelBackgrounds/fightPerformancePanelBgStyles/" + fname + ".png"),
				ImageUtil.loadImageResource(PLUGIN.getClass(),
					"/panelBackgrounds/fightPerformancePanelBgStyles/" + fnameHovered + ".png"),
				enabled
			);
		}
		BackgroundStyle(String name, Color highlightColor, BufferedImage bgImg, BufferedImage hoverBgImg, boolean enabled)
		{
			this.name = name;
			this.highlightColor = highlightColor;
			this.bgImg = bgImg;
			this.hoverBgImg = hoverBgImg;
			this.enabled = enabled;
		}
	}

	@Getter
	@Inject
	private ClientThread clientThread;

	private boolean hovered = false;
	private BackgroundStyle bgStyle = BackgroundStyle.DEFAULT;

	// Panel to display previous fight performance data.
	// intended layout:
	//
	// Player name ; 									Opponent Name
	// Player off-pray hit stats ; 						Opponent off-pray hit stats
	// Player expected dps stats ; 						Opponent expected dps stats
	// Player damage dealt ; 							Opponent damage dealt
	// Player magic hits/expected magic hits ; 			Opponent magic hits/expected magic hits
	// Player offensive pray stats ; 					N/A (no data, client only)
	// Player hp healed ; 								N/A (no data, client only)
	// Player Hits on robes ;							Opponent Hits on robes
	// Player Total KO Chances ;						Opponent Total KO Chances
	// Player Ghost barrages & extra expected damage ; 	N/A (no data, client only)
	// The greater stats will be highlighted green. In this example, the player would have all the green highlights.
	// example:
	//
	//     PlayerName      OpponentName
	//	   32/55 (58%)      28/49 (57%) // off-pray
	//     176 (+12)          164 (-12) // expected
	//     156 (-28)          184 (+28) // dealt
	//     10/16 (92.7)   12/20 (80.7%) // expected magic
	//     27/55 (49%)              N/A // offensive pray
	//     100                      N/A // hp healed
	//	   11/31 (35.5%)  13/34 (38.2%) // hits on robes
	//     2 (110.0%)         1 (65.0%) // ko chances
	//     4 G.B. (37)				N/A // ghost barrages
	//

	public FightPerformancePanel(PvpPerformanceTrackerPanel pluginPanel, FightPerformance fight, WorldService worldService, IntSupplier worldLocationSupplier, boolean showOriginal, boolean enableActions)
	{
		boolean pvpHubSynced = fight.hasPvpHubSyncedFight();
		FightPerformance displayFight = showOriginal ? fight : fight.getPvpHubDisplayFight();

		// save Fighters temporarily for more direct access
		Fighter competitor = displayFight.getCompetitor();
		Fighter opponent = displayFight.getOpponent();

		setLayout(new BorderLayout(0, 0));


		final String baseTooltipText = competitor.getName() + " vs. " + opponent.getName() + ": This fight ended at " +
			DATE_FORMAT.format(Date.from(Instant.ofEpochMilli(displayFight.getLastFightTime())));
		final String worldDisplayTooltipText = (displayFight.getWorld() > 0
			? " (" + WorldFlag.getTooltip(displayFight.getWorld(), worldService, worldLocationSupplier.getAsInt()) + ")"
			: "");

		bgStyle = fight.getBgStyle();

		// border size: 0px left, 0px right, 8px top, 4px bottom (plus extra 4px bottom invisible offset)
		// The primary background color under the panels is ColorScheme.SCROLL_TRACK_COLOR, but
		// the main background, on PvpPerformanceTrackerPanel, is BORDER_COLOR
		Border border = PanelFactory.combineBorders(
			BorderFactory.createMatteBorder(0, 0, BOTTOM_SPACING_PX, 0, ColorScheme.SCROLL_TRACK_COLOR),
			PanelFactory.createGradientBorder(2, 2, 2, 2,
				ColorUtil.colorWithAlpha(bgStyle.highlightColor, 96),
				ColorUtil.colorWithAlpha(bgStyle.highlightColor, 32)),
			BorderFactory.createEmptyBorder(6, 0, 2, 0));
		Border hoverBorder = PanelFactory.combineBorders(
			BorderFactory.createMatteBorder(0, 0, BOTTOM_SPACING_PX, 0, ColorScheme.SCROLL_TRACK_COLOR),
			PanelFactory.createGradientBorder(2, 2, 2, 2,
				ColorUtil.colorWithAlpha(bgStyle == BackgroundStyle.DEFAULT ? ColorScheme.BRAND_ORANGE : bgStyle.highlightColor, 164),
				ColorUtil.colorWithAlpha(bgStyle == BackgroundStyle.DEFAULT ? ColorScheme.BRAND_ORANGE : bgStyle.highlightColor, 64)),
			BorderFactory.createEmptyBorder(6, 0, 2, 0));

		setBorder(border);

		final String bgStyleDisplayTooltipText;
		if (bgStyle != BackgroundStyle.DEFAULT && bgStyle.enabled)
		{
			bgStyleDisplayTooltipText = "<br><br>" +
				"<strong>Border Style: </strong><u>" + bgStyle.name + "</u>";
		}
		else
		{
			bgStyleDisplayTooltipText = "";
		}
		final String fullTooltipText = (baseTooltipText + worldDisplayTooltipText + bgStyleDisplayTooltipText);
		setToolTipText("<html>" + fullTooltipText);

		ArrayList<JPanel> panelLines = new ArrayList<>();

		// boxlayout panel to hold each of the lines.
		JPanel fightPanel = new JPanel();
		fightPanel.setLayout(new BoxLayout(fightPanel, BoxLayout.Y_AXIS));
		fightPanel.setBackground(CURRENT_BG_COLOR);

		// FIRST LINE: both player names, with centered world flag or label/hidden depending on config
		ImageIcon worldIcon = null;
		if (displayFight.getWorld() > 0)
		{
			int worldLocation = worldLocationSupplier.getAsInt();
			worldIcon = WorldFlag.getIcon(displayFight.getWorld(), worldService, worldLocation);
		}
		JPanel playerNamesLine = PanelFactory.createPlayerNamesStatsLine(fight, fullTooltipText, worldIcon);
		panelLines.add(playerNamesLine);

		boolean showGhostBarrages = competitor.getGhostBarrageCount() > 0 || opponent.getGhostBarrageCount() > 0;
		for (TrackedStatistic stat : TrackedStatistic.values())
		{
			if (stat == TrackedStatistic.GHOST_BARRAGES && !showGhostBarrages)
			{
				continue;
			}

			panelLines.add(stat.getPanelComponent(displayFight));
		}

		// setup mouse events for hovering and clicking to open the fight log
		MouseAdapter fightPerformanceMouseListener = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (enableActions)
				{
					hovered = true;
					setCursor(new Cursor(Cursor.HAND_CURSOR));
					setBorder(hoverBorder);
					repaint();
				}

			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hovered = false;
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				setBorder(border);

				repaint();
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				// ignore right clicks since that should be used for context menus/popup menus.
				// btn1: left click, btn2: middle click, btn3: right click
				if (!enableActions || e.getButton() == MouseEvent.BUTTON3)
				{
					return;
				}

				FightLogFrame.createFightLogFrame(fight, getRootPane());
			}
		};
		addMouseListener(fightPerformanceMouseListener);

		// We are setting the background colors to transparent here,
		// this is required even with the image background, otherwise some of the backgrounds are opaque by default and
		// cover the image. This ensures everything is transparent and doesn't cover the image.
		setFullBackgroundColor(CURRENT_BG_COLOR);

		JPopupMenu popupMenu = null;
		if (enableActions)
		{
			popupMenu = new JPopupMenu();

			// Create "Show fight log" menu (same action as left click)
			final JMenuItem displayFightLog = new JMenuItem("Display Fight Log");
			displayFightLog.addActionListener(e -> FightLogFrame.createFightLogFrame(fight, getRootPane()));

			final JMenuItem displayOriginalFightLog = new JMenuItem("Display Original Fight Log");
			displayOriginalFightLog.addActionListener(e -> FightLogFrame.createOriginalFightLogFrame(fight, getRootPane()));

			// Create "Show attack summary" menu
			final JMenuItem displayAttackSummary = new JMenuItem("Display Attack Summary");
			displayAttackSummary.addActionListener(e -> AttackSummaryFrame.createAttackSummaryFrame(displayFight, getRootPane()));

			// Create "Favorite/De-Favorite fight" menu
			final JMenuItem favoriteFightToggleMenuItem = new JMenuItem();
			updateFavoriteFightMenuItem(favoriteFightToggleMenuItem, fight);
			ActionListener favoriteFightAction = e ->
			{
				PLUGIN.getClientThread().invokeLater(() ->
				{
					if (!fight.isFavorite())
					{
						FightPerformanceSerializer.serializeFavoriteFight(fight);
					}
					else
					{
						FightPerformanceSerializer.deFavoriteFight(fight);
						pluginPanel.enqueueRebuild();
					}

					updateFavoriteFightMenuItem(favoriteFightToggleMenuItem, fight);
				});
			};
			favoriteFightToggleMenuItem.addActionListener(favoriteFightAction);

			// Create "Open on PvP-Hub" popup menu/context menu
			final JMenuItem openOnPvpHub = new JMenuItem("<html>&#8599;&nbsp;<u>Open on PvP-Hub Website</u></html>");
			openOnPvpHub.addActionListener(e -> openOnPvpHub(fight));
			openOnPvpHub.setForeground(PvpColorScheme.BLUE_TEXT_URL);
			try
			{
				openOnPvpHub.setToolTipText(PvpUtils.getUrlButtonTooltip(buildPvpHubUrl(fight)));
			}
			catch (Exception ignored) { }

			// Create "Copy Fight Data" popup menu/context menu
			final JMenuItem copyFight = new JMenuItem("<html>&#10063;&nbsp;Copy Fight JSON (Advanced)");
			copyFight.addActionListener(e -> PLUGIN.exportFightAsJson(fight));
			copyFight.setForeground(PvpColorScheme.GREEN_TEXT_ACTION_WHITER);

			// Create "Hide Fight (until reload)" popup menu/context menu
			final JMenuItem hideFight = new JMenuItem("<html>&#128065;&nbsp;Hide Fight (until reload)");
			hideFight.addActionListener(e ->
			{
				PLUGIN.removeFight(fight, false);
			});
			hideFight.setForeground(PvpColorScheme.ORANGE_TEXT_ACTION);

			// Create "Remove Fight" popup menu/context menu
			final JMenuItem removeFightPermanently = new JMenuItem("<html><b>&#128465;&nbsp;Delete Fight Permanently</b>");
			removeFightPermanently.setForeground(PvpColorScheme.RED_TEXT_WARNING_ACTION);
			removeFightPermanently.addActionListener(e ->
			{
				int dialogResult = JOptionPane.showConfirmDialog(this,
					"<html>Are you sure you want to remove this fight, <b><u>permanently</u></b>? This cannot be undone.",
					"Warning: PvP Performance Tracker - Delete Fight Permanently?", JOptionPane.YES_NO_OPTION);
				if (dialogResult == JOptionPane.YES_OPTION)
				{
					PLUGIN.removeFight(fight);
				}
			});


			popupMenu.add(displayFightLog);
			if (pvpHubSynced)
			{
				popupMenu.add(displayOriginalFightLog);
			}
			popupMenu.add(displayAttackSummary);
			popupMenu.add(favoriteFightToggleMenuItem);
			if (hasPvpHubUrl(fight))
			{
				popupMenu.add(openOnPvpHub);
			}
			popupMenu.add(copyFight);

			// if the fight wasn't loaded from file, then it's not saved yet, and can only be deleted permanently
			// at this moment, so only display the "hide fight (until reload)" option if it's already written
			if (fight.isSavedToFile())
			{
				popupMenu.add(hideFight);
			}
			popupMenu.add(removeFightPermanently);
			setComponentPopupMenu(popupMenu);
		}

		for (JPanel line : panelLines)
		{
			fightPanel.add(line);
			line.addMouseListener(fightPerformanceMouseListener);
			line.setComponentPopupMenu(popupMenu);
		}

		add(fightPanel, BorderLayout.NORTH);

		setMaximumSize(new Dimension(PvpPerformanceTrackerPanel.FIGHT_PERFORMANCE_PANEL_WIDTH, (int) getPreferredSize().getHeight()));
	}

	private void setFullBackgroundColor(Color color)
	{
		this.setBackground(color);
		for (Component c : getComponents())
		{
			c.setBackground(color);
		}
	}

	private boolean hasPvpHubUrl(FightPerformance fight)
	{
		return fight != null
			&& fight.getFightId() != null
			&& !fight.getFightId().trim().isEmpty()
			&& fight.getCompetitor() != null
			&& fight.getCompetitor().getName() != null
			&& !fight.getCompetitor().getName().trim().isEmpty();
	}

	private void openOnPvpHub(FightPerformance fight)
	{
		if (!hasPvpHubUrl(fight))
		{
			return;
		}

		try
		{
			LinkBrowser.browse(buildPvpHubUrl(fight));
		}
		catch (URISyntaxException e)
		{
			PLUGIN.createConfirmationModal(false, "Could not create the PvP-Hub URL for this fight.");
		}
	}

	private String buildPvpHubUrl(FightPerformance fight) throws URISyntaxException
	{
		String playerName = CONFIG.hideRsnOnPvpHub()
			? PLUGIN.getPvpHubHiddenName()
			: fight.getCompetitor().getName();
		String path = "/" + playerName.trim() + "/" + fight.getFightId().trim();
		return new URI("https", PVP_HUB_HOST, path, null).toASCIIString();
	}

	private void updateFavoriteFightMenuItem(JMenuItem item, FightPerformance fight)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (fight.isFavorite())
			{
				item.setText("<html>&#9734;&nbsp;<i>Un-Favorite Fight");
				item.setForeground(PvpColorScheme.FAVORITE_GOLD.darker());
			}
			else
			{
				item.setText("<html>&#9733;&nbsp;Favorite Fight");
				item.setForeground(PvpColorScheme.FAVORITE_GOLD);
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		// Draw bgImage scaled to panel size (ideally should be the same anyway)
		if (!CONFIG.hidePanelBgImages() && hovered && bgStyle.hoverBgImg != null)
		{
			g.drawImage(bgStyle.hoverBgImg, 0, 0, getWidth(), getHeight() - BOTTOM_SPACING_PX, this);
		}
		else if (!CONFIG.hidePanelBgImages() && bgStyle.bgImg != null)
		{
			g.drawImage(bgStyle.bgImg, 0, 0, getWidth(), getHeight() - BOTTOM_SPACING_PX, this);
		}

		super.paintComponent(g);
	}
}
