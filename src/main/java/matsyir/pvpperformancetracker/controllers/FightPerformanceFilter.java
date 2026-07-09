package matsyir.pvpperformancetracker.controllers;

import java.util.function.BiPredicate;
import joptsimple.internal.Strings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import matsyir.pvpperformancetracker.views.FightPerformancePanel;

@Getter
@Slf4j
public enum FightPerformanceFilter
{
	// order these by priority/which should be checked first. We'll start with preset filter styles,
	// since those check for a basic string match instead of potentially complex filtering (even the more complex
	// ones check for starting strings so won't check much aside from 1 or 2 startsWith() in most cases

	FAVORITE("Favorite Fights", true, "favorite", (_f, f) -> f.isFavorite()),
	SYNC("Synced Fights (via PvP-Hub)", true, "sync", (_f, f) -> f.hasPvpHubSyncedFight()),
	KILLS("Kills", true, "kills", (_f, f) -> f.getOpponent().isDead()),
	DEATHS("Deaths", true, "deaths", (_f, f) -> f.getCompetitor().isDead()),
	DOUBLE_DEATHS("Double-Deaths", true, "doubledeaths", (_f, f) -> f.getCompetitor().isDead() && f.getOpponent().isDead()),
	BG_STYLE_ALL("Background Style / Special KO borders", true, FightPerformancePanel.BackgroundStyle.PRESET_FILTER_STYLE_KEYWORD, (_f, f) ->
		f.getBgStyle() != null && f.getBgStyle().isEnabled() && f.getBgStyle() != FightPerformancePanel.BackgroundStyle.DEFAULT),

	COMPETITOR_ATTACK_COUNT("Competitor Attack Count >", true, "atk", "atk>20", (filter, f) -> {
		ComparisonTypeResult comparisonRes = ComparisonTypeResult.from("atk", filter);
		ComparisonType cType = comparisonRes.type;
		int cVal = comparisonRes.filterNumber;
		if (cType == ComparisonType.INVALID || cVal < 0)
		{
			return false;
		}

		return (cType == ComparisonType.GT && f.getCompetitor().getAttackCount() > cVal) ||
			(cType == ComparisonType.GTE && f.getCompetitor().getAttackCount() >= cVal) ||
			(cType == ComparisonType.LT && f.getCompetitor().getAttackCount() < cVal) ||
			(cType == ComparisonType.LTE && f.getCompetitor().getAttackCount() <= cVal);
	}),
	COMPETITOR_EXPECTED_DMG("Competitor Expected Damage >", true, "edmg", "edmg>50", (filter, f) -> {
		ComparisonTypeResult comparisonRes = ComparisonTypeResult.from("edmg", filter);
		ComparisonType cType = comparisonRes.type;
		int cVal = comparisonRes.filterNumber;
		if (cType == ComparisonType.INVALID || cVal < 0)
		{
			return false;
		}

		return (cType == ComparisonType.GT && f.getCompetitor().getExpectedDamage() > cVal) ||
			(cType == ComparisonType.GTE && f.getCompetitor().getExpectedDamage() >= cVal) ||
			(cType == ComparisonType.LT && f.getCompetitor().getExpectedDamage() < cVal) ||
			(cType == ComparisonType.LTE && f.getCompetitor().getExpectedDamage() <= cVal);
	}),
	COMPETITOR_DMG_DEALT("Competitor Damage Dealt >", true, "dmg", "dmg>50", (filter, f) -> {
		ComparisonTypeResult comparisonRes = ComparisonTypeResult.from("dmg", filter);
		ComparisonType cType = comparisonRes.type;
		int cVal = comparisonRes.filterNumber;
		if (cType == ComparisonType.INVALID || cVal < 0)
		{
			return false;
		}

		return (cType == ComparisonType.GT && f.getCompetitor().getDamageDealt() > cVal) ||
			(cType == ComparisonType.GTE && f.getCompetitor().getDamageDealt() >= cVal) ||
			(cType == ComparisonType.LT && f.getCompetitor().getDamageDealt() < cVal) ||
			(cType == ComparisonType.LTE && f.getCompetitor().getDamageDealt() <= cVal);
	}),

	// non-preset filters: directly compares the filter value with certain fields in the fight, rather than looking
	// for any hardcoded keywords at all.
	USERNAME_MATCH("Username Match", (filter, f) -> CONFIG.exactNameFilter()
		? (f.getCompetitor().getName().toLowerCase().equals(filter) || f.getOpponent().getName().toLowerCase().equals(filter))
		: (f.getCompetitor().getName().toLowerCase().startsWith(filter) || f.getOpponent().getName().toLowerCase().startsWith(filter))),

	// bgStyle.name match. e.g, you can search 'max' or 'max hit' to see fights that ended in a max hit ko.
	BG_STYLE_SPECIFIC("Bg Style", (filter, f) -> (
		(f.getBgStyle() != FightPerformancePanel.BackgroundStyle.DEFAULT
			&& f.getBgStyle().isEnabled()
			&& (f.getBgStyle().getName().toLowerCase().startsWith(filter)
			|| f.getBgStyle().getName().replace(" ", "").toLowerCase().startsWith(filter.replace(" ", ""))))));

	String name;
	boolean isPresetFilter; // determines if the filter should be shown on the preset filters dropdown.
	String filterPrefix; // keyword needed for hardcoded preset filters, or prefix keyword used for dynamic preset filters
	String defaultFilterVal; // for dynamic filters, actual default filter used instead of prefix
	BiPredicate<String, FightPerformance> matchProvider;

	FightPerformanceFilter(String name, BiPredicate<String, FightPerformance> matchProvider)
	{
		this(name, false, Strings.EMPTY, matchProvider);
	}

	FightPerformanceFilter(String name, boolean isPresetFilter, String filterPrefix, BiPredicate<String, FightPerformance> matchProvider)
	{
		this.name = name;
		this.isPresetFilter = isPresetFilter;
		this.filterPrefix = filterPrefix;
		this.defaultFilterVal = filterPrefix;
		this.matchProvider = matchProvider;
	}

	FightPerformanceFilter(String name, boolean isPresetFilter, String filterPrefix, String defaultFilterVal, BiPredicate<String, FightPerformance> matchProvider)
	{
		this.name = name;
		this.isPresetFilter = isPresetFilter;
		this.filterPrefix = filterPrefix;
		this.defaultFilterVal = defaultFilterVal;
		this.matchProvider = matchProvider;
	}

	@Override
	public String toString()
	{
		return name;
	}

	public static boolean matches(FightPerformance fight)
	{
		String filter = CONFIG.fightFilter();
		// empty is a valid filter, it means display every fight - don't filter them
		if (Strings.isNullOrEmpty(filter))
		{
			return true;
		}

		// temp dev filters
		// for testing ui with long names
//		try
//		{
//			if (filter.startsWith("long"))
//			{
//				return competitor.getName().length() >= 12 || opponent.getName().length() >= 12;
//			}
//		}
//		catch (Exception e) { }

		// for finding vw spec or other weapons to debug
//		if (filter.equals("voidwaker") && getAllFightLogEntries().stream().anyMatch(l -> l.getAnimationData() == AnimationData.MELEE_VOIDWAKER_SPEC))
//		{
//			return true;
//		}

		try
		{
			for (FightPerformanceFilter fStyle : FightPerformanceFilter.values())
			{
				if ((!fStyle.filterPrefix.isEmpty() && filter.startsWith(fStyle.filterPrefix))
					&& fStyle.matchProvider.test(filter, fight))
				{
					return true;
				}
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

		GT(">"),
		LT("<"),
		GTE(">="),
		LTE("<=");

		String keyword;

		ComparisonType(String keyword)
		{
			this.keyword = keyword;
		}

		// returns INVALID if no match
		static ComparisonType from(String requiredPrefix, String filter)
		{
			if (Strings.isNullOrEmpty(requiredPrefix) || Strings.isNullOrEmpty(filter))
			{
				return INVALID; // invalid params
			}

			int minLen = requiredPrefix.length() + GT.keyword.length() + 1;
			if (filter.length() < minLen || !filter.startsWith(requiredPrefix))
			{
				return INVALID; // doesn't start with prefix
			}

			boolean isGtFilter = filter.startsWith(requiredPrefix + ">");
			boolean isLtFilter = filter.startsWith(requiredPrefix + "<");
			if (!isGtFilter && !isLtFilter && filter.length() < (minLen + 1))
			{
				return INVALID;
			}
			boolean isGteFilter = filter.startsWith(requiredPrefix + ">=");
			boolean isLteFilter = filter.startsWith(requiredPrefix + "<=");

			if (!isGtFilter && !isGteFilter && !isLtFilter && !isLteFilter)
			{
				return INVALID;
			}

			return isGteFilter ? GTE
				: isGtFilter ? GT
				: isLteFilter ? LTE
				: isLtFilter ? LT
				: INVALID;
		}
	}

	private static class ComparisonTypeResult
	{
		ComparisonType type;
		int filterNumber;

		public static ComparisonTypeResult from(String requiredPrefix, String filter)
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

				// finally, parse int from string
				int filterNumber = Integer.parseInt(sb.toString());

				return new ComparisonTypeResult(type, filterNumber);
			}
			catch (Exception e)
			{
				return new ComparisonTypeResult(ComparisonType.INVALID, 0);
			}
		}

		private ComparisonTypeResult(ComparisonType type, int filterNumber)
		{
			this.type = type;
			this.filterNumber = filterNumber;
		}
	}
}
