package matsyir.pvpperformancetracker.controllers;

import java.lang.reflect.Field;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class FighterAncientSpellSignalTest
{
	@Test
	public void sameTickSignalPrefersNewCastOverPreviousCastInLookback() throws Exception
	{
		Fighter fighter = new Fighter("attacker");
		FightLogEntry previousCast = ancientEntry(100, AnimationData.MAGIC_ICE_RUSH);
		FightLogEntry currentCast = ancientEntry(105, AnimationData.MAGIC_ICE_RUSH);
		fighter.getFightLogEntries().add(previousCast);
		fighter.getFightLogEntries().add(currentCast);

		FightLogEntry match = fighter.refineRecentAncientSpell(
			AnimationData.MAGIC_ICE_RUSH, 105, false);

		assertSame(currentCast, match);
	}

	private static FightLogEntry ancientEntry(int tick, AnimationData exactSpell) throws Exception
	{
		FightLogEntry entry = new FightLogEntry(
			new int[12], 0, 0, 0, 0, new int[12], "attacker", tick, 0);
		setField(entry, "animationData", AnimationData.MAGIC_ANCIENT_SINGLE_TARGET);
		setField(entry, "isFullEntry", true);
		entry.setExactMagicSpell(exactSpell);
		return entry;
	}

	private static void setField(FightLogEntry entry, String name, Object value) throws Exception
	{
		Field field = FightLogEntry.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(entry, value);
	}
}
