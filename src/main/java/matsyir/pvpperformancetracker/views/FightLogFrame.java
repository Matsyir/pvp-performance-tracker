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
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
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
import matsyir.pvpperformancetracker.controllers.Fighter;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.BrewState;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN_ICON;

import matsyir.pvpperformancetracker.utils.PvpPerformanceTrackerUtils;
import net.runelite.api.ItemID;
import net.runelite.api.SpriteID;

@Slf4j
public class FightLogFrame extends JFrame
{
	// column index constants
	// there's gotta be a better way to do this but this is at least better than hardcoded indexes everywhere
	public static final int COLIDX_ATTACKER_NAME = 0;
	public static final int COLIDX_STYLE_ICON = 1;
	public static final int COLIDX_BREW_STATE = 2;
	public static final int COLIDX_HIT_RANGE = 3;
	public static final int COLIDX_ACCURACY = 4;
	public static final int COLIDX_AVG_HIT = 5;
	public static final int COLIDX_DMG_DEALT = 6;
	public static final int COLIDX_DEF_HP = 7;
	public static final int COLIDX_KO_CHANCE = 8;
	public static final int COLIDX_SPEC = 9;
	public static final int COLIDX_OFF_PRAY = 10;
	public static final int COLIDX_DEF_PRAYER = 11;
	public static final int COLIDX_SPLASH = 12;
	public static final int COLIDX_OFFENSIVE_PRAY = 13;
	public static final int COLIDX_TIME = 14;
	public static final int[] COL_INDEXES = {
		COLIDX_ATTACKER_NAME,
		COLIDX_STYLE_ICON,
		COLIDX_BREW_STATE,
		COLIDX_HIT_RANGE,
		COLIDX_ACCURACY,
		COLIDX_AVG_HIT,
		COLIDX_DMG_DEALT,
		COLIDX_DEF_HP,
		COLIDX_KO_CHANCE,
		COLIDX_SPEC,
		COLIDX_OFF_PRAY,
		COLIDX_DEF_PRAYER,
		COLIDX_SPLASH,
		COLIDX_OFFENSIVE_PRAY,
		COLIDX_TIME
	};

	private static JFrame fightLogFrame; // save frame as static instance so there's only one at a time, to avoid window clutter.

	private static final NumberFormat nf = NumberFormat.getInstance();
	private static final NumberFormat nfPercent = NumberFormat.getPercentInstance(); // For KO Chance %

	static
	{
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

	public static void createFightLogFrame(FightPerformance fight, JRootPane rootPane)
	{
		createFightLogFrame(fight, rootPane, false);
	}

	public static void createFightLogFrame(FightPerformance fight, JRootPane rootPane, boolean pvpHubSynced)
	{
		// destroy current frame if it exists so we only have one at a time (static field)
		if (fightLogFrame != null)
		{
			fightLogFrame.dispose();
		}

		// show error modal if the fight has no log entries to display.
		ArrayList<FightLogEntry> fightLogEntries = new ArrayList<>(fight.getAllFightLogEntries());
		fightLogEntries.removeIf(e -> !e.isFullEntry());
		if (fightLogEntries.isEmpty())
		{
			PLUGIN.createConfirmationModal(false, "This fight has no attack logs to display, or the data is outdated.");
		}
		else
		{
			fightLogFrame = new FightLogFrame(fight,
				fightLogEntries,
				rootPane,
				pvpHubSynced);
		}
	}

	// expects logEntries composing of only "full" log entries, that contain full attack data, not defender entries.
	private FightLogFrame(FightPerformance fight, ArrayList<FightLogEntry> logEntries, JRootPane rootPane, boolean pvpHubSynced)
	{
		//String title = fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName();
		super("Fight Log - " + fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName()
			+ " on world " + fight.getWorld() + (pvpHubSynced ? " *SYNCED*" : ""));

		fightLogEntries = logEntries;
		fightLogEntries.removeIf(e -> !e.isFullEntry());

		// if always on top is supported, and the core RL plugin has "always on top" set, make the frame always
		// on top as well so it can be above the client.
		if (isAlwaysOnTopSupported())
		{
			setAlwaysOnTop(PLUGIN.getRuneliteConfig().gameAlwaysOnTop());
		}

		setIconImage(PLUGIN_ICON);
		setSize(1088, 503);
		setLocation(rootPane.getLocationOnScreen());

		JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
		Object[][] stats = new Object[fightLogEntries.size()][COL_INDEXES.length];
		int i = 0;
		int initialTick = 0;

		for (FightLogEntry fightEntry : fightLogEntries)
		{
			if (i == 0)
			{
				initialTick = fightEntry.getTick();
			}

			int styleIcon = fightEntry.getAnimationData().attackStyle.getStyleSpriteId();
			JLabel styleIconLabel = new JLabel();
			PLUGIN.addSpriteToLabelIfValid(styleIconLabel, styleIcon, this::repaint);
			styleIconLabel.setToolTipText(fightEntry.getAnimationData().attackStyle.toString());

			Fighter attacker = fightEntry.getAttackerName().equals(fight.getCompetitor().getName())
				? fight.getCompetitor()
				: fight.getOpponent();
			BrewState brewState = attacker.getBrewState(fightEntry);
			JLabel brewStateLabel = new JLabel();
			brewStateLabel.setForeground(brewState.getCategory().getTextColor());
			brewStateLabel.setToolTipText("<html>Amount of levels below the max potted level." +
				"<br>For example, if ranging with 110 range, it will display -2 since 112 is max potted (at level 99)." +
				"<br>You want this to stay as close to 0 as possible." +
				"<br><br>This does look at your actual potted/brewed level" + (pvpHubSynced ? "." :
				", although it isn't used for Expected Damage<br>calculations, unless the fight is merged/synced"));

			brewStateLabel.setText(brewState.getFightLogText());

			stats[i][COLIDX_ATTACKER_NAME] = fightEntry.getAttackerName();
			stats[i][COLIDX_STYLE_ICON] = styleIconLabel;
			stats[i][COLIDX_BREW_STATE] = brewStateLabel;
			stats[i][COLIDX_HIT_RANGE] = fightEntry.getHitRange();
			stats[i][COLIDX_ACCURACY] = nf.format(fightEntry.getAccuracy() * 100) + '%';
			stats[i][COLIDX_AVG_HIT] = nf.format(fightEntry.getExpectedDamage());

			// Actual Dmg column
			JLabel dmgLabel = new JLabel();
			if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC && fightEntry.isSplash())
			{
				int splashIcon = SpriteID.SPELL_ICE_BARRAGE_DISABLED;
				PLUGIN.addSpriteToLabelIfValid(dmgLabel, splashIcon, this::repaint);
			}
			else if (fightEntry.getMatchedHitsCount() <= 0)
			{
				dmgLabel.setText("?");
			}
			else
			{
				dmgLabel.setText(nf.format(fightEntry.getActualDamageSum()));
			}
			stats[i][COLIDX_DMG_DEALT] = dmgLabel;
			// HP column - Display as Current/Max
			Integer hp = fightEntry.getDisplayHpBefore();
			Integer maxHp = fightEntry.getOpponentMaxHp();
			stats[i][COLIDX_DEF_HP] = (hp != null && maxHp != null) ? hp + "/" + maxHp : (hp != null ? String.valueOf(hp) : "-");
			Double koChance = fightEntry.getKoChance();
			stats[i][COLIDX_KO_CHANCE] = koChance != null ? nfPercent.format(koChance) : "-";
			stats[i][COLIDX_SPEC] = fightEntry.getAnimationData().isSpecial ? "✔" : "";
			stats[i][COLIDX_OFF_PRAY] = fightEntry.success() ? "✔" : "";

			// Def Prayer + sotd/ely
			int prayIcon = PvpPerformanceTrackerUtils.getSpriteForHeadIcon(fightEntry.getDefenderOverhead());
			boolean hasPray = prayIcon > 0;
			boolean hasEly = fightEntry.isDefenderElyProc();
			boolean hasStaffReduction = fightEntry.isDefenderSotdMeleeReductionProc();

			if (!hasPray && !hasEly && !hasStaffReduction)
			{
				stats[i][COLIDX_DEF_PRAYER] = "";
			}
			else if ((hasPray ? 1 : 0) + (hasEly ? 1 : 0) + (hasStaffReduction ? 1 : 0) == 1)
			{
				JLabel label = new JLabel();
				if (hasPray)
				{
					PLUGIN.addSpriteToLabelIfValid(label, prayIcon, this::repaint);
					label.setToolTipText("Defensive Prayer");
				}
				else if (hasEly)
				{
					PLUGIN.addItemToLabelIfValid(label, ItemID.ELYSIAN_SPIRIT_SHIELD, false, this::repaint, "Elysian proc");
				}
				else
				{
					PLUGIN.addItemToLabelIfValid(label, ItemID.STAFF_OF_THE_DEAD, false, this::repaint, "Staff spec damage reduction");
				}
				stats[i][COLIDX_DEF_PRAYER] = label;
			}
			else
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
				panel.setOpaque(false);

				if (hasPray)
				{
					JLabel defPrayLabel = new JLabel();
					PLUGIN.addSpriteToLabelIfValid(defPrayLabel, prayIcon, this::repaint);
					defPrayLabel.setToolTipText("Defensive Prayer");
					panel.add(defPrayLabel);
				}

				if (hasEly)
				{
					JLabel elyLabel = new JLabel();
					PLUGIN.addItemToLabelIfValid(elyLabel, ItemID.ELYSIAN_SPIRIT_SHIELD, false, this::repaint, "Elysian proc");
					panel.add(elyLabel);
				}

				if (hasStaffReduction)
				{
					JLabel staffLabel = new JLabel();
					PLUGIN.addItemToLabelIfValid(staffLabel, ItemID.STAFF_OF_THE_DEAD, false, this::repaint, "Staff spec damage reduction");
					panel.add(staffLabel);
				}

				stats[i][COLIDX_DEF_PRAYER] = panel;
			}
			// Splash
			if (fightEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC)
			{
				int splashIcon = fightEntry.isSplash() ? SpriteID.SPELL_ICE_BARRAGE_DISABLED : SpriteID.SPELL_ICE_BARRAGE;
				JLabel splashLabel = new JLabel();
				PLUGIN.addSpriteToLabelIfValid(splashLabel, splashIcon, this::repaint);
				stats[i][COLIDX_SPLASH] = splashLabel;
			}
			else
			{
				stats[i][COLIDX_SPLASH] = "";
			}
			// Offensive Pray
			JLabel attPrayLabel = new JLabel();
			if (fightEntry.getAttackerOffensivePray() > 0)
			{
				PLUGIN.addSpriteToLabelIfValid(attPrayLabel, fightEntry.getAttackerOffensivePray(), this::repaint);
				stats[i][COLIDX_OFFENSIVE_PRAY] = attPrayLabel;
			}
			else
			{
				stats[i][COLIDX_OFFENSIVE_PRAY] = "";
			}
			int tickDuration = fightEntry.getTick() - initialTick;
			int durationMillis = (tickDuration * 600); // (* 0.6) to get duration in secs from ticks, so *600 for ms
			Duration duration = Duration.ofMillis(durationMillis);
			String time = String.format("%02d:%02d.%01d",
				duration.toMinutes(),
				duration.getSeconds() % 60,
				durationMillis % 1000 / 100) + " (" + tickDuration + ")";
			stats[i][COLIDX_TIME] = time;

			i++;
		}

		String[] header = {"Attacker", "Style", "Level Change", "Hit Range", "Accuracy", "Avg Hit", "Actual Dmg", "HP", "KO Chance", "Special?",
			"Off-Pray?", "Def Prayer", "Splash", "Offensive Pray", "Time, (Tick)"};
		table = new JTable(stats, header);
		table.setRowHeight(30);
		table.setDefaultEditor(Object.class, null);

		table.getColumnModel().getColumn(COLIDX_STYLE_ICON).setCellRenderer(new BufferedImageCellRenderer()); // Style
		table.getColumnModel().getColumn(COLIDX_STYLE_ICON).setPreferredWidth(50);
		table.getColumnModel().getColumn(COLIDX_DMG_DEALT).setCellRenderer(new BufferedImageCellRenderer()); // Actual Dmg

		table.getColumnModel().getColumn(COLIDX_DEF_PRAYER).setCellRenderer(new BufferedImageCellRenderer()); // Def Prayer
		table.getColumnModel().getColumn(COLIDX_DEF_PRAYER).setPreferredWidth(96); // room for def pray + proc icons

		table.getColumnModel().getColumn(COLIDX_SPLASH).setCellRenderer(new BufferedImageCellRenderer()); // Splash
		table.getColumnModel().getColumn(COLIDX_OFFENSIVE_PRAY).setCellRenderer(new BufferedImageCellRenderer()); // Offensive Pray
		table.getColumnModel().getColumn(COLIDX_OFFENSIVE_PRAY).setPreferredWidth(50);

		// keep it compact, these are just a checkmark, dont need much width
		table.getColumnModel().getColumn(COLIDX_SPEC).setPreferredWidth(44);
		table.getColumnModel().getColumn(COLIDX_OFF_PRAY).setPreferredWidth(50);

		table.getColumnModel().getColumn(COLIDX_BREW_STATE).setCellRenderer(new BufferedImageCellRenderer());
		table.getColumnModel().getColumn(COLIDX_BREW_STATE).setPreferredWidth(92);

		// if the fight has no baseLevels, then we have 0 stats for brew state anyways, so remove the column
		// NOTE: THIS HAS TO STAY AFTER ALL THE setRenderers or any other usage of getColumn, or the indexes get messed up
		if (fight.getCompetitor().getBaseLevels() == null && fight.getOpponent().getBaseLevels() == null)
		{
			table.removeColumn(table.getColumnModel().getColumn(COLIDX_BREW_STATE));
		}

		onRowSelected = e ->
		{
			int row = table.getSelectedRow();

			if (fightLogDetailFrame != null)
			{
				if (fightLogDetailFrame.rowIdx == row)
				{
					return;
				}

				fightLogDetailFrame.dispose();
				fightLogDetailFrame = null;
			}

			fightLogDetailFrame = new FightLogDetailFrame(fight, fightLogEntries.get(row), row,
				new Point( // place the new detail frame roughly to the right of the fight log window.
					this.getLocation().x + this.getSize().width,
					this.getLocation().y)
			);
		};

		table.getSelectionModel().addListSelectionListener(onRowSelected);

		mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

		add(mainPanel);
		setVisible(true);
	}

	private static class BufferedImageCellRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof JPanel)
			{
				JPanel panel = (JPanel) value;
				panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
				panel.setOpaque(true);
				return panel;
			}
			else if (value instanceof BufferedImage)
			{
				setText("");
				setIcon(new ImageIcon((BufferedImage) value));
			}
			else if (value instanceof JLabel)
			{
				JLabel val = (JLabel) value;
				setIcon(val.getIcon());
				setText(val.getText());
				setToolTipText(val.getToolTipText());
				setForeground(val.getForeground());
			}
			else
			{
				setText("");
				setIcon(null);
			}

			return this;
		}
	}
}
