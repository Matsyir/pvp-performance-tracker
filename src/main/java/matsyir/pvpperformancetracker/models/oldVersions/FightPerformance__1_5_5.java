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
package matsyir.pvpperformancetracker.models.oldVersions;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import matsyir.pvpperformancetracker.controllers.Fighter;

// Old stripped down version of FightPerformance, so they can be properly deserialized
// and upgraded to the newer version.
// When going from 1.5.5 -> 1.5.6, the boolean isLmsFight is updated to the FightType enum.
// the core upgrade logic is a simple constructor in FightPerformance
@Getter
public class FightPerformance__1_5_5
{
	@Expose
	@SerializedName("c") // use 1 letter serialized variable names for more compact storage
	public Fighter competitor;
	@Expose
	@SerializedName("o")
	public Fighter opponent;
	@Expose
	@SerializedName("t")
	public long lastFightTime; // last fight time saved as epochMilli timestamp (serializing an Instant was a bad time)
	@Expose
	@SerializedName("l")
	public boolean isLmsFight; // save a boolean if the fight was done in LMS, so we can know those stats/rings/ammo are used.
}