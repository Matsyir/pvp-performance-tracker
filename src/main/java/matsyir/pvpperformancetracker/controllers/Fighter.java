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
import static matsyir.pvpperformancetracker.utils.PvpPerformanceTrackerUtils.fixItemId;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.EquipmentData;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import net.runelite.api.GraphicID;

@Slf4j
@Getter
public
class Fighter
{
	private static final NumberFormat nf = NumberFormat.getInstance();
	static // initialize number format
	{
		nf.setMaximumFractionDigits(1);
		nf.setRoundingMode(RoundingMode.HALF_UP);
	}

	// Target graphics IDs indicating special attacks
	private static final int GFX_TARGET_DBOW_SPEC = 1100;   // dragon-arrow gfx on target
	private static final int GFX_TARGET_DCBOW_SPEC = 157;    // Annihilate AOE gfx on target

	@Setter
	private Player player;
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
	@SerializedName("d")
	private double deservedDamage; // total deserved damage based on gear & opponent's pray
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
	private double magicHitCountDeserved; // cumulative magic accuracy percentage for each attack

	@Expose
	@SerializedName("p")
	private int offensivePraySuccessCount;

	@Expose
	@SerializedName("g")
	private int ghostBarrageCount;
	@Expose
	@SerializedName("y")
	private double ghostBarrageDeservedDamage;

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
		deservedDamage = 0;
		damageDealt = 0;
		totalMagicAttackCount = 0;
		magicHitCount = 0;
		magicHitCountDeserved = 0;
		offensivePraySuccessCount = 0;
		dead = false;
		pvpDamageCalc = new PvpDamageCalc(fight);
		fightLogEntries = new ArrayList<>();
		pendingAttacks = new LinkedList<>();
	}

	// fighter for merging fight logs together for detailed data (fight analysis)
	Fighter(FightPerformance fight, String name, ArrayList<FightLogEntry> logs)
	{
		player = null;
		this.name = name;
		attackCount = 0;
		offPraySuccessCount = 0;
		deservedDamage = 0;
		damageDealt = 0;
		totalMagicAttackCount = 0;
		magicHitCount = 0;
		magicHitCountDeserved = 0;
		dead = false;
		pvpDamageCalc = new PvpDamageCalc(fight);
		fightLogEntries = logs;
		pendingAttacks = new LinkedList<>();
	}

	// create a basic Fighter to only hold stats, for the TotalStatsPanel,
	// but not actually updated during a fight.
	public Fighter(String name)
	{
		player = null;
		this.name = name;
		attackCount = 0;
		offPraySuccessCount = 0;
		deservedDamage = 0;
		damageDealt = 0;
		totalMagicAttackCount = 0;
		magicHitCount = 0;
		magicHitCountDeserved = 0;
		dead = false;
		pvpDamageCalc = null;
		fightLogEntries = new ArrayList<>();
		pendingAttacks = new LinkedList<>();
	}

	// Fighter for AnalyzedFightPerformance
	public Fighter(FightPerformance fight, String name)
	{
		this(name);
		pvpDamageCalc = new PvpDamageCalc(fight);
	}

	// add an attack to the counters depending if it is successful or not.
	// also update the success rate with the new counts.
	// Used for regular, ongoing fights
	void addAttack(Player opponent, AnimationData animationData, int offensivePray)
	{
		addAttack(opponent, animationData, offensivePray, null);
	}

	// Levels can be null
	void addAttack(Player opponent, AnimationData animationData, int offensivePray, CombatLevels levels)
	{
		int[] attackerItems = player.getPlayerComposition().getEquipmentIds();

		// correct re-used animations into their separate AnimationData so it uses the correct attack style
		// for overhead success & accuracy calcs
		EquipmentData weapon = EquipmentData.fromId(fixItemId(attackerItems[KitType.WEAPON.getIndex()]));

		boolean successful = opponent.getOverheadIcon() != animationData.attackStyle.getProtection();

		// Granite Maul specific handling
		boolean isGmaulSpec = animationData == AnimationData.MELEE_GRANITE_MAUL_SPEC;

		// --- Detect dark bow & dragon crossbow specials via GFX ---
		if (weapon == EquipmentData.DARK_BOW && animationData == AnimationData.RANGED_SHORTBOW)
		{
			boolean spec = opponent.getGraphic() == GFX_TARGET_DBOW_SPEC;

			animationData = spec ? AnimationData.RANGED_DARK_BOW_SPEC : AnimationData.RANGED_DARK_BOW;
		}
		else if (weapon == EquipmentData.DRAGON_CROSSBOW &&
				(animationData == AnimationData.RANGED_CROSSBOW_PVP || animationData == AnimationData.RANGED_RUNE_CROSSBOW))
		{
			boolean spec = opponent.getGraphic() == GFX_TARGET_DCBOW_SPEC;

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

		pvpDamageCalc.updateDamageStats(player, opponent, successful, animationData);
		deservedDamage += pvpDamageCalc.getAverageHit();

		if (animationData.attackStyle == AnimationData.AttackStyle.MAGIC)
		{
			totalMagicAttackCount++;
			magicHitCountDeserved += pvpDamageCalc.getAccuracy();

			if (opponent.getGraphic() != GraphicID.SPLASH)
			{
				magicHitCount++;
			}
		}

		FightLogEntry fightLogEntry = new FightLogEntry(player, opponent, pvpDamageCalc, offensivePray, levels, animationData);
		fightLogEntry.setGmaulSpecial(isGmaulSpec);
		if (PvpPerformanceTrackerPlugin.CONFIG.fightLogInChat())
		{
			PvpPerformanceTrackerPlugin.PLUGIN.sendTradeChatMessage(fightLogEntry.toChatMessage());
		}
		fightLogEntries.add(fightLogEntry);
		pendingAttacks.add(fightLogEntry);
	}

	// add an attack from fight log, without player references, for merging fight logs (fight analysis)
	void addAttack(FightLogEntry logEntry, FightLogEntry defenderLog)
	{
		attackCount++;
		if (logEntry.success())
		{
			offPraySuccessCount++;
		}
		if (logEntry.getAnimationData().attackStyle.isUsingSuccessfulOffensivePray(logEntry.getAttackerOffensivePray()))
		{
			offensivePraySuccessCount++;
		}

		pvpDamageCalc.updateDamageStats(logEntry, defenderLog);
		deservedDamage += pvpDamageCalc.getAverageHit();

		if (logEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC)
		{
			totalMagicAttackCount++;
			magicHitCountDeserved += pvpDamageCalc.getAccuracy();
			// actual magicHitCount is directly added, as it can no longer
			// be detected and should have been accurate initially.
		}

		fightLogEntries.add(new FightLogEntry(logEntry, pvpDamageCalc));
	}

	public void addGhostBarrage(boolean successful, Player opponent, AnimationData animationData, int offensivePray, CombatLevels levels)
	{
		int currentTick = PLUGIN.getClient().getTickCount();
		if (currentTick <= lastGhostBarrageCheckedTick)
		{
			return;
		}
		lastGhostBarrageCheckedTick = currentTick;

		pvpDamageCalc.updateDamageStats(player, opponent, successful, animationData);

		ghostBarrageCount++;
		ghostBarrageDeservedDamage += pvpDamageCalc.getAverageHit();

		// TODO: Create separate FightLog array for ghost barrages and include those in fight log table
		// ^^^ also so they could be used in fight analysis/merge. Unused params will be used for this
	}

	// used to manually build Fighters in AnalyzedFightPerformance.
	public void setTotalGhostBarrageStats(int ghostBarrageCount, double ghostBarrageDeservedDamage)
	{
		this.ghostBarrageCount = ghostBarrageCount;
		this.ghostBarrageDeservedDamage = ghostBarrageDeservedDamage;
	}

	// this is to be used from the TotalStatsPanel which saves a total of multiple fights.
	public void addAttacks(int success, int total, double deservedDamage, int damageDealt, int totalMagicAttackCount, int magicHitCount, double magicHitCountDeserved, int offensivePraySuccessCount, int hpHealed, int ghostBarrageCount, double ghostBarrageDeservedDamage)
	{
		offPraySuccessCount += success;
		attackCount += total;
		this.deservedDamage += deservedDamage;
		this.damageDealt += damageDealt;
		this.totalMagicAttackCount += totalMagicAttackCount;
		this.magicHitCount += magicHitCount;
		this.magicHitCountDeserved += magicHitCountDeserved;
		this.offensivePraySuccessCount += offensivePraySuccessCount;
		this.hpHealed += hpHealed;
		this.ghostBarrageCount += ghostBarrageCount;
		this.ghostBarrageDeservedDamage += ghostBarrageDeservedDamage;
	}

	void addDamageDealt(int damage)
	{
		this.damageDealt += damage;
	}

	// will be used for merging fight logs (fight analysis)
	void addMagicHitCount(int count)
	{
		this.magicHitCount += count;
	}

	void addHpHealed(int hpHealed)
	{
		this.hpHealed += hpHealed;
	}

	void died()
	{
		dead = true;
	}

	AnimationData getAnimationData()
	{
		return AnimationData.fromId(player.getAnimation());
	}

	// the "addAttack" for a defensive log that creates an "incomplete" fight log entry.
	void addDefensiveLogs(CombatLevels levels, int offensivePray)
	{
		fightLogEntries.add(new FightLogEntry(name, levels, offensivePray));
	}

	// Return a simple string to display the current player's success rate.
	// ex. "42/59 (71%)". The name is not included as it will be in a separate view.
	// if shortString is true, the percentage is omitted, it only returns the fraction.
	public String getOffPrayStats(boolean shortString)
	{
		nf.setMaximumFractionDigits(1);
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
		nf.setMaximumFractionDigits(1);
		String luckPercentage = magicHitCountDeserved != 0 ?
			nf.format(((double)magicHitCount / magicHitCountDeserved) * 100.0) :
			"0";
		stats += " (" + luckPercentage + "%)";
		return stats;
	}

	public String getShortMagicHitStats()
	{
		nf.setMaximumFractionDigits(1);
		return magicHitCountDeserved != 0 ?
			nf.format(((double)magicHitCount / magicHitCountDeserved) * 100.0) + "%" :
			"0%";
	}

	public String getDeservedDmgString(Fighter opponent, int precision, boolean onlyDiff)
	{
		nf.setMaximumFractionDigits(precision);
		double difference = deservedDamage - opponent.deservedDamage;
		return onlyDiff ? (difference > 0 ? "+" : "") + nf.format(difference) :
			nf.format(deservedDamage) + " (" + (difference > 0 ? "+" : "") + nf.format(difference) + ")";
	}
	public String getDeservedDmgString(Fighter opponent)
	{
		return getDeservedDmgString(opponent, 0, false);
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
		nf.setMaximumFractionDigits(1);
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
		return ghostBarrageCount + " G.B. (" + nf.format(ghostBarrageDeservedDamage) + ")";
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
