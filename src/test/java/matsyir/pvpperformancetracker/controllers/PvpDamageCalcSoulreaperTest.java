package matsyir.pvpperformancetracker.controllers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import matsyir.pvpperformancetracker.models.AnimationData.AttackStyle;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.EquipmentData;
import org.junit.Test;
import sun.misc.Unsafe;

import static org.junit.Assert.assertEquals;

public class PvpDamageCalcSoulreaperTest
{
	@Test
	public void fullStackNormalAndSpecialUseUpdatedModifiers() throws Exception
	{
		PvpDamageCalc calc = newCalc();
		Method getMeleeMaxHit = PvpDamageCalc.class.getDeclaredMethod(
			"getMeleeMaxHit",
			int.class,
			boolean.class,
			EquipmentData.class,
			EquipmentData.VoidStyle.class,
			int.class,
			int.class);
		getMeleeMaxHit.setAccessible(true);

		getMeleeMaxHit.invoke(calc, 125, false, EquipmentData.SOULREAPER_AXE, EquipmentData.VoidStyle.NONE, -1, 5);
		assertEquals(41, calc.getMaxHit());

		getMeleeMaxHit.invoke(calc, 125, true, EquipmentData.SOULREAPER_AXE, EquipmentData.VoidStyle.NONE, -1, 5);
		assertEquals(41, calc.getMaxHit());
		assertEquals(9, calc.getMinHit());

		Method getMeleeAccuracy = PvpDamageCalc.class.getDeclaredMethod(
			"getMeleeAccuracy",
			int[].class,
			int[].class,
			AttackStyle.class,
			boolean.class,
			EquipmentData.class,
			EquipmentData.VoidStyle.class,
			int.class,
			double.class,
			int.class);
		getMeleeAccuracy.setAccessible(true);

		int[] attackerStats = new int[13];
		int[] defenderStats = new int[13];
		attackerStats[1] = 100;
		defenderStats[6] = 100;

		getMeleeAccuracy.invoke(calc, attackerStats, defenderStats, AttackStyle.SLASH, true,
			EquipmentData.SOULREAPER_AXE, EquipmentData.VoidStyle.NONE, -1, 1.0, 5);
		double fullStackAccuracy = calc.getAccuracy();
		double baseAttackRoll = Math.floor((99 + 8) * (100 + 64));
		double fullStackAttackRoll = baseAttackRoll * 1.6;
		double defenderRoll = Math.floor((99 + 8) * (100 + 64));
		double expectedAccuracy = 1 - (defenderRoll + 2) / (2 * (fullStackAttackRoll + 1));
		assertEquals(expectedAccuracy, fullStackAccuracy, 0.000001);

		Method getAverageHit = PvpDamageCalc.class.getDeclaredMethod(
			"getAverageHit", boolean.class, EquipmentData.class, boolean.class);
		getAverageHit.setAccessible(true);
		getAverageHit.invoke(calc, true, EquipmentData.SOULREAPER_AXE, true);
		assertEquals(fullStackAccuracy * 25, calc.getAverageHit(), 0.000001);
	}

	private static PvpDamageCalc newCalc() throws Exception
	{
		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		Unsafe unsafe = (Unsafe) unsafeField.get(null);
		PvpDamageCalc calc = (PvpDamageCalc) unsafe.allocateInstance(PvpDamageCalc.class);
		setField(calc, "attackerLevels", new CombatLevels(99, 99, 99, 99, 99, 99));
		setField(calc, "defenderLevels", new CombatLevels(99, 99, 99, 99, 99, 99));
		return calc;
	}

	private static void setField(PvpDamageCalc calc, String fieldName, Object value) throws Exception
	{
		Field field = PvpDamageCalc.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(calc, value);
	}
}
