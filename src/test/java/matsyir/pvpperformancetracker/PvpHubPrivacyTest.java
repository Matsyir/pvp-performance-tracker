package matsyir.pvpperformancetracker;

import matsyir.pvpperformancetracker.utils.PvpHubPrivacy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PvpHubPrivacyTest
{
	@Test
	public void hiddenNameForReturnsStableHiddenName()
	{
		String anonymousId = "9c7b2aca-6865-4b4c-9f2b-f1f3f9f08dbb";

		String hiddenName = PvpHubPrivacy.hiddenNameFor(anonymousId);

		assertEquals(hiddenName, PvpHubPrivacy.hiddenNameFor(anonymousId));
		assertTrue(hiddenName.matches("^Hidden-[A-Z0-9]{5}$"));
	}

	@Test
	public void hiddenNameForChangesWithAnonymousId()
	{
		String firstHiddenName = PvpHubPrivacy.hiddenNameFor("9c7b2aca-6865-4b4c-9f2b-f1f3f9f08dbb");
		String secondHiddenName = PvpHubPrivacy.hiddenNameFor("2b1234e0-0861-4f0d-a488-c018a1d78a3f");

		assertNotEquals(firstHiddenName, secondHiddenName);
	}
}
