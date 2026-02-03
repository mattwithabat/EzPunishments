package me.mattwithabat.ezPunishments.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerUtil {

    public static UUID getUUID(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return offline.getUniqueId();
        }
        return null;
    }

    public static String getPlayerIp(Player player) {
        if (player.getAddress() != null) {
            return player.getAddress().getAddress().getHostAddress();
        }
        return null;
    }

    public static String getPlayerIp(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return getPlayerIp(player);
        }
        return null;
    }
}
