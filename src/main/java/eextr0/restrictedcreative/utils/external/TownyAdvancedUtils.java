package eextr0.restrictedcreative.utils.external;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import eextr0.restrictedcreative.RestrictedCreative;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class TownyAdvancedUtils {
    public static boolean canBuildHere(RestrictedCreative main, Player p, Block b, Material m) {
        if (!main.getSettings().isEnabled("limit.regions.owner-based.enabled"))
            return false;

        // Gets the player or block location
        Location loc = (b != null) ? b.getLocation() : p.getLocation();

        // Owner check
        try {
            TownyAPI towny = TownyAPI.getInstance();

            Resident resident = towny.getResident(p.getUniqueId());
            Town town = towny.getTownBlock(loc).getTown();

            if (resident.getTown().equals(town))
                return true;
        } catch (NotRegisteredException | NullPointerException e) {
            return false;
        }

        return false;
    }
}
