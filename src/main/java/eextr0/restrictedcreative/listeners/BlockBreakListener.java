package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import eextr0.restrictedcreative.utils.MaterialHandler;
import eextr0.restrictedcreative.utils.Utils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.PistonHead;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {
    private final RestrictedCreative main;

    public BlockBreakListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    /*
     * Called when a block is broken by a player.
     *
     * Note: Plugins wanting to simulate a traditional block drop should set the
     * block to air and utilize their own methods for determining what the default
     * drop for the block being broken is and what to do about it, if anything.
     *
     * If a Block Break event is cancelled, the block will not break and experience
     * will not drop.
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        BlockData bd = b.getBlockData();
        Material m = b.getType();

        // No need to control blocks in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        /*
         * Disabled blocks for creative players
         *
         * Must come before excluded check in order for it to work when tracking is disabled
         */
        if (p.getGameMode() == GameMode.CREATIVE && getMain().getUtils().isDisabledBreaking(m)
                && !p.hasPermission("rc.bypass.disable.breaking")
                && !p.hasPermission("rc.bypass.disable.breaking." + m)) {
            getMain().getUtils().sendMessage(p, true, "disabled.general");
            e.setCancelled(true);
            return;
        }

        // No need to control excluded blocks
        if (getMain().getUtils().isExcludedFromTracking(b.getType()))
            return;

        // Piston head
        // Needs to be BEFORE isTracked() because PistonHead is not tracked
        if (bd instanceof PistonHead) {
            PistonHead head = (PistonHead) bd;
            Block piston = b.getRelative(head.getFacing().getOppositeFace());

            // No need to control non-tracked pistons
            if (!BlockHandler.isTracked(piston))
                return;

            if (RestrictedCreative.DEBUG)
                System.out.println("PistonHead: " + piston.getType());

            remove(e, p, false, piston, b);
        }

        // No need to control non-tracked blocks
        if (!BlockHandler.isTracked(b))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onBlockBreak: " + m);

        // Door
        if (bd instanceof Door) {
            Door door = (Door) bd;
            Block bl;

            if (door.getHalf() == Bisected.Half.TOP) {
                bl = b.getRelative(BlockFace.DOWN);
            } else {
                bl = b.getRelative(BlockFace.UP);
            }

            remove(e, p, false, bl);
        }

        // Bed
        else if (bd instanceof Bed) {
            Bed bed = (Bed) bd;
            Block bl;

            if (bed.getPart() == Bed.Part.HEAD) {
                bl = b.getRelative(bed.getFacing().getOppositeFace());
            } else {
                bl = b.getRelative(bed.getFacing());
            }

            remove(e, p, false, bl);
        }

        // Double plant
        else if (MaterialHandler.isDoublePlant(b)) {
            Block bl = b.getRelative(BlockFace.DOWN);

            // If block below isn't a double plant, the one above must be
            if (!MaterialHandler.isDoublePlant(bl))
                bl = b.getRelative(BlockFace.UP);

            remove(e, p, false, bl);
        }

        // Others
        remove(e, p, true, b);
    }

    private void remove(BlockBreakEvent e, Player p, boolean update, Block... blocks) {
        if (Utils.isInstalled("Jobs") || Utils.isInstalled("mcMMO")) {
            for (Block b : blocks)
                BlockHandler.breakBlock(b, p, update);

            e.setCancelled(true);
        } else {
            for (Block b : blocks) {
                BlockHandler.removeTracking(b);
            }

            e.setDropItems(false);
            e.setExpToDrop(0);
        }

        // Notify the breaker why there's no drops
        if (p.getGameMode() != GameMode.CREATIVE &&
                getMain().getSettings().isEnabled("tracking.blocks.notify"))
            getMain().getUtils().sendMessage(p, true, "disabled.drops");
    }
}
