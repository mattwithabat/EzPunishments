package me.mattwithabat.ezPunishments.command;

import me.mattwithabat.ezPunishments.EzPunishments;
import me.mattwithabat.ezPunishments.model.Punishment;
import me.mattwithabat.ezPunishments.model.PunishmentType;
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

public class KickCommand implements CommandExecutor, TabCompleter {

    private final EzPunishments plugin;

    public KickCommand(EzPunishments plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ezpunishments.kick")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageUtil().get("usage.kick"));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-online"));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        String punisherName = sender instanceof Player ? sender.getName() : "Console";
        UUID punisherUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        String targetIp = PlayerUtil.getPlayerIp(target);

        Punishment punishment = new Punishment(
                plugin.getPunishmentManager().createPunishmentId(),
                target.getUniqueId(),
                targetName,
                PunishmentType.KICK,
                reason,
                punisherName,
                punisherUuid,
                System.currentTimeMillis(),
                -1,
                targetIp,
                false
        );

        plugin.getPunishmentManager().punish(punishment).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage(plugin.getMessageUtil().get("kick.success", punishment));

            String broadcastMsg = plugin.getMessageUtil().get("kick.broadcast", punishment);
            if (!broadcastMsg.isEmpty()) {
                Bukkit.broadcastMessage(broadcastMsg);
            }
        }));

        String kickMessage = plugin.getMessageUtil().get("kick.screen", punishment);
        target.kickPlayer(kickMessage);

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
