package eextr0.restrictedcreative.utils;

import eextr0.restrictedcreative.RestrictedCreative;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.type.*;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MaterialHandler {
    private static final HashSet<Material> top = new HashSet<>(Arrays.asList(Material.DEAD_BUSH,
            Material.DANDELION, Material.ORANGE_TULIP, Material.PINK_TULIP, Material.RED_TULIP,
            Material.WHITE_TULIP, Material.BLUE_ORCHID, Material.ALLIUM, Material.POPPY,
            Material.AZURE_BLUET, Material.OXEYE_DAISY, Material.RED_MUSHROOM,
            Material.BROWN_MUSHROOM, Material.SUGAR_CANE, Material.MELON_STEM,
            Material.PUMPKIN_STEM, Material.ATTACHED_MELON_STEM, Material.ATTACHED_PUMPKIN_STEM,
            Material.CACTUS, Material.LILY_PAD, Material.KELP, Material.KELP_PLANT, Material.GRASS,
            Material.FERN, Material.TALL_SEAGRASS, Material.STONE_PRESSURE_PLATE,
            Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Material.NETHER_WART));

    // Crops
    private static final HashSet<Material> crops = new HashSet<>(
            Arrays.asList(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS));

    // Double plants
    private static final HashSet<Material> doublePlants = new HashSet<>(
            Arrays.asList(Material.TALL_GRASS, Material.LARGE_FERN));

    // Placeable entities (but not hangables)
    private static final HashSet<Material> entities = new HashSet<>(Arrays.asList(Material.END_CRYSTAL,
            Material.ARMOR_STAND, Material.MINECART, Material.CHEST_MINECART,
            Material.COMMAND_BLOCK_MINECART, Material.FURNACE_MINECART, Material.HOPPER_MINECART,
            Material.TNT_MINECART, Material.ACACIA_BOAT, Material.BIRCH_BOAT,
            Material.DARK_OAK_BOAT, Material.JUNGLE_BOAT, Material.OAK_BOAT, Material.SPRUCE_BOAT));

    public static boolean needsBlockBelow(Block b) {
        BlockData bd = b.getBlockData();
        Material m = b.getType();

        if (getTop().contains(m))
            return true;

        if (bd instanceof Cake)
            return true;
        if (bd instanceof Comparator)
            return true;
        if (bd instanceof RedstoneWire)
            return true;
        if (bd instanceof Repeater)
            return true;
        if (bd instanceof Sapling)
            return true;
        if (bd instanceof SeaPickle)
            return true;
        if (bd instanceof Sign)
            return true;
        if (bd instanceof Snow)
            return true;

        // Standing banners are not directional
        if (Tag.BANNERS.isTagged(m) && !(bd instanceof Directional))
            return true;
        if (Tag.CORALS.isTagged(m))
            return true;
        if (Tag.FLOWER_POTS.isTagged(m))
            return true;
        if (Tag.WOODEN_PRESSURE_PLATES.isTagged(m))
            return true;

        if (isDoor(b))
            return true;
        if (isCrop(b))
            return true;
        if (isRail(b))
            return true;
        if (isDoublePlant(b))
            return true;
        if (isCarpet(b))
            return true;

        return getNeededFace(b) == BlockFace.DOWN;
    }

    @SuppressWarnings("deprecation")
    public static BlockFace getNeededFace(Block b) {
        BlockData bd = b.getBlockData();

        if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_14)) {
            if (bd instanceof Lantern) {
                if (RestrictedCreative.DEBUG)
                    System.out.println("getNeededFace: Lantern");

                return ((Lantern) bd).isHanging() ? BlockFace.UP : BlockFace.DOWN;
            }

            if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_16)) {
                if (b.getType() == Material.valueOf("SOUL_TORCH"))
                    return BlockFace.DOWN;
            }
        }

        if (b.getType() == Material.TORCH)
            return BlockFace.DOWN;
        if (b.getType() == Material.REDSTONE_TORCH)
            return BlockFace.DOWN;

        if (!(bd instanceof Directional))
            return null;

        Directional d = (Directional) bd;

        // getOppositeFace() because getFacing() returns where the item is "looking",
        // opposite of where it is attached
        // NB: this doesn't seem to be always true
        if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_16)) {
            if (b.getType() == Material.valueOf("SOUL_WALL_TORCH"))
                return ((Directional) bd).getFacing().getOppositeFace();
        }
        if (b.getType() == Material.WALL_TORCH)
            return ((Directional) bd).getFacing().getOppositeFace(); // TESTED 1.15.2
        if (bd instanceof RedstoneWallTorch)
            return d.getFacing().getOppositeFace();
        if (bd instanceof Cocoa)
            return d.getFacing(); // TESTED 1.13.2
        if (bd instanceof Ladder)
            return d.getFacing().getOppositeFace(); // TESTED 1.13.2
        if (bd instanceof CoralWallFan)
            return d.getFacing().getOppositeFace(); // TESTED 1.13.2
        if (bd instanceof TripwireHook)
            return d.getFacing().getOppositeFace(); // TESTED 1.13.2
        if (bd instanceof WallSign)
            return d.getFacing().getOppositeFace(); // TESTED 1.13.2
        if (Utils.isVersionOlderThanInclusive(MinecraftVersion.v1_14) && bd instanceof Switch) {
            // TODO: FaceAttachable
            switch (((Switch) bd).getFace()) {
                case CEILING:
                    return BlockFace.UP;
                case FLOOR:
                    return BlockFace.DOWN;
                case WALL:
                    return d.getFacing().getOppositeFace(); // TESTED 1.13.2
            }
        }
        if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_15)
                && bd instanceof FaceAttachable) {
            switch (((FaceAttachable) bd).getAttachedFace()) {
                case CEILING:
                    return BlockFace.UP;
                case FLOOR:
                    return BlockFace.DOWN;
                case WALL:
                    return d.getFacing().getOppositeFace(); // TESTED 1.15.2
            }
        }
        if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_14) && bd instanceof Bell) {
            switch (((Bell) bd).getAttachment()) {
                case CEILING:
                    return BlockFace.UP;
                case FLOOR:
                    return BlockFace.DOWN;
                case SINGLE_WALL:
                case DOUBLE_WALL:
                    return d.getFacing(); // TESTED 1.14
            }
        }

        Material m = b.getType();

        if (Tag.BANNERS.isTagged(m))
            return d.getFacing().getOppositeFace(); // TESTED 1.13.2

        return null;
    }

    public static boolean isDoor(Block b) {
        return b.getBlockData() instanceof Door;
    }

    public static boolean isCrop(Block b) {
        return crops.contains(b.getType());
    }

    public static boolean isRail(Block b) {
        return b.getBlockData() instanceof Rail;
    }

    public static boolean isDoublePlant(Block b) {
        if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_15)) {
            doublePlants.addAll(Tag.TALL_FLOWERS.getValues());
        } else {
            doublePlants.add(Material.SUNFLOWER);
            doublePlants.add(Material.LILAC);
            doublePlants.add(Material.ROSE_BUSH);
            doublePlants.add(Material.PEONY);
        }
        return doublePlants.contains(b.getType());
    }

    public static boolean isCarpet(Block b) {
        return Tag.CARPETS.isTagged(b.getType());
    }

    public static boolean isPlaceableEntity(Material m) {
        return entities.contains(m);
    }

    public static List<ItemStack> getArmorList(String type) {
        switch (type.toLowerCase()) {
            case "netherite":
                return Arrays.asList(new ItemStack(Material.valueOf("NETHERITE_BOOTS")),
                        new ItemStack(Material.valueOf("NETHERITE_LEGGINGS")),
                        new ItemStack(Material.valueOf("NETHERITE_CHESTPLATE")),
                        new ItemStack(Material.valueOf("NETHERITE_HELMET")));
            case "diamond":
                return Arrays.asList(new ItemStack(Material.DIAMOND_BOOTS), new ItemStack(Material.DIAMOND_LEGGINGS),
                        new ItemStack(Material.DIAMOND_CHESTPLATE), new ItemStack(Material.DIAMOND_HELMET));
            case "golden":
                return Arrays.asList(new ItemStack(Material.GOLDEN_BOOTS), new ItemStack(Material.GOLDEN_LEGGINGS),
                        new ItemStack(Material.GOLDEN_CHESTPLATE), new ItemStack(Material.GOLDEN_HELMET));
            case "iron":
                return Arrays.asList(new ItemStack(Material.IRON_BOOTS), new ItemStack(Material.IRON_LEGGINGS),
                        new ItemStack(Material.IRON_CHESTPLATE), new ItemStack(Material.IRON_HELMET));
            case "chainmail":
                return Arrays.asList(new ItemStack(Material.CHAINMAIL_BOOTS), new ItemStack(Material.CHAINMAIL_LEGGINGS),
                        new ItemStack(Material.CHAINMAIL_CHESTPLATE), new ItemStack(Material.CHAINMAIL_HELMET));
            case "leather":
            default:
                return Arrays.asList(new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS),
                        new ItemStack(Material.LEATHER_CHESTPLATE), new ItemStack(Material.LEATHER_HELMET));
        }
    }

    private static HashSet<Material> getTop() {
        if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_14)) {
            top.add(Material.valueOf("BAMBOO"));
            top.add(Material.valueOf("BAMBOO_SAPLING"));
            top.add(Material.valueOf("CORNFLOWER"));
            top.add(Material.valueOf("LILY_OF_THE_VALLEY"));
            top.add(Material.valueOf("WITHER_ROSE"));
            top.add(Material.valueOf("SWEET_BERRY_BUSH"));

            if (Utils.isVersionNewerThanInclusive(MinecraftVersion.v1_16)) {
                top.add(Material.valueOf("CRIMSON_FUNGUS"));
                top.add(Material.valueOf("WARPED_FUNGUS"));
                top.add(Material.valueOf("CRIMSON_ROOTS"));
                top.add(Material.valueOf("WARPED_ROOTS"));
                top.add(Material.valueOf("NETHER_SPROUTS"));
                top.add(Material.valueOf("POLISHED_BLACKSTONE_PRESSURE_PLATE"));
            }
        } else {
            top.add(Material.valueOf("DANDELION_YELLOW"));
        }
        return top;
    }
}
