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

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.inject.Provides;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.HeadIcon;
import net.runelite.api.HitsplatID;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@PluginDescriptor(
	name = "PvP Performance Tracker"
)
public class PvpPerformanceTrackerPlugin extends Plugin
{
	// static fields
	public static final String PLUGIN_VERSION = "1.5.4";
	public static final String CONFIG_KEY = "pvpperformancetracker";
	public static final String DATA_FOLDER = "pvp-performance-tracker";
	public static final String FIGHT_HISTORY_DATA_FNAME = "FightHistoryData.json";
	public static final File FIGHT_HISTORY_DATA_DIR;
	public static PvpPerformanceTrackerConfig CONFIG;
	public static PvpPerformanceTrackerPlugin PLUGIN;
	public static Image PLUGIN_ICON;
	public static AsyncBufferedImage DEFAULT_NONE_SYMBOL; // save bank filler image to display a generic "None" or N/A state.
	public static Gson GSON;

	// Last man standing map regions, not including lobby
	private static final Set<Integer> LAST_MAN_STANDING_REGIONS = ImmutableSet.of(12344, 12600, 13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432);

	static
	{
		FIGHT_HISTORY_DATA_DIR = new File(RuneLite.RUNELITE_DIR, DATA_FOLDER);
		FIGHT_HISTORY_DATA_DIR.mkdirs();
	}

	// "native"/core RL fields/injected fields
	@Getter(AccessLevel.PACKAGE)
	private NavigationButton navButton;
	private boolean navButtonShown = false;

	@Getter(AccessLevel.PACKAGE)
	private PvpPerformanceTrackerPanel panel;

	@Inject
	private PvpPerformanceTrackerConfig config;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Getter
	@Inject
	private Client client;

	@Getter
	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Getter
	@Inject
	private RuneLiteConfig runeliteConfig;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PvpPerformanceTrackerOverlay overlay;

	@Getter
	@Inject
	private ItemManager itemManager;

	@Inject
	private ScheduledExecutorService executor;

	// custom fields/props
	public ArrayList<FightPerformance> fightHistory;
	@Getter
	private FightPerformance currentFight;
	private Map<Integer, ImageIcon> spriteCache; // sprite cache since a small amount of sprites is re-used a lot
	// do not cache items in the same way since we could potentially cache a very large amount of them.

	// #################################################################################################################
	// ##################################### Core RL plugin functions & RL Events ######################################
	// #################################################################################################################

	@Provides
	PvpPerformanceTrackerConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvpPerformanceTrackerConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		CONFIG = config; // save static instances of config/plugin to easily use in
		PLUGIN = this;   // other contexts without passing them all the way down or injecting
		fightHistory = new ArrayList<>();

		GSON = new GsonBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.registerTypeAdapter(Double.class, (JsonSerializer<Double>) (value, theType, context) ->
				value.isNaN() ? new JsonPrimitive(0) // Convert NaN to zero, otherwise, return as BigDecimal with scale of 3.
					: new JsonPrimitive(BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP))
			).create();

		if (!config.pluginVersion().equals(PLUGIN_VERSION))
		{
			this.update(config.pluginVersion());
		}

		panel = injector.getInstance(PvpPerformanceTrackerPanel.class);
		final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "/skull_red.png");
		PLUGIN_ICON = new ImageIcon(icon).getImage();
		navButton = NavigationButton.builder()
			.tooltip("PvP Fight History")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();

		importFightHistoryData();

		// add the panel's nav button depending on config
		if (config.showFightHistoryPanel() &&
			(!config.restrictToLms() || (client.getGameState() == GameState.LOGGED_IN && isAtLMS())))
		{
			navButtonShown = true;
			clientToolbar.addNavigation(navButton);
		}

		overlayManager.add(overlay);

		spriteCache = new HashMap<>(); // prepare sprite cache

		// prepare default N/A or None symbol for eventual use.
		clientThread.invokeLater(() -> DEFAULT_NONE_SYMBOL = itemManager.getImage(20594));
	}

	private void update(String oldVersion)
	{
//		switch (oldVersion)
//		{
//			case "1.4.2":
//				// update logic....
//				break;
//		}

		configManager.setConfiguration(CONFIG_KEY, "pluginVersion", PLUGIN_VERSION);
	}

	@Override
	protected void shutDown() throws Exception
	{
		updateFightHistoryData();

		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(CONFIG_KEY)) { return; }

		switch(event.getKey())
		{
			// if a user enables the panel or restricts/unrestricts the location to LMS, hide/show the panel accordingly
			case "showFightHistoryPanel":
			case "restrictToLms":
				boolean isAtLms = isAtLMS();
				if (!navButtonShown && config.showFightHistoryPanel() &&
					(!config.restrictToLms() || isAtLms))
				{
					SwingUtilities.invokeLater(() -> clientToolbar.addNavigation(navButton));
					navButtonShown = true;
				}
				else if (navButtonShown && (!config.showFightHistoryPanel() || (config.restrictToLms() && !isAtLms)))
				{
					SwingUtilities.invokeLater(() -> clientToolbar.removeNavigation(navButton));
					navButtonShown = false;
				}
				break;
			// If a user makes any changes to the overlay configuration, reset the shown lines accordingly
			case "showOverlayTitle":
			case "showOverlayNames":
			case "showOverlayOffPray":
			case "showOverlayDeservedDmg":
			case "showOverlayDmgDealt":
			case "showOverlayMagicHits":
			case "showOverlayOffensivePray":
			case "showOverlayHpHealed":
			case "showOverlayGhostBarrage":
				overlay.setLines();
				break;
			// If the user updates the fight history limit, remove fights as necessary
			case "fightHistoryLimit":
			case "fightHistoryRenderLimit":
				if (config.fightHistoryLimit() > 0 && fightHistory.size() > config.fightHistoryLimit())
				{
					int numToRemove = fightHistory.size() - config.fightHistoryLimit();
					// Remove oldest fightHistory until the size is smaller than the limit.
					// Should only remove one fight in most cases.
					fightHistory.removeIf((FightPerformance f) -> fightHistory.indexOf(f) < numToRemove);
				}
				panel.rebuild();
				break;
			case "exactNameFilter":
				panel.rebuild();
				break;
			case "settingsConfigured":
				boolean enableConfigWarning = !config.settingsConfigured();
				panel.setConfigWarning(enableConfigWarning);
				break;
			// potential future code for level presets/dynamic config if RL ever supports it.
//			case "attackLevel":
//			case "strengthLevel":
//			case "defenceLevel":
//			case "rangedLevel":
//			case "magicLevel":
//				log.info("TEST-just set a level");
//				configManager.setConfiguration(CONFIG_KEY, "levelPresetChoice", LevelConfigPreset.CUSTOM);
//				break;
//			case "levelPresetChoice":
//				log.info("TEST- just chose level preset choice");
//				LevelConfigPreset p = config.levelPresetChoice();
//				switch (p)
//				{
//					case CUSTOM:
//						break;
//					case LMS_STATS:
//					case NH_STAKE:
//						configManager.setConfiguration(CONFIG_KEY, "attackLevel", p.getAtk());
//						configManager.setConfiguration(CONFIG_KEY, "strengthLevel", p.getStr());
//						configManager.setConfiguration(CONFIG_KEY, "defenceLevel", p.getDef());
//						configManager.setConfiguration(CONFIG_KEY, "rangedLevel", p.getRange());
//						configManager.setConfiguration(CONFIG_KEY, "magicLevel", p.getMage());
//						break;
//
//				}
//				break;
		}
	}

	// Keep track of a player's new target using this event.
	// It's worth noting that if you aren't in a fight, all player interactions including
	// trading & following will trigger a new fight and a new opponent. Due to this, set the lastFightTime
	// (in FightPerformance) in the past to only be 5 seconds before the time NEW_FIGHT_DELAY would trigger
	// and unset the opponent, in case the player follows a different player before actually starting
	// a fight or getting attacked. In other words, remain skeptical of the validity of this event.
	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (config.restrictToLms() && !isAtLMS())
		{
			return;
		}

		stopFightIfOver();

		// if the client player already has a valid opponent,
		// or the event source/target aren't players, skip any processing.
		if ((hasOpponent() && currentFight.fightStarted())
			|| !(event.getSource() instanceof Player)
			|| !(event.getTarget() instanceof Player))
		{
			return;
		}

		Actor opponent;

		// If the event source is the player, then it is the player interacting with their potential opponent.
		if (event.getSource().equals(client.getLocalPlayer()))
		{
			opponent = event.getTarget();
		}
		else if (event.getTarget().equals(client.getLocalPlayer()))
		{
			opponent = event.getSource();
		}
		else // if neither source or target was the player, skip
		{
			return;
		}

		// start a new fight with the new found opponent, if a new one.
		if (!hasOpponent() || !currentFight.getOpponent().getName().equals(opponent.getName()))
		{
			currentFight = new FightPerformance(client.getLocalPlayer(), (Player)opponent);
			overlay.setFight(currentFight);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		// hide or show panel depending if config is restricted to LMS and if player is at LMS
		if (config.restrictToLms())
		{
			if (isAtLMS())
			{
				if (!navButtonShown && config.showFightHistoryPanel())
				{
					clientToolbar.addNavigation(navButton);
					navButtonShown = true;
				}
			}
			else
			{
				if (navButtonShown)
				{
					clientToolbar.removeNavigation(navButton);
					navButtonShown = false;
				}
			}
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		stopFightIfOver();

		// delay the animation processing, since we will also want to use equipment data for deserved
		// damage, and equipment updates are loaded after the animation updates.
		clientThread.invokeLater(() ->
		{
			if (hasOpponent() && event.getActor() instanceof Player && event.getActor().getName() != null)
			{
				currentFight.checkForAttackAnimations((Player)event.getActor(), new CombatLevels(client));
			}
		});
	}

	@Subscribe
	// track damage dealt/taken
	public void onHitsplatApplied(HitsplatApplied event)
	{
		int hitType = event.getHitsplat().getHitsplatType();

		// if there's no opponent, the target is not a player, or the hitsplat is not relevant to pvp damage,
		// skip the hitsplat. Otherwise, add it to the fight, which will only include it if it is one of the
		// Fighters in the fight being hit.
		if (!hasOpponent() || !(event.getActor() instanceof Player) ||
			!(hitType == HitsplatID.DAMAGE_ME || hitType == HitsplatID.DAMAGE_ME_ORANGE || hitType == HitsplatID.DAMAGE_OTHER_ORANGE || hitType == HitsplatID.DAMAGE_OTHER || hitType == HitsplatID.DAMAGE_MAX_ME || hitType == HitsplatID.DAMAGE_MAX_ME_ORANGE ||
				hitType == HitsplatID.POISON || hitType == HitsplatID.VENOM))
		{
			return;
		}

		currentFight.addDamageDealt(event.getActor().getName(), event.getHitsplat().getAmount());
	}

	@Subscribe
	// track hitpoints healed & ghost barrages for main competitor/client player
	public void onStatChanged(StatChanged statChanged)
	{
		Skill skill = statChanged.getSkill();
		if (!hasOpponent()) { return; }

		if (skill == Skill.HITPOINTS)
		{
			currentFight.updateCompetitorHp(client.getBoostedSkillLevel(Skill.HITPOINTS));
		}

		if (skill == Skill.MAGIC)
		{
			int magicXp = client.getSkillExperience(Skill.MAGIC);
			if (magicXp > currentFight.competitor.getLastGhostBarrageCheckedMageXp())
			{
				currentFight.competitor.setLastGhostBarrageCheckedMageXp(PLUGIN.getClient().getSkillExperience(Skill.MAGIC));
				clientThread.invokeLater(this::checkForGhostBarrage);
			}
		}
	}

	@Subscribe
	// track ghost barrages for main competitor/client player
	public void onFakeXpDrop(FakeXpDrop fakeXpDrop)
	{
		if (!hasOpponent() || fakeXpDrop.getSkill() != Skill.MAGIC) { return; }

		clientThread.invokeLater(this::checkForGhostBarrage);
	}

	// if the player gained magic xp but doesn't have a magic-attack animation, consider it as a ghost barrage.
	// however this won't be added as a normal attack, it is for an extra ghost-barrage statistic as
	// we can only detect this for the local player
	private void checkForGhostBarrage()
	{
		if (!hasOpponent()) { return; }

		currentFight.checkForLocalGhostBarrage(new CombatLevels(client), client.getLocalPlayer());
	}

	// When the config is reset, also reset the fight history data, as a way to restart
	// if the current data is causing problems.
	@Override
	public void resetConfiguration()
	{
		super.resetConfiguration();
		resetFightHistory();
	}

	// when the client shuts down, save the fight history data locally.
	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		event.waitFor(executor.submit(this::updateFightHistoryData));
	}

	// #################################################################################################################
	// ################################## Plugin-specific functions & global helpers ###################################
	// #################################################################################################################

	// Returns true if the player has an opponent.
	private boolean hasOpponent()
	{
		return currentFight != null;
	}

	private void stopFightIfOver()
	{
		if (hasOpponent() && currentFight.isFightOver())
		{
			// add fight to fight history if it actually started
			if (currentFight.fightStarted())
			{
				addToFightHistory(currentFight);
			}
			currentFight = null;
		}
	}

	// save the currently loaded fightHistory to the local json data so it is saved for the next client launch.
	private void updateFightHistoryData()
	{
		// silently ignore errors, which shouldn't really happen - but if they do, don't prevent the plugin
		// from continuing to work, even if there are issues saving the data.
		try
		{
			File fightHistoryData = new File(FIGHT_HISTORY_DATA_DIR, FIGHT_HISTORY_DATA_FNAME);
			Writer writer = new FileWriter(fightHistoryData);
			GSON.toJson(fightHistory, writer);
			writer.flush();
			writer.close();
		}
		catch (Exception e)
		{
			log.warn("Error ignored while updating fight history data: " + e.getMessage());
		}
	}

	// add fight to loaded fight history
	void addToFightHistory(FightPerformance fight)
	{
		if (fight == null) { return; }
		fightHistory.add(fight);
		// no need to sort, since they sort chronologically, but they should automatically be added that way.

		// remove fights as necessary to respect the fightHistoryLimit.
		if (config.fightHistoryLimit() > 0 && fightHistory.size() > config.fightHistoryLimit())
		{
			int numToRemove = fightHistory.size() - config.fightHistoryLimit();
			// Remove oldest fightHistory until the size is equal to the limit.
			// Should only remove one fight in most cases.
			fightHistory.removeIf((FightPerformance f) -> fightHistory.indexOf(f) < numToRemove);
			panel.rebuild();
		}
		else
		{
			panel.addFight(fight);
		}
	}

	// import complete fight history data from the saved json data file
	// this function only handles the direct file processing and json deserialization.
	// more specific FightPerformance processing is done in importFights()
	void importFightHistoryData()
	{
		// catch and ignore any errors we may have forgotten to handle - the import will fail but at least the plugin
		// will continue to function. This should only happen if their fight history data is corrupted/outdated.
		// The user will be notified by a modal if this happens.
		try
		{
			FIGHT_HISTORY_DATA_DIR.mkdirs();
			File fightHistoryData = new File(FIGHT_HISTORY_DATA_DIR, FIGHT_HISTORY_DATA_FNAME);

			// if the fight history data file doesn't exist, create it with an empty array.
			if (!fightHistoryData.exists())
			{
				Writer writer = new FileWriter(fightHistoryData);
				writer.write("[]");
				writer.close();
			}

			// read the saved fights from the file
			List<FightPerformance> savedFights = Arrays.asList(
				GSON.fromJson(new FileReader(fightHistoryData), FightPerformance[].class));

			fightHistory.clear();
			importFights(savedFights);
		}
		catch (Exception e)
		{
			log.warn("Error while deserializing fight history data: " + e.getMessage());
			// Display no modal for this error since it could happen on client load and that has odd behavior.
			return;
		}

		panel.rebuild();
	}

	// import additional/extra fight history data supplied by the user
	// this only does the direct json deserialization and success response (modals)
	// more specific FightPerformance processing is done in importFights()
	public void importUserFightHistoryData(String data)
	{
		try
		{
			// read saved fights from the data string and import them
			List<FightPerformance> savedFights = Arrays.asList(GSON.fromJson(data, FightPerformance[].class));
			importFights(savedFights);
			createConfirmationModal(true, "Fight history data was successfully imported.");
		}
		catch (Exception e)
		{
			log.warn("Error while importing user's fight history data: " + e.getMessage());
			// If an error was detected while deserializing fights, display that as a message dialog.
			createConfirmationModal(false, "Fight history data was invalid, and could not be imported.");
			return;
		}

		panel.rebuild();
	}

	// set fight log names after importing since they aren't serialized but are on the parent class
	public void initializeImportedFight(FightPerformance f)
	{
		// check for nulls in case the data was corrupted and entries are corrupted.
		if (f.getCompetitor() == null || f.getOpponent() == null ||
			f.getCompetitor().getFightLogEntries() == null || f.getOpponent().getFightLogEntries() == null)
		{
			return;
		}

		f.getCompetitor().getFightLogEntries().forEach((FightLogEntry l) ->
			l.attackerName = f.getCompetitor().getName());
		f.getOpponent().getFightLogEntries().forEach((FightLogEntry l) ->
			l.attackerName = f.getOpponent().getName());
	}

	// process and add a list of deserialized json fights to the currently loaded fights
	// can throw NullPointerException if some of the serialized data is corrupted
	void importFights(List<FightPerformance> fights) throws NullPointerException
	{
		if (fights == null || fights.size() < 1) { return; }

		fights.removeIf(Objects::isNull);
		fightHistory.addAll(fights);
		fightHistory.sort(FightPerformance::compareTo);

		// remove fights to respect the fightHistoryLimit.
		if (config.fightHistoryLimit() > 0 && fightHistory.size() > config.fightHistoryLimit())
		{
			int numToRemove = fightHistory.size() - config.fightHistoryLimit();
			// Remove oldest fightHistory until the size is equal to the limit.
			// Should only remove one fight in most cases.
			fightHistory.removeIf((FightPerformance f) -> fightHistory.indexOf(f) < numToRemove);
		}

		// set fight log names since they aren't serialized but are on the parent class
		for (FightPerformance f : fightHistory)
		{
			initializeImportedFight(f);
		}
	}

	// reset the loaded fight history as well as the saved json data
	public void resetFightHistory()
	{
		fightHistory.clear();
		updateFightHistoryData();
		panel.rebuild();
	}

	// remove a fight from the loaded fight history
	public void removeFight(FightPerformance fight)
	{
		fightHistory.remove(fight);
		panel.rebuild();
	}

	public boolean isAtLMS()
	{
		final int[] mapRegions = client.getMapRegions();

		for (int region : LAST_MAN_STANDING_REGIONS)
		{
			if (ArrayUtils.contains(mapRegions, region))
			{
				return true;
			}
		}

		return false;
	}

	// Send a message to the chat. Send them messages to the trade chat since it is uncommonly
	// used while fighting, but game, public, private, and clan chat all have their uses.
	public void sendChatMessage(String chatMessage)
	{
		chatMessageManager
			.queue(QueuedMessage.builder()
				.type(ChatMessageType.TRADE)
				.runeLiteFormattedMessage(chatMessage)
				.build());
	}

	// create a simple confirmation modal, using a custom dialog so it can be always
	// on top (if the client is, to prevent being stuck under the client).
	public void createConfirmationModal(boolean success, String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			JOptionPane optionPane = new JOptionPane();
			optionPane.setMessage(message);
			optionPane.setOptionType(JOptionPane.DEFAULT_OPTION);
			JDialog dialog = optionPane.createDialog(panel, "PvP Tracker: " + (success ? "Success" : "Error"));
			if (dialog.isAlwaysOnTopSupported())
			{
				dialog.setAlwaysOnTop(runeliteConfig.gameAlwaysOnTop());
			}
			dialog.setIconImage(PLUGIN_ICON);
			dialog.setVisible(true);
		});
	}

	// save the complete fight history data to the clipboard.
	public void exportFightHistory()
	{
		String fightHistoryDataJson = GSON.toJson(fightHistory.toArray(new FightPerformance[0]), FightPerformance[].class);
		final StringSelection contents = new StringSelection(fightHistoryDataJson);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, null);

		createConfirmationModal(true, "Fight history data was copied to the clipboard.");
	}

	public void exportFight(FightPerformance fight)
	{
		if (fight == null) { return; }
		String fightDataJson = GSON.toJson(fight, FightPerformance.class);
		final StringSelection contents = new StringSelection(fightDataJson);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, null);

		boolean success = false;
		String confirmMessage;
		if (fight.getCompetitor() != null && fight.getCompetitor().getName() != null &&
			fight.getOpponent() != null && fight.getOpponent().getName() != null)
		{
			success = true;
			confirmMessage = "Fight data of " + fight.getCompetitor().getName() + " vs " +
				fight.getOpponent().getName() + " was copied to the clipboard.";
		}
		else
		{
			confirmMessage = "Warning: Fight data was copied to the clipboard, but it's likely corrupted.";
		}
		createConfirmationModal(success, confirmMessage);
	}

	public void copyFightAsDiscordMsg(FightPerformance fight)
	{
		if (fight == null) { return; }

		String fightMsg = fight.getAsDiscordMessage();

		final StringSelection contents = new StringSelection(fightMsg);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, null);

		createConfirmationModal(true, "Discord message of fight data was copied to the clipboard.");
	}

	// retrieve offensive pray as SpriteID since that's all we will directly use it for,
	// aside from comparison/equality checks, so we save an extra mapping this way
	public int currentlyUsedOffensivePray()
	{
		return client.isPrayerActive(Prayer.PIETY) 				? SpriteID.PRAYER_PIETY :
			client.isPrayerActive(Prayer.ULTIMATE_STRENGTH) ? SpriteID.PRAYER_ULTIMATE_STRENGTH :
				client.isPrayerActive(Prayer.RIGOUR) 			? SpriteID.PRAYER_RIGOUR :
					client.isPrayerActive(Prayer.EAGLE_EYE) 		? SpriteID.PRAYER_EAGLE_EYE :
						client.isPrayerActive(Prayer.AUGURY) 			? SpriteID.PRAYER_AUGURY :
							client.isPrayerActive(Prayer.MYSTIC_MIGHT)		? SpriteID.PRAYER_MYSTIC_MIGHT :
								0;
	}

	// returns SpriteID for a given HeadIcon. returns -1 if not found
	public int getSpriteForHeadIcon(HeadIcon icon)
	{
		if (icon == null) { return -1; }
		switch (icon)
		{
			case MELEE: return SpriteID.PRAYER_PROTECT_FROM_MELEE;
			case RANGED: return SpriteID.PRAYER_PROTECT_FROM_MISSILES;
			case MAGIC: return SpriteID.PRAYER_PROTECT_FROM_MAGIC;
			case SMITE: return SpriteID.PRAYER_SMITE;
			case RETRIBUTION: return SpriteID.PRAYER_RETRIBUTION;
			case REDEMPTION: return SpriteID.PRAYER_REDEMPTION;
			default: return -1;
		}
	}

	public int getSpriteForSkill(Skill skill)
	{
		switch (skill)
		{
			case ATTACK: return SpriteID.SKILL_ATTACK;
			case STRENGTH: return SpriteID.SKILL_STRENGTH;
			case DEFENCE: return SpriteID.SKILL_DEFENCE;
			case RANGED: return SpriteID.SKILL_RANGED;
			case MAGIC: return SpriteID.SKILL_MAGIC;
			case HITPOINTS: return SpriteID.SKILL_HITPOINTS;
			default: return -1;
		}
	}

	public void addSpriteToLabelIfValid(JLabel label, int spriteId, Runnable swingCallback)
	{
		// Invalid sprite: set icon to bank filler and tooltip to N/A
		if (spriteId <= 0)
		{
			DEFAULT_NONE_SYMBOL.addTo(label);
			label.setToolTipText("N/A");

			if (swingCallback != null)
			{
				SwingUtilities.invokeLater(swingCallback);
			}
			return;
		}

		// if sprite id found in sprite cache: use it.
		if (spriteCache.containsKey(spriteId))
		{
			label.setIcon(spriteCache.get(spriteId));
			if (swingCallback != null)
			{
				SwingUtilities.invokeLater(swingCallback);
			}
			return;
		}

		// if sprite id wasn't in sprite cache: get it, use it and add it to the cache
		clientThread.invokeLater(() ->
		{
			BufferedImage sprite = spriteManager.getSprite(spriteId, 0);
			if (sprite != null)
			{
				ImageIcon icon = new ImageIcon(sprite);
				spriteCache.put(spriteId, icon);
				label.setIcon(icon);
			}
			else
			{
				DEFAULT_NONE_SYMBOL.addTo(label);
				label.setToolTipText("N/A");
			}

			if (swingCallback != null)
			{
				SwingUtilities.invokeLater(swingCallback);
			}
		});
	}

	public void addSpriteToLabelIfValid(JLabel label, int spriteId)
	{
		addSpriteToLabelIfValid(label, spriteId, null);
	}

	// if verifyId is true, takes in itemId directly from PlayerComposition, so need to -512 for actual itemid
	// otherwise, assume valid itemId
	public void addItemToLabelIfValid(JLabel label, int itemId, boolean verifyId, Runnable swingCallback, String tooltipOverride)
	{
		if (itemId > 512 || !verifyId)
		{
			final int finalItemId = itemId - (verifyId ? 512 : 0);
			clientThread.invokeLater(() -> {
				itemManager.getImage(finalItemId).addTo(label);
				if (tooltipOverride != null && tooltipOverride.length() > 0)
				{
					label.setToolTipText(tooltipOverride);
				}
				else
				{
					String name = itemManager.getItemComposition(finalItemId).getName();
					label.setToolTipText(name != null ? name : "Item Name Not Found");
				}

				if (swingCallback != null)
				{
					SwingUtilities.invokeLater(swingCallback);
				}
			});
		}
		else
		{
			DEFAULT_NONE_SYMBOL.addTo(label);
			label.setToolTipText("N/A: empty slot or invalid item");
			if (swingCallback != null)
			{
				SwingUtilities.invokeLater(swingCallback);
			}
		}
	}

	public void addItemToLabelIfValid(JLabel label, RangeAmmoData data, boolean verifyId, Runnable swingCallback)
	{
		addItemToLabelIfValid(label, data.getItemId(), verifyId, swingCallback, data.toString());
	}

	public void addItemToLabelIfValid(JLabel label, int itemId, boolean verifyId, Runnable swingCallback)
	{
		addItemToLabelIfValid(label, itemId, verifyId, swingCallback, null);
	}

	public void addItemToLabelIfValid(JLabel label, int itemId)
	{
		addItemToLabelIfValid(label, itemId, true, null);
	}

	// fix an itemId that came from getPlayerComposition().getEquipmentIds()
	// you have to do -512 if >512 if it's a real item.
	public static int fixItemId(int itemId)
	{
		return itemId > 512 ? itemId - 512 : itemId;
	}

	// create new array so we don't modify original array
	public static int[] fixItemIds(int[] itemIds)
	{
		if (itemIds == null || itemIds.length < 1)
		{
			return new int[] { 0 };
		}
		int[] fixedItemIds = new int[itemIds.length];
		for (int i = 0; i < itemIds.length; i++)
		{
			fixedItemIds[i] = fixItemId(itemIds[i]);
		}

		return fixedItemIds;
	}

	public void updateNameFilterConfig(String newFilterName)
	{
		configManager.setConfiguration(CONFIG_KEY, "nameFilter", newFilterName.trim().toLowerCase());
	}
}
