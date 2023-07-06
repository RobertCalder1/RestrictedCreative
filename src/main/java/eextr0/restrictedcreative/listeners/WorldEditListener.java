package eextr0.restrictedcreative.listeners;

import com.google.common.eventbus.Subscribe;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import eextr0.restrictedcreative.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WorldEditListener {
    private RestrictedCreative main;

    public WorldEditListener(RestrictedCreative main) {
        this.main = main;

        if (RestrictedCreative.DEBUG)
            System.out.println("Loaded WorldEditListener");
    }

    @Subscribe
    public void wrapForLogging(EditSessionEvent e) {
        // Otherwise, this method is called 3 times for a single event
        if (e.getStage() != EditSession.Stage.BEFORE_CHANGE)
            return;

        Actor a = e.getActor();

        if (a == null || !a.isPlayer() || e.getWorld() == null)
            return;

        String world = e.getWorld().getName();

        if (main.getUtils().isDisabledWorld(world))
            return;

        Player p = Bukkit.getServer().getPlayer(a.getUniqueId());
        World w = main.getServer().getWorld(world);

        if (p.hasPermission("rc.bypass.tracking.worldedit"))
            return;

        if (!main.getSettings().isEnabled("tracking.worldedit.extended")
                && p.getGameMode() != GameMode.CREATIVE)
            return;

        if (RestrictedCreative.DEBUG)
            System.out.println("wrapForLogging: " + p.getGameMode());

        e.setExtent(new AbstractDelegateExtent(e.getExtent()) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public boolean setBlock(BlockVector3 position, BlockStateHolder newBlock)
                    throws WorldEditException {
                Block b = w.getBlockAt(position.getBlockX(), position.getBlockY(),
                        position.getBlockZ());

                if (RestrictedCreative.EXTRADEBUG)
                    System.out.println("setBlock: " + b.getType() + " " + Utils.getBlockString(b)
                            + ", " + newBlock.getAsString());

                // If a tracked block is removed
                if (newBlock.getBlockType().getMaterial().isAir()) {
                    BlockHandler.removeTracking(b);
                }
                // The block is changed/placed
                else {
                    BlockHandler.setAsTracked(b);
                }

                return getExtent().setBlock(position, newBlock);
            }
        });
    }
}
