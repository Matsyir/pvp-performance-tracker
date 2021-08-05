/*
 * Copyright (c) 2021, Matsyir <https://github.com/matsyir>
 * Copyright (c) 2020, Mazhar <https://twitter.com/maz_rs>
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
package matsyir.pvpperformancetracker.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.awt.Color;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import matsyir.pvpperformancetracker.controllers.PvpDamageCalc;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.api.GraphicID;
import net.runelite.api.HeadIcon;
import net.runelite.api.Player;
import net.runelite.api.kit.KitType;
import net.runelite.client.chat.ChatMessageBuilder;
import org.apache.commons.text.WordUtils;

// A fight log entry for a single Fighter. Will be saved in a List of FightLogEntries in the Fighter class.
@Getter
public class FightLogEntry implements Comparable<FightLogEntry>
{
	public static final NumberFormat nf;
	static
	{
		nf = NumberFormat.getInstance();
		nf.setRoundingMode(RoundingMode.HALF_UP);
		nf.setMaximumFractionDigits(2);
	}

	// general data
	// don't expose attacker name since it is present in the parent class (Fighter), so it is
	// redundant use of storage
	public String attackerName;
	@Expose
	@SerializedName("t")
	private long time;

	@Setter
	@Expose
	@SerializedName("T")
	private int tick;

	// this boolean represents if this is a "complete" fight log entry or not.
	// if a fight log entry is full/complete, then it has all attack data.
	// an "incomplete" fight log entry means it's only holding the current attacker's defensive stats to be used with
	// an opposing attack, to be matched up with fight analysis/data merging
	// very rough way to do this but itll work
	@Expose
	@SerializedName("f")
	private boolean isFullEntry;


	// attacker data
	@Expose
	@SerializedName("G")
	// current attacker's gear. The attacker is not necessarily the competitor.
	// Set using PlayerComposition::getEquipmentIds
	private int[] attackerGear;
	@Expose
	@SerializedName("O")
	private HeadIcon attackerOverhead;
	@Expose
	@SerializedName("m") // m because movement?
	private AnimationData animationData;
	@Expose
	@SerializedName("d")
	private double deservedDamage;
	@Expose
	@SerializedName("a")
	private double accuracy;
	@Expose
	@SerializedName("h") // h for highest hit
	private int maxHit;
	@Expose
	@SerializedName("l") // l for lowest hit
	private int minHit;
	@Expose
	@SerializedName("s")
	private boolean splash; // true if it was a magic attack and it splashed

	@Expose
	@SerializedName("C")
	private CombatLevels attackerLevels; // CAN BE NULL


	// defender data
	@Expose
	@SerializedName("g")
	private int[] defenderGear;
	@Expose
	@SerializedName("o")
	private HeadIcon defenderOverhead;

	@Expose
	@SerializedName("p")
	private int attackerOffensivePray; // offensive pray saved as SpriteID since that's all we use it for.

	public FightLogEntry(Player attacker, Player defender, PvpDamageCalc pvpDamageCalc, int attackerOffensivePray, CombatLevels levels)
	{
		this.isFullEntry = true;

		// general
		this.attackerName = attacker.getName();
		this.time = Instant.now().toEpochMilli();
		this.tick = PLUGIN.getClient().getTickCount();

		this.animationData = AnimationData.fromId(attacker.getAnimation());

		int[] attackerItems = attacker.getPlayerComposition().getEquipmentIds();
		int weaponId = attackerItems[KitType.WEAPON.getIndex()];
		EquipmentData weapon = EquipmentData.fromId(weaponId > 512 ? weaponId - 512 : weaponId);

		if (CONFIG.dlongIsVls() && weapon == EquipmentData.DRAGON_LONGSWORD)
		{
			// have to +512 here because the stat additions later will -512 for real itemIds
			attackerItems[KitType.WEAPON.getIndex()] = EquipmentData.VESTAS_LONGSWORD.getItemId() + 512;
			if (this.animationData.isSpecial)
			{
				this.animationData = AnimationData.MELEE_VLS_SPEC;
			}
			else
			{
				this.animationData = AnimationData.MELEE_SCIM_SLASH;
			}
		}

		// attacker data
		this.attackerGear = attackerItems;
		this.attackerOverhead = attacker.getOverheadIcon();

		this.deservedDamage = pvpDamageCalc.getAverageHit();
		this.accuracy = pvpDamageCalc.getAccuracy();
		this.minHit = pvpDamageCalc.getMinHit();
		this.maxHit = pvpDamageCalc.getMaxHit();
		this.splash = animationData.attackStyle == AnimationData.AttackStyle.MAGIC && defender.getGraphic() == GraphicID.SPLASH;
		this.attackerLevels = levels; // CAN BE NULL

		// defender data
		this.defenderGear = defender.getPlayerComposition().getEquipmentIds();
		this.defenderOverhead = defender.getOverheadIcon();
		this.attackerOffensivePray = attackerOffensivePray;
	}

	// create incomplete entry to save competitor's defensive stats which are only client side
	// in this context, the "attacker" is not attacking, only defending.
	public FightLogEntry(String attackerName, CombatLevels levels, int attackerOffensivePray)
	{
		this.isFullEntry = false;

		this.attackerName = attackerName;
		this.time = Instant.now().toEpochMilli();
		this.tick = PLUGIN.getClient().getTickCount();

		this.attackerLevels = levels;
		this.attackerOffensivePray = attackerOffensivePray;
	}

	// create new fightlogentry based on existing entry but new damage calcs (for fight analysis/stat merging)
	public FightLogEntry(FightLogEntry e, PvpDamageCalc pvpDamageCalc)
	{
		this.isFullEntry = true;

		// general
		this.attackerName = e.attackerName;
		this.time = e.time;
		this.tick = e.tick;

		// attacker data
		this.attackerGear = e.attackerGear;
		this.attackerOverhead = e.attackerOverhead;
		this.animationData = e.animationData;
		this.deservedDamage = pvpDamageCalc.getAverageHit();
		this.accuracy = pvpDamageCalc.getAccuracy();
		this.minHit = pvpDamageCalc.getMinHit();
		this.maxHit = pvpDamageCalc.getMaxHit();
		this.splash = e.splash;
		this.attackerLevels = e.attackerLevels;

		// defender data
		this.defenderGear = e.defenderGear;
		this.defenderOverhead = e.defenderOverhead;
		this.attackerOffensivePray = e.attackerOffensivePray;
	}

	// randomized entry used for testing
	public FightLogEntry(int [] attackerGear, int deservedDamage, double accuracy, int minHit, int maxHit, int [] defenderGear, String attackerName)
	{
		this.attackerName = attackerName;
		this.attackerGear = attackerGear;
		this.attackerOverhead = HeadIcon.MAGIC;
		this.animationData = Math.random() <= 0.5 ? AnimationData.MELEE_DAGGER_SLASH : AnimationData.MAGIC_ANCIENT_MULTI_TARGET;
		this.deservedDamage = deservedDamage;
		this.accuracy = accuracy;
		this.minHit = minHit;
		this.maxHit = maxHit;
		this.splash = Math.random() >= 0.5;
		this.time = Instant.now().toEpochMilli();
		this.defenderGear = defenderGear;
		this.defenderOverhead = HeadIcon.MAGIC;
	}


	public boolean success()
	{
		return animationData.attackStyle.getProtection() != defenderOverhead;
	}

	public String toChatMessage()
	{
		Color darkRed = new Color(127, 0, 0); // same color as default clan chat color
		return new ChatMessageBuilder()
			.append(darkRed, attackerName + ": ")
			.append(Color.BLACK, "Style: ")
			.append(darkRed, WordUtils.capitalizeFully(animationData.attackStyle.toString()))
			.append(Color.BLACK, "  Hit: ")
			.append(darkRed, getHitRange())
			.append(Color.BLACK, "  Acc: ")
			.append(darkRed, nf.format(accuracy))
			.append(Color.BLACK, "  AvgHit: ")
			.append(darkRed, nf.format(deservedDamage))
			.append(Color.BLACK, " Spec?: ")
			.append(darkRed, animationData.isSpecial ? "Y" : "N")
			.append(Color.BLACK, " OffP?:")
			.append(darkRed, success() ? "Y" : "N")
			.build();
	}

	public String getHitRange()
	{
		return minHit + "-" + maxHit;
	}

	// use to sort by last fight time, to sort fights by date/time.
	@Override
	public int compareTo(FightLogEntry o)
	{
		long diff = tick - o.tick;

		// if diff = 0, return 0. Otherwise, divide diff by its absolute value. This will result in
		// -1 for negative numbers, and 1 for positive numbers, keeping the sign and a safely small int.
		return diff == 0 ? 0 :
			(int)(diff / Math.abs(diff));
	}
}
