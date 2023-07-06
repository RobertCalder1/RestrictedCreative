package eextr0.restrictedcreative;

import com.sk89q.worldedit.WorldEdit;
import eextr0.restrictedcreative.commands.MainCommand;
import eextr0.restrictedcreative.commands.SwitchCommand;
import eextr0.restrictedcreative.listeners.*;
import eextr0.restrictedcreative.storage.ConfigProvider;
import eextr0.restrictedcreative.storage.Database;
import eextr0.restrictedcreative.storage.SyncData;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import eextr0.restrictedcreative.storage.handlers.InventoryHandler;
import eextr0.restrictedcreative.storage.handlers.PermissionHandler;
import eextr0.restrictedcreative.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RestrictedCreative extends JavaPlugin {

    public static boolean DEBUG = false;
    public static boolean EXTRADEBUG = false;

    private Database database;
    private Utils utils;

    private ConfigProvider config;
    private ConfigProvider messages;

    private static FixedMetadataValue fmv;
    private WorldEditListener weListener;

    @Override
    public void onEnable() {
        setFMV(new FixedMetadataValue(getInstance(), "true"));
        setUtils(new Utils(this));

        loadConfigs();
        registerCommands();
        registerListeners();
        loadData();
    }

    @Override
    public void onDisable() {
        for (Player p : getServer().getOnlinePlayers()) {
            getUtils().saveInventory(p);
        }

        // Save data for the last time
        final Set<String> fAdd = new HashSet<>(BlockHandler.addToDatabase);
        final Set<String> fDel = new HashSet<>(BlockHandler.removeFromDatabase);
        new SyncData(this, fAdd, fDel, true).run();

        getDB().closeConnection();
    }

    /**
     * Load settings and messages from config files
     */
    public void loadConfigs() {
        this.config = new ConfigProvider(this, "config.yml");
        this.messages = new ConfigProvider(this, "messages.yml");
    }

    /**
     * Reload settings and messages from config files, don't overwrite
     */
    public void reloadConfigs() {
        this.config.reload();
        this.messages.reload();

        // Reload command messages, aliases etc as well
        registerCommands();
    }

    /**
     * Register event listeners
     */
    public void registerListeners() {
        if (Utils.isInstalled("WorldEdit")) {
            if (DEBUG)
                System.out.println("registerListeners: WorldEdit "
                        + getSettings().isEnabled("tracking.worldedit.enabled"));

            if (this.weListener != null) {
                if (DEBUG)
                    System.out.println("registerListeners: unregister WorldEdit");

                WorldEdit.getInstance().getEventBus().unregister(this.weListener);
                this.weListener = null;
            }

            if (getSettings().isEnabled("tracking.worldedit.enabled")) {
                if (DEBUG)
                    System.out.println("registerListeners: register WorldEdit");

                this.weListener = new WorldEditListener(this);
                WorldEdit.getInstance().getEventBus().register(this.weListener);
            }
        }

        // In case of plugin reload
        HandlerList.unregisterAll(this);

        PluginManager manager = getServer().getPluginManager();

        manager.registerEvents(new BlockPlaceListener(this), this);
        manager.registerEvents(new BlockBreakListener(this), this);
        manager.registerEvents(new BlockUpdateListener(this), this);
        manager.registerEvents(new BlockChangeListener(this), this);
        manager.registerEvents(new BlockExplodeListener(this), this);
        manager.registerEvents(new BlockPistonListener(this), this);

        manager.registerEvents(new ChunkListener(), this);

        manager.registerEvents(new EntityDamageListener(this), this);
        manager.registerEvents(new EntityCreateListener(this), this);

        manager.registerEvents(new PlayerInteractListener(this), this);
        manager.registerEvents(new PlayerInventoryListener(this), this);
        manager.registerEvents(new PlayerItemListener(this), this);
        manager.registerEvents(new PlayerMiscListener(this), this);
    }

    /**
     * Register plugin commands
     */
    private void registerCommands() {
        // Register commands
        for (Map.Entry<String, Map<String, Object>> entry : getDescription().getCommands().entrySet()) {
            String name = entry.getKey();
            PluginCommand cmd = getCommand(name);

            if (cmd == null)
                continue;

            cmd.setExecutor(getExecutor(name));
            cmd.setPermissionMessage(getMessages().getMessage("no-permission"));
            cmd.setDescription(getSettings().getMessage("commands." + name + ".description"));
            cmd.setUsage(getSettings().getMessage("commands." + name + ".usage"));
        }

        // Using old way of handling aliases... TODO
        PermissionHandler.setUsingOldAliases(true);
    }

    /**
     * Load data from database
     */
    private void loadData() {
        setDB(new Database(this, null));

        BlockHandler.setUsingSQLite(
                getSettings().getString("database.type").equalsIgnoreCase("sqlite"));
        InventoryHandler.setForceGamemodeEnabled(Utils.isForceGamemodeEnabled());

        // Tracked blocks
        getDB().executeUpdate("CREATE TABLE IF NOT EXISTS " + getDB().getBlocksTable()
                + " (block VARCHAR(255), UNIQUE (block))");

        // Tracked inventories
        if (BlockHandler.isUsingSQLite()) {
            String tableName = getDB().getInvsTable();
            ResultSet rs = getDB()
                    .executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='"
                            + tableName + "'");

            boolean tableExists = false;
            try {
                tableExists = rs.next();
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (tableExists)
                tableName += "_tmp";

            getDB().executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName
                    + " (player VARCHAR(36), type TINYINT(1), storage TEXT, armor TEXT, extra TEXT, effects TEXT, xp BIGINT, lastused BIGINT(11), UNIQUE (player))");

            if (tableExists) {
                // Retain only the latest inventory from each player
                try {
                    Statement stm = getDB().getConnection().createStatement();
                    stm.addBatch("INSERT INTO " + tableName
                            + " SELECT player, type, storage, armor, extra, effects, xp, MAX(lastused) as lastused FROM "
                            + getDB().getInvsTable() + " GROUP BY player");
                    stm.addBatch("DROP TABLE " + getDB().getInvsTable());
                    stm.addBatch(
                            "ALTER TABLE " + tableName + " RENAME TO " + getDB().getInvsTable());
                    stm.executeBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            getDB().executeUpdate("CREATE TABLE IF NOT EXISTS " + getDB().getInvsTable()
                    + " (player VARCHAR(36), type TINYINT(1), storage TEXT, armor TEXT, extra TEXT, effects TEXT, xp BIGINT, lastused BIGINT(11), UNIQUE (player))");
        }

        // Purge old database entries
        if (getSettings().isEnabled("general.saving.inventories.purge.enabled")) {
            long survival = Instant.now().getEpochSecond()
                    - 86400L * getSettings().getInt("general.saving.inventories.purge.survival");
            long creative = Instant.now().getEpochSecond()
                    - 86400L * getSettings().getInt("general.saving.inventories.purge.creative");

            getDB().executeUpdate(
                    "DELETE FROM " + getDB().getInvsTable() + " WHERE type = 0 AND lastused < "
                            + survival + " OR type = 1 AND lastused < " + creative);
        }

        if (getSettings().isEnabled("general.loading.use-old-system")) {
            BlockHandler.usingAdvancedLoading = false;
            BlockHandler.loadFromDatabaseBasic(this);
        } else {
            BlockHandler.usingAdvancedLoading = true;
            BlockHandler.loadFromDatabaseAdvanced(this);
        }

        BlockHandler.startDataSync(this);
    }

    private CommandExecutor getExecutor(String name) {
        switch (name) {
            case "rc":
                return new MainCommand(this);
            case "creative":
                return new SwitchCommand(this, GameMode.CREATIVE);
            case "survival":
                return new SwitchCommand(this, GameMode.SURVIVAL);
            case "adventure":
                return new SwitchCommand(this, GameMode.ADVENTURE);
            case "spectator":
                return new SwitchCommand(this, GameMode.SPECTATOR);
            default:
                return null;
        }
    }

    /**
     * @return RestrictedCreative plugin instance
     */
    public static Plugin getInstance() {
        return Bukkit.getPluginManager().getPlugin("RestrictedCreative");
    }

    /**
     * @return Settings provider
     */
    public ConfigProvider getSettings() {
        return this.config;
    }

    /**
     * @return Messages provider
     */
    public ConfigProvider getMessages() {
        return this.messages;
    }

    /**
     * @return FixedMetadataValue used by RestrictedCreative
     */
    public static FixedMetadataValue getFMV() {
        return fmv;
    }

    /**
     * @param fmv FixedMetadataValue used by RestrictedCreative
     */
    public static void setFMV(FixedMetadataValue fmv) {
        RestrictedCreative.fmv = fmv;
    }

    public Database getDB() {
        return database;
    }

    public void setDB(Database database) {
        this.database = database;
    }

    public Utils getUtils() {
        return utils;
    }

    public void setUtils(Utils utils) {
        this.utils = utils;
    }
}
