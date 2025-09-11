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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
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
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.Fighter;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;

import matsyir.pvpperformancetracker.models.TrackedStatistic;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

// Panel to display fight performance. The first line shows player stats while the second is the opponent.
// There is a skull icon beside a player's name if they died. The usernames are fixed to the left and the
// stats are fixed to the right.

public class FightPerformancePanel extends JPanel
{
	private static ImageIcon deathIcon;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss 'on' yyyy/MM/dd");
	private static final Border normalBorder;
	private static final Border hoverBorder;
	static
	{
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

	private AnalyzedFightPerformance analyzedFight;
	private boolean showBorders;

	// Panel to display previous fight performance data.
	// intended layout:
	//
	// Player name ; 									Opponent Name
	// Player off-pray hit stats ; 						Opponent off-pray hit stats
	// Player average dps stats ; 						Opponent average dps stats
	// Player damage dealt ; 							Opponent damage dealt
	// Player magic hits/average magic hits ; 			Opponent magic hits/average magic hits
	// Player offensive pray stats ; 					N/A (no data, client only)
	// Player hp healed ; 								N/A (no data, client only)
	// Player Hits on robes ;							Opponent Hits on robes
	// Player Total KO Chances ;						Opponent Total KO Chances
	// Player Ghost barrages & extra average damage ; 	N/A (no data, client only)
	// The greater stats will be highlighted green. In this example, the player would have all the green highlights.
	// example:
	//
	//     PlayerName      OpponentName
	//	   32/55 (58%)      28/49 (57%) // off-pray
	//     176 (+12)          164 (-12) // avg dmg
	//     156 (-28)          184 (+28) // dealt
	//     10/16 (92.7)   12/20 (80.7%) // avg magic hits
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

	public FightPerformancePanel(FightPerformance fight, boolean showActions, boolean showBorders, boolean showOpponentClientStats, FightPerformance oppFight) {
		this.showBorders = showBorders;
		if (deathIcon == null) {
			// load & rescale red skull icon used to show if a player/opponent died in a fight and as the frame icon.
			deathIcon = new ImageIcon(PLUGIN_ICON.getScaledInstance(12, 12, Image.SCALE_DEFAULT));
		}

		// save Fighters temporarily for more direct access
		Fighter competitor = fight.getCompetitor();
		Fighter opponent = fight.getOpponent();

		setLayout(new BorderLayout(5, 0));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		String baseTooltipText = competitor.getName() + " vs. " + opponent.getName() + (fight.getWorld() > 0 ? (" (W" + fight.getWorld()) + ")" : "") +
				": This fight ended at " + DATE_FORMAT.format(Date.from(Instant.ofEpochMilli(fight.getLastFightTime())));
		setToolTipText(baseTooltipText);

		if (showBorders) {
			setBorder(normalBorder);
		}

		ArrayList<JPanel> panelLines = new ArrayList<JPanel>();

		// boxlayout panel to hold each of the lines.
		JPanel fightPanel = new JPanel();
		fightPanel.setLayout(new BoxLayout(fightPanel, BoxLayout.Y_AXIS));
		fightPanel.setBackground(null);

		// FIRST LINE: both player names, with centered world label
		JPanel playerNamesLine = new JPanel(new BorderLayout()) {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (CONFIG.showWorldInSummary() && fight.getWorld() > 0) {
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
		if (competitor.isDead()) {
			playerStatsName.setIcon(deathIcon);
		}
		playerStatsName.setText(competitor.getName());
		playerStatsName.setForeground(Color.WHITE);
		playerNamesLine.add(playerStatsName, BorderLayout.WEST);

		// player name RIGHT: opponent name
		JLabel opponentStatsName = new JLabel();
		if (opponent.isDead()) {
			opponentStatsName.setIcon(deathIcon);
		}
		opponentStatsName.setText(opponent.getName());
		opponentStatsName.setForeground(Color.WHITE);
		playerNamesLine.add(opponentStatsName, BorderLayout.EAST);
		playerNamesLine.setToolTipText(baseTooltipText);

		panelLines.add(playerNamesLine);

		boolean showGhostBarrages = competitor.getGhostBarrageCount() > 0 || opponent.getGhostBarrageCount() > 0;
		for (TrackedStatistic stat : TrackedStatistic.values())
		{
			if (stat == TrackedStatistic.GHOST_BARRAGES && !showGhostBarrages) { continue; }

			panelLines.add(stat.getPanelComponent(fight, oppFight));
		}

		// setup mouse events for hovering and clicking to open the fight log
		MouseAdapter fightPerformanceMouseListener = new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				setFullBackgroundColor(ColorScheme.DARK_GRAY_COLOR);
				setOutline(true);
				setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				setFullBackgroundColor(ColorScheme.DARKER_GRAY_COLOR);
				setOutline(false);
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// ignore right clicks since that should be used for context menus/popup menus.
				// btn1: left click, btn2: middle click, btn3: right click
				if (e.getButton() == MouseEvent.BUTTON3) {
					return;
				}

				FightLogFrame.createFightLogFrame(fight, analyzedFight, getRootPane());
			}
		};
		addMouseListener(fightPerformanceMouseListener);

		// skip the remaining code if we aren't showing actions.
		if (!showActions) {
			return;
		}

		JPopupMenu popupMenu = new JPopupMenu();

		// Create "Show fight log" menu (same action as left click)
		final JMenuItem displayFightLog = new JMenuItem("Display Fight Log");
		displayFightLog.addActionListener(e -> FightLogFrame.createFightLogFrame(fight, analyzedFight, getRootPane()));

		// Create "Show attack summary" menu
		final JMenuItem displayAttackSummary = new JMenuItem("Display Attack Summary");
		displayAttackSummary.addActionListener(e -> AttackSummaryFrame.createAttackSummaryFrame(fight, getRootPane()));

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
			if (dialogResult == JOptionPane.YES_OPTION) {
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

		for (JPanel line : panelLines)
		{
			fightPanel.add(line);
			line.addMouseListener(fightPerformanceMouseListener);
			line.setComponentPopupMenu(popupMenu);
		}

		add(fightPanel, BorderLayout.NORTH);

		setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, (int) getPreferredSize().getHeight()));
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
}
