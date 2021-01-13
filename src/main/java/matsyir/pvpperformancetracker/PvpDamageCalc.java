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
package matsyir.pvpperformancetracker;

import lombok.Getter;
import static matsyir.pvpperformancetracker.AnimationData.AttackStyle;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.FightLogEntry.nf;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.api.kit.KitType;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;
import org.apache.commons.lang3.ArrayUtils;
import net.runelite.api.Player;

// Pvp damage calculations
// call updateDamageStats(...) with required parameters, and retrieve results by using the field getters
// for averageHit, accuracy, maxHit, and minHit.
//
// combat formulas source thread QFC: 317,318,461,66138854
@Slf4j
public class PvpDamageCalc
{
	private static final int WEAPON_SLOT = 3,
		STAB_ATTACK = 0, SLASH_ATTACK = 1, CRUSH_ATTACK = 2, MAGIC_ATTACK = 3, RANGE_ATTACK = 4,
		STAB_DEF = 5, SLASH_DEF = 6, CRUSH_DEF = 7, MAGIC_DEF = 8, RANGE_DEF = 9,
		STRENGTH_BONUS = 10, RANGE_STRENGTH = 11, MAGIC_DAMAGE = 12;

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
	private static final double AUGURY_MAGEDEF_PRAYER_MODIFIER = 1; // assume we never use augury during defence for now.
	private static final double RIGOUR_DEF_PRAYER_MODIFIER = 1.25;

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

	// 0.975x is a simplified average brimstone mage def formula, where x = opponent's mage def
	// 25% of attacks ignore 10% of mage def, therefore 25% of attacks are 90% mage def and 75% are the usual 100%.
	// original formula: 0.25(0.9x) + 0.75x ==> 0.975x
	public static final double BRIMSTONE_RING_OPPONENT_DEF_MODIFIER = 0.975;
	public static final double SMOKE_BATTLESTAFF_DMG_ACC_MODIFIER = 1.1; // both dmg & accuracy modifier
	public static final double TOME_OF_FIRE_DMG_MODIFIER = 1.5;


	@Getter
	private double averageHit = 0;
	@Getter
	private double accuracy = 0;
	@Getter
	private int minHit = 0;
	@Getter
	private int maxHit = 0;
	private Player attacker;

	public PvpDamageCalc()
	{

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

		this.attacker = attacker;

		AnimationData.AttackStyle attackStyle = animationData.attackStyle; // basic style: melee/ranged/magic

		int[] attackerItems = attacker.getPlayerComposition().getEquipmentIds();
		int[] defenderItems = defender.getPlayerComposition().getEquipmentIds();
		int[] playerStats = this.calculateBonuses(attackerItems);
		int[] opponentStats = this.calculateBonuses(defenderItems);

		// Special attack used will be determined based on the currently used weapon, if its special attack has been implemented.
		boolean isSpecial = animationData.isSpecial;

		int weaponId = attackerItems[WEAPON_SLOT] > 512 ? attackerItems[WEAPON_SLOT] - 512 : attackerItems[WEAPON_SLOT];
		EquipmentData weapon = EquipmentData.getEquipmentDataFor(weaponId);

		VoidStyle voidStyle = VoidStyle.getVoidStyleFor(attacker.getPlayerComposition().getEquipmentIds());

		if (attackStyle.isMelee())
		{
			getMeleeMaxHit(playerStats[STRENGTH_BONUS], isSpecial, weapon, voidStyle, true);
			getMeleeAccuracy(playerStats, opponentStats, attackStyle, isSpecial, weapon, voidStyle, true);
		}
		else if (attackStyle == AttackStyle.RANGED)
		{
			getRangedMaxHit(playerStats[RANGE_STRENGTH], isSpecial, weapon, voidStyle, true);
			getRangeAccuracy(playerStats[RANGE_ATTACK], opponentStats[RANGE_DEF], isSpecial, weapon, voidStyle, true);
		}
		// this should always be true at this point, but just in case. unknown animation styles won't
		// make it here, they should be stopped in FightPerformance::checkForAttackAnimations
		else if (attackStyle == AttackStyle.MAGIC)
		{
			getMagicMaxHit(attackerItems[KitType.SHIELD.getIndex()], playerStats[MAGIC_DAMAGE], animationData, weapon, voidStyle, true);
			getMagicAccuracy(playerStats[MAGIC_ATTACK], opponentStats[MAGIC_DEF], weapon, animationData, voidStyle, true);
		}

		getAverageHit(success, weapon, isSpecial);

		maxHit = (int)(maxHit * (success ? 1 : UNSUCCESSFUL_PRAY_DMG_MODIFIER));

		log.info("attackStyle: " + attackStyle.toString() + ", avgHit: " + nf.format(averageHit) + ", acc: " + nf.format(accuracy) +
			"\nattacker(" + attacker.getName() + ")stats: " + Arrays.toString(playerStats) +
			"\ndefender(" +  defender.getName() + ")stats: " + Arrays.toString(opponentStats));
	}

	private void getAverageHit(boolean success, EquipmentData weapon, boolean usingSpec)
	{
		boolean dbow = weapon == EquipmentData.DARK_BOW;
		boolean ags = weapon == EquipmentData.ARMADYL_GODSWORD;
		boolean claws = weapon == EquipmentData.DRAGON_CLAWS;
		boolean vls = weapon == EquipmentData.VESTAS_LONGSWORD || weapon == EquipmentData.BLIGHTED_VESTAS_LONGSWORD;;
		boolean swh = weapon == EquipmentData.STATIUS_WARHAMMER;

		double agsModifier = ags ? AGS_SPEC_FINAL_DMG_MODIFIER : 1;
		double prayerModifier = success ? 1 : UNSUCCESSFUL_PRAY_DMG_MODIFIER;
		double averageSuccessfulHit;
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
		else
		{
			averageSuccessfulHit = maxHit / 2.0;
		}

		averageHit = accuracy * averageSuccessfulHit * prayerModifier * agsModifier;
	}

	private void getMeleeMaxHit(int meleeStrength, boolean usingSpec, EquipmentData weapon, VoidStyle voidStyle, boolean successfulOffensive)
	{
		boolean ags = weapon == EquipmentData.ARMADYL_GODSWORD;
		boolean dds = ArrayUtils.contains(EquipmentData.DRAGON_DAGGERS, weapon);
		boolean vls = weapon == EquipmentData.VESTAS_LONGSWORD || weapon == EquipmentData.BLIGHTED_VESTAS_LONGSWORD;
		boolean swh = weapon == EquipmentData.STATIUS_WARHAMMER;

		int effectiveLevel = (int) Math.floor((CONFIG.strengthLevel() * (successfulOffensive ? PIETY_STR_PRAYER_MODIFIER : 1)) + 8 + 3);
		// apply void bonus if applicable
		if (voidStyle == VoidStyle.VOID_ELITE_MELEE || voidStyle == VoidStyle.VOID_MELEE)
		{
			effectiveLevel *= voidStyle.dmgModifier;
		}

		int baseDamage = (int) Math.floor(0.5 + effectiveLevel * (meleeStrength + 64) / 640);
		double modifier = ags && usingSpec ? AGS_SPEC_INITIAL_DMG_MODIFIER : 1;
		modifier = (swh && usingSpec) ? SWH_SPEC_DMG_MODIFIER : modifier;
		modifier = (dds && usingSpec) ? DDS_SPEC_DMG_MODIFIER : modifier;
		modifier = (vls && usingSpec) ? VLS_SPEC_DMG_MODIFIER : modifier;
		maxHit = (int) (modifier * baseDamage);
	}

	private void getRangedMaxHit(int rangeStrength, boolean usingSpec, EquipmentData weapon, VoidStyle voidStyle, boolean successfulOffensive)
	{
		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(weapon);
		boolean ballista = weapon == EquipmentData.HEAVY_BALLISTA;
		boolean dbow = weapon == EquipmentData.DARK_BOW;

		int ammoStrength = weaponAmmo == null ? 0 : weaponAmmo.getRangeStr();

		rangeStrength += ammoStrength;

		int effectiveLevel = (int) Math.floor((CONFIG.rangedLevel() * (successfulOffensive ? RIGOUR_OFFENSIVE_PRAYER_DMG_MODIFIER : 1)) + 8);
		// apply void bonus if applicable
		if (voidStyle == VoidStyle.VOID_ELITE_RANGE || voidStyle == VoidStyle.VOID_RANGE)
		{
			effectiveLevel *= voidStyle.dmgModifier;
		}
		int baseDamage = (int) Math.floor(0.5 + effectiveLevel * (rangeStrength + 64) / 640);

		double modifier = weaponAmmo == null ? 1 : weaponAmmo.getDmgModifier();
		modifier = ballista && usingSpec ? BALLISTA_SPEC_DMG_MODIFIER : modifier;
		modifier = dbow && !usingSpec ? DBOW_DMG_MODIFIER : modifier;
		modifier = dbow && usingSpec ? DBOW_SPEC_DMG_MODIFIER : modifier;
		maxHit = weaponAmmo == null ?
			(int) (modifier * baseDamage) :
			(int) ((modifier * baseDamage) + weaponAmmo.getBonusMaxHit());
	}

	private void getMagicMaxHit(int shieldItemId, int mageDamageBonus, AnimationData animationData, EquipmentData weapon, VoidStyle voidStyle, boolean successfulOffensive)
	{
		boolean smokeBstaff = weapon == EquipmentData.SMOKE_BATTLESTAFF;
		boolean tome = EquipmentData.getEquipmentDataFor(shieldItemId) == EquipmentData.TOME_OF_FIRE;

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
		boolean vls = weapon == EquipmentData.VESTAS_LONGSWORD || weapon == EquipmentData.BLIGHTED_VESTAS_LONGSWORD;;
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

		double accuracyModifier = dds ? DDS_SPEC_ACCURACY_MODIFIER : ags ? AGS_SPEC_ACCURACY_MODIFIER : 1;

		/**
		 * Attacker Chance
		 */
		effectiveLevelPlayer = Math.floor(((CONFIG.attackLevel() * (successfulOffensive ? PIETY_ATK_PRAYER_MODIFIER : 1)) + STANCE_BONUS) + 8);
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
			baseChance = baseChance * accuracyModifier;
		}

		attackerChance = baseChance;

		/**
		 * Defender Chance
		 */
		effectiveLevelTarget = Math.floor(((CONFIG.defenceLevel() * PIETY_DEF_PRAYER_MODIFIER) + STANCE_BONUS) + 8);

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

	private void getRangeAccuracy(int playerRangeAtt, int opponentRangeDef, boolean usingSpec, EquipmentData weapon, VoidStyle voidStyle, boolean successfulOffensive)
	{
		RangeAmmoData weaponAmmo = EquipmentData.getWeaponAmmo(weapon);
		boolean diamonds = ArrayUtils.contains(RangeAmmoData.DIAMOND_BOLTS, weaponAmmo);
		double effectiveLevelPlayer;
		double effectiveLevelTarget;
		double rangeModifier;
		double attackerChance;
		double defenderChance;

		/**
		 * Attacker Chance
		 */
		effectiveLevelPlayer = Math.floor(((CONFIG.rangedLevel() * (successfulOffensive ? RIGOUR_OFFENSIVE_PRAYER_ATTACK_MODIFIER : 1)) + STANCE_BONUS) + 8);
		// apply void bonus if applicable
		if (voidStyle == VoidStyle.VOID_ELITE_RANGE || voidStyle == VoidStyle.VOID_RANGE)
		{
			effectiveLevelPlayer *= voidStyle.accuracyModifier;
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
		effectiveLevelTarget = Math.floor(((CONFIG.defenceLevel() * RIGOUR_DEF_PRAYER_MODIFIER) + STANCE_BONUS) + 8);
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

		// diamond bolts accuracy: 10% of attacks are 100% accuracy, so apply avg accuracy as:
		// (90% of normal accuracy) + (10% of 100% accuracy)
		accuracy = diamonds ? (accuracy * .9) + .1 : accuracy;
	}

	private void getMagicAccuracy(int playerMageAtt, int opponentMageDef, EquipmentData weapon, AnimationData animationData, VoidStyle voidStyle, boolean successfulOffensive)
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
		effectiveLevelPlayer = Math.floor(((CONFIG.magicLevel() * (successfulOffensive ? AUGURY_OFFENSIVE_PRAYER_MODIFIER : 1))) + 8);
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
		effectiveLevelTarget = Math.floor(((CONFIG.defenceLevel() * AUGURY_DEF_PRAYER_MODIFIER) + STANCE_BONUS) + 8);
		effectiveMagicLevelTarget = Math.floor((CONFIG.magicLevel() * AUGURY_MAGEDEF_PRAYER_MODIFIER) * 0.70);
		reducedDefenceLevelTarget = Math.floor(effectiveLevelTarget * 0.30);
		effectiveMagicDefenceTarget = effectiveMagicLevelTarget + reducedDefenceLevelTarget;

		// 0.975x is a simplified brimstone accuracy formula, where x = mage def
		defenderChance = CONFIG.ringChoice() == RingData.BRIMSTONE_RING ?
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
		// provide accuracy buff from smoke battlestaff if applicable
		if (smokeBstaff && AnimationData.isStandardSpellbookSpell(animationData))
		{
			accuracy *= SMOKE_BATTLESTAFF_DMG_ACC_MODIFIER;
		}
	}

	// Retrieve item stats for a single item, returned as an int array so they can be modified.
	// First, try to get the item stats from the item manager. If stats weren't present in the
	// itemManager, try get the 'real' item id from the EquipmentData. If it's not defined in EquipmentData, it will return null
	// and count as 0 stats, but that should be very rare.
	public int[] getItemStats(int itemId)
	{
		ItemStats itemStats = PLUGIN.itemManager.getItemStats(itemId, false);
		if (itemStats == null)
		{
			EquipmentData itemData = EquipmentData.getEquipmentDataFor(itemId);
			if (itemData != null)
			{
				itemId = itemData.getItemId();
				itemStats = PLUGIN.itemManager.getItemStats(itemId, false);
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
		int[] equipmentBonuses = CONFIG.ringChoice() == RingData.NONE ?
			new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } :
			getItemStats(CONFIG.ringChoice().getItemId());

		if (equipmentBonuses == null) // shouldn't happen, but as a failsafe if the ring lookup fails
		{
			equipmentBonuses = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		}

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

	enum VoidStyle
	{
		VOID_MELEE(1.1, 1.1),
		VOID_RANGE(1.1, 1.1),
		VOID_MAGE(1.45, 1),
		VOID_ELITE_MELEE(1.1, 1.1),
		VOID_ELITE_RANGE(1.125, 1.125),
		VOID_ELITE_MAGE(1.45, 1.025),
		NONE(1, 1);

		double accuracyModifier;
		double dmgModifier;

		VoidStyle(double accuracyModifier, double dmgModifier)
		{
			this.accuracyModifier = accuracyModifier;
			this.dmgModifier = dmgModifier;
		}

		// return a void style for a given PlayerComposition
		public static VoidStyle getVoidStyleFor(int[] playerComposition)
		{
			if (playerComposition == null) { return NONE; }

			EquipmentData gloves = EquipmentData.getEquipmentDataFor(playerComposition[KitType.HANDS.getIndex()]);

			if (gloves != EquipmentData.VOID_GLOVES) { return NONE; }

			EquipmentData helm = EquipmentData.getEquipmentDataFor(playerComposition[KitType.HEAD.getIndex()]);
			EquipmentData torso = EquipmentData.getEquipmentDataFor(playerComposition[KitType.TORSO.getIndex()]);
			EquipmentData legs = EquipmentData.getEquipmentDataFor(playerComposition[KitType.LEGS.getIndex()]);

			if (torso == EquipmentData.VOID_BODY && legs == EquipmentData.VOID_LEGS)
			{
				return helm == EquipmentData.VOID_MAGE_HELM ? VOID_MAGE
					: helm == EquipmentData.VOID_RANGE_HELM ? VOID_RANGE
					: helm == EquipmentData.VOID_MELEE_HELM ? VOID_MELEE
					: NONE;
			}
			else if (torso == EquipmentData.VOID_ELITE_BODY && legs == EquipmentData.VOID_ELITE_LEGS)
			{
				return helm == EquipmentData.VOID_MAGE_HELM ? VOID_ELITE_MAGE
					: helm == EquipmentData.VOID_RANGE_HELM ? VOID_ELITE_RANGE
					: helm == EquipmentData.VOID_MELEE_HELM ? VOID_ELITE_MELEE
					: NONE;
			}

			return NONE;
		}
	}
}
