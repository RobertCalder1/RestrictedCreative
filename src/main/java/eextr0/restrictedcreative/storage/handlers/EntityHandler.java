package eextr0.restrictedcreative.storage.handlers;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EntityHandler {
    private static final Set<String> trackedLocs = new HashSet<>();

    public static boolean isTracked(Entity e) {
        if (e == null)
            return false;

        if (RestrictedCreative.DEBUG)
            System.out.println("isTracked: " + e.getType() + " "
                    + Arrays.toString(e.getScoreboardTags().toArray()));

        return e.getScoreboardTags().contains("GMC");
    }

    public static void setAsTracked(Entity e) {
        if (e == null)
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("setAsTracked: " + e.getType());

        e.addScoreboardTag("GMC");
    }

    public static void removeTracking(Entity e) {
        if (e == null)
            return;

        e.removeScoreboardTag("GMC");
    }

    public static boolean hasTrackedItem(ItemFrame frame) {
        if (frame == null)
            return false;

        return frame.getScoreboardTags().contains("GMC_IF");
    }

    public static void setAsTrackedItem(ItemFrame frame) {
        if (frame == null)
            return;

        frame.addScoreboardTag("GMC_IF");
    }

    public static void removeItemTracking(ItemFrame frame) {
        if (frame == null)
            return;

        frame.removeScoreboardTag("GMC_IF");
    }

    public static void removeItem(ItemFrame frame) {
        if (frame == null)
            return;

        frame.setItem(new ItemStack(Material.AIR));
        removeItemTracking(frame);
    }

    public static boolean isTrackedSlot(ArmorStand stand, EquipmentSlot slot) {
        if (stand == null || slot == null)
            return false;

        return stand.getScoreboardTags().contains("GMC_AS_" + slot);
    }

    public static void setAsTrackedSlot(ArmorStand stand, EquipmentSlot slot) {
        if (stand == null)
            return;

        stand.addScoreboardTag("GMC_AS_" + slot);
    }

    public static void removeSlotTracking(ArmorStand stand, EquipmentSlot slot) {
        if (stand == null)
            return;

        stand.removeScoreboardTag("GMC_AS_" + slot);
    }

    public static boolean isTrackedLoc(Location loc) {
        return trackedLocs.contains(Utils.getLocString(loc));
    }

    public static void addToTrackedLocs(Location loc) {
        if (RestrictedCreative.DEBUG)
            System.out.println("addToTrackedLocs: " + Utils.getLocString(loc));

        trackedLocs.add(Utils.getLocString(loc));
    }

    public static void removeFromTrackedLocs(Location loc) {
        trackedLocs.remove(Utils.getLocString(loc));
    }
}
