package me.mattwithabat.ezPunishments.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import me.mattwithabat.ezPunishments.EzPunishments;
import me.mattwithabat.ezPunishments.model.Punishment;
import me.mattwithabat.ezPunishments.model.PunishmentType;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MongoDatabase implements Database {

    private final EzPunishments plugin;
    private MongoClient client;
    private com.mongodb.client.MongoDatabase database;
    private MongoCollection<Document> collection;

    public MongoDatabase(EzPunishments plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        String uri = plugin.getConfig().getString("database.mongodb.uri", "mongodb://localhost:27017");
        String dbName = plugin.getConfig().getString("database.mongodb.database", "ezpunishments");
        String collectionName = plugin.getConfig().getString("database.mongodb.collection", "punishments");

        try {
            client = MongoClients.create(uri);
            database = client.getDatabase(dbName);
            collection = database.getCollection(collectionName);

            collection.createIndex(Indexes.ascending("target_uuid"));
            collection.createIndex(Indexes.ascending("target_ip"));
            collection.createIndex(Indexes.ascending("active"));

            plugin.getLogger().info("Connected to MongoDB database.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to MongoDB database: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        if (client != null) {
            client.close();
            plugin.getLogger().info("Disconnected from MongoDB database.");
        }
    }

    @Override
    public CompletableFuture<Void> savePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            Document doc = new Document()
                    .append("_id", punishment.getId())
                    .append("target_uuid", punishment.getTargetUuid().toString())
                    .append("target_name", punishment.getTargetName())
                    .append("type", punishment.getType().name())
                    .append("reason", punishment.getReason())
                    .append("punisher_name", punishment.getPunisherName())
                    .append("punisher_uuid", punishment.getPunisherUuid() != null ? punishment.getPunisherUuid().toString() : null)
                    .append("created_at", punishment.getCreatedAt())
                    .append("expires_at", punishment.getExpiresAt())
                    .append("target_ip", punishment.getTargetIp())
                    .append("active", punishment.isActive())
                    .append("removed_by", punishment.getRemovedBy())
                    .append("removed_at", punishment.getRemovedAt());
            collection.insertOne(doc);
        });
    }

    @Override
    public CompletableFuture<Void> updatePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            collection.updateOne(
                    Filters.eq("_id", punishment.getId()),
                    new Document("$set", new Document()
                            .append("active", punishment.isActive())
                            .append("removed_by", punishment.getRemovedBy())
                            .append("removed_at", punishment.getRemovedAt()))
            );
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishments(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            for (Document doc : collection.find(Filters.eq("target_uuid", uuid.toString()))) {
                punishments.add(fromDocument(doc));
            }
            punishments.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            return punishments;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getActivePunishments(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            for (Document doc : collection.find(Filters.and(
                    Filters.eq("target_uuid", uuid.toString()),
                    Filters.eq("active", true)))) {
                Punishment p = fromDocument(doc);
                if (!p.hasExpired()) {
                    punishments.add(p);
                }
            }
            punishments.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            return punishments;
        });
    }

    @Override
    public CompletableFuture<Punishment> getActivePunishment(UUID uuid, PunishmentType... types) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> typeNames = Arrays.stream(types).map(Enum::name).collect(Collectors.toList());
            for (Document doc : collection.find(Filters.and(
                    Filters.eq("target_uuid", uuid.toString()),
                    Filters.eq("active", true),
                    Filters.in("type", typeNames)))) {
                Punishment p = fromDocument(doc);
                if (!p.hasExpired()) {
                    return p;
                }
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Punishment> getActiveIpBan(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            for (Document doc : collection.find(Filters.and(
                    Filters.eq("target_ip", ip),
                    Filters.eq("type", "IP_BAN"),
                    Filters.eq("active", true)))) {
                Punishment p = fromDocument(doc);
                if (!p.hasExpired()) {
                    return p;
                }
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getAllPunishments(UUID uuid) {
        return getPunishments(uuid);
    }

    private Punishment fromDocument(Document doc) {
        String punisherUuidStr = doc.getString("punisher_uuid");
        UUID punisherUuid = punisherUuidStr != null ? UUID.fromString(punisherUuidStr) : null;

        Punishment p = new Punishment(
                doc.getString("_id"),
                UUID.fromString(doc.getString("target_uuid")),
                doc.getString("target_name"),
                PunishmentType.valueOf(doc.getString("type")),
                doc.getString("reason"),
                doc.getString("punisher_name"),
                punisherUuid,
                doc.getLong("created_at"),
                doc.getLong("expires_at"),
                doc.getString("target_ip"),
                doc.getBoolean("active")
        );
        p.setRemovedBy(doc.getString("removed_by"));
        Long removedAt = doc.getLong("removed_at");
        p.setRemovedAt(removedAt != null ? removedAt : 0);
        return p;
    }
}
