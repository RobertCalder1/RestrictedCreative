package eextr0.restrictedcreative.utils.external;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WorldGuardUtils {
    public static boolean canBuildHere(RestrictedCreative main, Player p, Block b, Material m) {
        Utils utils = main.getUtils();
        WorldGuardPlugin wg = (WorldGuardPlugin) Bukkit.getServer().getPluginManager()
                .getPlugin("WorldGuard");

        // Gets the player or block location
        Location loc = (b != null) ? b.getLocation() : p.getLocation();
        LocalPlayer lp = wg.wrapPlayer(p);

        // Gets all regions covering the block or player location
        RegionQuery rq = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet set = rq.getApplicableRegions(BukkitAdapter.adapt(loc));

        // Loops through applicable regions
        for (ProtectedRegion rg : set) {
            // Whitelist check
            if (main.getSettings().isEnabled("limit.regions.whitelist.enabled")) {
                // If it's whitelisted
                if (utils.isWhitelistedRegion(rg.getId()))
                    return true;
            }

            // Owner check
            if (main.getSettings().isEnabled("limit.regions.owner-based.enabled")) {
                if (rg.isOwner(lp))
                    return true;

                // Member check
                if (main.getSettings().isEnabled("limit.regions.owner-based.allow-members")) {
                    if (rg.isMember(lp))
                        return true;
                }
            }
        }

        return false;
    }
}
