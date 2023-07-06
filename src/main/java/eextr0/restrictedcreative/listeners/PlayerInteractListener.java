package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import eextr0.restrictedcreative.storage.handlers.CommandHandler;
import eextr0.restrictedcreative.storage.handlers.EntityHandler;
import eextr0.restrictedcreative.utils.MaterialHandler;
import eextr0.restrictedcreative.utils.Utils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class PlayerInteractListener implements Listener {
    private final RestrictedCreative main;

    private static final List<BlockFace> ALL_SIDES = Arrays.asList(BlockFace.DOWN, BlockFace.UP,
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    public PlayerInteractListener(RestrictedCreative main) {
        this.main = main;
    }

    private RestrictedCreative getMain() {
        return this.main;
    }

    /*
     * Represents an event that is called when a player interacts with an object or
     * air, potentially fired once for each hand. The hand can be determined using
     * getHand().
     *
     * This event will fire as cancelled if the vanilla behavior is to do nothing
     * (e.g interacting with air)
     */
    // LOWEST required for signshops and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractLowest(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        // No need to track entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to track non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        ItemStack is = e.getItem();
        Material ma = is == null ? Material.AIR : is.getType();

        // Region check
        if ((getMain().getSettings().isEnabled("limit.regions.owner-based.enabled")
                || getMain().getSettings().isEnabled("limit.regions.whitelist.enabled"))
                && getMain().getUtils().cannotBuildHere(p, e.getClickedBlock(), ma)) {
            e.setCancelled(true);
            getMain().getUtils().sendMessage(p, true, "disabled.region");
            return;
        }

        // Confiscate
        if (is != null && getMain().getUtils().shouldConfiscate(p, is)) {
            p.getInventory().remove(is);
            e.setCancelled(true);

            if (RestrictedCreative.DEBUG)
                System.out.println("shouldConfiscate: " + is.getType());

            return;
        }

        // We only need to control right click interactions on blocks
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block b = e.getClickedBlock();

        // If block doesn't exist
        if (b == null || b.getType() == Material.AIR)
            return;

        Material m = b.getType();

        // No need to control bypassed players
        if (p.hasPermission("rc.bypass.disable.interacting.on-ground")
                || p.hasPermission("rc.bypass.disable.interacting.on-ground." + m))
            return;

        // No need to control non-blocked items
        if (!getMain().getSettings().getMaterialList("disable.interacting.on-ground").contains(m))
            return;

        e.setCancelled(true);

        // Prevent double message
        if (e.getHand() != EquipmentSlot.OFF_HAND)
            getMain().getUtils().sendMessage(p, true, "disabled.general");
    }

    /*
     * Represents an event that is called when a player interacts with an object or
     * air, potentially fired once for each hand. The hand can be determined using
     * getHand().
     *
     * This event will fire as cancelled if the vanilla behavior is to do nothing
     * (e.g interacting with air)
     */
    // "ignoreCancelled = true" skipped EYE_OF_ENDER for the latter reason
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        // No need to track entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to track non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        ItemStack is = e.getItem();

        if (is == null || is.getType() == Material.AIR)
            return;

        Material m = is.getType();

        // No need to track bypassed players
        if (p.hasPermission("rc.bypass.disable.interacting.in-hand")
                || p.hasPermission("rc.bypass.disable.interacting.in-hand." + m))
            return;

        // No need to control non-blocked items
        if (!getMain().getSettings().getMaterialList("disable.interacting.in-hand").contains(m))
            return;

        e.setCancelled(true);

        // Prevent double message
        if (e.getHand() != EquipmentSlot.OFF_HAND)
            getMain().getUtils().sendMessage(p, true, "disabled.general");
    }

    /*
     * Represents an event that is called when a player interacts with an object or
     * air, potentially fired once for each hand. The hand can be determined using
     * getHand().
     *
     * This event will fire as cancelled if the vanilla behavior is to do nothing
     * (e.g interacting with air)
     */
    // HIGHEST required for WorldGuard and similar plugins
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractHighest(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        // No need to track entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // We only need to control right click interactions on blocks
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block b = e.getClickedBlock();

        if (b == null || b.getType() == Material.AIR)
            return;

        /* Command /block */
        if (CommandHandler.isInfoWithCommand(p)) {
            if (BlockHandler.isTracked(b)) {
                Utils.sendMessage(p, getMain().getUtils().getMessage(true, "block.info.true")
                        .replaceAll("%material%", b.getType().toString()));
            } else {
                Utils.sendMessage(p, getMain().getUtils().getMessage(true, "block.info.false")
                        .replaceAll("%material%", b.getType().toString()));
            }

            CommandHandler.removeInfoWithCommand(p);
            e.setCancelled(true);
        } else if (CommandHandler.isAddWithCommand(p)) {
            BlockHandler.setAsTracked(b);
            CommandHandler.removeAddWithCommand(p);
            e.setCancelled(true);

            Utils.sendMessage(p, getMain().getUtils().getMessage(true, "block.add.added")
                    .replaceAll("%material%", b.getType().toString()));
        } else if (CommandHandler.isRemoveWithCommand(p)) {
            BlockHandler.removeTracking(b);
            CommandHandler.removeRemoveWithCommand(p);
            e.setCancelled(true);

            Utils.sendMessage(p, getMain().getUtils().getMessage(true, "block.remove.removed")
                    .replaceAll("%material%", b.getType().toString()));
        }

        // Creative placed cake shouldn't be edible
        if (BlockHandler.isTracked(b) && b.getType() == Material.CAKE) {
            getMain().getUtils().sendMessage(p, true, "disabled.interact");

            e.setCancelled(true);
            return;
        }

        BlockData bd = b.getBlockData();

        // Door and trapdoor attachment check
        if (bd instanceof Door || bd instanceof TrapDoor) {
            checkForAttachments(p, b);
        }

        if (e.getItem() == null)
            return;

        Material m = e.getItem().getType();

        // Pumpkins can be carved with shears and they drop seeds
        if (BlockHandler.isTracked(b) && m == Material.SHEARS && b.getType() == Material.PUMPKIN) {
            getMain().getUtils().sendMessage(p, true, "disabled.interact");

            e.setCancelled(true);
            return;
        }

        // No need to track non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // No need to track bypassed players
        if (p.hasPermission("rc.bypass.tracking.blocks")
                || p.hasPermission("rc.bypass.tracking.blocks." + m))
            return;

        // No need to track non-entity materials
        if (!MaterialHandler.isPlaceableEntity(m))
            return;

        EntityHandler.addToTrackedLocs(b.getLocation());
    }

    /*
     * Represents an event that is called when a player right clicks an entity.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();

        // No need to track entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        Entity en = e.getRightClicked();
        EntityType et = en.getType();

        /* Command /block */
        if (CommandHandler.isInfoWithCommand(p)) {
            if (EntityHandler.isTracked(en)) {
                Utils.sendMessage(p, getMain().getUtils().getMessage(true, "block.info.true")
                        .replaceAll("%material%", et.toString()));
            } else {
                Utils.sendMessage(p, getMain().getUtils().getMessage(true, "block.info.false")
                        .replaceAll("%material%", et.toString()));
            }

            CommandHandler.removeInfoWithCommand(p);
            e.setCancelled(true);
        } else if (CommandHandler.isAddWithCommand(p)) {
            EntityHandler.setAsTracked(en);
            CommandHandler.removeAddWithCommand(p);
            e.setCancelled(true);

            Utils.sendMessage(p, getMain().getUtils().getMessage(true, "block.add.added")
                    .replaceAll("%material%", et.toString()));
        } else if (CommandHandler.isRemoveWithCommand(p)) {
            EntityHandler.removeTracking(en);
            CommandHandler.removeRemoveWithCommand(p);
            e.setCancelled(true);

            Utils.sendMessage(p, getMain().getUtils().getMessage(true, "block.remove.removed")
                    .replaceAll("%material%", et.toString()));
        }

        // No need to track non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // If creative player wants to put something in an empty item frame
        if (en instanceof ItemFrame && !p.hasPermission("rc.bypass.tracking.blocks")
                && !p.hasPermission("rc.bypass.tracking.blocks." + et)) {
            ItemStack is = e.getHand() == EquipmentSlot.HAND ? p.getInventory().getItemInMainHand()
                    : p.getInventory().getItemInOffHand();
            ItemFrame frame = (ItemFrame) en;
            ItemStack fis = frame.getItem();

            if (!is.getType().isAir() && !fis.getType().isAir()) {
                EntityHandler.setAsTrackedItem(frame);
                return;
            }
        }

        // No need to control disabled features
        if (!getMain().getSettings().isEnabled("limit.interact.entities"))
            return;

        // No need to track bypassed players
        if (p.hasPermission("rc.bypass.limit.interact.entities")
                || p.hasPermission("rc.bypass.limit.interact.entities." + et))
            return;

        e.setCancelled(true);

        // Prevent double message
        if (e.getHand() != EquipmentSlot.OFF_HAND)
            getMain().getUtils().sendMessage(p, true, "disabled.general");
    }

    /*
     * Called when a player interacts with an armor stand and will either swap,
     * retrieve or place an item.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        Player p = e.getPlayer();
        ArmorStand a = e.getRightClicked();
        EquipmentSlot slot = e.getSlot();

        // No need to track entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to track bypassed players
        if (p.hasPermission("rc.bypass.limit.interact.entities")
                || p.hasPermission("rc.bypass.limit.interact.entities." + a.getType()))
            return;

        // No need to control disabled features
        if (p.getGameMode() == GameMode.CREATIVE
                && getMain().getSettings().isEnabled("limit.interact.entities")) {
            getMain().getUtils().sendMessage(p, true, "disabled.general");

            if (RestrictedCreative.DEBUG)
                System.out.println("cancelEvent: " + slot);

            e.setCancelled(true);
            return;
        }

        if (e.getArmorStandItem().getType().isAir())
            return;

        // Survival player is taking creative item from armor stand
        if (p.getGameMode() != GameMode.CREATIVE && !e.getArmorStandItem().getType().isAir()
                && EntityHandler.isTrackedSlot(a, slot)) {
            e.setCancelled(true);

            EntityEquipment inv = a.getEquipment();
            ItemStack air = new ItemStack(Material.AIR);

            if (inv == null)
                return;

            switch (slot) {
                case CHEST:
                    inv.setChestplate(air);
                    break;
                case FEET:
                    inv.setBoots(air);
                    break;
                case HEAD:
                    inv.setHelmet(air);
                    break;
                case LEGS:
                    inv.setLeggings(air);
                    break;
                case HAND:
                    inv.setItemInMainHand(air);
                    break;
                case OFF_HAND:
                    inv.setItemInOffHand(air);
                    break;
                default:
                    break;
            }

            if (RestrictedCreative.DEBUG)
                System.out.println("removeSlotTracking: " + slot);

            EntityHandler.removeSlotTracking(a, slot);

            // Prevent double message
            if (e.getHand() != EquipmentSlot.OFF_HAND)
                getMain().getUtils().sendMessage(p, true, "disabled.interact");

            return;
        }

        // Only creative players going forward
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onPlayerArmorStandManipulate: " + slot);

        // Creative player is taking a creative item from armor stand
        if (e.getArmorStandItem().getType() != Material.AIR && EntityHandler.isTrackedSlot(a, slot))
            EntityHandler.removeSlotTracking(a, slot);

        // Creative player is putting an item on the armor stand
        if (e.getPlayerItem().getType() != Material.AIR)
            EntityHandler.setAsTrackedSlot(a, slot);
    }

    /*
     * Called when one Entity breeds with another Entity.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent e) {
        // Only players going forward
        if (!(e.getBreeder() instanceof Player))
            return;

        Player p = (Player) e.getBreeder();

        // No need to track entities in disabled worlds
        if (getMain().getUtils().isDisabledWorld(p.getWorld().getName()))
            return;

        // No need to track non-creative players
        if (p.getGameMode() != GameMode.CREATIVE)
            return;

        // No need to control disabled features
        if (!getMain().getSettings().isEnabled("limit.interact.breeding"))
            return;

        EntityType et = e.getEntityType();

        // No need to track bypassed players
        if (p.hasPermission("rc.bypass.limit.interact.breeding")
                || p.hasPermission("rc.bypass.limit.interact.breeding." + et))
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("onEntityBreed: " + et);

        e.setCancelled(true);
    }

    private void checkForAttachments(Player p, Block b) {
        checkSurroundingBlocks(p, b, ALL_SIDES);
        BlockData bd = b.getBlockData();

        // Check other half of the door as well
        if (bd instanceof Door) {
            Door door = (Door) bd;

            Block bl = door.getHalf() == Bisected.Half.TOP ? b.getRelative(BlockFace.DOWN)
                    : b.getRelative(BlockFace.UP);
            checkSurroundingBlocks(p, bl, ALL_SIDES);
        }
    }

    private void checkSurroundingBlocks(Player p, Block door, List<BlockFace> sides) {
        for (BlockFace bf : sides) {
            Block attachable = door.getRelative(bf);

            // Checks if the surrounding block is placed in creative
            if (!BlockHandler.isTracked(attachable))
                continue;

            BlockFace dir = MaterialHandler.getNeededFace(attachable);

            // If it's attached to the original door
            if (attachable.getFace(door) == dir && isNeededDirectionOk(attachable, dir))
                BlockHandler.breakBlock(attachable, p);
        }
    }

    private boolean isNeededDirectionOk(Block bl, BlockFace dir) {
        BlockData bd = bl.getBlockData();

        // Signs can be attached to a door no matter the direction
        if (bd instanceof WallSign)
            return false;

        if (bd instanceof Door) {
            if (RestrictedCreative.DEBUG)
                System.out.println("getDoorFace: " + getDoorFace((Door) bd) + " vs " + dir);

            // If they're not facing each other, they're in illegal position
            return getDoorFace((Door) bd) == dir.getOppositeFace();
        }

        if (bd instanceof TrapDoor) {
            TrapDoor door = (TrapDoor) bd;
            BlockFace face = door.getFacing();
            boolean open = door.isOpen();

            if (RestrictedCreative.DEBUG)
                System.out.println("trapdoor: " + face + " vs " + dir + " " + open);

            // Trapdoors always face down when they're closed
            if (!open && dir != BlockFace.UP)
                return false;

            // If trapdoors are open, they have the same face as the blocks attached to them
            return !open || dir == face;
        }

        return true;
    }

    private BlockFace getDoorFace(Door door) {
        Door.Hinge hinge = door.getHinge();
        BlockFace face = door.getFacing();

        if (RestrictedCreative.DEBUG)
            System.out.println("getDoorFace: " + hinge + " " + face + " " + door.isOpen());

        // If a door is closed, it's actually facing the opposite of placement face
        if (!door.isOpen())
            return face.getOppositeFace();

        return getHingeFace(face, hinge).getOppositeFace();
    }

    private BlockFace getHingeFace(BlockFace face, Door.Hinge hinge) {
        if (RestrictedCreative.DEBUG)
            System.out.println("getHingeFace: " + face + " " + hinge);

        switch (face) {
            case NORTH:
                return hinge == Door.Hinge.LEFT ? BlockFace.EAST : BlockFace.WEST;
            case EAST:
                return hinge == Door.Hinge.LEFT ? BlockFace.SOUTH : BlockFace.NORTH;
            case SOUTH:
                return hinge == Door.Hinge.LEFT ? BlockFace.WEST : BlockFace.EAST;
            case WEST:
                return hinge == Door.Hinge.LEFT ? BlockFace.NORTH : BlockFace.SOUTH;
            default:
                return null;
        }
    }
}
