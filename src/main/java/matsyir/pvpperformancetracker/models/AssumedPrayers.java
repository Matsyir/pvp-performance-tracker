/*
 * Copyright (c) 2026, LogicalSolutions <https://github.com/LogicalSoIutions>
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

import matsyir.pvpperformancetracker.models.AnimationData.AttackStyle;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;

/**
 * Opponent prayer assumptions for local (unsynced) tracking.
 * RuneLite cannot see another player's prayers, so we assume they match the
 * local player's prayer unlocks based on the local real Prayer level.
 */
public final class AssumedPrayers
{
	public static final int DEADEYE_LEVEL = 62;
	public static final int MYSTIC_VIGOR_LEVEL = 63;

	public static final int DEFENCE_LEVEL_REQ = 70; // same for piety, rigour, and augury
	public static final int PIETY_LEVEL = 70;
	public static final int RIGOUR_LEVEL = 74;
	public static final int AUGURY_LEVEL = 77;

	/** Steel Skin / equivalent 15% Defence prayer. */
	public static final double STEEL_SKIN_DEF_PRAYER_MODIFIER = 1.15;

	private AssumedPrayers()
	{
	}

	public static int localPrayerLevel(Client client)
	{
		return client.getRealSkillLevel(Skill.PRAYER);
	}

	/**
	 * Assumed offensive prayer sprite for an opponent attack of the given style.
	 */
	public static int assumedOffensivePray(AttackStyle attackStyle, FightType fightType, int prayerLevel, int defenceLevel)
	{
		if (attackStyle == null)
		{
			return 0;
		}

		if (attackStyle.isMelee())
		{
			return prayerLevel >= PIETY_LEVEL && defenceLevel >= DEFENCE_LEVEL_REQ
				? SpriteID.PRAYER_PIETY
				: SpriteID.PRAYER_ULTIMATE_STRENGTH;
		}
		if (attackStyle == AttackStyle.RANGED)
		{
			return prayerLevel >= RIGOUR_LEVEL && defenceLevel >= DEFENCE_LEVEL_REQ
				? SpriteID.PRAYER_RIGOUR
				: (fightType.isLmsFight() || prayerLevel < DEADEYE_LEVEL ? SpriteID.PRAYER_EAGLE_EYE : SpriteID.PRAYER_DEADEYE);
		}
		if (attackStyle == AttackStyle.MAGIC)
		{
			return prayerLevel >= AUGURY_LEVEL && defenceLevel >= DEFENCE_LEVEL_REQ
				? SpriteID.PRAYER_AUGURY
				: (fightType.isLmsFight() || prayerLevel < MYSTIC_VIGOR_LEVEL ? SpriteID.PRAYER_MYSTIC_MIGHT : SpriteID.PRAYER_MYSTIC_VIGOUR);
		}
		return 0;
	}

	/**
	 * Defence-level prayer modifier for the defender against an incoming attack style.
	 * High unlocks use Piety/Rigour/Augury (25%); otherwise Steel Skin (15%).
	 */
	public static double assumedDefencePrayerModifier(AttackStyle incomingAttackStyle, int prayerLevel, int defenceLevel)
	{
		if (incomingAttackStyle == null)
		{
			return STEEL_SKIN_DEF_PRAYER_MODIFIER;
		}

		if (incomingAttackStyle.isMelee())
		{
			return prayerLevel >= PIETY_LEVEL && defenceLevel >= DEFENCE_LEVEL_REQ ? 1.25 : STEEL_SKIN_DEF_PRAYER_MODIFIER;
		}
		if (incomingAttackStyle == AttackStyle.RANGED)
		{
			return prayerLevel >= RIGOUR_LEVEL && defenceLevel >= DEFENCE_LEVEL_REQ ? 1.25 : STEEL_SKIN_DEF_PRAYER_MODIFIER;
		}
		if (incomingAttackStyle == AttackStyle.MAGIC)
		{
			return prayerLevel >= AUGURY_LEVEL && defenceLevel >= DEFENCE_LEVEL_REQ ? 1.25 : STEEL_SKIN_DEF_PRAYER_MODIFIER;
		}
		return STEEL_SKIN_DEF_PRAYER_MODIFIER;
	}

	/** Whether Augury's Magic Defence bonus should be assumed while defending. */
	public static boolean assumedDefensiveAugury(int prayerLevel, int defenceLevel)
	{
		return prayerLevel >= AUGURY_LEVEL && defenceLevel >= DEFENCE_LEVEL_REQ ;
	}
}
