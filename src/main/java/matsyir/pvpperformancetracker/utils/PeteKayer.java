package matsyir.pvpperformancetracker.utils;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.ItemID;
import matsyir.pvpperformancetracker.models.RangeAmmoData;
import net.runelite.api.coords.WorldPoint;
import matsyir.pvpperformancetracker.models.CombatLevels;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.EquipmentData;
import net.runelite.api.kit.KitType;

/** Pete support is deliberately limited to the supplied arena and attackable cache forms. */
public final class PeteKayer
{
    public static final int REGION = 11100;
    private PeteKayer() {}

    public static boolean isArena(Client client)
    {
        if (client.getLocalPlayer() == null) return false;
        WorldPoint point = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
        return isArena(point);
    }

    public static boolean isArena(WorldPoint point)
    {
        return point != null && point.getRegionID() == REGION && point.getPlane() == 0;
    }

    public static boolean isPete(Actor actor)
    {
        return actor instanceof NPC && bonuses(((NPC) actor).getId()) != null;
    }

    public static boolean canTrack(Client client, Actor actor)
    {
        return isPete(actor) && isArena(client);
    }

    // Cache stats order: attack, defence, strength, hitpoints, ranged, magic.
    public static CombatLevels levels()
    {
        return new CombatLevels(118, 118, 120, 112, 99, 115);
    }

    // Reported NPC combat parameters in the calculator's bonus order; omitted values are zero.
    public static int[] bonuses(int id)
    {
        switch (id)
        {
            case 16579: return new int[] {0, 0, 0, 140, 0, 241, 237, 283, 116, 0, 0, 0, 0};
            case 16580: return new int[] {0, 107, 0, 0, 0, 274, 273, 292, 176, 0, 121, 0, 0};
            case 16581: return new int[] {0, 159, 0, 0, 0, 204, 197, 220, 164, 0, 176, 0, 0};
            case 16582: return new int[] {0, 0, 0, 0, 207, 288, 286, 304, 189, 0, 0, 131, 0};
            case 16583: return new int[] {82, 0, 0, 0, 0, 274, 275, 295, 191, 0, 0, 0, 0};
            case 16584: return new int[] {0, 107, 0, 0, 0, 270, 271, 287, 152, 0, 121, 0, 0};
            case 16585: return new int[] {0, 0, 0, 0, 153, 276, 270, 296, 146, 0, 0, 127, 0};
            case 16586: return new int[] {0, 0, 0, 112, 0, 245, 239, 288, 140, 0, 0, 0, 0};
            case 16588: return new int[] {77, 92, 22, 80, 21, 130, 134, 158, 93, 0, 99, 122, 17};
            case 16589: return new int[] {22, 22, 22, -8, 138, 262, 255, 288, 106, 0, 27, 122, 2};
            case 16590: return new int[] {47, 136, 45, -11, 42, 234, 224, 238, 101, 0, 119, 122, 2};
            case 16591: return new int[] {63, 79, 18, -8, 44, 222, 226, 222, 104, 0, 83, 122, 2};
            case 16592: return new int[] {77, 92, 22, 45, 51, 177, 176, 208, 138, 0, 99, 122, 17};
            case 16593: return new int[] {102, 116, 45, 6, 42, 234, 227, 241, 118, 0, 105, 122, 17};
            case 16594: return new int[] {22, 112, 22, -8, 44, 262, 255, 288, 106, 0, 113, 122, 2};
            case 16595: return new int[] {47, 46, 45, -11, 136, 234, 224, 238, 101, 0, 33, 122, 2};
            case 16596: return new int[] {77, 92, 22, 9, 44, 262, 258, 291, 123, 0, 99, 122, 17};
            case 16597: return new int[] {77, 92, 22, 80, 21, 130, 134, 158, 93, 0, 99, 122, 17};
            default: return null;
        }
    }

    public static HeadIcon overhead(Actor actor)
    {
        if (actor instanceof Player) return ((Player) actor).getOverheadIcon();
        if (actor instanceof NPC)
        {
            short[] sprites = ((NPC) actor).getOverheadSpriteIds();
            int[] archives = ((NPC) actor).getOverheadArchiveIds();
            if (sprites != null && archives != null)
                for (int i = 0; i < Math.min(sprites.length, archives.length); i++)
                    if (archives[i] == 440 && sprites[i] >= 0 && sprites[i] <= 2)
                        return HeadIcon.values()[sprites[i]];
        }
        return null;
    }

    /**
     * Reference items for the worn meshes in OpenRS2 cache 2735 (build 241).
     * Duplicate/charged/degraded variants are not distinguishable by appearance.
     * Max treads remain the supplied kit assumption; recolors do not change item stats.
     * See docs/pete-cache/README.md for provenance and the per-form color differences.
     */
    public static int[] equipment(int npcId)
    {
        if (bonuses(npcId) == null) return null;
        boolean highGear = npcId <= 16586;
        int[] gear = new int[12];
        equip(gear, KitType.HEAD, highGear ? ItemID.TORVA_FULL_HELM : EquipmentData.HELM_OF_NEITIZNOT.getItemId());
        equip(gear, KitType.CAPE, highGear ? EquipmentData.IMBUED_ANCIENT_CAPE.getItemId() : EquipmentData.IMBUED_SARADOMIN_CAPE.getItemId());
        equip(gear, KitType.AMULET, EquipmentData.AMULET_OF_FURY.getItemId());
        equip(gear, KitType.HANDS, EquipmentData.BARROWS_GLOVES.getItemId());
        equip(gear, KitType.BOOTS, highGear ? EquipmentData.AVERNIC_TREADS_MAX.getItemId() : EquipmentData.DRAGON_BOOTS.getItemId());
        if (!highGear)
        {
            equip(gear, KitType.TORSO, (npcId == 16588 || npcId == 16597)
                ? EquipmentData.MYSTIC_ROBE_TOP.getItemId() : EquipmentData.KARILS_TOP.getItemId());
            equip(gear, KitType.LEGS, (npcId == 16588 || npcId == 16592 || npcId == 16597)
                ? EquipmentData.MYSTIC_ROBE_BOTTOM.getItemId() : EquipmentData.VERACS_PLATESKIRT.getItemId());
            EquipmentData weapon;
            switch (npcId)
            {
                case 16589:
                case 16595: weapon = EquipmentData.DRAGON_CROSSBOW; break;
                case 16590:
                case 16594: weapon = EquipmentData.ABYSSAL_TENTACLE; break;
                case 16591: weapon = EquipmentData.DRAGON_CLAWS; break;
                default: weapon = EquipmentData.STAFF_OF_THE_DEAD; break;
            }
            equip(gear, KitType.WEAPON, weapon.getItemId());
            if (weapon != EquipmentData.DRAGON_CLAWS)
                equip(gear, KitType.SHIELD, (npcId == 16590 || npcId == 16593 || npcId == 16595)
                    ? EquipmentData.DRAGON_DEFENDER.getItemId() : EquipmentData.BLESSED_SPIRIT_SHIELD.getItemId());
            return gear;
        }
        equip(gear, KitType.TORSO, (npcId == 16579 || npcId == 16585 || npcId == 16586)
            ? EquipmentData.VIRTUS_ROBE_TOP.getItemId() : EquipmentData.MASORI_BODY_F.getItemId());
        equip(gear, KitType.LEGS, (npcId == 16579 || npcId == 16584)
            ? EquipmentData.VIRTUS_ROBE_BOTTOM.getItemId() : EquipmentData.MASORI_CHAPS_F.getItemId());
        EquipmentData weapon;
        switch (npcId)
        {
            case 16580:
            case 16584: weapon = EquipmentData.VOIDWAKER; break;
            case 16581: weapon = EquipmentData.NOXIOUS_HALBERD; break;
            case 16582:
            case 16585: weapon = EquipmentData.ZARYTE_CROSSBOW; break;
            default: weapon = EquipmentData.STAFF_OF_THE_DEAD; break;
        }
        equip(gear, KitType.WEAPON, weapon.getItemId());
        if (weapon != EquipmentData.NOXIOUS_HALBERD)
            equip(gear, KitType.SHIELD, (npcId == 16579 || npcId == 16586)
                ? EquipmentData.ELIDINIS_WARD_F.getItemId() : EquipmentData.DRAGONFIRE_SHIELD.getItemId());
        // Ring/ammo are recorded separately, not inferred from worn models.
        return gear;
    }

    private static void equip(int[] gear, KitType slot, int itemId)
    {
        gear[slot.getIndex()] = itemId + PlayerComposition.ITEM_OFFSET;
    }

    public static Integer ammoItemId(int npcId)
    {
        return npcId == 16582 || npcId == 16585
            ? RangeAmmoData.StrongBoltAmmo.OPAL_DRAGON_BOLTS_E.getItemId() : null;
    }

    public static Integer ammoItemId(Actor actor)
    {
        return actor instanceof NPC ? ammoItemId(((NPC) actor).getId()) : null;
    }

    /**
     * Preserve reported NPC bonuses. Missing fields are estimates from the reference
     * items' cache parameters, not proof that Pete uses player equipment mechanics.
     * Pinned values avoid silently losing the new cape when ItemManager lacks its stats.
     */
    public static int[] combatBonuses(int npcId)
    {
        int[] reported = bonuses(npcId);
        int[] worn = missingBonusEstimates(npcId);
        if (reported == null || worn == null) return reported;
        // These fields are omitted by the cache report on the relevant forms.
        reported[9] = worn[0];
        if (reported[10] == 0) reported[10] = worn[1];
        if (reported[11] == 0) reported[11] = worn[2];
        if (reported[12] == 0) reported[12] = worn[3];
        return reported;
    }

    // Ranged defence, strength, ranged strength (without ammo), magic damage percent.
    // Item params: 9, 10, 189, 299/10. Reference gear: pete-cache-loadouts.json.
    private static int[] missingBonusEstimates(int npcId)
    {
        switch (npcId)
        {
            case 16579: return new int[] {146, 106, 3, 28};
            case 16580: return new int[] {213, 121, 9, 4};
            case 16581: return new int[] {191, 176, 9, 4};
            case 16582: return new int[] {229, 41, 9, 4};
            case 16583: return new int[] {213, 113, 9, 19};
            case 16584: return new int[] {176, 121, 7, 6};
            case 16585: return new int[] {169, 41, 5, 6};
            case 16586: return new int[] {183, 106, 5, 26};
            case 16588: return new int[] {109, 99, 0, 17};
            case 16589: return new int[] {250, 27, 0, 2};
            case 16590: return new int[] {196, 119, 0, 2};
            case 16591: return new int[] {198, 83, 0, 2};
            case 16592: return new int[] {166, 99, 0, 17};
            case 16593: return new int[] {196, 105, 0, 17};
            case 16594: return new int[] {250, 113, 0, 2};
            case 16595: return new int[] {196, 33, 0, 2};
            case 16596: return new int[] {250, 99, 0, 17};
            case 16597: return new int[] {109, 99, 0, 17};
            default: return null;
        }
    }

    public static int[] equipment(Actor actor)
    {
        if (actor instanceof Player) return ((Player) actor).getPlayerComposition().getEquipmentIds();
        if (actor instanceof NPC)
        {
            int[] known = equipment(((NPC) actor).getId());
            if (known != null) return known;
        }
        int[] gear = new int[12];
        // Inventory and worn item IDs are unknown. Only distinctive special animations
        // already identified by this plugin can establish a weapon.
        AnimationData animation =
            AnimationData.fromId(actor.getAnimation());
        EquipmentData weapon = null;
        if (animation != null)
        {
            switch (animation)
            {
                case MELEE_ARMADYL_GODSWORD_SPEC:
                case MELEE_ARMADYL_GODSWORD_OR_SPEC:
                    weapon = EquipmentData.ARMADYL_GODSWORD; break;
                case MELEE_ANCIENT_GODSWORD_SPEC:
                    weapon = EquipmentData.ANCIENT_GODSWORD; break;
                case MELEE_VOIDWAKER_SPEC:
                    weapon = EquipmentData.VOIDWAKER; break;
                case MELEE_DRAGON_CLAWS_SPEC:
                    weapon = EquipmentData.DRAGON_CLAWS; break;
                case MELEE_GRANITE_MAUL_SPEC:
                    weapon = EquipmentData.GRANITE_MAUL; break;
                default: break;
            }
        }
        if (weapon != null) gear[KitType.WEAPON.getIndex()] =
            weapon.getItemId() + PlayerComposition.ITEM_OFFSET;
        return gear;
    }
}
