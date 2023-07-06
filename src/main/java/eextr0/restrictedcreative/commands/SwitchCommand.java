package eextr0.restrictedcreative.commands;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SwitchCommand implements CommandExecutor {
    private RestrictedCreative main;
    private GameMode gm;

    public SwitchCommand(RestrictedCreative plugin, GameMode gm) {
        this.main = plugin;
        this.gm = gm;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (args.length) {
            case 0:
                if (!(sender instanceof Player))
                    return false;

                Player p = (Player) sender;

                // Commands shouldn't work in disabled worlds
                if (getMain().getUtils().isDisabledWorld(p.getWorld().getName())
                        && !p.hasPermission("rc.bypass.general.disabled-worlds")) {
                    getMain().getUtils().sendMessage(p, true, "disabled.region");
                    return true;
                }

                // Don't switch to creative mode when it's not allowed at player's height
                if (this.gm == GameMode.CREATIVE && !getMain().getUtils().isHeightOk(p)) {
                    getMain().getUtils().sendMessage(p, true, "disabled.region");
                    return true;
                }

                // Commands shouldn't work in disabled regions
                if ((getMain().getSettings().isEnabled("limit.regions.owner-based.enabled")
                        || getMain().getSettings().isEnabled("limit.regions.whitelist.enabled"))
                        && getMain().getUtils().cannotBuildHere(p, p.getLocation().getBlock(),
                        Material.DIRT)) {
                    getMain().getUtils().sendMessage(p, true, "disabled.region");
                    return true;
                }

                p.setGameMode(this.gm);
                main.getUtils().sendMessage(p, true, "gamemodes." + getName() + ".me");
                return true;
            case 1:
                if (sender.hasPermission("rc.commands." + getName() + ".others")) {
                    Player p1 = Bukkit.getPlayer(args[0]);

                    if (p1 == null || !p1.isOnline()) {
                        Utils.sendMessage(sender, main.getUtils().getMessage(true, "not-found")
                                .replaceAll("%player%", args[0]));
                        return true;
                    }

                    // Commands shouldn't work in disabled worlds
                    if (getMain().getUtils().isDisabledWorld(p1.getWorld().getName())
                            && !p1.hasPermission("rc.bypass.general.disabled-worlds")) {
                        getMain().getUtils().sendMessage(sender, true, "disabled.region");
                        return true;
                    }

                    p1.setGameMode(this.gm);
                    main.getUtils().sendMessage(p1, true, "gamemodes." + getName() + ".me");
                    Utils.sendMessage(sender,
                            main.getUtils().getMessage(true, "gamemodes." + getName() + ".other")
                                    .replaceAll("%player%", p1.getName()));
                    return true;
                }

                break;
            default:
                return false;
        }

        main.getUtils().sendMessage(sender, false, "no-permission");
        return true;
    }

    private String getName() {
        return this.gm.toString().toLowerCase();
    }
}
