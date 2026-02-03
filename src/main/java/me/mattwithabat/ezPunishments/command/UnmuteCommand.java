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

public class UnmuteCommand implements CommandExecutor, TabCompleter {

    private final EzPunishments plugin;

    public UnmuteCommand(EzPunishments plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ezpunishments.unmute")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getMessageUtil().get("usage.unmute"));
            return true;
        }

        String targetName = args[0];
        UUID targetUuid = PlayerUtil.getUUID(targetName);

        if (targetUuid == null) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-found"));
            return true;
        }

        String removerName = sender instanceof Player ? sender.getName() : "Console";

        plugin.getPunishmentManager().unmute(targetUuid, removerName).thenAccept(success -> {
            if (success) {
                String msg = plugin.getMessageUtil().get("unmute.success");
                msg = plugin.getMessageUtil().replace(msg, "{player}", targetName);
                sender.sendMessage(msg);

                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null) {
                    target.sendMessage(plugin.getMessageUtil().get("unmute.target-message"));
                }
            } else {
                String msg = plugin.getMessageUtil().get("unmute.not-muted");
                msg = plugin.getMessageUtil().replace(msg, "{player}", targetName);
                sender.sendMessage(msg);
            }
        });

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
