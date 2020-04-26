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
import java.math.RoundingMode;
import java.text.NumberFormat;
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
	private String name; // username
	@Expose
	private int attackCount; // total number of attacks
	@Expose
	private int successCount; // total number of successful attacks
	@Expose
	private double deservedDamage; // total deserved damage based on gear & opponent's pray
	@Expose
	private int damageDealt;
	@Expose
	private int magicHitCount;
	@Expose
	private double magicHitCountDeserved;
	@Expose
	private boolean dead; // will be true if the fighter died in the fight

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
	void addAttack(boolean successful, Player opponent, AnimationAttackStyle attackStyle, AnimationAttackType animationType)
	{
		double deservedDamage = pvpDamageCalc.getDamage(this.player, opponent, successful, animationType);
		this.deservedDamage += deservedDamage;
		attackCount++;

		// Assume every magic attack is a successful hit, but reduce a hit afterwards if a splash is detected.
		if (attackStyle == AnimationAttackStyle.Magic)
		{
			magicHitCountDeserved += pvpDamageCalc.getLastUsedMagicAccuracy();

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

		if (successful)
		{
			successCount++;
		}
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

	void addSplash()
	{
		magicHitCount--;
	}

	void died()
	{
		dead = true;
	}

	AnimationAttackStyle getAnimationAttackStyle()
	{
		return AnimationAttackStyle.styleForAnimation(player.getAnimation());
	}

	AnimationAttackType getAnimationAttackType()
	{
		return AnimationAttackType.typeForAnimation(player.getAnimation());
	}

	// Return a simple string to display the current player's success rate.
	// ex. "42/59 (71%)". The name is not included as it will be in a separate view.
	// if shortString is true, the percentage is omitted, it only returns the fraction.
	String getOffPrayStats(boolean shortString)
	{
		return shortString ?
			successCount + "/" + attackCount :
			successCount + "/" + attackCount + " (" + Math.round(calculateSuccessPercentage()) + "%)";
	}

	String getOffPrayStats()
	{
		return getOffPrayStats(false);
	}

	String getMagicHitStats()
	{
		return magicHitCount + "/" + nf.format(magicHitCountDeserved);
	}

	double calculateSuccessPercentage()
	{
		return (double) successCount / attackCount * 100.0;
	}
}
