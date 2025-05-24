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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN_ICON;
import static matsyir.pvpperformancetracker.utils.PvpPerformanceTrackerUtils.fixItemId;
import matsyir.pvpperformancetracker.controllers.AnalyzedFightPerformance;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.EquipmentData;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.Fighter;
import matsyir.pvpperformancetracker.controllers.PvpDamageCalc;
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import matsyir.pvpperformancetracker.utils.PvpPerformanceTrackerUtils;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemEquipmentStats;

@Slf4j
class FightLogDetailFrame extends JFrame
{
	public static final int DEFAULT_WIDTH = 400;

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

	// this is a regular fightLogDetailFrame, for a normal non-merged fight.
	FightLogDetailFrame(FightPerformance fight, FightLogEntry log, int rowIdx, Point location)
	{
		super("Fight Log Details - " + fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName()
			+ " on world " + fight.getWorld());

		this.rowIdx = rowIdx;

		setSize(DEFAULT_WIDTH, 640);
		setMinimumSize(new Dimension(DEFAULT_WIDTH, 200));
		setLayout(new BorderLayout());
		setLocation(location);
		setIconImage(PLUGIN_ICON);

		// if always on top is supported, and the core RL plugin has "always on top" set, make the frame always
		// on top as well so it can be above the client.
		if (isAlwaysOnTopSupported())
		{
			setAlwaysOnTop(PLUGIN.getRuneliteConfig().gameAlwaysOnTop());
		}

		JPanel mainPanel = new JPanel(); //new GridLayout(0, 2)
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setVisible(true);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

		isCompetitorLog = log.attackerName.equals(fight.getCompetitor().getName());
		attacker = isCompetitorLog ? fight.getCompetitor() : fight.getOpponent();
		defender = isCompetitorLog ? fight.getOpponent() : fight.getCompetitor();

		JPanel namesLine = new JPanel(new BorderLayout());
		GridLayout attackerNameContainerLayout = new GridLayout(1, 2);
		attackerNameContainerLayout.setHgap(4);
		JPanel attackerNameContainer = new JPanel(attackerNameContainerLayout);
		JLabel attackerName = new JLabel();
		attackerName.setText("<html>Attacker:<br/><strong>" + log.attackerName + "</strong></html>");
		PLUGIN.addSpriteToLabelIfValid(attackerName, log.getAnimationData().attackStyle.getStyleSpriteId());
		attackerName.setToolTipText(log.getAnimationData().attackStyle.toString());
		attackerNameContainer.add(attackerName);

		// if it was a magic attack, display if splash or not:
		if (log.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC)
		{
			int splashIcon = log.isSplash() ? SpriteID.SPELL_ICE_BARRAGE_DISABLED : SpriteID.SPELL_ICE_BARRAGE;
			JLabel splashIconLabel = new JLabel();
			PLUGIN.addSpriteToLabelIfValid(splashIconLabel, splashIcon);
			splashIconLabel.setText("<html> <br/> </html>"); // set text to be 2 lines so the icon lines up with the style icon
			splashIconLabel.setToolTipText("Splashed: " + (log.isSplash() ? "Yes" : "No"));
			splashIconLabel.setHorizontalAlignment(SwingConstants.LEFT);
			attackerNameContainer.add(splashIconLabel);
		}
		namesLine.add(attackerNameContainer, BorderLayout.WEST);

		JLabel defenderName = new JLabel();
		defenderName.setText("<html>Defender:<br/><strong>" + defender.getName() + "</strong></html>");
		namesLine.add(defenderName, BorderLayout.EAST);

		// prayer display line
		JPanel praysUsedLine = new JPanel(new BorderLayout());
		GridLayout attackerPrayLayout = new GridLayout(1, 2);
		attackerPrayLayout.setHgap(4);
		JPanel attackerPrays = new JPanel(attackerPrayLayout);
		// attacker overhead
		int attackerOverheadSpriteId = PvpPerformanceTrackerUtils.getSpriteForHeadIcon(log.getAttackerOverhead());
		JLabel attackerOverheadLabel = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerOverheadLabel, attackerOverheadSpriteId);
		attackerOverheadLabel.setToolTipText("Overhead Prayer");
		attackerPrays.add(attackerOverheadLabel);
		// attacker offensive
		attackerOffensiveLabel = new JLabel();
		attackerOffensiveLabel.setToolTipText("Offensive Prayer");

		// if there's a valid offensive pray, display it.
		PLUGIN.addSpriteToLabelIfValid(attackerOffensiveLabel, log.getAttackerOffensivePray());

		if (!isCompetitorLog) // if it wasn't a competitor log, then this is N/A in most cases, and
		{ // do NOT put a bank filler, to indicate that this is fully N/A, and not "None"
			attackerOffensiveLabel.setText("N/A");
			attackerOffensiveLabel.setIcon(null);
		}

		attackerPrays.add(attackerOffensiveLabel);
		praysUsedLine.add(attackerPrays, BorderLayout.WEST);

		GridLayout defenderPrayLayout = new GridLayout(1, 2);
		defenderPrayLayout.setHgap(4);
		JPanel defenderPrays = new JPanel(defenderPrayLayout);
		// defender overhead
		int defenderOverheadSpriteId = PvpPerformanceTrackerUtils.getSpriteForHeadIcon(log.getDefenderOverhead());
		JLabel defenderOverheadLabel = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderOverheadLabel, defenderOverheadSpriteId);
		defenderOverheadLabel.setToolTipText("Overhead Prayer");
		defenderPrays.add(defenderOverheadLabel);

		// defender offensive (N/A in normal cases)
		defenderOffensiveLabel = new JLabel("N/A");
		defenderOffensiveLabel.setToolTipText("Offensive Prayer");
		defenderPrays.add(defenderOffensiveLabel);

		praysUsedLine.add(defenderPrays, BorderLayout.EAST);

		CombatLevels levels = fight.fightType.getCombatLevelsForType();
		combatLevelsLine = new JPanel(new BorderLayout());
		JPanel attackerCombatLevels = new JPanel(new GridLayout(2, 3));
		attackerAtkLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerAtkLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.ATTACK));
		attackerAtkLvl.setText(String.valueOf(levels.atk));
		attackerAtkLvl.setToolTipText("Attack Level");
		attackerStrLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerStrLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.STRENGTH));
		attackerStrLvl.setText(String.valueOf(levels.str));
		attackerStrLvl.setToolTipText("Strength Level");
		attackerDefLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerDefLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.DEFENCE));
		attackerDefLvl.setText(String.valueOf(levels.def));
		attackerDefLvl.setToolTipText("Defence Level");
		attackerRangeLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerRangeLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.RANGED));
		attackerRangeLvl.setText(String.valueOf(levels.range));
		attackerRangeLvl.setToolTipText("Ranged Level");
		attackerMageLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerMageLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.MAGIC));
		attackerMageLvl.setText(String.valueOf(levels.mage));
		attackerMageLvl.setToolTipText("Magic Level");
		attackerHpLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(attackerHpLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.HITPOINTS));
		attackerHpLvl.setText(String.valueOf(levels.hp));
		attackerHpLvl.setToolTipText("Hitpoints Level");

		attackerCombatLevels.add(attackerAtkLvl);
		attackerCombatLevels.add(attackerStrLvl);
		attackerCombatLevels.add(attackerDefLvl);
		attackerCombatLevels.add(attackerRangeLvl);
		attackerCombatLevels.add(attackerMageLvl);
		attackerCombatLevels.add(attackerHpLvl);
		combatLevelsLine.add(attackerCombatLevels, BorderLayout.WEST);

		JPanel defenderCombatLevels = new JPanel(new GridLayout(2, 3));
		defenderAtkLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderAtkLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.ATTACK));
		defenderAtkLvl.setText(String.valueOf(levels.atk));
		defenderAtkLvl.setToolTipText("Attack Level");
		defenderStrLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderStrLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.STRENGTH));
		defenderStrLvl.setText(String.valueOf(levels.str));
		defenderStrLvl.setToolTipText("Strength Level");
		defenderDefLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderDefLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.DEFENCE));
		defenderDefLvl.setText(String.valueOf(levels.def));
		defenderDefLvl.setToolTipText("Defence Level");
		defenderRangeLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderRangeLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.RANGED));
		defenderRangeLvl.setText(String.valueOf(levels.range));
		defenderRangeLvl.setToolTipText("Ranged Level");
		defenderMageLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderMageLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.MAGIC));
		defenderMageLvl.setText(String.valueOf(levels.mage));
		defenderMageLvl.setToolTipText("Magic Level");
		defenderHpLvl = new JLabel();
		PLUGIN.addSpriteToLabelIfValid(defenderHpLvl, PvpPerformanceTrackerUtils.getSpriteForSkill(Skill.HITPOINTS));
		defenderHpLvl.setText(String.valueOf(levels.hp));
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
		PLUGIN.getClientThread().invokeLater(() ->
			attackerStatsLabel.setText(getItemEquipmentStatsString(log.getAttackerGear())));
		equipmentStatsLine.add(attackerStatsLabel, BorderLayout.WEST);

		JLabel defenderStatsLabel = new JLabel();
		PLUGIN.getClientThread().invokeLater(() ->
			defenderStatsLabel.setText(getItemEquipmentStatsString(log.getDefenderGear())));
		equipmentStatsLine.add(defenderStatsLabel, BorderLayout.EAST);

		JPanel equipmentRenderLine = new JPanel(new BorderLayout());
		JPanel attackerEquipmentRender = getEquipmentRender(log.getAttackerGear());
		JPanel defenderEquipmentRender = getEquipmentRender(log.getDefenderGear());
		equipmentRenderLine.add(attackerEquipmentRender, BorderLayout.WEST);
		equipmentRenderLine.add(defenderEquipmentRender, BorderLayout.EAST);

		// Animation detected line
		JPanel animationDetectedLine = new JPanel(new BorderLayout());
		JLabel attackerAnimationDetected = new JLabel();
		attackerAnimationDetected.setText("<html><strong>Animation Detected:</strong> " + log.getAnimationData().toString() + "</html>");
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

	// extra-detailed frame for a merged fight, which has both an attacker and defender log for each attack
	FightLogDetailFrame(AnalyzedFightPerformance fight, FightLogEntry attackerLog, FightLogEntry defenderLog, int rowIdx, Point location)
	{
		this(fight, attackerLog, rowIdx, location);

		// attacker lvls
		CombatLevels aLvls = attackerLog.getAttackerLevels();
		if (aLvls == null)
		{
			log.info("Fight Analysis: attackerLvls are null! This shouldn't happen.");
			aLvls = fight.fightType.getCombatLevelsForType();
		}
		attackerAtkLvl.setText(String.valueOf(aLvls.atk));
		attackerStrLvl.setText(String.valueOf(aLvls.str));
		attackerDefLvl.setText(String.valueOf(aLvls.def));
		attackerRangeLvl.setText(String.valueOf(aLvls.range));
		attackerMageLvl.setText(String.valueOf(aLvls.mage));
		attackerHpLvl.setText(String.valueOf(aLvls.hp));

		// defender lvls
		CombatLevels dLvls = defenderLog.getAttackerLevels();
		if (dLvls == null)
		{
			log.info("Fight Analysis: defenderLvls are null! This shouldn't happen.");
			dLvls = fight.fightType.getCombatLevelsForType();
		}
		defenderAtkLvl.setText(String.valueOf(dLvls.atk));
		defenderStrLvl.setText(String.valueOf(dLvls.str));
		defenderDefLvl.setText(String.valueOf(dLvls.def));
		defenderRangeLvl.setText(String.valueOf(dLvls.range));
		defenderMageLvl.setText(String.valueOf(dLvls.mage));
		defenderHpLvl.setText(String.valueOf(dLvls.hp));

		defenderOffensiveLabel.setText("");
		PLUGIN.addSpriteToLabelIfValid(defenderOffensiveLabel, defenderLog.getAttackerOffensivePray());

		attackerOffensiveLabel.setText("");
		PLUGIN.addSpriteToLabelIfValid(attackerOffensiveLabel, attackerLog.getAttackerOffensivePray(), () -> {
			validate();
			repaint();
		});
	}

	private JPanel getEquipmentRender(int[] itemIds)
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

		// ammo: get config's ammo for current weapon
		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(EquipmentData.fromId(fixItemId(itemIds[KitType.WEAPON.getIndex()])));
		if (weaponAmmo != null)
		{
			JLabel ammo = new JLabel();
			PLUGIN.addItemToLabelIfValid(ammo, weaponAmmo, false, null);
			capeLine.add(ammo, BorderLayout.EAST);
		}

		JLabel wep = new JLabel();
		PLUGIN.addItemToLabelIfValid(wep, itemIds[KitType.WEAPON.getIndex()]);
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
		PLUGIN.addItemToLabelIfValid(ring, CONFIG.ringChoice().getItemId(), false, () -> {
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

	//
	String getItemEquipmentStatsString(int[] equipment)
	{
		ItemEquipmentStats stats = PvpDamageCalc.calculateBonusesToStats(equipment);
		// ammo: get config's ammo for current weapon
		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(EquipmentData.fromId(fixItemId(equipment[KitType.WEAPON.getIndex()])));
		int ammoRangeStr = 0;
		if (weaponAmmo != null)
		{
			ammoRangeStr = weaponAmmo.getRangeStr();
		}
		String sep = "<br/>&nbsp;&nbsp;";
		return "<html><strong>Attack bonus</strong>" + sep +
			"Stab: " + prependPlusIfPositive(stats.getAstab()) + sep +
			"Slash: " + prependPlusIfPositive(stats.getAslash()) + sep +
			"Crush: " + prependPlusIfPositive(stats.getAcrush()) + sep +
			"Magic: " + prependPlusIfPositive(stats.getAmagic()) + sep +
			"Range: " + prependPlusIfPositive(stats.getArange()) +
			"<br/><strong>Defence bonus</strong>" + sep +
			"Stab: " + prependPlusIfPositive(stats.getDstab()) + sep +
			"Slash: " + prependPlusIfPositive(stats.getDslash()) + sep +
			"Crush: " + prependPlusIfPositive(stats.getDcrush()) + sep +
			"Magic: " + prependPlusIfPositive(stats.getDmagic()) + sep +
			"Range: " + prependPlusIfPositive(stats.getDrange()) +
			"<br/><strong>Other bonuses</strong>" + sep +
			"Melee strength: " + prependPlusIfPositive(stats.getStr()) + sep +
			"Ranged strength: " + prependPlusIfPositive(stats.getRstr() + ammoRangeStr) + sep +
			"Magic damage: " + prependPlusIfPositive((int)stats.getMdmg()) + "%" + sep +
			"</html>";
	}

	String prependPlusIfPositive(int number)
	{
		if (number >= 0)
		{
			return "+" + number;
		}

		return String.valueOf(number);
	}
}
