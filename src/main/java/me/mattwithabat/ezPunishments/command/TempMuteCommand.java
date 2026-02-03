package me.mattwithabat.ezPunishments.command;

import me.mattwithabat.ezPunishments.EzPunishments;
import me.mattwithabat.ezPunishments.model.Punishment;
import me.mattwithabat.ezPunishments.model.PunishmentType;
import me.mattwithabat.ezPunishments.util.PlayerUtil;
import me.mattwithabat.ezPunishments.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TempMuteCommand implements CommandExecutor, TabCompleter {

    private final EzPunishments plugin;

    public TempMuteCommand(EzPunishments plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ezpunishments.tempmute")) {
            sender.sendMessage(plugin.getMessageUtil().get("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getMessageUtil().get("usage.tempmute"));
            return true;
        }

        String targetName = args[0];
        UUID targetUuid = PlayerUtil.getUUID(targetName);

        if (targetUuid == null) {
            sender.sendMessage(plugin.getMessageUtil().get("player-not-found"));
            return true;
        }

        long duration = TimeUtil.parseDuration(args[1]);
        if (duration <= 0) {
            sender.sendMessage(plugin.getMessageUtil().get("invalid-duration"));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        String punisherName = sender instanceof Player ? sender.getName() : "Console";
        UUID punisherUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        String targetIp = PlayerUtil.getPlayerIp(targetUuid);

        long now = System.currentTimeMillis();
        Punishment punishment = new Punishment(
                plugin.getPunishmentManager().createPunishmentId(),
                targetUuid,
                targetName,
                PunishmentType.TEMP_MUTE,
                reason,
                punisherName,
                punisherUuid,
                now,
                now + duration,
                targetIp,
                true
        );

        plugin.getPunishmentManager().punish(punishment).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage(plugin.getMessageUtil().get("tempmute.success", punishment));

            String broadcastMsg = plugin.getMessageUtil().get("tempmute.broadcast", punishment);
            if (!broadcastMsg.isEmpty()) {
                Bukkit.broadcastMessage(broadcastMsg);
            }

            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null) {
                target.sendMessage(plugin.getMessageUtil().get("tempmute.target-message", punishment));
            }
        }));

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
        } else if (args.length == 2) {
            completions.add("1h");
            completions.add("1d");
            completions.add("7d");
            completions.add("30d");
        }
        return completions;
    }
}
