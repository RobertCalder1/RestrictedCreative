package eextr0.restrictedcreative.storage.handlers;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class CommandHandler {
    private static Set<Player> addWithCommand = new HashSet<>();
    private static Set<Player> removeWithCommand = new HashSet<>();
    private static Set<Player> infoWithCommand = new HashSet<>();

    public static Set<Player> getAddWithCommand() {
        return addWithCommand;
    }

    public static Set<Player> getRemoveWithCommand() {
        return removeWithCommand;
    }

    public static Set<Player> getInfoWithCommand() {
        return infoWithCommand;
    }

    public static boolean isAddWithCommand(Player p) {
        return getAddWithCommand().contains(p);
    }

    public static boolean isRemoveWithCommand(Player p) {
        return getRemoveWithCommand().contains(p);
    }

    public static boolean isInfoWithCommand(Player p) {
        return getInfoWithCommand().contains(p);
    }

    public static void removeAddWithCommand(Player p) {
        if (isAddWithCommand(p))
            getAddWithCommand().remove(p);
    }

    public static void removeRemoveWithCommand(Player p) {
        if (isRemoveWithCommand(p))
            getRemoveWithCommand().remove(p);
    }

    public static void removeInfoWithCommand(Player p) {
        if (isInfoWithCommand(p))
            getInfoWithCommand().remove(p);
    }

    public static void setAddWithCommand(Player p) {
        getAddWithCommand().add(p);
    }

    public static void setRemoveWithCommand(Player p) {
        getRemoveWithCommand().add(p);
    }

    public static void setInfoWithCommand(Player p) {
        getInfoWithCommand().add(p);
    }
}
