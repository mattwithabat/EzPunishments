package me.mattwithabat.ezPunishments.model;

import java.util.UUID;

public class Punishment {

    private final String id;
    private final UUID targetUuid;
    private final String targetName;
    private final PunishmentType type;
    private final String reason;
    private final String punisherName;
    private final UUID punisherUuid;
    private final long createdAt;
    private final long expiresAt;
    private final String targetIp;
    private boolean active;
    private String removedBy;
    private long removedAt;

    public Punishment(String id, UUID targetUuid, String targetName, PunishmentType type,
                      String reason, String punisherName, UUID punisherUuid,
                      long createdAt, long expiresAt, String targetIp, boolean active) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.type = type;
        this.reason = reason;
        this.punisherName = punisherName;
        this.punisherUuid = punisherUuid;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.targetIp = targetIp;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public PunishmentType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public String getPunisherName() {
        return punisherName;
    }

    public UUID getPunisherUuid() {
        return punisherUuid;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(String removedBy) {
        this.removedBy = removedBy;
    }

    public long getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(long removedAt) {
        this.removedAt = removedAt;
    }

    public boolean isPermanent() {
        return expiresAt == -1;
    }

    public boolean hasExpired() {
        if (isPermanent()) return false;
        return System.currentTimeMillis() >= expiresAt;
    }
}
