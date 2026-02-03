package me.mattwithabat.ezPunishments.listener;

import me.mattwithabat.ezPunishments.EzPunishments;
import me.mattwithabat.ezPunishments.model.Punishment;
import me.mattwithabat.ezPunishments.util.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PunishmentListener implements Listener {

    private final EzPunishments plugin;

    public PunishmentListener(EzPunishments plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();

        try {
            Punishment ipBan = plugin.getPunishmentManager().getActiveIpBan(ip).get();
            if (ipBan != null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        plugin.getMessageUtil().getIpBanMessage(ipBan));
                return;
            }

            Punishment ban = plugin.getPunishmentManager().getActiveBan(uuid).get();
            if (ban != null) {
                if (ban.hasExpired()) {
                    ban.setActive(false);
                    plugin.getPunishmentManager().invalidateCache(uuid);
                    plugin.getDatabase().updatePunishment(ban);
                } else {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            plugin.getMessageUtil().getBanMessage(ban));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().warning("Failed to check bans for " + event.getName() + ": " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        try {
            Punishment mute = plugin.getPunishmentManager().getActiveMute(uuid).get();
            if (mute != null) {
                if (mute.hasExpired()) {
                    mute.setActive(false);
                    plugin.getPunishmentManager().invalidateCache(uuid);
                    plugin.getDatabase().updatePunishment(mute);
                } else {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getMessageUtil().get("muted-message", mute));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().warning("Failed to check mute for " + player.getName() + ": " + e.getMessage());
        }
    }
}
