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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.models.FightLogEntry;

@Slf4j
public class AnalyzedFightPerformance extends FightPerformance
{
	// fight's full fight logs, saved in "pairs", as an array:
	// [0]: attacker's full log entry
	// [1]: defender's defensive log entry
	@Getter
	ArrayList<FightLogEntry[]> analyzedMatchingLogs;

	@Getter
	FightPerformance mainFight;
	@Getter
	FightPerformance opposingFight;
	// create a more detailed fight performance by merging data from two opposing fight logs
	// also include the fights for easier access to general info
	public AnalyzedFightPerformance(FightPerformance mainFight, FightPerformance opposingFight, Runnable swingCallback) throws Exception
	{
		this.mainFight = mainFight;
		this.opposingFight = opposingFight;
		String cName = mainFight.competitor.getName();
		String oName = mainFight.opponent.getName();
		this.competitor = new Fighter(mainFight, cName);
		this.opponent = new Fighter(mainFight, oName);
		if (mainFight.competitor.isDead())
		{
			this.competitor.died();
		}
		if (mainFight.opponent.isDead())
		{
			this.opponent.died();
		}
		this.lastFightTime = Math.max(mainFight.lastFightTime, opposingFight.lastFightTime);

		// before looping through logs, set "global"/constant values that won't change depending on dps
		// calculations: successful magic hits, actual damage dealt, and hp healed.
		// in case either fighter somehow missed an attack, use the max potential available data
		// detected, as we could rarely miss an attack but never add an extra one.
		this.competitor.addDamageDealt(Math.max(mainFight.competitor.getDamageDealt(), opposingFight.opponent.getDamageDealt()));
		this.competitor.addMagicHitCount(Math.max(mainFight.competitor.getMagicHitCount(), opposingFight.opponent.getMagicHitCount()));
		this.competitor.addHpHealed(mainFight.competitor.getHpHealed());
		this.competitor.setTotalGhostBarrageStats(mainFight.competitor.getGhostBarrageCount(), mainFight.getCompetitor().getGhostBarrageDeservedDamage());

		this.opponent.addDamageDealt(Math.max(opposingFight.competitor.getDamageDealt(), mainFight.opponent.getDamageDealt()));
		this.opponent.addMagicHitCount(Math.max(opposingFight.competitor.getMagicHitCount(), mainFight.opponent.getMagicHitCount()));
		this.opponent.addHpHealed(opposingFight.competitor.getHpHealed());
		this.opponent.setTotalGhostBarrageStats(opposingFight.competitor.getGhostBarrageCount(), opposingFight.getCompetitor().getGhostBarrageDeservedDamage());

		ArrayList<FightLogEntry> mainFightLogEntries = mainFight.getAllFightLogEntries();
		mainFightLogEntries.sort(FightLogEntry::compareTo);
		ArrayList<FightLogEntry> opponentFightLogEntries = opposingFight.getAllFightLogEntries();
		opponentFightLogEntries.sort(FightLogEntry::compareTo);

		// save only full entries into separate arrays, as we'll loop through those a lot.
		ArrayList<FightLogEntry> fullMainFightLogEntries = mainFightLogEntries.stream()
			.filter(FightLogEntry::isFullEntry).sorted().collect(Collectors.toCollection(ArrayList::new));
		ArrayList<FightLogEntry> fullOpponentFightLogEntries = opponentFightLogEntries.stream()
			.filter(FightLogEntry::isFullEntry).sorted().collect(Collectors.toCollection(ArrayList::new));

		int offsetsToCheck = 2; // total number of fight log offsets to start from and find matches in the opposing fight.
		int attacksToCheck = 12; // total number of opposing logs to check starting from the offset.
		// TODO: THIS SHOULDNT BREAK IF attacksToCheck IS 300+ or unused & using all attacks/logs ????
		// ^ or maybe we dont have specific enough data to have it working 100% this way?

		// save matching logs as "pairs", in an array:
		// [0] = main fight's log entry match
		// [1] = opposing fight's log entry match
		// for each different offset start (from mainFight), save an array of the matching logs.
		ArrayList<ArrayList<FightLogEntry[]>> matchingLogs = new ArrayList<>();


		// go through up to the first 5 full logs to match with the opponent's first 6 attacks.
		// do this with a few different offsets and go with the results that had the most matches in 6 attacks.
		//
		// Why multiple offsets rather than one pass, or setting starting tick to 0?
		// It's possible one of the clients misses the first attack, and that people use the same gear
		// for many attacks, which may cause logs to match each-other while being offset, causing future
		// attacks to often be missed, and always be misleading. This way, if initial attacks are missed
		for (int mainOffset = 0; mainOffset < offsetsToCheck; mainOffset++)
		{
			for (int oppOffset = 0; oppOffset < offsetsToCheck; oppOffset++)
			{
				ArrayList<FightLogEntry[]> currentOffsetMatches = new ArrayList<>();
				int highestMatchIdx = -1;
				for (int i = mainOffset; i < fullMainFightLogEntries.size() && i < attacksToCheck; i++)
				{
					FightLogEntry entry = fullMainFightLogEntries.get(i);

					// .skip: do not check for matches on an attack we already got a match for, so skip it.
					//     if the highestMatchIdx is still <0, no matches were found yet so keep checking with oppOffset.
					int skip = highestMatchIdx < 0 ? oppOffset : highestMatchIdx + 1;
					// .limit: similarly to .skip, reduce the amount of attacks we check by the amount we skipped.
					// .filter: find matching fight log entries from the opposing fight.
					// do not use dps calc values for comparison as they can be different depending on each player's
					// config. could potentially fix this by recalculating fights first, but comparing all of these
					// should be ok enough. if gear and pray is the same, then so would dps anyways (before we do
					// the proper brew/level merge we are currently doing)
					FightLogEntry matchingOppLog = fullOpponentFightLogEntries.stream()
						.skip(skip)
						.limit(attacksToCheck - skip)
						.filter(oppEntry -> entry.attackerName.equals(oppEntry.attackerName) &&
							entry.getAnimationData() == oppEntry.getAnimationData() &&
							Arrays.equals(entry.getAttackerGear(), oppEntry.getAttackerGear()) &&
							Arrays.equals(entry.getDefenderGear(), oppEntry.getDefenderGear()) &&
							entry.getAttackerOverhead() == oppEntry.getAttackerOverhead() &&
							entry.getDefenderOverhead() == oppEntry.getDefenderOverhead() &&
							entry.success() == oppEntry.success() &&
							entry.isSplash() == oppEntry.isSplash())
						.findFirst()
						.orElse(null);

					// if a match was found, save the index of the match, and add both entries to the current offset matches.
					if (matchingOppLog != null)
					{
						int logEntryIdx = fullOpponentFightLogEntries.indexOf(matchingOppLog);

						if (logEntryIdx <= highestMatchIdx) // this should never happen.
						{
							throw new Exception("Invalid state during fight merge: logEntryIdx was under highestMatchIdx");
						}

						highestMatchIdx = logEntryIdx;
						currentOffsetMatches.add(new FightLogEntry[]{entry, matchingOppLog});
					}
				}

				if (currentOffsetMatches.size() >= 2)
				{
					matchingLogs.add(currentOffsetMatches);
				}
			}
		}

		if (matchingLogs.size() < 1)
		{
			throw new Exception("Unable to match initial attacks for fight analysis.");
		}

		// get the array of log entry matches that has the highest number of matches, as it should be the most accurate.
		// we'll use the matches in this array to determine the tick offset between the two sets of logs.
		// sort log matches by size, meaning most matches first.
		matchingLogs.sort(Comparator.comparing(ArrayList<FightLogEntry[]>::size).reversed());

		// ensure the tick difference found is valid: consistent difference between the first few matching attacks found.
		// this could potentially somehow still accept unrelated/invalid fights, as it could accept a single attack match,
		// but it should properly merge actual matching fights
		int bestTickDiff = 0;
		boolean foundValidTickDiff = false;
		for (ArrayList<FightLogEntry[]> logMatches : matchingLogs)
		{
			int tickDiff = 0;
			boolean tickDiffValid = true;
			for (int i = 0; i < logMatches.size(); i++)
			{
				FightLogEntry[] match = logMatches.get(i);

				int curTickDiff = match[0].getTick() - match[1].getTick();

				// ensure we have a consistent tick difference between the first few matching attacks.
				if (i == 0)
				{
					tickDiff = curTickDiff;
					continue;
				}

				if (curTickDiff != tickDiff)
				{
					tickDiffValid = false;
					break;
				}
			}

			if (tickDiffValid)
			{
				bestTickDiff = tickDiff;
				foundValidTickDiff = true;
				break;
			}
		}

		if (!foundValidTickDiff)
		{
			throw new Exception("Could not find matching initial attack logs in order to merge fights.");
		}

		// now that we have the best tick diff, we're ready to actually merge the two sets of log entries.
		// start by adjusting the opponent's logs' ticks so they line up with the main fight.
		for (FightLogEntry log : opponentFightLogEntries)
		{
			log.setTick(log.getTick() + bestTickDiff);
		}

		analyzedMatchingLogs = new ArrayList<>();

		// now that all the ticks should be lined up on the opponent fight log entries, find matching tick pairs for offensive : defensive logs.
		// skip the 'main' client's opponent logs, as those are the ones with less data we are trying to improve by using the opponent's data.
		// so, only loop through the main competitor's logs. We will add opponent attacks when we detect a defensive log.
		PLUGIN.getClientThread().invokeLater(() ->
		{
			mainFightLogEntries.stream()
				.filter(log -> log.attackerName.equals(mainFight.competitor.getName()))
				.forEachOrdered(log -> {
					// if the log is a full entry, then this is an attacking log coming from the competitor,
					// so we need to find a matching defensive log from the opponent.
					if (log.isFullEntry())
					{
						opponentFightLogEntries.stream()
							.filter(ol -> ol.getTick() == log.getTick())
							.filter(ol -> !ol.isFullEntry())
							.filter(ol -> ol.attackerName.equals(mainFight.opponent.getName()))
							.findFirst() // when a match is finally found, add the attack.
							.ifPresent(matchingDefenderLog -> addCompetitorAttack(log, matchingDefenderLog));
					}
					else // if the log is not a full entry, it's a defensive log coming from the competitor,
					{    // meaning we need to match it to an opponent's attacking log.
						opponentFightLogEntries.stream()
							.filter(ol -> ol.getTick() == log.getTick())
							.filter(FightLogEntry::isFullEntry)
							.filter(ol -> ol.attackerName.equals(mainFight.opponent.getName()))
							.findFirst() // when a match is finally found, add the attack.
							.ifPresent(matchingAttackerLog -> addOpponentAttack(matchingAttackerLog, log));
					}
				});

			SwingUtilities.invokeLater(swingCallback);
		});
	}

	void addCompetitorAttack(FightLogEntry attackerLog, FightLogEntry defenderLog)
	{
			this.competitor.addAttack(attackerLog, defenderLog);
			this.analyzedMatchingLogs.add(new FightLogEntry[]{ attackerLog, defenderLog });
	}

	void addOpponentAttack(FightLogEntry attackerLog, FightLogEntry defenderLog)
	{
			this.opponent.addAttack(attackerLog, defenderLog);
			this.analyzedMatchingLogs.add(new FightLogEntry[]{attackerLog, defenderLog});
	}
}
