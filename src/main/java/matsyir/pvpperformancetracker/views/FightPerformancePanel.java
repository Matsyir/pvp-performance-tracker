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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
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
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import lombok.Getter;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPanel;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN_ICON;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.Fighter;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.models.TrackedStatistic;
import matsyir.pvpperformancetracker.utils.PvpColorScheme;
import matsyir.pvpperformancetracker.utils.WorldFlag;
import net.runelite.client.game.WorldService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

// Panel to display fight performance. The first line shows player stats while the second is the opponent.
// There is a skull icon beside a player's name if they died. The usernames are fixed to the left and the
// stats are fixed to the right.

public class FightPerformancePanel extends JPanel
{
	private static final String PVP_HUB_HOST = "osrs.pvp-hub.com";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss 'on' yyyy/MM/dd");

	private static final Color BG_COLOR = new Color(0, 0, 0, 0);

	private static final int BOTTOM_SPACING_PX = 4; // vertical px gap between fights
	private static final int DEATH_ICON_SIDE_SPACING_PX = 12;

	// load & rescale red skull icon used to show if a player/opponent died in a fight and as the frame icon.
	public static final ImageIcon deathIcon = new ImageIcon(PLUGIN_ICON.getScaledInstance(12, 12, Image.SCALE_DEFAULT));

	private static final Border paddingBorder;
	// border size: 0px left, 0px right, 5px top, 4px bottom (plus extra 4px bottom invisible offset)
	// also note for border: The primary background color of the FightPerformancePanel is DARKER_GRAY_COLOR,
	// and the primary background color PvpPerformanceTrackerPanel is DARK_GRAY_COLOR
	static
	{
		paddingBorder = BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, BOTTOM_SPACING_PX, 0, ColorScheme.SCROLL_TRACK_COLOR),
			BorderFactory.createEmptyBorder(5, 0, 4, 0));
	}

	@Getter
	public enum BackgroundStyle
	{
		DEFAULT("Default", ColorScheme.BRAND_ORANGE, "GB_Default"),
		MAX_HIT_KO("Max Hit KO", Color.RED, "GB_Red"),
		SPEC_KO("Spec KO", new Color(57, 125, 59), "GB_Green"),
		MAX_SPEC_KO("Max Spec Ko", Color.MAGENTA.darker(), "GB_Purple"),
		PUNCH_KO("Punch KO", DEFAULT, false),
		KICK_KO("Kick KO", DEFAULT, false),
		STAFF_KO("Staff KO", DEFAULT, false);

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
			this(name, highlightColor, fname, true);
		}
		BackgroundStyle(String name, Color highlightColor, String fname, boolean enabled)
		{
			this(name,
				highlightColor,
				ImageUtil.loadImageResource(PLUGIN.getClass(), "/panelBackgrounds/fightPerformancePanelBgStyles/" + fname + ".png"),
				ImageUtil.loadImageResource(PLUGIN.getClass(), "/panelBackgrounds/fightPerformancePanelBgStyles/" + fname + "_Hovered.png"),
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

	private boolean hovered = false;
	private BackgroundStyle bgStyle = BackgroundStyle.DEFAULT;
	private final String bgStyleDisplayTooltipText;

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
	// these are the params to use for a normal panel.
	public FightPerformancePanel(FightPerformance fight)
	{
		this(fight, null);
	}

	public FightPerformancePanel(FightPerformance fight, WorldService worldService)
	{
		this(fight, false, null, worldService, -1);
	}

	public FightPerformancePanel(FightPerformance fight, WorldService worldService, int worldLocation)
	{
		this(fight, false, null, worldService, worldLocation);
	}

	public FightPerformancePanel(FightPerformance fight, WorldService worldService, IntSupplier worldLocationSupplier)
	{
		this(fight, false, null, worldService, worldLocationSupplier);
	}

	public FightPerformancePanel(FightPerformance fight, boolean showActions, boolean showBorders, boolean showOpponentClientStats, FightPerformance oppFight)
	{
		this(fight, showOpponentClientStats, oppFight, null, -1);
	}

	public FightPerformancePanel(FightPerformance fight, boolean showOpponentClientStats, FightPerformance oppFight, WorldService worldService, int worldLocation)
	{
		this(fight, showOpponentClientStats, oppFight, worldService, () -> worldLocation);
	}

	public FightPerformancePanel(FightPerformance fight, boolean showOpponentClientStats, FightPerformance oppFight, WorldService worldService, IntSupplier worldLocationSupplier)
	{
		boolean pvpHubSynced = fight.hasPvpHubSyncedFight();
		FightPerformance displayFight = fight.getPvpHubDisplayFight();

		// save Fighters temporarily for more direct access
		Fighter competitor = displayFight.getCompetitor();
		Fighter opponent = displayFight.getOpponent();
		boolean competitorDied = competitor.isDead();
		boolean opponentDied = opponent.isDead();

		setLayout(new BorderLayout(0, 0));


		final String baseTooltipText = "<html>" + competitor.getName() + " vs. " + opponent.getName() + ": This fight ended at " +
			DATE_FORMAT.format(Date.from(Instant.ofEpochMilli(displayFight.getLastFightTime())));
		final String worldDisplayTooltipText = (displayFight.getWorld() > 0 ?
			" (" + WorldFlag.getTooltip(displayFight.getWorld(), worldService, worldLocationSupplier.getAsInt()) + ")" :
			"");
		setToolTipText(baseTooltipText);

		setBorder(paddingBorder);

		ArrayList<JPanel> panelLines = new ArrayList<>();

		// boxlayout panel to hold each of the lines.
		JPanel fightPanel = new JPanel();
		fightPanel.setLayout(new BoxLayout(fightPanel, BoxLayout.Y_AXIS));
		fightPanel.setBackground(null);

		// FIRST LINE: both player names, with centered world flag
		JPanel playerNamesLine = new JPanel(new BorderLayout())
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);

				if (CONFIG.getWorldDisplayChoice() == WorldFlag.WorldDisplayChoice.HIDDEN)
				{
					return;
				}

				if (displayFight.getWorld() > 0)
				{
					int worldLocation = worldLocationSupplier.getAsInt();
					setToolTipText(baseTooltipText + worldDisplayTooltipText + bgStyleDisplayTooltipText);
					ImageIcon worldIcon = WorldFlag.getIcon(displayFight.getWorld(), worldService, worldLocation);
					boolean isDisplayingWorldLabel = CONFIG.getWorldDisplayChoice() == WorldFlag.WorldDisplayChoice.WORLD_LABEL;

					Graphics g2 = g.create();

					if (worldIcon != null)
					{
						int x = (getWidth() - worldIcon.getIconWidth()) / 2;
						int y = (getHeight() - worldIcon.getIconHeight()) / 2;
						worldIcon.paintIcon(this, g2, x, y);
					}

					if (CONFIG.getWorldDisplayChoice() == WorldFlag.WorldDisplayChoice.WORLD_LABEL)
					{
						String worldText = String.valueOf(fight.getWorld());
						g2.setFont(PanelFactory.getIndexFontFrom(g2.getFont()).deriveFont(13f));
						FontMetrics fm = g2.getFontMetrics(g2.getFont());
						int x = (getWidth() - fm.stringWidth(worldText)) / 2;
						int y = (getHeight() + fm.getAscent()) / 2 - fm.getDescent();

						g2.translate(1, 1);
						g2.setColor(Color.BLACK);
						g2.drawString(worldText, x, y); // draw text shadow 1/3

						g2.translate(-2, 0);
						g2.drawString(worldText, x, y); // draw text shadow 2/3
						g2.translate(1, 1);
						g2.drawString(worldText, x, y); // draw text shadow 2/3

						g2.translate(0, -2);
						g2.setColor(Color.WHITE);

						g2.drawString(worldText, x, y); // draw text

						g2.dispose();
					}
				}
			}
		};
		playerNamesLine.setBackground(null);
		playerNamesLine.setBorder(PanelFactory.bottomLineBorder);

		// player names
		JShadowedLabel playerStatsName = new JShadowedLabel();
		playerStatsName.setForeground(PvpColorScheme.neutralColor());
		playerStatsName.setHorizontalAlignment(SwingConstants.CENTER);

		JShadowedLabel opponentStatsName = new JShadowedLabel();
		opponentStatsName.setForeground(PvpColorScheme.neutralColor());
		opponentStatsName.setHorizontalAlignment(SwingConstants.CENTER);

		// player names line LEFT: player name
		playerStatsName.setText(competitor.getName());
		playerStatsName.setPreferredSize(new Dimension(PanelFactory.PREFERRED_LABEL_WIDTH, playerStatsName.getPreferredSize().height));
		playerNamesLine.add(playerStatsName, BorderLayout.WEST);

		// player names line RIGHT: opponent name
		opponentStatsName.setText(opponent.getName());
		opponentStatsName.setPreferredSize(new Dimension(PanelFactory.PREFERRED_LABEL_WIDTH, opponentStatsName.getPreferredSize().height));
		playerNamesLine.add(opponentStatsName, BorderLayout.EAST);

		panelLines.add(playerNamesLine);

		boolean showGhostBarrages = competitor.getGhostBarrageCount() > 0 || opponent.getGhostBarrageCount() > 0;
		for (TrackedStatistic stat : TrackedStatistic.values())
		{
			if (stat == TrackedStatistic.GHOST_BARRAGES && !showGhostBarrages)
			{
				continue;
			}

			panelLines.add(stat.getPanelComponent(displayFight, oppFight));
		}

		// setup mouse events for hovering and clicking to open the fight log
		MouseAdapter fightPerformanceMouseListener = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				hovered = true;
				setCursor(new Cursor(Cursor.HAND_CURSOR));

				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hovered = false;
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

				repaint();
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

				FightLogFrame.createFightLogFrame(displayFight, getRootPane(), pvpHubSynced);
			}
		};
		addMouseListener(fightPerformanceMouseListener);

		// We are setting the background colors to transparent here,
		// this is required even with the image background, otherwise some of the backgrounds are opaque by default and
		// cover the image. This ensures everything is transparent and doesn't cover the image.
		setFullBackgroundColor(BG_COLOR);

		JPopupMenu popupMenu = new JPopupMenu();

		// Create "Show fight log" menu (same action as left click)
		final JMenuItem displayFightLog = new JMenuItem("Display Fight Log");
		displayFightLog.addActionListener(e -> FightLogFrame.createFightLogFrame(displayFight, getRootPane(), pvpHubSynced));

		final JMenuItem displayOriginalFightLog = new JMenuItem("Display Original Fight Log");
		displayOriginalFightLog.addActionListener(e -> FightLogFrame.createFightLogFrame(fight, getRootPane()));

		// Create "Show attack summary" menu
		final JMenuItem displayAttackSummary = new JMenuItem("Display Attack Summary");
		displayAttackSummary.addActionListener(e -> AttackSummaryFrame.createAttackSummaryFrame(displayFight, getRootPane()));

		// Create "Open on PvP-Hub" popup menu/context menu
		final JMenuItem openOnPvpHub = new JMenuItem("<html><u>Open on PvP-Hub Website</u>&nbsp;&#8599;</html>");
		openOnPvpHub.addActionListener(e -> openOnPvpHub(fight));
		openOnPvpHub.setForeground(ColorScheme.GRAND_EXCHANGE_LIMIT);

		// Create "Copy Fight Data" popup menu/context menu
		final JMenuItem copyFight = new JMenuItem("Copy Fight Data (Advanced)");
		copyFight.addActionListener(e -> PLUGIN.exportFight(fight));
		copyFight.setForeground(ColorScheme.BRAND_ORANGE);

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
		removeFight.setForeground(PvpColorScheme.RED_TEXT_ACTION);

		popupMenu.add(displayFightLog);
		if (pvpHubSynced)
		{
			popupMenu.add(displayOriginalFightLog);
		}
		popupMenu.add(displayAttackSummary);
		if (hasPvpHubUrl(fight))
		{
			popupMenu.add(openOnPvpHub);
		}
		popupMenu.add(copyFight);
		popupMenu.add(removeFight);
		setComponentPopupMenu(popupMenu);

		for (JPanel line : panelLines)
		{
			fightPanel.add(line);
			line.addMouseListener(fightPerformanceMouseListener);
			line.setComponentPopupMenu(popupMenu);
		}

		add(fightPanel, BorderLayout.NORTH);

		setMaximumSize(new Dimension(PvpPerformanceTrackerPanel.FIGHT_PERFORMANCE_PANEL_WIDTH, (int) getPreferredSize().getHeight()));

		bgStyle = fight.getBgStyle();

		if (bgStyle != BackgroundStyle.DEFAULT && bgStyle.enabled)
		{
			bgStyleDisplayTooltipText = "<br><br>" +
				"<strong>Border Style: </strong><u>" + bgStyle.name + "</u>";
			setToolTipText(baseTooltipText + bgStyleDisplayTooltipText);
		}
		else
		{
			bgStyleDisplayTooltipText = "";
		}

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

	@Override
	protected void paintComponent(Graphics g)
	{
		// Draw bgImage scaled to panel size (ideally should be the same anyway)
		if (hovered && bgStyle.hoverBgImg != null)
		{
			g.drawImage(bgStyle.hoverBgImg, 0, 0, getWidth(), getHeight() - BOTTOM_SPACING_PX, this);
		}
		else if (bgStyle.bgImg != null)
		{
			g.drawImage(bgStyle.bgImg, 0, 0, getWidth(), getHeight() - BOTTOM_SPACING_PX, this);
		}

		super.paintComponent(g);
	}
}
