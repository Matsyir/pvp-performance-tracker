/*
 * Copyright (c) 2021, Mazhar <https://twitter.com/maz_rs>
 * Copyright (c) 2021, Matsyir <https://github.com/Matsyir>
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
import java.awt.Component;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.controllers.AnalyzedFightPerformance;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN_ICON;
import net.runelite.api.SpriteID;

@Slf4j
public class FightLogFrame extends JFrame {
	private static final NumberFormat nf = NumberFormat.getInstance();
	private static final NumberFormat nfPercent = NumberFormat.getPercentInstance(); // For KO Chance %

	static {
		// initialize number format
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);

		// initialize percent format
		nfPercent.setMaximumFractionDigits(1);
		nfPercent.setRoundingMode(RoundingMode.HALF_UP);
	}

	private FightLogDetailFrame fightLogDetailFrame;
	private JTable table;
	private ListSelectionListener onRowSelected;
	private ArrayList<FightLogEntry> fightLogEntries;

	// expects logEntries composing of only "full" log entries, that contain full
	// attack data, not defender entries.
	FightLogFrame(FightPerformance fight, ArrayList<FightLogEntry> logEntries, JRootPane rootPane) {
		// String title = fight.getCompetitor().getName() + " vs " +
		// fight.getOpponent().getName();
		super(fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName());

		fightLogEntries = logEntries;
		fightLogEntries.removeIf(e -> !e.isFullEntry());

		// if always on top is supported, and the core RL plugin has "always on top"
		// set, make the frame always
		// on top as well so it can be above the client.
		if (isAlwaysOnTopSupported()) {
			setAlwaysOnTop(PLUGIN.getRuneliteConfig().gameAlwaysOnTop());
		}

		setIconImage(PLUGIN_ICON);
		setSize(820, 503); // Increased width slightly for new column
		setLocation(rootPane.getLocationOnScreen());

		JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
		Object[][] stats = new Object[fightLogEntries.size()][12]; // Increased column count
		int i = 0;
		int initialTick = 0;

		for (FightLogEntry fightEntry : fightLogEntries) {
			if (i == 0) {
				initialTick = fightEntry.getTick();
			}

			int styleIcon = fightEntry.getAnimationData().attackStyle.getStyleSpriteId();
			JLabel styleIconLabel = new JLabel();
			PLUGIN.addSpriteToLabelIfValid(styleIconLabel, styleIcon, this::repaint);
			styleIconLabel.setToolTipText(fightEntry.getAnimationData().attackStyle.toString());

			stats[i][0] = fightEntry.getAttackerName();
			stats[i][1] = styleIconLabel;
			stats[i][2] = fightEntry.getHitRange();
			stats[i][3] = nf.format(fightEntry.getAccuracy() * 100) + '%';
			stats[i][4] = nf.format(fightEntry.getDeservedDamage());

			// KO Chance column (Index 5)
			Double koChance = fightEntry.getKoChance();
			stats[i][5] = koChance != null ? nfPercent.format(koChance) : "-";

			// Shifted original columns
			stats[i][6] = fightEntry.getAnimationData().isSpecial ? "✔" : ""; // Was 5
			stats[i][7] = fightEntry.success() ? "✔" : ""; // Was 6

			int prayIcon = PLUGIN.getSpriteForHeadIcon(fightEntry.getDefenderOverhead());
			if (prayIcon > 0) { // Index 8 (Was 7)
				JLabel prayIconLabel = new JLabel();
				PLUGIN.addSpriteToLabelIfValid(prayIconLabel, prayIcon, this::repaint);
				stats[i][8] = prayIconLabel; // Was 7
			} else {
				stats[i][8] = ""; // Was 7
			}

			if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC) { // Index 9 (Was 8)
				int freezeIcon = fightEntry.isSplash() ? SpriteID.SPELL_ICE_BARRAGE_DISABLED
						: SpriteID.SPELL_ICE_BARRAGE;
				JLabel freezeIconLabel = new JLabel();
				PLUGIN.addSpriteToLabelIfValid(freezeIconLabel, freezeIcon, this::repaint);
				stats[i][9] = freezeIconLabel; // Was 8
			} else {
				stats[i][9] = ""; // Was 8
			}

			// offensive pray shown as icon or blank if none(0) // Index 10 (Was 9)
			JLabel attackerOffensivePrayLabel = new JLabel();
			if (fightEntry.getAttackerOffensivePray() > 0) {
				PLUGIN.addSpriteToLabelIfValid(attackerOffensivePrayLabel, fightEntry.getAttackerOffensivePray(),
						this::repaint);
				stats[i][10] = attackerOffensivePrayLabel; // Was 9
			} else {
				stats[i][10] = ""; // Was 9
			}

			int tickDuration = fightEntry.getTick() - initialTick; // Index 11 (Was 10)
			int durationMillis = (tickDuration * 600); // (* 0.6) to get duration in secs from ticks, so *600 for ms
			Duration duration = Duration.ofMillis(durationMillis);
			String time = String.format("%02d:%02d.%01d",
					duration.toMinutes(),
					duration.getSeconds() % 60,
					durationMillis % 1000 / 100) + " (" + tickDuration + ")";
			stats[i][11] = time; // Was 10

			i++;
		}

		String[] header = { "Attacker", "Style", "Hit Range", "Accuracy", "Avg Hit", "KO Chance", "Special?",
				"Off-Pray?",
				"Def Prayer", "Splash", "Offensive Pray", "Time, (Tick)" }; // Added KO Chance header
		table = new JTable(stats, header);
		table.setRowHeight(30);
		table.setDefaultEditor(Object.class, null);

		table.getColumnModel().getColumn(1).setCellRenderer(new BufferedImageCellRenderer()); // Style
		table.getColumnModel().getColumn(8).setCellRenderer(new BufferedImageCellRenderer()); // Def Prayer (was 7)
		table.getColumnModel().getColumn(9).setCellRenderer(new BufferedImageCellRenderer()); // Splash (was 8)
		table.getColumnModel().getColumn(10).setCellRenderer(new BufferedImageCellRenderer()); // Offensive Pray (was 9)

		onRowSelected = e -> {
			int row = table.getSelectedRow();

			if (fightLogDetailFrame != null) {
				if (fightLogDetailFrame.rowIdx == row) {
					return;
				}

				fightLogDetailFrame.dispose();
				fightLogDetailFrame = null;
			}

			fightLogDetailFrame = new FightLogDetailFrame(fight, fightLogEntries.get(row), row,
					new Point( // place the new detail frame roughly to the right of the fight log window.
							this.getLocation().x + this.getSize().width,
							this.getLocation().y));
		};

		table.getSelectionModel().addListSelectionListener(onRowSelected);

		mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

		add(mainPanel);
		setVisible(true);
	}

	static class BufferedImageCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof BufferedImage) {
				setText("");
				setIcon(new ImageIcon((BufferedImage) value));
			} else if (value instanceof JLabel) {
				JLabel val = (JLabel) value;
				setIcon(val.getIcon());
				setText(val.getText());
				setToolTipText(val.getToolTipText());
			} else {
				setText("");
				setIcon(null);
			}

			return this;
		}
	}

	// initialize frame using an AnalyzedFight, in order to pass the analyzed fight
	// data
	// to the detailed frame.
	FightLogFrame(AnalyzedFightPerformance fight, JRootPane rootPane) {
		this(fight,
				new ArrayList(fight.getAllFightLogEntries().stream()
						.filter(FightLogEntry::isFullEntry) // send only attacker logs, and don't use the matching logs
															// since
						.collect(Collectors.toList())), // those have old 'dps' values, they're only used for defender
														// lvls/pray/etc client data
				rootPane);

		// test
		if (new ArrayList(fight.getAllFightLogEntries().stream()
				.filter(FightLogEntry::isFullEntry) // send only attacker logs
				.collect(Collectors.toList())).size() != fight.getAnalyzedMatchingLogs().size()) {
			log.info(
					"FIGHT ANALYSIS: ERROR! allFightLogEntries.filter::isFullEntry different size than analyzedMatchingLogs - should not happen");
		}

		table.getSelectionModel().removeListSelectionListener(onRowSelected);
		onRowSelected = e -> {
			int row = table.getSelectedRow();

			if (fightLogDetailFrame != null) {
				if (fightLogDetailFrame.rowIdx == row) {
					return;
				}

				fightLogDetailFrame.dispose();
				fightLogDetailFrame = null;
			}

			fightLogDetailFrame = new FightLogDetailFrame(fight, fightLogEntries.get(row),
					fight.getAnalyzedMatchingLogs().get(row)[1], row,
					new Point( // place the new detail frame roughly to the right of the fight log window.
							this.getLocationOnScreen().x + (this.getSize().width),
							this.getLocationOnScreen().y));
		};

		table.getSelectionModel().addListSelectionListener(onRowSelected);
	}
}
