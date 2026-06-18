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
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerConfig;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;

/**
 * Handles uploading fight data to PvP-Hub.com.
 * Uploads are asynchronous and failures are silently logged.
 */
@Slf4j
public class PvpHubUploader
{
	private static final String UPLOAD_URL = "https://osrs.pvp-hub.com/api/fights";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	static final int RANDOM_DELAY_MIN_SECONDS = 6 * 60;
	static final int RANDOM_DELAY_MAX_SECONDS = 20 * 60;

	/**
	 * Asynchronously uploads a completed fight to PvP-Hub.com.
	 * Failures are logged but do not interrupt the user.
	 *
	 * @param fight      the completed fight performance data
	 * @param gson       the Gson instance configured for serialization
	 * @param httpClient the OkHttpClient to use for the request
	 */
	public static void uploadFight(FightPerformance fight, Gson gson, OkHttpClient httpClient)
	{
		uploadFight(fight, gson, httpClient, null);
	}

	/**
	 * Asynchronously uploads a completed fight to PvP-Hub.com with an optional local-player hidden name.
	 *
	 * @param fight      the completed fight performance data
	 * @param gson       the Gson instance configured for serialization
	 * @param httpClient the OkHttpClient to use for the request
	 * @param hiddenName when present, replaces the local competitor RSN in the upload payload
	 */
	public static void uploadFight(FightPerformance fight, Gson gson, OkHttpClient httpClient, String hiddenName)
	{
		if (fight == null || fight.getFightId() == null || fight.getFightId().isEmpty())
		{
			log.debug("Skipping PvP-Hub upload: fight or fightId is null/empty");
			return;
		}

		String json;
		try
		{
			int publicDelaySeconds = resolvePublicDelaySeconds(CONFIG.pvpHubVisibilityDelay());
			json = serializeFightUpload(fight, gson, publicDelaySeconds, hiddenName);
		}
		catch (Exception e)
		{
			log.warn("Failed to serialize fight for PvP-Hub upload: {}", e.getMessage());
			return;
		}

		RequestBody body = RequestBody.create(JSON, json);
		Request request = new Request.Builder()
			.url(UPLOAD_URL)
			.post(body)
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("PvP-Hub upload failed (network error): {}", e.getMessage());
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try
				{
					if (response.isSuccessful())
					{
						log.info("PvP-Hub upload successful for fight {} (HTTP {})", fight.getFightId(), response.code());
					}
					else
					{
						log.warn("PvP-Hub upload returned HTTP {}: {}", response.code(),
							response.body() != null ? response.body().string() : "no body");
					}
				}
				catch (IOException e)
				{
					log.warn("Error reading PvP-Hub upload response: {}", e.getMessage());
				}
				finally
				{
					response.close();
				}
			}
		});
	}

	static int resolvePublicDelaySeconds(PvpPerformanceTrackerConfig.PvpHubVisibilityDelay delayMode)
	{
		PvpPerformanceTrackerConfig.PvpHubVisibilityDelay safeDelayMode =
			delayMode != null ? delayMode : PvpPerformanceTrackerConfig.PvpHubVisibilityDelay.RANDOM_6_20;
		if (safeDelayMode == PvpPerformanceTrackerConfig.PvpHubVisibilityDelay.RANDOM_6_20)
		{
			return ThreadLocalRandom.current().nextInt(RANDOM_DELAY_MIN_SECONDS, RANDOM_DELAY_MAX_SECONDS + 1);
		}

		return safeDelayMode.getDelaySeconds();
	}

	static String serializeFightUpload(FightPerformance fight, Gson gson, int publicDelaySeconds)
	{
		return serializeFightUpload(fight, gson, publicDelaySeconds, null);
	}

	static String serializeFightUpload(FightPerformance fight, Gson gson, int publicDelaySeconds, String hiddenName)
	{
		JsonObject payload = gson.toJsonTree(new FightUploadPayload(fight, publicDelaySeconds)).getAsJsonObject();
		if (hiddenName != null && !hiddenName.trim().isEmpty())
		{
			payload.getAsJsonObject("c").addProperty("n", hiddenName);
		}

		return gson.toJson(payload);
	}

	static final class FightUploadPayload
	{
		@Expose
		@SerializedName("c")
		final Fighter c;
		@Expose
		@SerializedName("o")
		final Fighter o;
		@Expose
		@SerializedName("t")
		final long t;
		@Expose
		@SerializedName("fightID")
		final String fightID;
		@Expose
		@SerializedName("l")
		final matsyir.pvpperformancetracker.models.FightType l;
		@Expose
		@SerializedName("w")
		final int w;
		@Expose
		final int publicDelaySeconds;

		FightUploadPayload(FightPerformance fight, int publicDelaySeconds)
		{
			this.c = fight.competitor;
			this.o = fight.opponent;
			this.t = fight.lastFightTime;
			this.fightID = fight.getFightId();
			this.l = fight.getFightType();
			this.w = fight.getWorld();
			this.publicDelaySeconds = publicDelaySeconds;
		}
	}
}
