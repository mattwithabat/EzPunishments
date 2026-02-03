package me.mattwithabat.ezPunishments.command;

import me.mattwithabat.ezPunishments.EzPunishments;
import me.mattwithabat.ezPunishments.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnbanCommand implements CommandExecutor, TabCompleter {

    private final EzPunishments plugin;

    public UnbanCommand(EzPunishments plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ezpunishments.unban")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().get("usage.unban"));
            return true;
        }

        String targetName = args[0];
        UUID targetUuid = PlayerUtil.getUUID(targetName);

        if (targetUuid == null) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-found"));
            return true;
        }

        String removerName = sender instanceof Player ? sender.getName() : "Console";

        plugin.getPunishmentManager().unban(targetUuid, removerName).thenAccept(success ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        String msg = plugin.getMessageUtil().get("unban.success");
                        msg = plugin.getMessageUtil().replace(msg, "{player}", targetName);
                        sender.sendMessage(msg);
                    } else {
                        String msg = plugin.getMessageUtil().get("unban.not-banned");
                        msg = plugin.getMessageUtil().replace(msg, "{player}", targetName);
                        sender.sendMessage(msg);
                    }
                })
        );

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}
