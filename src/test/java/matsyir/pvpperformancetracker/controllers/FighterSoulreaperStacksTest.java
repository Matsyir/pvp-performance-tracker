package matsyir.pvpperformancetracker.controllers;

import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.EquipmentData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FighterSoulreaperStacksTest
{
	@Test
	public void mapsAndTracksSoulreaperStatePerFighter()
	{
		assertEquals(AnimationData.MELEE_SOULREAPER_AXE_CRUSH, AnimationData.fromId(10171));
		assertEquals(AnimationData.MELEE_SOULREAPER_AXE_SLASH, AnimationData.fromId(10172));
		assertEquals(AnimationData.MELEE_SOULREAPER_AXE_SPEC, AnimationData.fromId(10173));
		assertEquals(EquipmentData.SOULREAPER_AXE, EquipmentData.fromId(28338));
		assertEquals(EquipmentData.SOULREAPER_AXE, EquipmentData.fromId(33335));

		Fighter fighter = new Fighter("fighter");
		Fighter otherFighter = new Fighter("other");

		assertEquals(0, fighter.updateSoulreaperStacks(AnimationData.MELEE_SOULREAPER_AXE_CRUSH, 100));
		assertEquals(1, fighter.updateSoulreaperStacks(AnimationData.MELEE_SOULREAPER_AXE_SLASH, 105));
		assertEquals(2, fighter.updateSoulreaperStacks(AnimationData.MELEE_SOULREAPER_AXE_SLASH, 110));
		assertEquals(3, fighter.updateSoulreaperStacks(AnimationData.MELEE_SOULREAPER_AXE_SLASH, 115));
		assertEquals(4, fighter.updateSoulreaperStacks(AnimationData.MELEE_SOULREAPER_AXE_SLASH, 120));
		assertEquals(4, fighter.updateSoulreaperStacks(AnimationData.MELEE_SOULREAPER_AXE_SLASH, 170));
		assertEquals(5, fighter.updateSoulreaperStacks(AnimationData.MELEE_SOULREAPER_AXE_SPEC, 175));
		assertEquals(0, fighter.updateSoulreaperStacks(AnimationData.MELEE_SOULREAPER_AXE_SLASH, 180));

		assertEquals(0, otherFighter.updateSoulreaperStacks(AnimationData.MELEE_SOULREAPER_AXE_SLASH, 180));
	}
}
