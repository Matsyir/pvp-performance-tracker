

package com.pvpperformancetracker;

import lombok.Getter;
import java.util.Map;
import java.util.HashMap;
import static com.pvpperformancetracker.AnimationID.*;

import static net.runelite.api.AnimationID.*;

public enum AnimationAttackType
{
	Special_Range(
			RANGED_MAGIC_SHORTBOW_SPEC
	),
	Special_Stab(

	),
	Special_Slash(
		MELEE_ARMADYL_GODSWORD_SPEC,
		MELEE_DRAGON_CLAWS_SPEC,
		MELEE_DRAGON_DAGGER_SPEC
	),
	Special_Crush(
		MELEE_DRAGON_WARHAMMER_SPEC,
		MELEE_GRANITE_MAUL_SPEC
	),
	Stab(
			MELEE_SPEAR_STAB,
			MELEE_SWORD_STAB,
			MELEE_STAFF_STAB,
			MELEE_GHAZI_RAPIER_STAB
	),
	Slash(
			MELEE_DAGGER_SLASH,
			MELEE_SCIM_SLASH,
			MELEE_STAFF_SLASH,
			MELEE_ABYSSAL_WHIP,
			MELEE_DHAROKS_GREATAXE_SLASH,
			MELEE_LEAF_BLADED_BATTLEAXE_SLASH,
			MELEE_GODSWORD_SLASH
	),
	Crush(
			MELEE_STAFF_CRUSH,
			MELEE_GRANITE_MAUL,
			MELEE_DHAROKS_GREATAXE_CRUSH,
			MELEE_LEAF_BLADED_BATTLEAXE_CRUSH,
			MELEE_BARRELCHEST_ANCHOR_CRUSH,
			MELEE_GODSWORD_CRUSH,
			MELEE_ELDER_MAUL
	),
	Blitz(
			MAGIC_ANCIENT_SINGLE_TARGET
		  ),
	Barrage(
			MAGIC_ANCIENT_MULTI_TARGET
	),
	Ranged(
			RANGED_SHORTBOW,
		   RANGED_RUNE_KNIFE_PVP,
		   RANGED_CROSSBOW_PVP,
		   RANGED_BLOWPIPE,
		   RANGED_DARTS,
		   RANGED_BALLISTA,
		   RANGED_DRAGON_THROWNAXE_SPEC,
		   RANGED_RUNE_CROSSBOW,
		   RANGED_RUNE_KNIFE,
		   RANGED_DRAGON_KNIFE,
		   RANGED_DRAGON_KNIFE_POISONED
		   );

	@Getter
	private final int[] animationIds;

	AnimationAttackType(int... animationIds)
	{
		this.animationIds = animationIds;
	}

	private static final Map<Integer, String> idForString = new HashMap<>();

	static {
		for (AnimationAttackType data : AnimationAttackType.values()) {

			for (int id : data.getAnimationIds())
				idForString.put(id, data.name());
		}
	}

	public static String getAnimationType(int id) {
		String animationType = idForString.get(id);
		if (animationType == null) {
			return "None";
		}
		return animationType;
	}
}
