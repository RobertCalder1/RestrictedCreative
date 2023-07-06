package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import eextr0.restrictedcreative.utils.MaterialHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockPistonListener implements Listener {
    private RestrictedCreative main;

    public BlockPistonListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    /*
     * Called when a piston retracts
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPull(BlockPistonRetractEvent e) {
        // No need to control blocks in disabled worlds
        if (getMain().getUtils().isDisabledWorld(e.getBlock().getWorld().getName()))
            return;

        // Non-sticky piston doesn't pull anything
        if (!e.isSticky())
            return;

        List<Block> newBlocks = new ArrayList<>();
        List<Block> oldBlocks = new ArrayList<>();
        List<BlockFace> blockfaces = getSurroundingBlockFaces(e.getDirection());

        if (RestrictedCreative.DEBUG)
            System.out.println("onBlockPullBlocks: " + e.getBlocks().size());

        // Loop through pulled blocks
        for (Block b : e.getBlocks()) {
            checkSurroundingBlocks(e, b, blockfaces);

            if (e.isCancelled())
                return;

            if (!BlockHandler.isTracked(b))
                continue;

            if (RestrictedCreative.DEBUG)
                System.out.println("onBlockPull: " + b.getType() + " " + b.getPistonMoveReaction());

            BlockData bd = b.getBlockData();

            /* Bed */
            if (bd instanceof Bed) {
                Bed bed = (Bed) bd;
                Block block = bed.getPart() == Bed.Part.FOOT ? b.getRelative(bed.getFacing())
                        : b.getRelative(bed.getFacing().getOppositeFace());

                if (RestrictedCreative.DEBUG)
                    System.out.println("bedPull: " + b.getType() + " " + block.getType());

                BlockHandler.breakBlock(b, null, false);
                BlockHandler.breakBlock(block, null, false);
                continue;
            }

            if (b.getPistonMoveReaction() == PistonMoveReaction.BREAK)
                BlockHandler.breakBlock(b, null);

            // If it's on top of another moving block and needs a block below
            if (e.getBlocks().contains(b.getRelative(BlockFace.DOWN)) && MaterialHandler.needsBlockBelow(b))
                BlockHandler.breakBlock(b, null);

            // If it's attached to another moving block
            BlockFace neededFace = MaterialHandler.getNeededFace(b);
            if (neededFace != null && e.getBlocks().contains(b.getRelative(neededFace)))
                BlockHandler.breakBlock(b, null);

            oldBlocks.add(b);
            newBlocks.add(b.getRelative(e.getDirection()));
        }

        // Remove old blocks first
        for (Block b : oldBlocks) {
            BlockHandler.removeTracking(b);
        }
        // Add new blocks afterwards
        for (Block b : newBlocks) {
            BlockHandler.setAsTracked(b);
        }
    }

    /*
     * Called when a piston extends
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPush(BlockPistonExtendEvent e) {
        // No need to control blocks in disabled worlds
        if (getMain().getUtils().isDisabledWorld(e.getBlock().getWorld().getName()))
            return;

        List<Block> newBlocks = new ArrayList<>();
        List<Block> oldBlocks = new ArrayList<>();
        List<BlockFace> blockfaces = getSurroundingBlockFaces(e.getDirection());

        if (RestrictedCreative.DEBUG)
            System.out.println("onBlockPushBlocks: " + e.getBlocks().size());

        // Loop through pushed blocks
        for (Block b : e.getBlocks()) {
            checkSurroundingBlocks(e, b, blockfaces);

            if (e.isCancelled())
                return;

            if (!BlockHandler.isTracked(b))
                continue;

            if (RestrictedCreative.DEBUG)
                System.out.println("onBlockPush: " + b.getType() + " " + b.getPistonMoveReaction());

            BlockData bd = b.getBlockData();

            /* Bed */
            if (bd instanceof Bed) {
                Bed bed = (Bed) bd;
                Block block = bed.getPart() == Bed.Part.FOOT ? b.getRelative(bed.getFacing())
                        : b.getRelative(bed.getFacing().getOppositeFace());

                if (RestrictedCreative.DEBUG)
                    System.out.println("bedPush: " + b.getType() + " " + block.getType());

                BlockHandler.breakBlock(b, null, false);
                BlockHandler.breakBlock(block, null, false);
                continue;
            }

            /* Door */
            if (bd instanceof Door) {
                Door door = (Door) bd;
                Block block = door.getHalf() == Bisected.Half.BOTTOM ? b.getRelative(BlockFace.UP)
                        : b.getRelative(BlockFace.DOWN);

                if (RestrictedCreative.DEBUG)
                    System.out.println("removeSurroundingBlock: door " + b.getType() + " " + block.getType());

                BlockHandler.breakBlock(b, null, false);
                BlockHandler.breakBlock(block, null, false);
                continue;
            }

            if (b.getPistonMoveReaction() == PistonMoveReaction.BREAK)
                BlockHandler.breakBlock(b, null);

            // If it's on top of another moving block and need a block below
            if (e.getBlocks().contains(b.getRelative(BlockFace.DOWN)) && MaterialHandler.needsBlockBelow(b))
                BlockHandler.breakBlock(b, null);

            // If it's attached to another moving block
            BlockFace neededFace = MaterialHandler.getNeededFace(b);
            if (neededFace != null && e.getBlocks().contains(b.getRelative(neededFace)))
                BlockHandler.breakBlock(b, null);

            oldBlocks.add(b);
            newBlocks.add(b.getRelative(e.getDirection()));
        }

        // Remove old blocks first
        for (Block b : oldBlocks) {
            BlockHandler.removeTracking(b);
        }
        // Add new blocks afterwards
        for (Block b : newBlocks) {
            BlockHandler.setAsTracked(b);
        }
    }

    private List<BlockFace> getSurroundingBlockFaces(BlockFace direction) {
        // Gets the non-pushed/-pulled faces
        BlockFace bf1 = null, bf2 = null, bf3 = null, bf4 = null, bf5 = direction.getOppositeFace();

        switch (direction.name()) {
            case ("UP"):
            case ("DOWN"):
                bf1 = BlockFace.WEST;
                bf2 = BlockFace.EAST;
                bf3 = BlockFace.NORTH;
                bf4 = BlockFace.SOUTH;
                break;
            case ("NORTH"):
            case ("SOUTH"):
                bf1 = BlockFace.WEST;
                bf2 = BlockFace.EAST;
                bf3 = BlockFace.UP;
                bf4 = BlockFace.DOWN;
                break;
            case ("EAST"):
            case ("WEST"):
                bf1 = BlockFace.NORTH;
                bf2 = BlockFace.SOUTH;
                bf3 = BlockFace.UP;
                bf4 = BlockFace.DOWN;
                break;
        }

        return Arrays.asList(bf1, bf2, bf3, bf4, bf5);
    }

    private void checkSurroundingBlocks(BlockPistonEvent e, Block b, List<BlockFace> sides) {
        for (BlockFace bf : sides) {
            Block bl = b.getRelative(bf);

            // Checks if the surrounding block is placed in creative
            if (!BlockHandler.isTracked(bl))
                continue;

            // If it's attached to the moving block
            if (bl.getFace(b) == BlockFace.DOWN && MaterialHandler.needsBlockBelow(bl)) {
                // Rails don't cooperate with slime blocks
                if (MaterialHandler.isRail(bl) && b.getType() == Material.SLIME_BLOCK) {
                    if (RestrictedCreative.DEBUG)
                        System.out.println("removeSurroundingBlock: slime + " + bl.getType());

                    e.setCancelled(true);
                    return;
                }

                BlockData bd = b.getBlockData();

                /* Door */
                if (bd instanceof Door) {
                    Door door = (Door) bd;
                    Block block = door.getHalf() == Bisected.Half.BOTTOM ? b.getRelative(BlockFace.UP)
                            : b.getRelative(BlockFace.DOWN);

                    if (RestrictedCreative.DEBUG)
                        System.out.println("removeSurroundingBlock: door " + b.getType() + " " + block.getType());

                    BlockHandler.breakBlock(b, null, false);
                    BlockHandler.breakBlock(block, null, false);
                    continue;
                }

                if (RestrictedCreative.DEBUG)
                    System.out.println("removeSurroundingBlock: " + bl.getType());

                BlockHandler.breakBlock(bl, null);
            }

            if (bl.getFace(b) == MaterialHandler.getNeededFace(bl)) {
                if (RestrictedCreative.DEBUG)
                    System.out.println("removeSurroundingBlock: " + bl.getType());

                BlockHandler.breakBlock(bl, null);
            }
        }
    }
}
