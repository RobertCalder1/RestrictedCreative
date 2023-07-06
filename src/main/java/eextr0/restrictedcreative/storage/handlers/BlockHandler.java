package eextr0.restrictedcreative.storage.handlers;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.SyncData;
import eextr0.restrictedcreative.utils.Utils;
import eextr0.restrictedcreative.utils.external.CoreProtectUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class BlockHandler {
    public static Set<String> addToDatabase = new HashSet<>();
    public static Set<String> removeFromDatabase = new HashSet<>();

    public static boolean isLoadingDone = false;
    public static boolean usingAdvancedLoading = false;

    private static Map<String, Set<String>> blocksInChunk = new HashMap<>();
    private static boolean usingSQLite = false;
    private static int totalCount = -1;

    public static boolean isTracked(Block b) {
        if (b == null)
            return false;

        for (MetadataValue mdv : b.getMetadata("GMC")) {
            if (mdv.asBoolean())
                return true;
        }

        return false;
    }

    public static void setAsTracked(Block b) {
        if (b == null || isTracked(b))
            return;

        b.setMetadata("GMC", RestrictedCreative.getFMV());
        addToDatabase.add(Utils.getBlockString(b));
        removeFromDatabase.remove(Utils.getBlockString(b));

        if (RestrictedCreative.EXTRADEBUG)
            System.out.println("setAsTracked: " + b.getType() + " "
                    + Utils.getChunkString(b.getChunk()) + ", " + Utils.getBlockString(b));
    }

    public static void removeTracking(Block b) {
        if (!isTracked(b))
            return;

        b.removeMetadata("GMC", RestrictedCreative.getInstance());
        removeTracking(Utils.getBlockString(b));

        if (RestrictedCreative.EXTRADEBUG)
            System.out.println("removeTracking: " + b);
    }

    public static void removeTracking(String b) {
        addToDatabase.remove(b);
        removeFromDatabase.add(b);
    }

    public static void breakBlock(Block b, Player p) {
        breakBlock(b, p, true);
    }

    public static void breakBlock(Block b, Player p, boolean update) {
        if (Utils.isInstalled("CoreProtect")) {
            new CoreProtectUtils(b, p, update);
        } else {
            b.setType(Material.AIR, update);
        }

        removeTracking(b);
    }

    public static void setTotalCount(int totalCount) {
        BlockHandler.totalCount = totalCount;
    }

    public static String getTotalCount() {
        return String.valueOf(totalCount);
    }

    public static boolean isUsingSQLite() {
        return usingSQLite;
    }

    public static void setUsingSQLite(boolean usingSQLite) {
        BlockHandler.usingSQLite = usingSQLite;
    }

    public static boolean isTrackedChunk(Chunk c) {
        return !getBlocksInChunk(c).isEmpty();
    }

    public static Set<String> getBlocksInChunk(Chunk c) {
        return getBlocksInChunk(Utils.getChunkString(c));
    }

    public static Set<String> getBlocksInChunk(String c) {
        return blocksInChunk.getOrDefault(c, new HashSet<>());
    }

    public static void removeBlocksInChunk(Chunk c) {
        removeBlocksInChunk(Utils.getChunkString(c));
    }

    public static void removeBlocksInChunk(String c) {
        blocksInChunk.remove(c);
    }

    public static void loadBlocks(Chunk c) {
        long start = System.currentTimeMillis();

        // No need to handle untracked chunks
        if (!isTrackedChunk(c))
            return;

        for (String block : getBlocksInChunk(c)) {
            Block b = Utils.getBlock(block);

            if (b == null || b.isEmpty()) {
                removeTracking(block);
            } else {
                b.setMetadata("GMC", RestrictedCreative.getFMV());
            }
        }

        removeBlocksInChunk(c);

        if (RestrictedCreative.DEBUG)
            System.out.println("loadFromDatabase: " + c + " took "
                    + (System.currentTimeMillis() - start) + "ms");
    }

    public static void loadFromDatabaseBasic(RestrictedCreative main) {
        Bukkit.getScheduler().runTaskAsynchronously(RestrictedCreative.getInstance(), () -> {
            long start = System.currentTimeMillis();

            main.getUtils().sendMessage(Bukkit.getConsoleSender(), true, "database.load");

            // Gets all blocks from database
            ResultSet rs = main.getDB()
                    .executeQuery("SELECT * FROM " + main.getDB().getBlocksTable());

            int count = 0;
            try {
                while (rs.next()) {
                    String block = rs.getString("block");
                    Block b = Utils.getBlock(block);

                    if (b == null || b.isEmpty()) {
                        removeFromDatabase.add(block);
                    } else {
                        count++;
                        b.setMetadata("GMC", RestrictedCreative.getFMV());
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            setTotalCount(count);
            BlockHandler.isLoadingDone = true;

            Utils.sendMessage(Bukkit.getConsoleSender(),
                    main.getUtils().getMessage(true, "database.loaded").replaceAll("%blocks%",
                            getTotalCount()));

            String took = String.valueOf(System.currentTimeMillis() - start);

            Utils.sendMessage(Bukkit.getConsoleSender(), main.getUtils()
                    .getMessage(true, "database.done").replaceAll("%mills%", took));
        });
    }

    public static void loadFromDatabaseAdvanced(RestrictedCreative main) {
        main.getUtils().sendMessage(Bukkit.getConsoleSender(), true, "database.load");

        // Start async processing
        Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
            long start = System.currentTimeMillis();

            // Gets all blocks from database
            ResultSet rs = main.getDB()
                    .executeQuery("SELECT * FROM " + main.getDB().getBlocksTable());

            long current = System.currentTimeMillis();
            float average = current - start;
            long prevTime = current;

            if (RestrictedCreative.DEBUG)
                System.out
                        .println("loadFromDatabaseAdvanced: resultset took " + average + "ms");

            int count = 0;
            Map<String, Set<String>> blocksInChunk = new HashMap<>();

            try {
                while (rs.next()) {
                    String block = rs.getString("block");
                    String chunk = Utils.getBlockChunk(block);

                    // TODO: move somewhere else?
                    /*
                     * String world = blockParts[0]; if (main.getUtils().isDisabledWorld(world)
                     * || Bukkit.getWorld(world) == null) continue;
                     *
                     * addBlockToChunk(chunk, block);
                     */

                    Set<String> blocks = blocksInChunk.getOrDefault(chunk, new HashSet<>());
                    blocks.add(block);
                    blocksInChunk.put(chunk, blocks);

                    count++;
                    if (count % 5000 == 0 && RestrictedCreative.DEBUG) {
                        current = System.currentTimeMillis();
                        average = 5000f / (current - prevTime);
                        prevTime = current;
                        System.out.println("loadFromDatabaseAdvanced: " + count + " avg "
                                + average + " b/ms");
                    }
                }
            } catch (SQLException e) {
                Bukkit.getLogger().log(Level.WARNING,
                        "Data loading was interrupted! Restarting...");
                BlockHandler.loadFromDatabaseAdvanced(main);
            }

            final Map<String, Set<String>> fBlocksInChunk = blocksInChunk;
            final int fBlocksLoaded = count;
            final int fChunksLoaded = blocksInChunk.size();
            final long fTook = System.currentTimeMillis() - start;

            if (RestrictedCreative.DEBUG) {
                current = System.currentTimeMillis();
                average = (float) count / (current - start);
                System.out.println("loadFromDatabaseAdvanced: total: " + count + " blocks, "
                        + fChunksLoaded + " chunks, " + (current - start) + " ms (avg "
                        + average + " b/ms)");
            }

            Bukkit.getScheduler().runTask(main, () -> {
                long start1 = System.currentTimeMillis();
                BlockHandler.blocksInChunk = fBlocksInChunk;

                // Even if plugin.yml has "load: STARTUP" the blocks haven't loaded from the
                // database when chunk load events are fired
                // TODO: remember all the chunks that load before database is loaded
                //  and parse them afterwards
                int radius = 8;
                for (World world : Bukkit.getWorlds()) {
                    // Ignore disabled worlds
                    if (main.getUtils().isDisabledWorld(world.getName()))
                        continue;

                    Chunk center = world.getSpawnLocation().getChunk();

                    for (int x = center.getX() - radius; x < center.getX() + radius; x++) {
                        for (int z = center.getZ() - radius; z < center.getZ()
                                + radius; z++) {
                            Chunk c = world.getChunkAt(x, z);
                            loadBlocks(c);
                        }
                    }
                }

                long chunksTook = System.currentTimeMillis() - start1;
                if (RestrictedCreative.DEBUG)
                    System.out.println("loadFromDatabaseAdvanced: chunk loading took "
                            + chunksTook + " ms");

                setTotalCount(fBlocksLoaded);
                BlockHandler.isLoadingDone = true;

                Utils.sendMessage(Bukkit.getConsoleSender(),
                        main.getUtils().getMessage(true, "database.loaded")
                                .replaceAll("%blocks%", getTotalCount())
                                .replaceAll("%chunks%", String.valueOf(fChunksLoaded)));

                String took = String.valueOf(fTook + chunksTook);

                Utils.sendMessage(Bukkit.getConsoleSender(), main.getUtils()
                        .getMessage(true, "database.done").replaceAll("%mills%", took));
            });
        });
    }

    public static void startDataSync(RestrictedCreative main) {
        int interval = main.getSettings().getInt("general.saving.interval");

        Bukkit.getServer().getScheduler().runTaskTimer(main, () -> {
            final HashSet<String> fAdd = new HashSet<>(addToDatabase);
            final HashSet<String> fDel = new HashSet<>(removeFromDatabase);

            Bukkit.getScheduler().runTaskAsynchronously(main,
                    new SyncData(main, fAdd, fDel, false));
        }, interval, interval);
    }
}
