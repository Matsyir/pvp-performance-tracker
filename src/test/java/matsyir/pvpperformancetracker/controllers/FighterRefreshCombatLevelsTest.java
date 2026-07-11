package matsyir.pvpperformancetracker.controllers;

import java.util.ArrayList;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FighterRefreshCombatLevelsTest
{
	@Test
	public void refreshCombatLevelsForTickUpdatesOnlyMatchingTickEntriesWithLevels()
	{
		ArrayList<FightLogEntry> entries = new ArrayList<>();
		int[] attackerGear = {1, 2, 3};
		int[] defenderGear = {4, 5, 6};

		FightLogEntry previousTickEntry = new FightLogEntry(attackerGear, 21, 0.5, 1, 12, defenderGear, "competitor", 103, 1000L);
		previousTickEntry.setAttackerLevels(new CombatLevels(99, 99, 99, 99, 90, 99));

		FightLogEntry targetTickEntry = new FightLogEntry(attackerGear, 21, 0.5, 1, 12, defenderGear, "competitor", 104, 1200L);
		targetTickEntry.setAttackerLevels(new CombatLevels(99, 99, 99, 99, 90, 99));

		FightLogEntry targetTickWithoutLevels = new FightLogEntry(attackerGear, 21, 0.5, 1, 12, defenderGear, "competitor", 104, 1300L);

		entries.add(previousTickEntry);
		entries.add(targetTickEntry);
		entries.add(targetTickWithoutLevels);

		Fighter.refreshCombatLevelsForTick(entries, 104, new CombatLevels(99, 99, 99, 99, 99, 99));

		assertEquals(90, previousTickEntry.getAttackerLevels().mage);
		assertEquals(99, targetTickEntry.getAttackerLevels().mage);
		assertNull(targetTickWithoutLevels.getAttackerLevels());
	}
}
