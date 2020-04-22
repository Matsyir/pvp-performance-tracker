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
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.AnimationID;
import net.runelite.api.Player;
import net.runelite.client.game.ItemManager;

// Basic class to hold information about PvP fight performances. A "successful" attack
// is dealt by not attacking with the style of the opponent's overhead. For example,
// attacking with range or melee against someone using protect from magic is a successful
// attack. Using melee against someone using protect from melee is not successful.
// This is not a guaranteed way of determining the better competitor, since someone casting
// barrage in full tank gear can count as a successful attack. It's a good general idea, though.
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
	private Fighter competitor;
	@Expose
	private Fighter opponent;
	@Expose
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

		return new FightPerformance("Matsyir", "TEST_DATA", cSuccess, cTotal, cDamage, oSuccess, oTotal, oDamage, cDead, secOffset);
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
	private FightPerformance(String cName, String oName, int cSuccess, int cTotal, double cDamage, int oSuccess, int oTotal, double oDamage, boolean cDead, int secondOffset)
	{
		this.competitor = new Fighter(cName);
		this.opponent = new Fighter(oName);

		competitor.addAttacks(cSuccess, cTotal, cDamage);
		opponent.addAttacks(oSuccess, oTotal, oDamage);

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
		
		if (playerName.equals(competitor.getName()))
		{
			AnimationAttackStyle attackStyle = competitor.getAnimationAttackStyle();
			AnimationAttackType animationType = competitor.getAnimationAttackType();
			if (attackStyle != null)
			{
				competitor.addAttack(opponent.getPlayer().getOverheadIcon() != attackStyle.getProtection(), opponent.getPlayer(), animationType);
				lastFightTime = Instant.now().toEpochMilli();
			}
		}
		else if (playerName.equals(opponent.getName()))
		{
			AnimationAttackStyle attackStyle = opponent.getAnimationAttackStyle();
			AnimationAttackType animationType = opponent.getAnimationAttackType();
			if (attackStyle != null)
			{
				opponent.addAttack(competitor.getPlayer().getOverheadIcon() != attackStyle.getProtection(), competitor.getPlayer(), animationType);
				lastFightTime = Instant.now().toEpochMilli();
			}
		}
	}

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

	// returns true if competitor off-pray hit success rate > opponent success rate.
	// the following functions have similar behaviour.
	boolean competitorOffPraySuccessIsGreater()
	{
		return competitor.calculateSuccessPercentage() > opponent.calculateSuccessPercentage();
	}

	boolean opponentOffPraySuccessIsGreater()
	{
		return opponent.calculateSuccessPercentage() > competitor.calculateSuccessPercentage();
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

	// only count the fight as started if the competitor attacked, not the enemy because
	// the person the competitor clicked on might be attacking someone else
	boolean fightStarted()
	{
		return competitor.getAttackCount() > 0;
	}

	// get full value of deserved dmg as well as difference, for the competitor
	public String getCompetitorDeservedDmgString()
	{
		int difference = (int)Math.round(competitor.getDeservedDamage() - opponent.getDeservedDamage());
		return (int)Math.round(competitor.getDeservedDamage()) + " (" + (difference > 0 ? "+" : "") + difference + ")";
	}

	// get full value of deserved dmg as well as difference, for the opponent
	public String getOpponentDeservedDmgString()
	{
		int difference = (int)Math.round(opponent.getDeservedDamage() - competitor.getDeservedDamage());
		return (int)Math.round(opponent.getDeservedDamage()) + " (" + (difference > 0 ? "+" : "") + difference + ")";
	}

	// get full value of deserved dmg as well as difference, for the competitor. 1 decimal space.
	public String getLongCompetitorDeservedDmgString()
	{
		double difference = competitor.getDeservedDamage() - opponent.getDeservedDamage();
		String differenceStr = nf.format(difference);
		return nf.format(competitor.getDeservedDamage()) + " (" + (difference > 0 ? "+" : "") + differenceStr + ")";
	}

	// get full value of deserved dmg as well as difference, for the opponent
	public String getLongOpponentDeservedDmgString()
	{
		double difference = opponent.getDeservedDamage() - competitor.getDeservedDamage();
		String differenceStr = nf.format(difference);
		return nf.format(opponent.getDeservedDamage()) + " (" + (difference > 0 ? "+" : "") + differenceStr + ")";
	}

	public double getCompetitorDeservedDmgDiff()
	{
		return competitor.getDeservedDamage() - opponent.getDeservedDamage();
	}

	@Override
	public int compareTo(FightPerformance o)
	{
		long diff = lastFightTime - o.lastFightTime;

		// if diff = 0, return 0. Otherwise, divide diff by its absolute value. This will result in
		// -1 for negative numbers, and 1 for positive numbers, keeping the sign and safely a small int.
		return diff == 0 ? 0 :
			(int)(diff / Math.abs(diff));
	}
}
