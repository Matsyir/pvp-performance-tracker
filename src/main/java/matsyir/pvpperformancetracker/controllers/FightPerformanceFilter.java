package matsyir.pvpperformancetracker.controllers;

import java.awt.Color;
import java.security.InvalidParameterException;
import java.util.function.BiPredicate;
import javax.inject.Inject;
import joptsimple.internal.Strings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.utils.PvpColorScheme;
import matsyir.pvpperformancetracker.utils.PvpPerformanceTrackerUtils;
import matsyir.pvpperformancetracker.views.FightPerformancePanel;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.ColorUtil;

@Getter
@Slf4j
public enum FightPerformanceFilter
{
	// order these by priority/which should be checked first. We'll start with preset filter styles,
	// since those start with a basic string match instead of potentially complex filtering (even the more complex
	// ones check for prefix strings so won't check much aside from 1 or 2 startsWith() in most cases)

	// dev filters for testing:
	// long RSNs: to test UI with the longest names
	//_DEV_LONG_RSN("DEV: Long RSNs", true, "long", (_f, f) -> f.getCompetitor().getName().length() >= 12 || f.getOpponent().getName().length() >= 12),

	FAVORITE("<html><font color='" + ColorUtil.colorToHexCode(PvpColorScheme.FAVORITE_GOLD) + "'>&#9733;&nbsp;Favorite Fights",
		true, "favorite", (_f, f) -> f.isFavorite()),
	SYNC("<html><font color='" + ColorUtil.colorToHexCode(PvpColorScheme.GREEN_TEXT_ACTION) + "'>&#8645;&nbsp;Synced Fights</font> (via <font color='" + ColorUtil.colorToHexCode(PvpColorScheme.BLUE_TEXT_URL) + "'><u>PvP-Hub</u></font>)",
		true, "synced", (_f, f) -> f.hasPvpHubSyncedFight()),
	KILLS("<html><font color='" + ColorUtil.colorToHexCode(PvpColorScheme.BRAND_ORANGE) + "'>&#9876;&nbsp;Kills",
		true, "kills", (_f, f) -> f.getOpponent().isDead()),
	DEATHS("<html><font color='" + ColorUtil.colorToHexCode(PvpColorScheme.BLOOD_RED_ORANGE) + "'>&#9760;&nbsp;Deaths",
		true, "deaths", (_f, f) -> f.getCompetitor().isDead()),
	DOUBLE_DEATHS("<html><font color='" + ColorUtil.colorToHexCode(PvpColorScheme.BLOOD_RED_ORANGE_REDDER) + "'>&#9760;&#9760;&nbsp;Double-Deaths", true, "doubledeaths", (_f, f) -> f.getCompetitor().isDead() && f.getOpponent().isDead()),

	COMPETITOR_ATTACK_COUNT("<html><font color='" + ColorUtil.colorToHexCode(ColorUtil.colorLerp(PvpColorScheme.BLUE_TEXT_URL, Color.BLUE, 0.5f)) +
		"'>&#8807;&sup1;&nbsp;Competitor Attack Count >", true, "::atk", "::atk>20", (filter, f) ->
		ComparisonType.parseAndCompareInt(f.getCompetitor().getAttackCount(), "::atk", filter)),

	COMPETITOR_EXPECTED_DMG("<html><font color='" + ColorUtil.colorToHexCode(ColorUtil.colorLerp(PvpColorScheme.BLUE_TEXT_URL, Color.BLUE, 0.5f)) +
		"'>&#8807;&sup2;&nbsp;Competitor Expected Damage >",
		true, "::ed", "::ed>50", (filter, f) ->
		ComparisonType.parseAndCompareInt((int)f.getCompetitor().getExpectedDamage(), "::ed", filter)),

	COMPETITOR_DMG_DEALT("<html><font color='" + ColorUtil.colorToHexCode(ColorUtil.colorLerp(PvpColorScheme.BLUE_TEXT_URL, Color.BLUE, 0.5f)) +
		"'>&#8807;&sup3;&nbsp;Competitor Damage Dealt >",
		true, "::d", "::d>50", (filter, f) ->
		ComparisonType.parseAndCompareInt(f.getCompetitor().getDamageDealt(), "::d", filter)),

	// non-preset filters: directly compares the filter value with certain fields in the fight, rather than looking
	// for any hardcoded keywords or prefixes at all.
	USERNAME_MATCH("Username Match", (filter, f) -> CONFIG.exactNameFilter()
		? (f.getCompetitor().getName().toLowerCase().equals(filter) || f.getOpponent().getName().toLowerCase().equals(filter))
		: (f.getCompetitor().getName().toLowerCase().startsWith(filter) || f.getOpponent().getName().toLowerCase().startsWith(filter))),

	// bgStyle.name match. e.g, you can search 'max' or 'max hit' to see fights that ended in a max hit ko.
	BG_STYLE_SPECIFIC("Bg Style", (filter, f) -> (
		(f.getBgStyle() != FightPerformancePanel.BackgroundStyle.DEFAULT
			&& f.getBgStyle().isEnabled()
			&& (f.getBgStyle().getName().toLowerCase().startsWith(filter)
			|| f.getBgStyle().getName().replace(" ", "").toLowerCase().startsWith(filter.replace(" ", "")))))),

	HAS_WEAPON("<html><font color='" + ColorUtil.colorToHexCode(ColorUtil.colorLerp(PvpColorScheme.BLUE_TEXT_URL, Color.MAGENTA.darker(), 0.3f)) +
		"'>&#9876;&nbsp;Fight Includes Weapon", true, "hasweapon", "hasweapon=voidwaker", (filter, f) ->
		f.getAllFightLogEntries().stream().anyMatch((log) ->
		{
			try
			{
				int fixedItemId = log.getAttackerGear()[KitType.WEAPON.getIndex()] - PlayerComposition.ITEM_OFFSET;
				boolean attackerHasWep = fixedItemId > 0 && PLUGIN.getItemManager().getItemComposition(fixedItemId).getName().toLowerCase().startsWith(filter.substring("hasweapon=".length()));
				if (attackerHasWep)
				{
					return true;
				}

				fixedItemId = log.getDefenderGear()[KitType.WEAPON.getIndex()] - PlayerComposition.ITEM_OFFSET;
				return fixedItemId > 0 && PLUGIN.getItemManager().getItemComposition(fixedItemId).getName().toLowerCase().startsWith(filter.substring("hasweapon=".length()));
			}
			catch (Exception ignored)
			{
				return false;
			}
		})),
	BG_STYLE_ALL("<html><font color='" + ColorUtil.colorToHexCode(FightPerformancePanel.BackgroundStyle.SPEC_KO.getHighlightColor()) +
		"'>&#9744;</font>&nbsp;Border Styles (KO style detection)", true, FightPerformancePanel.BackgroundStyle.PRESET_FILTER_STYLE_KEYWORD, (_f, f) ->
		f.getBgStyle() != null && f.getBgStyle().isEnabled() && f.getBgStyle() != FightPerformancePanel.BackgroundStyle.DEFAULT),
	BG_STYLE_MINUS_SPEC("<html><font color='" + ColorUtil.colorToHexCode(FightPerformancePanel.BackgroundStyle.MAX_HIT_KO.getHighlightColor()) +
		"'>&#9744;</font>&nbsp;Border Styles (excluding Spec KO)", true, FightPerformancePanel.BackgroundStyle.PRESET_FILTER_STYLE_KEYWORD_NO_SPEC, (_f, f) ->
		f.getBgStyle() != null && f.getBgStyle().isEnabled() && f.getBgStyle() != FightPerformancePanel.BackgroundStyle.DEFAULT && f.getBgStyle() != FightPerformancePanel.BackgroundStyle.SPEC_KO);

	final String name;
	final boolean isPresetFilter; // determines if the filter should be shown on the preset filters dropdown.
	final String filterPrefix; // keyword needed for hardcoded preset filters, or prefix keyword used for dynamic preset filters
	final String defaultFilterVal; // for dynamic filters, actual default filter used instead of prefix
	final boolean requiresExactKeywordMatch;
	final BiPredicate<String, FightPerformance> matchProvider;

	@Inject
	private static ItemManager itemManager;

	FightPerformanceFilter(String name, BiPredicate<String, FightPerformance> matchProvider)
	{
		this(name, false, Strings.EMPTY, matchProvider);
	}

	FightPerformanceFilter(String name, boolean isPresetFilter, String filterPrefix, BiPredicate<String, FightPerformance> matchProvider)
	{
		this(name, isPresetFilter, filterPrefix, filterPrefix, matchProvider);
	}

	FightPerformanceFilter(String name, boolean isPresetFilter, String filterPrefix, String defaultFilterVal, BiPredicate<String, FightPerformance> matchProvider)
	{
		this.name = name;
		this.isPresetFilter = isPresetFilter;
		this.filterPrefix = filterPrefix;
		this.defaultFilterVal = defaultFilterVal;
		this.requiresExactKeywordMatch = isPresetFilter && filterPrefix.equals(defaultFilterVal);
		this.matchProvider = matchProvider;

		if ((isPresetFilter && PvpPerformanceTrackerUtils.anyStringNullOrEmpty(this.name, this.filterPrefix, this.defaultFilterVal))
			|| this.matchProvider == null)
		{
			throw new InvalidParameterException("Attempted to initialize FightPerformanceFilter with missing fields.");
		}
	}

	@Override
	public String toString()
	{
		return name;
	}

	public static boolean matches(FightPerformance fight)
	{
		String requestedFilter = CONFIG.fightFilter();
		// empty is a valid filter, it means display every fight - don't filter them
		if (Strings.isNullOrEmpty(requestedFilter))
		{
			return true;
		}

		try
		{
			for (FightPerformanceFilter fStyle : FightPerformanceFilter.values())
			{
				try
				{
					if ((fStyle.requiresExactKeywordMatch ? requestedFilter.equals(fStyle.filterPrefix) : requestedFilter.startsWith(fStyle.filterPrefix))
						&& fStyle.matchProvider.test(requestedFilter, fight))
					{
						return true;
					}
				}
				catch (Exception ignored) {}
			}
			return false;
		}
		catch (Exception e)
		{
			log.warn("FightPerformanceFilter.matches: Unexpected error while trying to process filter: " + e.getMessage());
			return false;
		}
	}


	private enum ComparisonType
	{
		INVALID(""),

		EQUALS("="),
		GT(">"),
		LT("<"),
		GTE(">="),
		LTE("<=");

		final String keyword;

		ComparisonType(String keyword)
		{
			this.keyword = keyword;
		}

		// returns INVALID if no match
		private static ComparisonType from(String requiredPrefix, String filter)
		{
			if (PvpPerformanceTrackerUtils.anyStringNullOrEmpty(requiredPrefix, filter))
			{
				return INVALID; // invalid params
			}

			int minLen = requiredPrefix.length() + GT.keyword.length() + 1; // +1 since we need 1 digit or more as well
			if (filter.length() < minLen || !filter.startsWith(requiredPrefix))
			{
				return INVALID; // doesn't start with prefix, or has no digit
			}

			// check for 1-char ComparisonTypes first
			boolean isEqualsFilter = filter.startsWith(requiredPrefix + "=");
			boolean isGtFilter = filter.startsWith(requiredPrefix + ">");
			boolean isLtFilter = filter.startsWith(requiredPrefix + "<");
			// GTE & LTE have the same start as GT & LT, so if it's neither GT or LT, it's not gonna be GTE/LTE either,
			// so we can skip checking those here.
			if (!isEqualsFilter && !isGtFilter && !isLtFilter && filter.length() < (minLen + 1))
			{
				return INVALID;
			}
			boolean isGteFilter = filter.startsWith(requiredPrefix + ">=");
			boolean isLteFilter = filter.startsWith(requiredPrefix + "<=");

			if (!isEqualsFilter && !isGtFilter && !isGteFilter && !isLtFilter && !isLteFilter)
			{
				return INVALID;
			}

			return isEqualsFilter ? EQUALS
				: isGteFilter ? GTE
				: isGtFilter ? GT
				: isLteFilter ? LTE
				: isLtFilter ? LT
				: INVALID;
		}

		public static boolean parseAndCompareInt(int intToCompare, String requiredPrefix, String filter)
		{
			try
			{
				ComparisonType type = ComparisonType.from(requiredPrefix, filter);
				if (type == ComparisonType.INVALID)
				{
					throw new Exception("Invalid Comparison");
				}

				StringBuilder sb = new StringBuilder();

				// remove prefix + gt/gte symbol, remove all chars that aren't digits, append to string
				filter.substring(requiredPrefix.length() + type.keyword.length())
					.chars()
					.mapToObj(c -> (char) c)
					.filter(Character::isDigit)
					.forEach(sb::append);

				// finally, parse int from remaining string
				int parsedFilterInt = Integer.parseInt(sb.toString());

				return (type == ComparisonType.EQUALS && intToCompare == parsedFilterInt) ||
					(type == ComparisonType.GT && intToCompare > parsedFilterInt) ||
					(type == ComparisonType.GTE && intToCompare >= parsedFilterInt) ||
					(type == ComparisonType.LT && intToCompare < parsedFilterInt) ||
					(type == ComparisonType.LTE && intToCompare <= parsedFilterInt);
			}
			catch (Exception e)
			{
				return false;
			}
		}
		public static boolean parseAndCompareString(String strToCompare, String requiredPrefix, String filter)
		{
			try
			{
				ComparisonType type = ComparisonType.from(requiredPrefix, filter);
				if (type != ComparisonType.EQUALS) // only support EQUALS for string comparison.
				{
					throw new Exception("Invalid Comparison");
				}

				// remove prefix + gt/gte symbol, the remaining string is what we want to compare with.
				return filter.substring(requiredPrefix.length() + type.keyword.length()).startsWith(strToCompare);
			}
			catch (Exception e)
			{
				return false;
			}
		}
	}
}
