/*
 * Copyright (c) 2026, LogicalSolutions <https://github.com/LogicalSoIutions>
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

import matsyir.pvpperformancetracker.models.AnimationData.AttackStyle;

public enum BrewState
{
	POTTED("Potted"),
	NEUTRAL("Neutral"),
	BREWED_DOWN("Brewed down");

	private final String displayName;

	BrewState(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public static BrewState from(AttackStyle attackStyle, CombatLevels currentLevels, CombatLevels baseLevels)
	{
		if (attackStyle == null || currentLevels == null || baseLevels == null)
		{
			return NEUTRAL;
		}

		int currentLevel;
		int baseLevel;
		if (attackStyle.isMelee())
		{
			currentLevel = currentLevels.str;
			baseLevel = baseLevels.str;
		}
		else if (attackStyle == AttackStyle.RANGED)
		{
			currentLevel = currentLevels.range;
			baseLevel = baseLevels.range;
		}
		else
		{
			currentLevel = currentLevels.mage;
			baseLevel = baseLevels.mage;
		}

		if (currentLevel > baseLevel)
		{
			return POTTED;
		}
		if (currentLevel < baseLevel)
		{
			return BREWED_DOWN;
		}
		return NEUTRAL;
	}
}
