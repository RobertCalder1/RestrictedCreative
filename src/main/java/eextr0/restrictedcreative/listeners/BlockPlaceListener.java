package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockPlaceListener implements Listener {
    private final RestrictedCreative main;

    public BlockPlaceListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    /*
     * Fired when a single block placement action of a player triggers the creation
     * of multiple blocks(e.g. placing a bed block). The block returned by
     * BlockPlaceEvent.getBlockPlaced() and its related methods is the block where
     * the placed block would exist if the placement only affected a single block.
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockMultiPlace(BlockMultiPlaceEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlockPlaced();
        List<BlockState> states = e.getReplacedBlockStates();
        Material m = b.getType();

        // No need to track blocks in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to track non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        /*
         * Disabled blocks
         *
         * Must come before excluded check in order for it to work when tracking is
         * disabled
         */
        if (getMain().getUtils().isDisabledPlacing(m)
                && !p.hasPermission("rc.bypass.disable.placing")
                && !p.hasPermission("rc.bypass.disable.placing." + m)) {
            getMain().getUtils().sendMessage(p, true, "disabled.general");
            e.setCancelled(true);
            return;
        }

        // No need to track excluded blocks
        if (getMain().getUtils().isExcludedFromTracking(b.getType()))
            return;

        // No need to track bypassed players
        if (p.hasPermission("rc.bypass.tracking.blocks")
                || p.hasPermission("rc.bypass.tracking.blocks." + m))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onBlockMultiPlace: " + b.getType());

        for (BlockState bs : states)
            BlockHandler.setAsTracked(bs.getBlock());
    }

    /*
     * Called when a block is placed by a player.
     *
     * If a Block Place event is cancelled, the block will not be placed.
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlockPlaced();
        Material m = b.getType();

        // No need to track blocks in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to track non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        /*
         * Disabled blocks
         *
         * Must come before excluded check in order for it to work when tracking is
         * disabled
         */
        if (getMain().getUtils().isDisabledPlacing(m)
                && !p.hasPermission("rc.bypass.disable.placing")
                && !p.hasPermission("rc.bypass.disable.placing." + m)) {
            getMain().getUtils().sendMessage(p, true, "disabled.general");
            e.setCancelled(true);
            return;
        }

        // No need to track excluded blocks
        // AIR is allowed because ItemsAdder plugin had issues otherwise:
        // https://github.com/PluginBugs/Issues-ItemsAdder/issues/866
        if (getMain().getUtils().isExcludedFromTracking(b.getType()) && m != Material.AIR)
            return;

        // No need to track bypassed players
        if (p.hasPermission("rc.bypass.tracking.blocks")
                || p.hasPermission("rc.bypass.tracking.blocks." + m))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onBlockPlace: " + b.getType());

        BlockHandler.setAsTracked(b);
    }

    /*
     * Called when a block is placed by a player.
     *
     * If a Block Place event is cancelled, the block will not be placed.
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCreatureCreate(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Block head = e.getBlockPlaced();

        // No need to check creations in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        switch (head.getType()) {
            // Wither
            case WITHER_SKELETON_SKULL:
            case WITHER_SKELETON_WALL_SKULL:
                if (!getMain().getSettings().isEnabled("limit.creation.wither"))
                    return;

                if (!couldWitherBeBuilt(head))
                    return;

                // Wither was built in survival mode
                if (p.getGameMode() != GameMode.CREATIVE && canSurvivalBuildWither(head))
                    return;

                getMain().getUtils().sendMessage(p, true, "disabled.creature");
                e.setCancelled(true);
                return;
            // Golem
            case PUMPKIN:
            case CARVED_PUMPKIN:
            case JACK_O_LANTERN:
                // Iron golem
                if (getMain().getSettings().isEnabled("limit.creation.iron-golem")
                        && couldIronGolemBeBuilt(head)) {
                    // Golem was built in survival mode
                    if (p.getGameMode() != GameMode.CREATIVE && canSurvivalBuildIronGolem(head))
                        return;

                    getMain().getUtils().sendMessage(p, true, "disabled.creature");
                    e.setCancelled(true);
                }
                // Snow golem
                else if (getMain().getSettings().isEnabled("limit.creation.snow-golem")
                        && couldSnowGolemBeBuilt(head)) {
                    // Golem was built in survival mode
                    if (p.getGameMode() != GameMode.CREATIVE && canSurvivalBuildSnowGolem(head))
                        return;

                    getMain().getUtils().sendMessage(p, true, "disabled.creature");
                    e.setCancelled(true);
                }
        }
    }

    /*
     * Called when an item is dispensed from a block.
     *
     * If a Block Dispense event is cancelled, the block will not dispense the item.
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockDispense(BlockDispenseEvent e) {
        Block b = e.getBlock();

        // No need to check creations in disabled worlds
        if (getMain().getUtils().isDisabledWorld(b.getWorld().getName()))
            return;

        BlockData bd = b.getBlockData();

        // Only dispensers can place blocks
        if (!(bd instanceof Dispenser))
            return;

        BlockFace dir = ((Directional) bd).getFacing();
        Block head = b.getRelative(dir);

        switch (e.getItem().getType()) {
            // Wither
            case WITHER_SKELETON_SKULL:
            case WITHER_SKELETON_WALL_SKULL:
                if (!getMain().getSettings().isEnabled("limit.creation.wither"))
                    return;

                if (!couldWitherBeBuilt(head))
                    return;

                // Wither was built in survival mode
                if (canSurvivalBuildWither(head))
                    return;

                e.setCancelled(true);
                return;
            // Golem
            case PUMPKIN:
            case CARVED_PUMPKIN:
            case JACK_O_LANTERN:
                // Iron golem
                if (getMain().getSettings().isEnabled("limit.creation.iron-golem")
                        && couldIronGolemBeBuilt(head)) {
                    // Golem was built in survival mode
                    if (canSurvivalBuildIronGolem(head))
                        return;

                    e.setCancelled(true);
                }
                // Snow golem
                else if (getMain().getSettings().isEnabled("limit.creation.snow-golem")
                        && couldSnowGolemBeBuilt(head)) {
                    // Golem was built in survival mode
                    if (canSurvivalBuildSnowGolem(head))
                        return;

                    e.setCancelled(true);
                }
        }
    }

    // Return the direction in which the row is situated or null if it's not a row
    BlockFace isInRow(Block head, Material type) {
        BlockFace bodyDir = null;
        boolean isbodyDir = false;

        for (int i = 0; !isbodyDir; i++) {
            switch (i) {
                case 0:
                    bodyDir = BlockFace.EAST;
                    break;
                case 1:
                    bodyDir = BlockFace.WEST;
                    break;
                case 2:
                    bodyDir = BlockFace.NORTH;
                    break;
                case 3:
                    bodyDir = BlockFace.SOUTH;
                    break;
                case 4:
                    bodyDir = BlockFace.UP;
                    break;
                case 5:
                    bodyDir = BlockFace.DOWN;
                    break;
                default:
                    return null;
            }

            Block middle = head.getRelative(bodyDir);

            if (middle.getType() == type)
                isbodyDir = middle.getRelative(bodyDir).getType() == type;
        }

        return bodyDir;
    }

    // Return whether the snow golem would spawn if the head was placed
    public boolean couldSnowGolemBeBuilt(Block head) {
        return isInRow(head, Material.SNOW_BLOCK) != null;
    }

    // Return whether the snow golem's body is built in survival
    // (whether a survival player should be allowed to create a golem from it)
    public boolean canSurvivalBuildSnowGolem(Block head) {
        BlockFace bodyDir = isInRow(head, Material.SNOW_BLOCK);
        Block middle = head.getRelative(bodyDir);
        Block bottom = middle.getRelative(bodyDir);

        return !(BlockHandler.isTracked(middle) || BlockHandler.isTracked(bottom));
    }

    // Return whether the iron golem would spawn if the head was placed
    public boolean couldIronGolemBeBuilt(Block head) {
        BlockFace middle = getMiddleBody(head, Material.IRON_BLOCK);
        if (middle == null)
            return false;

        Block bottom = head.getRelative(middle).getRelative(middle);

        return bottom.getType() == Material.IRON_BLOCK;
    }

    // Return whether the iron golem's body is built in survival
    // (whether a survival player should be allowed to create a golem from it)
    public boolean canSurvivalBuildIronGolem(Block head) {
        BlockFace bodyDir = getMiddleBody(head, Material.IRON_BLOCK);
        Block middle = head.getRelative(bodyDir);
        Block bottom = middle.getRelative(bodyDir);

        BlockFace middleDir = getRowDirection(middle, Material.IRON_BLOCK);
        Block middle1 = head.getRelative(middleDir);
        Block middle2 = head.getRelative(middleDir.getOppositeFace());

        boolean middleRow = BlockHandler.isTracked(middle) || BlockHandler.isTracked(middle1)
                || BlockHandler.isTracked(middle2);
        boolean headBottom = BlockHandler.isTracked(head) || BlockHandler.isTracked(bottom);

        return !(headBottom || middleRow);
    }

    // Return the direction in which the row is located
    // or null if the given block isn't in the middle
    BlockFace getRowDirection(Block middle, Material... type) {
        List<Material> types = new ArrayList<>(Arrays.asList(type));

        Block east = middle.getRelative(BlockFace.EAST);
        Block west = middle.getRelative(BlockFace.WEST);
        Block north = middle.getRelative(BlockFace.NORTH);
        Block south = middle.getRelative(BlockFace.SOUTH);
        Block up = middle.getRelative(BlockFace.UP);
        Block down = middle.getRelative(BlockFace.DOWN);

        boolean eastwest = types.contains(east.getType()) && types.contains(west.getType());
        boolean northsouth = types.contains(north.getType()) && types.contains(south.getType());
        boolean updown = types.contains(up.getType()) && types.contains(down.getType());

        if (eastwest) {
            return BlockFace.EAST;
        } else if (northsouth) {
            return BlockFace.NORTH;
        } else if (updown) {
            return BlockFace.UP;
        } else {
            return null;
        }
    }

    // Return the wither's middle head as a block or null if there is no middle head
    Block getMiddleHead(Block head) {
        boolean isMiddleHead = getRowDirection(head, Material.WITHER_SKELETON_SKULL,
                Material.WITHER_SKELETON_WALL_SKULL) != null;

        if (isMiddleHead)
            return head;

        Block newHead = null;

        for (int i = 0; !isMiddleHead; i++) {
            BlockFace bf;

            switch (i) {
                case 0:
                    bf = BlockFace.EAST;
                    break;
                case 1:
                    bf = BlockFace.WEST;
                    break;
                case 2:
                    bf = BlockFace.NORTH;
                    break;
                case 3:
                    bf = BlockFace.SOUTH;
                    break;
                case 4:
                    bf = BlockFace.UP;
                    break;
                case 5:
                    bf = BlockFace.DOWN;
                    break;
                default:
                    return null;
            }

            newHead = head.getRelative(bf);

            if (newHead.getType() == Material.WITHER_SKELETON_SKULL
                    || newHead.getType() == Material.WITHER_SKELETON_WALL_SKULL)
                isMiddleHead = getRowDirection(newHead, Material.WITHER_SKELETON_SKULL,
                        Material.WITHER_SKELETON_WALL_SKULL) != null;
        }

        return newHead;
    }

    // Return the direction in which the body is situated
    // or null if it's not a complete body
    BlockFace getMiddleBody(Block head, Material... types) {
        BlockFace body = null;
        boolean isMiddleBody = false;

        for (int i = 0; !isMiddleBody; i++) {
            switch (i) {
                case 0:
                    body = BlockFace.EAST;
                    break;
                case 1:
                    body = BlockFace.WEST;
                    break;
                case 2:
                    body = BlockFace.NORTH;
                    break;
                case 3:
                    body = BlockFace.SOUTH;
                    break;
                case 4:
                    body = BlockFace.UP;
                    break;
                case 5:
                    body = BlockFace.DOWN;
                    break;
                default:
                    return null;
            }

            Block middle = head.getRelative(body);
            if (Arrays.asList(types).contains(middle.getType())) {
                isMiddleBody = getRowDirection(middle, types) != null;
            }
        }

        return body;
    }

    // Return whether the wither could be built if the head was placed
    public boolean couldWitherBeBuilt(Block head) {
        head = getMiddleHead(head);
        if (head == null)
            return false;

        BlockFace middle = getMiddleBody(head, Material.SOUL_SAND, Material.SOUL_SOIL);
        if (middle == null)
            return false;

        Block bottom = head.getRelative(middle).getRelative(middle);

        return bottom.getType() == Material.SOUL_SAND || bottom.getType() == Material.SOUL_SOIL;
    }

    // Return whether the wither's body is built in survival
    // (whether a survival player should be allowed to create a wither from it)
    public boolean canSurvivalBuildWither(Block head) {
        head = getMiddleHead(head);
        BlockFace headDir = getRowDirection(head, Material.WITHER_SKELETON_SKULL,
                Material.WITHER_SKELETON_WALL_SKULL);
        Block head1 = head.getRelative(headDir);
        Block head2 = head.getRelative(headDir.getOppositeFace());

        BlockFace bodyDir = getMiddleBody(head, Material.SOUL_SAND, Material.SOUL_SOIL);
        Block middle = head.getRelative(bodyDir);
        Block bottom = middle.getRelative(bodyDir);
        Block middle1 = head.getRelative(headDir);
        Block middle2 = head.getRelative(headDir.getOppositeFace());

        boolean headRow = BlockHandler.isTracked(head) || BlockHandler.isTracked(head1)
                || BlockHandler.isTracked(head2);
        boolean middleRow = BlockHandler.isTracked(middle1) || BlockHandler.isTracked(middle2);
        boolean middleBottom = BlockHandler.isTracked(middle) || BlockHandler.isTracked(bottom);

        return !(middleBottom || headRow || middleRow);
    }
}
