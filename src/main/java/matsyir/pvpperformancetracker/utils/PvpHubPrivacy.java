/*
 * Copyright (c) 2026, LogicalSolutions <https://github.com/LogicalSolutions>
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
package matsyir.pvpperformancetracker.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class PvpHubPrivacy
{
	private static final String HIDDEN_NAME_PREFIX = "Hidden-";
	private static final char[] HIDDEN_NAME_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	private static final int HIDDEN_NAME_LENGTH = 5;

	private PvpHubPrivacy()
	{
	}

	public static String createAnonymousId()
	{
		return UUID.randomUUID().toString();
	}

	public static String hiddenNameFor(String anonymousId)
	{
		if (anonymousId == null || anonymousId.trim().isEmpty())
		{
			throw new IllegalArgumentException("anonymousId must not be blank");
		}

		byte[] hash;
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			hash = digest.digest(anonymousId.trim().getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException("SHA-256 not available", e);
		}

		char[] suffix = new char[HIDDEN_NAME_LENGTH];
		for (int i = 0; i < HIDDEN_NAME_LENGTH; i++)
		{
			suffix[i] = HIDDEN_NAME_CHARS[(hash[i] & 0xFF) % HIDDEN_NAME_CHARS.length];
		}

		return HIDDEN_NAME_PREFIX + new String(suffix);
	}
}
