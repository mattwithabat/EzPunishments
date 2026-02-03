package me.mattwithabat.ezPunishments;

import me.mattwithabat.ezPunishments.command.*;
import me.mattwithabat.ezPunishments.database.Database;
import me.mattwithabat.ezPunishments.database.MongoDatabase;
import me.mattwithabat.ezPunishments.database.MySQLDatabase;
import me.mattwithabat.ezPunishments.database.SQLiteDatabase;
import me.mattwithabat.ezPunishments.listener.PunishmentListener;
import me.mattwithabat.ezPunishments.manager.PunishmentManager;
import me.mattwithabat.ezPunishments.util.MessageUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class EzPunishments extends JavaPlugin {

    private Database database;
    private PunishmentManager punishmentManager;
    private MessageUtil messageUtil;
    private FileConfiguration messagesConfig;
    private FileConfiguration menuConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("menu.yml", false);

        loadConfigs();

        initDatabase();
        punishmentManager = new PunishmentManager(this, database);
        messageUtil = new MessageUtil(this);

        registerCommands();
        registerListeners();

        getLogger().info("EzPunishments enabled.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.disconnect();
        }
        getLogger().info("EzPunishments disabled.");
    }

    private void initDatabase() {
        String type = getConfig().getString("database.type", "sqlite").toLowerCase();

        database = switch (type) {
            case "mysql" -> new MySQLDatabase(this);
            case "mongodb", "mongo" -> new MongoDatabase(this);
            default -> new SQLiteDatabase(this);
        };

        database.connect();
    }

    private void loadConfigs() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        File menuFile = new File(getDataFolder(), "menu.yml");
        menuConfig = YamlConfiguration.loadConfiguration(menuFile);
    }

    public void reloadAllConfigs() {
        reloadConfig();
        loadConfigs();
        if (messageUtil != null) {
            messageUtil.reload();
        }
    }

    private void registerCommands() {
        registerCommand("ban", new BanCommand(this));
        registerCommand("tempban", new TempBanCommand(this));
        registerCommand("mute", new MuteCommand(this));
        registerCommand("tempmute", new TempMuteCommand(this));
        registerCommand("ipban", new IpBanCommand(this));
        registerCommand("kick", new KickCommand(this));
        registerCommand("unban", new UnbanCommand(this));
        registerCommand("unmute", new UnmuteCommand(this));
        registerCommand("unipban", new UnipbanCommand(this));
        registerCommand("history", new HistoryCommand(this));
        registerCommand("ezpunishments", new EzPunishmentsCommand(this));
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor((org.bukkit.command.CommandExecutor) executor);
            if (executor instanceof org.bukkit.command.TabCompleter) {
                cmd.setTabCompleter((org.bukkit.command.TabCompleter) executor);
            }
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PunishmentListener(this), this);
    }

    public Database getDatabase() {
        return database;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }
}
