package eextr0.restrictedcreative.utils.external;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CoreProtectUtils {
    public CoreProtectUtils(Block b, Player p, boolean update) {
        String player = p == null ? "RestrictedCreative" : p.getName();
        Location location = b.getLocation();
        Material type = b.getType();
        BlockData oldBlock = b.getBlockData();

        b.setType(Material.AIR, update);

        CoreProtectAPI coreProtect = getCoreProtect();
        if (coreProtect == null)
            return;

        coreProtect.logRemoval(player, location, type, oldBlock);
    }

    private CoreProtectAPI getCoreProtect() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (!(plugin instanceof CoreProtect))
            return null;

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
        if (!CoreProtect.isEnabled())
            return null;

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 6)
            return null;

        return CoreProtect;
    }
}
