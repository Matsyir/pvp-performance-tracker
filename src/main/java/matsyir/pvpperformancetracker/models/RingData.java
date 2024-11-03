/*
 * Copyright (c) 2020, Matsyir <https://github.com/matsyir>
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
import net.runelite.api.ItemID;

@Getter
public enum RingData
{
	SEERS_RING("Seers Ring", ItemID.SEERS_RING),
	ARCHERS_RING("Archers Ring", ItemID.ARCHERS_RING),
	BERSERKER_RING("Berserker Ring", ItemID.BERSERKER_RING),
	RING_OF_SUFFERING("Ring of Suffering", ItemID.RING_OF_SUFFERING),
	SEERS_RING_I("Seers Ring (i)", ItemID.SEERS_RING_I),
	ARCHERS_RING_I("Archers Ring (i)", ItemID.ARCHERS_RING_I),
	BERSERKER_RING_I("Berserker Ring (i)", ItemID.BERSERKER_RING_I),
	RING_OF_SUFFERING_I("Ring of Suffering (i)", ItemID.RING_OF_SUFFERING_I),
	BRIMSTONE_RING("Brimstone Ring", ItemID.BRIMSTONE_RING),
	MAGUS_RING("Magus ring", ItemID.MAGUS_RING),
	VENATOR_RING("Venator ring", ItemID.VENATOR_RING),
	BELLATOR_RING("Bellator ring", ItemID.BELLATOR_RING),
	ULTOR_RING("Ultor ring", ItemID.ULTOR_RING),
	RING_OF_SHADOWS("Ring of Shadows", ItemID.RING_OF_SHADOWS),
	NONE("None", -1);

	private String name;
	private int itemId;

	RingData(String name, int itemId)
	{
		this.name = name;
		this.itemId = itemId;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
