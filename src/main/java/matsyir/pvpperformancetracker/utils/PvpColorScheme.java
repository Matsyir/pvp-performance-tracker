package matsyir.pvpperformancetracker.utils;

import java.awt.Color;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ColorUtil;

public class PvpColorScheme
{
	public static final Color EMPTY = ColorUtil.colorWithAlpha(Color.BLACK, 0);

	public static final Color BRAND_ORANGE = ColorScheme.BRAND_ORANGE;
	public static final Color BLOOD_RED_ORANGE = ColorUtil.colorLerp(ColorScheme.BRAND_ORANGE, Color.RED, 0.5f);

	public static final Color GREEN_TEXT_ACTION = new Color(30, 230, 30);
	public static final Color RED_TEXT_ACTION = ColorUtil.colorLerp(ColorScheme.BRAND_ORANGE, Color.RED, 0.75f);

	// default colors used for config fields. Ideally don't directly use these unless we really have to, use the static getters to get the config instead.
	public static final Color DEFAULT_SUCCESS = Color.GREEN;
	public static final Color DEFAULT_UNSUCCESS = BLOOD_RED_ORANGE;
	public static final Color DEFAULT_NEUTRAL = Color.WHITE;

	public static final Color DEFAULT_DMG_SUCCESS = Color.GREEN;
	public static final Color DEFAULT_DMG_UNSUCCESS = BLOOD_RED_ORANGE;
	public static final Color DEFAULT_DMG_NEUTRAL = Color.WHITE;

	public static final Color DEFAULT_GHOST_BARRAGE = new Color(163, 165, 251); // ice barrage-y light-cyan

	public static Color successColor()
	{
		return CONFIG == null || CONFIG.successColor() == null
			? DEFAULT_SUCCESS
			: CONFIG.successColor();
	}

	public static Color unsuccessColor()
	{
		return CONFIG == null || CONFIG.unsuccessColor() == null
			? DEFAULT_UNSUCCESS
			: CONFIG.unsuccessColor();
	}

	public static Color neutralColor()
	{
		return CONFIG == null || CONFIG.neutralColor() == null
			? DEFAULT_NEUTRAL
			: CONFIG.neutralColor();
	}

	public static Color successDmgColor()
	{
		return CONFIG == null || CONFIG.successDmgColor() == null
			? DEFAULT_DMG_SUCCESS
			: CONFIG.successDmgColor();
	}

	public static Color unsuccessDmgColor()
	{
		return CONFIG == null || CONFIG.unsuccessDmgColor() == null
			? DEFAULT_DMG_UNSUCCESS
			: CONFIG.unsuccessDmgColor();
	}

	public static Color neutralDmgColor()
	{
		return CONFIG == null || CONFIG.neutralDmgColor() == null
			? DEFAULT_DMG_NEUTRAL
			: CONFIG.neutralDmgColor();
	}

	public static Color getGbColor()
	{
		return CONFIG == null || CONFIG.gbColor() == null
			? DEFAULT_GHOST_BARRAGE
			: CONFIG.gbColor();
	}
}
