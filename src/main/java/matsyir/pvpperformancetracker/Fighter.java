/*
 * Copyright (c)  2020, Matsyir <https://github.com/Matsyir>
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
import java.util.ArrayList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GraphicID;
import net.runelite.api.Player;
import net.runelite.client.game.ItemManager;

@Slf4j
@Getter
class Fighter
{
	private static final NumberFormat nf = NumberFormat.getInstance();
	static // initialize number format
	{
		nf.setMaximumFractionDigits(1);
		nf.setRoundingMode(RoundingMode.HALF_UP);
	}

	private Player player;
	@Expose
	@SerializedName("n") // use 1 letter serialized variable names for more compact storage
	private String name; // username
	@Expose
	@SerializedName("a")
	private int attackCount; // total number of attacks
	@Expose
	@SerializedName("s")
	private int successCount; // total number of successful attacks
	@Expose
	@SerializedName("d")
	private double deservedDamage; // total deserved damage based on gear & opponent's pray
	@Expose
	@SerializedName("h") // h for "hitsplats", real hits
	private int damageDealt;
	@Expose
	@SerializedName("m")
	private int magicHitCount;
	@Expose
	@SerializedName("M")
	private double magicHitCountDeserved;
	@Expose
	@SerializedName("x") // x for X_X
	private boolean dead; // will be true if the fighter died in the fight

	private ArrayList<FightLogEntry> fightLogEntries;

	private PvpDamageCalc pvpDamageCalc;

	// fighter that is bound to a player and gets updated during a fight
	Fighter(Player player, ItemManager itemManager)
	{
		this.player = player;
		name = player.getName();
		attackCount = 0;
		successCount = 0;
		deservedDamage = 0;
		damageDealt = 0;
		magicHitCount = 0;
		magicHitCountDeserved = 0;
		dead = false;
		pvpDamageCalc = new PvpDamageCalc(itemManager);
		fightLogEntries = new ArrayList<>();
	}

	// fighter that is bound to a player and gets updated during a fight
	Fighter(String name, ArrayList<FightLogEntry> logs)
	{
		player = null;
		this.name = name;
		attackCount = 0;
		successCount = 0;
		deservedDamage = 0;
		damageDealt = 0;
		magicHitCount = 0;
		magicHitCountDeserved = 0;
		dead = false;
		fightLogEntries = logs;
	}

	// create a basic Fighter to only hold stats, for the TotalStatsPanel,
	// but not actually updated during a fight.
	Fighter(String name)
	{
		player = null;
		this.name = name;
		attackCount = 0;
		successCount = 0;
		deservedDamage = 0;
		damageDealt = 0;
		magicHitCount = 0;
		magicHitCountDeserved = 0;
		dead = false;
	}

	// add an attack to the counters depending if it is successful or not.
	// also update the success rate with the new counts.
	void addAttack(boolean successful, Player opponent, AnimationData animationData)
	{
		attackCount++;
		if (successful)
		{
			successCount++;
		}

		pvpDamageCalc.updateDamageStats(player, opponent, successful, animationData);
		deservedDamage += pvpDamageCalc.getAverageHit();

		// Assume every magic attack is a successful hit, but reduce a hit afterwards if a splash is detected.
		if (animationData.attackStyle == AnimationData.AttackStyle.MAGIC)
		{
			magicHitCountDeserved += pvpDamageCalc.getAccuracy();

			if (opponent.getGraphic() != GraphicID.SPLASH)
			{
				PvpPerformanceTrackerPlugin.PLUGIN.log(name + ": we got a mage attack, no splash");
				magicHitCount++;
			}
			else
			{
				PvpPerformanceTrackerPlugin.PLUGIN.log(name + ": we got a mage attack, SPLASH");
			}
		}

		log.warn("attacker: " + name);
		log.warn("defender: " + opponent.getName());
		log.warn("deservedDamage: " + deservedDamage);

		FightLogEntry fightLogEntry = new FightLogEntry(player, opponent, pvpDamageCalc);
		PvpPerformanceTrackerPlugin.PLUGIN.log(fightLogEntry.toChatMessage(name));
		fightLogEntries.add(fightLogEntry);
	}

	// this is to be used from the TotalStatsPanel which saves a total of multiple fights.
	void addAttacks(int success, int total, double deservedDamage, int damageDealt, int magicHitCount, double magicHitCountDeserved)
	{
		successCount += success;
		attackCount += total;
		this.deservedDamage += deservedDamage;
		this.damageDealt += damageDealt;
		this.magicHitCount += magicHitCount;
		this.magicHitCountDeserved += magicHitCountDeserved;
	}

	void addDamageDealt(int damage)
	{
		this.damageDealt += damage;
	}

	void died()
	{
		dead = true;
	}

	AnimationData getAnimationData()
	{
		return AnimationData.dataForAnimation(player.getAnimation());
	}

	// Return a simple string to display the current player's success rate.
	// ex. "42/59 (71%)". The name is not included as it will be in a separate view.
	// if shortString is true, the percentage is omitted, it only returns the fraction.
	String getOffPrayStats(boolean shortString)
	{
		nf.setMaximumFractionDigits(0);
		return shortString ?
			successCount + "/" + attackCount :
			nf.format(successCount) + "/" + nf.format(attackCount) + " (" + Math.round(calculateSuccessPercentage()) + "%)";
	}

	String getOffPrayStats()
	{
		return getOffPrayStats(false);
	}

	String getMagicHitStats()
	{
		nf.setMaximumFractionDigits(0);
		String stats = nf.format(magicHitCount);
		nf.setMaximumFractionDigits(2);
		stats += "/" + nf.format(magicHitCountDeserved);
		return stats;
	}

	String getDeservedDmgString(Fighter opponent, int precision, boolean onlyDiff)
	{
		nf.setMaximumFractionDigits(precision);
		double difference = deservedDamage - opponent.deservedDamage;
		return onlyDiff ? "(" + (difference > 0 ? "+" : "") + nf.format(difference) + ")" :
			nf.format(deservedDamage) + " (" + (difference > 0 ? "+" : "") + nf.format(difference) + ")";
	}
	String getDeservedDmgString(Fighter opponent)
	{
		return getDeservedDmgString(opponent, 0, false);
	}


	String getDmgDealtString(Fighter opponent, boolean onlyDiff)
	{
		int difference = damageDealt - opponent.damageDealt;
		return onlyDiff ? "(" + (difference > 0 ? "+" : "") + difference + ")" :
			damageDealt + " (" + (difference > 0 ? "+" : "") + difference + ")";
	}
	String getDmgDealtString(Fighter opponent)
	{
		return getDmgDealtString(opponent, false);
	}

	double calculateSuccessPercentage()
	{
		return attackCount == 0 ? 0 :
		(double) successCount / attackCount * 100.0;
	}
}
