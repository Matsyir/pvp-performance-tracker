package matsyir.pvpperformancetracker.controllers;

import matsyir.pvpperformancetracker.utils.PvpUtils;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PvpDamageCalcCrimsonKistenTest
{
	private static final double DELTA = 0.000000001;

	@Test
	public void calculatesFourRollAccuracyProbabilities()
	{
		double[] noAccuracy = PvpDamageCalc.getCrimsonKistenAccuracyRollProbabilities(0);
		assertArrayEquals(new double[] {1, 0, 0, 0, 0}, noAccuracy, DELTA);
		assertEquals(0, PvpDamageCalc.getCrimsonKistenDisplayedAccuracy(0), DELTA);
		assertEquals(0, PvpDamageCalc.getExpectedDamage(
			PvpDamageCalc.buildCrimsonKistenDamageDistribution(0, 40)), DELTA);

		double[] halfAccuracy = PvpDamageCalc.getCrimsonKistenAccuracyRollProbabilities(0.5);
		assertArrayEquals(new double[] {1 / 16.0, 4 / 16.0, 6 / 16.0, 4 / 16.0, 1 / 16.0},
			halfAccuracy, DELTA);
		assertEquals(15 / 16.0, PvpDamageCalc.getCrimsonKistenDisplayedAccuracy(0.5), DELTA);

		double[] fullAccuracy = PvpDamageCalc.getCrimsonKistenAccuracyRollProbabilities(1);
		assertArrayEquals(new double[] {0, 0, 0, 0, 1}, fullAccuracy, DELTA);
		assertEquals(1, PvpDamageCalc.getCrimsonKistenDisplayedAccuracy(1), DELTA);
	}

	@Test
	public void calculatesInclusiveDamageBandsAndExpectedDamage()
	{
		int[][] ranges = PvpDamageCalc.getCrimsonKistenDamageRanges(40);
		assertArrayEquals(new int[] {28, 44}, ranges[0]);
		assertArrayEquals(new int[] {36, 52}, ranges[1]);
		assertArrayEquals(new int[] {44, 60}, ranges[2]);
		assertArrayEquals(new int[] {52, 68}, ranges[3]);

		double[] distribution = PvpDamageCalc.buildCrimsonKistenDamageDistribution(0.5, 40);
		double expectedDamage =
			(4 / 16.0) * ((28 + 44) / 2.0) +
			(6 / 16.0) * ((36 + 52) / 2.0) +
			(4 / 16.0) * ((44 + 60) / 2.0) +
			(1 / 16.0) * ((52 + 68) / 2.0);
		assertEquals(expectedDamage, PvpDamageCalc.getExpectedDamage(distribution), DELTA);

		double[] fullAccuracyDistribution = PvpDamageCalc.buildCrimsonKistenDamageDistribution(1, 40);
		for (int damage = 0; damage < fullAccuracyDistribution.length; damage++)
		{
			double expectedProbability = damage >= 52 && damage <= 68 ? 1 / 17.0 : 0;
			assertEquals(expectedProbability, fullAccuracyDistribution[damage], DELTA);
		}
	}

	@Test
	public void calculatesKoChanceFromTheMixedDamageDistribution()
	{
		double accuracy = 0.5;
		double[] distribution = PvpDamageCalc.buildCrimsonKistenDamageDistribution(accuracy, 40);
		int[] hitpoints = {30, 40, 45, 50, 60};
		for (int hp : hitpoints)
		{
			assertEquals(expectedKoChance(accuracy, hp),
				PvpUtils.calculateKoChance(distribution, hp), DELTA);
		}
		assertNull(PvpUtils.calculateKoChance(distribution, 69));
	}

	@Test
	public void appliesDamageReductionsBeforeCalculatingKoChance()
	{
		double[] original = PvpDamageCalc.buildCrimsonKistenDamageDistribution(0.5, 40);
		double[] protectedDistribution = PvpDamageCalc.scaleDamageDistribution(original, 0.6);
		double[] reducedDistribution = PvpDamageCalc.scaleDamageDistribution(protectedDistribution, 0.75);
		int hp = 25;

		double expectedKoChance = 0;
		double expectedDamage = 0;
		for (int damage = 0; damage < original.length; damage++)
		{
			int reducedDamage = (int) Math.floor(Math.floor(damage * 0.6) * 0.75);
			expectedDamage += reducedDamage * original[damage];
			if (reducedDamage >= hp)
			{
				expectedKoChance += original[damage];
			}
		}

		assertEquals(expectedDamage, PvpDamageCalc.getExpectedDamage(reducedDistribution), DELTA);
		assertEquals(expectedKoChance, PvpUtils.calculateKoChance(reducedDistribution, hp), DELTA);
		assertNull(PvpUtils.calculateKoChance(reducedDistribution, 31));
	}

	private static double expectedKoChance(double perRollAccuracy, int hp)
	{
		double[] rollProbabilities = PvpDamageCalc.getCrimsonKistenAccuracyRollProbabilities(perRollAccuracy);
		int[][] ranges = PvpDamageCalc.getCrimsonKistenDamageRanges(40);
		double chance = 0;
		for (int successes = 1; successes <= 4; successes++)
		{
			int minimum = ranges[successes - 1][0];
			int maximum = ranges[successes - 1][1];
			if (hp <= minimum)
			{
				chance += rollProbabilities[successes];
			}
			else if (hp <= maximum)
			{
				chance += rollProbabilities[successes] *
					(maximum - hp + 1) / (maximum - minimum + 1.0);
			}
		}
		return chance;
	}
}
