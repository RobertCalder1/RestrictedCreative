package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import eextr0.restrictedcreative.storage.handlers.CommandHandler;
import eextr0.restrictedcreative.storage.handlers.InventoryHandler;
import eextr0.restrictedcreative.storage.handlers.PermissionHandler;
import org.bukkit.GameMode;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerMiscListener implements Listener {
    private final RestrictedCreative main;

    public PlayerMiscListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();

        // No need to control gamemode changes in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        if (e.getNewGameMode() == p.getGameMode())
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println(
                    "onPlayerGameModeChange: " + p.getGameMode() + " -> " + e.getNewGameMode());

        // Player wants to switch into creative mode
        if (e.getNewGameMode() == GameMode.CREATIVE) {
            // Player height check
            if (!getMain().getUtils().isHeightOk(p)) {
                getMain().getUtils().sendMessage(p, true, "disabled.region");
                e.setCancelled(true);
                return;
            }

            // Prevents opening a container, switching to creative mode, and dumping items
            if (illegalContainerOpened(p)) {
                getMain().getUtils().sendMessage(p, true, "disabled.general");
                e.setCancelled(true);
                return;
            }

            // Switch inventories, permissions etc
            getMain().getUtils().setCreative(p, true);
        }

        // Player want's to switch out of creative
        else if (p.getGameMode() == GameMode.CREATIVE) {
            // Switch inventories, permissions etc
            getMain().getUtils().setCreative(p, false);
        } else {
            return;
        }

        InventoryHandler.setPreviousGameMode(p, p.getGameMode());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onCommandAliases(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String command = e.getMessage();

        /* Old way of handling aliases */
        if (!PermissionHandler.isUsingOldAliases())
            return;

        ConfigurationSection section = getMain().getSettings().getConfig().getConfigurationSection("commands");
        if (section == null)
            return;

        // Loop through command list
        for (String cmd : section.getKeys(false)) {
            // Loop through alias list
            for (String alias : getMain().getSettings().getStringList("commands." + cmd + ".aliases")) {
                // If the message doesn't match or start with the alias
                // (+ space to not catch other commands)
                if (!command.startsWith("/" + alias + " ")
                        && !command.equalsIgnoreCase("/" + alias))
                    continue;

                String[] arguments = command.substring(alias.length() + 1).split(" ");
                List<String> argList = new ArrayList<>(Arrays.asList(arguments));

                // Remove empty strings caused by double spaces and such
                argList.removeAll(Arrays.asList("", null));
                arguments = argList.toArray(new String[0]);

                PluginCommand pc = main.getCommand(cmd);
                if (pc != null) {
                    pc.execute(p, alias, arguments);
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String command = e.getMessage();

        // No need to control commands in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to control non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // No need to control bypassed players
        if (p.hasPermission("rc.bypass.limit.commands")
                || p.hasPermission("rc.bypass.limit.commands." + command.split(" ")[0]))
            return;

        // Loops through all disabled commands
        for (String regex : getMain().getSettings().getStringList("limit.commands")) {
            // .substring(1) removes "/" from the command
            if (command.substring(1).toLowerCase().matches(regex)) {
                e.setCancelled(true);
                getMain().getUtils().sendMessage(p, true, "disabled.general");
                return;
            }
        }
    }

    /*
     * Called when a player switches to another world.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();

        // No need to control world changing when both worlds are enabled or disabled
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()) == getMain().getUtils()
                .isDisabledWorld(e.getFrom().getName()))
            return;

        // No need to control non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onPlayerChangedWorld");

        // Removes creative mode
        p.setGameMode(InventoryHandler.getPreviousGameMode(p));

        // Switch inventories, permissions etc
        getMain().getUtils().setCreative(p, false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (!getMain().getSettings().isEnabled("limit.moving.enabled"))
            return;

        Player p = e.getPlayer();

        // No need to control moving in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to control non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        if (getMain().getUtils().isHeightOk(p))
            return;

        p.setGameMode(InventoryHandler.getPreviousGameMode(p));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent e) {
        // Ignore other than players
        if (!(e.getEntity() instanceof Player))
            return;

        Player p = (Player) e.getEntity();

        // No need to control damage in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to control non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // No need to control disabled features
        if (!getMain().getSettings().isEnabled("limit.damage"))
            return;

        // No need to control bypassed players
        if (p.hasPermission("rc.bypass.limit.damage"))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onPlayerDamage");

        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        // No need to control moving in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to control non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // No need to control disabled features
        if (!getMain().getSettings().isEnabled("limit.item.drop"))
            return;

        // No need to control bypassed players
        if (p.hasPermission("rc.bypass.limit.item.drop"))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onPlayerDeath");

        // Removes all drops
        e.getDrops().clear();
        e.setDroppedExp(0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(PlayerLoginEvent e) {
        // We don't care if blocks have already been loaded
        if (BlockHandler.isLoadingDone)
            return;

        // No need to control disabled features
        if (!getMain().getSettings().isEnabled("general.loading.delay-login"))
            return;

        e.disallow(PlayerLoginEvent.Result.KICK_OTHER, getMain().getUtils().getMessage(false, "database.load"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        getMain().getUtils().loadInventory(p);

        // When force-gamemode is enabled, PlayerGamemodeChangeEvent isn't fired onjoin
        if (!InventoryHandler.isForceGamemodeEnabled())
            return;

        if (RestrictedCreative.DEBUG) {
            System.out.println("onPlayerJoin: forced " + main.getServer().getDefaultGameMode());
            System.out.println("... c?" + (InventoryHandler.getCreativeInv(p) != null) + " s?"
                    + (InventoryHandler.getSurvivalInv(p) != null));
        }

        // If player was switched to creative by default and it was previously survival
        if (p.getGameMode() == GameMode.CREATIVE && InventoryHandler.getCreativeInv(p) != null) {
            if (RestrictedCreative.DEBUG)
                System.out.println("onPlayerJoin: setCreative true");

            // Switch inventories, permissions etc
            getMain().getUtils().setCreative(p, true);
            InventoryHandler.setPreviousGameMode(p, GameMode.SURVIVAL);
            return;
        }

        // If player was switched to ~survival by default and it was previously creative
        if (p.getGameMode() != GameMode.CREATIVE && InventoryHandler.getSurvivalInv(p) != null) {
            if (RestrictedCreative.DEBUG)
                System.out.println("onPlayerJoin: setCreative false");

            // Switch inventories, permissions etc
            getMain().getUtils().setCreative(p, false);
            InventoryHandler.setPreviousGameMode(p, GameMode.CREATIVE);

            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        CommandHandler.removeInfoWithCommand(p);
        CommandHandler.removeAddWithCommand(p);
        CommandHandler.removeRemoveWithCommand(p);

        getMain().getUtils().saveInventory(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent e) {
        Player p = e.getPlayer();

        CommandHandler.removeInfoWithCommand(p);
        CommandHandler.removeAddWithCommand(p);
        CommandHandler.removeRemoveWithCommand(p);

        getMain().getUtils().saveInventory(p);
    }

    private boolean illegalContainerOpened(Player p) {
        InventoryType it = p.getOpenInventory().getType();

        return it != InventoryType.PLAYER && it != InventoryType.CRAFTING
                && it != InventoryType.CREATIVE && !p.hasPermission("rc.bypass.tracking.inventory")
                && getMain().getSettings().isEnabled("limit.item.drop");
    }
}