/*
 * Copyright (c) 2021, Matsyir <https://github.com/matsyir>
 * Copyright (c) 2020, Mazhar <https://twitter.com/maz_rs>
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
package matsyir.pvpperformancetracker.controllers;

import lombok.Getter;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.models.AnimationData;
import static matsyir.pvpperformancetracker.models.AnimationData.AttackStyle;
import static matsyir.pvpperformancetracker.models.FightLogEntry.nf;
import static matsyir.pvpperformancetracker.models.AnimationData.MAGIC_VOLATILE_NIGHTMARE_STAFF_SPEC;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import static matsyir.pvpperformancetracker.utils.PvpPerformanceTrackerUtils.fixItemId;
import matsyir.pvpperformancetracker.models.EquipmentData;
import matsyir.pvpperformancetracker.models.EquipmentData.VoidStyle;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import matsyir.pvpperformancetracker.models.RingData;
import net.runelite.api.PlayerComposition;
import net.runelite.api.SpriteID;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemStats;
import org.apache.commons.lang3.ArrayUtils;
import net.runelite.api.Player;

// Pvp damage calculations
// call updateDamageStats(...) with required parameters, and retrieve results by using the field getters
// for averageHit, accuracy, maxHit, and minHit.
//
// combat formulas source thread QFC: 317,318,461,66138854
// osrs wiki was also used a lot
@Slf4j
public class PvpDamageCalc
{
	private static final int STAB_ATTACK = 0, SLASH_ATTACK = 1, CRUSH_ATTACK = 2, MAGIC_ATTACK = 3,
		RANGE_ATTACK = 4, STAB_DEF = 5, SLASH_DEF = 6, CRUSH_DEF = 7, MAGIC_DEF = 8;
	public static final int RANGE_DEF = 9;
	private static final int STRENGTH_BONUS = 10, RANGE_STRENGTH = 11, MAGIC_DAMAGE = 12;

	private static final int STANCE_BONUS = 0; // assume they are not in controlled or defensive
	private static final double UNSUCCESSFUL_PRAY_DMG_MODIFIER = 0.6; // modifier for when you unsuccessfully hit off-pray

	// Offensive pray: assume you have valid. Piety for melee, Rigour for range, Augury for mage
	private static final double PIETY_ATK_PRAYER_MODIFIER = 1.2;
	private static final double PIETY_STR_PRAYER_MODIFIER = 1.23;
	private static final double AUGURY_OFFENSIVE_PRAYER_MODIFIER = 1.25;
	private static final double RIGOUR_OFFENSIVE_PRAYER_DMG_MODIFIER = 1.23;
	private static final double RIGOUR_OFFENSIVE_PRAYER_ATTACK_MODIFIER = 1.2;

	// Defensive pray: Assume you have one of the defensive prays active, but don't assume you have augury
	// while getting maged, since you would likely be planning to range or melee & using rigour/piety instead.
	private static final double PIETY_DEF_PRAYER_MODIFIER = 1.25;
	private static final double AUGURY_DEF_PRAYER_MODIFIER = 1.25;
	private static final double AUGURY_MAGEDEF_PRAYER_MODIFIER = 1.25; // assume we never use augury during defence for now (unless merging stats).
	private static final double RIGOUR_DEF_PRAYER_MODIFIER = 1.25;

	private static final double BALLISTA_SPEC_ACCURACY_MODIFIER = 1.25;
	private static final double BALLISTA_SPEC_DMG_MODIFIER = 1.25;

	private static final int ACB_SPEC_ACCURACY_MODIFIER = 2;

	private static final int DBOW_DMG_MODIFIER = 2;
	private static final int DBOW_SPEC_DMG_MODIFIER = 3;
	private static final int DBOW_SPEC_MIN_HIT = 16;
	private static final double DRAGON_CBOW_SPEC_DMG_MODIFIER = 1.2;

	private static final double DDS_SPEC_ACCURACY_MODIFIER = 1.25;
	private static final double DDS_SPEC_DMG_MODIFIER = 2.3;

	private static final int ARMA_GS_SPEC_ACCURACY_MODIFIER = 2;
	private static final double ARMA_GS_SPEC_DMG_MODIFIER = 1.375;
	private static final int ANCIENT_GS_SPEC_ACCURACY_MODIFIER = 2;
	private static final double FANG_SPEC_ACCURACY_MODIFIER = 1.5;
	private static final double ANCIENT_GS_SPEC_DMG_MODIFIER = 1.1;
	private static final int ANCIENT_GS_FIXED_DAMAGE = 25;

	private static final double VLS_SPEC_DMG_MODIFIER = 1.2;
	private static final double VLS_SPEC_MIN_DMG_MODIFIER = .2;
	private static final double VLS_SPEC_DEFENCE_SCALE = .25;
	private static final double SWH_SPEC_DMG_MODIFIER = 1.25;
	private static final double SWH_SPEC_MIN_DMG_MODIFIER = .25;
	private static final double DWH_SPEC_DMG_MODIFIER = 1.5;
	private static final double VOIDWAKER_SPEC_DMG_MODIFIER = 1.5;
	private static final double VOIDWAKER_SPEC_MIN_DMG_MODIFIER = .5;
	private static final double VOIDWAKER_FIXED_ACCURACY = 1;

	private static final double ABYSSAL_DAGGER_SPEC_ACCURACY_MODIFIER = 1.25;
	private static final double ABYSSAL_DAGGER_SPEC_DMG_MODIFIER = 0.85;

	// 0.975x is a simplified average brimstone mage def formula, where x = opponent's mage def
	// 25% of attacks ignore 10% of mage def, therefore 25% of attacks are 90% mage def and 75% are the usual 100%.
	// original formula: 0.25(0.9x) + 0.75x ==> 0.975x
	public static final double BRIMSTONE_RING_OPPONENT_DEF_MODIFIER = 0.975;
	public static final double SMOKE_BATTLESTAFF_DMG_ACC_MODIFIER = 1.1; // both dmg & accuracy modifier
	public static final double TOME_OF_FIRE_DMG_MODIFIER = 1.5;
	public static final double VOLATILE_NIGHTMARE_STAFF_ACC_MODIFIER = 0.5;


	@Getter
	private double averageHit = 0;
	@Getter
	private double accuracy = 0;
	@Getter
	private int minHit = 0;
	@Getter
	private int maxHit = 0;

	private CombatLevels attackerLevels;
	private CombatLevels defenderLevels;

	private RingData ringUsed;
	boolean isLmsFight;

	public PvpDamageCalc(FightPerformance relatedFight)
	{
		isLmsFight = relatedFight.fightType.isLmsFight();
		this.attackerLevels = relatedFight.fightType.getCombatLevelsForType();
		this.defenderLevels = relatedFight.fightType.getCombatLevelsForType();

		this.ringUsed = isLmsFight ? RingData.BERSERKER_RING : CONFIG.ringChoice();
	}

	// main function used to update stats during an ongoing fight
	public void updateDamageStats(Player attacker, Player defender, boolean success, AnimationData animationData)
	{
		// shouldn't be possible, but just in case
		if (attacker == null || defender == null) { return; }

		averageHit = 0;
		accuracy = 0;
		minHit = 0;
		maxHit = 0;

		int[] attackerItems = attacker.getPlayerComposition().getEquipmentIds();
		int[] defenderItems = defender.getPlayerComposition().getEquipmentIds();

		EquipmentData weapon = EquipmentData.fromId(fixItemId(attackerItems[KitType.WEAPON.getIndex()]));

		int[] playerStats = this.calculateBonusesWithRing(attackerItems);
		int[] opponentStats = this.calculateBonusesWithRing(defenderItems);
		AnimationData.AttackStyle attackStyle = animationData.attackStyle; // basic style: stab/slash/crush/ranged/magic

		// Special attack used will be determined based on the currently used weapon, if its special attack has been implemented.
		// the animation just serves to tell if they actually did a special attack animation, since some animations
		// are used for multiple special attacks.
		boolean isSpecial = animationData.isSpecial;
		VoidStyle voidStyle = VoidStyle.getVoidStyleFor(attacker.getPlayerComposition().getEquipmentIds());

		if (attackStyle.isMelee() || animationData == AnimationData.MELEE_VOIDWAKER_SPEC)
		{
			getMeleeMaxHit(playerStats[STRENGTH_BONUS], isSpecial, weapon, voidStyle, true);
			getMeleeAccuracy(playerStats, opponentStats, attackStyle, isSpecial, weapon, voidStyle, true);
		}
		else if (attackStyle == AttackStyle.RANGED)
		{
			getRangedMaxHit(playerStats[RANGE_STRENGTH], isSpecial, weapon, voidStyle, true, attackerItems);
			getRangeAccuracy(playerStats[RANGE_ATTACK], opponentStats[RANGE_DEF], isSpecial, weapon, voidStyle, true, attackerItems);
		}
		// this should always be true at this point, but just in case. unknown animation styles won't
		// make it here, they should be stopped in FightPerformance::checkForAttackAnimations
		else if (attackStyle == AttackStyle.MAGIC)
		{
			EquipmentData shield = EquipmentData.fromId(fixItemId(attackerItems[KitType.SHIELD.getIndex()]));
			getMagicMaxHit(shield, playerStats[MAGIC_DAMAGE], animationData, weapon, voidStyle, true);
			getMagicAccuracy(playerStats[MAGIC_ATTACK], opponentStats[MAGIC_DEF], weapon, animationData, voidStyle, true, false);
		}

		getAverageHit(success, weapon, isSpecial);

		maxHit = (int)(maxHit * (success ? 1 : UNSUCCESSFUL_PRAY_DMG_MODIFIER));
		minHit = (int)(minHit * (success ? 1 : UNSUCCESSFUL_PRAY_DMG_MODIFIER));

		log.debug("attackStyle: " + attackStyle.toString() + ", avgHit: " + nf.format(averageHit) + ", acc: " + nf.format(accuracy) +
			"\nattacker(" + attacker.getName() + ")stats: " + Arrays.toString(playerStats) +
			"\ndefender(" +  defender.getName() + ")stats: " + Arrays.toString(opponentStats));
	}

	// secondary function used to analyze fights from the fight log (fight analysis/fight merge)
	public void updateDamageStats(FightLogEntry atkLog, FightLogEntry defenderLog)
	{
		this.attackerLevels = atkLog.getAttackerLevels();
		this.defenderLevels = defenderLog.getAttackerLevels();
		int[] attackerItems = atkLog.getAttackerGear();
		int[] defenderItems = atkLog.getDefenderGear();
		boolean success = atkLog.success();
		AnimationData animationData = atkLog.getAnimationData();
		boolean successfulOffensive = atkLog.getAnimationData().attackStyle.isUsingSuccessfulOffensivePray(atkLog.getAttackerOffensivePray());

		averageHit = 0;
		accuracy = 0;
		minHit = 0;
		maxHit = 0;

		EquipmentData weapon = EquipmentData.fromId(fixItemId(attackerItems[KitType.WEAPON.getIndex()]));

		int[] playerStats = this.calculateBonuses(attackerItems);
		int[] opponentStats = this.calculateBonuses(defenderItems);
		AnimationData.AttackStyle attackStyle = animationData.attackStyle; // basic style: stab/slash/crush/ranged/magic

		// Special attack used will be determined based on the currently used weapon, if its special attack has been implemented.
		// the animation just serves to tell if they actually did a special attack animation, since some animations
		// are used for multiple special attacks.
		boolean isSpecial = animationData.isSpecial;
		VoidStyle voidStyle = VoidStyle.getVoidStyleFor(attackerItems);

		if (attackStyle.isMelee())
		{
			getMeleeMaxHit(playerStats[STRENGTH_BONUS], isSpecial, weapon, voidStyle, successfulOffensive);
			getMeleeAccuracy(playerStats, opponentStats, attackStyle, isSpecial, weapon, voidStyle, successfulOffensive);
		}
		else if (attackStyle == AttackStyle.RANGED)
		{
			getRangedMaxHit(playerStats[RANGE_STRENGTH], isSpecial, weapon, voidStyle, successfulOffensive, attackerItems);
			getRangeAccuracy(playerStats[RANGE_ATTACK], opponentStats[RANGE_DEF], isSpecial, weapon, voidStyle, successfulOffensive, attackerItems);
		}
		// this should always be true at this point, but just in case. unknown animation styles won't
		// make it here, they should be stopped in FightPerformance::checkForAttackAnimations
		else if (attackStyle == AttackStyle.MAGIC)
		{
			EquipmentData shield = EquipmentData.fromId(fixItemId(attackerItems[KitType.SHIELD.getIndex()]));
			getMagicMaxHit(shield, playerStats[MAGIC_DAMAGE], animationData, weapon, voidStyle, successfulOffensive);
			getMagicAccuracy(playerStats[MAGIC_ATTACK], opponentStats[MAGIC_DEF], weapon, animationData, voidStyle, successfulOffensive, defenderLog.getAttackerOffensivePray() == SpriteID.PRAYER_AUGURY);
		}

		getAverageHit(success, weapon, isSpecial);

		maxHit = (int)(maxHit * (success ? 1 : UNSUCCESSFUL_PRAY_DMG_MODIFIER));
		minHit = (int)(minHit * (success ? 1 : UNSUCCESSFUL_PRAY_DMG_MODIFIER));
	}

	private void getAverageHit(boolean success, EquipmentData weapon, boolean usingSpec)
	{
		boolean ancientGs = weapon == EquipmentData.ANCIENT_GODSWORD;
		boolean dbow = weapon == EquipmentData.DARK_BOW;
		boolean claws = weapon == EquipmentData.DRAGON_CLAWS;
		boolean fang = weapon == EquipmentData.OSMUMTENS_FANG;
		boolean vls = weapon == EquipmentData.VESTAS_LONGSWORD;
		boolean swh = weapon == EquipmentData.STATIUS_WARHAMMER;
		boolean voidwaker = weapon == EquipmentData.VOIDWAKER;
		boolean burningClaws = weapon == EquipmentData.BURNING_CLAWS;

		double prayerModifier = success ? 1 : UNSUCCESSFUL_PRAY_DMG_MODIFIER;
		double averageSuccessfulHit;
		// average hit calculation for attacks that have minimum hits that are skewed towards hitting minimum hit more often
		if (usingSpec && (dbow || vls || swh))
		{
			double accuracyAdjuster = dbow ? accuracy : 1;
			minHit = dbow ? DBOW_SPEC_MIN_HIT : 0;
			minHit = vls ? (int) (maxHit * VLS_SPEC_MIN_DMG_MODIFIER) : minHit;
			minHit = swh ? (int) (maxHit * SWH_SPEC_MIN_DMG_MODIFIER) : minHit;

			int total = 0;

			// this odd logic is used to calculate avg hit because when there is a minimum hit,
			// it does not simply change the potential hit range as you would expect:
			// potential hit rolls (min=0 max=5): 0, 1, 2, 3, 4, 5
			// potential hit rolls (min=3 max=5): 3, 3, 3, 3, 4, 5 (intuitively it would just be 3, 4, 5, but nope)
			// so, it is more common to roll the minimum hit and that has to be accounted for in the average hit.
			for (int i = 0; i <= maxHit; i++)
			{
				total += i < minHit ? minHit / accuracyAdjuster : i;
			}

			averageSuccessfulHit = (double) total / maxHit;
		}
		else if (usingSpec && claws)
		{
			// if first 1-2 claws miss, it's a 150% dmg multiplier because when the 3rd att hits, the last
			// 2 hits are 75% dmg multiplier, so 75% + 75% = 150%. It's a matter of a 2x multiplier or a
			// 1.5x multiplier and the chance of a 2x multiplier is what higherModifierChance is for

			// inverted accuracy is used to calculate the chances of missing specifically 1, 2 or 3 times in a row
			double invertedAccuracy = 1 - accuracy;
			double averageSuccessfulRegularHit = maxHit / 2;
			double higherModifierChance = (accuracy + (accuracy * invertedAccuracy));
			double lowerModifierChance = ((accuracy * Math.pow(invertedAccuracy, 2)) + (accuracy * Math.pow(invertedAccuracy, 3)));
			double averageSpecialHit = ((higherModifierChance * 2) + (lowerModifierChance * 1.5)) * averageSuccessfulRegularHit;

			averageHit = averageSpecialHit * prayerModifier;
			accuracy = higherModifierChance + lowerModifierChance;
			// the random +1 is not included in avg hit but it is included in the max hit to be seen from fight logs
			maxHit = maxHit * 2 + 1;
			return;
		}
		else if (burningClaws && usingSpec)
		{
			double baseMaxHit = maxHit;
			double acc = accuracy;
			double miss = 1 - acc;

			// Case 1: 1st roll success
			int minD1 = (int)Math.floor(0.75 * baseMaxHit);
			int maxD1 = (int)Math.floor(1.75 * baseMaxHit);
			double avgTotalDmg1 = getAverageBurningClawDamage(minD1, maxD1);

			// Case 2: 1st fail, 2nd success
			int minD2 = (int)Math.floor(0.50 * baseMaxHit);
			int maxD2 = (int)Math.floor(1.50 * baseMaxHit);
			double avgTotalDmg2 = getAverageBurningClawDamage(minD2, maxD2);

			// Case 3: 1st, 2nd fail, 3rd success
			int minD3 = (int)Math.floor(0.25 * baseMaxHit);
			int maxD3 = (int)Math.floor(1.25 * baseMaxHit);
			double avgTotalDmg3 = getAverageBurningClawDamage(minD3, maxD3);

			// Case 4: all 3 fail
			int minD4 = 0;
			int maxD4 = (int)Math.floor(baseMaxHit);
			double avgTotalDmg4 = getAverageBurningClawDamage(minD4, maxD4);

			double expectedDamage =
				(acc) * avgTotalDmg1 +
					(miss * acc) * avgTotalDmg2 +
					(miss * miss * acc) * avgTotalDmg3 +
					(miss * miss * miss) * avgTotalDmg4;

			this.averageHit = expectedDamage * prayerModifier;

			// Keep the original per-swing accuracy; do not override it here.
			this.maxHit = calculateBurningClawTotalDamage(maxD1);
			this.minHit = 0;

			return;
		}
		else if (fang)
		{
			double maxHitMultiplier = usingSpec ? 1: 0.85; // max hit when using spec is 100% but minHit stays the same
			// accuracy rolls twice for the fang, so the accuracy is equal to 1 - chance of hit1 OR hit2
			double invertedAccuracy = 1 - accuracy; // example: if accuracy is 20% and thus 0.2, inverted accuracy is 0.8
			double chanceOfMissingTwice = Math.pow(invertedAccuracy, 2); // 0.8 squared is 0.64 or 64%
			accuracy = 1 - chanceOfMissingTwice; // thus 64% chance of missing, or 36% accuracy
			// max hit is 0.85% and min hit is 15%
			// unlike VLS/SWH/Dbow I believe this rolls between min and max instead of raising hits between 0 - minHit to minHit
			int base = maxHit;
			minHit = (int) Math.ceil(base * 0.15);
			maxHit = (int) Math.ceil(maxHitMultiplier * base); // Use ceil for the upper bound of fang's passive
			averageSuccessfulHit = (minHit + maxHit) / 2.0;
		}
		// average hit calculation for attacks with minimum hits that use a more 'intuitive' average hit, similar to osmuten's fang
		else if (usingSpec && voidwaker)
		{
			// back-calc base hit from floored spec-max, then apply 50% for correct rounding
			minHit = (int) Math.floor((maxHit / VOIDWAKER_SPEC_DMG_MODIFIER) * VOIDWAKER_SPEC_MIN_DMG_MODIFIER);
			averageSuccessfulHit = (minHit + maxHit) / 2.0;
		}
		else
		{
			// divide by double to get accurate decimals, since this is the averageHit result,
			// not a core OSRS damage calc that is meant to be rounded down by int
			// If reaching this part of the code, the minHit should always be 0, but include it anyways
			// just in case.
			averageSuccessfulHit = (minHit + maxHit) / 2.0;

			// Abyssal Dagger Spec: Hits twice, adjust average and double displayed max hit.
			boolean abyssalDagger = weapon == EquipmentData.ABYSSAL_DAGGER;
			if (abyssalDagger && usingSpec)
			{
				averageSuccessfulHit *= 2; // Account for two hits in average
				maxHit *= 2; // Double the max hit for display
			}

			if (minHit > 0)
			{
				log.info("PvpDamageCalc:getAverageHit: Fell into default avg hit calculation with a minHit > 0 (" +
					minHit + "). Shouldn't happen. Weapon: " + weapon.toString());
			}
		}

		averageHit = accuracy * averageSuccessfulHit * prayerModifier;

		if (usingSpec && ancientGs)
		{
			averageHit += ANCIENT_GS_FIXED_DAMAGE;
		}
	}

	private int calculateBurningClawTotalDamage(int D)
	{
		return (int)Math.floor(0.25 * D) + (int)Math.floor(0.25 * D) + (int)Math.floor(0.5 * D);
	}

	private double getAverageBurningClawDamage(int minD, int maxD)
	{
		if (minD > maxD)
		{
			return 0;
		}
		double totalDamageSum = 0;
		for (int D = minD; D <= maxD; D++)
		{
			totalDamageSum += calculateBurningClawTotalDamage(D);
		}
		return totalDamageSum / (maxD - minD + 1);
	}

	private void getMeleeMaxHit(int meleeStrength, boolean usingSpec, EquipmentData weapon, VoidStyle voidStyle, boolean successfulOffensive)
	{
		boolean ags = weapon == EquipmentData.ARMADYL_GODSWORD;
		boolean ancientGs = weapon == EquipmentData.ANCIENT_GODSWORD;
		boolean dds = weapon == EquipmentData.DRAGON_DAGGER;
		boolean vls = weapon == EquipmentData.VESTAS_LONGSWORD;
		boolean swh = weapon == EquipmentData.STATIUS_WARHAMMER;
		boolean dwh = weapon == EquipmentData.DRAGON_WARHAMMER;
		boolean voidwaker = weapon == EquipmentData.VOIDWAKER;
		boolean abyssalDagger = weapon == EquipmentData.ABYSSAL_DAGGER;

		int effectiveLevel = (int) Math.floor((attackerLevels.str * (successfulOffensive ? PIETY_STR_PRAYER_MODIFIER : 1)) + 8 + 3);
		// apply void bonus if applicable
		if (voidStyle == VoidStyle.VOID_ELITE_MELEE || voidStyle == VoidStyle.VOID_MELEE)
		{
			effectiveLevel *= voidStyle.dmgModifier;
		}

		int baseDamage = (int) Math.floor(0.5 + effectiveLevel * (meleeStrength + 64) / 640.0);
		double damageModifier = (ags && usingSpec) ? ARMA_GS_SPEC_DMG_MODIFIER :
			(ancientGs && usingSpec) ? ANCIENT_GS_SPEC_DMG_MODIFIER :
			(swh && usingSpec) ? SWH_SPEC_DMG_MODIFIER :
			(dds && usingSpec) ? DDS_SPEC_DMG_MODIFIER :
			(vls && usingSpec) ? VLS_SPEC_DMG_MODIFIER :
			(dwh && usingSpec) ? DWH_SPEC_DMG_MODIFIER :
			(voidwaker && usingSpec) ? VOIDWAKER_SPEC_DMG_MODIFIER :
			(abyssalDagger && usingSpec) ? ABYSSAL_DAGGER_SPEC_DMG_MODIFIER :
			1;
		maxHit = (int) (damageModifier * baseDamage);
	}

	private void getRangedMaxHit(int rangeStrength, boolean usingSpec, EquipmentData weapon, VoidStyle voidStyle, boolean successfulOffensive, int[] attackerComposition)
	{
		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(weapon);
		EquipmentData head = EquipmentData.fromId(fixItemId(attackerComposition[KitType.HEAD.getIndex()]));
		EquipmentData body = EquipmentData.fromId(fixItemId(attackerComposition[KitType.TORSO.getIndex()]));
		EquipmentData legs = EquipmentData.fromId(fixItemId(attackerComposition[KitType.LEGS.getIndex()]));

		// if it's an LMS fight and bolts are used, force diamond bolts (e) or opal dragon bolts (e) based on weapon used.
		if (this.isLmsFight)
		{
			weaponAmmo = weaponAmmo instanceof RangeAmmoData.StrongBoltAmmo ? RangeAmmoData.StrongBoltAmmo.OPAL_DRAGON_BOLTS_E :
				weaponAmmo instanceof RangeAmmoData.BoltAmmo ? RangeAmmoData.BoltAmmo.DIAMOND_BOLTS_E : weaponAmmo;
		}

		boolean ballista = weapon == EquipmentData.HEAVY_BALLISTA;
		boolean dbow = weapon == EquipmentData.DARK_BOW;
		boolean dragonCbow = weapon == EquipmentData.DRAGON_CROSSBOW;

		int ammoStrength = weaponAmmo == null ? 0 : weaponAmmo.getRangeStr();

		rangeStrength += ammoStrength;

		double effectiveLevel = Math.floor((attackerLevels.range * (successfulOffensive ? RIGOUR_OFFENSIVE_PRAYER_DMG_MODIFIER : 1)) + 8);
		// apply void bonus if applicable
		if (voidStyle == VoidStyle.VOID_ELITE_RANGE || voidStyle == VoidStyle.VOID_RANGE)
		{
			effectiveLevel *= voidStyle.dmgModifier;
		}

		int baseDamage = (int) Math.floor(0.5 + (effectiveLevel * (rangeStrength + 64) / 640.0));

		double modifier = weaponAmmo == null ? 1 : weaponAmmo.getDmgModifier();
		modifier = ballista && usingSpec ? BALLISTA_SPEC_DMG_MODIFIER : modifier;
		modifier = dbow && !usingSpec ? DBOW_DMG_MODIFIER : modifier;
		modifier = dbow && usingSpec ? DBOW_SPEC_DMG_MODIFIER : modifier;
		modifier = dragonCbow && usingSpec ? DRAGON_CBOW_SPEC_DMG_MODIFIER : modifier;

		// Eclipse Atlatl uses Melee Str for max hit but Ranged prayers/void
		if (weapon == EquipmentData.ECLIPSE_ATLATL)
		{
			int[] playerStats = calculateBonusesWithRing(attackerComposition);
			// Recalculate effective level using Strength level but Ranged prayer modifier
			effectiveLevel = Math.floor(((attackerLevels.str * (successfulOffensive ? RIGOUR_OFFENSIVE_PRAYER_DMG_MODIFIER : 1)) + STANCE_BONUS) + 8);

			// Apply Ranged void bonus if applicable
			if (voidStyle == VoidStyle.VOID_ELITE_RANGE || voidStyle == VoidStyle.VOID_RANGE)
			{
				effectiveLevel *= voidStyle.dmgModifier;
			}

			baseDamage = (int) Math.floor(0.5 + (effectiveLevel * (playerStats[STRENGTH_BONUS] + 64) / 640.0));
			maxHit = baseDamage;
		}
		else // Standard Ranged Max Hit Calc
		{
		maxHit = weaponAmmo == null ?
			(int) (modifier * baseDamage) :
			(int) ((modifier * baseDamage) + weaponAmmo.getBonusMaxHit(attackerLevels.range));
		}

		// apply crystal armor bonus if using bow
		if ((weapon == EquipmentData.BOW_OF_FAERDHINEN || weapon == EquipmentData.CRYSTAL_BOW || weapon == EquipmentData.CRYSTAL_BOW_I) &&
			(head == EquipmentData.CRYSTAL_HELM || body == EquipmentData.CRYSTAL_BODY || legs == EquipmentData.CRYSTAL_LEGS))
		{
			double dmgModifier = 1 +
				(head == EquipmentData.CRYSTAL_HELM ? 0.025 : 0) +
				(body == EquipmentData.CRYSTAL_BODY ? 0.075 : 0) +
				(legs == EquipmentData.CRYSTAL_LEGS ? 0.05 : 0);

			maxHit *= dmgModifier;
		}
	}

	private void getMagicMaxHit(EquipmentData shield, int mageDamageBonus, AnimationData animationData, EquipmentData weapon, VoidStyle voidStyle, boolean successfulOffensive)
	{
		boolean smokeBstaff = weapon == EquipmentData.SMOKE_BATTLESTAFF;
		boolean tome = shield == EquipmentData.TOME_OF_FIRE;

		double magicBonus = 1 + (mageDamageBonus / 100.0);
		// provide dmg buff from smoke battlestaff if applicable
		if (smokeBstaff && AnimationData.isStandardSpellbookSpell(animationData))
		{
			magicBonus *= SMOKE_BATTLESTAFF_DMG_ACC_MODIFIER;
		}
		// provide dmg buff from tome of fire if applicable
		if (tome && AnimationData.isFireSpell(animationData))
		{
			magicBonus *= TOME_OF_FIRE_DMG_MODIFIER;
		}
		// apply void bonus if applicable
		if (voidStyle == VoidStyle.VOID_ELITE_MAGE || voidStyle == VoidStyle.VOID_MAGE)
		{
			magicBonus *= voidStyle.dmgModifier;
		}

		maxHit = (int)(animationData.baseSpellDamage * magicBonus);
	}

	private void getMeleeAccuracy(int[] playerStats, int[] opponentStats, AttackStyle attackStyle, boolean usingSpec, EquipmentData weapon, VoidStyle voidStyle, boolean successfulOffensive)
	{
		boolean vls = weapon == EquipmentData.VESTAS_LONGSWORD;
		boolean ags = weapon == EquipmentData.ARMADYL_GODSWORD;
		boolean ancientGs = weapon == EquipmentData.ANCIENT_GODSWORD;
		boolean dds = weapon == EquipmentData.DRAGON_DAGGER;
		boolean fang = weapon == EquipmentData.OSMUMTENS_FANG;
		boolean voidwaker = weapon == EquipmentData.VOIDWAKER;
		boolean abyssalDagger = weapon == EquipmentData.ABYSSAL_DAGGER;

		if (voidwaker && usingSpec)
		{
			accuracy = VOIDWAKER_FIXED_ACCURACY;
			return;
		}

		double stabBonusPlayer = playerStats[STAB_ATTACK];
		double slashBonusPlayer = playerStats[SLASH_ATTACK];
		double crushBonusPlayer = playerStats[CRUSH_ATTACK];

		double stabBonusTarget = opponentStats[STAB_DEF];
		double slashBonusTarget = opponentStats[SLASH_DEF];
		double crushBonusTarget = opponentStats[CRUSH_DEF];
		double magicBonusTarget = opponentStats[MAGIC_DEF];

		double effectiveLevelPlayer;
		double effectiveLevelTarget;

		double baseChance;
		double attackerChance;
		double defenderChance;

		double accuracyModifier = dds ? DDS_SPEC_ACCURACY_MODIFIER :
			ags ? ARMA_GS_SPEC_ACCURACY_MODIFIER :
			ancientGs ? ANCIENT_GS_SPEC_ACCURACY_MODIFIER :
			fang ? FANG_SPEC_ACCURACY_MODIFIER :
			1;

		/**
		 * Attacker Chance
		 */
		effectiveLevelPlayer = Math.floor(((attackerLevels.atk * (successfulOffensive ? PIETY_ATK_PRAYER_MODIFIER : 1)) + STANCE_BONUS) + 8);
		// apply void bonus if applicable
		if (voidStyle == VoidStyle.VOID_ELITE_MELEE || voidStyle == VoidStyle.VOID_MELEE)
		{
			effectiveLevelPlayer *= voidStyle.accuracyModifier;
		}

		final double attackBonus = attackStyle == AttackStyle.STAB ? stabBonusPlayer
			: attackStyle == AttackStyle.SLASH ? slashBonusPlayer : crushBonusPlayer;

		final double targetDefenceBonus = attackStyle == AttackStyle.STAB ? stabBonusTarget
			: attackStyle == AttackStyle.SLASH ? slashBonusTarget : crushBonusTarget;


		baseChance = Math.floor(effectiveLevelPlayer * (attackBonus + 64));
		if (usingSpec)
		{
			// Don't apply the generic modifier if it's the Abyssal Dagger (handled separately below)
			if (weapon != EquipmentData.ABYSSAL_DAGGER) {
			baseChance = baseChance * accuracyModifier;
			}
		}

		// Apply Abyssal Dagger spec modifier specifically here
		if (abyssalDagger && usingSpec) {
			baseChance *= ABYSSAL_DAGGER_SPEC_ACCURACY_MODIFIER;
		}

		attackerChance = baseChance;

		/**
		 * Defender Chance
		 */
		effectiveLevelTarget = Math.floor(((defenderLevels.def * PIETY_DEF_PRAYER_MODIFIER) + STANCE_BONUS) + 8);

		if (vls && usingSpec)
		{
			defenderChance = Math.floor((effectiveLevelTarget * (stabBonusTarget + 64)) * VLS_SPEC_DEFENCE_SCALE);
		}
		else
		{
			defenderChance = Math.floor(effectiveLevelTarget * (targetDefenceBonus + 64));
		}
//        log.debug("MELEE ATTACK: " + defenderChance );
		/**
		 * Calculate Accuracy
		 */
		if (attackerChance > defenderChance)
		{
			accuracy = 1 - (defenderChance + 2) / (2 * (attackerChance + 1));
		}
		else
		{
			accuracy = attackerChance / (2 * (defenderChance + 1));
		}
	}

	private void getRangeAccuracy(int playerRangeAtt, int opponentRangeDef, boolean usingSpec, EquipmentData weapon, VoidStyle voidStyle, boolean successfulOffensive, int[] attackerComposition)
	{
		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(weapon);
		// if it's an LMS fight and bolts are used, don't use config bolt, just use diamond bolts(e)
		if (this.isLmsFight && (weaponAmmo instanceof RangeAmmoData.BoltAmmo ||
			weaponAmmo instanceof RangeAmmoData.StrongBoltAmmo))
		{
			weaponAmmo = RangeAmmoData.BoltAmmo.DIAMOND_BOLTS_E;
		}

		boolean diamonds = ArrayUtils.contains(RangeAmmoData.DIAMOND_BOLTS, weaponAmmo);
		boolean opals = ArrayUtils.contains(RangeAmmoData.OPAL_BOLTS, weaponAmmo);
		double effectiveLevelPlayer;
		double effectiveLevelTarget;
		double rangeModifier;
		double attackerChance;
		double defenderChance;

		/**
		 * Attacker Chance
		 */
		effectiveLevelPlayer = Math.floor(((attackerLevels.range * (successfulOffensive ? RIGOUR_OFFENSIVE_PRAYER_ATTACK_MODIFIER : 1)) + STANCE_BONUS) + 8);
		// apply void bonus if applicable
		if (voidStyle == VoidStyle.VOID_ELITE_RANGE || voidStyle == VoidStyle.VOID_RANGE)
		{
			effectiveLevelPlayer *= voidStyle.accuracyModifier;
		}

		// apply crystal armor bonus if using bow
		EquipmentData head = EquipmentData.fromId(fixItemId(attackerComposition[KitType.HEAD.getIndex()]));
		EquipmentData body = EquipmentData.fromId(fixItemId(attackerComposition[KitType.TORSO.getIndex()]));
		EquipmentData legs = EquipmentData.fromId(fixItemId(attackerComposition[KitType.LEGS.getIndex()]));

		if ((weapon == EquipmentData.BOW_OF_FAERDHINEN || weapon == EquipmentData.CRYSTAL_BOW || weapon == EquipmentData.CRYSTAL_BOW_I) &&
			(head == EquipmentData.CRYSTAL_HELM || body == EquipmentData.CRYSTAL_BODY || legs == EquipmentData.CRYSTAL_LEGS))
		{
			double accuracyModifier = 1 +
				(head == EquipmentData.CRYSTAL_HELM ? 0.05 : 0) +
				(body == EquipmentData.CRYSTAL_BODY ? 0.15 : 0) +
				(legs == EquipmentData.CRYSTAL_LEGS ? 0.1 : 0);

			effectiveLevelPlayer *= accuracyModifier;
		}

		rangeModifier = Math.floor(effectiveLevelPlayer * ((double) playerRangeAtt + 64));
		if (usingSpec)
		{
			boolean acb = weapon == EquipmentData.ARMADYL_CROSSBOW;
			boolean ballista = weapon == EquipmentData.HEAVY_BALLISTA;

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
		effectiveLevelTarget = Math.floor(((defenderLevels.def * RIGOUR_DEF_PRAYER_MODIFIER) + STANCE_BONUS) + 8);
		defenderChance = Math.floor(effectiveLevelTarget * ((double) opponentRangeDef + 64));

		/**
		 * Calculate Accuracy
		 */
		if (attackerChance > defenderChance)
		{
			accuracy = 1 - (defenderChance + 2) / (2 * (attackerChance + 1));
		}
		else
		{
			accuracy = attackerChance / (2 * (defenderChance + 1));
		}

		// the effect in pvp is 5% instead of 10% like it is for pvm
		// upon further testing this effect applies to opal dragon bolts as well
		// diamond bolts and opal bolts accuracy: 5% of attacks are 100% accuracy, so apply avg accuracy as:
		// (95% of normal accuracy) + (5% of 100% accuracy)
		boolean dragonCbow = weapon == EquipmentData.DRAGON_CROSSBOW;
		// Apply diamond/opal accuracy passive, but skip it during dragon crossbow special so that Annihilate's
		// projectile-based accuracy is not unintentionally boosted.
		accuracy = (diamonds || opals) && !(dragonCbow && usingSpec) ? (accuracy * .95) + .05 : accuracy;
	}

	private void getMagicAccuracy(int playerMageAtt, int opponentMageDef, EquipmentData weapon, AnimationData animationData, VoidStyle voidStyle, boolean successfulOffensive, boolean defensiveAugurySuccess)
	{
		double effectiveLevelPlayer;

		double reducedDefenceLevelTarget;
		double effectiveMagicDefenceTarget;
		double effectiveMagicLevelTarget;

		double effectiveLevelTarget;

		double magicModifier;

		double attackerChance;
		double defenderChance;

		/**
		 * Attacker Chance
		 */
		effectiveLevelPlayer = Math.floor(((attackerLevels.mage * (successfulOffensive ? AUGURY_OFFENSIVE_PRAYER_MODIFIER : 1))) + 8);
		// apply void bonus if applicable
		if (voidStyle == VoidStyle.VOID_ELITE_MAGE || voidStyle == VoidStyle.VOID_MAGE)
		{
			effectiveLevelPlayer *= voidStyle.accuracyModifier;
		}

		magicModifier = Math.floor(effectiveLevelPlayer * ((double) playerMageAtt + 64));
		attackerChance = magicModifier;

		/**
		 * Defender Chance
		 */
		effectiveLevelTarget = Math.floor(((defenderLevels.def * AUGURY_DEF_PRAYER_MODIFIER) + STANCE_BONUS) + 8);
		effectiveMagicLevelTarget = Math.floor((defenderLevels.mage * (defensiveAugurySuccess ? AUGURY_MAGEDEF_PRAYER_MODIFIER : 1)) * 0.70);
		reducedDefenceLevelTarget = Math.floor(effectiveLevelTarget * 0.30);
		effectiveMagicDefenceTarget = effectiveMagicLevelTarget + reducedDefenceLevelTarget;

		// 0.975x is a simplified brimstone accuracy formula, where x = mage def
		defenderChance = ringUsed == RingData.BRIMSTONE_RING ?
			Math.floor(effectiveMagicDefenceTarget * ((BRIMSTONE_RING_OPPONENT_DEF_MODIFIER * opponentMageDef) + 64)) :
			Math.floor(effectiveMagicDefenceTarget * ((double) opponentMageDef + 64));

		/**
		 * Calculate Accuracy
		 */
		if (attackerChance > defenderChance)
		{
			accuracy = 1 - (defenderChance + 2) / (2 * (attackerChance + 1));
		}
		else
		{
			accuracy = attackerChance / (2 * (defenderChance + 1));
		}

		boolean smokeBstaff = weapon == EquipmentData.SMOKE_BATTLESTAFF;
		boolean volatileStaff = weapon == EquipmentData.VOLATILE_NIGHTMARE_STAFF;
		// provide accuracy buff from smoke battlestaff or volatile staff spec if applicable
		if (smokeBstaff && AnimationData.isStandardSpellbookSpell(animationData))
		{
			accuracy *= SMOKE_BATTLESTAFF_DMG_ACC_MODIFIER;
		}
		else if (volatileStaff && animationData == MAGIC_VOLATILE_NIGHTMARE_STAFF_SPEC)
		{
			accuracy *= VOLATILE_NIGHTMARE_STAFF_ACC_MODIFIER;
		}
	}

	// Retrieve item stats for a single item, returned as an int array so they can be modified.
	// First, try to get the item stats from the item manager. If stats weren't present in the
	// itemManager, try get the 'real' item id from the EquipmentData. If it's not defined in EquipmentData, it will return null
	// and count as 0 stats, but that should be very rare.
	public static int[] getItemStats(int itemId)
	{
		ItemStats itemStats = PLUGIN.getItemManager().getItemStats(itemId);
		if (itemStats == null)
		{
			EquipmentData itemData = EquipmentData.fromId(itemId);
			if (itemData != null)
			{
				itemId = itemData.getItemId();
				itemStats = PLUGIN.getItemManager().getItemStats(itemId);
			}
		}

		if (itemStats == null)
		{
			return null;
		}
		final ItemEquipmentStats equipmentStats = itemStats.getEquipment();
		if (equipmentStats == null)
		{
			return null;
		}
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
			(int)equipmentStats.getMdmg(),	// 12
		};
	}

	// this is used to calculate bonuses including the currently used ring, in case we're in LMS but the config ring
	// is different.
	private int[] calculateBonusesWithRing(int[] itemIds)
	{
		return calculateBonuses(itemIds, this.ringUsed);
	}

	public static int[] calculateBonuses(int[] itemIds)
	{
		return calculateBonuses(itemIds, CONFIG.ringChoice());
	}
	// Calculate total equipment bonuses for all given items
	public static int[] calculateBonuses(int[] itemIds, RingData ringUsed)
	{
		int[] equipmentBonuses = ringUsed == null || ringUsed == RingData.NONE ?
			new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } :
			getItemStats(ringUsed.getItemId());

		if (equipmentBonuses == null) // shouldn't happen, but as a failsafe if the ring lookup fails
		{
			equipmentBonuses = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		}

		for (int item : itemIds)
		{
			if (item > PlayerComposition.ITEM_OFFSET)
			{
				int[] bonuses = getItemStats(item - PlayerComposition.ITEM_OFFSET);

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

	public static ItemEquipmentStats calculateBonusesToStats(int[] itemIds)
	{
		int[] bonuses = calculateBonuses(itemIds);
		return ItemEquipmentStats.builder()
			.astab(bonuses[STAB_ATTACK])	// 0
			.aslash(bonuses[SLASH_ATTACK])	// 1
			.acrush(bonuses[CRUSH_ATTACK])	// 2
			.amagic(bonuses[MAGIC_ATTACK])	// 3
			.arange(bonuses[RANGE_ATTACK])	// 4
			.dstab(bonuses[STAB_DEF])		// 5
			.dslash(bonuses[SLASH_DEF])		// 6
			.dcrush(bonuses[CRUSH_DEF])		// 7
			.dmagic(bonuses[MAGIC_DEF])		// 8
			.drange(bonuses[RANGE_DEF])		// 9
			.str(bonuses[STRENGTH_BONUS])	// 10
			.rstr(bonuses[RANGE_STRENGTH]) 	// 11
			.mdmg(bonuses[MAGIC_DAMAGE])	// 12
			.build();
	}
}
