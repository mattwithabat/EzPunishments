package me.mattwithabat.ezPunishments.command;

import me.mattwithabat.ezPunishments.EzPunishments;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class EzPunishmentsCommand implements CommandExecutor, TabCompleter {

    private final EzPunishments plugin;

    public EzPunishmentsCommand(EzPunishments plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ezpunishments.reload")) {
                sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
                return true;
            }

            plugin.reloadAllConfigs();
            sender.sendMessage(plugin.getMessageUtil().get("reload-success"));
            return true;
        }

        sender.sendMessage(plugin.getMessageUtil().color("&6EzPunishments &7v" + plugin.getDescription().getVersion()));
        sender.sendMessage(plugin.getMessageUtil().color("&7Use &f/ezpunishments reload &7to reload configs."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("ezpunishments.reload")) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
        }
        return completions;
    }
}
