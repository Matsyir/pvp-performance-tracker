package matsyir.pvpperformancetracker;

import matsyir.pvpperformancetracker.models.FightLogEntry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FightLogEntryTimingTest
{
	@Test
	public void suppliedTickAndTimeAreStoredOnTestEntry()
	{
		int[] attackerGear = {1, 2, 3};
		int[] defenderGear = {4, 5, 6};
		int capturedTick = 12345;
		long capturedTime = 1782053037173L;

		FightLogEntry entry = new FightLogEntry(
			attackerGear,
			21,
			0.5,
			1,
			12,
			defenderGear,
			"attacker",
			capturedTick,
			capturedTime);

		assertEquals(capturedTick, entry.getTick());
		assertEquals(capturedTime, entry.getTime());
	}
}
