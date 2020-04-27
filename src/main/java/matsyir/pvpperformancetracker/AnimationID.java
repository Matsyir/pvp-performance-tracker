///*
// * Copyright (c) 2020, Matsyir <https://github.com/matsyir>
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// * 1. Redistributions of source code must retain the above copyright notice, this
// *    list of conditions and the following disclaimer.
// * 2. Redistributions in binary form must reproduce the above copyright notice,
// *    this list of conditions and the following disclaimer in the documentation
// *    and/or other materials provided with the distribution.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
// * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package matsyir.pvpperformancetracker;
//
//public class AnimationID
//{
//	// Melee Attack Animations
//	public static final int MELEE_DAGGER_SLASH = 376; // tested w/ dds
//	public static final int MELEE_SPEAR_STAB = 381; // tested w/ zammy hasta
//	public static final int MELEE_SWORD_STAB = 386; // tested w/ dragon sword, obby sword, d long
//	public static final int MELEE_SCIM_SLASH = 390; // tested w/ rune & dragon scim, d sword, VLS, obby sword
//	public static final int MELEE_STAFF_CRUSH = 393; // tested w/ zuriel's staff, d long slash
//	public static final int MELEE_BATTLEAXE_SLASH = 395; // tested w/ rune baxe
//	public static final int MELEE_MACE_STAB = 400; // tested w/ d mace
//	public static final int MELEE_BATTLEAXE_CRUSH = 401; // tested w/ rune baxe, dwh & statius warhammer animation, d mace
//	public static final int MELEE_2H_CRUSH = 406; // tested w/ rune & dragon 2h
//	public static final int MELEE_2H_SLASH = 407; // tested w/ rune & dragon 2h
//	public static final int MELEE_STAFF_CRUSH_2 = 414; // tested w/ ancient staff, 3rd age wand
//	public static final int MELEE_STAFF_CRUSH_3 = 419; // Common staff crush. Air/fire/etc staves, smoke battlestaff, SOTD/SOL crush, zammy hasta crush
//	public static final int MELEE_PUNCH = 422;
//	public static final int MELEE_KICK = 423;
//	public static final int MELEE_STAFF_STAB = 428; // tested w/ SOTD/SOL jab, vesta's spear stab, c hally
//	public static final int MELEE_SPEAR_CRUSH = 429; // tested w/ vesta's spear
//	public static final int MELEE_STAFF_SLASH = 440; // tested w/ SOTD/SOL slash, zammy hasta slash, vesta's spear slash, c hally
//	public static final int MELEE_SCEPTRE_CRUSH = 1058; // tested w/ thammaron's sceptre, d long spec
//	public static final int MELEE_DRAGON_MACE_SPEC = 1060;
//	public static final int MELEE_DRAGON_DAGGER_SPEC = 1062;
//	public static final int MELEE_DRAGON_WARHAMMER_SPEC = 1378; // tested w/ dwh, statius warhammer spec
//	public static final int MELEE_ABYSSAL_WHIP = 1658; // tested w/ whip, tent whip
//	public static final int MELEE_GRANITE_MAUL = 1665; // tested w/ normal gmaul, ornate maul
//	public static final int MELEE_GRANITE_MAUL_SPEC = 1667; // tested w/ normal gmaul, ornate maul
//	public static final int MELEE_DHAROKS_GREATAXE_CRUSH = 2066;
//	public static final int MELEE_DHAROKS_GREATAXE_SLASH = 2067;
//	public static final int MELEE_AHRIMS_STAFF_CRUSH = 2078;
//	public static final int MELEE_OBBY_MAUL_CRUSH = 2661;
//	public static final int MELEE_ABYSSAL_DAGGER_STAB = 3297;
//	public static final int MELEE_ABYSSAL_BLUDGEON_CRUSH = 3298;
//	public static final int MELEE_LEAF_BLADED_BATTLEAXE_CRUSH = 3852;
//	public static final int MELEE_BARRELCHEST_ANCHOR_CRUSH = 5865;
//	public static final int MELEE_LEAF_BLADED_BATTLEAXE_SLASH = 7004;
//	public static final int MELEE_GODSWORD_SLASH = 7045; // tested w/ AGS, BGS, ZGS, SGS, sara sword
//	public static final int MELEE_GODSWORD_CRUSH = 7054; // tested w/ AGS, BGS, ZGS, SGS, sara sword
//	public static final int MELEE_DRAGON_CLAWS_SPEC = 7514;
//	public static final int MELEE_DRAGON_SWORD_SPEC = 7515; // also VLS spec
//	public static final int MELEE_ELDER_MAUL = 7516;
//	public static final int MELEE_ZAMORAK_GODSWORD_SPEC = 7638;
//	public static final int MELEE_SARADOMIN_GODSWORD_SPEC = 7640;
//	public static final int MELEE_BANDOS_GODSWORD_SPEC = 7642;
//	public static final int MELEE_ARMADYL_GODSWORD_SPEC = 7644;
//	public static final int MELEE_SCYTHE = 8056; // tested w/ all scythe styles
//	public static final int MELEE_GHAZI_RAPIER_STAB = 8145; // rapier slash is 390, basic slash animation. Also VLS stab.
//
//	// Magic Attack/Casting Animations
//	public static final int MAGIC_STANDARD_BIND = 710; // tested w/ bind, snare, entangle
//	public static final int MAGIC_STANDARD_STRIKE_BOLT_BLAST = 711; // tested w/ bolt
//	public static final int MAGIC_STANDARD_BIND_STAFF = 1161; // tested w/ bind, snare, entangle, various staves
//	public static final int MAGIC_STANDARD_STRIKE_BOLT_BLAST_STAFF = 1162; // strike, bolt and blast (tested all spells, different weapons)
//	public static final int MAGIC_STANDARD_WAVE_STAFF = 1167; // tested many staves
//	public static final int MAGIC_STANDARD_SURGE_STAFF = 7855; // tested many staves
//	public static final int MAGIC_ANCIENT_SINGLE_TARGET = 1978; // Rush & Blitz animations (tested all 8, different weapons)
//	public static final int MAGIC_ANCIENT_MULTI_TARGET = 1979; // Burst & Barrage animations (tested all 8, different weapons)
//
//	// Ranged Attack Animations
//	public static final int RANGED_SHORTBOW = 426; // Confirmed same w/ 3 types of arrows, w/ maple, magic, & hunter's shortbow, craw's bow, dbow, dbow spec
//	public static final int RANGED_RUNE_KNIFE_PVP = 929; // 1 tick animation, has 1 tick delay between attacks. likely same for all knives. Same for morrigan's javelins, both spec & normal attack.
//	public static final int RANGED_MAGIC_SHORTBOW_SPEC = 1074;
//	public static final int RANGED_CROSSBOW_PVP = 4230; // Tested RCB & ACB w/ dragonstone bolts (e) & diamond bolts (e)
//	public static final int RANGED_BLOWPIPE = 5061; // tested in PvP with all styles. Has 1 tick delay between animations in pvp.
//	public static final int RANGED_DARTS = 6600; // tested w/ addy darts. Seems to be constant animation but sometimes stalls and doesn't animate
//	public static final int RANGED_BALLISTA = 7218; // Tested w/ dragon javelins.
//	public static final int RANGED_DRAGON_THROWNAXE_SPEC = 7521;
//	public static final int RANGED_RUNE_CROSSBOW = 7552;
//	public static final int RANGED_BALLISTA_2 = 7555; // tested w/ light & heavy ballista, dragon & iron javelins.
//	public static final int RANGED_RUNE_KNIFE = 7617; // 1 tick animation, has 1 tick delay between attacks. Also d thrownaxe
//	public static final int RANGED_DRAGON_KNIFE = 8194;
//	public static final int RANGED_DRAGON_KNIFE_POISONED = 8195; // tested w/ d knife p++
//	public static final int RANGED_DRAGON_KNIFE_SPEC = 8292;
//}
