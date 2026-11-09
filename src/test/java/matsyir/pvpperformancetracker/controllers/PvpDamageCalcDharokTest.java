package matsyir.pvpperformancetracker.controllers;

import net.runelite.api.ItemID;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvpDamageCalcDharokTest
{
	@Test
	public void detectsNormalMixedDegradedAndLmsSets()
	{
		assertTrue(PvpDamageCalc.isFullDharokSet(dharokEquipment(
			ItemID.DHAROKS_HELM,
			ItemID.DHAROKS_GREATAXE,
			ItemID.DHAROKS_PLATEBODY,
			ItemID.DHAROKS_PLATELEGS)));
		assertTrue(PvpDamageCalc.isFullDharokSet(dharokEquipment(
			ItemID.DHAROKS_HELM_75,
			ItemID.DHAROKS_GREATAXE_25,
			ItemID.DHAROKS_PLATEBODY_100,
			ItemID.DHAROKS_PLATELEGS_50)));
		assertTrue(PvpDamageCalc.isFullDharokSet(dharokEquipment(
			ItemID.DHAROKS_HELM_23639,
			ItemID.DHAROKS_GREATAXE_25516,
			ItemID.DHAROKS_PLATEBODY_25515,
			ItemID.DHAROKS_PLATELEGS_23633)));
	}

	@Test
	public void excludesBrokenAndIncompleteSets()
	{
		assertFalse(PvpDamageCalc.isFullDharokSet(dharokEquipment(
			ItemID.DHAROKS_HELM_0,
			ItemID.DHAROKS_GREATAXE,
			ItemID.DHAROKS_PLATEBODY,
			ItemID.DHAROKS_PLATELEGS)));
		assertFalse(PvpDamageCalc.isFullDharokSet(dharokEquipment(
			ItemID.DHAROKS_HELM,
			ItemID.DHAROKS_GREATAXE_0,
			ItemID.DHAROKS_PLATEBODY,
			ItemID.DHAROKS_PLATELEGS)));
		assertFalse(PvpDamageCalc.isFullDharokSet(dharokEquipment(
			ItemID.DHAROKS_HELM,
			ItemID.DHAROKS_GREATAXE,
			ItemID.DHAROKS_PLATEBODY_0,
			ItemID.DHAROKS_PLATELEGS)));
		assertFalse(PvpDamageCalc.isFullDharokSet(dharokEquipment(
			ItemID.DHAROKS_HELM,
			ItemID.DHAROKS_GREATAXE,
			ItemID.DHAROKS_PLATEBODY,
			ItemID.DHAROKS_PLATELEGS_0)));

		int[] missingBody = dharokEquipment(
			ItemID.DHAROKS_HELM,
			ItemID.DHAROKS_GREATAXE,
			ItemID.DHAROKS_PLATEBODY,
			ItemID.DHAROKS_PLATELEGS);
		missingBody[KitType.TORSO.getIndex()] = 0;
		assertFalse(PvpDamageCalc.isFullDharokSet(missingBody));
		assertFalse(PvpDamageCalc.isFullDharokSet(new int[1]));
	}

	@Test
	public void scalesDharokMaxHitAndDoesNotPenalizeOverhealing()
	{
		assertEquals(50, PvpDamageCalc.scaleDharokMaxHit(50, 99, 99));
		assertEquals(74, PvpDamageCalc.scaleDharokMaxHit(50, 50, 99));
		assertEquals(98, PvpDamageCalc.scaleDharokMaxHit(50, 1, 99));
		assertEquals(50, PvpDamageCalc.scaleDharokMaxHit(50, 120, 99));
	}

	private static int[] dharokEquipment(int helm, int axe, int body, int legs)
	{
		int[] equipment = new int[12];
		equipment[KitType.HEAD.getIndex()] = helm + PlayerComposition.ITEM_OFFSET;
		equipment[KitType.WEAPON.getIndex()] = axe + PlayerComposition.ITEM_OFFSET;
		equipment[KitType.TORSO.getIndex()] = body + PlayerComposition.ITEM_OFFSET;
		equipment[KitType.LEGS.getIndex()] = legs + PlayerComposition.ITEM_OFFSET;
		return equipment;
	}

}
