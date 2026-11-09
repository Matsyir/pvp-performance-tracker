package matsyir.pvpperformancetracker.controllers;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class PvpHubFightSyncTest
{
	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void savesOnlyValidFightIds() throws Exception
	{
		File directory = temp.newFolder();
		PvpHubFightSync.saveSyncedFight(directory, "ABCDEF1234", "{}");
		assertTrue(new File(directory, "ABCDEF1234.json").isFile());
		for (String id : new String[]{"../escape", "abcDEF1234", " ABCDEF1234", "ABCDEF12345"})
		{
			PvpHubFightSync.saveSyncedFight(directory, id, "{}");
		}
		assertFalse(new File(temp.getRoot(), "escape.json").exists());
		assertTrue(directory.list().length == 1);
	}
	@Test
	public void acceptsCanonicalFightReturnedForRequestedAlias()
	{
		PvpHubFightSync.FightDetail detail = new PvpHubFightSync.FightDetail();
		detail.fightId = "CANONICAL1";
		detail.requestedFightId = "ALIAS00001";

		assertNotNull(PvpHubFightSync.validateSyncedFightDetail(detail, "ALIAS00001"));
	}
}
