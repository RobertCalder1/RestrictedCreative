package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.EntityHandler;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

public class EntityCreateListener implements Listener {
    private RestrictedCreative main;

    public EntityCreateListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    /*
     * Called when a creature is spawned into a world.
     *
     * If a Creature Spawn event is cancelled, the creature will not spawn.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        Entity en = e.getEntity();

        // No need to track entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(en.getWorld().getName()))
            return;

        Location loc = e.getLocation();

        // No need to track non-tracked entities
        if (!EntityHandler.isTrackedLoc(loc))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onCreatureSpawn: " + en.getType());

        EntityHandler.removeFromTrackedLocs(loc);
        EntityHandler.setAsTracked(en);
    }

    /*
     * Raised when a vehicle is created.
     */
    @EventHandler(ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent e) {
        Entity en = e.getVehicle();

        // No need to control entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(en.getWorld().getName()))
            return;

        Location loc = en.getLocation();

        // No need to track non-tracked entities
        if (!EntityHandler.isTrackedLoc(loc))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onVehicleCreate: " + en.getType());

        EntityHandler.removeFromTrackedLocs(loc);
        EntityHandler.setAsTracked(en);
    }

    /*
     * Triggered when a hanging entity is created in the world
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onHangingPlace(HangingPlaceEvent e) {
        Player p = e.getPlayer();
        Entity en = e.getEntity();
        EntityType et = en.getType();

        // No need to track entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(en.getWorld().getName()))
            return;

        // No need to track disabled features
        if (getMain().getUtils().isTrackingDisabled())
            return;

        // No need to track non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // No need to track bypassed players
        if (p.hasPermission("rc.bypass.tracking.entities") || p.hasPermission("rc.bypass.tracking.entities." + et))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onHangingPlace: " + et);

        EntityHandler.setAsTracked(en);
    }

    /*
     * Called when a projectile is launched.
     */
    @EventHandler(ignoreCancelled = true)
    public void onShoot(ProjectileLaunchEvent e) {
        Projectile en = e.getEntity();

        // No need to control entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(e.getEntity().getWorld().getName()))
            return;

        // No need to control disabled features
        if (!getMain().getSettings().isEnabled("limit.item.throw"))
            return;

        // No need to control non-players
        if (!(en.getShooter() instanceof Player))
            return;

        Player p = (Player) en.getShooter();
        EntityType et = en.getType();

        // No need to track non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // No need to track bypassed players
        if (p.hasPermission("rc.bypass.limit.item.throw") || p.hasPermission("rc.bypass.limit.item.throw." + et))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onShoot: " + et);

        e.setCancelled(true);
        getMain().getUtils().sendMessage(p, true, "disabled.general");
    }
}
