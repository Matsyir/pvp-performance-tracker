/*
 * Copyright (c) 2020, Matsyir <https://github.com/matsyir>
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
package matsyir.pvpperformancetracker;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.HeadIcon;

public enum AnimationData
{
	// MELEE
	MELEE_DAGGER_SLASH(376, AttackStyle.MELEE, AttackType.SLASH), // tested w/ dds
	MELEE_SPEAR_STAB(381, AttackStyle.MELEE, AttackType.STAB), // tested w/ zammy hasta
	MELEE_SWORD_STAB(386, AttackStyle.MELEE, AttackType.STAB), // tested w/ dragon sword, obby sword, d long
	MELEE_SCIM_SLASH(390, AttackStyle.MELEE, AttackType.CRUSH), // tested w/ rune & dragon scim, d sword, VLS, obby sword
	MELEE_STAFF_CRUSH(393, AttackStyle.MELEE, AttackType.CRUSH), // tested w/ zuriel's staff, d long slash
	MELEE_BATTLEAXE_SLASH(395, AttackStyle.MELEE, AttackType.SLASH), // tested w/ rune baxe
	MELEE_MACE_STAB(400, AttackStyle.MELEE, AttackType.STAB), // tested w/ d mace
	MELEE_BATTLEAXE_CRUSH(401, AttackStyle.MELEE, AttackType.CRUSH), // tested w/ rune baxe, dwh & statius warhammer animation, d mace
	MELEE_2H_CRUSH(406, AttackStyle.MELEE, AttackType.CRUSH), // tested w/ rune & dragon 2h
	MELEE_2H_SLASH(407, AttackStyle.MELEE, AttackType.SLASH), // tested w/ rune & dragon 2h
	MELEE_STAFF_CRUSH_2(414, AttackStyle.MELEE, AttackType.CRUSH), // tested w/ ancient staff, 3rd age wand
	MELEE_STAFF_CRUSH_3(419, AttackStyle.MELEE, AttackType.CRUSH), // Common staff crush. Air/fire/etc staves, smoke battlestaff, SOTD/SOL crush, zammy hasta crush
	MELEE_PUNCH(422, AttackStyle.MELEE, AttackType.CRUSH),
	MELEE_KICK(423, AttackStyle.MELEE, AttackType.CRUSH),
	MELEE_STAFF_STAB(428, AttackStyle.MELEE, AttackType.STAB), // tested w/ SOTD/SOL jab, vesta's spear stab, c hally
	MELEE_SPEAR_CRUSH(429, AttackStyle.MELEE, AttackType.CRUSH), // tested w/ vesta's spear
	MELEE_STAFF_SLASH(440, AttackStyle.MELEE, AttackType.SLASH), // tested w/ SOTD/SOL slash, zammy hasta slash, vesta's spear slash, c hally
	MELEE_SCEPTRE_CRUSH(1058, AttackStyle.MELEE, AttackType.CRUSH), // tested w/ thammaron's sceptre, d long spec
	MELEE_DRAGON_MACE_SPEC(1060, AttackStyle.MELEE, AttackType.CRUSH_SPECIAL),
	MELEE_DRAGON_DAGGER_SPEC(1062, AttackStyle.MELEE, AttackType.SLASH_SPECIAL),
	MELEE_DRAGON_WARHAMMER_SPEC(1378, AttackStyle.MELEE, AttackType.CRUSH_SPECIAL), // tested w/ dwh, statius warhammer spec
	MELEE_ABYSSAL_WHIP(1658, AttackStyle.MELEE, AttackType.SLASH), // tested w/ whip, tent whip
	MELEE_GRANITE_MAUL(1665, AttackStyle.MELEE, AttackType.CRUSH), // tested w/ normal gmaul, ornate maul
	MELEE_GRANITE_MAUL_SPEC(1667, AttackStyle.MELEE, AttackType.CRUSH_SPECIAL), // tested w/ normal gmaul, ornate maul
	MELEE_DHAROKS_GREATAXE_CRUSH(2066, AttackStyle.MELEE, AttackType.CRUSH),
	MELEE_DHAROKS_GREATAXE_SLASH(2067, AttackStyle.MELEE, AttackType.SLASH),
	MELEE_AHRIMS_STAFF_CRUSH(2078, AttackStyle.MELEE, AttackType.CRUSH),
	MELEE_OBBY_MAUL_CRUSH(2661, AttackStyle.MELEE, AttackType.CRUSH),
	MELEE_ABYSSAL_DAGGER_STAB(3297, AttackStyle.MELEE, AttackType.STAB),
	MELEE_ABYSSAL_BLUDGEON_CRUSH(3298, AttackStyle.MELEE, AttackType.CRUSH),
	MELEE_LEAF_BLADED_BATTLEAXE_CRUSH(3852, AttackStyle.MELEE, AttackType.CRUSH),
	MELEE_BARRELCHEST_ANCHOR_CRUSH(5865, AttackStyle.MELEE, AttackType.CRUSH),
	MELEE_LEAF_BLADED_BATTLEAXE_SLASH(7004, AttackStyle.MELEE, AttackType.SLASH),
	MELEE_GODSWORD_SLASH(7045, AttackStyle.MELEE, AttackType.SLASH), // tested w/ AGS, BGS, ZGS, SGS, sara sword
	MELEE_GODSWORD_CRUSH(7054, AttackStyle.MELEE, AttackType.CRUSH), // tested w/ AGS, BGS, ZGS, SGS, sara sword
	MELEE_DRAGON_CLAWS_SPEC(7514, AttackStyle.MELEE, AttackType.SLASH_SPECIAL),
	MELEE_DRAGON_SWORD_SPEC(7515, AttackStyle.MELEE, AttackType.STAB_SPECIAL), // also VLS spec
	MELEE_ELDER_MAUL(7516, AttackStyle.MELEE, AttackType.CRUSH),
	MELEE_ZAMORAK_GODSWORD_SPEC(7638, AttackStyle.MELEE, AttackType.SLASH_SPECIAL),
	MELEE_SARADOMIN_GODSWORD_SPEC(7640, AttackStyle.MELEE, AttackType.SLASH_SPECIAL),
	MELEE_BANDOS_GODSWORD_SPEC(7642, AttackStyle.MELEE, AttackType.SLASH_SPECIAL),
	MELEE_ARMADYL_GODSWORD_SPEC(7644, AttackStyle.MELEE, AttackType.SLASH_SPECIAL),
	MELEE_SCYTHE(8056, AttackStyle.MELEE, AttackType.SLASH), // tested w/ all scythe styles (so could be crush, but unlikely)
	MELEE_GHAZI_RAPIER_STAB(8145, AttackStyle.MELEE, AttackType.STAB), // rapier slash is 390, basic slash animation. Also VLS stab.

	// RANGED
	RANGED_SHORTBOW(426, AttackStyle.RANGED, AttackType.RANGED), // Confirmed same w/ 3 types of arrows, w/ maple, magic, & hunter's shortbow, craw's bow, dbow, dbow spec
	RANGED_RUNE_KNIFE_PVP(929, AttackStyle.RANGED, AttackType.RANGED), // 1 tick animation, has 1 tick delay between attacks. likely same for all knives. Same for morrigan's javelins, both spec & normal attack.
	RANGED_MAGIC_SHORTBOW_SPEC(1074, AttackStyle.RANGED, AttackType.RANGED_SPECIAL),
	RANGED_CROSSBOW_PVP(4230, AttackStyle.RANGED, AttackType.RANGED), // Tested RCB & ACB w/ dragonstone bolts (e) & diamond bolts (e)
	RANGED_BLOWPIPE(5061, AttackStyle.RANGED, AttackType.RANGED), // tested in PvP with all styles. Has 1 tick delay between animations in pvp.
	RANGED_DARTS(6600, AttackStyle.RANGED, AttackType.RANGED), // tested w/ addy darts. Seems to be constant animation but sometimes stalls and doesn't animate
	RANGED_BALLISTA(7218, AttackStyle.RANGED, AttackType.RANGED), // Tested w/ dragon javelins.
	RANGED_DRAGON_THROWNAXE_SPEC(7521, AttackStyle.RANGED, AttackType.RANGED_SPECIAL),
	RANGED_RUNE_CROSSBOW(7552, AttackStyle.RANGED, AttackType.RANGED),
	RANGED_BALLISTA_2(7555, AttackStyle.RANGED, AttackType.RANGED), // tested w/ light & heavy ballista, dragon & iron javelins.
	RANGED_RUNE_KNIFE(7617, AttackStyle.RANGED, AttackType.RANGED), // 1 tick animation, has 1 tick delay between attacks. Also d thrownaxe
	RANGED_DRAGON_KNIFE(8194, AttackStyle.RANGED, AttackType.RANGED),
	RANGED_DRAGON_KNIFE_POISONED(8195, AttackStyle.RANGED, AttackType.RANGED), // tested w/ d knife p++
	RANGED_DRAGON_KNIFE_SPEC(8292, AttackStyle.RANGED, AttackType.RANGED_SPECIAL),

	// MAGIC - uses highest base damage available when animations are re-used. No damage = 0 damage.
	// for example, strike/bolt/blast animation will be fire blast base damage, multi target ancient spells will be ice barrage.
	MAGIC_STANDARD_BIND(710, AttackStyle.MAGIC, AttackType.MAGIC), // tested w/ bind, snare, entangle
	MAGIC_STANDARD_STRIKE_BOLT_BLAST(711, AttackStyle.MAGIC, AttackType.MAGIC, 16), // tested w/ bolt
	MAGIC_STANDARD_BIND_STAFF(1161, AttackStyle.MAGIC, AttackType.MAGIC), // tested w/ bind, snare, entangle, various staves
	MAGIC_STANDARD_STRIKE_BOLT_BLAST_STAFF(1162, AttackStyle.MAGIC, AttackType.MAGIC, 16), // strike, bolt and blast (tested all spells, different weapons)
	MAGIC_STANDARD_WAVE_STAFF(1167, AttackStyle.MAGIC, AttackType.MAGIC, 20), // tested many staves
	MAGIC_STANDARD_SURGE_STAFF(7855, AttackStyle.MAGIC, AttackType.MAGIC, 24), // tested many staves
	MAGIC_ANCIENT_SINGLE_TARGET(1978, AttackStyle.MAGIC, AttackType.MAGIC, 26), // Rush & Blitz animations (tested all 8, different weapons)
	MAGIC_ANCIENT_MULTI_TARGET(1979, AttackStyle.MAGIC, AttackType.MAGIC, 30); // Burst & Barrage animations (tested all 8, different weapons)

	private static final Map<Integer, AnimationData> DATA;

	int animationId;
	AttackStyle attackStyle;
	AttackType attackType;
	int baseSpellDamage;

	// Simple animation data constructor for all melee and range attacks
	AnimationData(int animationId, AttackStyle attackStyle, AttackType attackType)
	{
		this.animationId = animationId;
		this.attackStyle = attackStyle;
		this.attackType = attackType;
		this.baseSpellDamage = 0;
	}
	// Magic spell animation data constructor including base spell damage
	AnimationData(int animationId, AttackStyle attackStyle, AttackType attackType, int baseSpellDamage)
	{
		this.animationId = animationId;
		this.attackStyle = attackStyle;
		this.attackType = attackType;
		this.baseSpellDamage = baseSpellDamage;
	}

	static
	{
		ImmutableMap.Builder<Integer, AnimationData> builder = new ImmutableMap.Builder<>();

		for (AnimationData data : values())
		{
			builder.put(data.animationId, data);
		}

		DATA = builder.build();
	}

	public static AnimationData dataForAnimation(int animationId)
	{
		return DATA.get(animationId);
	}

	// A simple enum of the 3 combat styles and their respective HeadIcon protection
	public enum AttackStyle
	{
		MELEE(HeadIcon.MELEE),
		RANGED(HeadIcon.RANGED),
		MAGIC(HeadIcon.MAGIC);

		@Getter
		private final HeadIcon protection;

		AttackStyle(HeadIcon protection)
		{
			this.protection = protection;
		}
	}
	// An enum of more in-depth combat "types" (including stab, slash, crush) and special variants.
	// If the type represents a special attack, it will also have a "root type" of its "normal" attackType.
	public enum AttackType
	{
		STAB,
		SLASH,
		CRUSH,
		STAB_SPECIAL(true, STAB),
		SLASH_SPECIAL(true, SLASH),
		CRUSH_SPECIAL(true, CRUSH),
		RANGED,
		RANGED_SPECIAL(true, RANGED),
		MAGIC,
		MAGIC_SPECIAL;

		static AttackType[] MELEE_STYLES = {STAB, SLASH, CRUSH};

		boolean isSpecial; // true if the attack type is a special attack
		@Getter
		private AttackType rootType; // root attack type for special attacks, null if not a special.

		// Basic attack type constructor. No special, no root type.
		AttackType()
		{
			this.isSpecial = false;
			this.rootType = this;
		}
		// special attack constructor w/ root type
		AttackType(boolean isSpecial, AttackType rootType)
		{
			this.isSpecial = isSpecial;
			this.rootType = rootType;
		}
	}
}
