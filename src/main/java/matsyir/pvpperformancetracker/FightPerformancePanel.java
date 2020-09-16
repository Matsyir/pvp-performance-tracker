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
package matsyir.pvpperformancetracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.api.HeadIcon;
import net.runelite.api.SpriteID;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

// Panel to display fight performance. The first line shows player stats while the second is the opponent.
// There is a skull icon beside a player's name if they died. The usernames are fixed to the left and the
// stats are fixed to the right.

class FightPerformancePanel extends JPanel
{
	private static JFrame fightLogFrame; // save frame as static instance so there's only one at a time, to avoid window clutter.
	private static Image frameIcon;
	private static ImageIcon deathIcon;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss 'on' yyyy/MM/dd");
	private static final NumberFormat nf = NumberFormat.getInstance();
	private static final Border normalBorder;
	private static final Border hoverBorder;
	static
	{
		// initialize number format
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);

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

	// Panel to display previous fight performance data.
	// intended layout:
	//
	// Line 1: Player name - Opponent Name
	// Line 2: Player off-pray hit stats - opponent off-pray hit stats
	// Line 3: Player deserved dps stats - opponent deserved dps stats
	// Line 4: Player damage dealt - opponent damage dealt
	// Line 5: Player magic hits/deserved magic hits - opponent magic hits/deserved magic hits
	// Line 6: Player offensive pray stats - N/A (no data)
	// The greater stats will be highlighted green. In this example, the player would have all the green highlights.
	// example:
	//
	//     PlayerName      OpponentName
	//	   32/55 (58%)      28/49 (57%)
	//     176 (+12)          164 (-12)
	//     156 (-28)          184 (+28)
	//     8/7.62               11/9.21
	//     27/55 (49%)              N/A
	//
	FightPerformancePanel(FightPerformance fight)
	{
		if (frameIcon == null || deathIcon == null)
		{
			// load & rescale red skull icon used to show if a player/opponent died in a fight and as the frame icon.
			frameIcon = new ImageIcon(ImageUtil.getResourceStreamFromClass(getClass(), "/skull_red.png")).getImage();
			deathIcon = new ImageIcon(frameIcon.getScaledInstance(12, 12,  Image.SCALE_DEFAULT));
		}

		this.fight = fight;
		// save Fighters temporarily for more direct access
		Fighter competitor = fight.getCompetitor();
		Fighter opponent = fight.getOpponent();

		setLayout(new BorderLayout(5, 0));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		String tooltipText = "Ended at " + DATE_FORMAT.format(Date.from(Instant.ofEpochMilli(fight.getLastFightTime())));
		setToolTipText(tooltipText);
		setBorder(normalBorder);

		// boxlayout panel to hold each of the lines.
		JPanel fightPanel = new JPanel();
		fightPanel.setLayout(new BoxLayout(fightPanel, BoxLayout.Y_AXIS));
		fightPanel.setBackground(null);

		// FIRST LINE: both player names
		JPanel playerNamesLine = new JPanel();
		playerNamesLine.setLayout(new BorderLayout());
		playerNamesLine.setBackground(null);

		// first line LEFT: player name
		JLabel playerStatsName = new JLabel();
		if (competitor.isDead())
		{
			playerStatsName.setIcon(deathIcon);
		}
		playerStatsName.setText(competitor.getName());
		playerStatsName.setForeground(Color.WHITE);
		playerNamesLine.add(playerStatsName, BorderLayout.WEST);


		// first line RIGHT: opponent name
		JLabel opponentStatsName = new JLabel();
		if (opponent.isDead())
		{
			opponentStatsName.setIcon(deathIcon);
		}
		opponentStatsName.setText(opponent.getName());
		opponentStatsName.setForeground(Color.WHITE);
		playerNamesLine.add(opponentStatsName, BorderLayout.EAST);

		// SECOND LINE: both player's off-pray hit stats
		JPanel offPrayStatsLine = new JPanel();
		offPrayStatsLine.setLayout(new BorderLayout());
		offPrayStatsLine.setBackground(null);

		// second line LEFT: player's off-pray hit stats
		JLabel playerOffPrayStats = new JLabel();
		playerOffPrayStats.setText(competitor.getOffPrayStats());
		playerOffPrayStats.setToolTipText(competitor.getOffPraySuccessCount() + " successful off-pray attacks/" +
			competitor.getAttackCount() + " total attacks (" +
			nf.format(competitor.calculateOffPraySuccessPercentage()) + "%)");
		playerOffPrayStats.setForeground(fight.competitorOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		offPrayStatsLine.add(playerOffPrayStats, BorderLayout.WEST);

		// second line RIGHT:, opponent's off-pray hit stats
		JLabel opponentOffPrayStats = new JLabel();
		opponentOffPrayStats.setText(opponent.getOffPrayStats());
		opponentOffPrayStats.setToolTipText(opponent.getOffPraySuccessCount() + " successful off-pray attacks/" +
			opponent.getAttackCount() + " total attacks (" +
			nf.format(opponent.calculateOffPraySuccessPercentage()) + "%)");
		opponentOffPrayStats.setForeground(fight.opponentOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		offPrayStatsLine.add(opponentOffPrayStats, BorderLayout.EAST);

		// THIRD LINE: both player's deserved dps stats
		JPanel deservedDpsStatsLine = new JPanel();
		deservedDpsStatsLine.setLayout(new BorderLayout());
		deservedDpsStatsLine.setBackground(null);

		// third line LEFT: player's deserved dps stats
		JLabel playerDeservedDpsStats = new JLabel();
		playerDeservedDpsStats.setText(competitor.getDeservedDmgString(opponent));
		//playerDeservedDpsStats.setToolTipText(fight.getCompetitorDeservedDmgString(1, false) +  ": Average damage deserved based on gear/pray (difference vs opponent in brackets)");
		playerDeservedDpsStats.setToolTipText(
			competitor.getName() + " deserved to deal " + nf.format(competitor.getDeservedDamage()) +
			" damage based on gear/pray (" + competitor.getDeservedDmgString(opponent, 1, true) + " vs opponent)");
		playerDeservedDpsStats.setForeground(fight.competitorDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);
		deservedDpsStatsLine.add(playerDeservedDpsStats, BorderLayout.WEST);

		// third line RIGHT: opponent's deserved dps stats
		JLabel opponentDeservedDpsStats = new JLabel();
		opponentDeservedDpsStats.setText(opponent.getDeservedDmgString(competitor));
		//opponentDeservedDpsStats.setToolTipText(fight.getOpponentDeservedDmgString(1, false) + ": Average damage deserved based on gear/pray (difference vs opponent in brackets)");
		opponentDeservedDpsStats.setToolTipText(
			opponent.getName() + " deserved to deal " + nf.format(opponent.getDeservedDamage()) +
			" damage based on gear/pray (" + opponent.getDeservedDmgString(competitor, 1, true) + " vs you)");
		opponentDeservedDpsStats.setForeground(fight.opponentDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);
		deservedDpsStatsLine.add(opponentDeservedDpsStats, BorderLayout.EAST);

		// FOURTH LINE: both player's damage dealt
		JPanel dmgDealtStatsLine = new JPanel();
		dmgDealtStatsLine.setLayout(new BorderLayout());
		dmgDealtStatsLine.setBackground(null);

		// fourth line LEFT: player's damage dealt
		JLabel playerDmgDealtStats = new JLabel();
		playerDmgDealtStats.setText(competitor.getDmgDealtString(opponent));
		playerDmgDealtStats.setToolTipText(competitor.getName() + " dealt " + competitor.getDamageDealt() +
			" damage (" + competitor.getDmgDealtString(opponent, true) + " vs opponent)");
		playerDmgDealtStats.setForeground(fight.competitorDmgDealtIsGreater() ? Color.GREEN : Color.WHITE);
		dmgDealtStatsLine.add(playerDmgDealtStats, BorderLayout.WEST);

		// fourth line RIGHT: opponent's damage dealt
		JLabel opponentDmgDealtStats = new JLabel();
		opponentDmgDealtStats.setText(String.valueOf(opponent.getDamageDealt()));
		opponentDmgDealtStats.setToolTipText(opponent.getName() + " dealt " + opponent.getDamageDealt() +
			" damage (" + opponent.getDmgDealtString(competitor, true) + " vs you)");
		opponentDmgDealtStats.setForeground(fight.opponentDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);
		dmgDealtStatsLine.add(opponentDmgDealtStats, BorderLayout.EAST);

		// FIFTH LINE: both player's magic hit stats (successful magic attacks/deserved successful magic attacks)
		JPanel magicHitStatsLine = new JPanel();
		magicHitStatsLine.setLayout(new BorderLayout());
		magicHitStatsLine.setBackground(null);

		// fifth line LEFT: player's magic hit stats
		JLabel playerMagicHitStats = new JLabel();
		playerMagicHitStats.setText(String.valueOf(competitor.getMagicHitStats()));
		playerMagicHitStats.setToolTipText(competitor.getName() + " hit " +
			competitor.getMagicHitCount() + " magic attacks, but deserved to hit " +
			nf.format(competitor.getMagicHitCountDeserved()));
		playerMagicHitStats.setForeground(fight.competitorMagicHitsLuckier() ? Color.GREEN : Color.WHITE);
		magicHitStatsLine.add(playerMagicHitStats, BorderLayout.WEST);

		// fifth line RIGHT: opponent's magic hit stats
		JLabel opponentMagicHitStats = new JLabel();
		opponentMagicHitStats.setText(String.valueOf(opponent.getMagicHitStats()));
		opponentMagicHitStats.setToolTipText(opponent.getName() + " hit " +
			opponent.getMagicHitCount() + " magic attacks, but deserved to hit " +
			nf.format(opponent.getMagicHitCountDeserved()));
		opponentMagicHitStats.setForeground(fight.opponentMagicHitsLuckier() ? Color.GREEN : Color.WHITE);
		magicHitStatsLine.add(opponentMagicHitStats, BorderLayout.EAST);

		// SIXTH LINE: player's offensive pray stats (only player's, no data for opponent)
		JPanel offensivePrayStatsLine = new JPanel();
		offensivePrayStatsLine.setLayout(new BorderLayout());
		offensivePrayStatsLine.setBackground(null);

		// sixth line LEFT: player's offensive pray stats
		JLabel playerOffensivePrayStats = new JLabel();
		playerOffensivePrayStats.setText(String.valueOf(competitor.getOffensivePrayStats()));
		playerOffensivePrayStats.setToolTipText(competitor.getOffensivePrayStats() + " successful offensive prayers/" +
			competitor.getAttackCount() + " total attacks (" +
			nf.format(competitor.calculateOffensivePraySuccessPercentage()) + "%)");
		playerOffensivePrayStats.setForeground(Color.WHITE);
		offensivePrayStatsLine.add(playerOffensivePrayStats, BorderLayout.WEST);

		// sixth line RIGHT: "N/A", no data.
		JLabel opponentOffensivePrayStats = new JLabel();
		opponentOffensivePrayStats.setText("N/A");
		opponentOffensivePrayStats.setToolTipText("No data is available for the opponent's offensive prayers.");
		opponentOffensivePrayStats.setForeground(Color.WHITE);
		offensivePrayStatsLine.add(opponentOffensivePrayStats, BorderLayout.EAST);

		fightPanel.add(playerNamesLine);
		fightPanel.add(offPrayStatsLine);
		fightPanel.add(deservedDpsStatsLine);
		fightPanel.add(dmgDealtStatsLine);
		fightPanel.add(magicHitStatsLine);
		fightPanel.add(offensivePrayStatsLine);

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

		// Create "Remove Fight" popup menu/context menu
		JPopupMenu popupMenu = new JPopupMenu();
		final JMenuItem removeFight = new JMenuItem("Remove Fight");
		removeFight.addActionListener(e -> PLUGIN.removeFight(fight));
		popupMenu.add(removeFight);
		setComponentPopupMenu(popupMenu);
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
		this.setBorder(visible ? hoverBorder : normalBorder);
	}

	private void createFightLogFrame()
	{
		// destroy current frame if it exists so we only have one at a time (static field)
		if (fightLogFrame != null)
		{
			fightLogFrame.dispose();
		}

		ArrayList<FightLogEntry> fightLogEntries = fight.getAllFightLogEntries();
		if (fightLogEntries == null || fightLogEntries.size() < 1)
		{
			PLUGIN.createConfirmationModal("Info", "There are no fight log entries available for this fight.");
			return;
		}
		String title = fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName();
		fightLogFrame = new JFrame(title);
		// if always on top is supported, and the core RL plugin has "always on top" set, make the frame always
		// on top as well so it can be above the client.
		if (fightLogFrame.isAlwaysOnTopSupported())
		{
			fightLogFrame.setAlwaysOnTop(PLUGIN.getRuneliteConfig().gameAlwaysOnTop());
		}

		fightLogFrame.setIconImage(frameIcon);
		fightLogFrame.setSize(765, 503); // default to same as osrs on fixed
		fightLogFrame.setLocation(FightPerformancePanel.this.getRootPane().getLocationOnScreen());

		JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
		Object[][] stats = new Object[fightLogEntries.size()][11];
		int i = 0;
		long initialTime = 0;

		for (FightLogEntry fightEntry : fightLogEntries)
		{
			if (i == 0)
			{
				initialTime = fightEntry.getTime();
			}
			int styleIcon;
			if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.RANGED)
			{
				styleIcon = SpriteID.SKILL_RANGED;
			}
			else if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC)
			{
				styleIcon = SpriteID.SKILL_MAGIC;
			}
			else
			{
				styleIcon = SpriteID.SKILL_ATTACK;
			}
			int prayIcon = 0;
			boolean noOverhead = false;
			if (fightEntry.getDefenderOverhead() == HeadIcon.RANGED)
			{
				prayIcon = SpriteID.PRAYER_PROTECT_FROM_MISSILES;
			}
			else if (fightEntry.getDefenderOverhead() == HeadIcon.MAGIC)
			{
				prayIcon = SpriteID.PRAYER_PROTECT_FROM_MAGIC;
			}
			else if (fightEntry.getDefenderOverhead() == HeadIcon.MELEE)
			{
				prayIcon = SpriteID.PRAYER_PROTECT_FROM_MELEE;
			}
			else
			{
				noOverhead = true;
			}

			BufferedImage styleIconRendered = PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(styleIcon, 0);
			stats[i][0] = fightEntry.getAttackerName();
			stats[i][1] = styleIconRendered;
			stats[i][2] = fightEntry.getHitRange();
			stats[i][3] = nf.format(fightEntry.getAccuracy() * 100) + '%';
			stats[i][4] = nf.format(fightEntry.getDeservedDamage());
			stats[i][5] = fightEntry.getAnimationData().isSpecial ? "✔" : "";
			stats[i][6] = fightEntry.success() ? "✔" : "";
			stats[i][7] = noOverhead ? "" : PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(prayIcon, 0);

			if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC)
			{
				int freezeIcon = fightEntry.isSplash() ? SpriteID.SPELL_ICE_BARRAGE_DISABLED : SpriteID.SPELL_ICE_BARRAGE;
				BufferedImage freezeIconRendered = PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(freezeIcon, 0);
				stats[i][8] = freezeIconRendered;
			}
			else
			{
				stats[i][8] = "";
			}

			// offensive pray shown as icon or blank if none(0)
			stats[i][9] = fightEntry.getAttackerOffensivePray() <= 0 ? "" :
				PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(fightEntry.getAttackerOffensivePray(), 0);

			long durationLong = fightEntry.getTime() - initialTime;
			Duration duration = Duration.ofMillis(durationLong);
			String time = String.format("%02d:%02d.%01d",
				duration.toMinutes(),
				duration.getSeconds() % 60,
				durationLong % 1000 / 100);
			stats[i][10] = time;

			i++;
		}

		String[] header = { "Attacker", "Style", "Hit Range", "Accuracy", "Avg Hit", "Special?", "Off-Pray?", "Def Prayer", "Splash", "Offensive Pray", "Time" };
		JTable table = new JTable(stats, header);
		table.setRowHeight(30);
		table.setDefaultEditor(Object.class, null);

		table.getColumnModel().getColumn(1).setCellRenderer(new BufferedImageCellRenderer());
		table.getColumnModel().getColumn(7).setCellRenderer(new BufferedImageCellRenderer());
		table.getColumnModel().getColumn(8).setCellRenderer(new BufferedImageCellRenderer());
		table.getColumnModel().getColumn(9).setCellRenderer(new BufferedImageCellRenderer());

		mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

		fightLogFrame.add(mainPanel);
		fightLogFrame.setVisible(true);
	}

	static class BufferedImageCellRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setText("");
			setIcon(value instanceof BufferedImage ? new ImageIcon((BufferedImage)value) : null);

			return this;
		}
	}
}
