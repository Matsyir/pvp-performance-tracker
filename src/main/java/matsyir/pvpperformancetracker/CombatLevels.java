/*
 * Copyright (c) 2021, Matsyir <https://github.com/matsyir>
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
package matsyir.pvpperformancetracker;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Skill;

// Basic class that will be used to save current combat levels (including boosts/drains)
@Getter
public class CombatLevels
{
	int atk;
	int str;
	int def;
	int range;
	int mage;
	int hp; // not currently used but potential dh support in future?

	public CombatLevels(int atk, int str, int def, int range, int mage, int hp)
	{
		this.atk = atk;
		this.str = str;
		this.def = def;
		this.range = range;
		this.mage = mage;
		this.hp = hp;
	}

	public CombatLevels(Client client)
	{
		this.atk = client.getBoostedSkillLevel(Skill.ATTACK);
		this.str = client.getBoostedSkillLevel(Skill.STRENGTH);
		this.def = client.getBoostedSkillLevel(Skill.DEFENCE);
		this.range = client.getBoostedSkillLevel(Skill.RANGED);
		this.mage = client.getBoostedSkillLevel(Skill.MAGIC);
		this.hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
	}
}
