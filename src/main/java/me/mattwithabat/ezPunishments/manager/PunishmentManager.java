package me.mattwithabat.ezPunishments.manager;

import me.mattwithabat.ezPunishments.EzPunishments;
import me.mattwithabat.ezPunishments.database.Database;
import me.mattwithabat.ezPunishments.model.Punishment;
import me.mattwithabat.ezPunishments.model.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager {

    private final EzPunishments plugin;
    private final Database database;
    private final Map<UUID, List<Punishment>> cache = new ConcurrentHashMap<>();
    private final Map<String, Punishment> ipBanCache = new ConcurrentHashMap<>();

    public PunishmentManager(EzPunishments plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public CompletableFuture<Void> punish(Punishment punishment) {
        return database.savePunishment(punishment).thenRun(() -> {
            cache.computeIfAbsent(punishment.getTargetUuid(), k -> new ArrayList<>()).add(0, punishment);
            if (punishment.getType() == PunishmentType.IP_BAN && punishment.getTargetIp() != null) {
                ipBanCache.put(punishment.getTargetIp(), punishment);
            }
        });
    }

    public CompletableFuture<Boolean> unban(UUID uuid, String removerName) {
        return database.getActivePunishment(uuid, PunishmentType.BAN, PunishmentType.TEMP_BAN)
                .thenCompose(punishment -> {
                    if (punishment == null) return CompletableFuture.completedFuture(false);
                    punishment.setActive(false);
                    punishment.setRemovedBy(removerName);
                    punishment.setRemovedAt(System.currentTimeMillis());
                    return database.updatePunishment(punishment).thenApply(v -> {
                        invalidateCache(uuid);
                        return true;
                    });
                });
    }

    public CompletableFuture<Boolean> unmute(UUID uuid, String removerName) {
        return database.getActivePunishment(uuid, PunishmentType.MUTE, PunishmentType.TEMP_MUTE)
                .thenCompose(punishment -> {
                    if (punishment == null) return CompletableFuture.completedFuture(false);
                    punishment.setActive(false);
                    punishment.setRemovedBy(removerName);
                    punishment.setRemovedAt(System.currentTimeMillis());
                    return database.updatePunishment(punishment).thenApply(v -> {
                        invalidateCache(uuid);
                        return true;
                    });
                });
    }

    public CompletableFuture<Boolean> unipban(String ip, String removerName) {
        return database.getActiveIpBan(ip).thenCompose(punishment -> {
            if (punishment == null) return CompletableFuture.completedFuture(false);
            punishment.setActive(false);
            punishment.setRemovedBy(removerName);
            punishment.setRemovedAt(System.currentTimeMillis());
            return database.updatePunishment(punishment).thenApply(v -> {
                ipBanCache.remove(ip);
                invalidateCache(punishment.getTargetUuid());
                return true;
            });
        });
    }

    public CompletableFuture<Punishment> getActiveBan(UUID uuid) {
        return database.getActivePunishment(uuid, PunishmentType.BAN, PunishmentType.TEMP_BAN);
    }

    public CompletableFuture<Punishment> getActiveMute(UUID uuid) {
        return database.getActivePunishment(uuid, PunishmentType.MUTE, PunishmentType.TEMP_MUTE);
    }

    public CompletableFuture<Punishment> getActiveIpBan(String ip) {
        Punishment cached = ipBanCache.get(ip);
        if (cached != null && cached.isActive() && !cached.hasExpired()) {
            return CompletableFuture.completedFuture(cached);
        }
        return database.getActiveIpBan(ip).thenApply(p -> {
            if (p != null) ipBanCache.put(ip, p);
            return p;
        });
    }

    public CompletableFuture<List<Punishment>> getHistory(UUID uuid) {
        return database.getAllPunishments(uuid);
    }

    public void invalidateCache(UUID uuid) {
        cache.remove(uuid);
    }

    public void clearCache() {
        cache.clear();
        ipBanCache.clear();
    }

    public String createPunishmentId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public void kickPlayer(UUID uuid, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.kickPlayer(message);
            }
        });
    }
}
