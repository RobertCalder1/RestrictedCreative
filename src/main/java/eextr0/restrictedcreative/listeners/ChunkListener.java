package eextr0.restrictedcreative.listeners;

import eextr0.restrictedcreative.RestrictedCreative;
import eextr0.restrictedcreative.storage.handlers.BlockHandler;
import eextr0.restrictedcreative.utils.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListener implements Listener {
    /*
     * Called when a chunk is loaded
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        // New chunks can't contain creative blocks
        if (e.isNewChunk())
            return;

        if (RestrictedCreative.EXTRADEBUG)
            System.out.println("onChunkLoad: " + Utils.getChunkString(e.getChunk()));

        // BlockHandler.loadChunkFromDatabase(e.getChunk());
        BlockHandler.loadBlocks(e.getChunk());
    }
}
