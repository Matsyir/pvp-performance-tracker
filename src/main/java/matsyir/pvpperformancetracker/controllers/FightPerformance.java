/*
 * Copyright (c) 2021, Matsyir <https://github.com/matsyir>
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
package matsyir.pvpperformancetracker.controllers;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import joptsimple.internal.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.AnimationData.AttackStyle;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import matsyir.pvpperformancetracker.models.FightType;
import matsyir.pvpperformancetracker.utils.FightIdGenerator;
import matsyir.pvpperformancetracker.views.FightPerformancePanel;
import net.runelite.api.AnimationID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerConfig;
import net.runelite.api.kit.KitType;
import static matsyir.pvpperformancetracker.utils.PvpPerformanceTrackerUtils.fixItemId;
import static matsyir.pvpperformancetracker.controllers.PvpDamageCalc.RANGE_DEF;

// Holds two Fighters which contain data about PvP fight performance, and has many methods to
// add to the fight, display stats or check the status of the fight.
@Slf4j
@Getter
public class FightPerformance implements Comparable<FightPerformance>
{
	private static final int[] DEATH_ANIMATIONS = {
		AnimationID.DEATH,	// Default
		10629,	// League IV
		11902,	// League V
	};
	// Delay to assume a fight is over. May seem long, but sometimes people barrage &
	// stand under for a while to eat. Fights will automatically end when either competitor dies.
	private static final Duration NEW_FIGHT_DELAY = Duration.ofSeconds(21);

	@Expose
	@SerializedName("c") // use 1 letter serialized variable names for more compact storage
	public Fighter competitor;
	@Expose
	@SerializedName("o")
	public Fighter opponent;
	@Expose
	@SerializedName("t")
	public long lastFightTime; // last fight time saved as epochMilli timestamp (serializing an Instant was a bad time)
	@Expose
	@SerializedName("l")
	public FightType fightType; // save a FightType if the fight was done in LMS, and with what build
	@Expose
	@SerializedName("w")
	private int world;
	@Expose
	@SerializedName("fightID")
	@Getter
	private String fightId;
	@Expose
	@SerializedName("v")
	@Getter
	private String pluginVersion;
	@Expose
	@SerializedName("pn")
	private String pvpHubUploadName;
	@Expose
	@SerializedName("i")
	private InventorySnapshots inventorySnapshots;

	@Getter
	private transient FightPerformance pvpHubSyncedFight;
	@Getter
	// returns true if this IS the pvpHubSyncedFight, rather than the core/parent fight containing it.
	private transient boolean isSyncedFight = false;
	@Getter
	private transient boolean pvpHubSyncInProgress = false;
	private transient boolean fightIdGenerated = false;
	private transient long initialTime = 0;
	private transient int initialFightTick = -1;
	private transient boolean logTicksRelative = false;
	private transient int[] lastNonEmptyInventorySnapshot;

	private int competitorPrevHp; // intentionally don't serialize this, temp variable used to calculate hp healed.

	// KO Chance stats, updated per-attack. Not serialized.
	@Getter
	private transient double competitorTotalKoChance = 0;
	@Getter
	private transient double opponentTotalKoChance = 0;
	@Getter
	private transient Double competitorLastKoChance = null;
	@Getter
	private transient Double opponentLastKoChance = null;
	@Getter
	private transient int competitorKoChanceCount = 0;
	@Getter
	private transient int opponentKoChanceCount = 0;
	private transient double competitorSurvivalProb = 1.0;
	private transient double opponentSurvivalProb = 1.0;
	@Getter
	@Setter
	private transient String loadedFromFname;
	@Getter
	@Setter
	private transient boolean isFavorite;

	// shouldn't be used, just here so we can make a subclass, weird java thing
	public FightPerformance()
	{

	}

	// constructor which initializes a fight from the 2 Players, starting stats at 0. Regular use constructor.
	public FightPerformance(Player competitor, Player opponent)
	{
		int defLvl = PLUGIN.getClient().getBoostedSkillLevel(Skill.DEFENCE);

		// determine fight type based on being at LMS areas & use def level to check for LMS builds.
		this.fightType = !PLUGIN.isAtLMS() ? FightType.NORMAL :
			defLvl <= FightType.LMS_1DEF.getCombatLevelsForType().def ? FightType.LMS_1DEF :
			defLvl <= FightType.LMS_ZERK.getCombatLevelsForType().def ? FightType.LMS_ZERK :
			FightType.LMS_MAXMED;

		// initialize world
		this.world = PLUGIN.getClient().getWorld();

		// this is initialized soon before the NEW_FIGHT_DELAY time because the event we
		// determine the opponent from is not fully reliable.
		lastFightTime = Instant.now().minusSeconds(NEW_FIGHT_DELAY.getSeconds() - 5).toEpochMilli();
		initialTime = Instant.now().toEpochMilli();
		int[] startInventory = captureInventoryItemIds();
		this.inventorySnapshots = new InventorySnapshots(startInventory, null);
		recordNonEmptyInventorySnapshot(startInventory);

		this.competitor = new Fighter(this, competitor);
		this.opponent = new Fighter(this, opponent);

		this.competitorPrevHp = PLUGIN.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
		this.competitor.setLastGhostBarrageCheckedMageXp(PLUGIN.getClient().getSkillExperience(Skill.MAGIC));

		this.pluginVersion = PLUGIN.PLUGIN_VERSION;
	}

	// return a random fightPerformance used for testing UI
	static FightPerformance getTestInstance()
	{
		int cTotal = (int)(Math.random() * 60) + 8;
		int cSuccess = (int)(Math.random() * (cTotal - 4)) + 4;
		double cDamage = (Math.random() * (cSuccess * 25));

		int oTotal = (int)(Math.random() * 60) + 8;
		int oSuccess = (int)(Math.random() * (oTotal - 4)) + 4;
		double oDamage = (Math.random() * (oSuccess * 25));

		int secOffset = (int)(Math.random() * 57600) - 28800;

		boolean cDead = Math.random() >= 0.5;

		ArrayList<FightLogEntry> fightLogEntries = new ArrayList<>();
		int [] attackerItems = {0, 0, 0};
		int [] defenderItems = {0, 0, 0};
		String attackerName = "testname";
		FightLogEntry fightLogEntry = new FightLogEntry(attackerItems, 21, 0.5, 1, 12, defenderItems, attackerName);
		FightLogEntry fightLogEntry2 = new FightLogEntry(attackerItems, 11, 0.2, 1, 41, defenderItems, attackerName);
		FightLogEntry fightLogEntry3 = new FightLogEntry(attackerItems, 12, 0.3, 1, 21, defenderItems, attackerName);
		FightLogEntry fightLogEntry4 = new FightLogEntry(attackerItems, 43, 0.1, 1, 23, defenderItems, attackerName);
		fightLogEntries.add(fightLogEntry);
		fightLogEntries.add(fightLogEntry2);
		fightLogEntries.add(fightLogEntry3);
		fightLogEntries.add(fightLogEntry4);
		return new FightPerformance("Matsyir", "TEST_DATA", cSuccess, cTotal, cDamage, oSuccess, oTotal, oDamage, cDead, secOffset, fightLogEntries);
	}

	// Used for testing purposes
	private FightPerformance(String cName, String oName, int cSuccess, int cTotal, double cDamage, int oSuccess, int oTotal, double oDamage, boolean cDead, int secondOffset, ArrayList<FightLogEntry> fightLogs)
	{
		this.competitor = new Fighter(this, cName, fightLogs);
		this.opponent = new Fighter(this, oName, fightLogs);

		competitor.addAttacks(cSuccess, cTotal, cDamage, (int)cDamage, 20, 12, 13, 11, 22, 25, 26);
		opponent.addAttacks(oSuccess, oTotal, oDamage, (int)oDamage, 20, 14, 13, 11, 22, 25, 26);

		if (cDead)
		{
			competitor.died();
		}
		else
		{
			opponent.died();
		}

		lastFightTime = Instant.now().minusSeconds(secondOffset).toEpochMilli();
		this.initialTime = lastFightTime;
	}

	// If the given playerName is in this fight, check the Fighter's current animation,
	// add an attack if attacking, and compare attack style used with the opponent's overhead
	// to determine if successful.
	public void checkForAttackAnimations(
		Player eventSource,
		String interactingName,
		AnimationData animationData,
		int animationTick,
		long animationTime,
		CombatLevels competitorLevels)
	{
		if (eventSource == null || eventSource.getName() == null || interactingName == null || animationData == null)
		{
			return;
		}

		String eName = eventSource.getName(); // event source name
		boolean addedAttack = false;

		// verify that the player is interacting with their tracked opponent before adding attacks
		if (eName.equals(competitor.getName()) && Objects.equals(interactingName, opponent.getName()))
		{
			recordInitialFightTick(animationTick);
			competitor.setPlayer(eventSource);
			int offensivePray = PLUGIN.currentlyUsedOffensivePray();
			competitor.addAttack(
				opponent.getPlayer(),
				animationData,
				offensivePray,
				competitorLevels,
				animationTick,
				animationTime);
			lastFightTime = animationTime;
			addedAttack = true;
			ensureFightIdGenerated();

		}
		else if (eName.equals(opponent.getName()) && Objects.equals(interactingName, competitor.getName()))
		{
			recordInitialFightTick(animationTick);
			opponent.setPlayer(eventSource);
			// there is no offensive prayer data for the opponent so hardcode 0
			opponent.addAttack(competitor.getPlayer(), animationData, 0, null, animationTick, animationTime);
			addedAttack = true;
			// add a defensive log for the competitor while the opponent is attacking, to be used with the fight analysis/merge
			competitor.addDefensiveLogs(competitorLevels, PLUGIN.currentlyUsedOffensivePray(), animationTick, animationTime);
			lastFightTime = animationTime;
			ensureFightIdGenerated();
		}

		// Ensure robe hits are calculated for the live overlay when enabled (otherwise, just calc'd once at the end)
		// ideally should be refactored into Fighter, using ongoing counters like most other stats, instead of a loop.
		// However, I think it's mostly negligible due to how fight logs are limited to not becoming too large, due
		// to how long fights last.
		if (addedAttack && CONFIG.showOverlayRobeHits())
		{
			calculateRobeHits(CONFIG.robeHitFilter());
		}
	}

	private void recordInitialFightTick(int animationTick)
	{
		if (initialFightTick < 0)
		{
			initialFightTick = animationTick;
		}
	}

	public void makeLogTicksRelativeToFightStart()
	{
		if (logTicksRelative)
		{
			return;
		}

		ArrayList<FightLogEntry> entries = getAllFightLogEntries();
		int startTick = initialFightTick;
		if (startTick < 0)
		{
			startTick = entries.stream()
				.mapToInt(FightLogEntry::getTick)
				.min()
				.orElse(0);
		}

		makeLogTicksRelativeToStart(entries, startTick);
		logTicksRelative = true;
	}

	static void makeLogTicksRelativeToStart(ArrayList<FightLogEntry> entries, int startTick)
	{
		for (FightLogEntry entry : entries)
		{
			entry.setTick(Math.max(0, entry.getTick() - startTick));
		}
	}

	// this only gets called when the local client player receives a magic xp drop.
	public void checkForLocalGhostBarrage(CombatLevels competitorLevels, Player localPlayer)
	{
		if (localPlayer == null)
		{
			log.info("Client player null while checking for ghost barrage - shouldn't happen");
			return;
		}

		competitor.setPlayer(localPlayer);
		if (localPlayer.getInteracting() instanceof Player && localPlayer.getInteracting().getName().equals(opponent.getName()))
		{
			opponent.setPlayer((Player)localPlayer.getInteracting());
		}

		AnimationData animationData = competitor.getAnimationData();

		if (animationData == null || animationData.attackStyle != AnimationData.AttackStyle.MAGIC)
		{
			animationData = AnimationData.MAGIC_ANCIENT_MULTI_TARGET;

			int offensivePray = PLUGIN.currentlyUsedOffensivePray();
			competitor.addGhostBarrage(opponent.getPlayer().getOverheadIcon() != animationData.attackStyle.getProtection(),
				opponent.getPlayer(),
				AnimationData.MAGIC_ANCIENT_MULTI_TARGET,
				offensivePray,
				competitorLevels);
		}
	}

	// add damage dealt to the opposite player.
	// the player name being passed in is the one who has the hitsplat on them.
	public void addDamageDealt(String playerName, int damage)
	{
		if (playerName == null) { return; }

		if (playerName.equals(competitor.getName()))
		{
			opponent.addDamageDealt(damage);
		}
		else if (playerName.equals(opponent.getName()))
		{
			competitor.addDamageDealt(damage);
		}
	}

	public void updateCompetitorHp(int currentHp)
	{
		if (currentHp > competitorPrevHp)
		{
			int hpHealed = currentHp - competitorPrevHp;
			competitor.addHpHealed(hpHealed);
		}
		competitorPrevHp = currentHp;
	}

	// Will return true if either competitor has died yet
	// Completely ending the fight on despawn is handled by the plugin rather than within FightPerformance.
	public boolean checkForDeathAnimations()
	{
		// If either competitor is playing a death animation, mark dead but do not end the fight here.
		if (Arrays.stream(DEATH_ANIMATIONS).anyMatch(e -> e == opponent.getPlayer().getAnimation()))
		{
			opponent.died();
		}
		if (Arrays.stream(DEATH_ANIMATIONS).anyMatch(e -> e == competitor.getPlayer().getAnimation()))
		{
			competitor.died();
		}

		return competitor.isDead() || opponent.isDead();
	}

	// returns true if the fight is considered inactive due to time. We should end the fight when this happens
	public boolean isInactive()
	{
		// If there was no fight actions in the last NEW_FIGHT_DELAY seconds, consider the fight done, because
		// presumably either the player or the opponent ran away/teleported at this point.
		return Duration.between(Instant.ofEpochMilli(lastFightTime), Instant.now()).compareTo(NEW_FIGHT_DELAY) > 0;
	}

	public ArrayList<FightLogEntry> getAllFightLogEntries()
	{
		if (competitor.getFightLogEntries() == null || opponent.getFightLogEntries() == null)
		{
			return new ArrayList<>();
		}

		ArrayList<FightLogEntry> combinedList = new ArrayList<>();
		combinedList.addAll(competitor.getFightLogEntries());
		combinedList.addAll(opponent.getFightLogEntries());
		combinedList.sort(FightLogEntry::compareTo);
		return combinedList;
	}

	public void initializeFightLogNames()
	{
		if (competitor != null && competitor.getFightLogEntries() != null)
		{
			competitor.getFightLogEntries().forEach((FightLogEntry l) -> l.attackerName = competitor.getName());
		}
		if (opponent != null && opponent.getFightLogEntries() != null)
		{
			opponent.getFightLogEntries().forEach((FightLogEntry l) -> l.attackerName = opponent.getName());
		}
	}

	public boolean hasPvpHubSyncedFight()
	{
		return pvpHubSyncedFight != null;
	}

	public void setPvpHubSyncInProgress(boolean inProgress)
	{
		pvpHubSyncInProgress = inProgress;
	}

	public void setPvpHubSyncedFight(FightPerformance syncedFight)
	{
		pvpHubSyncedFight = syncedFight;
		pvpHubSyncInProgress = false;
		if (pvpHubSyncedFight != null)
		{
			syncedFight.isSyncedFight = true;
			pvpHubSyncedFight.applyLocalDisplayIdentityFrom(this);
			pvpHubSyncedFight.initializeFightLogNames();
		}
	}

	public void recordPvpHubUploadName(String uploadName)
	{
		pvpHubUploadName = normalizeName(uploadName) == null ? null : uploadName.trim();
	}

	public void recordEndingInventorySnapshot()
	{
		if (inventorySnapshots == null)
		{
			inventorySnapshots = new InventorySnapshots(null, null);
		}
		int[] endingInventory = captureInventoryItemIds();
		if (hasAnyItem(endingInventory))
		{
			inventorySnapshots.end = endingInventory;
			recordNonEmptyInventorySnapshot(endingInventory);
			return;
		}

		inventorySnapshots.end = lastNonEmptyInventorySnapshot != null ? lastNonEmptyInventorySnapshot : endingInventory;
	}

	public void recordCurrentInventorySnapshot()
	{
		recordNonEmptyInventorySnapshot(captureInventoryItemIds());
	}

	private void recordNonEmptyInventorySnapshot(int[] inventory)
	{
		if (hasAnyItem(inventory))
		{
			lastNonEmptyInventorySnapshot = inventory;
		}
	}

	private boolean hasAnyItem(int[] inventory)
	{
		if (inventory == null)
		{
			return false;
		}
		for (int itemId : inventory)
		{
			if (itemId > 0)
			{
				return true;
			}
		}
		return false;
	}

	private int[] captureInventoryItemIds()
	{
		ItemContainer inventory = PLUGIN.getClient().getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			return new int[0];
		}

		Item[] items = inventory.getItems();
		int[] itemIds = new int[items.length];
		for (int i = 0; i < items.length; i++)
		{
			Item item = items[i];
			itemIds[i] = item == null ? -1 : item.getId();
		}
		return itemIds;
	}

	public FightPerformance getPvpHubDisplayFight()
	{
		if (pvpHubSyncedFight == null)
		{
			return this;
		}

		pvpHubSyncedFight.applyLocalDisplayIdentityFrom(this);
		pvpHubSyncedFight.initializeFightLogNames();
		return pvpHubSyncedFight;
	}

	private void applyLocalDisplayIdentityFrom(FightPerformance localFight)
	{
		if (localFight == null)
		{
			return;
		}
		alignSyncedFightPerspective(localFight);
		if (competitor != null && localFight.competitor != null)
		{
			competitor.setName(localFight.competitor.getName());
		}
		if (opponent != null && localFight.opponent != null)
		{
			opponent.setName(localFight.opponent.getName());
		}
		if (world <= 0)
		{
			world = localFight.world;
		}
		if (fightId == null || fightId.isEmpty())
		{
			fightId = localFight.fightId;
		}
		if (pluginVersion == null)
		{
			pluginVersion = localFight.pluginVersion;
		}
	}

	private void alignSyncedFightPerspective(FightPerformance localFight)
	{
		if (localFight.competitor == null || localFight.opponent == null || competitor == null || opponent == null)
		{
			return;
		}

		String localCompetitorName = normalizeName(localFight.competitor.getName());
		String localOpponentName = normalizeName(localFight.opponent.getName());
		String localUploadName = normalizeName(localFight.pvpHubUploadName);
		String syncedCompetitorName = normalizeName(competitor.getName());
		String syncedOpponentName = normalizeName(opponent.getName());

		int nameOrientation = resolveNameOrientation(localCompetitorName, localOpponentName, localUploadName,
			syncedCompetitorName, syncedOpponentName);
		if (nameOrientation > 0 || (nameOrientation == 0 && shouldSwapByFingerprint(localFight)))
		{
			swapFighters();
		}
	}

	private int resolveNameOrientation(String localCompetitorName, String localOpponentName, String localUploadName,
		String syncedCompetitorName, String syncedOpponentName)
	{
		if (syncedCompetitorName == null || syncedOpponentName == null)
		{
			return 0;
		}

		boolean syncedCompetitorIsLocalCompetitor = nameMatchesLocalCompetitor(syncedCompetitorName, localCompetitorName, localUploadName);
		boolean syncedOpponentIsLocalCompetitor = nameMatchesLocalCompetitor(syncedOpponentName, localCompetitorName, localUploadName);
		boolean syncedCompetitorIsLocalOpponent = localOpponentName != null && localOpponentName.equals(syncedCompetitorName);
		boolean syncedOpponentIsLocalOpponent = localOpponentName != null && localOpponentName.equals(syncedOpponentName);

		if (syncedCompetitorIsLocalCompetitor && !syncedOpponentIsLocalCompetitor)
		{
			return -1;
		}
		if (syncedOpponentIsLocalCompetitor && !syncedCompetitorIsLocalCompetitor)
		{
			return 1;
		}
		if (syncedCompetitorIsLocalOpponent && !syncedOpponentIsLocalOpponent && !syncedCompetitorIsLocalCompetitor)
		{
			return 1;
		}
		if (syncedOpponentIsLocalOpponent && !syncedCompetitorIsLocalOpponent && !syncedOpponentIsLocalCompetitor)
		{
			return -1;
		}

		return 0;
	}

	private boolean nameMatchesLocalCompetitor(String syncedName, String localCompetitorName, String localUploadName)
	{
		return syncedName != null && ((localCompetitorName != null && localCompetitorName.equals(syncedName)) ||
			(localUploadName != null && localUploadName.equals(syncedName)));
	}

	private boolean shouldSwapByFingerprint(FightPerformance localFight)
	{
		int currentScore = fighterFingerprintDistance(competitor, localFight.competitor) +
			fighterFingerprintDistance(opponent, localFight.opponent);
		int swappedScore = fighterFingerprintDistance(competitor, localFight.opponent) +
			fighterFingerprintDistance(opponent, localFight.competitor);
		return swappedScore + 5 < currentScore;
	}

	private int fighterFingerprintDistance(Fighter syncedFighter, Fighter localFighter)
	{
		if (syncedFighter == null || localFighter == null)
		{
			return 1_000_000;
		}

		int score = 0;
		score += Math.abs(syncedFighter.getDamageDealt() - localFighter.getDamageDealt()) * 4;
		score += Math.abs(syncedFighter.getAttackCount() - localFighter.getAttackCount()) * 3;
		score += Math.abs(syncedFighter.getOffPraySuccessCount() - localFighter.getOffPraySuccessCount()) * 2;
		score += Math.abs(syncedFighter.getOffensivePraySuccessCount() - localFighter.getOffensivePraySuccessCount()) * 2;
		score += Math.abs(syncedFighter.getTotalMagicAttackCount() - localFighter.getTotalMagicAttackCount()) * 2;
		score += Math.abs(syncedFighter.getMagicHitCount() - localFighter.getMagicHitCount()) * 2;
		score += Math.abs(syncedFighter.getHpHealed() - localFighter.getHpHealed());
		score += Math.abs(syncedFighter.getGhostBarrageCount() - localFighter.getGhostBarrageCount()) * 2;
		score += Math.abs(fightLogCount(syncedFighter) - fightLogCount(localFighter)) * 3;
		if (syncedFighter.isDead() != localFighter.isDead())
		{
			score += 25;
		}
		return score;
	}

	private int fightLogCount(Fighter fighter)
	{
		return fighter.getFightLogEntries() == null ? 0 : fighter.getFightLogEntries().size();
	}

	private void swapFighters()
	{
		Fighter originalCompetitor = competitor;
		competitor = opponent;
		opponent = originalCompetitor;
	}

	@Getter
	public static class InventorySnapshots
	{
		@Expose
		@SerializedName("s")
		private int[] start;
		@Expose
		@SerializedName("e")
		private int[] end;

		private InventorySnapshots(int[] start, int[] end)
		{
			this.start = start;
			this.end = end;
		}
	}

	private static String normalizeName(String name)
	{
		if (name == null)
		{
			return null;
		}

		String normalizedName = name.trim();
		return normalizedName.isEmpty() ? null : normalizedName.toLowerCase(Locale.ROOT);
	}

	// Count the fight as started if either:
	// 1. The competitor has attacked the opponent
	// 2. The opponent has attacked the competitor
	// Attacks are only counted when there is a confirmed interaction between the two players
	// Checking both makes sure that the very first interaction is counted/tracked
	public boolean fightStarted()
	{
		return competitor.getAttackCount() > 0 || opponent.getAttackCount() > 0;
	}

	// returns true if competitor off-pray hit success rate > opponent success rate.
	// the following functions have similar behaviour.
	public boolean competitorOffPraySuccessIsGreater()
	{
		return competitor.calculateOffPraySuccessPercentage() > opponent.calculateOffPraySuccessPercentage();
	}

	public boolean opponentOffPraySuccessIsGreater()
	{
		return opponent.calculateOffPraySuccessPercentage() > competitor.calculateOffPraySuccessPercentage();
	}

	public boolean competitorExpectedDmgIsGreater()
	{
		return competitor.getExpectedDamage() > opponent.getExpectedDamage();
	}

	public boolean opponentExpectedDmgIsGreater()
	{
		return opponent.getExpectedDamage() > competitor.getExpectedDamage();
	}

	public boolean competitorDmgDealtIsGreater()
	{
		return competitor.getDamageDealt() > opponent.getDamageDealt();
	}

	public boolean opponentDmgDealtIsGreater()
	{
		return opponent.getDamageDealt() > competitor.getDamageDealt();
	}

	public boolean competitorMagicHitsLuckier()
	{
		double competitorRate = (competitor.getMagicHitCountExpected() == 0) ? 0 :
			(competitor.getMagicHitCount() / competitor.getMagicHitCountExpected());
		double opponentRate = (opponent.getMagicHitCountExpected() == 0) ? 0 :
			(opponent.getMagicHitCount() / opponent.getMagicHitCountExpected());

		return competitorRate > opponentRate;
	}

	public boolean opponentMagicHitsLuckier()
	{
		double competitorRate = (competitor.getMagicHitCountExpected() == 0) ? 0 :
			(competitor.getMagicHitCount() / competitor.getMagicHitCountExpected());
		double opponentRate = (opponent.getMagicHitCountExpected() == 0) ? 0 :
			(opponent.getMagicHitCount() / opponent.getMagicHitCountExpected());

		return opponentRate > competitorRate;
	}

	public double getCompetitorExpectedDmgDiff()
	{
		return competitor.getExpectedDamage() - opponent.getExpectedDamage();
	}

	public double getCompetitorDmgDealtDiff()
	{
		return competitor.getDamageDealt() - opponent.getDamageDealt();
	}

	// use to sort by last fight time, to sort fights by date/time.
	@Override
	public int compareTo(FightPerformance o)
	{
		long diff = lastFightTime - o.lastFightTime;

		// if diff = 0, return 0. Otherwise, divide diff by its absolute value. This will result in
		// -1 for negative numbers, and 1 for positive numbers, keeping the sign and a safely small int.
		return diff == 0 ? 0 :
			(int)(diff / Math.abs(diff));
	}

	/**
	 * Calculates robe hits for both competitors based on defender gear in each log entry and the configured filter.
	 */
	public void calculateRobeHits(PvpPerformanceTrackerConfig.RobeHitFilter filter)
	{
		competitor.resetRobeHits();
		opponent.resetRobeHits();
		ArrayList<FightLogEntry> allFightLogEntries	= getAllFightLogEntries();
		if (filter == null || allFightLogEntries == null || allFightLogEntries.isEmpty())
		{
			return;
		}

		for (FightLogEntry entry : allFightLogEntries)
		{
			if (entry == null)
			{
				continue;
			}

			// Only consider melee and ranged attacks for robe hits
			AnimationData ad = entry.getAnimationData();
			if (ad == null)
			{
				continue;
			}
			AttackStyle style = ad.attackStyle;
			if (style == AttackStyle.MAGIC)
			{
				continue;
			}

			// Determine who the defender is for this specific entry
			Fighter defender = entry.getAttackerName().equals(competitor.getName()) ? opponent : competitor;
			if (defender == null)
			{
				continue;
			}

			int[] gear = entry.getDefenderGear();
			if (gear == null || gear.length <= Math.max(KitType.LEGS.getIndex(), KitType.TORSO.getIndex()))
			{
				continue;
			}

			int defenderLegsItemId = fixItemId(gear[KitType.LEGS.getIndex()]);
			int defenderBodyItemId = fixItemId(gear[KitType.TORSO.getIndex()]);

			// Compute stats for robe bottom/top
			boolean wearingRobeBottom = false;
			if (defenderLegsItemId != 0)
			{
				int[] legStats = PvpDamageCalc.getItemStats(defenderLegsItemId);
				if (legStats != null && legStats.length > RANGE_DEF)
				{
					wearingRobeBottom = legStats[RANGE_DEF] <= 0; // ==0 seems reliable but <=0 doesn't hurt
				}
			}

			boolean wearingRobeTop = false;
			if (defenderBodyItemId != 0)
			{
				int[] bodyStats = PvpDamageCalc.getItemStats(defenderBodyItemId);
				if (bodyStats != null && bodyStats.length > RANGE_DEF)
				{
					wearingRobeTop = bodyStats[RANGE_DEF] <= 0; // ==0 seems reliable but <=0 doesn't hurt
				}
			}

			boolean isHitOnRobes = false;
			switch (filter)
			{
				case BOTTOM:
					isHitOnRobes = wearingRobeBottom;
					break;
				case TOP:
					isHitOnRobes = wearingRobeTop;
					break;
				case BOTH:
					isHitOnRobes = wearingRobeBottom && wearingRobeTop;
					break;
				case EITHER:
					isHitOnRobes = wearingRobeBottom || wearingRobeTop;
					break;
			}

			if (isHitOnRobes)
			{
				if (defender == competitor)
				{
					competitor.addRobeHit();
				}
				else if (defender == opponent)
				{
					opponent.addRobeHit();
				}
			}
		}
	}

	/**
	 * Generates the fight ID if it hasn't been generated yet and the upload config is enabled.
	 * Uses both player names, world, and the first recorded fight-log timestamp as seed material.
	 */
	private void ensureFightIdGenerated()
	{
		if (fightIdGenerated || !CONFIG.uploadFightsToPvpHub())
		{
			return;
		}

		if (competitor.getName() != null && opponent.getName() != null)
		{
			fightId = FightIdGenerator.generateFightId(
				competitor.getName(),
				opponent.getName(),
				world,
				getFightIdAnchorTime()
			);
			fightIdGenerated = true;
		}
	}

	long getFightIdAnchorTime()
	{
		return getFightIdAnchorTime(getAllFightLogEntries(), lastFightTime);
	}

	static long getFightIdAnchorTime(ArrayList<FightLogEntry> entries, long fallbackTime)
	{
		return entries.stream()
			.mapToLong(FightLogEntry::getTime)
			.min()
			.orElse(fallbackTime);
	}

    public void updateKoChanceStats(FightLogEntry entry)
	{
		if (entry.getDisplayKoChance() == null) { return; }

		double koChance = entry.getDisplayKoChance();

		if (entry.getAttackerName().equals(competitor.getName()))
		{
			competitorKoChanceCount++;
			competitorLastKoChance = koChance;
			competitorSurvivalProb *= (1.0 - koChance);
			competitorTotalKoChance = 1.0 - competitorSurvivalProb;
		}
		else
		{
			opponentKoChanceCount++;
			opponentLastKoChance = koChance;
			opponentSurvivalProb *= (1.0 - koChance);
			opponentTotalKoChance = 1.0 - opponentSurvivalProb;
		}
	}

	public FightPerformancePanel.BackgroundStyle getBgStyle()
	{
		boolean cmpDied = competitor.isDead();
		boolean oppDied = opponent.isDead();
		FightLogEntry lastCmpLog = null;
		try
		{
			ArrayList<FightLogEntry> competitorLogs = new ArrayList<>(competitor.getFightLogEntries());;
			lastCmpLog = competitorLogs.get(competitorLogs.size() - 1);
		}
		catch (Exception e) {}
		FightLogEntry lastOppLog = null;
		try
		{
			ArrayList<FightLogEntry> oppLogs = new ArrayList<>(opponent.getFightLogEntries());
			lastOppLog = oppLogs.get(oppLogs.size() - 1);
		}
		catch (Exception e) {}

		boolean validCmpLog = (lastCmpLog != null && lastCmpLog.getAnimationData() != null);
		boolean validOppLog = (lastOppLog != null && lastOppLog.getAnimationData() != null);
		if (!validCmpLog && !validOppLog)
		{
			return FightPerformancePanel.BackgroundStyle.DEFAULT;
		}

		boolean isCmpMaxHitKo = validCmpLog && oppDied && lastCmpLog.getMaxHit() == lastCmpLog.getActualDamageSum();;
		boolean isCmpSpecKo = validCmpLog && oppDied && lastCmpLog.getAnimationData().isSpecial;
		boolean isCmpSpecMaxHitKo = isCmpSpecKo && isCmpMaxHitKo;

		boolean isCmpPunchKo = validCmpLog && oppDied && lastCmpLog.getAnimationData() == AnimationData.MELEE_PUNCH;
		boolean isCmpKickKo = validCmpLog && oppDied && lastCmpLog.getAnimationData() == AnimationData.MELEE_KICK;
		boolean isCmpStaffKo = validCmpLog && oppDied && lastCmpLog.getAnimationData().isStaffBash();

		boolean isOppMaxHitKo = validOppLog && cmpDied && lastOppLog.getMaxHit() == lastOppLog.getActualDamageSum();
		boolean isOppSpecKo = validOppLog && cmpDied && lastOppLog.getAnimationData().isSpecial;
		boolean isOppSpecMaxHitKo = isOppSpecKo && isOppMaxHitKo;

		boolean isOppPunchKo = validOppLog && cmpDied && lastOppLog.getAnimationData() == AnimationData.MELEE_PUNCH;
		boolean isOppKickKo = validOppLog && cmpDied && lastOppLog.getAnimationData() == AnimationData.MELEE_KICK;
		boolean isOppStaffKo = validOppLog && cmpDied && lastOppLog.getAnimationData().isStaffBash();

		if (isCmpSpecMaxHitKo || isOppSpecMaxHitKo)
		{
			return FightPerformancePanel.BackgroundStyle.MAX_SPEC_KO;
		}
		else if (isCmpSpecKo || isOppSpecKo)
		{
			return FightPerformancePanel.BackgroundStyle.SPEC_KO;
		}
		else if (isCmpPunchKo || isOppPunchKo)
		{
			return FightPerformancePanel.BackgroundStyle.PUNCH_KO;
		}
		else if (isCmpKickKo || isOppKickKo)
		{
			return FightPerformancePanel.BackgroundStyle.KICK_KO;
		}
		else if (isCmpStaffKo || isOppStaffKo)
		{
			return FightPerformancePanel.BackgroundStyle.STAFF_KO;
		}
		else if (isCmpMaxHitKo || isOppMaxHitKo)
		{
			return FightPerformancePanel.BackgroundStyle.MAX_HIT_KO;
		}

		return FightPerformancePanel.BackgroundStyle.DEFAULT;
	}

	public boolean isRelevantForFilter(String filter, FightPerformancePanel.BackgroundStyle bgStyle)
	{
		// empty is a valid filter, it means display every fight - don't filter them
		if (Strings.isNullOrEmpty(filter))
		{
			return true;
		}
		// temp dev filter for testing ui with long names
//		try
//		{
//			if (filter.startsWith("long"))
//			{
//				return competitor.getName().length() >= 12 || opponent.getName().length() >= 12;
//			}
//		}
//		catch (Exception e) { }

		try
		{
			// before even checking for normal results, see if the filter is a "preset hardcoded filter", in which case
			// we won't include other kinds of results along with those, only the desired filter.
			// skip all these checks if length<4 since our shortest hardcoded filter is 4 characters (kill)
			if (filter.length() >= 4)
			{
				if (filter.equals("favorite"))
				{
					return this.isFavorite;
				}
				if (filter.equals("kill"))
				{
					return opponent.isDead();
				}
				if (filter.equals("death"))
				{
					return competitor.isDead();
				}
				if (filter.equals("double") || filter.equals("doubledeath"))
				{
					return competitor.isDead() && opponent.isDead();
				}
				if (Arrays.asList(FightPerformancePanel.BackgroundStyle.SHOW_ALL_FILTER_WORDS).contains(filter))
				{
					return bgStyle.isEnabled() && bgStyle != FightPerformancePanel.BackgroundStyle.DEFAULT;
				}
			}


			boolean isRelevant = ( // first filter result type: basic name match. Whether it's an exact match or just startsWith depends on config.
					(CONFIG.exactNameFilter()
						? (competitor.getName().toLowerCase().equals(filter) || opponent.getName().toLowerCase().equals(filter))
						: (competitor.getName().toLowerCase().startsWith(filter) || opponent.getName().toLowerCase().startsWith(filter))
					)
				|| ( // second filter result type: bgStyle.name match. e.g, you can search 'max' or 'max hit' to see fights that ended in a max hit ko.
					(bgStyle != FightPerformancePanel.BackgroundStyle.DEFAULT
						&& bgStyle.isEnabled()
						&& (bgStyle.getName().toLowerCase().startsWith(filter)
						|| bgStyle.getName().replace(" ", "").toLowerCase().startsWith(filter.replace(" ", "")))
					)
				)
			);

			// if it's already relevant from one of the first 2 filter types, skip checking for the 3rd.
			if (isRelevant)
			{
				return isRelevant;
			}
		}
		// if there's somehow an exception during this filter (shouldn't happen), just return false
		catch (Exception e)
		{
			return false;
		}

		// third filter type: Filter by >, >=, <, or <= total number of attacks from the client player / competitor
		// e.g, >60 will show fights with over 60 attacks by the client player.
		// <=10 will show fights with <= 10 total attacks by the client player.
		try
		{
			if (filter.length() < 2)
			{
				return false;
			}
			boolean isGtFilter = filter.startsWith(">");
			boolean isLtFilter = filter.startsWith("<");
			if (!isGtFilter && !isLtFilter && filter.length() < 3)
			{
				return false;
			}
			boolean isGteFilter = filter.startsWith(">=");
			boolean isLteFilter = filter.startsWith("<=");

			if (!isGtFilter && !isGteFilter && !isLtFilter && !isLteFilter)
			{
				return false;
			}

			boolean is2charStart = isGteFilter || isLteFilter;
			StringBuilder sb = new StringBuilder();

			// remove gt/gte symbol, remove all chars that aren't digits, append to string
			filter.substring(is2charStart ? 2 : 1)
				.chars()
				.mapToObj(c -> (char)c)
				.filter(Character::isDigit)
				.forEach(sb::append);

			// parse int from string, only compare if it's >0
			int filterNumber = Integer.parseInt(sb.toString());
			if (filterNumber <= 0)
			{
				return false;
			}

			return (isGtFilter && competitor.getAttackCount() > filterNumber) ||
				(isGteFilter && competitor.getAttackCount() >= filterNumber) ||
				(isLtFilter && competitor.getAttackCount() < filterNumber) ||
				(isLteFilter && competitor.getAttackCount() <= filterNumber);
		}
		catch(Exception e)
		{
			return false;
		}
	}

	public boolean isSavedToFile()
	{
		return !Strings.isNullOrEmpty(loadedFromFname);
	}
}
