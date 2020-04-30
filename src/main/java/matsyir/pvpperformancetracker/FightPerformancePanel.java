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

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.nio.Buffer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

import net.runelite.api.HeadIcon;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.text.WordUtils;

// Panel to display fight performance. The first line shows player stats while the second is the opponent.
// There is a skull icon beside a player's name if they died. The usernames are fixed to the left and the
// stats are fixed to the right.

class FightPerformancePanel extends JPanel
{
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss 'on' yyyy/MM/dd");
	private static final NumberFormat nf = NumberFormat.getInstance();
	static // initialize number format
	{
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);
	}

	private void matchComponentBackground(JPanel panel, Color color)
	{
		panel.setBackground(color);
		for (Component c : panel.getComponents())
		{
			c.setBackground(color);
		}
	}


	// Panel to display previous fight performance data.
	// intended layout:
	//
	// Line 1: Player name - Opponent Name
	// Line 2: Player off-pray hit stats - opponent off-pray hit stats
	// Line 3: Player deserved dps stats - opponent deserved dps stats
	// The greater stats will be highlighted green. In this example, the player would have all the green highlights.
	// example:
	//
	//     PlayerName      OpponentName
	//	   32/55 (58%)     28/49 (57%)
	//     176 (+12)       164 (-12)
	//
	FightPerformancePanel(FightPerformance fight)
	{
		// save Fighters temporarily for more direct access
		Fighter competitor = fight.getCompetitor();
		Fighter opponent = fight.getOpponent();

		setLayout(new BorderLayout(5, 5));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		String tooltipText = "Ended at " + DATE_FORMAT.format(Date.from(Instant.ofEpochMilli(fight.getLastFightTime())));
		setToolTipText(tooltipText);

		Color background = getBackground();
		setBorder(new EmptyBorder(6, 6, 6, 6));

		// load & rescale red skull icon used to show if a player/opponent died in a fight.
		ImageIcon icon = new ImageIcon(new ImageIcon(
				ImageUtil.getResourceStreamFromClass(getClass(), "/skull_red.png"))
				.getImage()
				.getScaledInstance(12, 12, Image.SCALE_DEFAULT));
		Image image = icon.getImage();
		Image scaledImg = image.getScaledInstance(12, 12,  Image.SCALE_DEFAULT);
		icon = new ImageIcon(scaledImg);

		// boxlayout panel to hold each of the lines.
		JPanel fightPanel = new JPanel();
		fightPanel.setLayout(new BoxLayout(fightPanel, BoxLayout.Y_AXIS));
		fightPanel.setBackground(background);

		// FIRST LINE: both player names
		JPanel playerNamesLine = new JPanel();
		playerNamesLine.setLayout(new BorderLayout());
		playerNamesLine.setBackground(background);

		// first line LEFT: player name
		JLabel playerStatsName = new JLabel();
		if (competitor.isDead())
		{
			playerStatsName.setIcon(icon);
		}
		playerStatsName.setText(competitor.getName());
		playerStatsName.setForeground(Color.WHITE);
		playerNamesLine.add(playerStatsName, BorderLayout.WEST);


		// first line RIGHT: opponent name
		JLabel opponentStatsName = new JLabel();
		if (opponent.isDead())
		{
			opponentStatsName.setIcon(icon);
		}
		opponentStatsName.setText(opponent.getName());
		opponentStatsName.setForeground(Color.WHITE);
		playerNamesLine.add(opponentStatsName, BorderLayout.EAST);

		// SECOND LINE: both player's off-pray hit stats
		JPanel offPrayStatsLine = new JPanel();
		offPrayStatsLine.setLayout(new BorderLayout());
		offPrayStatsLine.setBackground(background);

		// second line LEFT: player's off-pray hit stats
		JLabel playerOffPrayStats = new JLabel();
		playerOffPrayStats.setText(competitor.getOffPrayStats());
		playerOffPrayStats.setToolTipText(competitor.getSuccessCount() + " successful off-pray attacks/" +
			competitor.getAttackCount() + " total attacks (" +
			nf.format(competitor.calculateSuccessPercentage()) + "%)");
		playerOffPrayStats.setForeground(fight.competitorOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		offPrayStatsLine.add(playerOffPrayStats, BorderLayout.WEST);

		// second line RIGHT:, opponent's off-pray hit stats
		JLabel opponentOffPrayStats = new JLabel();
		opponentOffPrayStats.setText(opponent.getOffPrayStats());
		opponentOffPrayStats.setToolTipText(opponent.getSuccessCount() + " successful off-pray attacks/" +
			opponent.getAttackCount() + " total attacks (" +
			nf.format(opponent.calculateSuccessPercentage()) + "%)");
		opponentOffPrayStats.setForeground(fight.opponentOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		offPrayStatsLine.add(opponentOffPrayStats, BorderLayout.EAST);


		// THIRD LINE: both player's deserved dps stats
		JPanel deservedDpsStatsLine = new JPanel();
		deservedDpsStatsLine.setLayout(new BorderLayout());
		deservedDpsStatsLine.setBackground(background);

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
		dmgDealtStatsLine.setBackground(background);

		// fourth line LEFT: player's damage dealt
		JLabel playerDmgDealtStats = new JLabel();
		playerDmgDealtStats.setText(competitor.getDmgDealtString(opponent));
		playerDmgDealtStats.setToolTipText(competitor.getName() + " dealt " + competitor.getDamageDealt() +
			" damage (" + competitor.getDmgDealtString(opponent, true) + "vs opponent)");
		playerDmgDealtStats.setForeground(fight.competitorDmgDealtIsGreater() ? Color.GREEN : Color.WHITE);
		dmgDealtStatsLine.add(playerDmgDealtStats, BorderLayout.WEST);

		// fourth line RIGHT: opponent's damage dealt
		JLabel opponentDmgDealtStats = new JLabel();
		opponentDmgDealtStats.setText(String.valueOf(opponent.getDamageDealt()));
		opponentDmgDealtStats.setToolTipText(opponent.getName() + " dealt " + opponent.getDamageDealt() +
			" damage (" + opponent.getDmgDealtString(competitor, true) + "vs you)");
		opponentDmgDealtStats.setForeground(fight.opponentDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);
		dmgDealtStatsLine.add(opponentDmgDealtStats, BorderLayout.EAST);

		// FIFTH LINE: both player's magic hit stats (successful magic attacks/deserved successful magic attacks)
		JPanel magicHitStatsLine = new JPanel();
		magicHitStatsLine.setLayout(new BorderLayout());
		magicHitStatsLine.setBackground(background);

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

		fightPanel.add(playerNamesLine);
		fightPanel.add(offPrayStatsLine);
		fightPanel.add(deservedDpsStatsLine);
		fightPanel.add(dmgDealtStatsLine);
		fightPanel.add(magicHitStatsLine);

		add(fightPanel, BorderLayout.NORTH);

		// setup mouse events for hovering and clicking to open
		MouseAdapter itemPanelMouseListener = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				matchComponentBackground(fightPanel, ColorScheme.DARK_GRAY_HOVER_COLOR);
				setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
				setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				matchComponentBackground(fightPanel, ColorScheme.DARKER_GRAY_COLOR);
				setBackground(ColorScheme.DARKER_GRAY_COLOR);
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				String title = competitor.getName() + " vs " + opponent.getName();
				JFrame x = new JFrame(title);
				x.setPreferredSize(new Dimension(400, 300));
				JPanel mainPanel = new JPanel(new BorderLayout(4,4));
				Object [][] stats = new Object[fight.sortFights().size()][10];
				int i = 0;
				long initialTime = 0;

				for (FightLogEntry fightEntry : fight.sortFights()) {
					if (i == 0) {
						initialTime = fightEntry.getTime();
					}
					int styleIcon;
					if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.RANGED) {
						styleIcon = SpriteID.COMBAT_STYLE_CROSSBOW_RAPID;
					} else if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC) {
						styleIcon = SpriteID.COMBAT_STYLE_MAGIC_RAPID;
					} else {
						styleIcon = SpriteID.COMBAT_STYLE_SWORD_SLASH;
					}
					int prayIcon = 0;
					boolean noOverhead = false;
					if (fightEntry.getDefenderOverhead() == HeadIcon.RANGED) {
						prayIcon = SpriteID.PRAYER_PROTECT_FROM_MISSILES;
					} else if (fightEntry.getDefenderOverhead() == HeadIcon.MAGIC) {
						prayIcon = SpriteID.PRAYER_PROTECT_FROM_MAGIC;
					}  else if (fightEntry.getDefenderOverhead() == HeadIcon.MELEE) {
						prayIcon = SpriteID.PRAYER_PROTECT_FROM_MELEE;
					} else {
						noOverhead = true;
					}
					int freezeIcon = fightEntry.isSplash() ? SpriteID.SPELL_ICE_BARRAGE_DISABLED : SpriteID.SPELL_ICE_BARRAGE;
					BufferedImage freezeIconRendered = PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(freezeIcon, 0);
					BufferedImage styleIconRendered = PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(styleIcon, 0);
					stats[i][0] = fightEntry.getAttackerName();
					stats[i][1] = styleIconRendered;
					stats[i][2] = fightEntry.getHitRange();
					stats[i][3] = nf.format(fightEntry.getAccuracy() * 100) + '%';
					stats[i][4] = nf.format(fightEntry.getDeservedDamage());
					stats[i][5] = fightEntry.getAnimationData().isSpecial ? "✔" : "";
					stats[i][6] = fightEntry.success() ? "✔" : "✖";
					stats[i][7] = noOverhead ? "" : PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(prayIcon, 0);
					if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC) {
						stats[i][8] = freezeIconRendered;
					} else {
						stats[i][8] = "";

					}
					stats[i][9] = nf.format((fightEntry.getTime() - initialTime) / 1000);
					i++;
				}

				String[] header = { "Attacker", "Style", "Hit", "Acc", "AvgHit", "Spec?", "OffP?", "D Prayer", "Splash", "Time" };
				JTable table = new JTable(stats, header);
				table.getColumnModel().getColumn(1).setCellRenderer(new BufferedImageCellRenderer());
				table.getColumnModel().getColumn(7).setCellRenderer(new BufferedImageCellRenderer());
				table.getColumnModel().getColumn(8).setCellRenderer(new BufferedImageCellRenderer());
				table.setRowHeight(30);
				mainPanel.add(new JScrollPane(table));

				x.setSize(550, 400);
				x.add(mainPanel);
				x.setVisible(true);
			}
		};

addMouseListener(itemPanelMouseListener);
	}
}

class BufferedImageCellRenderer extends DefaultTableCellRenderer {
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (value instanceof BufferedImage) {
			setIcon(new ImageIcon((BufferedImage)value));
			setText("");
		} else {
			setText("");
		}
		return this;
	}
}
