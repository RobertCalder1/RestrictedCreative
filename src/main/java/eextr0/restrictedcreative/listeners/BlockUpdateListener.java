package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import eextr0.restrictedcreative.utils.MaterialHandler;
import eextr0.restrictedcreative.utils.MinecraftVersion;
import eextr0.restrictedcreative.utils.Utils;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

import java.util.ArrayList;
import java.util.List;

public class BlockUpdateListener implements Listener {
    private RestrictedCreative main;

    BlockFace[] horisontal = { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };

    public BlockUpdateListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    /*
     * Thrown when a block physics check is called
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBlockUpdate(BlockPhysicsEvent e) {
        Block b = e.getBlock();

        // No need to control non-tracked blocks
        if (!BlockHandler.isTracked(b))
            return;

        // No need to control blocks in disabled worlds
        if (getMain().getUtils().isDisabledWorld(b.getWorld().getName()))
            return;

        Material m = b.getType();

        // No need to control excluded blocks
        if (getMain().getUtils().isExcludedFromTracking(m))
            return;

        Block bl = b.getRelative(BlockFace.DOWN);
        Material ma = bl.getType();

        if (RestrictedCreative.DEBUG && RestrictedCreative.EXTRADEBUG)
            System.out.println("onBlockUpdate: " + m + " (" + ma + ")");

        /* 1.-4. Rail */
        if (MaterialHandler.isRail(b)) {
            // If the block below the rail isn't solid or
            // if rail is on slope and there isn't a block to support it
            if (!isSolid(bl) || !isSlopeOk(b)) {
                e.setCancelled(true);
                BlockHandler.breakBlock(b, null);
            }
            return;
        }

        /* 1.-4. Chorus */
        if (m == Material.CHORUS_PLANT) {
            if (!isChorusOk(b)) {
                e.setCancelled(true);
                BlockHandler.breakBlock(b, null);

                if (RestrictedCreative.DEBUG)
                    System.out.println("isChorusOk: false");
            }
            return;
        }

        /* 1.-4. Scaffolding */
        if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_14)
                && m == Material.valueOf("SCAFFOLDING")) {
            if (!isScaffoldingOk(b)) {
                e.setCancelled(true);
                BlockHandler.breakBlock(b, null);

                if (RestrictedCreative.DEBUG)
                    System.out.println("isScaffoldingOk: false");
            }
            return;
        }

        /* 1.-4. Weeping & twisting vines */
        if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_16)) {
            if (m == Material.valueOf("WEEPING_VINES")
                    || m == Material.valueOf("WEEPING_VINES_PLANT")) {
                if (!isWeepingVinesOk(b)) {
                    if (RestrictedCreative.DEBUG)
                        System.out.println("isWeepingVinesOk: false");

                    e.setCancelled(true);
                    BlockHandler.breakBlock(b, null);
                }
                return;
            }

            if (m == Material.valueOf("TWISTING_VINES")
                    || m == Material.valueOf("TWISTING_VINES_PLANT")) {
                if (!isTwistingVinesOk(b)) {
                    if (RestrictedCreative.DEBUG)
                        System.out.println("isTwistingVinesOk: false");

                    e.setCancelled(true);
                    BlockHandler.breakBlock(b, null);
                }
                return;
            }
        }

        /* 5. Top blocks */
        if (MaterialHandler.needsBlockBelow(b)) {
            if (RestrictedCreative.DEBUG)
                System.out.println("needsBlockBelow: " + m);

            // Needs to be checked BEFORE isSolid()
            if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_14)
                    && (m == Material.valueOf("BAMBOO")
                    || m == Material.valueOf("BAMBOO_SAPLING"))) {
                if (!isBambooOk(b)) {
                    if (RestrictedCreative.DEBUG)
                        System.out.println("isBambooOk: false");

                    e.setCancelled(true);
                    BlockHandler.breakBlock(b, null);
                }
                return;
            }

            // Needs to be checked BEFORE isSolid()
            switch (m) {
                case LILY_PAD:
                    if (ma != Material.WATER) {
                        e.setCancelled(true);
                        BlockHandler.breakBlock(b, null);
                    }
                    return;
                case KELP:
                case KELP_PLANT:
                    if (!isKelpOk(b)) {
                        e.setCancelled(true);
                        BlockHandler.breakBlock(b, null);
                    }
                    return;
                case CACTUS:
                    if (!isCactusOk(b)) {
                        e.setCancelled(true);
                        BlockHandler.breakBlock(b, null);
                    }
                    return;
                case SUGAR_CANE:
                    if (!isSugarCaneOk(b)) {
                        e.setCancelled(true);
                        BlockHandler.breakBlock(b, null);
                    }
                    return;
                default:
                    break;
            }

            // Needs to be checked BEFORE isSolid()
            if (MaterialHandler.isDoublePlant(b)) {
                if (!isDoublePlantOk(b)) {
                    e.setCancelled(true);
                    BlockHandler.breakBlock(b, null);
                }
                return;
            }

            // Needs to be checked BEFORE isSolid()
            if (MaterialHandler.isCarpet(b)) {
                if (isBelowEmpty(b)) {
                    e.setCancelled(true);
                    BlockHandler.breakBlock(b, null);
                }
                return;
            }

            // Needs to be checked BEFORE isSolid()
            if (MaterialHandler.isCrop(b)) {
                if (!isLightingOk(b) || !isSolid(bl)) {
                    e.setCancelled(true);
                    BlockHandler.breakBlock(b, null);
                }
                return;
            }

            // Needs to be checked BEFORE isSolid()
            if (MaterialHandler.isDoor(b)) {
                if (!isDoorOk(b) || !isSolid(bl)) {
                    e.setCancelled(true);
                    BlockHandler.breakBlock(b, null);
                }
                return;
            }

            if (!isSolid(bl)) {
                e.setCancelled(true);
                BlockHandler.breakBlock(b, null);
            }
            return;
        }

        /* 5. Attachable */
        BlockFace bf = MaterialHandler.getNeededFace(b);
        if (bf != null) {
            if (RestrictedCreative.DEBUG)
                System.out.println("getNeededFace: " + bf);

            bl = b.getRelative(bf);

            // If the block (to which the first block is attached to) isn't solid
            if (!isSolid(bl)) {
                e.setCancelled(true);
                BlockHandler.breakBlock(b, null);
            }
        }
    }

    private boolean isWeepingVinesOk(Block b) {
        Block bl = b.getRelative(BlockFace.UP);
        Material ma = bl.getType();

        return ma == Material.valueOf("WEEPING_VINES")
                || ma == Material.valueOf("WEEPING_VINES_PLANT") || isSolid(bl);
    }

    private boolean isTwistingVinesOk(Block b) {
        Block bl = b.getRelative(BlockFace.DOWN);
        Material ma = bl.getType();

        return ma == Material.valueOf("TWISTING_VINES")
                || ma == Material.valueOf("TWISTING_VINES_PLANT") || isSolid(bl);
    }

    private boolean isScaffoldingOk(Block scaffolding) {
        Block down = scaffolding.getRelative(BlockFace.DOWN);
        if (down.getType().isSolid() || down.getType() == Material.valueOf("SCAFFOLDING")) {
            if (RestrictedCreative.DEBUG)
                System.out.println("isScaffoldingOk: true (down ok)");

            return true;
        }

        /*
         * TODO doesn't work int radius = 6; for (int x = scaffolding.getX() - radius; x
         * < scaffolding.getX() + radius; x++) { for (int z = scaffolding.getZ() -
         * radius; z < scaffolding.getZ() + radius; z++) { Block bl =
         * scaffolding.getRelative(x, 0, z); if (bl.getType() !=
         * Material.valueOf("SCAFFOLDING")) continue;
         *
         * Block bottom = bl.getRelative(BlockFace.DOWN); if (bottom.getType().isSolid()
         * || bottom.getType() == Material.valueOf("SCAFFOLDING")) { if (Main.DEBUG)
         * System.out.println("isScaffoldingOk: true (" + x + ", " + z + ")");
         *
         * return true; } } }
         */

        return false;
    }

    private boolean isSlopeOk(Block b) {
        Rail rail = (Rail) b.getBlockData();

        if (RestrictedCreative.DEBUG)
            System.out.println("isSlopeOk: " + rail.getShape());

        switch (rail.getShape()) {
            case ASCENDING_EAST:
                return b.getRelative(BlockFace.EAST).getType().isSolid();
            case ASCENDING_NORTH:
                return b.getRelative(BlockFace.NORTH).getType().isSolid();
            case ASCENDING_SOUTH:
                return b.getRelative(BlockFace.SOUTH).getType().isSolid();
            case ASCENDING_WEST:
                return b.getRelative(BlockFace.WEST).getType().isSolid();
            default:
                return true;
        }
    }

    private boolean isChorusOk(Block b) {
        List<Block> horizontalChoruses = horisontalChoruses(b);
        boolean validHorizontalChorusExists = validHorisontalChorusExists(horizontalChoruses);

        // Chorus plant will break unless the block below is (chorus plant or end stone)
        // or any horizontally adjacent block is a chorus plant above (chorus plant or
        // end stone)
        if (!isBelowChorusOk(b) && !validHorizontalChorusExists)
            return false;

        boolean isVerticalOk = b.getRelative(BlockFace.UP).getType() == Material.AIR
                || b.getRelative(BlockFace.DOWN).getType() == Material.AIR;

        // Chorus plant with at least one other chorus plant horizontally adjacent will
        // break unless at least one of the vertically adjacent blocks is air
        return horizontalChoruses.isEmpty() || isVerticalOk;
    }

    private boolean isBelowChorusOk(Block b) {
        Material m = b.getRelative(BlockFace.DOWN).getType();
        return m == Material.END_STONE || m == Material.CHORUS_PLANT;
    }

    private boolean isSolid(Block b) {
        Material m = b.getType();
        BlockData bd = b.getBlockData();

        boolean isPistonOk = !(bd instanceof Piston) || !((Piston) bd).isExtended();
        boolean isTrapdoorOk = !(bd instanceof TrapDoor) || !((TrapDoor) bd).isOpen();

        return m.isSolid() && isPistonOk && isTrapdoorOk;
    }

    private List<Block> horisontalChoruses(Block b) {
        List<Block> choruses = new ArrayList<>();

        for (BlockFace bf : horisontal) {
            Block bl = b.getRelative(bf);

            if (bl.getType() == Material.CHORUS_PLANT)
                choruses.add(bl);
        }

        return choruses;
    }

    private boolean validHorisontalChorusExists(List<Block> blocks) {
        for (Block b : blocks) {
            if (isBelowChorusOk(b))
                return true;
        }

        return false;
    }

    private boolean isLightingOk(Block b) {
        return b.getLightFromSky() >= 5 || b.getLightFromBlocks() >= 8;
    }

    private boolean isDoorOk(Block b) {
        Block bl = b.getRelative(BlockFace.DOWN);
        BlockData bd = bl.getBlockData();

        if (bd instanceof TrapDoor && ((TrapDoor) bd).isOpen())
            return false;

        return isSolid(bl);
    }

    private boolean isCactusOk(Block b) {
        boolean nothingAround = isAroundCactusOk(b.getRelative(BlockFace.EAST))
                && isAroundCactusOk(b.getRelative(BlockFace.WEST))
                && isAroundCactusOk(b.getRelative(BlockFace.NORTH))
                && isAroundCactusOk(b.getRelative(BlockFace.SOUTH));

        Material m = b.getRelative(BlockFace.DOWN).getType();
        boolean belowOk = m == Material.SAND || m == Material.RED_SAND || m == Material.CACTUS;

        boolean aboveOk = b.getRelative(BlockFace.UP).getType() != Material.WATER;

        return nothingAround && belowOk && aboveOk;
    }

    private boolean isAroundCactusOk(Block b) {
        Material m = b.getType();

        if (RestrictedCreative.DEBUG)
            System.out.println("isAroundCactusOk: " + m);

        return m == Material.AIR || m == Material.WATER;
    }

    private boolean isSugarCaneOk(Block b) {
        Block bl = b.getRelative(BlockFace.DOWN);
        Material ma = bl.getType();

        if (ma == Material.SUGAR_CANE)
            return true;

        boolean soil = ma == Material.GRASS_BLOCK || ma == Material.DIRT || ma == Material.SAND
                || ma == Material.PODZOL || ma == Material.COARSE_DIRT || ma == Material.RED_SAND;

        Material east = bl.getRelative(BlockFace.EAST).getType();
        Material west = bl.getRelative(BlockFace.WEST).getType();
        Material north = bl.getRelative(BlockFace.NORTH).getType();
        Material south = bl.getRelative(BlockFace.SOUTH).getType();

        boolean water = east == Material.WATER || west == Material.WATER || north == Material.WATER
                || south == Material.WATER;
        boolean frosted_ice = east == Material.FROSTED_ICE || west == Material.FROSTED_ICE
                || north == Material.FROSTED_ICE || south == Material.FROSTED_ICE;

        return soil && (water || frosted_ice);
    }

    private boolean isBambooOk(Block b) {
        Block bl = b.getRelative(BlockFace.DOWN);
        Material ma = bl.getType();

        return Tag.BAMBOO_PLANTABLE_ON.isTagged(ma);
    }

    private boolean isKelpOk(Block b) {
        Block bl = b.getRelative(BlockFace.DOWN);
        Material m = bl.getType();

        return m == Material.KELP || m == Material.KELP_PLANT || isSolid(bl);
    }

    private boolean isDoublePlantOk(Block b) {
        // Both up and down must be OK
        if (isSolid(b.getRelative(BlockFace.DOWN)))
            return b.getRelative(BlockFace.UP).getType() == b.getType();

        // Below is not solid
        return b.getRelative(BlockFace.DOWN).getType() == b.getType();
    }

    private boolean isBelowEmpty(Block b) {
        return b.getRelative(BlockFace.DOWN).isEmpty();
    }
}
