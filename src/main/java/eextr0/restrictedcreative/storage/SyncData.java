package eextr0.restrictedcreative.storage;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import eextr0.restrictedcreative.utils.Utils;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

public class SyncData implements Runnable {
    private RestrictedCreative main;
    private Set<String> toAdd;
    private Set<String> toRemove;
    private boolean onDisable;

    public SyncData(RestrictedCreative main, Set<String> fAdd, Set<String> fDel, boolean onDisable) {
        this.main = main;
        this.toAdd = fAdd;
        this.toRemove = fDel;
        this.onDisable = onDisable;
    }

    @Override
    public void run() {
        int addedCount = toAdd.size();
        int removedCount = toRemove.size();

        // If no changes should be made
        if (addedCount + removedCount == 0)
            return;

        long start = System.currentTimeMillis();
        String or = BlockHandler.isUsingSQLite() ? "OR " : "";

        main.getUtils().sendMessage(Bukkit.getConsoleSender(), true, "database.save");

        main.getDB().setAutoCommit(false);

        if (addedCount > 0)
            syncData(toAdd, "INSERT " + or + "IGNORE INTO " + main.getDB().getBlocksTable()
                    + " (block) VALUES (?)", "database.added");

        if (removedCount > 0)
            syncData(toRemove, "DELETE FROM " + main.getDB().getBlocksTable() + " WHERE block = ?",
                    "database.removed");

        main.getDB().setAutoCommit(true);

        if (onDisable) {
            BlockHandler.addToDatabase.clear();
            BlockHandler.removeFromDatabase.clear();

            String took = String.valueOf(System.currentTimeMillis() - start);

            Utils.sendMessage(Bukkit.getConsoleSender(),
                    main.getUtils().getMessage(true, "database.done").replaceAll("%mills%", took));
        } else {
            Bukkit.getScheduler().runTask(main, new Runnable() {
                @Override
                public void run() {
                    BlockHandler.addToDatabase.removeAll(toAdd);
                    BlockHandler.removeFromDatabase.removeAll(toRemove);

                    String took = String.valueOf(System.currentTimeMillis() - start);

                    Utils.sendMessage(Bukkit.getConsoleSender(), main.getUtils()
                            .getMessage(true, "database.done").replaceAll("%mills%", took));
                }
            });
        }
    }

    private void syncData(Set<String> blocks, String statement, String message) {
        PreparedStatement ps = main.getDB().getStatement(statement);
        int count = 0;

        if (RestrictedCreative.DEBUG)
            System.out.println("syncData: starting");

        try {
            for (String block : blocks) {
                ps.setString(1, block);
                ps.addBatch();
                count++;

                if (count % 4096 == 0) {
                    if (RestrictedCreative.DEBUG)
                        System.out.println("executeBatch: " + count);

                    ps.executeBatch();
                    ps.clearBatch();
                }
            }

            if (RestrictedCreative.DEBUG)
                System.out.println("executeBatch: " + count);

            ps.executeBatch();
            ps.clearBatch();
            main.getDB().commit();

            if (RestrictedCreative.DEBUG)
                System.out.println("commited");

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                ps.close();

                if (RestrictedCreative.DEBUG)
                    System.out.println("closed");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Utils.sendMessage(Bukkit.getConsoleSender(), main.getUtils().getMessage(true, message)
                .replaceAll("%blocks%", String.valueOf(count)));
    }
}
