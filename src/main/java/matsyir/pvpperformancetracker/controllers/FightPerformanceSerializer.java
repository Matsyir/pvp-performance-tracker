/*
 * Copyright (c) 2026, Matsyir <https://github.com/Matsyir>
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.GSON;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.client.RuneLite;

@Slf4j
public class FightPerformanceSerializer
{
	// ============================================== Constants & init ==============================================
	public static final String DATA_FOLDER = PvpPerformanceTrackerPlugin.DATA_FOLDER;
	public static final String FIGHT_HISTORY_DATA_FOLDER = "FightHistoryData"; // subfolder of DATA_FOLDER
	public static final String FAV_FIGHTS_FOLDER = "FavFights"; // subfolder of DATA_FOLDER

	public static final File BASE_DATA_DIR;
	public static final File FIGHT_HISTORY_DATA_DIR;
	public static final File FAV_FIGHTS_DIR;

	private static long sessionStartTime = Instant.now().toEpochMilli();

	static
	{
		BASE_DATA_DIR = PvpPerformanceTrackerPlugin.BASE_DATA_DIR;
		FIGHT_HISTORY_DATA_DIR = new File(RuneLite.RUNELITE_DIR, DATA_FOLDER + "/" + FIGHT_HISTORY_DATA_FOLDER);
		FAV_FIGHTS_DIR = new File(RuneLite.RUNELITE_DIR, DATA_FOLDER + "/" + FAV_FIGHTS_FOLDER);

		try
		{
			tryMkdirs(BASE_DATA_DIR);
			tryMkdirs(FIGHT_HISTORY_DATA_DIR);
			tryMkdirs(FAV_FIGHTS_DIR);
		}
		catch (FileNotFoundException ignored) { }
	}

	public enum JsonGzChunkType
	{
		IMPORTED_FROM_JSON("FightHistoryData-c", FIGHT_HISTORY_DATA_DIR),
		NEW_CHUNK("Fights_", FIGHT_HISTORY_DATA_DIR),
		FAVORITE_FIGHT_CHUNK("Fav_", FAV_FIGHTS_DIR);

		static final String DATA_CHUNK_FILE_EXT = ".json.gz";

		public static boolean isFileValidChunk(File file)
		{
			// ignore any chunks greater than 10mb, they really shouldn't be going > 1-2MB with normal use.
			// also ignore any that don't end with .json.gz
			if (file.length() > (10 * 1024 * 1024) || !file.getName().endsWith(DATA_CHUNK_FILE_EXT))
			{
				return false;
			}

			return getChunkType(file.getName()) != null;
		}
		public static JsonGzChunkType getChunkType(String fname)
		{
			for (JsonGzChunkType cType : JsonGzChunkType.values())
			{
				if (fname != null && !fname.isEmpty() && fname.startsWith(cType.fnamePrefix))
				{
					return cType;
				}
			}
			return null;
		}

		final String fnamePrefix;
		final File directory;

		JsonGzChunkType(String fnamePrefix, File directory)
		{
			this.fnamePrefix = fnamePrefix;
			this.directory = directory;
		}

		public File generateNewChunkFile(String appendedToPrefix)
		{
			return new File(this.directory, this.fnamePrefix + appendedToPrefix + DATA_CHUNK_FILE_EXT);
		}
	}

	// ============================================== SERIALIZATION ================================================

	// Save the currently loaded sessionFightHistory to local gzipped JSON.
	public static void serializeSessionFightHistory()
	{
		boolean written = serializeFightArray(PLUGIN.sessionFightHistory,
			JsonGzChunkType.NEW_CHUNK.generateNewChunkFile(String.valueOf(sessionStartTime)));

		if (written)
		{
			sessionStartTime = Instant.now().toEpochMilli();
		}
	}

	// TODO Actually use this + deserialize + UI
	// 	deserialize will just be a call to deserializeFightArray(), just need to include that in deserializeFightHistory
	//  to check both dirs instead of only main one
	public static void serializeFavoriteFight(FightPerformance fav)
	{
		// 1: copy fight to its own unique file
		String fightName = String.valueOf(fav.lastFightTime / 1000);
		try
		{
			fightName = fav.getCompetitor().getName() + "_vs_" + fav.getOpponent().getName() + "_" + fightName;
		}
		catch (Exception ignored) {}

		File newFavFightFile = JsonGzChunkType.FAVORITE_FIGHT_CHUNK.generateNewChunkFile(fightName);
		serializeFightArray(List.of(fav), newFavFightFile);


		// 2: remove fight from the chunk it was loaded from. can skip this if it's not saved yet / is from this session.
		// ensure we keep setLoadedFromFname set for any written files for later use
		removeFight(fav);
		fav.setLoadedFromFname(newFavFightFile.getName());
	}

	// returns true if we wrote to the file.
	// if returns false, doesn't necessarily mean there was an exception.
	private static boolean serializeFightArray(Collection<FightPerformance> fights, File newDataChunk)
	{
		// skip any data serialization if no fights were added in this session.
		if (fights.isEmpty())
		{
			return false;
		}

		try
		{
			tryMkdirs(newDataChunk);

			try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(newDataChunk.toPath())))
			{
				try (OutputStreamWriter writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8))
				{
					GSON.toJson(PLUGIN.sessionFightHistory, writer);

					return true;
				}
			}

		}
		catch (Exception e)
		{
			log.warn("FightPerformanceSerializer.serializeFightArray: Error ignored while writing fight data: {}", e.getMessage());
		}

		return false;
	}

	// ============================================== DESERIALIZATION ===============================================

	// import complete fight history data from the saved .json.gz data files
	// this function only handles the direct file processing and .json.gz deserialization.
	// more specific FightPerformance processing, and the addition to fightHistory is done in importFights()
	public static void deserializeFightHistory(Consumer<ArrayList<FightPerformance>> onReadCallback)
	{
		// catch and ignore any errors we may have forgotten to handle - the import will fail but at least the plugin
		// will continue to function. This should only happen if their fight history data is corrupted/outdated.
		try
		{
			tryMkdirs(FIGHT_HISTORY_DATA_DIR);

			List<File> fightDataChunkFiles = listAllDataChunks();
			// if there's no data files, then skip reading/importing data.
			if (fightDataChunkFiles.isEmpty())
			{
				log.info("FightPerformanceSerializer.deserializeFightHistory: Skipping deserialization due to no files found.");
				return;
			}

			ArrayList<FightPerformance> savedFights = new ArrayList<>();

			for (File fightDataChunk : fightDataChunkFiles)
			{
				// just use callback instead of the return, so it's O(1) instead of O(2), as we'd have to loop again
				// if we used savedFights.addAll(newArray)
				deserializeFightArray(fightDataChunk, savedFights::add);
			}

			if (!savedFights.isEmpty() && onReadCallback != null)
			{
				onReadCallback.accept(savedFights);
			}
		}
		catch (Exception e)
		{
			// Display no popup for this error since it could happen on client load and that has odd behavior.
			log.warn("FightPerformanceSerializer.deserializeFightHistory: Unexpected error while listing data files or deserializing fight history data: {}", e.getMessage());
		}
	}

	public static FightPerformance[] deserializeFightArray(File fightDataFile, Consumer<FightPerformance> perFightReadCallback)
	{
		FightPerformance[] fightsFromChunk = null;
		try (
			GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(fightDataFile.toPath()));
			InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8)
		)
		{
			fightsFromChunk = GSON.fromJson(reader, FightPerformance[].class);
			for (FightPerformance f : fightsFromChunk)
			{
				if (f == null || f.competitor == null || f.opponent == null)
				{
					continue;
				}

				// always set loadedFromFname on fights that were deserialized
				f.setLoadedFromFname(fightDataFile.getName());
				if (perFightReadCallback != null)
				{
					perFightReadCallback.accept(f);
				}
			}
		}
		catch (Exception e)
		{
			log.warn("FightPerformanceSerializer.deserializeFightArray: Error while deserializing fight history data chunk (path={}), errorMsg={}", fightDataFile.getAbsolutePath(), e.getMessage());
		}

		return fightsFromChunk;
	}

	// ============================================ UPDATING + DELETION ============================================

	// in importFightHistory, we set the loadedFromFname field to save which chunk the fight was loaded from.
	// Then find it by comparing a few fields, and re-write the chunk without it.
	// returns true if the fight was deleted and the related chunk was re-written or deleted
	public static boolean removeFight(FightPerformance fight)
	{
		if (Strings.isNullOrEmpty(fight.getLoadedFromFname()))
		{
			return false;
		}

		try
		{
			tryMkdirs(FIGHT_HISTORY_DATA_DIR);

			File loadedFromChunk = new File(FIGHT_HISTORY_DATA_DIR, fight.getLoadedFromFname());

			// if we can't find the file, then skip removing
			if (!loadedFromChunk.exists())
			{
				return false;
			}

			ArrayList<FightPerformance> fightsFromChunk = null;
			try (
				GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(loadedFromChunk.toPath()));
				InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8)
			)
			{
				fightsFromChunk = new ArrayList<>(Arrays.asList(GSON.fromJson(reader, FightPerformance[].class)));
			}
			catch (Exception e)
			{
				log.warn("FightPerformanceSerializer.removeFight: Error while deserializing fight history data chunk (path={}), errorMsg={}", loadedFromChunk.getAbsolutePath(), e.getMessage());
			}
			if (fightsFromChunk == null || fightsFromChunk.isEmpty())
			{
				return false;
			}

			int initialFightCount = fightsFromChunk.size();;

			fightsFromChunk.removeIf((f) -> f.lastFightTime == fight.lastFightTime
				&& f.opponent.getName().equals(fight.opponent.getName())
				&& f.getFightIdAnchorTime() == fight.getFightIdAnchorTime()
				&& f.getInitialTime() == fight.getInitialTime()
				&& f.getInitialFightTick() == fight.getInitialFightTick());

			if (fightsFromChunk.size() != (initialFightCount - 1))
			{
				log.debug("FightPerformanceSerializer.removeFight: Was going to delete more than 1 fight when attempting to only remove 1? Skipping removal - shouldn't happen - possible duplicate fight(s)?");
				return false;
			}

			// returns true on success
			return rewriteChunkWithFights(loadedFromChunk, fightsFromChunk);
		}
		catch (Exception e)
		{
			log.warn("FightPerformanceSerializer.removeFight: Unexpected error while listing data files or deserializing fight history data: {}", e.getMessage());
		}
		return false;
	}

	// simply delete all data chunks that match our expected naming scheme inside the fight history data folder
	public static boolean removeAllFights()
	{
		try
		{
			listAllDataChunks().forEach(file -> file.delete());
			return true;
		}
		catch (Exception e)
		{
			log.warn("FightPerformanceSerializer.removeAllFights: Unexpected error while removing one of the data chunk files.");
		}
		return false;
	}

	// ================================= private helper functions =================================
	private static List<File> listAllDataChunks()
	{
		try
		{
			tryMkdirs(FIGHT_HISTORY_DATA_DIR);
			return Arrays.stream(Objects.requireNonNull(
					FIGHT_HISTORY_DATA_DIR.listFiles(JsonGzChunkType::isFileValidChunk))
				).sorted(Comparator.comparingLong(File::lastModified).reversed())
				.collect(Collectors.toList());
		}
		catch (Exception e)
		{
			return Collections.emptyList();
		}
	}

	private static boolean rewriteChunkWithFights(File original, ArrayList<FightPerformance> newFightsToWrite)
	{
		try
		{

			String newChunkName = original.getName() + ".tmp";
			tryMkdirs(FIGHT_HISTORY_DATA_DIR);
			File updatedChunkFile = new File(FIGHT_HISTORY_DATA_DIR, newChunkName);

			try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(updatedChunkFile.toPath())))
			{
				try (OutputStreamWriter writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8))
				{
					GSON.toJson(newFightsToWrite, writer);
					writer.close();
					gzip.close();

					// returns true on successful move.
					return tryMoveAtomicOrStandard(updatedChunkFile, original);
				}
			}

		}
		catch (Exception e)
		{
			log.warn("FightPerformanceSerializer.removeFight: Error ignored while writing updated fight chunk: {}", e.getMessage());
		}

		return false;
	}

	// to be used as a means to replace a file with the highest success rate in 1 attempt.
	// attempts an atomic move, falls back to standard file replacement if it fails.
	// log.warn if neither worked.
	private static boolean tryMoveAtomicOrStandard(File from, File to)
	{
		try
		{
			if (from.length() > 0)
			{
				try
				{
					Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE);
				}
				catch(AtomicMoveNotSupportedException e)
				{
					Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}

				log.debug("FightPerformanceSerializer.tryMoveAtomicOrStandard: Successfully re-wrote chunk - {}", from.getName());
				return true;
			}
		}
		catch (Exception e)
		{
			log.warn("FightPerformanceSerializer.tryMoveAtomicOrStandard: Unexpected exception while attempting to move file: {}", e.getMessage());
		}

		return false;
	}

	// try directory.mkdirs, log & throw if it returns false. To be used in try-catch of a larger process
	private static void tryMkdirs(File directory) throws FileNotFoundException
	{
		if (directory == null || (!directory.exists() && !directory.mkdirs()))
		{

			log.warn("FightPerformanceSerializer.tryPrepareDir: directory.mkdirs failed on dir: " + (directory != null ? directory.getAbsolutePath() : ""));
			throw new FileNotFoundException("FightPerformanceSerializer.tryPrepareDir: Failed to prepare directory");
		}
	}

	// ================================= On-update functions, usually to migrate data =================================

	// convert saved fight history to chunked .gz files instead of 1 massive JSON
	// will also be re-applied onto 1.8.3 since we had a bug which made it so
	// the data migration didn't run if you had under 100 fights
	// old/original fightHistoryData .json file, to be imported 1.8.1 -> 1.8.2 (1 big .json to .json.gz chunks)
	private static final String _OLD_FIGHT_HISTORY_DATA_FNAME_JSON = "FightHistoryData.json";
	public static void updateFrom1_8_1to1_8_2()
	{
		log.info("PvpPerformanceTracker - starting update process - updateFrom1_8_1to1_8_2()");
		boolean success = true;
		final int MAX_FIGHTS_PER_UPDATED_GZ_CHUNK = 100;
		try
		{
			tryMkdirs(BASE_DATA_DIR);
			tryMkdirs(FIGHT_HISTORY_DATA_DIR);

			File originalJsonData = new File(BASE_DATA_DIR, _OLD_FIGHT_HISTORY_DATA_FNAME_JSON);

			// if the fight history data file doesn't exist, don't need to do anything.
			if (!originalJsonData.exists())
			{
				return;
			}

			// read the saved fights from the JSON file
			List<FightPerformance> savedFights = Arrays.asList(
				GSON.fromJson(new FileReader(originalJsonData), FightPerformance[].class));
			if (!savedFights.isEmpty())
			{
				ArrayList<FightPerformance> chunkedFights = new ArrayList<>();
				int fileIdx = 1; // number to be appended to filename
				for (int i = 0; i < savedFights.size(); i++)
				{
					chunkedFights.add(savedFights.get(i));

					// 100 fights max per chunk, so once it hits >= 100, write it, clear, and continue to next chunk
					// ensure we also write when we get to the end
					if (chunkedFights.size() >= MAX_FIGHTS_PER_UPDATED_GZ_CHUNK || (i+1) == savedFights.size())
					{
						try
						{
							File newGzChunkFile = new File(FIGHT_HISTORY_DATA_DIR, JsonGzChunkType.IMPORTED_FROM_JSON.fnamePrefix + fileIdx + ".json.gz");
							fileIdx++;

							try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(newGzChunkFile.toPath())))
							{
								try (OutputStreamWriter writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8))
								{
									GSON.toJson(chunkedFights, writer);
								}
							}
						}
						catch (Exception e)
						{
							log.warn("updateFrom1_8_1to1_8_2: Error ignored while writing updated fight history data gz chunk: {}", e.getMessage());
							success = false;
						}

						chunkedFights.clear();
					}
				}
			}
		}
		catch (Exception e)
		{
			log.warn("updateFrom1_8_1to1_8_2: Error while reading or writing fight history data: {}", e.getMessage());
			success = false;
		}

		log.info("PvpPerformanceTracker - completed update process - updateFrom1_8_1to1_8_2() - success={}", success);
	}
}
