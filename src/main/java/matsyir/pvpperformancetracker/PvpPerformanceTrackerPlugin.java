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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.temporal.ChronoUnit;
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
import matsyir.pvpperformancetracker.controllers.Fighter;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.models.HitsplatInfo;
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import matsyir.pvpperformancetracker.models.oldVersions.FightPerformance__1_5_5;
import matsyir.pvpperformancetracker.utils.PvpPerformanceTrackerUtils;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.HitsplatID;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.FakeXpDrop;
import net.runelite.api.events.GameTick;
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
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreManager;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;


@Slf4j
@PluginDescriptor(
	name = "PvP Performance Tracker"
)
public class PvpPerformanceTrackerPlugin extends Plugin
{
	// static fields
	public static final String PLUGIN_VERSION = "1.7.0";
	public static final String CONFIG_KEY = "pvpperformancetracker";
	// Data folder naming history:
	// "pvp-performance-tracker": From release, until 1.5.9 update @ 2024-08-19
	// "pvp-performance-tracker2": From 1.5.9 update, until present
	public static final String DATA_FOLDER = "pvp-performance-tracker2";
	public static final String FIGHT_HISTORY_DATA_FNAME = "FightHistoryData.json";
	public static final File FIGHT_HISTORY_DATA_DIR;
	public static PvpPerformanceTrackerConfig CONFIG;
	public static PvpPerformanceTrackerPlugin PLUGIN;
	public static Image PLUGIN_ICON;
	public static AsyncBufferedImage DEFAULT_NONE_SYMBOL; // save bank filler image to display a generic "None" or N/A state.
	public static Gson GSON;

	// Last man standing map regions, including ferox enclave
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

	@Inject
	private Gson injectedGson;

	@Inject
	private HiscoreManager hiscoreManager; // Added injection

	// custom fields/props
	public ArrayList<FightPerformance> fightHistory;
	@Getter
	private FightPerformance currentFight;
	private Map<Integer, ImageIcon> spriteCache; // sprite cache since a small amount of sprites is re-used a lot
	// do not cache items in the same way since we could potentially cache a very large amount of them.
	private final Map<Integer, List<HitsplatInfo>> hitsplatBuffer = new HashMap<>();
	private final Map<Integer, List<HitsplatInfo>> incomingHitsplatsBuffer = new ConcurrentHashMap<>(); // Stores hitsplats *received* by players per tick.
	private HiscoreEndpoint hiscoreEndpoint = HiscoreEndpoint.NORMAL; // Added field

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

		GSON = injectedGson.newBuilder()
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
		
		// Explicitly rebuild panel after all setup and import.
        SwingUtilities.invokeLater(() -> {
            if (panel != null) {
                panel.rebuild();
            }
        });
	}

	@Override
	protected void shutDown() throws Exception
	{
		saveFightHistoryData();

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
			case "robeHitFilter":
				recalculateAllRobeHits(true);
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

		// start a new fight with the new found opponent, if a new one.
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

		sendUpdateChatMessages();

		hiscoreEndpoint = HiscoreEndpoint.fromWorldTypes(client.getWorldType()); // Update endpoint on login/world change

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
		int amount = event.getHitsplat().getAmount();
		Actor target = event.getActor();

		// if there's no opponent, the target is not a player, or the hitsplat is not relevant to pvp damage,
		// skip the hitsplat. Otherwise, add it to the fight, which will only include it if it is one of the
		// Fighters in the fight being hit.
		if (!hasOpponent() || !(target instanceof Player))
		{
			return;
		}

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
				|| hitType == HitsplatID.VENOM))
			{
				return;
			}
		}

		currentFight.addDamageDealt(target.getName(), amount);

		// Exclude certain hitsplat types (like heal, poison, venom, disease)
		// from the buffer used for HP-before-hit calculations.
		boolean isExcludedType = hitType == HitsplatID.HEAL ||
								 hitType == HitsplatID.POISON ||
								 hitType == HitsplatID.VENOM ||
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
		event.waitFor(executor.submit(this::saveFightHistoryData));
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Process hitsplats from the previous tick
		int currentTick = client.getTickCount();
		int tickToProcess = currentTick - 1;
		int maxWindow = 5;
		List<HitsplatInfo> hitsplatsToProcess = hitsplatBuffer.remove(tickToProcess);

		// --- START: New Pre-processing Logic ---
		if (hitsplatsToProcess != null && !hitsplatsToProcess.isEmpty() && hasOpponent())
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
					.filter(e -> !e.isKoChanceCalculated() && e.isFullEntry() && !e.isSplash() && (tickToProcess - e.getTick() <= 5)) // Check if attack could land now
					.mapToInt(FightLogEntry::getExpectedHits)
					.sum();
			}
			// Sum expected hits from competitor's pending attacks targeting opponent
			if (currentFight.getCompetitor() != null)
			{
				totalExpectedAttackHits += currentFight.getCompetitor().getPendingAttacks().stream()
					.filter(e -> !e.isKoChanceCalculated() && e.isFullEntry() && !e.isSplash() && (tickToProcess - e.getTick() <= 5)) // Check if attack could land now
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

						// 4. Check Candidates (Vengeance/Recoil/Burn)
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

						// Burn Check (Lower priority, only if not already identified as Veng/Recoil)
						if (!isCandidate && hitAmount >= 1 && hitAmount <= 3)
						{
							log.debug("Tick {}: Found potential Burn hit ({} damage) on {}", tickToProcess, hitAmount, target.getName());
							isCandidate = true;
						}

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
		if (hitsplatsToProcess == null || hitsplatsToProcess.isEmpty()) // Modified condition
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

			// Determine max HP to use (config, Hiscores, or LMS override)
			int maxHpToUse;
			if (isAtLMS())
			{
				maxHpToUse = 99;
			}
			else
			{
				maxHpToUse = CONFIG.opponentHitpointsLevel();

				// Hiscores lookup should only happen if not in LMS
			if (opponent instanceof Player && opponent.getName() != null)
			{
				final HiscoreResult hiscoreResult = hiscoreManager.lookupAsync(opponent.getName(), hiscoreEndpoint);
				if (hiscoreResult != null)
				{
					final int hp = hiscoreResult.getSkill(HiscoreSkill.HITPOINTS).getLevel();
					if (hp > 0)
					{
						maxHpToUse = hp; // Use Hiscores HP if available
					}
				}
			}
			}

			// Determine attacker
			String actorName = ((Player) opponent).getName();
			Fighter attacker;
			if (actorName.equals(currentFight.getOpponent().getName()))
			{
				attacker = currentFight.getCompetitor();
			}
			else if (actorName.equals(currentFight.getCompetitor().getName()))
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
				.filter(e -> (client.getTickCount() - e.getTick()) <= 5)
				.sorted(Comparator.comparingInt(FightLogEntry::getTick))
				.collect(Collectors.toList());

			List<FightLogEntry> gmaulsMatchedThisTick = new ArrayList<>();
			int totalGmaulHitsMatchedThisTick = 0;

			// Iterate through candidate entries chronologically
			for (FightLogEntry entry : candidateEntries)
			{

				// Apply specific lookback for the entry's style
				int lookback;
				switch (entry.getAnimationData().attackStyle)
				{
					case STAB: case SLASH: case CRUSH: lookback = 3; break;
					case MAGIC: lookback = 5; break;
					case RANGED: default: lookback = 3; break;
				}
				if (client.getTickCount() - entry.getTick() > lookback)
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

				boolean isInstantGmaulCheck = entry.isGmaulSpecial() && entry.getTick() == tickToProcess;
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
						if (ratio >= 0 && scale > 0 && maxHpToUse > 0)
						{
							hpBefore = PvpPerformanceTrackerUtils.calculateHpBeforeHit(ratio, scale, maxHpToUse, entry.getActualDamageSum());
						}
						if (hpBefore > 0)
						{
							entry.setEstimatedHpBeforeHit(hpBefore);
							entry.setOpponentMaxHp(maxHpToUse);
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
					double originalDeserved = gmaulEntry.getDeservedDamage();
					gmaulEntry.setMaxHit(originalMax * totalGmaulHitsMatchedThisTick);
					gmaulEntry.setMinHit(originalMin * totalGmaulHitsMatchedThisTick);
					gmaulEntry.setDeservedDamage(originalDeserved * totalGmaulHitsMatchedThisTick);
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

						Double koChanceCurrent = (hpBeforeCurrent != null)
							? PvpPerformanceTrackerUtils.calculateKoChance(entry.getAccuracy(), entry.getMinHit(), entry.getMaxHit(), hpBeforeCurrent)
							: null;
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

	// #################################################################################################################
	// ################################## Plugin-specific functions & global helpers ###################################
	// #################################################################################################################

	private void sendUpdateChatMessages()
	{
		if (!config.updateNoteMay72025Shown_v2())
		{
			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.GAMEMESSAGE)
					.runeLiteFormattedMessage(config.updateNoteMay72025Shown_v2_MESSAGE)
					.build());
			configManager.setConfiguration(CONFIG_KEY, "updateNoteMay72025Shown_v2", true);
		}
		if (!config.updateNote1_7_0())
		{
			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.GAMEMESSAGE)
					.runeLiteFormattedMessage(config.updateNote1_7_0_MESSAGE)
					.build());
			configManager.setConfiguration(CONFIG_KEY, "updateNote1_7_0", true);
		}
	}

	private void update(String oldVersion)
	{
		switch (oldVersion)
		{
			case "1.4.0":
			case "1.4.1":
			case "1.4.2":
			case "1.4.3":
			case "1.4.4":
			case "1.4.5":
			case "1.4.6":
			case "1.4.7":
			case "1.4.8":
			case "1.5.0":
			case "1.5.1":
			case "1.5.2":
			case "1.5.3":
			case "1.5.4":
			case "1.5.5":
				updateFrom1_5_5to1_5_6();
				break;
			case "1.6.2":
				updateFrom1_6_2to1_6_3();
				break;
		}

		configManager.setConfiguration(CONFIG_KEY, "pluginVersion", PLUGIN_VERSION);
	}

	// very basic update: We added the new hit on robe statistic, instantly recalculate it on launch,
	// so that the statistic will be visible for new players (without having to change the config)
	private void updateFrom1_6_2to1_6_3()
	{
		importFightHistoryData();

		// don't rebuild the panel since it doesn't exist yet at this point, will be rebuilt after this update() call.
		recalculateAllRobeHits(false);

		// re-save the fights that were populated with robe hit statistics
		saveFightHistoryData();
	}

	private void updateFrom1_5_5to1_5_6()
	{
		try
		{
			log.info("Updating data from 1.5.5 (or earlier) to 1.5.6...");

			FIGHT_HISTORY_DATA_DIR.mkdirs();
			File fightHistoryData = new File(FIGHT_HISTORY_DATA_DIR, FIGHT_HISTORY_DATA_FNAME);

			// if the fight history data file doesn't exist, create it with an empty array.
			if (!fightHistoryData.exists())
			{
				Writer writer = new FileWriter(fightHistoryData);
				writer.write("[]");
				writer.close();
				return;
			}

			fightHistory.clear();

			// read the old saved fights from the file into an array, and add them as an updated
			// fight to the fightHistory list.
			Arrays.asList(GSON.fromJson(new FileReader(fightHistoryData), FightPerformance__1_5_5[].class))
				.forEach((oldFight) -> fightHistory.add(new FightPerformance(oldFight)));

			// now that the fights were deserialized and updated to the newest version, simply save them.
			// afterwards, they will be re-loaded normally. Bit inefficient but not a big deal
			saveFightHistoryData();
			log.info("Successfully updated from 1.5.5 to 1.5.6");
		}
		catch (Exception e)
		{
			log.warn("Error while updating fight history data from 1.5.5 to 1.5.6: " + e.getMessage());
			// Display no modal for this error since it could happen on client load and that has odd behavior.
		}
	}
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
	private void saveFightHistoryData()
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

	void recalculateAllRobeHits(boolean rebuildPanel)
	{
		clientThread.invokeLater(() ->
		{
			// Recalculate robe hits for all fights based on the new filter and refresh UI
			for (FightPerformance f : fightHistory)
			{
				f.calculateRobeHits(config.robeHitFilter());
			}
			if (currentFight != null)
			{
				currentFight.calculateRobeHits(config.robeHitFilter());
			}
			if (rebuildPanel)
			{
				// Explicitly rebuild panel after recalculation.
				SwingUtilities.invokeLater(() -> {
					if (panel != null) {
						panel.rebuild();
					}
				});
			}
		});
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
	}

	// import additional/extra fight history data supplied by the user
	// this only does the direct json deserialization and success response (modals)
	// more specific FightPerformance processing is done in importFights()
	public void importUserFightHistoryData(String data)
	{
		if (data == null || data.trim().isEmpty()) { return; }
		try
		{
			// read saved fights from the data string and import them
			List<FightPerformance> savedFights = Arrays.asList(GSON.fromJson(data, FightPerformance[].class));
			importFights(savedFights);
			panel.rebuild();
			createConfirmationModal(true, "Successfully imported " + savedFights.size() + " fights.");
		}
		catch (Exception e)
		{
			log.warn("Error while importing user's fight history data: " + e.getMessage());
			// If an error was detected while deserializing fights, display that as a message dialog.
			createConfirmationModal(false, "Fight history data was invalid, and could not be imported.");
			return;
		}
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
		if (fights == null || fights.isEmpty()) { return; }

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
		saveFightHistoryData();
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
	public void addItemToLabelIfValid(JLabel label, int itemId, boolean verifyId, Runnable swingCallback, String tooltipOverride)
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

	public void updateNameFilterConfig(String newFilterName)
	{
		configManager.setConfiguration(CONFIG_KEY, "nameFilter", newFilterName.trim().toLowerCase());
	}
}
