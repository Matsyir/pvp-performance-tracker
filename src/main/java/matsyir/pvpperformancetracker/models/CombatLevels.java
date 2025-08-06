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
package matsyir.pvpperformancetracker.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreManager;

// Basic class that will be used to save current combat levels (including boosts/drains)
@Getter
public class CombatLevels
{
	public static CombatLevels getConfigLevels()
	{
		return new CombatLevels(CONFIG.attackLevel(),
				CONFIG.strengthLevel(),
				CONFIG.defenceLevel(),
				CONFIG.rangedLevel(),
				CONFIG.magicLevel(),
				99);
	}

	@Expose
	@SerializedName("a")
	public int atk;
	@Expose
	@SerializedName("s")
	public int str;
	@Expose
	@SerializedName("d")
	public int def;
	@Expose
	@SerializedName("r")
	public int range;
	@Expose
	@SerializedName("m")
	public int mage;
	@Expose
	@SerializedName("h")
	public int hp; // not currently used but potential dh support in future?

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

	public CombatLevels(String username, HiscoreManager hiscoreManager) {
		try {
				HiscoreResult lookupResults = hiscoreManager.lookupAsync(username.toLowerCase(), HiscoreEndpoint.NORMAL);
				if(lookupResults == null) {
					//if highscores fails just pull from config
					this.atk = CONFIG.attackLevel();
					this.str = CONFIG.strengthLevel();
					this.def = CONFIG.defenceLevel();
					this.range = CONFIG.rangedLevel();
					this.mage = CONFIG.magicLevel();
					this.hp = 99;
				} else {
					int usernamesAttack = lookupResults.getSkill(HiscoreSkill.ATTACK).getLevel();
					int usernamesStrength = lookupResults.getSkill(HiscoreSkill.STRENGTH).getLevel();
					int usernamesDefence = lookupResults.getSkill(HiscoreSkill.DEFENCE).getLevel();
					int usernamesRange = lookupResults.getSkill(HiscoreSkill.RANGED).getLevel();
					int usernamesMagic = lookupResults.getSkill(HiscoreSkill.MAGIC).getLevel();
					int usernamesHitpoints = lookupResults.getSkill(HiscoreSkill.HITPOINTS).getLevel();

					//assume boosted
					this.atk = (int) Math.floor((usernamesAttack + 5) * 1.15);
					this.str = (int) Math.floor((usernamesStrength + 5) * 1.15);
					this.def = (int) Math.floor((usernamesDefence + 2) * 1.20);
					this.range = (int) Math.floor((usernamesRange + 4) * 1.10);
					this.mage = usernamesMagic; //nobody uses boosted magic for extended periods of time
					this.hp = usernamesHitpoints;
				}
		} catch (Exception exception) {
			//if highscores fails just pull from config
			this.atk = CONFIG.attackLevel();
			this.str = CONFIG.strengthLevel();
			this.def = CONFIG.defenceLevel();
			this.range = CONFIG.rangedLevel();
			this.mage = CONFIG.magicLevel();
			this.hp = 99;
		}
	}

	public int getSkill(Skill skill)
	{
		switch(skill)
		{
			case ATTACK:    return atk;
			case STRENGTH:  return str;
			case DEFENCE:   return def;
			case RANGED:    return range;
			case MAGIC:     return mage;
			case HITPOINTS: return hp;
			default:        return 0;
		}
	}
}
