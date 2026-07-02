package matsyir.pvpperformancetracker.utils;

import java.awt.Color;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG_KEY;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ColorUtil;

@Slf4j
public class PvpColorScheme
{
	public static final Color EMPTY = ColorUtil.colorWithAlpha(Color.BLACK, 0);

	// won't copy all ColorScheme values here, but this one should see a lot of use/save an import in many cases
	public static final Color BRAND_ORANGE = ColorScheme.BRAND_ORANGE;
	// a slightly darker WHITE, similar to ColorScheme.TEXT_COLOR, but a bit brighter
	public static final Color BRIGHTER_TEXT_COLOR = new Color(222, 224, 228);

	public static final Color BLOOD_RED_ORANGE = ColorUtil.colorLerp(BRAND_ORANGE, Color.RED, 0.5f);
	public static final Color BLOOD_RED_ORANGE_DARKER = ColorUtil.colorLerp(BRAND_ORANGE, Color.RED, 0.75f);

	public static final Color GREEN_TEXT_ACTION = new Color(30, 230, 30);
	public static final Color RED_TEXT_ACTION = BLOOD_RED_ORANGE_DARKER;

	// default colors used for config fields. Ideally don't directly use these unless we really have to, use the static getters to get the config instead.
	// the dmg colors use the same ones for now.
	public static final Color DEFAULT_SUCCESS = GREEN_TEXT_ACTION;
	public static final Color DEFAULT_UNSUCCESS = BLOOD_RED_ORANGE_DARKER;
	public static final Color DEFAULT_NEUTRAL = Color.WHITE;

	public static final Color DEFAULT_GHOST_BARRAGE = new Color(163, 165, 251); // ice barrage-y light-cyan

	@Getter
	public enum PanelColorPreset
	{
		DEFAULT("Default", DEFAULT_SUCCESS, DEFAULT_UNSUCCESS, DEFAULT_NEUTRAL, DEFAULT_GHOST_BARRAGE),
		CLASSIC("Classic", Color.GREEN, Color.WHITE, Color.WHITE, BRAND_ORANGE),
		HIGH_CONTRAST("High Contrast", Color.GREEN, Color.RED, Color.WHITE, Color.CYAN),

		DMGONLY_DEFAULT("<html>Default <font size='2'><u>*(dmgOnly)</u></font>", DEFAULT_NEUTRAL, DEFAULT_NEUTRAL, DEFAULT_NEUTRAL,
			DEFAULT_SUCCESS, DEFAULT_UNSUCCESS, DEFAULT_NEUTRAL, DEFAULT_GHOST_BARRAGE),
		DMGONLY_CLASSIC("<html>Classic <font size='2'><u>*(dmgOnly)</u></font>", Color.WHITE, Color.WHITE, Color.WHITE,
			Color.GREEN, Color.WHITE, Color.WHITE, BRAND_ORANGE),
		DMGONLY_HIGH_CONTRAST("<html>High Contrast <font size='2'><u>*(dmgOnly)</u></font>", Color.WHITE, Color.WHITE, Color.WHITE,
			Color.GREEN, Color.RED, Color.WHITE, Color.CYAN),

		CUSTOM("Custom", null, null, null, null);

		public final String name;

		public final Color success;

		public final Color unsuccess;
		public final Color neutral;

		public final Color dmgSuccess;
		public final Color dmgUnsuccess;
		public final Color dmgNeutral;

		public final Color gb;

		PanelColorPreset(String name, Color success, Color unsuccess, Color neutral, Color gb)
		{
			this(name, success, unsuccess, neutral, success, unsuccess, neutral, gb);
		}
		PanelColorPreset(String name, Color success, Color unsuccess, Color neutral, Color dmgSuccess, Color dmgUnsuccess, Color dmgNeutral, Color gb)
		{
			this.name = name;

			this.success = success;
			this.unsuccess = unsuccess;
			this.neutral = neutral;

			this.dmgSuccess = dmgSuccess;
			this.dmgUnsuccess = dmgUnsuccess;
			this.dmgNeutral = dmgNeutral;

			this.gb = gb;
		}

		@Override
		public String toString()
		{
			return this.name;
		}

		public void applyToConfig(ConfigManager cm)
		{
			if (this == CUSTOM || success == null)
			{
				return;
			}

			boolean configUpdateSuccess;
			try
			{
				PLUGIN.disableColorConfigEvents();

				cm.setConfiguration(CONFIG_KEY, "successColor", success);
				cm.setConfiguration(CONFIG_KEY, "unsuccessColor", unsuccess);
				cm.setConfiguration(CONFIG_KEY, "neutralColor", neutral);

				cm.setConfiguration(CONFIG_KEY, "successDmgColor", dmgSuccess);
				cm.setConfiguration(CONFIG_KEY, "unsuccessDmgColor", dmgUnsuccess);
				cm.setConfiguration(CONFIG_KEY, "neutralDmgColor", dmgNeutral);

				cm.setConfiguration(CONFIG_KEY, "gbColor", gb);

				configUpdateSuccess = true;
			}
			catch (Exception e)
			{
				configUpdateSuccess = false;
				log.info("Error while applying PanelColorPreset to config: " + e.getMessage());
			}

			PLUGIN.enableColorConfigEvents();

			PLUGIN.createConfirmationModal(configUpdateSuccess,
				configUpdateSuccess ? "<html>Your desired color preset was successfully applied to the config.<br>" +
					"Once you go back to the fight history panel, you should see your new theme applied.<br><br>" +
					"Note that the actual config screen won't update the colors until you close and re-open it."
				: "<html>An unknown error occurred while applying your desired color preset to the config.<br><br>" +
					"You should close and re-open the config before attempting to change colors or presets again."
			);
		}
	}

	public static Color successColor()
	{
		return CONFIG == null || CONFIG.successColor() == null
			? PanelColorPreset.DEFAULT.success
			: CONFIG.successColor();
	}

	public static Color unsuccessColor()
	{
		return CONFIG == null || CONFIG.unsuccessColor() == null
			? PanelColorPreset.DEFAULT.unsuccess
			: CONFIG.unsuccessColor();
	}

	public static Color neutralColor()
	{
		return CONFIG == null || CONFIG.neutralColor() == null
			? PanelColorPreset.DEFAULT.neutral
			: CONFIG.neutralColor();
	}

	public static Color successDmgColor()
	{
		return CONFIG == null || CONFIG.successDmgColor() == null
			? PanelColorPreset.DEFAULT.dmgSuccess
			: CONFIG.successDmgColor();
	}

	public static Color unsuccessDmgColor()
	{
		return CONFIG == null || CONFIG.unsuccessDmgColor() == null
			? PanelColorPreset.DEFAULT.dmgUnsuccess
			: CONFIG.unsuccessDmgColor();
	}

	public static Color neutralDmgColor()
	{
		return CONFIG == null || CONFIG.neutralDmgColor() == null
			? PanelColorPreset.DEFAULT.dmgNeutral
			: CONFIG.neutralDmgColor();
	}

	public static Color gbColor()
	{
		return CONFIG == null || CONFIG.gbColor() == null
			? PanelColorPreset.DEFAULT.gb
			: CONFIG.gbColor();
	}
}
