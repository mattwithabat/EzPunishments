package me.mattwithabat.ezPunishments.database;

import me.mattwithabat.ezPunishments.EzPunishments;
import me.mattwithabat.ezPunishments.model.Punishment;
import me.mattwithabat.ezPunishments.model.PunishmentType;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLiteDatabase implements Database {

    private final EzPunishments plugin;
    private Connection connection;

    public SQLiteDatabase(EzPunishments plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "punishments.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            plugin.getLogger().info("Connected to SQLite database.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to SQLite database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS punishments (
                id TEXT PRIMARY KEY,
                target_uuid TEXT NOT NULL,
                target_name TEXT NOT NULL,
                type TEXT NOT NULL,
                reason TEXT NOT NULL,
                punisher_name TEXT NOT NULL,
                punisher_uuid TEXT,
                created_at BIGINT NOT NULL,
                expires_at BIGINT NOT NULL,
                target_ip TEXT,
                active INTEGER NOT NULL,
                removed_by TEXT,
                removed_at BIGINT
            )
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_target_uuid ON punishments(target_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_target_ip ON punishments(target_ip)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_active ON punishments(active)");
        }
    }

    @Override
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Disconnected from SQLite database.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close SQLite connection: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Void> savePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO punishments (id, target_uuid, target_name, type, reason, punisher_name, punisher_uuid, created_at, expires_at, target_ip, active, removed_by, removed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, punishment.getId());
                stmt.setString(2, punishment.getTargetUuid().toString());
                stmt.setString(3, punishment.getTargetName());
                stmt.setString(4, punishment.getType().name());
                stmt.setString(5, punishment.getReason());
                stmt.setString(6, punishment.getPunisherName());
                stmt.setString(7, punishment.getPunisherUuid() != null ? punishment.getPunisherUuid().toString() : null);
                stmt.setLong(8, punishment.getCreatedAt());
                stmt.setLong(9, punishment.getExpiresAt());
                stmt.setString(10, punishment.getTargetIp());
                stmt.setInt(11, punishment.isActive() ? 1 : 0);
                stmt.setString(12, punishment.getRemovedBy());
                stmt.setLong(13, punishment.getRemovedAt());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save punishment: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> updatePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE punishments SET active = ?, removed_by = ?, removed_at = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, punishment.isActive() ? 1 : 0);
                stmt.setString(2, punishment.getRemovedBy());
                stmt.setLong(3, punishment.getRemovedAt());
                stmt.setString(4, punishment.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update punishment: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishments(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM punishments WHERE target_uuid = ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    punishments.add(fromResultSet(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get punishments: " + e.getMessage());
            }
            return punishments;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getActivePunishments(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM punishments WHERE target_uuid = ? AND active = 1 ORDER BY created_at DESC";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Punishment p = fromResultSet(rs);
                    if (!p.hasExpired()) {
                        punishments.add(p);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get active punishments: " + e.getMessage());
            }
            return punishments;
        });
    }

    @Override
    public CompletableFuture<Punishment> getActivePunishment(UUID uuid, PunishmentType... types) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sb = new StringBuilder("SELECT * FROM punishments WHERE target_uuid = ? AND active = 1 AND type IN (");
            for (int i = 0; i < types.length; i++) {
                sb.append("?");
                if (i < types.length - 1) sb.append(",");
            }
            sb.append(") ORDER BY created_at DESC LIMIT 1");

            try (PreparedStatement stmt = connection.prepareStatement(sb.toString())) {
                stmt.setString(1, uuid.toString());
                for (int i = 0; i < types.length; i++) {
                    stmt.setString(2 + i, types[i].name());
                }
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Punishment p = fromResultSet(rs);
                    if (!p.hasExpired()) {
                        return p;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get active punishment: " + e.getMessage());
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Punishment> getActiveIpBan(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM punishments WHERE target_ip = ? AND type = 'IP_BAN' AND active = 1 ORDER BY created_at DESC LIMIT 1";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, ip);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Punishment p = fromResultSet(rs);
                    if (!p.hasExpired()) {
                        return p;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get active IP ban: " + e.getMessage());
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getAllPunishments(UUID uuid) {
        return getPunishments(uuid);
    }

    private Punishment fromResultSet(ResultSet rs) throws SQLException {
        String punisherUuidStr = rs.getString("punisher_uuid");
        UUID punisherUuid = punisherUuidStr != null ? UUID.fromString(punisherUuidStr) : null;

        Punishment p = new Punishment(
                rs.getString("id"),
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("target_name"),
                PunishmentType.valueOf(rs.getString("type")),
                rs.getString("reason"),
                rs.getString("punisher_name"),
                punisherUuid,
                rs.getLong("created_at"),
                rs.getLong("expires_at"),
                rs.getString("target_ip"),
                rs.getInt("active") == 1
        );
        p.setRemovedBy(rs.getString("removed_by"));
        p.setRemovedAt(rs.getLong("removed_at"));
        return p;
    }
}
