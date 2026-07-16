/*
 * Copyright (c) 2021, Matsyir <https://github.com/matsyir>
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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN_ICON;
import matsyir.pvpperformancetracker.utils.PvpColorScheme;
import static matsyir.pvpperformancetracker.utils.PvpUtils.fixItemId;
import static matsyir.pvpperformancetracker.utils.PvpUtils.prependPlusIfPositive;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.EquipmentData;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.Fighter;
import matsyir.pvpperformancetracker.controllers.PvpDamageCalc;
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import matsyir.pvpperformancetracker.models.RingData;
import matsyir.pvpperformancetracker.utils.PvpUtils;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.ColorUtil;

@Slf4j
class FightLogDetailFrame extends JFrame
{
	public static final int DEFAULT_WIDTH = 400;
	public static final int DEFAULT_HEIGHT = 640;

	public int rowIdx;

	private boolean isCompetitorLog;
	private Fighter attacker;
	private Fighter defender;

	private JPanel combatLevelsLine;
	private JLabel attackerAtkLvl;
	private JLabel attackerStrLvl;
	private JLabel attackerDefLvl;
	private JLabel attackerRangeLvl;
	private JLabel attackerMageLvl;
	private JLabel attackerHpLvl;

	private JLabel defenderAtkLvl;
	private JLabel defenderStrLvl;
	private JLabel defenderDefLvl;
	private JLabel defenderRangeLvl;
	private JLabel defenderMageLvl;
	private JLabel defenderHpLvl;

	private JLabel attackerOffensiveLabel;
	private JLabel defenderOffensiveLabel;

	FightLogDetailFrame(FightPerformance fight, FightLogEntry logEntry, int rowIdx, Point location)
	{
		super("Fight Log Details - " + fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName()
			+ " on world " + fight.getWorld() + (fight.isSyncedFight() ? " *SYNCED*" : ""));

		this.rowIdx = rowIdx;

		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		setMinimumSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
		setLayout(new BorderLayout());
		setLocation(location);
		setIconImage(PLUGIN_ICON);

		// if always on top is supported, and the core RL plugin has "always on top" set, make the frame always
		// on top as well so it can be above the client.
		if (isAlwaysOnTopSupported())
		{
			setAlwaysOnTop(PLUGIN.getRuneliteConfig().gameAlwaysOnTop());
		}

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setVisible(true);
		Border mainBorder = BorderFactory.createEmptyBorder(8, 12, 8, 12);
		mainPanel.setBorder(mainBorder);

		isCompetitorLog = logEntry.attackerName.equals(fight.getCompetitor().getName());
		attacker = isCompetitorLog ? fight.getCompetitor() : fight.getOpponent();
		defender = isCompetitorLog ? fight.getOpponent() : fight.getCompetitor();

		final FightLogEntry defenderLog = null;
		// TODO smth like this to use the defender log details on this panel. wasn't working
//		if (fight.isSyncedFight())
//		{
//			log.info("FightLogDetailFrame: attempting to find defender log");
//			// attempt to get defender log based on idx?
//			ArrayList<FightLogEntry> attackerEntries = new ArrayList<>((isCompetitorLog ? fight.competitor : fight.opponent).getFightLogEntries());
//			attackerEntries.removeIf(e -> !e.isFullEntry());
//			int foundAttackIndex = attackerEntries.indexOf(logEntry);
//
//			if (foundAttackIndex >= 0)
//			{
//				log.info("FightLogDetailFrame: found attack idx=" + foundAttackIndex);
//				ArrayList<FightLogEntry> defenderEntries = new ArrayList<>((isCompetitorLog ? fight.opponent : fight.competitor).getFightLogEntries());
//				defenderEntries.removeIf(FightLogEntry::isFullEntry);
//
//				defenderLog = defenderEntries.get(foundAttackIndex);
//				log.info("FightLogDetailFrame: found defenderLog");
//			}
//			else
//			{
//				defenderLog = null;
//			}
//		}
//		else
//		{
//			defenderLog = null;
//		}

		JPanel namesLine = new JPanel(new BorderLayout());
		GridLayout attackerNameContainerLayout = new GridLayout(1, 2);
		attackerNameContainerLayout.setHgap(4);
		JPanel attackerNameContainer = new JPanel(attackerNameContainerLayout);
		JLabel attackerName = new JLabel();
		attackerName.setText("<html>Attacker:<br/><strong>" + logEntry.attackerName + "</strong></html>");
		PLUGIN.addSpriteToLabelIfValid(attackerName, logEntry.getAnimationData().attackStyle.getStyleSpriteId());
		attackerName.setToolTipText(logEntry.getAnimationData().attackStyle.toString());
		attackerNameContainer.add(attackerName);

		// if it was a magic attack, display if splash or not:
		if (logEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC)
		{
			int splashIcon = logEntry.isSplash() ? SpriteID.SPELL_ICE_BARRAGE_DISABLED : SpriteID.SPELL_ICE_BARRAGE;
			JLabel splashIconLabel = new JLabel();
			PLUGIN.addSpriteToLabelIfValid(splashIconLabel, splashIcon);
			splashIconLabel.setText("<html> <br/> </html>"); // set text to be 2 lines so the icon lines up with the style icon
			splashIconLabel.setToolTipText("Splashed: " + (logEntry.isSplash() ? "Yes" : "No"));
			splashIconLabel.setHorizontalAlignment(SwingConstants.LEFT);
			attackerNameContainer.add(splashIconLabel);
		}
		namesLine.add(attackerNameContainer, BorderLayout.WEST);

		JLabel defenderName = new JLabel();
		defenderName.setText("<html>Defender:<br/><strong>" + defender.getName() + "</strong></html>");
		namesLine.add(defenderName, BorderLayout.EAST);

		boolean isDharokAttack = PvpDamageCalc.isFullDharokSet(logEntry.getAttackerGear());

		// prayer display line
		JPanel praysUsedLine = new JPanel(new BorderLayout());
		GridLayout attackerPrayLayout = new GridLayout(1, 2);
		attackerPrayLayout.setHgap(4);
		JPanel attackerPrays = new JPanel(attackerPrayLayout);
		// attacker overhead
		int attackerOverheadSpriteId = PvpUtils.getSpriteForHeadIcon(logEntry.getAttackerOverhead());
		JLabel attackerOverheadLabel = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerOverheadLabel, attackerOverheadSpriteId);
		attackerOverheadLabel.setToolTipText("Overhead Prayer");
		attackerPrays.add(attackerOverheadLabel);
		// attacker offensive
		attackerOffensiveLabel = new JLabel();
		attackerOffensiveLabel.setToolTipText("Offensive Prayer");

		// if there's a valid offensive pray, display it.
		PLUGIN.addSpriteToLabelIfValid(attackerOffensiveLabel, logEntry.getAttackerOffensivePray());

		// Opponent prayers are assumed from local prayer unlocks when unsynced; only show N/A if none was stored.
		if (!isCompetitorLog && logEntry.getAttackerOffensivePray() <= 0)
		{
			attackerOffensiveLabel.setText("N/A");
			attackerOffensiveLabel.setIcon(null);
		}

		attackerPrays.add(attackerOffensiveLabel);

		praysUsedLine.add(attackerPrays, BorderLayout.WEST);

		GridLayout defenderPrayLayout = new GridLayout(1, 2);
		defenderPrayLayout.setHgap(4);
		JPanel defenderPrays = new JPanel(defenderPrayLayout);
		// defender overhead
		int defenderOverheadSpriteId = PvpUtils.getSpriteForHeadIcon(logEntry.getDefenderOverhead());
		JLabel defenderOverheadLabel = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderOverheadLabel, defenderOverheadSpriteId);
		defenderOverheadLabel.setToolTipText("Overhead Prayer");
		defenderPrays.add(defenderOverheadLabel);

		// defender offensive (N/A in normal cases)
		defenderOffensiveLabel = new JLabel("N/A");
		defenderOffensiveLabel.setToolTipText("Offensive Prayer");
		defenderPrays.add(defenderOffensiveLabel);

		praysUsedLine.add(defenderPrays, BorderLayout.EAST);

		CombatLevels atkLevels = fight.isSyncedFight() ? logEntry.getAttackerLevels() : fight.fightType.getCombatLevelsForType();
		combatLevelsLine = new JPanel(new BorderLayout());
		JPanel attackerCombatLevels = new JPanel(new GridLayout(2, 3));
		attackerAtkLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerAtkLvl, PvpUtils.getSpriteForSkill(Skill.ATTACK));
		attackerAtkLvl.setText(String.valueOf(atkLevels.atk));
		attackerAtkLvl.setToolTipText("Attack Level");
		attackerStrLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerStrLvl, PvpUtils.getSpriteForSkill(Skill.STRENGTH));
		attackerStrLvl.setText(String.valueOf(atkLevels.str));
		attackerStrLvl.setToolTipText("Strength Level");
		attackerDefLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerDefLvl, PvpUtils.getSpriteForSkill(Skill.DEFENCE));
		attackerDefLvl.setText(String.valueOf(atkLevels.def));
		attackerDefLvl.setToolTipText("Defence Level");
		attackerRangeLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerRangeLvl, PvpUtils.getSpriteForSkill(Skill.RANGED));
		attackerRangeLvl.setText(String.valueOf(atkLevels.range));
		attackerRangeLvl.setToolTipText("Ranged Level");
		attackerMageLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerMageLvl, PvpUtils.getSpriteForSkill(Skill.MAGIC));
		attackerMageLvl.setText(String.valueOf(atkLevels.mage));
		attackerMageLvl.setToolTipText("Magic Level");
		attackerHpLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerHpLvl, PvpUtils.getSpriteForSkill(Skill.HITPOINTS));
		attackerHpLvl.setText(isDharokAttack ? fight.getDharokHpAtAttackText(logEntry) : String.valueOf(atkLevels.hp));
		attackerHpLvl.setToolTipText(isDharokAttack ?
			"Hitpoints at attack. A leading ~ means the value was estimated locally." : "Hitpoints Level");

		attackerCombatLevels.add(attackerAtkLvl);
		attackerCombatLevels.add(attackerStrLvl);
		attackerCombatLevels.add(attackerDefLvl);
		attackerCombatLevels.add(attackerRangeLvl);
		attackerCombatLevels.add(attackerMageLvl);
		attackerCombatLevels.add(attackerHpLvl);
		combatLevelsLine.add(attackerCombatLevels, BorderLayout.WEST);

		CombatLevels defLevels = defenderLog != null ? defenderLog.getAttackerLevels() : fight.fightType.getCombatLevelsForType();
		JPanel defenderCombatLevels = new JPanel(new GridLayout(2, 3));
		defenderAtkLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderAtkLvl, PvpUtils.getSpriteForSkill(Skill.ATTACK));
		defenderAtkLvl.setText(String.valueOf(defLevels.atk));
		defenderAtkLvl.setToolTipText("Attack Level");
		defenderStrLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderStrLvl, PvpUtils.getSpriteForSkill(Skill.STRENGTH));
		defenderStrLvl.setText(String.valueOf(defLevels.str));
		defenderStrLvl.setToolTipText("Strength Level");
		defenderDefLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderDefLvl, PvpUtils.getSpriteForSkill(Skill.DEFENCE));
		defenderDefLvl.setText(String.valueOf(defLevels.def));
		defenderDefLvl.setToolTipText("Defence Level");
		defenderRangeLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderRangeLvl, PvpUtils.getSpriteForSkill(Skill.RANGED));
		defenderRangeLvl.setText(String.valueOf(defLevels.range));
		defenderRangeLvl.setToolTipText("Ranged Level");
		defenderMageLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderMageLvl, PvpUtils.getSpriteForSkill(Skill.MAGIC));
		defenderMageLvl.setText(String.valueOf(defLevels.mage));
		defenderMageLvl.setToolTipText("Magic Level");
		defenderHpLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderHpLvl, PvpUtils.getSpriteForSkill(Skill.HITPOINTS));
		defenderHpLvl.setText(String.valueOf(defLevels.hp));
		defenderHpLvl.setToolTipText("Hitpoints Level");

		defenderCombatLevels.add(defenderAtkLvl);
		defenderCombatLevels.add(defenderStrLvl);
		defenderCombatLevels.add(defenderDefLvl);
		defenderCombatLevels.add(defenderRangeLvl);
		defenderCombatLevels.add(defenderMageLvl);
		defenderCombatLevels.add(defenderHpLvl);
		combatLevelsLine.add(defenderCombatLevels, BorderLayout.EAST);

		// equipment stats line (stab attack, slash attack, etc)
		JPanel equipmentStatsLine = new JPanel(new BorderLayout());
		JLabel attackerStatsLabel = new JLabel();
		PLUGIN.getClientThread().invokeLater(() -> {
			RingData ringUsed = logEntry.getAttackerRingItemId() != null ? RingData.fromId(logEntry.getAttackerRingItemId()) : CONFIG.ringChoice();
			attackerStatsLabel.setText(getItemEquipmentStatsString(logEntry.getAttackerGear(), logEntry.getAttackerAmmoItemId(), ringUsed, fight.fightType.isLmsFight()));
		});
		equipmentStatsLine.add(attackerStatsLabel, BorderLayout.WEST);

		JLabel defenderStatsLabel = new JLabel();
		PLUGIN.getClientThread().invokeLater(() -> {
			RingData ringUsed = (defenderLog != null && defenderLog.getAttackerRingItemId() != null) ? RingData.fromId(defenderLog.getAttackerRingItemId()) : CONFIG.ringChoice();
			Integer ammoId = (defenderLog != null) ? defenderLog.getAttackerAmmoItemId() : null;
			defenderStatsLabel.setText(getItemEquipmentStatsString(logEntry.getDefenderGear(), ammoId, ringUsed, fight.fightType.isLmsFight()));
		});
		equipmentStatsLine.add(defenderStatsLabel, BorderLayout.EAST);

		JPanel equipmentRenderLine = new JPanel(new BorderLayout());
		JPanel attackerEquipmentRender = getEquipmentRender(logEntry.getAttackerGear(), logEntry, true, fight.fightType.isLmsFight());
		JPanel defenderEquipmentRender = getEquipmentRender(logEntry.getDefenderGear(), defenderLog != null ? defenderLog : logEntry, defenderLog != null, fight.fightType.isLmsFight());
		equipmentRenderLine.add(attackerEquipmentRender, BorderLayout.WEST);
		equipmentRenderLine.add(defenderEquipmentRender, BorderLayout.EAST);

		// add warning to clarify level/stats assumptions used here, and the difference between
		// this panel & level change in the fight log table
		// put it on this line because it has the most free space in the middle.
		// only put this here at all if the fight is not synced
		if (!fight.isSyncedFight())
		{
			JShadowedLabel levelAssumptionWarning = new JShadowedLabel("<html>&nbsp;<b><u>Warning</u></b><br>" +
				"<font size='3'>Lvls assumed</font><br><font size='18' color='" +
				ColorUtil.colorToHexCode(PvpColorScheme.BLOOD_RED_ORANGE_REDDER) + "'>" + "&nbsp;&nbsp;&nbsp;&#9888;</font><br>See tooltip!");
			final String levelWarningTooltip = "<html>" +
				"Beware that the Levels shown above on this panel <u>do not</u> reflect your in-game brewed/potted Levels." +
				"<br>The Levels shown here are the Levels that were used for the Expected Damage (eD) calculation, which" +
				"<br>assumes both players are always potted, in order to be as fair as possible for both players. These" +
				"<br>Levels are pulled from your config outside of LMS, and are hardcoded to potted LMS stats inside of LMS." +
				"<br><br>Opponent offensive prayers are assumed to match your Prayer unlocks for their attack style" +
				"<br>(Piety/Rigour/Augury, or Ultimate Strength/Deadeye/Mystic Vigour below those levels)." +
				"<br>While defending, Piety/Rigour/Augury (25% Defence) are assumed when unlocked; otherwise Steel Skin (15%)." +
				"<br>Augury Magic Defence is assumed only when your Prayer level unlocks Augury." +
				"<br><br>These assumptions are used for Expected Damage until the fight is merged/synced." +
				"<br><u>You can toggle the visibility of this warning by right clicking it.</u>";
			JPopupMenu warningTogglePopupMenu = new JPopupMenu();
			JMenuItem warningToggleMenuItem = new JMenuItem("Toggle Warning Visibility");
			warningToggleMenuItem.addActionListener(e ->
			{
				PLUGIN.toggleFightLogDetailFrameWarning();
				levelAssumptionWarning.setVisible(CONFIG.displayFightLogDetailWarning());
			});
			warningTogglePopupMenu.add(warningToggleMenuItem);

			mainPanel.setToolTipText(levelWarningTooltip);
			levelAssumptionWarning.setToolTipText(levelWarningTooltip);
			levelAssumptionWarning.setHorizontalAlignment(SwingConstants.CENTER);
			levelAssumptionWarning.setForeground(Color.WHITE);

			int offsetToTop = 21;
			levelAssumptionWarning.setBorder(PanelFactory.combineBorders(
				BorderFactory.createEmptyBorder(28 - offsetToTop, 12, 28 + offsetToTop, 12),
				BorderFactory.createLineBorder(PvpColorScheme.BLOOD_RED_ORANGE_REDDER, 1),
				BorderFactory.createMatteBorder(4,4,4,4, PvpColorScheme.BLOOD_RED_ORANGE_REDDER.darker())));

			levelAssumptionWarning.setComponentPopupMenu(warningTogglePopupMenu);
			levelAssumptionWarning.setVisible(CONFIG.displayFightLogDetailWarning());
			equipmentRenderLine.add(levelAssumptionWarning, BorderLayout.CENTER);

			equipmentRenderLine.setComponentPopupMenu(warningTogglePopupMenu);
		}

		// Animation detected line
		JPanel animationDetectedLine = new JPanel(new BorderLayout());
		JLabel attackerAnimationDetected = new JLabel();
		attackerAnimationDetected.setText("<html><strong>Animation Detected:</strong> " + logEntry.getAnimationData().toString() + "</html>");
		attackerAnimationDetected.setToolTipText("<html>Note that the animation can be misleading, as many animations are re-used, but this is normal.<br/>" +
			"For example, Zammy Hasta and Staff of Fire use the same crush animation.<br/>" +
			"These were not intended to ever be displayed, but why not include them here.</html>");
		animationDetectedLine.add(attackerAnimationDetected, BorderLayout.CENTER);


		mainPanel.add(namesLine);
		mainPanel.add(praysUsedLine);
		mainPanel.add(combatLevelsLine);
		mainPanel.add(equipmentRenderLine);
		mainPanel.add(equipmentStatsLine);
		mainPanel.add(animationDetectedLine);

		this.add(mainPanel, BorderLayout.CENTER);
		this.setVisible(true);
	}

	private JPanel getEquipmentRender(int[] itemIds, FightLogEntry log, boolean isAttacker, boolean isLmsFight)
	{
		JPanel equipmentRender = new JPanel();
		equipmentRender.setLayout(new BoxLayout(equipmentRender, BoxLayout.Y_AXIS));

		JPanel helmLine = new JPanel(new BorderLayout()); // helm only
		JPanel capeLine = new JPanel(new BorderLayout()); // cape, amulet, ammo
		JPanel weaponLine = new JPanel(new BorderLayout()); // weapon, torso, shield
		JPanel legsLine = new JPanel(new BorderLayout()); // legs
		JPanel glovesLine = new JPanel(new BorderLayout()); // gloves, boots, ring

		JLabel helm = new JLabel();
		helm.setHorizontalAlignment(SwingConstants.CENTER);
		PLUGIN.addItemToLabelIfValid(helm, itemIds[KitType.HEAD.getIndex()]);
		helmLine.add(helm, BorderLayout.CENTER);

		JLabel cape = new JLabel();
		PLUGIN.addItemToLabelIfValid(cape, itemIds[KitType.CAPE.getIndex()]);
		JLabel amulet = new JLabel();
		PLUGIN.addItemToLabelIfValid(amulet, itemIds[KitType.AMULET.getIndex()]);
		capeLine.add(cape, BorderLayout.WEST);
		capeLine.add(amulet, BorderLayout.CENTER);

		// ammo: use local-player equipped ammo when this attack was performed by us; otherwise fall back to inferred weapon ammo in config
		int ammoId = -1;
		if (isAttacker && log.getAttackerAmmoItemId() != null && log.getAttackerAmmoItemId() > 0)
		{
			ammoId = log.getAttackerAmmoItemId();
		}
		else
		{
			RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(EquipmentData.fromId(fixItemId(itemIds[KitType.WEAPON.getIndex()])), isLmsFight);
			if (weaponAmmo != null)
			{
				ammoId = weaponAmmo.getItemId();
			}
		}

		if (ammoId > 0)
		{
			JLabel ammo = new JLabel();
			PLUGIN.addItemToLabelIfValid(ammo, ammoId, false, null);
			capeLine.add(ammo, BorderLayout.EAST);
		}

		JLabel wep = new JLabel();
		PLUGIN.addItemToLabelIfValid(wep, itemIds[KitType.WEAPON.getIndex()], true, null, null, true);
		JLabel torso = new JLabel();
		PLUGIN.addItemToLabelIfValid(torso, itemIds[KitType.TORSO.getIndex()]);
		JLabel shield = new JLabel();
		PLUGIN.addItemToLabelIfValid(shield, itemIds[KitType.SHIELD.getIndex()]);
		weaponLine.add(wep, BorderLayout.WEST);
		weaponLine.add(torso, BorderLayout.CENTER);
		weaponLine.add(shield, BorderLayout.EAST);

		JLabel legs = new JLabel();
		legs.setHorizontalAlignment(SwingConstants.CENTER);
		PLUGIN.addItemToLabelIfValid(legs, itemIds[KitType.LEGS.getIndex()]);
		legsLine.add(legs, BorderLayout.CENTER);

		JLabel gloves = new JLabel();
		PLUGIN.addItemToLabelIfValid(gloves, itemIds[KitType.HANDS.getIndex()]);
		JLabel boots = new JLabel();
		PLUGIN.addItemToLabelIfValid(boots, itemIds[KitType.BOOTS.getIndex()]);
		
		JLabel ring = new JLabel();
		int ringId = -1;
		if (isAttacker && log.getAttackerRingItemId() != null && log.getAttackerRingItemId() > 0)
		{
			ringId = log.getAttackerRingItemId();
		}
		else
		{
			ringId = CONFIG.ringChoice().getItemId();
		}
		PLUGIN.addItemToLabelIfValid(ring, ringId, false, () -> {
			validate();
			repaint();
		});
		glovesLine.add(gloves, BorderLayout.WEST);
		glovesLine.add(boots, BorderLayout.CENTER);
		glovesLine.add(ring, BorderLayout.EAST);

		equipmentRender.add(helmLine);
		equipmentRender.add(capeLine);
		equipmentRender.add(weaponLine);
		equipmentRender.add(legsLine);
		equipmentRender.add(glovesLine);

		return equipmentRender;
	}

	String getItemEquipmentStatsString(int[] equipment, Integer ammoId, RingData ringUsed, boolean isLmsFight)
	{
		int[] bonuses = PvpDamageCalc.calculateBonuses(equipment, ringUsed);
		ItemEquipmentStats stats = ItemEquipmentStats.builder()
			.astab(bonuses[0])	// 0
			.aslash(bonuses[1])	// 1
			.acrush(bonuses[2])	// 2
			.amagic(bonuses[3])	// 3
			.arange(bonuses[4])	// 4
			.dstab(bonuses[5])		// 5
			.dslash(bonuses[6])		// 6
			.dcrush(bonuses[7])		// 7
			.dmagic(bonuses[8])		// 8
			.drange(bonuses[9])		// 9
			.str(bonuses[10])	// 10
			.rstr(bonuses[11]) 	// 11
			.mdmg(bonuses[12])	// 12
			.build();
		int ammoRangeStr = 0;
		int ammoRangeAtt = 0;
		if (ammoId != null && ammoId > 0)
		{
			RangeAmmoData ammoData = RangeAmmoData.fromId(ammoId);
			if (ammoData != null)
			{
				ammoRangeStr = ammoData.getRangeStr();
				ammoRangeAtt = RangeAmmoData.getSeekingArrowAccuracyBonus(ammoData, isLmsFight);
			}
		}
		else
		{
			RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(EquipmentData.fromId(fixItemId(equipment[KitType.WEAPON.getIndex()])), isLmsFight);
			if (weaponAmmo != null)
			{
				ammoRangeStr = weaponAmmo.getRangeStr();
				ammoRangeAtt = RangeAmmoData.getSeekingArrowAccuracyBonus(weaponAmmo, isLmsFight);
			}
		}
		String sep = "<br/>&nbsp;&nbsp;";
		return "<html><strong>Attack bonus</strong>" + sep +
			"Stab: " + prependPlusIfPositive(stats.getAstab()) + sep +
			"Slash: " + prependPlusIfPositive(stats.getAslash()) + sep +
			"Crush: " + prependPlusIfPositive(stats.getAcrush()) + sep +
			"Magic: " + prependPlusIfPositive(stats.getAmagic()) + sep +
			"Range: " + prependPlusIfPositive(stats.getArange() + ammoRangeAtt) +
			"<br/><strong>Defence bonus</strong>" + sep +
			"Stab: " + prependPlusIfPositive(stats.getDstab()) + sep +
			"Slash: " + prependPlusIfPositive(stats.getDslash()) + sep +
			"Crush: " + prependPlusIfPositive(stats.getDcrush()) + sep +
			"Magic: " + prependPlusIfPositive(stats.getDmagic()) + sep +
			"Range: " + prependPlusIfPositive(stats.getDrange()) +
			"<br/><strong>Other bonuses</strong>" + sep +
			"Melee strength: " + prependPlusIfPositive(stats.getStr()) + sep +
			"Ranged strength: " + prependPlusIfPositive(stats.getRstr() + ammoRangeStr) + sep +
			"Magic damage: " + prependPlusIfPositive((int) stats.getMdmg()) + "%" + sep +
			"</html>";
	}


}
