package me.mattwithabat.ezPunishments.command;

import me.mattwithabat.ezPunishments.EzPunishments;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class UnipbanCommand implements CommandExecutor, TabCompleter {

    private final EzPunishments plugin;

    public UnipbanCommand(EzPunishments plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ezpunishments.unipban")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().get("usage.unipban"));
            return true;
        }

        String ip = args[0];
        String removerName = sender instanceof Player ? sender.getName() : "Console";

        plugin.getPunishmentManager().unipban(ip, removerName).thenAccept(success -> {
            if (success) {
                String msg = plugin.getMessageUtil().get("unipban.success");
                msg = plugin.getMessageUtil().replace(msg, "{ip}", ip);
                sender.sendMessage(msg);
            } else {
                String msg = plugin.getMessageUtil().get("unipban.not-banned");
                msg = plugin.getMessageUtil().replace(msg, "{ip}", ip);
                sender.sendMessage(msg);
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
