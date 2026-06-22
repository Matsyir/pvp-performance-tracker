package matsyir.pvpperformancetracker.controllers;

import java.util.ArrayList;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FightPerformanceRelativeTickTest
{
	@Test
	public void makeLogTicksRelativeToFightStartUsesEarliestLogTickFallback()
	{
		ArrayList<FightLogEntry> logs = new ArrayList<>();
		int[] attackerGear = {1, 2, 3};
		int[] defenderGear = {4, 5, 6};

		FightLogEntry first = new FightLogEntry(attackerGear, 21, 0.5, 1, 12, defenderGear, "competitor", 100, 1000L);
		FightLogEntry second = new FightLogEntry(attackerGear, 21, 0.5, 1, 12, defenderGear, "opponent", 106, 4600L);
		logs.add(first);
		logs.add(second);

		FightPerformance.makeLogTicksRelativeToStart(logs, 100);

		assertEquals(0, first.getTick());
		assertEquals(6, second.getTick());
	}
}
