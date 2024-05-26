package de.hdg.keklist.events;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.extentions.WebhookManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.geysermc.geyser.api.event.connection.ConnectionRequestEvent;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GeyserConnectionEvent {

    private static final FileConfiguration config = Keklist.getInstance().getConfig();

    public static void onConnectionRequestEvent(ConnectionRequestEvent event) {
        String ip = event.inetSocketAddress().getAddress().getHostAddress();

        try (ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", ip)) {
            if (rsIp.next()) {
                if (config.getBoolean("blacklist.allow-join-with-admin")) {
                    for (Player player : Keklist.getInstance().getServer().getOnlinePlayers()) {
                        if (player.hasPermission(config.getString("blacklist.admin-permission"))) {
                            return;
                        }
                    }
                }

                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", ip)), "keklist.notify.kicked");

                if (Keklist.getWebhookManager() != null)
                    Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, ip, rsIp.getString("byPlayer"), null, System.currentTimeMillis());

                Bukkit.getConsoleSender().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", ip)));

                event.setCancelled(true);
            }
        } catch (SQLException e) {
            Keklist.getInstance().getLogger().severe(Keklist.getTranslations().get("geyser.events.error", e.getMessage()));
            Keklist.getInstance().getLogger().severe(Keklist.getTranslations().get("geyser.events.fallback"));
            event.setCancelled(true);
        }

    }
}
