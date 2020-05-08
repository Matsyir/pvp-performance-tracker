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
import net.runelite.client.config.Range;

@ConfigGroup("pvpperformancetracker")
public interface PvpPerformanceTrackerConfig extends Config
{
	int LEVEL_MIN = 1;
	int LEVEL_MAX = 120;

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
		keyName = "showFightHistoryPanel",
		name = "Show Fight History Panel",
		description = "Enables the side-panel which displays previous fight statistics.",
		position = 1
	)
	default boolean showFightHistoryPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFightOverlay",
		name = "Show Fight Overlay",
		description = "Display an overlay of statistics while fighting.",
		position = 2
	)
	default boolean showFightOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "useSimpleOverlay",
		name = "Use Simple Overlay",
		description = "The overlay will only display off-pray percentage as stats rather than various selected stats.",
		position = 3
	)
	default boolean useSimpleOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlayTitle",
		name = "Overlay: Show Title",
		description = "The overlay will have a title to display that it is PvP Performance.",
		position = 4
	)
	default boolean showOverlayTitle()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlayNames",
		name = "Overlay: Show Names",
		description = "The overlay will display names. Does not apply to the simple overlay.",
		position = 5
	)
	default boolean showOverlayNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayOffPray",
		name = "Overlay: Show Off-Pray",
		description = "The overlay will display off-pray stats as a fraction & percentage. Does not apply to the simple overlay.",
		position = 6
	)
	default boolean showOverlayOffPray()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayDeservedDmg",
		name = "Overlay: Show Deserved Dmg",
		description = "The overlay will display deserved damage & difference. Does not apply to the simple overlay.",
		position = 7
	)
	default boolean showOverlayDeservedDmg()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayDmgDealt",
		name = "Overlay: Show Dmg Dealt",
		description = "The overlay will display damage dealt. Does not apply to the simple overlay.",
		position = 8
	)
	default boolean showOverlayDmgDealt()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayMagicHits",
		name = "Overlay: Show Magic Hits",
		description = "The overlay will display successful magic hits & deserved magic hits. Does not apply to the simple overlay.",
		position = 9
	)
	default boolean showOverlayMagicHits()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ringChoice",
		name = "Ring Used",
		description = "Rings used for the deserved damage estimate.",
		position = 10
	)
	default RingData ringChoice()
	{
		return RingData.BERSERKER_RING;
	}

	@ConfigItem(
		keyName = "boltChoice",
		name = "RCB Ammo",
		description = "Bolts used for rune crossbow's deserved damage estimate. LMS uses diamond (e). Dragonfire protection not accounted for.",
		position = 11
	)
	default RangeAmmoData.BoltAmmo boltChoice()
	{
		return RangeAmmoData.BoltAmmo.DIAMOND_BOLTS_E;
	}

	@ConfigItem(
		keyName = "strongBoltChoice",
		name = "ACB/DCB/DHCB Ammo",
		description = "Bolts used for ACB/DCB/DHCB's deserved damage estimate. LMS uses regular diamond (e). Dragonfire protection not accounted for.",
		position = 12
	)
	default RangeAmmoData.StrongBoltAmmo strongBoltChoice()
	{
		return RangeAmmoData.StrongBoltAmmo.DIAMOND_BOLTS_E;
	}

	@ConfigItem(
		keyName = "bpDartChoice",
		name = "Blowpipe Ammo",
		description = "Darts used for blowpipe deserved damage estimate.",
		position = 13
	)
	default RangeAmmoData.DartAmmo bpDartChoice()
	{
		return RangeAmmoData.DartAmmo.DRAGON_DARTS;
	}

	@Range(
		min = LEVEL_MIN,
		max = LEVEL_MAX
	)
	@ConfigItem(
		keyName = "attackLevel",
		name = "Attack Level",
		description = "Attack level used for the deserved damage estimate (includes potion boost).",
		position = 14
	)
	default int attackLevel()
	{
		return 118;
	}

	@Range(
		min = LEVEL_MIN,
		max = LEVEL_MAX
	)
	@ConfigItem(
		keyName = "strengthLevel",
		name = "Strength Level",
		description = "Strength level used for the deserved damage estimate (includes potion boost).",
		position = 15
	)
	default int strengthLevel()
	{
		return 118;
	}

	@Range(
		min = LEVEL_MIN,
		max = LEVEL_MAX
	)
	@ConfigItem(
		keyName = "defenceLevel",
		name = "Defence Level",
		description = "Defence level used for the deserved damage estimate (includes potion boost).",
		position = 16
	)
	default int defenceLevel()
	{
		return 75;
	}

	@Range(
		min = LEVEL_MIN,
		max = LEVEL_MAX
	)
	@ConfigItem(
		keyName = "rangedLevel",
		name = "Ranged Level",
		description = "Ranged level used for the deserved damage estimate (includes potion boost).",
		position = 17
	)
	default int rangedLevel()
	{
		return 112;
	}

	@Range(
		min = LEVEL_MIN,
		max = LEVEL_MAX
	)
	@ConfigItem(
		keyName = "magicLevel",
		name = "Magic Level",
		description = "Magic level used for the deserved damage estimate (includes potion boost).",
		position = 18
	)
	default int magicLevel()
	{
		return 99;
	}

	@Range(
		min = 1,
		max = 1000
	)
	@ConfigItem(
		keyName = "fightHistoryLimit",
		name = "Fight History Limit",
		description = "Maximum number of previous fights to save and display in the panel. Only 10-15 can be exported to your RL account.",
		position = 19
	)
	default int fightHistoryLimit()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "fightLogInChat",
		name = "Fight Log In Chat",
		description = "Display basic fight logs in trade chat during a fight. This is very excessive, mostly for testing/verification.",
		position = 20
	)
	default boolean fightLogInChat()
	{
		return false;
	}

	// This config item saves a JSON encoded array of FightPerformances in order to save fight history.
	// across client restarts, as well as use between different computers.
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
