package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerItemListener implements Listener {
    private RestrictedCreative main;

    public PlayerItemListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    /*
     * Called when a player empties a bucket
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlockClicked().getRelative(e.getBlockFace());

        // No need to control buckets in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // Waterloggable blocks don't drop when filled with water
        if (e.getBlockClicked().getBlockData() instanceof Waterlogged)
            return;

        Material m = b.getType();

        // No need to control excluded blocks
        if (getMain().getUtils().isExcludedFromTracking(m))
            return;

        // No need to control bypassed players
        if (p.hasPermission("rc.bypass.tracking.blocks") || p.hasPermission("rc.bypass.tracking.blocks." + m))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onPlayerBucketEmpty: " + m);

        // This prevents players from pouring liquids directly onto tracked items
        // and thus getting the drops
        if (BlockHandler.isTracked(b))
            e.setCancelled(true);
    }

    /*
     * Thrown when a player drops an item from their inventory
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        Player p = e.getPlayer();

        // No need to control drops in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to control non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // No need to control disabled features
        if (!getMain().getSettings().isEnabled("limit.item.drop"))
            return;

        Material m = e.getItemDrop().getItemStack().getType();

        // No need to control bypassed players
        if (p.hasPermission("rc.bypass.limit.item.drop") || p.hasPermission("rc.bypass.limit.item.drop." + m))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onPlayerDropItem: " + m);

        e.setCancelled(true);
        p.updateInventory();
        getMain().getUtils().sendMessage(p, true, "disabled.general");
    }

    /*
     * Thrown when a entity picks an item up from the ground
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player))
            return;

        Player p = (Player) e.getEntity();

        // No need to control pickups in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to control non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // No need to control disabled features
        if (!getMain().getSettings().isEnabled("limit.item.pickup"))
            return;

        Material m = e.getItem().getItemStack().getType();

        // No need to control bypassed players
        if (p.hasPermission("rc.bypass.limit.item.pickup") || p.hasPermission("rc.bypass.limit.item.pickup." + m))
            return;

        if (RestrictedCreative.DEBUG && RestrictedCreative.EXTRADEBUG)
            System.out.println("onPlayerPickupItem: " + m);

        e.setCancelled(true);
    }
}
