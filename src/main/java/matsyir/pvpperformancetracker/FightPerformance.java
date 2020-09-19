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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.AnimationID;
import net.runelite.api.Player;
import net.runelite.client.game.ItemManager;

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

	// constructor which initializes a fight from the 2 Players, starting stats at 0.
	FightPerformance(Player competitor, Player opponent, ItemManager itemManager)
	{
		this.competitor = new Fighter(competitor, itemManager);
		this.opponent = new Fighter(opponent, itemManager);

		// this is initialized soon before the NEW_FIGHT_DELAY time because the event we
		// determine the opponent from is not fully reliable.
		lastFightTime = Instant.now().minusSeconds(NEW_FIGHT_DELAY.getSeconds() - 5).toEpochMilli();
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
	void checkForAttackAnimations(String playerName)
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
				int pray = PvpPerformanceTrackerPlugin.PLUGIN.currentlyUsedOffensivePray();
				competitor.addAttack(
					opponent.getPlayer().getOverheadIcon() != animationData.attackStyle.getProtection(),
					opponent.getPlayer(),
					animationData,
					pray);
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