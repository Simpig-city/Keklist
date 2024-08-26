package de.hdg.keklist.events;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.util.IpUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class NotifyJoinEvent implements Listener {

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        if (Keklist.getInstance().getConfig().getBoolean("ip.send-message-on-join")) {
            new IpUtil(event.getPlayer().getAddress().getAddress().getHostAddress()).getIpData().thenAccept(data ->
                    Bukkit.getOnlinePlayers().stream().filter(online -> online.hasPermission("keklist.notify.ip")).forEach(notifyPlayer -> {
                        notifyPlayer.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.ip-info")
                                .replace("%ip%", event.getPlayer().getAddress().getAddress().getHostAddress())
                                .replace("%country%", data.country())
                                .replace("%country_code%", data.countryCode())
                                .replace("%continent%", data.continent())
                                .replace("%continent_code%", data.continentCode())
                                .replace("%region%", data.regionName())
                                .replace("%city%", data.city())
                                .replace("%org%", data.org())
                                .replace("%as%", data.as())
                                .replace("%timezone%", data.timezone())
                                .replace("%mobile%", data.mobile() ? "<green>" + Keklist.getTranslations().get("yes") : "<red>" + Keklist.getTranslations().get("no"))
                                .replace("%proxy%", data.proxy() ? "<green>" + Keklist.getTranslations().get("yes") : "<red>" + Keklist.getTranslations().get("no"))
                                .replace("%hosting%", data.hosting() ? "<green>" + Keklist.getTranslations().get("yes") : "<red>" + Keklist.getTranslations().get("no"))
                                .replace("%query%", data.query())
                                .replace("%player%", event.getPlayer().getName())
                        ));
                    })
            );
        }
    }
}
