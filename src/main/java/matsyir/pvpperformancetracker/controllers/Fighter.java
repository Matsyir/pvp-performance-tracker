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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.fixItemId;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.EquipmentData;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.utils.PvpPerformanceTrackerUtils; // Added import
import net.runelite.api.Client; // Added import
import net.runelite.api.GraphicID;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Skill; // Added import
import net.runelite.api.kit.KitType;

@Slf4j
@Getter
public class Fighter {
	// Removed fightPerformance field
	private static final NumberFormat nf = NumberFormat.getInstance();
	static // initialize number format
	{
		nf.setMaximumFractionDigits(1);
		nf.setRoundingMode(RoundingMode.HALF_UP);
	}

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
	@SerializedName("x") // x for X_X
	private boolean dead; // will be true if the fighter died in the fight

	@Expose
	@SerializedName("l")
	private ArrayList<FightLogEntry> fightLogEntries;

	private PvpDamageCalc pvpDamageCalc;
	private int lastGhostBarrageCheckedTick = -1;
	@Setter
	private int lastGhostBarrageCheckedMageXp = -1;

	// fighter that is bound to a player and gets updated during a fight
	Fighter(FightPerformance fight, Player player) {
		// Removed fightPerformance initialization
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
	}

	// fighter for merging fight logs together for detailed data (fight analysis)
	Fighter(FightPerformance fight, String name, ArrayList<FightLogEntry> logs) {
		// Removed fightPerformance initialization
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
	}

	// create a basic Fighter to only hold stats, for the TotalStatsPanel,
	// but not actually updated during a fight.
	public Fighter(String name) {
		// Removed fightPerformance initialization
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
	}

	// Fighter for AnalyzedFightPerformance
	public Fighter(FightPerformance fight, String name) {
		// Removed fightPerformance initialization
		player = null;
		this.name = name;
		attackCount = 0;
		offPraySuccessCount = 0;
		deservedDamage = 0;
		damageDealt = 0;
		totalMagicAttackCount = 0;
		magicHitCount = 0;
		magicHitCountDeserved = 0;
		offensivePraySuccessCount = 0; // Added missing initialization
		ghostBarrageCount = 0; // Added missing initialization
		ghostBarrageDeservedDamage = 0; // Added missing initialization
		hpHealed = 0; // Added missing initialization
		dead = false;
		pvpDamageCalc = new PvpDamageCalc(fight);
		fightLogEntries = new ArrayList<>(); // Added missing initialization
	}

	// add an attack to the counters depending if it is successful or not.
	// also update the success rate with the new counts.
	// Used for regular, ongoing fights
	void addAttack(Player opponent, AnimationData animationData, int offensivePray) {
		addAttack(opponent, animationData, offensivePray, null);
	}

	// Levels can be null
	void addAttack(Player opponent, AnimationData animationData, int offensivePray, CombatLevels levels) {
		int[] attackerItems = player.getPlayerComposition().getEquipmentIds();

		// correct re-used animations into their separate AnimationData so it uses the
		// correct attack style
		// for overhead success & accuracy calcs
		EquipmentData weapon = EquipmentData.fromId(fixItemId(attackerItems[KitType.WEAPON.getIndex()]));

		boolean successful = opponent.getOverheadIcon() != animationData.attackStyle.getProtection();

		attackCount++;
		if (successful) {
			offPraySuccessCount++;
		}
		if (animationData.attackStyle.isUsingSuccessfulOffensivePray(offensivePray)) {
			offensivePraySuccessCount++;
		}

		// track dragon longsword as VLS if enabled, for dmm practice purposes.
		// also check if weapon = VLS because the itemId stays as VLS if they don't
		// switch weapons between
		// attacks, but we still need to update the animationData in case it's actually
		// a dlong.
		if (CONFIG.dlongIsVls() && weapon == EquipmentData.DRAGON_LONGSWORD
				|| weapon == EquipmentData.VESTAS_LONGSWORD) {
			// modifying attackerItems will modify the actual playerComposition, so future
			// .getPlayerComposition().getEquipmentIds() calls will also be modified
			attackerItems[KitType.WEAPON.getIndex()] = EquipmentData.VESTAS_LONGSWORD.getItemId()
					+ PlayerComposition.ITEM_OFFSET;
			animationData = animationData.isSpecial ? AnimationData.MELEE_VLS_SPEC : AnimationData.MELEE_SCIM_SLASH;
		}

		pvpDamageCalc.updateDamageStats(player, opponent, successful, animationData);
		deservedDamage += pvpDamageCalc.getAverageHit();

		if (animationData.attackStyle == AnimationData.AttackStyle.MAGIC) {
			totalMagicAttackCount++;
			magicHitCountDeserved += pvpDamageCalc.getAccuracy();

			if (opponent.getGraphic() != GraphicID.SPLASH) {
				magicHitCount++;
			}
		}

		// KO Chance calculation moved to onHitsplatApplied

		FightLogEntry fightLogEntry = new FightLogEntry(player, opponent, pvpDamageCalc, offensivePray, levels,
				animationData);
		// koChance will be set later in onHitsplatApplied

		if (PvpPerformanceTrackerPlugin.CONFIG.fightLogInChat()) {
			PvpPerformanceTrackerPlugin.PLUGIN.sendChatMessage(fightLogEntry.toChatMessage());
		}
		fightLogEntries.add(fightLogEntry);
	}

	// add an attack from fight log, without player references, for merging fight
	// logs (fight analysis)
	void addAttack(FightLogEntry logEntry, FightLogEntry defenderLog) {
		attackCount++;
		if (logEntry.success()) {
			offPraySuccessCount++;
		}
		if (logEntry.getAnimationData().attackStyle
				.isUsingSuccessfulOffensivePray(logEntry.getAttackerOffensivePray())) {
			offensivePraySuccessCount++;
		}

		pvpDamageCalc.updateDamageStats(logEntry, defenderLog);
		deservedDamage += pvpDamageCalc.getAverageHit();

		if (logEntry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC) {
			totalMagicAttackCount++;
			magicHitCountDeserved += pvpDamageCalc.getAccuracy();
			// actual magicHitCount is directly added, as it can no longer
			// be detected and should have been accurate initially.
		}

		fightLogEntries.add(new FightLogEntry(logEntry, pvpDamageCalc));
	}

	public void addGhostBarrage(boolean successful, Player opponent, AnimationData animationData, int offensivePray,
			CombatLevels levels) {
		int currentTick = PLUGIN.getClient().getTickCount();
		if (currentTick <= lastGhostBarrageCheckedTick) {
			return;
		}
		lastGhostBarrageCheckedTick = currentTick;

		pvpDamageCalc.updateDamageStats(player, opponent, successful, animationData);

		ghostBarrageCount++;
		ghostBarrageDeservedDamage += pvpDamageCalc.getAverageHit();

		// TODO: Create separate FightLog array for ghost barrages and include those in
		// fight log table
		// ^^^ also so they could be used in fight analysis/merge. Unused params will be
		// used for this
	}

	// used to manually build Fighters in AnalyzedFightPerformance.
	public void setTotalGhostBarrageStats(int ghostBarrageCount, double ghostBarrageDeservedDamage) {
		this.ghostBarrageCount = ghostBarrageCount;
		this.ghostBarrageDeservedDamage = ghostBarrageDeservedDamage;
	}

	// this is to be used from the TotalStatsPanel which saves a total of multiple
	// fights.
	public void addAttacks(int success, int total, double deservedDamage, int damageDealt, int totalMagicAttackCount,
			int magicHitCount, double magicHitCountDeserved, int offensivePraySuccessCount, int hpHealed,
			int ghostBarrageCount, double ghostBarrageDeservedDamage) {
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

	void addDamageDealt(int damage) {
		this.damageDealt += damage;
	}

	// will be used for merging fight logs (fight analysis)
	void addMagicHitCount(int count) {
		this.magicHitCount += count;
	}

	void addHpHealed(int hpHealed) {
		this.hpHealed += hpHealed;
	}

	void died() {
		dead = true;
	}

	AnimationData getAnimationData() {
		return AnimationData.fromId(player.getAnimation());
	}

	// the "addAttack" for a defensive log that creates an "incomplete" fight log
	// entry.
	void addDefensiveLogs(CombatLevels levels, int offensivePray) {
		fightLogEntries.add(new FightLogEntry(name, levels, offensivePray));
	}

	// Return a simple string to display the current player's success rate.
	// ex. "42/59 (71%)". The name is not included as it will be in a separate view.
	// if shortString is true, the percentage is omitted, it only returns the
	// fraction.
	public String getOffPrayStats(boolean shortString) {
		nf.setMaximumFractionDigits(0);
		return shortString ? offPraySuccessCount + "/" + attackCount
				: nf.format(offPraySuccessCount) + "/" + nf.format(attackCount) + " ("
						+ Math.round(calculateOffPraySuccessPercentage()) + "%)";
	}

	public String getOffPrayStats() {
		return getOffPrayStats(false);
	}

	public String getMagicHitStats() {
		nf.setMaximumFractionDigits(0);
		String stats = nf.format(magicHitCount);
		long magicAttackCount = getMagicAttackCount();
		stats += "/" + nf.format(magicAttackCount);
		nf.setMaximumFractionDigits(2);
		String luckPercentage = magicHitCountDeserved != 0
				? nf.format(((double) magicHitCount / magicHitCountDeserved) * 100.0)
				: "0";
		stats += " (" + luckPercentage + "%)";
		return stats;
	}

	public String getShortMagicHitStats() {
		nf.setMaximumFractionDigits(2);
		return magicHitCountDeserved != 0 ? nf.format(((double) magicHitCount / magicHitCountDeserved) * 100.0) + "%"
				: "0%";
	}

	public String getDeservedDmgString(Fighter opponent, int precision, boolean onlyDiff) {
		nf.setMaximumFractionDigits(precision);
		double difference = deservedDamage - opponent.deservedDamage;
		return onlyDiff ? (difference > 0 ? "+" : "") + nf.format(difference)
				: nf.format(deservedDamage) + " (" + (difference > 0 ? "+" : "") + nf.format(difference) + ")";
	}

	public String getDeservedDmgString(Fighter opponent) {
		return getDeservedDmgString(opponent, 0, false);
	}

	public String getDmgDealtString(Fighter opponent, boolean onlyDiff) {
		int difference = damageDealt - opponent.damageDealt;
		return onlyDiff ? (difference > 0 ? "+" : "") + difference
				: damageDealt + " (" + (difference > 0 ? "+" : "") + difference + ")";
	}

	public String getDmgDealtString(Fighter opponent) {
		return getDmgDealtString(opponent, false);
	}

	public double calculateOffPraySuccessPercentage() {
		return attackCount == 0 ? 0 : (double) offPraySuccessCount / attackCount * 100.0;
	}

	public double calculateOffensivePraySuccessPercentage() {
		return attackCount == 0 ? 0 : (double) offensivePraySuccessCount / attackCount * 100.0;
	}

	public int getMagicAttackCount() {
		return totalMagicAttackCount;
	}

	// Return a simple string to display the current player's offensive prayer
	// success rate.
	// ex. "42/59 (71%)". The name is not included as it will be in a separate view.
	// if shortString is true, the percentage is omitted, it only returns the
	// fraction.
	public String getOffensivePrayStats(boolean shortString) {
		nf.setMaximumFractionDigits(0);
		return shortString ? offensivePraySuccessCount + "/" + attackCount
				: nf.format(offensivePraySuccessCount) + "/" + nf.format(attackCount) + " ("
						+ Math.round(calculateOffensivePraySuccessPercentage()) + "%)";
	}

	public String getOffensivePrayStats() {
		return getOffensivePrayStats(false);
	}

	public String getGhostBarrageStats() {
		return ghostBarrageCount + " G.B. (" + nf.format(ghostBarrageDeservedDamage) + ")";
	}
}
