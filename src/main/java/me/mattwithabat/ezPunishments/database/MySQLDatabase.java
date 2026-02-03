package me.mattwithabat.ezPunishments.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.mattwithabat.ezPunishments.EzPunishments;
import me.mattwithabat.ezPunishments.model.Punishment;
import me.mattwithabat.ezPunishments.model.PunishmentType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySQLDatabase implements Database {

    private final EzPunishments plugin;
    private HikariDataSource dataSource;

    public MySQLDatabase(EzPunishments plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "ezpunishments");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");
        int poolSize = plugin.getConfig().getInt("database.mysql.pool-size", 10);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(600000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            dataSource = new HikariDataSource(config);
            createTables();
            plugin.getLogger().info("Connected to MySQL database.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
        }
    }

    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS punishments (
                id VARCHAR(36) PRIMARY KEY,
                target_uuid VARCHAR(36) NOT NULL,
                target_name VARCHAR(16) NOT NULL,
                type VARCHAR(20) NOT NULL,
                reason TEXT NOT NULL,
                punisher_name VARCHAR(16) NOT NULL,
                punisher_uuid VARCHAR(36),
                created_at BIGINT NOT NULL,
                expires_at BIGINT NOT NULL,
                target_ip VARCHAR(45),
                active TINYINT(1) NOT NULL,
                removed_by VARCHAR(16),
                removed_at BIGINT,
                INDEX idx_target_uuid (target_uuid),
                INDEX idx_target_ip (target_ip),
                INDEX idx_active (active)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """;
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create MySQL tables: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Disconnected from MySQL database.");
        }
    }

    @Override
    public CompletableFuture<Void> savePunishment(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO punishments (id, target_uuid, target_name, type, reason, punisher_name, punisher_uuid, created_at, expires_at, target_ip, active, removed_by, removed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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

            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sb.toString())) {
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
            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
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
