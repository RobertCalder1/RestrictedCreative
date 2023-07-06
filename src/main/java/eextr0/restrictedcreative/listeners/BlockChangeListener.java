package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import eextr0.restrictedcreative.storage.handlers.EntityHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class BlockChangeListener implements Listener {
    private RestrictedCreative main;

    public BlockChangeListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    /*
     * Represents events with a source block and a destination block, currently only
     * applies to liquid (lava and water) and teleporting dragon eggs.
     *
     * If a Block From To event is cancelled, the block will not move (the liquid
     * will not flow).
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockChange(BlockFromToEvent e) {
        Block b = e.getToBlock();

        // No need to control blocks in disabled worlds
        if (getMain().getUtils().isDisabledWorld(b.getWorld().getName()))
            return;

        // No need to control excluded blocks
        if (getMain().getUtils().isExcludedFromTracking(b.getType()))
            return;

        // Waterloggable blocks won't be affected by liquids
        if (b.getBlockData() instanceof Waterlogged)
            return;

        // No need to control non-tracked blocks
        if (!BlockHandler.isTracked(b))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onBlockChange: " + b.getType());

        // Removes, because otherwise a liquid would destroy it and drop the block
        e.setCancelled(true);
        BlockHandler.breakBlock(b, null);
    }

    /*
     * Called when any Entity, excluding players, changes a block.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        Block b = e.getBlock();
        Material m = b.getType();
        Entity en = e.getEntity();

        // No need to control blocks in disabled worlds
        if (getMain().getUtils().isDisabledWorld(b.getWorld().getName()))
            return;

        // No need to control excluded blocks (except AIR)
        if (getMain().getUtils().isExcludedFromTracking(m) && m != Material.AIR)
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onEntityChangeBlock: " + en.getType() + " " + e.getTo());

        // Crops trampled by mobs
        Block bl = b.getRelative(BlockFace.UP);
        if (BlockHandler.isTracked(bl) && m == Material.FARMLAND) {
            if (RestrictedCreative.DEBUG)
                System.out.println("Crops trampled by mobs: " + bl.getType());

            BlockHandler.breakBlock(bl, null);
            b.setType(Material.DIRT);
            e.setCancelled(true);
            return;
        }

        // No need to control non-tracked blocks and entities
        if (!BlockHandler.isTracked(b) && !EntityHandler.isTracked(en))
            return;

        // Lily pad broken by boat
        if (m == Material.LILY_PAD && BlockHandler.isTracked(b)) {
            if (RestrictedCreative.DEBUG)
                System.out.println("Lily pad broken by boat");

            BlockHandler.breakBlock(b, null);
            return;
        }

        // Wither destroying blocks
        if (en.getType() == EntityType.WITHER && BlockHandler.isTracked(b)) {
            if (RestrictedCreative.DEBUG)
                System.out.println("Block destroyed by Wither: " + b.getType());

            BlockHandler.breakBlock(b, null);
            return;
        }

        // Falling block transforming into regular block and vice versa
        if (e.getEntityType() == EntityType.FALLING_BLOCK) {
            // Regular block starts to fall and becomes FALLING_BLOCK
            if (e.getTo() == Material.AIR) {
                BlockHandler.removeTracking(b);
                EntityHandler.setAsTracked(en);

                ((FallingBlock) en).setDropItem(false);

                if (RestrictedCreative.DEBUG)
                    System.out.println("blockToFallingblock: " + m);
            }

            // FALLING_BLOCK is transforming into regular block
            else {
                BlockHandler.setAsTracked(b);

                if (RestrictedCreative.DEBUG)
                    System.out.println("fallingblockToBlock: " + e.getTo());
            }
        }
    }

    /*
     * Called when a sponge absorbs water from the world. The world will be in its
     * previous state, and getBlocks() will represent the changes to be made to the
     * world, if the event is not cancelled. As this is a physics based event it may
     * be called multiple times for "the same" changes.
     */
    @EventHandler(ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent e) {
        // No need to control blocks in disabled worlds
        if (getMain().getUtils().isDisabledWorld(e.getBlock().getWorld().getName()))
            return;

        for (BlockState bs : e.getBlocks()) {
            Block b = bs.getBlock();

            if (!BlockHandler.isTracked(b))
                continue;

            Material m = b.getType();

            if (m != Material.KELP && m != Material.KELP_PLANT)
                continue;

            BlockHandler.breakBlock(bs.getBlock(), null, false);

        }
    }
}
