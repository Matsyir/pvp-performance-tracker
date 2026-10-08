package matsyir.pvpperformancetracker.utils;

import java.lang.reflect.Proxy;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.atomic.AtomicInteger;
import matsyir.pvpperformancetracker.models.CombatLevels;
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;
import net.runelite.api.ItemID;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import matsyir.pvpperformancetracker.models.EquipmentData;
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class PeteKayerTest
{
    @Test
    public void arenaIncludesScreenshotTileButExcludesOtherRegionsAndPlanes()
    {
        assertTrue(PeteKayer.isArena(new WorldPoint(2783, 5920, 0)));
        assertTrue(PeteKayer.isArena(new WorldPoint(2752, 5888, 0)));
        assertTrue(PeteKayer.isArena(new WorldPoint(2815, 5951, 0)));
        assertFalse(PeteKayer.isArena(new WorldPoint(2816, 5920, 0)));
        assertFalse(PeteKayer.isArena(new WorldPoint(2783, 5887, 0)));
        assertFalse(PeteKayer.isArena(new WorldPoint(2783, 5920, 1)));
        assertFalse(PeteKayer.isArena((WorldPoint) null));
    }

    @Test
    public void allAttackableFormsAreRecognizedWithoutTreatingLobbyPeteAsAnOpponent()
    {
        for (int id = 16579; id <= 16597; id++)
        {
            if (id == 16587) continue;
            assertNotNull("Missing Pete form " + id, PeteKayer.bonuses(id));
        }
        for (int id : new int[] {16576, 16577, 16578, 16587, 16598, 16607})
            assertNull(PeteKayer.bonuses(id));
    }

    @Test
    public void sameNpcStaysRecognizedWhenItsSetupChanges()
    {
        AtomicInteger id = new AtomicInteger(16579);
        NPC pete = npc(id, null, null);
        assertTrue(PeteKayer.isPete(pete));
        assertEquals(140, PeteKayer.bonuses(id.get())[3]);
        id.set(16582);
        assertTrue(PeteKayer.isPete(pete));
        assertEquals(207, PeteKayer.bonuses(id.get())[4]);
        assertEquals(131, PeteKayer.bonuses(id.get())[11]);
        id.set(16597);
        assertTrue(PeteKayer.isPete(pete));
    }

    @Test
    public void levelsUseNpcCacheOrderAndDoNotShareMutableState()
    {
        CombatLevels levels = PeteKayer.levels();
        assertEquals(118, levels.atk);
        assertEquals(118, levels.str);
        assertEquals(120, levels.def);
        assertEquals(112, levels.range);
        assertEquals(99, levels.mage);
        assertEquals(115, levels.hp);
        levels.def = 1;
        assertEquals(120, PeteKayer.levels().def);
    }

    @Test
    public void overheadReadsPrayerSpritesAndIgnoresOtherArchives()
    {
        assertEquals(HeadIcon.MAGIC, PeteKayer.overhead(npc(new AtomicInteger(16579), new int[] {440}, new short[] {2})));
        assertNull(PeteKayer.overhead(npc(new AtomicInteger(16579), new int[] {999}, new short[] {2})));
        assertNull(PeteKayer.overhead(npc(new AtomicInteger(16579), null, null)));
    }

    @Test
    public void liveSwitchesProduceFreshGearSnapshotsAndRemoveTheShieldForHalberd()
    {
        AtomicInteger id = new AtomicInteger(16582);
        NPC pete = npc(id, null, null);
        int[] ranged = PeteKayer.equipment(pete);
        assertEquals(EquipmentData.ZARYTE_CROSSBOW.getItemId(), item(ranged, KitType.WEAPON));
        assertEquals(EquipmentData.DRAGONFIRE_SHIELD.getItemId(), item(ranged, KitType.SHIELD));
        assertEquals(ItemID.AMULET_OF_FURY, item(ranged, KitType.AMULET));
        assertEquals(34633, item(ranged, KitType.CAPE));
        assertEquals(Integer.valueOf(RangeAmmoData.StrongBoltAmmo.OPAL_DRAGON_BOLTS_E.getItemId()), PeteKayer.ammoItemId(pete));
        id.set(16581);
        int[] melee = PeteKayer.equipment(pete);
        assertEquals(EquipmentData.NOXIOUS_HALBERD.getItemId(), item(melee, KitType.WEAPON));
        assertEquals(0, melee[KitType.SHIELD.getIndex()]);
        assertNull(PeteKayer.ammoItemId(pete));
        // Previously captured attacks must keep their gear after Pete switches.
        assertEquals(EquipmentData.ZARYTE_CROSSBOW.getItemId(), item(ranged, KitType.WEAPON));
        id.set(16583);
        assertEquals(EquipmentData.STAFF_OF_THE_DEAD.getItemId(), item(PeteKayer.equipment(pete), KitType.WEAPON));
        id.set(16591);
        int[] claws = PeteKayer.equipment(pete);
        assertEquals(EquipmentData.DRAGON_CLAWS.getItemId(), item(claws, KitType.WEAPON));
        assertEquals(0, claws[KitType.SHIELD.getIndex()]);
        assertEquals(EquipmentData.IMBUED_SARADOMIN_CAPE.getItemId(), item(claws, KitType.CAPE));
        assertEquals(EquipmentData.STAFF_OF_THE_DEAD.getItemId(), item(PeteKayer.equipment(16583), KitType.WEAPON));
    }

    @Test
    public void magicAndMixedSetupsKeepTorsoAndLegSwitchesIndependent()
    {
        int[] magic = PeteKayer.equipment(16579);
        int[] mixed = PeteKayer.equipment(16586);
        assertEquals(EquipmentData.VIRTUS_ROBE_TOP.getItemId(), item(magic, KitType.TORSO));
        assertEquals(EquipmentData.VIRTUS_ROBE_BOTTOM.getItemId(), item(magic, KitType.LEGS));
        assertEquals(EquipmentData.MASORI_CHAPS_F.getItemId(), item(mixed, KitType.LEGS));
        assertEquals(EquipmentData.ELIDINIS_WARD_F.getItemId(), item(mixed, KitType.SHIELD));
        assertEquals(34633, item(magic, KitType.CAPE));
        assertNull(PeteKayer.equipment(16587));
    }

    @Test
    public void allLoadoutsAndMissingBonusEstimatesMatchThePinnedCacheEvidence() throws Exception
    {
        try (InputStreamReader reader = new InputStreamReader(
            getClass().getResourceAsStream("/pete-cache-loadouts.json"), StandardCharsets.UTF_8))
        {
            JsonObject loadouts = new JsonParser().parse(reader).getAsJsonObject().getAsJsonObject("loadouts");
            assertEquals(18, loadouts.size());
            for (Map.Entry<String, JsonElement> entry : loadouts.entrySet())
            {
                int id = Integer.parseInt(entry.getKey());
                JsonObject expected = entry.getValue().getAsJsonObject();
                int[] gear = PeteKayer.equipment(id);
                for (Map.Entry<String, JsonElement> slot : expected.getAsJsonObject("equipment").entrySet())
                {
                    int itemId = slot.getValue().getAsInt();
                    assertEquals("NPC " + id + " slot " + slot.getKey(),
                        itemId == 0 ? 0 : itemId + PlayerComposition.ITEM_OFFSET,
                        gear[Integer.parseInt(slot.getKey())]);
                }
                int[] reported = new int[13];
                for (int i = 0; i < reported.length; i++)
                    reported[i] = expected.getAsJsonArray("reportedBonuses").get(i).getAsInt();
                assertArrayEquals("Reported NPC stats " + id, reported, PeteKayer.bonuses(id));
                int[] merged = PeteKayer.combatBonuses(id);
                for (int i = 0; i < reported.length; i++)
                {
                    int value = i == 9 || (i >= 10 && reported[i] == 0)
                        ? expected.getAsJsonArray("wornBonuses").get(i).getAsInt() : reported[i];
                    assertEquals("NPC " + id + " bonus " + i, value, merged[i]);
                }
                // Captured calculations and equipment must survive later mutations.
                gear[0] = 0;
                merged[9] = -999;
                assertTrue(PeteKayer.equipment(id)[0] > PlayerComposition.ITEM_OFFSET);
                assertTrue(PeteKayer.combatBonuses(id)[9] >= 0);
                if (id >= 16588) assertNull(PeteKayer.ammoItemId(id));
            }
        }
    }

    private static int item(int[] gear, KitType slot)
    {
        return gear[slot.getIndex()] - PlayerComposition.ITEM_OFFSET;
    }

    private static NPC npc(AtomicInteger id, int[] archives, short[] sprites)
    {
        return (NPC) Proxy.newProxyInstance(NPC.class.getClassLoader(), new Class<?>[] {NPC.class}, (proxy, method, args) -> {
            switch (method.getName())
            {
                case "getId": return id.get();
                case "getOverheadArchiveIds": return archives;
                case "getOverheadSpriteIds": return sprites;
                case "getName": return "Pete Kayer";
                default: return null;
            }
        });
    }
}
