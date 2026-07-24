package matsyir.pvpperformancetracker.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.runelite.api.ItemID;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FightLogEntryDharokHpTest
{
	private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

	@Test
	public void formatsExactEstimatedFallbackAndLegacyHp()
	{
		FightLogEntry exact = entry();
		exact.setAttackerLevels(new CombatLevels(99, 118, 99, 99, 99, 37));
		assertEquals("37/99", exact.getDharokHpAtAttackText(99));

		FightLogEntry estimated = entry();
		estimated.setAttackerEstimatedHp(37);
		assertEquals("~37/99", estimated.getDharokHpAtAttackText(99));

		FightLogEntry fallback = entry();
		fallback.setAttackerEstimatedHp(99);
		assertEquals("~99/99", fallback.getDharokHpAtAttackText(99));

		assertEquals("-", entry().getDharokHpAtAttackText(99));
	}

	@Test
	public void estimatedHpIsAdditiveAndLegacyJsonStillLoads()
	{
		FightLogEntry entry = entry();
		entry.setAttackerEstimatedHp(42);

		String json = GSON.toJson(entry);
		assertTrue(json.contains("\"aH\":42"));

		JsonObject legacyJson = GSON.fromJson(json, JsonObject.class);
		legacyJson.remove("aH");
		FightLogEntry legacyEntry = GSON.fromJson(legacyJson, FightLogEntry.class);
		assertNull(legacyEntry.getAttackerEstimatedHp());
		assertEquals("-", legacyEntry.getDharokHpAtAttackText(99));
	}

	private static FightLogEntry entry()
	{
		int[] equipment = new int[12];
		equipment[KitType.HEAD.getIndex()] = ItemID.DHAROKS_HELM + PlayerComposition.ITEM_OFFSET;
		equipment[KitType.WEAPON.getIndex()] = ItemID.DHAROKS_GREATAXE + PlayerComposition.ITEM_OFFSET;
		equipment[KitType.TORSO.getIndex()] = ItemID.DHAROKS_PLATEBODY + PlayerComposition.ITEM_OFFSET;
		equipment[KitType.LEGS.getIndex()] = ItemID.DHAROKS_PLATELEGS + PlayerComposition.ITEM_OFFSET;
		return new FightLogEntry(equipment, 25, 0.75, 0, 50, new int[12], "attacker");
	}
}
