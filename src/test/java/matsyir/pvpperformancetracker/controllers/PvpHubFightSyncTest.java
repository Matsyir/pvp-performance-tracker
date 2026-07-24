package matsyir.pvpperformancetracker.controllers;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PvpHubFightSyncTest
{
	@Test
	public void acceptsCanonicalFightReturnedForRequestedAlias()
	{
		PvpHubFightSync.FightDetail detail = new PvpHubFightSync.FightDetail();
		detail.fightId = "CANONICAL1";
		detail.requestedFightId = "ALIAS00001";

		assertNotNull(PvpHubFightSync.validateSyncedFightDetail(detail, "ALIAS00001"));
	}
}
