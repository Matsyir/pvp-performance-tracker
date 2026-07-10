/*
 * Copyright (c) 2026, Matsyir <https://github.com/Matsyir>
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

import lombok.Getter;
import matsyir.pvpperformancetracker.utils.PvpUtils;

@Getter
public class BrewState
{
	// brewedLevels: amount of levels you brewed down from max potted. 0 = max potted, display number of levels below max potted.
	// For example, attacking with 116 str would be -2 since 118 is max potted,
	// and 111 range would be -1 since 112 is max potted (assuming 99str/range in this example)
	// use this logic to determine what counts as potted vs neutral vs brewed

	// any brewedLevels below this threshold will count as neutral or brewed down instead of potted
	public static final int BREWED_LEVELS_NEUTRAL_THRESHOLD = -3;
	private final int brewedLevels;
	private final int levelChange;
	private final int baseLevel;
	private final int maxPottedLevel;
	private final BrewStateCategory category;

	private BrewState()
	{
		this.brewedLevels = 0;
		this.levelChange = 0;
		this.baseLevel = 0;
		this.maxPottedLevel = 0;
		this.category = BrewStateCategory.UNKNOWN;
	}

	private BrewState(int currentLevel, int baseLevel, int maxPottedLevel)
	{
		this.brewedLevels = currentLevel - maxPottedLevel;
		this.levelChange = currentLevel - baseLevel;
		this.baseLevel = baseLevel;
		this.maxPottedLevel = maxPottedLevel;

		if (this.brewedLevels >= BREWED_LEVELS_NEUTRAL_THRESHOLD)
		{
			this.category = BrewStateCategory.POTTED;
		}
		else if (currentLevel >= baseLevel)
		{
			this.category = BrewStateCategory.NEUTRAL;
		}
		else
		{
			this.category = BrewStateCategory.BREWED_DOWN;
		}
	}

	public String getFightLogText()
	{
		if (this.category == BrewStateCategory.UNKNOWN)
		{
			return BrewStateCategory.UNKNOWN.getDisplayName();
		}

		if (this.category == BrewStateCategory.POTTED || this.category == BrewStateCategory.BREWED_DOWN)
		{
			return "<html><strong>" + PvpUtils.prependPlusIfPositive(levelChange) +
				"</strong>&nbsp;<i>(" + (baseLevel + levelChange) + "/" + maxPottedLevel + ")";
		}
		else
		{
			return "<html>" + PvpUtils.prependPlusIfPositive(levelChange) +
				"&nbsp;<i>(" + (baseLevel + levelChange) + "/" + maxPottedLevel + ")";
		}
	}

	public static BrewState from(AnimationData.AttackStyle attackStyle, CombatLevels currentLevels, CombatLevels baseLevels)
	{
		if (attackStyle == null || currentLevels == null || baseLevels == null)
		{
			return new BrewState();
		}

		int currentLevel;
		int baseLevel;
		int maxPottedLevel;
		if (attackStyle.isMelee())
		{
			currentLevel = currentLevels.str;
			baseLevel = baseLevels.str;
			maxPottedLevel = baseLevel + (5 + (int) (0.15 * baseLevel)); // super combat boost, should = 118 on 99
		}
		else if (attackStyle == AnimationData.AttackStyle.RANGED)
		{
			currentLevel = currentLevels.range;
			baseLevel = baseLevels.range;
			maxPottedLevel = baseLevel + (4 + (int) (0.10 * baseLevel)); // ranging potion, should = 112 on 99
		}
		else
		{
			currentLevel = currentLevels.mage;
			baseLevel = baseLevels.mage;
			maxPottedLevel = baseLevel; // don't expect people to always be using heart, treat 99 mage as 'potted'
		}

		return new BrewState(currentLevel, baseLevel, maxPottedLevel);
	}
}
