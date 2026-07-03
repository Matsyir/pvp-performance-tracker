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
package matsyir.pvpperformancetracker.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates a deterministic 10-character fight ID (A-Z, 0-9) from shared game state
 * that both clients in a fight can independently compute without communication.
 *
 * The ID is derived from:
 * - Both player names (sorted alphabetically so order doesn't matter)
 * - The world number
 * - A shared fight timestamp anchor, bucketed to absorb small clock differences
 */
public class FightIdGenerator
{
	private static final char[] BASE36_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	private static final int ID_LENGTH = 10;

	/**
	 * Generate a deterministic 10-character fight ID from shared game state.
	 * Both clients will produce the same ID because all inputs are derived from
	 * shared game state and NTP-synchronized system clocks.
	 *
	 * The epoch time is bucketed into a 10-second window to absorb small
	 * minor clock differences between clients (typically < 100ms with NTP).
	 *
	 * @param name1      one player's RSN
	 * @param name2      the other player's RSN
	 * @param world      the OSRS world number
	 * @param epochMillis the current system time in milliseconds (Instant.now().toEpochMilli())
	 * @return a 10-character uppercase alphanumeric string matching [A-Z0-9]{10}
	 */
	public static String generateFightId(String name1, String name2, int world, long epochMillis)
	{
		// Normalize name characters: replace underscores and non-breaking spaces with standard spaces,
		// trim, and uppercase, then sort alphabetically so both clients produce the same order.
		String n1 = name1.replace("\u00a0", " ").replace("_", " ").trim().toUpperCase();
		String n2 = name2.replace("\u00a0", " ").replace("_", " ").trim().toUpperCase();
		if (n1.compareTo(n2) > 0)
		{
			String temp = n1;
			n1 = n2;
			n2 = temp;
		}

		// Bucket to 10-second granularity so both clients land in the same bucket
		// even with minor clock differences (NTP typically syncs within <100ms).
		long roundedEpoch = epochMillis / 10000;
		String seed = n1 + ":" + n2 + ":" + world + ":" + roundedEpoch;

		byte[] hash;
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException e)
		{
			// SHA-256 is guaranteed to be available on all Java platforms
			throw new RuntimeException("SHA-256 not available", e);
		}

		// Convert first 8 bytes of the hash to a long (unsigned), then base-36 encode
		// to produce a 10-character ID from the A-Z0-9 character set.
		// We use the absolute value and mask to ensure positive values.
		char[] result = new char[ID_LENGTH];
		// Use first 8 bytes as a long seed for base-36 conversion
		long value = 0;
		for (int i = 0; i < 8; i++)
		{
			value = (value << 8) | (hash[i] & 0xFF);
		}
		// Ensure positive
		value = value & Long.MAX_VALUE;

		for (int i = ID_LENGTH - 1; i >= 0; i--)
		{
			result[i] = BASE36_CHARS[(int) (value % 36)];
			value /= 36;
		}

		return new String(result);
	}
}
