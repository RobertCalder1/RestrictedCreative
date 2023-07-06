package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.EntityHandler;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EntityDamageListener implements Listener {
    private final RestrictedCreative main;

    public EntityDamageListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    /*
     * Called when an entity is damaged by an entity
     */
    // HIGHEST required for WorldGuard and similar plugins
    // BEWARE - it was changed to HIGH in 2.3, but PlotSquared needs HIGHEST
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        Entity en = e.getEntity();

        // No need to control entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(en.getWorld().getName()))
            return;

        // Item frame
        if (en instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame) en;
            ItemStack is = frame.getItem();

            // The item isn't going to pop off
            if (is.getType() == Material.AIR)
                return;

            // Item frame doesn't contain creative items
            if (!EntityHandler.hasTrackedItem(frame))
                return;

            if (RestrictedCreative.DEBUG)
                System.out.println("removeItem: " + is.getType());

            EntityHandler.removeItem(frame);
        }

        // Armor stand
        if (en instanceof ArmorStand) {
            ArmorStand a = (ArmorStand) en;

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!EntityHandler.isTrackedSlot(a, slot))
                    continue;

                EntityEquipment inv = a.getEquipment();
                if (inv == null)
                    continue;

                ItemStack air = new ItemStack(Material.AIR);

                switch (slot) {
                    case CHEST:
                        inv.setChestplate(air);
                        break;
                    case FEET:
                        inv.setBoots(air);
                        break;
                    case HEAD:
                        inv.setHelmet(air);
                        break;
                    case LEGS:
                        inv.setLeggings(air);
                        break;
                    case HAND:
                        inv.setItemInMainHand(air);
                        break;
                    case OFF_HAND:
                        inv.setItemInOffHand(air);
                        break;
                    default:
                        break;
                }

                if (RestrictedCreative.DEBUG)
                    System.out.println("removeSlotTracking: " + slot);

                EntityHandler.removeSlotTracking(a, slot);
            }
        }

        // PVP and PVE
        if (e.getDamager() instanceof Player) {
            Player p = (Player) e.getDamager();

            // Only need to control creative players
            if (p.getGameMode() == GameMode.CREATIVE) {
                // PVE except armor stands
                if (en instanceof LivingEntity && !(en instanceof ArmorStand)) {
                    // Only need to control enabled features and non-bypassed players
                    if (!getMain().getSettings().isEnabled("limit.combat.pve")
                            && !(p.hasPermission("rc.bypass.limit.combat.pve") || p
                            .hasPermission("rc.bypass.limit.combat.pve." + en.getType()))) {
                        if (RestrictedCreative.DEBUG)
                            System.out.println("PVE: " + en.getType());

                        e.setCancelled(true);
                        getMain().getUtils().sendMessage(p, true, "disabled.general");
                        return;
                    }
                }

                // PVP
                else if (en instanceof Player) {
                    // Only need to control enabled features and non-bypassed players
                    if (!getMain().getSettings().isEnabled("limit.combat.pvp")
                            && !p.hasPermission("rc.bypass.limit.combat.pvp")) {
                        if (RestrictedCreative.DEBUG)
                            System.out.println("PVP: " + en.getName());

                        e.setCancelled(true);
                        getMain().getUtils().sendMessage(p, true, "disabled.general");
                        return;
                    }
                }
            }
        }

        // No need to control non-tracked entities
        if (!EntityHandler.isTracked(en))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onEntityDamage: " + en.getType());

        // Remove armor stands etc.
        en.remove();
        e.setCancelled(true);
    }

    /*
     * Raised when a vehicle is destroyed, which could be caused by either a player
     * or the environment. This is not raised if the boat is simply 'removed' due to
     * other means.
     */
    // HIGHEST required for PlotSquared and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onVehicleDestory(VehicleDestroyEvent e) {
        Entity en = e.getVehicle();

        // No need to control entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(en.getWorld().getName()))
            return;

        // No need to control non-tracked entities
        if (!EntityHandler.isTracked(en))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onVehicleDestroy: " + en.getType());

        // Remove boats, carts etc.
        en.remove();
        e.setCancelled(true);
    }

    /*
     * Triggered when a hanging entity is removed
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onHangingBreak(HangingBreakEvent e) {
        Entity en = e.getEntity();

        // No need to control entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(en.getWorld().getName()))
            return;

        if (!EntityHandler.isTracked(en)) {
            if (en instanceof ItemFrame) {
                ItemFrame frame = (ItemFrame) en;

                if (EntityHandler.hasTrackedItem(frame)) {
                    en.remove();
                    e.setCancelled(true);

                    if (RestrictedCreative.DEBUG)
                        System.out.println("removeTrackedItem: " + frame.getItem().getType());
                }
            }

            return;
        } else {
            if (en instanceof ItemFrame) {
                ItemFrame frame = (ItemFrame) en;

                if (!frame.getItem().getType().isAir() && !EntityHandler.hasTrackedItem(frame)) {
                    // Drop item frame content in the ground
                    en.getWorld().dropItem(en.getLocation(), frame.getItem());
                    en.remove();
                }
            }
        }

        if (RestrictedCreative.DEBUG)
            System.out.println("onHangingBreak: " + en.getType());

        en.remove();
        e.setCancelled(true);
    }
}