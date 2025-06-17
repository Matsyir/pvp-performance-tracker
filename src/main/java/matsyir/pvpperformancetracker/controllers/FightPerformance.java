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
package matsyir.pvpperformancetracker.controllers;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.AnimationData.AttackStyle;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.models.FightType;
import matsyir.pvpperformancetracker.models.oldVersions.FightPerformance__1_5_5;
import net.runelite.api.AnimationID;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerConfig;
import net.runelite.api.kit.KitType;
import static matsyir.pvpperformancetracker.utils.PvpPerformanceTrackerUtils.fixItemId;
import static matsyir.pvpperformancetracker.controllers.PvpDamageCalc.RANGE_DEF;

// Holds two Fighters which contain data about PvP fight performance, and has many methods to
// add to the fight, display stats or check the status of the fight.
@Slf4j
@Getter
public class FightPerformance implements Comparable<FightPerformance>
{
	private static final int[] DEATH_ANIMATIONS = {
		AnimationID.DEATH,	// Default
		10629,	// League IV
		11902,	// League V
	};
	// Delay to assume a fight is over. May seem long, but sometimes people barrage &
	// stand under for a while to eat. Fights will automatically end when either competitor dies.
	private static final Duration NEW_FIGHT_DELAY = Duration.ofSeconds(21);
	private static final NumberFormat nf = NumberFormat.getInstance();
	static // initialize number format
	{
		nf.setMaximumFractionDigits(1);
		nf.setRoundingMode(RoundingMode.HALF_UP);
	}

	@Expose
	@SerializedName("c") // use 1 letter serialized variable names for more compact storage
	public Fighter competitor;
	@Expose
	@SerializedName("o")
	public Fighter opponent;
	@Expose
	@SerializedName("t")
	public long lastFightTime; // last fight time saved as epochMilli timestamp (serializing an Instant was a bad time)
	@Expose
	@SerializedName("l")
	public FightType fightType; // save a boolean if the fight was done in LMS, so we can know those stats/rings/ammo are used.
	@Expose
	@SerializedName("w")
	private int world;

	private int competitorPrevHp; // intentionally don't serialize this, temp variable used to calculate hp healed.

	// KO Chance stats, updated per-attack. Not serialized.
	@Getter
	private transient double competitorTotalKoChance = 0;
	@Getter
	private transient double opponentTotalKoChance = 0;
	@Getter
	private transient Double competitorLastKoChance = null;
	@Getter
	private transient Double opponentLastKoChance = null;
	@Getter
	private transient int competitorKoChanceCount = 0;
	@Getter
	private transient int opponentKoChanceCount = 0;
	private transient double competitorSurvivalProb = 1.0;
	private transient double opponentSurvivalProb = 1.0;

	// shouldn't be used, just here so we can make a subclass, weird java thing
	public FightPerformance()
	{

	}

	// constructor which initializes a fight from the 2 Players, starting stats at 0. Regular use constructor.
	public FightPerformance(Player competitor, Player opponent)
	{
		int defLvl = PLUGIN.getClient().getBoostedSkillLevel(Skill.DEFENCE);

		// determine fight type based on being at LMS areas & use def level to check for LMS builds.
		this.fightType = !PLUGIN.isAtLMS() ? FightType.NORMAL :
			defLvl <= FightType.LMS_1DEF.getCombatLevelsForType().def ? FightType.LMS_1DEF :
			defLvl <= FightType.LMS_ZERK.getCombatLevelsForType().def ? FightType.LMS_ZERK :
			FightType.LMS_MAXMED;

		// initialize world
		this.world = PLUGIN.getClient().getWorld();

		// this is initialized soon before the NEW_FIGHT_DELAY time because the event we
		// determine the opponent from is not fully reliable.
		lastFightTime = Instant.now().minusSeconds(NEW_FIGHT_DELAY.getSeconds() - 5).toEpochMilli();

		this.competitor = new Fighter(this, competitor);
		this.opponent = new Fighter(this, opponent);

		this.competitorPrevHp = PLUGIN.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
		this.competitor.setLastGhostBarrageCheckedMageXp(PLUGIN.getClient().getSkillExperience(Skill.MAGIC));
	}

	// create a FightPerformance using an old 1.5.5 or earlier version.
	public FightPerformance(FightPerformance__1_5_5 old)
	{
		this.competitor = old.competitor;
		this.opponent = old.opponent;
		this.lastFightTime = old.lastFightTime;
		if (old.isLmsFight)
		{
			if (!competitor.getFightLogEntries().isEmpty())
			{
				int defLvl = competitor.getFightLogEntries().get(0).getAttackerLevels().def;
				this.fightType =
					defLvl <= FightType.LMS_1DEF.getCombatLevelsForType().def ? FightType.LMS_1DEF :
					defLvl <= FightType.LMS_ZERK.getCombatLevelsForType().def ? FightType.LMS_ZERK :
					FightType.LMS_MAXMED;
			}
			else
			{
				this.fightType = FightType.LMS_MAXMED;
			}
		}
		else
		{
			this.fightType = FightType.NORMAL;
		}
	}

	// return a random fightPerformance used for testing UI
	static FightPerformance getTestInstance()
	{
		int cTotal = (int)(Math.random() * 60) + 8;
		int cSuccess = (int)(Math.random() * (cTotal - 4)) + 4;
		double cDamage = (Math.random() * (cSuccess * 25));

		int oTotal = (int)(Math.random() * 60) + 8;
		int oSuccess = (int)(Math.random() * (oTotal - 4)) + 4;
		double oDamage = (Math.random() * (oSuccess * 25));

		int secOffset = (int)(Math.random() * 57600) - 28800;

		boolean cDead = Math.random() >= 0.5;

		ArrayList<FightLogEntry> fightLogEntries = new ArrayList<>();
		int [] attackerItems = {0, 0, 0};
		int [] defenderItems = {0, 0, 0};
		String attackerName = "testname";
		FightLogEntry fightLogEntry = new FightLogEntry(attackerItems, 21, 0.5, 1, 12, defenderItems, attackerName);
		FightLogEntry fightLogEntry2 = new FightLogEntry(attackerItems, 11, 0.2, 1, 41, defenderItems, attackerName);
		FightLogEntry fightLogEntry3 = new FightLogEntry(attackerItems, 12, 0.3, 1, 21, defenderItems, attackerName);
		FightLogEntry fightLogEntry4 = new FightLogEntry(attackerItems, 43, 0.1, 1, 23, defenderItems, attackerName);
		fightLogEntries.add(fightLogEntry);
		fightLogEntries.add(fightLogEntry2);
		fightLogEntries.add(fightLogEntry3);
		fightLogEntries.add(fightLogEntry4);
		return new FightPerformance("Matsyir", "TEST_DATA", cSuccess, cTotal, cDamage, oSuccess, oTotal, oDamage, cDead, secOffset, fightLogEntries);
	}

	// Used for testing purposes
	private FightPerformance(String cName, String oName, int cSuccess, int cTotal, double cDamage, int oSuccess, int oTotal, double oDamage, boolean cDead, int secondOffset, ArrayList<FightLogEntry> fightLogs)
	{
		this.competitor = new Fighter(this, cName, fightLogs);
		this.opponent = new Fighter(this, oName, fightLogs);

		competitor.addAttacks(cSuccess, cTotal, cDamage, (int)cDamage, 20, 12, 13, 11, 22, 25, 26);
		opponent.addAttacks(oSuccess, oTotal, oDamage, (int)oDamage, 20, 14, 13, 11, 22, 25, 26);

		if (cDead)
		{
			competitor.died();
		}
		else
		{
			opponent.died();
		}

		lastFightTime = Instant.now().minusSeconds(secondOffset).toEpochMilli();
	}

	// If the given playerName is in this fight, check the Fighter's current animation,
	// add an attack if attacking, and compare attack style used with the opponent's overhead
	// to determine if successful.
	public void checkForAttackAnimations(Player eventSource, CombatLevels competitorLevels)
	{
		if (eventSource == null || eventSource.getName() == null || eventSource.getInteracting() == null || eventSource.getInteracting().getName() == null)
		{
			return;
		}

		String eName = eventSource.getName(); // event source name
		String interactingName = eventSource.getInteracting().getName();
		boolean addedAttack = false;

		// verify that the player is interacting with their tracked opponent before adding attacks
		if (eName.equals(competitor.getName()) && Objects.equals(interactingName, opponent.getName()))
		{
			competitor.setPlayer(eventSource);
			AnimationData animationData = competitor.getAnimationData();
			if (animationData != null)
			{
				int offensivePray = PLUGIN.currentlyUsedOffensivePray();
				competitor.addAttack(
					opponent.getPlayer(),
					animationData,
					offensivePray,
					competitorLevels);
				lastFightTime = Instant.now().toEpochMilli();
				addedAttack = true;

			}
		}
		else if (eName.equals(opponent.getName()) && Objects.equals(interactingName, competitor.getName()))
		{
			opponent.setPlayer(eventSource);
			AnimationData animationData = opponent.getAnimationData();
			if (animationData != null)
			{
				// there is no offensive prayer data for the opponent so hardcode 0
				opponent.addAttack(competitor.getPlayer(), animationData, 0);
				addedAttack = true;
				// add a defensive log for the competitor while the opponent is attacking, to be used with the fight analysis/merge
				competitor.addDefensiveLogs(competitorLevels, PLUGIN.currentlyUsedOffensivePray());
				lastFightTime = Instant.now().toEpochMilli();
			}
		}

		// Ensure robe hits are calculated for the live overlay when enabled (otherwise, just calc'd once at the end)
		// ideally should be refactored into Fighter, using ongoing counters like most other stats, instead of a loop.
		// However, I think it's mostly negligible due to how fight logs are limited to not becoming too large, due
		// to how long fights last.
		if (addedAttack && CONFIG.showOverlayRobeHits())
		{
			calculateRobeHits(CONFIG.robeHitFilter());
		}
	}

	// this only gets called when the local client player receives a magic xp drop.
	public void checkForLocalGhostBarrage(CombatLevels competitorLevels, Player localPlayer)
	{
		if (localPlayer == null)
		{
			log.info("Client player null while checking for ghost barrage - shouldn't happen");
			return;
		}

		competitor.setPlayer(localPlayer);
		if (localPlayer.getInteracting() instanceof Player && localPlayer.getInteracting().getName().equals(opponent.getName()))
		{
			opponent.setPlayer((Player)localPlayer.getInteracting());
		}

		AnimationData animationData = competitor.getAnimationData();

		if (animationData == null || animationData.attackStyle != AnimationData.AttackStyle.MAGIC)
		{
			animationData = AnimationData.MAGIC_ANCIENT_MULTI_TARGET;

			int offensivePray = PLUGIN.currentlyUsedOffensivePray();
			competitor.addGhostBarrage(opponent.getPlayer().getOverheadIcon() != animationData.attackStyle.getProtection(),
				opponent.getPlayer(),
				AnimationData.MAGIC_ANCIENT_MULTI_TARGET,
				offensivePray,
				competitorLevels);
		}
	}

	// add damage dealt to the opposite player.
	// the player name being passed in is the one who has the hitsplat on them.
	public void addDamageDealt(String playerName, int damage)
	{
		if (playerName == null) { return; }

		if (playerName.equals(competitor.getName()))
		{
			opponent.addDamageDealt(damage);
		}
		else if (playerName.equals(opponent.getName()))
		{
			competitor.addDamageDealt(damage);
		}
	}

	public void updateCompetitorHp(int currentHp)
	{
		if (currentHp > competitorPrevHp)
		{
			int hpHealed = currentHp - competitorPrevHp;
			competitor.addHpHealed(hpHealed);
		}
		competitorPrevHp = currentHp;
	}

	// Will return true and stop the fight if the fight should be over.
	// if either competitor hasn't fought in NEW_FIGHT_DELAY, or either competitor died.
	// Will also add the currentFight to fightHistory if the fight ended.
	public boolean isFightOver()
	{
		boolean isOver = false;
		// if either competitor died, end the fight.
		if (Arrays.stream(DEATH_ANIMATIONS).anyMatch(e -> e == opponent.getPlayer().getAnimation()))
		{
			opponent.died();
			isOver = true;
		}
		if (Arrays.stream(DEATH_ANIMATIONS).anyMatch(e -> e == competitor.getPlayer().getAnimation()))
		{
			competitor.died();
			isOver = true;
		}
		// If there was no fight actions in the last NEW_FIGHT_DELAY seconds
		if (Duration.between(Instant.ofEpochMilli(lastFightTime), Instant.now()).compareTo(NEW_FIGHT_DELAY) > 0)
		{
			isOver = true;
		}

		if (isOver)
		{
			lastFightTime = Instant.now().toEpochMilli();
		}

		return isOver;
	}

	public ArrayList<FightLogEntry> getAllFightLogEntries()
	{
		if (competitor.getFightLogEntries() == null || opponent.getFightLogEntries() == null)
		{
			return new ArrayList<>();
		}

		ArrayList<FightLogEntry> combinedList = new ArrayList<>();
		combinedList.addAll(competitor.getFightLogEntries());
		combinedList.addAll(opponent.getFightLogEntries());
		combinedList.sort(FightLogEntry::compareTo);
		return combinedList;
	}

	// Count the fight as started if either:
	// 1. The competitor has attacked the opponent
	// 2. The opponent has attacked the competitor
	// Attacks are only counted when there is a confirmed interaction between the two players
	// Checking both makes sure that the very first interaction is counted/tracked
	public boolean fightStarted()
	{
		return competitor.getAttackCount() > 0 || opponent.getAttackCount() > 0;
	}

	// returns true if competitor off-pray hit success rate > opponent success rate.
	// the following functions have similar behaviour.
	public boolean competitorOffPraySuccessIsGreater()
	{
		return competitor.calculateOffPraySuccessPercentage() > opponent.calculateOffPraySuccessPercentage();
	}

	public boolean opponentOffPraySuccessIsGreater()
	{
		return opponent.calculateOffPraySuccessPercentage() > competitor.calculateOffPraySuccessPercentage();
	}

	public boolean competitorDeservedDmgIsGreater()
	{
		return competitor.getDeservedDamage() > opponent.getDeservedDamage();
	}

	public boolean opponentDeservedDmgIsGreater()
	{
		return opponent.getDeservedDamage() > competitor.getDeservedDamage();
	}

	public boolean competitorDmgDealtIsGreater()
	{
		return competitor.getDamageDealt() > opponent.getDamageDealt();
	}

	public boolean opponentDmgDealtIsGreater()
	{
		return opponent.getDamageDealt() > competitor.getDamageDealt();
	}

	public boolean competitorMagicHitsLuckier()
	{
		double competitorRate = (competitor.getMagicHitCountDeserved() == 0) ? 0 :
			(competitor.getMagicHitCount() / competitor.getMagicHitCountDeserved());
		double opponentRate = (opponent.getMagicHitCountDeserved() == 0) ? 0 :
			(opponent.getMagicHitCount() / opponent.getMagicHitCountDeserved());

		return competitorRate > opponentRate;
	}

	public boolean opponentMagicHitsLuckier()
	{
		double competitorRate = (competitor.getMagicHitCountDeserved() == 0) ? 0 :
			(competitor.getMagicHitCount() / competitor.getMagicHitCountDeserved());
		double opponentRate = (opponent.getMagicHitCountDeserved() == 0) ? 0 :
			(opponent.getMagicHitCount() / opponent.getMagicHitCountDeserved());

		return opponentRate > competitorRate;
	}

	public double getCompetitorDeservedDmgDiff()
	{
		return competitor.getDeservedDamage() - opponent.getDeservedDamage();
	}

	public double getCompetitorDmgDealtDiff()
	{
		return competitor.getDamageDealt() - opponent.getDamageDealt();
	}

	// use to sort by last fight time, to sort fights by date/time.
	@Override
	public int compareTo(FightPerformance o)
	{
		long diff = lastFightTime - o.lastFightTime;

		// if diff = 0, return 0. Otherwise, divide diff by its absolute value. This will result in
		// -1 for negative numbers, and 1 for positive numbers, keeping the sign and a safely small int.
		return diff == 0 ? 0 :
			(int)(diff / Math.abs(diff));
	}

	/**
	 * Calculates robe hits for both competitors based on defender gear in each log entry and the configured filter.
	 */
	public void calculateRobeHits(PvpPerformanceTrackerConfig.RobeHitFilter filter)
	{
		competitor.resetRobeHits();
		opponent.resetRobeHits();
		ArrayList<FightLogEntry> allFightLogEntries	= getAllFightLogEntries();
		if (filter == null || allFightLogEntries == null || allFightLogEntries.isEmpty())
		{
			return;
		}

		for (FightLogEntry entry : allFightLogEntries)
		{
			if (entry == null)
			{
				continue;
			}

			// Only consider melee and ranged attacks for robe hits
			AnimationData ad = entry.getAnimationData();
			if (ad == null)
			{
				continue;
			}
			AttackStyle style = ad.attackStyle;
			if (style == AttackStyle.MAGIC)
			{
				continue;
			}

			// Determine who the defender is for this specific entry
			Fighter defender = entry.getAttackerName().equals(competitor.getName()) ? opponent : competitor;
			if (defender == null)
			{
				continue;
			}

			int[] gear = entry.getDefenderGear();
			if (gear == null || gear.length <= Math.max(KitType.LEGS.getIndex(), KitType.TORSO.getIndex()))
			{
				continue;
			}

			int defenderLegsItemId = fixItemId(gear[KitType.LEGS.getIndex()]);
			int defenderBodyItemId = fixItemId(gear[KitType.TORSO.getIndex()]);

			// Compute stats for robe bottom/top
			boolean wearingRobeBottom = false;
			if (defenderLegsItemId != 0)
			{
				int[] legStats = PvpDamageCalc.getItemStats(defenderLegsItemId);
				if (legStats != null && legStats.length > RANGE_DEF)
				{
					wearingRobeBottom = legStats[RANGE_DEF] <= 0; // ==0 seems reliable but <=0 doesn't hurt
				}
			}

			boolean wearingRobeTop = false;
			if (defenderBodyItemId != 0)
			{
				int[] bodyStats = PvpDamageCalc.getItemStats(defenderBodyItemId);
				if (bodyStats != null && bodyStats.length > RANGE_DEF)
				{
					wearingRobeTop = bodyStats[RANGE_DEF] <= 0; // ==0 seems reliable but <=0 doesn't hurt
				}
			}

			boolean isHitOnRobes = false;
			switch (filter)
			{
				case BOTTOM:
					isHitOnRobes = wearingRobeBottom;
					break;
				case TOP:
					isHitOnRobes = wearingRobeTop;
					break;
				case BOTH:
					isHitOnRobes = wearingRobeBottom && wearingRobeTop;
					break;
				case EITHER:
					isHitOnRobes = wearingRobeBottom || wearingRobeTop;
					break;
			}

			if (isHitOnRobes)
			{
				if (defender == competitor)
				{
					competitor.addRobeHit();
				}
				else if (defender == opponent)
				{
					opponent.addRobeHit();
				}
			}
		}
	}

    public void updateKoChanceStats(FightLogEntry entry)
	{
		if (entry.getDisplayKoChance() == null) { return; }

		double koChance = entry.getDisplayKoChance();

		if (entry.getAttackerName().equals(competitor.getName()))
		{
			competitorKoChanceCount++;
			competitorLastKoChance = koChance;
			competitorSurvivalProb *= (1.0 - koChance);
			competitorTotalKoChance = 1.0 - competitorSurvivalProb;
		}
		else
		{
			opponentKoChanceCount++;
			opponentLastKoChance = koChance;
			opponentSurvivalProb *= (1.0 - koChance);
			opponentTotalKoChance = 1.0 - opponentSurvivalProb;
		}
	}
}