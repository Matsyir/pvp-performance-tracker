package com.pvpperformancetracker;

import java.awt.Color;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.text.NumberFormat;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;
import org.apache.commons.lang3.ArrayUtils;
import net.runelite.api.Player;


@Slf4j
public class LmsDamageCalc
{
	private final ItemManager itemManager;

	public LmsDamageCalc(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	public final int WEAPON_SLOT = 3, CHEST_SLOT = 4, LEG_SLOT = 7;
	public final int STAB_ATTACK = 0, SLASH_ATTACK = 1, CRUSH_ATTACK = 2, MAGIC_ATTACK = 3, RANGE_ATTACK = 4;
	public final int STAB_DEF = 5, SLASH_DEF = 6, CRUSH_DEF = 7, MAGIC_DEF = 8, RANGE_DEF = 9;
	public final int STRENGTH_BONUS = 10, MAGIC_DAMAGE = 13;
	public int topId;
	public int legId;
	public boolean success;
	public boolean isMysticTop;
	public boolean isAhrimsTop;
	public boolean isMysticLegs;
	public boolean isAhrimsLegs;
	public boolean wrongGear;

	public int getDamage(Player attacker, Player defender, boolean success, String animationType)
	{
		this.success = success;
		int[] attackerItems = attacker.getPlayerComposition().getEquipmentIds();
		int[] defenderItems = defender.getPlayerComposition().getEquipmentIds();
		int maxHit = 0;
		double accuracy = 0;
		int averageHit;
		int[] playerStats = this.calculateBonuses(attackerItems);
		int[] opponentStats = this.calculateBonuses(defenderItems);
		log.warn("attacker");
		log.warn(Arrays.toString(playerStats));
		log.warn("defender");
		log.warn(Arrays.toString(opponentStats));
		String adjustedType = animationType;
		log.warn("animationType" + animationType);
		int animationTypeSize = animationType.length();
		boolean isSpecial = false;
		if (animationTypeSize > 8)
		{
			isSpecial = animationType.substring(0, 7).equals("Special");
		}
		if (isSpecial)
		{
			adjustedType = animationType.substring(8, animationTypeSize);
		}
		String[] meleeStyles = {"Stab", "Slash", "Crush"};

		int weaponId = attackerItems[WEAPON_SLOT] > 512 ? attackerItems[WEAPON_SLOT] - 512 : attackerItems[WEAPON_SLOT];
		topId = defenderItems[CHEST_SLOT] > 512 ? defenderItems[CHEST_SLOT] - 512 : defenderItems[CHEST_SLOT];
		legId = defenderItems[LEG_SLOT] > 512 ? defenderItems[LEG_SLOT] - 512 : defenderItems[LEG_SLOT];
		boolean isMelee = ArrayUtils.contains(meleeStyles, adjustedType);
		isMysticTop = topId == 20425;
		isAhrimsTop = topId == 20598;
		isMysticLegs = legId == 20426;
		isAhrimsLegs = legId == 20599;
		wrongGear = isMysticTop || isAhrimsTop || isMysticLegs || isAhrimsLegs;

		if (isMelee)
		{
			maxHit = this.getMeleeMaxHit(playerStats[STRENGTH_BONUS], isSpecial, weaponId);
			accuracy = this.getMeleeAccuracy(playerStats, opponentStats, adjustedType, isSpecial, weaponId);

		}
		else if (animationType.equals("Ranged"))
		{
			maxHit = this.getRangedMaxHit(isSpecial, weaponId);
			accuracy = this.getRangeAccuracy(playerStats[RANGE_ATTACK], opponentStats[RANGE_DEF], isSpecial, weaponId);
		}
		else
		{
			maxHit = this.getMagicMaxHit(playerStats[MAGIC_DAMAGE], animationType);
			accuracy = this.getMagicAccuracy(playerStats[MAGIC_ATTACK], opponentStats[MAGIC_DEF]);
		}
		;
		averageHit = this.getAverageHit(maxHit, accuracy, success, weaponId, isSpecial);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		String accuracyString = nf.format(accuracy);

//        String chatMessage = new ChatMessageBuilder()
//                .append(ChatColorType.HIGHLIGHT)
//                .append("Type: ")
//                .append(ChatColorType.NORMAL)
//                .append(adjustedType)
//                .append(ChatColorType.HIGHLIGHT)
//                .append("  Max: ")
//                .append(ChatColorType.NORMAL)
//                .append("" + maxHit)
//                .append(ChatColorType.HIGHLIGHT)
//                .append("  Acc: ")
//                .append(ChatColorType.NORMAL)
//                .append("" + accuracyString)
//                .append(ChatColorType.HIGHLIGHT)
//                .append("  AvgHit: ")
//                .append(ChatColorType.NORMAL)
//                .append("" + averageHit)
//                .build();
//        lmsPlugin.log(chatMessage);
		return averageHit;
	}

	private int getAverageHit(int maxHit, double accuracy, boolean success, int weaponId, boolean usingSpec)
	{
		boolean dbow = weaponId == 20408;
		boolean ags = weaponId == 20593;
		boolean claws = weaponId == 20784;
		boolean vls = weaponId == 23615;
		boolean swh = weaponId == 23620;

		double agsModifier = ags ? 1.25 : 1;
		double prayerModifier = success ? 1 : 0.6;
		double averageSuccessfulHit;
		if ((dbow || vls || swh) && usingSpec)
		{
			double accuracyAdjuster = dbow ? accuracy : 1;
			int minHit = dbow ? 16 : 0;
			minHit = vls ? (int) (maxHit * 1.20) : minHit;
			minHit = swh ? (int) (maxHit * 1.25) : minHit;

			int total = 0;

			for (int i = 0; i <= maxHit; i++)
			{
				total += i < minHit ? minHit / accuracyAdjuster : i;
			}

			averageSuccessfulHit = (double) (total / maxHit);
		}
		else if (claws && usingSpec)
		{
			double invertedAccuracy = 1 - accuracy;
			double averageSuccessfulRegularHit = maxHit / 2;
			double higherMultiplierChance = 1 - (accuracy + (accuracy * invertedAccuracy));
			double lowerMultiplierChance = 1 - ((accuracy * Math.pow(invertedAccuracy, 2)) + (accuracy * Math.pow(invertedAccuracy, 3)));
			double averageSpecialHit = (higherMultiplierChance * 2) + (lowerMultiplierChance * 1.5) * averageSuccessfulRegularHit;
			return (int) (averageSpecialHit * prayerModifier);

		}
		else
		{
			averageSuccessfulHit = (double) (maxHit / 2);
		}
		return (int) (accuracy * averageSuccessfulHit * prayerModifier * agsModifier);
	}

	private int getMeleeMaxHit(int meleeStrength, boolean usingSpec, int weaponId)
	{
		boolean ags = weaponId == 20593;
		boolean dds = weaponId == 20407;
		boolean vls = weaponId == 23615;
		boolean swh = weaponId == 23620;

		int effectiveLevel = (int) Math.floor((118 * 1.23) + 8 + 3);
		int baseDamage = (int) Math.floor(0.5 + effectiveLevel * (meleeStrength + 64) / 640);
		double amplifier = ags && usingSpec ? 1.1 : 1;
		amplifier = (swh && usingSpec) ? 1.25 : amplifier;
		amplifier = (dds && usingSpec) ? 2.3 : amplifier;
		amplifier = (vls && usingSpec) ? 1.2 : amplifier;

		return (int) (amplifier * baseDamage);
	}

	private int getRangedMaxHit(boolean usingSpec, int weaponId)
	{
		boolean diamond = (weaponId == 23601 || weaponId == 23611);
		boolean ballista = weaponId == 23630;
		boolean dbow = weaponId == 20408;

		int rangeStrength = diamond ? 105 : ballista ? 150 : 60;

		int effectiveLevel = (int) Math.floor((112 * 1.23) + 8);
		int baseDamage = (int) Math.floor(0.5 + effectiveLevel * (rangeStrength + 64) / 640);

		double amplifier = diamond ? 1.015 : 1;
		amplifier = ballista && usingSpec ? 1.25 : amplifier;
		amplifier = dbow && !usingSpec ? 2 : amplifier;
		amplifier = dbow && usingSpec ? 3 : amplifier;
		return (int) (amplifier * baseDamage);
	}


	private int getMagicMaxHit(int mageDamageBonus, String animationType)
	{
		int baseDamage = animationType.equals("Barrage") ? 30 : 26;
		double magicBonus = 1 + (mageDamageBonus / 100);
		return (int) (baseDamage * magicBonus);
	}

	private double getMeleeAccuracy(int[] playerStats, int[] opponentStats, String animationType, boolean usingSpec, int weaponId)
	{
		boolean vls = weaponId == 23615;
		boolean ags = weaponId == 20593;
		boolean dds = weaponId == 20407;


		double attackLevelPlayer = 118;
		double defenceLevelTarget = 75;

		double attackPrayerMultiplier = 1.2;

		double defencePrayerMultiplier = 1.25;

		double stabBonusPlayer = playerStats[STAB_ATTACK];
		double slashBonusPlayer = playerStats[SLASH_ATTACK];
		double crushBonusPlayer = playerStats[CRUSH_ATTACK];

		double stabBonusTarget = opponentStats[STAB_DEF];
		double slashBonusTarget = opponentStats[SLASH_DEF];
		double crushBonusTarget = opponentStats[CRUSH_DEF];

		double stanceBonusPlayer = 0;
		double stanceBonusTarget = 0;

		double effectiveLevelPlayer;

		double effectiveLevelTarget = 0;

		double baseChance = 0;

		double attackerChance = 0;
		double defenderChance = 0;

		double hitChance;

		double accuracyMultiplier = dds ? 1.25 : ags ? 2 : 1;

		/**
		 * Attacker Chance
		 */

		effectiveLevelPlayer = Math.floor(((attackLevelPlayer * attackPrayerMultiplier) + stanceBonusPlayer) + 8);

		final double attackBonus = animationType.equals("Stab") ? stabBonusPlayer
			: animationType.equals("Slash") ? slashBonusPlayer : crushBonusPlayer;

		baseChance = Math.floor(effectiveLevelPlayer * (attackBonus + 64));
//        log.debug("baseChance : " + baseChance );
//        log.debug("baseChance : " + accuracyMultiplier );
		if (usingSpec)
		{
			baseChance = baseChance * accuracyMultiplier;
		}

		attackerChance = baseChance;
//        log.debug("MELEE ATTACK: " + attackerChance );
//        log.debug("dds : " + dds );
		/**
		 * Defender Chance
		 */
		effectiveLevelTarget = Math.floor(((defenceLevelTarget * defencePrayerMultiplier) + stanceBonusTarget) + 8);

		if (vls && usingSpec)
		{
			defenderChance = Math.floor((effectiveLevelTarget * (stabBonusTarget + 64)) * .25);
		}
		else if (animationType.equals("Slash"))
		{
			defenderChance = Math.floor(effectiveLevelTarget * (slashBonusTarget + 64));

		}
		else if (animationType.equals("Stab"))
		{
			defenderChance = Math.floor(effectiveLevelTarget * (stabBonusTarget + 64));

		}
		else if (animationType.equals("Crush"))
		{
			defenderChance = Math.floor(effectiveLevelTarget * (crushBonusTarget + 64));
		}
//        log.debug("MELEE ATTACK: " + defenderChance );
		/**
		 * Calculate Accuracy
		 */

		if (attackerChance > defenderChance)
		{

			hitChance = 1 - (defenderChance + 2) / (2 * (attackerChance + 1));
		}
		else
		{
			hitChance = attackerChance / (2 * (defenderChance + 1));

		}

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.format(hitChance);

		return hitChance;
	}

	private double getRangeAccuracy(int playerRangeAtt, int opponentRangeDef, boolean usingSpec, int weaponId)
	{
		boolean diamond = (weaponId == 23601 || weaponId == 23611);
		double rangeLevelPlayer = 112;
		double defenceLevelTarget = 75;
		double rangePrayerMultiplier = 1.23;
		double defencePrayerMultiplier = 1.25;
		double rangeBonusPlayer = playerRangeAtt;
		double rangeBonusTarget = opponentRangeDef;
		double stanceBonusPlayer = 0;
		double stanceBonusTarget = 0;
		double effectiveLevelPlayer = 0;
		double effectiveLevelTarget = 0;
		double rangeModifier = 0;
		double attackerChance = 0;
		double defenderChance = 0;
		double hitChance;
		double accuracyMultiplier = diamond ? 2 : 1.25;

		/**
		 * Attacker Chance
		 */

		effectiveLevelPlayer = Math.floor(((rangeLevelPlayer * rangePrayerMultiplier) + stanceBonusPlayer) + 8);
		rangeModifier = Math.floor(effectiveLevelPlayer * (rangeBonusPlayer + 64));
		if (usingSpec)
		{
			attackerChance = Math.floor(rangeModifier * accuracyMultiplier);
		}
		else
		{
			attackerChance = rangeModifier;
		}

		/**
		 * Defender Chance
		 */
		effectiveLevelTarget = Math.floor(((defenceLevelTarget * defencePrayerMultiplier) + stanceBonusTarget) + 8);
		defenderChance = Math.floor(effectiveLevelTarget * (rangeBonusTarget + 64));

		/**
		 * Calculate Accuracy
		 */
		if (attackerChance > defenderChance)
		{
			hitChance = 1 - (defenderChance + 2) / (2 * (attackerChance + 1));
		}
		else
		{
			hitChance = attackerChance / (2 * (defenderChance + 1));
		}

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.format(hitChance);

		return diamond ? hitChance * 1.1 : hitChance;

	}

	private double getMagicAccuracy(int playerMageAtt, int opponentMageDef)
	{
		double magicLevelPlayer = 99;

		double defenceLevelTarget = 75;
		double magicLevelTarget = 99;

		double magicPrayerMultiplier = 1.25;

		double defencePrayerMultiplier = 1.25;
		double magicPrayerMultiplierTarget = 1;

		double mageBonusPlayer = playerMageAtt;

		double mageBonusTarget = opponentMageDef;

		int targetStanceBonus = 0; //assume they are not in controlled or defensive

		double effectiveLevelPlayer;

		double reducedDefenceLevelTarget;
		double effectiveMagicDefenceTarget;
		double effectiveMagicLevelTarget;

		double effectiveLevelTarget;

		double magicModifier = 0;

		double attackerChance = 0;
		double defenderChance = 0;
		double hitChance;

		/**
		 * Attacker Chance
		 */

		effectiveLevelPlayer = Math.floor(((magicLevelPlayer * magicPrayerMultiplier)) + 8);

		magicModifier = Math.floor(effectiveLevelPlayer * (mageBonusPlayer + 64));

		attackerChance = magicModifier;

		/**
		 * Defender Chance
		 */

		effectiveLevelTarget = Math.floor(((defenceLevelTarget * defencePrayerMultiplier) + targetStanceBonus) + 8);

		effectiveMagicLevelTarget = Math.floor((magicLevelTarget * magicPrayerMultiplierTarget) * 0.70);

		reducedDefenceLevelTarget = Math.floor(effectiveLevelTarget * 0.30);

		effectiveMagicDefenceTarget = effectiveMagicLevelTarget + reducedDefenceLevelTarget;

		defenderChance = Math.floor(effectiveMagicDefenceTarget * (mageBonusTarget + 64));

		/**
		 * Calculate Accuracy
		 */

		if (attackerChance > defenderChance)
		{

			hitChance = 1 - (defenderChance + 2) / (2 * (attackerChance + 1));
		}
		else
		{
			hitChance = attackerChance / (2 * (defenderChance + 1));

		}

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.format(hitChance);

		return hitChance;
	}

	public int[] getItemStats(int itemId)
	{
		final ItemStats itemStats = this.itemManager.getItemStats(itemId, false);
		if (itemStats != null)
		{
			final ItemEquipmentStats equipmentStats = itemStats.getEquipment();
			int[] item = {
				equipmentStats.getAstab(),
				equipmentStats.getAslash(),
				equipmentStats.getAcrush(),
				equipmentStats.getAmagic(),
				equipmentStats.getArange(),
				equipmentStats.getDstab(),
				equipmentStats.getDslash(),
				equipmentStats.getDcrush(),
				equipmentStats.getDmagic(),
				equipmentStats.getDrange(),
				equipmentStats.getStr(),
				equipmentStats.getRstr(),
				equipmentStats.getMdmg(),
			};
			return item;
		}
		else
		{
			return LmsItemData.getItemStats(itemId);
		}
	}

	private int[] calculateBonuses(int[] itemIds)
	{
		int[] equipmentBonuses = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0};
		for (int item : itemIds)
		{
			if (item > 512)
			{
				int[] bonuses = getItemStats(item - 512);

				if (bonuses == null)
				{
					continue;
				}

				for (int id = 0; id < bonuses.length; id++)
				{

					equipmentBonuses[id] += bonuses[id];
				}
			}
		}
		return equipmentBonuses;
	}

	private enum LmsItemData
	{
		NONE(0, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
		ARMADYL_GODSWORD(20593, new int[]{0, 132, 80, 0, 0, 0, 0, 0, 0, 0, 132, 0, 8, 0}),
		DARK_BOW(20408, new int[]{0, 0, 0, 0, 95, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
		DRAGON_ARROW(20389, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 60, 0, 0}),
		DRAGON_CLAWS(20784, new int[]{41, 57, -4, 0, 0, 13, 26, 7, 0, 0, 56, 0, 0, 0}),
		GRANITE_MAUL(20557, new int[]{0, 0, 81, 0, 0, 0, 0, 0, 0, 0, 79, 0, 0, 0}),
		MAGES_BOOK(23652, new int[]{0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0}),
		SEERS_RING_I(23624, new int[]{0, 0, 0, 8, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0}),
		AHRIMS_ROBE_TOP(20598, new int[]{0, 0, 0, 30, -10, 52, 37, 63, 30, 0, 0, 0, 0, 0}),
		AHRIMS_ROBE_SKIRT(20599, new int[]{0, 0, 0, 22, -7, 33, 30, 36, 22, 0, 0, 0, 0, 0}),
		AMULET_OF_FURY(23640, new int[]{10, 10, 10, 10, 10, 15, 15, 15, 15, 15, 8, 0, 5, 0}),
		BANDOS_TASSETS(23646, new int[]{0, 0, 0, -21, -7, 71, 63, 66, -4, 93, 2, 0, 1, 0}),
		BLESSED_SPIRIT_SHIELD(23642, new int[]{0, 0, 0, 0, 0, 53, 55, 73, 2, 52, 0, 0, 3, 0}),
		DHAROKS_HELM(23639, new int[]{0, 0, 0, -3, -1, 45, 48, 44, -1, 51, 0, 0, 0, 0}),
		DHAROKS_PLATELEGS(23633, new int[]{0, 0, 0, -21, -7, 85, 82, 83, -4, 92, 0, 0, 0, 0}),
		GUTHANS_HELM(23638, new int[]{0, 0, 0, -6, -2, 55, 58, 54, -1, 62, 0, 0, 0, 0}),
		KARILS_TOP(23632, new int[]{0, 0, 0, -15, 30, 47, 42, 50, 65, 57, 0, 0, 0, 0}),
		TORAGS_HELM(23637, new int[]{0, 0, 0, -6, -2, 55, 58, 54, -1, 62, 0, 0, 0, 0}),
		TORAGS_PLATELEGS(23634, new int[]{0, 0, 0, -21, -7, 85, 82, 83, -4, 92, 0, 0, 0, 0}),
		VERACS_HELM(23636, new int[]{0, 0, 0, -6, -2, 55, 58, 54, 0, 56, 0, 0, 3, 0}),
		VERACS_PLATESKIRT(23635, new int[]{0, 0, 0, -21, -7, 85, 82, 83, 0, 84, 0, 0, 4, 0}),
		STATIUS_WARHAMMER(23620, new int[]{-4, -4, 123, 0, 0, 0, 0, 0, 0, 0, 114, 0, 0, 0}),
		VESTAS_LONGSWORD(23615, new int[]{106, 121, -2, 0, 0, 1, 4, 3, 0, 0, 118, 0, 0, 0}),
		ZURIELS_STAFF(23617, new int[]{13, -1, 65, 18, 0, 5, 7, 4, 18, 0, 72, 0, 0, 10}),
		MORRIGANS_JAVELIN(23619, new int[]{0, 0, 0, 0, 105, 0, 0, 0, 0, 0, 0, 145, 0, 0}),
		SPIRIT_SHIELD(23599, new int[]{0, 0, 0, 0, 0, 39, 41, 50, 1, 45, 0, 0, 1, 0}),
		HELM_OF_NEITIZNOT(23591, new int[]{0, 0, 0, 0, 0, 31, 29, 34, 3, 30, 3, 0, 3, 0}),
		AMULET_OF_GLORY(20586, new int[]{10, 10, 10, 10, 10, 3, 3, 3, 3, 3, 6, 0, 3, 0}),
		DIAMOND_BOLTS_E(23649, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 105, 0, 0}),
		ABYSSAL_WHIP(20405, new int[]{0, 82, 0, 0, 0, 0, 0, 0, 0, 0, 82, 0, 0, 0}),
		DRAGON_DEFENDER(23597, new int[]{25, 24, 23, -3, -2, 25, 24, 23, -3, -2, 6, 0, 0, 0}),
		BLACK_DHIDE_BODY(20423, new int[]{0, 0, 0, -15, 30, 55, 47, 60, 50, 55, 0, 0, 0, 0}),
		RUNE_PLATELEGS(20422, new int[]{0, 0, 0, -21, -7, 51, 49, 47, -4, 49, 0, 0, 0, 0}),
		ROCK_CLIMBING_BOOTS(20578, new int[]{0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 2, 0, 0, 0}),
		GLOVES(23593, new int[]{12, 12, 12, 6, 12, 12, 12, 12, 6, 12, 12, 0, 0, 0}),
		BERSERKER_RING_I(23595, new int[]{0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 8, 0, 0, 0}),
		AHRIMS_STAFF(23653, new int[]{12, -1, 65, 15, 0, 3, 5, 2, 15, 0, 68, 0, 0, 5}),
		DRAGON_DAGGER(20407, new int[]{40, 25, -4, 1, 0, 0, 0, 0, 1, 0, 40, 0, 0, 0}),
		RUNE_CROSSBOW(23601, new int[]{0, 0, 0, 0, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
		MYSTIC_ROBE_TOP(20425, new int[]{0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0}),
		MYSTIC_ROBE_BOTTOM(20426, new int[]{0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0}),

		ELDER_MAUL(21205, new int[]{0, 0, 135, -4, 0, 0, 0, 0, 0, 0, 147, 0, 0, 0}),

		ARMADYL_CROSSBOW(23611, new int[]{0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 1, 0}),
		STAFF_OF_THE_DEAD(23613, new int[]{55, 70, 0, 17, 0, 0, 3, 3, 17, 0, 72, 0, 0, 15}),
		INFERNAL_CAPE(23622, new int[]{4, 4, 4, 1, 1, 12, 12, 12, 12, 12, 8, 0, 2, 0}),
		KODAI_WAND(23626, new int[]{0, 0, 0, 28, 0, 0, 3, 3, 20, 0, 0, 0, 0, 15}),
		GHRAZI_RAPIER(23628, new int[]{94, 55, 0, 0, 0, 0, 0, 0, 0, 0, 89, 0, 0, 0}),
		IMBUED_ZAMORAK_CAPE(23605, new int[]{0, 0, 0, 15, 0, 3, 3, 3, 15, 0, 0, 0, 0, 2}),
		IMBUED_GUTHIX_CAPE(23603, new int[]{0, 0, 0, 15, 0, 3, 3, 3, 15, 0, 0, 0, 0, 2}),
		IMBUED_SARADOMIN_CAPE(23607, new int[]{0, 0, 0, 15, 0, 3, 3, 3, 15, 0, 0, 0, 0, 2}),
		DRAGON_JAVELIN(23648, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 150, 0, 0}),
		HEAVY_BALLISTA(23630, new int[]{0, 0, 0, 0, 125, 0, 0, 0, 0, 0, 0, 15, 0, 0}),
		OCCULT_NECKLACE(23654, new int[]{0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 2, 10}),
		ETERNAL_BOOTS(23644, new int[]{0, 0, 0, 8, 0, 5, 5, 5, 8, 5, 0, 0, 0, 0});

		public int getItemId()
		{
			return itemId;
		}

		public int[] getItemBonuses()
		{
			return itemBonuses;
		}

		private final int itemId;
		private final int[] itemBonuses;


		LmsItemData(int itemId, int[] itemBonuses)
		{
			this.itemId = itemId;
			this.itemBonuses = itemBonuses;
		}

		public static int[] getItemStats(int itemId)
		{
			return itemStats.get(itemId);
		}

		private static final Map<Integer, int[]> itemStats = new HashMap<>();

		public static int[] calculateBonuses(int[] itemIds)
		{
			int[] equipmentBonuses = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0};
			for (int item : itemIds)
			{
				if (item > 512)
				{
					int[] bonuses = getItemStats(item - 512);

					if (bonuses == null)
					{
						continue;
					}

					for (int id = 0; id < bonuses.length; id++)
					{

						equipmentBonuses[id] += bonuses[id];
					}
				}
			}
			return equipmentBonuses;
		}

		static
		{
			for (LmsItemData data : LmsItemData.values())
			{

				itemStats.put(data.getItemId(), data.getItemBonuses());
			}

		}
	}
}
