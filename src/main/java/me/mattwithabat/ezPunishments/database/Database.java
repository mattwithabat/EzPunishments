package me.mattwithabat.ezPunishments.database;

import me.mattwithabat.ezPunishments.model.Punishment;
import me.mattwithabat.ezPunishments.model.PunishmentType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Database {

    void connect();

    void disconnect();

    CompletableFuture<Void> savePunishment(Punishment punishment);

    CompletableFuture<Void> updatePunishment(Punishment punishment);

    CompletableFuture<List<Punishment>> getPunishments(UUID uuid);

    CompletableFuture<List<Punishment>> getActivePunishments(UUID uuid);

    CompletableFuture<Punishment> getActivePunishment(UUID uuid, PunishmentType... types);

    CompletableFuture<Punishment> getActiveIpBan(String ip);

    CompletableFuture<List<Punishment>> getAllPunishments(UUID uuid);
}
