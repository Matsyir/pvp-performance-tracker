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

import com.google.gson.Gson;
import java.io.File;
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
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.client.RuneLite;

@Slf4j
public class FightPerformanceSerializer
{
	public static final String DATA_FOLDER = PvpPerformanceTrackerPlugin.DATA_FOLDER;
	public static final String FIGHT_HISTORY_DATA_FOLDER = "FightHistoryData"; // subfolder of DATA_FOLDER

	// fname prefix used for fights imported from 1.8.1 -> 1.8.2 (going from 1 big .json to .json.gz chunks)
	// will get number added to it, e.g FightHistoryData-c1.json.gz
	public static final String FIGHT_HISTORY_DATA_FNAME_PREFIX_GZ_IMPORT_CHUNK = "FightHistoryData-c";

	// prefix for newly generated .json.gz files
	// will get epoch/timestamp long added to it, e.g Fights_1783190645201.json.gz
	// via sessionStartTime, so only 1 file per plugin launch (1 file per client session with normal use)
	public static final String FIGHT_HISTORY_DATA_FNAME_PREFIX_GZ_NEWCHUNK = "Fights_";

	public static final String JSON_GZ_CHUNK_ENDSWITH = ".json.gz";

	// old/original fightHistoryData .json file, to be imported 1.8.1 -> 1.8.2 (1 big .json to .json.gz chunks)
	public static final String _OLD_FIGHT_HISTORY_DATA_FNAME_JSON = "FightHistoryData.json";

	public static final File BASE_DATA_DIR;
	public static final File FIGHT_HISTORY_DATA_DIR;

	private static final long sessionStartTime = Instant.now().toEpochMilli();

	static
	{
		BASE_DATA_DIR = PvpPerformanceTrackerPlugin.BASE_DATA_DIR;

		FIGHT_HISTORY_DATA_DIR = new File(RuneLite.RUNELITE_DIR, DATA_FOLDER + "/" + FIGHT_HISTORY_DATA_FOLDER);
		FIGHT_HISTORY_DATA_DIR.mkdirs();
	}

	public enum JsonGzChunkType
	{
		IMPORTED_FROM_JSON(FIGHT_HISTORY_DATA_FNAME_PREFIX_GZ_IMPORT_CHUNK),
		NEW_CHUNK(FIGHT_HISTORY_DATA_FNAME_PREFIX_GZ_NEWCHUNK);

		String startsWith;
		JsonGzChunkType(String startsWith)
		{
			this.startsWith = startsWith;
		}

		public static boolean isFileValidChunk(File file)
		{
			// ignore any chunks greater than 10mb, they really shouldn't be going > 1-2MB with normal use.
			// also ignore any that don't end with .json.gz
			if (file.length() > (10 * 1024 * 1024) || !file.getName().endsWith(JSON_GZ_CHUNK_ENDSWITH))
			{
				return false;
			}

			JsonGzChunkType chunkType = null;
			for (JsonGzChunkType cType : JsonGzChunkType.values())
			{
				if (file.getName().startsWith(cType.startsWith))
				{
					chunkType = cType;
					break;
				}
			}

			return chunkType != null;
		}
	}
	private static List<File> listAllDataChunks()
	{
		return Arrays.stream(Objects.requireNonNull(
				FIGHT_HISTORY_DATA_DIR.listFiles(JsonGzChunkType::isFileValidChunk))
			).sorted(Comparator.comparingLong(File::lastModified).reversed())
			.collect(Collectors.toList());
	}

	// import complete fight history data from the saved .json.gz data files
	// this function only handles the direct file processing and .json.gz deserialization.
	// more specific FightPerformance processing, and the addition to fightHistory is done in importFights()
	public static void importFightHistoryData(Gson gson, Consumer<ArrayList<FightPerformance>> onReadCallback)
	{
		// catch and ignore any errors we may have forgotten to handle - the import will fail but at least the plugin
		// will continue to function. This should only happen if their fight history data is corrupted/outdated.
		try
		{
			FIGHT_HISTORY_DATA_DIR.mkdirs();


			List<File> fightDataChunkFiles = listAllDataChunks();
			// if there's no data files, then skip reading/importing data.
			if (fightDataChunkFiles.isEmpty())
			{
				log.info("Skipping importFightHistoryData due to no files");
				return;
			}

			ArrayList<FightPerformance> savedFights = new ArrayList<>();

			for (File fightDataChunk : fightDataChunkFiles)
			{
				try (
					GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(fightDataChunk.toPath()));
					InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8)
				)
				{
					List<FightPerformance> fightsFromChunk = Arrays.asList(gson.fromJson(reader, FightPerformance[].class));
					for (FightPerformance fightPerformance : fightsFromChunk)
					{
						fightPerformance.setLoadedFromFname(fightDataChunk.getName());
					}
					fightsFromChunk.removeIf((f) -> Objects.isNull(f) || Objects.isNull(f.competitor) || Objects.isNull(f.opponent));
					savedFights.addAll(fightsFromChunk);

					// skip reading remaining older chunks if we've already hit the fightHistoryLimit
					if (savedFights.size() >= CONFIG.fightHistoryLimit())
					{
						break;
					}
				}
				catch (Exception e)
				{
					log.warn("importFightHistoryData(): Error while deserializing fight history data chunk (path=" + fightDataChunk.getAbsolutePath() + "), errorMsg=" + e.getMessage());
				}
			}

			if (!savedFights.isEmpty() && onReadCallback != null)
			{
				onReadCallback.accept(savedFights);
			}
		}
		catch (Exception e)
		{
			log.warn("importFightHistoryData() Unexpected error while listing data files or deserializing fight history data: " + e.getMessage());
			// Display no popup for this error since it could happen on client load and that has odd behavior.
		}
	}

	// Save the currently loaded sessionFightHistory to local gzipped JSON.
	public static void saveFightHistoryData(Gson gson)
	{
		// skip any data writing if no fights were added in this session.
		if (PLUGIN.sessionFightHistory.isEmpty())
		{
			return;
		}

		try
		{
			FIGHT_HISTORY_DATA_DIR.mkdirs();
			File fightHistoryData = new File(FIGHT_HISTORY_DATA_DIR, FIGHT_HISTORY_DATA_FNAME_PREFIX_GZ_NEWCHUNK + sessionStartTime + ".json.gz");

			try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(fightHistoryData.toPath())))
			{
				try (OutputStreamWriter writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8))
				{
					gson.toJson(PLUGIN.sessionFightHistory, writer);
				}
			}
		}
		catch (Exception e)
		{
			log.warn("saveFightHistoryData(): Error ignored while writing fight history data: {}", e.getMessage());
		}
	}

	public static void removeAllFights()
	{
		try
		{
			listAllDataChunks().forEach(file -> file.delete());
		}
		catch (Exception e)
		{
		}
	}

	// in importFightHistory, we set the loadedFromFname field to save which chunk the fight was loaded from.
	// Then find it by comparing a few fields, and re-write the chunk without it.
	public static void removeFight(FightPerformance fight, Gson gson)
	{
		if (Strings.isNullOrEmpty(fight.getLoadedFromFname()))
		{
			return;
		}

		try
		{
			File loadedFromChunk = new File(FIGHT_HISTORY_DATA_DIR, fight.getLoadedFromFname());

			// if we can't find the file, then skip removing
			if (!loadedFromChunk.exists())
			{
				return;
			}

			ArrayList<FightPerformance> fightsFromChunk = null;
			try (
				GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(loadedFromChunk.toPath()));
				InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8)
			)
			{
				fightsFromChunk = new ArrayList<>(Arrays.asList(gson.fromJson(reader, FightPerformance[].class)));
			}
			catch (Exception e)
			{
				log.warn("removeFight(): Error while deserializing fight history data chunk (path=" + loadedFromChunk.getAbsolutePath() + "), errorMsg=" + e.getMessage());
			}
			if (fightsFromChunk == null || fightsFromChunk.isEmpty())
			{
				return;
			}

			int initialFightCount = fightsFromChunk.size();;

			fightsFromChunk.removeIf((f) -> f.lastFightTime == fight.lastFightTime
				&& f.opponent.getName().equals(fight.opponent.getName())
				&& f.getFightIdAnchorTime() == fight.getFightIdAnchorTime()
				&& f.getInitialTime() == fight.getInitialTime()
				&& f.getInitialFightTick() == fight.getInitialFightTick());

			if (fightsFromChunk.size() != (initialFightCount - 1))
			{
				log.debug("removeFight(): Was going to delete more than 1 fight when attempting to only remove 1?");
				return;
			}


			try
			{
				String newChunkName = fight.getLoadedFromFname() + ".tmp";
				File updatedChunkFile = new File(FIGHT_HISTORY_DATA_DIR, newChunkName);

				try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(updatedChunkFile.toPath())))
				{
					try (OutputStreamWriter writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8))
					{
						gson.toJson(fightsFromChunk, writer);
						writer.close();
						gzip.close();
						if (updatedChunkFile.length() > 0)
						{
							try
							{
								Files.move(updatedChunkFile.toPath(), loadedFromChunk.toPath(), StandardCopyOption.ATOMIC_MOVE);
							}
							catch(AtomicMoveNotSupportedException e)
							{
								Files.move(updatedChunkFile.toPath(), loadedFromChunk.toPath(), StandardCopyOption.REPLACE_EXISTING);
							}
							log.debug("removeFight(): Successfully re-wrote chunk - " + loadedFromChunk.getName());
						}
					}
				}

			}
			catch (Exception e)
			{
				log.warn("removeFight(): Error ignored while writing updated fight chunk: {}", e.getMessage());
			}

		}
		catch (Exception e)
		{
			log.warn("removeFight(): Unexpected error while listing data files or deserializing fight history data: " + e.getMessage());
			throw e;
		}
	}

	// Update functions

	// convert saved fight history to chunked .gz files instead of 1 massive JSON
	public static void updateFrom1_8_1to1_8_2(Gson gson)
	{
		log.info("PvpPerformanceTracker - starting update process - updateFrom1_8_1to1_8_2()");
		boolean success = true;
		final int MAX_FIGHTS_PER_UPDATED_GZ_CHUNK = 100;
		try
		{
			BASE_DATA_DIR.mkdirs();
			File originalJsonData = new File(BASE_DATA_DIR, _OLD_FIGHT_HISTORY_DATA_FNAME_JSON);
			FIGHT_HISTORY_DATA_DIR.mkdirs();

			// if the fight history data file doesn't exist, don't need to do anything.
			if (!originalJsonData.exists())
			{
				return;
			}

			// read the saved fights from the JSON file
			List<FightPerformance> savedFights = Arrays.asList(
				gson.fromJson(new FileReader(originalJsonData), FightPerformance[].class));
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
							File newGzChunkFile = new File(FIGHT_HISTORY_DATA_DIR, FIGHT_HISTORY_DATA_FNAME_PREFIX_GZ_IMPORT_CHUNK + fileIdx + ".json.gz");
							fileIdx++;

							try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(newGzChunkFile.toPath())))
							{
								try (OutputStreamWriter writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8))
								{
									gson.toJson(chunkedFights, writer);
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
			log.warn("updateFrom1_8_1to1_8_2: Error while reading or writing fight history data: " + e.getMessage());
			success = false;
			// Display no modal for this error since it could happen on client load and that has odd behavior.
			return;
		}

		log.info("PvpPerformanceTracker - completed update process - updateFrom1_8_1to1_8_2() - success=" + success);
	}
}
