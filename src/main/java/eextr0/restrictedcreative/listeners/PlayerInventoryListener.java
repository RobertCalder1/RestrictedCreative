package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class PlayerInventoryListener implements Listener {
    private final RestrictedCreative main;

    public PlayerInventoryListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    /*
     * This event is called when a player in creative mode puts down or picks up an
     * item in their inventory / hotbar and when they drop items from their
     * Inventory while in creative mode.
     *
     * e.getAction() is always PLACE_ALL for some reason
     *
     * e.getClick() is always CREATIVE
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryCreative(InventoryCreativeEvent e) {
        Player p = (Player) e.getWhoClicked();
        Inventory i = e.getClickedInventory();
        ItemStack is = e.getCursor();

        // No need to control inventories in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onInventoryCreative: " + is.getType());

        // Creative armor
        if (getMain().getSettings().isEnabled("creative.armor.enabled")
                && !p.hasPermission("rc.bypass.creative.armor")) {
            // Armor exists check
            if (!armorIsEquipped(p.getInventory().getArmorContents())) {
                getMain().getUtils().equipArmor(p);

                if (RestrictedCreative.DEBUG)
                    System.out.println("armorEquipped: " + e.getSlotType());

                getMain().getUtils().sendMessage(p, true, "disabled.general");
                e.setResult(Event.Result.DENY);
                return;
            }
        }

        // Middle-click check
        if (getMain().getSettings().isEnabled("confiscate.middle-click.enabled")
                && !p.hasPermission("rc.bypass.confiscate.middle-click")) {
            Material m = is.getType();

            // If it's a block (not an item) and it's not excluded
            if (m.isBlock() && !getMain().getUtils().isExcludedFromConfiscating(m)) {
                if (RestrictedCreative.DEBUG)
                    System.out.println("middleClick: " + is.getType());

                // Replaces it with a new one, but without NBT data
                e.setCursor(new ItemStack(is.getType(), is.getAmount()));
            }
        }

        if (i != null) {
            InventoryHolder h = i.getHolder();

            // Check if clicked inventory isn't player's own inventory
            if (i.getType() != InventoryType.PLAYER && (h instanceof Player)
                    && ((Player) h).getName().equalsIgnoreCase(p.getName()))
                return;
        }

        // Checks if cursor item exists
        // Must be after middle-click check, because "is" needs specific cursor itemstack
        if (is.getType() == Material.AIR)
            is = e.getCurrentItem();

        if (is == null || is.getType() == Material.AIR)
            return;

        // Confiscate
        if (getMain().getUtils().shouldConfiscate(p, is)) {
            e.setCurrentItem(new ItemStack(Material.AIR));
            e.setCursor(new ItemStack(Material.AIR));
            e.setResult(Event.Result.DENY);
            e.setCancelled(true);

            if (RestrictedCreative.DEBUG)
                System.out.println("confiscate: " + is.getType());
        }
    }

    /*
     * Represents a player related inventory event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        Player p = (Player) e.getPlayer();

        // No need to control inventories in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to control non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // No need to control disabled features
        if (!getMain().getSettings().isEnabled("limit.interact.inventories"))
            return;

        // No need to control bypassed players
        if (p.hasPermission("rc.bypass.limit.interact.inventories")
                || p.hasPermission("rc.bypass.limit.interact.inventories." + e.getView().getType()))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onInventoryOpen: " + e.getInventory().getType());

        e.setCancelled(true);
        getMain().getUtils().sendMessage(p, true, "disabled.general");
    }

    private boolean armorIsEquipped(ItemStack[] armorContents) {
        for (ItemStack is : armorContents)
            if (is == null)
                return false;

        return true;
    }
}