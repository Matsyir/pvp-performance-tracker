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

import lombok.Getter;

public interface RangeAmmoData
{
	RangeAmmoData[] DIAMOND_BOLTS = {
		BoltAmmo.DIAMOND_BOLTS_E,
		StrongBoltAmmo.DIAMOND_BOLTS_E,
		StrongBoltAmmo.DIAMOND_DRAGON_BOLTS_E
	};

	int getRangeStr();
	double getBonusMaxHit(); // damage bonus from bolt specs.
	double getDmgModifier(); // damage modifier from bolt specs.

	interface RangeAmmoConfigData extends RangeAmmoData
	{
		String getName();
	}

	@Getter
	enum BoltAmmo implements RangeAmmoConfigData
	{
		RUNITE_BOLTS("Runite Bolts", 115, 1),
		//DRAGONSTONE_BOLTS_E("Dstone Bolts (e)", 117, ((int)(RANGE_LEVEL * .2)) * 0.06, 1),
		DRAGONSTONE_BOLTS_E("Dstone Bolts (e)", 117, .2, 0.06, 1),
		DIAMOND_BOLTS_E("Diamond Bolts (e)", 105, 1.015);

		static EquipmentData[] WEAPONS_USING = { EquipmentData.RUNE_CROSSBOW };

		private String name;
		private int rangeStr;
		private double specRangeLevelModifier;
		private double specChance;
		private double dmgModifier;

		BoltAmmo(String name, int rangeStr, double specRangeLevelModifier, double specChance, double dmgModifier)
		{
			this.name = name;
			this.rangeStr = rangeStr;
			this.specRangeLevelModifier = specRangeLevelModifier;
			this.specChance = specChance;
			this.dmgModifier = dmgModifier;
		}
		BoltAmmo(String name, int rangeStr, double dmgModifier)
		{
			this.name = name;
			this.rangeStr = rangeStr;
			this.specRangeLevelModifier = 0;
			this.specChance = 0;
			this.dmgModifier = dmgModifier;
		}

		public double getBonusMaxHit()
		{
			return ((int)(PvpPerformanceTrackerPlugin.CONFIG.rangedLevel() * specRangeLevelModifier)) * specChance;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	@Getter
	enum StrongBoltAmmo implements RangeAmmoConfigData
	{
		RUNITE_BOLTS("Runite Bolts", 115, 1),
		DRAGONSTONE_BOLTS_E("Dstone Bolts (e)", 117, .2, .06, 1),
		DIAMOND_BOLTS_E("Diamond Bolts (e)", 105, 1.015),
		DRAGONSTONE_DRAGON_BOLTS_E("Dstone DBolts (e)", 122, .2, .06, 1),
		OPAL_DRAGON_BOLTS_E("Opal DBolts (e)", 122, .1, .05, 1),
		DIAMOND_DRAGON_BOLTS_E("Diamond DBolts (e)", 122, 1.015);

		static EquipmentData[] WEAPONS_USING = {
			EquipmentData.ARMADYL_CROSSBOW,
			EquipmentData.DRAGON_CROSSBOW,
			EquipmentData.DRAGON_HUNTER_CROSSBOW
		};

		private String name;
		private int rangeStr;
		private double specRangeLevelModifier;
		private double specChance;
		private double dmgModifier;

		StrongBoltAmmo(String name, int rangeStr, double specRangeLevelModifier, double specChance, double dmgModifier)
		{
			this.name = name;
			this.rangeStr = rangeStr;
			this.specRangeLevelModifier = specRangeLevelModifier;
			this.specChance = specChance;
			this.dmgModifier = dmgModifier;
		}
		StrongBoltAmmo(String name, int rangeStr, double dmgModifier)
		{
			this.name = name;
			this.rangeStr = rangeStr;
			this.specRangeLevelModifier = 0;
			this.specChance = 0;
			this.dmgModifier = dmgModifier;
		}

		public double getBonusMaxHit()
		{
			return ((int)(PvpPerformanceTrackerPlugin.CONFIG.rangedLevel() * specRangeLevelModifier)) * specChance;
		}


		@Override
		public String toString()
		{
			return name;
		}
	}

	@Getter
	public enum DartAmmo implements RangeAmmoConfigData
	{
		ADAMANT_DARTS("Adamant Darts", 10),
		RUNE_DARTS("Rune Darts", 14),
		DRAGON_DARTS("Dragon Darts", 20);

		static EquipmentData[] WEAPONS_USING = { EquipmentData.TOXIC_BLOWPIPE };

		private String name;
		private int rangeStr;
		private double bonusMaxHit;
		private double dmgModifier;

		DartAmmo(String name, int rangeStr)
		{
			this.name = name;
			this.rangeStr = rangeStr;
			this.bonusMaxHit = 0;
			this.dmgModifier = 1;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	@Getter
	public enum OtherAmmo implements RangeAmmoData
	{
		AMETHYST_ARROWS(55),
		DRAGON_ARROW(60),
		DRAGON_JAVELIN(150);

		private int rangeStr;
		private double bonusMaxHit;
		private double dmgModifier;

		OtherAmmo(int rangeStr)
		{
			this.rangeStr = rangeStr;
			this.bonusMaxHit = 0;
			this.dmgModifier = 1;
		}
	}
}




