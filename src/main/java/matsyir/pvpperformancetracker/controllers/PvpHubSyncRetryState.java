/*
 * Copyright (c) 2026, LogicalSolutions <https://github.com/LogicalSoIutions>
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PvpHubSyncRetryState
{
	private final int maxAttempts;
	private final long retryDelayMillis;
	private final Map<String, PendingSync> pending = new ConcurrentHashMap<>();

	public PvpHubSyncRetryState(int maxAttempts, long retryDelayMillis)
	{
		this.maxAttempts = maxAttempts;
		this.retryDelayMillis = retryDelayMillis;
	}

	public void enqueue(String fightId, long nowMillis)
	{
		String normalizedFightId = normalizeFightId(fightId);
		if (normalizedFightId == null)
		{
			return;
		}

		pending.computeIfAbsent(normalizedFightId, ignored -> new PendingSync(nowMillis));
	}

	public boolean isEmpty()
	{
		return pending.isEmpty();
	}

	public List<String> dueFightIds(long nowMillis)
	{
		List<String> dueFightIds = new ArrayList<>();
		for (Map.Entry<String, PendingSync> entry : pending.entrySet())
		{
			if (entry.getValue().nextAttemptAtMillis <= nowMillis)
			{
				dueFightIds.add(entry.getKey());
			}
		}
		return dueFightIds;
	}

	public boolean markAttemptStarted(String fightId, long nowMillis)
	{
		String normalizedFightId = normalizeFightId(fightId);
		if (normalizedFightId == null)
		{
			return false;
		}

		PendingSync sync = pending.get(normalizedFightId);
		if (sync == null)
		{
			return false;
		}

		synchronized (sync)
		{
			if (sync.attempts >= maxAttempts || sync.nextAttemptAtMillis > nowMillis)
			{
				if (sync.attempts >= maxAttempts)
				{
					pending.remove(normalizedFightId);
				}
				return false;
			}

			sync.attempts++;
			sync.nextAttemptAtMillis = nowMillis + retryDelayMillis;
			return true;
		}
	}

	public void markUnavailable(String fightId)
	{
		String normalizedFightId = normalizeFightId(fightId);
		if (normalizedFightId == null)
		{
			return;
		}

		PendingSync sync = pending.get(normalizedFightId);
		if (sync == null)
		{
			return;
		}

		synchronized (sync)
		{
			if (sync.attempts >= maxAttempts)
			{
				pending.remove(normalizedFightId);
			}
		}
	}

	public void remove(String fightId)
	{
		String normalizedFightId = normalizeFightId(fightId);
		if (normalizedFightId != null)
		{
			pending.remove(normalizedFightId);
		}
	}

	public void clear()
	{
		pending.clear();
	}

	private static String normalizeFightId(String fightId)
	{
		if (fightId == null || fightId.trim().isEmpty())
		{
			return null;
		}
		return fightId.trim();
	}

	private static final class PendingSync
	{
		private int attempts;
		private long nextAttemptAtMillis;

		private PendingSync(long nextAttemptAtMillis)
		{
			this.nextAttemptAtMillis = nextAttemptAtMillis;
		}
	}
}
