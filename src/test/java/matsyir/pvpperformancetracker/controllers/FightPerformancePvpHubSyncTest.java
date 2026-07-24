package matsyir.pvpperformancetracker.controllers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FightPerformancePvpHubSyncTest
{
	@Test
	public void syncedFightIsReorientedToLocalPlayerPerspective()
	{
		FightPerformance localFight = fight("Player A", "Player B", 10, 20);
		FightPerformance syncedFight = fight("Player B", "Player A", 200, 100);

		localFight.setPvpHubSyncedFight(syncedFight);

		FightPerformance displayFight = localFight.getPvpHubDisplayFight();
		assertEquals("Player A", displayFight.getCompetitor().getName());
		assertEquals(100, displayFight.getCompetitor().getDamageDealt());
		assertEquals("Player B", displayFight.getOpponent().getName());
		assertEquals(200, displayFight.getOpponent().getDamageDealt());
	}

	@Test
	public void syncedFightKeepsMatchingLocalPlayerPerspective()
	{
		FightPerformance localFight = fight("Player A", "Player B", 10, 20);
		FightPerformance syncedFight = fight("Player A", "Player B", 100, 200);

		localFight.setPvpHubSyncedFight(syncedFight);

		FightPerformance displayFight = localFight.getPvpHubDisplayFight();
		assertEquals("Player A", displayFight.getCompetitor().getName());
		assertEquals(100, displayFight.getCompetitor().getDamageDealt());
		assertEquals("Player B", displayFight.getOpponent().getName());
		assertEquals(200, displayFight.getOpponent().getDamageDealt());
	}

	@Test
	public void syncedFightCanReorientWhenOnlyOpponentNameMatches()
	{
		FightPerformance localFight = fight("Player A", "Player B", 10, 20);
		FightPerformance syncedFight = fight("Player B", "Hidden-12345", 200, 100);

		localFight.setPvpHubSyncedFight(syncedFight);

		FightPerformance displayFight = localFight.getPvpHubDisplayFight();
		assertEquals("Player A", displayFight.getCompetitor().getName());
		assertEquals(100, displayFight.getCompetitor().getDamageDealt());
		assertEquals("Player B", displayFight.getOpponent().getName());
		assertEquals(200, displayFight.getOpponent().getDamageDealt());
	}

	@Test
	public void syncedFightCanReorientWithStoredHiddenUploadName()
	{
		FightPerformance localFight = fight("Player A", "Player B", 10, 20);
		localFight.recordPvpHubUploadName("Hidden-11111");
		FightPerformance syncedFight = fight("Hidden-22222", "Hidden-11111", 200, 100);

		localFight.setPvpHubSyncedFight(syncedFight);

		FightPerformance displayFight = localFight.getPvpHubDisplayFight();
		assertEquals("Player A", displayFight.getCompetitor().getName());
		assertEquals(100, displayFight.getCompetitor().getDamageDealt());
		assertEquals("Player B", displayFight.getOpponent().getName());
		assertEquals(200, displayFight.getOpponent().getDamageDealt());
	}

	@Test
	public void syncedFightFallsBackToStatsWhenNamesCannotOrient()
	{
		FightPerformance localFight = fight("Player A", "Player B", 10, 80);
		FightPerformance syncedFight = fight("Hidden-22222", "Hidden-11111", 80, 10);

		localFight.setPvpHubSyncedFight(syncedFight);

		FightPerformance displayFight = localFight.getPvpHubDisplayFight();
		assertEquals("Player A", displayFight.getCompetitor().getName());
		assertEquals(10, displayFight.getCompetitor().getDamageDealt());
		assertEquals("Player B", displayFight.getOpponent().getName());
		assertEquals(80, displayFight.getOpponent().getDamageDealt());
	}

	@Test
	public void syncedFightDoesNotUseAmbiguousStatsToSwap()
	{
		FightPerformance localFight = fight("Player A", "Player B", 50, 50);
		FightPerformance syncedFight = fight("Hidden-22222", "Hidden-11111", 50, 50);

		localFight.setPvpHubSyncedFight(syncedFight);

		FightPerformance displayFight = localFight.getPvpHubDisplayFight();
		assertEquals("Player A", displayFight.getCompetitor().getName());
		assertEquals(50, displayFight.getCompetitor().getDamageDealt());
		assertEquals("Player B", displayFight.getOpponent().getName());
		assertEquals(50, displayFight.getOpponent().getDamageDealt());
	}

	@Test
	public void attackTimeOpponentHpPrefersVisibleHealthBarThenMatchedHit()
	{
		assertEquals(50, FightPerformance.estimateOpponentHpAtAttack(15, 30, 20, 99));
		assertEquals(20, FightPerformance.estimateOpponentHpAtAttack(-1, -1, 20, 99));
		assertEquals(99, FightPerformance.estimateOpponentHpAtAttack(-1, -1, null, 99));
		assertEquals(0, FightPerformance.estimateOpponentHpAtAttack(0, 30, 20, 99));
	}

	private static FightPerformance fight(String competitorName, String opponentName, int competitorDamage, int opponentDamage)
	{
		FightPerformance fight = new FightPerformance();
		fight.competitor = fighter(competitorName, competitorDamage);
		fight.opponent = fighter(opponentName, opponentDamage);
		return fight;
	}

	private static Fighter fighter(String name, int damage)
	{
		Fighter fighter = new Fighter(name);
		fighter.addAttacks(1, 1, damage, damage, 0, 0, 0, 0, 0, 0, 0);
		return fighter;
	}
}
