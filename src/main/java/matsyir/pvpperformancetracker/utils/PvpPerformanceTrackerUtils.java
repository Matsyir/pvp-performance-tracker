package matsyir.pvpperformancetracker.utils;

import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.models.AnimationData;
import net.runelite.api.HeadIcon;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;

import java.util.Arrays;

@Slf4j
public class PvpPerformanceTrackerUtils
{
	/**
	 * Calculates the chance of knocking out an opponent with a single hit.
	 *
	 * @param accuracy           The attacker's accuracy (0.0 to 1.0).
	 * @param minHit             The attacker's minimum possible hit.
	 * @param maxHit             The attacker's maximum possible hit.
	 * @param estimatedOpponentHp The estimated HP of the opponent before the hit.
	 * @return The KO chance (0.0 to 1.0), or null if the max hit is less than the opponent's HP.
	 */
	public static Double calculateKoChance(double accuracy, int minHit, int maxHit, int estimatedOpponentHp)
	{
		// Cannot KO if max hit is less than opponent's current HP.
		if (maxHit < estimatedOpponentHp || estimatedOpponentHp <= 0)
		{
			return null;
		}

		// Ensure minHit is not greater than maxHit (shouldn't happen, but safety check)
		// and not negative.
		minHit = Math.max(0, Math.min(minHit, maxHit));

		int totalPossibleHits = maxHit - minHit + 1;
		if (totalPossibleHits <= 0)
		{
			// Avoid division by zero if minHit somehow exceeds maxHit after clamping.
			log.warn("Calculated totalPossibleHits <= 0 (min: {}, max: {})", minHit, maxHit);
			return null;
		}

		// Number of hits that would result in a KO (damage >= opponent's HP).
		// Clamp estimatedOpponentHp to be at least minHit for calculation,
		// otherwise koHits could be larger than totalPossibleHits.
		int effectiveHpForCalc = Math.max(minHit, estimatedOpponentHp);
		int koHits = maxHit - effectiveHpForCalc + 1;

		// Ensure koHits is not negative (if effectiveHpForCalc > maxHit somehow)
		koHits = Math.max(0, koHits);

		// KO chance = Accuracy * (Number of KO hits / Total possible hits)
		double chance = accuracy * ((double) koHits / totalPossibleHits);

		// Clamp chance between 0 and 1 (due to potential floating point inaccuracies)
		return Math.max(0.0, Math.min(chance, 1.0));
	}

	/**
	 * Returns how many splats an attack animation should produce based on its group pattern.
	 */
	public static int getExpectedHits(AnimationData data)
	{
		if (data == null)
		{
			return 1;
		}
		// Sum the hitsplat groups defined in AnimationData
		return Arrays.stream(data.getHitsplatGroupPattern()).sum();
	}

	/**
	 * Calculates the opponent's HP before a hit based on their health bar ratio/scale after the hit.
	 *
	 * @param ratio      The opponent's health bar ratio after the hit (0-?).
	 * @param scale      The opponent's health bar scale.
	 * @param maxHp      The opponent's maximum HP.
	 * @param damageSum  The total damage dealt by the hitsplat(s).
	 * @return The estimated HP before the hit, or -1 if calculation is not possible.
	 */
	public static int calculateHpBeforeHit(int ratio, int scale, int maxHp, int damageSum)
	{
		if (ratio < 0 || scale <= 0 || maxHp <= 0)
		{
			return -1; // Cannot estimate
		}

		int hpAfter;
		if (ratio == 0)
		{
			hpAfter = 0;
		}
		else
		{
			int minHealth = 1;
			int maxHealth;
			if (scale > 1)
			{
				if (ratio > 1)
				{
					minHealth = (maxHp * (ratio - 1) + scale - 2) / (scale - 1);
				}
				maxHealth = (maxHp * ratio - 1) / (scale - 1);
				if (maxHealth > maxHp)
				{
					maxHealth = maxHp;
				}
			}
			else // scale = 1 implies ratio must be 1
			{
				maxHealth = maxHp;
			}
			hpAfter = (minHealth + maxHealth + 1) / 2; // Average the possible range
		}

		return hpAfter + damageSum;
	}

	/**
	 * Attempt-level KO probability for Dragon Claws specials across two ticks, factoring any healing between phases.
	 */
	public static Double calculateClawsTwoPhaseKo(double specAccuracy, int specMaxHit, int hpBefore, int healBetween)
	{
		if (specMaxHit <= 0 || hpBefore <= 0)
		{
			return null;
		}

		double accSpec = Math.max(0.0, Math.min(1.0, specAccuracy));
		int baseMax = Math.max(0, (specMaxHit - 1) / 2);
		if (baseMax <= 0)
		{
			return null;
		}

		double swingAccuracy;
		if (accSpec <= 0.0)
		{
			swingAccuracy = 0.0;
		}
		else if (accSpec >= 1.0)
		{
			swingAccuracy = 1.0;
		}
		else
		{
			swingAccuracy = 1.0 - Math.pow(1.0 - accSpec, 0.25);
		}

		double missChance = 1.0 - swingAccuracy;
		double p1 = swingAccuracy;
		double p2 = missChance * swingAccuracy;
		double p3 = missChance * missChance * swingAccuracy;
		double p4 = missChance * missChance * missChance * swingAccuracy;

		double inverseCount = 1.0 / (baseMax + 1);
		double ko = 0.0;

		for (int roll = 0; roll <= baseMax; roll++)
		{
			int halfCeil = (roll + 1) / 2;
			int halfFloor = roll / 2;
			int quarterFloor = roll / 4;
			int threeQuarterCeil = (int) Math.ceil(0.75 * roll);
			int threeQuarterFloor = (int) Math.floor(0.75 * roll);

			// Case E1: first swing connects (two hits tick k, two hits tick k+1)
			{
				int h1 = roll;
				int h2 = halfCeil;
				int h3 = quarterFloor;
				int used = h1 + h2 + h3;
				int remainder = Math.max(0, 2 * roll - used);
				int damageTick1 = h1 + h2;
				int damageTick2 = h3 + remainder;
				double contribution = p1 * inverseCount * (damageTick1 >= hpBefore
					? 1.0
					: (damageTick2 >= (hpBefore - damageTick1 + healBetween) ? 1.0 : 0.0));
				ko += contribution;
			}

			// Case E2: second swing connects (phase 1 does no damage)
			{
				int damageTick1 = roll;
				int damageTick2 = roll;
				double contribution = p2 * inverseCount * (damageTick1 >= hpBefore
					? 1.0
					: (damageTick2 >= (hpBefore - damageTick1 + healBetween) ? 1.0 : 0.0));
				ko += contribution;
			}

			// Case E3: third swing connects (all damage tick k+1)
			{
				int damageTick1 = 0;
				int damageTick2 = threeQuarterCeil + threeQuarterFloor;
				double contribution = p3 * inverseCount * (damageTick1 >= hpBefore
					? 1.0
					: (damageTick2 >= (hpBefore - damageTick1 + healBetween) ? 1.0 : 0.0));
				ko += contribution;
			}

			// Case E4: fourth swing connects (all damage tick k+1)
			{
				int damageTick1 = 0;
				int damageTick2 = threeQuarterCeil + threeQuarterFloor;
				double contribution = p4 * inverseCount * (damageTick1 >= hpBefore
					? 1.0
					: (damageTick2 >= (hpBefore - damageTick1 + healBetween) ? 1.0 : 0.0));
				ko += contribution;
			}
		}

		if (ko < 0.0)
		{
			ko = 0.0;
		}
		else if (ko > 1.0)
		{
			ko = 1.0;
		}
		return ko;
	}

    public static int getSpriteForSkill(Skill skill)
    {
        switch (skill)
        {
            case ATTACK: return SpriteID.SKILL_ATTACK;
            case STRENGTH: return SpriteID.SKILL_STRENGTH;
            case DEFENCE: return SpriteID.SKILL_DEFENCE;
            case RANGED: return SpriteID.SKILL_RANGED;
            case MAGIC: return SpriteID.SKILL_MAGIC;
            case HITPOINTS: return SpriteID.SKILL_HITPOINTS;
            default: return -1;
        }
    }

	// returns SpriteID for a given HeadIcon. returns -1 if not found
	public static int getSpriteForHeadIcon(HeadIcon icon)
	{
		if (icon == null) { return -1; }
		switch (icon)
		{
			case MELEE: return SpriteID.PRAYER_PROTECT_FROM_MELEE;
			case RANGED: return SpriteID.PRAYER_PROTECT_FROM_MISSILES;
			case MAGIC: return SpriteID.PRAYER_PROTECT_FROM_MAGIC;
			case SMITE: return SpriteID.PRAYER_SMITE;
			case RETRIBUTION: return SpriteID.PRAYER_RETRIBUTION;
			case REDEMPTION: return SpriteID.PRAYER_REDEMPTION;
			default: return -1;
		}
	}

	// fix an itemId that came from getPlayerComposition().getEquipmentIds()
	public static int fixItemId(int itemId)
	{
		return itemId > PlayerComposition.ITEM_OFFSET ? itemId - PlayerComposition.ITEM_OFFSET : itemId;
	}

	// create new array so we don't modify original array
	public static int[] fixItemIds(int[] itemIds)
	{
		if (itemIds == null || itemIds.length < 1)
		{
			return new int[] { 0 };
		}
		int[] fixedItemIds = new int[itemIds.length];
		for (int i = 0; i < itemIds.length; i++)
		{
			fixedItemIds[i] = fixItemId(itemIds[i]);
		}

		return fixedItemIds;
	}
}
