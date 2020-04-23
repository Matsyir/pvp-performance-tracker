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
import java.awt.Image;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

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
		if (fight.getCompetitor().isDead())
		{
			playerStatsName.setIcon(icon);
		}
		playerStatsName.setText(fight.getCompetitor().getName());
		playerStatsName.setForeground(Color.WHITE);
		playerNamesLine.add(playerStatsName, BorderLayout.WEST);


		// first line RIGHT: opponent name
		JLabel opponentStatsName = new JLabel();
		if (fight.getOpponent().isDead())
		{
			opponentStatsName.setIcon(icon);
		}
		opponentStatsName.setText(fight.getOpponent().getName());
		opponentStatsName.setForeground(Color.WHITE);
		playerNamesLine.add(opponentStatsName, BorderLayout.EAST);

		// SECOND LINE: both player's off-pray hit stats
		JPanel offPrayStatsLine = new JPanel();
		offPrayStatsLine.setLayout(new BorderLayout());
		offPrayStatsLine.setBackground(background);

		// second line LEFT: player's off-pray hit stats
		JLabel playerOffPrayStats = new JLabel();
		playerOffPrayStats.setText(fight.getCompetitor().getOffPrayStats());
		playerOffPrayStats.setToolTipText(fight.getCompetitor().getSuccessCount() + " successful off-pray attacks/" +
			fight.getCompetitor().getAttackCount() + " total attacks (" +
			nf.format(fight.getCompetitor().calculateSuccessPercentage()) + "%)");
		playerOffPrayStats.setForeground(fight.competitorOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		offPrayStatsLine.add(playerOffPrayStats, BorderLayout.WEST);

		// second line RIGHT:, opponent's off-pray hit stats
		JLabel opponentOffPrayStats = new JLabel();
		opponentOffPrayStats.setText(fight.getOpponent().getOffPrayStats());
		opponentOffPrayStats.setToolTipText(fight.getOpponent().getSuccessCount() + " successful off-pray attacks/" +
			fight.getOpponent().getAttackCount() + " total attacks (" +
			nf.format(fight.getOpponent().calculateSuccessPercentage()) + "%)");
		opponentOffPrayStats.setForeground(fight.opponentOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE);
		offPrayStatsLine.add(opponentOffPrayStats, BorderLayout.EAST);


		// THIRD LINE: both player's deserved dps stats
		JPanel deservedDpsStatsLine = new JPanel();
		deservedDpsStatsLine.setLayout(new BorderLayout());
		deservedDpsStatsLine.setBackground(background);

		// third line LEFT: player's deserved dps stats
		JLabel playerDeservedDpsStats = new JLabel();
		playerDeservedDpsStats.setText(fight.getCompetitorDeservedDmgString());
		//playerDeservedDpsStats.setToolTipText(fight.getCompetitorDeservedDmgString(1, false) +  ": Average damage deserved based on gear/pray (difference vs opponent in brackets)");
		playerDeservedDpsStats.setToolTipText(
			fight.getCompetitor().getName() + " deserved to deal " + fight.getCompetitor().getDeservedDamage() +
			" damage based on gear/pray (" + fight.getCompetitorDeservedDmgString(1, true) + " vs opponent)");
		playerDeservedDpsStats.setForeground(fight.competitorDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);
		deservedDpsStatsLine.add(playerDeservedDpsStats, BorderLayout.WEST);

		// third line RIGHT: opponent's deserved dps stats
		JLabel opponentDeservedDpsStats = new JLabel();
		opponentDeservedDpsStats.setText(fight.getOpponentDeservedDmgString());
		//opponentDeservedDpsStats.setToolTipText(fight.getOpponentDeservedDmgString(1, false) + ": Average damage deserved based on gear/pray (difference vs opponent in brackets)");
		opponentDeservedDpsStats.setToolTipText(
			fight.getOpponent().getName() + " deserved to deal " + fight.getOpponent().getDeservedDamage() +
			" damage based on gear/pray (" + fight.getOpponentDeservedDmgString(1, true) + " vs opponent)");
		opponentDeservedDpsStats.setForeground(fight.opponentDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);
		deservedDpsStatsLine.add(opponentDeservedDpsStats, BorderLayout.EAST);

		// FOURTH LINE: both player's damage dealt
		JPanel dmgDealtStatsLine = new JPanel();
		dmgDealtStatsLine.setLayout(new BorderLayout());
		dmgDealtStatsLine.setBackground(background);

		// fourth line LEFT: player's damage dealt
		JLabel playerdmgDealtStats = new JLabel();
		playerdmgDealtStats.setText(String.valueOf(fight.getCompetitor().getDamageDealt()));
		playerdmgDealtStats.setToolTipText(fight.getCompetitor().getName() + " dealt " + fight.getCompetitor().getDamageDealt() + " damage.");
		playerdmgDealtStats.setForeground(fight.competitorDmgDealtIsGreater() ? Color.GREEN : Color.WHITE);
		dmgDealtStatsLine.add(playerdmgDealtStats, BorderLayout.WEST);

		// fourth line RIGHT: opponent's damage dealt
		JLabel opponentdmgDealtStats = new JLabel();
		opponentdmgDealtStats.setText(String.valueOf(fight.getOpponent().getDamageDealt()));
		opponentdmgDealtStats.setToolTipText(fight.getOpponent().getName() + " dealt " + fight.getOpponent().getDamageDealt() + " damage.");
		opponentdmgDealtStats.setForeground(fight.opponentDeservedDmgIsGreater() ? Color.GREEN : Color.WHITE);
		dmgDealtStatsLine.add(opponentdmgDealtStats, BorderLayout.EAST);

		fightPanel.add(playerNamesLine);
		fightPanel.add(offPrayStatsLine);
		fightPanel.add(deservedDpsStatsLine);
		fightPanel.add(dmgDealtStatsLine);

		add(fightPanel, BorderLayout.NORTH);
	}
}