package me.mattwithabat.ezPunishments.util;

import me.mattwithabat.ezPunishments.EzPunishments;
import me.mattwithabat.ezPunishments.model.Punishment;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.stream.Collectors;

public class MessageUtil {

    private final EzPunishments plugin;
    private FileConfiguration messages;

    public MessageUtil(EzPunishments plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.messages = plugin.getMessagesConfig();
    }

    public String get(String path) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        return color(message);
    }

    public String get(String path, Punishment punishment) {
        String message = get(path);
        return replacePlaceholders(message, punishment);
    }

    public List<String> getList(String path) {
        return messages.getStringList(path).stream()
                .map(this::color)
                .collect(Collectors.toList());
    }

    public List<String> getList(String path, Punishment punishment) {
        return messages.getStringList(path).stream()
                .map(this::color)
                .map(s -> replacePlaceholders(s, punishment))
                .collect(Collectors.toList());
    }

    public String replacePlaceholders(String message, Punishment punishment) {
        if (punishment == null) return message;

        return message
                .replace("{player}", punishment.getTargetName())
                .replace("{punisher}", punishment.getPunisherName())
                .replace("{reason}", punishment.getReason())
                .replace("{duration}", punishment.isPermanent() ? "Permanent" : TimeUtil.formatDuration(punishment.getExpiresAt() - punishment.getCreatedAt()))
                .replace("{remaining}", TimeUtil.formatRemaining(punishment.getExpiresAt()))
                .replace("{date}", TimeUtil.formatDate(punishment.getCreatedAt()))
                .replace("{type}", formatType(punishment.getType().name()))
                .replace("{id}", punishment.getId())
                .replace("{ip}", punishment.getTargetIp() != null ? punishment.getTargetIp() : "N/A");
    }

    public String replace(String message, String placeholder, String value) {
        return message.replace(placeholder, value);
    }

    private String formatType(String type) {
        return type.replace("_", " ").toLowerCase();
    }

    public String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getBanMessage(Punishment punishment) {
        List<String> lines = getList("ban-screen", punishment);
        return String.join("\n", lines);
    }

    public String getIpBanMessage(Punishment punishment) {
        List<String> lines = getList("ipban-screen", punishment);
        return String.join("\n", lines);
    }
}
