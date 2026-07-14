package matsyir.pvpperformancetracker.controllers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.EquipmentData;
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import net.runelite.api.SpriteID;
import org.junit.Test;
import sun.misc.Unsafe;

import static org.junit.Assert.assertEquals;

public class PvpDamageCalcMagicPrayerTest
{
	@Test
	public void auguryAndVirtusAncientBonusProduceFortyIceBarrageMaxHit() throws Exception
	{
		PvpDamageCalc calc = newCalc();

		Method getMagicMaxHit = PvpDamageCalc.class.getDeclaredMethod(
			"getMagicMaxHit",
			int.class,
			AnimationData.class,
			int.class,
			EquipmentData.VoidStyle.class,
			EquipmentData.class,
			EquipmentData.class,
			EquipmentData.class,
			EquipmentData.class,
			EquipmentData.class);
		getMagicMaxHit.setAccessible(true);

		getMagicMaxHit.invoke(
			calc,
			25,
			AnimationData.MAGIC_ANCIENT_MULTI_TARGET,
			SpriteID.PRAYER_AUGURY,
			EquipmentData.VoidStyle.NONE,
			null,
			EquipmentData.ZURIELS_STAFF,
			null,
			EquipmentData.VIRTUS_ROBE_TOP,
			EquipmentData.VIRTUS_ROBE_BOTTOM);

		assertEquals(40, calc.getMaxHit());
	}

	@Test
	public void protectionPrayerReducesExpectedDamageButNotDisplayedMaxHit() throws Exception
	{
		PvpDamageCalc calc = newCalc();
		setField(calc, "maxHit", 40);
		setField(calc, "minHit", 0);
		setField(calc, "accuracy", 1.0);

		Method getAverageHit = PvpDamageCalc.class.getDeclaredMethod(
			"getAverageHit",
			boolean.class,
			EquipmentData.class,
			boolean.class);
		getAverageHit.setAccessible(true);

		getAverageHit.invoke(calc, false, EquipmentData.ZURIELS_STAFF, false);

		assertEquals(12.0, calc.getAverageHit(), 0.0001);
		assertEquals(40, calc.getMaxHit());
		assertEquals(0, calc.getMinHit());
	}

	@Test
	public void thrownJavelinDoesNotUseEquippedBoltsAsAmmo() throws Exception
	{
		PvpDamageCalc calc = newCalc();
		setField(calc, "isLmsFight", false);

		Method getWeaponAmmo = PvpDamageCalc.class.getDeclaredMethod(
			"getWeaponAmmo",
			EquipmentData.class,
			Integer.class);
		getWeaponAmmo.setAccessible(true);

		RangeAmmoData ammo = (RangeAmmoData) getWeaponAmmo.invoke(
			calc,
			EquipmentData.MORRIGANS_JAVELIN,
			RangeAmmoData.StrongBoltAmmo.OPAL_DRAGON_BOLTS_E.getItemId());

		assertEquals(null, ammo);
	}

	@Test
	public void bowCanUseCompatibleEquippedArrows() throws Exception
	{
		PvpDamageCalc calc = newCalc();
		setField(calc, "isLmsFight", false);

		Method getWeaponAmmo = PvpDamageCalc.class.getDeclaredMethod(
			"getWeaponAmmo",
			EquipmentData.class,
			Integer.class);
		getWeaponAmmo.setAccessible(true);

		RangeAmmoData ammo = (RangeAmmoData) getWeaponAmmo.invoke(
			calc,
			EquipmentData.MAGIC_SHORTBOW,
			RangeAmmoData.OtherAmmo.SEEKING_DRAGON_ARROW.getItemId());

		assertEquals(RangeAmmoData.OtherAmmo.SEEKING_DRAGON_ARROW, ammo);
	}

	private static PvpDamageCalc newCalc() throws Exception
	{
		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		Unsafe unsafe = (Unsafe) unsafeField.get(null);
		return (PvpDamageCalc) unsafe.allocateInstance(PvpDamageCalc.class);
	}

	private static void setField(PvpDamageCalc calc, String fieldName, Object value) throws Exception
	{
		Field field = PvpDamageCalc.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(calc, value);
	}
}
