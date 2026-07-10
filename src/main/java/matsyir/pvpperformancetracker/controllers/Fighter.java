/*
 * Copyright (c)  2021, Matsyir <https://github.com/Matsyir>
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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.models.BrewState;
import static matsyir.pvpperformancetracker.utils.NumberFormatter.nf;
import static matsyir.pvpperformancetracker.utils.NumberFormatter.nf1;
import static matsyir.pvpperformancetracker.utils.PvpUtils.fixItemId;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.EquipmentData;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.GraphicID;
import net.runelite.api.IterableHashTable;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.kit.KitType;

@Slf4j
@Getter
public
class Fighter
{
	// Target graphics IDs indicating special attacks
	private static final int GFX_TARGET_DBOW_SPEC = 1100;   // dragon-arrow gfx on target
	private static final int GFX_TARGET_DCBOW_SPEC = 157;    // Annihilate AOE gfx on target

	@Setter
	private Player player;
	@Setter
	@Expose
	@SerializedName("n") // use 1 letter serialized variable names for more compact storage
	private String name; // username
	@Expose
	@SerializedName("a")
	private int attackCount; // total number of attacks
	@Expose
	@SerializedName("s")
	private int offPraySuccessCount; // total number of successful off-pray attacks
									 // (when you use a different combat style than your opponent's overhead)
	@Expose
	@SerializedName("d") // NOTE: previously referred to as "Deserved damage"
	private double expectedDamage; // total expected damage based on gear & opponent's pray
	@Expose
	@SerializedName("h") // h for "hitsplats", real hits
	private int damageDealt; // actual damage dealt based on opponent's hitsplats

	@Expose
	@SerializedName("z") // z because idk and want to keep 1 character for most compact storage
	private int totalMagicAttackCount; // total count of magic attacks
	@Expose
	@SerializedName("m")
	private int magicHitCount; // count of 'successful' magic hits (where you don't splash)
	@Expose
	@SerializedName("M")
	private double magicHitCountExpected; // cumulative magic accuracy percentage for each attack

	@Expose
	@SerializedName("p")
	private int offensivePraySuccessCount;

	@Expose
	@SerializedName("g")
	private int ghostBarrageCount;
	@Expose
	@SerializedName("y")
	private double ghostBarrageExpectedDamage;

	@Expose
	@SerializedName("H")
	private int hpHealed;

	@Expose
	@SerializedName("rh") // robe hits
	private int robeHits = 0;

	@Expose
	@SerializedName("x") // x for X_X
	private boolean dead; // will be true if the fighter died in the fight

	@Expose
	@SerializedName("l")
	private ArrayList<FightLogEntry> fightLogEntries;
	@Expose
	@SerializedName("b")
	private CombatLevels baseLevels;

	private PvpDamageCalc pvpDamageCalc;
	private int lastGhostBarrageCheckedTick = -1;
	@Setter
	private int lastGhostBarrageCheckedMageXp = -1;

	@Getter
	private transient Queue<FightLogEntry> pendingAttacks;

	// fighter that is bound to a player and gets updated during a fight
	Fighter(FightPerformance fight, Player player)
	{
		this.player = player;
		name = player.getName();
		attackCount = 0;
		offPraySuccessCount = 0;
		expectedDamage = 0;
		damageDealt = 0;
		totalMagicAttackCount = 0;
		magicHitCount = 0;
		magicHitCountExpected = 0;
		offensivePraySuccessCount = 0;
		dead = false;
		pvpDamageCalc = new PvpDamageCalc(fight);
		fightLogEntries = new ArrayList<>();
		pendingAttacks = new LinkedList<>();
		baseLevels = player == PLUGIN.getClient().getLocalPlayer() ? CombatLevels.getRealLevels(PLUGIN.getClient()) : null;
	}

	// create a basic Fighter to only hold stats, for the TotalStatsPanel,
	// but not actually updated during a fight.
	public Fighter(String name)
	{
		player = null;
		this.name = name;
		attackCount = 0;
		offPraySuccessCount = 0;
		expectedDamage = 0;
		damageDealt = 0;
		totalMagicAttackCount = 0;
		magicHitCount = 0;
		magicHitCountExpected = 0;
		dead = false;
		pvpDamageCalc = null;
		fightLogEntries = new ArrayList<>();
		pendingAttacks = new LinkedList<>();
		baseLevels = null;
	}

	public BrewState getBrewState(FightLogEntry fightLogEntry)
	{
		return BrewState.from(
			fightLogEntry != null ? fightLogEntry.getAnimationData().attackStyle : null,
			fightLogEntry != null ? fightLogEntry.getAttackerLevels() : null,
			baseLevels);
	}

	// Levels can be null
	void addAttack(Player opponent, AnimationData animationData, int offensivePray, CombatLevels levels, int attackTick, long attackTime)
	{
		int[] attackerItems = player.getPlayerComposition().getEquipmentIds();

		// correct re-used animations into their separate AnimationData so it uses the correct attack style
		// for overhead success & accuracy calcs
		EquipmentData weapon = EquipmentData.fromId(fixItemId(attackerItems[KitType.WEAPON.getIndex()]));

		boolean successful = opponent.getOverheadIcon() != animationData.attackStyle.getProtection();

		// Granite Maul specific handling
		boolean isGmaulSpec = animationData == AnimationData.MELEE_GRANITE_MAUL_SPEC;
		boolean elyProc = hasTargetSpotAnim(opponent, SpotanimID.ELYSIAN_SHIELD_DEFEND_SPOTANIM);

		// --- Detect dark bow & dragon crossbow specials via GFX ---
		if (weapon == EquipmentData.DARK_BOW && animationData == AnimationData.RANGED_SHORTBOW)
		{
			boolean spec = hasTargetSpotAnim(opponent, GFX_TARGET_DBOW_SPEC);

			animationData = spec ? AnimationData.RANGED_DARK_BOW_SPEC : AnimationData.RANGED_DARK_BOW;
		}
		else if (weapon == EquipmentData.DRAGON_CROSSBOW &&
				(animationData == AnimationData.RANGED_CROSSBOW_PVP || animationData == AnimationData.RANGED_RUNE_CROSSBOW))
		{
			boolean spec = hasTargetSpotAnim(opponent, GFX_TARGET_DCBOW_SPEC);

			if (spec)
			{
				animationData = AnimationData.RANGED_DRAGON_CROSSBOW_SPEC;
			}
			}

		attackCount++;
		if (successful)
		{
			offPraySuccessCount++;
		}
		if (animationData.attackStyle.isUsingSuccessfulOffensivePray(offensivePray))
		{
			offensivePraySuccessCount++;
		}

		// track dragon longsword as VLS if enabled, for dmm practice purposes.
		// also check if weapon = VLS because the itemId stays as VLS if they don't switch weapons between
		// attacks, but we still need to update the animationData in case it's actually a dlong.
		if (CONFIG.dlongIsVls() && weapon == EquipmentData.DRAGON_LONGSWORD || weapon == EquipmentData.VESTAS_LONGSWORD)
		{
			// modifying attackerItems will modify the actual playerComposition, so future
			// .getPlayerComposition().getEquipmentIds() calls will also be modified
			attackerItems[KitType.WEAPON.getIndex()] = EquipmentData.VESTAS_LONGSWORD.getItemId() + PlayerComposition.ITEM_OFFSET;
			animationData = animationData.isSpecial ? AnimationData.MELEE_VLS_SPEC : AnimationData.MELEE_SCIM_SLASH;
		}

		boolean staffMeleeReduction = false;
		if (animationData.attackStyle.isMelee())
		{
			staffMeleeReduction = hasStaffMeleeReduction(opponent);
		}

		pvpDamageCalc.updateDamageStats(player, opponent, successful, animationData, offensivePray);
		if (elyProc)
		{
			pvpDamageCalc.applyElysianReduction();
		}
		if (staffMeleeReduction)
		{
			pvpDamageCalc.applyStaffMeleeReduction();
		}
		expectedDamage += pvpDamageCalc.getAverageHit();

		if (animationData.attackStyle == AnimationData.AttackStyle.MAGIC)
		{
			totalMagicAttackCount++;
			magicHitCountExpected += pvpDamageCalc.getAccuracy();

			if (opponent.getGraphic() != GraphicID.SPLASH)
			{
				magicHitCount++;
			}
		}

		FightLogEntry fightLogEntry = new FightLogEntry(player, opponent, pvpDamageCalc, offensivePray, levels, animationData, attackTick, attackTime);
		fightLogEntry.setDefenderElyProc(elyProc);
		fightLogEntry.setDefenderSotdMeleeReductionProc(staffMeleeReduction);
		fightLogEntry.setGmaulSpecial(isGmaulSpec);
		if (animationData.isSpecial && animationData != AnimationData.MELEE_GRANITE_MAUL_SPEC)
		{
			PvpPerformanceTrackerPlugin.PLUGIN.recordNonGmaulSpecial(player.getName(), fightLogEntry.getHitsplatMatchTick());
		}
		if (PvpPerformanceTrackerPlugin.CONFIG.fightLogInChat())
		{
			PvpPerformanceTrackerPlugin.PLUGIN.sendTradeChatMessage(fightLogEntry.toChatMessage());
		}
		fightLogEntries.add(fightLogEntry);
		pendingAttacks.add(fightLogEntry);
	}

	public void addGhostBarrage(boolean successful, Player opponent, AnimationData animationData, int offensivePray)
	{
		int currentTick = PLUGIN.getClient().getTickCount();
		if (currentTick <= lastGhostBarrageCheckedTick)
		{
			return;
		}
		lastGhostBarrageCheckedTick = currentTick;

		pvpDamageCalc.updateDamageStats(player, opponent, successful, animationData, offensivePray);

		ghostBarrageCount++;
		ghostBarrageExpectedDamage += pvpDamageCalc.getAverageHit();

		// TODO?: possible future feature - Create separate FightLog array for ghost barrages and include those in fight log table
		// Dunno if this really makes sense, we wouldn't have defender logs to do a proper merge with it
	}


	// this is to be used from the TotalStatsPanel which saves a total of multiple fights.
	public void addAttacks(int success, int total, double expectedDamage, int damageDealt, int totalMagicAttackCount, int magicHitCount, double magicHitCountExpected, int offensivePraySuccessCount, int hpHealed, int ghostBarrageCount, double ghostBarrageExpectedDamage)
	{
		offPraySuccessCount += success;
		attackCount += total;
		this.expectedDamage += expectedDamage;
		this.damageDealt += damageDealt;
		this.totalMagicAttackCount += totalMagicAttackCount;
		this.magicHitCount += magicHitCount;
		this.magicHitCountExpected += magicHitCountExpected;
		this.offensivePraySuccessCount += offensivePraySuccessCount;
		this.hpHealed += hpHealed;
		this.ghostBarrageCount += ghostBarrageCount;
		this.ghostBarrageExpectedDamage += ghostBarrageExpectedDamage;
	}

	void addDamageDealt(int damage)
	{
		this.damageDealt += damage;
	}

	void addHpHealed(int hpHealed)
	{
		this.hpHealed += hpHealed;
	}

	void died()
	{
		dead = true;
	}

	private static boolean hasTargetSpotAnim(Player opponent, int spotAnimId)
	{
		if (opponent.getGraphic() == spotAnimId)
		{
			return true;
		}

		IterableHashTable<ActorSpotAnim> spotAnims = opponent.getSpotAnims();
		if (spotAnims == null)
		{
			return false;
		}

		for (ActorSpotAnim spotAnim : spotAnims)
		{
			if (spotAnim != null && spotAnim.getId() == spotAnimId)
			{
				return true;
			}
		}

		return false;
	}

	private static boolean hasStaffMeleeReduction(Player opponent)
	{
		return hasTargetSpotAnim(opponent, SpotanimID.SOTD_SPECIAL_START) ||
			hasTargetSpotAnim(opponent, SpotanimID.SOTD_SPECIAL_EXTRA) ||
			hasTargetSpotAnim(opponent, SpotanimID.STAFF_OF_LIGHT_SPECIAL_START) ||
			hasTargetSpotAnim(opponent, SpotanimID.STAFF_OF_LIGHT_SPECIAL_EXTRA) ||
			hasTargetSpotAnim(opponent, SpotanimID.STAFF_OF_BALANCE_SPECIAL_START) ||
			hasTargetSpotAnim(opponent, SpotanimID.STAFF_OF_BALANCE_SPECIAL_EXTRA);
	}

	AnimationData getAnimationData()
	{
		return AnimationData.fromId(player.getAnimation());
	}

	// the "addAttack" for a defensive log that creates an "incomplete" fight log entry.
	void addDefensiveLogs(CombatLevels levels, int offensivePray, int attackTick, long attackTime)
	{
		fightLogEntries.add(new FightLogEntry(name, levels, offensivePray, attackTick, attackTime));
	}

	// Return a simple string to display the current player's success rate.
	// ex. "42/59 (71%)". The name is not included as it will be in a separate view.
	// if shortString is true, the percentage is omitted, it only returns the fraction.
	public String getOffPrayStats(boolean shortString)
	{
		return shortString ?
			offPraySuccessCount + "/" + attackCount :
			offPraySuccessCount + "/" + attackCount + " (" + nf.format(calculateOffPraySuccessPercentage()) + "%)";
	}

	public String getOffPrayStats()
	{
		return getOffPrayStats(false);
	}

	public String getMagicHitStats()
	{
		nf.setMaximumFractionDigits(0);
		String stats = nf.format(magicHitCount);
		long magicAttackCount = getMagicAttackCount();
		stats += "/" + nf.format(magicAttackCount);

		stats += " (" + getMagicHitLuckPercentage(nf) + "%)";
		return stats;
	}
	public String getMagicHitLuckPercentage(NumberFormat customNf)
	{
		return magicHitCountExpected != 0 ?
			customNf.format(((double) magicHitCount / magicHitCountExpected) * 100.0) :
			"0";
	}

	public String getShortMagicHitStats()
	{
		return magicHitCountExpected != 0 ?
			nf.format(((double) magicHitCount / magicHitCountExpected) * 100.0) + "%" :
			"0%";
	}

	public String getExpectedDmgString(Fighter opponent, int precision, boolean onlyDiff)
	{
		NumberFormat preciseNf = NumberFormat.getInstance();
		preciseNf.setRoundingMode(RoundingMode.HALF_UP);
		preciseNf.setMaximumFractionDigits(precision);
		double difference = expectedDamage - opponent.expectedDamage;
		return onlyDiff ? (difference > 0 ? "+" : "") + preciseNf.format(difference) :
			preciseNf.format(expectedDamage) + " (" + (difference > 0 ? "+" : "") + preciseNf.format(difference) + ")";
	}
	public String getExpectedDmgString(Fighter opponent)
	{
		return getExpectedDmgString(opponent, 0, false);
	}


	public String getDmgDealtString(Fighter opponent, boolean onlyDiff)
	{
		int difference = damageDealt - opponent.damageDealt;
		return onlyDiff ? (difference > 0 ? "+" : "") + difference:
			damageDealt + " (" + (difference > 0 ? "+" : "") + difference + ")";
	}
	public String getDmgDealtString(Fighter opponent)
	{
		return getDmgDealtString(opponent, false);
	}

	public double calculateOffPraySuccessPercentage()
	{
		return attackCount == 0 ? 0 :
		(double) offPraySuccessCount / attackCount * 100.0;
	}

	public double calculateOffensivePraySuccessPercentage()
	{
		return attackCount == 0 ? 0 :
			(double) offensivePraySuccessCount / attackCount * 100.0;
	}

	public int getMagicAttackCount()
	{
		return totalMagicAttackCount;
	}

	// Return a simple string to display the current player's offensive prayer success rate.
	// ex. "42/59 (71%)". The name is not included as it will be in a separate view.
	// if shortString is true, the percentage is omitted, it only returns the fraction.
	public String getOffensivePrayStats(boolean shortString)
	{
		return shortString ?
			offensivePraySuccessCount + "/" + attackCount :
			offensivePraySuccessCount + "/" + attackCount + " (" + nf.format(calculateOffensivePraySuccessPercentage()) + "%)";
	}

	public String getOffensivePrayStats()
	{
		return getOffensivePrayStats(false);
	}

	public String getGhostBarrageStats()
	{
		return ghostBarrageCount + " G.B. (" + nf1.format(ghostBarrageExpectedDamage) + ")";
	}

	public void resetRobeHits()
	{
		this.robeHits = 0;
	}
	public void addRobeHit()
	{
		this.robeHits++;
	}
}
