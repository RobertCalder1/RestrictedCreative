package eextr0.restrictedcreative.storage.handlers;

import eextr0.restrictedcreative.utils.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class InventoryHandler {
    private static Map<Player, PlayerInfo> survivalInvs = new HashMap<>();
    private static Map<Player, PlayerInfo> creativeInvs = new HashMap<>();

    private static Map<Player, GameMode> previousGameMode = new HashMap<>();
    private static boolean isForceGamemodeEnabled = false;

    public static GameMode getPreviousGameMode(Player p) {
        return previousGameMode.containsKey(p) ? previousGameMode.get(p)
                : Bukkit.getDefaultGameMode();
    }

    public static void setPreviousGameMode(Player p, GameMode gm) {
        previousGameMode.put(p, gm);
    }

    public static void removePreviousGameMode(Player p) {
        if (previousGameMode.containsKey(p))
            previousGameMode.remove(p);
    }

    public static void saveSurvivalInv(Player p, PlayerInfo pi) {
        survivalInvs.put(p, pi);
    }

    public static PlayerInfo getSurvivalInv(Player p) {
        return survivalInvs.containsKey(p) ? survivalInvs.get(p) : null;
    }

    public static void removeSurvivalInv(Player p) {
        if (survivalInvs.containsKey(p))
            survivalInvs.remove(p);
    }

    public static void saveCreativeInv(Player p, PlayerInfo pi) {
        creativeInvs.put(p, pi);
    }

    public static PlayerInfo getCreativeInv(Player p) {
        return creativeInvs.containsKey(p) ? creativeInvs.get(p) : null;
    }

    public static void removeCreativeInv(Player p) {
        if (creativeInvs.containsKey(p))
            creativeInvs.remove(p);
    }

    public static boolean isForceGamemodeEnabled() {
        return isForceGamemodeEnabled;
    }

    public static void setForceGamemodeEnabled(boolean isForceGamemodeEnabled) {
        InventoryHandler.isForceGamemodeEnabled = isForceGamemodeEnabled;
    }
}
