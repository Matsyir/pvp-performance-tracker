/*
 * Copyright (c) 2026, LogicalSolutions <https://github.com/LogicalSolutions>
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
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
public final class PvpHubFightSync
{
	public static final String SYNCED_FIGHTS_DIR_NAME = "PvpHubSyncedFights";
	private static final String FIGHT_DETAIL_URL = "https://osrs.pvp-hub.com/api/fights/";

	private PvpHubFightSync()
	{
	}

	public interface SyncCallback
	{
		void onSynced(FightPerformance syncedFight, String responseJson);

		void onNotSynced(String reason);
	}

	public static void fetchSyncedFight(FightPerformance fight, Gson gson, OkHttpClient httpClient, SyncCallback callback)
	{
		if (fight == null || fight.getFightId() == null || fight.getFightId().trim().isEmpty())
		{
			callback.onNotSynced("missing fight ID");
			return;
		}

		Request request = new Request.Builder()
			.url(FIGHT_DETAIL_URL + fight.getFightId().trim())
			.get()
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				callback.onNotSynced("network error: " + e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try
				{
					if (!response.isSuccessful())
					{
						callback.onNotSynced("HTTP " + response.code());
						return;
					}

					String body = response.body() != null ? response.body().string() : "";
					FightDetail detail = parseSyncedFightDetail(body, gson, fight.getFightId());
					if (detail == null || detail.fullData == null || detail.secondaryData == null)
					{
						callback.onNotSynced("secondary POV is not available yet");
						return;
					}

					callback.onSynced(detail.fullData, body);
				}
				catch (Exception e)
				{
					callback.onNotSynced("parse error: " + e.getMessage());
				}
				finally
				{
					response.close();
				}
			}
		});
	}

	public static FightPerformance loadSyncedFight(File syncedFightsDir, Gson gson, FightPerformance localFight)
	{
		if (localFight == null || localFight.getFightId() == null || localFight.getFightId().trim().isEmpty())
		{
			return null;
		}

		File syncedFightFile = getSyncedFightFile(syncedFightsDir, localFight.getFightId());
		if (!syncedFightFile.exists())
		{
			return null;
		}

		try (FileReader reader = new FileReader(syncedFightFile))
		{
			FightDetail detail = gson.fromJson(reader, FightDetail.class);
			if (detail == null || detail.fullData == null || detail.secondaryData == null ||
				!localFight.getFightId().equals(detail.fightId))
			{
				return null;
			}

			return detail.fullData;
		}
		catch (Exception e)
		{
			log.warn("Error loading synced PvP-Hub fight {}: {}", localFight.getFightId(), e.getMessage());
			return null;
		}
	}

	public static void saveSyncedFight(File syncedFightsDir, String fightId, String responseJson)
	{
		if (syncedFightsDir == null || fightId == null || fightId.trim().isEmpty() || responseJson == null || responseJson.trim().isEmpty())
		{
			return;
		}

		if (!syncedFightsDir.exists() && !syncedFightsDir.mkdirs())
		{
			log.warn("Could not create PvP-Hub synced fights directory: {}", syncedFightsDir);
			return;
		}

		try (Writer writer = new FileWriter(getSyncedFightFile(syncedFightsDir, fightId)))
		{
			writer.write(responseJson);
			log.info("Saved synced PvP-Hub fight {} to {}", fightId, getSyncedFightFile(syncedFightsDir, fightId));
		}
		catch (Exception e)
		{
			log.warn("Error saving synced PvP-Hub fight {}: {}", fightId, e.getMessage());
		}
	}

	static FightDetail parseSyncedFightDetail(String json, Gson gson, String expectedFightId)
	{
		FightDetail detail = gson.fromJson(json, FightDetail.class);
		if (detail == null)
		{
			return null;
		}

		String responseFightId = detail.fightId != null ? detail.fightId :
			detail.fullData != null ? detail.fullData.getFightId() : null;
		if (expectedFightId != null && !expectedFightId.equals(responseFightId))
		{
			return null;
		}

		return detail;
	}

	private static File getSyncedFightFile(File syncedFightsDir, String fightId)
	{
		return new File(syncedFightsDir, fightId.trim() + ".json");
	}

	static final class FightDetail
	{
		@Expose
		@SerializedName("fight_id")
		String fightId;
		@Expose
		@SerializedName("full_data")
		FightPerformance fullData;
		@Expose
		@SerializedName("secondary_data")
		FightPerformance secondaryData;
	}
}
