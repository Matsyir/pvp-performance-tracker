/*
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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.api.AnimationID;
import net.runelite.api.Player;
import net.runelite.api.Skill;

// Holds two Fighters which contain data about PvP fight performance, and has many methods to
// add to the fight, display stats or check the status of the fight.
@Slf4j
@Getter
public class FightPerformance implements Comparable<FightPerformance>
{
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
	private Fighter competitor;
	@Expose
	@SerializedName("o")
	private Fighter opponent;
	@Expose
	@SerializedName("t")
	private long lastFightTime; // last fight time saved as epochMilli timestamp (serializing an Instant was a bad time)

	private int competitorPrevHp; // intentionally don't serialize this, temp variable used to calculate hp healed.

	// constructor which initializes a fight from the 2 Players, starting stats at 0. Regular use constructor.
	FightPerformance(Player competitor, Player opponent)
	{
		this.competitor = new Fighter(competitor);
		this.opponent = new Fighter(opponent);

		// this is initialized soon before the NEW_FIGHT_DELAY time because the event we
		// determine the opponent from is not fully reliable.
		lastFightTime = Instant.now().minusSeconds(NEW_FIGHT_DELAY.getSeconds() - 5).toEpochMilli();

		this.competitorPrevHp = PLUGIN.client.getBoostedSkillLevel(Skill.HITPOINTS);
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

	// create a more detailed fight performance by merging data from two opposing fight logs
	// also include the fights for easier access to general info
	///////// OLD COMMENT: the fight log entry lists should only include one player in each
	FightPerformance(FightPerformance mainFight, FightPerformance opposingFight) throws Exception
	{
		String cName = mainFight.competitor.getName();
		String oName = mainFight.opponent.getName();
		this.competitor = new Fighter(cName);
		this.opponent = new Fighter(oName);
		this.lastFightTime = Math.max(mainFight.lastFightTime, opposingFight.lastFightTime);

		// before looping through logs, set "global"/constant values that won't change depending on dps
		// calculations: successful magic hits, actual damage dealt, and hp healed.
		// in case either fighter somehow missed an attack, use the max potential available data
		// detected, as we could rarely miss an attack but never add an extra one.
		this.competitor.addDamageDealt(Math.max(mainFight.competitor.getDamageDealt(), opposingFight.opponent.getDamageDealt()));
		this.competitor.addMagicHitCount(Math.max(mainFight.competitor.getMagicHitCount(), opposingFight.opponent.getMagicHitCount()));
		this.competitor.addHpHealed(mainFight.competitor.getHpHealed());

		this.opponent.addDamageDealt(Math.max(opposingFight.competitor.getDamageDealt(), mainFight.opponent.getDamageDealt()));
		this.opponent.addMagicHitCount(Math.max(opposingFight.competitor.getMagicHitCount(), mainFight.opponent.getMagicHitCount()));
		this.opponent.addHpHealed(opposingFight.competitor.getHpHealed());

		ArrayList<FightLogEntry> mainFightLogEntries = mainFight.getAllFightLogEntries();
		ArrayList<FightLogEntry> opponentFightLogEntries = opposingFight.getAllFightLogEntries();

		// save only full entries into separate arrays, as we'll loop through those a lot.
		ArrayList<FightLogEntry> fullMainFightLogEntries = mainFightLogEntries.stream()
			.filter(FightLogEntry::isFullEntry).collect(Collectors.toCollection(ArrayList::new));
		ArrayList<FightLogEntry> fullOpponentFightLogEntries = opponentFightLogEntries.stream()
			.filter(FightLogEntry::isFullEntry).collect(Collectors.toCollection(ArrayList::new));

		int offsetsToCheck = 4; // total number of mainFight offsets to start from and find matches in the opposing fight.
		int attacksToCheck = 6; // total number of opposing attacks to check starting from the offset.

		// save matching logs as "pairs", in an array:
		// [0] = main fight's log entry match
		// [1] = opposing fight's log entry match
		// for each different offset start (from mainFight), save an array of the matching logs.
		ArrayList<ArrayList<FightLogEntry[]>> matchingLogs = new ArrayList<>();


		// go through up to the first 5 full logs to match with the opponent's first 5 attacks.
		// do this with a few different offsets and go with the results that had the most matches in 5 attacks.
		for (int offset = 0; offset < offsetsToCheck; offset++)
		{
			ArrayList<FightLogEntry[]> currentOffsetMatches = new ArrayList<>();
			int highestMatchIdx = -1;
			for (int i = offset; i < fullMainFightLogEntries.size() && i < attacksToCheck; i++)
			{
				FightLogEntry entry = fullMainFightLogEntries.get(i);

				// .skip: do not check for matches on an attack we already got a match for, so skip it.
				// .limit: similarly, reduce the amount of attacks we check by the amount we skipped.
				// .filter: find matching fight log entries from the opposing fight.
				// do not use dps calc values for comparison as they can be different depending on each player's
				// config. could potentially fix this by recalculating fights first, but comparing all of these
				// should be ok enough. if gear and pray is the same, then so would dps anyways (before we do
				// the proper brew/level merge we are currently doing)
				FightLogEntry matchingOppLog = fullOpponentFightLogEntries.stream()
					.skip(highestMatchIdx + 1)
					.limit(attacksToCheck - (highestMatchIdx + 1))
					.filter(oppEntry -> entry.attackerName.equals(oppEntry.attackerName) &&
						entry.getAnimationData() == oppEntry.getAnimationData() &&
						Arrays.equals(entry.getAttackerGear(), oppEntry.getAttackerGear()) &&
						Arrays.equals(entry.getDefenderGear(), oppEntry.getDefenderGear()) &&
						entry.getAttackerOverhead() == oppEntry.getAttackerOverhead() &&
						entry.getDefenderOverhead() == oppEntry.getDefenderOverhead() &&
						entry.success() == oppEntry.success() &&
						entry.isSplash() == oppEntry.isSplash())
					//.forEach(matchingOppLog -> currentOffsetMatches.add(new Pair<>(entry, matchingOppLog)));
					.findFirst()
					.orElse(null);

				// if a match was found, save the index of the match, and add both entries to the current offset matches.
				if (matchingOppLog != null)
				{
					int logEntryIdx = fullOpponentFightLogEntries.indexOf(matchingOppLog);

					if (logEntryIdx < highestMatchIdx) // this should never happen.
					{
						throw new Exception("I fucked up");
					}

					highestMatchIdx = logEntryIdx;
					currentOffsetMatches.add(new FightLogEntry[]{ entry, matchingOppLog });
				}
			}

			if (currentOffsetMatches.size() > 0)
			{
				matchingLogs.add(currentOffsetMatches);
			}
		}


		if (matchingLogs.size() < 1)
		{
			throw new Exception("Unable to match initial attacks for fight analysis.");
		}

		// sort log matches by size, meaning most matches.
		matchingLogs.sort(Comparator.comparing(ArrayList<FightLogEntry[]>::size).reversed());
		// get the array of log entry matches that has the highest number of matches, as it should be the most accurate.
		// we'll use the matches in this array to determine the tick offset between the two sets of logs.
//		ArrayList<FightLogEntry[]> bestMatchPairs =
//			matchingLogs.stream().max(Comparator.comparing(ArrayList::size)).get();


		//ArrayList<FightLogEntry[]> bestMatchPairs = null;
		int bestTickDiff = 0;
		boolean foundValidTickDiff = false;
		for (ArrayList<FightLogEntry[]> logMatches : matchingLogs)
		{
			int tickDiff = 0;
			boolean tickDiffValid = false;
			for (int i = 0; i < logMatches.size(); i++)
			{
				FightLogEntry[] match = logMatches.get(i);

				int curTickDiff = match[0].getTick() - match[1].getTick();

				// ensure we have a consistent tick difference between the first few matching attacks.
				if (i == 0)
				{
					tickDiff = curTickDiff;
					tickDiffValid = true;
				}
				else if (curTickDiff != tickDiff)
				{
					tickDiffValid = false;
					break;
				}
			}

			if (tickDiffValid)
			{
				//bestMatchPairs = logMatches;
				bestTickDiff = tickDiff;
				foundValidTickDiff = true;
			}
		}

		if (!foundValidTickDiff)
		{
			throw new Exception("Could not find matching attacks to merge fights.");
		}

		// now that we have the best tick diff, we're ready to actually merge the two sets of log entries.
		// start by adjusting the opponent's logs' ticks so they line up with the main fight.
		for (FightLogEntry log : opponentFightLogEntries)
		{
			log.setTick(log.getTick() + bestTickDiff);
		}

		// TODO MERGE BY TICKS INTO PAIRS.
		// now that all the ticks should be lined up, find matching tick pairs for offensive : defensive logs.


		// the main fight's defensive logs, meaning logs that will go with the opponent's attack logs
//		ArrayList<FightLogEntry> mainFightDefensiveLogs = mainFightLogEntries.stream()
//			.filter(l -> !l.isFullEntry() && l.attackerName.equals(mainFight.competitor.getName()))
//			.collect(Collectors.toCollection(ArrayList::new));
//		ArrayList<FightLogEntry> opponentFightDefensiveLogs = opponentFightLogEntries.stream()
//			.filter(l -> !l.isFullEntry() && l.attackerName.equals(mainFight.competitor.getName()))
//			.collect(Collectors.toCollection(ArrayList::new));


		for (FightLogEntry log : mainFightLogEntries)
		{
			if (!log.attackerName.equals(mainFight.competitor.getName())) { continue; }

			// if the log is a full entry, then this is an attacking log coming from the competitor,
			// so we need to find a matching defensive log from the opponent.
			if (log.isFullEntry())
			{
				opponentFightLogEntries.stream()
					.filter(l -> !l.isFullEntry())
					.filter(l -> l.attackerName.equals(mainFight.opponent.getName()))
					.filter(l -> l.getTick() == log.getTick())
					.findFirst()
					.ifPresent(matchingDefenderLog -> this.competitor.addAttack(log, matchingDefenderLog));
			}
			else // if the log is not a full entry, it's a defensive log coming from the competitor,
			{    // meaning we need to match it to an opponent's attacking log.
				fullOpponentFightLogEntries.stream()
					.filter(l -> l.attackerName.equals(mainFight.opponent.getName()))
					.filter(l -> l.getTick() == log.getTick())
					.findFirst()
					.ifPresent(matchingAttackerLog -> this.opponent.addAttack(matchingAttackerLog, log));
			}
		}


//		ArrayList<FightLogEntry> allFightLogEntries = new ArrayList<>(mainFightLogEntries);
//		allFightLogEntries.addAll(opponentFightLogEntries);

		// TODO ACTUALLY ADD LEVELS & PRAY TO DPS
//		for (FightLogEntry logEntry : allFightLogEntries)
//		{
//			if (logEntry.attackerName.equals(cName))
//			{
//				competitor.addAttack(logEntry, defenderLog);
//			}
//			else if (logEntry.attackerName.equals(oName))
//			{
//				opponent.addAttack(logEntry, defenderLog);
//			}
//		}
	}

	// Used for testing purposes
	private FightPerformance(String cName, String oName, int cSuccess, int cTotal, double cDamage, int oSuccess, int oTotal, double oDamage, boolean cDead, int secondOffset, ArrayList<FightLogEntry> fightLogs)
	{
		this.competitor = new Fighter(cName, fightLogs);
		this.opponent = new Fighter(oName, fightLogs);

		competitor.addAttacks(cSuccess, cTotal, cDamage, (int)cDamage, 12, 13, 11);
		opponent.addAttacks(oSuccess, oTotal, oDamage, (int)oDamage, 14, 13, 11);

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
	void checkForAttackAnimations(String playerName, CombatLevels competitorLevels)
	{
		if (playerName == null)
		{
			return;
		}

		// verify that the player is interacting with their tracked opponent before adding attacks
		if (playerName.equals(competitor.getName()) && opponent.getPlayer().equals(competitor.getPlayer().getInteracting()))
		{
			AnimationData animationData = competitor.getAnimationData();
			if (animationData != null)
			{
				int pray = PLUGIN.currentlyUsedOffensivePray();
				competitor.addAttack(
					opponent.getPlayer().getOverheadIcon() != animationData.attackStyle.getProtection(),
					opponent.getPlayer(),
					animationData,
					pray,
					competitorLevels);
				lastFightTime = Instant.now().toEpochMilli();
			}
		}
		else if (playerName.equals(opponent.getName()) && competitor.getPlayer().equals(opponent.getPlayer().getInteracting()))
		{
			AnimationData animationData = opponent.getAnimationData();
			if (animationData != null)
			{
				// there is no offensive prayer data for the opponent so hardcode 0
				opponent.addAttack(competitor.getPlayer().getOverheadIcon() != animationData.attackStyle.getProtection(),
					competitor.getPlayer(), animationData, 0);
				competitor.addDefensiveLogs(competitorLevels, PLUGIN.currentlyUsedOffensivePray());
				lastFightTime = Instant.now().toEpochMilli();
			}
		}
	}

	// add damage dealt to the opposite player.
	// the player name being passed in is the one who has the hitsplat on them.
	void addDamageDealt(String playerName, int damage)
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

	void updateCompetitorHp(int currentHp)
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
	boolean isFightOver()
	{
		boolean isOver = false;
		// if either competitor died, end the fight.
		if (opponent.getPlayer().getAnimation() == AnimationID.DEATH)
		{
			opponent.died();
			isOver = true;
		}
		if (competitor.getPlayer().getAnimation() == AnimationID.DEATH)
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

	ArrayList<FightLogEntry> getAllFightLogEntries()
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

	// only count the fight as started if the competitor attacked, not the enemy because
	// the person the competitor clicked on might be attacking someone else
	boolean fightStarted()
	{
		return competitor.getAttackCount() > 0;
	}

	// returns true if competitor off-pray hit success rate > opponent success rate.
	// the following functions have similar behaviour.
	boolean competitorOffPraySuccessIsGreater()
	{
		return competitor.calculateOffPraySuccessPercentage() > opponent.calculateOffPraySuccessPercentage();
	}

	boolean opponentOffPraySuccessIsGreater()
	{
		return opponent.calculateOffPraySuccessPercentage() > competitor.calculateOffPraySuccessPercentage();
	}

	boolean competitorDeservedDmgIsGreater()
	{
		return competitor.getDeservedDamage() > opponent.getDeservedDamage();
	}

	boolean opponentDeservedDmgIsGreater()
	{
		return opponent.getDeservedDamage() > competitor.getDeservedDamage();
	}

	boolean competitorDmgDealtIsGreater()
	{
		return competitor.getDamageDealt() > opponent.getDamageDealt();
	}

	boolean opponentDmgDealtIsGreater()
	{
		return opponent.getDamageDealt() > competitor.getDamageDealt();
	}

	boolean competitorMagicHitsLuckier()
	{
		double competitorRate = (competitor.getMagicHitCountDeserved() == 0) ? 0 :
			(competitor.getMagicHitCount() / competitor.getMagicHitCountDeserved());
		double opponentRate = (opponent.getMagicHitCountDeserved() == 0) ? 0 :
			(opponent.getMagicHitCount() / opponent.getMagicHitCountDeserved());

		return competitorRate > opponentRate;
	}

	boolean opponentMagicHitsLuckier()
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
}