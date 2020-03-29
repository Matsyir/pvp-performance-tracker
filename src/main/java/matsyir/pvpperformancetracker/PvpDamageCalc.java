/*
 * Copyright (c) 2020, Mazhar <https://twitter.com/maz_rs>
 * Copyright (c) 2020, Matsyir <https://github.com/matsyir>
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

import static matsyir.pvpperformancetracker.AnimationAttackType.*;
import java.util.Arrays;
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
public class PvpDamageCalc
{
	private static final int WEAPON_SLOT = 3, CHEST_SLOT = 4, LEG_SLOT = 7,
		STAB_ATTACK = 0, SLASH_ATTACK = 1, CRUSH_ATTACK = 2, MAGIC_ATTACK = 3, RANGE_ATTACK = 4,
		STAB_DEF = 5, SLASH_DEF = 6, CRUSH_DEF = 7, MAGIC_DEF = 8, RANGE_DEF = 9,
		STRENGTH_BONUS = 10, RANGE_STRENGTH = 11, MAGIC_DAMAGE = 12;

	private static final double ATTACK_LEVEL = 118;
	private static final double STRENGTH_LEVEL = 118;
	private static final double DEFENCE_LEVEL = 75;
	private static final int RANGE_LEVEL = 112;
	private static final double MAGIC_LEVEL = 99;

	private static final int STANCE_BONUS = 0; // assume they are not in controlled or defensive
	private static final double UNSUCCESSFUL_PRAY_DMG_MODIFIER = 0.6; // modifier for when you don't successfully hit off-pray

	// Offensive pray: assume you have valid. Piety for melee, Rigour for range, Augury for mage
	private static final double ATTACK_OFFENSIVE_PRAYER_MODIFIER = 1.2;
	private static final double STRENGTH_OFFENSIVE_PRAYER_MODIFIER = 1.23;
	private static final double MAGIC_OFFENSIVE_PRAYER_MODIFIER = 1.25;
	private static final double RANGE_OFFENSIVE_PRAYER_DMG_MODIFIER = 1.23;
	private static final double RANGE_OFFENSIVE_PRAYER_ATTACK_MODIFIER = 1.2;

	// Defensive pray: Assume you have one of the defensive prays active, but don't assume you have augury
	// while getting maged, since you would likely be planning to range or melee & using rigour/piety instead.
	private static final double MELEE_DEFENSIVE_PRAYER_MODIFIER = 1.25;
	private static final double MAGIC_DEFENSIVE_DEF_PRAYER_MODIFIER = 1.25;
	private static final double MAGIC_DEFENSIVE_MAGE_PRAYER_MODIFIER = 1;
	private static final double RANGE_DEFENSIVE_PRAYER_MODIFIER = 1.25;

	private static final int BARRAGE_BASE_DMG = 30;
	private static final int BLITZ_BASE_DMG = 26;

	private static final double BALLISTA_SPEC_ACCURACY_MODIFIER = 1.25;
	private static final double BALLISTA_SPEC_DMG_MODIFIER = 1.25;

	private static final int ACB_SPEC_ACCURACY_MODIFIER = 2;

	private static final int DBOW_DMG_MODIFIER = 2;
	private static final int DBOW_SPEC_DMG_MODIFIER = 3;
	private static final int DBOW_SPEC_MIN_HIT = 16;

	private static final double DDS_SPEC_ACCURACY_MODIFIER = 1.25;
	private static final double DDS_SPEC_DMG_MODIFIER = 2.3;

	private static final int AGS_SPEC_ACCURACY_MODIFIER = 2;
	private static final double AGS_SPEC_INITIAL_DMG_MODIFIER = 1.1;
	private static final double AGS_SPEC_FINAL_DMG_MODIFIER = 1.25;

	private static final double VLS_SPEC_DMG_MODIFIER = 1.2;
	private static final double VLS_SPEC_MIN_DMG_MODIFIER = .2;
	private static final double VLS_SPEC_DEFENCE_SCALE = .25;
	private static final double SWH_SPEC_DMG_MODIFIER = 1.25;
	private static final double SWH_SPEC_MIN_DMG_MODIFIER = .25;

	private ItemManager itemManager;

	public PvpDamageCalc(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	public double getDamage(Player attacker, Player defender, boolean success, AnimationAttackType animationType)
	{
		// have never seen this occur, but just in case
		if (attacker == null)
		{
			log.warn("ATTACKER WAS NULL BEFORE AVG DAMAGE CALCULATION!");
			String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL).append("ATTACKER WAS NULL BEFORE AVG DAMAGE CALCULATION!")
				.build();
			PvpPerformanceTrackerPlugin.PLUGIN.log(chatMessage);
			return 0;
		}
		if (defender == null)
		{
			log.warn("DEFENDER WAS NULL BEFORE AVG DAMAGE CALCULATION!");
			String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL).append("DEFENDER WAS NULL BEFORE AVG DAMAGE CALCULATION!")
				.build();
			PvpPerformanceTrackerPlugin.PLUGIN.log(chatMessage);
			return 0;
		}

		int[] attackerItems = attacker.getPlayerComposition().getEquipmentIds();
		int[] defenderItems = defender.getPlayerComposition().getEquipmentIds();
		int maxHit;
		double accuracy;
		double averageHit;
		int[] playerStats = this.calculateBonuses(attackerItems);
		int[] opponentStats = this.calculateBonuses(defenderItems);
		log.warn("attackerStats: " + Arrays.toString(playerStats));
		log.warn("defenderStats: " + Arrays.toString(opponentStats));
		log.warn("animationType: " + (animationType == null ? "null" : animationType.toString()));
		boolean isSpecial = animationType != null && animationType.isSpecial;
		if (isSpecial)
		{
			animationType = animationType.getRootType();
		}

		int weaponId = attackerItems[WEAPON_SLOT] > 512 ? attackerItems[WEAPON_SLOT] - 512 : attackerItems[WEAPON_SLOT];
		EquipmentData weapon = EquipmentData.getEquipmentDataFor(weaponId);
		boolean isMelee = ArrayUtils.contains(MELEE_STYLES, animationType);

		if (isMelee)
		{
			maxHit = this.getMeleeMaxHit(playerStats[STRENGTH_BONUS], isSpecial, weapon);
			accuracy = this.getMeleeAccuracy(playerStats, opponentStats, animationType, isSpecial, weapon);
		}
		else if (animationType == Ranged)
		{
			maxHit = this.getRangedMaxHit(playerStats[RANGE_STRENGTH], isSpecial, weapon);
			accuracy = this.getRangeAccuracy(playerStats[RANGE_ATTACK], opponentStats[RANGE_DEF], isSpecial, weapon);
		}
		else
		{
			maxHit = this.getMagicMaxHit(playerStats[MAGIC_DAMAGE], animationType);
			accuracy = this.getMagicAccuracy(playerStats[MAGIC_ATTACK], opponentStats[MAGIC_DEF]);
		}

		averageHit = this.getAverageHit(maxHit, accuracy, success, weapon, isSpecial);


		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		String accuracyString = nf.format(accuracy);
		String averageHitString = nf.format(averageHit);

		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(weapon);
		boolean diamonds = ArrayUtils.contains(RangeAmmoData.DIAMOND_BOLTS, weaponAmmo);

		boolean animKnown = AnimationAttackStyle.styleForAnimation(attacker.getAnimation()) != null;

        String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL).append(attacker.getName())
                .append(ChatColorType.HIGHLIGHT).append("Type: ")
                .append(ChatColorType.NORMAL).append(animationType == null ? "null" : animationType.toString())
                .append(ChatColorType.HIGHLIGHT).append("  Max: ")
                .append(ChatColorType.NORMAL).append(String.valueOf(maxHit))
                .append(ChatColorType.HIGHLIGHT).append("  Acc: ")
                .append(ChatColorType.NORMAL).append(accuracyString)
                .append(ChatColorType.HIGHLIGHT).append("  AvgHit: ")
                .append(ChatColorType.NORMAL).append(averageHitString)
				.append(ChatColorType.HIGHLIGHT).append(" Spec?: ")
				.append(ChatColorType.NORMAL).append(isSpecial ? "Y" : "N")
				.append(ChatColorType.HIGHLIGHT).append(" Dia?:")
				.append(ChatColorType.NORMAL).append(diamonds ? "Y" : "N")
				.append(ChatColorType.HIGHLIGHT).append(" OffP?:")
				.append(ChatColorType.NORMAL).append(success ? "Y" : "N")
				.append(ChatColorType.HIGHLIGHT).append(" Anim:")
				// show animation + U/K for unknown/known
				.append(ChatColorType.NORMAL).append((String.valueOf(attacker.getAnimation())))
				.append(animKnown ? ChatColorType.NORMAL : ChatColorType.HIGHLIGHT).append(animKnown ? "K " : "U ")
                .build();
		PvpPerformanceTrackerPlugin.PLUGIN.log(chatMessage);

		return averageHit;
	}

	private double getAverageHit(int maxHit, double accuracy, boolean success, EquipmentData weapon, boolean usingSpec)
	{
		boolean dbow = weapon == EquipmentData.DARK_BOW;
		boolean ags = weapon == EquipmentData.ARMADYL_GODSWORD;
		boolean claws = weapon == EquipmentData.DRAGON_CLAWS;
		boolean vls = weapon == EquipmentData.VESTAS_LONGSWORD;
		boolean swh = weapon == EquipmentData.STATIUS_WARHAMMER;

		double agsModifier = ags ? AGS_SPEC_FINAL_DMG_MODIFIER : 1;
		double prayerModifier = success ? 1 : UNSUCCESSFUL_PRAY_DMG_MODIFIER;
		double averageSuccessfulHit;
		if ((dbow || vls || swh) && usingSpec)
		{
			double accuracyAdjuster = dbow ? accuracy : 1;
			int minHit = dbow ? DBOW_SPEC_MIN_HIT : 0;
			minHit = vls ? (int) (maxHit * VLS_SPEC_MIN_DMG_MODIFIER) : minHit;
			minHit = swh ? (int) (maxHit * SWH_SPEC_MIN_DMG_MODIFIER) : minHit;

			int total = 0;

			for (int i = 0; i <= maxHit; i++)
			{
				total += i < minHit ? minHit / accuracyAdjuster : i;
			}

			averageSuccessfulHit = (double) total / maxHit;
		}
		else if (claws && usingSpec)
		{
			double invertedAccuracy = 1 - accuracy;
			double averageSuccessfulRegularHit = maxHit / 2;
			double higherModifierChance = 1 - (accuracy + (accuracy * invertedAccuracy));
			double lowerModifierChance = 1 - ((accuracy * Math.pow(invertedAccuracy, 2)) + (accuracy * Math.pow(invertedAccuracy, 3)));
			double averageSpecialHit = (higherModifierChance * 2) + (lowerModifierChance * 1.5) * averageSuccessfulRegularHit;

			return averageSpecialHit * prayerModifier;
		}
		else
		{
			averageSuccessfulHit = maxHit / 2.0;
		}
		return accuracy * averageSuccessfulHit * prayerModifier * agsModifier;
	}

	private int getMeleeMaxHit(int meleeStrength, boolean usingSpec, EquipmentData weapon)
	{
		boolean ags = weapon == EquipmentData.ARMADYL_GODSWORD;
		boolean dds = weapon == EquipmentData.DRAGON_DAGGER;
		boolean vls = weapon == EquipmentData.VESTAS_LONGSWORD;
		boolean swh = weapon == EquipmentData.STATIUS_WARHAMMER;

		int effectiveLevel = (int) Math.floor((STRENGTH_LEVEL * STRENGTH_OFFENSIVE_PRAYER_MODIFIER) + 8 + 3);
		int baseDamage = (int) Math.floor(0.5 + effectiveLevel * (meleeStrength + 64) / 640);
		double modifier = ags && usingSpec ? AGS_SPEC_INITIAL_DMG_MODIFIER : 1;
		modifier = (swh && usingSpec) ? SWH_SPEC_DMG_MODIFIER : modifier;
		modifier = (dds && usingSpec) ? DDS_SPEC_DMG_MODIFIER : modifier;
		modifier = (vls && usingSpec) ? VLS_SPEC_DMG_MODIFIER : modifier;
		return (int) (modifier * baseDamage);
	}

	private int getRangedMaxHit(int rangeStrength, boolean usingSpec, EquipmentData weapon)
	{
		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(weapon);
		boolean ballista = weapon == EquipmentData.HEAVY_BALLISTA || weapon == EquipmentData.HEAVY_BALLISTA_PVP;
		boolean dbow = weapon == EquipmentData.DARK_BOW || weapon == EquipmentData.DARK_BOW_PVP;

		int ammoStrength = weaponAmmo == null ? 0 : weaponAmmo.getRangeStr();

		rangeStrength += ammoStrength;

		int effectiveLevel = (int) Math.floor((RANGE_LEVEL * RANGE_OFFENSIVE_PRAYER_DMG_MODIFIER) + 8);
		int baseDamage = (int) Math.floor(0.5 + effectiveLevel * (rangeStrength + 64) / 640);

		double modifier = weaponAmmo == null ? 1 : weaponAmmo.getDmgModifier();
		modifier = ballista && usingSpec ? BALLISTA_SPEC_DMG_MODIFIER : modifier;
		modifier = dbow && !usingSpec ? DBOW_DMG_MODIFIER : modifier;
		modifier = dbow && usingSpec ? DBOW_SPEC_DMG_MODIFIER : modifier;
		return weaponAmmo == null ?
			(int) (modifier * baseDamage) :
			(int) ((modifier * baseDamage) + weaponAmmo.getBonusMaxHit());
	}

	private int getMagicMaxHit(int mageDamageBonus, AnimationAttackType animationType)
	{
		int baseDamage = animationType == Barrage ? BARRAGE_BASE_DMG : BLITZ_BASE_DMG;
		double magicBonus = 1 + (mageDamageBonus / 100);
		return (int) (baseDamage * magicBonus);
	}

	private double getMeleeAccuracy(int[] playerStats, int[] opponentStats, AnimationAttackType animationType, boolean usingSpec, EquipmentData weapon)
	{
		boolean vls = weapon == EquipmentData.VESTAS_LONGSWORD;
		boolean ags = weapon == EquipmentData.ARMADYL_GODSWORD;
		boolean dds = weapon == EquipmentData.DRAGON_DAGGER;

		double stabBonusPlayer = playerStats[STAB_ATTACK];
		double slashBonusPlayer = playerStats[SLASH_ATTACK];
		double crushBonusPlayer = playerStats[CRUSH_ATTACK];

		double stabBonusTarget = opponentStats[STAB_DEF];
		double slashBonusTarget = opponentStats[SLASH_DEF];
		double crushBonusTarget = opponentStats[CRUSH_DEF];

		double effectiveLevelPlayer;
		double effectiveLevelTarget;

		double baseChance;
		double attackerChance;
		double defenderChance = 0;

		double hitChance;
		double accuracyModifier = dds ? DDS_SPEC_ACCURACY_MODIFIER : ags ? AGS_SPEC_ACCURACY_MODIFIER : 1;

		/**
		 * Attacker Chance
		 */
		effectiveLevelPlayer = Math.floor(((ATTACK_LEVEL * ATTACK_OFFENSIVE_PRAYER_MODIFIER) + STANCE_BONUS) + 8);

		final double attackBonus = animationType == Stab ? stabBonusPlayer
			: animationType == Slash ? slashBonusPlayer : crushBonusPlayer;

		baseChance = Math.floor(effectiveLevelPlayer * (attackBonus + 64));
//        log.debug("baseChance : " + baseChance );
//        log.debug("baseChance : " + accuracyModifier );
		if (usingSpec)
		{
			baseChance = baseChance * accuracyModifier;
		}

		attackerChance = baseChance;
//        log.debug("MELEE ATTACK: " + attackerChance );
//        log.debug("dds : " + dds );

		/**
		 * Defender Chance
		 */
		effectiveLevelTarget = Math.floor(((DEFENCE_LEVEL * MELEE_DEFENSIVE_PRAYER_MODIFIER) + STANCE_BONUS) + 8);

		if (vls && usingSpec)
		{
			defenderChance = Math.floor((effectiveLevelTarget * (stabBonusTarget + 64)) * VLS_SPEC_DEFENCE_SCALE);
		}
		else if (animationType == Slash)
		{
			defenderChance = Math.floor(effectiveLevelTarget * (slashBonusTarget + 64));
		}
		else if (animationType == Stab)
		{
			defenderChance = Math.floor(effectiveLevelTarget * (stabBonusTarget + 64));
		}
		else if (animationType == Crush)
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

	private double getRangeAccuracy(int playerRangeAtt, int opponentRangeDef, boolean usingSpec, EquipmentData weapon)
	{
		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(weapon);
		boolean diamonds = ArrayUtils.contains(RangeAmmoData.DIAMOND_BOLTS, weaponAmmo);
		double effectiveLevelPlayer;
		double effectiveLevelTarget;
		double rangeModifier;
		double attackerChance;
		double defenderChance;
		double hitChance;

		/**
		 * Attacker Chance
		 */
		effectiveLevelPlayer = Math.floor(((RANGE_LEVEL * RANGE_OFFENSIVE_PRAYER_ATTACK_MODIFIER) + STANCE_BONUS) + 8);
		rangeModifier = Math.floor(effectiveLevelPlayer * ((double) playerRangeAtt + 64));
		if (usingSpec)
		{
			boolean acb = weapon == EquipmentData.ARMADYL_CROSSBOW || weapon == EquipmentData.ARMADYL_CROSSBOW_PVP;
			boolean ballista = weapon == EquipmentData.HEAVY_BALLISTA || weapon == EquipmentData.HEAVY_BALLISTA_PVP;

			double specAccuracyModifier = acb ? ACB_SPEC_ACCURACY_MODIFIER :
				ballista ? BALLISTA_SPEC_ACCURACY_MODIFIER : 1;

			attackerChance = Math.floor(rangeModifier * specAccuracyModifier);
		}
		else
		{
			attackerChance = rangeModifier;
		}

		/**
		 * Defender Chance
		 */
		effectiveLevelTarget = Math.floor(((DEFENCE_LEVEL * RANGE_DEFENSIVE_PRAYER_MODIFIER) + STANCE_BONUS) + 8);
		defenderChance = Math.floor(effectiveLevelTarget * ((double) opponentRangeDef + 64));

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

		// diamond bolts accuracy: 10% of attacks are 100% accuracy, so apply avg accuracy as:
		// (90% of normal accuracy) + (10% of 100% accuracy)
		return diamonds ? (hitChance * .9) + .1 : hitChance;
	}

	private double getMagicAccuracy(int playerMageAtt, int opponentMageDef)
	{
		double effectiveLevelPlayer;

		double reducedDefenceLevelTarget;
		double effectiveMagicDefenceTarget;
		double effectiveMagicLevelTarget;

		double effectiveLevelTarget;

		double magicModifier;

		double attackerChance;
		double defenderChance;
		double hitChance;

		/**
		 * Attacker Chance
		 */
		effectiveLevelPlayer = Math.floor(((MAGIC_LEVEL * MAGIC_OFFENSIVE_PRAYER_MODIFIER)) + 8);
		magicModifier = Math.floor(effectiveLevelPlayer * ((double) playerMageAtt + 64));
		attackerChance = magicModifier;

		/**
		 * Defender Chance
		 */
		effectiveLevelTarget = Math.floor(((DEFENCE_LEVEL * MAGIC_DEFENSIVE_DEF_PRAYER_MODIFIER) + STANCE_BONUS) + 8);
		effectiveMagicLevelTarget = Math.floor((MAGIC_LEVEL * MAGIC_DEFENSIVE_MAGE_PRAYER_MODIFIER) * 0.70);
		reducedDefenceLevelTarget = Math.floor(effectiveLevelTarget * 0.30);
		effectiveMagicDefenceTarget = effectiveMagicLevelTarget + reducedDefenceLevelTarget;
		defenderChance = Math.floor(effectiveMagicDefenceTarget * ((double) opponentMageDef + 64));

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

	// Retrieve item stats for a single item, returned as an int array so they can be modified.
	// First, try to get the item stats from the item manager. If stats weren't present in the
	// itemManager, try get the 'real' item id from the EquipmentData. If it's not defined in EquipmentData, it will return null
	// and count as 0 stats, but that should be very rare.
	public int[] getItemStats(int itemId)
	{
		ItemStats itemStats = this.itemManager.getItemStats(itemId, false);
		if (itemStats == null)
		{
			EquipmentData itemData = EquipmentData.getEquipmentDataFor(itemId);
			if (itemData != null)
			{
				itemId = itemData.getRealItemId();
				itemStats = this.itemManager.getItemStats(itemId, false);
			}
		}

		if (itemStats != null)
		{
			final ItemEquipmentStats equipmentStats = itemStats.getEquipment();
			return new int[] {
				equipmentStats.getAstab(),	// 0
				equipmentStats.getAslash(),	// 1
				equipmentStats.getAcrush(),	// 2
				equipmentStats.getAmagic(),	// 3
				equipmentStats.getArange(),	// 4
				equipmentStats.getDstab(),	// 5
				equipmentStats.getDslash(),	// 6
				equipmentStats.getDcrush(),	// 7
				equipmentStats.getDmagic(),	// 8
				equipmentStats.getDrange(),	// 9
				equipmentStats.getStr(),	// 10
				equipmentStats.getRstr(),	// 11
				equipmentStats.getMdmg(),	// 12
			};
		}

		// when combining multiple items' stats, null stats will just be skipped, without affecting
		// the total stats.
		return null;
	}

	// Calculate total equipment bonuses for all given items
	private int[] calculateBonuses(int[] itemIds)
	{
		int[] equipmentBonuses = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, PvpPerformanceTrackerPlugin.CONFIG.assumeZerkRing() ? 4 : 0, 0, 0 };
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
}

