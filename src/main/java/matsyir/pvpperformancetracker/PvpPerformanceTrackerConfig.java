/*
 * Copyright (c) 2020, Matsyir <https://github.com/Matsyir>
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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pvpperformancetracker")
public interface PvpPerformanceTrackerConfig extends Config
{
	@ConfigItem(
		keyName = "restrictToLms",
		name = "Restrict to LMS",
		description = "Restricts use within the LMS area. WARNING: can be inaccurate outside LMS, as every attack animation's combat style must be manually mapped.",
		position = 0
	)
	default boolean restrictToLms()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showFightOverlay",
		name = "Show Fight Overlay",
		description = "Display an overlay of statistics while fighting.",
		position = 1
	)
	default boolean showFightOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFightHistoryPanel",
		name = "Show Fight History Panel",
		description = "Enables the side-panel which displays previous fight statistics.",
		position = 2
	)
	default boolean showFightHistoryPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "useSimpleOverlay",
		name = "Use Simple Overlay",
		description = "The overlay will only display percentage as stats rather than fraction, percentage & deserved dps.",
		position = 3
	)
	default boolean useSimpleOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlayTitle",
		name = "Show Overlay Title",
		description = "The overlay will have a title to display that it is PvP Performance.",
		position = 4
	)
	default boolean showOverlayTitle()
	{
		return false;
	}

	@ConfigItem(
		keyName = "fightHistoryLimit",
		name = "Fight History Limit",
		description = "Maximum number of previous fights to save. 0 means unlimited. They are lightweight, but will cause significant ram usage at ridiculously high numbers.",
		position = 5
	)
	default int fightHistoryLimit()
	{
		return 1000;
	}

	@ConfigItem(
		keyName = "boltChoice",
		name = "RCB Ammo",
		description = "Bolts used for rune crossbow's deserved damage estimate. LMS uses diamond (e). Dragonfire protection not accounted for.",
		position = 6
	)
	default RangeAmmoData.BoltAmmo boltChoice()
	{
		return RangeAmmoData.BoltAmmo.DIAMOND_BOLTS_E;
	}

	@ConfigItem(
		keyName = "strongBoltChoice",
		name = "ACB/DCB/DHCB Ammo",
		description = "Bolts used for ACB/DCB/DHCB's deserved damage estimate. LMS uses regular diamond (e). Dragonfire protection not accounted for.",
		position = 7
	)
	default RangeAmmoData.StrongBoltAmmo strongBoltChoice()
	{
		return RangeAmmoData.StrongBoltAmmo.DIAMOND_BOLTS_E;
	}

	@ConfigItem(
		keyName = "bpDartChoice",
		name = "Blowpipe Ammo",
		description = "Darts used for blowpipe deserved damage estimate.",
		position = 8
	)
	default RangeAmmoData.DartAmmo bpDartChoice()
	{
		return RangeAmmoData.DartAmmo.DRAGON_DARTS;
	}

	@ConfigItem(
		keyName = "assumeZerkRing",
		name = "Assume B Ring",
		description = "Assume both players are using an unimbued Berserker Ring, the starting LMS ring. Can't dynamically determine the ring.",
		position = 9
	)
	default boolean assumeZerkRing()
	{
		return true;
	}

	@ConfigItem(
		hidden = true,
		keyName = "fightHistoryData",
		name = "Fight History Data",
		description = "You shouldn't be seeing this without looking at the code. Fight history data is saved here. Do not edit.",
		position = 999
	)
	default String fightHistoryData()
	{
		return "[]";
	}
}
