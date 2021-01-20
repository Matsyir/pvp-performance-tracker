package matsyir.pvpperformancetracker.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.ITEM_MANAGER;
import matsyir.pvpperformancetracker.controllers.AnalyzedFightPerformance;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.EquipmentData;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.Fighter;
import matsyir.pvpperformancetracker.controllers.PvpDamageCalc;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.SPRITE_MANAGER;
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.kit.KitType;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.http.api.item.ItemEquipmentStats;

@Slf4j
class FightLogDetailFrame extends JFrame
{
	// save bank filler image to display a generic None or N/A state.
	public static final AsyncBufferedImage DEFAULT_NONE_SYMBOL;
	public static final int DEFAULT_WIDTH = 400;

	static
	{
		DEFAULT_NONE_SYMBOL = ITEM_MANAGER.getImage(20594);
	}

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

	private JLabel defenderAtkLvl;
	private JLabel defenderStrLvl;
	private JLabel defenderDefLvl;
	private JLabel defenderRangeLvl;
	private JLabel defenderMageLvl;

	private JLabel attackerOffensiveLabel;
	private JLabel defenderOffensiveLabel;

	// this is a regular fightLogDetailFrame, for a normal non-merged fight.
	FightLogDetailFrame(FightPerformance fight, FightLogEntry log, int rowIdx, Point location)
	{
		super(fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName() + " - Log Details");

		this.rowIdx = rowIdx;

		setSize(DEFAULT_WIDTH, 640);
		setMinimumSize(new Dimension(DEFAULT_WIDTH, 200));
		setLayout(new BorderLayout());
		setLocation(location);
		setIconImage(FightLogFrame.frameIcon);

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

//		ItemEquipmentStats attackerStats = PvpDamageCalc.calculateBonusesToStats(log.getAttackerGear());
//		ItemEquipmentStats defenderStats = PvpDamageCalc.calculateBonusesToStats(log.getDefenderGear());

		isCompetitorLog = log.attackerName.equals(fight.getCompetitor().getName());
		attacker = isCompetitorLog ? fight.getCompetitor() : fight.getOpponent();
		defender = isCompetitorLog ? fight.getOpponent() : fight.getCompetitor();

		JPanel namesLine = new JPanel(new BorderLayout());
		GridLayout attackerNameContainerLayout = new GridLayout(1, 2);
		attackerNameContainerLayout.setHgap(4);
		JPanel attackerNameContainer = new JPanel(attackerNameContainerLayout);
		JLabel attackerName = new JLabel();
		attackerName.setText("<html>Attacker:<br/><strong>" + log.attackerName + "</strong></html>");
		attackerName.setIcon(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(log.getAnimationData().attackStyle.getStyleSpriteId(), 0))));
		attackerName.setToolTipText(log.getAnimationData().attackStyle.toString());
		attackerNameContainer.add(attackerName);

		// if it was a magic attack, display if splash or not:
		if (log.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC)
		{
			int splashIcon = log.isSplash() ? SpriteID.SPELL_ICE_BARRAGE_DISABLED : SpriteID.SPELL_ICE_BARRAGE;
			JLabel splashIconLabel = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(splashIcon, 0))));
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
		int attackerOverheadSpriteId = PLUGIN.getSpriteForHeadIcon(log.getAttackerOverhead());
		JLabel attackerOverheadLabel = new JLabel();
		if (attackerOverheadSpriteId > 0)
		{
			attackerOverheadLabel.setIcon(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(attackerOverheadSpriteId, 0))));
		}
		else
		{
			DEFAULT_NONE_SYMBOL.addTo(attackerOverheadLabel);
		}
		attackerOverheadLabel.setToolTipText("Overhead Prayer");
		attackerPrays.add(attackerOverheadLabel);
		// attacker offensive
		attackerOffensiveLabel = new JLabel();
		attackerOffensiveLabel.setToolTipText("Offensive Prayer");
		if (log.getAttackerOffensivePray() > 0) // if there's a valid offensive pray, display it.
		{
			attackerOffensiveLabel.setIcon(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(log.getAttackerOffensivePray(), 0))));
		}
		else if (isCompetitorLog) // if it's a competitor log, but no valid offensive pray, display X to show there was none.
		{
			DEFAULT_NONE_SYMBOL.addTo(attackerOffensiveLabel);
		}
		else // if it wasn't a competitor log, then this is N/A in most cases.
		{
			attackerOffensiveLabel.setText("N/A");
		}

		attackerPrays.add(attackerOffensiveLabel);
		praysUsedLine.add(attackerPrays, BorderLayout.WEST);

		GridLayout defenderPrayLayout = new GridLayout(1, 2);
		defenderPrayLayout.setHgap(4);
		JPanel defenderPrays = new JPanel(defenderPrayLayout);
		// defender overhead
		int defenderOverheadSpriteId = PLUGIN.getSpriteForHeadIcon(log.getDefenderOverhead());
		JLabel defenderOverheadLabel = new JLabel();
		if (defenderOverheadSpriteId > 0)
		{
			defenderOverheadLabel.setIcon(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(defenderOverheadSpriteId, 0))));
		}
		else
		{
			DEFAULT_NONE_SYMBOL.addTo(defenderOverheadLabel);
		}
		defenderOverheadLabel.setToolTipText("Overhead Prayer");
		defenderPrays.add(defenderOverheadLabel);

		// defender offensive (N/A in normal cases)
		defenderOffensiveLabel = new JLabel("N/A");
		defenderOffensiveLabel.setToolTipText("Offensive Prayer");
		defenderPrays.add(defenderOffensiveLabel);

		praysUsedLine.add(defenderPrays, BorderLayout.EAST);


		// level display line
		CombatLevels levels = CombatLevels.getConfigLevels();
		combatLevelsLine = new JPanel(new BorderLayout());
		JPanel attackerCombatLevels = new JPanel(new GridLayout(2, 3));
		attackerAtkLvl = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(PLUGIN.getSpriteForSkill(Skill.ATTACK), 0))));
		attackerAtkLvl.setText(String.valueOf(levels.atk));
		attackerAtkLvl.setToolTipText("Attack Level");
		attackerStrLvl = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(PLUGIN.getSpriteForSkill(Skill.STRENGTH), 0))));
		attackerStrLvl.setText(String.valueOf(levels.str));
		attackerStrLvl.setToolTipText("Strength Level");
		attackerDefLvl = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(PLUGIN.getSpriteForSkill(Skill.DEFENCE), 0))));
		attackerDefLvl.setText(String.valueOf(levels.def));
		attackerDefLvl.setToolTipText("Defence Level");
		attackerRangeLvl = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(PLUGIN.getSpriteForSkill(Skill.RANGED), 0))));
		attackerRangeLvl.setText(String.valueOf(levels.range));
		attackerRangeLvl.setToolTipText("Ranged Level");
		attackerMageLvl = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(PLUGIN.getSpriteForSkill(Skill.MAGIC), 0))));
		attackerMageLvl.setText(String.valueOf(levels.mage));
		attackerMageLvl.setToolTipText("Magic Level");

		attackerCombatLevels.add(attackerAtkLvl);
		attackerCombatLevels.add(attackerStrLvl);
		attackerCombatLevels.add(attackerDefLvl);
		attackerCombatLevels.add(attackerRangeLvl);
		attackerCombatLevels.add(attackerMageLvl);
		combatLevelsLine.add(attackerCombatLevels, BorderLayout.WEST);

		JPanel defenderCombatLevels = new JPanel(new GridLayout(2, 3));
		defenderAtkLvl = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(PLUGIN.getSpriteForSkill(Skill.ATTACK), 0))));
		defenderAtkLvl.setText(String.valueOf(levels.atk));
		defenderAtkLvl.setToolTipText("Attack Level");
		defenderStrLvl = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(PLUGIN.getSpriteForSkill(Skill.STRENGTH), 0))));
		defenderStrLvl.setText(String.valueOf(levels.str));
		defenderStrLvl.setToolTipText("Strength Level");
		defenderDefLvl = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(PLUGIN.getSpriteForSkill(Skill.DEFENCE), 0))));
		defenderDefLvl.setText(String.valueOf(levels.def));
		defenderDefLvl.setToolTipText("Defence Level");
		defenderRangeLvl = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(PLUGIN.getSpriteForSkill(Skill.RANGED), 0))));
		defenderRangeLvl.setText(String.valueOf(levels.range));
		defenderRangeLvl.setToolTipText("Ranged Level");
		defenderMageLvl = new JLabel(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(PLUGIN.getSpriteForSkill(Skill.MAGIC), 0))));
		defenderMageLvl.setText(String.valueOf(levels.mage));
		defenderMageLvl.setToolTipText("Magic Level");

		defenderCombatLevels.add(defenderAtkLvl);
		defenderCombatLevels.add(defenderStrLvl);
		defenderCombatLevels.add(defenderDefLvl);
		defenderCombatLevels.add(defenderRangeLvl);
		defenderCombatLevels.add(defenderMageLvl);
		combatLevelsLine.add(defenderCombatLevels, BorderLayout.EAST);


		// equipment stats line (stab attack, slash attack, etc)
		JPanel equipmentStatsLine = new JPanel(new BorderLayout());
		JLabel attackerStatsLabel = new JLabel();
		attackerStatsLabel.setText(getItemEquipmentStatsString(log.getAttackerGear()));
		equipmentStatsLine.add(attackerStatsLabel, BorderLayout.WEST);

		JLabel defenderStatsLabel = new JLabel();
		defenderStatsLabel.setText(getItemEquipmentStatsString(log.getDefenderGear()));
		equipmentStatsLine.add(defenderStatsLabel, BorderLayout.EAST);

		JPanel equipmentRenderLine = new JPanel(new BorderLayout());
		JPanel attackerEquipmentRender = getEquipmentRender(log.getAttackerGear());
		JPanel defenderEquipmentRender = getEquipmentRender(log.getDefenderGear());
		equipmentRenderLine.add(attackerEquipmentRender, BorderLayout.WEST);
		equipmentRenderLine.add(defenderEquipmentRender, BorderLayout.EAST);

//		JPanel generalAttackerStatsLine = new JPanel(new BorderLayout());
//		JLabel generalAttackerStats = new JLabel();
//		generalAttackerStats.setText("<html></html>");

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

	FightLogDetailFrame(AnalyzedFightPerformance fight, FightLogEntry attackerLog, FightLogEntry defenderLog, int rowIdx, Point location)
	{
		this(fight, attackerLog, rowIdx, location);

		// attacker lvls
		CombatLevels aLvls = attackerLog.getAttackerLevels();
		if (aLvls == null)
		{
			log.info("Fight Analysis: attackerLvls are null! This shouldn't happen.");
			aLvls = CombatLevels.getConfigLevels();
		}
		attackerAtkLvl.setText(String.valueOf(aLvls.atk));
		attackerStrLvl.setText(String.valueOf(aLvls.str));
		attackerDefLvl.setText(String.valueOf(aLvls.def));
		attackerRangeLvl.setText(String.valueOf(aLvls.range));
		attackerMageLvl.setText(String.valueOf(aLvls.mage));

		// defender lvls
		CombatLevels dLvls = defenderLog.getAttackerLevels();
		if (dLvls == null)
		{
			log.info("Fight Analysis: defenderLvls are null! This shouldn't happen.");
			dLvls = CombatLevels.getConfigLevels();
		}
		defenderAtkLvl.setText(String.valueOf(dLvls.atk));
		defenderStrLvl.setText(String.valueOf(dLvls.str));
		defenderDefLvl.setText(String.valueOf(dLvls.def));
		defenderRangeLvl.setText(String.valueOf(dLvls.range));
		defenderMageLvl.setText(String.valueOf(dLvls.mage));

		defenderOffensiveLabel.setText("");
		int defOffensivePray = defenderLog.getAttackerOffensivePray();

		if (defOffensivePray > 0)
		{
			defenderOffensiveLabel.setIcon(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(defOffensivePray, 0))));
		}
		else
		{
			DEFAULT_NONE_SYMBOL.addTo(defenderOffensiveLabel);
		}

		attackerOffensiveLabel.setText("");
		int atkOffensivePray = attackerLog.getAttackerOffensivePray();

		if (atkOffensivePray > 0)
		{
			attackerOffensiveLabel.setIcon(new ImageIcon(Objects.requireNonNull(SPRITE_MANAGER.getSprite(atkOffensivePray, 0))));
		}
		else
		{
			DEFAULT_NONE_SYMBOL.addTo(attackerOffensiveLabel);
		}

		validate();
		repaint();
	}

	private static JPanel getEquipmentRender(int[] itemIds)
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
		addItemToLabelIfValid(helm, itemIds[KitType.HEAD.getIndex()]);
		helmLine.add(helm, BorderLayout.CENTER);

		JLabel cape = new JLabel();
		addItemToLabelIfValid(cape, itemIds[KitType.CAPE.getIndex()]);
		JLabel amulet = new JLabel();
		addItemToLabelIfValid(amulet, itemIds[KitType.AMULET.getIndex()]);
		capeLine.add(cape, BorderLayout.WEST);
		capeLine.add(amulet, BorderLayout.CENTER);

		// ammo: get config's ammo for current weapon
		int weaponId = itemIds[KitType.WEAPON.getIndex()] > 512 ?
			itemIds[KitType.WEAPON.getIndex()] - 512 : itemIds[KitType.WEAPON.getIndex()];
		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(EquipmentData.getEquipmentDataFor(weaponId));
		if (weaponAmmo != null)
		{
			JLabel ammo = new JLabel();
			ITEM_MANAGER.getImage(weaponAmmo.getItemId()).addTo(ammo);
			capeLine.add(ammo, BorderLayout.EAST);
		}

		JLabel wep = new JLabel();
		addItemToLabelIfValid(wep, itemIds[KitType.WEAPON.getIndex()]);
		JLabel torso = new JLabel();
		addItemToLabelIfValid(torso, itemIds[KitType.TORSO.getIndex()]);
		JLabel shield = new JLabel();
		addItemToLabelIfValid(shield, itemIds[KitType.SHIELD.getIndex()]);
		weaponLine.add(wep, BorderLayout.WEST);
		weaponLine.add(torso, BorderLayout.CENTER);
		weaponLine.add(shield, BorderLayout.EAST);

		JLabel legs = new JLabel();
		legs.setHorizontalAlignment(SwingConstants.CENTER);
		addItemToLabelIfValid(legs, itemIds[KitType.LEGS.getIndex()]);
		legsLine.add(legs, BorderLayout.CENTER);

		JLabel gloves = new JLabel();
		addItemToLabelIfValid(gloves, itemIds[KitType.HANDS.getIndex()]);
		JLabel boots = new JLabel();
		addItemToLabelIfValid(boots, itemIds[KitType.BOOTS.getIndex()]);
		JLabel ring = new JLabel();
		ITEM_MANAGER.getImage(CONFIG.ringChoice().getItemId()).addTo(ring);
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

	// takes in itemId directly from PlayerComposition, so need to -512 for actual itemid
	private static void addItemToLabelIfValid(JLabel label, int itemId)
	{
		if (itemId > 512)
		{
			ITEM_MANAGER.getImage(itemId - 512).addTo(label);
			label.setToolTipText(ITEM_MANAGER.getItemComposition(itemId - 512).getName());
		}
		else
		{
			DEFAULT_NONE_SYMBOL.addTo(label);
			//label.setOp
			//label.setIcon(new ImageIcon(SPRITE_MANAGER.getSprite(SpriteID.NO, 0)));
			label.setToolTipText("Empty Slot");
		}
	}

	//
	String getItemEquipmentStatsString(int[] equipment)
	{
		ItemEquipmentStats stats = PvpDamageCalc.calculateBonusesToStats(equipment);
		// ammo: get config's ammo for current weapon
		int weaponId = equipment[KitType.WEAPON.getIndex()] > 512 ?
			equipment[KitType.WEAPON.getIndex()] - 512 : equipment[KitType.WEAPON.getIndex()];
		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(EquipmentData.getEquipmentDataFor(weaponId));
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
			"Magic damage: " + prependPlusIfPositive(stats.getMdmg()) + "%" + sep +
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
