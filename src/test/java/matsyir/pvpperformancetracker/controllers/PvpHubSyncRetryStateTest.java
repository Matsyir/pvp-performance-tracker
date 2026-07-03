package matsyir.pvpperformancetracker.controllers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvpHubSyncRetryStateTest
{
	@Test
	public void enqueuedFightIsDueImmediately()
	{
		PvpHubSyncRetryState retryState = new PvpHubSyncRetryState(5, 60_000);

		retryState.enqueue(" FIGHT123 ", 1_000);

		assertEquals(1, retryState.dueFightIds(1_000).size());
		assertEquals("FIGHT123", retryState.dueFightIds(1_000).get(0));
	}

	@Test
	public void failedAttemptWaitsForRetryDelay()
	{
		PvpHubSyncRetryState retryState = new PvpHubSyncRetryState(5, 60_000);
		retryState.enqueue("FIGHT123", 1_000);

		assertTrue(retryState.markAttemptStarted("FIGHT123", 1_000));
		retryState.markUnavailable("FIGHT123");

		assertTrue(retryState.dueFightIds(60_999).isEmpty());
		assertEquals("FIGHT123", retryState.dueFightIds(61_000).get(0));
	}

	@Test
	public void failedAttemptsStopAtMaxAttempts()
	{
		PvpHubSyncRetryState retryState = new PvpHubSyncRetryState(2, 60_000);
		retryState.enqueue("FIGHT123", 1_000);

		assertTrue(retryState.markAttemptStarted("FIGHT123", 1_000));
		retryState.markUnavailable("FIGHT123");
		assertFalse(retryState.isEmpty());

		assertTrue(retryState.markAttemptStarted("FIGHT123", 61_000));
		retryState.markUnavailable("FIGHT123");

		assertTrue(retryState.isEmpty());
		assertTrue(retryState.dueFightIds(121_000).isEmpty());
	}

	@Test
	public void successRemovesPendingFight()
	{
		PvpHubSyncRetryState retryState = new PvpHubSyncRetryState(5, 60_000);
		retryState.enqueue("FIGHT123", 1_000);

		retryState.remove("FIGHT123");

		assertTrue(retryState.isEmpty());
	}

	@Test
	public void clearDropsAllPendingFights()
	{
		PvpHubSyncRetryState retryState = new PvpHubSyncRetryState(5, 60_000);
		retryState.enqueue("FIGHT123", 1_000);
		retryState.enqueue("FIGHT456", 1_000);

		retryState.clear();

		assertTrue(retryState.isEmpty());
	}
}
