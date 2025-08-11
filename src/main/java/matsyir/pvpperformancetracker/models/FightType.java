/*
 * Copyright (c) 2022, Matsyir <https://github.com/matsyir>
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

package matsyir.pvpperformancetracker.models;

import org.apache.commons.lang3.ArrayUtils;

public enum FightType
{
	ARENA_MAXMED(new CombatLevels(91, 118, 86, 112, 99, 99)),
	ARENA_ZERK(new CombatLevels(74, 118, 56, 112, 99, 99)),
	ARENA_1DEF(new CombatLevels(74, 118, 6, 112, 99, 99)),
	LMS_MAXMED(new CombatLevels(118, 118, 75, 112, 99, 99)),
	LMS_ZERK(new CombatLevels(91, 118, 50, 112, 99, 99)),
	LMS_1DEF(new CombatLevels(91, 118, 1, 112, 99, 99)),
	NORMAL(CombatLevels.getConfigLevels());

	private static FightType[] LMS_TYPES = { LMS_MAXMED, LMS_ZERK, LMS_1DEF };
	private static FightType[] ARENA_TYPES = { ARENA_MAXMED, ARENA_ZERK, ARENA_1DEF };

	private CombatLevels combatLevelsForType;
	FightType(CombatLevels combatLevelsForType)
	{
		this.combatLevelsForType = combatLevelsForType;
	}

	public CombatLevels getCombatLevelsForType()
	{
		if (this == NORMAL)
		{
			return CombatLevels.getConfigLevels();
		}

		return combatLevelsForType;
	}

	public boolean isLmsFight()
	{
		return ArrayUtils.contains(LMS_TYPES, this);
	}
	public boolean isArenaFight()
	{
		return ArrayUtils.contains(ARENA_TYPES, this);
	}
}
