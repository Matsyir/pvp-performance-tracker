

package com.pvpperformancetracker;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import java.util.Map;
import static com.pvpperformancetracker.AnimationID.*;

// These are AnimationID groupings to represent specific attack types, that are each treated
// uniquely when it comes to damage calculation.
public enum AnimationAttackType
{
	Stab(false, null,
		MELEE_SPEAR_STAB,
		MELEE_SWORD_STAB,
		MELEE_STAFF_STAB,
		MELEE_GHAZI_RAPIER_STAB
	),
	Slash(false, null,
		MELEE_DAGGER_SLASH,
		MELEE_SCIM_SLASH,
		MELEE_STAFF_SLASH,
		MELEE_ABYSSAL_WHIP,
		MELEE_DHAROKS_GREATAXE_SLASH,
		MELEE_LEAF_BLADED_BATTLEAXE_SLASH,
		MELEE_GODSWORD_SLASH
	),
	Crush(false, null,
		MELEE_STAFF_CRUSH,
		MELEE_GRANITE_MAUL,
		MELEE_DHAROKS_GREATAXE_CRUSH,
		MELEE_LEAF_BLADED_BATTLEAXE_CRUSH,
		MELEE_BARRELCHEST_ANCHOR_CRUSH,
		MELEE_GODSWORD_CRUSH,
		MELEE_ELDER_MAUL
	),
	Blitz(false, null,
		MAGIC_ANCIENT_SINGLE_TARGET
	),
	Barrage(false, null,
		MAGIC_ANCIENT_MULTI_TARGET
	),
	Ranged(false, null,
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
	),
	Special_Stab(true, Stab),
	Special_Slash(true, Slash,
		MELEE_ARMADYL_GODSWORD_SPEC,
		MELEE_DRAGON_CLAWS_SPEC,
		MELEE_DRAGON_DAGGER_SPEC
	),
	Special_Crush(true, Crush,
		MELEE_DRAGON_WARHAMMER_SPEC,
		MELEE_GRANITE_MAUL_SPEC
	),
	Special_Range(true, Ranged,
		RANGED_MAGIC_SHORTBOW_SPEC
	);

	static AnimationAttackType[] MELEE_STYLES = { Stab, Slash, Crush };
	private static final Map<Integer, AnimationAttackType> TYPES;

	boolean isSpecial; // true if the attack type is a special attack
	@Getter
	private AnimationAttackType rootType; // root attack type for special attacks, null if not a special.
	@Getter
	private final int[] animationIds;

	AnimationAttackType(boolean isSpecial, AnimationAttackType rootType, int... animationIds)
	{
		this.isSpecial = isSpecial;
		this.rootType = rootType;
		this.animationIds = animationIds;
	}



	static
	{
		ImmutableMap.Builder<Integer, AnimationAttackType> builder = new ImmutableMap.Builder<>();

		for (AnimationAttackType type : values())
		{
			for (int animationId : type.animationIds)
			{
				builder.put(animationId, type);
			}
		}

		TYPES = builder.build();
	}

	public static AnimationAttackType typeForAnimation(int animationId)
	{
		return TYPES.get(animationId);
	}
}
