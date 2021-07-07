/*
 * Copyright (c) 2020, Mazhar <https://twitter.com/maz_rs>
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
package matsyir.pvpperformancetracker.models;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin;
import net.runelite.api.ItemID;
import net.runelite.api.kit.KitType;
import org.apache.commons.lang3.ArrayUtils;

// Mostly to help fetch LMS gear stats, since LMS items are copies of real items, so their stats aren't
// cached like most items. Each LMS item will have the 'real' itemId so the stats can be looked up.
// A few non-LMS range weapons will be saved in order to help estimate ammo type/range strength based
// on current weapon itemId, or to determine special attacks used.
public enum EquipmentData
{
	// Non-LMS items:
	DRAGON_CROSSBOW(21902),
	DRAGON_HUNTER_CROSSBOW(21012),
	LIGHT_BALLISTA(19478),
	MAGIC_SHORTBOW(861),
	MAGIC_SHORTBOW_I(12788),
	TOXIC_BLOWPIPE(12926),
	VOLATILE_NIGHTMARE_STAFF(24424),
	//CRAWS_BOW(22550), // ammo bonus is built into weapon so we don't need to include it
	SMOKE_BATTLESTAFF(11998),
	TOME_OF_FIRE(20714), // (charged tome)
	VOID_MAGE_HELM(11663, 11674), // the first void helm numbers were tested (11663-11665), the extra IDs purposes are
	VOID_RANGE_HELM(11664, 11675), // unknown but included in case they are ever used.
	VOID_MELEE_HELM(11665, 11676),
	VOID_ELITE_BODY(13072),
	VOID_ELITE_LEGS(13073),
	VOID_GLOVES(8842),
	VOID_BODY(8839, 10611), // same here, not sure why there is a duplicate, but included just in case.
	VOID_LEGS(8840),
	CRYSTAL_HELM(ItemID.CRYSTAL_HELM),
	CRYSTAL_BODY(ItemID.CRYSTAL_BODY),
	CRYSTAL_LEGS(ItemID.CRYSTAL_LEGS),
	BOW_OF_FAERDHINEN(ItemID.BOW_OF_FAERDHINEN, ItemID.BOW_OF_FAERDHINEN_C, ItemID.BOW_OF_FAERDHINEN_C_25869, ItemID.BOW_OF_FAERDHINEN_C_25884, ItemID.BOW_OF_FAERDHINEN_C_25886, ItemID.BOW_OF_FAERDHINEN_C_25888, ItemID.BOW_OF_FAERDHINEN_C_25890, ItemID.BOW_OF_FAERDHINEN_C_25892, ItemID.BOW_OF_FAERDHINEN_C_25894, ItemID.BOW_OF_FAERDHINEN_C_25896),
	CRYSTAL_BOW(ItemID.CRYSTAL_BOW_FULL, ItemID.CRYSTAL_BOW_FULL_I, ItemID.CRYSTAL_BOW, ItemID.CRYSTAL_BOW_110, ItemID.CRYSTAL_BOW_110_I, ItemID.CRYSTAL_BOW_210, ItemID.CRYSTAL_BOW_210_I, ItemID.CRYSTAL_BOW_310, ItemID.CRYSTAL_BOW_310_I, ItemID.CRYSTAL_BOW_410, ItemID.CRYSTAL_BOW_410_I, ItemID.CRYSTAL_BOW_510, ItemID.CRYSTAL_BOW_510_I, ItemID.CRYSTAL_BOW_610, ItemID.CRYSTAL_BOW_610_I, ItemID.CRYSTAL_BOW_710, ItemID.CRYSTAL_BOW_710_I, ItemID.CRYSTAL_BOW_810, ItemID.CRYSTAL_BOW_810_I, ItemID.CRYSTAL_BOW_910, ItemID.CRYSTAL_BOW_910_I),

	// LMS items:
	RUNE_CROSSBOW(9185, 23601),
	ARMADYL_CROSSBOW(11785, 23611),
	DARK_BOW(11235, 20408),
	HEAVY_BALLISTA(19481, 23630),

	STATIUS_WARHAMMER(22622, 23620),
	VESTAS_LONGSWORD(22613, 23615, 24617),
	ARMADYL_GODSWORD(11802, 20593, 20368),
	DRAGON_CLAWS(13652, 20784),
	DRAGON_DAGGER(1215, 20407, 1231, 5680, 5698),
	GRANITE_MAUL(4153, 20557),
	AMULET_OF_FURY(6585, 23640),
	BANDOS_TASSETS(11834, 23646),
	BLESSED_SPIRIT_SHIELD(12831, 23642),
	DHAROKS_HELM(4716, 23639),
	DHAROKS_PLATELEGS(4722, 23633),
	GUTHANS_HELM(4724, 23638),
	KARILS_TOP(4736, 23632),
	TORAGS_HELM(4745, 23637),
	TORAGS_PLATELEGS(4751, 23634),
	VERACS_HELM(4753, 23636),
	VERACS_PLATESKIRT(4759, 23635),
	MORRIGANS_JAVELIN(22636, 23619),
	SPIRIT_SHIELD(12829, 23599),
	HELM_OF_NEITIZNOT(10828, 23591),
	AMULET_OF_GLORY(1704, 20586),
	ABYSSAL_WHIP(4151, 20405),
	DRAGON_DEFENDER(12954, 23597),
	BLACK_DHIDE_BODY(2503, 20423),
	RUNE_PLATELEGS(1079, 20422),
	ROCK_CLIMBING_BOOTS(2203, 20578),
	BARROWS_GLOVES(7462, 23593),
	ELDER_MAUL(21003, 21205),
	INFERNAL_CAPE(21295, 23622),
	GHRAZI_RAPIER(22324, 23628),

	ZURIELS_STAFF(22647, 23617),
	STAFF_OF_THE_DEAD(11791, 23613),
	KODAI_WAND(21006, 23626),
	AHRIMS_STAFF(4710, 23653),
	MYSTIC_ROBE_TOP(4091, 20425),
	MYSTIC_ROBE_BOTTOM(4093, 20426),
	AHRIMS_ROBE_TOP(4712, 20598),
	AHRIMS_ROBE_SKIRT(4714, 20599),
	OCCULT_NECKLACE(12002, 23654),
	MAGES_BOOK(6889, 23652),
	ETERNAL_BOOTS(13235, 23644),
	IMBUED_ZAMORAK_CAPE(21795, 23605),
	IMBUED_GUTHIX_CAPE(21793, 23603),
	IMBUED_SARADOMIN_CAPE(21791, 23607);

	private static final Map<Integer, EquipmentData> itemData = new HashMap<>();

	@Getter
	private final int itemId; // main id to be used for stat lookups
	@Getter
	private final int[] additionalIds; // extra ids that might represent the same item (like LMS versions, or a dragon dagger(p) = dds, or charged items etc)

	EquipmentData(int itemId)
	{
		this.itemId = itemId;
		this.additionalIds = null;
	}

	EquipmentData(int itemId, int... itemIds)
	{
		this.itemId = itemId;
		this.additionalIds = itemIds;
	}

	// Get the saved EquipmentData for a given itemId (could be null)
	public static EquipmentData getEquipmentDataFor(int itemId)
	{
		return itemData.get(itemId);
	}

	// get currently selected weapon ammo, based on weapon used & configured bolt choice.
	public static RangeAmmoData getWeaponAmmo(EquipmentData weapon)
	{
		if (ArrayUtils.contains(RangeAmmoData.BoltAmmo.WEAPONS_USING, weapon))
		{
			return PvpPerformanceTrackerPlugin.CONFIG.boltChoice();
		}
		else if (ArrayUtils.contains(RangeAmmoData.StrongBoltAmmo.WEAPONS_USING, weapon))
		{
			return PvpPerformanceTrackerPlugin.CONFIG.strongBoltChoice();
		}
		else if (ArrayUtils.contains(RangeAmmoData.DartAmmo.WEAPONS_USING, weapon))
		{
			return PvpPerformanceTrackerPlugin.CONFIG.bpDartChoice();
		}
		else if (weapon == HEAVY_BALLISTA || weapon == LIGHT_BALLISTA)
		{
			return RangeAmmoData.OtherAmmo.DRAGON_JAVELIN;
		}
		else if (weapon == DARK_BOW)
		{
			return RangeAmmoData.OtherAmmo.DRAGON_ARROW;
		}
		else if (weapon == MAGIC_SHORTBOW || weapon == MAGIC_SHORTBOW_I)
		{
			return RangeAmmoData.OtherAmmo.AMETHYST_ARROWS;
		}

		return null;
	}

	static
	{
		for (EquipmentData data : EquipmentData.values())
		{
			itemData.putIfAbsent(data.getItemId(), data);
			if (data.additionalIds != null && data.additionalIds.length > 0)
			{
				for (int id : data.additionalIds)
				{
					itemData.putIfAbsent(id, data);
				}
			}
		}
	}

	public enum VoidStyle
	{
		VOID_MELEE(1.1, 1.1),
		VOID_RANGE(1.1, 1.1),
		VOID_MAGE(1.45, 1),
		VOID_ELITE_MELEE(1.1, 1.1),
		VOID_ELITE_RANGE(1.125, 1.125),
		VOID_ELITE_MAGE(1.45, 1.025),
		NONE(1, 1);

		public double accuracyModifier;
		public double dmgModifier;

		VoidStyle(double accuracyModifier, double dmgModifier)
		{
			this.accuracyModifier = accuracyModifier;
			this.dmgModifier = dmgModifier;
		}

		// return a void style for a given PlayerComposition
		public static VoidStyle getVoidStyleFor(int[] playerComposition)
		{
			if (playerComposition == null) { return NONE; }

			EquipmentData gloves = EquipmentData.getEquipmentDataFor(playerComposition[KitType.HANDS.getIndex()]);

			if (gloves != EquipmentData.VOID_GLOVES) { return NONE; }

			EquipmentData helm = EquipmentData.getEquipmentDataFor(playerComposition[KitType.HEAD.getIndex()]);
			EquipmentData torso = EquipmentData.getEquipmentDataFor(playerComposition[KitType.TORSO.getIndex()]);
			EquipmentData legs = EquipmentData.getEquipmentDataFor(playerComposition[KitType.LEGS.getIndex()]);

			if (torso == EquipmentData.VOID_BODY && legs == EquipmentData.VOID_LEGS)
			{
				return helm == EquipmentData.VOID_MAGE_HELM ? VOID_MAGE
					: helm == EquipmentData.VOID_RANGE_HELM ? VOID_RANGE
					: helm == EquipmentData.VOID_MELEE_HELM ? VOID_MELEE
					: NONE;
			}
			else if (torso == EquipmentData.VOID_ELITE_BODY && legs == EquipmentData.VOID_ELITE_LEGS)
			{
				return helm == EquipmentData.VOID_MAGE_HELM ? VOID_ELITE_MAGE
					: helm == EquipmentData.VOID_RANGE_HELM ? VOID_ELITE_RANGE
					: helm == EquipmentData.VOID_MELEE_HELM ? VOID_ELITE_MELEE
					: NONE;
			}

			return NONE;
		}
	}
}
