package matsyir.pvpperformancetracker.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.api.gameval.SpriteID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FracturedArchiveSupportTest
{
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Test
    public void recognizesPrayerAndAllWeaponAnimations()
    {
        assertTrue(AnimationData.AttackStyle.CRUSH.isUsingSuccessfulOffensivePray(SpriteID.Prayeron.ZEAL));
        assertFalse(AnimationData.AttackStyle.RANGED.isUsingSuccessfulOffensivePray(SpriteID.Prayeron.ZEAL));
        for (int animation : new int[] {15108, 15113, 15114, 15115, 15116, 15122, 15123, 15130, 15131, 15139, 15140, 15146, 15147})
            assertNotNull("Missing animation " + animation, AnimationData.fromId(animation));
        assertEquals(EquipmentData.ASCENSION_CROSSBOWS, EquipmentData.fromId(34681));
        assertEquals(EquipmentData.OBLIGATOR, EquipmentData.fromId(34715));
        assertEquals(EquipmentData.ZORYAS_TOME, EquipmentData.fromId(34725));
        assertEquals(EquipmentData.RONDACHE, EquipmentData.fromId(34719));
    }

    @Test
    public void serializesKnownAbsenceAndLeavesLegacyStateUnknown()
    {
        FightLogEntry absent = GSON.fromJson("{\"ef\":0,\"rc\":0,\"zc\":0,\"ms\":\"MAGIC_BLOOD_BARRAGE\"}", FightLogEntry.class);
        assertEquals(Integer.valueOf(0), absent.getElementalFragments());
        String json = GSON.toJson(absent);
        assertTrue(json.contains("\"ef\":0"));
        assertTrue(json.contains("\"rc\":0"));
        assertTrue(json.contains("\"zc\":0"));
        assertTrue(json.contains("\"ms\":\"MAGIC_BLOOD_BARRAGE\""));
        FightLogEntry legacy = GSON.fromJson("{}", FightLogEntry.class);
        assertNull(legacy.getElementalFragments());
        assertNull(legacy.getRondacheCharges());
        assertNull(legacy.getZoryaCharges());
    }
}
