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

import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import lombok.Getter;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin;
import net.runelite.client.game.WorldService;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldResult;

public final class WorldFlag
{
	@Getter
	public enum WorldDisplayChoice
	{
		FLAG("Flag Only"),
		HIDDEN("Hidden"),
		WORLD_LABEL("<html><font color='#ff0000'>&#9888;</font> World Label");

		private String name;

		WorldDisplayChoice(String name)
		{
			this.name = name;
		}
		@Override
		public String toString()
		{
			return name;
		}

	}
	private static final int FLAG_WIDTH = 24;
	private static final int FLAG_HEIGHT = 12;
	private static final int LOCATION_US_WEST = -73;
	private static final int LOCATION_US_EAST = -42;

	private static final Map<Region, ImageIcon> ICONS_BY_REGION = new HashMap<>();

	private WorldFlag()
	{
	}

	public static ImageIcon getIcon(int world, WorldService worldService, int worldLocation)
	{
		Region region = getRegion(world, worldService, worldLocation);
		return ICONS_BY_REGION.computeIfAbsent(region, WorldFlag::loadIcon);
	}

	public static String getTooltip(int world, WorldService worldService, int worldLocation)
	{
		Region region = getRegion(world, worldService, worldLocation);
		boolean showWorld = PvpPerformanceTrackerPlugin.CONFIG.getWorldDisplayChoice() == WorldDisplayChoice.WORLD_LABEL && world > 0;
		return (showWorld ? "World " + world + " - " : "") + region.displayName;
	}

	private static Region getRegion(int worldId, WorldService worldService, int worldLocation)
	{
		if (worldService == null)
		{
			return Region.UNITED_STATES;
		}

		try
		{
			WorldResult worldResult = worldService.getWorlds();
			if (worldResult == null)
			{
				return Region.UNITED_STATES;
			}

			World world = worldResult.findWorld(worldId);
			if (world == null || world.getRegion() == null)
			{
				return Region.UNITED_STATES;
			}

			if (world.getRegion() == WorldRegion.UNITED_STATES_OF_AMERICA)
			{
				return Region.fromUsLocation(worldLocation);
			}

			return Region.fromWorldRegion(world.getRegion());
		}
		catch (RuntimeException e)
		{
			return Region.UNITED_STATES;
		}
	}

	private static ImageIcon loadIcon(Region region)
	{
		URL resource = WorldFlag.class.getResource(region.resourcePath);
		if (resource == null)
		{
			return null;
		}

		ImageIcon icon = new ImageIcon(resource);
		Image scaled = icon.getImage().getScaledInstance(FLAG_WIDTH, FLAG_HEIGHT, Image.SCALE_SMOOTH);
		return new ImageIcon(scaled);
	}

	private enum Region
	{
		UNITED_STATES("/United_States_flag.png", "United States"),
		UNITED_STATES_EAST("/United_States_(east)_flag.png", "United States east"),
		UNITED_STATES_WEST("/United_States_(west)_flag.png", "United States west"),
		UNITED_KINGDOM("/United_Kingdom_flag.png", "United Kingdom"),
		GERMANY("/Germany_flag.png", "Germany"),
		AUSTRALIA("/Australia_flag.png", "Australia"),
		SINGAPORE("/Singapore_flag.png", "Singapore"),
		SOUTH_AFRICA("/South_Africa_flag.png", "South Africa"),
		BRAZIL("/Brazil_flag.png", "Brazil"),
		JAPAN("/Japan_flag.png", "Japan");

		private final String resourcePath;
		private final String displayName;

		private static Region fromWorldRegion(WorldRegion worldRegion)
		{
			switch (worldRegion)
			{
				case UNITED_KINGDOM:
					return UNITED_KINGDOM;
				case AUSTRALIA:
					return AUSTRALIA;
				case GERMANY:
					return GERMANY;
				case BRAZIL:
					return BRAZIL;
				case JAPAN:
					return JAPAN;
				case SINGAPORE:
					return SINGAPORE;
				case SOUTH_AFRICA:
					return SOUTH_AFRICA;
				case UNITED_STATES_OF_AMERICA:
				default:
					return UNITED_STATES;
			}
		}

		Region(String resourcePath, String displayName)
		{
			this.resourcePath = resourcePath;
			this.displayName = displayName;
		}

		private static Region fromUsLocation(int location)
		{
			if (location == LOCATION_US_EAST)
			{
				return UNITED_STATES_EAST;
			}

			if (location == LOCATION_US_WEST)
			{
				return UNITED_STATES_WEST;
			}

			return UNITED_STATES;
		}
	}
}
