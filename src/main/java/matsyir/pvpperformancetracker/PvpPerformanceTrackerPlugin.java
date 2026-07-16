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
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.inject.Provides;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
import matsyir.pvpperformancetracker.controllers.FightPerformanceSerializer;
import matsyir.pvpperformancetracker.controllers.Fighter;
import matsyir.pvpperformancetracker.controllers.PvpHubFightSync;
import matsyir.pvpperformancetracker.controllers.PvpHubSyncRetryState;
import matsyir.pvpperformancetracker.controllers.PvpHubUploader;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.models.HitsplatInfo;
import matsyir.pvpperformancetracker.models.PrayerType;
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import matsyir.pvpperformancetracker.utils.PvpColorScheme;
import matsyir.pvpperformancetracker.utils.PvpHubPrivacy;
import matsyir.pvpperformancetracker.utils.PvpUtils;
import matsyir.pvpperformancetracker.views.TotalStatsPanel;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.HitsplatID;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Skill;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
@PluginDescriptor(
	name = "PvP Performance Tracker"
)
public class PvpPerformanceTrackerPlugin extends Plugin
{
	// static fields

	// reminder: the version number update is needed in a few different places.
	// Run a find-all of the old version number before updating version.
	public static final String PLUGIN_VERSION = "1.8.7";
	public static final String CONFIG_KEY = "pvpperformancetracker";
	// Data folder naming history:
	// "pvp-performance-tracker": From release, until 1.5.9 update @ 2024-08-19
	// "pvp-performance-tracker2": From 1.5.9 update, until present
	public static final String DATA_FOLDER = "pvp-performance-tracker2";


	public static final File BASE_DATA_DIR;

	public static PvpPerformanceTrackerConfig CONFIG;
	public static PvpPerformanceTrackerPlugin PLUGIN;
	public static Image PLUGIN_ICON;
	public static AsyncBufferedImage DEFAULT_NONE_SYMBOL; // save bank filler image to display a generic "None" or N/A state.
	public static Gson GSON;

	// Last man standing map regions, including ferox enclave
	private static final Set<Integer> LAST_MAN_STANDING_REGIONS = ImmutableSet.of(12344, 12600, 13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432);
	private static final int PVP_HUB_SYNC_MAX_ATTEMPTS = 5;
	private static final long PVP_HUB_SYNC_RETRY_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(1);
	private static final long PVP_HUB_UPLOAD_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(5);

	static
	{
		BASE_DATA_DIR = new File(RuneLite.RUNELITE_DIR, DATA_FOLDER);
		BASE_DATA_DIR.mkdirs();
	}

	// "native"/core RL fields/injected fields
	@Getter(AccessLevel.PACKAGE)
	private NavigationButton navButton;
	private boolean navButtonShown = false;

	@Getter
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

	@Getter
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

	@Inject
	private Gson injectedGson;

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private EventBus eventBus;

	@Inject
	private PluginManager pluginManager;

	// custom fields/props
	public ArrayDeque<FightPerformance> fightHistory;
	public ArrayDeque<FightPerformance> sessionFightHistory;
	@Getter
	private FightPerformance currentFight;
	private Map<Integer, ImageIcon> spriteCache; // sprite cache since a small amount of sprites is re-used a lot
	// do not cache items in the same way since we could potentially cache a very large amount of them.
	private final Map<Integer, List<HitsplatInfo>> hitsplatBuffer = new HashMap<>();
	private final Map<Integer, List<HitsplatInfo>> incomingHitsplatsBuffer = new ConcurrentHashMap<>(); // Stores hitsplats *received* by players per tick.
	private final Map<String, Integer> lastNonGmaulSpecTickByAttacker = new ConcurrentHashMap<>();
	private final PvpHubSyncRetryState pendingPvpHubSyncs = new PvpHubSyncRetryState(PVP_HUB_SYNC_MAX_ATTEMPTS, PVP_HUB_SYNC_RETRY_DELAY_MILLIS);
	private File pvpHubSyncedFightsDir;

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
		fightHistory = new ArrayDeque<>();
		sessionFightHistory = new ArrayDeque<>();

		GSON = injectedGson.newBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.registerTypeAdapter(Double.class, (JsonSerializer<Double>) (value, theType, context) ->
				value.isNaN() ? new JsonPrimitive(0) // Convert NaN to zero, otherwise, return as BigDecimal with scale of 3.
					: new JsonPrimitive(BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP))
			).create();
		pvpHubSyncedFightsDir = new File(BASE_DATA_DIR, PvpHubFightSync.SYNCED_FIGHTS_DIR_NAME);
		pvpHubSyncedFightsDir.mkdirs();

		if (!config.pluginVersion().equals(PLUGIN_VERSION))
		{
			this.update(config.pluginVersion());
		}

		panel = injector.getInstance(PvpPerformanceTrackerPanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/skull_red.png");
		PLUGIN_ICON = new ImageIcon(icon).getImage();
		navButton = NavigationButton.builder()
			.tooltip("PvP Fight History")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();

		FightPerformanceSerializer.deserializeFightHistory(this::importFights);
		executor.scheduleWithFixedDelay(this::syncPendingPvpHubFights, 60, 60, TimeUnit.SECONDS);

		// add the panel's nav button depending on config
		if (config.showFightHistoryPanel() &&
			(!config.restrictToLms() || (client.getGameState() == GameState.LOGGED_IN && isAtLmsIncludingFerox())))
		{
			navButtonShown = true;
			clientToolbar.addNavigation(navButton);
		}

		overlayManager.add(overlay);

		spriteCache = new HashMap<>(); // prepare sprite cache

		// prepare default N/A or None symbol for eventual use.
		clientThread.invokeLater(() -> DEFAULT_NONE_SYMBOL = itemManager.getImage(20594));

		TotalStatsPanel.loadBackgroundImages();

		// Explicitly force rebuild panel after all setup and import.
        SwingUtilities.invokeLater(() -> {
            if (panel != null) {
                panel.enqueueRebuild(true);
            }
        });

	}

	@Override
	protected void shutDown() throws Exception
	{
		FightPerformanceSerializer.serializeSessionFightHistory();

		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);
	}

	private boolean colorConfigEventsEnabled = true;
	public void disableColorConfigEvents()
	{
		colorConfigEventsEnabled = false;
	}
	public void enableColorConfigEvents()
	{
		colorConfigEventsEnabled = true;
	}
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(CONFIG_KEY)) { return; }

		if (event.getKey().startsWith("statisticLineEnabled_"))
		{
			panel.enqueueRebuild();
			return;
		}

		switch(event.getKey())
		{
			// if a user enables the panel or restricts/unrestricts the location to LMS, hide/show the panel accordingly
			case "showFightHistoryPanel":
			case "restrictToLms":
				boolean isAtLms = isAtLmsIncludingFerox();
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
			case "showOverlayExpectedDmg":
			case "showOverlayDmgDealt":
			case "showOverlayMagicHits":
			case "showOverlayOffensivePray":
			case "showOverlayHpHealed":
			case "showOverlayGhostBarrage":
				overlay.setLines();
				break;
			// If the user updates the fight history limit, remove fights as necessary
			case "fightHistoryLimit":
				while (config.fightHistoryLimit() > 0 && fightHistory.size() > config.fightHistoryLimit())
				{
					fightHistory.removeFirst();
				}
				break;
			case "settingsConfigured":
				boolean enableConfigWarning = !config.settingsConfigured();
				panel.setConfigWarning(enableConfigWarning);
				break;
			case "robeHitFilter":
				recalculateAllRobeHits(true);
				break;
				case "uploadFightsToPvpHub":
					if (!config.uploadFightsToPvpHub())
					{
						pendingPvpHubSyncs.clear();
					}
					if (panel != null)
					{
						panel.updatePvpHubHiddenName();
						panel.updatePopupMenuForPvpHubConfig();
					}
					break;
				case "hideRsnOnPvpHub":
					if (panel != null)
					{
					panel.updatePvpHubHiddenName();
					panel.updatePopupMenuForPvpHubConfig();
				}
				break;
			// provide presets for panel Colors: It works, although the config doesn't update itself when we force values while it's visible.
			case "panelColorPreset":
				if (!colorConfigEventsEnabled || config.panelColorPreset() == null || config.panelColorPreset() == PvpColorScheme.PanelColorPreset.CUSTOM)
				{
					break;
				}

				config.panelColorPreset().applyToConfig(configManager);
				panel.enqueueRebuild();
				break;
			case "successColor":
			case "unsuccessColor":
			case "neutralColor":
			case "successDmgColor":
			case "unsuccessDmgColor":
			case "neutralDmgColor":
			case "gbColor":
				if (!colorConfigEventsEnabled)
				{
					break;
				}
				panel.enqueueRebuild();
				configManager.setConfiguration(CONFIG_KEY, "panelColorPreset", PvpColorScheme.PanelColorPreset.CUSTOM);
				break;
			case "displayPanelSocialButtons":
				panel.updateSocialButtons();
				break;
			case "fightHistoryRenderLimit":
			case "exactNameFilter":
			case "centerPanelLabels":
			case "hidePanelBgImages":
			case "worldDisplayChoice":
			case "displayUnsyncedFightWarning":
				panel.enqueueRebuild();
				break;
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
		if (config.restrictToLms() && !isInLmsMatch())
		{
			return;
		}

		checkForFightEnd();

		// if the client player already has a valid opponent AND the fight has started,
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

		// start a new fight with the newfound opponent, if a new one.
		if (!hasOpponent() || !currentFight.getOpponent().getName().equals(opponent.getName()))
		{
			currentFight = new FightPerformance(client.getLocalPlayer(), (Player)opponent);
			overlay.setFight(currentFight);
			hitsplatBuffer.clear();
			incomingHitsplatsBuffer.clear();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		sendUpdateChatMessage();

		// hide or show panel depending if config is restricted to LMS and if player is at LMS
		if (config.restrictToLms())
		{
			if (isAtLmsIncludingFerox())
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
		if (!hasOpponent()) { return; }

		checkForFightEnd();

		Actor actor = event.getActor();
		if (!(actor instanceof Player) || actor.getName() == null)
		{
			return;
		}

		Player eventSource = (Player) actor;
		AnimationData animationData = AnimationData.fromId(eventSource.getAnimation());
		if (animationData == null)
		{
			return;
		}
		int animationTick = client.getTickCount();
		long animationTime = Instant.now().toEpochMilli();

		// delay the animation processing, since we will also want to use equipment data for expected
		// damage, and equipment updates are loaded after the animation updates.
		clientThread.invokeLater(() ->
		{
			if (hasOpponent() && eventSource.getName() != null)
			{
				Actor interacting = eventSource.getInteracting();
				if (!(interacting instanceof Player) || interacting.getName() == null)
				{
					return;
				}

				currentFight.checkForAttackAnimations(
					eventSource,
					interacting.getName(),
					animationData,
					animationTick,
					animationTime,
					new CombatLevels(client));
			}
		});
	}

	@Subscribe
	// track damage dealt/taken
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor target;

		// if there's no opponent, the target is not a player, or the hitsplat is not relevant to pvp damage,
		// skip the hitsplat. Otherwise, add it to the fight, which will only include it if it is one of the
		// Fighters in the fight being hit.
		if (!hasOpponent() || !((target = event.getActor()) instanceof Player))
		{
			return;
		}

		int hitType = event.getHitsplat().getHitsplatType();
		int amount = event.getHitsplat().getAmount();

		// for non-zero hits, only process relevant hitsplat types
		if (amount > 0)
		{
			if (!(hitType == HitsplatID.DAMAGE_ME
				|| hitType == HitsplatID.DAMAGE_ME_ORANGE
				|| hitType == HitsplatID.DAMAGE_OTHER_ORANGE
				|| hitType == HitsplatID.DAMAGE_OTHER
				|| hitType == HitsplatID.DAMAGE_MAX_ME
				|| hitType == HitsplatID.DAMAGE_MAX_ME_ORANGE
				|| hitType == HitsplatID.POISON
				|| hitType == HitsplatID.VENOM
				|| hitType == HitsplatID.BURN))
			{
				return;
			}
		}

		currentFight.addDamageDealt(target.getName(), amount);

		// Exclude certain hitsplat types (like heal, burn, poison, venom, disease)
		// from the buffer used for HP-before-hit calculations.
		boolean isExcludedType = hitType == HitsplatID.HEAL ||
								 hitType == HitsplatID.POISON ||
								 hitType == HitsplatID.VENOM ||
								 hitType == HitsplatID.BURN ||
								 hitType == HitsplatID.DISEASE;

		if (isExcludedType)
		{
			return; // Don't buffer these types for HP calc / matching
		}

		// Store hitsplats received by competitor or opponent for potential vengeance trigger lookup
		Player player = client.getLocalPlayer();
		if (target == player || (hasOpponent() && target == currentFight.getOpponent().getPlayer()))
		{
			int currentTick = client.getTickCount();
			incomingHitsplatsBuffer.computeIfAbsent(currentTick, k -> new CopyOnWriteArrayList<>()).add(new HitsplatInfo(event));
		}

		// Buffer the hitsplat event instead of processing immediately (unless excluded earlier)
		// Vengeance damage hitsplats WILL be included here initially.
		HitsplatInfo info = new HitsplatInfo(event);
		int tick = client.getTickCount();
		List<HitsplatInfo> tickEvents = hitsplatBuffer.computeIfAbsent(tick, k -> new ArrayList<>());
		tickEvents.add(info);

		// Get the HP of the actor on the client thread, after the hitsplat has been applied.
		clientThread.invokeLater(() ->
		{
			Actor hitActor = info.getEvent().getActor();
			if (hitActor != null)
			{
				info.setHp(hitActor.getHealthRatio(), hitActor.getHealthScale());
			}
		});
	}

	@Subscribe
	// track hitpoints healed & ghost barrages for main competitor/client player
	public void onStatChanged(StatChanged statChanged)
	{
		Skill skill = statChanged.getSkill();
		if (!hasOpponent()) { return; }

		if (isCombatBoostSkill(skill))
		{
			currentFight.refreshCompetitorLevelsForTick(client.getTickCount(), new CombatLevels(client));
		}

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

	private static boolean isCombatBoostSkill(Skill skill)
	{
		return skill == Skill.ATTACK
			|| skill == Skill.STRENGTH
			|| skill == Skill.DEFENCE
			|| skill == Skill.RANGED
			|| skill == Skill.MAGIC;
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
	// allow players to choose if they want to reset fight history.
	@Override
	public void resetConfiguration()
	{
		super.resetConfiguration();
		int dialogResult = JOptionPane.showConfirmDialog(panel,
			"Your configuration was successfully reset to default settings. " +
				"Do you also want to remove all saved fight data, including any saved favorites?",
			"Warning: PvP Performance Tracker - Delete Fights Permanently? (INCLUDING favorites)", JOptionPane.YES_NO_OPTION);
		if (dialogResult == JOptionPane.YES_OPTION)
		{
			resetFightHistory(true, true);
		}
	}

	// when the client shuts down, save the fight history data locally.
	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		event.waitFor(executor.submit(() -> FightPerformanceSerializer.serializeSessionFightHistory()));
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// if there is no ongoing fight, skip any onGameTick processing.
		// We should have enough extra ticks to calc any hitsplats during death animations and empty these queues.
		if (!hasOpponent()) { return; }

		currentFight.recordCurrentInventorySnapshot();

		// Process hitsplats from the previous tick
		int currentTick = client.getTickCount();
		int tickToProcess = currentTick - 1;
		int maxWindow = 5;
		List<HitsplatInfo> hitsplatsToProcess = hitsplatBuffer.remove(tickToProcess);

		// --- START: New Pre-processing Logic ---
		if (hitsplatsToProcess != null && !hitsplatsToProcess.isEmpty())
		{
			// 1. Calculate total expected hits from pending attacks for this tick
			int totalExpectedAttackHits = 0;
			Player player = client.getLocalPlayer();
			// Assuming getOpponentName() and getOpponentActor() exist or accessing opponent/competitor directly
			Actor opponentActor = currentFight.getOpponent().getPlayer();

			// Sum expected hits from opponent's pending attacks targeting player
			if (currentFight.getOpponent() != null)
			{
				totalExpectedAttackHits += currentFight.getOpponent().getPendingAttacks().stream()
					.filter(e -> !e.isKoChanceCalculated() && e.isFullEntry() && !e.isSplash() &&
						(tickToProcess - getHitsplatMatchTick(e) <= Math.max(5, getAttackLookback(e)))) // Check if attack could land now
					.mapToInt(FightLogEntry::getExpectedHits)
					.sum();
			}
			// Sum expected hits from competitor's pending attacks targeting opponent
			if (currentFight.getCompetitor() != null)
			{
				totalExpectedAttackHits += currentFight.getCompetitor().getPendingAttacks().stream()
					.filter(e -> !e.isKoChanceCalculated() && e.isFullEntry() && !e.isSplash() &&
						(tickToProcess - getHitsplatMatchTick(e) <= Math.max(5, getAttackLookback(e)))) // Check if attack could land now
					.mapToInt(FightLogEntry::getExpectedHits)
					.sum();
			}

			// 2. Compare observed vs expected
			if (hitsplatsToProcess.size() > totalExpectedAttackHits)
			{
				log.debug("Tick {}: Observed hits ({}) > Expected attack hits ({}). Checking for special hits...",
					tickToProcess, hitsplatsToProcess.size(), totalExpectedAttackHits);

				boolean removedHitInIteration;
				int safetyBreakCounter = 0;
				int maxIterations = hitsplatsToProcess.size() * 2; // Allow more iterations to be safe

				// 3. Loop while observed > expected (or until no more candidates found)
				while (hitsplatsToProcess.size() > totalExpectedAttackHits && safetyBreakCounter++ < maxIterations)
				{
					removedHitInIteration = false;
					Iterator<HitsplatInfo> iterator = hitsplatsToProcess.iterator();

					while (iterator.hasNext())
					{
						HitsplatInfo potentialSpecialHit = iterator.next();
						Actor target = potentialSpecialHit.getEvent().getActor();
						int hitAmount = potentialSpecialHit.getEvent().getHitsplat().getAmount();
						boolean isCandidate = false;

						// Determine who the 'other' player is (the one who might have *caused* veng/recoil)
						Actor otherPlayer = null;
						if (target == player)
						{
							otherPlayer = opponentActor;
						}
						else if (target == opponentActor)
						{
							otherPlayer = player;
						}

						// 4. Check Candidates (Vengeance/Recoil)
						if (otherPlayer != null)
						{
							List<HitsplatInfo> incomingHitsOnOther = incomingHitsplatsBuffer.get(tickToProcess);
							if (incomingHitsOnOther != null)
							{
								for (HitsplatInfo incomingHit : incomingHitsOnOther)
								{
									// Only check hits *received* by the other player
									if (incomingHit.getEvent().getActor() == otherPlayer)
									{
										int incomingDamage = incomingHit.getEvent().getHitsplat().getAmount();

										// Vengeance Check
										int expectedVengeance = Math.max(1, (int) Math.floor(incomingDamage * 0.75));
										if (hitAmount == expectedVengeance)
										{
											log.debug("Tick {}: Found potential Vengeance hit ({} damage) on {} based on {} incoming damage on {}",
												tickToProcess, hitAmount, target.getName(), incomingDamage, otherPlayer.getName());
											isCandidate = true;
											break; // Found a reason, no need to check other incoming hits for this potentialSpecialHit
										}

										// Recoil Check
										int expectedRecoil = Math.max(1, (int) Math.floor(incomingDamage * 0.10) + 1);
										if (hitAmount == expectedRecoil)
										{
											log.debug("Tick {}: Found potential Recoil hit ({} damage) on {} based on {} incoming damage on {}",
												tickToProcess, hitAmount, target.getName(), incomingDamage, otherPlayer.getName());
											isCandidate = true;
											break;
										}
									}
								}
							}
						}

						// burn hitsplats are excluded earlier in onHitsplatApplied

						// 5. Remove if Candidate Found
						if (isCandidate)
						{
							iterator.remove();
							removedHitInIteration = true;
							break; // Exit inner loop, re-check outer while condition
						}
					} // End inner iterator loop

					// Safety break if no hits were removed in a full pass
					if (!removedHitInIteration)
					{
						log.debug("Tick {}: No special hit candidates removed in iteration. Breaking pre-emptive removal.", tickToProcess);
						break;
					}
				} // End outer while loop
			} // End if (observed > expected)
		}
		// --- END: New Pre-processing Logic ---


		// Cleanup happens regardless of whether hitsplats were processed this tick
		// Check if hitsplatsToProcess became null or empty after pre-processing
		// If there is no active fight anymore, avoid accessing currentFight below.
		if (hitsplatsToProcess == null || hitsplatsToProcess.isEmpty() || !hasOpponent())
		{
			// Cleanup old entries from buffers
			hitsplatBuffer.keySet().removeIf(tick -> tick < currentTick - maxWindow);
			incomingHitsplatsBuffer.keySet().removeIf(tick -> tick < currentTick - maxWindow);
			return;
		}

		// --- Proceed with Regular Matching using the potentially modified hitsplatsToProcess ---

		// Group hitsplats by the actor receiving them (remaining hitsplats after special removal)
		final List<HitsplatInfo> finalHitsplatsToProcess = hitsplatsToProcess; // Create effectively final list
		Map<Actor, List<HitsplatInfo>> hitsByActor = finalHitsplatsToProcess.stream()
			.collect(Collectors.groupingBy((HitsplatInfo info) -> info.getEvent().getActor()));

		List<FightLogEntry> processedEntriesThisTick = new ArrayList<>();

		hitsByActor.forEach((opponent, hits) -> {
			if (!(opponent instanceof Player)) return; // Only process hits on players

			// Determine max HP to use (Either uses config lvl, or override to 99 for LMS)
			int maxHpToUse;
			if (isInLmsMatch())
			{
				maxHpToUse = 99;
			}
			else
			{
				maxHpToUse = CONFIG.opponentHitpointsLevel();
			}

			// Determine attacker safely (handle null names and prefer identity when possible)
			String actorName = ((Player) opponent).getName();
			Fighter attacker;
			Player trackedOppPlayer = currentFight.getOpponent().getPlayer();
			Player trackedCompPlayer = currentFight.getCompetitor().getPlayer();

			boolean opponentIsTrackedOpponent = opponent == trackedOppPlayer || Objects.equals(actorName, currentFight.getOpponent().getName());
			boolean opponentIsTrackedCompetitor = opponent == trackedCompPlayer || Objects.equals(actorName, currentFight.getCompetitor().getName());

			if (opponentIsTrackedOpponent)
			{
				attacker = currentFight.getCompetitor();
			}
			else if (opponentIsTrackedCompetitor)
			{
				attacker = currentFight.getOpponent();
			}
			else
			{
				return;
			}

			// Get all potentially relevant, unprocessed entries sorted by animation tick
			List<FightLogEntry> candidateEntries = attacker.getPendingAttacks().stream()
				.filter(e -> !e.isKoChanceCalculated() && e.isFullEntry() && !e.isSplash())
				.filter(e -> (client.getTickCount() - getHitsplatMatchTick(e)) <= Math.max(5, getAttackLookback(e)))
				.sorted(Comparator.comparingInt(this::getHitsplatMatchTick).thenComparingInt(FightLogEntry::getTick))
				.collect(Collectors.toList());

			List<FightLogEntry> gmaulsMatchedThisTick = new ArrayList<>();
			int totalGmaulHitsMatchedThisTick = 0;

			// Iterate through candidate entries chronologically
			for (FightLogEntry entry : candidateEntries)
			{

				// Apply specific lookback for the entry's style
				int lookback = getAttackLookback(entry);
				int hitsplatMatchTick = getHitsplatMatchTick(entry);
				if (client.getTickCount() - hitsplatMatchTick > lookback)
				{
					entry.setKoChanceCalculated(true);
					attacker.getPendingAttacks().remove(entry);
					continue;
				}

				int toMatch = entry.getExpectedHits() - entry.getMatchedHitsCount();
				if (toMatch <= 0)
				{
					entry.setKoChanceCalculated(true);
					attacker.getPendingAttacks().remove(entry);
					continue;
				}

				boolean isInstantGmaulCheck = entry.isGmaulSpecial() && hitsplatMatchTick == tickToProcess;
				boolean isDelayedAttack = !isInstantGmaulCheck;

				// Only try to match if it's either an instant GMaul or a delayed attack landing now
				if (isInstantGmaulCheck || isDelayedAttack)
				{
					int matchedThisCycle = 0;
					int damageThisCycle = 0;
					HitsplatInfo lastMatchedInfo = null;
					Iterator<HitsplatInfo> hitsIter = hits.iterator();

					// Gmaul can hit twice, others match expected hits
					int hitsToFind = entry.isGmaulSpecial() ? 2 : toMatch;

					// Enforce Dragon Claws 2+2 sequencing: limit phase one to two hits
					if (entry.getAnimationData() == AnimationData.MELEE_DRAGON_CLAWS_SPEC && entry.getMatchedHitsCount() < 2)
					{
						int remainingPhase1 = Math.max(0, 2 - entry.getMatchedHitsCount());
						hitsToFind = Math.min(hitsToFind, remainingPhase1);
					}

					// Simple double-GMaul gate: if a different special fired on the previous tick, cap to a single hit
					if (entry.isGmaulSpecial())
					{
						String attackerName = attacker.getName();
						if (attackerName != null)
						{
							Integer lastSpec = lastNonGmaulSpecTickByAttacker.get(attackerName);
							if (lastSpec != null && lastSpec == hitsplatMatchTick - 1)
							{
								hitsToFind = Math.min(hitsToFind, 1);
							}
						}
					}

					while (matchedThisCycle < hitsToFind && hitsIter.hasNext())
					{
						HitsplatInfo hInfo = hitsIter.next();
						int amt = hInfo.getEvent().getHitsplat().getAmount();
						damageThisCycle += amt;
						matchedThisCycle++;
						lastMatchedInfo = hInfo;
						hitsIter.remove();
					}

					if (matchedThisCycle > 0)
					{
						entry.setActualDamageSum(entry.getActualDamageSum() + damageThisCycle);
						entry.setMatchedHitsCount(entry.getMatchedHitsCount() + matchedThisCycle);
						processedEntriesThisTick.add(entry);

						if (entry.isGmaulSpecial())
						{
							gmaulsMatchedThisTick.add(entry);
							totalGmaulHitsMatchedThisTick += matchedThisCycle;
						}

						if (entry.getHitsplatTick() < 0 && lastMatchedInfo != null)
						{
							entry.setHitsplatTick(tickToProcess);
						}

						// Calculate and set estimated HP Before using polled HP
						int ratio = -1, scale = -1;
						if (lastMatchedInfo != null)
						{
							ratio = lastMatchedInfo.getHealthRatio();
							scale = lastMatchedInfo.getHealthScale();
						}
						// Fallback to current ratio/scale if polled is unavailable
						if (ratio < 0 || scale <= 0) { ratio = opponent.getHealthRatio(); scale = opponent.getHealthScale(); }
						int hpBefore = -1;
						int hpBeforeThisCycle = -1;
						if (ratio >= 0 && scale > 0 && maxHpToUse > 0)
						{
							hpBefore = PvpUtils.calculateHpBeforeHit(ratio, scale, maxHpToUse, entry.getActualDamageSum());
							hpBeforeThisCycle = PvpUtils.calculateHpBeforeHit(ratio, scale, maxHpToUse, damageThisCycle);
						}
						if (hpBefore > 0)
						{
							entry.setEstimatedHpBeforeHit(hpBefore);
							entry.setOpponentMaxHp(maxHpToUse);
						}

						if (entry.getAnimationData() == AnimationData.MELEE_DRAGON_CLAWS_SPEC)
						{
							int matched = entry.getMatchedHitsCount();
							if (matched == 2 && entry.getClawsHpBeforePhase1() == null && hpBeforeThisCycle > 0)
							{
								entry.setClawsHpBeforePhase1(hpBeforeThisCycle);
								entry.setClawsPhase1Damage(damageThisCycle);
								entry.setClawsHpAfterPhase1(hpBeforeThisCycle - damageThisCycle);
							}
							if (matched >= entry.getExpectedHits() && entry.getClawsHpBeforePhase2() == null && hpBeforeThisCycle > 0)
							{
								entry.setClawsHpBeforePhase2(hpBeforeThisCycle);
							}
						}
						else if (entry.getAnimationData() == AnimationData.RANGED_DARK_BOW ||
							entry.getAnimationData() == AnimationData.RANGED_DARK_BOW_SPEC)
						{
							int matchedAfter = entry.getMatchedHitsCount();
							int matchedBefore = matchedAfter - matchedThisCycle;

							if (matchedBefore == 0 && matchedThisCycle >= 2)
							{
								entry.setDarkBowHitsStacked(true);
								if (entry.getDarkBowHpBeforeHit1() == null && hpBeforeThisCycle > 0)
								{
									entry.setDarkBowHpBeforeHit1(hpBeforeThisCycle);
								}
							}
							else
							{
								if (matchedAfter >= 1 && entry.getDarkBowHpBeforeHit1() == null && hpBeforeThisCycle > 0)
								{
									entry.setDarkBowHpBeforeHit1(hpBeforeThisCycle);
									entry.setDarkBowHpAfterHit1(hpBeforeThisCycle - damageThisCycle);
								}
								if (matchedAfter >= entry.getExpectedHits() && entry.getDarkBowHpBeforeHit2() == null && hpBeforeThisCycle > 0)
								{
									entry.setDarkBowHpBeforeHit2(hpBeforeThisCycle);
								}
							}
						}
					}
				}

				// Mark entry as fully processed if all expected hits are matched OR if it's an instant Gmaul (even if only 1 hit matched)
				if (entry.getMatchedHitsCount() >= entry.getExpectedHits() || isInstantGmaulCheck)
				{
					entry.setKoChanceCalculated(true);
					attacker.getPendingAttacks().remove(entry);
				}
			}

			// Gmaul Damage Scaling (Applied after all matching for the tick)
			boolean isMultiHitGmaul = totalGmaulHitsMatchedThisTick >= 2;
			if (isMultiHitGmaul)
			{
				for (FightLogEntry gmaulEntry : gmaulsMatchedThisTick)
				{
					int originalMin = gmaulEntry.getMinHit();
					int originalMax = gmaulEntry.getMaxHit();
					double originalExpected = gmaulEntry.getExpectedDamage();
					gmaulEntry.setMaxHit(originalMax * totalGmaulHitsMatchedThisTick);
					gmaulEntry.setMinHit(originalMin * totalGmaulHitsMatchedThisTick);
					gmaulEntry.setExpectedDamage(originalExpected * totalGmaulHitsMatchedThisTick);
				}
			}
		});

		// Post-processing for Display HP/KO Chance
		if (!processedEntriesThisTick.isEmpty())
		{
			// Group processed entries by the tick they landed and the attacker
			Map<Integer, Map<String, List<FightLogEntry>>> groupedByTickAndAttacker = processedEntriesThisTick.stream()
				.filter(e -> e.getHitsplatTick() >= 0)
				.collect(Collectors.groupingBy(
					FightLogEntry::getHitsplatTick,
					Collectors.groupingBy(
						FightLogEntry::getAttackerName,
						Collectors.toList()
					)
				));

			groupedByTickAndAttacker.forEach((tick, attackerMap) -> {
				attackerMap.forEach((attackerName, entries) -> {
					if (entries.isEmpty()) return;

					// Sort entries within the tick group by their original animation tick
					entries.sort(Comparator.comparingInt(FightLogEntry::getTick));

					boolean isGroup = entries.size() > 1;

					// Calculate Correct Starting HP for Forward Cascade
					Integer hpBeforeSequence = null;
					FightLogEntry lastEntry = entries.get(entries.size() - 1);
					Integer hpBeforeLastHit = lastEntry.getEstimatedHpBeforeHit();
					Integer lastHitDamage = lastEntry.getActualDamageSum();

					// Ensure we have the necessary values from the last hit to calculate final HP
					if (hpBeforeLastHit != null && lastHitDamage != null)
					{
						Integer hpAfterSequence = hpBeforeLastHit - lastHitDamage;

						// Calculate total damage for the sequence
						int totalDamageInSequence = entries.stream()
							.mapToInt((FightLogEntry e) -> e.getActualDamageSum() != null ? e.getActualDamageSum() : 0)
							.sum();

						// Calculate HP Before the entire sequence
						hpBeforeSequence = hpAfterSequence + totalDamageInSequence;
					}

					// If hpBeforeSequence is still null (calculation failed), try fallback using first entry's estimate
					if (hpBeforeSequence == null)
					{
						hpBeforeSequence = entries.get(0).getEstimatedHpBeforeHit();
					}

					// Forward Cascade for Display
					Integer currentHp = hpBeforeSequence;
					for (FightLogEntry entry : entries)
					{
						Integer hpBeforeCurrent = currentHp;
						int damageCurrent = entry.getActualDamageSum() != null ? entry.getActualDamageSum() : 0;
						Integer hpAfterCurrent = (hpBeforeCurrent != null) ? hpBeforeCurrent - damageCurrent : null;

						entry.setDisplayHpBefore(hpBeforeCurrent);
						entry.setDisplayHpAfter(hpAfterCurrent);

						Double koChanceCurrent = null;
						boolean isClawsSpec = entry.getAnimationData() == AnimationData.MELEE_DRAGON_CLAWS_SPEC && entry.getExpectedHits() >= 4;
						boolean isDarkBow = entry.getAnimationData() == AnimationData.RANGED_DARK_BOW ||
							entry.getAnimationData() == AnimationData.RANGED_DARK_BOW_SPEC;
						if (isClawsSpec)
						{
							if (hpBeforeCurrent != null && entry.getMatchedHitsCount() >= entry.getExpectedHits())
							{
								int healBetween = 0;
								Integer hpAfterP1 = entry.getClawsHpAfterPhase1();
								Integer hpBeforeP2 = entry.getClawsHpBeforePhase2();
								if (hpAfterP1 != null && hpBeforeP2 != null)
								{
									healBetween = Math.max(0, hpBeforeP2 - hpAfterP1);
								}
								koChanceCurrent = PvpUtils.calculateClawsTwoPhaseKo(entry.getAccuracy(), entry.getMaxHit(), hpBeforeCurrent, healBetween);
							}
						}
						else if (isDarkBow)
						{
							if (hpBeforeCurrent != null && entry.getMatchedHitsCount() >= entry.getExpectedHits())
							{
								int healBetween = 0;
								if (!entry.isDarkBowHitsStacked())
								{
									Integer hpAfterHit1 = entry.getDarkBowHpAfterHit1();
									Integer hpBeforeHit2 = entry.getDarkBowHpBeforeHit2();
									if (hpAfterHit1 != null && hpBeforeHit2 != null)
									{
										healBetween = Math.max(0, hpBeforeHit2 - hpAfterHit1);
									}
								}
								koChanceCurrent = PvpUtils.calculateDarkBowTwoPhaseKo(
									entry.getAccuracy(),
									entry.getMinHit(),
									entry.getMaxHit(),
									hpBeforeCurrent,
									healBetween
								);
							}
						}
							else
							{
								if (hpBeforeCurrent != null)
								{
									switch (entry.getDamageRollDistribution())
									{
										case CLAMPED_TO_MINIMUM:
											koChanceCurrent = PvpUtils.calculateClampedKoChance(
												entry.getAccuracy(),
												entry.getMinHit(),
												entry.getMaxHit(),
												hpBeforeCurrent
											);
											break;
										case MULTI_HIT_CLAMPED_TO_MINIMUM:
											koChanceCurrent = PvpUtils.calculateMultiHitClampedKoChance(
												entry.getAccuracy(),
												entry.getMinHit(),
												entry.getMaxHit(),
												entry.getDamageRollHitCount(),
												hpBeforeCurrent
											);
											break;
										case STANDARD:
										default:
											koChanceCurrent = PvpUtils.calculateKoChance(
												entry.getAccuracy(),
												entry.getMinHit(),
												entry.getMaxHit(),
												hpBeforeCurrent
											);
											break;
									}
								}
							}

						if (koChanceCurrent != null && koChanceCurrent <= 0.0)
						{
							koChanceCurrent = null;
						}

						entry.setDisplayKoChance(koChanceCurrent);
						entry.setKoChance(koChanceCurrent);

						currentFight.updateKoChanceStats(entry);

						entry.setPartOfTickGroup(isGroup);

						// Update HP for the next iteration
						currentHp = hpAfterCurrent;
				}
				});
			});
		}

		// Cleanup old entries from buffers at the end of the tick processing
		hitsplatBuffer.keySet().removeIf(tick -> tick < currentTick - maxWindow);
		incomingHitsplatsBuffer.keySet().removeIf(tick -> tick < currentTick - maxWindow);
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned event)
	{
		if (!hasOpponent()) { return; }
		Player despawned = event.getPlayer();
		if (despawned == null || despawned.getName() == null) { return; }

		if (currentFight == null || currentFight.getOpponent() == null) { return; }
		String opponentName = currentFight.getOpponent().getName();
		if (opponentName == null) { return; }

		// End fight when opponent despawns after a death was observed on either side
		if (despawned.getName().equals(opponentName) && (currentFight.getOpponent().isDead() || currentFight.getCompetitor().isDead()))
		{
			onFightEnded();
		}
	}

	public void recordNonGmaulSpecial(String attackerName, int tick)
	{
		if (attackerName == null)
		{
			return;
		}
		lastNonGmaulSpecTickByAttacker.put(attackerName, tick);
	}

	private int getHitsplatMatchTick(FightLogEntry entry)
	{
		return entry.getHitsplatMatchTick() >= 0 ? entry.getHitsplatMatchTick() : entry.getTick();
	}

	private int getAttackLookback(FightLogEntry entry)
	{
		switch (entry.getAnimationData().attackStyle)
		{
			case STAB:
			case SLASH:
			case CRUSH:
				return 3;
			case MAGIC:
				return 6;
			case RANGED:
			default:
				return 4;
		}
	}

	// #################################################################################################################
	// ################################## Plugin-specific functions & global helpers ###################################
	// #################################################################################################################

	private void sendUpdateChatMessage()
	{
		// if the updateMsgKey=true, then they already had this update msg, skip
		if (config.updateMsgShown())
		{
			return;
		}

		// primary update message

		// don't directly use PLUGIN_VERSION in the prefix because there may be times we don't include a new update
		// message and want it to remain the same as the previous version. For example, if there's a small hotfix after a major update.
		// There should be intent behind the version number shown in the update message, not just automatically showing the current version.
		String updateMsgForVersion = "1.8.7";
		String updatePrefix = "<html><shad=000000><col=" + ColorUtil.colorToHexCode(PvpColorScheme.BLOOD_RED_ORANGE) +
			">PvP Performance Tracker</col> <col=" + ColorUtil.colorToHexCode(PvpColorScheme.BLOOD_RED_ORANGE_REDDER2) +
			"><u>v" + updateMsgForVersion + "</u></col> <col=" + ColorUtil.colorToHexCode(PvpColorScheme.BLOOD_RED_ORANGE) +
			">Update:</col></shad> ";
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(updatePrefix +
				"<col=" + ColorUtil.colorToHexCode(PvpColorScheme.DARK_ORANGE_BROWN_TEXT) + ">" +
				"Fix offensive pray tracking & related assumptions. " +
				"Improved filter functionality & added various new filters, with a dropdown to preview and help using them. " +
				"Likely fixed incorrect stats for same-tick restore attacks. " +
				"Fix min/maxHits in fight logs & KO Chance, fix HP for KO chance (use config rather than hiscores). " +
				"Allow comparing client vs. synced fight panels on synced fight logs. Fix dclaws max hit.")
				.build());

		configManager.setConfiguration(CONFIG_KEY, PvpPerformanceTrackerConfig.updateMsgKey, true);

		// remove unused update msg flags after updating
		for (String outdatedUpdateMsgKey : PvpPerformanceTrackerConfig.UPDATE_MSG_KEY_GRAVEYARD)
		{
			configManager.unsetConfiguration(CONFIG_KEY, outdatedUpdateMsgKey);
		}
	}

	private void update(String oldVersion)
	{
		boolean oldIs1_7to1_8_2 = oldVersion.startsWith("1.7.") || List.of("1.8.0", "1.8.1", "1.8.2").contains(oldVersion);
		if (oldIs1_7to1_8_2)
		{
			// force-change default maxRenderedFights 200 -> 30, most people probably dont even need 30,
			// with how the rendered vs. saved fights work its totally unnecessary to have 200
			configManager.setConfiguration(CONFIG_KEY, "fightHistoryRenderLimit", 30);

			FightPerformanceSerializer.updateFrom1_8_1to1_8_2();
		}

		configManager.setConfiguration(CONFIG_KEY, "pluginVersion", PLUGIN_VERSION);
	}

	// Returns true if the player has an opponent.
	private boolean hasOpponent()
	{
		return currentFight != null;
	}


	private void checkForFightEnd()
	{
		if (!hasOpponent()) { return; }

		// ensure we check for death animations so that Fighter.isDead gets set properly, but we don't need to
		// use the state of deaths for ending fights YET (not instantly), we do that within onPlayerDespawned
		// in order to give everything time to process and allow time to check for double deaths, hitsplats etc
		currentFight.checkForDeathAnimations();

		// if the fight has been inactive for 20+ secs however (FightPerformance.NEW_FIGHT_DELAY, plus however long
		// until they triggered an event for this check), just end it.
		if (currentFight.isInactive())
		{
			onFightEnded();
		}
	}

	private void onFightEnded()
	{
		// add fight to fight history if it actually started
		if (currentFight.fightStarted())
		{
			currentFight.recordEndingInventorySnapshot();
			currentFight.makeLogTicksRelativeToFightStart();
			addToFightHistory(currentFight);

			// Upload to PvP-Hub if enabled and a fight ID was generated
			if (CONFIG.uploadFightsToPvpHub() && currentFight.getFightId() != null
					&& !currentFight.getFightId().isEmpty())
			{
				final FightPerformance fightToUpload = currentFight;
				final String hiddenName = config.hideRsnOnPvpHub() ? getPvpHubHiddenName() : null;
				fightToUpload.recordPvpHubUploadName(hiddenName != null ? hiddenName : fightToUpload.getCompetitor().getName());
				executor.schedule(() ->
				{
					if (!CONFIG.uploadFightsToPvpHub())
					{
						return;
					}

					PvpHubUploader.uploadFight(fightToUpload, GSON, httpClient, hiddenName,
						() -> enqueuePvpHubSync(fightToUpload));
				}, PVP_HUB_UPLOAD_DELAY_MILLIS, TimeUnit.MILLISECONDS);
			}

			// send pluginMessage containing fight data to pvp leaderboard plugin, if the user has this plugin
			try
			{
				boolean userHasLeaderboard = pluginManager.getPlugins().stream().anyMatch((p) ->
					p.getClass().getName().equals("com.pvp.leaderboard.PvPLeaderboardPlugin")
					|| p.getName().equals("PvP Leaderboard")
				);
				if (userHasLeaderboard)
				{
					Map<String, Object> fightsToSend = Map.ofEntries(
						entry("fight", GSON.toJson(currentFight))
					);
					eventBus.post(new PluginMessage("PvPLeaderboard", "onFightEnded", fightsToSend));
				}
			}
			catch (Exception e)
			{
				log.warn("onFightEnded - error while sending PluginMessage containing fight data for PvP Leaderboard.");
			}
		}
		currentFight = null;
		hitsplatBuffer.clear();
		incomingHitsplatsBuffer.clear();
	}

	// add fight to loaded fight history
	void addToFightHistory(FightPerformance fight)
	{
		if (fight == null) { return; }
		fightHistory.addLast(fight);
		sessionFightHistory.addLast(fight);
		// no need to sort, since they sort chronologically, but they should automatically be added that way.
		try {
			fight.calculateRobeHits(config.robeHitFilter());
		}
		catch (Exception e)
		{
			log.warn("Error calculating robe hits for new fight ({} vs {}): {}",
				fight.getCompetitor() != null ? fight.getCompetitor().getName() : "N/A",
				fight.getOpponent() != null ? fight.getOpponent().getName() : "N/A",
				e.getMessage());
		}

		// remove fights as necessary to respect the fightHistoryLimit.
		while (config.fightHistoryLimit() > 0 && fightHistory.size() > config.fightHistoryLimit())
		{
			fightHistory.removeFirst();
		}

		// unlikely to happen for the sessionFightHistory, but do the same limit validation to it.
		while (config.fightHistoryLimit() > 0 && sessionFightHistory.size() > config.fightHistoryLimit())
		{
			sessionFightHistory.removeFirst();
		}

		panel.addFight(fight);
	}

	private void enqueuePvpHubSync(FightPerformance fight)
	{
		if (fight == null || fight.getFightId() == null || fight.getFightId().trim().isEmpty() || fight.hasPvpHubSyncedFight())
		{
			return;
		}

		pendingPvpHubSyncs.enqueue(fight.getFightId(), System.currentTimeMillis());
	}

	private void syncPendingPvpHubFights()
	{
		if (!CONFIG.uploadFightsToPvpHub())
		{
			pendingPvpHubSyncs.clear();
			return;
		}

		if (pendingPvpHubSyncs.isEmpty())
		{
			return;
		}

		long nowMillis = System.currentTimeMillis();
		for (String fightId : pendingPvpHubSyncs.dueFightIds(nowMillis))
		{
			FightPerformance fight = findFightById(fightId);
			if (fight == null || fight.hasPvpHubSyncedFight())
			{
				pendingPvpHubSyncs.remove(fightId);
				continue;
			}

			if (pendingPvpHubSyncs.markAttemptStarted(fightId, nowMillis))
			{
				requestPvpHubSync(fight);
			}
		}
	}

	private FightPerformance findFightById(String fightId)
	{
		if (fightId == null)
		{
			return null;
		}

		for (FightPerformance fight : fightHistory)
		{
			if (fightId.equals(fight.getFightId()))
			{
				return fight;
			}
		}
		return null;
	}

	private void requestPvpHubSync(FightPerformance fight)
	{
		if (fight == null || fight.getFightId() == null || fight.getFightId().trim().isEmpty()
			|| fight.hasPvpHubSyncedFight() || fight.isPvpHubSyncInProgress())
		{
			return;
		}

		fight.setPvpHubSyncInProgress(true);
		PvpHubFightSync.fetchSyncedFight(fight, GSON, httpClient, new PvpHubFightSync.SyncCallback()
		{
			@Override
			public void onSynced(FightPerformance syncedFight, String responseJson)
			{
				try
				{
						PvpHubFightSync.saveSyncedFight(pvpHubSyncedFightsDir, fight.getFightId(), responseJson);
						fight.setPvpHubSyncedFight(syncedFight);
						pendingPvpHubSyncs.remove(fight.getFightId());
					}
				catch (Exception e)
				{
					fight.setPvpHubSyncInProgress(false);
					log.warn("Error applying synced PvP-Hub fight {}: {}: {}", fight.getFightId(), e.getClass().getSimpleName(), e.getMessage());
					return;
				}

				clientThread.invokeLater(() ->
				{
					try
					{
						fight.getPvpHubDisplayFight().calculateRobeHits(config.robeHitFilter());
					}
					catch (Exception e)
					{
						log.warn("Error recalculating synced PvP-Hub robe hits {}: {}: {}", fight.getFightId(), e.getClass().getSimpleName(), e.getMessage());
					}

					SwingUtilities.invokeLater(() -> {
						if (panel != null) {
							panel.enqueueRebuild();
						}
					});
				});
			}

			@Override
				public void onNotSynced(String reason)
				{
					fight.setPvpHubSyncInProgress(false);
					pendingPvpHubSyncs.markUnavailable(fight.getFightId());
					log.debug("PvP-Hub sync unavailable for fight {}: {}", fight.getFightId(), reason);
				}
		});
	}

	void recalculateAllRobeHits(boolean rebuildPanel)
	{
		clientThread.invokeLater(() ->
		{
			// Recalculate robe hits for all fights based on the new filter and refresh UI
			for (FightPerformance f : fightHistory)
			{
				f.calculateRobeHits(config.robeHitFilter());
				if (f.hasPvpHubSyncedFight())
				{
					f.getPvpHubDisplayFight().calculateRobeHits(config.robeHitFilter());
				}
			}
			if (currentFight != null)
			{
				currentFight.calculateRobeHits(config.robeHitFilter());
			}
			if (rebuildPanel)
			{
				if (panel != null)
				{
					panel.enqueueRebuild();
				}
			}
		});
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

		f.initializeFightLogNames();
		attachPvpHubSyncedFight(f);
	}

	private void attachPvpHubSyncedFight(FightPerformance fight)
	{
		FightPerformance syncedFight = PvpHubFightSync.loadSyncedFight(pvpHubSyncedFightsDir, GSON, fight);
		if (syncedFight == null)
		{
			return;
		}

		try
		{
			fight.setPvpHubSyncedFight(syncedFight);
		}
		catch (Exception e)
		{
			log.warn("Error attaching synced PvP-Hub fight {}: {}: {}", fight.getFightId(), e.getClass().getSimpleName(), e.getMessage());
		}
	}

	// process and add a list of deserialized json fights to the currently loaded fights
	// can throw NullPointerException if some of the serialized data is corrupted
	void importFights(List<FightPerformance> fights) throws NullPointerException
	{
		if (fights == null || fights.isEmpty()) { return; }

		fights.removeIf(Objects::isNull);
		fights.sort(FightPerformance::compareTo); // ensure sorted by date, though this should happen automatically as well
		fightHistory.addAll(fights);

		// remove fights to respect the fightHistoryLimit if we're above it.
		while (config.fightHistoryLimit() > 0 && fightHistory.size() > config.fightHistoryLimit())
		{
			fightHistory.removeFirst();
		}

		// set fight log names since they aren't serialized but are on the parent class
		for (FightPerformance f : fightHistory)
		{
			initializeImportedFight(f);
		}
	}

	public void resetFightHistoryKeepingFavorites()
	{
		resetFightHistoryKeepingFavorites(true);
	}
	// reset the loaded fight history as well as the saved json data
	public void resetFightHistoryKeepingFavorites(boolean deletePermanently)
	{
		resetFightHistory(deletePermanently, false);
	}

	// reset the loaded fight history, and delete files if we're deleting permanently.
	public void resetFightHistory(boolean deletePermanently, boolean deleteFavorites)
	{
		clientThread.invokeLater(() ->
		{
			if (deletePermanently)
			{
				FightPerformanceSerializer.removeAllFights(deleteFavorites);
			}
			else // if we're just hiding the fights temporarily (as opposed to deleting permanently), we should save
			// the session fights if there are any, so those aren't lost, this action shouldn't delete anything.
			{
				FightPerformanceSerializer.serializeSessionFightHistory();
			}

			// if we're not including favorites in the deletion, then leave the favorites in the loaded data.
			// if we're only 'hiding' fights (i.e not deleting permanently), then remove the loaded favorite
			// fights regardless, so we don't have to make an extra menu for "hide favorite fights" along with "hide all fights".
			// or in other words, only keep the favorite fights when we're deleting permanently but not deleting favorites
			if (deletePermanently && !deleteFavorites)
			{
				fightHistory.removeIf((f) -> !f.isFavorite());
				sessionFightHistory.removeIf((f) -> !f.isFavorite());
			}
			else
			{
				fightHistory.clear();
				sessionFightHistory.clear();
			}

			panel.enqueueRebuild();
		});
	}

	// remove a fight - delete/update its file if deletePermanently is true, otherwise only remove
	// since users have to directly select/target a fight to remove it individually, provide no validation regarding
	// favorited fights - assume a user indeed wants to delete it. We could maybe add a popup to say are you sure you
	// want to delete this favorited fight? But I think it's unnecessary
	public void removeFight(FightPerformance fight)
	{
		removeFight(fight, true);
	}
	public void removeFight(FightPerformance fight, boolean deletePermanently)
	{
		clientThread.invokeLater(() ->
		{
			if (deletePermanently)
			{
				// if the fight wasn't loaded from a file, it won't have its fileName set to find where to delete/update,
				// so this won't really do anything. Don't have to do much validation outside this call.
				FightPerformanceSerializer.removeFight(fight, fight.getLoadedFromFname());
				// if the fight wasn't loaded from a file, then it's in the current session history, and we have to
				// remove it from there, or it will get saved next time we write.
				if (!fight.isSavedToFile())
				{
					sessionFightHistory.remove(fight);
				}
			}
			// if we aren't deleting permanently, then don't remove from sessionFightHistory, as those
			// aren't saved to file yet and would inadvertently be deleted permanently

			// remove fight from the total/global loaded fightHistory regardless
			fightHistory.remove(fight);
			panel.enqueueRebuild();
		});
	}

	public boolean isInLmsMatch()
	{
		//log.info("PvpPerformanceTrackerPlugin.isInLmsMatch - BR_INGAME varbit={}, BR_ACTIVE_BUILD_PLAYER={}, BR_MODE_SELECTED={}", client.getVarbitValue(VarbitID.BR_INGAME), client.getVarbitValue(VarbitID.BR_ACTIVE_BUILD_PLAYER), client.getVarbitValue(VarbitID.BR_MODE_SELECTED));
		return client.getVarbitValue(VarbitID.BR_INGAME) != 0;
	}

	public boolean isAtLmsIncludingFerox()
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
	public void sendTradeChatMessage(String chatMessage)
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

	public void exportFightAsJson(FightPerformance fight)
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
				fight.getOpponent().getName() + " was copied to the clipboard as JSON data.";
		}
		else
		{
			confirmMessage = "Warning: Fight data was copied to the clipboard, but it's likely corrupted.";
		}
		createConfirmationModal(success, confirmMessage);
	}

	// retrieve offensive pray as SpriteID since that's all we will directly use it for,
	// aside from comparison/equality checks, so we save an extra mapping this way
	public int currentlyUsedOffensivePray()
	{
		return PrayerType.PIETY.isActive(client) ? PrayerType.PIETY.getSpriteID() :
			PrayerType.PIETY.isActive(client) ? PrayerType.PIETY.getSpriteID() :
			PrayerType.ULTIMATE_STRENGTH.isActive(client) ? PrayerType.ULTIMATE_STRENGTH.getSpriteID() :
			PrayerType.RIGOUR.isActive(client) ? PrayerType.RIGOUR.getSpriteID() :
			PrayerType.EAGLE_EYE.isActive(client) ? PrayerType.EAGLE_EYE.getSpriteID() :
			PrayerType.DEADEYE.isActive(client) ? PrayerType.DEADEYE.getSpriteID() :
			PrayerType.AUGURY.isActive(client) ? PrayerType.AUGURY.getSpriteID() :
			PrayerType.MYSTIC_MIGHT.isActive(client) ? PrayerType.MYSTIC_MIGHT.getSpriteID() :
			PrayerType.MYSTIC_VIGOUR.isActive(client) ? PrayerType.MYSTIC_VIGOUR.getSpriteID() :
			0;
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

	// if verifyId is true, takes in itemId directly from PlayerComposition
	// otherwise, assume valid itemId
	public void addItemToLabelIfValid(JLabel label, int itemId, boolean verifyId, Runnable swingCallback, String tooltipOverride, boolean includeItemIdOnTooltip)
	{
		if (itemId > PlayerComposition.ITEM_OFFSET || !verifyId)
		{
			final int finalItemId = itemId - (verifyId ? PlayerComposition.ITEM_OFFSET : 0);
			clientThread.invokeLater(() -> {
				itemManager.getImage(finalItemId).addTo(label);
				if (tooltipOverride != null && !tooltipOverride.isEmpty())
				{
					label.setToolTipText(tooltipOverride);
				}
				else
				{
					String name = itemManager.getItemComposition(finalItemId).getName();
					if (name == null || name.isEmpty())
					{
						name = "Item Name Not Found";
					}
					if (includeItemIdOnTooltip)
					{
						name += " (ID=" + finalItemId + ")";
					}
					label.setToolTipText(name);
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

	public void addItemToLabelIfValid(JLabel label, int itemId, boolean verifyId, Runnable swingCallback, String tooltipOverride)
	{
		addItemToLabelIfValid(label, itemId, verifyId, swingCallback, tooltipOverride, false);
	}

	public void addItemToLabelIfValid(JLabel label, RangeAmmoData data, boolean verifyId, Runnable swingCallback)
	{
		addItemToLabelIfValid(label, data.getItemId(), verifyId, swingCallback, data.toString(), false);
	}

	public void addItemToLabelIfValid(JLabel label, int itemId, boolean verifyId, Runnable swingCallback)
	{
		addItemToLabelIfValid(label, itemId, verifyId, swingCallback, null, false);
	}

	public void addItemToLabelIfValid(JLabel label, int itemId)
	{
		addItemToLabelIfValid(label, itemId, true, null);
	}

	public String updateFightFilterConfig(String newFilterName)
	{
		String sanitizedFilterName = Text.sanitize(newFilterName.stripLeading().toLowerCase());

		configManager.setConfiguration(CONFIG_KEY, "nameFilter", sanitizedFilterName);

		return sanitizedFilterName;
	}

	public String getPvpHubHiddenName()
	{
		String anonymousId = config.pvpHubAnonymousId();
		if (anonymousId == null || anonymousId.trim().isEmpty())
		{
			anonymousId = PvpHubPrivacy.createAnonymousId();
			configManager.setConfiguration(CONFIG_KEY, "pvpHubAnonymousId", anonymousId);
		}

		return PvpHubPrivacy.hiddenNameFor(anonymousId);
	}

	public void resetPvpHubHiddenName()
	{
		configManager.unsetConfiguration(CONFIG_KEY, "pvpHubAnonymousId");
		panel.updatePvpHubHiddenName();
	}

	public void toggleFightLogDetailFrameWarning()
	{
		configManager.setConfiguration(CONFIG_KEY, "displayFightLogDetailWarning", !config.displayFightLogDetailWarning());
	}
	public void toggleSocialButtonsVisibility()
	{
		configManager.setConfiguration(CONFIG_KEY, "displayPanelSocialButtons", !config.displayPanelSocialButtons());
		// this will call panel.updateSocialButtons() via onConfigChanged event
	}
	public void toggleUnsyncedFightWarningVisibility()
	{
		configManager.setConfiguration(CONFIG_KEY, "displayUnsyncedFightWarning", !config.displayUnsyncedFightWarning());
	}
}
