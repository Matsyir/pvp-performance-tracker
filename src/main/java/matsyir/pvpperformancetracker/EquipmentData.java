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
package matsyir.pvpperformancetracker;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

// Mostly to save LMS gear stats, since LMS items are copies of real items, so their stats aren't
// cached like most items. A few non-LMS range weapons will be saved in order to help estimate ammo
// type/range strength.
public enum EquipmentData
{
	// Non-LMS items:
	NONE(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	RUNE_CROSSBOW_PVP(9185, 0, 0, 0, 0, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	ARMADYL_CROSSBOW_PVP(11785, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 1, 0),
	DRAGON_CROSSBOW(21902, 0, 0, 0, 0, 94, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	DRAGON_HUNTER_CROSSBOW(21012, 0, 0, 0, 0, 95, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	DARK_BOW_PVP(11235, 0, 0, 0, 0, 95, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	HEAVY_BALLISTA_PVP(19481, 0, 0, 0, 0, 125, 0, 0, 0, 0, 0, 0, 15, 0, 0),
	LIGHT_BALLISTA(19478, 0, 0, 0, 0, 110, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	MAGIC_SHORTBOW(861, 0, 0, 0, 0, 69, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	MAGIC_SHORTBOW_I(12788, 0, 0, 0, 0, 75, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	TOXIC_BLOWPIPE(12926, 0, 0, 0, 0, 60, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	CRAWS_BOW(22550, 0, 0, 0, 0, 75, 0, 0, 0, 0, 0, 0, 60, 0, 0),

	// LMS items:
	RUNE_CROSSBOW(23601, 0, 0, 0, 0, 90, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	ARMADYL_CROSSBOW(23611, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 1, 0),
	DARK_BOW(20408, 0, 0, 0, 0, 95, 0, 0, 0, 0, 0, 0, 0, 0, 0),
	HEAVY_BALLISTA(23630, 0, 0, 0, 0, 125, 0, 0, 0, 0, 0, 0, 15, 0, 0),

	ARMADYL_GODSWORD(20593, 0, 132, 80, 0, 0, 0, 0, 0, 0, 0, 132, 0, 8, 0),
	DRAGON_CLAWS(20784, 41, 57, -4, 0, 0, 13, 26, 7, 0, 0, 56, 0, 0, 0),
	GRANITE_MAUL(20557, 0, 0, 81, 0, 0, 0, 0, 0, 0, 0, 79, 0, 0, 0),
	MAGES_BOOK(23652, 0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0),
	SEERS_RING_I(23624, 0, 0, 0, 8, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0),
	AHRIMS_ROBE_TOP(20598, 0, 0, 0, 30, -10, 52, 37, 63, 30, 0, 0, 0, 0, 0),
	AHRIMS_ROBE_SKIRT(20599, 0, 0, 0, 22, -7, 33, 30, 36, 22, 0, 0, 0, 0, 0),
	AMULET_OF_FURY(23640, 10, 10, 10, 10, 10, 15, 15, 15, 15, 15, 8, 0, 5, 0),
	BANDOS_TASSETS(23646, 0, 0, 0, -21, -7, 71, 63, 66, -4, 93, 2, 0, 1, 0),
	BLESSED_SPIRIT_SHIELD(23642, 0, 0, 0, 0, 0, 53, 55, 73, 2, 52, 0, 0, 3, 0),
	DHAROKS_HELM(23639, 0, 0, 0, -3, -1, 45, 48, 44, -1, 51, 0, 0, 0, 0),
	DHAROKS_PLATELEGS(23633, 0, 0, 0, -21, -7, 85, 82, 83, -4, 92, 0, 0, 0, 0),
	GUTHANS_HELM(23638, 0, 0, 0, -6, -2, 55, 58, 54, -1, 62, 0, 0, 0, 0),
	KARILS_TOP(23632, 0, 0, 0, -15, 30, 47, 42, 50, 65, 57, 0, 0, 0, 0),
	TORAGS_HELM(23637, 0, 0, 0, -6, -2, 55, 58, 54, -1, 62, 0, 0, 0, 0),
	TORAGS_PLATELEGS(23634, 0, 0, 0, -21, -7, 85, 82, 83, -4, 92, 0, 0, 0, 0),
	VERACS_HELM(23636, 0, 0, 0, -6, -2, 55, 58, 54, 0, 56, 0, 0, 3, 0),
	VERACS_PLATESKIRT(23635, 0, 0, 0, -21, -7, 85, 82, 83, 0, 84, 0, 0, 4, 0),
	STATIUS_WARHAMMER(23620, -4, -4, 123, 0, 0, 0, 0, 0, 0, 0, 114, 0, 0, 0),
	VESTAS_LONGSWORD(23615, 106, 121, -2, 0, 0, 1, 4, 3, 0, 0, 118, 0, 0, 0),
	ZURIELS_STAFF(23617, 13, -1, 65, 18, 0, 5, 7, 4, 18, 0, 72, 0, 0, 10),
	MORRIGANS_JAVELIN(23619, 0, 0, 0, 0, 105, 0, 0, 0, 0, 0, 0, 145, 0, 0),
	SPIRIT_SHIELD(23599, 0, 0, 0, 0, 0, 39, 41, 50, 1, 45, 0, 0, 1, 0),
	HELM_OF_NEITIZNOT(23591, 0, 0, 0, 0, 0, 31, 29, 34, 3, 30, 3, 0, 3, 0),
	AMULET_OF_GLORY(20586, 10, 10, 10, 10, 10, 3, 3, 3, 3, 3, 6, 0, 3, 0),
	ABYSSAL_WHIP(20405, 0, 82, 0, 0, 0, 0, 0, 0, 0, 0, 82, 0, 0, 0),
	DRAGON_DEFENDER(23597, 25, 24, 23, -3, -2, 25, 24, 23, -3, -2, 6, 0, 0, 0),
	BLACK_DHIDE_BODY(20423, 0, 0, 0, -15, 30, 55, 47, 60, 50, 55, 0, 0, 0, 0),
	RUNE_PLATELEGS(20422, 0, 0, 0, -21, -7, 51, 49, 47, -4, 49, 0, 0, 0, 0),
	ROCK_CLIMBING_BOOTS(20578, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 2, 0, 0, 0),
	GLOVES(23593, 12, 12, 12, 6, 12, 12, 12, 12, 6, 12, 12, 0, 0, 0),
	BERSERKER_RING_I(23595, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 8, 0, 0, 0),
	AHRIMS_STAFF(23653, 12, -1, 65, 15, 0, 3, 5, 2, 15, 0, 68, 0, 0, 5),
	DRAGON_DAGGER(20407, 40, 25, -4, 1, 0, 0, 0, 0, 1, 0, 40, 0, 0, 0),
	MYSTIC_ROBE_TOP(20425, 0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0),
	MYSTIC_ROBE_BOTTOM(20426, 0, 0, 0, 15, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0),
	ELDER_MAUL(21205, 0, 0, 135, -4, 0, 0, 0, 0, 0, 0, 147, 0, 0, 0),
	STAFF_OF_THE_DEAD(23613, 55, 70, 0, 17, 0, 0, 3, 3, 17, 0, 72, 0, 0, 15),
	INFERNAL_CAPE(23622, 4, 4, 4, 1, 1, 12, 12, 12, 12, 12, 8, 0, 2, 0),
	KODAI_WAND(23626, 0, 0, 0, 28, 0, 0, 3, 3, 20, 0, 0, 0, 0, 15),
	GHRAZI_RAPIER(23628, 94, 55, 0, 0, 0, 0, 0, 0, 0, 0, 89, 0, 0, 0),
	IMBUED_ZAMORAK_CAPE(23605, 0, 0, 0, 15, 0, 3, 3, 3, 15, 0, 0, 0, 0, 2),
	IMBUED_GUTHIX_CAPE(23603, 0, 0, 0, 15, 0, 3, 3, 3, 15, 0, 0, 0, 0, 2),
	IMBUED_SARADOMIN_CAPE(23607, 0, 0, 0, 15, 0, 3, 3, 3, 15, 0, 0, 0, 0, 2),
	OCCULT_NECKLACE(23654, 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 2, 10),
	ETERNAL_BOOTS(23644, 0, 0, 0, 8, 0, 5, 5, 5, 8, 5, 0, 0, 0, 0);

	private static final Map<Integer, EquipmentData> itemData = new HashMap<>();

	@Getter
	private final int itemId;
	@Getter
	private final int[] itemBonuses;

	@Inject
	EquipmentData(int itemId, int... itemBonuses)
	{
		this.itemId = itemId;
		this.itemBonuses = itemBonuses;
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
		else if (weapon == HEAVY_BALLISTA || weapon == HEAVY_BALLISTA_PVP || weapon == LIGHT_BALLISTA)
		{
			return RangeAmmoData.OtherAmmo.DRAGON_JAVELIN;
		}
		else if (weapon == DARK_BOW || weapon == DARK_BOW_PVP)
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
			itemData.put(data.getItemId(), data);
		}
	}
}
