/*
 * Copyright (c) 2021, Matsyir <https://github.com/Matsyir>
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
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import matsyir.pvpperformancetracker.models.RingData;
import matsyir.pvpperformancetracker.utils.WorldFlag;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("pvpperformancetracker")
public interface PvpPerformanceTrackerConfig extends Config
{
	/**
	 * Filter for hits on robes statistic.
	 */
	enum RobeHitFilter
	{
		BOTTOM,
		TOP,
		BOTH,
		EITHER
	}

	enum PvpHubVisibilityDelay
	{
		INSTANT("Instant", 0),
		RANDOM_6_20("Rand: 6-20min", 0),
		MINUTES_15("15 minutes", 15 * 60),
		MINUTES_30("30 minutes", 30 * 60),
		HOUR_1("1 hour", 60 * 60),
		HOUR_2("2 hours", 2 * 60 * 60);

		@Getter
		private final String displayName;
		@Getter
		private final int delaySeconds;

		PvpHubVisibilityDelay(String displayName, int delaySeconds)
		{
			this.displayName = displayName;
			this.delaySeconds = delaySeconds;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	int LEVEL_MIN = 1;
	int LEVEL_MAX = 120;

	// ================================= Sections =================================

	@ConfigSection(name = "PvP-Hub Website",
		description = "Contains settings relating to the PvP-Hub website.<br>" +
			"Note that the client remains entirely client-side if you don't opt into these features.",
		position = 1399,
		closedByDefault = false
	)
	String pvpHubSection = "pvpHubSection";

	@ConfigSection(name = "Overlay (5 lines max)",
		description = "Contains overlay settings (MAX of 5 lines allowed)",
		position = 2000,
		closedByDefault = true
	)
	String overlaySection = "overlay";

	@ConfigSection(name = "Gear/Ammo",
		description = "Contains gear/ammo settings for fights outside LMS.<br>" +
			"This is what's used for expected damage if the fight isn't merged.",
		position = 11000,
		closedByDefault = false
	)
	String gearAmmoSection = "gearAmmo";

	@ConfigSection(name = "Levels",
		description = "Contains level settings for fights outside of LMS (including boosts).<br>" +
			"This is what's used for expected damage if the fight isn't merged.",
		position = 15000,
		closedByDefault = false
	)
	String levelsSection = "levels";

	// ================================= General =================================

	@ConfigItem(
		keyName = "pluginVersion",
		name = "Plugin Version",
		description = "Hidden plugin version in order to potentially 'gracefully migrate' data in the future.",
		hidden = true
	)
	default String pluginVersion()
	{
		return PvpPerformanceTrackerPlugin.PLUGIN_VERSION;
	}

	@ConfigItem(
		keyName = "settingsConfigured",
		name = "I have verified my settings",
		description = "Some settings affect damage calculations, and every player should set them based on how they're pking." +
			"<br>Please confirm them and tick this box in order to hide the config warning.",
		position = -1
	)
	default boolean settingsConfigured()
	{
		return false;
	}

	@ConfigItem(
		keyName = "restrictToLms",
		name = "Restrict to LMS",
		description = "Restricts functionality and visibility to the LMS areas & its lobby (Ferox Enclave)." +
			"<br>WARNING: can be inaccurate outside LMS, as every attack animation's combat style must be manually mapped.",
		position = 100
	)
	default boolean restrictToLms()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showFightHistoryPanel",
		name = "Show Fight History Panel",
		description = "Enables the side-panel which displays previous fight's statistics.",
		position = 1000
	)
	default boolean showFightHistoryPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "robeHitFilter",
		name = "Hits on Robes Filter",
		description = "Which part of the robe to count hits on: bottom, top, both, or either.",
		position = 1100
	)
	default RobeHitFilter robeHitFilter()
	{
		return RobeHitFilter.EITHER;
	}

	@ConfigItem(
		keyName = "worldDisplayChoice",
		name = "World Display Style",
		description = "Choose how you want the world to be displayed in the fight history panel, or hide it entirely.",
		position = 1150
	)
	default WorldFlag.WorldDisplayChoice getWorldDisplayChoice()
	{
		return WorldFlag.WorldDisplayChoice.FLAG;
	}

	@ConfigItem(
		keyName = "uploadFightsToPvpHub",
		name = "Upload Fights to PvP-Hub.com",
		description = "When enabled, fights will be assigned a shared ID and uploaded to PvP-Hub.com after they end." +
			"<br>Public visibility follows the PvP-Hub upload delay setting below.",
		warning = "This feature will submit fight logs and IP address to a 3rd-party server not controlled or verified by Runelite developers. " +
			"Your RSN is submitted unless PvP-Hub RSN privacy is enabled.",
		position = 1400,
		section = pvpHubSection
	)
	default boolean uploadFightsToPvpHub()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pvpHubVisibilityDelay",
		name = "PvP-Hub Upload Delay",
		description = "Choose how long uploaded fights stay hidden before becoming public on PvP-Hub.<br>" +
			"The \"Rand\" option uses a freshly randomized 6-20 minute delay for each uploaded fight.<br>" +
			"Worlds are never publicly displayed on the website, so it shouldn't be much of a concern to keep it on Instant.",
		position = 1410,
		section = pvpHubSection
	)
	default PvpHubVisibilityDelay pvpHubVisibilityDelay()
	{
		return PvpHubVisibilityDelay.INSTANT;
	}

	@ConfigItem(
		keyName = "hideRsnOnPvpHub",
		name = "Hide RSN on PvP-Hub",
		description = "Replace your RSN in PvP-Hub uploads with the hidden name shown in the side panel.<br><br>" +
			"If you wish to keep that hidden identity private, be careful not to show this on stream, screenshots, or screen share.<br><br>" +
			"If you've leaked this name or would like to change it, you can do so by right-clicking <br>" +
			"the button which displays your hidden name, or the Total Stats panel above your fight history.",
		warning = "<html>Your PvP-Hub hidden name will be shown in the PvP Performance Tracker panel.<br><br>" +
			"If you wish to keep that hidden identity private, be careful not to show this on stream, screenshots, or screen share.<br><br>" +
			"If you've leaked this name or would like to change it, you can do so by right-clicking <br>" +
			"the button which displays your hidden name, or the Total Stats panel above your fight history.",
		position = 1420,
		section = pvpHubSection
	)
	default boolean hideRsnOnPvpHub()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pvpHubAnonymousId",
		name = "PvP-Hub Anonymous ID",
		description = "Hidden local random ID used to derive your PvP-Hub hidden name.",
		position = 1430,
		hidden = true
	)
	default String pvpHubAnonymousId()
	{
		return "";
	}

	// ================================= Overlay =================================

	@ConfigItem(
		keyName = "showFightOverlay",
		name = "Show Fight Overlay",
		description = "Display an overlay of statistics while fighting.",
		position = 2000,
		section = overlaySection
	)
	default boolean showFightOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayTitle",
		name = "Overlay: Show Title",
		description = "The overlay will have a title to display that it is PvP Performance.",
		position = 4000,
		section = overlaySection
	)
	default boolean showOverlayTitle()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlayNames",
		name = "Overlay: Show Names",
		description = "The overlay will display names.<br>Max. of 5 lines on the overlay",
		position = 5000,
		section = overlaySection
	)
	default boolean showOverlayNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayOffPray",
		name = "Overlay: Show Off-Pray",
		description = "The overlay will display off-pray stats as a fraction & percentage.<br>Max. of 5 lines on the overlay",
		position = 6000,
		section = overlaySection
	)
	default boolean showOverlayOffPray()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayDeservedDmg", // Not renaming this key so users keep their config.
		name = "Overlay: Show Expected Dmg", // Note: Used to be called Deserved Damage.
		description = "The overlay will display expected damage & difference.<br>Max. of 5 lines on the overlay",
		position = 7000,
		section = overlaySection
	)
	default boolean showOverlayExpectedDmg()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayDmgDealt",
		name = "Overlay: Show Dmg Dealt",
		description = "The overlay will display damage dealt.<br>Max. of 5 lines on the overlay",
		position = 8000,
		section = overlaySection
	)
	default boolean showOverlayDmgDealt()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlayMagicHits",
		name = "Overlay: Show Magic Hits",
		description = "The overlay will display successful magic hits & expected magic hits.<br>Max. of 5 lines on the overlay",
		position = 9000,
		section = overlaySection
	)
	default boolean showOverlayMagicHits()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlayOffensivePray",
		name = "Overlay: Show Offensive Pray",
		description = "The overlay will display offensive pray stats.<br>Max. of 5 lines on the overlay",
		position = 10000,
		section = overlaySection
	)
	default boolean showOverlayOffensivePray()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlayHpHealed",
		name = "Overlay: Show HP Healed",
		description = "The overlay will display hitpoints healed.<br>Max. of 5 lines on the overlay",
		position = 10500,
		section = overlaySection
	)
	default boolean showOverlayHpHealed()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlayRobeHits",
		name = "Overlay: Show Hits on Robes",
		description = "The overlay will display hits on robes ratio (melee & ranged attacks).<br>Max. of 5 lines on the overlay",
		position = 10700,
		section = overlaySection
	)
	default boolean showOverlayRobeHits()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlayTotalKoChance",
		name = "Overlay: Show Total KO Chance",
		description = "The overlay will display total KO chances and sum percentage.<br>Max. of 5 lines on the overlay",
		position = 10800,
		section = overlaySection
	)
	default boolean showOverlayTotalKoChance()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlayLastKoChance",
		name = "Overlay: Show Last KO Chance",
		description = "The overlay will display the last KO chance percentage.<br>Max. of 5 lines on the overlay",
		position = 10900,
		section = overlaySection
	)
	default boolean showOverlayLastKoChance()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showOverlayGhostBarrage",
			name = "Overlay: Show Ghost Barrage",
			description = "(Advanced): The overlay will display ghost barrage stats.<br>Max. of 5 lines on the overlay",
			position = 10950,
			section = overlaySection
	)
	default boolean showOverlayGhostBarrage()
	{
		return false;
	}

	// ================================= Gear/Ammo =================================

	@ConfigItem(
		keyName = "ringChoice",
		name = "Ring Used",
		description = "Rings used for the expected damage calculations outside of LMS.",
		position = 11000,
		section = gearAmmoSection
	)
	default RingData ringChoice()
	{
		return RingData.BERSERKER_RING_I;
	}

	@ConfigItem(
		keyName = "boltChoice",
		name = "RCB Ammo",
		description = "Bolts used for rune crossbow's expected damage calculation." +
			"<br>LMS fights always use diamond (e). Dragonfire protection not accounted for.",
		position = 12000,
		section = gearAmmoSection
	)
	default RangeAmmoData.BoltAmmo boltChoice()
	{
		return RangeAmmoData.BoltAmmo.DIAMOND_BOLTS_E;
	}

	@ConfigItem(
		keyName = "strongBoltChoice",
		name = "DCB+ Ammo",
		description = "Bolts used for stronger crossbow's expected damage calculation. (e.g DCB, ACB, ZCB, DHCB)" +
			"<br>LMS fights always use opal dragon bolts(e).",
		position = 13000,
		section = gearAmmoSection
	)
	default RangeAmmoData.StrongBoltAmmo strongBoltChoice()
	{
		return RangeAmmoData.StrongBoltAmmo.OPAL_DRAGON_BOLTS_E;
	}

	@ConfigItem(
		keyName = "bpDartChoice",
		name = "Blowpipe Ammo",
		description = "Darts used for blowpipe expected damage calculation.",
		position = 14000,
		section = gearAmmoSection
	)
	default RangeAmmoData.DartAmmo bpDartChoice()
	{
		return RangeAmmoData.DartAmmo.DRAGON_DARTS;
	}

	// ================================= Levels =================================

	@Range(
		min = LEVEL_MIN,
		max = LEVEL_MAX
	)
	@ConfigItem(
		keyName = "attackLevel",
		name = "Attack Level",
		description = "Attack level used for the expected damage calculations outside of LMS (includes potion boost).",
		position = 16000,
		section = levelsSection
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
		description = "Strength level used for the expected damage calculations outside of LMS (includes potion boost).",
		position = 17000,
		section = levelsSection
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
		description = "Defence level used for the expected damage calculations outside of LMS (includes potion boost).",
		position = 18000,
		section = levelsSection
	)
	default int defenceLevel()
	{
		return 120;
	}

	@Range(
		min = LEVEL_MIN,
		max = LEVEL_MAX
	)
	@ConfigItem(
		keyName = "rangedLevel",
		name = "Ranged Level",
		description = "Ranged level used for the expected damage calculations outside of LMS (includes potion boost).",
		position = 19000,
		section = levelsSection
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
		description = "Magic level used for the expected damage calculations outside of LMS (includes potion boost).",
		position = 20000,
		section = levelsSection
	)
	default int magicLevel()
	{
		return 99;
	}

	@Range(
		min = LEVEL_MIN,
		max = LEVEL_MAX
	)
	@ConfigItem(
		keyName = "opponentHitpointsLevel",
		name = "Opponent HP Level",
		description = "Assumed Hitpoints level for opponents when calculating KO chance.",
		position = 20100, // Place it after other levels
		section = levelsSection
	)
	default int opponentHitpointsLevel()
	{
		return 99; // Default to 99 HP
	}

	// ================================= Misc/Less-Used-General =================================

	@Range(
		min = 1,
		max = 1000
	)
	@ConfigItem(
		keyName = "fightHistoryRenderLimit",
		name = "Max Rendered Fights",
		description = "Maximum number of previous fights to be displayed and searchable in the fight history side-panel." +
			"<br>WILL cause lag spikes at very high numbers. This should be small if you have low RAM",
		position = 20500
	)
	default int fightHistoryRenderLimit()
	{
		return 200;
	}

	@Range(
		max = 1000000
	)
	@ConfigItem(
		keyName = "fightHistoryLimit",
		name = "Max Saved Fights",
		description = "Maximum number of previous fights to be saved to a local file and used for total/avg stats." +
			"<br>0 means unlimited. May cause lag spikes at extreme numbers on low-end PCs",
		position = 21000
	)
	default int fightHistoryLimit()
	{
		return 10000;
	}

	@ConfigItem(
		keyName = "exactNameFilter",
		name = "Exact Name Filter",
		description = "Makes the username filter look for an exact match (case-insensitive), rather than any name starting with the filter.<br>" +
			"This can help reduce lag while searching, especially if you've increased your Max Rendered Fights.",
		position = 22000
	)
	default boolean exactNameFilter()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dlongIsVls",
		name = "Dlong = VLS",
		description = "Track Dragon Longsword & its spec as a Vesta's Longsword for expected damage." +
			"<br>Requested/used for for DMM practice purposes.",
		position = 23000
	)
	default boolean dlongIsVls()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fightLogInChat",
		name = "Fight Log In Chat",
		description = "Display basic fight logs in trade chat during a fight." +
			"<br><strong>This is very excessive, mostly for testing/verification.<strong>",
		position = 50000
	)
	default boolean fightLogInChat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "nameFilter",
		name = "Name Filter",
		description = "Hidden config used to save user's selected name/RSN filter for the panel views.",
		position = 100000,
		hidden = true
	)
	default String nameFilter()
	{
		return "";
	}


	// ================================= On-update flags for chat message update summaries =================================
	// to avoid spamming multiple update messages for a user who was inactive, just use and overwrite one at a time.
	String updateMsgKey = "updateMsgShown1_7_8";
	@ConfigItem(
		keyName = updateMsgKey,
		name = "Update Msg flag for most recent update",
		description = "Tracks if the update chat message for the most recent update has been shown.",
		hidden = true
	)
	default boolean updateMsgShown()
	{
		return false;
	}

	// throw any updateMsgShown keys into this as we change it. Or any other config we won't be using anymore.
	public static final String[] UPDATE_MSG_KEY_GRAVEYARD = {
		"showWorldInSummary",
		"updateMsgShown1_7_7",
		"updateMsgShown1_7_6",
		"updateMsgShown1_7_4",
		"updateMsgShown1_7_2",
		"updateMsgShown1_7_1",
		"updateNote1_7_0",
		"updateNote1_6_3",
		"updateNoteMay72025Shown_v2"
	};
}
